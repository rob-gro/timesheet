-- Migrate existing data to use default seller
-- Creates Agnieszka Markiewicz as the default seller and assigns all existing data to her

-- Insert default seller (Agnieszka Markiewicz)
INSERT INTO sellers (
    name, street, postcode, city,
    service_description,
    bank_name, account_number, sort_code,
    email, phone,
    legal_form, active
) VALUES (
    'Agnieszka Markiewicz',
    '28 Ballater Place',
    'DD4 8SF',
    'Dundee',
    'Cleaning services',
    'TSB',
    '75040460',
    '87-68-20',
    'agnieszka.markiewicz.szkocja@gmail.com',
    '+44 747 8385 228',
    'Sole Trader',
    TRUE
);

-- Store the seller ID for use in subsequent updates
SET @default_seller_id = LAST_INSERT_ID();

-- Assign all existing invoices to default seller
UPDATE invoices
SET seller_id = @default_seller_id
WHERE seller_id IS NULL;

-- Assign all existing timesheets to default seller
UPDATE timesheets
SET seller_id = @default_seller_id
WHERE seller_id IS NULL;

-- Make seller_id NOT NULL in timesheets (now that all records have a value)
ALTER TABLE timesheets MODIFY seller_id BIGINT NOT NULL;

-- Optionally set default seller for existing users
-- UPDATE users SET default_seller_id = @default_seller_id WHERE default_seller_id IS NULL;

-- Verification queries
SELECT CONCAT('✅ Created seller: ', name, ' (ID: ', id, ')') AS status FROM sellers WHERE id = @default_seller_id;
SELECT CONCAT('✅ Updated ', COUNT(*), ' invoices') AS status FROM invoices WHERE seller_id = @default_seller_id;
SELECT CONCAT('✅ Updated ', COUNT(*), ' timesheets') AS status FROM timesheets WHERE seller_id = @default_seller_id;
