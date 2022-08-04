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

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.keycloak.quarkus.runtime.cli.command.AbstractStartCommand.OPTIMIZED_BUILD_OPTION_LONG;

import org.junit.jupiter.api.Test;
import org.keycloak.it.cli.StartCommandTest;
import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.DistributionTest;

import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import org.keycloak.it.utils.KeycloakDistribution;

@DistributionTest
public class StartCommandDistTest extends StartCommandTest {

    @Test
    @Launch({ "--profile=dev", "start", "--http-enabled=true", "--hostname-strict=false" })
    void failIfAutoBuildUsingDevProfile(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        assertThat(cliResult.getErrorOutput(), containsString("You can not 'start' the server in development mode. Please re-build the server first, using 'kc.sh build' for the default production mode."));
        assertEquals(4, cliResult.getErrorStream().size());
    }

    @Test
    @Launch({ "start", "--http-enabled=true" })
    void failNoHostnameNotSet(LaunchResult result) {
        assertTrue(result.getErrorOutput().contains("ERROR: Strict hostname resolution configured but no hostname was set"),
                () -> "The Output:\n" + result.getOutput() + "doesn't contains the expected string.");
    }

    @Test
    @Launch({ "start", "--http-enabled=true", "--hostname-strict=false", "--cache=local" })
    void testStartUsingAutoBuild(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertMessage("Changes detected in configuration. Updating the server image.");
        cliResult.assertMessage("Updating the configuration and installing your custom providers, if any. Please wait.");
        cliResult.assertMessage("Server configuration updated and persisted. Run the following command to review the configuration:");
        cliResult.assertMessage(KeycloakDistribution.SCRIPT_CMD + " show-config");
        cliResult.assertMessage("Next time you run the server, just run:");
        cliResult.assertMessage(KeycloakDistribution.SCRIPT_CMD + " start " + OPTIMIZED_BUILD_OPTION_LONG + " --http-enabled=true --hostname-strict=false");
        assertFalse(cliResult.getOutput().contains("--cache"));
        cliResult.assertStarted();
    }

    @Test
    @Launch({ "start", "--optimized", "--http-enabled=true", "--hostname-strict=false", "--cache=local" })
    void testStartUsingOptimizedDoesNotAllowBuildOptions(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertError("Unknown option: '--cache'");
    }

}
