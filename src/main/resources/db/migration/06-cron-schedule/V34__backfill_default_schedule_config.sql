-- Backfill default schedule config for existing active sellers.
-- Inactive sellers will get a config created on first UI load (lazy creation in service).
-- last_run_period pre-set to previous month with SUCCESS to prevent spurious run on first deploy.
INSERT INTO seller_schedule_config (seller_id, scheduled_day, scheduled_hour, enabled, last_run_period, last_run_status, created_at)
SELECT id, 1, 12, 1, DATE_FORMAT(DATE_SUB(NOW(), INTERVAL 1 MONTH), '%Y-%m'), 'SUCCESS', NOW()
FROM sellers
WHERE active = 1;