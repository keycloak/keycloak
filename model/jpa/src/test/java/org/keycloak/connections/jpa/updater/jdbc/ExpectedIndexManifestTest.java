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
package org.keycloak.connections.jpa.updater.jdbc;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.keycloak.enums.SqlDialect;
import org.keycloak.representations.migration.ExpectedIndexDescriptor;
import org.keycloak.representations.migration.MigrationManifest;

import org.junit.Assert;
import org.junit.Test;

/**
 * Verifies that the generated manifest carries the expected indexes per dialect, used by the index checker.
 */
public class ExpectedIndexManifestTest {

    private static final String PARTIAL_INDEX = "IDX_OFFLINE_USS_BY_BROKER_SESSION_ID";

    @Test
    public void everyDialectHasExpectedIndexes() {
        MigrationManifest manifest = MigrationManifestLoader.loadFresh();
        for (SqlDialect dialect : SqlDialect.values()) {
            if (dialect == SqlDialect.OTHER) {
                continue;
            }
            List<ExpectedIndexDescriptor> indexes = manifest.getExpectedIndexes().get(dialect.manifestKey());
            Assert.assertNotNull("No expected indexes for " + dialect, indexes);
            Assert.assertTrue("Too few expected indexes for " + dialect, indexes.size() > 100);
            for (ExpectedIndexDescriptor index : indexes) {
                Assert.assertNotNull(index.getTableName());
                Assert.assertNotNull(index.getIndexName());
                Assert.assertTrue(index.getSql(), index.getSql().toUpperCase().contains("CREATE"));
                Assert.assertFalse("Unresolved placeholder: " + index.getSql(), index.getSql().contains("${"));
            }
        }
    }

    @Test
    public void indexNamesAreUniquePerDialect() {
        MigrationManifest manifest = MigrationManifestLoader.loadFresh();
        manifest.getExpectedIndexes().forEach((dialect, indexes) -> {
            Set<String> names = indexes.stream().map(i -> i.getIndexName().toUpperCase()).collect(Collectors.toSet());
            Assert.assertEquals("Duplicate index names for " + dialect, indexes.size(), names.size());
        });
    }

    @Test
    public void partialIndexSqlIsDialectSpecific() {
        MigrationManifest manifest = MigrationManifestLoader.loadFresh();
        for (String dialect : List.of("postgresql", "mssql")) {
            Assert.assertTrue(dialect, sqlOf(manifest, dialect).contains("BROKER_SESSION_ID IS NOT NULL"));
        }
        for (String dialect : List.of("mysql", "mariadb", "oracle")) {
            Assert.assertFalse(dialect, sqlOf(manifest, dialect).contains("WHERE"));
        }
    }

    private static String sqlOf(MigrationManifest manifest, String dialect) {
        return manifest.getExpectedIndexes().get(dialect).stream()
                .filter(i -> PARTIAL_INDEX.equalsIgnoreCase(i.getIndexName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError(PARTIAL_INDEX + " missing for " + dialect))
                .getSql();
    }
}
