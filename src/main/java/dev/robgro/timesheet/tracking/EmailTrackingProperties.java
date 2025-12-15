package dev.robgro.timesheet.tracking;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.email-tracking")
@Getter
@Setter
public class EmailTrackingProperties {

    /**
     * Enable/disable email tracking feature
     */
    private boolean enabled = true;

    /**
     * Number of days after which tracking tokens expire
     * Default: 90 days
     */
    private int tokenExpiryDays = 90;

    /**
     * Email address to receive tracking notifications
     */
    private String notificationEmail = "contact@robgro.dev";

    /**
     * Send instant report after each email open
     * If false, reports are batched (future feature)
     */
    private boolean sendInstantReport = true;
}
