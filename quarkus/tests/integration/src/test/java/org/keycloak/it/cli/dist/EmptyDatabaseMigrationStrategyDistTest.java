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

import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.KeycloakRunner;
import org.keycloak.it.junit5.extension.RawDistOnly;
import org.keycloak.it.utils.RawDistRootPath;
import org.keycloak.it.utils.RawKeycloakDistribution;

import io.quarkus.deployment.util.FileUtil;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * An explicit migration strategy must win over the implicit initialization of an empty database.
 * Without an explicit {@code initialize-empty}, {@code manual} has to produce the export script and
 * {@code validate} has to fail startup instead of both being silently overridden by the default
 * {@code initialize-empty=true} and initializing the schema in place.
 */
@DistributionTest
@RawDistOnly(reason = "Containers are immutable")
@Tag(DistributionTest.SLOW)
public class EmptyDatabaseMigrationStrategyDistTest {

    @Test
    void testManualStrategyAloneExportsScriptForEmptyDatabase(KeycloakRunner runner, RawDistRootPath rawDistRootPath) throws IOException {
        forceEmptyDatabase(runner);

        CLIResult result = runner.run("start-dev",
                "--spi-connections-jpa-quarkus-migration-strategy=manual");

        result.assertMessage("Database not initialized, please initialize database with");

        File script = rawDistRootPath.getDistRootPath().resolve("bin").resolve("keycloak-database-update.sql").toFile();
        assertTrue(script.isFile(), "Export file must exist when migration-strategy=manual");
    }

    @Test
    void testValidateStrategyAloneFailsStartupForEmptyDatabase(KeycloakRunner runner) throws IOException {
        forceEmptyDatabase(runner);

        CLIResult result = runner.run("start-dev",
                "--spi-connections-jpa-quarkus-migration-strategy=validate");

        result.assertMessage("Database not initialized, please enable database initialization");
    }

    @Test
    void testExplicitInitializeEmptyStillOverridesManualStrategy(KeycloakRunner runner) throws IOException {
        forceEmptyDatabase(runner);

        CLIResult result = runner.run("start-dev",
                "--spi-connections-jpa-quarkus-migration-strategy=manual",
                "--spi-connections-jpa-quarkus-initialize-empty=true");

        result.assertStartedDevMode();
    }

    private static void forceEmptyDatabase(KeycloakRunner runner) throws IOException {
        RawKeycloakDistribution rawDist = runner.getDistribution(RawKeycloakDistribution.class);
        FileUtil.deleteDirectory(rawDist.getDistPath().resolve("data").resolve("h2").toAbsolutePath());
    }
}
