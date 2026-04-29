package dev.robgro.timesheet.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.time.Clock;

@Data
@Component
@ConfigurationProperties(prefix = "scheduling.invoicing")
public class InvoicingSchedulerProperties {
    private boolean enabled;
    private String adminEmail;
    private boolean sendSummaryEmail;
    private boolean sendEmptyClientWarning;
    private int stuckTimeoutMinutes = 90;

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}