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

import java.util.concurrent.TimeUnit;

import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.KeycloakRunner;
import org.keycloak.it.junit5.extension.RawDistOnly;
import org.keycloak.it.junit5.extension.StopServer;
import org.keycloak.it.junit5.extension.StopServer.Mode;
import org.keycloak.it.junit5.extension.TestProvider;
import org.keycloak.it.junit5.extension.WithEnvVars;
import org.keycloak.it.resource.realm.TestRealmResourceTestProvider;
import org.keycloak.it.utils.KeycloakDistribution;
import org.keycloak.it.utils.RawDistributionLifecycleManager;
import org.keycloak.it.utils.RawKeycloakDistribution;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.main.Launch;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.keycloak.quarkus.runtime.cli.command.Main.CONFIG_FILE_LONG_NAME;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DistributionTest
@Tag(DistributionTest.WIN)
@QuarkusTestResource(RawDistributionLifecycleManager.class)
public class StartCommandDistTest {

    @StopServer(Mode.BEFORE_QUARKUS)
    @Test
    @Launch({ "start", "--db=dev-file", "--hostname-strict=false" })
    void failNoTls(CLIResult cliResult) {
        assertTrue(cliResult.getErrorOutput().contains("Key material not provided to setup HTTPS"),
                () -> "The Output:\n" + cliResult.getErrorOutput() + "doesn't contains the expected string.");
    }

    @StopServer(Mode.BEFORE_QUARKUS)
    @Test
    @Launch({ "start", "--db=dev-file", "--spi-events-listener-jboss-logging-success-level" })
    void failSpiArgMissingValue(CLIResult cliResult) {
        assertTrue(cliResult.getErrorOutput().contains("spi argument --spi-events-listener-jboss-logging-success-level requires a value"),
                () -> "The Output:\n" + cliResult.getErrorOutput() + "doesn't contains the expected string.");
    }

    @StopServer(Mode.BEFORE_QUARKUS)
    @Test
    @Launch({ "build", "--db=dev-file", "--spi-events-listener-jboss-logging-success-level=debug" })
    void warnSpiRuntimeAtBuildtime(CLIResult cliResult) {
        assertTrue(cliResult.getOutput().contains("The following run time options were found, but will be ignored during build time: kc.spi-events-listener-jboss-logging-success-level"),
                () -> "The Output:\n" + cliResult.getOutput() + "doesn't contains the expected string.");
    }

    @StopServer(Mode.BEFORE_QUARKUS)
    @Test
    @RawDistOnly(reason = "Containers are immutable")
    void errorSpiBuildtimeAtRuntime(KeycloakRunner runner) {
        CLIResult cliResult = runner.run("build",  "--db=dev-file");
        cliResult.assertBuild();

        cliResult = runner.run("start", "--optimized", "--http-enabled=true", "--hostname-strict=false", "--spi-events-listener--jboss-logging--enabled=false");
        cliResult.assertError("The following build time options have values that differ from what is persisted - the new values will NOT be used until another build is run: kc.spi-events-listener--jboss-logging--enabled");
    }

    @StopServer(Mode.BEFORE_QUARKUS)
    @WithEnvVars({"KC_SPI_EVENTS_LISTENER__JBOSS_LOGGING__ENABLED", "false"})
    @Test
    @RawDistOnly(reason = "Containers are immutable")
    void noErrorSpiBuildtimeNotChanged(KeycloakRunner runner) {
        CLIResult cliResult = runner.run("build", "--db=dev-file");
        cliResult.assertBuild();

        cliResult = runner.run("start", "--optimized", "--http-enabled=true", "--hostname-strict=false");
        cliResult.assertNoError("The following build time options");
    }

    @Test
    @RawDistOnly(reason = "Containers are immutable")
    void terminateStartOptimized(KeycloakRunner runner) {
        CLIResult cliResult = runner.run("build", "--db=dev-file");
        cliResult.assertBuild();

        runner.setStopServer(Mode.MANUAL);
        cliResult = runner.run("start", "--optimized", "--http-enabled=true", "--hostname-strict=false");
        cliResult.assertStarted();

        // if the child java process does not clean up, then subsequent start will fail
        runner.stop();

        cliResult = runner.run("start", "--optimized", "--http-enabled=true", "--hostname-strict=false");
        cliResult.assertStarted();
    }

    @StopServer(Mode.BEFORE_QUARKUS)
    @Test
    @Launch({ "--profile=dev", "start",  "--db=dev-file" })
    void failUsingDevProfile(CLIResult cliResult) {
        assertTrue(cliResult.getErrorOutput().contains("You can not 'start' the server in development mode. Please re-build the server first, using '" + KeycloakDistribution.SCRIPT_CMD + " build' for the default production mode."),
                () -> "The Output:\n" + cliResult.getErrorOutput() + "doesn't contains the expected string.");
    }

    @Test
    @Launch({ "-v", "start", "--db=dev-file", "--http-enabled=true", "--hostname-strict=false" })
    void testHttpEnabled(CLIResult cliResult) {
        cliResult.assertStarted();
    }

    @StopServer(Mode.BEFORE_QUARKUS)
    @Test
    @Launch({ "--profile=dev", "start", "--http-enabled=true", "--hostname-strict=false" })
    void failIfAutoBuildUsingDevProfile(CLIResult cliResult) {
        assertThat(cliResult.getErrorOutput(), containsString("You can not 'start' the server in development mode. Please re-build the server first, using '" + KeycloakDistribution.SCRIPT_CMD + " build' for the default production mode."));
        assertEquals(4, cliResult.getErrorStream().size());
    }

    @StopServer(Mode.BEFORE_QUARKUS)
    @WithEnvVars({"KC_HTTP_ENABLED", "true", "KC_HOSTNAME_STRICT", "false"})
    @Test
    @Launch({ "start", "--optimized" })
    @Order(1)
    void failIfOptimizedUsedForFirstFastStartup(CLIResult cliResult) {
        cliResult.assertError("The '--optimized' flag was used for first ever server start.");
    }

    @StopServer(Mode.BEFORE_QUARKUS)
    @Test
    @Launch({ "start", "--optimized", "--http-enabled=true", "--hostname-strict=false" })
    @Order(2)
    void failIfOptimizedUsedForFirstStartup(CLIResult cliResult) {
        cliResult.assertError("The '--optimized' flag was used for first ever server start.");
    }

    @Test
    @Launch({ "start", "--db=dev-file", "--http-enabled=true" })
    void failNoHostnameNotSet(CLIResult cliResult) {
        assertTrue(cliResult.getErrorOutput().contains("ERROR: hostname is not configured; either configure hostname, or set hostname-strict to false"),
                () -> "The Output:\n" + cliResult.getOutput() + "doesn't contains the expected string.");
    }

    @StopServer(Mode.BEFORE_QUARKUS)
    @Test
    @Launch({ "start", "--db=dev-file", "--http-enabled=true", "--hostname-strict=false", "--metrics-enabled=true" })
    void testStartUsingAutoBuild(CLIResult cliResult) {
        cliResult.assertNoMessage("ignored during build");
        cliResult.assertMessage("Changes detected in configuration. Updating the server image.");
        cliResult.assertMessage("Updating the configuration and installing your custom providers, if any. Please wait.");
        cliResult.assertMessage("Server configuration updated and persisted. Run the following command to review the configuration:");
        cliResult.assertMessage(KeycloakDistribution.SCRIPT_CMD + " show-config");
        cliResult.assertMessage("Next time you run the server, just add --optimized to the command to ensure this build is used.");
        assertFalse(cliResult.getOutput().contains("--metrics-enabled"));
        assertTrue(cliResult.getErrorOutput().isBlank(), cliResult.getErrorOutput());
    }

    @StopServer(Mode.BEFORE_QUARKUS)
    @Test
    @Launch({ "start", "--db=dev-file", "--http-enabled=true", "--cache-remote-host=localhost", "--hostname-strict=false", "--cache-remote-tls-enabled=false", "--transaction-xa-enabled=true" })
    void testStartNoWarningOnDisabledRuntimeOption(CLIResult cliResult) {
        cliResult.assertNoMessage("cache-remote-tls-enabled: Available only when remote host is set");
    }

    @StopServer(Mode.BEFORE_QUARKUS)
    @Test
    @WithEnvVars({"KC_LOG", "invalid"})
    @Launch({ "start", "--db=dev-file", "--http-enabled=false", "--hostname-strict=false" })
    void testStartUsingOptimizedInvalidEnvOption(CLIResult cliResult) {
        cliResult.assertError("Invalid value for option 'KC_LOG': invalid. Expected values are: console, file, syslog");
    }

    @StopServer(Mode.BEFORE_QUARKUS)
    @Test
    @RawDistOnly(reason = "Containers are immutable")
    void testWarningWhenOverridingBuildOptionsDuringStart(KeycloakRunner runner) {
        CLIResult cliResult = runner.run("build", "--db=postgres", "--features=preview");
        cliResult.assertBuild();
        cliResult = runner.run("start", "--db=dev-file", "--hostname=localhost", "--http-enabled=true");
        cliResult.assertMessage("The previous optimized build will be overridden with the following build options:");
        cliResult.assertMessage("- db=postgres > db=dev-file"); // back to the default value
        cliResult.assertMessage("- features=preview > features=<unset>"); // no default value, the <unset> is shown
        cliResult.assertMessage("To avoid that, run the 'build' command again and then start the optimized server instance using the '--optimized' flag.");
        assertTrue(cliResult.getErrorOutput().isBlank());
        // should not show warning if the re-augmentation did not happen through the build command
        // an optimized server image should ideally be created by running a build
        cliResult = runner.run("start", "--db=dev-mem", "--hostname=localhost", "--http-enabled=true");
        cliResult.assertNoMessage("The previous optimized build will be overridden with the following build options:");
        assertTrue(cliResult.getErrorOutput().isBlank());
        runner.run("build", "--db=postgres");
        cliResult = runner.run("start", "--db=dev-file", "--hostname=localhost", "--http-enabled=true");
        cliResult.assertMessage("- db=postgres > db=dev-file");
        cliResult.assertNoMessage("- features=preview > features=<unset>");
        assertTrue(cliResult.getErrorOutput().isBlank());
        runner.run("build", "--db=postgres");
        cliResult = runner.run("start", "--db=dev-mem", "--hostname=localhost", "--http-enabled=true");
        cliResult.assertMessage("- db=postgres > db=dev-mem"); // option overridden during the start
        assertTrue(cliResult.getErrorOutput().isBlank());
        runner.run("build", "--db=dev-mem");
        cliResult = runner.run("start", "--db=dev-mem", "--hostname=localhost", "--http-enabled=true");
        cliResult.assertNoMessage("- db=postgres > db=postgres"); // option did not change not need to show
        assertTrue(cliResult.getErrorOutput().isBlank());
        runner.run("build", "--db=dev-mem");
        cliResult = runner.run("start", "--db=dev-mem", "--cache=local", "--hostname=localhost", "--http-enabled=true");
        cliResult.assertNoMessage("The previous optimized build will be overridden with the following build options:"); // no message, same values provided during auto-build
    }

    @StopServer(Mode.BEFORE_QUARKUS)
    @Test
    @RawDistOnly(reason = "Containers are immutable")
    void testStartAfterStartDev(KeycloakRunner runner) {
        CLIResult cliResult = runner.run("start-dev");
        cliResult.assertStartedDevMode();

        cliResult = runner.run("start", "--db=dev-file", "--http-enabled", "true", "--hostname-strict", "false");
        cliResult.assertNotDevMode();
        assertTrue(cliResult.getErrorOutput().isBlank());
    }

    @StopServer(Mode.BEFORE_QUARKUS)
    @Test
    @RawDistOnly(reason = "Containers are immutable")
    void testErrorWhenOverridingNonCliBuildOptionsDuringStart(KeycloakRunner runner) {
        CLIResult cliResult = runner.run("build", "--db=dev-file", "--features=preview");
        cliResult.assertBuild();
        runner.setEnvVar("KC_DB", "postgres");
        cliResult = runner.run("start", "--optimized", "--hostname=localhost", "--http-enabled=true");
        cliResult.assertError("The following build time options have values that differ from what is persisted - the new values will NOT be used until another build is run: kc.db");
    }

    @StopServer(Mode.BEFORE_QUARKUS)
    @Test
    @Launch({CONFIG_FILE_LONG_NAME + "=src/test/resources/non-existing.conf", "start", "--db=dev-file"})
    void testInvalidConfigFileOption(CLIResult cliResult) {
        cliResult.assertError("File specified via '--config-file' or '-cf' option does not exist.");
        cliResult.assertError(String.format("Try '%s --help' for more information on the available options.", KeycloakDistribution.SCRIPT_CMD));
    }

    @StopServer(Mode.BEFORE_QUARKUS)
    @Test
    @Launch({CONFIG_FILE_LONG_NAME + "=src/test/resources/keycloak.properties", "start", "--db=dev-file"})
    void testConfigFileWithWrongExtension(CLIResult cliResult) {
        cliResult.assertError("Configuration file specified via '--config-file' or '-cf' must have the '.conf' extension.");
    }

    @RawDistOnly(reason = "Containers are immutable")
    @Test
    void testRuntimeValuesAreNotCaptured(KeycloakRunner runner) {
        // confirm that the invalid value prevents startup - if this passes, then we need to use a different
        // spi provider
        CLIResult cliResult = runner.run("start", "--db=dev-file", "--spi-events-listener-jboss-logging-success-level=invalid", "--http-enabled", "true", "--hostname-strict", "false");
        cliResult.assertError("Failed to start quarkus");

        // if there was no auto-build use an explicit build to potentially capture the runtime default
        if (!cliResult.getOutput().contains("Server configuration updated and persisted")) {
            cliResult = runner.run("build", "--db=dev-file", "--spi-events-listener-jboss-logging-success-level=invalid");
            cliResult.assertBuild();
        }

        // the invalid value should not be the default
        cliResult = runner.run("start", "--db=dev-file", "--http-enabled", "true", "--hostname-strict", "false");
        cliResult.assertNoBuild();
        cliResult.assertStarted();
    }
    
    @RawDistOnly(reason = "Containers are immutable")
    @Test
    @TestProvider(TestRealmResourceTestProvider.class)
    void testAsyncBootstrapFails(KeycloakRunner runner) {
        RawKeycloakDistribution rawDist = runner.getDistribution(RawKeycloakDistribution.class);
        runner.setStopServer(Mode.MANUAL);
        CLIResult result = runner.run("start", "--server-async-bootstrap=true", "--hostname-strict=false", "--db=dev-file", "--http-enabled=true", "--spi-realm-restapi-extension--test-resources--fail=true");
        rawDist.waitFor(false, TimeUnit.MINUTES.toMillis(2));
        runner.stop();
        result.assertMessage("Failed to start server");
        result.assertMessage("I've failed");
        assertEquals(1, rawDist.getExitCode());
    }

}
