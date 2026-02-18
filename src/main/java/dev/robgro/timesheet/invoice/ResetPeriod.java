package dev.robgro.timesheet.invoice;

import java.time.LocalDate;

/**
 * Defines when invoice sequence numbers reset to 1.
 * Impacts period_month value: 1-12 for MONTHLY, 0 for YEARLY/NEVER
 */
public enum ResetPeriod {
    /**
     * Reset to 1 every month (January = 1, February = 1, etc.)
     * Results in: 001-01-2026, 001-02-2026, etc.
     */
    MONTHLY,

    /**
     * Reset to 1 every year (new year starts from 1)
     * Results in: 001-2026, 002-2026, etc.
     */
    YEARLY,

    /**
     * Never reset - continuous numbering from 1 forever
     * Results in: 001, 002, 003, ... (no year/month)
     */
    NEVER;

    /**
     * Calculates period_month value based on issue date.
     * CRITICAL: Returns 0 (not NULL) for YEARLY/FISCAL_YEAR/NEVER to avoid UNIQUE constraint issues.
     *
     * @param issueDate The date when invoice is issued
     * @return 1-12 for MONTHLY (actual month), 0 for YEARLY/FISCAL_YEAR/NEVER
     */
    public int getPeriodMonth(LocalDate issueDate) {
        return this == MONTHLY ? issueDate.getMonthValue() : 0;
    }

    /**
     * Checks if this reset period requires month component in numbering
     */
    public boolean requiresMonthComponent() {
        return this == MONTHLY;
    }

    /**
     * Returns human-readable description of the reset period
     */
    public String getDescription() {
        return switch (this) {
            case MONTHLY -> "Reset to 1 every month";
            case YEARLY -> "Reset to 1 every year";
            case NEVER -> "Never reset (continuous)";
        };
    }
}
