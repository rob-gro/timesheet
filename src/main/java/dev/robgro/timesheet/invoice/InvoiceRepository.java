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

    List<Invoice> findAllByOrderByIssueDateDesc();

    List<Invoice> findByIssueDateBetweenOrderByIssueDateDesc(LocalDate startDate, LocalDate endDate);

    List<Invoice> findByClientId(Long clientId);

    List<Invoice> findByInvoiceNumberEndingWith(String yearMonth);

    List<Invoice> getInvoicesByClientId(Long clientId);

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
            "(:month IS NULL OR MONTH(i.issueDate) = :month)")
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
}
