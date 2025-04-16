package dev.robgro.timesheet.repository;

import dev.robgro.timesheet.model.entity.Timesheet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RestResource;

import java.time.LocalDate;
import java.util.List;

public interface TimesheetRepository extends JpaRepository<Timesheet, Long> {
    List<Timesheet> findByInvoiced(boolean invoiced);

    @RestResource(path = "byClientId")
    List<Timesheet> findAllByClientId(Long clientId);

    List<Timesheet> findByClientIdAndInvoicedFalse(Long clientId);

    List<Timesheet> findByClient_IdAndServiceDateBetween(Long clientId, LocalDate startDate, LocalDate endDate);

    List<Timesheet> findByClientIdAndInvoiced(Long clientId, boolean invoiced);

    List<Timesheet> findByClientIdAndPaymentDateIsNotNull(Long clientId);

    @Query("SELECT t FROM Timesheet t WHERE t.client.id = :clientId AND t.paymentDate IS NULL")
    List<Timesheet> findByClientIdAndPaymentDateIsNull(@Param("clientId") Long clientId);

    @RestResource(path = "byClientIdAndPaymentDateIsNotNullPaged")
    Page<Timesheet> findByClientIdAndPaymentDateIsNotNull(Long clientId, Pageable pageable);

    @RestResource(path = "byClientIdAndPaymentDateIsNullPaged")
    @Query("SELECT t FROM Timesheet t WHERE t.client.id = :clientId AND t.paymentDate IS NULL")
    Page<Timesheet> findByClientIdAndPaymentDateIsNull(@Param("clientId") Long clientId, Pageable pageable);

    Page<Timesheet> findByClientId(Long clientId, Pageable pageable);

    @Query("SELECT t FROM Timesheet t WHERE t.invoiced = false AND " +
            "(:clientId IS NULL OR t.client.id = :clientId)")
    List<Timesheet> findUnbilledTimesheetsByClientId(@Param("clientId") Long clientId);

    @Query(value = "SELECT t.* FROM timesheets t " +
            "LEFT JOIN invoices i ON t.invoice_id = i.id " +
            "WHERE (:clientId IS NULL OR t.client_id = :clientId) " +
            "ORDER BY STR_TO_DATE(i.invoice_number, '%d-%m-%Y') ASC",
            countQuery = "SELECT COUNT(*) FROM timesheets t WHERE (:clientId IS NULL OR t.client_id = :clientId)",
            nativeQuery = true)
    Page<Timesheet> findAllSortedByInvoiceNumber(@Param("clientId") Long clientId, Pageable pageable);

    Page<Timesheet> findAll(Pageable pageable);

    @RestResource(path = "byClientIdPaged")
    Page<Timesheet> findAllByClientId(Long clientId, Pageable pageable);

    @Query(value = "SELECT t FROM Timesheet t WHERE " +
            "(:clientId IS NULL OR t.client.id = :clientId) AND " +
            "(:paymentStatus IS NULL OR " +
            "(:paymentStatus = 'true' AND t.paymentDate IS NOT NULL) OR " +
            "(:paymentStatus = 'false' AND t.paymentDate IS NULL))",
            countQuery = "SELECT COUNT(t) FROM Timesheet t WHERE " +
                    "(:clientId IS NULL OR t.client.id = :clientId) AND " +
                    "(:paymentStatus IS NULL OR " +
                    "(:paymentStatus = 'true' AND t.paymentDate IS NOT NULL) OR " +
                    "(:paymentStatus = 'false' AND t.paymentDate IS NULL))")
    Page<Timesheet> findByClientIdAndPaymentStatus(
            @Param("clientId") Long clientId,
            @Param("paymentStatus") String paymentStatus,
            Pageable pageable);
}
