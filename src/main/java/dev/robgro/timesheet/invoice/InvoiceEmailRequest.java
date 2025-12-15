package dev.robgro.timesheet.invoice;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record InvoiceEmailRequest(
        String recipientEmail,
        String ccEmail,
        String firstName,
        String invoiceNumber,
        String month,
        String fileName,
        byte[] attachment,
        int numberOfVisits,
        BigDecimal totalAmount,
        String trackingToken  // Tracking pixel token (null if tracking disabled)
) {
}
