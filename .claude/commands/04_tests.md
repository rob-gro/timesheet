# Plan Testów: Comprehensive Test Strategy (MID/SENIOR Level)

## Decyzje Finalne

1. ✅ **Scope:** Tylko krytyczne fazy (1-8), pomiń opcjonalne (9-10)
2. ✅ **Kolejność:** Zaczynamy od FAZA 1 (infrastructure), potem FAZA 2 (unit tests)
3. ✅ **Database:** H2 in-memory (szybkie, już skonfigurowane)
4. ✅ **JaCoCo:** Tylko reporting (bez enforcement na początku)
5. ✅ **Git Strategy:** Małe commity per faza na master

**Total Effort:** 105-135h, 8 commitów

---

## Kolejność Wykonania

1. **FAZA 1** - Infrastructure Setup (3h) → COMMIT
2. **FAZA 2** - Missing Unit Tests (15-20h) → COMMIT
3. **FAZA 3** - Repository Tests (15-20h) → COMMIT
4. **FAZA 4** - Service Integration (15-20h) → COMMIT
5. **FAZA 5** - REST Controller Tests (20-25h) → COMMIT
6. **FAZA 6** - View Controller Tests (15-18h) → COMMIT
7. **FAZA 7** - E2E Tests (12-15h) → COMMIT
8. **FAZA 8** - Security Tests (6-8h) → COMMIT

---

## Stan Obecny

**Co mamy (11 test files, 4,345 linii):**
- TimesheetServiceImplTest - 54 testy
- InvoiceServiceImplTest - 28 testów
- UserServiceImplTest - 30 testów
- ClientServiceImplTest - 17 testów
- BillingServiceImplTest - 15 testów
- InvoiceCreationServiceImplTest - 19 testów
- + 5 innych

**Co brakuje:**
- ZERO integration tests (@SpringBootTest, @DataJpaTest)
- ZERO controller tests (15 controllerów)
- ZERO E2E tests
- Nieprzetestowane: Auth, JWT, Security, Mappers, PDF Generator

---

## FAZA 1: Infrastructure Setup (3h)

### 1.1 Test Data Builders
**Package:** `src/test/java/dev/robgro/timesheet/fixtures/`

**Przykład:**
```java
public class ClientTestDataBuilder {
    private Long id = 1L;
    private String clientName = "Test Client";
    private double hourlyRate = 50.0;

    public static ClientTestDataBuilder aClient() {
        return new ClientTestDataBuilder();
    }

    public ClientTestDataBuilder withName(String name) {
        this.clientName = name;
        return this;
    }

    public Client build() {
        Client client = new Client();
        client.setId(id);
        client.setClientName(clientName);
        client.setHourlyRate(hourlyRate);
        return client;
    }
}
```

**Builders do utworzenia:**
- ClientTestDataBuilder
- TimesheetTestDataBuilder
- InvoiceTestDataBuilder
- UserTestDataBuilder
- InvoiceItemTestDataBuilder

### 1.2 JaCoCo Setup
**pom.xml:**
```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.11</version>
    <executions>
        <execution>
            <goals><goal>prepare-agent</goal></goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals><goal>report</goal></goals>
        </execution>
    </executions>
</plugin>
```

### 1.3 Remove TestNG
```xml
<!-- DELETE THIS from pom.xml -->
<dependency>
    <groupId>org.testng</groupId>
    <artifactId>testng</artifactId>
</dependency>
```

---

## FAZA 2: Missing Unit Tests (15-20h)

### AuthServiceImplTest (CRITICAL)
```java
@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {
    @Mock private AuthenticationManager authManager;
    @Mock private JwtTokenProvider tokenProvider;
    @InjectMocks private AuthServiceImpl authService;

    @Test
    void shouldLoginSuccessfully_whenValidCredentials() { }

    @Test
    void shouldThrowException_whenInvalidCredentials() { }
}
```

### JwtTokenProviderTest
### CustomUserDetailsServiceTest
### PdfGeneratorTest
### Mapper Tests (6 mappers)
### GlobalExceptionHandlerTest

---

## FAZA 3: Repository Integration Tests (15-20h)

**Wzorzec:**
```java
@DataJpaTest
class ClientRepositoryTest {
    @Autowired private ClientRepository repository;
    @Autowired private TestEntityManager entityManager;

    @Test
    void shouldFindActiveClients() {
        Client active = aClient().withName("Active").build();
        entityManager.persist(active);
        entityManager.flush();

        List<Client> result = repository.findByActiveTrue();

        assertThat(result).hasSize(1);
    }
}
```

**Testy:**
- ClientRepositoryTest
- TimesheetRepositoryTest (complex queries)
- InvoiceRepositoryTest
- UserRepositoryTest
- RoleRepositoryTest

---

## FAZA 4: Service Integration Tests (15-20h)

**Wzorzec:**
```java
@SpringBootTest
@Transactional
class InvoiceServiceIntegrationTest {
    @Autowired private InvoiceService invoiceService;
    @Autowired private InvoiceRepository invoiceRepository;

    @Test
    void shouldDeleteInvoice_andDetachTimesheets_withFlush() {
        // Test real DB behavior with transactions
    }
}
```

---

## FAZA 5: REST Controller Tests (20-25h)

**Wzorzec:**
```java
@WebMvcTest(ClientController.class)
class ClientControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private ClientService clientService;

    @Test
    void shouldGetAllClients_return200() throws Exception {
        mockMvc.perform(get("/api/v1/clients"))
            .andExpect(status().isOk());
    }
}
```

**Controllers:**
- ClientControllerTest
- TimesheetControllerTest
- InvoiceControllerTest
- UserControllerTest
- AuthControllerTest

---

## FAZA 6: View Controller Tests (15-18h)

Thymeleaf view controllers z MockMvc

---

## FAZA 7: E2E Tests (12-15h)

**Wzorzec:**
```java
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class InvoiceCreationWorkflowE2ETest {
    @Test
    void shouldCreateInvoice_fromTimesheets_fullWorkflow() {
        // 1. Create client
        // 2. Create timesheets
        // 3. Create invoice via API
        // 4. Verify DB state
    }
}
```

---

## FAZA 8: Security Tests (6-8h)

- SecurityConfigTest
- JwtAuthenticationFilterTest

---

## Best Practices

### Unit Tests
- ✅ `@ExtendsWith(MockitoExtension.class)`
- ✅ AssertJ fluent assertions ONLY
- ✅ Given-When-Then structure
- ✅ `should[Action]_when[Condition]` naming

### Integration Tests
- ✅ `@SpringBootTest` or `@DataJpaTest`
- ✅ `@Transactional` for rollback
- ✅ H2 in-memory database

### Controller Tests
- ✅ `@WebMvcTest(ControllerClass.class)`
- ✅ MockMvc for HTTP testing
- ✅ `@MockBean` for services

---

## Metryki Sukcesu

- **Overall Coverage:** 70%+
- **Service Layer:** 80%+
- **Controller Layer:** 70%+
- **Total Tests:** 400-470
- **Execution Time:** < 5 min

---

**Pełny plan:** `C:\Users\rgrod\.claude\plans\valiant-exploring-phoenix.md`