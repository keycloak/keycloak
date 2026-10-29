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
 * Migration task registered for the historic manifest class name {@code JpaUpdate4_0_0_DefaultClientScopes}.
 * Inserts CLIENT_SCOPE_CLIENT rows based on each client's current CLIENT_TEMPLATE_ID.
 */
public class JpaUpdate4_0_0_DefaultClientScopesTask implements JpaMigrationTask {

    @Override
    public String getTaskId() {
        return "Update 4.0.0.Final (Default client scopes)";
    }

    @Override
    public List<String> generateStatements(KeycloakSession session, Connection connection, String defaultSchema,
            SqlDialect dialect)
            throws SQLException {
        if (!NativeSqlHelper.isApplicable(connection, defaultSchema, dialect)) {
            return List.of();
        }

        String clientTable = NativeSqlHelper.qualified("CLIENT", defaultSchema);
        String scopeClientTable = NativeSqlHelper.qualified("CLIENT_SCOPE_CLIENT", defaultSchema);
        List<String> stmts = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT ID, CLIENT_TEMPLATE_ID FROM " + clientTable);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String clientId = rs.getString(1);
                String templateId = rs.getString(2);
                if (clientId == null || clientId.isBlank()) continue;
                if (templateId == null || templateId.isBlank()) continue;
                stmts.add("INSERT INTO " + scopeClientTable
                        + " (CLIENT_ID, SCOPE_ID, DEFAULT_SCOPE) VALUES ("
                        + NativeSqlHelper.sqlLiteral(clientId.trim()) + ", "
                        + NativeSqlHelper.sqlLiteral(templateId.trim()) + ", "
                        + NativeSqlHelper.trueLiteral(dialect) + ")");
            }
        }
        return stmts;
    }
}
