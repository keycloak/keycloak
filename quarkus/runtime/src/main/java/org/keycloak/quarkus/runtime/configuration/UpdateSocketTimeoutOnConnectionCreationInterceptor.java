package org.keycloak.quarkus.runtime.configuration;

import io.agroal.api.AgroalPoolInterceptor;

import org.apache.commons.lang3.StringUtils;

import java.sql.Connection;
import java.sql.SQLException;
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

    private final Executor executor;

    public UpdateSocketTimeoutOnConnectionCreationInterceptor() {
        this.executor = Executors.newCachedThreadPool();
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
