package dev.robgro.timesheet.invoice;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for InvoiceNumberCounterService using real MySQL via Testcontainers.
 *
 * <p><b>Regression test for February 2026 incident:</b>
 * On fresh INSERT with LAST_INSERT_ID(), MariaDB returned the auto_increment row ID (64)
 * instead of the sequence value (1), because previous rollbacks had bumped the auto_increment
 * counter to 64. Fixed by replacing LAST_INSERT_ID() with direct last_value read.
 *
 * <p><b>Status: DISABLED - Requires Docker</b>
 * Remove {@code @Disabled} and ensure Docker Desktop is running to execute.
 *
 * <p><b>Why MySQL container (not H2)?</b>
 * H2 does not support {@code INSERT ... ON DUPLICATE KEY UPDATE} by default.
 * Using Testcontainers ensures test matches production MariaDB behavior.
 */
@Disabled("Requires Docker/Testcontainers - enable manually when Docker available")
@SpringBootTest
@Testcontainers
// NO @Transactional here - each nextSequence call must commit to be readable by subsequent calls
class InvoiceNumberCounterServiceIT {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
        .withDatabaseName("timesheet_test")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void mysqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    @Autowired
    private InvoiceNumberCounterService service;

    @Autowired
    private InvoiceNumberCounterRepository counterRepo;

    private static final Long SELLER_ID = 9901L;
    private static final String TEST_PERIOD = "2099-01";

    @BeforeEach
    void cleanup() {
        counterRepo.deleteCounter(SELLER_ID, "MONTHLY", TEST_PERIOD);
    }

    /**
     * Core regression test: fresh month must always start at 1, 2, 3 regardless of
     * the auto_increment value in the table.
     *
     * <p>This is the exact scenario that produced invoice numbers 064-072-02-2026
     * instead of 001-009-02-2026.
     */
    @Test
    void freshMonth_shouldReturn1_then2_then3() {
        int s1 = service.nextSequence(SELLER_ID, ResetPeriod.MONTHLY, TEST_PERIOD, null, 2099, 1);
        int s2 = service.nextSequence(SELLER_ID, ResetPeriod.MONTHLY, TEST_PERIOD, null, 2099, 1);
        int s3 = service.nextSequence(SELLER_ID, ResetPeriod.MONTHLY, TEST_PERIOD, null, 2099, 1);

        assertThat(s1).isEqualTo(1);
        assertThat(s2).isEqualTo(2);
        assertThat(s3).isEqualTo(3);
    }

    /**
     * Verify counter persists across multiple calls and increments monotonically.
     */
    @Test
    void subsequentCalls_shouldIncrementMonotonically() {
        for (int expected = 1; expected <= 5; expected++) {
            int seq = service.nextSequence(SELLER_ID, ResetPeriod.MONTHLY, TEST_PERIOD, null, 2099, 1);
            assertThat(seq).isEqualTo(expected);
        }
    }

    /**
     * Verify counterExistsForPeriod works correctly before and after first bump.
     */
    @Test
    void counterExistsForPeriod_shouldBeFalseBeforeAndTrueAfterFirstCall() {
        assertThat(service.counterExistsForPeriod(SELLER_ID, ResetPeriod.MONTHLY, TEST_PERIOD)).isFalse();

        service.nextSequence(SELLER_ID, ResetPeriod.MONTHLY, TEST_PERIOD, null, 2099, 1);

        assertThat(service.counterExistsForPeriod(SELLER_ID, ResetPeriod.MONTHLY, TEST_PERIOD)).isTrue();
    }
}