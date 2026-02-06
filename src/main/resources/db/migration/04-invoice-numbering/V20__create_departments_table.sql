-- Create Departments Table (v1 Feature)
-- Enables multi-department sellers (e.g., DUT, DUI)
-- Structure only - implementation in future release

CREATE TABLE departments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    seller_id BIGINT NOT NULL,

    -- Department identifiers
    code VARCHAR(10) NOT NULL COMMENT 'Short code for templates (e.g., DUT, DUI)',
    name VARCHAR(100) NOT NULL COMMENT 'Full department name',

    -- Soft delete
    active BOOLEAN NOT NULL DEFAULT TRUE,

    -- Audit fields
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    -- Foreign Keys
    CONSTRAINT fk_department_seller
        FOREIGN KEY (seller_id) REFERENCES sellers(id) ON DELETE CASCADE,

    -- One code per seller
    UNIQUE KEY unique_seller_code (seller_id, code)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add FK constraint to invoices.department_id (column added in V19)
ALTER TABLE invoices
ADD CONSTRAINT fk_invoice_department
    FOREIGN KEY (department_id) REFERENCES departments(id) ON DELETE SET NULL;
