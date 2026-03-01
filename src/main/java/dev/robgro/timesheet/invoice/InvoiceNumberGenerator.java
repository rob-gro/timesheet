package dev.robgro.timesheet.invoice;

import dev.robgro.timesheet.exception.BusinessRuleViolationException;
import dev.robgro.timesheet.exception.NoSchemeConfiguredException;

import java.time.LocalDate;

/**
 * Service for generating invoice numbers using configured numbering schemes.
 *
 * Uses seller-specific numbering schemes to generate component-based invoice numbers.
 * Seller is passed explicitly to support scheduler/non-HTTP contexts.
 */
public interface InvoiceNumberGenerator {
    /**
     * Generate next invoice number for the given seller and issue date.
     * Uses numbering scheme effective on the issue date (supports backdated invoices).
     *
     * @param sellerId  ID of the seller (tenant) — must not be null
     * @param issueDate Date when invoice is issued (can be in the past for backdating)
     * @param department Optional department for multi-department numbering (null in MVP)
     * @return Generated invoice number with all components
     * @throws NoSchemeConfiguredException if no scheme configured for seller
     * @throws BusinessRuleViolationException if sellerId is null
     */
    GeneratedInvoiceNumber generateInvoiceNumber(Long sellerId, LocalDate issueDate, Department department);

    /**
     * Peek next invoice number WITHOUT reserving it (for preview purposes).
     *
     * <p>CRITICAL: Read-only operation - does NOT increment counter.
     * Use this for invoice preview to show what number WOULD be generated.
     * Actual invoice creation must use {@link #generateInvoiceNumber} to reserve number.
     *
     * @param sellerId  ID of the seller (tenant) — must not be null
     * @param issueDate Date when invoice is issued (can be in the past for backdating)
     * @param department Optional department for multi-department numbering (null in MVP)
     * @return Generated invoice number with all components (WITHOUT reserving it)
     * @throws NoSchemeConfiguredException if no scheme configured for seller
     * @throws BusinessRuleViolationException if sellerId is null
     */
    GeneratedInvoiceNumber peekNextInvoiceNumber(Long sellerId, LocalDate issueDate, Department department);
}
