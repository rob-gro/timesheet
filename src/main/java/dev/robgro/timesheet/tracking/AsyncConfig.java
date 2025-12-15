package dev.robgro.timesheet.tracking;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Async configuration for email tracking notifications
 *
 * Configures a dedicated thread pool for sending tracking notification emails
 * without blocking the tracking pixel response.
 */
@Configuration
@EnableAsync
@Slf4j
public class AsyncConfig {

    /**
     * Custom executor for async email notifications
     *
     * Thread pool configuration:
     * - Core pool: 2 threads (sufficient for ~8 clients with occasional opens)
     * - Max pool: 5 threads (handles burst of simultaneous opens)
     * - Queue: 25 tasks (prevents memory issues during high load)
     * - Thread naming: "email-tracking-" prefix for easy log filtering
     *
     * When queue is full, CallerRunsPolicy executes task in calling thread
     * (blocks pixel response but prevents task rejection)
     */
    @Bean(name = "emailTrackingExecutor")
    public Executor emailTrackingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // Core pool size - number of threads to keep alive
        executor.setCorePoolSize(2);

        // Maximum pool size - max threads when queue is full
        executor.setMaxPoolSize(5);

        // Queue capacity - max waiting tasks before creating new threads
        executor.setQueueCapacity(25);

        // Thread naming pattern for logging/debugging
        executor.setThreadNamePrefix("email-tracking-");

        // Rejection policy - run in calling thread if pool + queue full
        // This ensures notifications are never lost (at cost of blocking)
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());

        // Wait for tasks to complete on shutdown
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);

        executor.initialize();

        log.info("Initialized email tracking executor: core={}, max={}, queue={}",
                executor.getCorePoolSize(),
                executor.getMaxPoolSize(),
                executor.getQueueCapacity());

        return executor;
    }
}
