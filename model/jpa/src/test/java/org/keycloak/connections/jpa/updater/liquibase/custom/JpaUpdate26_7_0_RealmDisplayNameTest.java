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

import org.keycloak.connections.jpa.updater.liquibase.LiquibaseConstants;

import liquibase.Scope;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.executor.Executor;
import liquibase.executor.ExecutorService;
import liquibase.statement.SqlStatement;
import org.junit.Assert;
import org.junit.Test;

/**
 * Verifies the migration of the displayName attribute to the REALM.DISPLAY_NAME column, in particular that values
 * longer than the column are truncated during the migration instead of failing the change set on databases that
 * enforce the column length (see https://github.com/keycloak/keycloak/issues/54004).
 */
public class JpaUpdate26_7_0_RealmDisplayNameTest {

    private static final String EMOJI = "\uD83D\uDE00";

    @Test
    public void testMigrateTruncatesValuesTooLongForColumn() throws Exception {
        String longDisplayName = "x".repeat(300);
        String displayNameWithAstralChars = EMOJI.repeat(130) + "y".repeat(100);
        String displayNameAtColumnLimit = "e".repeat(255);

        try (Connection connection = DriverManager.getConnection("jdbc:h2:mem:realmDisplayNameMigrationTest;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1")) {
            connection.setAutoCommit(false);
            try (Statement statement = connection.createStatement()) {
                statement.execute("CREATE TABLE REALM (ID VARCHAR(36) PRIMARY KEY, DISPLAY_NAME NVARCHAR(255))");
                statement.execute("CREATE TABLE REALM_ATTRIBUTE (NAME VARCHAR(255) NOT NULL, VALUE NCLOB, REALM_ID VARCHAR(36) NOT NULL)");
            }
            insertRealm(connection, "realm-long");
            insertRealmAttribute(connection, "realm-long", longDisplayName);
            insertRealm(connection, "realm-astral");
            insertRealmAttribute(connection, "realm-astral", displayNameWithAstralChars);
            insertRealm(connection, "realm-at-limit");
            insertRealmAttribute(connection, "realm-at-limit", displayNameAtColumnLimit);
            insertRealm(connection, "realm-short");
            insertRealmAttribute(connection, "realm-short", "short-display-name");
            insertRealm(connection, "realm-null");
            insertRealmAttribute(connection, "realm-null", null);
            insertRealm(connection, "realm-without-attribute");
            connection.commit();

            JpaUpdate26_7_0_RealmDisplayName task = new JpaUpdate26_7_0_RealmDisplayName();
            Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
            SqlStatement[] statements = task.generateStatements(database);
            Executor executor = Scope.getCurrentScope().getSingleton(ExecutorService.class).getExecutor(LiquibaseConstants.JDBC_EXECUTOR, database);
            for (SqlStatement sqlStatement : statements) {
                executor.execute(sqlStatement);
            }
            connection.commit();

            Assert.assertEquals("x".repeat(255), displayName(connection, "realm-long"));
            // 255 UTF-16 code units is the middle of the 128th emoji; the pair must not be split
            Assert.assertEquals(EMOJI.repeat(127), displayName(connection, "realm-astral"));
            Assert.assertEquals(displayNameAtColumnLimit, displayName(connection, "realm-at-limit"));
            Assert.assertEquals("short-display-name", displayName(connection, "realm-short"));
            Assert.assertNull(displayName(connection, "realm-null"));
            Assert.assertNull(displayName(connection, "realm-without-attribute"));

            Assert.assertTrue(task.getConfirmationMessage(), task.getConfirmationMessage().contains("2 value(s) were truncated"));
        }
    }

    private void insertRealm(Connection connection, String realmId) throws Exception {
        try (PreparedStatement ps = connection.prepareStatement("INSERT INTO REALM (ID) VALUES (?)")) {
            ps.setString(1, realmId);
            ps.executeUpdate();
        }
    }

    private void insertRealmAttribute(Connection connection, String realmId, String displayName) throws Exception {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO REALM_ATTRIBUTE (NAME, VALUE, REALM_ID) VALUES ('displayName', ?, ?)")) {
            ps.setString(1, displayName);
            ps.setString(2, realmId);
            ps.executeUpdate();
        }
    }

    private String displayName(Connection connection, String realmId) throws Exception {
        try (PreparedStatement ps = connection.prepareStatement("SELECT DISPLAY_NAME FROM REALM WHERE ID = ?")) {
            ps.setString(1, realmId);
            try (ResultSet rs = ps.executeQuery()) {
                Assert.assertTrue(rs.next());
                return rs.getString(1);
            }
        }
    }
}
