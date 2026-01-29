-- Add seller_id to invoices table
-- Links each invoice to a seller

ALTER TABLE invoices
ADD COLUMN seller_id BIGINT;
