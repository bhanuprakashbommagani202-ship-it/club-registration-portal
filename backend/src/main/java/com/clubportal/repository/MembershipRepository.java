package com.clubportal.repository;

import com.clubportal.model.Membership;
import com.clubportal.model.MembershipStatus;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MembershipRepository extends JpaRepository<Membership, Long> {

    Optional<Membership> findByUserIdAndClubId(Long userId, Long clubId);

    /** SELECT ... FOR UPDATE: two coordinators can't decide the same application at once. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Membership m where m.id = :id")
    Optional<Membership> lockById(@Param("id") Long id);

    @EntityGraph(attributePaths = {"club", "club.coordinator", "decidedBy"})
    List<Membership> findByUserIdOrderByAppliedAtDesc(Long userId);

    @EntityGraph(attributePaths = {"user", "club", "decidedBy"})
    List<Membership> findByClubIdOrderByAppliedAtDesc(Long clubId);

    @EntityGraph(attributePaths = {"user", "club"})
    List<Membership> findByClubCoordinatorIdAndStatusOrderByAppliedAtAsc(Long coordinatorId, MembershipStatus status);

    @EntityGraph(attributePaths = {"user", "club"})
    List<Membership> findByStatusOrderByAppliedAtAsc(MembershipStatus status);

    long countByUserIdAndStatusIn(Long userId, Collection<MembershipStatus> statuses);

    long countByClubIdAndStatus(Long clubId, MembershipStatus status);

    long countByStatus(MembershipStatus status);

    /** [clubId, count] for every club that has memberships with this status. */
    @Query("select m.club.id, count(m) from Membership m where m.status = :status group by m.club.id")
    List<Object[]> countPerClub(@Param("status") MembershipStatus status);

    @Query("select m.club.category, count(m) from Membership m where m.status = :status group by m.club.category")
    List<Object[]> countPerCategory(@Param("status") MembershipStatus status);
}
