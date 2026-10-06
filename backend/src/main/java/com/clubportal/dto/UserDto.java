package com.clubportal.dto;

import com.clubportal.model.Role;
import com.clubportal.model.User;
import java.time.Instant;

public record UserDto(Long id, String name, String email, Role role, String department, String rollNumber,
                      boolean active, Instant createdAt) {

    public static UserDto from(User u) {
        return new UserDto(u.getId(), u.getName(), u.getEmail(), u.getRole(), u.getDepartment(), u.getRollNumber(),
                u.isActive(), u.getCreatedAt());
    }
}
