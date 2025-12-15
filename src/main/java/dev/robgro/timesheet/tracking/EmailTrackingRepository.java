package dev.robgro.timesheet.tracking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmailTrackingRepository extends JpaRepository<EmailTracking, Long> {

    /**
     * Find tracking by token
     */
    Optional<EmailTracking> findByTrackingToken(String trackingToken);

    /**
     * Find tracking by invoice ID
     */
    Optional<EmailTracking> findByInvoiceId(Long invoiceId);

    /**
     * Find all unopened emails older than specified date
     */
    @Query("SELECT et FROM EmailTracking et WHERE et.openedAt IS NULL AND et.createdAt < :threshold")
    List<EmailTracking> findUnopenedOlderThan(@Param("threshold") LocalDateTime threshold);

    /**
     * Find all expired tokens
     */
    @Query("SELECT et FROM EmailTracking et WHERE et.expiresAt < :now")
    List<EmailTracking> findExpiredTokens(@Param("now") LocalDateTime now);

    /**
     * Count total opened emails
     */
    @Query("SELECT COUNT(et) FROM EmailTracking et WHERE et.openedAt IS NOT NULL")
    long countOpened();

    /**
     * Count total sent emails
     */
    @Query("SELECT COUNT(et) FROM EmailTracking et")
    long countTotal();

    /**
     * Count emails opened within specified hours
     */
    @Query("SELECT COUNT(et) FROM EmailTracking et " +
           "WHERE et.openedAt IS NOT NULL " +
           "AND et.openedAt >= :since")
    long countOpenedSince(@Param("since") LocalDateTime since);

    /**
     * Get average time to first open in minutes
     * Uses native SQL for TIMESTAMPDIFF function
     */
    @Query(value = "SELECT AVG(TIMESTAMPDIFF(MINUTE, i.email_sent_at, et.opened_at)) " +
           "FROM email_tracking et " +
           "JOIN invoices i ON et.invoice_id = i.id " +
           "WHERE et.opened_at IS NOT NULL " +
           "AND i.email_sent_at IS NOT NULL",
           nativeQuery = true)
    Double getAverageTimeToFirstOpenMinutes();

    /**
     * Find all tracking records created before specified date (for cleanup)
     */
    @Query("SELECT et FROM EmailTracking et WHERE et.createdAt < :threshold")
    List<EmailTracking> findCreatedBefore(@Param("threshold") LocalDateTime threshold);
}
