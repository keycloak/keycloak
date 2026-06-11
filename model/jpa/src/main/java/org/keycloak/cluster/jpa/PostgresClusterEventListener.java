/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.cluster.jpa;

import java.io.Closeable;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.jboss.logging.Logger;

/**
 * Listens for PostgreSQL NOTIFY events on the {@code keycloak_cluster_event} channel
 * using a dedicated JDBC connection and triggers event processing when notifications arrive.
 *
 * <p>Uses reflection to access {@code org.postgresql.PGConnection.getNotifications(int)}
 * to avoid a compile-time dependency on the PostgreSQL JDBC driver.</p>
 */
public class PostgresClusterEventListener implements Closeable {

    private static final Logger logger = Logger.getLogger(PostgresClusterEventListener.class);

    private static final String NOTIFICATION_CHANNEL = "keycloak_cluster_event";
    private static final int NOTIFICATION_TIMEOUT_MS = 5000;
    private static final long RECONNECT_DELAY_MS = TimeUnit.SECONDS.toMillis(5);
    private static final long MAX_RECONNECT_DELAY_MS = TimeUnit.SECONDS.toMillis(60);

    private final Supplier<Connection> connectionSupplier;
    private final Runnable onNotification;
    private final Thread listenerThread;
    private volatile boolean running = true;
    private volatile Connection connection;

    public PostgresClusterEventListener(Supplier<Connection> connectionSupplier, Runnable onNotification) {
        this.connectionSupplier = connectionSupplier;
        this.onNotification = onNotification;
        this.listenerThread = new Thread(this::listenLoop, "pg-cluster-event-listener");
        this.listenerThread.setDaemon(true);
    }

    public void start() {
        listenerThread.start();
    }

    @Override
    public void close() {
        running = false;
        listenerThread.interrupt();
        closeConnection();
    }

    /**
     * Checks whether the given connection is a PostgreSQL connection that supports LISTEN/NOTIFY.
     */
    public static boolean isSupported(Connection connection) {
        Class<?> pgClass = getPgConnectionClass();
        if (pgClass == null) {
            return false;
        }
        try {
            return connection.isWrapperFor(pgClass);
        } catch (SQLException e) {
            return false;
        }
    }

    private void listenLoop() {
        long reconnectDelay = RECONNECT_DELAY_MS;

        while (running) {
            try {
                connect();
                reconnectDelay = RECONNECT_DELAY_MS;

                Class<?> pgClass = getPgConnectionClass();
                if (pgClass == null) {
                    logger.error("PostgreSQL JDBC driver not found, stopping LISTEN/NOTIFY listener");
                    return;
                }

                Object pgConn = connection.unwrap(pgClass);
                Method getNotifications = pgClass.getMethod("getNotifications", int.class);

                while (running) {
                    Object[] notifications = (Object[]) getNotifications.invoke(pgConn, NOTIFICATION_TIMEOUT_MS);
                    if (notifications != null && notifications.length > 0) {
                        if (logger.isTraceEnabled()) {
                            logger.tracef("Received %d PostgreSQL notification(s) on channel '%s'",
                                    notifications.length, NOTIFICATION_CHANNEL);
                        }
                        try {
                            onNotification.run();
                        } catch (Exception e) {
                            logger.warnf(e, "Error processing cluster event notification");
                        }
                    }
                }
            } catch (Exception e) {
                closeConnection();
                if (!running) {
                    break;
                }
                logger.warnf(e, "PostgreSQL LISTEN connection lost, reconnecting in %d ms", reconnectDelay);
                try {
                    Thread.sleep(reconnectDelay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
                reconnectDelay = Math.min(reconnectDelay * 2, MAX_RECONNECT_DELAY_MS);
            }
        }
    }

    private void connect() throws SQLException {
        closeConnection();
        connection = connectionSupplier.get();
        connection.setAutoCommit(true);
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("LISTEN " + NOTIFICATION_CHANNEL);
        }
        logger.infof("Listening for PostgreSQL notifications on channel '%s'", NOTIFICATION_CHANNEL);
    }

    private void closeConnection() {
        Connection c = this.connection;
        this.connection = null;
        if (c != null) {
            try {
                c.close();
            } catch (SQLException e) {
                logger.debug("Error closing PostgreSQL LISTEN connection", e);
            }
        }
    }

    private static Class<?> getPgConnectionClass() {
        try {
            return Class.forName("org.postgresql.PGConnection");
        } catch (ClassNotFoundException e) {
            return null;
        }
    }
}
