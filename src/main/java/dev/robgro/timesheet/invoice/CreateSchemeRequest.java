package dev.robgro.timesheet.invoice;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Request DTO for creating a new invoice numbering scheme.
 * Used by both REST API and web forms.
 */
public record CreateSchemeRequest(
    @NotBlank(message = "Template is required")
    @Size(max = 64, message = "Template must not exceed 64 characters")
    String template,

    @NotNull(message = "Reset period is required")
    ResetPeriod resetPeriod,

    @NotNull(message = "Effective from date is required")
    LocalDate effectiveFrom
) {
}
