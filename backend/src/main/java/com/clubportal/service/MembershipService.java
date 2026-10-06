package com.clubportal.service;

import com.clubportal.dto.ApplyRequest;
import com.clubportal.dto.DecisionRequest;
import com.clubportal.dto.MembershipView;
import com.clubportal.exception.ApiException;
import com.clubportal.model.*;
import com.clubportal.repository.ClubRepository;
import com.clubportal.repository.MembershipRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MembershipService {

    static final List<MembershipStatus> ACTIVE = List.of(MembershipStatus.PENDING, MembershipStatus.APPROVED);

    private final MembershipRepository membershipRepository;
    private final ClubRepository clubRepository;
    private final int maxActiveClubs;

    public MembershipService(MembershipRepository membershipRepository, ClubRepository clubRepository,
                             @Value("${app.max-active-clubs:3}") int maxActiveClubs) {
        this.membershipRepository = membershipRepository;
        this.clubRepository = clubRepository;
        this.maxActiveClubs = maxActiveClubs;
    }

    // ---- student side ---------------------------------------------------------------------

    public MembershipView apply(Long clubId, ApplyRequest req, User student) {
        if (student.getRole() != Role.STUDENT) {
            throw ApiException.forbidden("Only students can apply to join clubs.");
        }
        Club club = clubRepository.findById(clubId).orElseThrow(() -> ApiException.notFound("Club"));
        if (!club.isRegistrationOpen()) {
            throw ApiException.conflict("REGISTRATION_CLOSED", club.getName() + " isn't accepting new members right now.");
        }
        Membership membership = membershipRepository.findByUserIdAndClubId(student.getId(), clubId).orElse(null);
        if (membership != null && membership.getStatus() == MembershipStatus.PENDING) {
            throw ApiException.conflict("ALREADY_APPLIED",
                    "You've already applied to this club. Wait for the coordinator's decision.");
        }
        if (membership != null && membership.getStatus() == MembershipStatus.APPROVED) {
            throw ApiException.conflict("ALREADY_MEMBER", "You are already a member of this club.");
        }
        if (membershipRepository.countByUserIdAndStatusIn(student.getId(), ACTIVE) >= maxActiveClubs) {
            throw ApiException.conflict("CLUB_LIMIT_REACHED", "You can be part of at most " + maxActiveClubs
                    + " clubs at a time, and pending applications count too. Leave a club or withdraw an application first.");
        }
        if (membershipRepository.countByClubIdAndStatus(clubId, MembershipStatus.APPROVED) >= club.getCapacity()) {
            throw ApiException.conflict("CLUB_FULL", club.getName() + " is full.");
        }
        if (membership == null) {
            membership = new Membership(student, club);
        }
        membership.apply(req.motivation().trim(), Instant.now()); // re-applying reuses the same row
        membershipRepository.save(membership);
        return toView(membership);
    }

    /** Student withdraws a pending application or leaves a club they belong to. */
    public MembershipView leave(Long membershipId, User student) {
        Membership m = membershipRepository.lockById(membershipId)
                .filter(x -> x.getUser().getId().equals(student.getId()))
                .orElseThrow(() -> ApiException.notFound("Membership"));
        MembershipStatus next = switch (m.getStatus()) {
            case PENDING -> MembershipStatus.WITHDRAWN;
            case APPROVED -> MembershipStatus.LEFT;
            default -> throw ApiException.conflict("INVALID_STATE", "This membership is no longer active.");
        };
        m.decide(next, null, null, Instant.now()); // the student's own action, no coordinator involved
        return toView(m);
    }

    @Transactional(readOnly = true)
    public List<MembershipView> mine(User student) {
        return membershipRepository.findByUserIdOrderByAppliedAtDesc(student.getId()).stream()
                .map(this::toView).toList();
    }

    // ---- coordinator / admin side ---------------------------------------------------------

    @Transactional(readOnly = true)
    public List<MembershipView> forClub(Long clubId, MembershipStatus status, User actor) {
        Club club = clubRepository.findById(clubId).orElseThrow(() -> ApiException.notFound("Club"));
        if (!AccessPolicy.manages(club, actor)) {
            throw ApiException.forbidden("Only this club's coordinator or an admin can see its members.");
        }
        return membershipRepository.findByClubIdOrderByAppliedAtDesc(clubId).stream()
                .filter(m -> status == null || m.getStatus() == status)
                .map(this::toView).toList();
    }

    /** Applications waiting for a decision: a coordinator sees their clubs, an admin sees all. */
    @Transactional(readOnly = true)
    public List<MembershipView> pendingFor(User actor) {
        List<Membership> list = switch (actor.getRole()) {
            case ADMIN -> membershipRepository.findByStatusOrderByAppliedAtAsc(MembershipStatus.PENDING);
            case COORDINATOR -> membershipRepository.findByClubCoordinatorIdAndStatusOrderByAppliedAtAsc(
                    actor.getId(), MembershipStatus.PENDING);
            case STUDENT -> throw ApiException.forbidden("Only coordinators and admins can review applications.");
        };
        return list.stream().map(this::toView).toList();
    }

    /**
     * PENDING -> APPROVED / REJECTED, APPROVED -> REMOVED.
     * The application and the club rows are locked (SELECT ... FOR UPDATE) so the capacity check and the
     * approval happen atomically, and two coordinators can't decide the same application at once.
     */
    public MembershipView decide(Long membershipId, DecisionRequest req, User actor) {
        // Both reads below are locking reads (SELECT ... FOR UPDATE), taken before any plain read, so the
        // member count further down sees every approval committed by others while we waited for the lock.
        Membership m = membershipRepository.lockById(membershipId)
                .orElseThrow(() -> ApiException.notFound("Application"));
        Club club = clubRepository.lockById(m.getClub().getId()).orElseThrow(() -> ApiException.notFound("Club"));
        if (!AccessPolicy.manages(club, actor)) {
            throw ApiException.forbidden("Only this club's coordinator or an admin can do this.");
        }
        MembershipStatus from = m.getStatus();
        MembershipStatus to = req.status();
        String remark = Text.clean(req.remark());

        switch (to) {
            case APPROVED, REJECTED -> {
                if (from != MembershipStatus.PENDING) {
                    throw ApiException.conflict("INVALID_STATE", "Only pending applications can be approved or rejected.");
                }
            }
            case REMOVED -> {
                if (from != MembershipStatus.APPROVED) {
                    throw ApiException.conflict("INVALID_STATE", "Only current members can be removed.");
                }
            }
            default -> throw ApiException.badRequest("INVALID_STATUS", "Status must be APPROVED, REJECTED or REMOVED.");
        }
        if (to != MembershipStatus.APPROVED && remark == null) {
            throw ApiException.badRequest("REMARK_REQUIRED", "Please give the student a reason.");
        }
        if (to == MembershipStatus.APPROVED
                && membershipRepository.countByClubIdAndStatus(club.getId(), MembershipStatus.APPROVED) >= club.getCapacity()) {
            throw ApiException.conflict("CLUB_FULL", club.getName() + " is already at full capacity.");
        }
        m.decide(to, actor, remark, Instant.now());
        return toView(m);
    }

    MembershipView toView(Membership m) {
        User u = m.getUser();
        Club c = m.getClub();
        return new MembershipView(m.getId(), c.getId(), c.getName(), c.getCategory(), u.getId(), u.getName(),
                u.getEmail(), u.getDepartment(), u.getRollNumber(), m.getStatus(), m.getMotivation(), m.getRemark(),
                m.getAppliedAt(), m.getDecidedAt(), m.getDecidedBy() == null ? null : m.getDecidedBy().getName());
    }
}
