package dev.robgro.timesheet.invoice.delivery;

/**
 * Lifecycle states of an {@link InvoiceDeliveryJob}.
 *
 * <p>DB uses VARCHAR(20) — enum provides type-safety in Java code.
 *
 * <p>is_active mapping:
 * <ul>
 *   <li>PENDING, PROCESSING → is_active = 1 (active in queue)</li>
 *   <li>SENT, FAILED        → is_active = NULL (historical)</li>
 * </ul>
 */
public enum DeliveryStatus {
    PENDING,
    PROCESSING,
    SENT,
    FAILED
}