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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.keycloak.models.Constants;
import org.keycloak.models.GroupModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.jpa.entities.GroupEntity;

import liquibase.datatype.DataTypeFactory;
import liquibase.exception.CustomChangeException;
import liquibase.statement.core.InsertStatement;
import liquibase.statement.core.UpdateStatement;
import liquibase.structure.core.Table;

public class JpaUpdate26_8_0_OrganizationRoles extends CustomKeycloakTask {

    static final int MAX_ROLE_NAME_LENGTH = 255;
    static final int MAX_ROLE_NAME_CANDIDATES = 1_000;
    static final int MAX_ROLE_ID_CANDIDATES = 100;

    @Override
    protected void generateStatementsImpl() throws CustomChangeException {
        migrateRoleTypes();
        migrateOrganizationDefaultRoles();
    }

    private void migrateRoleTypes() {
        String roleTable = database.correctObjectName("KEYCLOAK_ROLE", Table.class);
        String clientRole = DataTypeFactory.getInstance().getTrueBooleanValue(database);
        String realmRole = DataTypeFactory.getInstance().getFalseBooleanValue(database);

        statements.add(new UpdateStatement(null, null, roleTable)
                .addNewColumnValue("TYPE", RoleModel.Type.CLIENT.name())
                .setWhereClause("CLIENT_ROLE=" + clientRole));
        statements.add(new UpdateStatement(null, null, roleTable)
                .addNewColumnValue("TYPE", RoleModel.Type.REALM.name())
                .setWhereClause("CLIENT_ROLE=" + realmRole));
    }

    private void migrateOrganizationDefaultRoles() throws CustomChangeException {
        List<OrganizationRoleMigration> organizations = readOrganizations();
        validateOrganizations(organizations);

        for (OrganizationRoleMigration organization : organizations) {
            String roleName = determineDefaultRoleName(organization);
            String roleId = determineAvailableRoleId();

            createDefaultRole(roleId, organization.realmId(), organization.id(), roleName);
            setOrganizationDefaultRole(organization.id(), roleId);
            assignDefaultRoleToGroup(roleId, organization.groupId());
        }
    }

    private List<OrganizationRoleMigration> readOrganizations() throws CustomChangeException {
        List<OrganizationRoleMigration> organizations = new ArrayList<>();
        String sql = "SELECT O.ID AS ORG_PRIMARY_KEY, O.REALM_ID AS ORG_REALM_ID, O.ALIAS AS ORG_ALIAS, "
                + "O.NAME AS ORG_NAME, O.GROUP_ID AS ORG_GROUP_ID, G.ID AS ROOT_ID, "
                + "G.REALM_ID AS ROOT_REALM_ID, G.TYPE AS ROOT_TYPE, G.ORG_ID AS ROOT_ORG_ID, "
                + "G.PARENT_GROUP AS ROOT_PARENT_ID FROM " + getTableName("ORG") + " O LEFT JOIN "
                + getTableName("KEYCLOAK_GROUP") + " G ON O.GROUP_ID=G.ID WHERE O.DEFAULT_ROLE_ID IS NULL";

        try (PreparedStatement statement = jdbcConnection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                int rootType = resultSet.getInt("ROOT_TYPE");
                Integer nullableRootType = resultSet.wasNull() ? null : rootType;
                organizations.add(new OrganizationRoleMigration(
                        resultSet.getString("ORG_PRIMARY_KEY"),
                        resultSet.getString("ORG_REALM_ID"),
                        resultSet.getString("ORG_ALIAS"),
                        resultSet.getString("ORG_NAME"),
                        resultSet.getString("ORG_GROUP_ID"),
                        resultSet.getString("ROOT_ID"),
                        resultSet.getString("ROOT_REALM_ID"),
                        nullableRootType,
                        resultSet.getString("ROOT_ORG_ID"),
                        resultSet.getString("ROOT_PARENT_ID")));
            }
        } catch (Exception e) {
            throw new CustomChangeException(getTaskId() + ": Exception when migrating organization default roles", e);
        }

        return organizations;
    }

    private void validateOrganizations(List<OrganizationRoleMigration> organizations) throws CustomChangeException {
        Set<String> rootIds = new HashSet<>();

        for (OrganizationRoleMigration organization : organizations) {
            if (!isBlank(organization.groupId()) && !rootIds.add(organization.groupId())) {
                throw invalidRoot(organization.id(), "root group is referenced by more than one organization");
            }
        }

        for (OrganizationRoleMigration organization : organizations) {
            String organizationId = organization.id();

            if (isBlank(organizationId) || isBlank(organization.realmId())) {
                throw invalidRoot(organizationId, "organization id and realm must be set");
            }
            if (isBlank(organization.groupId())) {
                throw invalidRoot(organizationId, "root group id is missing");
            }
            if (organization.rootId() == null) {
                throw invalidRoot(organizationId, "root group does not exist");
            }
            if (!organization.realmId().equals(organization.rootRealmId())) {
                throw invalidRoot(organizationId, "root group belongs to another realm");
            }
            if (!Integer.valueOf(GroupModel.Type.ORGANIZATION.intValue()).equals(organization.rootType())) {
                throw invalidRoot(organizationId, "root group has an invalid type");
            }
            if (!organizationId.equals(organization.rootOrganizationId())) {
                throw invalidRoot(organizationId, "root group belongs to another organization");
            }
            if (!GroupEntity.TOP_PARENT_ID.equals(organization.rootParentId())) {
                throw invalidRoot(organizationId, "root group is not a top-level group");
            }
            if (exists("SELECT ROLE_ID FROM " + getTableName("GROUP_ROLE_MAPPING") + " WHERE GROUP_ID=?",
                    "checking the organization root role mappings", organization.groupId())) {
                throw invalidRoot(organizationId, "root group already has role mappings");
            }
        }
    }

    private CustomChangeException invalidRoot(String organizationId, String reason) {
        return new CustomChangeException(getTaskId() + ": Invalid root for organization " + organizationId + ": " + reason);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String determineAvailableRoleId() throws CustomChangeException {
        for (int candidate = 0; candidate < MAX_ROLE_ID_CANDIDATES; candidate++) {
            String roleId = UUID.randomUUID().toString();
            if (!exists("SELECT ID FROM " + getTableName("KEYCLOAK_ROLE") + " WHERE ID=?",
                    "checking organization role id availability", roleId)) {
                return roleId;
            }
        }

        throw new CustomChangeException(getTaskId() + ": Unable to determine an available organization role id");
    }

    private void createDefaultRole(String roleId, String realmId, String organizationId, String roleName) {
        statements.add(new InsertStatement(null, null, database.correctObjectName("KEYCLOAK_ROLE", Table.class))
                .addColumnValue("ID", roleId)
                .addColumnValue("CLIENT_REALM_CONSTRAINT", organizationId)
                .addColumnValue("CLIENT_ROLE", Boolean.FALSE)
                .addColumnValue("DESCRIPTION", "${role_default-roles}")
                .addColumnValue("NAME", roleName)
                .addColumnValue("REALM_ID", realmId)
                .addColumnValue("TYPE", RoleModel.Type.ORGANIZATION.name())
                .addColumnValue("ORG_ID", organizationId));
    }

    private void setOrganizationDefaultRole(String organizationId, String roleId) {
        statements.add(new UpdateStatement(null, null, database.correctObjectName("ORG", Table.class))
                .addNewColumnValue("DEFAULT_ROLE_ID", roleId)
                .setWhereClause("ID=?")
                .addWhereParameter(organizationId));
    }

    private void assignDefaultRoleToGroup(String roleId, String groupId) {
        statements.add(new InsertStatement(null, null, database.correctObjectName("GROUP_ROLE_MAPPING", Table.class))
                .addColumnValue("GROUP_ID", groupId)
                .addColumnValue("ROLE_ID", roleId));
    }

    private String determineDefaultRoleName(OrganizationRoleMigration organization) throws CustomChangeException {
        String alias = organization.alias();
        String name = isBlank(alias) ? organization.name() : alias;
        if (isBlank(name)) {
            throw new CustomChangeException(getTaskId() + ": Organization " + organization.id() + " has no name or alias");
        }
        String baseRoleName = Constants.DEFAULT_ORGANIZATION_ROLES_ROLE_PREFIX + "-" + name.toLowerCase(Locale.ROOT);

        for (int candidate = 0; candidate < MAX_ROLE_NAME_CANDIDATES; candidate++) {
            String suffix = candidate == 0 ? "" : "-" + candidate;
            String roleName = truncateWithoutSplittingSurrogate(baseRoleName, MAX_ROLE_NAME_LENGTH - suffix.length()) + suffix;
            if (isRoleNameAvailable(organization.id(), roleName)) {
                return roleName;
            }
        }
        throw new CustomChangeException(getTaskId() + ": Unable to determine default organization role name.");
    }

    private boolean isRoleNameAvailable(String organizationId, String roleName) throws CustomChangeException {
        return !exists("SELECT ID FROM " + getTableName("KEYCLOAK_ROLE")
                + " WHERE CLIENT_REALM_CONSTRAINT=? AND NAME=?",
                "checking organization role name availability", organizationId, roleName);
    }

    private static String truncateWithoutSplittingSurrogate(String value, int maximumLength) {
        if (value.length() <= maximumLength) {
            return value;
        }

        int end = maximumLength;
        if (end > 0 && Character.isHighSurrogate(value.charAt(end - 1)) && Character.isLowSurrogate(value.charAt(end))) {
            end--;
        }
        return value.substring(0, end);
    }

    private boolean exists(String sql, String operation, String... parameters) throws CustomChangeException {
        try (PreparedStatement statement = jdbcConnection.prepareStatement(sql)) {
            for (int i = 0; i < parameters.length; i++) {
                statement.setString(i + 1, parameters[i]);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (Exception e) {
            throw new CustomChangeException(getTaskId() + ": Exception when " + operation, e);
        }
    }

    @Override
    protected String getTaskId() {
        return "Migrate organization roles (26.8.0)";
    }

    private record OrganizationRoleMigration(String id, String realmId, String alias, String name, String groupId,
            String rootId, String rootRealmId, Integer rootType, String rootOrganizationId, String rootParentId) {
    }
}
