package dev.robgro.timesheet.invoice.delivery;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Canonical entry-point for atomic delivery job claiming.
 *
 * <p>Uses JDBC TransactionTemplate (READ_COMMITTED, REQUIRES_NEW) instead of
 * JPA @Transactional to work around HibernateJpaDialect's restriction on
 * custom isolation levels.
 *
 * <p>READ_COMMITTED prevents gap locks when ORDER BY + LIMIT is used in UPDATE
 * (InnoDB default REPEATABLE_READ + ORDER BY + LIMIT = sporadic deadlocks with 2+ instances).
 *
 * <p>Workers MUST call only this service — never the repository directly —
 * to ensure correct isolation is always applied.
 */
@Service
@Slf4j
public class DeliveryClaimService {

    private static final String CLAIM_SQL = """
            UPDATE invoice_delivery_job
            SET status      = 'PROCESSING',
                locked_at   = NOW(),
                locked_by   = :lockedBy,
                claim_token = :token,
                updated_at  = NOW()
            WHERE status = 'PENDING'
              AND next_retry_at <= NOW()
              AND attempts < :maxAttempts
              AND locked_at IS NULL
              AND is_active = 1
            ORDER BY next_retry_at ASC, id ASC
            LIMIT :batchSize
            """;

    private final InvoiceDeliveryJobRepository repository;
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final TransactionTemplate claimTx;

    public DeliveryClaimService(
            InvoiceDeliveryJobRepository repository,
            NamedParameterJdbcTemplate jdbcTemplate,
            @Qualifier("deliveryClaimTxTemplate") TransactionTemplate claimTx) {
        this.repository = repository;
        this.jdbcTemplate = jdbcTemplate;
        this.claimTx = claimTx;
    }

    /**
     * Atomically claim up to {@code batchSize} PENDING jobs and return them.
     *
     * @param lockedBy    worker identifier (e.g., hostname + PID)
     * @param maxAttempts maximum attempts allowed (jobs exceeding this are skipped)
     * @param batchSize   maximum number of jobs to claim
     * @return claimed jobs in PROCESSING state, empty list if nothing available
     */
    public List<InvoiceDeliveryJob> claimBatch(String lockedBy, int maxAttempts, int batchSize) {
        String token = UUID.randomUUID().toString();

        Integer claimed = claimTx.execute(status -> {
            var params = new MapSqlParameterSource()
                    .addValue("lockedBy", lockedBy)
                    .addValue("token", token)
                    .addValue("maxAttempts", maxAttempts)
                    .addValue("batchSize", batchSize);
            return jdbcTemplate.update(CLAIM_SQL, params);
        });

        if (claimed == null || claimed == 0) {
            return Collections.emptyList();
        }

        List<InvoiceDeliveryJob> jobs = repository.findByClaimTokenOrderByIdAsc(token);
        log.debug("Claimed {} delivery jobs (token={})", jobs.size(), token);
        return jobs;
    }
}