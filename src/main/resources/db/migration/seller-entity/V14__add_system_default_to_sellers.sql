-- Add is_system_default column to sellers table for CRON job
-- Indicates which seller is used by CRON job for automatic monthly invoicing
-- Only one seller should have this flag set to TRUE at a time
ALTER TABLE sellers
ADD COLUMN is_system_default BOOLEAN NOT NULL DEFAULT FALSE;

-- Set the first active seller as system default
UPDATE sellers
SET is_system_default = TRUE
WHERE id = (
    SELECT id
    FROM sellers
    WHERE active = TRUE
    ORDER BY id ASC
    LIMIT 1
);

