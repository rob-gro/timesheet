package dev.robgro.timesheet.tracking;

import jakarta.mail.MessagingException;

public interface EmailTrackingNotificationService {

    /**
     * Sends instant tracking notification to admin
     */
    void sendTrackingNotification(EmailTracking tracking) throws MessagingException;

}
