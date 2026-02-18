package dev.robgro.timesheet.invoice;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RepositoryRestResource(exported = false)
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    List<Invoice> findByClientIdAndIssueDateBetween(Long clientId, LocalDate startDate, LocalDate endDate);

    long countByInvoiceNumberEndingWith(String yearMonth);

    // Sort by invoice number components (year DESC, month DESC, sequence DESC)
    // NOT by invoice_number string (alphabetical) or ID (insertion order)
    // Allows backdated invoices to appear in correct logical order
    List<Invoice> findAllByOrderByPeriodYearDescPeriodMonthDescSequenceNumberDesc();

    List<Invoice> findByIssueDateBetweenOrderByPeriodYearDescPeriodMonthDescSequenceNumberDesc(LocalDate startDate, LocalDate endDate);

    List<Invoice> findByClientIdOrderByPeriodYearDescPeriodMonthDescSequenceNumberDesc(Long clientId);

    List<Invoice> findByInvoiceNumberEndingWith(String yearMonth);

    @Modifying
    @Query("DELETE FROM InvoiceItem i WHERE i.id = :id")
    void deleteInvoiceItem(@Param("id") Long id);

    @Modifying
    @Query(value = "DELETE FROM invoice_items WHERE invoice_id = :invoiceId", nativeQuery = true)
    void deleteInvoiceItemsByInvoiceId(@Param("invoiceId") Long invoiceId);

    @Query("SELECT i FROM Invoice i WHERE " +
            "(:fromDate IS NULL OR i.issueDate >= :fromDate) AND " +
            "(:toDate IS NULL OR i.issueDate <= :toDate) AND " +
            "(:clientId IS NULL OR i.client.id = :clientId)")
    Page<Invoice> findByDateRangeAndClient(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("clientId") Long clientId,
            Pageable pageable);

    @Query("SELECT i FROM Invoice i WHERE " +
            "(:clientId IS NULL OR i.client.id = :clientId) AND " +
            "(:year IS NULL OR YEAR(i.issueDate) = :year) AND " +
            "(:month IS NULL OR MONTH(i.issueDate) = :month) " +
            "ORDER BY i.periodYear DESC, i.periodMonth DESC, i.sequenceNumber DESC")
    List<Invoice> findFilteredInvoices(
            @Param("clientId") Long clientId,
            @Param("year") Integer year,
            @Param("month") Integer month);

    @Query("SELECT i FROM Invoice i WHERE " +
            "(:clientId IS NULL OR i.client.id = :clientId) AND " +
            "(:year IS NULL OR YEAR(i.issueDate) = :year) AND " +
            "(:month IS NULL OR MONTH(i.issueDate) = :month)")
    Page<Invoice> findFilteredInvoices(
            @Param("clientId") Long clientId,
            @Param("year") Integer year,
            @Param("month") Integer month,
            Pageable pageable);

    @Query("SELECT i FROM Invoice i WHERE " +
            "(:clientId IS NULL OR i.client.id = :clientId) AND " +
            "(:fromDate IS NULL OR i.issueDate >= :fromDate) AND " +
            "(:toDate IS NULL OR i.issueDate <= :toDate)")
    List<Invoice> findForReporting(
            @Param("clientId") Long clientId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            Sort sort);

    @Modifying
    @Query(value = "INSERT INTO invoice_items (invoice_id, service_date, description, duration, amount, timesheet_id) VALUES (:invoiceId, :serviceDate, :description, :duration, :amount, :timesheetId)", nativeQuery = true)
    void insertInvoiceItem(
            @Param("invoiceId") Long invoiceId,
            @Param("serviceDate") java.sql.Date serviceDate,
            @Param("description") String description,
            @Param("duration") double duration,
            @Param("amount") BigDecimal amount,
            @Param("timesheetId") Long timesheetId
    );

    // ===== Configurable Invoice Numbering =====

    /**
     * Find maximum sequence number for seller and period.
     * Used for drift detection in observability endpoint (compares counter with actual MAX).
     *
     * <p>Note: Department does NOT affect sequence generation - only display.
     * This query excludes department filtering as counters are scoped per (seller, period) only.
     *
     * @param sellerId Seller ID (tenant isolation)
     * @param year Period year (e.g., 2026)
     * @param month Period month: 1-12 for MONTHLY, 0 for YEARLY/NEVER (NOT NULL!)
     * @return Maximum sequence number for this period, or null if no invoices exist
     */
    @Query("SELECT MAX(i.sequenceNumber) FROM Invoice i WHERE " +
           "i.seller.id = :sellerId AND " +
           "i.periodYear = :year AND " +
           "i.periodMonth = :month")
    Integer findMaxSequenceNumber(
        @Param("sellerId") Long sellerId,
        @Param("year") Integer year,
        @Param("month") Integer month
    );

    /**
     * Count total invoices for seller and period.
     * Used for observability endpoint to show invoice count vs counter value.
     *
     * @param sellerId Seller ID (tenant isolation)
     * @param year Period year (e.g., 2026)
     * @param month Period month: 1-12 for MONTHLY, 0 for YEARLY/NEVER
     * @return Total invoice count for this period
     */
    Long countBySellerIdAndPeriodYearAndPeriodMonth(
        Long sellerId,
        Integer year,
        Integer month
    );

    /**
     * Find last invoice for seller and period (by sequence number DESC).
     * Used for observability endpoint to show last invoice display number.
     *
     * @param sellerId Seller ID (tenant isolation)
     * @param year Period year (e.g., 2026)
     * @param month Period month: 1-12 for MONTHLY, 0 for YEARLY/NEVER
     * @return Last invoice (highest sequence number), or empty if no invoices
     */
    Optional<Invoice> findTopBySellerIdAndPeriodYearAndPeriodMonthOrderBySequenceNumberDesc(
        Long sellerId,
        Integer year,
        Integer month
    );
}
