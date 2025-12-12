package dev.robgro.timesheet.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "scheduling.invoicing")
public class InvoicingSchedulerProperties {
    private boolean enabled;
    private String cron;
    private String adminEmail;
    private boolean sendSummaryEmail;
    private boolean sendEmptyClientWarning;
}
