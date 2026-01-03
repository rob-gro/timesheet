package dev.robgro.timesheet.scheduler;

import dev.robgro.timesheet.invoice.InvoiceDto;

/**
 * Immutable result of invoice processing operation
 * Following SOLID principles and Clean Code best practices
 */
public record InvoiceProcessingResult(
        InvoiceDto invoice,
        boolean success,
        String errorMessage,
        Exception exception
) {
    /**
     * Factory method for successful processing
     */
    public static InvoiceProcessingResult success(InvoiceDto invoice) {
        return new InvoiceProcessingResult(invoice, true, null, null);
    }

    /**
     * Factory method for failed processing
     */
    public static InvoiceProcessingResult failure(InvoiceDto invoice, Exception e) {
        return new InvoiceProcessingResult(invoice, false, e.getMessage(), e);
    }

    /**
     * Convenience method for checking success
     * Improves code readability
     */
    public boolean isSuccess() {
        return success;
    }
}
