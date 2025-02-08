package dev.robgro.timesheet.repository;

import dev.robgro.timesheet.model.entity.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.nio.channels.FileChannel;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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
    @Query("DELETE FROM InvoiceItem i WHERE i.invoice.id = :invoiceId")
    void deleteInvoiceItemsByInvoiceId(@Param("invoiceId") Long invoiceId);

    @Query(value = """
        SELECT i FROM Invoice i
        WHERE (:clientId IS NULL OR i.client.id = :clientId)
        AND (:year IS NULL OR YEAR(i.issueDate) = :year)
        AND (:month IS NULL OR MONTH(i.issueDate) = :month)
        """)
    Page<Invoice> findFilteredInvoices(
            @Param("clientId") Long clientId,
            @Param("year") Integer year,
            @Param("month") Integer month,
            Pageable pageable
    );

    @Query("""
    SELECT i FROM Invoice i 
    WHERE (:clientId IS NULL OR i.client.id = :clientId) 
    AND (:fromDate IS NULL OR i.issueDate >= :fromDate) 
    AND (:toDate IS NULL OR i.issueDate <= :toDate)
""")
    Page<Invoice> findByDateRangeAndClient(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("clientId") Long clientId,
            Pageable pageable
    );
}
