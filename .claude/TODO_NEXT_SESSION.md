# 🚨 TODO - Następna Sesja

## 1. ⚠️ PILNE - Fix Dark Mode w Email do Klienta

**Problem:**
- Na telefonie z dark mode: litery w bannerach są ciemne (nieczytelne)
- Na komputerze: białe litery (OK)

**Rozwiązanie:**
Dodać `!important` do kolorów tekstu w email template żeby wymuszać białe litery:

**Plik:** `src/main/java/dev/robgro/timesheet/invoice/EmailMessageService.java`

**Co zrobić:**
```css
.header-bar {
    background: linear-gradient(to right, #2c3e50, #3498db) !important;
    padding: 30px 40px;
    color: white !important;  /* <-- DODAĆ !important */
}
.company-logo {
    color: white !important;  /* <-- DODAĆ !important */
}
.company-tagline {
    color: #e3f2fd !important;  /* <-- DODAĆ !important */
}
.invoice-label {
    color: #ffffff !important;  /* <-- DODAĆ !important */
}
.invoice-num {
    color: #ffffff !important;  /* <-- DODAĆ !important */
}
```

**Już dodane meta tagi (OK):**
```html
<meta name="color-scheme" content="light">
<meta name="supported-color-schemes" content="light">
```

---

## 2. ✅ CO JEST JUŻ ZROBIONE

### Email Templates:
- ✅ **Admin emails** (piękne, gradient, profesjonalne):
  - Summary notification
  - Error notification
  - Empty clients warning
  - Plik: `AdminNotificationServiceImpl.java`

- ✅ **Client email** (Corporate + Feature Tiles):
  - Gradient header z "AGA CLEANING"
  - 3 kafelki: Month, Services (12 visits), Amount (£450.00)
  - PDF access section z animowaną strzałką
  - Plik: `EmailMessageService.java`
  - ⚠️ BRAKUJE: `!important` na kolory (fix dark mode)

### Refactoring:
- ✅ DTO Pattern - `InvoiceEmailRequest` zamiast 9 parametrów
- ✅ Wszystkie testy zaktualizowane

### Scheduler:
- ✅ Implementacja schedulera (CRON)
- ✅ Wszystkie komponenty: TaskService, Scheduler, Properties, DTOs
- ✅ Test endpoint: `InvoicingSchedulerTestController.java`
- ✅ Security exception dla `/api/v1/test/**`

### Konfiguracja:
- ✅ Rozdzielone DB: `robgro_test_invoices` vs `robgro_aga_invoices`
- ✅ Rozdzielone FTP: `/files/invoices_test` vs `/files/invoices`
- ✅ Scheduler w `application.properties`:
  ```properties
  scheduling.invoicing.enabled=true
  scheduling.invoicing.cron=0 * * * * ?  # TESTOWE - co minutę
  ```

---

## 3. ⚠️ PRZED DEPLOYEM NA PROD - USUNĄĆ

**Plik checklist:** `.claude/BEFORE_PROD_COMMIT.md`

### Security (USUNĄĆ):
1. `SecurityConfig.java:84` - linia:
   ```java
   .requestMatchers("/api/v1/test/**").permitAll()
   ```

2. `InvoicingSchedulerTestController.java` - CAŁY PLIK

### Configuration (ZMIENIĆ):
3. **Database** - przestaw na PROD:
   ```properties
   spring.datasource.url=jdbc:mariadb://...robgro_aga_invoices...
   ```

4. **FTP** - przestaw na PROD:
   ```properties
   ftp.invoices.directory=/files/invoices
   ```

5. **CRON** - ustaw właściwy czas:
   ```properties
   scheduling.invoicing.cron=0 0 15 3 * ?  # 3. dzień miesiąca, 15:00
   ```

---

## 4. 🧪 TESTY

### Lokalne:
- ⚠️ Certyfikat FTP ważny OD: 12 listopada 2025
- Data systemowa musi być >= 12.11.2025 żeby FTP/Email działało
- Przykład: ustaw datę na 3 grudnia 2025, dodaj timesheety z listopada 2025

### Manualne:
- Wysłać fakturę do klienta
- Sprawdzić email na telefonie (dark mode) - **TO TERAZ NIE DZIAŁA!**
- Sprawdzić email na komputerze

---

## 5. 📁 WAŻNE PLIKI

### Email Templates:
- `src/main/java/dev/robgro/timesheet/invoice/EmailMessageService.java` ⚠️ FIX DARK MODE
- `src/main/java/dev/robgro/timesheet/invoice/InvoiceEmailRequest.java` (DTO)
- `src/main/java/dev/robgro/timesheet/scheduler/AdminNotificationServiceImpl.java` ✅

### Scheduler:
- `src/main/java/dev/robgro/timesheet/scheduler/InvoicingScheduler.java`
- `src/main/java/dev/robgro/timesheet/scheduler/InvoicingTaskServiceImpl.java`
- `src/main/java/dev/robgro/timesheet/scheduler/InvoicingSchedulerTestController.java` ⚠️ USUNĄĆ PRZED PROD

### Config:
- `src/main/resources/application.properties` ⚠️ ZMIENIĆ NA PROD

### Preview:
- `client_email_FINAL.html` - podgląd emaila do klienta
- `email_preview.html` - podgląd emaili do admina

---

## 6. 🎯 KOLEJNOŚĆ DZIAŁAŃ NA NASTĘPNĄ SESJĘ

1. **FIX DARK MODE** (5 min):
   - Dodać `!important` do wszystkich kolorów tekstu w email template
   - Przetestować na telefonie

2. **Build & Test** (5 min):
   ```bash
   mvn clean package
   mvn test
   ```

3. **Manual Test** (10 min):
   - Wysłać fakturę
   - Sprawdzić email na telefonie (dark mode)
   - Sprawdzić email na komputerze

4. **Commit** (jeśli testy OK):
   ```bash
   git add .
   git commit -m "feat: beautiful email templates + scheduler implementation"
   git push
   ```

5. **Produkcja** (jeśli user potwierdzi):
   - Przeczytać `.claude/BEFORE_PROD_COMMIT.md`
   - Usunąć test endpoint + security exception
   - Przestawić DB, FTP, CRON na PROD
   - Deploy

---

## 7. 💡 NOTATKI

- User lubi design #2 (Corporate) + kafelki z #4
- Bez gradientu w stopce
- Bez "Payment Terms"
- 3 kafelki zamiast 4
- Month, Services (visits), Amount
- Tytuł emaila NIE zmieniać: "Invoice INV-2025-001 from Aga"

---

**Status:** Email templates piękne, scheduler działa, BRAKUJE tylko fix dark mode! 🚀