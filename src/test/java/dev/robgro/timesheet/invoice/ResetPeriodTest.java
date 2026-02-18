package dev.robgro.timesheet.invoice;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ResetPeriodTest {

    @Test
    void shouldReturnActualMonthForMonthly() {
        // Given
        LocalDate januaryDate = LocalDate.of(2026, 1, 15);
        LocalDate decemberDate = LocalDate.of(2026, 12, 25);

        // When
        int januaryMonth = ResetPeriod.MONTHLY.getPeriodMonth(januaryDate);
        int decemberMonth = ResetPeriod.MONTHLY.getPeriodMonth(decemberDate);

        // Then
        assertThat(januaryMonth).isEqualTo(1);
        assertThat(decemberMonth).isEqualTo(12);
    }

    @Test
    void shouldReturnZeroForYearly() {
        // Given
        LocalDate anyDate = LocalDate.of(2026, 6, 15);

        // When
        int month = ResetPeriod.YEARLY.getPeriodMonth(anyDate);

        // Then
        assertThat(month).isEqualTo(0);
    }

    @Test
    void shouldReturnZeroForNever() {
        // Given
        LocalDate anyDate = LocalDate.of(2026, 6, 15);

        // When
        int month = ResetPeriod.NEVER.getPeriodMonth(anyDate);

        // Then
        assertThat(month).isEqualTo(0);
    }

    @Test
    void shouldRequireMonthComponentForMonthly() {
        // When & Then
        assertThat(ResetPeriod.MONTHLY.requiresMonthComponent()).isTrue();
    }

    @Test
    void shouldNotRequireMonthComponentForYearly() {
        // When & Then
        assertThat(ResetPeriod.YEARLY.requiresMonthComponent()).isFalse();
    }

    @Test
    void shouldNotRequireMonthComponentForNever() {
        // When & Then
        assertThat(ResetPeriod.NEVER.requiresMonthComponent()).isFalse();
    }

    @Test
    void shouldHaveDescription() {
        // When & Then
        assertThat(ResetPeriod.MONTHLY.getDescription()).isNotBlank();
        assertThat(ResetPeriod.YEARLY.getDescription()).isNotBlank();
        assertThat(ResetPeriod.NEVER.getDescription()).isNotBlank();
    }
}
