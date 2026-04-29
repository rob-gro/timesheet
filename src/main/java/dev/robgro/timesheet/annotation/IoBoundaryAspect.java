package dev.robgro.timesheet.annotation;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Fail-fast guard for {@link IoBoundary} methods.
 *
 * <p>Throws {@code IllegalStateException} if called inside an active transaction.
 * {@code @Order(HIGHEST_PRECEDENCE)} ensures this aspect fires BEFORE TransactionInterceptor,
 * so we see the real TX state, not the post-suspension state.
 *
 * <p>Note: even if ordering is violated, the meta-annotation {@code NOT_SUPPORTED}
 * on {@link IoBoundary} will still suspend the TX — this aspect is an additional assertion,
 * not the sole protection.
 */
@Aspect
@Order(Ordered.HIGHEST_PRECEDENCE)
@Component
public class IoBoundaryAspect {

    @Around("@annotation(dev.robgro.timesheet.annotation.IoBoundary)")
    public Object guard(ProceedingJoinPoint pjp) throws Throwable {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException(
                    "I/O boundary executed inside TX context: " + pjp.getSignature()
                            + ". Annotate the caller with @IoBoundary or remove @Transactional from it.");
        }
        return pjp.proceed();
    }
}