package com.clubportal.controller;

import com.clubportal.dto.DecisionRequest;
import com.clubportal.dto.MembershipView;
import com.clubportal.model.User;
import com.clubportal.service.MembershipService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/memberships")
public class MembershipController {

    private final MembershipService membershipService;

    public MembershipController(MembershipService membershipService) {
        this.membershipService = membershipService;
    }

    /** The logged-in student's applications and memberships. */
    @GetMapping("/my")
    public List<MembershipView> mine(@AuthenticationPrincipal User user) {
        return membershipService.mine(user);
    }

    /** Applications waiting for the logged-in coordinator / admin. */
    @GetMapping("/pending")
    public List<MembershipView> pending(@AuthenticationPrincipal User user) {
        return membershipService.pendingFor(user);
    }

    /** Coordinator / admin: {"status": "APPROVED" | "REJECTED" | "REMOVED", "remark": "..."} */
    @PatchMapping("/{id}/status")
    public MembershipView decide(@AuthenticationPrincipal User user, @PathVariable("id") Long id,
                                 @Valid @RequestBody DecisionRequest req) {
        return membershipService.decide(id, req, user);
    }

    /** Student: withdraw a pending application or leave a club. */
    @PostMapping("/{id}/leave")
    public MembershipView leave(@AuthenticationPrincipal User user, @PathVariable("id") Long id) {
        return membershipService.leave(id, user);
    }
}
