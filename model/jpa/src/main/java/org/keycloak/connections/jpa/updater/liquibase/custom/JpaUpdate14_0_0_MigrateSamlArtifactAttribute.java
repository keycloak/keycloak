/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.connections.jpa.updater.liquibase.custom;

import liquibase.exception.CustomChangeException;
import liquibase.statement.core.InsertStatement;
import liquibase.structure.core.Table;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

import static org.keycloak.protocol.saml.util.ArtifactBindingUtils.computeArtifactBindingIdentifierString;

public class JpaUpdate14_0_0_MigrateSamlArtifactAttribute extends CustomKeycloakTask {

    private static final String SAML_ARTIFACT_BINDING_IDENTIFIER = "saml.artifact.binding.identifier";

    private final Map<String, String> clientIds = new HashMap<>();

    @Override
    protected void generateStatementsImpl() throws CustomChangeException {
        extractClientsData("SELECT C.ID, C.CLIENT_ID FROM " + getTableName("CLIENT") + " C " +
                "LEFT JOIN " + getTableName("CLIENT_ATTRIBUTES") + " CA " +
                "ON C.ID = CA.CLIENT_ID AND CA.NAME='" + SAML_ARTIFACT_BINDING_IDENTIFIER + "' " +
                "WHERE C.PROTOCOL='saml' AND CA.NAME IS NULL");

        for (Map.Entry<String, String> clientPair : clientIds.entrySet()) {
            String id = clientPair.getKey();

            String clientId = clientPair.getValue();
            String samlIdentifier = computeArtifactBindingIdentifierString(clientId);

            statements.add(
                    new InsertStatement(null, null, database.correctObjectName("CLIENT_ATTRIBUTES", Table.class))
                            .addColumnValue("CLIENT_ID", id)
                            .addColumnValue("NAME", SAML_ARTIFACT_BINDING_IDENTIFIER)
                            .addColumnValue("VALUE", samlIdentifier)
            );
        }
    }

    private void extractClientsData(String sql) throws CustomChangeException {
        try (PreparedStatement statement = jdbcConnection.prepareStatement(sql);
                ResultSet rs = statement.executeQuery()) {

            while (rs.next()) {
                String id = rs.getString(1);
                String clientId = rs.getString(2);

                if (id == null || id.trim().isEmpty()
                        || clientId == null || clientId.trim().isEmpty()) {
                    continue;
                }

                clientIds.put(id, clientId);
            }

        } catch (Exception e) {
            throw new CustomChangeException(getTaskId() + ": Exception when extracting data from previous version", e);
        }
    }

    @Override
    protected String getTaskId() {
        return "Migrate Saml attributes (14.0.0)";
    }

}
