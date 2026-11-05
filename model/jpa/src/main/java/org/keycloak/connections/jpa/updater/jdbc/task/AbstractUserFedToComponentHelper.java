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
import java.util.List;

import org.keycloak.enums.SqlDialect;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.storage.UserStorageProvider;

/**
 * Shared helper for USER_FEDERATION_PROVIDER → COMPONENT migration tasks.
 */
final class AbstractUserFedToComponentHelper {

    private AbstractUserFedToComponentHelper() {
    }

    /**
     * Generates INSERT/DELETE statements to migrate one user federation provider type to the
     * component model.
     *
     * @param connection  live JDBC connection (read-only)
     * @param schema      configured default schema
     * @param providerId  the federation provider name (e.g. "ldap", "kerberos")
     * @param newMapperType  if non-null, federation mappers are also migrated with this provider type
     * @param stmts       list to append generated statements to
     * @param dialect     database dialect
     */
    static void convertFedProviderToComponent(Connection connection, String schema,
            String providerId, String newMapperType, List<String> stmts, SqlDialect dialect) throws SQLException {
        String ufpTable = SqlDialect.qualified("USER_FEDERATION_PROVIDER", schema);
        String ufcTable = SqlDialect.qualified("USER_FEDERATION_CONFIG", schema);
        String componentTable = SqlDialect.qualified("COMPONENT", schema);
        String componentConfigTable = SqlDialect.qualified("COMPONENT_CONFIG", schema);

        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT ID, REALM_ID, PRIORITY, DISPLAY_NAME, FULL_SYNC_PERIOD, CHANGED_SYNC_PERIOD, LAST_SYNC"
                        + " FROM " + ufpTable + " WHERE PROVIDER_NAME = ?")) {
            ps.setString(1, providerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String id = rs.getString(1);
                    String realmId = rs.getString(2);
                    int priority = rs.getInt(3);
                    String displayName = rs.getString(4);
                    int fullSyncPeriod = rs.getInt(5);
                    int changedSyncPeriod = rs.getInt(6);
                    int lastSync = rs.getInt(7);

                    stmts.add("INSERT INTO " + componentTable
                            + " (ID, REALM_ID, PARENT_ID, NAME, PROVIDER_ID, PROVIDER_TYPE) VALUES ("
                            + dialect.literal(id) + ", "
                            + dialect.literal(realmId) + ", "
                            + dialect.literal(realmId) + ", "
                            + dialect.literal(displayName) + ", "
                            + dialect.literal(providerId) + ", "
                            + dialect.literal(UserStorageProvider.class.getName()) + ")");

                    stmts.add(componentConfigInsert(componentConfigTable, id, "priority", Integer.toString(priority), dialect));
                    stmts.add(componentConfigInsert(componentConfigTable, id, "fullSyncPeriod", Integer.toString(fullSyncPeriod), dialect));
                    stmts.add(componentConfigInsert(componentConfigTable, id, "changedSyncPeriod", Integer.toString(changedSyncPeriod), dialect));
                    stmts.add(componentConfigInsert(componentConfigTable, id, "lastSync", Integer.toString(lastSync), dialect));

                    // Copy federation config entries
                    try (PreparedStatement cfgPs = connection.prepareStatement(
                            "SELECT NAME, VALUE FROM " + ufcTable + " WHERE USER_FEDERATION_PROVIDER_ID = ?")) {
                        cfgPs.setString(1, id);
                        try (ResultSet cfgRs = cfgPs.executeQuery()) {
                            while (cfgRs.next()) {
                                stmts.add(componentConfigInsert(componentConfigTable, id,
                                        cfgRs.getString(1), cfgRs.getString(2), dialect));
                            }
                        }
                    }

                    if (newMapperType != null) {
                        convertFedMapperToComponent(connection, schema, id, realmId, newMapperType, stmts, dialect);
                    }

                    // Delete the old federation config and provider records
                    stmts.add("DELETE FROM " + ufcTable
                            + " WHERE USER_FEDERATION_PROVIDER_ID = " + dialect.literal(id));
                    stmts.add("DELETE FROM " + ufpTable
                            + " WHERE ID = " + dialect.literal(id));
                }
            }
        }
    }

    private static void convertFedMapperToComponent(Connection connection, String schema,
            String parentId, String realmId, String newMapperType, List<String> stmts, SqlDialect dialect) throws SQLException {
        String ufmTable = SqlDialect.qualified("USER_FEDERATION_MAPPER", schema);
        String ufmcTable = SqlDialect.qualified("USER_FEDERATION_MAPPER_CONFIG", schema);
        String componentTable = SqlDialect.qualified("COMPONENT", schema);
        String componentConfigTable = SqlDialect.qualified("COMPONENT_CONFIG", schema);

        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT ID, NAME, FEDERATION_MAPPER_TYPE FROM " + ufmTable
                        + " WHERE FEDERATION_PROVIDER_ID = ?")) {
            ps.setString(1, parentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String id = rs.getString(1);
                    String mapperName = rs.getString(2);
                    String fedMapperType = rs.getString(3);

                    stmts.add("INSERT INTO " + componentTable
                            + " (ID, REALM_ID, PARENT_ID, NAME, PROVIDER_ID, PROVIDER_TYPE) VALUES ("
                            + dialect.literal(id) + ", "
                            + dialect.literal(realmId) + ", "
                            + dialect.literal(parentId) + ", "
                            + dialect.literal(mapperName) + ", "
                            + dialect.literal(fedMapperType) + ", "
                            + dialect.literal(newMapperType) + ")");

                    try (PreparedStatement cfgPs = connection.prepareStatement(
                            "SELECT NAME, VALUE FROM " + ufmcTable
                                    + " WHERE USER_FEDERATION_MAPPER_ID = ?")) {
                        cfgPs.setString(1, id);
                        try (ResultSet cfgRs = cfgPs.executeQuery()) {
                            while (cfgRs.next()) {
                                stmts.add(componentConfigInsert(componentConfigTable, id,
                                        cfgRs.getString(1), cfgRs.getString(2), dialect));
                            }
                        }
                    }

                    stmts.add("DELETE FROM " + ufmcTable
                            + " WHERE USER_FEDERATION_MAPPER_ID = " + dialect.literal(id));
                    stmts.add("DELETE FROM " + ufmTable
                            + " WHERE ID = " + dialect.literal(id));
                }
            }
        }
    }

    static String componentConfigInsert(String componentConfigTable, String componentId,
            String name, String value, SqlDialect dialect) {
        return "INSERT INTO " + componentConfigTable
                + " (ID, COMPONENT_ID, NAME, VALUE) VALUES ("
                + dialect.literal(KeycloakModelUtils.generateId()) + ", "
                + dialect.literal(componentId) + ", "
                + dialect.literal(name) + ", "
                + dialect.literal(value) + ")";
    }
}
