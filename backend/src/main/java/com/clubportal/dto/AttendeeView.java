package com.clubportal.dto;

import java.time.Instant;

public record AttendeeView(Long userId, String name, String email, String department, String rollNumber,
                           Instant registeredAt) {
}
