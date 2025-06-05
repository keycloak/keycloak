/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.jgroups.protocol;

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
                        connection.setAutoCommit(true);
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
