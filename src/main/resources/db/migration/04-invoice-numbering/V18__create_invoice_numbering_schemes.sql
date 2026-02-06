-- Create Invoice Numbering Schemes Table
-- Stores configurable invoice numbering formats per seller

CREATE TABLE invoice_numbering_schemes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    seller_id BIGINT NOT NULL,

    -- Template using tokens: {SEQ:3}-{MM}-{YYYY}
    template VARCHAR(64) NOT NULL COMMENT 'Format template: {SEQ:3}-{MM}-{YYYY}',

    -- Reset strategy: MONTHLY (reset each month), YEARLY (reset each year), NEVER (continuous)
    reset_period ENUM('MONTHLY', 'YEARLY', 'NEVER') NOT NULL DEFAULT 'MONTHLY',

    -- When this scheme becomes effective (supports backdated invoices)
    effective_from DATE NOT NULL COMMENT 'Scheme applies from this date',

    -- Status: ACTIVE (current), ARCHIVED (historical, for backdated), DRAFT (future)
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE, ARCHIVED, DRAFT',

    -- Audit fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT,

    -- Foreign Keys
    CONSTRAINT fk_scheme_seller
        FOREIGN KEY (seller_id) REFERENCES sellers(id) ON DELETE CASCADE,
    CONSTRAINT fk_scheme_creator
        FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,

    -- Indexes
    INDEX idx_seller_effective (seller_id, effective_from DESC),

    -- CRITICAL: One scheme per seller per date (allows multiple schemes for history)
    UNIQUE KEY unique_seller_effective_from (seller_id, effective_from)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
