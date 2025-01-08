package dev.robgro.timesheet.repository;

import dev.robgro.timesheet.model.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    List<Invoice> getInvoicesByClientId(Long clientId);

    @Modifying
    @Query("DELETE FROM InvoiceItem i WHERE i.invoice.id = :invoiceId")
    void deleteInvoiceItemsByInvoiceId(@Param("invoiceId") Long invoiceId);

    @Modifying
    @Query("DELETE FROM InvoiceItem i WHERE i.id = :id")
    void deleteInvoiceItem(@Param("id") Long id);
}
