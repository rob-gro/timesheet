# Plan Manualnego Testowania: Historyzacja Stawek Godzinowych

## ✅ Checklist Przygotowania

- [ ] Backup bazy danych wykonany
- [ ] Migracja SQL uruchomiona
- [ ] Aplikacja działa
- [ ] Dostęp do bazy danych (MySQL Workbench / phpMyAdmin / CLI)

---

## 📋 KROK 1: Przygotowanie - Backup Bazy

**Cel:** Zabezpieczenie danych przed testami

```bash
# Backup bazy danych
mysqldump -u root -p timesheet_db > backup_before_test_$(date +%Y%m%d_%H%M%S).sql

# Przykład:
mysqldump -u root -p robgro_test_invoices > backup_before_test_20250105_220000.sql
```

**Oczekiwany rezultat:**
- ✅ Plik backup został utworzony
- ✅ Rozmiar pliku > 0 KB

---

## 📋 KROK 2: Uruchomienie Migracji

**Cel:** Dodanie kolumny `hourly_rate` do tabeli `timesheets`

```bash
# Z katalogu głównego projektu
mysql -u root -p timesheet_db < docs/database/migrations/001_add_hourly_rate_to_timesheets.sql

# LUB dla MariaDB na alwaysdata:
mysql -u robgro_aga -p robgro_test_invoices < docs/database/migrations/001_add_hourly_rate_to_timesheets.sql
```

**Oczekiwany rezultat:**
- ✅ Brak błędów w terminalu
- ✅ Komunikat o sukcesie

---

## 📋 KROK 3: Weryfikacja Struktury Tabeli

**Cel:** Sprawdzenie czy kolumna została dodana

```sql
-- Sprawdź strukturę tabeli
DESCRIBE timesheets;
```

**Oczekiwany rezultat:**
```
Field         | Type   | Null | Key | Default | Extra
--------------|--------|------|-----|---------|-------
id            | bigint | NO   | PRI | NULL    | auto_increment
client_id     | bigint | YES  | MUL | NULL    |
service_date  | date   | NO   |     | NULL    |
duration      | double | YES  |     | NULL    |
hourly_rate   | double | NO   |     | NULL    |    ← NOWA KOLUMNA!
is_invoice    | bit(1) | YES  |     | NULL    |
invoice_id    | bigint | YES  | MUL | NULL    |
invoice_number| varchar| YES  |     | NULL    |
payment_date  | date   | YES  |     | NULL    |
```

**Sprawdź:**
- ✅ Kolumna `hourly_rate` istnieje
- ✅ Typ: `DOUBLE`
- ✅ NOT NULL: `NO`

---

## 📋 KROK 4: Sprawdzenie Istniejących Danych

**Cel:** Sprawdzenie czy stare timesheety zostały wypełnione

```sql
-- Sprawdź czy wszystkie timesheety mają hourly_rate
SELECT COUNT(*) as total_timesheets,
       COUNT(hourly_rate) as with_hourly_rate,
       COUNT(*) - COUNT(hourly_rate) as missing_hourly_rate
FROM timesheets;
```

**Oczekiwany rezultat:**
```
total_timesheets | with_hourly_rate | missing_hourly_rate
-----------------|------------------|--------------------
100              | 100              | 0
```

- ✅ `missing_hourly_rate = 0` (wszystkie wypełnione)

```sql
-- Sprawdź przykładowe dane
SELECT
    t.id,
    c.client_name,
    t.service_date,
    t.duration,
    t.hourly_rate as timesheet_rate,
    c.hourly_rate as current_client_rate,
    CASE
        WHEN t.hourly_rate = c.hourly_rate THEN 'SAME'
        ELSE 'DIFFERENT'
    END as status
FROM timesheets t
JOIN clients c ON c.id = t.client_id
ORDER BY t.service_date DESC
LIMIT 10;
```

**Oczekiwany rezultat:**
- ✅ Wszystkie `timesheet_rate` są wypełnione
- ✅ Większość ma status `SAME` (bo dopiero co skopiowaliśmy z clients)

---

## 📋 KROK 5: Uruchomienie Aplikacji

**Cel:** Sprawdzenie czy aplikacja startuje poprawnie

```bash
# Uruchom aplikację
mvn spring-boot:run

# LUB jeśli używasz JAR:
java -jar target/Timesheet-0.0.1-SNAPSHOT.jar
```

**Oczekiwany rezultat:**
- ✅ Aplikacja startuje bez błędów
- ✅ Brak błędów związanych z `hourly_rate` w logach
- ✅ Możesz otworzyć aplikację w przeglądarce

---

## 📋 TEST 1: Tworzenie Timesheet - Zapis Stawki

**Cel:** Sprawdzenie czy nowy timesheet zapisuje aktualną stawkę klienta

### Krok 1.1: Wybierz klienta i sprawdź jego stawkę

```sql
-- Znajdź klienta do testu
SELECT id, client_name, hourly_rate
FROM clients
WHERE active = 1
LIMIT 5;

-- Przykład: wybierz klienta id=1, stawka=50
```

**Zapisz:**
- 📝 Client ID: ______
- 📝 Client Name: ______
- 📝 Aktualna stawka: ______

### Krok 1.2: Dodaj timesheet przez aplikację

1. Otwórz aplikację w przeglądarce
2. Przejdź do `Timesheets` → `Add New`
3. Wypełnij formularz:
   - **Client:** [wybrany klient]
   - **Service Date:** dzisiejsza data
   - **Duration:** 2.0
4. Zapisz

### Krok 1.3: Sprawdź w bazie danych

```sql
-- Znajdź ostatnio dodany timesheet
SELECT
    t.id,
    c.client_name,
    t.service_date,
    t.duration,
    t.hourly_rate as saved_rate,
    c.hourly_rate as current_client_rate
FROM timesheets t
JOIN clients c ON c.id = t.client_id
ORDER BY t.id DESC
LIMIT 1;
```

**Oczekiwany rezultat:**
- ✅ `saved_rate` = aktualna stawka klienta (np. 50.0)
- ✅ `saved_rate` = `current_client_rate`

**Zapisz ID timesheet do dalszych testów:**
- 📝 Timesheet ID (przed zmianą): ______

---

## 📋 TEST 2: Zmiana Stawki Klienta

**Cel:** Zmiana stawki klienta w bazie danych

```sql
-- Zmień stawkę klienta (np. z 50 na 60)
UPDATE clients
SET hourly_rate = 60.0
WHERE id = [CLIENT_ID];

-- Sprawdź czy się zmieniło
SELECT id, client_name, hourly_rate
FROM clients
WHERE id = [CLIENT_ID];
```

**Oczekiwany rezultat:**
- ✅ Stawka klienta = 60.0

### Sprawdź stary timesheet

```sql
-- WAŻNE: Stary timesheet NIE POWINIEN się zmienić!
SELECT
    t.id,
    t.service_date,
    t.hourly_rate as old_timesheet_rate,
    c.hourly_rate as new_client_rate
FROM timesheets t
JOIN clients c ON c.id = t.client_id
WHERE t.id = [TIMESHEET_ID_Z_TESTU_1];
```

**Oczekiwany rezultat:**
- ✅ `old_timesheet_rate` = 50.0 (stara stawka, NIE ZMIENIŁA SIĘ!)
- ✅ `new_client_rate` = 60.0 (nowa stawka klienta)

---

## 📋 TEST 3: Nowy Timesheet Po Zmianie Stawki

**Cel:** Sprawdzenie czy nowy timesheet zapisze nową stawkę

### Krok 3.1: Dodaj kolejny timesheet

1. W aplikacji: `Timesheets` → `Add New`
2. Wypełnij:
   - **Client:** ten sam klient
   - **Service Date:** jutrzejsza data
   - **Duration:** 3.0
3. Zapisz

### Krok 3.2: Sprawdź w bazie

```sql
-- Znajdź oba timesheety dla tego klienta
SELECT
    t.id,
    t.service_date,
    t.duration,
    t.hourly_rate,
    c.hourly_rate as current_client_rate
FROM timesheets t
JOIN clients c ON c.id = t.client_id
WHERE c.id = [CLIENT_ID]
ORDER BY t.service_date DESC
LIMIT 2;
```

**Oczekiwany rezultat:**
```
id  | service_date | duration | hourly_rate | current_client_rate
----|--------------|----------|-------------|--------------------
123 | 2025-01-06   | 3.0      | 60.0        | 60.0    ← NOWY (po zmianie)
122 | 2025-01-05   | 2.0      | 50.0        | 60.0    ← STARY (przed zmianą)
```

- ✅ Stary timesheet: `hourly_rate = 50.0`
- ✅ Nowy timesheet: `hourly_rate = 60.0`
- ✅ Oba mają różne stawki!

**Zapisz ID nowego timesheet:**
- 📝 Timesheet ID (po zmianie): ______

---

## 📋 TEST 4: Generowanie Faktury

**Cel:** Sprawdzenie czy faktura używa stawek z timesheets, nie z clients

### Krok 4.1: Wygeneruj fakturę

1. W aplikacji: `Invoices` → `Create New`
2. Wybierz:
   - **Client:** [ten sam klient]
   - **Timesheets:** zaznacz OBA timesheety (2h @ 50 zł + 3h @ 60 zł)
   - **Issue Date:** dzisiejsza data
3. Kliknij `Generate Invoice`

### Krok 4.2: Sprawdź w aplikacji

**Oczekiwane kwoty na fakturze:**
```
Pozycja 1: 2025-01-05, 2.0h  →  100.00 zł  (2 × 50)
Pozycja 2: 2025-01-06, 3.0h  →  180.00 zł  (3 × 60)
----------------------------------------------------
TOTAL:                         280.00 zł

NIE 300 zł! (5h × 60 byłoby błędem)
```

- ✅ Pozycja 1: 100.00 zł
- ✅ Pozycja 2: 180.00 zł
- ✅ Total: 280.00 zł

### Krok 4.3: Sprawdź w bazie danych

```sql
-- Znajdź ostatnią fakturę
SELECT * FROM invoices
ORDER BY id DESC
LIMIT 1;

-- Pobierz invoice_id i sprawdź pozycje:
SELECT
    ii.id,
    ii.service_date,
    ii.duration,
    ii.amount,
    t.hourly_rate as rate_used,
    ii.timesheet_id
FROM invoice_items ii
JOIN timesheets t ON t.id = ii.timesheet_id
WHERE ii.invoice_id = [INVOICE_ID]
ORDER BY ii.service_date;
```

**Oczekiwany rezultat:**
```
service_date | duration | amount  | rate_used | timesheet_id
-------------|----------|---------|-----------|-------------
2025-01-05   | 2.0      | 100.00  | 50.0      | 122
2025-01-06   | 3.0      | 180.00  | 60.0      | 123
```

- ✅ Każda pozycja ma poprawną kwotę (duration × rate_used)
- ✅ `rate_used` pochodzi z `timesheets.hourly_rate`, nie z `clients.hourly_rate`

**Zapisz ID faktury:**
- 📝 Invoice ID: ______
- 📝 Invoice Number: ______

---

## 📋 TEST 5: Kopia Faktury Po Kolejnej Zmianie Stawki

**Cel:** Sprawdzenie czy stara faktura nie zmieni kwot po zmianie stawki klienta

### Krok 5.1: Zmień stawkę klienta ponownie

```sql
-- Zmień stawkę na 70 zł
UPDATE clients
SET hourly_rate = 70.0
WHERE id = [CLIENT_ID];

-- Sprawdź
SELECT id, client_name, hourly_rate
FROM clients
WHERE id = [CLIENT_ID];
```

**Oczekiwany rezultat:**
- ✅ Nowa stawka klienta = 70.0

### Krok 5.2: Otwórz starą fakturę

1. W aplikacji: `Invoices` → `View All`
2. Znajdź fakturę z Testu 4
3. Otwórz / wyświetl / wygeneruj PDF

### Krok 5.3: Sprawdź kwoty

**Oczekiwany rezultat:**
```
Pozycja 1: 2025-01-05, 2.0h  →  100.00 zł  (NIE ZMIENIŁO SIĘ!)
Pozycja 2: 2025-01-06, 3.0h  →  180.00 zł  (NIE ZMIENIŁO SIĘ!)
----------------------------------------------------
TOTAL:                         280.00 zł  (NIE 350 zł!)
```

- ✅ Kwoty NIE zmieniły się
- ✅ Total nadal 280.00 zł (nie 350 zł = 5h × 70)

### Krok 5.4: Weryfikacja w bazie

```sql
-- Sprawdź czy kwoty w invoice_items się nie zmieniły
SELECT
    ii.id,
    ii.service_date,
    ii.amount as saved_amount,
    t.hourly_rate as timesheet_rate,
    c.hourly_rate as current_client_rate,
    ii.amount as should_be,
    (t.duration * c.hourly_rate) as would_be_if_wrong
FROM invoice_items ii
JOIN timesheets t ON t.id = ii.timesheet_id
JOIN clients c ON c.id = t.client_id
WHERE ii.invoice_id = [INVOICE_ID];
```

**Oczekiwany rezultat:**
```
service_date | saved_amount | timesheet_rate | current_client_rate | should_be | would_be_if_wrong
-------------|--------------|----------------|---------------------|-----------|------------------
2025-01-05   | 100.00       | 50.0           | 70.0                | 100.00    | 140.00
2025-01-06   | 180.00       | 60.0           | 70.0                | 180.00    | 210.00
```

- ✅ `saved_amount` = `should_be` (poprawne kwoty zachowane)
- ✅ `saved_amount` ≠ `would_be_if_wrong` (uniknięto błędu)

---

## 📋 TEST 6: Edycja Timesheet - Zmiana Klienta

**Cel:** Sprawdzenie czy przy zmianie klienta stawka się aktualizuje

### Krok 6.1: Utwórz nowego klienta z inną stawką

```sql
-- Dodaj testowego klienta
INSERT INTO clients (client_name, hourly_rate, house_number, street_name, city, post_code, email, active)
VALUES ('Test Client 2', 80.0, 123, 'Test Street', 'Warsaw', '00-001', 'test2@example.com', 1);

-- Pobierz ID nowego klienta
SELECT id, client_name, hourly_rate
FROM clients
WHERE client_name = 'Test Client 2';
```

**Zapisz:**
- 📝 Nowy Client ID: ______
- 📝 Stawka: 80.0

### Krok 6.2: Edytuj stary timesheet - zmień klienta

1. W aplikacji: `Timesheets` → znajdź timesheet z Testu 1
2. Kliknij `Edit`
3. Zmień:
   - **Client:** Test Client 2 (nowy klient)
   - Pozostaw duration i datę bez zmian
4. Zapisz

### Krok 6.3: Sprawdź w bazie

```sql
-- Sprawdź zaktualizowany timesheet
SELECT
    t.id,
    c.client_name,
    t.service_date,
    t.duration,
    t.hourly_rate as timesheet_rate,
    c.hourly_rate as client_rate
FROM timesheets t
JOIN clients c ON c.id = t.client_id
WHERE t.id = [TIMESHEET_ID_Z_TESTU_1];
```

**Oczekiwany rezultat:**
- ✅ `client_name` = 'Test Client 2'
- ✅ `timesheet_rate` = 80.0 (zaktualizowana do nowej stawki klienta)
- ✅ `client_rate` = 80.0

---

## 📊 Podsumowanie Testów

### Checklist Rezultatów

- [ ] **Test 1:** Nowy timesheet zapisuje aktualną stawkę klienta ✅
- [ ] **Test 2:** Zmiana stawki klienta NIE wpływa na stare timesheety ✅
- [ ] **Test 3:** Nowy timesheet po zmianie stawki zapisuje nową stawkę ✅
- [ ] **Test 4:** Faktura używa stawek z timesheets (50 + 60 = 280 zł, nie 300) ✅
- [ ] **Test 5:** Stara faktura NIE zmienia kwot po kolejnej zmianie stawki ✅
- [ ] **Test 6:** Edycja timesheet - zmiana klienta aktualizuje stawkę ✅

---

## 🐛 Co Robić Gdy Test Nie Przechodzi?

### Problem 1: Nowy timesheet NIE ma wypełnionego hourly_rate

**Możliwe przyczyny:**
- Kod nie został wdrożony / aplikacja używa starej wersji
- Baza danych nie została zmigrowana

**Rozwiązanie:**
```bash
# Przebuduj aplikację
mvn clean install

# Uruchom ponownie
mvn spring-boot:run
```

### Problem 2: Faktura pokazuje błędne kwoty (używa aktualnej stawki)

**Możliwa przyczyna:**
- Kod `InvoiceCreationServiceImpl` nie został zmieniony lub nie działa

**Sprawdź:**
```java
// InvoiceCreationServiceImpl.java linia ~94
item.setAmount(calculateAmount(timesheet.duration(), timesheet.hourlyRate()));
// Powinno być timesheet.hourlyRate(), NIE invoice.getClient().getHourlyRate()
```

### Problem 3: Kolumna hourly_rate nie istnieje

**Rozwiązanie:**
```bash
# Uruchom migrację ponownie
mysql -u root -p timesheet_db < docs/database/migrations/001_add_hourly_rate_to_timesheets.sql
```

---

## 📝 Notatki

**Data testu:** ______________________

**Tester:** _________________________

**Uwagi:**
_________________________________________
_________________________________________
_________________________________________

**Znalezione problemy:**
_________________________________________
_________________________________________
_________________________________________

---

## ✅ GOTOWE!

Jeśli wszystkie testy przeszły - **implementacja działa poprawnie!** 🎉

Problemy które zostały rozwiązane:
1. ✅ Faktury używają historycznej stawki
2. ✅ Zmiana stawki nie psuje starych faktur
3. ✅ Każdy timesheet ma swoją "zamrożoną" stawkę
4. ✅ Stawka w połowie miesiąca nie powoduje błędnych obliczeń
