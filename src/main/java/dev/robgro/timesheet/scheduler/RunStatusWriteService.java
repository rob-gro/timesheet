package dev.robgro.timesheet.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates and updates scheduler run records.
 */
@Service
@RequiredArgsConstructor
public class RunStatusWriteService {

    private final SchedulerRunRepository schedulerRunRepository;

    @Transactional
    public void createRun(String runId, String triggeredBy) {
        schedulerRunRepository.save(SchedulerRun.create(runId, triggeredBy));
    }
}