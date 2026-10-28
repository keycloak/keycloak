package org.keycloak.schema.generator;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.List;

import org.keycloak.representations.migration.ChangeSetDescriptor;
import org.keycloak.representations.migration.ChangeSetStep;
import org.keycloak.representations.migration.ExpectedIndexDescriptor;
import org.keycloak.representations.migration.MigrationManifest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SchemaGeneratorTest {

    @Test
    public void testGenerateSchemaAndManifest(@TempDir Path tempDir) throws Exception {
        File outputDir = tempDir.toFile();
        SchemaGenerator generator = new SchemaGenerator(
                "test-changelog.xml",
                outputDir,
                List.of("postgresql", "mysql", "h2"),
                getClass().getClassLoader()
        );

        MigrationManifest manifest = generator.generate();

        assertNotNull(manifest);
        assertEquals(2, manifest.getChangeSets().size());

        // --- cs1: createTable change-set with NOT/tableExists precondition ---
        ChangeSetDescriptor cs1 = manifest.getChangeSets().get(0);
        assertNotNull(cs1.getCheckSum());
        assertEquals("1.0.0-test-table", cs1.getId());
        assertEquals("keycloak", cs1.getAuthor());
        assertFalse(cs1.isCustomTask());
        assertNotNull(cs1.getPreConditions());
        assertEquals("MARK_RAN", cs1.getPreConditions().getOnFail());
        assertEquals("not", cs1.getPreConditions().getConditions().get(0).getType());
        assertEquals("tableExists", cs1.getPreConditions().getConditions().get(0).getNestedConditions().get(0).getType());
        assertEquals("TEST_ENTITY", cs1.getPreConditions().getConditions().get(0).getNestedConditions().get(0).getTableName());

        // Should have exactly one sql step covering all three dialects
        List<ChangeSetStep> steps1 = cs1.getSteps();
        assertEquals(1, steps1.size());
        ChangeSetStep sqlStep = steps1.get(0);
        assertEquals("sql", sqlStep.getType());
        assertTrue(sqlStep.getStatements().containsKey("postgresql"), "Expected postgresql statements");
        assertTrue(sqlStep.getStatements().containsKey("mysql"), "Expected mysql statements");
        assertTrue(sqlStep.getStatements().containsKey("h2"), "Expected h2 statements");
        // Verify the statement content looks like a CREATE TABLE
        String firstPgStmt = sqlStep.getStatements().get("postgresql").get(0);
        assertTrue(firstPgStmt.toUpperCase().contains("CREATE TABLE"), "Expected CREATE TABLE: " + firstPgStmt);
        assertTrue(firstPgStmt.toUpperCase().contains("TEST_ENTITY"), "Expected TEST_ENTITY: " + firstPgStmt);
        // Verify no schema placeholder remains
        assertFalse(firstPgStmt.contains("${database.defaultSchemaName}"), "Schema placeholder must not appear in statements");

        // --- cs2: createIndex change-set ---
        ChangeSetDescriptor cs2 = manifest.getChangeSets().get(1);
        assertEquals("1.1.0-test-index", cs2.getId());
        assertFalse(cs2.isCustomTask());

        // Should have exactly one createIndex step
        List<ChangeSetStep> steps2 = cs2.getSteps();
        assertEquals(1, steps2.size());
        ChangeSetStep idxStep = steps2.get(0);
        assertEquals("createIndex", idxStep.getType());
        assertEquals("TEST_ENTITY", idxStep.getTableName());
        assertEquals("IDX_TEST_ENTITY_NAME", idxStep.getIndexName());
        assertTrue(idxStep.getStatements().containsKey("postgresql"), "Expected postgresql index statements");

        File manifestFile = new File(outputDir, "migration-manifest.json");
        assertTrue(manifestFile.exists());

        // Expected indexes per dialect
        for (String dialect : List.of("postgresql", "mysql", "h2")) {
            List<ExpectedIndexDescriptor> expected = manifest.getExpectedIndexes().get(dialect);
            assertNotNull(expected, "Expected indexes for " + dialect);
            assertEquals(1, expected.size());
            assertEquals("TEST_ENTITY", expected.get(0).getTableName());
            assertEquals("IDX_TEST_ENTITY_NAME", expected.get(0).getIndexName());
            assertTrue(expected.get(0).getSql().toUpperCase().contains("CREATE INDEX"), expected.get(0).getSql());
        }
    }

    @Test
    public void testGenerateFromMasterChangelog(@TempDir Path tempDir) throws Exception {
        File modelJpaResources = new File("../../model/jpa/src/main/resources");
        if (!modelJpaResources.exists()) {
            modelJpaResources = new File("model/jpa/src/main/resources");
        }
        if (!modelJpaResources.exists()) {
            return; // Skip if run from isolated context where relative path is different
        }

        URLClassLoader cl = new URLClassLoader(
                new URL[]{modelJpaResources.toURI().toURL()},
                getClass().getClassLoader()
        );

        File outputDir = tempDir.toFile();
        SchemaGenerator generator = new SchemaGenerator(
                "META-INF/jpa-changelog-master.xml",
                outputDir,
                List.of("postgresql", "mysql", "mariadb", "oracle", "mssql", "h2"),
                cl
        );

        MigrationManifest manifest = generator.generate();

        assertNotNull(manifest);
        assertFalse(manifest.getChangeSets().isEmpty());
        assertEquals(237, manifest.getChangeSets().size());

        File manifestFile = new File(outputDir, "migration-manifest.json");
        assertTrue(manifestFile.exists());

        // Verify JSON roundtrip
        ObjectMapper mapper = new ObjectMapper();
        MigrationManifest readManifest = mapper.readValue(manifestFile, MigrationManifest.class);
        assertEquals(237, readManifest.getChangeSets().size());

        for (int i = 0; i < readManifest.getChangeSets().size(); i++) {
            ChangeSetDescriptor cs = readManifest.getChangeSets().get(i);
            assertEquals(i + 1, cs.getOrder());
            assertNotNull(cs.getId());
            assertNotNull(cs.getAuthor());
            assertNotNull(cs.getChangeLogFile());
        }

        long customTaskCount = manifest.getChangeSets().stream().filter(ChangeSetDescriptor::isCustomTask).count();
        long withSqlStepsCount = manifest.getChangeSets().stream()
                .filter(cs -> cs.getSteps().stream().anyMatch(s -> "sql".equals(s.getType())))
                .count();
        long withCreateIndexCount = manifest.getChangeSets().stream()
                .filter(cs -> cs.getSteps().stream().anyMatch(s -> "createIndex".equals(s.getType())))
                .count();

        assertTrue(customTaskCount > 0, "Expected custom migration tasks");
        assertTrue(withSqlStepsCount > 0, "Expected change-sets with sql steps");
        assertTrue(withCreateIndexCount > 0, "Expected change-sets with createIndex steps");

        // Mixed change-sets: some change-sets must have both sql and customTask steps
        long mixedCount = manifest.getChangeSets().stream()
                .filter(cs -> cs.isCustomTask()
                        && cs.getSteps().stream().anyMatch(s -> "sql".equals(s.getType()) || "createIndex".equals(s.getType())))
                .count();
        assertTrue(mixedCount > 0, "Expected mixed change-sets with both sql and customTask steps");

        // Every dialect must have expected indexes, and the known partial index must be present
        for (String dialect : List.of("postgresql", "mysql", "mariadb", "oracle", "mssql", "h2")) {
            List<ExpectedIndexDescriptor> expected = readManifest.getExpectedIndexes().get(dialect);
            assertNotNull(expected, "Expected indexes for " + dialect);
            assertTrue(expected.size() > 100, "Too few expected indexes for " + dialect + ": " + expected.size());
            assertTrue(expected.stream().anyMatch(i -> "IDX_OFFLINE_USS_BY_BROKER_SESSION_ID".equalsIgnoreCase(i.getIndexName())),
                    "Missing IDX_OFFLINE_USS_BY_BROKER_SESSION_ID for " + dialect);
        }
        String pgSql = readManifest.getExpectedIndexes().get("postgresql").stream()
                .filter(i -> "IDX_OFFLINE_USS_BY_BROKER_SESSION_ID".equalsIgnoreCase(i.getIndexName()))
                .findFirst().orElseThrow().getSql();
        assertTrue(pgSql.contains("BROKER_SESSION_ID IS NOT NULL"), pgSql);

        // No schema placeholder must remain in any statement across all dialects
        for (ChangeSetDescriptor cs : readManifest.getChangeSets()) {
            for (ChangeSetStep step : cs.getSteps()) {
                if (step.getStatements() != null) {
                    for (List<String> stmts : step.getStatements().values()) {
                        for (String stmt : stmts) {
                            assertFalse(stmt.contains("${"),
                                    "Unresolved placeholder in changeset " + cs.getId() + ": " + stmt);
                        }
                    }
                }
            }
        }
    }
}
