package dev.robgro.timesheet.repository;

import dev.robgro.timesheet.model.entity.Timesheet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface TimesheetRepository extends JpaRepository<Timesheet, Long> {
    List<Timesheet> findByIsInvoice(boolean isInvoice);

    List<Timesheet> findAllByClientId(Long clientId);

    List<Timesheet> findByClientIdAndIsInvoiceFalse(Long clientId);

    List<Timesheet> findByClient_IdAndServiceDateBetween(Long clientId, LocalDate startDate, LocalDate endDate);
}
