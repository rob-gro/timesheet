package dev.robgro.timesheet.invoice;

import dev.robgro.timesheet.client.Client;
import dev.robgro.timesheet.seller.Seller;
import dev.robgro.timesheet.timesheet.Timesheet;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "invoices")
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invoice_number", unique = true)
    private String invoiceNumber;

    @Column(name = "issue_date")
    private LocalDate issueDate;

    @Column(name = "total_amount")
    private BigDecimal totalAmount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id")
    private Seller seller;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InvoiceItem> itemsList = new ArrayList<>();

    @Column(name = "issued_date")
    private LocalDateTime issuedDate;

    @Column(name = "pdf_path")
    private String pdfPath;

    @Column(name = "pdf_generated_at")
    private LocalDateTime pdfGeneratedAt;

    @Column(name = "email_sent_at")
    private LocalDateTime emailSentAt;

    @Column(name = "email_tracking_token", length = 255)
    private String emailTrackingToken;

    @Column(name = "email_opened_at")
    private LocalDateTime emailOpenedAt;

    @Column(name = "email_open_count")
    private Integer emailOpenCount = 0;

    @Column(name = "last_email_opened_at")
    private LocalDateTime lastEmailOpenedAt;

    @OneToMany(mappedBy = "invoice")
    private List<Timesheet> timesheets = new ArrayList<>();

    /**
     * Updates email tracking status from EmailTracking entity
     * Called when email is opened
     */
    public void updateEmailOpenStatus(dev.robgro.timesheet.tracking.EmailTracking tracking) {
        this.emailOpenedAt = tracking.getOpenedAt();
        this.emailOpenCount = tracking.getOpenCount();
        this.lastEmailOpenedAt = tracking.getLastOpenedAt();
    }

    /**
     * Checks if email was sent but not opened within given days
     */
    public boolean isEmailUnopened(int daysThreshold) {
        if (emailSentAt == null) return false;
        if (emailOpenedAt != null) return false;

        return emailSentAt.plusDays(daysThreshold).isBefore(LocalDateTime.now());
    }

    /**
     * Gets email status as human-readable string
     */
    public String getEmailStatus() {
        if (emailSentAt == null) return "NOT_SENT";
        if (emailOpenedAt == null) {
            if (isEmailUnopened(7)) return "UNOPENED_WARNING";
            return "SENT_UNOPENED";
        }
        return "OPENED";
    }
}
