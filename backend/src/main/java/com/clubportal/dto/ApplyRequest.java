package com.clubportal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ApplyRequest(
        @NotBlank(message = "Tell the coordinator why you want to join.")
        @Size(min = 10, max = 500, message = "Your reason must be 10-500 characters.")
        String motivation
) {
}
