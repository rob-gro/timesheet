# 🧪 INSTRUKCJA TESTOWANIA EMAIL TRACKING LOKALNIE

## ⚠️ WAŻNE OGRANICZENIA TESTÓW LOKALNYCH

Tracking z `app.base-url=http://localhost:8080` zadziała **TYLKO** gdy:
- ✅ Otworzysz email na **TYM SAMYM komputerze** gdzie działa aplikacja
- ✅ Użyjesz **desktopowego klienta email** (NIE webowy Gmail/Outlook)
- ✅ Aplikacja **działa** podczas otwierania emaila

**NIE ZADZIAŁA jeśli:**
- ❌ Otworzysz email w **przeglądarce** (Gmail web, Outlook web)
- ❌ Otworzysz email na **telefonie/tablecie**
- ❌ Otworzysz email na **innym komputerze**

---

## 📋 PROCEDURA TESTOWANIA

### **1. Wykonaj SQL migration (JEDNORAZOWO)**

```sql
-- W kliencie SQL połączonym z robgro_test_invoices
-- Skopiuj i wykonaj cały plik: EXECUTE_NOW_test_database.sql
-- Sprawdź output, potem:
COMMIT;
```

### **2. Uruchom aplikację**

```bash
cd C:\_newJ\Timesheet
mvn spring-boot:run
```

**Poczekaj aż zobaczysz:**
```
Started TimesheetApplication in X.XXX seconds
```

### **3. Zweryfikuj że tracking endpoint działa**

W **nowym terminalu**:
```bash
curl http://localhost:8080/api/track/health
```

**Powinno zwrócić:**
```json
{"status":"OK","message":"Email tracking service is operational","timestamp":...}
```

### **4. Wyślij testową fakturę**

1. Otwórz aplikację w przeglądarce: `http://localhost:8080`
2. Zaloguj się
3. Utwórz timesheety dla klienta
4. Wygeneruj i wyślij fakturę

### **5. Sprawdź logi aplikacji**

Szukaj w logach Spring Boot:
```
Created email tracking token: <UUID> for invoice: <ID>
```

Jeśli zobaczysz ten log, token został utworzony ✅

### **6. Otwórz email NA TYM SAMYM KOMPUTERZE**

**WAŻNE:** Musisz użyć desktopowego klienta email:

**Windows:**
- Thunderbird (polecam - darmowy)
- Outlook desktop (nie webowy!)

**Mac:**
- Apple Mail
- Thunderbird

**Linux:**
- Thunderbird
- Evolution

**NIE UŻYWAJ:**
- ❌ Gmail w przeglądarce (gmail.com)
- ❌ Outlook web (outlook.com)
- ❌ Telefonu
- ❌ Innego komputera

### **7. Sprawdź czy tracking zadziałał**

**A) W logach aplikacji szukaj:**
```
✅ FIRST email open recorded for invoice X (INV-XXX), client: YYY, device: Desktop, client: Thunderbird
```

**B) Sprawdź bazę danych:**
```sql
SELECT * FROM email_tracking ORDER BY created_at DESC LIMIT 1;
-- Sprawdź czy opened_at ma wartość (nie NULL)

SELECT
    id,
    invoice_number,
    email_tracking_token,
    email_opened_at,
    email_open_count
FROM invoices
WHERE email_tracking_token IS NOT NULL
ORDER BY id DESC LIMIT 1;
```

**C) Email notification:**
⚠️ **UWAGA:** Notification email NA LOCALHOST może nie działać jeśli:
- Gmail SMTP blokuje wysyłkę z localhost
- Firewall blokuje outbound SMTP

**Jeśli NIE dostaniesz email notification, to normalne na localhost!**
Sprawdź logi aplikacji czy tracking został zarejestrowany (punkt A).

---

## 🐛 TROUBLESHOOTING

### Problem: "Connection refused" na endpoint
**Rozwiązanie:** Aplikacja nie działa. Uruchom `mvn spring-boot:run`

### Problem: Email nie ma obrazów
**Rozwiązanie:**
1. Sprawdź czy klient email nie blokuje obrazów (często domyślnie blokowane)
2. W kliencie email: Settings → Zezwól na obrazy / "Always show images"

### Problem: Token nie jest tworzony
**Rozwiązanie:**
1. Sprawdź czy wykonałeś SQL migration: `SELECT * FROM email_tracking LIMIT 1;`
2. Sprawdź logi: `Created email tracking token`
3. Sprawdź config: `app.email-tracking.enabled=true`

### Problem: Tracking nie działa mimo wszystko
**Rozwiązanie:**
1. Sprawdź czy aplikacja DZIAŁA podczas otwierania emaila
2. Sprawdź czy otwierasz email NA TYM SAMYM komputerze
3. Sprawdź czy używasz DESKTOPOWEGO klienta (nie web)
4. Zobacz "View Source" emaila i znajdź: `<img src="http://localhost:8080/api/track/...png"`

---

## ✅ SUKCES - Jak poznać że działa?

**KROK 1 - Token utworzony:**
```
[LOG] Created email tracking token: abc-123-def for invoice: 42
```

**KROK 2 - Email otwarty:**
```
[LOG] ✅ FIRST email open recorded for invoice 42 (001-12-2024)
```

**KROK 3 - Dane w bazie:**
```sql
SELECT opened_at, device_type, email_client FROM email_tracking WHERE id = 1;
-- opened_at:    2024-12-15 14:23:45
-- device_type:  Desktop
-- email_client: Thunderbird
```

**To oznacza że tracking działa! 🎉**

---

## 🚀 NASTĘPNY KROK: Deployment na PROD

Gdy tracking działa lokalnie, następny krok to wdrożenie na PROD:
1. Zmień `app.base-url=https://timesheet.robgro.dev`
2. Wykonaj SQL migration na `robgro_aga_invoices`
3. Build i deploy
4. Wtedy tracking będzie działać z KAŻDEGO urządzenia! 📱💻🖥️
