package com.clubportal.repository;

import com.clubportal.model.EventRegistration;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventRegistrationRepository extends JpaRepository<EventRegistration, Long> {

    boolean existsByEventIdAndUserId(Long eventId, Long userId);

    Optional<EventRegistration> findByEventIdAndUserId(Long eventId, Long userId);

    long countByEventId(Long eventId);

    @EntityGraph(attributePaths = "user")
    List<EventRegistration> findByEventIdOrderByCreatedAtAsc(Long eventId);

    /** [eventId, count] for the given events. */
    @Query("select r.event.id, count(r) from EventRegistration r where r.event.id in :ids group by r.event.id")
    List<Object[]> countFor(@Param("ids") Collection<Long> ids);

    @Query("select r.event.id from EventRegistration r where r.user.id = :userId and r.event.id in :ids")
    List<Long> findRegisteredEventIds(@Param("userId") Long userId, @Param("ids") Collection<Long> ids);
}
