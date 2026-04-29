package dev.robgro.timesheet.scheduler;

import dev.robgro.timesheet.invoice.delivery.InvoiceDeliveryJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads scheduler run status including delivery job counts.
 */
@Service
@RequiredArgsConstructor
public class RunStatusService {

    private final SchedulerRunRepository schedulerRunRepository;
    private final InvoiceDeliveryJobRepository deliveryJobRepository;

    @Transactional(readOnly = true)
    public RunStatusDto getRunStatus(String runId) {
        SchedulerRun run = schedulerRunRepository.findById(runId)
                .orElseThrow(() -> new IllegalArgumentException("Run not found: " + runId));

        long pending = deliveryJobRepository.countByRunIdAndStatus(runId, "PENDING")
                     + deliveryJobRepository.countByRunIdAndStatus(runId, "PROCESSING");
        long sent = deliveryJobRepository.countByRunIdAndStatus(runId, "SENT");
        long failed = deliveryJobRepository.countByRunIdAndStatus(runId, "FAILED");

        return new RunStatusDto(run.getRunId(), run.getTriggeredBy(), run.getStartedAt(),
                pending, sent, failed);
    }
}