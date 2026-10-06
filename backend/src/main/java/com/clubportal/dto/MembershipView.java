package com.clubportal.dto;

import com.clubportal.model.ClubCategory;
import com.clubportal.model.MembershipStatus;
import java.time.Instant;

public record MembershipView(
        Long id,
        Long clubId,
        String clubName,
        ClubCategory clubCategory,
        Long userId,
        String studentName,
        String studentEmail,
        String department,
        String rollNumber,
        MembershipStatus status,
        String motivation,
        String remark,
        Instant appliedAt,
        Instant decidedAt,
        String decidedByName
) {
}
