package dev.robgro.timesheet.invoice;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for invoice numbering schemes.
 * Manages configurable invoice numbering per seller (tenant).
 */
@RepositoryRestResource(exported = false)
public interface InvoiceNumberingSchemeRepository extends JpaRepository<InvoiceNumberingScheme, Long> {

    /**
     * CRITICAL: Find scheme effective on specific date.
     * Priority: ACTIVE scheme wins over ARCHIVED regardless of effectiveFrom order.
     * Secondary: among same status, latest effectiveFrom wins.
     * Tertiary: among same effectiveFrom, highest version wins.
     *
     * Rationale: sellers test multiple formats (same or different effectiveFrom).
     * The ACTIVE scheme is the one explicitly chosen — it must always be used
     * for new invoices, even if ARCHIVED schemes have more recent effectiveFrom dates.
     *
     * Example:
     * - Scheme A: effectiveFrom=2020-01-01, v4, ACTIVE   → returned ✅
     * - Scheme B: effectiveFrom=2026-02-03, v1, ARCHIVED → skipped ✅
     *
     * Both ACTIVE and ARCHIVED are searched (ARCHIVED needed for backdating when
     * no ACTIVE scheme exists). DRAFT schemes are excluded.
     *
     * @param sellerId Seller ID (tenant isolation)
     * @param issueDate Issue date of invoice (NOT LocalDate.now()!)
     * @return Best-matching scheme with effectiveFrom <= issueDate, or empty if none found
     */
    @Query("""
        SELECT s FROM InvoiceNumberingScheme s
        WHERE s.seller.id = :sellerId
          AND s.effectiveFrom <= :issueDate
          AND s.status IN (dev.robgro.timesheet.invoice.SchemeStatus.ACTIVE,
                          dev.robgro.timesheet.invoice.SchemeStatus.ARCHIVED)
        ORDER BY CASE WHEN s.status = dev.robgro.timesheet.invoice.SchemeStatus.ACTIVE THEN 0 ELSE 1 END ASC,
                 s.effectiveFrom DESC,
                 s.version DESC
        LIMIT 1
        """)
    Optional<InvoiceNumberingScheme> findEffectiveScheme(
        @Param("sellerId") Long sellerId,
        @Param("issueDate") LocalDate issueDate
    );

    /**
     * Find all schemes for seller, ordered by effective date (newest first).
     * Used for displaying numbering history in UI.
     *
     * @param sellerId Seller ID
     * @return All schemes for this seller, ordered by effectiveFrom DESC
     */
    @Query("""
        SELECT s FROM InvoiceNumberingScheme s
        WHERE s.seller.id = :sellerId
        ORDER BY CASE WHEN s.status = dev.robgro.timesheet.invoice.SchemeStatus.ACTIVE THEN 0 ELSE 1 END ASC,
                 s.version DESC
        """)
    List<InvoiceNumberingScheme> findAllBySeller(@Param("sellerId") Long sellerId);

    /**
     * Find only active schemes for seller.
     * Used for UI configuration pages to show current active schemes.
     *
     * @param sellerId Seller ID
     * @return Active schemes for this seller, ordered by effectiveFrom DESC
     */
    @Query("""
        SELECT s FROM InvoiceNumberingScheme s
        WHERE s.seller.id = :sellerId
          AND s.status = dev.robgro.timesheet.invoice.SchemeStatus.ACTIVE
        ORDER BY s.effectiveFrom DESC
        """)
    List<InvoiceNumberingScheme> findActiveBySeller(@Param("sellerId") Long sellerId);

    /**
     * Lock the ACTIVE scheme for given (seller, effectiveFrom) — SELECT FOR UPDATE.
     * Used by TransactionalWorker to serialize concurrent "replace scheme" requests
     * for the same effective date.
     *
     * If no ACTIVE scheme exists for this date, returns empty (no lock acquired).
     * In that case unique_active_scheme constraint acts as last line of defense.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT s FROM InvoiceNumberingScheme s
        WHERE s.seller.id = :sellerId
          AND s.effectiveFrom = :effectiveFrom
          AND s.status = dev.robgro.timesheet.invoice.SchemeStatus.ACTIVE
        """)
    Optional<InvoiceNumberingScheme> findActiveForUpdate(
        @Param("sellerId") Long sellerId,
        @Param("effectiveFrom") LocalDate effectiveFrom
    );

    /**
     * Returns max version for given (seller, effectiveFrom), or 0 if none exist.
     * Next version = result + 1.
     * Not locked — retry + unique_seller_effective_version handles concurrent inserts.
     */
    @Query("""
        SELECT COALESCE(MAX(s.version), 0) FROM InvoiceNumberingScheme s
        WHERE s.seller.id = :sellerId
          AND s.effectiveFrom = :effectiveFrom
        """)
    int findMaxVersion(
        @Param("sellerId") Long sellerId,
        @Param("effectiveFrom") LocalDate effectiveFrom
    );
}
