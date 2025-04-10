package dev.robgro.timesheet.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordChangeDto(
        @NotBlank String currentPassword,

        @NotBlank
        @Size(min = 6, max = 100)
        String newPassword
) {
}
