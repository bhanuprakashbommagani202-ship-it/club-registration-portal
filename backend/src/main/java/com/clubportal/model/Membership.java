package com.clubportal.model;

import jakarta.persistence.*;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** One row per student per club. Re-applying reuses the row, so the UNIQUE constraint always holds. */
@Entity
@Table(name = "memberships",
        uniqueConstraints = @UniqueConstraint(name = "uq_memberships_user_club", columnNames = {"user_id", "club_id"}),
        indexes = {
                @Index(name = "idx_memberships_club_status", columnList = "club_id, status"),
                @Index(name = "idx_memberships_user_status", columnList = "user_id, status")
        })
public class Membership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "club_id", nullable = false)
    private Club club;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private MembershipStatus status = MembershipStatus.PENDING;

    /** Why the student wants to join. */
    @Column(length = 500)
    private String motivation;

    /** Coordinator's note (required for reject / remove). */
    @Column(length = 300)
    private String remark;

    @Column(name = "applied_at", nullable = false)
    private Instant appliedAt;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decided_by_id")
    private User decidedBy;

    public Membership() {
    }

    public Membership(User user, Club club) {
        this.user = user;
        this.club = club;
    }

    /** (Re)starts an application. */
    public void apply(String motivation, Instant at) {
        this.status = MembershipStatus.PENDING;
        this.motivation = motivation;
        this.appliedAt = at;
        this.remark = null;
        this.decidedAt = null;
        this.decidedBy = null;
    }

    public void decide(MembershipStatus newStatus, User by, String remark, Instant at) {
        this.status = newStatus;
        this.decidedBy = by;
        this.remark = remark;
        this.decidedAt = at;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public Club getClub() { return club; }
    public MembershipStatus getStatus() { return status; }
    public void setStatus(MembershipStatus status) { this.status = status; }
    public String getMotivation() { return motivation; }
    public String getRemark() { return remark; }
    public Instant getAppliedAt() { return appliedAt; }
    public Instant getDecidedAt() { return decidedAt; }
    public void setDecidedAt(Instant decidedAt) { this.decidedAt = decidedAt; }
    public User getDecidedBy() { return decidedBy; }
}
