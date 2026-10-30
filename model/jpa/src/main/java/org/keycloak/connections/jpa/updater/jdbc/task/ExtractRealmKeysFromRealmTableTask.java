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
import org.keycloak.keys.KeyProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.utils.KeycloakModelUtils;

/**
 * Migration task registered for the historic manifest class name {@code ExtractRealmKeysFromRealmTable}.
 * Reads key material from REALM and inserts COMPONENT + COMPONENT_CONFIG rows.
 */
public class ExtractRealmKeysFromRealmTableTask implements JpaMigrationTask {

    @Override
    public String getTaskId() {
        return "Update 2.3.0.Final";
    }

    @Override
    public List<String> generateStatements(KeycloakSession session, Connection connection, String defaultSchema,
            SqlDialect dialect)
            throws SQLException {
        String realmTable = SqlDialect.qualified("REALM", defaultSchema);
        String componentTable = SqlDialect.qualified("COMPONENT", defaultSchema);
        String componentConfigTable = SqlDialect.qualified("COMPONENT_CONFIG", defaultSchema);
        List<String> stmts = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT ID, PRIVATE_KEY, CERTIFICATE FROM " + realmTable);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String realmId = rs.getString(1);
                String privateKeyPem = rs.getString(2);
                String certificatePem = rs.getString(3);
                String componentId = KeycloakModelUtils.generateId();

                stmts.add("INSERT INTO " + componentTable
                        + " (ID, REALM_ID, PARENT_ID, NAME, PROVIDER_ID, PROVIDER_TYPE) VALUES ("
                        + dialect.literal(componentId) + ", "
                        + dialect.literal(realmId) + ", "
                        + dialect.literal(realmId) + ", "
                        + dialect.literal("rsa") + ", "
                        + dialect.literal("rsa") + ", "
                        + dialect.literal(KeyProvider.class.getName()) + ")");

                stmts.add(componentConfigInsert(componentConfigTable, componentId, "priority", "100", dialect));
                stmts.add(componentConfigInsert(componentConfigTable, componentId, "privateKey", privateKeyPem, dialect));
                stmts.add(componentConfigInsert(componentConfigTable, componentId, "certificate", certificatePem, dialect));
            }
        }
        return stmts;
    }

    static String componentConfigInsert(String componentConfigTable, String componentId, String name, String value,
            SqlDialect dialect) {
        return "INSERT INTO " + componentConfigTable
                + " (ID, COMPONENT_ID, NAME, VALUE) VALUES ("
                + dialect.literal(KeycloakModelUtils.generateId()) + ", "
                + dialect.literal(componentId) + ", "
                + dialect.literal(name) + ", "
                + dialect.literal(value) + ")";
    }
}
