package com.clubportal.dto;

/** Tells the UI which actions to show. The server re-checks every action anyway. */
public record ClubPermissions(
        boolean canApply,
        String applyBlockedReason,
        boolean canManage,
        boolean canEditAll
) {
}
