package dev.robgro.timesheet.invoice;

import dev.robgro.timesheet.exception.BusinessRuleViolationException;
import dev.robgro.timesheet.exception.EntityNotFoundException;
import dev.robgro.timesheet.security.SecurityUtils;
import dev.robgro.timesheet.seller.Seller;
import dev.robgro.timesheet.seller.SellerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Orchestrator for invoice numbering scheme management.
 * NOT @Transactional at class level — createScheme() uses retry with REQUIRES_NEW,
 * which requires each attempt to be a separate transaction.
 * Read methods and archiveScheme() carry their own @Transactional annotations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceNumberingSchemeService {

    private final InvoiceNumberingSchemeTransactionalWorker worker;
    private final InvoiceNumberingSchemeRepository schemeRepository;
    private final SellerRepository sellerRepository;
    private final TemplateParser templateParser;
    private final InvoiceNumberingSchemeDtoMapper schemeMapper;

    /**
     * Create and activate a new numbering scheme for current seller.
     * Automatically archives existing active schemes.
     * Retries up to 2 times on version or active-scheme constraint collision.
     */
    public InvoiceNumberingSchemeDto createScheme(CreateSchemeRequest request) {
        templateParser.validateTemplate(request.template());

        Seller seller = getCurrentSeller();
        CreateSchemeCommand cmd = new CreateSchemeCommand(
            seller.getId(),
            request.template(),
            request.resetPeriod(),
            request.effectiveFrom()
        );

        int attempts = 0;
        while (true) {
            try {
                worker.createAndActivateOnce(cmd);
                break;
            } catch (DataIntegrityViolationException e) {
                attempts++;
                String constraint = ConstraintNameExtractor.tryGetConstraintName(e);
                boolean retryable =
                    "unique_seller_effective_version".equalsIgnoreCase(constraint) ||
                    "unique_active_scheme".equalsIgnoreCase(constraint);

                if (!retryable || attempts >= 2) {
                    log.warn("Scheme creation failed after {} attempt(s), constraint={}",
                        attempts, constraint, e);
                    throw new BusinessRuleViolationException(
                        "Another scheme was activated concurrently. Please refresh and try again."
                    );
                }
                log.warn("Version/active collision on attempt {}, constraint={}, retrying...",
                    attempts, constraint);
            }
        }

        return schemeRepository.findActiveBySeller(seller.getId()).stream()
            .filter(s -> s.getEffectiveFrom().equals(request.effectiveFrom()))
            .findFirst()
            .map(schemeMapper)
            .orElseThrow(() -> new EntityNotFoundException(
                "Scheme was saved but could not be retrieved — unexpected state"
            ));
    }

    /**
     * Get all schemes for current seller (for history view).
     */
    @Transactional(readOnly = true)
    public List<InvoiceNumberingSchemeDto> getAllSchemes() {
        Seller seller = getCurrentSeller();
        return schemeRepository.findAllBySeller(seller.getId()).stream()
            .map(schemeMapper)
            .collect(Collectors.toList());
    }

    /**
     * Get active schemes for current seller.
     */
    @Transactional(readOnly = true)
    public List<InvoiceNumberingSchemeDto> getActiveSchemes() {
        Seller seller = getCurrentSeller();
        return schemeRepository.findActiveBySeller(seller.getId()).stream()
            .map(schemeMapper)
            .collect(Collectors.toList());
    }

    /**
     * Archive a scheme by ID.
     */
    @Transactional
    public void archiveScheme(Long schemeId) {
        InvoiceNumberingScheme scheme = schemeRepository.findById(schemeId)
            .orElseThrow(() -> new EntityNotFoundException("Numbering scheme not found: " + schemeId));

        Seller currentSeller = getCurrentSeller();
        if (!scheme.getSeller().getId().equals(currentSeller.getId())) {
            throw new BusinessRuleViolationException(
                "Cannot archive scheme: you don't have permission to modify this scheme"
            );
        }

        scheme.archive();
        schemeRepository.save(scheme);

        log.info("Archived invoice numbering scheme: id={}, seller={}", schemeId, currentSeller.getId());
    }

    /**
     * Generate preview of template with example values.
     */
    public String previewTemplate(String template) {
        return templateParser.preview(template);
    }

    private Seller getCurrentSeller() {
        Long sellerId = SecurityUtils.getCurrentSellerId();
        if (sellerId == null) {
            throw new BusinessRuleViolationException(
                "Current user has no seller assigned. User must complete seller onboarding first."
            );
        }
        return sellerRepository.findById(sellerId)
            .orElseThrow(() -> new EntityNotFoundException("Seller not found: " + sellerId));
    }
}