# Migracje Bazy Danych

## 📋 Jak Uruchomić Migrację

### ⚠️ PRZED MIGRACJĄ - Backup!

```bash
# MySQL
mysqldump -u [username] -p [database_name] > backup_before_001_$(date +%Y%m%d).sql

# Przykład:
mysqldump -u root -p timesheet_db > backup_before_001_20250115.sql
```

### ▶️ Uruchomienie Migracji

**Opcja 1: Przez MySQL CLI**
```bash
mysql -u [username] -p [database_name] < docs/database/migrations/001_add_hourly_rate_to_timesheets.sql
```

**Opcja 2: Przez MySQL Workbench / phpMyAdmin**
1. Otwórz plik `001_add_hourly_rate_to_timesheets.sql`
2. Skopiuj zawartość
3. Wklej i wykonaj w query editorze

**Opcja 3: Przez aplikację (Spring Boot)**
1. Dodaj Flyway do `pom.xml` (opcjonalnie na przyszłość):
```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-mysql</artifactId>
</dependency>
```
2. Przenieś skrypt do `src/main/resources/db/migration/V001__add_hourly_rate_to_timesheets.sql`
3. Uruchom aplikację - Flyway wykona migrację automatycznie

---

## ✅ Weryfikacja Po Migracji

### 1. Sprawdź strukturę tabeli
```sql
DESCRIBE timesheets;
-- Powinna być kolumna: hourly_rate DOUBLE NOT NULL
```

### 2. Sprawdź dane
```sql
-- Wszystkie rekordy powinny mieć wypełnione hourly_rate
SELECT COUNT(*) FROM timesheets WHERE hourly_rate IS NULL;
-- Wynik: 0

-- Porównaj stawki
SELECT
    t.id,
    t.service_date,
    c.client_name,
    t.hourly_rate as saved_rate,
    c.hourly_rate as current_rate
FROM timesheets t
JOIN clients c ON c.id = t.client_id
ORDER BY t.service_date DESC
LIMIT 10;
```

### 3. Uruchom aplikację
```bash
mvn spring-boot:run
# lub
./mvnw spring-boot:run
```

Sprawdź logi - nie powinno być błędów związanych z `hourly_rate`.

---

## 🧪 Testy Manualne

### Test 1: Tworzenie Timesheet
1. Zaloguj się do aplikacji
2. Dodaj nowy timesheet dla klienta
3. Sprawdź w bazie:
```sql
SELECT * FROM timesheets ORDER BY id DESC LIMIT 1;
-- hourly_rate powinien być wypełniony
```

### Test 2: Zmiana Stawki Klienta
1. Klient ma stawkę 50 zł/h
2. Dodaj timesheet (2h)
3. Zmień stawkę klienta na 60 zł/h
4. Dodaj kolejny timesheet (3h)
5. Sprawdź w bazie:
```sql
SELECT
    t.id,
    t.service_date,
    t.duration,
    t.hourly_rate,
    c.hourly_rate as client_current_rate
FROM timesheets t
JOIN clients c ON c.id = t.client_id
WHERE c.id = [client_id]
ORDER BY t.service_date DESC;

-- Pierwszy timesheet: hourly_rate = 50
-- Drugi timesheet: hourly_rate = 60
```

### Test 3: Generowanie Faktury
1. Wygeneruj fakturę dla klienta z powyższego testu
2. Sprawdź kwoty:
   - Timesheet 2h @ 50 zł/h = 100 zł
   - Timesheet 3h @ 60 zł/h = 180 zł
   - **Total = 280 zł** (nie 300 zł!)
3. Sprawdź w bazie:
```sql
SELECT
    i.invoice_number,
    ii.service_date,
    ii.duration,
    ii.amount,
    t.hourly_rate
FROM invoices i
JOIN invoice_items ii ON ii.invoice_id = i.id
JOIN timesheets t ON t.id = ii.timesheet_id
WHERE i.id = [invoice_id];
```

### Test 4: Kopia Faktury
1. Zmień stawkę klienta na 70 zł/h
2. Otwórz starą fakturę / wygeneruj kopię
3. Kwoty **nie powinny się zmienić** (nadal 280 zł)

---

## 🔄 Rollback (awaryjny)

Jeśli coś poszło nie tak:

```sql
-- TYLKO W RAZIE POTRZEBY!
ALTER TABLE timesheets DROP COLUMN hourly_rate;

-- Przywróć backup
mysql -u [username] -p [database_name] < backup_before_001_YYYYMMDD.sql
```

---

## 📝 Historia Migracji

| # | Data | Plik | Opis |
|---|------|------|------|
| 001 | 2025-01-XX | `001_add_hourly_rate_to_timesheets.sql` | Dodanie kolumny hourly_rate do historyzacji stawek |

---

## 📚 Powiązane Dokumenty

- `.claude/commands/hourly-rate-historization-plan.md` - Pełny plan implementacji
- Zmiany w kodzie:
  - `Timesheet.java` - dodano pole `hourlyRate`
  - `TimesheetDtoMapper.java` - używa `timesheet.getHourlyRate()`
  - `TimesheetServiceImpl.java` - automatyczne zapisywanie stawki
  - `InvoiceCreationServiceImpl.java` - używa stawki z timesheet zamiast client
