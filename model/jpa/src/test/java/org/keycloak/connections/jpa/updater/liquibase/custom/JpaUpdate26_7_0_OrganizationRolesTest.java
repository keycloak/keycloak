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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;

import org.keycloak.models.Constants;
import org.keycloak.models.RoleModel;

import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.sql.Sql;
import liquibase.sqlgenerator.SqlGeneratorFactory;
import liquibase.statement.SqlStatement;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThrows;

public class JpaUpdate26_7_0_OrganizationRolesTest {

    @Test
    public void shouldMigrateRoleTypesAndExistingOrganizationDefaults() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1")) {
            createSchema(connection);
            seedData(connection);

            Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
            SqlStatement[] statements = new JpaUpdate26_7_0_OrganizationRoles().generateStatements(database);
            executeStatements(database, connection, statements);

            assertThat(roleType(connection, "realm-role"), is(RoleModel.Type.REALM.name()));
            assertThat(roleType(connection, "client-role"), is(RoleModel.Type.CLIENT.name()));

            String defaultRoleId = organizationDefaultRole(connection, "org-1");
            assertThat(defaultRoleId, notNullValue());
            assertThat(defaultRoleName(connection, defaultRoleId), is(Constants.DEFAULT_ROLES_ROLE_PREFIX + "-acme"));
            assertThat(roleType(connection, defaultRoleId), is(RoleModel.Type.ORGANIZATION.name()));
            assertThat(roleOrganization(connection, defaultRoleId), is("org-1"));
            assertThat(roleClientRealmConstraint(connection, defaultRoleId), is("org-1"));
            assertThat(localRoleMappings(connection, "user-1", defaultRoleId), is(1));
            assertThat(federatedRoleMappings(connection, "fed-user-1", defaultRoleId, "realm-1", "storage-1"), is(1));
        }
    }

    @Test
    public void shouldChooseAvailableDefaultOrganizationRoleName() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1")) {
            createSchema(connection);
            seedData(connection);
            insertOrganizationRoleNameConflict(connection);

            Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
            SqlStatement[] statements = new JpaUpdate26_7_0_OrganizationRoles().generateStatements(database);
            executeStatements(database, connection, statements);

            String defaultRoleId = organizationDefaultRole(connection, "org-1");
            assertThat(defaultRoleName(connection, defaultRoleId), is(Constants.DEFAULT_ROLES_ROLE_PREFIX + "-acme-2"));
        }
    }

    @Test
    public void shouldWrapRoleTypeMigrationFailures() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1")) {
            createRealmSchema(connection);

            Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
            CustomChangeException exception = assertThrows(CustomChangeException.class,
                    () -> new JpaUpdate26_7_0_OrganizationRoles().generateStatements(database));

            assertThat(exception.getMessage(), containsString("Exception when migrating role types"));
        }
    }

    @Test
    public void shouldWrapOrganizationDefaultRoleMigrationFailures() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1")) {
            createRoleTypeSchema(connection);

            Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
            CustomChangeException exception = assertThrows(CustomChangeException.class,
                    () -> new JpaUpdate26_7_0_OrganizationRoles().generateStatements(database));

            assertThat(exception.getMessage(), containsString("Exception when migrating organization default roles"));
        }
    }

    @Test
    public void shouldWrapRoleNameAvailabilityFailures() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1")) {
            createSchemaWithoutOrganizationRoleColumn(connection);

            Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
            CustomChangeException exception = assertThrows(CustomChangeException.class,
                    () -> new JpaUpdate26_7_0_OrganizationRoles().generateStatements(database));

            assertThat(exception.getMessage(), containsString("Exception when checking organization role name availability"));
        }
    }

    private void createSchema(Connection connection) throws Exception {
        createRealmSchema(connection);
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE KEYCLOAK_ROLE (" +
                    "ID VARCHAR(36) NOT NULL, " +
                    "CLIENT_REALM_CONSTRAINT VARCHAR(255), " +
                    "CLIENT_ROLE BOOLEAN, " +
                    "DESCRIPTION VARCHAR(255), " +
                    "NAME VARCHAR(255), " +
                    "REALM_ID VARCHAR(36), " +
                    "CLIENT VARCHAR(36), " +
                    "TYPE VARCHAR(32), " +
                    "ORG_ID VARCHAR(255))");
            statement.execute("CREATE TABLE ORG (" +
                    "ID VARCHAR(36) NOT NULL, " +
                    "REALM_ID VARCHAR(36), " +
                    "ALIAS VARCHAR(255), " +
                    "NAME VARCHAR(255), " +
                    "GROUP_ID VARCHAR(36), " +
                    "DEFAULT_ROLE_ID VARCHAR(36))");
            statement.execute("CREATE TABLE USER_GROUP_MEMBERSHIP (USER_ID VARCHAR(36), GROUP_ID VARCHAR(36))");
            statement.execute("CREATE TABLE USER_ROLE_MAPPING (USER_ID VARCHAR(36), ROLE_ID VARCHAR(36))");
            statement.execute("CREATE TABLE FED_USER_GROUP_MEMBERSHIP (" +
                    "USER_ID VARCHAR(36), " +
                    "GROUP_ID VARCHAR(36), " +
                    "REALM_ID VARCHAR(36), " +
                    "STORAGE_PROVIDER_ID VARCHAR(36))");
            statement.execute("CREATE TABLE FED_USER_ROLE_MAPPING (" +
                    "USER_ID VARCHAR(36), " +
                    "ROLE_ID VARCHAR(36), " +
                    "REALM_ID VARCHAR(36), " +
                    "STORAGE_PROVIDER_ID VARCHAR(36))");
        }
    }

    private void createRealmSchema(Connection connection) throws Exception {
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE REALM (ID VARCHAR(36) NOT NULL)");
            statement.execute("INSERT INTO REALM (ID) VALUES ('realm-1')");
        }
    }

    private void createRoleTypeSchema(Connection connection) throws Exception {
        createRealmSchema(connection);
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE KEYCLOAK_ROLE (" +
                    "ID VARCHAR(36) NOT NULL, " +
                    "CLIENT_ROLE BOOLEAN, " +
                    "TYPE VARCHAR(32))");
        }
    }

    private void createSchemaWithoutOrganizationRoleColumn(Connection connection) throws Exception {
        createRealmSchema(connection);
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE KEYCLOAK_ROLE (" +
                    "ID VARCHAR(36) NOT NULL, " +
                    "CLIENT_REALM_CONSTRAINT VARCHAR(255), " +
                    "CLIENT_ROLE BOOLEAN, " +
                    "DESCRIPTION VARCHAR(255), " +
                    "NAME VARCHAR(255), " +
                    "REALM_ID VARCHAR(36), " +
                    "CLIENT VARCHAR(36), " +
                    "TYPE VARCHAR(32))");
            statement.execute("CREATE TABLE ORG (" +
                    "ID VARCHAR(36) NOT NULL, " +
                    "REALM_ID VARCHAR(36), " +
                    "ALIAS VARCHAR(255), " +
                    "NAME VARCHAR(255), " +
                    "GROUP_ID VARCHAR(36), " +
                    "DEFAULT_ROLE_ID VARCHAR(36))");
            statement.execute("INSERT INTO ORG (ID, REALM_ID, ALIAS, NAME, GROUP_ID, DEFAULT_ROLE_ID) " +
                    "VALUES ('org-1', 'realm-1', 'acme', 'Acme Inc', 'group-1', NULL)");
        }
    }

    private void seedData(Connection connection) throws Exception {
        try (Statement statement = connection.createStatement()) {
            statement.execute("INSERT INTO KEYCLOAK_ROLE (ID, CLIENT_REALM_CONSTRAINT, CLIENT_ROLE, NAME, REALM_ID, CLIENT) " +
                    "VALUES ('realm-role', 'realm-1', FALSE, 'realm-role', 'realm-1', NULL)");
            statement.execute("INSERT INTO KEYCLOAK_ROLE (ID, CLIENT_REALM_CONSTRAINT, CLIENT_ROLE, NAME, REALM_ID, CLIENT) " +
                    "VALUES ('client-role', 'client-1', TRUE, 'client-role', 'realm-1', 'client-1')");
            statement.execute("INSERT INTO ORG (ID, REALM_ID, ALIAS, NAME, GROUP_ID, DEFAULT_ROLE_ID) " +
                    "VALUES ('org-1', 'realm-1', 'acme', 'Acme Inc', 'group-1', NULL)");
            statement.execute("INSERT INTO USER_GROUP_MEMBERSHIP (USER_ID, GROUP_ID) VALUES ('user-1', 'group-1')");
            statement.execute("INSERT INTO FED_USER_GROUP_MEMBERSHIP (USER_ID, GROUP_ID, REALM_ID, STORAGE_PROVIDER_ID) " +
                    "VALUES ('fed-user-1', 'group-1', 'realm-1', 'storage-1')");
        }
    }

    private void insertOrganizationRoleNameConflict(Connection connection) throws Exception {
        try (Statement statement = connection.createStatement()) {
            statement.execute("INSERT INTO KEYCLOAK_ROLE (ID, CLIENT_REALM_CONSTRAINT, CLIENT_ROLE, NAME, REALM_ID, ORG_ID) " +
                    "VALUES ('existing-org-role', 'existing-org-role', FALSE, 'default-roles-acme', 'realm-1', 'org-1')");
            statement.execute("INSERT INTO KEYCLOAK_ROLE (ID, CLIENT_REALM_CONSTRAINT, CLIENT_ROLE, NAME, REALM_ID, ORG_ID) " +
                    "VALUES ('existing-org-role-1', 'existing-org-role-1', FALSE, 'default-roles-acme-1', 'realm-1', 'org-1')");
        }
    }

    private void executeStatements(Database database, Connection connection, SqlStatement[] statements) throws Exception {
        try (Statement statement = connection.createStatement()) {
            for (SqlStatement sqlStatement : statements) {
                for (Sql sql : SqlGeneratorFactory.getInstance().generateSql(sqlStatement, database)) {
                    statement.execute(sql.toSql());
                }
            }
        }
    }

    private String roleType(Connection connection, String roleId) throws Exception {
        return queryString(connection, "SELECT TYPE FROM KEYCLOAK_ROLE WHERE ID = ?", roleId);
    }

    private String roleOrganization(Connection connection, String roleId) throws Exception {
        return queryString(connection, "SELECT ORG_ID FROM KEYCLOAK_ROLE WHERE ID = ?", roleId);
    }

    private String roleClientRealmConstraint(Connection connection, String roleId) throws Exception {
        return queryString(connection, "SELECT CLIENT_REALM_CONSTRAINT FROM KEYCLOAK_ROLE WHERE ID = ?", roleId);
    }

    private String defaultRoleName(Connection connection, String roleId) throws Exception {
        return queryString(connection, "SELECT NAME FROM KEYCLOAK_ROLE WHERE ID = ?", roleId);
    }

    private String organizationDefaultRole(Connection connection, String organizationId) throws Exception {
        return queryString(connection, "SELECT DEFAULT_ROLE_ID FROM ORG WHERE ID = ?", organizationId);
    }

    private int localRoleMappings(Connection connection, String userId, String roleId) throws Exception {
        return queryCount(connection, "SELECT COUNT(*) FROM USER_ROLE_MAPPING WHERE USER_ID = ? AND ROLE_ID = ?", userId, roleId);
    }

    private int federatedRoleMappings(Connection connection, String userId, String roleId, String realmId, String storageProviderId) throws Exception {
        return queryCount(connection, "SELECT COUNT(*) FROM FED_USER_ROLE_MAPPING WHERE USER_ID = ? AND ROLE_ID = ? AND REALM_ID = ? AND STORAGE_PROVIDER_ID = ?",
                userId, roleId, realmId, storageProviderId);
    }

    private String queryString(Connection connection, String sql, String parameter) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, parameter);
            try (ResultSet resultSet = statement.executeQuery()) {
                assertThat(resultSet.next(), is(true));
                return resultSet.getString(1);
            }
        }
    }

    private int queryCount(Connection connection, String sql, String... parameters) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < parameters.length; i++) {
                statement.setString(i + 1, parameters[i]);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                assertThat(resultSet.next(), is(true));
                return resultSet.getInt(1);
            }
        }
    }
}
