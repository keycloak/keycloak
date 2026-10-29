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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.keycloak.connections.jpa.updater.jdbc.JpaMigrationTask;
import org.keycloak.connections.jpa.updater.jdbc.NativeSqlHelper;
import org.keycloak.enums.SqlDialect;
import org.keycloak.models.KeycloakSession;

/**
 * Migration task registered for the historic manifest class name {@code JpaUpdate1_2_0_CR1}.
 * Inserts REALM_CLIENT rows for all clients with CONSENT_REQUIRED = TRUE.
 * Note: On Oracle/MSSQL boolean columns are stored as 1/0; on others as TRUE/FALSE.
 */
public class JpaUpdate1_2_0_CR1Task implements JpaMigrationTask {

    @Override
    public String getTaskId() {
        return "Update 1.2.0.CR1";
    }

    @Override
    public List<String> generateStatements(KeycloakSession session, Connection connection, String defaultSchema,
            SqlDialect dialect)
            throws SQLException {
        if (!NativeSqlHelper.isApplicable(connection, defaultSchema, dialect)) {
            return List.of();
        }
        String clientTable = NativeSqlHelper.qualified("CLIENT", defaultSchema);
        String realmClientTable = NativeSqlHelper.qualified("REALM_CLIENT", defaultSchema);
        String trueValue = NativeSqlHelper.trueLiteral(dialect);
        List<String> stmts = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT CLIENT.REALM_ID, CLIENT.ID FROM " + clientTable
                        + " CLIENT WHERE CLIENT.CONSENT_REQUIRED = " + trueValue);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String realmId = rs.getString(1);
                String clientId = rs.getString(2);
                stmts.add("INSERT INTO " + realmClientTable
                        + " (REALM_ID, CLIENT_ID) VALUES ("
                        + NativeSqlHelper.sqlLiteral(realmId) + ", "
                        + NativeSqlHelper.sqlLiteral(clientId) + ")");
            }
        }
        return stmts;
    }
}
