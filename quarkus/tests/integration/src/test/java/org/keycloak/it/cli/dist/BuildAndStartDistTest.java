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

import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.RawDistOnly;
import org.keycloak.it.junit5.extension.WithEnvVars;
import org.keycloak.it.utils.KeycloakDistribution;
import org.keycloak.it.utils.RawKeycloakDistribution;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.keycloak.quarkus.runtime.cli.command.AbstractStartCommand.OPTIMIZED_BUILD_OPTION_LONG;

@DistributionTest
@RawDistOnly(reason = "Containers are immutable")
@TestMethodOrder(OrderAnnotation.class)
public class BuildAndStartDistTest {

    @Test
    void testBuildAndStart(KeycloakDistribution dist) {
        RawKeycloakDistribution rawDist = dist.unwrap(RawKeycloakDistribution.class);
        // start using based on the build options set via CLI
        CLIResult cliResult = rawDist.run("build");
        cliResult.assertBuild();
        cliResult = rawDist.run("start", "--http-enabled=true", "--hostname-strict=false", OPTIMIZED_BUILD_OPTION_LONG);
        cliResult.assertNoBuild();
        cliResult.assertStarted();

        // start using based on the build options set via conf file
        rawDist.setProperty("http-enabled", "true");
        rawDist.setProperty("hostname-strict", "false");
        rawDist.setProperty("http-relative-path", "/auth");
        cliResult = rawDist.run("build");
        cliResult.assertBuild();
        cliResult = rawDist.run("start", OPTIMIZED_BUILD_OPTION_LONG);
        cliResult.assertNoBuild();
        cliResult.assertStarted();
        // running start without optimized flag should not cause a build
        cliResult = rawDist.run("start");
        cliResult.assertNoBuild();
        cliResult.assertStarted();

        // remove the build option from conf file to force a build during start
        rawDist.removeProperty("http-relative-path");
        cliResult = rawDist.run("start");
        cliResult.assertBuild();
        cliResult.assertStarted();
    }

    @Test
    @WithEnvVars({"KEYCLOAK_ADMIN", "admin123", "KEYCLOAK_ADMIN_PASSWORD", "admin123"})
    @Launch({"start-dev"})
    void testCreateAdmin(KeycloakDistribution dist, LaunchResult result) {
        assertAdminCreation(dist, result, "admin123", "admin123", "admin123");
    }

    @Test
    @WithEnvVars({"KEYCLOAK_ADMIN", "admin123", "KEYCLOAK_ADMIN_PASSWORD", "admin123"})
    @Launch({"start-dev"})
    void testCreateDifferentAdmin(KeycloakDistribution dist, LaunchResult result) {
        assertAdminCreation(dist, result, "admin123", "new-admin", "new-admin");
    }

    private void assertAdminCreation(KeycloakDistribution dist, LaunchResult result, String initialUsername, String nextUsername, String password) {
        assertTrue(result.getOutput().contains("Added user '" + initialUsername + "' to realm 'master'"),
                () -> "The Output:\n" + result.getOutput() + "doesn't contains the expected string.");

        dist.setEnvVar("KEYCLOAK_ADMIN", nextUsername);
        dist.setEnvVar("KEYCLOAK_ADMIN_PASSWORD", password);
        CLIResult cliResult = dist.run("start-dev", "--log-level=debug");

        cliResult.assertMessage("Skipping create admin user. Admin already exists in realm 'master'.");
        cliResult.assertStartedDevMode();
    }
}
