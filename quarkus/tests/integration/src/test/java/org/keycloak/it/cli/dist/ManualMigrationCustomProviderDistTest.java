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

package org.keycloak.it.cli.dist;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import org.keycloak.it.jpa.migration.MigrationTestProvider;
import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.KeycloakRunner;
import org.keycloak.it.junit5.extension.RawDistOnly;
import org.keycloak.it.junit5.extension.TestProvider;
import org.keycloak.it.utils.RawDistRootPath;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DistributionTest
@RawDistOnly(reason = "Containers are immutable")
@TestProvider(MigrationTestProvider.class)
public class ManualMigrationCustomProviderDistTest {

    @Test
    void testCustomProviderChangesetExportedInManualMode(KeycloakRunner runner, RawDistRootPath rawDistRootPath) {
        // The framework preinitializes the raw H2 distribution with the master changelog applied,
        // so we only need to rebuild (to include the custom provider) and start in manual mode.
        // Master is VALID, custom is OUTDATED → triggers the custom-only export path the fix addresses.
        runner.run("build").assertBuild();

        CLIResult startResult = runner.run("start", "--optimized",
                "--spi-connections-jpa-quarkus-migration-strategy=manual",
                "--spi-connections-jpa-quarkus-initialize-empty=false",
                "--http-enabled=true", "--hostname-strict=false");

        startResult.assertMessage("Database not up-to-date, please migrate database with");

        File script = rawDistRootPath.getDistRootPath().resolve("bin").resolve("keycloak-database-update.sql").toFile();
        assertTrue(script.isFile(), "Export file must exist when migration-strategy=manual");

        String output;
        try {
            output = FileUtils.readFileToString(script, Charset.defaultCharset());
        } catch (IOException e) {
            throw new AssertionError("Failed to read keycloak-database-update.sql", e);
        }

        assertThat(output, containsString("MIGRATION_TEST_ENTITY"));
    }
}
