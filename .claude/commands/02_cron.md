# Plan Implementacji: Automatyczne Wystawianie Faktur (CRON)

## Cel
Automatyczne generowanie faktur za poprzedni miesiąc dla wszystkich aktywnych klientów, wykonywane 3. dnia każdego miesiąca o 15:00, z pełną automatyzacją (faktury + PDF + email).

## Wymagania Biznesowe
- **Harmonogram**: 3. dzień miesiąca, godz. 15:00
- **Data faktury**: Ostatni dzień poprzedniego miesiąca
- **Zakres**: Wszystkie aktywne klienty z niezafakturowanymi timesheetami
- **Automatyzacja**: Utworzenie faktur → Generowanie PDF → Wysyłka emaili
- **Error handling**: Logowanie błędów + email do admina z pełnymi szczegółami
- **Konfiguracja**: Możliwość włączenia/wyłączenia przez application.properties

## Analiza Istniejącej Infrastruktury

### ✅ Gotowe Komponenty (DO WYKORZYSTANIA)
1. **BillingService.generateMonthlyInvoices(year, month)**
   - Lokalizacja: `src/main/java/dev/robgro/timesheet/invoice/BillingServiceImpl.java`
   - Już zaimplementowane, nieużywane
   - Generuje faktury dla wszystkich klientów za dany miesiąc

2. **InvoiceService.savePdfAndSendInvoice(invoiceId)**
   - Lokalizacja: `src/main/java/dev/robgro/timesheet/invoice/InvoiceService.java`
   - Generuje PDF i wysyła email do klienta

3. **EmailMessageService**
   - Lokalizacja: `src/main/java/dev/robgro/timesheet/invoice/EmailMessageService.java`
   - Wysyła emaile z załącznikami (factury PDF)

### ❌ Do Implementacji
1. Spring Scheduler (@EnableScheduling)
2. Scheduled task class
3. Admin notification service (błędy + raporty)
4. Configuration properties
5. Comprehensive error handling
6. Tests

---

## Architektura Rozwiązania (SOLID Principles)

### 1. Single Responsibility Principle (SRP)
- **InvoicingScheduler** - tylko harmonogram i orkiestracja
- **InvoicingTaskService** - logika biznesowa zadania
- **AdminNotificationService** - powiadomienia dla admina
- **BillingService** - generowanie faktur (już istnieje)
- **InvoiceService** - PDF + email (już istnieje)

### 2. Open/Closed Principle
- Interfejs dla notification service (możliwość dodania SMS, Slack itp.)
- Strategia error handling konfigurowalna

### 3. Liskov Substitution
- Wszystkie serwisy przez interfejsy

### 4. Interface Segregation
- Osobne interfejsy dla różnych typów powiadomień

### 5. Dependency Inversion
- Zależności przez interfejsy, nie konkretne klasy

---

## Implementacja - Krok po Kroku

### **KROK 1: Konfiguracja Properties**
**Plik**: `src/main/resources/application.properties`

```properties
# Automated Invoicing Scheduler
scheduling.invoicing.enabled=true
scheduling.invoicing.cron=0 0 15 3 * ?
scheduling.invoicing.admin-email=robgrodev@gmail.com
scheduling.invoicing.send-summary-email=true
scheduling.invoicing.send-empty-client-warning=true
```

**Dlaczego**:
- Oddzielenie konfiguracji od kodu (12-factor app)
- Łatwa zmiana bez rebuildu (zwłaszcza na Heroku)
- Możliwość wyłączenia w dev (`scheduling.invoicing.enabled=false`)

---

### **KROK 2: Configuration Class**
**Nowy plik**: `src/main/java/dev/robgro/timesheet/config/SchedulingConfig.java`

```java
@Configuration
@EnableScheduling
@ConditionalOnProperty(
    name = "scheduling.invoicing.enabled",
    havingValue = "true",
    matchIfMissing = false
)
public class SchedulingConfig {
    // Scheduler thread pool configuration
}
```

**Dlaczego**:
- @ConditionalOnProperty - włącza/wyłącza scheduler przez properties
- Centralna konfiguracja schedulingu
- Thread pool config dla async operations

---

### **KROK 3: Properties Class**
**Nowy plik**: `src/main/java/dev/robgro/timesheet/config/InvoicingSchedulerProperties.java`

```java
@ConfigurationProperties(prefix = "scheduling.invoicing")
@Validated
public class InvoicingSchedulerProperties {
    private boolean enabled;
    @NotBlank private String cron;
    @Email @NotBlank private String adminEmail;
    private boolean sendSummaryEmail;
    private boolean sendEmptyClientWarning;
    // getters/setters
}
```

**Dlaczego**:
- Type-safe configuration
- Validation na poziomie properties
- IDE autocomplete dla konfiguracji

---

### **KROK 4: Admin Notification Service**
**Nowy plik**: `src/main/java/dev/robgro/timesheet/scheduler/AdminNotificationService.java`

**Interface**:
```java
public interface AdminNotificationService {
    void sendErrorNotification(String subject, String details, Exception e);
    void sendSummaryNotification(InvoicingSummary summary);
    void sendEmptyClientWarning(List<String> clientNames);
}
```

**Implementation**:
```java
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminNotificationServiceImpl implements AdminNotificationService {
    private final JavaMailSender emailSender;
    private final InvoicingSchedulerProperties properties;

    // Implementacja wysyłki emaili do admina
    // - Błędy: pełny stacktrace + context
    // - Summary: lista wygenerowanych faktur, statystyki
    // - Empty clients: lista klientów bez timesheetów
}
```

**Dlaczego**:
- Oddzielenie logiki notyfikacji od biznesowej
- Łatwe dodanie innych kanałów (Slack, SMS)
- Testowalne (mock email sender)

---

### **KROK 5: Invoicing Task Service**
**Nowy plik**: `src/main/java/dev/robgro/timesheet/scheduler/InvoicingTaskService.java`

**Interface**:
```java
public interface InvoicingTaskService {
    InvoicingSummary executeMonthlyInvoicing();
}
```

**Implementation**:
```java
@Slf4j
@Service
@RequiredArgsConstructor
public class InvoicingTaskServiceImpl implements InvoicingTaskService {

    private final BillingService billingService;
    private final InvoiceService invoiceService;
    private final ClientService clientService;
    private final AdminNotificationService notificationService;
    private final InvoicingSchedulerProperties properties;

    @Override
    @Transactional
    public InvoicingSummary executeMonthlyInvoicing() {
        log.info("=== ROZPOCZĘCIE AUTOMATYCZNEGO GENEROWANIA FAKTUR ===");

        // 1. Oblicz poprzedni miesiąc
        YearMonth previousMonth = YearMonth.now().minusMonths(1);

        // 2. Generuj faktury (używa istniejącego BillingService!)
        List<InvoiceDto> createdInvoices = billingService
            .generateMonthlyInvoices(previousMonth.getYear(), previousMonth.getMonthValue());

        // 3. Dla każdej faktury: generuj PDF + wyślij email
        List<InvoiceProcessingResult> results = new ArrayList<>();
        for (InvoiceDto invoice : createdInvoices) {
            InvoiceProcessingResult result = processInvoice(invoice);
            results.add(result);
        }

        // 4. Sprawdź klientów bez timesheetów (tylko aktywni)
        List<String> emptyClients = findActiveClientsWithoutTimesheets(previousMonth);

        // 5. Zbuduj podsumowanie
        InvoicingSummary summary = InvoicingSummary.builder()
            .executionTime(LocalDateTime.now())
            .previousMonth(previousMonth)
            .totalInvoices(createdInvoices.size())
            .successfulInvoices(countSuccessful(results))
            .failedInvoices(countFailed(results))
            .clientsWithoutTimesheets(emptyClients)
            .processingResults(results)
            .build();

        // 6. Wyślij powiadomienia do admina
        sendAdminNotifications(summary, emptyClients);

        log.info("=== ZAKOŃCZONO GENEROWANIE FAKTUR: {} sukces, {} błędów ===",
            summary.getSuccessfulInvoices(), summary.getFailedInvoices());

        return summary;
    }

    private InvoiceProcessingResult processInvoice(InvoiceDto invoice) {
        try {
            log.info("Przetwarzanie faktury: {}, klient: {}",
                invoice.invoiceNumber(), invoice.clientName());

            // Używa istniejącej metody!
            invoiceService.savePdfAndSendInvoice(invoice.id());

            return InvoiceProcessingResult.success(invoice);

        } catch (Exception e) {
            log.error("Błąd przy przetwarzaniu faktury {}: {}",
                invoice.invoiceNumber(), e.getMessage(), e);

            // Email do admina o konkretnym błędzie
            notificationService.sendErrorNotification(
                "Błąd faktury " + invoice.invoiceNumber(),
                buildErrorDetails(invoice, e),
                e
            );

            return InvoiceProcessingResult.failure(invoice, e);
        }
    }

    private List<String> findActiveClientsWithoutTimesheets(YearMonth month) {
        List<ClientDto> activeClients = clientService.getAllClients()
            .stream()
            .filter(ClientDto::active)
            .toList();

        List<String> emptyClients = new ArrayList<>();
        for (ClientDto client : activeClients) {
            List<TimesheetDto> timesheets = timesheetService
                .getMonthlyTimesheets(client.id(), month.getYear(), month.getMonthValue());

            if (timesheets.isEmpty()) {
                log.info("Klient {} nie ma timesheetów w {}",
                    client.clientName(), month);
                emptyClients.add(client.clientName());
            }
        }

        return emptyClients;
    }

    private void sendAdminNotifications(InvoicingSummary summary, List<String> emptyClients) {
        // Summary email
        if (properties.isSendSummaryEmail()) {
            notificationService.sendSummaryNotification(summary);
        }

        // Empty clients warning
        if (properties.isSendEmptyClientWarning() && !emptyClients.isEmpty()) {
            notificationService.sendEmptyClientWarning(emptyClients);
        }
    }
}
```

**Dlaczego**:
- **SRP**: tylko logika zadania automatycznego, bez schedulingu
- **Transactional**: atomowość operacji DB
- **Error handling**: per-invoice errors nie przerywają całego procesu
- **Używa istniejących serwisów**: BillingService, InvoiceService
- **Comprehensive logging**: każdy krok logowany
- **Admin notifications**: email przy każdym błędzie + summary na końcu

---

### **KROK 6: Scheduled Task**
**Nowy plik**: `src/main/java/dev/robgro/timesheet/scheduler/InvoicingScheduler.java`

```java
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "scheduling.invoicing.enabled", havingValue = "true")
public class InvoicingScheduler {

    private final InvoicingTaskService invoicingTaskService;
    private final AdminNotificationService notificationService;

    @Scheduled(cron = "${scheduling.invoicing.cron}")
    public void generateMonthlyInvoices() {
        log.info("▶ Uruchamianie scheduled task: generowanie faktur miesięcznych");

        try {
            InvoicingSummary summary = invoicingTaskService.executeMonthlyInvoicing();

            log.info("✅ Scheduled task zakończony pomyślnie. " +
                "Wygenerowano {} faktur", summary.getTotalInvoices());

        } catch (Exception e) {
            log.error("❌ KRYTYCZNY BŁĄD scheduled task: {}", e.getMessage(), e);

            // Email do admina o globalnym błędzie
            notificationService.sendErrorNotification(
                "KRYTYCZNY: Scheduled task invoicing nieudany",
                "Cały proces automatycznego generowania faktur zakończył się błędem.",
                e
            );

            // Re-throw aby Spring zarejestrowało błąd
            throw new RuntimeException("Scheduled invoicing task failed", e);
        }
    }
}
```

**Dlaczego**:
- **SRP**: tylko scheduling, logika w service
- **@ConditionalOnProperty**: automatycznie disabled gdy properties=false
- **Cron z properties**: łatwa zmiana bez rebuildu
- **Global error handling**: catch-all dla nieoczekiwanych błędów
- **Re-throw**: Spring Framework wie że task failed

---

### **KROK 7: DTOs**
**Nowy plik**: `src/main/java/dev/robgro/timesheet/scheduler/InvoicingSummary.java`

```java
@Data
@Builder
public class InvoicingSummary {
    private LocalDateTime executionTime;
    private YearMonth previousMonth;
    private int totalInvoices;
    private int successfulInvoices;
    private int failedInvoices;
    private List<String> clientsWithoutTimesheets;
    private List<InvoiceProcessingResult> processingResults;
}

@Data
@Builder
public class InvoiceProcessingResult {
    private InvoiceDto invoice;
    private boolean success;
    private String errorMessage;
    private Exception exception;

    public static InvoiceProcessingResult success(InvoiceDto invoice) {
        return InvoiceProcessingResult.builder()
            .invoice(invoice)
            .success(true)
            .build();
    }

    public static InvoiceProcessingResult failure(InvoiceDto invoice, Exception e) {
        return InvoiceProcessingResult.builder()
            .invoice(invoice)
            .success(false)
            .errorMessage(e.getMessage())
            .exception(e)
            .build();
    }
}
```

---

### **KROK 8: Enable Scheduling w Application**
**Edytuj**: `src/main/java/dev/robgro/timesheet/TimesheetApplication.java`

```java
@SpringBootApplication
@EnableScheduling  // ← DODAJ
public class TimesheetApplication {
    public static void main(String[] args) {
        SpringApplication.run(TimesheetApplication.class, args);
    }
}
```

---

### **KROK 9: Tests**

#### 9.1 Unit Test: InvoicingTaskServiceTest
**Nowy plik**: `src/test/java/dev/robgro/timesheet/scheduler/InvoicingTaskServiceTest.java`

```java
@ExtendWith(MockitoExtension.class)
class InvoicingTaskServiceTest {

    @Mock private BillingService billingService;
    @Mock private InvoiceService invoiceService;
    @Mock private ClientService clientService;
    @Mock private TimesheetService timesheetService;
    @Mock private AdminNotificationService notificationService;
    @Mock private InvoicingSchedulerProperties properties;

    @InjectMocks
    private InvoicingTaskServiceImpl invoicingTaskService;

    @Test
    void shouldGenerateInvoicesSuccessfully() {
        // given: 2 faktury do wygenerowania
        // when: executeMonthlyInvoicing()
        // then: 2 faktury created, PDF + email wysłane, summary correct
    }

    @Test
    void shouldHandlePartialFailures() {
        // given: 3 faktury, 1 failuje przy PDF generation
        // when: executeMonthlyInvoicing()
        // then: 2 succesu, 1 failure, notification wysłany tylko dla błędu
    }

    @Test
    void shouldDetectActiveClientsWithoutTimesheets() {
        // given: 3 aktywnych klientów, 1 bez timesheetów
        // when: executeMonthlyInvoicing()
        // then: empty client warning email wysłany z 1 klientem
    }

    @Test
    void shouldNotSendNotificationsWhenDisabled() {
        // given: properties.sendSummaryEmail = false
        // when: executeMonthlyInvoicing()
        // then: notificationService.sendSummary() NEVER called
    }
}
```

#### 9.2 Integration Test: InvoicingSchedulerIntegrationTest
**Nowy plik**: `src/test/java/dev/robgro/timesheet/scheduler/InvoicingSchedulerIntegrationTest.java`

```java
@SpringBootTest
@TestPropertySource(properties = {
    "scheduling.invoicing.enabled=false", // Wyłącz automatyczne uruchomienie
    "scheduling.invoicing.cron=0 0 15 3 * ?",
    "scheduling.invoicing.admin-email=test@test.com"
})
class InvoicingSchedulerIntegrationTest {

    @Autowired private InvoicingTaskService invoicingTaskService;
    @Autowired private ClientRepository clientRepository;
    @Autowired private TimesheetRepository timesheetRepository;
    @Autowired private InvoiceRepository invoiceRepository;

    @Test
    @Transactional
    void shouldGenerateInvoicesForPreviousMonth() {
        // Setup: Create test clients + timesheets for previous month
        // Execute: invoicingTaskService.executeMonthlyInvoicing()
        // Verify: Invoices created in DB, PDFs generated, emails sent
    }
}
```

#### 9.3 Scheduler Test (Cron Expression)
**Nowy plik**: `src/test/java/dev/robgro/timesheet/scheduler/InvoicingSchedulerTest.java`

```java
@ExtendWith(MockitoExtension.class)
class InvoicingSchedulerTest {

    @Mock private InvoicingTaskService taskService;
    @Mock private AdminNotificationService notificationService;

    @InjectMocks
    private InvoicingScheduler scheduler;

    @Test
    void shouldCallTaskServiceWhenScheduled() {
        // given: mock successful execution
        when(taskService.executeMonthlyInvoicing())
            .thenReturn(InvoicingSummary.builder().build());

        // when: scheduler.generateMonthlyInvoices()
        scheduler.generateMonthlyInvoices();

        // then: taskService called once
        verify(taskService, times(1)).executeMonthlyInvoicing();
    }

    @Test
    void shouldSendNotificationOnCriticalError() {
        // given: task service throws exception
        when(taskService.executeMonthlyInvoicing())
            .thenThrow(new RuntimeException("Database error"));

        // when: scheduler.generateMonthlyInvoices() + expect exception
        assertThrows(RuntimeException.class,
            () -> scheduler.generateMonthlyInvoices());

        // then: notification sent to admin
        verify(notificationService).sendErrorNotification(
            anyString(), anyString(), any(Exception.class));
    }
}
```

---

## Struktura Plików (Nowe + Modyfikacje)

### Nowe pliki:
```
src/main/java/dev/robgro/timesheet/
├── config/
│   ├── SchedulingConfig.java                    [NOWY]
│   └── InvoicingSchedulerProperties.java        [NOWY]
├── scheduler/
│   ├── InvoicingScheduler.java                  [NOWY]
│   ├── InvoicingTaskService.java                [NOWY - interface]
│   ├── InvoicingTaskServiceImpl.java            [NOWY]
│   ├── AdminNotificationService.java            [NOWY - interface]
│   ├── AdminNotificationServiceImpl.java        [NOWY]
│   ├── InvoicingSummary.java                    [NOWY - DTO]
│   └── InvoiceProcessingResult.java             [NOWY - DTO]

src/test/java/dev/robgro/timesheet/scheduler/
├── InvoicingTaskServiceTest.java                [NOWY]
├── InvoicingSchedulerTest.java                  [NOWY]
└── InvoicingSchedulerIntegrationTest.java       [NOWY]
```

### Modyfikacje:
```
src/main/java/dev/robgro/timesheet/
└── TimesheetApplication.java                    [DODAJ @EnableScheduling]

src/main/resources/
└── application.properties                       [DODAJ scheduler properties]
```

---

## Cron Expression Explained

```
0 0 15 3 * ?
│ │ │  │ │ │
│ │ │  │ │ └─ Day of week (? = any)
│ │ │  │ └─── Month (* = every month)
│ │ │  └───── Day of month (3 = 3rd day)
│ │ └──────── Hour (15 = 3 PM)
│ └────────── Minute (0)
└──────────── Second (0)
```

**Wykonanie**: Co miesiąc, 3. dnia o 15:00:00

---

## Configuration per Environment

### Development (local):
```properties
scheduling.invoicing.enabled=false  # Wyłączone, uruchomienie ręczne przez test/endpoint
```

### Test:
```properties
scheduling.invoicing.enabled=false  # Wyłączone, testy kontrolują wykonanie
scheduling.invoicing.admin-email=test@test.com
```

### Production (Heroku):
```properties
scheduling.invoicing.enabled=true
scheduling.invoicing.cron=0 0 15 3 * ?
scheduling.invoicing.admin-email=robgrodev@gmail.com
scheduling.invoicing.send-summary-email=true
scheduling.invoicing.send-empty-client-warning=true
```

**Ustawianie na Heroku**:
```bash
heroku config:set SCHEDULING_INVOICING_ENABLED=true
heroku config:set SCHEDULING_INVOICING_ADMIN_EMAIL=robgrodev@gmail.com
```

---

## Error Handling Strategy

### Per-Invoice Errors (Non-Critical):
- ❌ Błąd PDF generation dla faktury #123
- ✅ Logowanie ERROR + email do admina
- ✅ Kontynuuj z następną fakturą
- ✅ Summary na końcu: "10 sukces, 1 błąd"

### Global Errors (Critical):
- ❌ Database connection failure
- ❌ BillingService całkowicie nieudany
- ✅ Logowanie ERROR + email do admina
- ✅ Re-throw exception → Spring rejestruje failure
- ✅ Administrator musi naprawić i uruchomić ręcznie

### Email Notifications:
1. **Per-invoice error**: Natychmiast po błędzie
2. **Summary**: Na końcu każdego uruchomienia (sukces lub partial failure)
3. **Empty clients**: Na końcu, jeśli włączone w properties
4. **Critical error**: Natychmiast, scheduled task failed

---

## Logging Strategy

### INFO Level:
- Start/stop scheduled task
- Rozpoczęcie generowania faktur
- Każda faktura: "Processing invoice #123 for client XYZ"
- Podsumowanie: "Generated 10 invoices, 9 success, 1 failure"

### WARN Level:
- Klient bez timesheetów (tylko jeśli send-warning=true)

### ERROR Level:
- Błąd generowania faktury (per-invoice)
- Błąd PDF generation
- Błąd wysyłki email
- Krytyczny błąd scheduled task

### DEBUG Level:
- Szczegóły obliczania poprzedniego miesiąca
- Lista klientów do przetworzenia
- Szczegóły każdej operacji

---

## Bezpieczeństwo

### Email Security:
- Nie loguj pełnej treści emaili (zawierają dane osobowe)
- Admin email z properties (nie hardcoded)
- Stacktrace'y w emailach tylko dla admina (nie dla klientów)

### Database:
- @Transactional na executeMonthlyInvoicing()
- Rollback w przypadku krytycznego błędu
- Per-invoice transactions (jeden błąd nie psuje innych)

### Scheduler Security:
- @ConditionalOnProperty zapobiega przypadkowemu włączeniu
- Cron z properties (nie hardcoded)
- Logowanie każdego uruchomienia (audit trail)

---

## Testing Strategy

### Unit Tests (Mockito):
- InvoicingTaskService - logika biznesowa
- AdminNotificationService - wysyłka emaili
- InvoicingScheduler - scheduling logic

### Integration Tests (SpringBootTest):
- End-to-end: scheduler → billing → PDF → email
- Database persistence
- Transaction rollback

### Manual Testing:
- Uruchomienie ręczne przez endpoint (opcjonalnie)
- Test na środowisku testowym przed produkcją
- Weryfikacja emaili

---

## Deployment Checklist

### Przed deploymentem:
1. ✅ Wszystkie testy przechodzą
2. ✅ Configuration properties dodane do Heroku
3. ✅ Admin email poprawny
4. ✅ Scheduler DISABLED na Heroku (początkowo)
5. ✅ Zbudować i przetestować lokalnie

### Po deploymencie:
1. Uruchomić ręcznie (przez test/endpoint) na production
2. Zweryfikować: faktury created, PDFy generated, emaile wysłane
3. Sprawdzić logi Heroku
4. Włączyć scheduler: `heroku config:set SCHEDULING_INVOICING_ENABLED=true`
5. Monitorować pierwsze automatyczne uruchomienie (3. dzień miesiąca)

---

## Monitoring & Maintenance

### Monitoring:
- Heroku logs: `heroku logs --tail | grep "invoicing"`
- Email inbox: sprawdzić summary emails
- Database: ilość faktur per miesiąc

### Maintenance:
- Przegląd logów co miesiąc po automatycznym uruchomieniu
- Update cron expression jeśli potrzeba zmiany czasu
- Regularne sprawdzanie czy wszystkie faktury są generowane

---

## SOLID Principles - Summary

### Single Responsibility:
- **InvoicingScheduler**: tylko harmonogram
- **InvoicingTaskService**: logika biznesowa
- **AdminNotificationService**: powiadomienia
- **BillingService**: generowanie faktur (już istnieje)
- **InvoiceService**: PDF + email (już istnieje)

### Open/Closed:
- Interface dla NotificationService → łatwo dodać Slack, SMS
- Strategy pattern dla error handling

### Liskov Substitution:
- Wszystkie implementacje przez interfejsy
- Mockowanie w testach

### Interface Segregation:
- Oddzielne interfejsy dla task service i notification service
- Nie ma "god interface"

### Dependency Inversion:
- Zależności przez interfejsy
- Spring DI (@RequiredArgsConstructor)
- Testowalne (constructor injection)

---

## Podsumowanie

### Co wykorzystujemy (istniejące):
✅ BillingService.generateMonthlyInvoices()
✅ InvoiceService.savePdfAndSendInvoice()
✅ EmailMessageService

### Co dodajemy (nowe):
➕ SchedulingConfig + Properties
➕ InvoicingScheduler (CRON)
➕ InvoicingTaskService (orchestration)
➕ AdminNotificationService (alerts)
➕ Comprehensive error handling
➕ Testy (unit + integration)

### Rezultat:
🎯 Automatyczne faktury każdego 3. dnia miesiąca o 15:00
🎯 Full automation: invoices → PDF → email
🎯 Admin alerts: errors + summary + empty clients
🎯 Konfigurowalne przez properties
🎯 Production-ready, SOLID, testowalne

---

## Kolejność Implementacji (Execution Plan)

1. **Properties** → application.properties
2. **DTOs** → InvoicingSummary, InvoiceProcessingResult
3. **Properties Class** → InvoicingSchedulerProperties
4. **Admin Notification Service** → interface + impl
5. **Invoicing Task Service** → interface + impl
6. **Scheduler Config** → SchedulingConfig
7. **Scheduler** → InvoicingScheduler
8. **Enable Scheduling** → TimesheetApplication.java
9. **Tests** → Unit tests + Integration tests
10. **Manual Testing** → Local run
11. **Deployment** → Heroku config + deploy
12. **Production Testing** → Ręczne uruchomienie na prod
13. **Enable Scheduler** → SCHEDULING_INVOICING_ENABLED=true

---

## Pytania do Potwierdzenia

1. ✅ Czy scheduler ma się uruchamiać 3. dnia każdego miesiąca o 15:00? **TAK**
2. ✅ Czy data faktury to ostatni dzień poprzedniego miesiąca? **TAK**
3. ✅ Czy automatycznie generować PDF i wysyłać email? **TAK (Full automation)**
4. ✅ Czy email do admina przy każdym błędzie? **TAK + na końcu summary**
5. ✅ Czy logować INFO dla klientów bez timesheetów? **TAK + email do admina**
6. ✅ Czy scheduler ma być konfigurowalny przez properties? **TAK**

---

**Plan gotowy do implementacji!** 🚀