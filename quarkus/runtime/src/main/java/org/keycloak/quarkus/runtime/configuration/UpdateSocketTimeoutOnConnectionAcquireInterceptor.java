package org.keycloak.quarkus.runtime.configuration;

import io.agroal.api.AgroalPoolInterceptor;

import io.quarkus.runtime.configuration.DurationConverter;

import org.jspecify.annotations.NonNull;

import org.keycloak.config.TransactionOptions;
import org.keycloak.models.utils.KeycloakModelUtils;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class UpdateSocketTimeoutOnConnectionAcquireInterceptor implements AgroalPoolInterceptor {

    private final Executor executor;
    private final Integer transactionDefaultTimeout;

    private static final int THREAD_POOL_SIZE = 1;

    public UpdateSocketTimeoutOnConnectionAcquireInterceptor() {
        this.executor = Executors
                .newFixedThreadPool(THREAD_POOL_SIZE, new InternalThreadFactory());
        this.transactionDefaultTimeout =
                Long.valueOf(DurationConverter.parseDuration(
                        Configuration.getConfigValue(TransactionOptions.TRANSACTION_DEFAULT_TIMEOUT).getValue()
                ).toMillis()).intValue();
    }

    @Override
    public void onConnectionAcquire(Connection connection) {
        try {
            Optional<Integer> timeout = KeycloakModelUtils.getTransactionLimit();

            if (timeout.isPresent()) {
                int timeoutMillis = timeout.get() * 1000;
                connection.setNetworkTimeout(executor, timeoutMillis);
            } else {
                connection.setNetworkTimeout(executor, transactionDefaultTimeout);
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private static class InternalThreadFactory implements ThreadFactory {

        private static final String THREAD_NAME = "jdbc-network-timeout";

        @Override
        public Thread newThread(@NonNull Runnable runnable) {
            Thread thread = new Thread(runnable);
            thread.setName(THREAD_NAME);
            thread.setDaemon(true);
            return thread;
        }
    }
}
