package com.clubportal.repository;

import com.clubportal.model.Event;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventRepository extends JpaRepository<Event, Long> {

    @Query("select e from Event e join fetch e.club where (:clubId is null or e.club.id = :clubId)"
            + " and e.startsAt >= :from order by e.startsAt asc")
    List<Event> findUpcoming(@Param("clubId") Long clubId, @Param("from") Instant from, Pageable pageable);

    @Query("select e from Event e join fetch e.club where (:clubId is null or e.club.id = :clubId)"
            + " and e.startsAt < :before order by e.startsAt desc")
    List<Event> findPast(@Param("clubId") Long clubId, @Param("before") Instant before, Pageable pageable);

    @Query("select e from Event e join fetch e.club where e.startsAt >= :from and e.id in"
            + " (select r.event.id from EventRegistration r where r.user.id = :userId) order by e.startsAt asc")
    List<Event> findUpcomingRegisteredBy(@Param("userId") Long userId, @Param("from") Instant from, Pageable pageable);

    @Query("select e from Event e join fetch e.club c where c.coordinator.id = :coordinatorId"
            + " and e.startsAt >= :from order by e.startsAt asc")
    List<Event> findUpcomingForCoordinator(@Param("coordinatorId") Long coordinatorId, @Param("from") Instant from,
                                           Pageable pageable);

    /** SELECT ... FOR UPDATE so two students can't take the last seat at the same time. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Event e where e.id = :id")
    Optional<Event> lockById(@Param("id") Long id);

    long countByStartsAtAfterAndCancelledFalse(Instant from);

    @Query("select count(e) from Event e where e.startsAt >= :from and e.id in"
            + " (select r.event.id from EventRegistration r where r.user.id = :userId)")
    long countUpcomingRegisteredBy(@Param("userId") Long userId, @Param("from") Instant from);
}
