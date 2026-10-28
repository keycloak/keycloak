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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.connections.jpa.updater.jdbc.JpaMigrationTask;
import org.keycloak.connections.jpa.updater.jdbc.NativeSqlHelper;
import org.keycloak.enums.SqlDialect;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.saml.util.ArtifactBindingUtils;

/**
 * Migration task registered for the historic manifest class name {@code JpaUpdate14_0_0_MigrateSamlArtifactAttribute}.
 * Adds the saml.artifact.binding.identifier attribute to SAML clients that don't have it.
 */
public class JpaUpdate14_0_0_MigrateSamlArtifactAttributeTask implements JpaMigrationTask {

    private static final String SAML_ARTIFACT_BINDING_IDENTIFIER = "saml.artifact.binding.identifier";

    @Override
    public String getTaskId() {
        return "Migrate Saml attributes (14.0.0)";
    }

    @Override
    public List<String> generateStatements(KeycloakSession session, Connection connection, String defaultSchema)
            throws SQLException {
        if (!NativeSqlHelper.isApplicable(connection, defaultSchema, SqlDialect.of(connection))) {
            return List.of();
        }
        String clientTable = NativeSqlHelper.qualified("CLIENT", defaultSchema);
        String clientAttrsTable = NativeSqlHelper.qualified("CLIENT_ATTRIBUTES", defaultSchema);
        Map<String, String> clientIds = new HashMap<>(); // internal id → CLIENT_ID name

        String query = "SELECT C.ID, C.CLIENT_ID FROM " + clientTable + " C "
                + "LEFT JOIN " + clientAttrsTable + " CA "
                + "ON C.ID = CA.CLIENT_ID AND CA.NAME = " + NativeSqlHelper.sqlLiteral(SAML_ARTIFACT_BINDING_IDENTIFIER) + " "
                + "WHERE C.PROTOCOL = 'saml' AND CA.NAME IS NULL";

        try (PreparedStatement ps = connection.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String id = rs.getString(1);
                String clientId = rs.getString(2);
                if (id != null && !id.isBlank() && clientId != null && !clientId.isBlank()) {
                    clientIds.put(id.trim(), clientId.trim());
                }
            }
        }

        List<String> stmts = new ArrayList<>();
        for (Map.Entry<String, String> entry : clientIds.entrySet()) {
            String identifier = ArtifactBindingUtils.computeArtifactBindingIdentifierString(entry.getValue());
            stmts.add("INSERT INTO " + clientAttrsTable
                    + " (CLIENT_ID, NAME, VALUE) VALUES ("
                    + NativeSqlHelper.sqlLiteral(entry.getKey()) + ", "
                    + NativeSqlHelper.sqlLiteral(SAML_ARTIFACT_BINDING_IDENTIFIER) + ", "
                    + NativeSqlHelper.sqlLiteral(identifier) + ")");
        }
        return stmts;
    }
}
