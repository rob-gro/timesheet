-- ============================================================================
-- Migration: Add hourly_rate column to timesheets table
-- Date: 2025-01-XX
-- Description: Historyzacja stawek godzinowych - każdy timesheet przechowuje
--              stawkę obowiązującą w dniu wykonania usługi
-- ============================================================================

-- KROK 1: Dodaj kolumnę (nullable na początku, bo istniejące rekordy są puste)
-- ----------------------------------------------------------------------------
ALTER TABLE timesheets
ADD COLUMN hourly_rate DOUBLE;

-- KROK 2: Wypełnij istniejące rekordy aktualną stawką z clients
-- ----------------------------------------------------------------------------
-- UWAGA: Dla starych rekordów nie mamy historii stawek, więc kopiujemy
--        aktualną stawkę klienta. To nie naprawi starych faktur, ale
--        zapobiegnie problemom w przyszłości.

UPDATE timesheets t
SET hourly_rate = (
    SELECT c.hourly_rate
    FROM clients c
    WHERE c.id = t.client_id
)
WHERE hourly_rate IS NULL;

-- KROK 3: Sprawdź czy wszystkie rekordy zostały wypełnione
-- ----------------------------------------------------------------------------
-- Powinno zwrócić 0 rekordów
SELECT COUNT(*) as missing_hourly_rate_count
FROM timesheets
WHERE hourly_rate IS NULL;

-- KROK 4: Ustaw NOT NULL constraint
-- ----------------------------------------------------------------------------
ALTER TABLE timesheets
MODIFY COLUMN hourly_rate DOUBLE NOT NULL;

-- ============================================================================
-- Weryfikacja
-- ============================================================================

-- Sprawdź strukturę tabeli
DESCRIBE timesheets;

-- Sprawdź przykładowe dane
SELECT
    t.id,
    t.service_date,
    t.duration,
    t.hourly_rate as timesheet_rate,
    c.hourly_rate as current_client_rate,
    CASE
        WHEN t.hourly_rate = c.hourly_rate THEN 'OK'
        ELSE 'DIFFERENT'
    END as rate_status
FROM timesheets t
JOIN clients c ON c.id = t.client_id
ORDER BY t.service_date DESC
LIMIT 10;

-- ============================================================================
-- Rollback (w razie potrzeby)
-- ============================================================================
-- UWAGA: Użyj tylko jeśli coś poszło nie tak!

-- ALTER TABLE timesheets DROP COLUMN hourly_rate;

-- ============================================================================
-- Notatki
-- ============================================================================
--
-- Po tej migracji:
-- 1. Każdy nowy timesheet będzie automatycznie zapisywał stawkę z klienta
--    (dzięki zmianom w TimesheetServiceImpl.createTimesheet())
--
-- 2. Faktury będą używać stawki z timesheet, a nie aktualnej stawki klienta
--    (dzięki zmianom w InvoiceCreationServiceImpl.createInvoiceItem())
--
-- 3. Stare timesheety mają teraz aktualną stawkę klienta - to ograniczenie
--    rozwiązania, bo nie mieliśmy historii stawek przed tą migracją
--
-- ============================================================================
