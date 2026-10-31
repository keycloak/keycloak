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
import org.keycloak.enums.SqlDialect;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OrganizationModel;

/**
 * Migration task registered for the historic manifest class name {@code JpaUpdate26_0_0_IdentityProviderAttributesMigration}.
 * Migrates organization ID and hideOnLoginPage flag from IDP config to IDP table columns.
 */
public class JpaUpdate26_0_0_IdentityProviderAttributesMigrationTask implements JpaMigrationTask {

    @Override
    public String getTaskId() {
        return "Migrate kc.org and hideOnLoginPage from the IDP config to the IDP itself";
    }

    @Override
    public List<String> generateStatements(KeycloakSession session, Connection connection, String defaultSchema,
            SqlDialect dialect)
            throws SQLException {
        String idpTable = SqlDialect.qualified("IDENTITY_PROVIDER", defaultSchema);
        String idpConfigTable = SqlDialect.qualified("IDENTITY_PROVIDER_CONFIG", defaultSchema);
        List<String> stmts = new ArrayList<>();

        // Move organization id
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT c.IDENTITY_PROVIDER_ID, c.VALUE FROM " + idpConfigTable
                        + " c WHERE c.NAME = " + dialect.literal(OrganizationModel.ORGANIZATION_ATTRIBUTE));
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String id = rs.getString(1);
                String value = rs.getString(2);
                stmts.add("UPDATE " + idpTable
                        + " SET ORGANIZATION_ID = " + dialect.literal(value)
                        + " WHERE INTERNAL_ID = " + dialect.literal(id));
            }
        }
        stmts.add("DELETE FROM " + idpConfigTable
                + " WHERE NAME = " + dialect.literal(OrganizationModel.ORGANIZATION_ATTRIBUTE));

        // Move hide on login page
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT c.IDENTITY_PROVIDER_ID, c.VALUE FROM " + idpConfigTable
                        + " c WHERE c.NAME = " + dialect.literal(IdentityProviderModel.LEGACY_HIDE_ON_LOGIN_ATTR));
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String id = rs.getString(1);
                boolean value = Boolean.parseBoolean(rs.getString(2));
                stmts.add("UPDATE " + idpTable
                        + " SET HIDE_ON_LOGIN = " + dialect.literal(value)
                        + " WHERE INTERNAL_ID = " + dialect.literal(id));
            }
        }
        stmts.add("DELETE FROM " + idpConfigTable
                + " WHERE NAME = " + dialect.literal(IdentityProviderModel.LEGACY_HIDE_ON_LOGIN_ATTR));

        // Move kc.org.broker.public (inverted)
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT c.IDENTITY_PROVIDER_ID, c.VALUE FROM " + idpConfigTable
                        + " c WHERE c.NAME = 'kc.org.broker.public'");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String id = rs.getString(1);
                boolean orgBrokerPublic = Boolean.parseBoolean(rs.getString(2));
                // hide_on_login is the inverse of kc.org.broker.public
                stmts.add("UPDATE " + idpTable
                        + " SET HIDE_ON_LOGIN = " + dialect.literal(!orgBrokerPublic)
                        + " WHERE INTERNAL_ID = " + dialect.literal(id));
            }
        }
        stmts.add("DELETE FROM " + idpConfigTable + " WHERE NAME = 'kc.org.broker.public'");

        return stmts;
    }
}
