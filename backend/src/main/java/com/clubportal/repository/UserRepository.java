package com.clubportal.repository;

import com.clubportal.model.Role;
import com.clubportal.model.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    List<User> findByRoleOrderByNameAsc(Role role);

    List<User> findAllByOrderByRoleAscNameAsc();

    long countByRole(Role role);
}
