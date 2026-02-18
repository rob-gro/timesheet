package dev.robgro.timesheet.invoice;

/**
 * Simple record holding period components for invoice numbering.
 * Calculated based on issue date and reset period.
 *
 * @param year Period year (e.g., 2026)
 * @param month Period month: 1-12 for MONTHLY, 0 for YEARLY/NEVER
 */
public record PeriodComponents(
        Integer year,
        Integer month) {
}
