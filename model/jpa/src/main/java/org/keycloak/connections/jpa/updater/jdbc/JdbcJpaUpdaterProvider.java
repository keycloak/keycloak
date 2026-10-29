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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.keycloak.connections.jpa.entityprovider.JpaEntityProvider;
import org.keycloak.connections.jpa.updater.JpaUpdaterProvider;
import org.keycloak.connections.jpa.updater.jdbc.ChangeLogHistory.ChangeSetKey;
import org.keycloak.connections.jpa.util.JpaUtils;
import org.keycloak.enums.SqlDialect;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.migration.ChangeSetDescriptor;
import org.keycloak.representations.migration.MigrationManifest;

import org.jboss.logging.Logger;

/**
 * JDBC implementation of {@link JpaUpdaterProvider}.
 *
 * <h3>Architecture</h3>
 * Reads the build-time migration manifest ({@code META-INF/schema/migration-manifest.json}),
 * evaluates preconditions using {@link PreconditionEvaluator}, and executes or exports change-sets
 * through a {@link SqlSink} ({@link JdbcSqlSink} for live execution, {@link ScriptSqlSink} for export).
 * The shared {@link ChangeSetRunner} drives both modes from a single code path.
 *
 * <h3>Compatibility</h3>
 * Reads and writes the {@code DATABASECHANGELOG} / {@code DATABASECHANGELOGLOCK} tables in the format
 * that was historically created by Liquibase, so databases created by earlier versions are upgraded in place.
 *
 * <h3>Custom {@link JpaEntityProvider} changelogs</h3>
 * Providers that need schema changes implement {@link JpaEntityProvider#getMigrationManifest()} and
 * return a {@link MigrationManifest}.  The same native engine runs each provider's manifest against
 * a dedicated history table ({@code DATABASECHANGELOG_<factoryId>}).
 */
public class JdbcJpaUpdaterProvider implements JpaUpdaterProvider {

    private static final Logger logger = Logger.getLogger(JdbcJpaUpdaterProvider.class);

    /**
     * Session attribute name set by {@code QuarkusJpaConnectionProviderFactory}
     * to indicate whether the master changelog should be verified and run on this boot.
     * When {@code false} (same-version restart) we skip the master changelog check entirely.
     */
    public static final String VERIFY_AND_RUN_MASTER_CHANGELOG = "VERIFY_AND_RUN_MASTER_CHANGELOG";

    private final KeycloakSession session;
    /**
     * Whether the master {@code DATABASECHANGELOG} table already existed when this server started.
     * Used by {@code export()} to decide whether to emit the {@code CREATE TABLE} DDL.
     * {@code null} means not yet checked.
     */
    private Boolean masterChangelogTablePreExisted;

    private final long indexCreationThreshold;

    public JdbcJpaUpdaterProvider(KeycloakSession session, long indexCreationThreshold) {
        this.session = session;
        this.indexCreationThreshold = indexCreationThreshold;
    }

    // -------------------------------------------------------------------------
    // validate()
    // -------------------------------------------------------------------------

    @Override
    public Status validate(Connection connection, String defaultSchema) {
        logger.debug("Validating if database is updated");
        try {
            recordMasterTablePreExistence(connection, defaultSchema);

            if (needsVerifyMasterChangelog()) {
                Status masterStatus = validateMasterChangelog(connection, defaultSchema);
                if (masterStatus != Status.VALID) {
                    return masterStatus;
                }
            }

            // Custom JpaEntityProvider manifests — native path.
            SqlDialect sqlDialect = SqlDialect.of(connection);
            Set<JpaEntityProvider> jpaProviders = session.getAllProviders(JpaEntityProvider.class);
            for (JpaEntityProvider jpaProvider : jpaProviders) {
                MigrationManifest providerManifest = jpaProvider.getMigrationManifest();
                if (providerManifest != null) {
                    String changelogTableName = JpaUtils.getCustomChangelogTableName(jpaProvider.getFactoryId());
                    Set<ChangeSetKey> executed = ChangeLogHistory.loadExecuted(connection, defaultSchema, changelogTableName);
                    List<ChangeSetDescriptor> pending = ChangeLogHistory.computePending(providerManifest, executed, sqlDialect);
                    if (!pending.isEmpty()) {
                        return Status.OUTDATED;
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to validate database", e);
        }

        return Status.VALID;
    }

    private Status validateMasterChangelog(Connection connection, String defaultSchema) throws SQLException {
        SqlDialect sqlDialect = SqlDialect.of(connection);
        MigrationManifest manifest = MigrationManifestLoader.load();

        applySchema(connection, defaultSchema);

        Set<ChangeSetKey> executed = ChangeLogHistory.loadExecuted(connection, defaultSchema,
                ChangeLogHistory.CHANGELOG_TABLE);
        List<ChangeSetDescriptor> pending = ChangeLogHistory.computePending(manifest, executed, sqlDialect);

        if (pending.isEmpty()) {
            logger.debugf("Validation passed. Database is up-to-date for master changelog (%d change-sets)",
                    manifest.getChangeSets().size());
            return Status.VALID;
        }

        // change-sets restricted to other DBMS never become pending, so compare against the applicable ones
        int applicable = ChangeLogHistory.computePending(manifest, Set.of(), sqlDialect).size();
        if (pending.size() == applicable) {
            logger.debugf("Validation: no executed change-sets found — database is EMPTY");
            return Status.EMPTY;
        }

        logger.debugf("Validation failed. %d pending change-set(s) out of %d applicable", pending.size(),
                applicable);
        return Status.OUTDATED;
    }

    // -------------------------------------------------------------------------
    // update()
    // -------------------------------------------------------------------------

    @Override
    public void update(Connection connection, String defaultSchema) {
        logger.debug("Starting database update");
        try {
            if (needsVerifyMasterChangelog()) {
                runMasterChangelog(connection, null, defaultSchema);
            }
            runCustomProviderChangelogs(connection, null, defaultSchema);
        } catch (IOException | SQLException e) {
            throw new RuntimeException("Failed to update database", e);
        }
    }

    // -------------------------------------------------------------------------
    // export()
    // -------------------------------------------------------------------------

    @Override
    public void export(Connection connection, String defaultSchema, File file) {
        logger.debug("Starting database export (manual migration mode)");
        try (FileWriter writer = new FileWriter(file)) {
            // Oracle SQL*Plus treats '&' in quoted strings as substitution variables.
            // Disable that behaviour up-front so that any stored values containing '&'
            // (e.g. URIs with query parameters) are not corrupted when the script is applied.
            if (SqlDialect.of(connection) == SqlDialect.ORACLE) {
                writer.write("SET DEFINE OFF");
                writer.write(System.lineSeparator());
                writer.write(System.lineSeparator());
            }
            runMasterChangelog(connection, writer, defaultSchema);
            runCustomProviderChangelogs(connection, writer, defaultSchema);
        } catch (IOException | SQLException e) {
            throw new RuntimeException("Failed to export database migration script", e);
        }
    }

    // -------------------------------------------------------------------------
    // Shared execution entry-point
    // -------------------------------------------------------------------------

    /**
     * Runs the master changelog using the native engine.
     *
     * @param writer when non-null: export mode (write to file); when null: update mode (execute on connection)
     */
    private void runMasterChangelog(Connection connection, Writer writer, String defaultSchema)
            throws SQLException, IOException {
        runChangelog(connection, writer, defaultSchema, MigrationManifestLoader.load(),
                ChangeLogHistory.CHANGELOG_TABLE, true);
    }

    /**
     * Runs custom {@link JpaEntityProvider} manifests using the native engine.
     * Each provider gets its own history table ({@code DATABASECHANGELOG_<factoryId>}).
     */
    private void runCustomProviderChangelogs(Connection connection, Writer writer, String defaultSchema)
            throws IOException, SQLException {
        Set<JpaEntityProvider> jpaProviders = session.getAllProviders(JpaEntityProvider.class);
        for (JpaEntityProvider jpaProvider : jpaProviders) {
            MigrationManifest providerManifest = jpaProvider.getMigrationManifest();
            if (providerManifest != null) {
                String changelogTableName = JpaUtils.getCustomChangelogTableName(jpaProvider.getFactoryId());
                runChangelog(connection, writer, defaultSchema, providerManifest, changelogTableName, false);
            }
        }
    }

    /**
     * Runs one manifest against its history table; shared by the master changelog and the provider changelogs.
     *
     * @param writer   when non-null: export mode (write to file); when null: update mode (execute on connection)
     * @param isMaster whether this is the master changelog, which has the export header and may need the
     *                 {@code DEPLOYMENT_ID} column backfilled
     */
    private void runChangelog(Connection connection, Writer writer, String defaultSchema,
            MigrationManifest manifest, String historyTable, boolean isMaster) throws SQLException, IOException {
        SqlDialect sqlDialect = SqlDialect.of(connection);
        applySchema(connection, defaultSchema);

        boolean wasAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            boolean tableExisted = ChangeLogHistory.namedTableExists(connection, defaultSchema, historyTable);
            boolean missingDeploymentId = isMaster && tableExisted
                    && ChangeLogHistory.missingDeploymentIdColumn(connection, defaultSchema);
            String deploymentId = generateDeploymentId();

            SqlSink sink;
            if (writer != null) {
                ScriptSqlSink script = new ScriptSqlSink(writer, sqlDialect);
                sink = script;

                if (isMaster) {
                    // Emit header when DB is empty
                    if (Boolean.FALSE.equals(masterChangelogTablePreExisted)) {
                        script.comment("*********************************************************************");
                        script.comment("* Keycloak database creation script - apply this script to empty DB *");
                        script.comment("*********************************************************************");
                        script.comment("");
                        script.comment("Create Database Change Log Table");
                        script.execute(HistoryTableManager.createTableSql(sqlDialect, defaultSchema, historyTable),
                                "create-changelog-table");
                        if (sqlDialect == SqlDialect.MYSQL || sqlDialect == SqlDialect.MARIADB) {
                            script.comment("Add primary key to DATABASECHANGELOG table for MySQL/MariaDB");
                            script.execute(HistoryTableManager.mysqlAddPrimaryKeySql(defaultSchema, historyTable),
                                    "create-changelog-pk");
                        }
                    }
                } else if (!tableExisted) {
                    script.comment("Create changelog table for custom provider");
                    script.execute(HistoryTableManager.createTableSql(sqlDialect, defaultSchema, historyTable),
                            "create-provider-changelog-table");
                    if (sqlDialect == SqlDialect.MYSQL || sqlDialect == SqlDialect.MARIADB) {
                        script.execute(HistoryTableManager.mysqlAddPrimaryKeySql(defaultSchema, historyTable),
                                "create-provider-changelog-pk");
                    }
                }

                // Emit DEPLOYMENT_ID backfill DDL when column is missing
                if (missingDeploymentId) {
                    script.comment("Add missing DEPLOYMENT_ID column");
                    script.execute(HistoryTableManager.addDeploymentIdColumnSql(sqlDialect, defaultSchema, historyTable),
                            "add-deployment-id");
                    script.execute(
                            HistoryTableManager.backfillDeploymentIdSql(defaultSchema, historyTable)
                                    .replace("?", "'" + deploymentId + "'"),
                            "backfill-deployment-id");
                    script.execute(HistoryTableManager.setDeploymentIdNotNullSql(sqlDialect, defaultSchema, historyTable),
                            "set-deployment-id-not-null");
                }
            } else {
                sink = new JdbcSqlSink(connection);

                // Create history table if needed
                if (!tableExisted) {
                    try (Statement st = connection.createStatement()) {
                        st.execute(HistoryTableManager.createTableSql(sqlDialect, defaultSchema, historyTable));
                        connection.commit();
                    }
                    if ((sqlDialect == SqlDialect.MYSQL || sqlDialect == SqlDialect.MARIADB)
                            && !hasPrimaryKey(connection, defaultSchema, historyTable)) {
                        try (Statement st = connection.createStatement()) {
                            st.execute(HistoryTableManager.mysqlAddPrimaryKeySql(defaultSchema, historyTable));
                            connection.commit();
                        }
                    }
                }

                // Backfill DEPLOYMENT_ID if missing
                if (missingDeploymentId) {
                    HistoryTableManager.backfillDeploymentId(connection, sqlDialect, defaultSchema,
                            historyTable, deploymentId);
                }
            }

            Set<ChangeSetKey> executed = ChangeLogHistory.loadExecuted(connection, defaultSchema, historyTable);
            int startOrder = ChangeLogHistory.maxOrderExecuted(connection, defaultSchema, historyTable);
            ChangeSetRunner runner = new ChangeSetRunner(connection, defaultSchema, historyTable,
                    sqlDialect, manifest, indexCreationThreshold, session);
            runner.run(sink, executed, deploymentId, startOrder);
        } finally {
            connection.setAutoCommit(wasAutoCommit);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private boolean needsVerifyMasterChangelog() {
        return session.getAttributeOrDefault(VERIFY_AND_RUN_MASTER_CHANGELOG, Boolean.TRUE);
    }

    private void recordMasterTablePreExistence(Connection connection, String schema) throws SQLException {
        if (masterChangelogTablePreExisted == null) {
            masterChangelogTablePreExisted = ChangeLogHistory.tableExists(connection, schema);
        }
    }

    private static void applySchema(Connection connection, String schema) {
        if (schema == null || schema.isBlank()) {
            return;
        }
        try {
            connection.setSchema(ChangeLogHistory.effectiveSchema(connection, schema));
        } catch (SQLException e) {
            logger.warnf("Could not set connection schema to '%s': %s", schema, e.getMessage());
        }
    }

    private static String generateDeploymentId() {
        // 10 digit random number matching Liquibase's format
        long val = (long) (new Random().nextDouble() * 9_000_000_000L) + 1_000_000_000L;
        return String.valueOf(val);
    }

    private static boolean hasPrimaryKey(Connection connection, String schema, String tableName) throws SQLException {
        try (ResultSet rs = connection.getMetaData().getPrimaryKeys(null, schema, tableName)) {
            return rs.next();
        }
    }

    @Override
    public void close() {
    }
}
