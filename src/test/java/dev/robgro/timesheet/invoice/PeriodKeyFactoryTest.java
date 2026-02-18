package dev.robgro.timesheet.invoice;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class PeriodKeyFactoryTest {

    private PeriodKeyFactory factory;

    @BeforeEach
    void setUp() {
        factory = new PeriodKeyFactory();
    }

    // ----- MONTHLY Tests -----

    @Test
    void shouldBuildMonthlyPeriodKey_whenValidYearAndMonth() {
        // Given
        ResetPeriod resetPeriod = ResetPeriod.MONTHLY;
        int year = 2026;
        int month = 2;

        // When
        String periodKey = factory.build(resetPeriod, year, month);

        // Then
        assertThat(periodKey).isEqualTo("2026-02");
    }

    @Test
    void shouldPadSingleDigitMonth_whenMonthlyPeriodKey() {
        // Given - January (month 1)
        String periodKey = factory.build(ResetPeriod.MONTHLY, 2026, 1);

        // Then - should be zero-padded
        assertThat(periodKey).isEqualTo("2026-01");
    }

    @Test
    void shouldHandleDecember_whenMonthlyPeriodKey() {
        // Given - December (month 12)
        String periodKey = factory.build(ResetPeriod.MONTHLY, 2026, 12);

        // Then
        assertThat(periodKey).isEqualTo("2026-12");
    }

    @Test
    void shouldThrowException_whenMonthlyWithMonthZero() {
        // Given - MONTHLY with invalid month=0
        ResetPeriod resetPeriod = ResetPeriod.MONTHLY;

        // When/Then - should throw IllegalStateException
        assertThatThrownBy(() -> factory.build(resetPeriod, 2026, 0))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("MONTHLY reset cannot have month=0")
            .hasMessageContaining("counter drift");
    }

    // ----- YEARLY Tests -----

    @Test
    void shouldBuildYearlyPeriodKey_whenValidYear() {
        // Given
        ResetPeriod resetPeriod = ResetPeriod.YEARLY;
        int year = 2026;

        // When
        String periodKey = factory.build(resetPeriod, year, 0);

        // Then
        assertThat(periodKey).isEqualTo("2026");
    }

    @Test
    void shouldIgnoreMonth_whenYearlyPeriodKey() {
        // Given - YEARLY with month=0 (ignored)
        String periodKey1 = factory.build(ResetPeriod.YEARLY, 2026, 0);
        String periodKey2 = factory.build(ResetPeriod.YEARLY, 2026, 5); // month ignored

        // Then - both produce same key
        assertThat(periodKey1).isEqualTo("2026");
        assertThat(periodKey2).isEqualTo("2026");
    }

    @Test
    void shouldHandleFourDigitYear_whenYearlyPeriodKey() {
        // Given - year with 4 digits
        String periodKey = factory.build(ResetPeriod.YEARLY, 2025, 0);

        // Then
        assertThat(periodKey).isEqualTo("2025");
    }

    // ----- NEVER Tests -----

    @Test
    void shouldBuildNeverPeriodKey_whenNeverReset() {
        // Given
        ResetPeriod resetPeriod = ResetPeriod.NEVER;

        // When
        String periodKey = factory.build(resetPeriod, 0, 0);

        // Then
        assertThat(periodKey).isEqualTo("NEVER");
    }

    @Test
    void shouldIgnoreYearAndMonth_whenNeverPeriodKey() {
        // Given - NEVER ignores year and month
        String periodKey1 = factory.build(ResetPeriod.NEVER, 0, 0);
        String periodKey2 = factory.build(ResetPeriod.NEVER, 2026, 5);
        String periodKey3 = factory.build(ResetPeriod.NEVER, 9999, 12);

        // Then - all produce "NEVER"
        assertThat(periodKey1).isEqualTo("NEVER");
        assertThat(periodKey2).isEqualTo("NEVER");
        assertThat(periodKey3).isEqualTo("NEVER");
    }

    // ----- V27 Backfill Compatibility Tests -----

    @Test
    void shouldMatchV27BackfillFormat_forMonthly() {
        // V27 SQL: CONCAT(period_year, '-', LPAD(period_month, 2, '0'))
        // Example: 2026 + '-' + LPAD(2, 2, '0') = '2026-02'

        String periodKey = factory.build(ResetPeriod.MONTHLY, 2026, 2);

        assertThat(periodKey)
            .as("Must match V27 backfill SQL format for MONTHLY")
            .isEqualTo("2026-02");
    }

    @Test
    void shouldMatchV27BackfillFormat_forYearly() {
        // V27 SQL: CAST(period_year AS CHAR)
        // Example: CAST(2026 AS CHAR) = '2026'

        String periodKey = factory.build(ResetPeriod.YEARLY, 2026, 0);

        assertThat(periodKey)
            .as("Must match V27 backfill SQL format for YEARLY")
            .isEqualTo("2026");
    }

    @Test
    void shouldMatchV27BackfillFormat_forNever() {
        // V27 SQL: 'NEVER' (literal)

        String periodKey = factory.build(ResetPeriod.NEVER, 0, 0);

        assertThat(periodKey)
            .as("Must match V27 backfill SQL format for NEVER")
            .isEqualTo("NEVER");
    }
}
