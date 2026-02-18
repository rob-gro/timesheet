package dev.robgro.timesheet.invoice;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for invoice numbering scheme.
 * Used for transferring scheme data between layers.
 */
public record InvoiceNumberingSchemeDto(
    Long id,
    Long sellerId,
    String sellerName,
    String template,
    ResetPeriod resetPeriod,
    LocalDate effectiveFrom,
    Integer version,
    SchemeStatus status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    String createdByUsername
) {
}
