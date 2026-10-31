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

package org.keycloak.it.jpa.migration;

import java.util.List;

import org.keycloak.connections.jpa.entityprovider.JpaEntityProvider;
import org.keycloak.representations.migration.ChangeSetDescriptor;
import org.keycloak.representations.migration.ChangeSetStep;
import org.keycloak.representations.migration.MigrationManifest;

public class MigrationTestJpaEntityProvider implements JpaEntityProvider {

    /** Manifest with a single change-set that creates the MIGRATION_TEST_ENTITY table. */
    private static final MigrationManifest MANIFEST = buildManifest();

    private static MigrationManifest buildManifest() {
        // Standard SQL supported by all Keycloak databases.
        String createTable =
                "CREATE TABLE MIGRATION_TEST_ENTITY (" +
                "ID VARCHAR(36) NOT NULL, " +
                "NAME VARCHAR(255), " +
                "CONSTRAINT PK_MIGRATION_TEST_ENTITY PRIMARY KEY (ID))";

        ChangeSetStep step = new ChangeSetStep();
        step.setType("sql");
        for (String dialect : List.of("h2", "postgresql", "mysql", "mariadb", "oracle", "mssql")) {
            step.addStatement(dialect, createTable);
        }

        ChangeSetDescriptor cs = new ChangeSetDescriptor();
        cs.setId("migration-test-1.0");
        cs.setAuthor("keycloak");
        cs.setChangeLogFile("META-INF/migration-test-changelog.xml");
        cs.setCheckSum("9:migration-test-1.0");
        cs.getSteps().add(step);

        MigrationManifest manifest = new MigrationManifest();
        manifest.getChangeSets().add(cs);
        return manifest;
    }

    @Override
    public List<Class<?>> getEntities() {
        return List.of(MigrationTestEntity.class);
    }

    @Override
    public MigrationManifest getMigrationManifest() {
        return MANIFEST;
    }

    @Override
    public String getFactoryId() {
        return MigrationTestJpaEntityProviderFactory.ID;
    }

    @Override
    public void close() {
    }
}
