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
package org.keycloak.connections.jpa.updater.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.keycloak.connections.jpa.updater.jdbc.ChangeLogHistory.ChangeSetKey;
import org.keycloak.enums.SqlDialect;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.migration.ChangeSetDescriptor;
import org.keycloak.representations.migration.ChangeSetStep;
import org.keycloak.representations.migration.MigrationManifest;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link JpaMigrationTaskRegistry}, {@link NativeSqlHelper},
 * and the {@link ChangeSetRunner} customTask dispatch.
 */
public class JpaMigrationTaskRegistryTest {

    private static final String H2_URL = "jdbc:h2:mem:registry_test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";

    private Connection connection;

    @Before
    public void setUp() throws Exception {
        connection = DriverManager.getConnection(H2_URL, "sa", "");
        connection.setAutoCommit(true);
    }

    @After
    public void tearDown() throws Exception {
        try (Statement st = connection.createStatement()) {
            st.execute("DROP TABLE IF EXISTS DATABASECHANGELOG");
            st.execute("DROP TABLE IF EXISTS REALM");
        }
        connection.close();
    }

    // -------------------------------------------------------------------------
    // JpaMigrationTaskRegistry
    // -------------------------------------------------------------------------

    @Test
    public void testAllManifestTasksAreRegistered() {
        MigrationManifest manifest = MigrationManifestLoader.loadFresh();
        List<String> unregistered = new ArrayList<>();
        for (ChangeSetDescriptor cs : manifest.getChangeSets()) {
            for (ChangeSetStep step : cs.getSteps()) {
                if ("customTask".equals(step.getType())) {
                    String className = step.getClassName();
                    if (!JpaMigrationTaskRegistry.isRegistered(className)) {
                        unregistered.add(className + " (change-set " + cs.getId() + ")");
                    }
                }
            }
        }
        Assert.assertTrue("Unregistered custom tasks in manifest: " + unregistered, unregistered.isEmpty());
    }

    @Test
    public void testRegistryLookupByFqn() {
        JpaMigrationTask task = JpaMigrationTaskRegistry.get(
                "org.keycloak.connections.jpa.updater.liquibase.custom.JpaUpdate26_8_0_BackfillGroupOrgId");
        Assert.assertNotNull("Expected task for BackfillGroupOrgId", task);
        Assert.assertNotNull("Task must have a taskId", task.getTaskId());
        Assert.assertTrue(JpaMigrationTaskRegistry.isRegistered(
                "org.keycloak.connections.jpa.updater.liquibase.custom.JpaUpdate26_8_0_BackfillGroupOrgId"));
    }

    @Test
    public void testRegistryFallbackClassLookupByClassName() {
        String className = "org.keycloak.connections.jpa.updater.jdbc.task.JpaUpdate26_8_0_BackfillGroupOrgIdTask";
        JpaMigrationTask task = JpaMigrationTaskRegistry.get(className);
        Assert.assertNotNull("Expected task instantiated via reflection fallback", task);
        Assert.assertEquals("Backfill KEYCLOAK_GROUP.ORG_ID from ORG.GROUP_ID", task.getTaskId());
        Assert.assertTrue(JpaMigrationTaskRegistry.isRegistered(className));
    }

    @Test
    public void testRegistryReturnsFreshInstance() {
        JpaMigrationTask t1 = JpaMigrationTaskRegistry.get(
                "org.keycloak.connections.jpa.updater.liquibase.custom.JpaUpdate22_0_5_UpdateAccountTheme");
        JpaMigrationTask t2 = JpaMigrationTaskRegistry.get(
                "org.keycloak.connections.jpa.updater.liquibase.custom.JpaUpdate22_0_5_UpdateAccountTheme");
        Assert.assertNotSame("Registry should return fresh instances", t1, t2);
    }

    @Test
    public void testRegistryReturnNullForUnknown() {
        Assert.assertNull(JpaMigrationTaskRegistry.get("com.example.NonExistentTask"));
        Assert.assertFalse(JpaMigrationTaskRegistry.isRegistered("com.example.NonExistentTask"));
    }

    @Test
    public void testRegistryReturnNullForNonTaskClass() {
        Assert.assertNull(JpaMigrationTaskRegistry.get(String.class.getName()));
        Assert.assertFalse(JpaMigrationTaskRegistry.isRegistered(String.class.getName()));
    }

    // -------------------------------------------------------------------------
    // NativeSqlHelper
    // -------------------------------------------------------------------------

    @Test
    public void testQualifiedWithSchema() {
        Assert.assertEquals("myschema.REALM", NativeSqlHelper.qualified("REALM", "myschema"));
    }

    @Test
    public void testQualifiedWithoutSchema() {
        Assert.assertEquals("REALM", NativeSqlHelper.qualified("REALM", null));
        Assert.assertEquals("REALM", NativeSqlHelper.qualified("REALM", ""));
        Assert.assertEquals("REALM", NativeSqlHelper.qualified("REALM", "   "));
    }

    @Test
    public void testQualifiedOmitsSchemaThatNeedsQuoting() {
        // a schema such as "keycloak-t" is applied with Connection.setSchema, not embedded in SQL
        Assert.assertEquals("REALM", NativeSqlHelper.qualified("REALM", "keycloak-t"));
    }

    @Test
    public void testSqlLiteralNull() {
        Assert.assertEquals("NULL", NativeSqlHelper.sqlLiteral(null));
    }

    @Test
    public void testSqlLiteralBoolean() {
        Assert.assertEquals("TRUE", NativeSqlHelper.sqlLiteral(true));
        Assert.assertEquals("FALSE", NativeSqlHelper.sqlLiteral(false));
    }

    @Test
    public void testSqlLiteralNumber() {
        Assert.assertEquals("42", NativeSqlHelper.sqlLiteral(42));
        Assert.assertEquals("3.14", NativeSqlHelper.sqlLiteral(3.14));
    }

    @Test
    public void testSqlLiteralString() {
        Assert.assertEquals("'hello'", NativeSqlHelper.sqlLiteral("hello"));
        Assert.assertEquals("'it''s'", NativeSqlHelper.sqlLiteral("it's"));
    }

    @Test
    public void testIsApplicableEmptyDb() throws Exception {
        // No REALM table → not applicable
        Assert.assertFalse(NativeSqlHelper.isApplicable(connection, null, SqlDialect.H2));
    }

    @Test
    public void testIsApplicableEmptyRealmTable() throws Exception {
        try (Statement st = connection.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS REALM (ID VARCHAR(36) NOT NULL PRIMARY KEY)");
        }
        // REALM table exists but has no rows → not applicable
        Assert.assertFalse(NativeSqlHelper.isApplicable(connection, null, SqlDialect.H2));
    }

    @Test
    public void testIsApplicableWithRow() throws Exception {
        try (Statement st = connection.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS REALM (ID VARCHAR(36) NOT NULL PRIMARY KEY)");
            st.execute("INSERT INTO REALM VALUES ('realm-1')");
        }
        Assert.assertTrue(NativeSqlHelper.isApplicable(connection, null, SqlDialect.H2));
    }

    @Test
    public void testTaskIsApplicableDefaultsToRealmRowCheck() throws Exception {
        JpaMigrationTask task = JpaMigrationTaskRegistry.get(
                "org.keycloak.connections.jpa.updater.liquibase.custom.RemoveDuplicateOfflineSessions");
        Assert.assertFalse(task.isApplicable(connection, null, SqlDialect.H2));
        try (Statement st = connection.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS REALM (ID VARCHAR(36) NOT NULL PRIMARY KEY)");
            st.execute("INSERT INTO REALM VALUES ('realm-1')");
        }
        Assert.assertTrue(task.isApplicable(connection, null, SqlDialect.H2));
    }

    @Test
    public void testOrgCharsetTaskAlwaysApplicable() throws Exception {
        JpaMigrationTask task = JpaMigrationTaskRegistry.get(
                "org.keycloak.connections.jpa.updater.liquibase.custom.JpaUpdate26_5_0_OrgTablesSetDefaultCharsetAndCollation");
        Assert.assertTrue(task.isApplicable(connection, null, SqlDialect.H2));
    }

    // -------------------------------------------------------------------------
    // ChangeSetRunner customTask dispatch
    // -------------------------------------------------------------------------

    @Test
    public void testCustomTaskDispatchViaRunner() throws Exception {
        // Register a test task that returns a known SQL statement
        final List<String> capturedSql = new ArrayList<>();

        // Use a real task that doesn't need DB state — JpaUpdate22_0_5_UpdateAccountTheme
        // It returns one UPDATE statement; we verify the runner collects it.
        connection.setAutoCommit(false);

        String ddl = HistoryTableManager.createTableSql(SqlDialect.H2, null, ChangeLogHistory.CHANGELOG_TABLE);
        try (Statement st = connection.createStatement()) {
            st.execute(ddl);
            // Tasks are only applicable once the REALM table has data
            st.execute("CREATE TABLE IF NOT EXISTS REALM (ID VARCHAR(36) NOT NULL PRIMARY KEY)");
            st.execute("INSERT INTO REALM VALUES ('realm-1')");
        }
        connection.commit();

        // Build a manifest with a single customTask change-set
        MigrationManifest manifest = new MigrationManifest();
        ChangeSetDescriptor cs = new ChangeSetDescriptor();
        cs.setId("test-custom-task");
        cs.setAuthor("test");
        cs.setChangeLogFile("test.xml");
        ChangeSetStep step = new ChangeSetStep();
        step.setType("customTask");
        step.setClassName("org.keycloak.connections.jpa.updater.liquibase.custom.JpaUpdate22_0_5_UpdateAccountTheme");
        cs.getSteps().add(step);
        manifest.getChangeSets().add(cs);

        // Capture what the sink executes
        SqlSink capturingSink = new SqlSink() {
            @Override
            public void execute(String sql, String changeSetId) {
                capturedSql.add(sql);
            }
            @Override
            public void comment(String text) {}
            @Override
            public boolean isExportMode() { return false; }
        };

        Set<ChangeSetKey> executed = ChangeLogHistory.loadExecuted(connection, null, ChangeLogHistory.CHANGELOG_TABLE);
        ChangeSetRunner runner = new ChangeSetRunner(connection, null, ChangeLogHistory.CHANGELOG_TABLE,
                SqlDialect.H2, manifest, (KeycloakSession) null);
        runner.run(capturingSink, executed, "1234567890", 0);

        // The task generates one UPDATE statement + the history INSERT
        boolean foundUpdate = capturedSql.stream().anyMatch(s -> s.toUpperCase().startsWith("UPDATE"));
        Assert.assertTrue("Expected at least one UPDATE from customTask", foundUpdate);

        connection.setAutoCommit(true);
    }
}
