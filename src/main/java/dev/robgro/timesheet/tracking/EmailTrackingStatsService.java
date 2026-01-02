package dev.robgro.timesheet.tracking;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service responsible for calculating email tracking statistics
 * Separated to avoid circular dependency between EmailTrackingService and EmailTrackingNotificationService
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailTrackingStatsService {

    private final EmailTrackingRepository trackingRepository;

    /**
     * Gets comprehensive tracking statistics
     */
    @Transactional(readOnly = true)
    public EmailTrackingStats getStats() {
        long total = trackingRepository.countTotal();
        long opened = trackingRepository.countOpened();
        double openRate = total > 0 ? (opened * 100.0 / total) : 0.0;

        // Opened in last 24h
        LocalDateTime last24h = LocalDateTime.now().minusHours(24);
        long openedLast24h = trackingRepository.countOpenedSince(last24h);

        // Opened in last 7 days
        LocalDateTime last7days = LocalDateTime.now().minusDays(7);
        long openedLast7days = trackingRepository.countOpenedSince(last7days);

        // Average time to first open
        Double avgMinutes = trackingRepository.getAverageTimeToFirstOpenMinutes();
        Double avgHours = avgMinutes != null ? avgMinutes / 60.0 : null;

        return new EmailTrackingStats(
                total,
                opened,
                openRate,
                openedLast24h,
                openedLast7days,
                avgMinutes,
                avgHours
        );
    }
}
