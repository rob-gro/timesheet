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
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailTrackingNotificationServiceImpl implements EmailTrackingNotificationService {

    private final JavaMailSender emailSender;
    private final EmailTrackingProperties trackingProperties;
    private final EmailTrackingStatsService statsService;
    private final TemplateEngine templateEngine;

    @Value("${app.base-url}")
    private String baseUrl;

    /**
     * Sends instant tracking notification to admin
     */
    @Override
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
     * Builds email content using Thymeleaf template
     */
    private String buildEmailContent(EmailTracking tracking, Invoice invoice, Client client) {
        // Get statistics from dedicated service (DRY principle)
        EmailTrackingStats stats = statsService.getStats();

        // Create Thymeleaf context with all variables
        Context context = new Context();
        context.setVariable("tracking", tracking);
        context.setVariable("invoice", invoice);
        context.setVariable("client", client);
        context.setVariable("stats", stats);
        context.setVariable("baseUrl", baseUrl);

        // Process template and return HTML
        return templateEngine.process("email/tracking-notification", context);
    }

    /**
     * Sends HTML email
     */
    private void sendEmail(String subject, String htmlContent) throws MessagingException {
        MimeMessage message = emailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(trackingProperties.getNotificationEmail());
        helper.setSubject(subject);
        helper.setText(htmlContent, true);

        emailSender.send(message);
    }

}
