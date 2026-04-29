package dev.robgro.timesheet.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "scheduling.invoicing.enabled", havingValue = "true")
public class InvoicingScheduler {

    private final SellerScheduleConfigService configService;
    private final InvoicingReservationService reservationService;
    private final InvoicingExecutionService executionService;
    private final AdminNotificationService notificationService;

    @Scheduled(cron = "0 0 * * * ?")
    public void masterInvoicingTick() {
        List<SellerScheduleConfig> configs = configService.getAllEnabledConfigs();
        log.debug("Master invoicing tick: {} enabled sellers", configs.size());

        for (SellerScheduleConfig config : configs) {
            try {
                // Jitter 0–200ms — prevents thundering herd with many sellers
                Thread.sleep(ThreadLocalRandom.current().nextInt(0, 200));

                ZonedDateTime now = ZonedDateTime.now(ZoneId.of(config.getSeller().getTimezone()));
                if (!reservationService.shouldRunForSeller(config, now)) {
                    continue;
                }

                // tryReserve called on a different bean → Spring proxy active → @Transactional works
                reservationService.tryReserve(config.getId())
                        .ifPresent(executionService::executeAndRecord);

            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Error processing seller '{}' (id={}): {}. Skipping this tick.",
                        config.getSeller().getName(), config.getSeller().getId(), e.getMessage(), e);
                notificationService.sendErrorNotification(
                        "Scheduler tick error – seller: " + config.getSeller().getName(),
                        "Seller will be retried on next hourly tick.", e);
            }
        }
    }

    /**
     * Admin entry point — bypasses day/hour guard.
     * Called from ScheduleSettingsViewController via Optional<InvoicingScheduler>.
     * forceReserve() is @Transactional on a different bean → proxy works correctly.
     */
    public void forceReserveAndRun(Long configId, String adminUsername) {
        log.info("Admin '{}' manually triggered invoicing for config {}", adminUsername, configId);
        RunContext ctx = reservationService.forceReserve(configId);
        executionService.executeAndRecord(ctx);
    }
}