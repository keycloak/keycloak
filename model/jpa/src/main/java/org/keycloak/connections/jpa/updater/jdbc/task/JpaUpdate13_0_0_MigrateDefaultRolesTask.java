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
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.utils.KeycloakModelUtils;

/**
 * Migration task registered for the historic manifest class name {@code JpaUpdate13_0_0_MigrateDefaultRoles}.
 * Creates new default roles per realm and wires up composite roles.
 */
public class JpaUpdate13_0_0_MigrateDefaultRolesTask implements JpaMigrationTask {

    @Override
    public String getTaskId() {
        return "Migrate Default roles (13.0.0)";
    }

    @Override
    public List<String> generateStatements(KeycloakSession session, Connection connection, String defaultSchema)
            throws SQLException {
        if (!NativeSqlHelper.isApplicable(connection, defaultSchema, SqlDialect.of(connection))) {
            return List.of();
        }

        String realmTable = NativeSqlHelper.qualified("REALM", defaultSchema);
        String roleTable = NativeSqlHelper.qualified("KEYCLOAK_ROLE", defaultSchema);
        String compositeTable = NativeSqlHelper.qualified("COMPOSITE_ROLE", defaultSchema);
        String realmDefaultRoles = NativeSqlHelper.qualified("REALM_DEFAULT_ROLES", defaultSchema);
        String clientDefaultRoles = NativeSqlHelper.qualified("CLIENT_DEFAULT_ROLES", defaultSchema);
        String clientTable = NativeSqlHelper.qualified("CLIENT", defaultSchema);

        Map<String, String> realms = new HashMap<>(); // id → name
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT ID, NAME FROM " + realmTable);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                realms.put(rs.getString(1), rs.getString(2));
            }
        }

        List<String> stmts = new ArrayList<>();
        for (Map.Entry<String, String> entry : realms.entrySet()) {
            String realmId = entry.getKey();
            String realmName = entry.getValue();
            String roleName = determineDefaultRoleName(connection, roleTable, realmId, realmName);
            String roleId = KeycloakModelUtils.generateId();

            // Create new default role
            stmts.add("INSERT INTO " + roleTable
                    + " (ID, CLIENT_REALM_CONSTRAINT, CLIENT_ROLE, DESCRIPTION, NAME, REALM_ID, REALM) VALUES ("
                    + NativeSqlHelper.sqlLiteral(roleId) + ", "
                    + NativeSqlHelper.sqlLiteral(realmId) + ", "
                    + NativeSqlHelper.sqlLiteral(false) + ", "
                    + NativeSqlHelper.sqlLiteral("${role_" + roleName + "}") + ", "
                    + NativeSqlHelper.sqlLiteral(roleName) + ", "
                    + NativeSqlHelper.sqlLiteral(realmId) + ", "
                    + NativeSqlHelper.sqlLiteral(realmId) + ")");

            // Assign role to realm
            stmts.add("UPDATE " + realmTable
                    + " SET DEFAULT_ROLE = " + NativeSqlHelper.sqlLiteral(roleId)
                    + " WHERE ID = " + NativeSqlHelper.sqlLiteral(realmId));

            // Copy realm default roles to composite_role
            stmts.add("INSERT INTO " + compositeTable + " (COMPOSITE, CHILD_ROLE) "
                    + "SELECT " + NativeSqlHelper.sqlLiteral(roleId) + ", ROLE_ID FROM " + realmDefaultRoles
                    + " WHERE REALM_ID = " + NativeSqlHelper.sqlLiteral(realmId));

            // Copy client default roles to composite_role
            stmts.add("INSERT INTO " + compositeTable + " (COMPOSITE, CHILD_ROLE) "
                    + "SELECT " + NativeSqlHelper.sqlLiteral(roleId) + ", " + clientDefaultRoles + ".ROLE_ID FROM "
                    + clientDefaultRoles + " INNER JOIN " + clientTable + " ON " + clientTable + ".ID = "
                    + clientDefaultRoles + ".CLIENT_ID AND " + clientTable + ".REALM_ID = "
                    + NativeSqlHelper.sqlLiteral(realmId));
        }
        return stmts;
    }

    private String determineDefaultRoleName(Connection conn, String roleTable, String realmId, String realmName)
            throws SQLException {
        String baseName = Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + realmName.toLowerCase();
        if (isRoleNameAvailable(conn, roleTable, realmId, baseName)) {
            return baseName;
        }
        for (int i = 1; i < Integer.MAX_VALUE; i++) {
            String candidate = baseName + "-" + i;
            if (isRoleNameAvailable(conn, roleTable, realmId, candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Unable to determine default role name for realm " + realmId);
    }

    private boolean isRoleNameAvailable(Connection conn, String roleTable, String realmId, String roleName)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT ID FROM " + roleTable + " WHERE REALM_ID = ? AND NAME = ?")) {
            ps.setString(1, realmId);
            ps.setString(2, roleName);
            try (ResultSet rs = ps.executeQuery()) {
                return !rs.next();
            }
        }
    }
}
