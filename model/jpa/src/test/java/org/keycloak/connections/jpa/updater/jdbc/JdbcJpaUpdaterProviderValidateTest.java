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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.keycloak.connections.jpa.updater.JpaUpdaterProvider.Status;
import org.keycloak.connections.jpa.updater.jdbc.ChangeLogHistory.ChangeSetKey;
import org.keycloak.enums.SqlDialect;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.migration.ChangeSetDescriptor;
import org.keycloak.representations.migration.ChangeSetStep;
import org.keycloak.representations.migration.MigrationManifest;
import org.keycloak.representations.migration.PreconditionContainerDescriptor;
import org.keycloak.representations.migration.PreconditionDescriptor;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link JdbcJpaUpdaterProvider#validate(Connection, String)} and the
 * supporting classes {@link ChangeLogHistory}, {@link PreconditionEvaluator}, and
 * {@link SqlDialect}.
 *
 * <p>All tests run against an in-memory H2 database so no external server is needed.
 */
public class JdbcJpaUpdaterProviderValidateTest {

    private static final String H2_URL = "jdbc:h2:mem:validate_test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";

    private Connection connection;

    @Before
    public void setUp() throws Exception {
        connection = DriverManager.getConnection(H2_URL, "sa", "");
        connection.setAutoCommit(true);
    }

    @After
    public void tearDown() throws Exception {
        // Drop history table so tests are independent
        try (Statement st = connection.createStatement()) {
            st.execute("DROP TABLE IF EXISTS DATABASECHANGELOG");
            st.execute("DROP TABLE IF EXISTS DATABASECHANGELOG_SAMPLE");
            st.execute("DROP TABLE IF EXISTS TEST_TABLE");
            st.execute("DROP TABLE IF EXISTS ANOTHER_TABLE");
            st.execute("DROP TABLE IF EXISTS CUSTOM_ENTITY");
        }
        connection.close();
    }

    // -------------------------------------------------------------------------
    // SqlDialect detection
    // -------------------------------------------------------------------------

    @Test
    public void testDialectDetectionH2() throws Exception {
        SqlDialect dialect = SqlDialect.of(connection);
        Assert.assertEquals(SqlDialect.H2, dialect);
        Assert.assertEquals("h2", dialect.manifestKey());
    }

    @Test
    public void testDialectMatchesDbmsType() {
        Assert.assertTrue(SqlDialect.H2.matchesDbmsType("h2"));
        Assert.assertTrue(SqlDialect.POSTGRESQL.matchesDbmsType("postgresql"));
        Assert.assertFalse(SqlDialect.H2.matchesDbmsType("oracle"));
        Assert.assertFalse(SqlDialect.H2.matchesDbmsType(null));
    }

    // -------------------------------------------------------------------------
    // ChangeLogHistory
    // -------------------------------------------------------------------------

    @Test
    public void testTableExistsFalseOnEmptyDb() throws Exception {
        Assert.assertFalse(ChangeLogHistory.tableExists(connection, null));
    }

    @Test
    public void testTableExistsTrueAfterCreate() throws Exception {
        createChangelogTable();
        Assert.assertTrue(ChangeLogHistory.tableExists(connection, null));
    }

    @Test
    public void testLoadExecutedEmpty() throws Exception {
        // No history table — should return empty set without exception
        Set<ChangeSetKey> executed = ChangeLogHistory.loadExecuted(connection, null, ChangeLogHistory.CHANGELOG_TABLE);
        Assert.assertTrue(executed.isEmpty());
    }

    @Test
    public void testMaxOrderExecutedMissingTable() throws Exception {
        // Export mode on an empty database: the history table is only written to the script
        Assert.assertEquals(0, ChangeLogHistory.maxOrderExecuted(connection, null, ChangeLogHistory.CHANGELOG_TABLE));
    }

    @Test
    public void testLoadExecutedWithRows() throws Exception {
        createChangelogTable();
        insertHistoryRow("cs1", "author1", "file1.xml");
        insertHistoryRow("cs2", "author2", "file2.xml");

        Set<ChangeSetKey> executed = ChangeLogHistory.loadExecuted(connection, null, ChangeLogHistory.CHANGELOG_TABLE);
        Assert.assertEquals(2, executed.size());
        Assert.assertTrue(executed.contains(new ChangeSetKey("cs1", "author1", "file1.xml")));
        Assert.assertTrue(executed.contains(new ChangeSetKey("cs2", "author2", "file2.xml")));
    }

    @Test
    public void testChangeSetKeyEqualityCaseInsensitive() {
        ChangeSetKey key1 = new ChangeSetKey("CS1", "Author1", "File1.xml");
        ChangeSetKey key2 = new ChangeSetKey("cs1", "author1", "file1.xml");
        Assert.assertEquals(key1, key2);
        Assert.assertEquals(key1.hashCode(), key2.hashCode());
    }

    @Test
    public void testMissingDeploymentIdColumn() throws Exception {
        createChangelogTable();
        // DATABASECHANGELOG table without DEPLOYMENT_ID column
        try (Statement st = connection.createStatement()) {
            st.execute("ALTER TABLE DATABASECHANGELOG DROP COLUMN DEPLOYMENT_ID");
        }
        Assert.assertTrue(ChangeLogHistory.missingDeploymentIdColumn(connection, null));
    }

    @Test
    public void testDeploymentIdColumnPresent() throws Exception {
        createChangelogTable();
        Assert.assertFalse(ChangeLogHistory.missingDeploymentIdColumn(connection, null));
    }

    // -------------------------------------------------------------------------
    // ChangeLogHistory.computePending
    // -------------------------------------------------------------------------

    @Test
    public void testComputePendingAllPending() throws Exception {
        MigrationManifest manifest = twoChangeSetManifest();
        Set<ChangeSetKey> executed = new HashSet<>();
        List<ChangeSetDescriptor> pending = ChangeLogHistory.computePending(manifest, executed, SqlDialect.H2);
        Assert.assertEquals(2, pending.size());
    }

    @Test
    public void testComputePendingNonePending() throws Exception {
        MigrationManifest manifest = twoChangeSetManifest();
        Set<ChangeSetKey> executed = new HashSet<>();
        for (ChangeSetDescriptor cs : manifest.getChangeSets()) {
            executed.add(new ChangeSetKey(cs.getId(), cs.getAuthor(), cs.getChangeLogFile()));
        }
        List<ChangeSetDescriptor> pending = ChangeLogHistory.computePending(manifest, executed, SqlDialect.H2);
        Assert.assertTrue(pending.isEmpty());
    }

    @Test
    public void testComputePendingDbmsFiltered() throws Exception {
        // A change-set restricted to oracle should not appear as pending on H2
        MigrationManifest manifest = new MigrationManifest();
        ChangeSetDescriptor cs = makeChangeSet("oracle-only", "author", "file.xml");
        cs.setDbms(Set.of("oracle"));
        manifest.getChangeSets().add(cs);

        List<ChangeSetDescriptor> pending = ChangeLogHistory.computePending(manifest, new HashSet<>(), SqlDialect.H2);
        Assert.assertTrue(pending.isEmpty());
    }

    @Test
    public void testComputePendingDbmsMatchH2() throws Exception {
        // A change-set restricted to h2 SHOULD appear as pending on H2
        MigrationManifest manifest = new MigrationManifest();
        ChangeSetDescriptor cs = makeChangeSet("h2-only", "author", "file.xml");
        cs.setDbms(Set.of("h2"));
        manifest.getChangeSets().add(cs);

        List<ChangeSetDescriptor> pending = ChangeLogHistory.computePending(manifest, new HashSet<>(), SqlDialect.H2);
        Assert.assertEquals(1, pending.size());
    }

    // -------------------------------------------------------------------------
    // PreconditionEvaluator
    // -------------------------------------------------------------------------

    @Test
    public void testPreconditionNullContainer() throws Exception {
        PreconditionEvaluator eval = makeEvaluator(new HashSet<>(), false);
        Assert.assertEquals(PreconditionEvaluator.Outcome.RUN, eval.evaluate(null));
    }

    @Test
    public void testPreconditionTableExistsTrue() throws Exception {
        createTestTable();
        PreconditionContainerDescriptor container = markRanContainer(tableExistsDesc("TEST_TABLE"));
        PreconditionEvaluator eval = makeEvaluator(new HashSet<>(), false);
        Assert.assertEquals(PreconditionEvaluator.Outcome.RUN, eval.evaluate(container));
    }

    @Test
    public void testPreconditionTableExistsFalse() throws Exception {
        PreconditionContainerDescriptor container = markRanContainer(tableExistsDesc("TEST_TABLE"));
        PreconditionEvaluator eval = makeEvaluator(new HashSet<>(), false);
        Assert.assertEquals(PreconditionEvaluator.Outcome.MARK_RAN, eval.evaluate(container));
    }

    @Test
    public void testPreconditionColumnExistsTrue() throws Exception {
        createTestTableWithColumns();
        PreconditionContainerDescriptor container = markRanContainer(columnExistsDesc("TEST_TABLE", "COL_A"));
        PreconditionEvaluator eval = makeEvaluator(new HashSet<>(), false);
        Assert.assertEquals(PreconditionEvaluator.Outcome.RUN, eval.evaluate(container));
    }

    @Test
    public void testPreconditionColumnExistsFalse() throws Exception {
        createTestTable();
        PreconditionContainerDescriptor container = markRanContainer(columnExistsDesc("TEST_TABLE", "NONEXISTENT"));
        PreconditionEvaluator eval = makeEvaluator(new HashSet<>(), false);
        Assert.assertEquals(PreconditionEvaluator.Outcome.MARK_RAN, eval.evaluate(container));
    }

    @Test
    public void testPreconditionIndexExistsTrue() throws Exception {
        createTestTableWithIndex();
        PreconditionContainerDescriptor container = markRanContainer(indexExistsDesc("TEST_TABLE", "IDX_TEST_COL"));
        PreconditionEvaluator eval = makeEvaluator(new HashSet<>(), false);
        Assert.assertEquals(PreconditionEvaluator.Outcome.RUN, eval.evaluate(container));
    }

    @Test
    public void testPreconditionIndexExistsFalse() throws Exception {
        createTestTable();
        PreconditionContainerDescriptor container = markRanContainer(indexExistsDesc("TEST_TABLE", "IDX_MISSING"));
        PreconditionEvaluator eval = makeEvaluator(new HashSet<>(), false);
        Assert.assertEquals(PreconditionEvaluator.Outcome.MARK_RAN, eval.evaluate(container));
    }

    @Test
    public void testPreconditionIndexExistsNoTableName() throws Exception {
        createTestTableWithIndex();
        // indexExists without table name — scan all tables
        PreconditionContainerDescriptor container = markRanContainer(indexExistsDesc(null, "IDX_TEST_COL"));
        PreconditionEvaluator eval = makeEvaluator(new HashSet<>(), false);
        Assert.assertEquals(PreconditionEvaluator.Outcome.RUN, eval.evaluate(container));
    }

    @Test
    public void testPreconditionDbmsTrue() throws Exception {
        PreconditionContainerDescriptor container = markRanContainer(dbmsDesc("h2"));
        PreconditionEvaluator eval = makeEvaluator(new HashSet<>(), false);
        Assert.assertEquals(PreconditionEvaluator.Outcome.RUN, eval.evaluate(container));
    }

    @Test
    public void testPreconditionDbmsFalse() throws Exception {
        PreconditionContainerDescriptor container = markRanContainer(dbmsDesc("oracle"));
        PreconditionEvaluator eval = makeEvaluator(new HashSet<>(), false);
        Assert.assertEquals(PreconditionEvaluator.Outcome.MARK_RAN, eval.evaluate(container));
    }

    @Test
    public void testPreconditionNotTrue() throws Exception {
        // NOT(tableExists("MISSING_TABLE")) → true → RUN
        PreconditionDescriptor notDesc = notDesc(tableExistsDesc("MISSING_TABLE"));
        PreconditionContainerDescriptor container = markRanContainer(notDesc);
        PreconditionEvaluator eval = makeEvaluator(new HashSet<>(), false);
        Assert.assertEquals(PreconditionEvaluator.Outcome.RUN, eval.evaluate(container));
    }

    @Test
    public void testPreconditionNotWithSeveralChildrenFailsIfAnyChildPasses() throws Exception {
        // Liquibase semantics: <not> is true only if NO nested precondition passes
        createTestTable();
        PreconditionDescriptor notDesc = notDesc(tableExistsDesc("MISSING_TABLE"));
        notDesc.addNestedCondition(tableExistsDesc("TEST_TABLE"));
        PreconditionEvaluator eval = makeEvaluator(new HashSet<>(), false);
        Assert.assertEquals(PreconditionEvaluator.Outcome.MARK_RAN, eval.evaluate(markRanContainer(notDesc)));

        PreconditionDescriptor noneMatch = notDesc(tableExistsDesc("MISSING_TABLE"));
        noneMatch.addNestedCondition(tableExistsDesc("OTHER_MISSING_TABLE"));
        Assert.assertEquals(PreconditionEvaluator.Outcome.RUN, eval.evaluate(markRanContainer(noneMatch)));
    }

    @Test
    public void testScriptSqlSinkTerminators() throws Exception {
        java.io.StringWriter oracle = new java.io.StringWriter();
        new ScriptSqlSink(oracle, SqlDialect.ORACLE).execute("CREATE TABLE T (ID INT)", "cs");
        Assert.assertTrue(oracle.toString().startsWith("CREATE TABLE T (ID INT);"));
        Assert.assertFalse(oracle.toString().contains("GO"));

        java.io.StringWriter mssql = new java.io.StringWriter();
        new ScriptSqlSink(mssql, SqlDialect.MSSQL).execute("CREATE TABLE T (ID INT)", "cs");
        Assert.assertTrue(mssql.toString().contains("\nGO") || mssql.toString().contains("\r\nGO"));
        Assert.assertFalse(mssql.toString().contains(";"));
    }

    @Test
    public void testValidateOnEmptyDatabaseReturnsEmpty() throws Exception {
        KeycloakSession session = (KeycloakSession) java.lang.reflect.Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[] { KeycloakSession.class }, (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getAttributeOrDefault": return args[1];
                        case "getAllProviders": return new HashSet<>();
                        default: throw new UnsupportedOperationException(method.getName());
                    }
                });
        JdbcJpaUpdaterProvider provider = new JdbcJpaUpdaterProvider(session, ChangeSetRunner.DEFAULT_INDEX_THRESHOLD);
        Assert.assertEquals(Status.EMPTY, provider.validate(connection, null));
        // Validation runs without the database lock, so it must not create the history table (#54321)
        Assert.assertFalse(ChangeLogHistory.tableExists(connection, null));
    }

    @Test
    public void testPreconditionAndTrue() throws Exception {
        createTestTable();
        PreconditionDescriptor andDesc = andDesc(tableExistsDesc("TEST_TABLE"), dbmsDesc("h2"));
        PreconditionContainerDescriptor container = markRanContainer(andDesc);
        PreconditionEvaluator eval = makeEvaluator(new HashSet<>(), false);
        Assert.assertEquals(PreconditionEvaluator.Outcome.RUN, eval.evaluate(container));
    }

    @Test
    public void testPreconditionAndFalse() throws Exception {
        createTestTable();
        PreconditionDescriptor andDesc = andDesc(tableExistsDesc("TEST_TABLE"), dbmsDesc("oracle"));
        PreconditionContainerDescriptor container = markRanContainer(andDesc);
        PreconditionEvaluator eval = makeEvaluator(new HashSet<>(), false);
        Assert.assertEquals(PreconditionEvaluator.Outcome.MARK_RAN, eval.evaluate(container));
    }

    @Test
    public void testPreconditionChangeSetExecutedTrue() throws Exception {
        Set<ChangeSetKey> executed = new HashSet<>();
        executed.add(new ChangeSetKey("cs1", "author1", "file1.xml"));
        PreconditionContainerDescriptor container = markRanContainer(changeSetExecutedDesc("cs1", "author1", "file1.xml"));
        PreconditionEvaluator eval = makeEvaluator(executed, false);
        Assert.assertEquals(PreconditionEvaluator.Outcome.RUN, eval.evaluate(container));
    }

    @Test
    public void testPreconditionChangeSetExecutedFalse() throws Exception {
        PreconditionContainerDescriptor container = markRanContainer(changeSetExecutedDesc("cs1", "author1", "file1.xml"));
        PreconditionEvaluator eval = makeEvaluator(new HashSet<>(), false);
        Assert.assertEquals(PreconditionEvaluator.Outcome.MARK_RAN, eval.evaluate(container));
    }

    @Test
    public void testPreconditionHaltOutcome() throws Exception {
        PreconditionContainerDescriptor container = haltContainer(dbmsDesc("oracle"));
        PreconditionEvaluator eval = makeEvaluator(new HashSet<>(), false);
        Assert.assertEquals(PreconditionEvaluator.Outcome.HALT, eval.evaluate(container));
    }

    @Test
    public void testPreconditionOnSqlOutputIgnoreInExportMode() throws Exception {
        // onSqlOutput=IGNORE + export mode → SKIP_EXPORT regardless of condition
        PreconditionContainerDescriptor container = new PreconditionContainerDescriptor();
        container.setOnFail("MARK_RAN");
        container.setOnSqlOutput("IGNORE");
        container.getConditions().add(dbmsDesc("oracle")); // would be false for H2
        PreconditionEvaluator eval = makeEvaluator(new HashSet<>(), true);
        Assert.assertEquals(PreconditionEvaluator.Outcome.SKIP_EXPORT, eval.evaluate(container));
    }

    @Test
    public void testPreconditionOnSqlOutputIgnoreNotInExportMode() throws Exception {
        // onSqlOutput=IGNORE + NOT export mode → normal evaluation (oracle precondition fails → MARK_RAN)
        PreconditionContainerDescriptor container = new PreconditionContainerDescriptor();
        container.setOnFail("MARK_RAN");
        container.setOnSqlOutput("IGNORE");
        container.getConditions().add(dbmsDesc("oracle"));
        PreconditionEvaluator eval = makeEvaluator(new HashSet<>(), false);
        Assert.assertEquals(PreconditionEvaluator.Outcome.MARK_RAN, eval.evaluate(container));
    }

    // -------------------------------------------------------------------------
    // MigrationManifestLoader
    // -------------------------------------------------------------------------

    @Test
    public void testManifestLoads() {
        MigrationManifest manifest = MigrationManifestLoader.loadFresh();
        Assert.assertNotNull(manifest);
        Assert.assertFalse(manifest.getChangeSets().isEmpty());
        Assert.assertEquals(237, manifest.getChangeSets().size());
    }

    // -------------------------------------------------------------------------
    // validate() end-to-end using real manifest
    // -------------------------------------------------------------------------

    @Test
    public void testValidateEmptyDatabase() throws Exception {
        // No history table → EMPTY
        MigrationManifest manifest = MigrationManifestLoader.loadFresh();
        SqlDialect dialect = SqlDialect.of(connection);
        Set<ChangeSetKey> executed = ChangeLogHistory.loadExecuted(connection, null, ChangeLogHistory.CHANGELOG_TABLE);
        List<ChangeSetDescriptor> pending = ChangeLogHistory.computePending(manifest, executed, dialect);
        Status status = computeStatus(pending, manifest.getChangeSets().size());
        Assert.assertEquals(Status.EMPTY, status);
    }

    @Test
    public void testValidateUpToDate() throws Exception {
        // Create history table with all change-sets → VALID
        createChangelogTable();
        MigrationManifest manifest = MigrationManifestLoader.loadFresh();
        for (ChangeSetDescriptor cs : manifest.getChangeSets()) {
            insertHistoryRow(cs.getId(), cs.getAuthor(), cs.getChangeLogFile());
        }
        SqlDialect dialect = SqlDialect.of(connection);
        Set<ChangeSetKey> executed = ChangeLogHistory.loadExecuted(connection, null, ChangeLogHistory.CHANGELOG_TABLE);
        List<ChangeSetDescriptor> pending = ChangeLogHistory.computePending(manifest, executed, dialect);
        Status status = computeStatus(pending, manifest.getChangeSets().size());
        Assert.assertEquals(Status.VALID, status);
    }

    @Test
    public void testValidateOutdated() throws Exception {
        // Create history table with only the first change-set → OUTDATED
        createChangelogTable();
        MigrationManifest manifest = MigrationManifestLoader.loadFresh();
        ChangeSetDescriptor first = manifest.getChangeSets().get(0);
        insertHistoryRow(first.getId(), first.getAuthor(), first.getChangeLogFile());

        SqlDialect dialect = SqlDialect.of(connection);
        Set<ChangeSetKey> executed = ChangeLogHistory.loadExecuted(connection, null, ChangeLogHistory.CHANGELOG_TABLE);
        List<ChangeSetDescriptor> pending = ChangeLogHistory.computePending(manifest, executed, dialect);
        Status status = computeStatus(pending, manifest.getChangeSets().size());
        Assert.assertEquals(Status.OUTDATED, status);
    }

    @Test
    public void testValidateMissingHistoryTableIsEmpty() throws Exception {
        // tableExists() should return false → EMPTY
        Assert.assertFalse(ChangeLogHistory.tableExists(connection, null));
        // Simulate the logic in validate()
        MigrationManifest manifest = MigrationManifestLoader.loadFresh();
        SqlDialect dialect = SqlDialect.of(connection);
        Set<ChangeSetKey> executed = ChangeLogHistory.loadExecuted(connection, null, ChangeLogHistory.CHANGELOG_TABLE);
        Assert.assertTrue(executed.isEmpty());
        List<ChangeSetDescriptor> pending = ChangeLogHistory.computePending(manifest, executed, dialect);
        Status status = computeStatus(pending, manifest.getChangeSets().size());
        Assert.assertEquals(Status.EMPTY, status);
    }

    // -------------------------------------------------------------------------
    // Helpers for status computation (mirrors JdbcJpaUpdaterProvider logic)
    // -------------------------------------------------------------------------

    private static Status computeStatus(List<ChangeSetDescriptor> pending, int total) {
        if (pending.isEmpty()) return Status.VALID;
        if (pending.size() == total) return Status.EMPTY;
        return Status.OUTDATED;
    }

    // -------------------------------------------------------------------------
    // DB helpers
    // -------------------------------------------------------------------------

    private void createChangelogTable() throws Exception {
        try (Statement st = connection.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS DATABASECHANGELOG (" +
                    "ID VARCHAR(255) NOT NULL, " +
                    "AUTHOR VARCHAR(255) NOT NULL, " +
                    "FILENAME VARCHAR(255) NOT NULL, " +
                    "DATEEXECUTED TIMESTAMP NOT NULL, " +
                    "ORDEREXECUTED INT NOT NULL, " +
                    "EXECTYPE VARCHAR(10) NOT NULL, " +
                    "MD5SUM VARCHAR(35), " +
                    "DESCRIPTION VARCHAR(255), " +
                    "COMMENTS VARCHAR(255), " +
                    "TAG VARCHAR(255), " +
                    "LIQUIBASE VARCHAR(20), " +
                    "CONTEXTS VARCHAR(255), " +
                    "LABELS VARCHAR(255), " +
                    "DEPLOYMENT_ID VARCHAR(10))");
        }
    }

    private void insertHistoryRow(String id, String author, String filename) throws Exception {
        try (Statement st = connection.createStatement()) {
            st.execute("INSERT INTO DATABASECHANGELOG (ID, AUTHOR, FILENAME, DATEEXECUTED, ORDEREXECUTED, EXECTYPE) " +
                    "VALUES ('" + id.replace("'", "''") + "', '" + author.replace("'", "''") + "', '" +
                    filename.replace("'", "''") + "', CURRENT_TIMESTAMP, 1, 'EXECUTED')");
        }
    }

    private void createTestTable() throws Exception {
        try (Statement st = connection.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS TEST_TABLE (ID VARCHAR(36) NOT NULL PRIMARY KEY)");
        }
    }

    private void createTestTableWithColumns() throws Exception {
        try (Statement st = connection.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS TEST_TABLE (ID VARCHAR(36) NOT NULL PRIMARY KEY, COL_A VARCHAR(255))");
        }
    }

    private void createTestTableWithIndex() throws Exception {
        createTestTableWithColumns();
        try (Statement st = connection.createStatement()) {
            st.execute("CREATE INDEX IF NOT EXISTS IDX_TEST_COL ON TEST_TABLE (COL_A)");
        }
    }

    // -------------------------------------------------------------------------
    // Precondition descriptor builders
    // -------------------------------------------------------------------------

    private PreconditionEvaluator makeEvaluator(Set<ChangeSetKey> executed, boolean exportMode) throws Exception {
        return new PreconditionEvaluator(connection, null, SqlDialect.H2, executed, exportMode);
    }

    private static PreconditionContainerDescriptor markRanContainer(PreconditionDescriptor condition) {
        PreconditionContainerDescriptor c = new PreconditionContainerDescriptor();
        c.setOnFail("MARK_RAN");
        c.getConditions().add(condition);
        return c;
    }

    private static PreconditionContainerDescriptor haltContainer(PreconditionDescriptor condition) {
        PreconditionContainerDescriptor c = new PreconditionContainerDescriptor();
        c.setOnFail("HALT");
        c.getConditions().add(condition);
        return c;
    }

    private static PreconditionDescriptor tableExistsDesc(String tableName) {
        PreconditionDescriptor d = new PreconditionDescriptor();
        d.setType("tableExists");
        d.setTableName(tableName);
        return d;
    }

    private static PreconditionDescriptor columnExistsDesc(String table, String column) {
        PreconditionDescriptor d = new PreconditionDescriptor();
        d.setType("columnExists");
        d.setTableName(table);
        d.setColumnName(column);
        return d;
    }

    private static PreconditionDescriptor indexExistsDesc(String table, String index) {
        PreconditionDescriptor d = new PreconditionDescriptor();
        d.setType("indexExists");
        d.setTableName(table);
        d.setIndexName(index);
        return d;
    }

    private static PreconditionDescriptor dbmsDesc(String dbmsType) {
        PreconditionDescriptor d = new PreconditionDescriptor();
        d.setType("dbms");
        d.setDbmsType(dbmsType);
        return d;
    }

    private static PreconditionDescriptor notDesc(PreconditionDescriptor inner) {
        PreconditionDescriptor d = new PreconditionDescriptor();
        d.setType("not");
        d.addNestedCondition(inner);
        return d;
    }

    private static PreconditionDescriptor andDesc(PreconditionDescriptor... inners) {
        PreconditionDescriptor d = new PreconditionDescriptor();
        d.setType("and");
        for (PreconditionDescriptor inner : inners) {
            d.addNestedCondition(inner);
        }
        return d;
    }

    private static PreconditionDescriptor changeSetExecutedDesc(String id, String author, String file) {
        PreconditionDescriptor d = new PreconditionDescriptor();
        d.setType("changeSetExecuted");
        d.setChangeSetId(id);
        d.setChangeSetAuthor(author);
        d.setChangeSetFile(file);
        return d;
    }

    // -------------------------------------------------------------------------
    // Manifest builders
    // -------------------------------------------------------------------------

    private static MigrationManifest twoChangeSetManifest() {
        MigrationManifest manifest = new MigrationManifest();
        manifest.getChangeSets().add(makeChangeSet("cs1", "author1", "file1.xml"));
        manifest.getChangeSets().add(makeChangeSet("cs2", "author2", "file2.xml"));
        return manifest;
    }

    private static ChangeSetDescriptor makeChangeSet(String id, String author, String file) {
        ChangeSetDescriptor cs = new ChangeSetDescriptor();
        cs.setId(id);
        cs.setAuthor(author);
        cs.setChangeLogFile(file);
        return cs;
    }

    // -------------------------------------------------------------------------
    // ChangeSetRunner / execution tests
    // -------------------------------------------------------------------------

    @Test
    public void testHistoryTableDdlIsValid() throws Exception {
        // HistoryTableManager.createTableSql must produce valid H2 DDL
        String ddl = HistoryTableManager.createTableSql(SqlDialect.H2, null, ChangeLogHistory.CHANGELOG_TABLE);
        try (Statement st = connection.createStatement()) {
            st.execute(ddl);
        }
        Assert.assertTrue(ChangeLogHistory.tableExists(connection, null));
        // Drop it again so tearDown does not complain
        try (Statement st = connection.createStatement()) {
            st.execute("DROP TABLE DATABASECHANGELOG");
        }
    }

    @Test
    public void testRunnerIdempotence() throws Exception {
        // Running the ChangeSetRunner twice on H2 should process nothing on the second run.
        // We use a minimal 2-changeset manifest with sql steps so no custom tasks are involved.
        connection.setAutoCommit(false);

        String ddl = HistoryTableManager.createTableSql(SqlDialect.H2, null, ChangeLogHistory.CHANGELOG_TABLE);
        try (Statement st = connection.createStatement()) {
            st.execute(ddl);
        }
        connection.commit();

        MigrationManifest manifest = twoSqlChangeSetManifest();
        String deploymentId = "1234567890";
        JdbcSqlSink sink = new JdbcSqlSink(connection);

        // First run
        Set<ChangeSetKey> executed1 = ChangeLogHistory.loadExecuted(connection, null, ChangeLogHistory.CHANGELOG_TABLE);
        ChangeSetRunner runner1 = new ChangeSetRunner(connection, null, ChangeLogHistory.CHANGELOG_TABLE,
                SqlDialect.H2, manifest);
        int processed1 = runner1.run(sink, executed1, deploymentId, 0);
        Assert.assertEquals(2, processed1);

        // Second run with freshly loaded executed set — should process nothing
        Set<ChangeSetKey> executed2 = ChangeLogHistory.loadExecuted(connection, null, ChangeLogHistory.CHANGELOG_TABLE);
        ChangeSetRunner runner2 = new ChangeSetRunner(connection, null, ChangeLogHistory.CHANGELOG_TABLE,
                SqlDialect.H2, manifest);
        int processed2 = runner2.run(sink, executed2, deploymentId, 2);
        Assert.assertEquals(0, processed2);

        connection.setAutoCommit(true);
    }

    @Test
    public void testHistoryInsertAndSelect() throws Exception {
        createChangelogTable();
        HistoryTableManager.insertHistoryRow(connection, SqlDialect.H2, null, ChangeLogHistory.CHANGELOG_TABLE,
                "my-id", "author", "file.xml", "9:abc123", "some desc", null, "EXECUTED", 1, "1234567890",
                HistoryTableManager.DEFAULT_LIQUIBASE_VERSION);

        Set<ChangeSetKey> loaded = ChangeLogHistory.loadExecuted(connection, null, ChangeLogHistory.CHANGELOG_TABLE);
        Assert.assertEquals(1, loaded.size());
        Assert.assertTrue(loaded.contains(new ChangeSetKey("my-id", "author", "file.xml")));
        Assert.assertEquals(1, ChangeLogHistory.maxOrderExecuted(connection, null, ChangeLogHistory.CHANGELOG_TABLE));
    }

    @Test
    public void testMarkRanOutcome() throws Exception {
        // A change-set with onFail=MARK_RAN and a false condition should be recorded with MARK_RAN exectype
        connection.setAutoCommit(false);

        String ddl = HistoryTableManager.createTableSql(SqlDialect.H2, null, ChangeLogHistory.CHANGELOG_TABLE);
        try (Statement st = connection.createStatement()) {
            st.execute(ddl);
        }
        connection.commit();

        // Changeset with dbms=oracle precondition (never true on H2), onFail=MARK_RAN
        MigrationManifest manifest = new MigrationManifest();
        ChangeSetDescriptor cs = makeChangeSet("mark-ran-cs", "author", "file.xml");
        cs.setPreConditions(markRanContainer(dbmsDesc("oracle")));
        manifest.getChangeSets().add(cs);

        JdbcSqlSink sink = new JdbcSqlSink(connection);
        Set<ChangeSetKey> executed = ChangeLogHistory.loadExecuted(connection, null, ChangeLogHistory.CHANGELOG_TABLE);
        ChangeSetRunner runner = new ChangeSetRunner(connection, null, ChangeLogHistory.CHANGELOG_TABLE,
                SqlDialect.H2, manifest);
        int processed = runner.run(sink, executed, "1234567890", 0);
        Assert.assertEquals(1, processed);

        // Verify the row was inserted
        Set<ChangeSetKey> after = ChangeLogHistory.loadExecuted(connection, null, ChangeLogHistory.CHANGELOG_TABLE);
        Assert.assertEquals(1, after.size());
        Assert.assertTrue(after.contains(new ChangeSetKey("mark-ran-cs", "author", "file.xml")));

        connection.setAutoCommit(true);
    }

    // -------------------------------------------------------------------------
    // Custom provider manifest tests
    // -------------------------------------------------------------------------

    /**
     * loadExecuted on a non-existent custom provider history table must return an empty set,
     * not throw a SQLException.  This is the "first boot" case.
     */
    @Test
    public void testLoadExecutedOnMissingCustomProviderTableReturnsEmpty() throws Exception {
        String customTable = "DATABASECHANGELOG_SAMPLE";
        // The table has never been created — loadExecuted must not throw
        Set<ChangeLogHistory.ChangeSetKey> executed =
                ChangeLogHistory.loadExecuted(connection, null, customTable);
        Assert.assertTrue(executed.isEmpty());
    }

    /**
     * computePending for a custom provider manifest returns all change-sets when none have been
     * recorded in the custom history table yet (table does not exist → empty executed set).
     */
    @Test
    public void testCustomProviderManifestAllPendingWhenTableMissing() throws Exception {
        MigrationManifest manifest = customProviderManifest();
        SqlDialect dialect = SqlDialect.of(connection);

        Set<ChangeLogHistory.ChangeSetKey> executed =
                ChangeLogHistory.loadExecuted(connection, null, "DATABASECHANGELOG_SAMPLE");
        List<ChangeSetDescriptor> pending = ChangeLogHistory.computePending(manifest, executed, dialect);

        Assert.assertEquals(1, pending.size());
        Assert.assertEquals("custom-1.0", pending.get(0).getId());
    }

    /**
     * computePending returns empty when the custom provider change-set is already recorded.
     */
    @Test
    public void testCustomProviderManifestNoPendingWhenAlreadyExecuted() throws Exception {
        String customTable = "DATABASECHANGELOG_SAMPLE";
        createCustomProviderHistoryTable(customTable);
        insertCustomHistoryRow(customTable, "custom-1.0", "keycloak", "META-INF/custom-changelog.xml");

        MigrationManifest manifest = customProviderManifest();
        SqlDialect dialect = SqlDialect.of(connection);

        Set<ChangeLogHistory.ChangeSetKey> executed =
                ChangeLogHistory.loadExecuted(connection, null, customTable);
        List<ChangeSetDescriptor> pending = ChangeLogHistory.computePending(manifest, executed, dialect);

        Assert.assertTrue(pending.isEmpty());
    }

    /**
     * ChangeSetRunner correctly executes a custom provider manifest and records history in the
     * dedicated custom history table.
     */
    @Test
    public void testCustomProviderRunnerExecutesAndRecordsHistory() throws Exception {
        String customTable = "DATABASECHANGELOG_SAMPLE";
        connection.setAutoCommit(false);

        // Create the custom history table (in update mode this is done by runProviderChangelog)
        try (Statement st = connection.createStatement()) {
            st.execute(HistoryTableManager.createTableSql(SqlDialect.H2, null, customTable));
        }
        connection.commit();

        MigrationManifest manifest = customProviderManifest();
        Set<ChangeLogHistory.ChangeSetKey> executed =
                ChangeLogHistory.loadExecuted(connection, null, customTable);
        JdbcSqlSink sink = new JdbcSqlSink(connection);
        ChangeSetRunner runner = new ChangeSetRunner(connection, null, customTable, SqlDialect.H2, manifest);
        int processed = runner.run(sink, executed, "9876543210", 0);

        Assert.assertEquals(1, processed);

        // The CUSTOM_ENTITY table must have been created
        Assert.assertTrue(ChangeLogHistory.namedTableExists(connection, null, "CUSTOM_ENTITY"));

        // The history row must be recorded in the custom history table
        Set<ChangeLogHistory.ChangeSetKey> after =
                ChangeLogHistory.loadExecuted(connection, null, customTable);
        Assert.assertEquals(1, after.size());
        Assert.assertTrue(after.contains(
                new ChangeLogHistory.ChangeSetKey("custom-1.0", "keycloak", "META-INF/custom-changelog.xml")));

        connection.setAutoCommit(true);
    }

    /**
     * namedTableExists returns false for a non-existent table and true once the table is created.
     */
    @Test
    public void testNamedTableExistsFalseAndTrue() throws Exception {
        Assert.assertFalse(ChangeLogHistory.namedTableExists(connection, null, "CUSTOM_ENTITY"));
        try (Statement st = connection.createStatement()) {
            st.execute("CREATE TABLE CUSTOM_ENTITY (ID VARCHAR(36) NOT NULL PRIMARY KEY)");
        }
        Assert.assertTrue(ChangeLogHistory.namedTableExists(connection, null, "CUSTOM_ENTITY"));
    }

    // -------------------------------------------------------------------------
    // Custom provider manifest helpers
    // -------------------------------------------------------------------------

    /** Minimal single-changeset manifest for the 'sample' custom provider. */
    private static MigrationManifest customProviderManifest() {
        String createTable =
                "CREATE TABLE IF NOT EXISTS CUSTOM_ENTITY (ID VARCHAR(36) NOT NULL PRIMARY KEY)";

        ChangeSetStep step = new ChangeSetStep();
        step.setType("sql");
        step.addStatement("h2", createTable);

        ChangeSetDescriptor cs = new ChangeSetDescriptor();
        cs.setId("custom-1.0");
        cs.setAuthor("keycloak");
        cs.setChangeLogFile("META-INF/custom-changelog.xml");
        cs.setCheckSum("9:custom-1.0");
        cs.getSteps().add(step);

        MigrationManifest manifest = new MigrationManifest();
        manifest.getChangeSets().add(cs);
        return manifest;
    }

    private void createCustomProviderHistoryTable(String tableName) throws Exception {
        try (Statement st = connection.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                    "ID VARCHAR(255) NOT NULL, " +
                    "AUTHOR VARCHAR(255) NOT NULL, " +
                    "FILENAME VARCHAR(255) NOT NULL, " +
                    "DATEEXECUTED TIMESTAMP NOT NULL, " +
                    "ORDEREXECUTED INT NOT NULL, " +
                    "EXECTYPE VARCHAR(10) NOT NULL, " +
                    "MD5SUM VARCHAR(35), " +
                    "DESCRIPTION VARCHAR(255), " +
                    "COMMENTS VARCHAR(255), " +
                    "TAG VARCHAR(255), " +
                    "LIQUIBASE VARCHAR(20), " +
                    "CONTEXTS VARCHAR(255), " +
                    "LABELS VARCHAR(255), " +
                    "DEPLOYMENT_ID VARCHAR(10))");
        }
    }

    private void insertCustomHistoryRow(String tableName, String id, String author, String filename) throws Exception {
        try (Statement st = connection.createStatement()) {
            st.execute("INSERT INTO " + tableName + " (ID, AUTHOR, FILENAME, DATEEXECUTED, ORDEREXECUTED, EXECTYPE) " +
                    "VALUES ('" + id + "', '" + author + "', '" + filename + "', CURRENT_TIMESTAMP, 1, 'EXECUTED')");
        }
    }

    /** Minimal 2-changeset manifest with sql steps (no custom tasks, no preconditions). */
    private static MigrationManifest twoSqlChangeSetManifest() {
        MigrationManifest manifest = new MigrationManifest();
        // First change-set: CREATE TABLE TEST_TABLE
        ChangeSetDescriptor cs1 = makeChangeSet("sql-cs1", "author", "file.xml");
        ChangeSetStep step1 = new ChangeSetStep();
        step1.setType("sql");
        step1.addStatement("h2", "CREATE TABLE IF NOT EXISTS TEST_TABLE (ID VARCHAR(36) NOT NULL PRIMARY KEY)");
        cs1.getSteps().add(step1);
        manifest.getChangeSets().add(cs1);

        // Second change-set: CREATE TABLE ANOTHER_TABLE
        ChangeSetDescriptor cs2 = makeChangeSet("sql-cs2", "author", "file.xml");
        ChangeSetStep step2 = new ChangeSetStep();
        step2.setType("sql");
        step2.addStatement("h2", "CREATE TABLE IF NOT EXISTS ANOTHER_TABLE (ID VARCHAR(36) NOT NULL PRIMARY KEY)");
        cs2.getSteps().add(step2);
        manifest.getChangeSets().add(cs2);

        return manifest;
    }
}
