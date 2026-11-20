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

package org.keycloak.it.storage.database;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.KeycloakRunner;
import org.keycloak.it.utils.RawDistRootPath;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * End-to-end check of {@code migration-strategy=manual} on an empty database: the schema produced by applying the
 * generated script must be the schema produced by the automatic migration.
 * <ol>
 *   <li>start the server with the default strategy against an empty database and snapshot the resulting schema</li>
 *   <li>empty the database again and export the script with {@code migration-strategy=manual}</li>
 *   <li>empty the database once more (the export creates the Liquibase tables on the live connection), apply the
 *       script over plain JDBC and snapshot the resulting schema</li>
 *   <li>start the server with {@code migration-strategy=validate}, which only succeeds if the script left behind a
 *       database the server accepts as up to date</li>
 *   <li>compare both snapshots</li>
 * </ol>
 * The snapshots are taken from JDBC metadata, so the comparison is the same for every database. It covers tables,
 * columns (type, size, nullability, default), primary keys, indexes (columns, uniqueness, filter condition) and foreign
 * keys, plus the set of recorded changesets.
 * <p>
 * Differences that are known and not yet fixed are passed in as allowlists. An allowlisted entry that no longer
 * differs fails the test, so the lists can only shrink.
 */
public final class ManualMigrationParity {

    private static final String SCRIPT_NAME = "keycloak-database-update.sql";
    private static final String STARTED = "Listening on:";

    /** Gives the test access to the database under test. */
    public interface Database {

        Connection open() throws Exception;

        /** Remove everything from the database, leaving it as a fresh empty database. */
        void reset() throws Exception;

        /** Whether the database has sequences and exposes them in {@code INFORMATION_SCHEMA.SEQUENCES}. */
        default boolean supportsSequences() {
            return true;
        }
    }

    /**
     * Known drift of the script on an empty database, as prefixes of snapshot entries. Both lists exist because the
     * {@code TEST} preconditions of these changesets are evaluated against the empty generation database: the index or
     * column they remove does not exist there yet, so the removal is not emitted although the script creates it.
     * Fixing a changeset means deleting its entries here.
     */
    public static final List<String> KNOWN_EXTRA_IN_SCRIPT = List.of(
            // 25.0.0-28265-index-cleanup-*
            "INDEX OFFLINE_USER_SESSION IDX_OFFLINE_USS_CREATEDON(",
            "INDEX OFFLINE_USER_SESSION IDX_OFFLINE_USS_PRELOAD(",
            "INDEX OFFLINE_USER_SESSION IDX_OFFLINE_USS_BY_USERSESS(",
            "INDEX OFFLINE_CLIENT_SESSION IDX_OFFLINE_CSS_PRELOAD(",
            // the same index, which 26.0.0-32583-drop-redundant-index-on-client-session is expected to remove
            "INDEX OFFLINE_CLIENT_SESSION IDX_US_SESS_ID_ON_CL_SESS(",
            // replaced by 26.5.0-drop-sess-refresh-idx
            "INDEX OFFLINE_USER_SESSION IDX_OFFLINE_USS_BY_LAST_SESSION_REFRESH(",
            // 26.5.0-remove-workflow-provider-id-column
            "COLUMN WORKFLOW_STATE.WORKFLOW_PROVIDER_ID ",
            "INDEX WORKFLOW_STATE IDX_WORKFLOW_STATE_PROVIDER(");

    public static final List<String> KNOWN_MISSING_FROM_SCRIPT = List.of(
            // the single-column version that 26.5.0-remove-workflow-provider-id-column creates
            "INDEX WORKFLOW_STATE IDX_WORKFLOW_STATE_PROVIDER(");

    private ManualMigrationParity() {
    }

    /**
     * @param startArgs the command and options needed to start the server against {@code database}, without any
     *                  migration strategy
     * @param onlyInUpdate known entries that exist after the automatic migration but not after the script
     * @param onlyInScript known entries that exist after the script but not after the automatic migration
     */
    public static void verify(KeycloakRunner runner, RawDistRootPath rawDistRootPath, Database database, List<String> startArgs,
            Collection<String> onlyInUpdate, Collection<String> onlyInScript) throws Exception {
        database.reset();

        // automatic migration
        runner.run(startArgs).assertMessage(STARTED);
        Snapshot updated;
        try (Connection connection = database.open()) {
            updated = snapshot(connection, database.supportsSequences());
        }

        // export. Fails to start by design, the script is what matters
        database.reset();
        CLIResult export = runner.run(withArgs(startArgs,
                "--spi-connections-jpa-quarkus-migration-strategy=manual",
                "--spi-connections-jpa-quarkus-initialize-empty=false"));
        export.assertMessage("Database not initialized, please initialize database with");
        String script = readAndDeleteScript(rawDistRootPath);

        // apply the script to an empty database
        database.reset();
        Snapshot scripted;
        try (Connection connection = database.open()) {
            execute(connection, script);
            scripted = snapshot(connection, database.supportsSequences());
        }

        // the server must accept the result as an up-to-date database
        CLIResult validate = runner.run(withArgs(startArgs, "--spi-connections-jpa-quarkus-migration-strategy=validate"));
        validate.assertMessage(STARTED);
        validate.assertNoMessage("Database not up-to-date");

        compare(updated, scripted, onlyInUpdate, onlyInScript);
    }

    private static List<String> withArgs(List<String> base, String... extra) {
        List<String> args = new ArrayList<>(base);
        args.addAll(List.of(extra));
        return args;
    }

    private static String readAndDeleteScript(RawDistRootPath rawDistRootPath) throws IOException {
        File script = rawDistRootPath.getDistRootPath().resolve("bin").resolve(SCRIPT_NAME).toFile();
        assertTrue(script.isFile(), "Export file must exist when migration-strategy=manual");
        String content = Files.readString(script.toPath(), StandardCharsets.UTF_8);
        Files.delete(script.toPath());
        return content;
    }

    // ---------------------------------------------------------------------------------------------------------
    // Script execution
    // ---------------------------------------------------------------------------------------------------------

    /**
     * Runs the script one statement at a time. Statements end at a semicolon outside of a quoted string; {@code --}
     * comments are skipped. This is enough for the output of Liquibase, which is the only producer of these scripts.
     */
    static void execute(Connection connection, String script) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            for (String sql : split(script)) {
                try {
                    statement.execute(sql);
                } catch (SQLException e) {
                    String shortSql = sql.length() > 400 ? sql.substring(0, 400) + "..." : sql;
                    throw new SQLException("Failed to apply script statement: " + shortSql, e);
                }
            }
        }
    }

    static List<String> split(String script) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;

        for (int i = 0; i < script.length(); i++) {
            char c = script.charAt(i);

            if (quoted) {
                current.append(c);
                if (c == '\'') {
                    quoted = false; // an escaped quote ('') leaves and re-enters the string, which is equivalent
                }
            } else if (c == '\'') {
                quoted = true;
                current.append(c);
            } else if (c == '-' && i + 1 < script.length() && script.charAt(i + 1) == '-') {
                while (i < script.length() && script.charAt(i) != '\n') {
                    i++;
                }
                current.append('\n');
            } else if (c == ';') {
                addIfNotBlank(statements, current);
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        addIfNotBlank(statements, current);

        return statements;
    }

    private static void addIfNotBlank(List<String> statements, StringBuilder statement) {
        String sql = statement.toString().trim();
        if (!sql.isEmpty()) {
            statements.add(sql);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Snapshot and comparison
    // ---------------------------------------------------------------------------------------------------------

    record Snapshot(SortedSet<String> structure, SortedMap<String, String> changesets) {
    }

    /**
     * Snapshot of the catalog and schema the connection points at. MySQL and MariaDB have a catalog and no schema,
     * PostgreSQL has both, so both are taken from the connection and passed to every metadata call.
     */
    static Snapshot snapshot(Connection connection, boolean sequences) throws SQLException {
        String catalog = connection.getCatalog();
        String schema = connection.getSchema();
        DatabaseMetaData metaData = connection.getMetaData();
        SortedSet<String> structure = new TreeSet<>();

        List<String> tables = new ArrayList<>();
        try (ResultSet rs = metaData.getTables(catalog, schema, "%", new String[] { "TABLE" })) {
            while (rs.next()) {
                tables.add(rs.getString("TABLE_NAME"));
            }
        }

        for (String table : tables) {
            String name = upper(table);
            structure.add("TABLE " + name);
            addColumns(metaData, catalog, schema, table, name, structure);
            addPrimaryKey(metaData, catalog, schema, table, name, structure);
            addIndexes(metaData, catalog, schema, table, name, structure);
            addForeignKeys(metaData, catalog, schema, table, name, structure);
        }

        // INFORMATION_SCHEMA names the schema of an object TABLE_SCHEMA, which is the catalog on MySQL and MariaDB
        String informationSchema = schema != null ? schema : catalog;
        addConstraints(connection, informationSchema, structure);
        if (sequences) {
            addSequences(connection, informationSchema, structure);
        }

        SortedMap<String, String> changesets = new TreeMap<>();
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT ID, AUTHOR, FILENAME, EXECTYPE FROM DATABASECHANGELOG")) {
            while (rs.next()) {
                changesets.put(rs.getString(3) + "::" + rs.getString(1) + "::" + rs.getString(2), rs.getString(4));
            }
        }

        return new Snapshot(structure, changesets);
    }

    private static void addColumns(DatabaseMetaData metaData, String catalog, String schema, String table, String name, Set<String> out) throws SQLException {
        try (ResultSet rs = metaData.getColumns(catalog, schema, table, "%")) {
            while (rs.next()) {
                out.add("COLUMN %s.%s %s(%s,%s) nullable=%s default=%s".formatted(
                        name, upper(rs.getString("COLUMN_NAME")), upper(rs.getString("TYPE_NAME")),
                        rs.getString("COLUMN_SIZE"), rs.getString("DECIMAL_DIGITS"),
                        rs.getString("IS_NULLABLE"), normalize(rs.getString("COLUMN_DEF"))));
            }
        }
    }

    private static void addPrimaryKey(DatabaseMetaData metaData, String catalog, String schema, String table, String name, Set<String> out) throws SQLException {
        SortedMap<Integer, String> columns = new TreeMap<>();
        String pkName = null;
        try (ResultSet rs = metaData.getPrimaryKeys(catalog, schema, table)) {
            while (rs.next()) {
                pkName = rs.getString("PK_NAME");
                columns.put(rs.getInt("KEY_SEQ"), upper(rs.getString("COLUMN_NAME")));
            }
        }
        if (!columns.isEmpty()) {
            out.add("PRIMARY KEY %s %s(%s)".formatted(name, generatedName(pkName), String.join(",", columns.values())));
        }
    }

    private static void addIndexes(DatabaseMetaData metaData, String catalog, String schema, String table, String name, Set<String> out) throws SQLException {
        Map<String, SortedMap<Integer, String>> columns = new TreeMap<>();
        Map<String, String> attributes = new TreeMap<>();

        try (ResultSet rs = metaData.getIndexInfo(catalog, schema, table, false, false)) {
            while (rs.next()) {
                String index = rs.getString("INDEX_NAME");
                String column = rs.getString("COLUMN_NAME");
                if (index == null || column == null) {
                    continue;
                }
                String key = generatedName(index);
                columns.computeIfAbsent(key, k -> new TreeMap<>()).put(rs.getInt("ORDINAL_POSITION"), upper(column));
                attributes.put(key, "unique=%s filter=%s".formatted(!rs.getBoolean("NON_UNIQUE"), normalize(rs.getString("FILTER_CONDITION"))));
            }
        }

        columns.forEach((index, cols) ->
                out.add("INDEX %s %s(%s) %s".formatted(name, index, String.join(",", cols.values()), attributes.get(index))));
    }

    private static void addForeignKeys(DatabaseMetaData metaData, String catalog, String schema, String table, String name, Set<String> out) throws SQLException {
        Map<String, SortedMap<Integer, String>> columns = new TreeMap<>();
        Map<String, String> targets = new TreeMap<>();

        try (ResultSet rs = metaData.getImportedKeys(catalog, schema, table)) {
            while (rs.next()) {
                String fk = upper(rs.getString("FK_NAME"));
                columns.computeIfAbsent(fk, k -> new TreeMap<>())
                        .put(rs.getInt("KEY_SEQ"), upper(rs.getString("FKCOLUMN_NAME")) + "->" + upper(rs.getString("PKCOLUMN_NAME")));
                targets.put(fk, "%s onDelete=%s onUpdate=%s".formatted(upper(rs.getString("PKTABLE_NAME")),
                        rs.getInt("DELETE_RULE"), rs.getInt("UPDATE_RULE")));
            }
        }

        columns.forEach((fk, cols) ->
                out.add("FOREIGN KEY %s %s(%s) %s".formatted(name, fk, String.join(",", cols.values()), targets.get(fk))));
    }

    /**
     * Unique and check constraints, from {@code information_schema}, which JDBC metadata has no portable API for.
     * A unique constraint also shows up as a unique index, but only here is it a constraint rather than a bare index.
     * {@code NOT NULL} is reported as a check constraint by some databases, with a name derived from internal ids;
     * it is already covered by the nullability of the column.
     */
    private static void addConstraints(Connection connection, String schema, Set<String> out) throws SQLException {
        String sql = "SELECT tc.TABLE_NAME, tc.CONSTRAINT_NAME, tc.CONSTRAINT_TYPE, cc.CHECK_CLAUSE"
                + " FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS tc"
                + " LEFT JOIN INFORMATION_SCHEMA.CHECK_CONSTRAINTS cc"
                + " ON cc.CONSTRAINT_SCHEMA = tc.CONSTRAINT_SCHEMA AND cc.CONSTRAINT_NAME = tc.CONSTRAINT_NAME"
                + " WHERE tc.TABLE_SCHEMA = ? AND tc.CONSTRAINT_TYPE IN ('UNIQUE', 'CHECK')";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, schema);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    String type = rs.getString(3);
                    String clause = normalize(rs.getString(4));
                    if ("CHECK".equals(type) && (clause == null || clause.contains("IS NOT NULL"))) {
                        continue;
                    }
                    out.add("CONSTRAINT %s %s %s%s".formatted(upper(rs.getString(1)), generatedName(rs.getString(2)), type,
                            clause == null ? "" : " " + clause));
                }
            }
        }
    }

    /** Sequences, including those behind identity columns. */
    private static void addSequences(Connection connection, String schema, Set<String> out) throws SQLException {
        String sql = "SELECT SEQUENCE_NAME, DATA_TYPE, START_VALUE, MINIMUM_VALUE, MAXIMUM_VALUE, INCREMENT, CYCLE_OPTION"
                + " FROM INFORMATION_SCHEMA.SEQUENCES WHERE SEQUENCE_SCHEMA = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, schema);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    out.add("SEQUENCE %s %s start=%s min=%s max=%s increment=%s cycle=%s".formatted(
                            generatedName(rs.getString(1)), upper(rs.getString(2)), rs.getString(3), rs.getString(4),
                            rs.getString(5), rs.getString(6), upper(rs.getString(7))));
                }
            }
        }
    }

    private static String upper(String value) {
        return value == null ? null : value.toUpperCase(Locale.ROOT);
    }

    private static String normalize(String value) {
        return value == null ? null : value.replaceAll("\\s+", " ").trim().toUpperCase(Locale.ROOT);
    }

    /** Names that the database generates itself carry a counter that depends on the order of creation. */
    private static String generatedName(String name) {
        return name == null ? null : upper(name).replaceAll("^(PRIMARY_KEY|CONSTRAINT_INDEX|CONSTRAINT|INDEX|SYSTEM_SEQUENCE)_[0-9A-F_]+$", "$1_<generated>")
                .replaceAll("_INDEX_[0-9A-F]+$", "_INDEX_<generated>");
    }

    private static void compare(Snapshot updated, Snapshot scripted, Collection<String> onlyInUpdate, Collection<String> onlyInScript) {
        SortedSet<String> missingFromScript = new TreeSet<>(updated.structure());
        missingFromScript.removeAll(scripted.structure());
        SortedSet<String> extraInScript = new TreeSet<>(scripted.structure());
        extraInScript.removeAll(updated.structure());

        // the recorded changesets must be the same. Their exec type may legitimately differ: whether a precondition
        // passes is decided against the live database in one case and against the generation database in the other
        for (String changeset : updated.changesets().keySet()) {
            if (!scripted.changesets().containsKey(changeset)) {
                missingFromScript.add("CHANGESET " + changeset);
            }
        }
        for (String changeset : scripted.changesets().keySet()) {
            if (!updated.changesets().containsKey(changeset)) {
                extraInScript.add("CHANGESET " + changeset);
            }
        }

        List<String> problems = new ArrayList<>();
        reportUnexpected(problems, "exist after the automatic migration but not after applying the script", missingFromScript, onlyInUpdate);
        reportUnexpected(problems, "exist after applying the script but not after the automatic migration", extraInScript, onlyInScript);
        reportStale(problems, "are allowlisted as missing from the script but no longer are", missingFromScript, onlyInUpdate);
        reportStale(problems, "are allowlisted as extra in the script but no longer are", extraInScript, onlyInScript);

        if (!problems.isEmpty()) {
            fail("The schema created by the manual migration script differs from the one created by the automatic migration:\n"
                    + String.join("\n", problems));
        }

        assertThat(scripted.changesets().size(), is(updated.changesets().size()));
    }

    private static boolean allowed(String entry, Collection<String> allowedPrefixes) {
        return allowedPrefixes.stream().anyMatch(entry::startsWith);
    }

    private static void reportUnexpected(List<String> problems, String what, Set<String> actual, Collection<String> allowed) {
        SortedSet<String> unexpected = new TreeSet<>(actual);
        unexpected.removeIf(entry -> allowed(entry, allowed));
        if (!unexpected.isEmpty()) {
            problems.add("Entries that " + what + ":\n  " + String.join("\n  ", unexpected));
        }
    }

    private static void reportStale(List<String> problems, String what, Set<String> actual, Collection<String> allowed) {
        SortedSet<String> stale = new TreeSet<>(allowed);
        stale.removeIf(prefix -> actual.stream().anyMatch(entry -> entry.startsWith(prefix)));
        if (!stale.isEmpty()) {
            problems.add("Entries that " + what + ":\n  " + String.join("\n  ", stale));
        }
    }
}
