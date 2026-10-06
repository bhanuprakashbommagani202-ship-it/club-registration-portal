package com.clubportal.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record EventRequest(
        @NotNull(message = "Choose a club.") Long clubId,

        @NotBlank(message = "Title is required.")
        @Size(min = 3, max = 150, message = "Title must be 3-150 characters.")
        String title,

        @Size(max = 1000, message = "Description can be at most 1000 characters.")
        String description,

        @NotNull(message = "Start time is required.")
        @Future(message = "Start time must be in the future.")
        Instant startsAt,

        @NotNull(message = "End time is required.")
        Instant endsAt,

        @NotBlank(message = "Venue is required.")
        @Size(max = 150, message = "Venue can be at most 150 characters.")
        String venue,

        @NotNull(message = "Capacity is required.")
        @Min(value = 1, message = "Capacity must be at least 1.")
        @Max(value = 2000, message = "Capacity can be at most 2000.")
        Integer capacity,

        boolean membersOnly
) {
}
