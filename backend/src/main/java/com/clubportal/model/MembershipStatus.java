package com.clubportal.model;

/**
 * PENDING  -> APPROVED | REJECTED   (coordinator / admin decides)
 * PENDING  -> WITHDRAWN             (student cancels the application)
 * APPROVED -> LEFT                  (student leaves the club)
 * APPROVED -> REMOVED               (coordinator / admin removes the member)
 * Any inactive status can apply again, which moves the same row back to PENDING.
 */
public enum MembershipStatus {
    PENDING, APPROVED, REJECTED, WITHDRAWN, LEFT, REMOVED;

    /** Pending and approved memberships count toward the per-student club limit. */
    public boolean isActive() {
        return this == PENDING || this == APPROVED;
    }
}
