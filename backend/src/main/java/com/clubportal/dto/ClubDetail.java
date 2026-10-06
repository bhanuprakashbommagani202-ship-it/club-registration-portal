package com.clubportal.dto;

import com.clubportal.model.ClubCategory;
import java.time.Instant;
import java.util.List;

public record ClubDetail(
        Long id,
        String name,
        ClubCategory category,
        String tagline,
        String description,
        String meetingSchedule,
        String venue,
        int capacity,
        long memberCount,
        Long pendingCount,
        boolean registrationOpen,
        boolean full,
        PersonRef coordinator,
        Instant createdAt,
        MyMembership myMembership,
        ClubPermissions permissions,
        List<EventSummary> upcomingEvents
) {
}
