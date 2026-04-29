-- ============================================================
-- V36: Scheduler Run — tracks "Run Now" executions
-- ============================================================
-- run_id (UUID) is generated when "Run Now" is triggered.
-- last_activity_at is bumped by each markSent/markFailed call.
-- Used by RunStatusService for STALLED detection:
--   STALLED = RUNNING but (now - last_activity_at) > run-timeout-minutes
-- ============================================================

CREATE TABLE scheduler_run (
    run_id           VARCHAR(36)   NOT NULL,
    started_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finished_at      DATETIME      NULL,
    last_activity_at DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status           VARCHAR(20)   NOT NULL DEFAULT 'RUNNING',
    triggered_by     VARCHAR(100)  NOT NULL,  -- CRON, ADMIN

    PRIMARY KEY (run_id)
) ENGINE = InnoDB;