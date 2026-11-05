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
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.keycloak.connections.jpa.updater.jdbc.ChangeLogHistory.ChangeSetKey;
import org.keycloak.enums.SqlDialect;
import org.keycloak.representations.migration.PreconditionContainerDescriptor;
import org.keycloak.representations.migration.PreconditionDescriptor;

import org.jboss.logging.Logger;

/**
 * Evaluates the precondition AST stored in the migration manifest against a live JDBC connection.
 *
 * <h3>Supported types</h3>
 * {@code tableExists}, {@code columnExists}, {@code indexExists}, {@code foreignKeyExists},
 * {@code uniqueConstraintExists}, {@code primaryKeyExists}, {@code dbms}, {@code sqlCheck},
 * {@code changeSetExecuted}, {@code and}, {@code or}, {@code not}.
 *
 * <h3>Return values</h3>
 * <ul>
 *   <li>{@link Outcome#RUN} — precondition met; execute the change-set.</li>
 *   <li>{@link Outcome#MARK_RAN} — precondition failed and {@code onFail=MARK_RAN}; record without executing.</li>
 *   <li>{@link Outcome#HALT} — precondition failed and {@code onFail=HALT}; abort migration.</li>
 *   <li>{@link Outcome#SKIP_EXPORT} — precondition container has {@code onSqlOutput=IGNORE} and we are in
 *       export mode; skip the precondition check and always emit the SQL.</li>
 * </ul>
 *
 * <h3>Identity normalisation</h3>
 * {@link DatabaseMetaData} lookups use the schema and normalise identifiers via
 * {@link DatabaseMetaData#storesUpperCaseIdentifiers()} / {@link DatabaseMetaData#storesLowerCaseIdentifiers()}
 * so the same logic works on Oracle (upper-case) and Postgres (lower-case).
 */
public class PreconditionEvaluator {

    private static final Logger logger = Logger.getLogger(PreconditionEvaluator.class);

    /** The result of evaluating a precondition container. */
    public enum Outcome {
        /** Precondition met — execute the change-set. */
        RUN,
        /** Precondition failed with {@code onFail=MARK_RAN}. */
        MARK_RAN,
        /** Precondition failed with {@code onFail=HALT}. */
        HALT,
        /**
         * Container has {@code onSqlOutput=IGNORE} and we are in export (SQL output) mode.
         * The caller should skip the precondition check and always emit the change-set SQL.
         */
        SKIP_EXPORT
    }

    private final Connection connection;
    private final String schema;
    private final SqlDialect dialect;
    /** Set of change-sets already known to be executed in this run (from history + earlier in this run). */
    private final Set<ChangeSetKey> executed;
    private final boolean exportMode;
    private final DatabaseMetaData meta;

    public PreconditionEvaluator(Connection connection, String schema, SqlDialect dialect,
            Set<ChangeSetKey> executed, boolean exportMode) throws SQLException {
        this.connection = connection;
        this.schema = schema;
        this.dialect = dialect;
        this.executed = executed;
        this.exportMode = exportMode;
        this.meta = connection.getMetaData();
    }

    /**
     * Evaluates a precondition container and returns the appropriate {@link Outcome}.
     * When {@code container} is {@code null} the change-set has no preconditions and the outcome is
     * {@link Outcome#RUN}.
     */
    public Outcome evaluate(PreconditionContainerDescriptor container) throws SQLException {
        if (container == null) {
            return Outcome.RUN;
        }

        // onSqlOutput=IGNORE: in export mode skip evaluation and always emit SQL.
        // onSqlOutput=TEST (the Keycloak default): evaluate normally even in export mode — handled by
        // falling through to the regular evaluateConditions() path below.
        if (exportMode && "IGNORE".equalsIgnoreCase(container.getOnSqlOutput())) {
            return Outcome.SKIP_EXPORT;
        }

        boolean conditionMet = evaluateConditions(container.getConditions());
        if (conditionMet) {
            return Outcome.RUN;
        }

        // Precondition not met — check onFail
        String onFail = container.getOnFail();
        if ("MARK_RAN".equalsIgnoreCase(onFail)) {
            return Outcome.MARK_RAN;
        }
        if (onFail != null && !onFail.isBlank()
                && !onFail.equalsIgnoreCase("HALT")) {
            logger.warnf("Unsupported onFail value '%s' in precondition container — treating as HALT", onFail);
        }
        return Outcome.HALT;
    }

    // -------------------------------------------------------------------------
    // Condition evaluation
    // -------------------------------------------------------------------------

    /** Evaluates a list of conditions as an implicit AND (all must be true). */
    private boolean evaluateConditions(List<PreconditionDescriptor> conditions) throws SQLException {
        if (conditions == null || conditions.isEmpty()) {
            return true;
        }
        for (PreconditionDescriptor cond : conditions) {
            if (!evaluateSingle(cond)) {
                return false;
            }
        }
        return true;
    }

    private boolean evaluateSingle(PreconditionDescriptor desc) throws SQLException {
        if (desc == null || desc.getType() == null) {
            return true;
        }
        return switch (desc.getType().toLowerCase(Locale.ROOT)) {
            case "tableexists" -> tableExists(desc.getTableName());
            case "columnexists" -> columnExists(desc.getTableName(), desc.getColumnName());
            case "indexexists" -> indexExists(desc.getTableName(), desc.getIndexName());
            case "foreignkeyexists" -> foreignKeyExists(desc.getTableName(), desc.getForeignKeyName());
            case "uniqueconstraintexists" -> uniqueConstraintExists(desc.getTableName(), desc.getConstraintName());
            case "primarykeyexists" -> primaryKeyExists(desc.getTableName(), desc.getPrimaryKeyName());
            case "dbms" -> dialect.matchesDbmsType(desc.getDbmsType());
            case "sqlcheck" -> sqlCheck(desc.getSql(), desc.getExpectedResult());
            case "changesetexecuted" -> changeSetExecuted(desc.getChangeSetId(), desc.getChangeSetAuthor(), desc.getChangeSetFile());
            case "and" -> evaluateAll(desc.getNestedConditions());
            case "or" -> evaluateAny(desc.getNestedConditions());
            // Liquibase semantics: <not> fails if any nested precondition passes
            case "not" -> !evaluateAny(desc.getNestedConditions());
            default -> {
                logger.warnf("Unknown precondition type '%s' — treating as true", desc.getType());
                yield true;
            }
        };
    }

    private boolean evaluateAll(List<PreconditionDescriptor> conditions) throws SQLException {
        if (conditions == null || conditions.isEmpty()) return true;
        for (PreconditionDescriptor c : conditions) {
            if (!evaluateSingle(c)) return false;
        }
        return true;
    }

    private boolean evaluateAny(List<PreconditionDescriptor> conditions) throws SQLException {
        if (conditions == null || conditions.isEmpty()) return false;
        for (PreconditionDescriptor c : conditions) {
            if (evaluateSingle(c)) return true;
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // Individual precondition implementations
    // -------------------------------------------------------------------------

    private boolean tableExists(String tableName) throws SQLException {
        if (tableName == null) return false;
        String effectiveSchema = ChangeLogHistory.effectiveSchema(connection, schema);
        String name = normaliseIdentifier(tableName);
        try (ResultSet rs = meta.getTables(null, effectiveSchema, name, null)) {
            if (rs.next()) return true;
        }
        // Try alternate case as fallback
        try (ResultSet rs = meta.getTables(null, effectiveSchema, alternateCaseName(tableName), null)) {
            return rs.next();
        }
    }

    private boolean columnExists(String tableName, String columnName) throws SQLException {
        if (tableName == null || columnName == null) return false;
        String effectiveSchema = ChangeLogHistory.effectiveSchema(connection, schema);
        try (ResultSet rs = meta.getColumns(null, effectiveSchema,
                normaliseIdentifier(tableName), normaliseIdentifier(columnName))) {
            if (rs.next()) return true;
        }
        // Fallback with alternate case
        try (ResultSet rs = meta.getColumns(null, effectiveSchema,
                alternateCaseName(tableName), alternateCaseName(columnName))) {
            return rs.next();
        }
    }

    private boolean indexExists(String tableName, String indexName) throws SQLException {
        if (indexName == null) return false;
        String effectiveSchema = ChangeLogHistory.effectiveSchema(connection, schema);
        String normIndex = normaliseIdentifier(indexName);

        if (tableName != null) {
            // Fast path: known table
            String normTable = normaliseIdentifier(tableName);
            if (indexExistsOnTable(effectiveSchema, normTable, normIndex)) return true;
            return indexExistsOnTable(effectiveSchema, alternateCaseName(tableName), alternateCaseName(indexName));
        }

        // F6: indexExists without a table name — scan all tables in the schema
        try (ResultSet tables = meta.getTables(null, effectiveSchema, "%", new String[]{"TABLE"})) {
            while (tables.next()) {
                String tbl = tables.getString("TABLE_NAME");
                if (indexExistsOnTable(effectiveSchema, tbl, normIndex)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean indexExistsOnTable(String schemaName, String tableName, String indexName) throws SQLException {
        try (ResultSet rs = meta.getIndexInfo(null, schemaName, tableName, false, false)) {
            while (rs.next()) {
                String idx = rs.getString("INDEX_NAME");
                if (indexName.equalsIgnoreCase(idx)) {
                    return true;
                }
            }
        } catch (SQLException e) {
            // Some drivers throw on non-existent table; treat as "not found"
            logger.tracef("Could not read index info for table %s.%s: %s", schemaName, tableName, e.getMessage());
        }
        return false;
    }

    private boolean foreignKeyExists(String tableName, String foreignKeyName) throws SQLException {
        if (foreignKeyName == null) return false;
        String effectiveSchema = ChangeLogHistory.effectiveSchema(connection, schema);
        String normFk = normaliseIdentifier(foreignKeyName);

        if (tableName != null) {
            return fkExistsOnTable(effectiveSchema, normaliseIdentifier(tableName), normFk);
        }

        // Scan all tables
        try (ResultSet tables = meta.getTables(null, effectiveSchema, "%", new String[]{"TABLE"})) {
            while (tables.next()) {
                String tbl = tables.getString("TABLE_NAME");
                if (fkExistsOnTable(effectiveSchema, tbl, normFk)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean fkExistsOnTable(String schemaName, String tableName, String fkName) throws SQLException {
        try (ResultSet rs = meta.getImportedKeys(null, schemaName, tableName)) {
            while (rs.next()) {
                String fk = rs.getString("FK_NAME");
                if (fkName.equalsIgnoreCase(fk)) {
                    return true;
                }
            }
        } catch (SQLException e) {
            logger.tracef("Could not read FK info for table %s.%s: %s", schemaName, tableName, e.getMessage());
        }
        return false;
    }

    private boolean uniqueConstraintExists(String tableName, String constraintName) throws SQLException {
        if (tableName == null || constraintName == null) return false;
        // Unique constraints appear as indexes with NON_UNIQUE=false; also check as an index name
        String effectiveSchema = ChangeLogHistory.effectiveSchema(connection, schema);
        String normTable = normaliseIdentifier(tableName);
        String normConstraint = normaliseIdentifier(constraintName);
        try (ResultSet rs = meta.getIndexInfo(null, effectiveSchema, normTable, true, false)) {
            while (rs.next()) {
                String idx = rs.getString("INDEX_NAME");
                if (normConstraint.equalsIgnoreCase(idx)) {
                    return true;
                }
            }
        } catch (SQLException e) {
            logger.tracef("Could not read unique constraint info for %s: %s", tableName, e.getMessage());
        }
        return false;
    }

    private boolean primaryKeyExists(String tableName, String primaryKeyName) throws SQLException {
        if (tableName == null) return false;
        String effectiveSchema = ChangeLogHistory.effectiveSchema(connection, schema);
        String normTable = normaliseIdentifier(tableName);
        try (ResultSet rs = meta.getPrimaryKeys(null, effectiveSchema, normTable)) {
            if (primaryKeyName == null) {
                return rs.next();
            }
            while (rs.next()) {
                String pk = rs.getString("PK_NAME");
                if (primaryKeyName.equalsIgnoreCase(pk)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean sqlCheck(String sql, String expectedResult) throws SQLException {
        if (sql == null) return false;
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                String actual = rs.getString(1);
                return expectedResult == null || expectedResult.equals(actual == null ? null : actual.trim());
            }
        }
        return false;
    }

    private boolean changeSetExecuted(String id, String author, String filename) {
        if (id == null || author == null || filename == null) return false;
        return executed.contains(new ChangeSetKey(id, author, filename));
    }

    // -------------------------------------------------------------------------
    // Identifier normalisation
    // -------------------------------------------------------------------------

    /**
     * Normalises an identifier for use in {@link DatabaseMetaData} lookups.
     * Oracle stores unquoted identifiers in upper case; Postgres in lower case; H2/MSSQL are case-insensitive.
     */
    private String normaliseIdentifier(String name) throws SQLException {
        if (name == null) return null;
        if (meta.storesUpperCaseIdentifiers()) {
            return name.toUpperCase(Locale.ROOT);
        }
        if (meta.storesLowerCaseIdentifiers()) {
            return name.toLowerCase(Locale.ROOT);
        }
        return name;
    }

    /** Returns the identifier in the case opposite to what {@link #normaliseIdentifier} produces. */
    private String alternateCaseName(String name) throws SQLException {
        if (name == null) return null;
        if (meta.storesUpperCaseIdentifiers()) {
            return name.toLowerCase(Locale.ROOT);
        }
        if (meta.storesLowerCaseIdentifiers()) {
            return name.toUpperCase(Locale.ROOT);
        }
        return name.toUpperCase(Locale.ROOT);
    }
}
