-- V29: Add versioning + generated active_key for single ACTIVE enforcement
-- Rationale: SaaS users need to test/change schemes retroactively
-- Solution: Version field (history) + generated column (enforce single ACTIVE per seller)

-- STEP 0: Clean up any duplicate ACTIVE schemes before adding unique constraint
-- Business rule: ONE ACTIVE per seller at a time.
-- Keep: highest effective_from, then highest id (version column doesn't exist yet at this point).
-- ROW_NUMBER() supported in MariaDB 10.2+ (prod is 10.11)
UPDATE invoice_numbering_schemes
SET status = 'ARCHIVED'
WHERE status = 'ACTIVE'
  AND id NOT IN (
    SELECT id FROM (
      SELECT id,
             ROW_NUMBER() OVER (
               PARTITION BY seller_id
               ORDER BY effective_from DESC, id DESC
             ) AS rn
      FROM invoice_numbering_schemes
      WHERE status = 'ACTIVE'
    ) ranked
    WHERE rn = 1
  );

-- STEP A: Add version column (all existing schemes get version=1)
-- IF NOT EXISTS: safe if DDL auto-commit partially applied in previous failed run
ALTER TABLE invoice_numbering_schemes
  ADD COLUMN IF NOT EXISTS version INT NOT NULL DEFAULT 1
  COMMENT 'Revision number for schemes with same effective_from'
  AFTER effective_from;

-- STEP A.5: Backfill version numbers per (seller_id, effective_from) group
-- Problem: STEP A gave all existing rows version=1; multiple rows with same (seller_id, effective_from)
-- would violate UNIQUE(seller_id, effective_from, version) in STEP C.
-- Fix: assign ROW_NUMBER() ordered by id as version — deterministic, no gaps.
-- Safe to re-run: if STEP C constraint exists, same ORDER BY id produces same values (no new violations).
UPDATE invoice_numbering_schemes s
JOIN (
  SELECT id,
         ROW_NUMBER() OVER (
           PARTITION BY seller_id, effective_from
           ORDER BY id
         ) AS new_version
  FROM invoice_numbering_schemes
) ranked ON s.id = ranked.id
SET s.version = ranked.new_version;

-- STEP B: Drop old UNIQUE(seller_id, effective_from)
-- IF EXISTS: safe if index was already dropped in previous failed run (DDL auto-commit)
DROP INDEX IF EXISTS unique_seller_effective_from ON invoice_numbering_schemes;

-- STEP C: Add new UNIQUE(seller_id, effective_from, version)
-- Allows multiple schemes with same effective_from, different versions
-- CREATE UNIQUE INDEX IF NOT EXISTS: safe if index already exists (DDL auto-commit)
CREATE UNIQUE INDEX IF NOT EXISTS unique_seller_effective_version
  ON invoice_numbering_schemes (seller_id, effective_from, version);

-- STEP D: Add generated column active_key
-- Semantics: ACTIVE → seller_id (as string), ARCHIVED → NULL
-- NULL values can repeat; non-NULL must be unique → enforces ONE ACTIVE per seller
-- IF NOT EXISTS: safe if DDL auto-commit partially applied in previous failed run
ALTER TABLE invoice_numbering_schemes
  ADD COLUMN IF NOT EXISTS active_key VARCHAR(64)
    GENERATED ALWAYS AS (
      CASE
        WHEN status = 'ACTIVE'
        THEN CAST(seller_id AS CHAR)
        ELSE NULL
      END
    ) STORED
    COMMENT 'Generated key for enforcing single ACTIVE scheme per seller';

-- STEP E: Add UNIQUE on active_key
-- Split from STEP D — ADD UNIQUE KEY in ALTER TABLE has no IF NOT EXISTS in MariaDB
-- CREATE UNIQUE INDEX IF NOT EXISTS: safe if index already exists (DDL auto-commit)
CREATE UNIQUE INDEX IF NOT EXISTS unique_active_scheme
  ON invoice_numbering_schemes (active_key);

-- RESULT:
-- ✅ Multiple versions of same effective_from: (seller=2, effective=2020-02-01, v1/v2/v3)
-- ✅ Only ONE ACTIVE per seller: enforced by unique_active_scheme(active_key)
-- ✅ Database-level protection: not just application validation
-- ✅ MariaDB compatible: generated columns + ROW_NUMBER() work on 10.11

-- Example data after migration:
-- (seller=2, effective=2020-02-01, version=1, ARCHIVED, active_key=NULL) ✅
-- (seller=2, effective=2020-02-01, version=3, ACTIVE,   active_key='2')  ✅
-- (seller=2, effective=2020-02-03, version=1, ARCHIVED, active_key=NULL) ✅
-- (seller=2, effective=2020-02-03, version=1, ACTIVE,   active_key='2')  ❌ FAILS — only one ACTIVE per seller!

-- ===================================================
-- PART B: Audit trail — which scheme generated each invoice number
-- ===================================================

-- NULL allowed: historical invoices pre-V29 don't have this data
-- New invoices will always have scheme_id set by the application
ALTER TABLE invoices
    ADD COLUMN IF NOT EXISTS scheme_id BIGINT NULL;

-- Supports audit queries: "which invoices used scheme X?"
CREATE INDEX IF NOT EXISTS idx_invoices_seller_scheme
    ON invoices (seller_id, scheme_id);