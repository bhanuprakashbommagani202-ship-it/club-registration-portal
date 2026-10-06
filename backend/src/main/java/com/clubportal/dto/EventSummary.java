package com.clubportal.dto;

import com.clubportal.model.ClubCategory;
import java.time.Instant;

public record EventSummary(
        Long id,
        Long clubId,
        String clubName,
        ClubCategory clubCategory,
        String title,
        String description,
        Instant startsAt,
        Instant endsAt,
        String venue,
        int capacity,
        long registeredCount,
        boolean membersOnly,
        boolean cancelled,
        boolean registeredByMe,
        boolean canManage
) {
}
