package dev.robgro.timesheet.scheduler;

import dev.robgro.timesheet.exception.EntityNotFoundException;
import dev.robgro.timesheet.seller.Seller;
import dev.robgro.timesheet.seller.SellerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.DateTimeException;
import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class SellerScheduleConfigServiceImpl implements SellerScheduleConfigService {

    private final SellerScheduleConfigRepository configRepository;
    private final SellerRepository sellerRepository;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public SellerScheduleConfigDto getConfigForSeller(Long sellerId) {
        SellerScheduleConfig config = getOrCreateConfig(sellerId);
        return toDto(config);
    }

    @Override
    @Transactional
    public SellerScheduleConfigDto saveConfig(Long sellerId, SaveScheduleConfigRequest request) {
        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new EntityNotFoundException("Seller", sellerId));

        // Canonicalize + validate timezone — catches aliases and invalid strings
        String canonicalTimezone;
        try {
            canonicalTimezone = ZoneId.of(request.timezone()).getId();
        } catch (DateTimeException e) {
            throw new IllegalArgumentException("Invalid timezone: " + request.timezone(), e);
        }

        SellerScheduleConfig config = getOrCreateConfig(sellerId);
        config.setScheduledDay(request.scheduledDay());
        config.setScheduledHour(request.scheduledHour());
        config.setEnabled(request.enabled());

        // Atomic: save both config and seller timezone in the same TX
        seller.setTimezone(canonicalTimezone);
        sellerRepository.save(seller);
        configRepository.save(config);

        log.info("Saved schedule config for seller {} (day={}, hour={}, tz={}, enabled={})",
                sellerId, request.scheduledDay(), request.scheduledHour(), canonicalTimezone, request.enabled());

        return toDto(config);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SellerScheduleConfig> getAllEnabledConfigs() {
        return configRepository.findAllEnabledWithSeller();
    }

    @Override
    @Transactional(readOnly = true)
    public ZonedDateTime calculateNextRun(SellerScheduleConfig config) {
        ZoneId zone = ZoneId.of(config.getSeller().getTimezone());
        ZonedDateTime now = ZonedDateTime.now(clock.withZone(zone));

        YearMonth billingPeriod = YearMonth.from(now).minusMonths(1);
        String billingPeriodStr = billingPeriod.toString();

        // runScheduleMonth: which calendar month the next tick fires in
        YearMonth runScheduleMonth = billingPeriodStr.equals(config.getLastRunPeriod())
                ? YearMonth.from(now).plusMonths(1)  // billing done → next run in following month
                : YearMonth.from(now);               // billing pending → run this month

        int effectiveDay = Math.min(config.getScheduledDay(), runScheduleMonth.lengthOfMonth());
        ZonedDateTime candidate = runScheduleMonth.atDay(effectiveDay)
                .atTime(config.getScheduledHour(), 0)
                .atZone(zone);

        // Log DST normalization so support is not surprised by UI showing different hour
        if (candidate.getHour() != config.getScheduledHour()) {
            log.info("DST normalization for seller '{}': scheduled hour={}, actual run hour={} (zone={})",
                    config.getSeller().getName(), config.getScheduledHour(),
                    candidate.getHour(), config.getSeller().getTimezone());
        }

        return candidate;
    }

    @Override
    public void updateRunResult(Long configId, ScheduleRunStatus status, int success, int fail,
                                String errorSummary, LocalDateTime nowUtc) {
        SellerScheduleConfig config = configRepository.findById(configId)
                .orElseThrow(() -> new EntityNotFoundException("SellerScheduleConfig", configId));
        config.updateRunResult(status, success, fail, errorSummary, nowUtc);
        configRepository.save(config);
    }

    // ===== Helpers =====

    /**
     * Get config for seller, lazily creating a default if not found.
     * Race condition: two concurrent requests may try to INSERT simultaneously.
     * Handled by catching DataIntegrityViolationException (unique_seller_schedule constraint).
     */
    private SellerScheduleConfig getOrCreateConfig(Long sellerId) {
        return configRepository.findBySellerId(sellerId)
                .orElseGet(() -> createDefaultConfig(sellerId));
    }

    private SellerScheduleConfig createDefaultConfig(Long sellerId) {
        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new EntityNotFoundException("Seller", sellerId));

        SellerScheduleConfig config = new SellerScheduleConfig();
        config.setSeller(seller);
        config.setScheduledDay(1);
        config.setScheduledHour(12);
        config.setEnabled(true);

        try {
            return configRepository.save(config);
        } catch (DataIntegrityViolationException e) {
            // Another instance inserted concurrently — reload from DB
            log.debug("Concurrent config creation for seller {} — reloading from DB", sellerId);
            return configRepository.findBySellerId(sellerId).orElseThrow();
        }
    }

    private SellerScheduleConfigDto toDto(SellerScheduleConfig config) {
        return new SellerScheduleConfigDto(
                config.getId(),
                config.getSeller().getId(),
                config.getSeller().getName(),
                config.getScheduledDay(),
                config.getScheduledHour(),
                config.getSeller().getTimezone(),
                config.isEnabled(),
                config.getLastRunAt(),
                config.getRunStartedAt(),
                config.getLastRunPeriod(),
                config.getLastRunStatus(),
                config.getLastRunSuccessCount(),
                config.getLastRunFailCount(),
                config.getLastErrorSummary(),
                config.getRunRetryCount(),
                calculateNextRun(config)
        );
    }
}