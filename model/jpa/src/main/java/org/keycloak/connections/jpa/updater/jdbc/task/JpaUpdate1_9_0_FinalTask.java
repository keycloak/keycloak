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
 * Migration task registered for the historic manifest class name {@code JpaUpdate1_9_0_Final}.
 * Populates REALM_ID in USER_FEDERATION_PROVIDER from the legacy FED_PROVIDERS join table.
 */
public class JpaUpdate1_9_0_FinalTask implements JpaMigrationTask {

    @Override
    public String getTaskId() {
        return "Update 1.9.0.Final";
    }

    @Override
    public List<String> generateStatements(KeycloakSession session, Connection connection, String defaultSchema,
            SqlDialect dialect)
            throws SQLException {
        if (!NativeSqlHelper.isApplicable(connection, defaultSchema, dialect)) {
            return List.of();
        }
        String fedProvidersTable = NativeSqlHelper.qualified("FED_PROVIDERS", defaultSchema);
        String ufpTable = NativeSqlHelper.qualified("USER_FEDERATION_PROVIDER", defaultSchema);
        List<String> stmts = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT REALM_ID, USERFEDERATIONPROVIDERS_ID FROM " + fedProvidersTable);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String realmId = rs.getString(1);
                String providerId = rs.getString(2);
                stmts.add("UPDATE " + ufpTable
                        + " SET REALM_ID = " + NativeSqlHelper.sqlLiteral(realmId)
                        + " WHERE ID = " + NativeSqlHelper.sqlLiteral(providerId));
            }
        }
        return stmts;
    }
}
