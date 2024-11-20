package dev.robgro.timesheet.repository;

import dev.robgro.timesheet.model.Timesheet;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TimesheetRepository extends JpaRepository<Timesheet, Long> {
}
