package dev.robgro.timesheet.invoice;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

@Slf4j
@Service
public class EmailMessageService {

    private final JavaMailSender emailSender;
    public static final String CONTACT_EMAIL = "contact@robgro.dev";
    public static final String COPY_EMAIL = "robgrodev@gmail.com";

    @Value("${app.base-url}")
    private String baseUrl;

    public EmailMessageService(JavaMailSender emailSender) {
        this.emailSender = emailSender;
    }

    public void sendInvoiceEmail(InvoiceEmailRequest request) throws MessagingException {
        log.info("Preparing to send invoice email to: {}, invoice: {}",
                request.recipientEmail(), request.invoiceNumber());

        MimeMessage message = emailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(request.recipientEmail());
        helper.setFrom("invoice.aga.cleaning@gmail.com");  // Explicit FROM for invoices
        helper.setCc(request.ccEmail());
        helper.setSubject("Invoice " + request.invoiceNumber() + " from Aga");

        // Format amount as currency
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.UK);
        String formattedAmount = currencyFormat.format(request.totalAmount());

        // Format number of visits
        String visitsText = request.numberOfVisits() + (request.numberOfVisits() == 1 ? " visit" : " visits");

        String emailContent = String.format("""
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="color-scheme" content="light">
                    <meta name="supported-color-schemes" content="light">
                    <meta name="x-apple-disable-message-reformatting">
                    <meta name="format-detection" content="telephone=no,date=no,address=no,email=no,url=no">
                    <style>
                        /* Base styles */
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 0; }

                        /* Gmail Dark Mode Fix - CRITICAL */
                        u + .body .gm-fix { color: #ffffff !important; }
                        u + .body .header-bar { background-color: #2c3e50 !important; }
                        u + .body .invoice-card { background-color: #667eea !important; }

                        /* Apple Mail Dark Mode Fix - CRITICAL */
                        @media (prefers-color-scheme: dark) {
                            .gm-fix { color: #ffffff !important; }
                            .company-tagline-fix { color: #e3f2fd !important; }
                            .header-bar { background-color: #2c3e50 !important; }
                            .invoice-card { background-color: #667eea !important; }
                            .invoice-label { color: #ffffff !important; }
                            .invoice-num { color: #ffffff !important; }
                        }

                        /* Outlook Dark Mode Fix */
                        [data-ogsc] .gm-fix { color: #ffffff !important; }
                        [data-ogsc] .header-bar { background-color: #2c3e50 !important; }

                        /* Standard styles (fallback) */
                        .header-bar {
                            background: linear-gradient(to right, #2c3e50, #3498db);
                            padding: 30px 40px;
                            color: white;
                        }
                        .company-logo {
                            font-size: 32px;
                            font-weight: 700;
                            margin: 0 0 5px 0;
                            letter-spacing: 2px;
                            color: white;
                        }
                        .company-tagline {
                            font-size: 13px;
                            margin: 0;
                            color: #e3f2fd;
                            font-weight: 500;
                        }
                        .content { padding: 40px; }
                        h2 { font-size: 22px; color: #2c3e50; margin: 0 0 25px 0; font-weight: 600; }
                        p { font-size: 15px; line-height: 1.7; color: #444; margin: 0 0 15px 0; }
                        .invoice-card {
                            background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                            color: white;
                            padding: 25px;
                            border-radius: 8px;
                            margin: 30px 0;
                            text-align: center;
                        }
                        .invoice-label {
                            font-size: 13px;
                            text-transform: uppercase;
                            letter-spacing: 2px;
                            margin: 0 0 12px 0;
                            color: #ffffff;
                            font-weight: 600;
                        }
                        .invoice-num {
                            font-size: 32px;
                            font-weight: 900;
                            margin: 0;
                            color: #ffffff;
                            text-shadow: 2px 2px 4px rgba(0,0,0,0.3);
                            letter-spacing: 1px;
                        }
                        .feature-grid {
                            display: grid;
                            grid-template-columns: 1fr 1fr 1fr;
                            gap: 15px;
                            margin: 25px 0;
                        }
                        .feature-box {
                            background-color: #f8f9fa;
                            padding: 20px;
                            border-radius: 10px;
                            border-left: 5px solid;
                            text-align: center;
                        }
                        .feature-box.blue { border-color: #3498db; }
                        .feature-box.green { border-color: #2ecc71; }
                        .feature-box.purple { border-color: #9b59b6; }
                        .feature-icon { font-size: 32px; margin-bottom: 10px; }
                        .feature-title {
                            font-size: 13px;
                            color: #666;
                            font-weight: 600;
                            text-transform: uppercase;
                            margin: 0 0 5px 0;
                        }
                        .feature-value {
                            font-size: 18px;
                            color: #2d3436;
                            font-weight: 700;
                            margin: 0;
                        }
                        .pdf-section {
                            background-color: #e8f4f8;
                            border: 2px solid #3498db;
                            border-radius: 8px;
                            padding: 25px;
                            margin: 30px 0;
                            text-align: center;
                        }
                        .pdf-icon { font-size: 48px; margin-bottom: 15px; }
                        .pdf-title { font-size: 18px; color: #2c3e50; font-weight: 700; margin: 0 0 10px 0; }
                        .pdf-text { font-size: 14px; color: #666; margin: 0 0 20px 0; }
                        .pdf-arrow { font-size: 24px; color: #3498db; margin: 15px 0 0 0; }
                        .info-table { width: 100%%; border-collapse: collapse; margin: 25px 0; }
                        .info-table td { padding: 12px; border-bottom: 1px solid #e0e0e0; }
                        .info-table td:first-child { font-weight: 600; color: #2c3e50; width: 35%%; }
                        .footer-bar { background-color: #f8f9fa; padding: 30px 40px; border-top: 3px solid #3498db; }
                        .footer-bar p { margin: 0 0 8px 0; font-size: 13px; color: #666; }
                    </style>
                </head>
                <body class="body">
                    <div class="header-bar" style="background: linear-gradient(to right, #2c3e50, #3498db); background-color: #2c3e50 !important; padding: 30px 40px; color: #ffffff; mso-line-height-rule: exactly;">
                        <div class="company-logo gm-fix" style="font-size: 32px; font-weight: 700; margin: 0 0 5px 0; letter-spacing: 2px; color: #ffffff !important; -webkit-text-fill-color: #ffffff !important; mso-line-height-rule: exactly;">AGA CLEANING</div>
                        <p class="company-tagline company-tagline-fix" style="font-size: 13px; margin: 0; color: #e3f2fd !important; -webkit-text-fill-color: #e3f2fd !important; font-weight: 500; mso-line-height-rule: exactly;">Professional Cleaning Services</p>
                    </div>

                    <div class="content">
                        <h2>Invoice Notification</h2>

                        <p>Dear %s,</p>

                        <p>I hope all is well with you!</p>

                        <p>I wanted to let you know that your invoice for %s's cleaning services is ready and attached to this email.</p>

                        <div class="invoice-card" style="background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); background-color: #667eea !important; color: #ffffff; padding: 25px; border-radius: 8px; margin: 30px 0; text-align: center; mso-line-height-rule: exactly;">
                            <p class="invoice-label gm-fix" style="font-size: 13px; text-transform: uppercase; letter-spacing: 2px; margin: 0 0 12px 0; color: #ffffff !important; -webkit-text-fill-color: #ffffff !important; font-weight: 600; mso-line-height-rule: exactly;">Invoice Number</p>
                            <p class="invoice-num gm-fix" style="font-size: 32px; font-weight: 900; margin: 0; color: #ffffff !important; -webkit-text-fill-color: #ffffff !important; text-shadow: 2px 2px 4px rgba(0,0,0,0.3); letter-spacing: 1px; mso-line-height-rule: exactly;">%s</p>
                        </div>

                        <p><strong>Invoice Summary:</strong></p>

                        <div class="feature-grid">
                            <div class="feature-box blue">
                                <div class="feature-icon">📅</div>
                                <p class="feature-title">Month</p>
                                <p class="feature-value">%s</p>
                            </div>
                            <div class="feature-box green">
                                <div class="feature-icon">🏠</div>
                                <p class="feature-title">Services</p>
                                <p class="feature-value">%s</p>
                            </div>
                            <div class="feature-box purple">
                                <div class="feature-icon">💷</div>
                                <p class="feature-title">Amount</p>
                                <p class="feature-value">%s</p>
                            </div>
                        </div>

                        <div class="pdf-section">
                            <div class="pdf-icon">📎</div>
                            <p class="pdf-title">Your Invoice PDF is Attached</p>
                            <p class="pdf-text">
                                The invoice document (%s.pdf) is attached to this email.<br>
                                Scroll down to the attachments section to view or download it.
                            </p>
                            <p class="pdf-arrow">⬇️</p>
                            <p style="font-size: 12px; color: #999; margin: 10px 0 0 0;">
                                Look for the attachment icon at the bottom of this email
                            </p>
                        </div>

                        <table class="info-table">
                            <tr>
                                <td>Invoice Date:</td>
                                <td>%s</td>
                            </tr>
                            <tr>
                                <td>Attachment:</td>
                                <td><strong>%s.pdf</strong></td>
                            </tr>
                            <tr style="border: none;">
                                <td>Questions?</td>
                                <td>%s</td>
                            </tr>
                        </table>

                        <p>If there's anything you'd like to discuss or if you spot anything that needs fixing, don't hesitate to drop me a message.</p>

                        <p>Thanks again, and wishing you a lovely day!</p>

                        <p style="margin-top: 30px; font-size: 16px; font-weight: 600; color: #2c3e50;">Aga</p>
                    </div>

                    <div class="footer-bar">
                        <p><strong>Contact Information</strong></p>
                        <p>📧 Email: %s</p>
                        <p>📱 Aga: +44 7922 322 002 | Rob: +44 747 8385 228</p>
                        <p>🌐 Web: <a href="https://robgro.dev" style="color: #3498db; text-decoration: none;">robgro.dev</a></p>
                        <p style="margin-top: 20px; font-size: 11px; color: #999;">
                            <em>Please consider the environment before printing this email.</em>
                        </p>
                    </div>
                    %s
                </body>
                </html>
                """,
                request.firstName(),           // Dear %s
                request.month(),               // %s's cleaning services
                request.invoiceNumber(),       // Invoice number
                request.month(),               // Month tile
                visitsText,                    // Services tile (12 visits)
                formattedAmount,               // Amount tile (£450.00)
                request.invoiceNumber(),       // PDF filename
                java.time.LocalDate.now().toString(),  // Invoice Date
                request.invoiceNumber(),       // Attachment filename
                CONTACT_EMAIL,                 // Questions?
                CONTACT_EMAIL,                 // Footer email
                buildTrackingPixel(request.trackingToken())  // Tracking pixel
        );

        helper.setText(emailContent, true);
        helper.addAttachment(request.fileName(), new ByteArrayResource(request.attachment()));

        log.debug("Sending email with attachment: {}", request.fileName());
        emailSender.send(message);
        log.info("Successfully sent invoice email to: {}, invoice: {}",
                request.recipientEmail(), request.invoiceNumber());
    }

    /**
     * Builds tracking pixel HTML
     * Returns empty string if tracking token is null (tracking disabled)
     *
     * Cache buster parameter (?v=timestamp) helps prevent Gmail image proxy caching
     */
    private String buildTrackingPixel(String trackingToken) {
        if (trackingToken == null || trackingToken.isEmpty()) {
            log.debug("No tracking token provided, skipping pixel injection");
            return ""; // Tracking disabled or token not generated
        }

        // Add cache buster to prevent Gmail proxy caching
        long cacheBuster = System.currentTimeMillis();

        return String.format(
                "<!-- Email Tracking Pixel -->" +
                        "<img src=\"%s/api/track/%s.png?v=%d\" " +
                        "width=\"1\" height=\"1\" alt=\"\" " +
                        "style=\"display:none; width:1px; height:1px; opacity:0;\" />",
                baseUrl,
                trackingToken,
                cacheBuster
        );
    }
}
