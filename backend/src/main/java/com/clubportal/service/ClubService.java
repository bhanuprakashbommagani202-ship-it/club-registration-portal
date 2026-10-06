package com.clubportal.service;

import com.clubportal.dto.*;
import com.clubportal.exception.ApiException;
import com.clubportal.model.*;
import com.clubportal.repository.ClubRepository;
import com.clubportal.repository.EventRepository;
import com.clubportal.repository.MembershipRepository;
import com.clubportal.repository.UserRepository;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ClubService {

    private final ClubRepository clubRepository;
    private final MembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final EventService eventService;
    private final int maxActiveClubs;

    public ClubService(ClubRepository clubRepository, MembershipRepository membershipRepository,
                       UserRepository userRepository, EventRepository eventRepository, EventService eventService,
                       @Value("${app.max-active-clubs:3}") int maxActiveClubs) {
        this.clubRepository = clubRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.eventService = eventService;
        this.maxActiveClubs = maxActiveClubs;
    }

    // ---- read -----------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<ClubSummary> list(User viewer, String q, ClubCategory category, boolean openOnly) {
        String text = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        List<Club> clubs = clubRepository.findAllByOrderByNameAsc().stream()
                .filter(c -> category == null || c.getCategory() == category)
                .filter(c -> !openOnly || c.isRegistrationOpen())
                .filter(c -> text.isEmpty() || contains(c.getName(), text) || contains(c.getTagline(), text)
                        || contains(c.getDescription(), text))
                .toList();
        return summaries(clubs, viewer);
    }

    @Transactional(readOnly = true)
    public ClubDetail get(Long id, User viewer) {
        Club club = clubRepository.findById(id).orElseThrow(() -> ApiException.notFound("Club"));
        boolean manages = AccessPolicy.manages(club, viewer);
        long members = membershipRepository.countByClubIdAndStatus(id, MembershipStatus.APPROVED);
        Long pending = manages ? membershipRepository.countByClubIdAndStatus(id, MembershipStatus.PENDING) : null;
        boolean full = members >= club.getCapacity();

        Membership mine = membershipRepository.findByUserIdAndClubId(viewer.getId(), id).orElse(null);
        MyMembership myMembership = mine == null ? null : new MyMembership(mine.getId(), mine.getStatus(),
                mine.getMotivation(), mine.getRemark(), mine.getAppliedAt(), mine.getDecidedAt());

        String blocked = applyBlockedReason(club, viewer, mine, full);
        ClubPermissions permissions = new ClubPermissions(blocked == null, blocked, manages, viewer.isAdmin());

        User coordinator = club.getCoordinator();
        List<EventSummary> events = eventService.summaries(
                eventRepository.findUpcoming(id, Instant.now(), PageRequest.of(0, 10)), viewer);

        return new ClubDetail(club.getId(), club.getName(), club.getCategory(), club.getTagline(),
                club.getDescription(), club.getMeetingSchedule(), club.getVenue(), club.getCapacity(), members,
                pending, club.isRegistrationOpen(), full,
                coordinator == null ? null : new PersonRef(coordinator.getId(), coordinator.getName(),
                        coordinator.getEmail()),
                club.getCreatedAt(), myMembership, permissions, events);
    }

    // ---- write ----------------------------------------------------------------------------

    public ClubDetail create(ClubRequest req, User admin) {
        String name = req.name().trim();
        if (clubRepository.existsByNameIgnoreCase(name)) {
            throw ApiException.conflict("DUPLICATE_CLUB", "A club with this name already exists.");
        }
        Club club = new Club();
        club.setName(name);
        club.setCategory(req.category());
        club.setCoordinator(resolveCoordinator(req.coordinatorId()));
        applyEditableFields(club, req);
        clubRepository.save(club);
        return get(club.getId(), admin);
    }

    /** Admins can change everything; a club's coordinator can change everything except name, category, coordinator. */
    public ClubDetail update(Long id, ClubRequest req, User actor) {
        Club club = clubRepository.lockById(id).orElseThrow(() -> ApiException.notFound("Club"));
        if (!AccessPolicy.manages(club, actor)) {
            throw ApiException.forbidden("Only this club's coordinator or an admin can edit it.");
        }
        long members = membershipRepository.countByClubIdAndStatus(id, MembershipStatus.APPROVED);
        if (req.capacity() < members) {
            throw ApiException.conflict("CAPACITY_TOO_LOW",
                    "Capacity can't be lower than the current " + members + " members.");
        }
        if (actor.isAdmin()) {
            String name = req.name().trim();
            if (clubRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
                throw ApiException.conflict("DUPLICATE_CLUB", "A club with this name already exists.");
            }
            club.setName(name);
            club.setCategory(req.category());
            club.setCoordinator(resolveCoordinator(req.coordinatorId()));
        }
        applyEditableFields(club, req);
        return get(id, actor);
    }

    // ---- helpers --------------------------------------------------------------------------

    /** Builds list cards with member counts, pending counts (for managers) and the viewer's own status. */
    @Transactional(readOnly = true)
    public List<ClubSummary> summaries(List<Club> clubs, User viewer) {
        if (clubs.isEmpty()) {
            return List.of();
        }
        Map<Long, Long> members = countMap(membershipRepository.countPerClub(MembershipStatus.APPROVED));
        Map<Long, Long> pending = countMap(membershipRepository.countPerClub(MembershipStatus.PENDING));
        Map<Long, MembershipStatus> mine = new HashMap<>();
        for (Membership m : membershipRepository.findByUserIdOrderByAppliedAtDesc(viewer.getId())) {
            mine.put(m.getClub().getId(), m.getStatus());
        }
        return clubs.stream().map(c -> {
            long count = members.getOrDefault(c.getId(), 0L);
            return new ClubSummary(c.getId(), c.getName(), c.getCategory(), c.getTagline(), c.getMeetingSchedule(),
                    c.getVenue(), c.getCapacity(), count,
                    AccessPolicy.manages(c, viewer) ? pending.getOrDefault(c.getId(), 0L) : null,
                    c.isRegistrationOpen(), count >= c.getCapacity(),
                    c.getCoordinator() == null ? null : c.getCoordinator().getName(),
                    mine.get(c.getId()));
        }).toList();
    }

    private String applyBlockedReason(Club club, User viewer, Membership mine, boolean full) {
        if (viewer.getRole() != Role.STUDENT) {
            return "Only students can join clubs.";
        }
        if (mine != null && mine.getStatus() == MembershipStatus.APPROVED) {
            return "You are a member of this club.";
        }
        if (mine != null && mine.getStatus() == MembershipStatus.PENDING) {
            return "Your application is waiting for the coordinator.";
        }
        if (!club.isRegistrationOpen()) {
            return "Registrations are closed right now.";
        }
        long active = membershipRepository.countByUserIdAndStatusIn(viewer.getId(),
                List.of(MembershipStatus.PENDING, MembershipStatus.APPROVED));
        if (active >= maxActiveClubs) {
            return "You've reached the limit of " + maxActiveClubs + " clubs (pending applications count too).";
        }
        if (full) {
            return "This club is full.";
        }
        return null;
    }

    private void applyEditableFields(Club club, ClubRequest req) {
        club.setTagline(Text.clean(req.tagline()));
        club.setDescription(req.description().trim());
        club.setMeetingSchedule(Text.clean(req.meetingSchedule()));
        club.setVenue(Text.clean(req.venue()));
        club.setCapacity(req.capacity());
        if (req.registrationOpen() != null) {
            club.setRegistrationOpen(req.registrationOpen());
        }
    }

    private User resolveCoordinator(Long coordinatorId) {
        if (coordinatorId == null) {
            return null;
        }
        return userRepository.findById(coordinatorId)
                .filter(u -> u.getRole() == Role.COORDINATOR && u.isActive())
                .orElseThrow(() -> ApiException.badRequest("INVALID_COORDINATOR", "Choose an active coordinator."));
    }

    static Map<Long, Long> countMap(List<Object[]> rows) {
        Map<Long, Long> map = new HashMap<>();
        for (Object[] row : rows) {
            map.put((Long) row[0], (Long) row[1]);
        }
        return map;
    }

    private static boolean contains(String haystack, String needle) {
        return haystack != null && haystack.toLowerCase(Locale.ROOT).contains(needle);
    }
}
