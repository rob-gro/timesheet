package dev.robgro.timesheet.scheduler;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SchedulerRunRepository extends JpaRepository<SchedulerRun, String> {
}