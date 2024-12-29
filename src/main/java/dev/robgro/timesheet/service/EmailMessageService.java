package dev.robgro.timesheet.service;

import dev.robgro.timesheet.model.entity.Client;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
@RequiredArgsConstructor
public class EmailMessageService {

    private final JavaMailSender emailSender;


    public void sendInvoiceEmail(String recipientEmail, String firstName, String invoiceNumber, String month, String email, File attachment) throws MessagingException {
        MimeMessage message = emailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(recipientEmail);
        helper.setSubject("Invoice " + invoiceNumber);


        String emailContent = """
                <html>
                <body style='font-family: Verdana, sans-serif;'>
                    <h4>Dear %s,</h4>
                    <p>I hope everything is going well with you!</p>
                    <p>Please find attached your invoice number: <strong>%s</strong>,</p>
                    <p>for cleaning services for %s.</p>
                <p>If you notice anything that doesn’t look right or have any questions,</p>
                <p>feel free to reach out via email <a href='mailto:%s'>%s</a>.</p>
                <br>
                <p>Best regards,</p>
                <p>Aga</p>
                <hr>
                <p style='color: #666; font-size: 12px;'>
                    Mobile: +44 7922 322 002<br>
                    Email: <a href='mailto:contact@robgro.dev'>contact@robgro.dev</a><br>
                    Web: <a href='https://robgro.dev' target='_blank'>https://robgro.dev</a>
                </p>
                <p style='color: #888; font-size: 12px;'>
                    <em>Please consider the environment before printing this email.</em>
                </p>
                    </body>
                    </html>
                """.formatted(firstName, invoiceNumber, month, email, email);

        helper.setText(emailContent, true);
        helper.addAttachment(attachment.getName(), attachment);

        emailSender.send(message);
    }
}
