package dev.robgro.timesheet.repository;

import dev.robgro.timesheet.model.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    List<Invoice> findByClientIdAndIssueDateBetween(Long clientId, LocalDate startDate, LocalDate endDate);

    long countByInvoiceNumberEndingWith(String yearMonth);
}
