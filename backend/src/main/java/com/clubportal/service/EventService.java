package com.clubportal.service;

import com.clubportal.dto.AttendeeView;
import com.clubportal.dto.EventRequest;
import com.clubportal.dto.EventSummary;
import com.clubportal.dto.RsvpResponse;
import com.clubportal.exception.ApiException;
import com.clubportal.model.*;
import com.clubportal.repository.ClubRepository;
import com.clubportal.repository.EventRegistrationRepository;
import com.clubportal.repository.EventRepository;
import com.clubportal.repository.MembershipRepository;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class EventService {

    private static final PageRequest LIMIT = PageRequest.of(0, 100);

    private final EventRepository eventRepository;
    private final EventRegistrationRepository registrationRepository;
    private final ClubRepository clubRepository;
    private final MembershipRepository membershipRepository;

    public EventService(EventRepository eventRepository, EventRegistrationRepository registrationRepository,
                        ClubRepository clubRepository, MembershipRepository membershipRepository) {
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
        this.clubRepository = clubRepository;
        this.membershipRepository = membershipRepository;
    }

    /** scope: upcoming (default), past, mine (upcoming events I registered for). */
    @Transactional(readOnly = true)
    public List<EventSummary> list(User viewer, String scope, Long clubId) {
        Instant now = Instant.now();
        List<Event> events = switch (scope == null || scope.isBlank() ? "upcoming" : scope) {
            case "upcoming" -> eventRepository.findUpcoming(clubId, now, LIMIT);
            case "past" -> eventRepository.findPast(clubId, now, LIMIT);
            case "mine" -> eventRepository.findUpcomingRegisteredBy(viewer.getId(), now, LIMIT);
            default -> throw ApiException.badRequest("INVALID_PARAMETER", "scope must be upcoming, past or mine.");
        };
        return summaries(events, viewer);
    }

    public EventSummary create(EventRequest req, User actor) {
        Club club = clubRepository.findById(req.clubId()).orElseThrow(() -> ApiException.notFound("Club"));
        if (!AccessPolicy.manages(club, actor)) {
            throw ApiException.forbidden("Only this club's coordinator or an admin can create its events.");
        }
        if (!req.endsAt().isAfter(req.startsAt())) {
            throw ApiException.badRequest("INVALID_TIME", "The event must end after it starts.");
        }
        Event e = new Event();
        e.setClub(club);
        e.setTitle(req.title().trim());
        e.setDescription(Text.clean(req.description()));
        e.setStartsAt(req.startsAt());
        e.setEndsAt(req.endsAt());
        e.setVenue(req.venue().trim());
        e.setCapacity(req.capacity());
        e.setMembersOnly(req.membersOnly());
        e.setCreatedBy(actor);
        eventRepository.save(e);
        return summaries(List.of(e), actor).get(0);
    }

    public EventSummary cancel(Long eventId, User actor) {
        Event e = eventRepository.findById(eventId).orElseThrow(() -> ApiException.notFound("Event"));
        if (!AccessPolicy.manages(e.getClub(), actor)) {
            throw ApiException.forbidden("Only this club's coordinator or an admin can cancel its events.");
        }
        if (e.isCancelled()) {
            throw ApiException.conflict("ALREADY_CANCELLED", "This event is already cancelled.");
        }
        if (e.hasStarted()) {
            throw ApiException.conflict("EVENT_STARTED", "An event that has already started can't be cancelled.");
        }
        e.setCancelled(true);
        return summaries(List.of(e), actor).get(0);
    }

    /**
     * Takes a seat. The event row is locked (SELECT ... FOR UPDATE) so the capacity check and the insert
     * are atomic: two students can never both get the last seat.
     */
    public RsvpResponse register(Long eventId, User student) {
        if (student.getRole() != Role.STUDENT) {
            throw ApiException.forbidden("Only students can register for events.");
        }
        Event e = eventRepository.lockById(eventId).orElseThrow(() -> ApiException.notFound("Event"));
        if (e.isCancelled()) {
            throw ApiException.conflict("EVENT_CANCELLED", "This event has been cancelled.");
        }
        if (e.hasStarted()) {
            throw ApiException.conflict("EVENT_STARTED", "Registration closed - this event has already started.");
        }
        if (registrationRepository.existsByEventIdAndUserId(eventId, student.getId())) {
            throw ApiException.conflict("ALREADY_REGISTERED", "You're already registered for this event.");
        }
        if (e.isMembersOnly() && membershipRepository.findByUserIdAndClubId(student.getId(), e.getClub().getId())
                .filter(m -> m.getStatus() == MembershipStatus.APPROVED).isEmpty()) {
            throw ApiException.forbidden("This event is only for members of " + e.getClub().getName() + ".");
        }
        long taken = registrationRepository.countByEventId(eventId);
        if (taken >= e.getCapacity()) {
            throw ApiException.conflict("EVENT_FULL", "Sorry, this event is full.");
        }
        registrationRepository.save(new EventRegistration(e, student));
        return new RsvpResponse(taken + 1, true);
    }

    public RsvpResponse unregister(Long eventId, User student) {
        EventRegistration r = registrationRepository.findByEventIdAndUserId(eventId, student.getId())
                .orElseThrow(() -> ApiException.notFound("Registration"));
        if (r.getEvent().hasStarted()) {
            throw ApiException.conflict("EVENT_STARTED", "You can't cancel after the event has started.");
        }
        registrationRepository.delete(r);
        registrationRepository.flush();
        return new RsvpResponse(registrationRepository.countByEventId(eventId), false);
    }

    @Transactional(readOnly = true)
    public List<AttendeeView> attendees(Long eventId, User actor) {
        Event e = eventRepository.findById(eventId).orElseThrow(() -> ApiException.notFound("Event"));
        if (!AccessPolicy.manages(e.getClub(), actor)) {
            throw ApiException.forbidden("Only this club's coordinator or an admin can see the attendee list.");
        }
        return registrationRepository.findByEventIdOrderByCreatedAtAsc(eventId).stream()
                .map(r -> new AttendeeView(r.getUser().getId(), r.getUser().getName(), r.getUser().getEmail(),
                        r.getUser().getDepartment(), r.getUser().getRollNumber(), r.getCreatedAt()))
                .toList();
    }

    /** Adds registration counts, "registered by me" and "can manage" to a list of events. */
    @Transactional(readOnly = true)
    public List<EventSummary> summaries(List<Event> events, User viewer) {
        if (events.isEmpty()) {
            return List.of();
        }
        List<Long> ids = events.stream().map(Event::getId).toList();
        Map<Long, Long> counts = ClubService.countMap(registrationRepository.countFor(ids));
        Set<Long> mine = new HashSet<>(registrationRepository.findRegisteredEventIds(viewer.getId(), ids));
        return events.stream().map(e -> {
            Club c = e.getClub();
            return new EventSummary(e.getId(), c.getId(), c.getName(), c.getCategory(), e.getTitle(),
                    e.getDescription(), e.getStartsAt(), e.getEndsAt(), e.getVenue(), e.getCapacity(),
                    counts.getOrDefault(e.getId(), 0L), e.isMembersOnly(), e.isCancelled(), mine.contains(e.getId()),
                    AccessPolicy.manages(c, viewer));
        }).toList();
    }
}
