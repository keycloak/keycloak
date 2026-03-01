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

import java.nio.file.Paths;

import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.DryRun;
import org.keycloak.it.junit5.extension.RawDistOnly;
import org.keycloak.it.junit5.extension.WithEnvVars;
import org.keycloak.it.utils.KeycloakDistribution;

import io.quarkus.test.junit.main.Launch;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.keycloak.quarkus.runtime.cli.command.Main.CONFIG_FILE_LONG_NAME;

@DistributionTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class OptionsDistTest {

    @DryRun
    @Test
    @Order(1)
    @Launch({"build", "--db=invalid"})
    public void failInvalidOptionValue(CLIResult result) {
        result.assertError("Invalid value for option '--db': invalid. Expected values are: dev-file, dev-mem, mariadb, mssql, mysql, oracle, postgres");
    }

    @DryRun
    @Test
    @Order(2)
    @Launch({"start", "--db=dev-file", "--test=invalid"})
    public void testServerDoesNotStartIfValidationFailDuringReAugStart(CLIResult result) {
        result.assertError("Unknown option: '--test'");
    }

    @DryRun
    @Test
    @Order(3)
    @Launch({"start", "--db=dev-file", "--log=console", "--log-file-output=json", "--http-enabled=true", "--hostname-strict=false"})
    public void testServerDoesNotStartIfDisabledFileLogOption(CLIResult result) {
        result.assertError("Disabled option: '--log-file-output'. Available only when File log handler is activated");
        result.assertError("--log, --log-async, --log-console-output, --log-console-level, --log-console-format, --log-console-color, --log-console-async, --log-level, --log-level-<category>");
    }

    @DryRun
    @Test
    @Order(4)
    @Launch({"start", "--db=dev-file", "--log=file", "--log-file-output=json", "--http-enabled=true", "--hostname-strict=false"})
    public void testServerStartIfEnabledFileLogOption(CLIResult result) {
        result.assertNoError("Disabled option: '--log-file-output'. Available only when File log handler is activated");
    }

    @Test
    @Order(5)
    @WithEnvVars({"KC_SPI_CONNECTIONS_HTTP_CLIENT__DEFAULT__EXPECT_CONTINUE_ENABLED", "true", "KC_LOG", "console", "KC_LOG_FILE", "something-env", "KC_HTTP_ENABLED", "true", "KC_HOSTNAME_STRICT", "false"})
    @Launch({"start", "--db=dev-file"})
    public void testSettingEnvVars(CLIResult cliResult) {
        cliResult.assertMessage("The following used run time options are UNAVAILABLE and will be ignored during build time:");
        cliResult.assertMessage("- log-file: Available only when File log handler is activated.");
        cliResult.assertNoMessage("kc.spi-connections-http-client"); // no info/warning expected
        cliResult.assertStarted();
    }

    @DryRun
    @Test
    @Order(6)
    @RawDistOnly(reason = "Raw is enough and we avoid issues with including custom conf file in the container")
    public void testExpressionsInConfigFile(KeycloakDistribution distribution) {
        distribution.setEnvVar("MY_LOG_LEVEL", "warn");
        CLIResult result = distribution.run(CONFIG_FILE_LONG_NAME + "=" + Paths.get("src/test/resources/OptionsDistTest/keycloak.conf").toAbsolutePath().normalize(), "start", "--db=dev-file", "--http-enabled=true", "--hostname-strict=false");
        result.assertNoMessage("INFO [io.quarkus]");
        result.assertNoMessage("Listening on:");

        // specified in the OptionsDistTest/keycloak.conf
        result.assertMessage("The following used run time options are UNAVAILABLE and will be ignored during build time:");
        result.assertMessage("- log-syslog-protocol: Available only when Syslog is activated.");
        result.assertMessage("- log-syslog-app-name: Available only when Syslog is activated.");
    }

    // Start-dev should be executed as last tests - build is done for development mode

    @DryRun
    @Test
    @Order(7)
    @Launch({"start-dev", "--test=invalid"})
    public void testServerDoesNotStartIfValidationFailDuringReAugStartDev(CLIResult result) {
        result.assertError("Unknown option: '--test'");
    }

    @DryRun
    @Test
    @Order(8)
    @Launch({"start-dev", "--log=console", "--log-file-output=json"})
    public void testServerDoesNotStartDevIfDisabledFileLogOption(CLIResult result) {
        result.assertError("Disabled option: '--log-file-output'. Available only when File log handler is activated");
        result.assertError("Possible solutions: --log, --log-async, --log-console-output, --log-console-level, --log-console-format, --log-console-color, --log-console-async, --log-level, --log-level-<category>");
    }

    @DryRun
    @Test
    @Order(9)
    @Launch({"start-dev", "--log=file", "--log-file-output=json", "--log-console-color=true"})
    public void testServerStartDevIfEnabledFileLogOption(CLIResult result) {
        result.assertNoError("Disabled option: '--log-file-output'. Available only when File log handler is activated");
        result.assertError("Disabled option: '--log-console-color'. Available only when Console log handler is activated");
        result.assertError("Possible solutions: --log, --log-async, --log-file, --log-file-level, --log-file-format, --log-file-json-format, --log-file-output, --log-file-async, --log-level, --log-level-<category>");
    }

    @DryRun
    @Test
    @Order(10)
    @Launch({"start-dev", "--cache-remote-host=localhost"})
    public void testCacheRemoteHostWithoutMultiSite(CLIResult result) {
        result.assertError( "cache-remote-host available only when feature 'multi-site' or 'clusterless' is set");
    }

    @DryRun
    @Test
    @Order(11)
    @Launch({"start-dev", "--cache-remote-port=11222"})
    public void testCacheRemotePortWithoutCacheRemoteHost(CLIResult result) {
        assertDisabledDueToMissingRemoteHost(result, "--cache-remote-port");
    }

    @DryRun
    @Test
    @Order(12)
    @Launch({"start-dev", "--cache-remote-username=user"})
    public void testCacheRemoteUsernameWithoutCacheRemoteHost(CLIResult result) {
        assertDisabledDueToMissingRemoteHost(result, "--cache-remote-username");
    }

    @DryRun
    @Test
    @Order(13)
    @Launch({"start-dev", "--cache-remote-password=pass"})
    public void testCacheRemotePasswordWithoutCacheRemoteHost(CLIResult result) {
        assertDisabledDueToMissingRemoteHost(result, "--cache-remote-password");
    }

    @DryRun
    @Test
    @Order(14)
    @Launch({"start-dev", "--cache-remote-tls-enabled=false"})
    public void testCacheRemoteTlsEnabledWithoutCacheRemoteHost(CLIResult result) {
        assertDisabledDueToMissingRemoteHost(result, "--cache-remote-tls-enabled");
    }

    @DryRun
    @Test
    @Order(15)
    @Launch({"start-dev", "--features=multi-site"})
    public void testMultiSiteWithoutCacheRemoteHost(CLIResult result) {
        result.assertError("- cache-remote-host: Required when feature 'multi-site' or 'clusterless' is set.");
    }

    @DryRun
    @Test
    @Order(16)
    @Launch({"start-dev", "--features=multi-site", "--cache-remote-host=localhost", "--cache-remote-username=user"})
    public void testCacheRemoteUsernameWithoutCacheRemotePassword(CLIResult result) {
        result.assertError("The option 'cache-remote-password' is required when 'cache-remote-username' is set.");
    }

    @DryRun
    @Test
    @Order(17)
    @Launch({"start-dev", "--features=multi-site", "--cache-remote-host=localhost", "--cache-remote-password=secret"})
    public void testCacheRemotePasswordWithoutCacheRemoteUsername(CLIResult result) {
        result.assertError("The option 'cache-remote-username' is required when 'cache-remote-password' is set.");
    }

    private static void assertDisabledDueToMissingRemoteHost(CLIResult result, String option) {
        result.assertError("Disabled option: '%s'. Available only when remote host is set".formatted(option));
    }
}
