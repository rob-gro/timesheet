package dev.robgro.timesheet.invoice;

import dev.robgro.timesheet.exception.BusinessRuleViolationException;
import dev.robgro.timesheet.exception.ValidationException;
import dev.robgro.timesheet.seller.Seller;
import dev.robgro.timesheet.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Configurable invoice numbering scheme for a seller.
 * Each seller can have multiple schemes over time (historical + current).
 * Schemes are selected based on invoice issue date using effectiveFrom.
 *
 * LEGAL REQUIREMENT: Invoice numbers are immutable after creation.
 * Changing a scheme NEVER renumbers existing invoices.
 * This is required for:
 * - Legal compliance (unchangeable accounting records)
 * - Audit trails (traceable invoice history)
 * - Tax authority requirements (fixed invoice sequences)
 */
@Entity
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor
@Table(
    name = "invoice_numbering_schemes",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "unique_seller_effective_version",
            columnNames = {"seller_id", "effective_from", "version"}
        )
    },
    indexes = {
        @Index(name = "idx_seller_effective", columnList = "seller_id, effective_from DESC")
    }
)
public class InvoiceNumberingScheme {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private Seller seller;

    /**
     * Template string with tokens: {SEQ:3}, {MM}, {YYYY}, {DEPT}
     * Example: "{SEQ:3}-{MM}-{YYYY}" produces "001-01-2026"
     */
    @Column(nullable = false, length = 64)
    private String template;

    /**
     * Defines when sequence resets to 1: MONTHLY, YEARLY, or NEVER
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "reset_period", nullable = false, length = 20)
    private ResetPeriod resetPeriod;

    /**
     * Date from which this scheme applies to new invoices.
     * For backdated invoices, system selects scheme where effectiveFrom <= issueDate.
     * CRITICAL: Must be unique per seller to avoid ambiguity.
     */
    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    /**
     * Version/revision number for schemes with same effective_from date.
     * Allows multiple schemes with same effective date (e.g., for testing, corrections).
     * Latest version is used for new invoices. Older versions preserved for audit trail.
     */
    @Column(name = "version", nullable = false)
    private Integer version = 1;

    /**
     * Scheme status: ACTIVE (current), ARCHIVED (historical), or DRAFT (planned)
     * Both ACTIVE and ARCHIVED schemes work for backdated invoices.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SchemeStatus status = SchemeStatus.ACTIVE;

    /**
     * Generated column for enforcing single ACTIVE scheme per (seller, effective_from).
     * Value: CAST(seller_id AS CHAR) when ACTIVE, NULL when ARCHIVED.
     * UNIQUE constraint on this column prevents two ACTIVE schemes for same seller.
     * Read-only - managed entirely by the database (insertable=false, updatable=false).
     */
    @Column(name = "active_key", insertable = false, updatable = false)
    private String activeKey;

    // Audit fields - automatically managed by Spring Data JPA Auditing
    // SECURITY: updatable=false prevents overwriting via merge/mapping
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @CreatedBy
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id", nullable = false, updatable = false)
    private User createdBy;

    /**
     * Factory method to create a new active scheme.
     * Validates required fields and sets default status.
     *
     * @param seller Seller this scheme belongs to (required)
     * @param template Template string with tokens (required, max 64 chars)
     * @param resetPeriod When sequence resets (required)
     * @param effectiveFrom Date from which scheme applies (required)
     * @return New scheme with ACTIVE status
     * @throws IllegalArgumentException if validation fails
     */
    public static InvoiceNumberingScheme create(
        Seller seller,
        String template,
        ResetPeriod resetPeriod,
        LocalDate effectiveFrom,
        Integer version
    ) {
        if (seller == null) {
            throw new ValidationException("Seller is required");
        }
        if (template == null || template.isBlank()) {
            throw new ValidationException("Template is required");
        }
        if (template.length() > 64) {
            throw new ValidationException("Template too long (max 64 characters)");
        }
        if (resetPeriod == null) {
            throw new ValidationException("Reset period is required");
        }
        if (effectiveFrom == null) {
            throw new ValidationException("Effective from date is required");
        }
        if (version == null || version < 1) {
            throw new ValidationException("Version must be >= 1");
        }

        InvoiceNumberingScheme scheme = new InvoiceNumberingScheme();
        scheme.seller = seller;
        scheme.template = template;
        scheme.resetPeriod = resetPeriod;
        scheme.effectiveFrom = effectiveFrom;
        scheme.version = version;
        scheme.status = SchemeStatus.ACTIVE;
        return scheme;
    }

    /**
     * Archives this scheme - marks as historical.
     * Archived schemes remain available for backdated invoices.
     *
     * @throws IllegalStateException if scheme is already archived
     */
    public void archive() {
        if (this.status == SchemeStatus.ARCHIVED) {
            throw new BusinessRuleViolationException("Scheme is already archived");
        }
        this.status = SchemeStatus.ARCHIVED;
    }

    /**
     * Checks if this scheme is effective for a given invoice date.
     * A scheme is effective if:
     * - effectiveFrom <= date (not in the future relative to invoice date)
     * - status is ACTIVE or ARCHIVED (both work for backdating)
     *
     * DRAFT schemes are never effective.
     *
     * @param date Invoice issue date to check
     * @return true if scheme applies to this date
     */
    public boolean isEffectiveOn(LocalDate date) {
        if (date == null) {
            return false;
        }
        // Effective if scheme started on or before the invoice date
        boolean dateMatches = !date.isBefore(effectiveFrom);

        // Status must be ACTIVE or ARCHIVED (DRAFT not allowed)
        boolean statusValid = (status == SchemeStatus.ACTIVE || status == SchemeStatus.ARCHIVED);

        return dateMatches && statusValid;
    }

}
