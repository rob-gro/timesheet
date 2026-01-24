-- =====================================================
-- ROLLBACK: Seller Entity Migrations (V4-V8)
-- Database: robgro_test_invoices (TEST)
-- Purpose: Remove seller-related schema changes to match PRODUCTION
-- =====================================================
--
-- WHAT THIS DOES:
-- 1. Removes foreign key constraints
-- 2. Drops indexes
-- 3. Removes seller_id columns from timesheets, invoices, users
-- 4. Drops sellers table
--
-- IMPACT:
-- ✅ Fixes: "Cannot add or update a child row: FK constraint fails" error
-- ✅ Matches PRODUCTION schema
-- ⚠️  Loses: Any data in sellers table (if exists)
--
-- EXECUTION TIME: ~2-5 seconds
-- =====================================================

START TRANSACTION;

-- Safety check: Verify we're on the correct database
SELECT DATABASE() as current_database,
       'Expected: robgro_test_invoices' as expected_database;

-- =====================================================
-- STEP 1: Drop Foreign Key Constraints
-- =====================================================

SELECT '>>> STEP 1/5: Dropping foreign key constraints...' AS status;

-- Drop FK from invoices table
ALTER TABLE invoices
DROP FOREIGN KEY IF EXISTS fk_invoice_seller;

-- Drop FK from timesheets table
ALTER TABLE timesheets
DROP FOREIGN KEY IF EXISTS fk_timesheet_seller;

-- Drop FK from users table
ALTER TABLE users
DROP FOREIGN KEY IF EXISTS fk_user_default_seller;

SELECT '✅ Foreign key constraints dropped' AS status;

-- =====================================================
-- STEP 2: Drop Indexes
-- =====================================================

SELECT '>>> STEP 2/5: Dropping indexes...' AS status;

-- Drop index from invoices
DROP INDEX IF EXISTS idx_invoice_seller ON invoices;

-- Drop index from timesheets
DROP INDEX IF EXISTS idx_timesheet_seller ON timesheets;

-- Drop index from users
DROP INDEX IF EXISTS idx_user_default_seller ON users;

SELECT '✅ Indexes dropped' AS status;

-- =====================================================
-- STEP 3: Check for existing data (WARNING if data exists)
-- =====================================================

SELECT '>>> STEP 3/5: Checking for existing data...' AS status;

SELECT
    'sellers' as table_name,
    COUNT(*) as row_count,
    CASE
        WHEN COUNT(*) > 0 THEN '⚠️  WARNING: Data will be lost!'
        ELSE '✅ No data to lose'
    END as warning
FROM sellers
UNION ALL
SELECT
    'invoices.seller_id' as table_name,
    COUNT(*) as row_count,
    CASE
        WHEN COUNT(*) > 0 THEN '⚠️  WARNING: seller_id values will be lost!'
        ELSE '✅ No data to lose'
    END as warning
FROM invoices
WHERE seller_id IS NOT NULL
UNION ALL
SELECT
    'timesheets.seller_id' as table_name,
    COUNT(*) as row_count,
    CASE
        WHEN COUNT(*) > 0 THEN '⚠️  WARNING: seller_id values will be lost!'
        ELSE '✅ No data to lose'
    END as warning
FROM timesheets
WHERE seller_id IS NOT NULL
UNION ALL
SELECT
    'users.default_seller_id' as table_name,
    COUNT(*) as row_count,
    CASE
        WHEN COUNT(*) > 0 THEN '⚠️  WARNING: default_seller_id values will be lost!'
        ELSE '✅ No data to lose'
    END as warning
FROM users
WHERE default_seller_id IS NOT NULL;

-- =====================================================
-- STEP 4: Drop Columns
-- =====================================================

SELECT '>>> STEP 4/5: Dropping columns...' AS status;

-- Drop seller_id from invoices
ALTER TABLE invoices
DROP COLUMN IF EXISTS seller_id;

-- Drop seller_id from timesheets
ALTER TABLE timesheets
DROP COLUMN IF EXISTS seller_id;

-- Drop default_seller_id from users
ALTER TABLE users
DROP COLUMN IF EXISTS default_seller_id;

SELECT '✅ Columns dropped' AS status;

-- =====================================================
-- STEP 5: Drop sellers table
-- =====================================================

SELECT '>>> STEP 5/5: Dropping sellers table...' AS status;

DROP TABLE IF EXISTS sellers;

SELECT '✅ Sellers table dropped' AS status;

-- =====================================================
-- VERIFICATION
-- =====================================================

SELECT '>>> VERIFICATION: Checking schema...' AS status;

-- Verify invoices table schema (should NOT have seller_id)
SELECT
    'invoices' as table_name,
    GROUP_CONCAT(COLUMN_NAME ORDER BY ORDINAL_POSITION) as columns
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'robgro_test_invoices'
  AND TABLE_NAME = 'invoices'
  AND COLUMN_NAME LIKE '%seller%';

-- Verify timesheets table schema (should NOT have seller_id)
SELECT
    'timesheets' as table_name,
    GROUP_CONCAT(COLUMN_NAME ORDER BY ORDINAL_POSITION) as columns
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'robgro_test_invoices'
  AND TABLE_NAME = 'timesheets'
  AND COLUMN_NAME LIKE '%seller%';

-- Verify users table schema (should NOT have default_seller_id)
SELECT
    'users' as table_name,
    GROUP_CONCAT(COLUMN_NAME ORDER BY ORDINAL_POSITION) as columns
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'robgro_test_invoices'
  AND TABLE_NAME = 'users'
  AND COLUMN_NAME LIKE '%seller%';

-- Verify sellers table doesn't exist
SELECT
    CASE
        WHEN COUNT(*) = 0 THEN '✅ sellers table successfully removed'
        ELSE '❌ ERROR: sellers table still exists!'
    END as verification
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'robgro_test_invoices'
  AND TABLE_NAME = 'sellers';

-- =====================================================
-- FLYWAY CLEANUP (IMPORTANT!)
-- =====================================================

SELECT '>>> FLYWAY: Removing migration history...' AS status;

-- Delete Flyway records for seller migrations (V4-V8)
-- This prevents Flyway from thinking these migrations are still applied
DELETE FROM flyway_schema_history
WHERE version IN ('4', '5', '6', '7', '8')
  AND description LIKE '%seller%';

SELECT CONCAT('✅ Deleted ', ROW_COUNT(), ' Flyway migration records') AS status;

-- Show remaining Flyway history
SELECT
    version,
    description,
    type,
    installed_on,
    success
FROM flyway_schema_history
ORDER BY installed_rank DESC
LIMIT 10;

-- =====================================================
-- FINAL STATUS
-- =====================================================

SELECT '
╔═══════════════════════════════════════════════════════════╗
║           ROLLBACK READY FOR COMMIT                       ║
╠═══════════════════════════════════════════════════════════╣
║ Review the verification output above.                     ║
║                                                           ║
║ If everything looks correct:                             ║
║   - seller_id columns removed from tables                ║
║   - sellers table dropped                                ║
║   - No unexpected errors                                 ║
║                                                           ║
║ Then execute: COMMIT;                                    ║
║                                                           ║
║ If there are errors or unexpected results:               ║
║   Execute: ROLLBACK;                                     ║
╚═══════════════════════════════════════════════════════════╝
' AS NEXT_STEPS;

-- DO NOT AUTO-COMMIT!
-- Manually review all output above, then execute ONE of:
--
COMMIT;
-- ROLLBACK;    (if there are any issues)
