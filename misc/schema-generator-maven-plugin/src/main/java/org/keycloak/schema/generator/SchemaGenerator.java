package org.keycloak.schema.generator;

import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.keycloak.enums.SqlDialect;
import org.keycloak.representations.migration.ChangeSetDescriptor;
import org.keycloak.representations.migration.ChangeSetStep;
import org.keycloak.representations.migration.MigrationManifest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import liquibase.ChecksumVersion;
import liquibase.Scope;
import liquibase.change.Change;
import liquibase.change.core.CreateIndexChange;
import liquibase.change.custom.CustomChangeWrapper;
import liquibase.changelog.ChangeLogParameters;
import liquibase.changelog.ChangeSet;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.DatabaseList;
import liquibase.database.OfflineConnection;
import liquibase.parser.core.xml.XMLChangeLogSAXParser;
import liquibase.resource.ResourceAccessor;
import liquibase.sql.Sql;
import liquibase.sql.visitor.SqlVisitor;
import liquibase.sqlgenerator.SqlGeneratorFactory;
import liquibase.statement.SqlStatement;
import liquibase.util.LiquibaseUtil;

public class SchemaGenerator {

    /**
     * Placeholder emitted by Liquibase for {@code ${database.defaultSchemaName}} in raw {@code <sql>}
     * changes. The generator strips this token (including a trailing dot) so scripts contain unqualified
     * table names; the runtime applies the configured schema to the connection instead.
     */
    static final String SCHEMA_PLACEHOLDER = "${database.defaultSchemaName}";

    public static final List<String> DEFAULT_DIALECTS = Stream.of(SqlDialect.values())
            .filter(d -> d != SqlDialect.OTHER)
            .map(SqlDialect::manifestKey)
            .toList();

    private final String changelogFile;
    private final File outputDirectory;
    private final List<String> dialects;
    private final ClassLoader classLoader;
    private final ResourceAccessor resourceAccessor;
    private final ObjectMapper objectMapper;

    public SchemaGenerator(String changelogFile, File outputDirectory, List<String> dialects, ClassLoader classLoader) {
        this.changelogFile = changelogFile;
        this.outputDirectory = outputDirectory;
        this.dialects = (dialects == null || dialects.isEmpty()) ? DEFAULT_DIALECTS : dialects;
        LenientClassLoaderResourceAccessor accessor = new LenientClassLoaderResourceAccessor(
                classLoader != null ? classLoader : getClass().getClassLoader());
        this.classLoader = accessor.getClassLoader();
        this.resourceAccessor = accessor;
        this.objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    }

    public MigrationManifest generate() throws Exception {
        System.setProperty("liquibase.duplicateFileMode", "WARN");
        Map<String, Object> scopeValues = new HashMap<>();
        scopeValues.put(Scope.Attr.resourceAccessor.name(), resourceAccessor);
        scopeValues.put(Scope.Attr.classLoader.name(), classLoader);

        return Scope.child(scopeValues, () -> {
            XMLChangeLogSAXParser parser = new XMLChangeLogSAXParser();
            ChangeLogParameters changeLogParameters = new ChangeLogParameters();
            DatabaseChangeLog changeLog = parser.parse(changelogFile, changeLogParameters, resourceAccessor);

            Map<String, Database> databaseMap = new HashMap<>();
            for (String dialect : dialects) {
                OfflineConnection offlineConnection = new OfflineConnection("offline:" + dialect, resourceAccessor);
                Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(offlineConnection);
                // Do not qualify table names with a default schema in generated scripts.
                // The runtime applies the configured schema to the connection
                // (e.g. connection.setSchema(schema) / SET search_path for Postgres) so that
                // unqualified names resolve correctly without any text substitution.
                database.setOutputDefaultSchema(false);
                databaseMap.put(dialect, database);
            }

            MigrationManifest manifest = new MigrationManifest();
            manifest.setVersion(1);
            manifest.setGeneratedAt(Instant.now().toString());
            manifest.setLiquibaseVersion(LiquibaseUtil.getBuildVersion());

            int order = 1;
            for (ChangeSet changeSet : changeLog.getChangeSets()) {
                ChangeSetDescriptor descriptor = buildDescriptor(changeSet, order, databaseMap);
                manifest.getChangeSets().add(descriptor);
                order++;
            }

            for (Map.Entry<String, Database> entry : databaseMap.entrySet()) {
                manifest.getExpectedIndexes().put(entry.getKey(), ExpectedIndexComputer.compute(changeLog, entry.getValue()));
            }

            File manifestFile = new File(outputDirectory, "migration-manifest.json");
            outputDirectory.mkdirs();
            objectMapper.writeValue(manifestFile, manifest);

            return manifest;
        });
    }

    // -------------------------------------------------------------------------
    // Descriptor construction
    // -------------------------------------------------------------------------

    private ChangeSetDescriptor buildDescriptor(ChangeSet changeSet, int order, Map<String, Database> databaseMap) throws Exception {
        ChangeSetDescriptor descriptor = new ChangeSetDescriptor();
        descriptor.setOrder(order);
        descriptor.setId(changeSet.getId());
        descriptor.setAuthor(changeSet.getAuthor());
        descriptor.setChangeLogFile(changeSet.getFilePath());
        if (changeSet.getComments() != null && !changeSet.getComments().isBlank()) {
            descriptor.setComments(changeSet.getComments());
        }
        try {
            descriptor.setDescription(changeSet.getDescription());
        } catch (Exception ignored) {
        }
        if (changeSet.getDbmsSet() != null && !changeSet.getDbmsSet().isEmpty()) {
            descriptor.setDbms(changeSet.getDbmsSet());
        }
        try {
            descriptor.setCheckSum(changeSet.generateCheckSum(ChecksumVersion.latest()).toString());
        } catch (Exception e) {
            // A missing checksum would be written as NULL into DATABASECHANGELOG.MD5SUM, so fail the build.
            throw new IllegalStateException("Failed to generate checksum for change-set "
                    + changeSet.getId() + " by " + changeSet.getAuthor() + " in " + changeSet.getFilePath(), e);
        }
        if (changeSet.isAlwaysRun()) {
            throw new IllegalStateException("Change-set " + changeSet.getId() + " by " + changeSet.getAuthor()
                    + " in " + changeSet.getFilePath() + " uses runAlways=true which is not supported"
                    + " by the JDBC migration engine. Remove the attribute or migrate the change-set to a"
                    + " native JpaMigrationTask.");
        }
        if (changeSet.isRunOnChange()) {
            throw new IllegalStateException("Change-set " + changeSet.getId() + " by " + changeSet.getAuthor()
                    + " in " + changeSet.getFilePath() + " uses runOnChange=true which is not supported"
                    + " by the JDBC migration engine. Remove the attribute or migrate the change-set to a"
                    + " native JpaMigrationTask.");
        }
        if (changeSet.getPreconditions() != null) {
            descriptor.setPreConditions(PreconditionParser.parseContainer(changeSet.getPreconditions()));
        }

        // Build ordered steps: group consecutive non-custom changes into sql/createIndex steps,
        // emit a customTask step for each CustomChangeWrapper.
        List<ChangeSetStep> steps = buildSteps(changeSet, databaseMap);
        descriptor.setSteps(steps);

        // Flag for quick filtering: true when any step is a customTask.
        boolean hasCustomTask = steps.stream().anyMatch(s -> "customTask".equals(s.getType()));
        descriptor.setCustomTask(hasCustomTask);

        return descriptor;
    }

    /**
     * Converts the ordered list of {@link Change} objects in a change-set into an ordered list of
     * {@link ChangeSetStep}s.
     * <ul>
     *   <li>Each {@link CustomChangeWrapper} becomes a {@code customTask} step.</li>
     *   <li>Each {@link CreateIndexChange} becomes a {@code createIndex} step tagged with table/index name.</li>
     *   <li>All other changes (consecutive or not) are accumulated into {@code sql} steps; a new {@code sql}
     *       step is started whenever a non-SQL change (customTask/createIndex) interrupts the run.</li>
     * </ul>
     */
    private List<ChangeSetStep> buildSteps(ChangeSet changeSet, Map<String, Database> databaseMap) {
        List<ChangeSetStep> steps = new ArrayList<>();
        // Current SQL accumulator step (null when no SQL run is open)
        ChangeSetStep currentSqlStep = null;

        for (Change change : changeSet.getChanges()) {
            if (change instanceof CustomChangeWrapper customWrapper) {
                // Flush any open SQL step first
                if (currentSqlStep != null && hasAnyStatements(currentSqlStep)) {
                    steps.add(currentSqlStep);
                }
                currentSqlStep = null;
                steps.add(ChangeSetStep.customTask(customWrapper.getClassName()));

            } else if (change instanceof CreateIndexChange indexChange) {
                // Flush any open SQL step first
                if (currentSqlStep != null && hasAnyStatements(currentSqlStep)) {
                    steps.add(currentSqlStep);
                }
                currentSqlStep = null;
                ChangeSetStep indexStep = buildCreateIndexStep(changeSet, indexChange, databaseMap);
                if (indexStep != null) {
                    steps.add(indexStep);
                }

            } else {
                // Regular SQL change – accumulate into the current sql step
                if (currentSqlStep == null) {
                    currentSqlStep = new ChangeSetStep();
                    currentSqlStep.setType("sql");
                }
                accumulateSqlChange(currentSqlStep, change, changeSet, databaseMap);
            }
        }

        // Flush any remaining SQL step
        if (currentSqlStep != null && hasAnyStatements(currentSqlStep)) {
            steps.add(currentSqlStep);
        }

        return steps;
    }

    /** Generates SQL for one non-custom, non-index {@link Change} and adds it to {@code step}. */
    private void accumulateSqlChange(ChangeSetStep step, Change change, ChangeSet changeSet,
            Map<String, Database> databaseMap) {
        for (Map.Entry<String, Database> entry : databaseMap.entrySet()) {
            String dialect = entry.getKey();
            Database database = entry.getValue();

            if (!dialectMatchesChangeSet(changeSet, database)) {
                continue;
            }

            SqlStatement[] statements = change.generateStatements(database);
            if (statements == null) {
                continue;
            }
            for (SqlStatement stmt : statements) {
                Sql[] generatedSql = SqlGeneratorFactory.getInstance().generateSql(stmt, database);
                if (generatedSql == null) {
                    continue;
                }
                for (Sql sql : generatedSql) {
                    String sqlStr = applySqlVisitors(sql.toSql(), changeSet, database);
                    sqlStr = resolveSchemaPlaceholder(sqlStr);
                    if (sqlStr != null && !sqlStr.isBlank()) {
                        step.addStatement(dialect, sqlStr.trim());
                    }
                }
            }
        }
    }

    /**
     * Builds a {@code createIndex} step for a {@link CreateIndexChange}, tagged with table name and
     * index name so the runtime can apply the index-creation threshold check.
     * Returns {@code null} when the change produces no SQL for any dialect.
     */
    private ChangeSetStep buildCreateIndexStep(ChangeSet changeSet, CreateIndexChange indexChange,
            Map<String, Database> databaseMap) {
        ChangeSetStep step = new ChangeSetStep();
        step.setType("createIndex");
        step.setTableName(indexChange.getTableName());
        step.setIndexName(indexChange.getIndexName());

        for (Map.Entry<String, Database> entry : databaseMap.entrySet()) {
            String dialect = entry.getKey();
            Database database = entry.getValue();

            if (!dialectMatchesChangeSet(changeSet, database)) {
                continue;
            }

            SqlStatement[] statements = indexChange.generateStatements(database);
            if (statements == null) {
                continue;
            }
            for (SqlStatement stmt : statements) {
                Sql[] generatedSql = SqlGeneratorFactory.getInstance().generateSql(stmt, database);
                if (generatedSql == null) {
                    continue;
                }
                for (Sql sql : generatedSql) {
                    String sqlStr = applySqlVisitors(sql.toSql(), changeSet, database);
                    sqlStr = resolveSchemaPlaceholder(sqlStr);
                    if (sqlStr != null && !sqlStr.isBlank()) {
                        step.addStatement(dialect, sqlStr.trim());
                    }
                }
            }
        }

        return hasAnyStatements(step) ? step : null;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private boolean dialectMatchesChangeSet(ChangeSet changeSet, Database database) {
        if (changeSet.getDbmsSet() == null || changeSet.getDbmsSet().isEmpty()) {
            return true;
        }
        return DatabaseList.definitionMatches(changeSet.getDbmsSet(), database, true);
    }

    static String applySqlVisitors(String sqlStr, ChangeSet changeSet, Database database) {
        if (changeSet.getSqlVisitors() != null) {
            for (SqlVisitor visitor : changeSet.getSqlVisitors()) {
                if (DatabaseList.definitionMatches(visitor.getApplicableDbms(), database, true)) {
                    sqlStr = visitor.modifySql(sqlStr, database);
                }
            }
        }
        return sqlStr;
    }

    /**
     * Removes the {@code ${database.defaultSchemaName}.} prefix (with optional trailing dot) from SQL
     * generated from raw {@code <sql>} changes. The schema is applied to the connection at runtime, so
     * table names in the scripts must be unqualified.
     */
    static String resolveSchemaPlaceholder(String sql) {
        if (sql == null) {
            return null;
        }
        // Replace "${database.defaultSchemaName}." (with trailing dot) first, then bare token
        sql = sql.replace(SCHEMA_PLACEHOLDER + ".", "");
        sql = sql.replace(SCHEMA_PLACEHOLDER, "");
        return sql;
    }

    private boolean hasAnyStatements(ChangeSetStep step) {
        return step.getStatements() != null && !step.getStatements().isEmpty();
    }

    private String sanitizeFilename(String id) {
        return id.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
