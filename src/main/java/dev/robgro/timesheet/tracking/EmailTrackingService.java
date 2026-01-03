package dev.robgro.timesheet.tracking;

import dev.robgro.timesheet.invoice.Invoice;
import jakarta.servlet.http.HttpServletRequest;

public interface EmailTrackingService {

    /**
     * Creates a new tracking token for an invoice
     */
    String createTrackingToken(Invoice invoice);

    /**
     * Records email open event and sends notification if enabled
     */
    boolean recordEmailOpen(String token, HttpServletRequest request);

    /**
     * Gets comprehensive tracking statistics
     */
    EmailTrackingStats getStats();

    /**
     * Cleanup job - removes tracking data older than retention period
     */
    void cleanupOldTrackingData();
}
