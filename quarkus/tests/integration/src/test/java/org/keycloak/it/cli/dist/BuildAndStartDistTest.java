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
import org.keycloak.it.junit5.extension.KeycloakRunner;
import org.keycloak.it.junit5.extension.RawDistOnly;
import org.keycloak.it.junit5.extension.StopServer;
import org.keycloak.it.junit5.extension.StopServer.Mode;
import org.keycloak.it.junit5.extension.WithEnvVars;
import org.keycloak.it.utils.RawKeycloakDistribution;

import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.keycloak.quarkus.runtime.cli.command.AbstractAutoBuildCommand.OPTIMIZED_BUILD_OPTION_LONG;

import static org.junit.jupiter.api.Assertions.assertTrue;

@DistributionTest
@RawDistOnly(reason = "Containers are immutable")
@TestMethodOrder(OrderAnnotation.class)
@Tag(DistributionTest.WIN)
public class BuildAndStartDistTest {

    @StopServer(Mode.BEFORE_QUARKUS)
    @Test
    void testBuildAndStart(KeycloakRunner runner) {
        RawKeycloakDistribution rawDist = runner.getDistribution(RawKeycloakDistribution.class);
        // start using based on the build options set via CLI
        CLIResult cliResult = runner.run("build", "--db=dev-file");
        cliResult.assertBuild();
        cliResult = runner.run("start", "--http-enabled=true", "--hostname-strict=false", OPTIMIZED_BUILD_OPTION_LONG);
        cliResult.assertNoBuild();
        assertTrue(cliResult.getErrorOutput().isBlank());

        // start using based on the build options set via conf file
        rawDist.setProperty("http-enabled", "true");
        rawDist.setProperty("hostname-strict", "false");
        rawDist.setProperty("http-relative-path", "/auth");
        rawDist.setProperty("db", "dev-file");
        cliResult = runner.run("build");
        cliResult.assertBuild();
        cliResult = runner.run("start", OPTIMIZED_BUILD_OPTION_LONG);
        cliResult.assertNoBuild();
        assertTrue(cliResult.getErrorOutput().isBlank(), cliResult.getErrorOutput());
        // running start without optimized flag should not cause a build
        cliResult = runner.run("start");
        cliResult.assertNoBuild();
        assertTrue(cliResult.getErrorOutput().isBlank());

        // remove the build option from conf file to force a build during start
        rawDist.removeProperty("http-relative-path");
        cliResult = runner.run("start");
        cliResult.assertBuild();
        assertTrue(cliResult.getErrorOutput().isBlank());
    }

    @Test
    @WithEnvVars({"KEYCLOAK_ADMIN", "oldadmin123", "KEYCLOAK_ADMIN_PASSWORD", "oldadmin123"})
    @Launch({"start-dev"})
    void testCreateLegacyAdmin(KeycloakRunner runner, LaunchResult result) {
        assertAdminCreation(runner, result, "oldadmin123", "oldadmin123", "oldadmin123");
    }

    @Test
    @WithEnvVars({"KC_BOOTSTRAP_ADMIN_USERNAME", "admin123", "KC_BOOTSTRAP_ADMIN_PASSWORD", "admin123"})
    @Launch({"start-dev"})
    void testCreateAdmin(KeycloakRunner runner, LaunchResult result) {
        assertAdminCreation(runner, result, "admin123", "admin123", "admin123");
    }

    @Test
    @WithEnvVars({"KC_BOOTSTRAP_ADMIN_USERNAME", "admin123", "KC_BOOTSTRAP_ADMIN_PASSWORD", "admin123"})
    @Launch({"start-dev"})
    void testCreateDifferentAdmin(KeycloakRunner runner, LaunchResult result) {
        assertAdminCreation(runner, result, "admin123", "new-admin", "new-admin");
    }

    private void assertAdminCreation(KeycloakRunner runner, LaunchResult result, String initialUsername, String nextUsername, String password) {
        assertTrue(result.getOutput().contains("Created temporary admin user with username " + initialUsername),
                () -> "The Output:\n" + result.getOutput() + "doesn't contains the expected string.");

        runner.setEnvVar("KC_BOOTSTRAP_ADMIN_USERNAME", nextUsername);
        runner.setEnvVar("KC_BOOTSTRAP_ADMIN_PASSWORD", password);
        CLIResult cliResult = runner.run("start-dev", "--log-level=org.keycloak.services:debug");

        cliResult.assertNoMessage("Added temporary admin user '");
        cliResult.assertStartedDevMode();
    }
}
