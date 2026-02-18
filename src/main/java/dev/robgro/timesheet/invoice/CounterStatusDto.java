package dev.robgro.timesheet.invoice;

import java.time.LocalDateTime;

/**
 * Counter status with drift detection for observability endpoint.
 *
 * <p>Drift occurs when counter.lastValue != MAX(sequence_number) in invoices table.
 * This can happen due to:
 * - Race conditions (should be impossible with atomic UPSERT)
 * - Manual database edits
 * - Migration errors
 * - Transaction rollbacks
 *
 * <p>Used by admin/support to verify counter integrity and debug issues.
 */
public record CounterStatusDto(
    /**
     * Reset strategy (MONTHLY, YEARLY, NEVER).
     */
    ResetPeriod resetPeriod,

    /**
     * Period identifier matching V27 format (e.g., "2026-02", "2026", "NEVER").
     */
    String periodKey,

    /**
     * Current counter value (last sequence number issued).
     */
    Integer lastValue,

    /**
     * When counter was last updated (invoice creation timestamp).
     */
    LocalDateTime updatedAt,

    /**
     * Total invoices created in this period (COUNT from invoices table).
     */
    Long invoiceCount,

    /**
     * Last invoice display number (e.g., "INV-2026-017"), null if no invoices.
     */
    String lastInvoiceNumber,

    /**
     * True if counter diverged from actual invoice data (drift detected).
     * Drift = counter.lastValue != MAX(sequence_number).
     */
    boolean hasDrift,

    /**
     * Expected counter value based on actual invoice data.
     * Should equal lastValue if no drift.
     */
    Integer expectedValue
) {
}
