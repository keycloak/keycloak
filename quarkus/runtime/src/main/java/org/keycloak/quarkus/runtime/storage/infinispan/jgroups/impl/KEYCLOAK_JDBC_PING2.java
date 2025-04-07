package org.keycloak.quarkus.runtime.storage.infinispan.jgroups.impl;

import org.jgroups.protocols.JDBC_PING2;
import org.jgroups.protocols.PingData;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Enhanced JDBC_PING2 to handle entries transactionally.
 * <p>
 * Workaround for issue <a href="https://issues.redhat.com/browse/JGRP-2870">JGRP-2870</a>
 */
public class KEYCLOAK_JDBC_PING2 extends JDBC_PING2 {
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
}
