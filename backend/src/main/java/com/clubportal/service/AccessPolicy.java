package com.clubportal.service;

import com.clubportal.model.Club;
import com.clubportal.model.User;
import java.util.Objects;

/** "Who may manage this club" lives in one place. */
public final class AccessPolicy {

    private AccessPolicy() {
    }

    public static boolean isCoordinatorOf(Club club, User user) {
        return club.getCoordinator() != null && Objects.equals(club.getCoordinator().getId(), user.getId());
    }

    /** Admins manage every club; coordinators manage the clubs assigned to them. */
    public static boolean manages(Club club, User user) {
        return user.isAdmin() || isCoordinatorOf(club, user);
    }
}
