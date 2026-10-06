package com.clubportal.dto;

import com.clubportal.model.ClubCategory;
import com.clubportal.model.MembershipStatus;

public record ClubSummary(
        Long id,
        String name,
        ClubCategory category,
        String tagline,
        String meetingSchedule,
        String venue,
        int capacity,
        long memberCount,
        Long pendingCount,          // only filled in for the club's coordinator / admins
        boolean registrationOpen,
        boolean full,
        String coordinatorName,
        MembershipStatus myStatus   // the viewer's own membership status, or null
) {
}
