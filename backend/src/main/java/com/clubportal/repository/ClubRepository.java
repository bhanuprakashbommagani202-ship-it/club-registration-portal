package com.clubportal.repository;

import com.clubportal.model.Club;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClubRepository extends JpaRepository<Club, Long> {

    @EntityGraph(attributePaths = "coordinator")
    List<Club> findAllByOrderByNameAsc();

    @EntityGraph(attributePaths = "coordinator")
    List<Club> findByCoordinatorIdOrderByNameAsc(Long coordinatorId);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    /**
     * SELECT ... FOR UPDATE on the club row. Approvals for the same club wait for each other,
     * so two coordinators can't both approve the last free seat.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Club c where c.id = :id")
    Optional<Club> lockById(@Param("id") Long id);

    @Query("select c.category, count(c) from Club c group by c.category")
    List<Object[]> countByCategory();
}
