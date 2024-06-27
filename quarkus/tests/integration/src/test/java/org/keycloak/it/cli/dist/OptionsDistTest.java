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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.RawDistOnly;
import org.keycloak.it.junit5.extension.WithEnvVars;
import org.keycloak.it.utils.KeycloakDistribution;

import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.keycloak.quarkus.runtime.cli.command.Main.CONFIG_FILE_LONG_NAME;

@DistributionTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class OptionsDistTest {

    @Test
    @Order(1)
    @Launch({"build", "--db=invalid"})
    public void failInvalidOptionValue(LaunchResult result) {
        Assertions.assertTrue(result.getErrorOutput().contains("Invalid value for option '--db': invalid. Expected values are: dev-file, dev-mem, mariadb, mssql, mysql, oracle, postgres"));
    }

    @Test
    @Order(2)
    @Launch({"start", "--test=invalid"})
    public void testServerDoesNotStartIfValidationFailDuringReAugStart(LaunchResult result) {
        assertEquals(1, result.getErrorStream().stream().filter(s -> s.contains("Unknown option: '--test'")).count());
    }

    @Test
    @Order(3)
    @Launch({"start", "--log=console", "--log-file-output=json", "--http-enabled=true", "--hostname-strict=false"})
    public void testServerDoesNotStartIfDisabledFileLogOption(LaunchResult result) {
        assertEquals(1, result.getErrorStream().stream().filter(s -> s.contains("Disabled option: '--log-file-output'. Available only when File log handler is activated")).count());
        assertEquals(1, result.getErrorStream().stream().filter(s -> s.contains("Possible solutions: --log, --log-console-output, --log-console-format, --log-console-color")).count());
    }

    @Test
    @Order(4)
    @Launch({"start", "--log=file", "--log-file-output=json", "--http-enabled=true", "--hostname-strict=false"})
    public void testServerStartIfEnabledFileLogOption(LaunchResult result) {
        assertEquals(0, result.getErrorStream().stream().filter(s -> s.contains("Disabled option: '--log-file-output'. Available only when File log handler is activated")).count());
    }

    @Test
    @Order(5)
    @WithEnvVars({"KC_LOG", "console", "KC_LOG_CONSOLE_COLOR", "true", "KC_LOG_FILE", "something-env", "KC_LOG_GELF_VERSION", "1.1", "KC_HTTP_ENABLED", "true", "KC_HOSTNAME_STRICT", "false"})
    @Launch({"start"})
    public void testSettingEnvVars(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;

        cliResult.assertMessage("The following used run time options are UNAVAILABLE and will be ignored during build time:");
        cliResult.assertMessage("- log-file: Available only when File log handler is activated.");
        cliResult.assertMessage("- log-gelf-version: Available only when GELF is activated.");
        cliResult.assertMessage("quarkus.log.console.color");
        cliResult.assertMessage("config property is deprecated and should not be used anymore");
    }

    @Test
    @Order(6)
    @RawDistOnly(reason = "Raw is enough and we avoid issues with including custom conf file in the container")
    public void testExpressionsInConfigFile(KeycloakDistribution distribution) {
        distribution.setEnvVar("MY_LOG_LEVEL", "warn");
        CLIResult result = distribution.run(CONFIG_FILE_LONG_NAME + "=" + Paths.get("src/test/resources/OptionsDistTest/keycloak.conf").toAbsolutePath().normalize(), "start", "--http-enabled=true", "--hostname-strict=false", "--optimized");
        result.assertNoMessage("INFO [io.quarkus]");
        result.assertNoMessage("Listening on:");

        // specified in the OptionsDistTest/keycloak.conf
        result.assertMessage("The following used run time options are UNAVAILABLE and will be ignored during build time:");
        result.assertMessage("- log-gelf-level: Available only when GELF is activated.");
        result.assertMessage("- log-gelf-version: Available only when GELF is activated.");
    }

    @Test
    @Order(7)
    @Launch({"start-dev", "--log=console", "--log-gelf-include-stack-trace=true"})
    public void testDisabledGelfOption(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertError("Disabled option: '--log-gelf-include-stack-trace'. Available only when GELF is activated");
        cliResult.assertError("Possible solutions: --log, --log-console-output, --log-console-format, --log-console-color, --log-level");
        cliResult.assertError("Try '" + KeycloakDistribution.SCRIPT_CMD + " start-dev --help' for more information on the available options.");
        cliResult.assertError("Specify '--help-all' to obtain information on all options and their availability.");
    }

    // Start-dev should be executed as last tests - build is done for development mode

    @Test
    @Order(8)
    @Launch({"start-dev", "--test=invalid"})
    public void testServerDoesNotStartIfValidationFailDuringReAugStartDev(LaunchResult result) {
        assertEquals(1, result.getErrorStream().stream().filter(s -> s.contains("Unknown option: '--test'")).count());
    }

    @Test
    @Order(9)
    @Launch({"start-dev", "--log=console", "--log-file-output=json"})
    public void testServerDoesNotStartDevIfDisabledFileLogOption(LaunchResult result) {
        assertEquals(1, result.getErrorStream().stream().filter(s -> s.contains("Disabled option: '--log-file-output'. Available only when File log handler is activated")).count());
        assertEquals(1, result.getErrorStream().stream().filter(s -> s.contains("Possible solutions: --log, --log-console-output, --log-console-format, --log-console-color")).count());
    }

    @Test
    @Order(10)
    @Launch({"start-dev", "--log=file", "--log-file-output=json", "--log-console-color=true"})
    public void testServerStartDevIfEnabledFileLogOption(LaunchResult result) {
        assertEquals(0, result.getErrorStream().stream().filter(s -> s.contains("Disabled option: '--log-file-output'. Available only when File log handler is activated")).count());
        assertEquals(1, result.getErrorStream().stream().filter(s -> s.contains("Disabled option: '--log-console-color'. Available only when Console log handler is activated")).count());
        assertEquals(1, result.getErrorStream().stream().filter(s -> s.contains("Possible solutions: --log, --log-file, --log-file-format, --log-file-output, --log-level")).count());
    }
}
