package org.keycloak.jgroups.impl;

import org.jgroups.protocols.JDBC_PING2;
import org.jgroups.protocols.PingData;
import org.keycloak.connections.jpa.JpaConnectionProviderFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

/**
 * Enhanced JDBC_PING2 to handle entries transactionally.
 * <p>
 * Workaround for issue <a href="https://issues.redhat.com/browse/JGRP-2870">JGRP-2870</a>
 */
public class KEYCLOAK_JDBC_PING2 extends JDBC_PING2 {

    private JpaConnectionProviderFactory factory;

    @Override
    protected void writeToDB(PingData data, String clustername) throws SQLException {
        lock.lock();
        try (Connection connection = getConnection()) {
            if(call_insert_sp != null && insert_sp != null)
                callInsertStoredProcedure(connection, data, clustername);
            else {
                boolean isAutocommit = connection.getAutoCommit();
                try {
                    if (isAutocommit) {
                        // Always use a transaction for the delete+insert to make it atomic
                        // to avoid the short moment where there is no entry in the table.
                        connection.setAutoCommit(false);
                    }
                    delete(connection, clustername, data.getAddress());
                    insert(connection, data, clustername);
                    if (isAutocommit) {
                        connection.commit();
                    }
                } catch (SQLException e) {
                    if (isAutocommit) {
                        connection.rollback();
                    }
                    throw e;
                } finally {
                    if (isAutocommit) {
                        connection.setAutoCommit(isAutocommit);
                    }
                }
            }
        } finally {
            lock.unlock();
        }

    }

    @Override
    protected void loadDriver() {
        //no-op, using JpaConnectionProviderFactory
    }

    @Override
    protected Connection getConnection() {
        return factory.getConnection();
    }

    public void setJpaConnectionProviderFactory(JpaConnectionProviderFactory factory) {
        this.factory = Objects.requireNonNull(factory);
    }
}
