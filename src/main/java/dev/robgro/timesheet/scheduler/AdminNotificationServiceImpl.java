package dev.robgro.timesheet.scheduler;

import dev.robgro.timesheet.config.InvoicingSchedulerProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminNotificationServiceImpl implements AdminNotificationService {

    private final JavaMailSender emailSender;
    private final InvoicingSchedulerProperties properties;

    // Common CSS styles - inspired by professional email templates
    private static final String COMMON_STYLES = """
            <style>
                body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                h2 { color: #2196F3; margin-bottom: 20px; }
                h3 { color: #333; margin-top: 20px; margin-bottom: 10px; }
                h4 { color: #666; margin-top: 15px; margin-bottom: 8px; }
                .detail-table {
                    width: 100%%;
                    border-collapse: collapse;
                    margin: 15px 0;
                    background-color: #fafafa;
                }
                .detail-table td {
                    padding: 10px;
                    border-bottom: 1px solid #e0e0e0;
                }
                .detail-table td:first-child {
                    font-weight: bold;
                    width: 30%%;
                    color: #666;
                }
                .detail-table th {
                    padding: 10px;
                    text-align: left;
                    background-color: #2196F3;
                    color: white;
                    font-weight: bold;
                }
                .metric-card {
                    background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                    color: white;
                    padding: 20px;
                    border-radius: 8px;
                    margin: 20px 0;
                    text-align: center;
                }
                .metric-value {
                    font-size: 36px;
                    font-weight: bold;
                    margin: 10px 0;
                }
                .success-box {
                    margin-top: 10px;
                    padding: 15px;
                    background-color: #e8f5e9;
                    border-left: 4px solid #4caf50;
                }
                .error-box {
                    margin-top: 10px;
                    padding: 15px;
                    background-color: #ffebee;
                    border-left: 4px solid #f44336;
                }
                .warning-box {
                    margin-top: 10px;
                    padding: 15px;
                    background-color: #fff3cd;
                    border-left: 4px solid #ffc107;
                }
                .info-box {
                    margin-top: 10px;
                    padding: 10px;
                    background-color: #e3f2fd;
                    border-left: 4px solid #2196F3;
                }
                .footer {
                    margin-top: 30px;
                    padding-top: 20px;
                    border-top: 1px solid #e0e0e0;
                    color: #666;
                    font-size: 12px;
                    text-align: center;
                }
                .invoice-row-success {
                    background-color: #f1f8f4;
                }
                .invoice-row-error {
                    background-color: #ffebee;
                }
            </style>
            """;

    @Override
    public void sendErrorNotification(String subject, String details, Exception e) {
        try {
            log.info("Sending error notification to admin: {}", subject);

            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(properties.getAdminEmail());
            helper.setSubject("❌ [SCHEDULER ERROR] " + subject);

            String stackTrace = getStackTrace(e);
            String errorType = e.getClass().getSimpleName();

            String emailContent = String.format("""
                    <html>
                    <head>
                        %s
                    </head>
                    <body>
                        <div class="container">
                            <div style="background-color: #f44336; color: white; padding: 20px; margin: -20px -20px 20px -20px; border-radius: 8px 8px 0 0;">
                                <h2 style="color: white; margin: 0;">❌ Scheduler Error Alert</h2>
                                <p style="margin: 10px 0 0 0; opacity: 0.9;">Immediate attention required</p>
                            </div>

                            <div class="error-box">
                                <h3 style="margin-top: 0; color: #f44336;">%s</h3>
                                <p><strong>Error Type:</strong> %s</p>
                            </div>

                            <h3>📋 Error Details</h3>
                            <table class="detail-table">
                                <tr>
                                    <td>Timestamp:</td>
                                    <td>%s</td>
                                </tr>
                                <tr>
                                    <td>Error Message:</td>
                                    <td><strong style="color: #f44336;">%s</strong></td>
                                </tr>
                                <tr>
                                    <td>Context:</td>
                                    <td>%s</td>
                                </tr>
                            </table>

                            <h3>🔍 Stack Trace</h3>
                            <div style="background-color: #f5f5f5; padding: 15px; border: 1px solid #ddd; border-radius: 4px; overflow-x: auto;">
                                <pre style="margin: 0; font-size: 11px; white-space: pre-wrap; word-wrap: break-word;">%s</pre>
                            </div>

                            <h3>⚙️ Recommended Actions</h3>
                            <div class="warning-box">
                                • Check application logs for additional context<br/>
                                • Verify database connectivity and permissions<br/>
                                • Ensure FTP/Email services are accessible<br/>
                                • Review recent configuration changes<br/>
                                • Retry failed operations manually if needed
                            </div>

                            <div class="footer">
                                <p>This is an automated error notification from Timesheet Invoicing Scheduler.</p>
                                <p style="color: #f44336; font-weight: bold;">⚠️ This error requires immediate attention</p>
                            </div>
                        </div>
                    </body>
                    </html>
                    """,
                    COMMON_STYLES,
                    subject,
                    errorType,
                    java.time.LocalDateTime.now(),
                    e.getMessage(),
                    details,
                    stackTrace
            );

            helper.setText(emailContent, true);
            emailSender.send(message);

            log.info("Error notification sent successfully");
        } catch (MessagingException ex) {
            log.error("Failed to send error notification email", ex);
        }
    }

    @Override
    public void sendSummaryNotification(InvoicingSummary summary) {
        try {
            log.info("Sending summary notification to admin");

            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(properties.getAdminEmail());

            // Calculate success rate
            double successRate = summary.getTotalInvoices() > 0
                ? (double) summary.getSuccessfulInvoices() / summary.getTotalInvoices() * 100
                : 0;

            helper.setSubject(String.format("[SCHEDULER] Monthly Invoicing - %.0f%% Success (%d/%d)",
                successRate, summary.getSuccessfulInvoices(), summary.getTotalInvoices()));

            // Build detailed invoice table
            StringBuilder invoiceTableRows = new StringBuilder();
            for (InvoiceProcessingResult result : summary.getProcessingResults()) {
                String rowClass = result.isSuccess() ? "invoice-row-success" : "invoice-row-error";
                String statusIcon = result.isSuccess() ? "✅" : "❌";
                String statusText = result.isSuccess() ? "SUCCESS" : "FAILED";
                String statusColor = result.isSuccess() ? "#4caf50" : "#f44336";

                invoiceTableRows.append(String.format(
                        "<tr class='%s'><td>%s</td><td>%s</td><td><strong style='color: %s;'>%s %s</strong></td></tr>",
                        rowClass,
                        result.getInvoice().invoiceNumber(),
                        result.getInvoice().clientName(),
                        statusColor,
                        statusIcon,
                        statusText
                ));

                // Add error details row if failed
                if (!result.isSuccess() && result.getErrorMessage() != null) {
                    invoiceTableRows.append(String.format(
                            "<tr class='%s'><td colspan='3' style='padding-left: 30px; color: #f44336; font-size: 12px;'>↳ Error: %s</td></tr>",
                            rowClass,
                            result.getErrorMessage()
                    ));
                }
            }

            // Build recommendations section
            StringBuilder recommendations = new StringBuilder();
            if (summary.getFailedInvoices() > 0) {
                recommendations.append("• Review failed invoices and retry manually if needed<br/>");
            }
            if (!summary.getClientsWithoutTimesheets().isEmpty()) {
                recommendations.append(String.format("• %d active client(s) have no timesheets - verify if expected<br/>",
                    summary.getClientsWithoutTimesheets().size()));
            }
            if (recommendations.length() == 0) {
                recommendations.append("✅ All invoices processed successfully - no action needed");
            }

            String emailContent = String.format("""
                    <html>
                    <head>
                        %s
                    </head>
                    <body>
                        <div class="container">
                            <div style="background-color: #2196F3; color: white; padding: 20px; margin: -20px -20px 20px -20px; border-radius: 8px 8px 0 0;">
                                <h2 style="color: white; margin: 0;">📊 Monthly Invoicing Completed</h2>
                                <p style="margin: 10px 0 0 0; opacity: 0.9;">Automated invoice generation summary</p>
                            </div>

                            <div class="metric-card">
                                <div style="opacity: 0.9; font-size: 14px;">SUCCESS RATE</div>
                                <div class="metric-value">%.0f%%</div>
                                <div style="opacity: 0.9; font-size: 12px;">%d successful • %d failed • %d total</div>
                            </div>

                            <h3>📅 Execution Details</h3>
                            <table class="detail-table">
                                <tr>
                                    <td>Execution Time:</td>
                                    <td>%s</td>
                                </tr>
                                <tr>
                                    <td>Month Processed:</td>
                                    <td><strong>%s</strong></td>
                                </tr>
                                <tr style="background-color: #e8f5e9;">
                                    <td>✅ Successful Invoices:</td>
                                    <td><strong style="color: #4caf50;">%d</strong></td>
                                </tr>
                                <tr style="background-color: #ffebee;">
                                    <td>❌ Failed Invoices:</td>
                                    <td><strong style="color: #f44336;">%d</strong></td>
                                </tr>
                                <tr>
                                    <td>📋 Total Invoices:</td>
                                    <td><strong>%d</strong></td>
                                </tr>
                            </table>

                            <h3>📄 Invoice Processing Results</h3>
                            <table class="detail-table">
                                <tr>
                                    <th>Invoice #</th>
                                    <th>Client</th>
                                    <th>Status</th>
                                </tr>
                                %s
                            </table>

                            <h3>🎯 Recommendations</h3>
                            <div class="warning-box">
                                %s
                            </div>

                            <div class="footer">
                                <p>This is an automated message from Timesheet Invoicing Scheduler.</p>
                            </div>
                        </div>
                    </body>
                    </html>
                    """,
                    COMMON_STYLES,
                    successRate,
                    summary.getSuccessfulInvoices(),
                    summary.getFailedInvoices(),
                    summary.getTotalInvoices(),
                    summary.getExecutionTime(),
                    summary.getPreviousMonth(),
                    summary.getSuccessfulInvoices(),
                    summary.getFailedInvoices(),
                    summary.getTotalInvoices(),
                    invoiceTableRows.toString(),
                    recommendations.toString()
            );

            helper.setText(emailContent, true);
            emailSender.send(message);

            log.info("Summary notification sent successfully");
        } catch (MessagingException e) {
            log.error("Failed to send summary notification email", e);
        }
    }

    @Override
    public void sendEmptyClientWarning(List<String> clientNames) {
        try {
            log.info("Sending empty client warning to admin for {} clients", clientNames.size());

            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(properties.getAdminEmail());
            helper.setSubject(String.format("⚠️ [SCHEDULER] %d Active Client%s Without Timesheets",
                clientNames.size(), clientNames.size() > 1 ? "s" : ""));

            // Build clients list with professional formatting
            StringBuilder clientsListHtml = new StringBuilder();
            for (int i = 0; i < clientNames.size(); i++) {
                String icon = i < 3 ? "🔴" : "⚠️";
                clientsListHtml.append(String.format(
                        "<tr><td style='padding: 8px; border-bottom: 1px solid #e0e0e0;'>%s <strong>%s</strong></td></tr>",
                        icon,
                        clientNames.get(i)
                ));
            }

            String emailContent = String.format("""
                    <html>
                    <head>
                        %s
                    </head>
                    <body>
                        <div class="container">
                            <div style="background-color: #ff9800; color: white; padding: 20px; margin: -20px -20px 20px -20px; border-radius: 8px 8px 0 0;">
                                <h2 style="color: white; margin: 0;">⚠️ Client Activity Warning</h2>
                                <p style="margin: 10px 0 0 0; opacity: 0.9;">Active clients without billable work</p>
                            </div>

                            <div class="warning-box">
                                <h3 style="margin-top: 0; color: #ff9800;">Missing Timesheets Detected</h3>
                                <p><strong>%d active client%s</strong> have no timesheets for the previous month and were not invoiced.</p>
                            </div>

                            <h3>📋 Affected Clients</h3>
                            <table class="detail-table">
                                %s
                            </table>

                            <h3>🔍 Required Actions</h3>
                            <div class="info-box">
                                <strong>Please verify the following:</strong><br/><br/>
                                • Is this expected behavior (e.g., client on hold, vacation, project break)?<br/>
                                • Were timesheets forgotten or missed?<br/>
                                • Should these clients still be marked as active?<br/>
                                • Do any of these clients need manual invoicing?
                            </div>

                            <h3>💡 Recommendations</h3>
                            <div style="padding: 15px; background-color: #f5f5f5; border-radius: 4px; margin: 15px 0;">
                                <strong>If timesheets were missed:</strong><br/>
                                1. Add missing timesheets to the system<br/>
                                2. Manually generate invoices for affected clients<br/>
                                <br/>
                                <strong>If no work was performed:</strong><br/>
                                1. Verify client status with project managers<br/>
                                2. Consider marking inactive clients as such<br/>
                                3. Document the reason for no billable work
                            </div>

                            <div class="footer">
                                <p>This is an automated notification from Timesheet Invoicing Scheduler.</p>
                                <p style="color: #ff9800; font-weight: bold;">⚠️ Review recommended to ensure billing accuracy</p>
                            </div>
                        </div>
                    </body>
                    </html>
                    """,
                    COMMON_STYLES,
                    clientNames.size(),
                    clientNames.size() > 1 ? "s" : "",
                    clientsListHtml.toString()
            );

            helper.setText(emailContent, true);
            emailSender.send(message);

            log.info("Empty client warning sent successfully");
        } catch (MessagingException e) {
            log.error("Failed to send empty client warning email", e);
        }
    }

    private String getStackTrace(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }
}
