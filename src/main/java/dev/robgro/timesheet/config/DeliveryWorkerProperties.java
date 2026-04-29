package dev.robgro.timesheet.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.springframework.context.annotation.Bean;

import jakarta.annotation.PreDestroy;

/**
 * Configuration for the async invoice delivery worker.
 * Bound to prefix {@code scheduling.delivery}.
 */
@Data
@Component
@ConfigurationProperties(prefix = "scheduling.delivery")
public class DeliveryWorkerProperties {

    private boolean enabled = true;
    private long pollIntervalMs = 30000;
    private long pollJitterMs = 5000;
    private int batchSize = 5;
    private int maxParallelIo = 4;
    private int maxAttempts = 3;
    /** Backoff per attempt: attempt 1 → index 0, attempt 2 → index 1, etc. */
    private List<Integer> retryBackoffMinutes = List.of(1, 15, 60);
    private int staleLockMinutes = 10;
    private int ioTimeoutSeconds = 60;

    private ThreadPoolExecutor deliveryIoExecutorInstance;

    /**
     * Bounded thread pool for I/O operations (PDF generation, FTP, SMTP).
     *
     * <p>Bounded queue (100): backpressure when SMTP/FTP hangs —
     * new jobs get RejectedExecutionException → markRetryableFailure → retry.
     *
     * <p>AbortPolicy: worker handles RejectedExecutionException explicitly
     * (must NOT leave job in PROCESSING state).
     *
     * <p>@PreDestroy: graceful shutdown on JVM restart — prevents thread leaks.
     */
    @Bean("deliveryIoExecutor")
    public ThreadPoolExecutor deliveryIoExecutor() {
        deliveryIoExecutorInstance = new ThreadPoolExecutor(
                maxParallelIo,
                maxParallelIo,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(100),
                new ThreadPoolExecutor.AbortPolicy()
        );
        return deliveryIoExecutorInstance;
    }

    @PreDestroy
    public void shutdownDeliveryExecutor() {
        if (deliveryIoExecutorInstance != null) {
            deliveryIoExecutorInstance.shutdown();
            try {
                deliveryIoExecutorInstance.awaitTermination(30, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}