package dev.robgro.timesheet.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;

/**
 * JDBC transaction config for atomic delivery job claiming.
 *
 * <p>DeliveryClaimService requires READ_COMMITTED isolation to prevent gap locks
 * when using ORDER BY + LIMIT on InnoDB (default REPEATABLE_READ causes sporadic
 * deadlocks under concurrent workers). HibernateJpaDialect does not support
 * custom isolation levels — a dedicated JDBC TransactionTemplate is used instead.
 *
 * <p>DataSourceTransactionManager is NOT exposed as a Spring bean to avoid
 * triggering @ConditionalOnMissingBean(TransactionManager.class) in
 * JpaBaseConfiguration, which would prevent JpaTransactionManager auto-configuration.
 */
@Configuration
public class DeliveryClaimConfig {

    @Bean
    @Qualifier("deliveryClaimTxTemplate")
    public TransactionTemplate deliveryClaimTxTemplate(DataSource dataSource) {
        var tm = new DataSourceTransactionManager(dataSource); // inline, NOT a @Bean
        var tt = new TransactionTemplate(tm);
        tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        tt.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        return tt;
    }
}