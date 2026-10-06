package com.clubportal.dto;

import com.clubportal.model.ClubCategory;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Create / update a club. Coordinators may only change tagline, description, schedule, venue,
 * capacity and registrationOpen; name, category and coordinator are admin-only.
 */
public record ClubRequest(
        @NotBlank(message = "Club name is required.")
        @Size(max = 100, message = "Club name can be at most 100 characters.")
        String name,

        @NotNull(message = "Category is required.")
        ClubCategory category,

        @Size(max = 150, message = "Tagline can be at most 150 characters.")
        String tagline,

        @NotBlank(message = "Description is required.")
        @Size(min = 20, max = 2000, message = "Description must be 20-2000 characters.")
        String description,

        @Size(max = 150, message = "Meeting schedule can be at most 150 characters.")
        String meetingSchedule,

        @Size(max = 150, message = "Venue can be at most 150 characters.")
        String venue,

        @NotNull(message = "Capacity is required.")
        @Min(value = 1, message = "Capacity must be at least 1.")
        @Max(value = 500, message = "Capacity can be at most 500.")
        Integer capacity,

        Boolean registrationOpen,

        Long coordinatorId
) {
}
