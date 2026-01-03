package dev.robgro.timesheet.tracking;

/**
 * Email tracking statistics DTO
 * Contains comprehensive metrics about email tracking performance
 */
public record EmailTrackingStats(
        long totalSent,
        long totalOpened,
        double openRate,
        long openedLast24h,
        long openedLast7days,
        Double avgTimeToFirstOpenMinutes,
        Double avgTimeToFirstOpenHours
) {
}
