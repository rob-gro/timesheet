package dev.robgro.timesheet.invoice.delivery;

import dev.robgro.timesheet.exception.EntityNotFoundException;
import dev.robgro.timesheet.invoice.Invoice;
import dev.robgro.timesheet.invoice.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceDeliveryJobServiceImpl implements InvoiceDeliveryJobService {

    private final InvoiceDeliveryJobRepository jobRepository;
    private final InvoiceRepository invoiceRepository;

    @Override
    @Transactional(readOnly = true)
    public InvoiceDeliveryJobDto getDeliveryStatus(Long invoiceId) {
        return jobRepository.findByInvoiceIdAndIsActive(invoiceId, 1)
                .or(() -> jobRepository.findTopByInvoiceIdOrderByCreatedAtDesc(invoiceId))
                .map(this::toDto)
                .orElse(null);
    }

    @Override
    @Transactional
    public InvoiceDeliveryJobDto requestDelivery(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new EntityNotFoundException("Invoice", invoiceId));

        // Check for active job (PENDING or PROCESSING, is_active=1)
        Optional<InvoiceDeliveryJob> activeJob = jobRepository.findByInvoiceIdAndIsActive(invoiceId, 1);
        if (activeJob.isPresent()) {
            log.debug("Delivery already active for invoice {}, status={}", invoiceId, activeJob.get().getStatus());
            return toDto(activeJob.get());
        }

        // No active job — inspect latest historical record
        Optional<InvoiceDeliveryJob> latestJob = jobRepository.findTopByInvoiceIdOrderByCreatedAtDesc(invoiceId);
        if (latestJob.isPresent()) {
            InvoiceDeliveryJob job = latestJob.get();

            if (DeliveryStatus.FAILED.name().equals(job.getStatus())) {
                // Reset FAILED job for retry
                log.info("Resetting FAILED delivery job {} for invoice {}", job.getId(), invoiceId);
                job.resetForRetry();
                return toDto(jobRepository.save(job));
            }

            if (DeliveryStatus.SENT.name().equals(job.getStatus())) {
                // User wants a deliberate resend — create new manual job
                log.info("Creating manual resend job for already-sent invoice {}", invoiceId);
                InvoiceDeliveryJob manualJob = InvoiceDeliveryJob.createManualResend(invoice);
                return toDto(jobRepository.save(manualJob));
            }
        }

        // No previous job — create first PENDING job
        log.info("Creating delivery job for invoice {}", invoiceId);
        InvoiceDeliveryJob job = InvoiceDeliveryJob.createPending(invoice);
        return toDto(jobRepository.save(job));
    }

    private InvoiceDeliveryJobDto toDto(InvoiceDeliveryJob job) {
        return new InvoiceDeliveryJobDto(
                job.getId(),
                job.getInvoice().getId(),
                job.getStatus(),
                job.getAttempts(),
                job.getLastError(),
                job.getCreatedAt()
        );
    }
}