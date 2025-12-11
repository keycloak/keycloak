/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.DryRun;
import org.keycloak.it.junit5.extension.RawDistOnly;
import org.keycloak.it.junit5.extension.TestProvider;
import org.keycloak.it.utils.KeycloakDistribution;

import com.acme.provider.legacy.jpa.user.CustomUserProvider;
import io.quarkus.test.junit.main.Launch;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.keycloak.quarkus.runtime.cli.command.AbstractAutoBuildCommand.OPTIMIZED_BUILD_OPTION_LONG;

import static org.junit.jupiter.api.Assertions.assertTrue;

@DistributionTest
@RawDistOnly(reason = "Containers are immutable")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class StartAutoBuildDistTest {

    @DryRun
    @Test
    @Launch({ "--verbose", "start", "--db=dev-file", "--http-enabled=true", "--hostname-strict=false" })
    @Order(1)
    void testStartAutoBuild(CLIResult cliResult) {
        cliResult.assertMessage("Changes detected in configuration. Updating the server image.");
        cliResult.assertMessage("Updating the configuration and installing your custom providers, if any. Please wait.");
        cliResult.assertMessage("Server configuration updated and persisted. Run the following command to review the configuration:");
        cliResult.assertMessage(KeycloakDistribution.SCRIPT_CMD + " show-config");
        cliResult.assertMessage("Next time you run the server, just run:");
        cliResult.assertMessage(KeycloakDistribution.SCRIPT_CMD + " --verbose start --http-enabled=true --hostname-strict=false " + OPTIMIZED_BUILD_OPTION_LONG);
        cliResult.assertNoMessage("--cache");
        assertTrue(cliResult.getErrorOutput().isBlank());
    }

    @DryRun
    @Test
    @Launch({ "start", "--db=dev-file", "--http-enabled=true", "--hostname-strict=false" })
    @Order(2)
    void testShouldNotReAugIfConfigIsSame(CLIResult cliResult) {
        cliResult.assertNoBuild();
        assertTrue(cliResult.getErrorOutput().isBlank());
    }

    @DryRun
    @Test
    @Launch({ "start", "--db=dev-mem", "--http-enabled=true", "--hostname-strict=false" })
    @Order(3)
    void testShouldReAugIfConfigChanged(CLIResult cliResult) {
        cliResult.assertBuild();
        assertTrue(cliResult.getErrorOutput().isBlank());
    }

    @DryRun
    @Test
    @Launch({ "start", "--db=dev-mem", "--http-enabled=true", "--hostname-strict=false" })
    @Order(4)
    void testShouldNotReAugIfSameDatabase(CLIResult cliResult) {
        cliResult.assertNoBuild();
        assertTrue(cliResult.getErrorOutput().isBlank());
    }

    @DryRun
    @Test
    @Launch({ "build", "--db=postgres" })
    @Order(5)
    void testBuildForReAugWhenAutoBuild(CLIResult cliResult) {
        cliResult.assertBuild();
    }

    @DryRun
    @Test
    @Launch({ "start", "--db=dev-file", "--http-enabled=true", "--hostname-strict=false" })
    @Order(6)
    void testReAugWhenNoOptionAfterBuild(CLIResult cliResult) {
        cliResult.assertBuild();
        assertTrue(cliResult.getErrorOutput().isBlank());
    }

    @DryRun
    @Test
    @Launch({ "start", "--db=postgres", "--http-enabled=true", "--hostname-strict=false" })
    @Order(7)
    void testShouldReAugWithoutAutoBuildOptionAfterDatabaseChange(CLIResult cliResult) {
        cliResult.assertBuild();
    }

    @DryRun
    @Test
    @Launch({ "start", "--db=dev-file", "--http-enabled=true", "--hostname-strict=false", OPTIMIZED_BUILD_OPTION_LONG})
    @Order(8)
    void testShouldReAugAndNeedsAutoBuildOptionBecauseHasNoAutoBuildOption(CLIResult cliResult) {
        cliResult.assertNoBuild();
    }

    @DryRun
    @Test
    @Launch({ "start-dev" })
    @Order(8)
    void testStartDevFirstTime(CLIResult cliResult) {
        cliResult.assertMessage("Updating the configuration and installing your custom providers, if any. Please wait.");
        cliResult.assertStartedDevMode();
    }

    @DryRun
    @Test
    @Launch({ "start-dev" })
    @Order(9)
    void testShouldNotReAugStartDevIfConfigIsSame(CLIResult cliResult) {
        cliResult.assertNoMessage("Updating the configuration and installing your custom providers, if any. Please wait.");
        cliResult.assertNoBuild();
        cliResult.assertStartedDevMode();
    }

    @DryRun
    @Test
    @TestProvider(CustomUserProvider.class)
    @Order(10)
    void testSpiAutoBuild(KeycloakDistribution dist) {
        CLIResult cliResult = dist.run("start-dev", "--spi-user-provider=custom_jpa", "--spi-user-jpa-enabled=false");
        cliResult.assertMessage("Updating the configuration");
        cliResult.assertStartedDevMode();
        dist.stop();

        // we should persist the spi provider and know not to rebuild
        cliResult = dist.run("start-dev", "--spi-user-provider=custom_jpa", "--spi-user-jpa-enabled=false");
        cliResult.assertNoMessage("Updating the configuration");
        cliResult.assertStartedDevMode();
    }

    @Test
    @Order(11)
    void testLogLevelNotPeristed(KeycloakDistribution dist) {
        CLIResult cliResult = dist.run("start", "--db=dev-file", "--log-level=org.hibernate.SQL:debug", "--http-enabled=true", "--hostname-strict=false");
        cliResult.assertMessage("DEBUG [org.hibernate.SQL]");
        cliResult.assertStarted();
        dist.stop();

        // logging runtime defaults should not be used
        cliResult = dist.run("start", "--db=dev-file", "--http-enabled=true", "--hostname-strict=false");
        cliResult.assertNoMessage("DEBUG [org.hibernate.SQL]");
        cliResult.assertStarted();
    }

    @Test
    @Order(12)
    void testLogLevelWildcardNotPeristed(KeycloakDistribution dist) {
        CLIResult cliResult = dist.run("start-dev", "--log-level-org.hibernate.SQL=debug");
        cliResult.assertMessage("DEBUG [org.hibernate.SQL]");
        cliResult.assertStartedDevMode();
        dist.stop();

        // logging runtime defaults should not be used
        cliResult = dist.run("start-dev");
        cliResult.assertNoMessage("DEBUG [org.hibernate.SQL]");
        cliResult.assertStartedDevMode();
    }

}
