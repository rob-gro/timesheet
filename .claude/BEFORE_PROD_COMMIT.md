# ⚠️ PRZED COMMITEM NA PROD - USUNĄĆ!

## Security - Endpoint testowy schedulera

**MUSI BYĆ USUNIĘTE przed deployem na produkcję:**

1. **SecurityConfig.java** - linia z `/api/v1/test/**`:
   ```java
   .requestMatchers("/api/v1/test/**").permitAll()
   ```
   Lokalizacja: `src/main/java/dev/robgro/timesheet/security/SecurityConfig.java:84`

2. **InvoicingSchedulerTestController.java** - cały plik:
   ```
   src/main/java/dev/robgro/timesheet/scheduler/InvoicingSchedulerTestController.java
   ```

**Dlaczego?**
- Endpoint testowy pozwala każdemu bez logowania uruchomić generowanie faktur
- To jest TYLKO do testów lokalnych
- Na produkcji to security hole

## Configuration - application.properties

**MUSI BYĆ ZMIENIONE przed deployem na produkcję:**

3. **Database URL** - przestaw na prod:
   ```properties
   # Odkomentuj tę linię:
   spring.datasource.url=jdbc:mariadb://mysql-robgro.alwaysdata.net:3306/robgro_aga_invoices?sslMode=REQUIRED&requireSSL=true&verifyServerCertificate=true
   # Zakomentuj tę linię:
   #spring.datasource.url=jdbc:mariadb://mysql-robgro.alwaysdata.net:3306/robgro_test_invoices?sslMode=REQUIRED&requireSSL=true&verifyServerCertificate=true
   ```

4. **FTP Directory** - przestaw na prod:
   ```properties
   # Odkomentuj tę linię:
   ftp.invoices.directory=/files/invoices
   # Zakomentuj tę linię:
   #ftp.invoices.directory=/files/invoices_test
   ```

5. **Scheduler CRON** - ustaw na właściwy czas:
   ```properties
   scheduling.invoicing.cron=0 0 15 3 * ?
   # Zamiast testowego:
   #scheduling.invoicing.cron=0 * * * * ?
   ```

**Jak zacommitować scheduler na prod:**
1. Usunąć powyższe 2 rzeczy (SecurityConfig + TestController)
2. Przestawić DB na prod (robgro_aga_invoices)
3. Przestawić FTP na prod (/files/invoices)
4. Ustawić właściwy CRON (0 0 15 3 * ?)
5. Scheduler będzie działał przez CRON (enabled=true na Heroku)
6. Testowanie na prod: zmienić cron na `0 * * * * ?` (co minutę), przetestować, wrócić do `0 0 15 3 * ?`