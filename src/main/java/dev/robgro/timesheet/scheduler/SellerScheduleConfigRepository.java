package dev.robgro.timesheet.scheduler;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SellerScheduleConfigRepository extends JpaRepository<SellerScheduleConfig, Long> {

    Optional<SellerScheduleConfig> findBySellerId(Long sellerId);

    /**
     * Join-fetch seller to prevent N+1 when iterating in the master CRON tick.
     */
    @Query("SELECT c FROM SellerScheduleConfig c JOIN FETCH c.seller WHERE c.enabled = true")
    List<SellerScheduleConfig> findAllEnabledWithSeller();

    /**
     * PESSIMISTIC_WRITE lock for atomic reservation.
     * 5s timeout — avoids blocking the scheduler thread indefinitely on DB hiccup.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000"))
    @Query("SELECT c FROM SellerScheduleConfig c WHERE c.id = :id")
    Optional<SellerScheduleConfig> findByIdWithLock(@Param("id") Long id);
}