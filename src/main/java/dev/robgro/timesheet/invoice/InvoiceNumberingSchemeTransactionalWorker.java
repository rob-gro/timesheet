package dev.robgro.timesheet.invoice;

import dev.robgro.timesheet.seller.Seller;
import dev.robgro.timesheet.seller.SellerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Transactional worker for creating and activating invoice numbering schemes.
 * Exists as a separate bean so that @Transactional(REQUIRES_NEW) is applied
 * through Spring AOP proxy — self-invocation would bypass the proxy.
 *
 * Responsibilities (single atomic attempt):
 * 1. Lock current ACTIVE scheme for same (seller, effectiveFrom) — prevents concurrent activation
 * 2. Archive ALL active schemes for this seller (one ACTIVE per seller, always)
 * 3. Calculate next version (max + 1)
 * 4. Create and save new ACTIVE scheme
 *
 * Retry logic and exception mapping live in InvoiceNumberingSchemeService (orchestrator).
 */
@Service
@RequiredArgsConstructor
@Slf4j
class InvoiceNumberingSchemeTransactionalWorker {

    private final InvoiceNumberingSchemeRepository schemeRepository;
    private final SellerRepository sellerRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void createAndActivateOnce(CreateSchemeCommand cmd) {
        // Step 1: Lock ACTIVE scheme for exact same date (concurrency protection)
        // If it exists, archive it. SELECT FOR UPDATE serializes concurrent requests.
        schemeRepository.findActiveForUpdate(cmd.sellerId(), cmd.effectiveFrom())
            .ifPresent(existing -> {
                existing.archive();
                log.debug("Locked and archived scheme id={} for seller={}, effectiveFrom={}",
                    existing.getId(), cmd.sellerId(), cmd.effectiveFrom());
            });

        // Step 2: Archive ALL remaining ACTIVE schemes for this seller
        // Business rule: one ACTIVE per seller at a time, regardless of effectiveFrom
        schemeRepository.findActiveBySeller(cmd.sellerId())
            .forEach(s -> {
                s.archive();
                log.debug("Auto-archived scheme id={} (effectiveFrom={}) due to new scheme",
                    s.getId(), s.getEffectiveFrom());
            });

        // Step 3: Calculate next version
        int nextVersion = schemeRepository.findMaxVersion(cmd.sellerId(), cmd.effectiveFrom()) + 1;

        // Step 4: Create new ACTIVE scheme
        Seller seller = sellerRepository.getReferenceById(cmd.sellerId());
        InvoiceNumberingScheme scheme = InvoiceNumberingScheme.create(
            seller,
            cmd.template(),
            cmd.resetPeriod(),
            cmd.effectiveFrom(),
            nextVersion
        );

        // saveAndFlush: force constraint check within this transaction (not at commit time)
        // Ensures DataIntegrityViolationException is catchable by the retry loop in orchestrator
        schemeRepository.saveAndFlush(scheme);

        log.info("Activated scheme: seller={}, effectiveFrom={}, version={}, template={}",
            cmd.sellerId(), cmd.effectiveFrom(), nextVersion, cmd.template());
    }
}