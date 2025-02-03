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

import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.DryRun;
import org.keycloak.it.junit5.extension.RawDistOnly;
import org.keycloak.it.junit5.extension.WithEnvVars;
import org.keycloak.it.utils.KeycloakDistribution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.keycloak.quarkus.runtime.cli.command.Main.CONFIG_FILE_LONG_NAME;

@DistributionTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class OptionsDistTest {

    @DryRun
    @Test
    @Order(1)
    @Launch({"build", "--db=invalid"})
    public void failInvalidOptionValue(LaunchResult result) {
        Assertions.assertTrue(result.getErrorOutput().contains("Invalid value for option '--db': invalid. Expected values are: dev-file, dev-mem, mariadb, mssql, mysql, oracle, postgres"));
    }

    @DryRun
    @Test
    @Order(2)
    @Launch({"start", "--db=dev-file", "--test=invalid"})
    public void testServerDoesNotStartIfValidationFailDuringReAugStart(LaunchResult result) {
        assertEquals(1, result.getErrorStream().stream().filter(s -> s.contains("Unknown option: '--test'")).count());
    }

    @DryRun
    @Test
    @Order(3)
    @Launch({"start", "--db=dev-file", "--log=console", "--log-file-output=json", "--http-enabled=true", "--hostname-strict=false"})
    public void testServerDoesNotStartIfDisabledFileLogOption(LaunchResult result) {
        assertEquals(1, result.getErrorStream().stream().filter(s -> s.contains("Disabled option: '--log-file-output'. Available only when File log handler is activated")).count());
        assertEquals(1, result.getErrorStream().stream().filter(s -> s.contains("Possible solutions: --log, --log-console-output, --log-console-level, --log-console-format, --log-console-color, --log-level")).count());
    }

    @DryRun
    @Test
    @Order(4)
    @Launch({"start", "--db=dev-file", "--log=file", "--log-file-output=json", "--http-enabled=true", "--hostname-strict=false"})
    public void testServerStartIfEnabledFileLogOption(LaunchResult result) {
        assertEquals(0, result.getErrorStream().stream().filter(s -> s.contains("Disabled option: '--log-file-output'. Available only when File log handler is activated")).count());
    }

    @Test
    @Order(5)
    @WithEnvVars({"KC_LOG", "console", "KC_LOG_FILE", "something-env", "KC_HTTP_ENABLED", "true", "KC_HOSTNAME_STRICT", "false"})
    @Launch({"start", "--db=dev-file"})
    public void testSettingEnvVars(CLIResult cliResult) {
        cliResult.assertMessage("The following used run time options are UNAVAILABLE and will be ignored during build time:");
        cliResult.assertMessage("- log-file: Available only when File log handler is activated.");
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

    @Test
    @Order(7)
    @Launch({"start", "--db=dev-file", "--cache-embedded-mtls-enabled=true", "--http-enabled=true", "--hostname-strict=false"})
    public void testCacheEmbeddedMtlsEnabled(LaunchResult result) {
        assertTrue(result.getOutputStream().stream().anyMatch(s -> s.contains("Property cache-embedded-mtls-key-store-file required but not specified")));
    }

    // Start-dev should be executed as last tests - build is done for development mode

    @DryRun
    @Test
    @Order(8)
    @Launch({"start-dev", "--test=invalid"})
    public void testServerDoesNotStartIfValidationFailDuringReAugStartDev(LaunchResult result) {
        assertEquals(1, result.getErrorStream().stream().filter(s -> s.contains("Unknown option: '--test'")).count());
    }

    @DryRun
    @Test
    @Order(9)
    @Launch({"start-dev", "--log=console", "--log-file-output=json"})
    public void testServerDoesNotStartDevIfDisabledFileLogOption(LaunchResult result) {
        assertEquals(1, result.getErrorStream().stream().filter(s -> s.contains("Disabled option: '--log-file-output'. Available only when File log handler is activated")).count());
        assertEquals(1, result.getErrorStream().stream().filter(s -> s.contains("Possible solutions: --log, --log-console-output, --log-console-level, --log-console-format, --log-console-color, --log-level")).count());
    }

    @DryRun
    @Test
    @Order(10)
    @Launch({"start-dev", "--log=file", "--log-file-output=json", "--log-console-color=true"})
    public void testServerStartDevIfEnabledFileLogOption(LaunchResult result) {
        assertEquals(0, result.getErrorStream().stream().filter(s -> s.contains("Disabled option: '--log-file-output'. Available only when File log handler is activated")).count());
        assertEquals(1, result.getErrorStream().stream().filter(s -> s.contains("Disabled option: '--log-console-color'. Available only when Console log handler is activated")).count());
        assertEquals(1, result.getErrorStream().stream().filter(s -> s.contains("Possible solutions: --log, --log-file, --log-file-level, --log-file-format, --log-file-json-format, --log-file-output, --log-level, --log-level")).count());
    }

    @DryRun
    @Test
    @Order(10)
    @Launch({"start-dev", "--cache-remote-host=localhost"})
    public void testCacheRemoteHostWithoutMultiSite(LaunchResult result) {
        assertErrorStreamContains(result, "cache-remote-host available only when feature 'multi-site', 'clusterless' or 'cache-embedded-remote-store' is set");
    }

    @DryRun
    @Test
    @Order(11)
    @Launch({"start-dev", "--cache-remote-port=11222"})
    public void testCacheRemotePortWithoutCacheRemoteHost(LaunchResult result) {
        assertDisabledDueToMissingRemoteHost(result, "--cache-remote-port");
    }

    @DryRun
    @Test
    @Order(12)
    @Launch({"start-dev", "--cache-remote-username=user"})
    public void testCacheRemoteUsernameWithoutCacheRemoteHost(LaunchResult result) {
        assertDisabledDueToMissingRemoteHost(result, "--cache-remote-username");
    }

    @DryRun
    @Test
    @Order(13)
    @Launch({"start-dev", "--cache-remote-password=pass"})
    public void testCacheRemotePasswordWithoutCacheRemoteHost(LaunchResult result) {
        assertDisabledDueToMissingRemoteHost(result, "--cache-remote-password");
    }

    @DryRun
    @Test
    @Order(14)
    @Launch({"start-dev", "--cache-remote-tls-enabled=false"})
    public void testCacheRemoteTlsEnabledWithoutCacheRemoteHost(LaunchResult result) {
        assertDisabledDueToMissingRemoteHost(result, "--cache-remote-tls-enabled");
    }

    @DryRun
    @Test
    @Order(15)
    @Launch({"start-dev", "--features=multi-site"})
    public void testMultiSiteWithoutCacheRemoteHost(LaunchResult result) {
        assertErrorStreamContains(result, "- cache-remote-host: Required when feature 'multi-site' or 'clusterless' is set.");
    }

    @DryRun
    @Test
    @Order(16)
    @Launch({"start-dev", "--features=multi-site", "--cache-remote-host=localhost", "--cache-remote-username=user"})
    public void testCacheRemoteUsernameWithoutCacheRemotePassword(LaunchResult result) {
        assertErrorStreamContains(result, "The option 'cache-remote-password' is required when 'cache-remote-username' is set.");
    }

    @DryRun
    @Test
    @Order(17)
    @Launch({"start-dev", "--features=multi-site", "--cache-remote-host=localhost", "--cache-remote-password=secret"})
    public void testCacheRemotePasswordWithoutCacheRemoteUsername(LaunchResult result) {
        assertErrorStreamContains(result, "The option 'cache-remote-username' is required when 'cache-remote-password' is set.");
    }

    private static void assertDisabledDueToMissingRemoteHost(LaunchResult result, String option) {
        assertErrorStreamContains(result, "Disabled option: '%s'. Available only when remote host is set".formatted(option));
    }

    private static void assertErrorStreamContains(LaunchResult result, String msg) {
        assertTrue(result.getErrorStream().stream().anyMatch(s -> s.contains(msg)));
    }
}
