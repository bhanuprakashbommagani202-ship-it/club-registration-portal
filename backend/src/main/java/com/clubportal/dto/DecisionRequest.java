package com.clubportal.dto;

import com.clubportal.model.MembershipStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Coordinator / admin decision: APPROVED or REJECTED for a pending application, REMOVED for a member. */
public record DecisionRequest(
        @NotNull(message = "Status is required.") MembershipStatus status,
        @Size(max = 300, message = "Remark can be at most 300 characters.") String remark
) {
}
