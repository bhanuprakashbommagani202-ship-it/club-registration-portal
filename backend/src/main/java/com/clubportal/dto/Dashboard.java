package com.clubportal.dto;

import java.util.List;

/** One shape for all roles; sections that don't apply to a role are empty lists. */
public record Dashboard(
        String role,
        List<StatItem> stats,
        List<ClubSummary> clubs,                 // student: my clubs, coordinator: managed clubs, admin: top clubs
        List<MembershipView> pendingApplications, // student: my pending, coordinator/admin: waiting for decision
        List<EventSummary> upcomingEvents,
        List<CountItem> membersByCategory
) {
}
