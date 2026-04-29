package dev.robgro.timesheet.scheduler;

import dev.robgro.timesheet.seller.Seller;
import dev.robgro.timesheet.seller.SellerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.DateTimeException;
import java.util.List;

/**
 * Validates seller timezones on startup.
 *
 * <p>Invalid timezone in DB would cause {@link DateTimeException} on the first scheduler tick
 * for that seller, potentially silently skipping it. This validator surfaces the issue
 * immediately with an ERROR log and an admin email at application start.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SellerTimezoneStartupValidator implements ApplicationListener<ApplicationReadyEvent> {

    private final SellerRepository sellerRepository;
    private final AdminNotificationService notificationService;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        List<Seller> sellers = sellerRepository.findAll();
        List<String> invalid = sellers.stream()
                .filter(s -> !isValidTimezone(s.getTimezone()))
                .map(s -> "Seller[id=" + s.getId() + ", timezone='" + s.getTimezone() + "']")
                .toList();

        if (!invalid.isEmpty()) {
            log.error("INVALID TIMEZONE in DB — scheduler will crash on tick for these sellers: {}", invalid);
            notificationService.sendErrorNotification(
                    "STARTUP: " + invalid.size() + " seller(s) have invalid timezone",
                    "Sellers with invalid timezone (will fail on scheduler tick):\n"
                            + String.join("\n", invalid),
                    null);
        }
    }

    private boolean isValidTimezone(String tz) {
        if (tz == null || tz.isBlank()) return false;
        try {
            ZoneId.of(tz);
            return true;
        } catch (DateTimeException e) {
            return false;
        }
    }
}