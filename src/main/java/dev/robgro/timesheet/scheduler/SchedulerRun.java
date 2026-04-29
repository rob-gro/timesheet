package dev.robgro.timesheet.scheduler;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Records a scheduler run for status tracking and auditing.
 * Stored in {@code scheduler_run} table (created by V38 migration).
 */
@Entity
@Table(name = "scheduler_run")
@Getter
@Setter
@NoArgsConstructor
public class SchedulerRun {

    @Id
    @Column(name = "run_id", length = 36)
    private String runId;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "last_activity_at")
    private LocalDateTime lastActivityAt;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "triggered_by", length = 100)
    private String triggeredBy;

    public static SchedulerRun create(String runId, String triggeredBy) {
        SchedulerRun run = new SchedulerRun();
        run.runId = runId;
        run.triggeredBy = triggeredBy;
        run.startedAt = LocalDateTime.now();
        run.lastActivityAt = LocalDateTime.now();
        run.status = "RUNNING";
        return run;
    }
}