package com.clubportal.controller;

import com.clubportal.dto.AttendeeView;
import com.clubportal.dto.EventRequest;
import com.clubportal.dto.EventSummary;
import com.clubportal.dto.RsvpResponse;
import com.clubportal.model.User;
import com.clubportal.service.EventService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    /** e.g. /api/events?scope=upcoming|past|mine&clubId=3 */
    @GetMapping
    public List<EventSummary> list(@AuthenticationPrincipal User user,
                                   @RequestParam(name = "scope", defaultValue = "upcoming") String scope,
                                   @RequestParam(name = "clubId", required = false) Long clubId) {
        return eventService.list(user, scope, clubId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventSummary create(@AuthenticationPrincipal User user, @Valid @RequestBody EventRequest req) {
        return eventService.create(req, user);
    }

    @PostMapping("/{id}/cancel")
    public EventSummary cancel(@AuthenticationPrincipal User user, @PathVariable("id") Long id) {
        return eventService.cancel(id, user);
    }

    @PostMapping("/{id}/register")
    public RsvpResponse register(@AuthenticationPrincipal User user, @PathVariable("id") Long id) {
        return eventService.register(id, user);
    }

    @DeleteMapping("/{id}/register")
    public RsvpResponse unregister(@AuthenticationPrincipal User user, @PathVariable("id") Long id) {
        return eventService.unregister(id, user);
    }

    @GetMapping("/{id}/attendees")
    public List<AttendeeView> attendees(@AuthenticationPrincipal User user, @PathVariable("id") Long id) {
        return eventService.attendees(id, user);
    }
}
