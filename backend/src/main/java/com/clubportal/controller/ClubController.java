package com.clubportal.controller;

import com.clubportal.dto.*;
import com.clubportal.model.ClubCategory;
import com.clubportal.model.MembershipStatus;
import com.clubportal.model.User;
import com.clubportal.service.ClubService;
import com.clubportal.service.MembershipService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/clubs")
public class ClubController {

    private final ClubService clubService;
    private final MembershipService membershipService;

    public ClubController(ClubService clubService, MembershipService membershipService) {
        this.clubService = clubService;
        this.membershipService = membershipService;
    }

    /** e.g. /api/clubs?q=music&category=CULTURAL&openOnly=true */
    @GetMapping
    public List<ClubSummary> list(@AuthenticationPrincipal User user,
                                  @RequestParam(name = "q", required = false) String q,
                                  @RequestParam(name = "category", required = false) ClubCategory category,
                                  @RequestParam(name = "openOnly", defaultValue = "false") boolean openOnly) {
        return clubService.list(user, q, category, openOnly);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ClubDetail create(@AuthenticationPrincipal User user, @Valid @RequestBody ClubRequest req) {
        return clubService.create(req, user);
    }

    @GetMapping("/{id}")
    public ClubDetail get(@AuthenticationPrincipal User user, @PathVariable("id") Long id) {
        return clubService.get(id, user);
    }

    @PutMapping("/{id}")
    public ClubDetail update(@AuthenticationPrincipal User user, @PathVariable("id") Long id,
                             @Valid @RequestBody ClubRequest req) {
        return clubService.update(id, req, user);
    }

    @PostMapping("/{id}/apply")
    @ResponseStatus(HttpStatus.CREATED)
    public MembershipView apply(@AuthenticationPrincipal User user, @PathVariable("id") Long id,
                                @Valid @RequestBody ApplyRequest req) {
        return membershipService.apply(id, req, user);
    }

    /** Coordinator / admin: everyone who ever applied, optionally filtered by status. */
    @GetMapping("/{id}/members")
    public List<MembershipView> members(@AuthenticationPrincipal User user, @PathVariable("id") Long id,
                                        @RequestParam(name = "status", required = false) MembershipStatus status) {
        return membershipService.forClub(id, status, user);
    }
}
