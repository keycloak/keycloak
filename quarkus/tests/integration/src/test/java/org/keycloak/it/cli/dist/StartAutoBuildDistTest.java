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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.RawDistOnly;

import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;

@DistributionTest(reInstall = DistributionTest.ReInstall.NEVER)
@RawDistOnly(reason = "Containers are immutable")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class StartAutoBuildDistTest {

    @Test
    @Launch({ "start", "--auto-build", "--http-enabled=true", "--hostname-strict=false", "--cache=local" })
    @Order(1)
    void testStartAutoBuild(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertMessage("Changes detected in configuration. Updating the server image.");
        cliResult.assertMessage("Updating the configuration and installing your custom providers, if any. Please wait.");
        cliResult.assertMessage("Server configuration updated and persisted. Run the following command to review the configuration:");
        cliResult.assertMessage("kc.sh show-config");
        cliResult.assertMessage("Next time you run the server, just run:");
        cliResult.assertMessage("kc.sh start --http-enabled=true --hostname-strict=false");
        assertFalse(cliResult.getOutput().contains("--cache"));
        cliResult.assertStarted();
    }

    @Test
    @Launch({ "start", "--auto-build", "--http-enabled=true", "--hostname-strict=false", "--cache=local" })
    @Order(2)
    void testShouldNotReAugIfConfigIsSame(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertNoBuild();
        cliResult.assertStarted();
    }

    @Test
    @Launch({ "start", "--auto-build", "--db=dev-mem", "--http-enabled=true", "--hostname-strict=false", "--cache=local" })
    @Order(3)
    void testShouldReAugIfConfigChanged(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertBuild();
        cliResult.assertStarted();
    }

    @Test
    @Launch({ "start", "--auto-build", "--db=dev-mem", "--http-enabled=true", "--hostname-strict=false", "--cache=local" })
    @Order(4)
    void testShouldNotReAugIfSameDatabase(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertNoBuild();
        cliResult.assertStarted();
    }

    @Test
    @Launch({ "build", "--db=postgres" })
    @Order(5)
    void testBuildForReAugWhenAutoBuild(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertBuild();
    }

    @Test
    @Launch({ "start", "--auto-build", "--http-enabled=true", "--hostname-strict=false", "--cache=local" })
    @Order(6)
    void testReAugWhenNoOptionAfterBuild(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertBuild();
        cliResult.assertStarted();
    }

    @Test
    @Launch({ "start-dev" })
    @Order(7)
    void testStartDevFirstTime(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        assertTrue(cliResult.getOutput().contains("Updating the configuration and installing your custom providers, if any. Please wait."));
        cliResult.assertStartedDevMode();
    }

    @Test
    @Launch({ "start-dev" })
    @Order(8)
    void testShouldNotReAugStartDevIfConfigIsSame(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        assertFalse(cliResult.getOutput().contains("Updating the configuration and installing your custom providers, if any. Please wait."));
        cliResult.assertStartedDevMode();
    }
}
