package dev.robgro.timesheet.tracking;

import dev.robgro.timesheet.invoice.Invoice;
import dev.robgro.timesheet.invoice.InvoiceRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailTrackingServiceImpl implements EmailTrackingService {

    private final EmailTrackingRepository trackingRepository;
    private final InvoiceRepository invoiceRepository;
    private final EmailTrackingProperties trackingProperties;
    private final EmailTrackingNotificationService notificationService;
    private final EmailTrackingStatsService statsService;

    /**
     * Creates a new tracking token for an invoice
     * Token expires after configured number of days (default: 90)
     */
    @Override
    @Transactional
    public String createTrackingToken(Invoice invoice) {
        if (!trackingProperties.isEnabled()) {
            log.debug("Email tracking is disabled, skipping token creation");
            return null;
        }

        String token = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(trackingProperties.getTokenExpiryDays());

        EmailTracking tracking = EmailTracking.builder()
                .invoice(invoice)
                .trackingToken(token)
                .openCount(0)
                .expiresAt(expiresAt)
                .build();

        trackingRepository.save(tracking);

        // Update invoice with token
        invoice.setEmailTrackingToken(token);
        invoiceRepository.save(invoice);

        log.info("Created tracking token {} for invoice {} (expires: {})",
                token, invoice.getId(), expiresAt);
        return token;
    }

    /**
     * Records an email open event
     * Returns true if this was the first open, false otherwise
     *
     * This method is async-wrapped to avoid blocking the pixel response
     */
    @Override
    @Transactional
    public boolean recordEmailOpen(String token, HttpServletRequest request) {
        if (!trackingProperties.isEnabled()) {
            log.debug("Email tracking is disabled");
            return false;
        }

        return trackingRepository.findByTrackingToken(token)
                .map(tracking -> {
                    // Check if token expired
                    if (tracking.isExpired()) {
                        log.warn("Tracking pixel requested with expired token: {} (expired: {})",
                                token, tracking.getExpiresAt());
                        return false;
                    }

                    boolean isFirstOpen = tracking.getOpenedAt() == null;

                    String ipAddress = getClientIp(request);
                    String userAgent = request.getHeader("User-Agent");

                    tracking.recordOpen(ipAddress, userAgent);
                    trackingRepository.save(tracking);

                    // Update invoice denormalized fields
                    Invoice invoice = tracking.getInvoice();
                    invoice.updateEmailOpenStatus(tracking);
                    invoiceRepository.save(invoice);

                    if (isFirstOpen) {
                        log.info("✅ FIRST email open recorded for invoice {} ({}), client: {}, device: {}, client: {}",
                                invoice.getId(),
                                invoice.getInvoiceNumber(),
                                invoice.getClient().getClientName(),
                                tracking.getDeviceType(),
                                tracking.getEmailClient());

                        // Send instant notification (async)
                        if (trackingProperties.isSendInstantReport()) {
                            sendInstantNotification(tracking);
                        }
                    } else {
                        log.info("🔄 Email re-opened for invoice {} (count: {}), device: {}, client: {}",
                                invoice.getId(),
                                tracking.getOpenCount(),
                                tracking.getDeviceType(),
                                tracking.getEmailClient());

                        // Send notification for re-opens too (for now, as per requirements)
                        if (trackingProperties.isSendInstantReport()) {
                            sendInstantNotification(tracking);
                        }
                    }

                    return isFirstOpen;
                })
                .orElseGet(() -> {
                    log.warn("⚠️ Tracking pixel requested with invalid token: {}", token);
                    return false;
                });
    }

    /**
     * Sends instant notification email about tracking event
     * Executed asynchronously to not block pixel response
     * Uses dedicated "emailTrackingExecutor" thread pool
     */
    @Async("emailTrackingExecutor")
    protected void sendInstantNotification(EmailTracking tracking) {
        try {
            notificationService.sendTrackingNotification(tracking);
        } catch (Exception e) {
            log.error("Failed to send tracking notification for token {}: {}",
                    tracking.getTrackingToken(), e.getMessage(), e);
            // Don't rethrow - notification failure should not affect tracking
        }
    }

    /**
     * Gets client IP address from request, handling proxies and load balancers
     */
    private String getClientIp(HttpServletRequest request) {
        // Check X-Forwarded-For header (set by proxies/load balancers)
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            // X-Forwarded-For can contain multiple IPs: "client, proxy1, proxy2"
            // Take the first one (original client IP)
            ip = ip.split(",")[0].trim();
            return ip;
        }

        // Check X-Real-IP header (Nginx)
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }

        // Fallback to remote address
        return request.getRemoteAddr();
    }

    /**
     * Gets comprehensive tracking statistics
     * Delegates to EmailTrackingStatsService to avoid code duplication
     */
    @Override
    @Transactional(readOnly = true)
    public EmailTrackingStats getStats() {
        return statsService.getStats();
    }

    /**
     * Cleanup job - removes tracking data older than retention period
     * Should be scheduled to run periodically (e.g., daily)
     */
    @Override
    @Transactional
    public void cleanupOldTrackingData() {
        // Keep data for 12 months as per GDPR compliance plan
        LocalDateTime threshold = LocalDateTime.now().minusMonths(12);
        List<EmailTracking> oldRecords = trackingRepository.findCreatedBefore(threshold);

        if (!oldRecords.isEmpty()) {
            log.info("Deleting {} old email tracking records (older than 12 months)", oldRecords.size());
            trackingRepository.deleteAll(oldRecords);
        } else {
            log.debug("No old tracking records to clean up");
        }
    }
}
