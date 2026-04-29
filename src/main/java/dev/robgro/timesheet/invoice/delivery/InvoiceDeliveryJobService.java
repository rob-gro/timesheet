package dev.robgro.timesheet.invoice.delivery;

/**
 * API for managing invoice delivery jobs.
 */
public interface InvoiceDeliveryJobService {

    /**
     * Get current delivery status for an invoice.
     *
     * @param invoiceId invoice ID
     * @return latest delivery job DTO, or {@code null} if no job exists
     */
    InvoiceDeliveryJobDto getDeliveryStatus(Long invoiceId);

    /**
     * Request async delivery (or re-delivery) for an invoice.
     *
     * <p>Rules:
     * <ol>
     *   <li>No job exists → create PENDING</li>
     *   <li>PENDING or PROCESSING active → return existing (202, worker will handle it)</li>
     *   <li>FAILED → reset for retry (status=PENDING, is_active=1, delivery_hash=NULL)</li>
     *   <li>SENT → create new job with manual=true (user-triggered resend, bypasses hash check)</li>
     * </ol>
     *
     * @param invoiceId invoice ID
     * @return delivery job DTO
     */
    InvoiceDeliveryJobDto requestDelivery(Long invoiceId);
}