/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.keycloak.models.Constants;
import org.keycloak.models.RoleModel;

import liquibase.exception.CustomChangeException;
import liquibase.statement.core.InsertStatement;
import liquibase.statement.core.RawSqlStatement;
import liquibase.statement.core.UpdateStatement;
import liquibase.structure.core.Table;

public class JpaUpdate26_7_0_OrganizationRoles extends CustomKeycloakTask {

    @Override
    protected void generateStatementsImpl() throws CustomChangeException {
        migrateRoleTypes();
        migrateOrganizationDefaultRoles();
    }

    private void migrateRoleTypes() throws CustomChangeException {
        String roleTable = database.correctObjectName("KEYCLOAK_ROLE", Table.class);

        try (PreparedStatement statement = jdbcConnection.prepareStatement("SELECT ID, CLIENT_ROLE FROM " + getTableName("KEYCLOAK_ROLE"));
                ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                int type = resultSet.getBoolean("CLIENT_ROLE") ? RoleModel.Type.CLIENT.intValue() : RoleModel.Type.REALM.intValue();
                statements.add(new UpdateStatement(null, null, roleTable)
                        .addNewColumnValue("TYPE", type)
                        .setWhereClause("ID=?")
                        .addWhereParameter(resultSet.getString("ID")));
            }
        } catch (Exception e) {
            throw new CustomChangeException(getTaskId() + ": Exception when migrating role types", e);
        }
    }

    private void migrateOrganizationDefaultRoles() throws CustomChangeException {
        List<OrganizationRoleMigration> organizations = new ArrayList<>();

        try (PreparedStatement statement = jdbcConnection.prepareStatement(
                "SELECT ID, REALM_ID, ALIAS, NAME, GROUP_ID FROM " + getTableName("ORG") + " WHERE DEFAULT_ROLE_ID IS NULL");
                ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                organizations.add(new OrganizationRoleMigration(
                        resultSet.getString("ID"),
                        resultSet.getString("REALM_ID"),
                        resultSet.getString("ALIAS"),
                        resultSet.getString("NAME"),
                        resultSet.getString("GROUP_ID")));
            }
        } catch (Exception e) {
            throw new CustomChangeException(getTaskId() + ": Exception when migrating organization default roles", e);
        }

        for (OrganizationRoleMigration organization : organizations) {
            String roleId = UUID.randomUUID().toString();

            createDefaultRole(roleId, organization.realmId(), organization.id(), determineDefaultRoleName(organization));
            setOrganizationDefaultRole(organization.id(), roleId);
            assignDefaultRoleToMembers(roleId, organization.realmId(), organization.groupId());
        }
    }

    private void createDefaultRole(String roleId, String realmId, String organizationId, String roleName) {
        statements.add(new InsertStatement(null, null, database.correctObjectName("KEYCLOAK_ROLE", Table.class))
                .addColumnValue("ID", roleId)
                .addColumnValue("CLIENT_REALM_CONSTRAINT", organizationId)
                .addColumnValue("CLIENT_ROLE", Boolean.FALSE)
                .addColumnValue("DESCRIPTION", "${role_default-roles}")
                .addColumnValue("NAME", roleName)
                .addColumnValue("REALM_ID", realmId)
                .addColumnValue("TYPE", RoleModel.Type.ORGANIZATION.intValue())
                .addColumnValue("ORG_ID", organizationId));
    }

    private void setOrganizationDefaultRole(String organizationId, String roleId) {
        statements.add(new UpdateStatement(null, null, database.correctObjectName("ORG", Table.class))
                .addNewColumnValue("DEFAULT_ROLE_ID", roleId)
                .setWhereClause("ID=?")
                .addWhereParameter(organizationId));
    }

    private void assignDefaultRoleToMembers(String roleId, String realmId, String groupId) {
        String escapedRoleId = database.escapeStringForDatabase(roleId);
        String escapedRealmId = database.escapeStringForDatabase(realmId);
        String escapedGroupId = database.escapeStringForDatabase(groupId);

        statements.add(new RawSqlStatement("INSERT INTO " + getTableName("USER_ROLE_MAPPING") + " (USER_ID, ROLE_ID) " +
                "SELECT USER_ID, '" + escapedRoleId + "' FROM " + getTableName("USER_GROUP_MEMBERSHIP") +
                " WHERE GROUP_ID = '" + escapedGroupId + "'"));

        statements.add(new RawSqlStatement("INSERT INTO " + getTableName("FED_USER_ROLE_MAPPING") + " (USER_ID, ROLE_ID, REALM_ID, STORAGE_PROVIDER_ID) " +
                "SELECT USER_ID, '" + escapedRoleId + "', REALM_ID, STORAGE_PROVIDER_ID FROM " + getTableName("FED_USER_GROUP_MEMBERSHIP") +
                " WHERE GROUP_ID = '" + escapedGroupId + "' AND REALM_ID = '" + escapedRealmId + "'"));
    }

    private String determineDefaultRoleName(OrganizationRoleMigration organization) throws CustomChangeException {
        String alias = organization.alias();
        String name = alias == null || alias.isBlank() ? organization.name() : alias;
        String baseRoleName = Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + name.toLowerCase(Locale.ROOT);

        return determineDefaultRoleName(organization.id(), baseRoleName, Integer.MAX_VALUE);
    }

    String determineDefaultRoleName(String organizationId, String baseRoleName, int maxSuffix) throws CustomChangeException {
        if (isRoleNameAvailable(organizationId, baseRoleName)) {
            return baseRoleName;
        }
        for (int i = 1; i < maxSuffix; i++) {
            String roleName = baseRoleName + "-" + i;
            if (isRoleNameAvailable(organizationId, roleName)) {
                return roleName;
            }
        }
        throw new CustomChangeException(getTaskId() + ": Unable to determine default organization role name.");
    }

    private boolean isRoleNameAvailable(String organizationId, String roleName) throws CustomChangeException {
        try (PreparedStatement statement = jdbcConnection.prepareStatement("SELECT ID FROM " + getTableName("KEYCLOAK_ROLE") +
                " WHERE ORG_ID=? AND NAME=?")) {
            statement.setString(1, organizationId);
            statement.setString(2, roleName);
            try (ResultSet resultSet = statement.executeQuery()) {
                return !resultSet.next();
            }
        } catch (Exception e) {
            throw new CustomChangeException(getTaskId() + ": Exception when checking organization role name availability", e);
        }
    }

    @Override
    protected String getTaskId() {
        return "Migrate organization roles (26.7.0)";
    }

    private record OrganizationRoleMigration(String id, String realmId, String alias, String name, String groupId) {
    }
}
