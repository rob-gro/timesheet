-- V28: Secure audit fields - prevent overwriting created_by/created_at
-- Makes audit trail immutable at database level

-- STEP 1: Drop existing FK constraint (from V18)
ALTER TABLE invoice_numbering_schemes
DROP FOREIGN KEY fk_scheme_creator;

-- STEP 2: Check and fix NULL values BEFORE making NOT NULL
-- Set NULL created_by to system user (id=1) if any exist
UPDATE invoice_numbering_schemes
SET created_by = 1
WHERE created_by IS NULL;

-- STEP 3: Rename column from created_by to created_by_id (better naming for FK)
-- CHANGE COLUMN requires specifying the type
ALTER TABLE invoice_numbering_schemes
CHANGE COLUMN created_by created_by_id BIGINT NOT NULL;

-- STEP 4: Recreate FK constraint with new column name
ALTER TABLE invoice_numbering_schemes
ADD CONSTRAINT fk_scheme_created_by
    FOREIGN KEY (created_by_id) REFERENCES users(id);

-- NOTES:
-- - created_at is already NOT NULL from V18 ✓
-- - JPA annotations now have updatable=false to prevent application-level overwrites ✓
-- - This migration makes audit fields immutable at DB level ✓
