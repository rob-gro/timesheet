-- Create Sellers Table
-- Stores seller/business information for invoices

CREATE TABLE IF NOT EXISTS sellers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    -- Basic information
    name VARCHAR(255) NOT NULL,
    street VARCHAR(255) NOT NULL,
    postcode VARCHAR(50) NOT NULL,
    city VARCHAR(100) NOT NULL,

    -- Service description (what seller does)
    service_description VARCHAR(255) NOT NULL,  -- e.g., "Cleaning services", "Programming", "Trimming hedge", "Windows cleaning"

    -- Bank details (UK)
    bank_name VARCHAR(100),
    account_number VARCHAR(20),
    sort_code VARCHAR(10),

    -- Contact information
    email VARCHAR(255),
    phone VARCHAR(50),

    -- Company/Legal information
    company_registration_number VARCHAR(50),
    legal_form VARCHAR(50),  -- e.g., "Sole Trader", "Ltd", "PLC"

    -- Tax information (optional - different services may have different tax rates)
    vat_number VARCHAR(50),
    tax_id VARCHAR(50),

    -- Soft delete pattern
    active BOOLEAN NOT NULL DEFAULT TRUE,

    -- Audit fields
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT,

    -- Indexes
    INDEX idx_active (active),
    INDEX idx_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
