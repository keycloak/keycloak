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
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.keycloak.connections.jpa.updater.jdbc.ChangeLogHistory.ChangeSetKey;
import org.keycloak.enums.SqlDialect;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.migration.ChangeSetDescriptor;
import org.keycloak.representations.migration.ChangeSetStep;
import org.keycloak.representations.migration.MigrationManifest;

import org.jboss.logging.Logger;

/**
 * Shared change-set execution loop used by both {@code update()} and {@code export()} in
 * {@link JdbcJpaUpdaterProvider}.
 *
 * <p>The runner iterates over pending change-sets from the manifest in order, evaluates preconditions,
 * and dispatches each step to the provided {@link SqlSink}.  A history INSERT is produced for every
 * processed change-set (either {@code EXECUTED} or {@code MARK_RAN}).
 *
 * <p>Transactional behaviour in update mode:
 * <ul>
 *   <li><b>Precondition:</b> the caller must have set {@code connection.setAutoCommit(false)} before
 *       constructing this runner; {@code ChangeSetRunner} does not modify the auto-commit flag itself.</li>
 *   <li>Each change-set is committed as a unit: SQL + history INSERT.</li>
 *   <li>On failure the change-set is rolled back and the exception is re-thrown (aborting the migration).</li>
 *   <li>In export mode no connection state is modified.</li>
 * </ul>
 *
 * <p>Index-creation threshold: when a {@code createIndex} step exceeds the configured row-count threshold
 * (default 300,000, SPI {@code connectionsJpaUpdater}, option {@code index-creation-threshold}),
 * the index is skipped with a warning log.  In export mode the index SQL is always emitted.
 */
public class ChangeSetRunner {

    private static final Logger logger = Logger.getLogger(ChangeSetRunner.class);

    /** Default row-count threshold above which index creation is skipped at runtime (F5). */
    static final long DEFAULT_INDEX_THRESHOLD = 300_000L;

    private final Connection connection;
    private final String schema;
    private final String historyTableName;
    private final SqlDialect dialect;
    private final MigrationManifest manifest;
    private final long indexCreationThreshold;
    private final KeycloakSession session;

    public ChangeSetRunner(Connection connection, String schema, String historyTableName,
            SqlDialect dialect, MigrationManifest manifest, long indexCreationThreshold,
            KeycloakSession session) {
        this.connection = connection;
        this.schema = schema;
        this.historyTableName = historyTableName;
        this.dialect = dialect;
        this.manifest = manifest;
        this.indexCreationThreshold = indexCreationThreshold;
        this.session = session;
    }

    public ChangeSetRunner(Connection connection, String schema, String historyTableName,
            SqlDialect dialect, MigrationManifest manifest, long indexCreationThreshold) {
        this(connection, schema, historyTableName, dialect, manifest, indexCreationThreshold, null);
    }

    public ChangeSetRunner(Connection connection, String schema, String historyTableName,
            SqlDialect dialect, MigrationManifest manifest, KeycloakSession session) {
        this(connection, schema, historyTableName, dialect, manifest, DEFAULT_INDEX_THRESHOLD, session);
    }

    public ChangeSetRunner(Connection connection, String schema, String historyTableName,
            SqlDialect dialect, MigrationManifest manifest) {
        this(connection, schema, historyTableName, dialect, manifest, DEFAULT_INDEX_THRESHOLD, null);
    }

    /**
     * Runs all pending change-sets against the provided sink.
     *
     * @param sink          the execution target (JDBC or script file)
     * @param existingKeys  the set of already-executed change-set keys (modified in place as each change-set is processed)
     * @param deploymentId  the 10-digit deployment id for this run (all new rows use the same id)
     * @param startOrder    the {@code ORDEREXECUTED} value to use for the first new row
     * @return the number of change-sets processed (executed + marked-ran)
     */
    public int run(SqlSink sink, Set<ChangeSetKey> existingKeys, String deploymentId, int startOrder)
            throws SQLException {
        List<ChangeSetDescriptor> pending = ChangeLogHistory.computePending(manifest, existingKeys, dialect);

        if (pending.isEmpty()) {
            logger.debugf("Database is up to date for history table %s", historyTableName);
            return 0;
        }

        if (existingKeys.isEmpty()) {
            logger.infof("Initializing database schema. History table: %s", historyTableName);
        } else {
            logger.infof("Updating database. %d pending change-set(s) in %s", pending.size(), historyTableName);
        }

        // Build a mutable set of in-progress keys so that changeSetExecuted preconditions work within a run
        Set<ChangeSetKey> inProgressKeys = new HashSet<>(existingKeys);
        int order = startOrder;

        for (ChangeSetDescriptor cs : pending) {
            order = processChangeSet(cs, sink, inProgressKeys, deploymentId, order);
        }

        logger.debugf("Completed database update for history table %s", historyTableName);
        return pending.size();
    }

    /**
     * Processes one change-set: evaluates preconditions, executes or marks ran, inserts history row.
     * Returns the next {@code ORDEREXECUTED} value.
     */
    private int processChangeSet(ChangeSetDescriptor cs, SqlSink sink,
            Set<ChangeSetKey> inProgressKeys, String deploymentId, int order) throws SQLException {
        PreconditionEvaluator evaluator = new PreconditionEvaluator(
                connection, schema, dialect, inProgressKeys, sink.isExportMode());

        PreconditionEvaluator.Outcome outcome = evaluator.evaluate(cs.getPreConditions());

        String execType;
        switch (outcome) {
            case RUN, SKIP_EXPORT -> {
                execType = "EXECUTED";
                if (sink.isExportMode()) {
                    sink.comment("Changeset " + cs.getChangeLogFile() + "::" + cs.getId() + "::" + cs.getAuthor());
                }
                executeSteps(cs, sink);
            }
            case MARK_RAN -> {
                execType = "MARK_RAN";
                logger.debugf("Marking change-set %s as ran (precondition not met)", cs.getId());
                if (sink.isExportMode()) {
                    sink.comment("Changeset " + cs.getChangeLogFile() + "::" + cs.getId() + "::" + cs.getAuthor()
                            + " skipped (precondition not met — MARK_RAN)");
                }
            }
            case HALT -> throw new RuntimeException(
                    "Migration halted: precondition failed (onFail=HALT) for change-set " + cs.getId());
            default -> throw new IllegalStateException("Unknown outcome: " + outcome);
        }

        // Emit history INSERT
        int nextOrder = order + 1;
        emitHistoryInsert(cs, sink, execType, nextOrder, deploymentId);

        // Record in in-progress set so changeSetExecuted preconditions work within this run
        inProgressKeys.add(new ChangeSetKey(cs.getId(), cs.getAuthor(), cs.getChangeLogFile()));

        // Commit after each change-set in update mode (DDL auto-commits on some DBs; always commit here)
        if (!sink.isExportMode()) {
            connection.commit();
        }

        return nextOrder;
    }

    /** Iterates the steps of a change-set and dispatches each to the sink. */
    private void executeSteps(ChangeSetDescriptor cs, SqlSink sink) throws SQLException {
        for (ChangeSetStep step : cs.getSteps()) {
            switch (step.getType()) {
                case "sql" -> executeSqlStep(cs.getId(), step, sink);
                case "createIndex" -> executeCreateIndexStep(cs.getId(), step, sink);
                case "customTask" -> executeCustomTaskStep(cs.getId(), step, sink);
                default -> logger.warnf("Unknown step type '%s' in change-set '%s' — skipping",
                        step.getType(), cs.getId());
            }
        }
    }

    /** Dispatches a {@code customTask} step to the registered native {@link JpaMigrationTask}. */
    private void executeCustomTaskStep(String changeSetId, ChangeSetStep step, SqlSink sink)
            throws SQLException {
        String className = step.getClassName();
        JpaMigrationTask task = JpaMigrationTaskRegistry.get(className);
        if (task == null) {
            logger.warnf("No native JpaMigrationTask registered for '%s' in change-set '%s' — skipping.",
                    className, changeSetId);
            return;
        }
        List<String> stmts;
        try {
            if (!task.isApplicable(connection, schema, dialect)) {
                logger.debugf("%s: no update applicable for this task (change-set '%s')", task.getTaskId(), changeSetId);
                return;
            }
            stmts = task.generateStatements(session, connection, schema, dialect);
        } catch (SQLException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Custom migration task '" + task.getTaskId()
                    + "' (change-set '" + changeSetId + "') failed: " + e.getMessage(), e);
        }
        for (String sql : stmts) {
            sink.execute(sql, changeSetId);
        }
    }

    private void executeSqlStep(String changeSetId, ChangeSetStep step, SqlSink sink) throws SQLException {
        List<String> stmts = statementsForDialect(step);
        if (stmts == null || stmts.isEmpty()) {
            return;
        }
        for (String sql : stmts) {
            sink.execute(sql, changeSetId);
        }
    }

    private void executeCreateIndexStep(String changeSetId, ChangeSetStep step, SqlSink sink) throws SQLException {
        // In export mode always emit the index SQL
        if (sink.isExportMode()) {
            executeSqlStep(changeSetId, step, sink);
            return;
        }

        // In update mode apply the index-creation threshold (F5)
        if (indexCreationThreshold > 0 && step.getTableName() != null) {
            long rowCount = countRows(step.getTableName());
            if (rowCount > indexCreationThreshold) {
                logger.warnf("Index '%s' on table '%s' was not created because the table has %d rows" +
                        " (threshold is %d). Create the index manually when the load is low.",
                        step.getIndexName(), step.getTableName(), rowCount, indexCreationThreshold);
                return;
            }
        }

        executeSqlStep(changeSetId, step, sink);
    }

    private long countRows(String tableName) {
        String qualifiedTable = ChangeLogHistory.qualifiedName(tableName, schema);
        try (java.sql.Statement st = connection.createStatement();
             java.sql.ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + qualifiedTable)) {
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            logger.warnf("Could not count rows in table %s for index threshold check: %s", tableName, e.getMessage());
        }
        return 0;
    }

    private void emitHistoryInsert(ChangeSetDescriptor cs, SqlSink sink,
            String execType, int order, String deploymentId) throws SQLException {
        String liquibaseVersion = manifest.getLiquibaseVersion() != null
                ? manifest.getLiquibaseVersion()
                : HistoryTableManager.DEFAULT_LIQUIBASE_VERSION;
        String insertSql = HistoryTableManager.historyInsertSql(
                dialect, schema, historyTableName,
                cs.getId(), cs.getAuthor(), cs.getChangeLogFile(),
                cs.getCheckSum(), cs.getDescription(), cs.getComments(),
                execType, order, deploymentId, liquibaseVersion);

        if (sink.isExportMode()) {
            sink.execute(insertSql, cs.getId());
        } else {
            HistoryTableManager.insertHistoryRow(connection, dialect, schema, historyTableName,
                    cs.getId(), cs.getAuthor(), cs.getChangeLogFile(),
                    cs.getCheckSum(), cs.getDescription(), cs.getComments(),
                    execType, order, deploymentId, liquibaseVersion);
        }
    }

    private List<String> statementsForDialect(ChangeSetStep step) {
        if (step.getStatements() == null) {
            return null;
        }
        return step.getStatements().get(dialect.manifestKey());
    }
}
