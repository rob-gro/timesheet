package dev.robgro.timesheet.invoice;

import org.springframework.stereotype.Component;

/**
 * Factory for building period keys matching V27 backfill SQL format.
 *
 * <p><b>CRITICAL:</b> Period key format MUST match V27 backfill logic exactly,
 * otherwise counters will diverge from actual invoices.
 *
 * <p>Period key formats:
 * <ul>
 *   <li>MONTHLY: "YYYY-MM" (e.g., "2026-02")</li>
 *   <li>YEARLY: "YYYY" (e.g., "2026")</li>
 *   <li>NEVER: "NEVER" (literal string)</li>
 * </ul>
 *
 * <p>V27 backfill SQL reference:
 * <pre>
 * CASE
 *     WHEN reset_period = 'MONTHLY' THEN CONCAT(period_year, '-', LPAD(period_month, 2, '0'))
 *     WHEN reset_period = 'YEARLY'  THEN CAST(period_year AS CHAR)
 *     WHEN reset_period = 'NEVER'   THEN 'NEVER'
 * END
 * </pre>
 */
@Component
public class PeriodKeyFactory {

    /**
     * Build period key exactly as V27 backfill SQL does.
     *
     * <p><b>CRITICAL:</b> Must match SQL logic 1:1 for counter consistency.
     *
     * @param resetPeriod Reset strategy (MONTHLY, YEARLY, NEVER)
     * @param periodYear Year component (0 for NEVER)
     * @param periodMonth Month component (0 for YEARLY and NEVER, 1-12 for MONTHLY)
     * @return Period key string matching V27 format
     * @throws IllegalStateException if MONTHLY with periodMonth=0 (invalid state)
     */
    public String build(ResetPeriod resetPeriod, int periodYear, int periodMonth) {
        // Guard: prevent MONTHLY + month=0 (would break counter consistency)
        if (resetPeriod == ResetPeriod.MONTHLY && periodMonth == 0) {
            throw new IllegalStateException(
                "Invalid period: MONTHLY reset cannot have month=0. " +
                "This would cause counter drift. " +
                "periodYear=" + periodYear + ", periodMonth=" + periodMonth
            );
        }

        return switch (resetPeriod) {
            case MONTHLY -> String.format("%04d-%02d", periodYear, periodMonth); // "2026-02"
            case YEARLY  -> String.valueOf(periodYear);                         // "2026"
            case NEVER   -> "NEVER";                                           // "NEVER"
        };
    }
}
