package dev.robgro.timesheet.invoice;

import java.util.List;

/**
 * Response for counter observability endpoint showing all counters for a seller.
 *
 * <p>Used by admin/support to:
 * - Verify counter integrity after migrations
 * - Debug "why did invoice number jump" support tickets
 * - Monitor for drift (counter != actual invoice data)
 * - Audit multi-tenant data without direct DB access
 *
 * <p>Example response:
 * <pre>
 * {
 *   "sellerId": 123,
 *   "currentTemplate": "INV-{YYYY}-{SEQ:3}",
 *   "counters": [
 *     {
 *       "resetPeriod": "MONTHLY",
 *       "periodKey": "2026-02",
 *       "lastValue": 17,
 *       "invoiceCount": 17,
 *       "hasDrift": false
 *     }
 *   ]
 * }
 * </pre>
 */
public record CounterObservabilityResponse(
    /**
     * Seller (tenant) ID whose counters are shown.
     */
    Long sellerId,

    /**
     * Current active invoice numbering template (e.g., "INV-{YYYY}-{SEQ:3}").
     * Null if no scheme configured.
     */
    String currentTemplate,

    /**
     * List of all counters for this seller, ordered by resetPeriod ASC, periodKey DESC.
     * Shows newest periods first within each reset strategy.
     */
    List<CounterStatusDto> counters
) {
}
