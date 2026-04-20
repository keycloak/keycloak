package org.keycloak.quarkus.runtime.configuration;

import io.agroal.api.AgroalPoolInterceptor;

import io.quarkus.runtime.configuration.DurationConverter;

import org.apache.commons.lang3.StringUtils;

import org.keycloak.config.TransactionOptions;
import org.keycloak.models.utils.KeycloakModelUtils;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Temporarily overrides {@code socketTimeout} during MySQL/TiDB connection creation to prevent threads
 * from hanging indefinitely, since {@code loginTimeout} does not cover the full creation phase. During a
 * failover, this causes connection creation to hang, exhausting the pool and blocking all DB operations.
 * The original {@code socketTimeout} is restored after the connection is created.
 *
 * @see <a href="https://github.com/keycloak/keycloak/issues/42256">DB Connection Pool acquisition timeout errors on database failover</a>
 * @see <a href="https://github.com/keycloak/keycloak/issues/47174">MySQL/TiDB: Configure DB socket timeouts on the fly during connection creation phase</a>
 */
public class UpdateSocketTimeoutOnConnectionCreationInterceptor extends SocketTimeoutInterceptor implements AgroalPoolInterceptor {

    private Executor executor;
    private volatile boolean supported = true;
    private volatile Integer networkTimeout;

    public UpdateSocketTimeoutOnConnectionCreationInterceptor() {
        this.executor = Executors.newCachedThreadPool();
    }

    private int getDefaultNetworkTimeout() {
        if (networkTimeout == null) {
            synchronized (this) {
                networkTimeout = (int) DurationConverter.parseDuration(Configuration.getConfigValue(TransactionOptions.TRANSACTION_DEFAULT_TIMEOUT).getValue()).toMillis();
            }
        }
        return networkTimeout;
    }

    @Override
    public void onConnectionAcquire(Connection connection) {
        if (supported) {
            try {
                int timeout = KeycloakModelUtils.getTransactionLimit();
                if (timeout == 0) {
                    timeout = getDefaultNetworkTimeout();
                } else {
                    timeout = timeout * 1000;
                }
                connection.setNetworkTimeout(executor, timeout);
            } catch (SQLFeatureNotSupportedException e) {
                supported = false;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        AgroalPoolInterceptor.super.onConnectionAcquire(connection);
    }

    public void onConnectionReturn(Connection connection) {
        if (supported) {
            try {
                connection.setNetworkTimeout(executor, getDefaultNetworkTimeout());
            } catch (SQLFeatureNotSupportedException e) {
                supported = false;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        AgroalPoolInterceptor.super.onConnectionReturn(connection);
    }

    @Override
    public void onConnectionCreate(Connection connection) {
        if (isSupported) {
            String effectiveTimeout =
                    StringUtils.firstNonBlank(dbUrlSocketTimeout, dbUrlPropertiesSocketTimeout);

            try {
                connection.setNetworkTimeout(executor,
                        StringUtils.isNotBlank(effectiveTimeout) ? Integer.parseInt(effectiveTimeout) : 0);
            } catch (SQLException ex) {
                throw new IllegalStateException(ex);
            }
        }
    }
}
