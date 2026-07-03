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
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.keycloak.connections.jpa.updater.liquibase.ThreadLocalSessionContext;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RoleModel;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.changelog.ChangeSet;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.sql.Sql;
import liquibase.sqlgenerator.SqlGeneratorFactory;
import liquibase.statement.SqlStatement;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class JpaUpdate26_7_0_OrganizationRolesTest {

    private static final String ORGANIZATION_ROLE_CHANGELOG = "META-INF/jpa-changelog-26.7.0.xml";
    private static final List<String> ORGANIZATION_ROLE_CHANGESETS = List.of(
            "26.7.0-40585-organization-roles-schema",
            "26.7.0-40585-organization-roles-migrate-data",
            "26.7.0-40585-organization-roles-constraints"
    );

    @Test
    public void shouldMigrateRoleTypesAndExistingOrganizationDefaults() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1")) {
            createSchema(connection);
            seedData(connection);

            Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
            SqlStatement[] statements = new JpaUpdate26_7_0_OrganizationRoles().generateStatements(database);
            executeStatements(database, connection, statements);

            assertThat(roleType(connection, "realm-role"), is(RoleModel.Type.REALM.intValue()));
            assertThat(roleType(connection, "client-role"), is(RoleModel.Type.CLIENT.intValue()));

            String defaultRoleId = organizationDefaultRole(connection, "org-1");
            assertThat(defaultRoleId, notNullValue());
            assertThat(defaultRoleName(connection, defaultRoleId), is(Constants.DEFAULT_ROLES_ROLE_PREFIX + "-acme"));
            assertThat(roleType(connection, defaultRoleId), is(RoleModel.Type.ORGANIZATION.intValue()));
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
    public void shouldApplyOrganizationRoleChangelogWithoutNameCollisions() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1")) {
            createPre267Schema(connection);
            seedPre267Data(connection);

            assertThat(applyOrganizationRoleChangelog(connection, false), is(ORGANIZATION_ROLE_CHANGESETS));

            assertFinalOrganizationRoleSchema(connection);
            assertMigratedOrganizationRoleData(connection, Constants.DEFAULT_ROLES_ROLE_PREFIX + "-acme");
        }
    }

    @Test
    public void shouldApplyOrganizationRoleChangelogWithNameCollisions() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1")) {
            createPre267Schema(connection);
            seedPre267Data(connection);

            assertThat(applyOrganizationRoleChangelog(connection, true), is(ORGANIZATION_ROLE_CHANGESETS));

            assertFinalOrganizationRoleSchema(connection);
            assertMigratedOrganizationRoleData(connection, Constants.DEFAULT_ROLES_ROLE_PREFIX + "-acme-2");
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

    @Test
    public void shouldFailWhenDefaultOrganizationRoleNameCannotBeDetermined() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1")) {
            createSchema(connection);
            seedData(connection);
            insertOrganizationRoleNameConflict(connection);

            Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
            JpaUpdate26_7_0_OrganizationRoles task = new JpaUpdate26_7_0_OrganizationRoles();
            task.database = database;
            task.jdbcConnection = (JdbcConnection) database.getConnection();

            CustomChangeException exception = assertThrows(CustomChangeException.class,
                    () -> task.determineDefaultRoleName("org-1", Constants.DEFAULT_ROLES_ROLE_PREFIX + "-acme", 2));

            assertThat(exception.getMessage(), containsString("Unable to determine default organization role name"));
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
                    "TYPE INT, " +
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

    private void createPre267Schema(Connection connection) throws Exception {
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE REALM (ID VARCHAR(36) NOT NULL, CONSTRAINT PK_REALM PRIMARY KEY (ID))");
            statement.execute("INSERT INTO REALM (ID) VALUES ('realm-1')");
            statement.execute("CREATE TABLE KEYCLOAK_ROLE (" +
                    "ID VARCHAR(36) NOT NULL, " +
                    "CLIENT_REALM_CONSTRAINT VARCHAR(255), " +
                    "CLIENT_ROLE BOOLEAN, " +
                    "DESCRIPTION VARCHAR(255), " +
                    "NAME VARCHAR(255), " +
                    "REALM_ID VARCHAR(36), " +
                    "CLIENT VARCHAR(36), " +
                    "CONSTRAINT PK_KEYCLOAK_ROLE PRIMARY KEY (ID))");
            statement.execute("CREATE TABLE ORG (" +
                    "ID VARCHAR(36) NOT NULL, " +
                    "REALM_ID VARCHAR(36), " +
                    "ALIAS VARCHAR(255), " +
                    "NAME VARCHAR(255), " +
                    "GROUP_ID VARCHAR(36), " +
                    "CONSTRAINT PK_ORG PRIMARY KEY (ID))");
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
                    "TYPE INT)");
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
                    "TYPE INT)");
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

    private void seedPre267Data(Connection connection) throws Exception {
        try (Statement statement = connection.createStatement()) {
            statement.execute("INSERT INTO KEYCLOAK_ROLE (ID, CLIENT_REALM_CONSTRAINT, CLIENT_ROLE, NAME, REALM_ID, CLIENT) " +
                    "VALUES ('realm-role', 'realm-1', FALSE, 'realm-role', 'realm-1', NULL)");
            statement.execute("INSERT INTO KEYCLOAK_ROLE (ID, CLIENT_REALM_CONSTRAINT, CLIENT_ROLE, NAME, REALM_ID, CLIENT) " +
                    "VALUES ('client-role', 'client-1', TRUE, 'client-role', 'realm-1', 'client-1')");
            statement.execute("INSERT INTO ORG (ID, REALM_ID, ALIAS, NAME, GROUP_ID) " +
                    "VALUES ('org-1', 'realm-1', 'acme', 'Acme Inc', 'group-1')");
            statement.execute("INSERT INTO USER_GROUP_MEMBERSHIP (USER_ID, GROUP_ID) VALUES ('user-1', 'group-1')");
            statement.execute("INSERT INTO FED_USER_GROUP_MEMBERSHIP (USER_ID, GROUP_ID, REALM_ID, STORAGE_PROVIDER_ID) " +
                    "VALUES ('fed-user-1', 'group-1', 'realm-1', 'storage-1')");
        }
    }

    private void insertOrganizationRoleNameConflict(Connection connection) throws Exception {
        try (Statement statement = connection.createStatement()) {
            statement.execute("INSERT INTO KEYCLOAK_ROLE (ID, CLIENT_REALM_CONSTRAINT, CLIENT_ROLE, NAME, REALM_ID, ORG_ID) " +
                    "VALUES ('existing-org-role', 'org-1', FALSE, 'default-roles-acme', 'realm-1', 'org-1')");
            statement.execute("INSERT INTO KEYCLOAK_ROLE (ID, CLIENT_REALM_CONSTRAINT, CLIENT_ROLE, NAME, REALM_ID, ORG_ID) " +
                    "VALUES ('existing-org-role-1', 'org-1', FALSE, 'default-roles-acme-1', 'realm-1', 'org-1')");
        }
    }

    private List<String> applyOrganizationRoleChangelog(Connection connection, boolean insertNameCollisions) throws Exception {
        Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
        List<String> applied = new ArrayList<>();
        ThreadLocalSessionContext.setCurrentSession(proxy(KeycloakSession.class, (sessionProxy, method, args) -> null));
        try {
            Liquibase liquibase = new Liquibase(ORGANIZATION_ROLE_CHANGELOG, new ClassLoaderResourceAccessor(getClass().getClassLoader()), database);
            DatabaseChangeLog changeLog = liquibase.getDatabaseChangeLog();
            changeLog.validate(database, new Contexts(), new LabelExpression());
            for (ChangeSet changeSet : changeLog.getChangeSets()) {
                if (!ORGANIZATION_ROLE_CHANGESETS.contains(changeSet.getId())) {
                    continue;
                }
                if (insertNameCollisions && "26.7.0-40585-organization-roles-migrate-data".equals(changeSet.getId())) {
                    insertOrganizationRoleNameConflict(connection);
                    database.commit();
                }
                changeSet.execute(changeLog, database);
                applied.add(changeSet.getId());
                database.commit();
            }
            database.commit();
            return applied;
        } finally {
            ThreadLocalSessionContext.removeCurrentSession();
        }
    }

    private void assertFinalOrganizationRoleSchema(Connection connection) throws Exception {
        ColumnMetadata typeColumn = columnMetadata(connection, "KEYCLOAK_ROLE", "TYPE");
        assertThat(typeColumn.dataType(), is(Types.INTEGER));
        assertThat(typeColumn.nullable(), is(DatabaseMetaData.columnNoNulls));
        assertTrue(columnExists(connection, "KEYCLOAK_ROLE", "ORG_ID"));
        assertTrue(columnExists(connection, "ORG", "DEFAULT_ROLE_ID"));
        assertFalse(columnExists(connection, "KEYCLOAK_ROLE", "CLIENT_ROLE"));
        assertTrue(indexExists(connection, "KEYCLOAK_ROLE", "IDX_KEYCLOAK_ROLE_ORG_ID"));
        assertTrue(indexExists(connection, "KEYCLOAK_ROLE", "IDX_KEYCLOAK_ROLE_TYPE"));
        assertTrue(importedKeyExists(connection, "KEYCLOAK_ROLE", "FK_KEYCLOAK_ROLE_ORG"));
        assertTrue(importedKeyExists(connection, "ORG", "FK_ORG_DEFAULT_ROLE"));
    }

    private void assertMigratedOrganizationRoleData(Connection connection, String expectedDefaultRoleName) throws Exception {
        assertThat(roleType(connection, "realm-role"), is(RoleModel.Type.REALM.intValue()));
        assertThat(roleType(connection, "client-role"), is(RoleModel.Type.CLIENT.intValue()));
        assertThat(roleOrganization(connection, "realm-role"), nullValue());
        assertThat(roleOrganization(connection, "client-role"), nullValue());

        String defaultRoleId = organizationDefaultRole(connection, "org-1");
        assertThat(defaultRoleId, notNullValue());
        assertThat(defaultRoleName(connection, defaultRoleId), is(expectedDefaultRoleName));
        assertThat(roleType(connection, defaultRoleId), is(RoleModel.Type.ORGANIZATION.intValue()));
        assertThat(roleOrganization(connection, defaultRoleId), is("org-1"));
        assertThat(roleClientRealmConstraint(connection, defaultRoleId), is("org-1"));
        assertThat(localRoleMappings(connection, "user-1", defaultRoleId), is(1));
        assertThat(federatedRoleMappings(connection, "fed-user-1", defaultRoleId, "realm-1", "storage-1"), is(1));
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

    private int roleType(Connection connection, String roleId) throws Exception {
        return queryCount(connection, "SELECT TYPE FROM KEYCLOAK_ROLE WHERE ID = ?", roleId);
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

    private boolean columnExists(Connection connection, String tableName, String columnName) throws Exception {
        try (ResultSet resultSet = connection.getMetaData().getColumns(null, null, tableName, columnName)) {
            return resultSet.next();
        }
    }

    private ColumnMetadata columnMetadata(Connection connection, String tableName, String columnName) throws Exception {
        try (ResultSet resultSet = connection.getMetaData().getColumns(null, null, tableName, columnName)) {
            assertThat(resultSet.next(), is(true));
            return new ColumnMetadata(resultSet.getInt("DATA_TYPE"), resultSet.getInt("NULLABLE"));
        }
    }

    private boolean indexExists(Connection connection, String tableName, String indexName) throws Exception {
        try (ResultSet resultSet = connection.getMetaData().getIndexInfo(null, null, tableName, false, false)) {
            while (resultSet.next()) {
                if (indexName.equalsIgnoreCase(resultSet.getString("INDEX_NAME"))) {
                    return true;
                }
            }
            return false;
        }
    }

    private boolean importedKeyExists(Connection connection, String tableName, String constraintName) throws Exception {
        try (ResultSet resultSet = connection.getMetaData().getImportedKeys(null, null, tableName)) {
            while (resultSet.next()) {
                if (constraintName.equalsIgnoreCase(resultSet.getString("FK_NAME"))) {
                    return true;
                }
            }
            return false;
        }
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

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return (T) java.lang.reflect.Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type }, (proxy, method, args) -> {
            if (method.getDeclaringClass().equals(Object.class)) {
                return switch (method.getName()) {
                    case "equals" -> proxy == args[0];
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "toString" -> type.getSimpleName() + "@" + Integer.toHexString(System.identityHashCode(proxy));
                    default -> null;
                };
            }
            return handler.invoke(proxy, method, args);
        });
    }

    private record ColumnMetadata(int dataType, int nullable) {
    }
}
