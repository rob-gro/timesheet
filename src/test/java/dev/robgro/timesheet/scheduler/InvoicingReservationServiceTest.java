package dev.robgro.timesheet.scheduler;

import dev.robgro.timesheet.seller.Seller;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static dev.robgro.timesheet.scheduler.ScheduleRunStatus.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Tests for InvoicingReservationService.shouldRunForSeller().
 *
 * Key scenario: after deploying feature/cron-ui-per-seller-schedule to Heroku,
 * the new seller_schedule_config table is populated by V34 migration.
 * Without the fix, last_run_period=NULL causes a spurious March run on deploy day.
 * With the fix, last_run_period pre-set to "2026-03"/SUCCESS blocks the spurious run,
 * and May 1st correctly triggers the April invoicing.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("InvoicingReservationService - shouldRunForSeller")
class InvoicingReservationServiceTest {

    @Mock private SellerScheduleConfigRepository configRepository;
    @Mock private SellerScheduleConfigService configService;
    @Mock private Clock clock;
    @Mock private Seller seller;

    private InvoicingReservationService reservationService;
    private SellerScheduleConfig config;

    // April 29, 2026 at 13:00 — deploy day, after scheduledHour=12
    private static final ZonedDateTime APRIL_29_13H =
            ZonedDateTime.of(2026, 4, 29, 13, 0, 0, 0, ZoneId.of("Europe/Warsaw"));

    // May 1, 2026 at 12:00 — expected April invoicing run
    private static final ZonedDateTime MAY_1_12H =
            ZonedDateTime.of(2026, 5, 1, 12, 0, 0, 0, ZoneId.of("Europe/Warsaw"));

    // May 1, 2026 at 11:00 — before scheduledHour
    private static final ZonedDateTime MAY_1_11H =
            ZonedDateTime.of(2026, 5, 1, 11, 0, 0, 0, ZoneId.of("Europe/Warsaw"));

    // May 2, 2026 at 12:00 — day after April run completed
    private static final ZonedDateTime MAY_2_12H =
            ZonedDateTime.of(2026, 5, 2, 12, 0, 0, 0, ZoneId.of("Europe/Warsaw"));

    @BeforeEach
    void setUp() {
        reservationService = new InvoicingReservationService(configRepository, configService, clock);
        config = new SellerScheduleConfig();
        config.setSeller(seller);
        config.setScheduledDay(1);
        config.setScheduledHour(12);
        config.setEnabled(true);
    }

    @Nested
    @DisplayName("V34 deploy safety — spurious run prevention")
    class DeploySpuriousRunTests {

        @Test
        @DisplayName("Should NOT fire when V34 pre-sets last_run_period=2026-03 SUCCESS (fixed migration)")
        void shouldNotFire_whenLastRunPeriodPreSetToSuccessOnDeploy() {
            // given — V34 fixed: INSERT includes last_run_period=previous month + last_run_status=SUCCESS
            config.setLastRunPeriod("2026-03");
            config.setLastRunStatus(SUCCESS);

            // when — first scheduler tick after deploy on April 29
            boolean result = reservationService.shouldRunForSeller(config, APRIL_29_13H);

            // then — no spurious March run
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should fire spuriously when last_run_period is NULL (unfixed migration — demonstrates the bug)")
        void shouldFireSpuriously_whenLastRunPeriodIsNullAfterDeploy() {
            // given — V34 unfixed: last_run_period not set → NULL
            config.setLastRunPeriod(null);
            config.setLastRunStatus(null);

            // when — April 29, day=29 >= scheduledDay=1, hour=13 >= scheduledHour=12
            boolean result = reservationService.shouldRunForSeller(config, APRIL_29_13H);

            // then — spurious March run triggers (NULL bypasses idempotency check)
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should NOT fire on April 29 before scheduledHour even with NULL last_run_period")
        void shouldNotFire_beforeScheduledHourOnDeployDay() {
            // given — unfixed migration, but deploy happened at 11:00
            config.setLastRunPeriod(null);
            config.setLastRunStatus(null);
            ZonedDateTime april29At11 = ZonedDateTime.of(2026, 4, 29, 11, 0, 0, 0, ZoneId.of("Europe/Warsaw"));

            // when
            boolean result = reservationService.shouldRunForSeller(config, april29At11);

            // then — hour 11 < scheduledHour 12 → no run
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("Normal scheduling — May 1st April invoices")
    class NormalSchedulingTests {

        @Test
        @DisplayName("Should fire on May 1st 12:00 for April invoices after V34-fixed deploy")
        void shouldFireOnMay1st_whenLastRunPeriodIsMarchwithSuccess() {
            // given — state after fixed V34 deploy: last run = 2026-03 SUCCESS
            config.setLastRunPeriod("2026-03");
            config.setLastRunStatus(SUCCESS);

            // when — May 1st 12:00, previousMonth=2026-04 ≠ "2026-03" → day/hour check
            boolean result = reservationService.shouldRunForSeller(config, MAY_1_12H);

            // then — April invoicing fires correctly
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should NOT fire on May 1st before scheduledHour")
        void shouldNotFire_beforeScheduledHourOnMay1st() {
            // given
            config.setLastRunPeriod("2026-03");
            config.setLastRunStatus(SUCCESS);

            // when — 11:00 < scheduledHour 12
            boolean result = reservationService.shouldRunForSeller(config, MAY_1_11H);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should NOT fire again on May 2nd when April already completed with SUCCESS")
        void shouldNotFire_whenAprilAlreadySuccess() {
            // given — May 1st run completed, last_run_period advanced to 2026-04
            config.setLastRunPeriod("2026-04");
            config.setLastRunStatus(SUCCESS);

            // when — May 2nd tick
            boolean result = reservationService.shouldRunForSeller(config, MAY_2_12H);

            // then — idempotency guard blocks re-run
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should NOT fire on May 2nd when April completed with PARTIAL_FAIL")
        void shouldNotFire_whenAprilPartialFail() {
            // given — some invoices failed but run is considered done
            config.setLastRunPeriod("2026-04");
            config.setLastRunStatus(PARTIAL_FAIL);

            boolean result = reservationService.shouldRunForSeller(config, MAY_2_12H);

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("Retry logic")
    class RetryLogicTests {

        @Test
        @DisplayName("Should allow one automatic retry after FAIL")
        void shouldAllowRetry_whenRunRetryCountIsZeroAndStatusFail() {
            // given — March run failed, no retry used yet
            config.setLastRunPeriod("2026-03");
            config.setLastRunStatus(FAIL);
            config.setRunRetryCount(0);

            boolean result = reservationService.shouldRunForSeller(config, APRIL_29_13H);

            // then — one retry allowed
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should block further retries when runRetryCount >= 1")
        void shouldBlockRetry_whenRetryAlreadyUsed() {
            // given — retry already consumed
            config.setLastRunPeriod("2026-03");
            config.setLastRunStatus(FAIL);
            config.setRunRetryCount(1);

            boolean result = reservationService.shouldRunForSeller(config, APRIL_29_13H);

            // then — gate closed, admin must force manually
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should clamp scheduledDay=31 to February length (28) — fires on Feb 28+")
        void shouldClampScheduledDay_whenPreviousMonthIsShorter() {
            // given — seller configured for day 31, previous month = February 2026 (28 days)
            config.setScheduledDay(31);
            config.setScheduledHour(12);
            config.setLastRunPeriod(null);
            // now = March 1, 2026 12:00 (previousMonth = Feb 2026, 28 days → effectiveDay=28)
            // day 1 < effectiveDay 28 → should NOT fire
            ZonedDateTime march1 = ZonedDateTime.of(2026, 3, 1, 12, 0, 0, 0, ZoneId.of("Europe/Warsaw"));

            boolean result = reservationService.shouldRunForSeller(config, march1);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should clamp scheduledDay=31 — fires on March 28 (effectiveDay=28 for Feb)")
        void shouldClampScheduledDay_andFireOnEffectiveDay() {
            // given — same config, but now = March 28 >= effectiveDay 28
            config.setScheduledDay(31);
            config.setScheduledHour(12);
            config.setLastRunPeriod(null);
            ZonedDateTime march28 = ZonedDateTime.of(2026, 3, 28, 12, 0, 0, 0, ZoneId.of("Europe/Warsaw"));

            boolean result = reservationService.shouldRunForSeller(config, march28);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should respect seller timezone — UTC+2 seller fires 2h before UTC seller")
        void shouldRespectTimezone_warsawFiresBeforeUtc() {
            // given — scheduledHour=12, last_run_period=null
            // now in Warsaw = 12:00 (UTC = 10:00) → Warsaw seller fires, UTC seller would not
            config.setLastRunPeriod(null);
            config.setScheduledHour(12);
            ZonedDateTime warsawNoon = ZonedDateTime.of(2026, 4, 29, 12, 0, 0, 0, ZoneId.of("Europe/Warsaw"));
            ZonedDateTime utc10h = warsawNoon.withZoneSameInstant(ZoneId.of("UTC"));

            boolean warsawResult = reservationService.shouldRunForSeller(config, warsawNoon);
            // UTC seller: hour=10 < scheduledHour=12 → false
            config.setScheduledHour(12);
            boolean utcResult = reservationService.shouldRunForSeller(config, utc10h);

            assertThat(warsawResult).isTrue();
            assertThat(utcResult).isFalse();
        }

        @Test
        @DisplayName("Should NOT fire when scheduledHour=23 and current hour=22")
        void shouldNotFire_whenScheduledHourNotYetReached() {
            // given — seller wants invoicing at 23:00
            config.setScheduledDay(1);
            config.setScheduledHour(23);
            config.setLastRunPeriod(null);
            ZonedDateTime may1At22 = ZonedDateTime.of(2026, 5, 1, 22, 0, 0, 0, ZoneId.of("Europe/Warsaw"));

            boolean result = reservationService.shouldRunForSeller(config, may1At22);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should fire when scheduledHour=23 and current hour=23")
        void shouldFire_whenScheduledHourExactlyMatches() {
            // given
            config.setScheduledDay(1);
            config.setScheduledHour(23);
            config.setLastRunPeriod(null);
            ZonedDateTime may1At23 = ZonedDateTime.of(2026, 5, 1, 23, 0, 0, 0, ZoneId.of("Europe/Warsaw"));

            boolean result = reservationService.shouldRunForSeller(config, may1At23);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should skip IN_PROGRESS run started less than 90 min ago")
        void shouldSkip_whenRunInProgressAndNotStuck() {
            // given — run started 30 min ago (well within 90 min window)
            config.setLastRunPeriod("2026-03");
            config.setLastRunStatus(IN_PROGRESS);
            LocalDateTime thirtyMinAgo = LocalDateTime.of(2026, 4, 29, 12, 30, 0);
            config.setRunStartedAt(thirtyMinAgo);
            LocalDateTime nowUtc = LocalDateTime.of(2026, 4, 29, 13, 0, 0);
            when(clock.instant()).thenReturn(nowUtc.atZone(ZoneId.of("UTC")).toInstant());
            when(clock.getZone()).thenReturn(ZoneId.of("UTC"));

            boolean result = reservationService.shouldRunForSeller(config, APRIL_29_13H);

            // then — not stuck yet, skip
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should override stuck IN_PROGRESS run started more than 90 min ago")
        void shouldOverride_whenRunStuckInProgressOver90Min() {
            // given — run started 2h ago (stuck)
            config.setLastRunPeriod("2026-03");
            config.setLastRunStatus(IN_PROGRESS);
            LocalDateTime twoHoursAgo = LocalDateTime.of(2026, 4, 29, 11, 0, 0);
            config.setRunStartedAt(twoHoursAgo);
            LocalDateTime nowUtc = LocalDateTime.of(2026, 4, 29, 13, 0, 0);
            when(seller.getName()).thenReturn("Test Seller");
            when(clock.instant()).thenReturn(nowUtc.atZone(ZoneId.of("UTC")).toInstant());
            when(clock.getZone()).thenReturn(ZoneId.of("UTC"));

            boolean result = reservationService.shouldRunForSeller(config, APRIL_29_13H);

            // then — stuck override: falls through to day/hour check → fires
            assertThat(result).isTrue();
        }
    }
}