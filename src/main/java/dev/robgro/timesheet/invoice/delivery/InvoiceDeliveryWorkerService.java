package dev.robgro.timesheet.invoice.delivery;

import dev.robgro.timesheet.annotation.IoBoundary;
import dev.robgro.timesheet.config.DeliveryWorkerProperties;
import dev.robgro.timesheet.invoice.InvoiceDocumentService;
import dev.robgro.timesheet.invoice.PrintMode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Async invoice delivery worker.
 *
 * <p>Poll loop (every poll-interval-ms + jitter):
 * <ol>
 *   <li>Stale lock recovery — reset PROCESSING jobs with expired locks</li>
 *   <li>Atomic claim — single UPDATE grabs up to batch-size PENDING jobs</li>
 *   <li>Process each job: PDF + FTP + SMTP via dedicated I/O executor</li>
 *   <li>markSent / markRetryableFailure / markTerminalFailed in short TX</li>
 * </ol>
 *
 * <p>Worker respects {@code scheduling.delivery.enabled=false} for dev/test environments.
 */
@Service
@Slf4j
public class InvoiceDeliveryWorkerService {

    private final DeliveryClaimService claimService;
    private final InvoiceDeliveryJobRepository jobRepository;
    private final InvoiceDocumentService documentService;
    private final DeliveryWorkerProperties cfg;
    private final ThreadPoolExecutor ioExecutor;
    private final TransactionTemplate txTemplate;

    private final String workerId;

    public InvoiceDeliveryWorkerService(
            DeliveryClaimService claimService,
            InvoiceDeliveryJobRepository jobRepository,
            InvoiceDocumentService documentService,
            DeliveryWorkerProperties cfg,
            @Qualifier("deliveryIoExecutor") ThreadPoolExecutor ioExecutor,
            @Qualifier("transactionManager") PlatformTransactionManager txManager) {
        this.claimService = claimService;
        this.jobRepository = jobRepository;
        this.documentService = documentService;
        this.cfg = cfg;
        this.ioExecutor = ioExecutor;
        this.txTemplate = new TransactionTemplate(txManager);
        this.workerId = ManagementFactory.getRuntimeMXBean().getName();
    }

    @Scheduled(fixedDelayString = "${scheduling.delivery.poll-interval-ms:30000}")
    public void poll() {
        if (!cfg.isEnabled()) {
            return;
        }

        // Jitter: prevents thundering herd when multiple instances start simultaneously
        long jitter = ThreadLocalRandom.current().nextLong(0, Math.max(1, cfg.getPollJitterMs()));
        if (jitter > 0) {
            try {
                Thread.sleep(jitter);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }

        // Step 0: stale lock recovery (does NOT increment attempts)
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(cfg.getStaleLockMinutes());
        int recovered = jobRepository.recoverStaleLocks(cutoff);
        if (recovered > 0) {
            log.warn("DeliveryWorker: recovered {} stale locks (cutoff={})", recovered, cutoff);
        }

        // Step 1: atomic claim
        List<InvoiceDeliveryJob> jobs = claimService.claimBatch(workerId, cfg.getMaxAttempts(), cfg.getBatchSize());
        if (jobs.isEmpty()) {
            return;
        }

        log.info("DeliveryWorker: claimed {} jobs", jobs.size());

        // Step 2-3: process each job
        for (InvoiceDeliveryJob job : jobs) {
            processJob(job);
        }

        // Periodic stats log
        logStats();
    }

    private void processJob(InvoiceDeliveryJob job) {
        Long jobId = job.getId();
        Long invoiceId = job.getInvoice().getId();
        PrintMode printMode = PrintMode.ORIGINAL;

        log.info("DeliveryWorker: processing job {} for invoice {}", jobId, invoiceId);

        // Idempotency check: skip if already sent with same hash (crash-recovery)
        if (!job.isManual()) {
            String hash = computeDeliveryHash(invoiceId, printMode);
            long alreadySent = jobRepository.countSentByInvoiceIdAndHash(invoiceId, hash);
            if (alreadySent > 0) {
                log.warn("DeliveryWorker: job {} already sent (hash match), marking SENT without re-send", jobId);
                doMarkSent(jobId, invoiceId, printMode);
                return;
            }
        }

        long startMs = System.currentTimeMillis();

        try {
            CompletableFuture.runAsync(
                    () -> documentService.savePdfAndSendInvoice(invoiceId, printMode),
                    ioExecutor
            ).orTimeout(cfg.getIoTimeoutSeconds(), TimeUnit.SECONDS).get();

            long elapsedMs = System.currentTimeMillis() - startMs;
            log.info("DeliveryWorker: job {} completed in {}ms", jobId, elapsedMs);

            doMarkSent(jobId, invoiceId, printMode);

        } catch (RejectedExecutionException e) {
            // Executor queue full — must NOT leave job in PROCESSING
            log.error("DeliveryWorker: job {} rejected by executor (queue full), will retry", jobId);
            doMarkRetryableFailure(jobId, "executor_rejected: " + e.getMessage());

        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            log.error("DeliveryWorker: job {} failed: {}", jobId, cause.getMessage(), cause);

            if (job.canRetry(cfg.getMaxAttempts())) {
                doMarkRetryableFailure(jobId, cause.getMessage());
            } else {
                doMarkTerminalFailed(jobId, cause.getMessage());
            }
        }
    }

    // ===== Transactional mark operations (via TransactionTemplate — proxy-safe) =====

    private void doMarkSent(Long jobId, Long invoiceId, PrintMode printMode) {
        txTemplate.executeWithoutResult(status -> {
            InvoiceDeliveryJob fresh = jobRepository.findById(jobId).orElseThrow();
            String hash = computeDeliveryHash(invoiceId, printMode);
            fresh.markSent(hash);
            jobRepository.save(fresh);
            log.debug("DeliveryWorker: job {} marked SENT (hash={})", jobId, hash);
        });
    }

    private void doMarkRetryableFailure(Long jobId, String error) {
        txTemplate.executeWithoutResult(status -> {
            InvoiceDeliveryJob fresh = jobRepository.findById(jobId).orElseThrow();
            // attempts is incremented inside markRetryableFailure()
            int nextAttemptIndex = fresh.getAttempts(); // pre-increment value = index for backoff
            LocalDateTime nextRetry = computeNextRetryAt(nextAttemptIndex);
            fresh.markRetryableFailure(error, nextRetry);
            jobRepository.save(fresh);
            log.info("DeliveryWorker: job {} will retry at {} (attempt {})",
                    jobId, nextRetry, fresh.getAttempts());
        });
    }

    private void doMarkTerminalFailed(Long jobId, String error) {
        txTemplate.executeWithoutResult(status -> {
            InvoiceDeliveryJob fresh = jobRepository.findById(jobId).orElseThrow();
            fresh.markTerminalFailed(error);
            jobRepository.save(fresh);
            log.error("DeliveryWorker: job {} TERMINAL FAILED after {} attempts", jobId, fresh.getAttempts());
        });
    }

    // ===== Helpers =====

    /**
     * Compute delivery hash: SHA-256(invoiceId | invoiceNumber-as-proxy | printMode).
     * Identifies a unique delivery attempt — same invoice + same printMode = same hash.
     * Written only in markSent(); used for crash-recovery idempotency.
     */
    private String computeDeliveryHash(Long invoiceId, PrintMode printMode) {
        // v1 = algorithm version; change to v2 when hash inputs change to force re-delivery
        String input = invoiceId + "|v1|" + printMode.name();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    /**
     * Compute next retry timestamp with jitter.
     *
     * <p>Attempt index mapping (after markRetryableFailure increments attempts):
     * attempts=1 → backoff[0]=1min, attempts=2 → backoff[1]=15min, attempts=3 → backoff[2]=60min.
     *
     * @param preIncrementAttempts attempts value BEFORE markRetryableFailure increments it
     */
    private LocalDateTime computeNextRetryAt(int preIncrementAttempts) {
        List<Integer> backoff = cfg.getRetryBackoffMinutes();
        int index = Math.min(preIncrementAttempts, backoff.size() - 1);
        int backoffMinutes = backoff.get(index);
        long jitterSeconds = ThreadLocalRandom.current().nextLong(0, 31); // 0..30s
        return LocalDateTime.now().plusMinutes(backoffMinutes).plusSeconds(jitterSeconds);
    }

    private void logStats() {
        log.debug("DeliveryWorker: poll complete (workerId={})", workerId);
    }
}