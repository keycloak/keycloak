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
import static org.keycloak.quarkus.runtime.cli.command.Main.CONFIG_FILE_LONG_NAME;

import org.junit.jupiter.api.Test;
import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.DistributionTest;

import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import org.keycloak.it.junit5.extension.RawDistOnly;
import org.keycloak.it.junit5.extension.WithEnvVars;
import org.keycloak.it.utils.KeycloakDistribution;

@DistributionTest
public class StartCommandDistTest {

    @Test
    @Launch({ "start", "--hostname-strict=false" })
    void failNoTls(LaunchResult result) {
        assertTrue(result.getErrorOutput().contains("Key material not provided to setup HTTPS"),
                () -> "The Output:\n" + result.getErrorOutput() + "doesn't contains the expected string.");
    }

    @Test
    @Launch({ "start", "--spi-events-listener-jboss-logging-success-level" })
    void failSpiArgMissingValue(LaunchResult result) {
        assertTrue(result.getErrorOutput().contains("spi argument --spi-events-listener-jboss-logging-success-level requires a value"),
                () -> "The Output:\n" + result.getErrorOutput() + "doesn't contains the expected string.");
    }

    @Test
    @Launch({ "--profile=dev", "start" })
    void failUsingDevProfile(LaunchResult result) {
        assertTrue(result.getErrorOutput().contains("ERROR: You can not 'start' the server in development mode. Please re-build the server first, using 'kc.sh build' for the default production mode."),
                () -> "The Output:\n" + result.getErrorOutput() + "doesn't contains the expected string.");
    }

    @Test
    @Launch({ "-v", "start", "--http-enabled=true", "--hostname-strict=false" })
    void testHttpEnabled(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertStarted();
    }

    @Test
    @Launch({ "-v", "start", "--db=dev-mem", OPTIMIZED_BUILD_OPTION_LONG})
    void failBuildPropertyNotAvailable(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertError("Build time option: '--db' not usable with pre-built image and --optimized");
    }

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
        assertTrue(result.getErrorOutput().contains("ERROR: hostname is not configured; either configure hostname, or set hostname-strict to false"),
                () -> "The Output:\n" + result.getOutput() + "doesn't contains the expected string.");
    }

    @Test
    @Launch({ "start", "--http-enabled=true", "--hostname-strict=false", "--metrics-enabled=true" })
    void testStartUsingAutoBuild(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertMessage("Changes detected in configuration. Updating the server image.");
        cliResult.assertMessage("Updating the configuration and installing your custom providers, if any. Please wait.");
        cliResult.assertMessage("Server configuration updated and persisted. Run the following command to review the configuration:");
        cliResult.assertMessage(KeycloakDistribution.SCRIPT_CMD + " show-config");
        cliResult.assertMessage("Next time you run the server, just run:");
        cliResult.assertMessage(KeycloakDistribution.SCRIPT_CMD + " start --http-enabled=true --hostname-strict=false " + OPTIMIZED_BUILD_OPTION_LONG);
        assertFalse(cliResult.getOutput().contains("--metrics-enabled"));
        cliResult.assertStarted();
    }

    @Test
    @Launch({ "start", "--optimized", "--http-enabled=true", "--hostname-strict=false", "--db=postgres" })
    void testStartUsingOptimizedDoesNotAllowBuildOptions(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertError("Build time option: '--db' not usable with pre-built image and --optimized");
    }

    @Test
    @Launch({ "start", "--http-enabled=true", "--cache-remote-host=localhost", "--hostname-strict=false", "--cache-remote-tls-enabled=false", "--transaction-xa-enabled=true" })
    void testStartNoWarningOnDisabledRuntimeOption(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertNoMessage("cache-remote-tls-enabled: Available only when remote host is set");
    }

    @Test
    @WithEnvVars({"KC_LOG", "invalid"})
    @Launch({ "start", "--optimized" })
    void testStartUsingOptimizedInvalidEnvOption(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertError("Invalid value for option 'KC_LOG': invalid. Expected values are: console, file, syslog, gelf");
    }

    @Test
    @RawDistOnly(reason = "Containers are immutable")
    void testWarningWhenOverridingBuildOptionsDuringStart(KeycloakDistribution dist) {
        CLIResult cliResult = dist.run("build", "--db=postgres", "--features=preview");
        cliResult.assertBuild();
        cliResult = dist.run("start", "--hostname=localhost", "--http-enabled=true");
        cliResult.assertMessage("The previous optimized build will be overridden with the following build options:");
        cliResult.assertMessage("- db=postgres > db=dev-file"); // back to the default value
        cliResult.assertMessage("- features=preview > features=<unset>"); // no default value, the <unset> is shown
        cliResult.assertMessage("To avoid that, run the 'build' command again and then start the optimized server instance using the '--optimized' flag.");
        cliResult.assertStarted();
        // should not show warning if the re-augmentation did not happen through the build command
        // an optimized server image should ideally be created by running a build
        cliResult = dist.run("start", "--db=dev-mem", "--hostname=localhost", "--http-enabled=true");
        cliResult.assertNoMessage("The previous optimized build will be overridden with the following build options:");
        cliResult.assertStarted();
        dist.run("build", "--db=postgres");
        cliResult = dist.run("start", "--hostname=localhost", "--http-enabled=true");
        cliResult.assertMessage("- db=postgres > db=dev-file");
        cliResult.assertNoMessage("- features=preview > features=<unset>");
        cliResult.assertStarted();
        dist.run("build", "--db=postgres");
        cliResult = dist.run("start", "--db=dev-mem", "--hostname=localhost", "--http-enabled=true");
        cliResult.assertMessage("- db=postgres > db=dev-mem"); // option overridden during the start
        cliResult.assertStarted();
        dist.run("build", "--db=dev-mem");
        cliResult = dist.run("start", "--db=dev-mem", "--hostname=localhost", "--http-enabled=true");
        cliResult.assertNoMessage("- db=postgres > db=postgres"); // option did not change not need to show
        cliResult.assertStarted();
        dist.run("build", "--db=dev-mem");
        cliResult = dist.run("start", "--db=dev-mem", "--cache=local", "--hostname=localhost", "--http-enabled=true");
        cliResult.assertNoMessage("The previous optimized build will be overridden with the following build options:"); // no message, same values provided during auto-build
    }

    @Test
    @RawDistOnly(reason = "Containers are immutable")
    void testWarningWhenOverridingNonCliBuildOptionsDuringStart(KeycloakDistribution dist) {
        CLIResult cliResult = dist.run("build", "--features=preview");
        cliResult.assertBuild();
        dist.setEnvVar("KC_DB", "postgres");
        cliResult = dist.run("start", "--optimized", "--hostname=localhost", "--http-enabled=true");
        cliResult.assertMessage("The following build time non-cli options have values that differ from what is persisted - the new values will NOT be used until another build is run: kc.db");
    }

    @Test
    @Launch({CONFIG_FILE_LONG_NAME + "=src/test/resources/non-existing.conf", "start"})
    void testInvalidConfigFileOption(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertError("File specified via '--config-file' or '-cf' option does not exist.");
        cliResult.assertError(String.format("Try '%s --help' for more information on the available options.", KeycloakDistribution.SCRIPT_CMD));
    }
}
