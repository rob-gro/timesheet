package dev.robgro.timesheet.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailMessageService {

    private final JavaMailSender emailSender;
    public static final String CONTACT_EMAIL = "contact@robgro.dev";
    public static final String COPY_EMAIL = "robgrodev@gmail.com";

    public void sendInvoiceEmailWithBytes(String recipientEmail, String ccEmail, String firstName,
                                          String invoiceNumber, String month, String fileName, byte[] attachment) throws MessagingException {
        log.info("Preparing to send invoice email to: {}, invoice: {}", recipientEmail, invoiceNumber);

        MimeMessage message = emailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(recipientEmail);
        helper.setCc(ccEmail);
        helper.setSubject("Invoice " + invoiceNumber + " from Aga");

        String emailContent = """
        <html>
        <body style='font-family: Verdana, sans-serif; font-size: 16px;'>
            <h4 style='font-size: 20px;'>Hello %s,</h4>
            <p>I hope all is well with you!</p>
            <p>I wanted to let you know that your invoice (<strong># %s</strong>) for %s's cleaning services is attached to this email.</p>
            
            <p>If there's anything you'd like to discuss or if you spot anything that needs fixing, 
            don't hesitate to drop me a message at <a href='mailto:%s'>%s</a>.</p>
            
            <p>Thanks again, and wishing you a lovely day!</p>
            <p>Aga</p>
            <hr>
            <p style='color: #666; font-size: 12px;'>
                Mobile: Aga: +44 7922 322 002; Rob: +44 747 8385 228<br>
                Email: <a href='mailto:contact@robgro.dev'>contact@robgro.dev</a><br>
                Web: <a href='https://robgro.dev' target='_blank'>https://robgro.dev</a>
            </p>
            <p style='color: #888; font-size: 12px;'>
                <em>Please consider the environment before printing this email.</em>
            </p>
        </body>
        </html>
        """.formatted(firstName, invoiceNumber, month, CONTACT_EMAIL, CONTACT_EMAIL);

        helper.setText(emailContent, true);
        helper.addAttachment(fileName, new ByteArrayResource(attachment));

        log.debug("Sending email with attachment: {}", fileName);
        emailSender.send(message);
        log.info("Successfully sent invoice email to: {}, invoice: {}", recipientEmail, invoiceNumber);
    }
}
