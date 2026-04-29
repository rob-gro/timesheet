-- ============================================================
-- V35: Invoice Delivery Job — async delivery queue
-- ============================================================
-- Architecture: at-least-once delivery with delivery_hash idempotency.
-- is_active = 1   → PENDING or PROCESSING (active in queue)
-- is_active = NULL → SENT or FAILED (historical, removed from queue)
-- UNIQUE(invoice_id, is_active): MariaDB allows multiple NULLs in UNIQUE index
-- → enforces at most ONE active job per invoice, unlimited historical records
-- ============================================================

CREATE TABLE invoice_delivery_job (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    invoice_id     BIGINT NOT NULL,
    run_id         VARCHAR(36)          NULL,
    status         VARCHAR(20)          NOT NULL DEFAULT 'PENDING',
    attempts       INT                  NOT NULL DEFAULT 0,
    next_retry_at  DATETIME             NOT NULL DEFAULT CURRENT_TIMESTAMP,
    locked_at      DATETIME             NULL,
    locked_by      VARCHAR(100)         NULL,
    last_error     TEXT                 NULL,
    created_at     DATETIME             NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME             NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    -- Idempotency token for atomic claim (UUID, cleared after processing)
    claim_token    VARCHAR(36)          NULL,

    -- Delivery hash: SHA-256(invoiceId | invoiceNumber | printMode)
    -- Written ONLY in markSent() — never at claim time.
    -- Before send: SELECT WHERE invoice_id=? AND delivery_hash=? AND status='SENT'
    -- If found → skip (crash-recovery idempotency). manual=1 → skip hash check.
    delivery_hash  VARCHAR(64)          NULL,

    -- manual=1: user-triggered resend, bypasses delivery_hash check
    manual         TINYINT(1)           NOT NULL DEFAULT 0,

    -- is_active: 1=active (PENDING/PROCESSING), NULL=historical (SENT/FAILED)
    -- UNIQUE index on (invoice_id, is_active=1) enforces single active job per invoice
    -- NULL values are not unique — allows unlimited historical records per invoice
    is_active      INT                  NULL DEFAULT 1,

    CONSTRAINT fk_delivery_invoice FOREIGN KEY (invoice_id) REFERENCES invoices(id),

    -- Enforce single active job per invoice (is_active=1 unique, NULLs allowed)
    UNIQUE INDEX uniq_invoice_active (invoice_id, is_active),

    -- Worker poll: find PENDING jobs ready for retry
    INDEX idx_delivery_status_retry (status, next_retry_at),

    -- Stale lock recovery: find PROCESSING jobs with old locked_at
    INDEX idx_delivery_processing_lock (status, locked_at),

    -- Claim result fetch: find jobs by claim token after atomic UPDATE
    INDEX idx_delivery_claim_token (claim_token),

    -- Run tracking: find all jobs for a scheduler run
    INDEX idx_delivery_run_id (run_id),

    -- Idempotency check: SELECT WHERE invoice_id=? AND delivery_hash=? AND status='SENT'
    INDEX idx_delivery_hash_lookup (invoice_id, status, delivery_hash)

) ENGINE = InnoDB;