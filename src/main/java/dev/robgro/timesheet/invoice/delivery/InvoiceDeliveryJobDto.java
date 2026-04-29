package dev.robgro.timesheet.invoice.delivery;

import java.time.LocalDateTime;

/**
 * Read-only view of an {@link InvoiceDeliveryJob} for API responses.
 */
public record InvoiceDeliveryJobDto(
        Long id,
        Long invoiceId,
        String status,
        int attempts,
        String lastError,
        LocalDateTime createdAt
) {
}