package dev.robgro.timesheet.passwordreset;

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

/**
 * Service for sending password reset emails.
 * Uses Thymeleaf template engine for email rendering.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetEmailService {

    private final JavaMailSender emailSender;
    private final TemplateEngine templateEngine;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${app.email.from:noreply@robgro.dev}")
    private String emailFrom;

    @Value("${app.support.email:contact@robgro.dev}")
    private String supportEmail;

    /**
     * Send password reset link email to user.
     *
     * @param recipientEmail User's email address
     * @param username User's username (for personalization)
     * @param resetToken Plaintext reset token (will be embedded in link)
     * @param expiryMinutes Token TTL in minutes (for display in email)
     * @throws MessagingException if email sending fails
     */
    public void sendResetLinkEmail(String recipientEmail, String username,
                                   String resetToken, int expiryMinutes)
            throws MessagingException {

        log.info("Sending password reset email to: {}", recipientEmail);

        // Build reset link with token
        String resetLink = baseUrl + "/reset-password?token=" + resetToken;

        // Prepare Thymeleaf context with template variables
        Context context = new Context();
        context.setVariable("username", username);
        context.setVariable("resetLink", resetLink);
        context.setVariable("expiryMinutes", expiryMinutes);
        context.setVariable("supportEmail", supportEmail);

        // Render template to HTML
        String emailContent = templateEngine.process("email/password-reset-link", context);

        // Create and send email
        MimeMessage message = emailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");

        helper.setTo(recipientEmail);
        helper.setFrom(emailFrom);  // noreply@robgro.dev
        helper.setSubject("Password Reset - Timesheet App");
        helper.setText(emailContent, true);  // true = HTML content

        emailSender.send(message);

        log.info("Password reset email sent successfully to: {}", recipientEmail);
    }
}
