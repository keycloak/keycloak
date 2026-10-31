/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.connections.jpa.updater.jdbc.task;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import org.keycloak.connections.jpa.updater.jdbc.JpaMigrationTask;
import org.keycloak.enums.SqlDialect;
import org.keycloak.models.KeycloakSession;

/**
 * Migration task registered for the historic manifest class name {@code JpaUpdate26_6_0_OfflineClientSessionRealm}.
 * Sets the REALM_ID column in OFFLINE_CLIENT_SESSION from the corresponding OFFLINE_USER_SESSION row.
 * Uses dialect-specific UPDATE syntax for performance.
 */
public class JpaUpdate26_6_0_OfflineClientSessionRealmTask implements JpaMigrationTask {

    @Override
    public String getTaskId() {
        return "Sets the realm column in offline_client_session";
    }

    @Override
    public List<String> generateStatements(KeycloakSession session, Connection connection, String defaultSchema,
            SqlDialect dialect)
            throws SQLException {
        String cs = SqlDialect.qualified("OFFLINE_CLIENT_SESSION", defaultSchema);
        String us = SqlDialect.qualified("OFFLINE_USER_SESSION", defaultSchema);

        return switch (dialect) {
            case POSTGRESQL -> List.of(
                    "UPDATE " + cs + " cs"
                    + " SET realm_id = us.realm_id"
                    + " FROM " + us + " us"
                    + " WHERE cs.user_session_id = us.user_session_id"
                    + " AND cs.offline_flag = us.offline_flag"
                    + " AND cs.realm_id IS NULL"
            );
            case MYSQL, MARIADB -> List.of(
                    "UPDATE " + cs + " cs"
                    + " INNER JOIN " + us + " us"
                    + " ON cs.user_session_id = us.user_session_id"
                    + " AND cs.offline_flag = us.offline_flag AND cs.realm_id IS NULL"
                    + " SET cs.realm_id = us.realm_id"
            );
            case MSSQL -> List.of(
                    "UPDATE cs"
                    + " SET cs.REALM_ID = us.REALM_ID"
                    + " FROM " + cs + " cs"
                    + " INNER JOIN " + us + " us"
                    + " ON cs.USER_SESSION_ID = us.USER_SESSION_ID"
                    + " AND cs.OFFLINE_FLAG = us.OFFLINE_FLAG AND cs.REALM_ID IS NULL"
            );
            case ORACLE -> List.of(
                    "MERGE INTO " + cs + " cs"
                    + " USING " + us + " us"
                    + " ON (cs.user_session_id = us.user_session_id AND cs.offline_flag = us.offline_flag)"
                    + " WHEN MATCHED THEN UPDATE SET cs.realm_id = us.realm_id"
            );
            default -> List.of(
                    "UPDATE " + cs + " cs"
                    + " SET cs.realm_id = ("
                    + " SELECT us.realm_id FROM " + us + " us"
                    + " WHERE us.user_session_id = cs.user_session_id AND cs.offline_flag = us.offline_flag"
                    + " ) WHERE cs.realm_id IS NULL"
            );
        };
    }
}
