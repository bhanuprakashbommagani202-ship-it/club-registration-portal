package com.clubportal.dto;

import com.clubportal.model.MembershipStatus;
import java.time.Instant;

public record MyMembership(Long id, MembershipStatus status, String motivation, String remark,
                           Instant appliedAt, Instant decidedAt) {
}
