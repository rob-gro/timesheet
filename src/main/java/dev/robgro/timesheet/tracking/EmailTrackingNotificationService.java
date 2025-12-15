package dev.robgro.timesheet.tracking;

import dev.robgro.timesheet.client.Client;
import dev.robgro.timesheet.invoice.Invoice;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailTrackingNotificationService {

    private final JavaMailSender emailSender;
    private final EmailTrackingProperties trackingProperties;
    private final EmailTrackingRepository trackingRepository;

    @Value("${app.base-url}")
    private String baseUrl;

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    /**
     * Sends instant tracking notification to admin
     */
    public void sendTrackingNotification(EmailTracking tracking) throws MessagingException {
        Invoice invoice = tracking.getInvoice();
        Client client = invoice.getClient();

        String subject = buildSubject(tracking, invoice, client);
        String htmlContent = buildEmailContent(tracking, invoice, client);

        sendEmail(subject, htmlContent);

        log.info("Sent tracking notification for invoice {} to {}",
                invoice.getInvoiceNumber(),
                trackingProperties.getNotificationEmail());
    }

    /**
     * Builds email subject
     */
    private String buildSubject(EmailTracking tracking, Invoice invoice, Client client) {
        if (tracking.isFirstOpen()) {
            return String.format("📧 Invoice Email Opened: %s - %s",
                    invoice.getInvoiceNumber(),
                    client.getClientName());
        } else {
            return String.format("📧 Invoice Email Re-opened: %s - %s (open #%d)",
                    invoice.getInvoiceNumber(),
                    client.getClientName(),
                    tracking.getOpenCount());
        }
    }

    /**
     * Builds comprehensive HTML email content with all metrics
     */
    private String buildEmailContent(EmailTracking tracking, Invoice invoice, Client client) {
        // Calculate overall statistics
        EmailTrackingService.EmailTrackingStats stats = getOverallStats();

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html lang='en'>");
        html.append("<head>");
        html.append("<meta charset='UTF-8'>");
        html.append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        html.append("<style>");
        html.append(getEmailStyles());
        html.append("</style>");
        html.append("</head>");
        html.append("<body>");

        // Header
        html.append("<div class='container'>");
        html.append("<div class='header'>");
        if (tracking.isFirstOpen()) {
            html.append("<h1>✅ Invoice Email Opened!</h1>");
        } else {
            html.append("<h1>🔄 Invoice Email Re-opened</h1>");
        }
        html.append("</div>");

        // Main Event Info
        html.append("<div class='section'>");
        html.append("<h2>📧 Event Information</h2>");
        html.append("<table class='info-table'>");
        html.append(buildTableRow("Client", client.getClientName(), true));
        html.append(buildTableRow("Invoice", invoice.getInvoiceNumber(), false));
        html.append(buildTableRow("Amount", formatAmount(invoice.getTotalAmount()), false));
        html.append(buildTableRow("Opened At", formatDateTime(tracking.getLastOpenedAt()), false));
        html.append(buildTableRow("Open Count", tracking.getOpenCount().toString(), tracking.getOpenCount() > 1));
        html.append("</table>");
        html.append("</div>");

        // Time Metrics (only for first open)
        if (tracking.isFirstOpen() && tracking.getTimeToFirstOpenMinutes() != null) {
            html.append("<div class='section highlight'>");
            html.append("<h2>⏱️ Response Time</h2>");
            html.append("<table class='info-table'>");
            html.append(buildTableRow("Email Sent", formatDateTime(invoice.getEmailSentAt()), false));
            html.append(buildTableRow("Email Opened", formatDateTime(tracking.getOpenedAt()), false));
            html.append(buildTableRow("Time to Open", tracking.getTimeToFirstOpenFormatted(), true));
            html.append(buildTableRow("In Minutes", tracking.getTimeToFirstOpenMinutes() + " min", false));
            html.append(buildTableRow("In Hours", String.format("%.1f h", tracking.getTimeToFirstOpenHours() / 1.0), false));
            html.append("</table>");
            html.append("</div>");
        }

        // Technical Details
        html.append("<div class='section'>");
        html.append("<h2>💻 Technical Details</h2>");
        html.append("<table class='info-table'>");
        html.append(buildTableRow("Device", tracking.getDeviceType(), false));
        html.append(buildTableRow("Email Client", tracking.getEmailClient(), false));
        html.append(buildTableRow("IP Address", tracking.getIpAddress(), false));
        html.append(buildTableRow("User-Agent", shortenUserAgent(tracking.getUserAgent()), false));
        html.append("</table>");
        html.append("</div>");

        // Client Information
        html.append("<div class='section'>");
        html.append("<h2>👤 Client Information</h2>");
        html.append("<table class='info-table'>");
        html.append(buildTableRow("Name", client.getClientName(), false));
        html.append(buildTableRow("Email", client.getEmail(), false));
        html.append(buildTableRow("Address",
                client.getStreetName() + " " + client.getHouseNo() + ", " +
                        client.getPostCode() + " " + client.getCity(), false));
        html.append(buildTableRow("Hourly Rate", formatAmount(BigDecimal.valueOf(client.getHourlyRate())), false));
        html.append("</table>");
        html.append("</div>");

        // Overall Statistics
        html.append("<div class='section stats'>");
        html.append("<h2>📊 Overall Statistics (All Invoices)</h2>");
        html.append("<div class='stats-grid'>");
        html.append(buildStatCard("Sent", String.valueOf(stats.totalSent()), "📤"));
        html.append(buildStatCard("Opened", String.valueOf(stats.totalOpened()), "✅"));
        html.append(buildStatCard("Open Rate", String.format("%.1f%%", stats.openRate()), "📈"));
        html.append(buildStatCard("Last 24h", String.valueOf(stats.openedLast24h()), "🕐"));
        html.append(buildStatCard("Last 7 days", String.valueOf(stats.openedLast7days()), "📅"));
        if (stats.avgTimeToFirstOpenHours() != null) {
            html.append(buildStatCard("Avg Time to Open",
                    String.format("%.1f h", stats.avgTimeToFirstOpenHours()), "⏱️"));
        }
        html.append("</div>");
        html.append("</div>");

        // Additional Insights
        html.append("<div class='section insights'>");
        html.append("<h2>💡 Insights</h2>");
        html.append("<ul>");
        html.append(buildInsight(tracking, stats));
        html.append("</ul>");
        html.append("</div>");

        // Footer
        html.append("<div class='footer'>");
        html.append("<p>🤖 Automated message generated by Email Tracking System</p>");
        html.append("<p><a href='").append(baseUrl).append("'>Timesheet Application</a></p>");
        html.append("<p style='font-size: 11px; color: #666;'>Token: ").append(tracking.getTrackingToken()).append("</p>");
        html.append("</div>");

        html.append("</div>"); // container
        html.append("</body>");
        html.append("</html>");

        return html.toString();
    }

    /**
     * Email styles
     */
    private String getEmailStyles() {
        return """
                body {
                    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
                    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                    margin: 0;
                    padding: 20px;
                }
                .container {
                    max-width: 700px;
                    margin: 0 auto;
                    background: white;
                    border-radius: 12px;
                    box-shadow: 0 10px 40px rgba(0,0,0,0.2);
                    overflow: hidden;
                }
                .header {
                    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                    color: white;
                    padding: 30px;
                    text-align: center;
                }
                .header h1 {
                    margin: 0;
                    font-size: 28px;
                    font-weight: 600;
                }
                .section {
                    padding: 25px 30px;
                    border-bottom: 1px solid #e0e0e0;
                }
                .section:last-child {
                    border-bottom: none;
                }
                .section h2 {
                    margin: 0 0 20px 0;
                    font-size: 18px;
                    color: #333;
                    font-weight: 600;
                }
                .highlight {
                    background: linear-gradient(135deg, #ffeaa7 0%, #fdcb6e 100%);
                }
                .info-table {
                    width: 100%;
                    border-collapse: collapse;
                }
                .info-table tr {
                    border-bottom: 1px solid #f0f0f0;
                }
                .info-table tr:last-child {
                    border-bottom: none;
                }
                .info-table td {
                    padding: 12px 8px;
                    font-size: 14px;
                }
                .info-table td:first-child {
                    font-weight: 600;
                    color: #555;
                    width: 35%;
                }
                .info-table td:last-child {
                    color: #333;
                }
                .highlight-row td:last-child {
                    font-weight: 700;
                    color: #d63031;
                }
                .stats-grid {
                    display: grid;
                    grid-template-columns: repeat(3, 1fr);
                    gap: 15px;
                    margin-top: 15px;
                }
                .stat-card {
                    background: linear-gradient(135deg, #74b9ff 0%, #0984e3 100%);
                    color: white;
                    padding: 20px;
                    border-radius: 8px;
                    text-align: center;
                }
                .stat-card .icon {
                    font-size: 32px;
                    margin-bottom: 10px;
                }
                .stat-card .value {
                    font-size: 28px;
                    font-weight: 700;
                    margin: 10px 0;
                }
                .stat-card .label {
                    font-size: 13px;
                    opacity: 0.9;
                }
                .insights ul {
                    margin: 0;
                    padding-left: 20px;
                }
                .insights li {
                    margin: 10px 0;
                    font-size: 14px;
                    line-height: 1.6;
                }
                .footer {
                    background: #f8f9fa;
                    padding: 20px;
                    text-align: center;
                    color: #666;
                    font-size: 13px;
                }
                .footer a {
                    color: #667eea;
                    text-decoration: none;
                }
                @media (max-width: 600px) {
                    .stats-grid {
                        grid-template-columns: repeat(2, 1fr);
                    }
                }
                """;
    }

    /**
     * Builds table row
     */
    private String buildTableRow(String label, String value, boolean highlight) {
        return String.format("<tr class='%s'><td>%s</td><td>%s</td></tr>",
                highlight ? "highlight-row" : "", label, value != null ? value : "N/A");
    }

    /**
     * Builds stat card
     */
    private String buildStatCard(String label, String value, String icon) {
        return String.format(
                "<div class='stat-card'>" +
                        "<div class='icon'>%s</div>" +
                        "<div class='value'>%s</div>" +
                        "<div class='label'>%s</div>" +
                        "</div>",
                icon, value, label);
    }

    /**
     * Builds insights based on tracking data
     */
    private String buildInsight(EmailTracking tracking, EmailTrackingService.EmailTrackingStats stats) {
        StringBuilder insights = new StringBuilder();

        // First open speed insight
        if (tracking.isFirstOpen() && tracking.getTimeToFirstOpenMinutes() != null) {
            long minutes = tracking.getTimeToFirstOpenMinutes();
            if (minutes < 30) {
                insights.append("<li>⚡ <strong>Very fast response!</strong> Client opened the invoice in less than 30 minutes.</li>");
            } else if (minutes < 120) {
                insights.append("<li>✅ <strong>Fast response.</strong> Client opened the invoice within 2 hours.</li>");
            } else if (minutes < 1440) { // 24h
                insights.append("<li>📅 <strong>Same-day response.</strong> Client opened the invoice within 24 hours.</li>");
            } else {
                long days = minutes / 1440;
                insights.append(String.format("<li>⏰ <strong>Delayed response.</strong> Client opened the invoice after %d day(s).</li>", days));
            }
        }

        // Device insight
        if ("Mobile".equals(tracking.getDeviceType())) {
            insights.append("<li>📱 Client opened the email on a mobile device - verify the invoice PDF is readable on small screens.</li>");
        }

        // Multiple opens insight
        if (tracking.getOpenCount() > 3) {
            insights.append(String.format("<li>🔄 Client opened the email %d times - they may have questions or concerns?</li>", tracking.getOpenCount()));
        }

        // Overall performance insight
        if (stats.openRate() >= 80.0) {
            insights.append(String.format("<li>📈 <strong>Excellent open rate: %.1f%%</strong> - clients regularly read invoices.</li>", stats.openRate()));
        } else if (stats.openRate() < 50.0) {
            insights.append(String.format("<li>⚠️ <strong>Low open rate: %.1f%%</strong> - consider phone follow-up with clients.</li>", stats.openRate()));
        }

        return insights.toString();
    }

    /**
     * Gets overall stats for all tracking
     */
    private EmailTrackingService.EmailTrackingStats getOverallStats() {
        long total = trackingRepository.countTotal();
        long opened = trackingRepository.countOpened();
        double openRate = total > 0 ? (opened * 100.0 / total) : 0.0;

        LocalDateTime last24h = LocalDateTime.now().minusHours(24);
        long openedLast24h = trackingRepository.countOpenedSince(last24h);

        LocalDateTime last7days = LocalDateTime.now().minusDays(7);
        long openedLast7days = trackingRepository.countOpenedSince(last7days);

        Double avgMinutes = trackingRepository.getAverageTimeToFirstOpenMinutes();
        Double avgHours = avgMinutes != null ? avgMinutes / 60.0 : null;

        return new EmailTrackingService.EmailTrackingStats(
                total, opened, openRate, openedLast24h, openedLast7days, avgMinutes, avgHours
        );
    }

    /**
     * Sends HTML email
     */
    private void sendEmail(String subject, String htmlContent) throws MessagingException {
        MimeMessage message = emailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom("robgrodev@gmail.com");
        helper.setTo(trackingProperties.getNotificationEmail());
        helper.setSubject(subject);
        helper.setText(htmlContent, true);

        emailSender.send(message);
    }

    /**
     * Formats date time
     */
    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATE_TIME_FORMATTER) : "N/A";
    }

    /**
     * Formats amount in GBP
     */
    private String formatAmount(BigDecimal amount) {
        return amount != null ? String.format("£%.2f", amount) : "N/A";
    }

    /**
     * Shortens user agent string for display
     */
    private String shortenUserAgent(String userAgent) {
        if (userAgent == null || userAgent.length() <= 80) {
            return userAgent;
        }
        return userAgent.substring(0, 77) + "...";
    }
}
