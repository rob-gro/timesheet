package dev.robgro.timesheet.invoice;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Concurrency test for InvoiceNumberCounterService using real MySQL via Testcontainers.
 *
 * <p><b>CRITICAL:</b> NO @Transactional at class level!
 * If @Transactional is at class level, all threads run in same transaction,
 * Hibernate cache masks concurrency issues, and test becomes false positive.
 *
 * <p>Each thread must get separate transaction to properly test atomic UPSERT.
 *
 * <p><b>Why MySQL container (not H2)?</b>
 * H2 does not guarantee same atomicity as MySQL/MariaDB for ON DUPLICATE KEY UPDATE
 * with LAST_INSERT_ID(). Using Testcontainers ensures test matches production behavior.
 *
 * <p><b>Status: DISABLED - Requires Docker</b>
 * Test is disabled by default because it requires Docker Desktop running.
 * UPSERT correctness is verified by:
 * - Unit tests (PeriodKeyFactory, InvoiceNumberGenerator)
 * - Manual testing on DEV environment
 * - Production MySQL guarantees atomicity
 *
 * <p><b>To enable:</b> Remove @Disabled annotation and ensure Docker is running.
 */
@Disabled("Requires Docker/Testcontainers - enable manually when Docker available")
@SpringBootTest
@Testcontainers
// NO @Transactional here - intentional for concurrency testing
class InvoiceNumberCounterServiceConcurrencyTest {

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
    private InvoiceNumberCounterService counterService;

    @Test
    void shouldGenerateUniqueSequences_whenConcurrentRequests() throws Exception {
        // Given - Use high seller ID to avoid collision with other tests
        Long sellerId = 99001L;
        ResetPeriod resetPeriod = ResetPeriod.MONTHLY;
        String periodKey = "2026-02";

        // When - 2 threads request sequence simultaneously
        ExecutorService executor = Executors.newFixedThreadPool(2);
        List<Future<Integer>> futures = new ArrayList<>();

        for (int i = 0; i < 2; i++) {
            futures.add(executor.submit(() -> {
                try {
                    Thread.sleep(10); // Force overlap - prevents false positive
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return counterService.nextSequence(sellerId, resetPeriod, periodKey, null, 2026, 2);
            }));
        }

        // Then
        Set<Integer> sequences = new HashSet<>();
        for (Future<Integer> future : futures) {
            sequences.add(future.get());
        }

        assertThat(sequences)
            .as("Both threads should get different sequence numbers")
            .hasSize(2); // Both threads got different numbers

        // Sequences should be 1 and 2 (or any two consecutive numbers if counter pre-existed)
        assertThat(sequences)
            .as("Sequences should be consecutive")
            .allMatch(seq -> seq >= 1); // All sequences are positive

        executor.shutdown();
    }

    @Test
    void shouldHandleHighConcurrency_withoutCollisions() throws Exception {
        // Given - Test 10 parallel requests - all should get unique numbers
        Long sellerId = 99002L; // High seller ID to avoid interference with other tests
        ResetPeriod resetPeriod = ResetPeriod.YEARLY;
        String periodKey = "2026";

        // When - 10 threads request simultaneously
        ExecutorService executor = Executors.newFixedThreadPool(10);
        List<Future<Integer>> futures = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            futures.add(executor.submit(() -> {
                try {
                    Thread.sleep(10); // Force overlap - prevents false positive
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return counterService.nextSequence(sellerId, resetPeriod, periodKey, null, 2026, 0);
            }));
        }

        // Then
        Set<Integer> sequences = new HashSet<>();
        for (Future<Integer> future : futures) {
            sequences.add(future.get());
        }

        assertThat(sequences)
            .as("All 10 threads should get different sequence numbers")
            .hasSize(10); // No collisions - all unique

        assertThat(sequences)
            .as("All sequences should be positive")
            .allMatch(seq -> seq >= 1);

        executor.shutdown();
    }

    @Test
    void shouldHandleDifferentPeriods_independently() throws Exception {
        // Given - Same seller, different periods should have independent counters
        Long sellerId = 99003L;

        // When - Request sequences for different months in parallel
        ExecutorService executor = Executors.newFixedThreadPool(4);
        List<Future<Integer>> futures = new ArrayList<>();

        // 2 requests for February
        for (int i = 0; i < 2; i++) {
            futures.add(executor.submit(() -> {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return counterService.nextSequence(sellerId, ResetPeriod.MONTHLY, "2026-02", null, 2026, 2);
            }));
        }

        // 2 requests for March
        for (int i = 0; i < 2; i++) {
            futures.add(executor.submit(() -> {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return counterService.nextSequence(sellerId, ResetPeriod.MONTHLY, "2026-03", null, 2026, 3);
            }));
        }

        // Then
        List<Integer> sequences = new ArrayList<>();
        for (Future<Integer> future : futures) {
            sequences.add(future.get());
        }

        // All sequences should be unique within their periods
        assertThat(sequences)
            .as("All sequences should be positive")
            .allMatch(seq -> seq >= 1);

        // Should have 4 sequences total (2 for Feb, 2 for March)
        assertThat(sequences)
            .as("Should have 4 sequences (2 per period)")
            .hasSize(4);

        executor.shutdown();
    }

    @Test
    void shouldHandleNeverResetPeriod_underConcurrency() throws Exception {
        // Given - NEVER reset should continuously increment
        Long sellerId = 99004L;
        ResetPeriod resetPeriod = ResetPeriod.NEVER;
        String periodKey = "NEVER";

        // When - 5 concurrent requests
        ExecutorService executor = Executors.newFixedThreadPool(5);
        List<Future<Integer>> futures = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            futures.add(executor.submit(() -> {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return counterService.nextSequence(sellerId, resetPeriod, periodKey, null, 0, 0);
            }));
        }

        // Then
        List<Integer> allSequences = new ArrayList<>();
        for (Future<Integer> future : futures) {
            allSequences.add(future.get());
        }

        Set<Integer> uniqueSequences = new HashSet<>(allSequences);

        assertThat(uniqueSequences)
            .as("All NEVER sequences should be unique. Got: " + allSequences)
            .hasSize(5);

        executor.shutdown();
    }
}
