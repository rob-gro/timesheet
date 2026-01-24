-- =====================================================
-- VERIFY EMAIL TRACKING SCHEMA ON PRODUCTION
-- Database: robgro_aga_invoices
-- =====================================================

-- 1. Check if email_tracking table exists
# SELECT '1. Email tracking table:' AS step;
# SELECT
#     TABLE_NAME,
#     ENGINE,
#     TABLE_COLLATION
# FROM
#     information_schema.TABLES
# WHERE
#     TABLE_SCHEMA = 'robgro_aga_invoices'
#     AND TABLE_NAME = 'email_tracking';

# -- 2. Check email_tracking table structure
# SELECT '2. Email tracking columns:' AS step;
# DESCRIBE email_tracking;
#
# -- 3. Check if new columns exist in invoices table
# SELECT '3. New invoice columns:' AS step;
# SELECT
#     COLUMN_NAME,
#     DATA_TYPE,
#     IS_NULLABLE,
#     COLUMN_DEFAULT
# FROM information_schema.COLUMNS
# WHERE TABLE_SCHEMA = 'robgro_aga_invoices'
#   AND TABLE_NAME = 'invoices'
#   AND COLUMN_NAME IN ('email_tracking_token', 'email_opened_at', 'email_open_count', 'last_email_opened_at')
# ORDER BY ORDINAL_POSITION;
#
# -- 4. Count tracking records
# SELECT '4. Tracking records count:' AS step;
# SELECT COUNT(*) as total_tracking_records FROM email_tracking;
#
# -- 5. Check indexes
# SELECT '5. Indexes on email_tracking:' AS step;
# SHOW INDEX FROM email_tracking;
#
# -- 6. Check foreign key
# SELECT '6. Foreign key constraint:' AS step;
# SELECT
#     CONSTRAINT_NAME,
#     TABLE_NAME,
#     REFERENCED_TABLE_NAME
# FROM information_schema.KEY_COLUMN_USAGE
# WHERE TABLE_SCHEMA = 'robgro_aga_invoices'
#   AND CONSTRAINT_NAME = 'fk_email_tracking_invoice';
#
# -- 7. Final summary
# SELECT '✅ VERIFICATION COMPLETE' AS status;
# SELECT 'If all steps above show data, the schema is correctly installed!' AS result;
