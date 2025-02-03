package dev.robgro.timesheet.repository;

import dev.robgro.timesheet.model.entity.Timesheet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface TimesheetRepository extends JpaRepository<Timesheet, Long> {

    Page<Timesheet> findAll(Pageable pageable);
    Page<Timesheet> findAllByClientId(Long clientId, Pageable pageable);

//    Page<Timesheet> findByInvoiced(boolean invoiced, Pageable pageable);
//    Page<Timesheet> findByClientIdAndInvoicedFalse(Long clientId, Pageable pageable);
//    Page<Timesheet> findByClient_IdAndServiceDateBetween(Long clientId, LocalDate startDate, LocalDate endDate, Pageable pageable);
//    Page<Timesheet> findByClientIdAndInvoiced(Long clientId, boolean invoiced, Pageable pageable);
    List<Timesheet> findByInvoiced(boolean invoiced);

    List<Timesheet> findAllByClientId(Long clientId);

    List<Timesheet> findByClientIdAndInvoicedFalse(Long clientId);

    List<Timesheet> findByClient_IdAndServiceDateBetween(Long clientId, LocalDate startDate, LocalDate endDate);

    List<Timesheet> findByClientIdAndInvoiced(Long clientId, boolean invoiced);

//    @Query(value = "SELECT t.* FROM timesheets t " +
//            "LEFT JOIN invoices i ON t.invoice_id = i.id " +
//            "WHERE (:clientId IS NULL OR t.client_id = :clientId) " +
//            "ORDER BY STR_TO_DATE(i.invoice_number, '%d-%m-%Y') ASC, t.id ASC",
//            countQuery = "SELECT COUNT(*) FROM timesheets t WHERE (:clientId IS NULL OR t.client_id = :clientId)",
//            nativeQuery = true)
//    Page<Timesheet> findAllSortedByInvoiceNumber(@Param("clientId") Long clientId, Pageable pageable);

    @Query(value = "SELECT t.* FROM timesheets t " +
            "LEFT JOIN invoices i ON t.invoice_id = i.id " +
            "WHERE (:clientId IS NULL OR t.client_id = :clientId) " +
            "ORDER BY STR_TO_DATE(i.invoice_number, '%d-%m-%Y') ASC",
            countQuery = "SELECT COUNT(*) FROM timesheets t WHERE (:clientId IS NULL OR t.client_id = :clientId)",
            nativeQuery = true)
    Page<Timesheet> findAllSortedByInvoiceNumber(@Param("clientId") Long clientId, Pageable pageable);
}
