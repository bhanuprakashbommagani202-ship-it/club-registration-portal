package com.clubportal.service;

import com.clubportal.dto.*;
import com.clubportal.model.*;
import com.clubportal.repository.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final ClubRepository clubRepository;
    private final MembershipRepository membershipRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final ClubService clubService;
    private final MembershipService membershipService;
    private final EventService eventService;
    private final int maxActiveClubs;

    public DashboardService(ClubRepository clubRepository, MembershipRepository membershipRepository,
                            EventRepository eventRepository, UserRepository userRepository, ClubService clubService,
                            MembershipService membershipService, EventService eventService,
                            @Value("${app.max-active-clubs:3}") int maxActiveClubs) {
        this.clubRepository = clubRepository;
        this.membershipRepository = membershipRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.clubService = clubService;
        this.membershipService = membershipService;
        this.eventService = eventService;
        this.maxActiveClubs = maxActiveClubs;
    }

    public Dashboard forUser(User user) {
        return switch (user.getRole()) {
            case STUDENT -> student(user);
            case COORDINATOR -> coordinator(user);
            case ADMIN -> admin(user);
        };
    }

    private Dashboard student(User user) {
        List<Membership> memberships = membershipRepository.findByUserIdOrderByAppliedAtDesc(user.getId());
        List<Club> myClubs = memberships.stream()
                .filter(m -> m.getStatus() == MembershipStatus.APPROVED).map(Membership::getClub).toList();
        List<MembershipView> pending = memberships.stream()
                .filter(m -> m.getStatus() == MembershipStatus.PENDING).map(membershipService::toView).toList();
        List<EventSummary> events = eventService.summaries(
                eventRepository.findUpcomingRegisteredBy(user.getId(), Instant.now(), PageRequest.of(0, 5)), user);
        long active = myClubs.size() + pending.size();

        List<StatItem> stats = List.of(
                new StatItem("My clubs", String.valueOf(myClubs.size()), "Approved memberships"),
                new StatItem("Pending", String.valueOf(pending.size()), "Waiting for a coordinator"),
                new StatItem("My upcoming events",
                        String.valueOf(eventRepository.countUpcomingRegisteredBy(user.getId(), Instant.now())),
                        "Events you registered for"),
                new StatItem("Club slots left", String.valueOf(Math.max(0, maxActiveClubs - active)),
                        "You can be in up to " + maxActiveClubs + " clubs"));
        return new Dashboard("STUDENT", stats, clubService.summaries(myClubs, user), pending, events, List.of());
    }

    private Dashboard coordinator(User user) {
        List<ClubSummary> clubs = clubService.summaries(clubRepository.findByCoordinatorIdOrderByNameAsc(user.getId()), user);
        List<MembershipView> pending = membershipService.pendingFor(user);
        List<EventSummary> events = eventService.summaries(
                eventRepository.findUpcomingForCoordinator(user.getId(), Instant.now(), PageRequest.of(0, 5)), user);
        long members = clubs.stream().mapToLong(ClubSummary::memberCount).sum();

        List<StatItem> stats = List.of(
                new StatItem("Clubs you run", String.valueOf(clubs.size()), null),
                new StatItem("Total members", String.valueOf(members), "Across your clubs"),
                new StatItem("Applications to review", String.valueOf(pending.size()), "Approve or reject below"),
                new StatItem("Upcoming events", String.valueOf(events.size()), "Next 5 shown"));
        return new Dashboard("COORDINATOR", stats, clubs, pending, events, List.of());
    }

    private Dashboard admin(User user) {
        Instant now = Instant.now();
        List<ClubSummary> all = clubService.summaries(clubRepository.findAllByOrderByNameAsc(), user);
        List<ClubSummary> top = all.stream()
                .sorted(Comparator.comparingLong(ClubSummary::memberCount).reversed()).limit(5).toList();
        List<MembershipView> pending = membershipService.pendingFor(user);
        List<EventSummary> events = eventService.summaries(eventRepository.findUpcoming(null, now, PageRequest.of(0, 5)), user);

        Map<String, Long> byCategory = new LinkedHashMap<>();
        for (ClubCategory c : ClubCategory.values()) {
            byCategory.put(c.name(), 0L);
        }
        for (Object[] row : membershipRepository.countPerCategory(MembershipStatus.APPROVED)) {
            byCategory.put(((ClubCategory) row[0]).name(), (Long) row[1]);
        }
        List<CountItem> categoryItems = new ArrayList<>();
        byCategory.forEach((k, v) -> categoryItems.add(new CountItem(k, v)));

        List<StatItem> stats = List.of(
                new StatItem("Clubs", String.valueOf(all.size()),
                        all.stream().filter(ClubSummary::registrationOpen).count() + " open for registration"),
                new StatItem("Students", String.valueOf(userRepository.countByRole(Role.STUDENT)), "Registered accounts"),
                new StatItem("Memberships", String.valueOf(membershipRepository.countByStatus(MembershipStatus.APPROVED)),
                        "Approved across all clubs"),
                new StatItem("Pending applications", String.valueOf(pending.size()), "Waiting for a decision"),
                new StatItem("Upcoming events", String.valueOf(eventRepository.countByStartsAtAfterAndCancelledFalse(now)),
                        null));
        return new Dashboard("ADMIN", stats, top, pending.stream().limit(10).toList(), events, categoryItems);
    }
}
