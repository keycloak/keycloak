package org.keycloak.quarkus.runtime.storage.infinispan.jgroups.impl;

import org.jgroups.protocols.JDBC_PING2;
import org.jgroups.protocols.PingData;

import java.sql.Connection;
import java.sql.SQLException;

public class KEYCLOAK_JDBC_PING2 extends JDBC_PING2 {
    @Override
    protected void writeToDB(PingData data, String clustername) throws SQLException {
        lock.lock();
        try(Connection connection=getConnection()) {
            if(call_insert_sp != null && insert_sp != null)
                callInsertStoredProcedure(connection, data, clustername);
            else {
                boolean isAutocommit = connection.getAutoCommit();
                try {
                    if (isAutocommit) connection.setAutoCommit(false);
                    delete(connection, clustername, data.getAddress());
                    insert(connection, data, clustername);
                    if (isAutocommit) connection.commit();
                } finally {
                    connection.setAutoCommit(isAutocommit);
                }
            }
        } finally {
            lock.unlock();
        }

    }
}
