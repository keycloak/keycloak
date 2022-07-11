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

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.keycloak.config.LoggingOptions;
import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.RawDistOnly;

import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import org.keycloak.it.utils.RawDistRootPath;
import org.keycloak.quarkus.runtime.configuration.mappers.LoggingPropertyMappers;
import org.testcontainers.shaded.org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;

@DistributionTest(reInstall = DistributionTest.ReInstall.NEVER)
@RawDistOnly(reason = "Too verbose for docker and enough to check raw dist")
public class LoggingDistTest {

    @Test
    @Launch({ "start-dev", "--log-level=debug" })
    void testSetRootLevel(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        assertTrue(cliResult.getOutput().contains("DEBUG [io.quarkus.resteasy.runtime]"));
        cliResult.assertStartedDevMode();
    }

    @Test
    @Launch({ "start-dev", "--log-level=org.keycloak:debug" })
    void testSetCategoryLevel(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        assertFalse(cliResult.getOutput().contains("DEBUG [org.hibernate"));
        assertTrue(cliResult.getOutput().contains("DEBUG [org.keycloak"));
        cliResult.assertStartedDevMode();
    }

    @Test
    @EnabledOnOs(value = { OS.LINUX, OS.MAC }, disabledReason = "different shell escaping behaviour on Windows.")
    @Launch({ "start-dev", "--log-level=off,org.keycloak:debug,org.infinispan:info" })
    void testRootAndCategoryLevels(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        assertFalse(cliResult.getOutput().contains("INFO  [io.quarkus"));
        assertTrue(cliResult.getOutput().contains("DEBUG [org.keycloak"));
        assertTrue(cliResult.getOutput().contains("INFO  [org.infinispan.CONTAINER]"));
    }

    @Test
    @EnabledOnOs(value = { OS.WINDOWS }, disabledReason = "different shell escaping behaviour on Windows.")
    @Launch({ "start-dev", "--log-level=\"off,org.keycloak:debug,org.infinispan:info\"" })
    void testWinRootAndCategoryLevels(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        assertFalse(cliResult.getOutput().contains("INFO  [io.quarkus"));
        assertTrue(cliResult.getOutput().contains("DEBUG [org.keycloak"));
        assertTrue(cliResult.getOutput().contains("INFO  [org.infinispan.CONTAINER]"));
    }

    @Test
    @EnabledOnOs(value = { OS.LINUX, OS.MAC }, disabledReason = "different shell escaping behaviour on Windows.")
    @Launch({ "start-dev", "--log-level=off,org.keycloak:warn,debug" })
    void testSetLastRootLevelIfMultipleSet(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        assertTrue(cliResult.getOutput().contains("DEBUG [io.quarkus.resteasy.runtime]"));
        assertFalse(cliResult.getOutput().contains("INFO  [org.keycloak"));
        cliResult.assertStartedDevMode();
    }

    @Test
    @EnabledOnOs(value = { OS.WINDOWS }, disabledReason = "different shell escaping behaviour on Windows.")
    @Launch({ "start-dev", "--log-level=\"off,org.keycloak:warn,debug\"" })
    void testWinSetLastRootLevelIfMultipleSet(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        assertTrue(cliResult.getOutput().contains("DEBUG [org.hibernate"));
        assertFalse(cliResult.getOutput().contains("INFO  [org.keycloak"));
        cliResult.assertStartedDevMode();
    }

    @Test
    @EnabledOnOs(value = { OS.LINUX, OS.MAC }, disabledReason = "different shell escaping behaviour on Windows.")
    @Launch({ "start-dev", "--log-console-format=\"%d{yyyy-MM-dd HH:mm:ss,SSS} %-5p [%c{1.}] %s%e%n\"" })
    void testSetLogFormat(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        assertFalse(cliResult.getOutput().contains("(keycloak-cache-init)"));
        cliResult.assertStartedDevMode();
    }

    @Test
    @Launch({ "start-dev", "--log-console-output=json" })
    void testJsonFormatApplied(LaunchResult result) throws JsonProcessingException {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertJsonLogDefaultsApplied();
        cliResult.assertStartedDevMode();
    }

    @Test
    @EnabledOnOs(value = { OS.LINUX, OS.MAC }, disabledReason = "different shell escaping behaviour on Windows.")
    @Launch({ "start-dev", "--log-level=off,org.keycloak:debug,org.infinispan:info", "--log-console-output=json" })
    void testLogLevelSettingsAppliedWhenJsonEnabled(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        assertFalse(cliResult.getOutput().contains("\"loggerName\":\"io.quarkus\",\"level\":\"INFO\")"));
        assertTrue(cliResult.getOutput().contains("\"loggerName\":\"org.keycloak.services.resources.KeycloakApplication\",\"level\":\"DEBUG\""));
        assertTrue(cliResult.getOutput().contains("\"loggerName\":\"org.infinispan.CONTAINER\",\"level\":\"INFO\""));
    }

    @Test
    @EnabledOnOs(value = { OS.WINDOWS }, disabledReason = "different shell escaping behaviour on Windows.")
    @Launch({ "start-dev", "--log-level=\"off,org.keycloak:debug,org.infinispan:info\"", "--log-console-output=json" })
    void testWinLogLevelSettingsAppliedWhenJsonEnabled(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        assertFalse(cliResult.getOutput().contains("\"loggerName\":\"io.quarkus\",\"level\":\"INFO\")"));
        assertTrue(cliResult.getOutput().contains("\"loggerName\":\"org.keycloak.services.resources.KeycloakApplication\",\"level\":\"DEBUG\""));
        assertTrue(cliResult.getOutput().contains("\"loggerName\":\"org.infinispan.CONTAINER\",\"level\":\"INFO\""));
    }

    @Test
    @EnabledOnOs(value = { OS.LINUX, OS.MAC }, disabledReason = "different shell escaping behaviour on Windows.")
    @Launch({ "start-dev", "--log=console,file"})
    void testKeycloakLogFileCreated(RawDistRootPath path) {
        Path logFilePath = Paths.get(path.getDistRootPath() + File.separator + LoggingOptions.DEFAULT_LOG_PATH);
        File logFile = new File(logFilePath.toString());
        assertTrue(logFile.isFile(), "Log file does not exist!");
    }

    @Test
    @EnabledOnOs(value = { OS.WINDOWS }, disabledReason = "different shell escaping behaviour on Windows.")
    @Launch({ "start-dev", "--log=\"console,file\""})
    void testWinKeycloakLogFileCreated(RawDistRootPath path) {
        Path logFilePath = Paths.get(path.getDistRootPath() + File.separator + LoggingOptions.DEFAULT_LOG_PATH);
        File logFile = new File(logFilePath.toString());
        assertTrue(logFile.isFile(), "Log file does not exist!");
    }

    @Test
    @EnabledOnOs(value = { OS.LINUX, OS.MAC }, disabledReason = "different shell escaping behaviour on Windows.")
    @Launch({ "start-dev", "--log=console,file", "--log-file-format=\"%d{HH:mm:ss} %-5p [%c{1.}] (%t) %s%e%n\""})
    void testFileLoggingHasDifferentFormat(RawDistRootPath path) throws IOException {
        Path logFilePath = Paths.get(path.getDistRootPath() + File.separator + LoggingOptions.DEFAULT_LOG_PATH);
        File logFile = new File(logFilePath.toString());

        String data = FileUtils.readFileToString(logFile, Charset.defaultCharset());
        assertTrue(data.contains("INFO  [i.quarkus] (main)"), "Format not applied");
    }

    @Test
    @Launch({ "start-dev", "--log=file"})
    void testFileOnlyLogsNothingToConsole(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        assertFalse(cliResult.getOutput().contains("INFO  [io.quarkus]"));
    }
}