package dev.robgro.timesheet.invoice;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity for atomic invoice number sequence counters.
 * Maps to invoice_number_counters table (created by V25 migration).
 *
 * <p>This entity stores the last used sequence number for each combination of:
 * - seller (tenant)
 * - reset period (MONTHLY, YEARLY, NEVER)
 * - period key (e.g., "2026-02", "2026", "NEVER")
 *
 * <p>The UPSERT pattern in repository ensures atomic increment without race conditions.
 */
@Entity
@Table(
    name = "invoice_number_counters",
    uniqueConstraints = @UniqueConstraint(
        name = "unique_counter_scope",
        columnNames = {"seller_id", "reset_period", "period_key"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceNumberCounter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @Enumerated(EnumType.STRING)
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.VARCHAR)
    @Column(name = "reset_period", nullable = false, length = 20)
    private ResetPeriod resetPeriod;

    /**
     * Period key matching V27 backfill format:
     * - MONTHLY: "YYYY-MM" (e.g., "2026-02")
     * - YEARLY: "YYYY" (e.g., "2026")
     * - NEVER: "NEVER"
     */
    @Column(name = "period_key", nullable = false, length = 32)
    private String periodKey;

    /**
     * Last sequence number generated for this scope.
     * Incremented atomically via UPSERT in repository.
     */
    @Column(name = "last_value", nullable = false)
    private Integer lastValue;

    /**
     * Fiscal year start year (for future FISCAL_YEAR support).
     * Currently unused (FISCAL_YEAR not implemented).
     */
    @Column(name = "fy_start_year")
    private Integer fyStartYear;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
