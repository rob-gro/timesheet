package dev.robgro.timesheet.tracking;

import dev.robgro.timesheet.invoice.Invoice;
import jakarta.persistence.*;
import lombok.*;

import java.time.Duration;
import java.time.LocalDateTime;

@Entity
@Table(name = "email_tracking")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailTracking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Column(name = "tracking_token", nullable = false, unique = true, length = 255)
    private String trackingToken;

    @Column(name = "opened_at")
    private LocalDateTime openedAt;

    @Column(name = "last_opened_at")
    private LocalDateTime lastOpenedAt;

    @Column(name = "open_count", nullable = false)
    @Builder.Default
    private Integer openCount = 0;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "device_type", length = 50)
    private String deviceType;

    @Column(name = "email_client", length = 100)
    private String emailClient;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    /**
     * Records an email open event
     */
    public void recordOpen(String ipAddress, String userAgent) {
        LocalDateTime now = LocalDateTime.now();

        if (this.openedAt == null) {
            this.openedAt = now; // First open
        }

        this.lastOpenedAt = now;
        this.openCount++;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.deviceType = detectDeviceType(userAgent);
        this.emailClient = detectEmailClient(userAgent);
    }

    /**
     * Checks if tracking token has expired
     */
    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }

    /**
     * Checks if this was the first open
     */
    public boolean isFirstOpen() {
        return openCount == 1 && openedAt != null;
    }

    /**
     * Gets time elapsed from email sent to first open (in minutes)
     * Returns null if email not yet opened or invoice not available
     */
    public Long getTimeToFirstOpenMinutes() {
        if (openedAt == null || invoice == null || invoice.getEmailSentAt() == null) {
            return null;
        }
        return Duration.between(invoice.getEmailSentAt(), openedAt).toMinutes();
    }

    /**
     * Gets time elapsed from email sent to first open (in hours)
     */
    public Long getTimeToFirstOpenHours() {
        Long minutes = getTimeToFirstOpenMinutes();
        return minutes != null ? minutes / 60 : null;
    }

    /**
     * Gets human-readable time to first open
     * Example: "2 hours 15 minutes"
     */
    public String getTimeToFirstOpenFormatted() {
        Long minutes = getTimeToFirstOpenMinutes();
        if (minutes == null) {
            return "N/A";
        }

        if (minutes < 60) {
            return minutes + " minutes";
        }

        long hours = minutes / 60;
        long remainingMinutes = minutes % 60;

        if (hours < 24) {
            return remainingMinutes > 0
                    ? hours + " hours " + remainingMinutes + " minutes"
                    : hours + " hours";
        }

        long days = hours / 24;
        long remainingHours = hours % 24;

        return days + " days" + (remainingHours > 0 ? " " + remainingHours + " hours" : "");
    }

    /**
     * Detects device type from User-Agent string
     */
    private String detectDeviceType(String userAgent) {
        if (userAgent == null) {
            return "Unknown";
        }

        String ua = userAgent.toLowerCase();

        if (ua.contains("mobile") || ua.contains("android") || ua.contains("iphone") || ua.contains("ipod")) {
            return "Mobile";
        } else if (ua.contains("tablet") || ua.contains("ipad")) {
            return "Tablet";
        } else {
            return "Desktop";
        }
    }

    /**
     * Detects email client from User-Agent string
     */
    private String detectEmailClient(String userAgent) {
        if (userAgent == null) {
            return "Unknown";
        }

        String ua = userAgent.toLowerCase();

        // Gmail
        if (ua.contains("gmail") || ua.contains("google")) {
            return "Gmail";
        }
        // Outlook
        if (ua.contains("outlook") || ua.contains("microsoft outlook")) {
            return "Outlook";
        }
        // Apple Mail
        if (ua.contains("applemail") || ua.contains("apple mail")) {
            return "Apple Mail";
        }
        // Thunderbird
        if (ua.contains("thunderbird")) {
            return "Thunderbird";
        }
        // Yahoo Mail
        if (ua.contains("yahoo")) {
            return "Yahoo Mail";
        }
        // ProtonMail
        if (ua.contains("proton")) {
            return "ProtonMail";
        }
        // Generic mobile email
        if (ua.contains("mail")) {
            return "Mail App";
        }

        // Browser-based (fallback to browser detection)
        if (ua.contains("chrome")) {
            return "Browser (Chrome)";
        } else if (ua.contains("firefox")) {
            return "Browser (Firefox)";
        } else if (ua.contains("safari")) {
            return "Browser (Safari)";
        } else if (ua.contains("edge")) {
            return "Browser (Edge)";
        }

        return "Unknown";
    }
}
