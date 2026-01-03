package dev.robgro.timesheet.scheduler;

import dev.robgro.timesheet.config.InvoicingSchedulerProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Admin notification service implementation
 * Uses Thymeleaf templates instead of hardcoded HTML/CSS
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminNotificationServiceImpl implements AdminNotificationService {

    private final JavaMailSender emailSender;
    private final InvoicingSchedulerProperties properties;
    private final TemplateEngine templateEngine;

    /**
     * Sends error notification email using Thymeleaf template
     * Follows Single Responsibility Principle - only handles email sending logic
     */
    @Override
    public void sendErrorNotification(String subject, String details, Exception e) {
        try {
            log.info("Sending error notification to admin: {}", subject);

            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(properties.getAdminEmail());
            helper.setSubject("❌ [SCHEDULER ERROR] " + subject);

            // Create Thymeleaf context with all variables
            Context context = new Context();
            context.setVariable("subject", subject);
            context.setVariable("details", details);
            context.setVariable("exception", e);
            context.setVariable("errorType", e.getClass().getSimpleName());
            context.setVariable("timestamp", LocalDateTime.now());
            context.setVariable("stackTrace", getStackTrace(e));

            // Process template and return HTML
            String htmlContent = templateEngine.process("email/admin-error-notification", context);

            helper.setText(htmlContent, true);
            emailSender.send(message);

            log.info("Error notification sent successfully");
        } catch (MessagingException ex) {
            log.error("Failed to send error notification email", ex);
        }
    }

    /**
     * Sends summary notification email using Thymeleaf template
     * All HTML rendering logic delegated to template (Separation of Concerns)
     */
    @Override
    public void sendSummaryNotification(InvoicingSummary summary) {
        try {
            log.info("Sending summary notification to admin");

            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(properties.getAdminEmail());

            // Calculate success rate
            double successRate = summary.totalInvoices() > 0
                ? (double) summary.successfulInvoices() / summary.totalInvoices() * 100
                : 0;

            helper.setSubject(String.format("[SCHEDULER] Monthly Invoicing - %.0f%% Success (%d/%d)",
                successRate, summary.successfulInvoices(), summary.totalInvoices()));

            // Create Thymeleaf context - all rendering delegated to template
            Context context = new Context();
            context.setVariable("summary", summary);
            context.setVariable("successRate", String.format("%.0f", successRate));

            // Process template and return HTML
            String htmlContent = templateEngine.process("email/admin-summary-notification", context);

            helper.setText(htmlContent, true);
            emailSender.send(message);

            log.info("Summary notification sent successfully");
        } catch (MessagingException e) {
            log.error("Failed to send summary notification email", e);
        }
    }

    /**
     * Sends empty client warning email using Thymeleaf template
     * DRY principle - client list rendering delegated to template
     */
    @Override
    public void sendEmptyClientWarning(List<String> clientNames) {
        try {
            log.info("Sending empty client warning to admin for {} clients", clientNames.size());

            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(properties.getAdminEmail());
            helper.setSubject(String.format("⚠️ [SCHEDULER] %d Active Client%s Without Timesheets",
                clientNames.size(), clientNames.size() > 1 ? "s" : ""));

            // Create Thymeleaf context - list rendering delegated to template
            Context context = new Context();
            context.setVariable("clientNames", clientNames);

            // Process template and return HTML
            String htmlContent = templateEngine.process("email/admin-empty-client-warning", context);

            helper.setText(htmlContent, true);
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
