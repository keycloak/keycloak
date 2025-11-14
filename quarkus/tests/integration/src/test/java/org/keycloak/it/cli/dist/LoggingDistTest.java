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

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.keycloak.config.LoggingOptions;
import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.DryRun;
import org.keycloak.it.junit5.extension.RawDistOnly;
import org.keycloak.it.utils.KeycloakDistribution;
import org.keycloak.it.utils.RawDistRootPath;
import org.keycloak.it.utils.RawKeycloakDistribution;

import io.quarkus.deployment.util.FileUtil;
import io.quarkus.test.junit.main.Launch;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.org.apache.commons.io.FileUtils;

import static org.keycloak.quarkus.runtime.cli.command.Main.CONFIG_FILE_LONG_NAME;

import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DistributionTest(keepAlive = true)
@RawDistOnly(reason = "Too verbose for docker and enough to check raw dist")
@Tag(DistributionTest.SLOW)
public class LoggingDistTest {

    @Test
    @Launch({ "start-dev", "--log-level=warn" })
    void testSetRootLevel(CLIResult cliResult) {
        assertFalse(cliResult.getOutput().contains("INFO  [io.quarkus]"));
        assertFalse(cliResult.getOutput().contains("Listening on:"));
        cliResult.assertStartedDevMode();
    }

    @Test
    @Launch({ "start-dev", "--log-level=org.keycloak:debug" })
    void testSetCategoryLevel(CLIResult cliResult) {
        assertFalse(cliResult.getOutput().contains("DEBUG [org.hibernate"));
        assertTrue(cliResult.getOutput().contains("DEBUG [org.keycloak"));
        cliResult.assertStartedDevMode();
    }

    @Test
    @Launch({ "start-dev", "--log-level=off,org.keycloak:debug" })
    void testRootAndCategoryLevels(CLIResult cliResult) {
        assertFalse(cliResult.getOutput().contains("INFO  [io.quarkus"));
        assertTrue(cliResult.getOutput().contains("DEBUG [org.keycloak"));
    }

    @Test
    @Launch({ "start-dev", "--log-level=off,org.keycloak:warn,warn" })
    void testSetLastRootLevelIfMultipleSet(CLIResult cliResult) {
        assertFalse(cliResult.getOutput().contains("INFO"));
        assertFalse(cliResult.getOutput().contains("DEBUG"));
        assertFalse(cliResult.getOutput().contains("Listening on:"));
        assertTrue(cliResult.getOutput().contains("Running the server in development mode."));
        cliResult.assertStartedDevMode();
    }

    @Test
    @Launch({ "start-dev", "--log-console-format=\"%d{yyyy-MM-dd HH:mm:ss,SSS} %-5p [%c{1.}] %s%e%n\"" })
    void testSetLogFormat(CLIResult cliResult) {
        assertFalse(cliResult.getOutput().contains("(keycloak-cache-init)"));
        cliResult.assertStartedDevMode();
    }

    @Test
    void testJsonFormatApplied(KeycloakDistribution dist) throws IOException {
        RawKeycloakDistribution rawDist = dist.unwrap(RawKeycloakDistribution.class);
        FileUtil.deleteDirectory(rawDist.getDistPath().resolve("data").resolve("h2").toAbsolutePath());
        CLIResult cliResult = dist.run("start-dev", "--log-console-output=json");
        cliResult.assertJsonLogDefaultsApplied();
        cliResult.assertStartedDevMode();
        assertFalse(cliResult.getOutput().contains("UPDATE SUMMARY"));
    }

    @Test
    void testLogLevelSettingsAppliedWhenJsonEnabled(KeycloakDistribution dist) throws IOException {
        RawKeycloakDistribution rawDist = dist.unwrap(RawKeycloakDistribution.class);
        FileUtil.deleteDirectory(rawDist.getDistPath().resolve("data").resolve("h2").toAbsolutePath());
        CLIResult cliResult = dist.run("start-dev", "--log-level=off,org.keycloak:debug,liquibase:debug", "--log-console-output=json");
        assertFalse(cliResult.getOutput().contains("\"loggerName\":\"io.quarkus\",\"level\":\"INFO\")"));
        assertTrue(cliResult.getOutput().contains("\"loggerName\":\"org.keycloak.services.resources.KeycloakApplication\",\"level\":\"DEBUG\""));
        assertTrue(cliResult.getOutput().contains("\"loggerName\":\"liquibase.servicelocator\",\"level\":\"FINE\""));
        assertTrue(cliResult.getOutput().contains("UPDATE SUMMARY"));
    }

    @Test
    @Launch({ "start-dev", "--log=console,file"})
    void testKeycloakLogFileCreated(RawDistRootPath path) {
        Path logFilePath = Paths.get(path.getDistRootPath() + File.separator + LoggingOptions.DEFAULT_LOG_PATH);
        File logFile = new File(logFilePath.toString());
        assertTrue(logFile.isFile(), "Log file does not exist!");
    }

    @Test
    @Launch({ "start-dev", "--log=console,file", "--log-file-format=\"%d{HH:mm:ss} %-5p [%c{1.}] (%t) %s%e%n\""})
    void testFileLoggingHasDifferentFormat(RawDistRootPath path) {
        String data = readDefaultFileLog(path);
        assertTrue(data.contains("INFO  [i.quarkus] (main)"), "Format not applied");
    }

    @Test
    @Launch({ "start-dev", "--log=file"})
    void testFileOnlyLogsNothingToConsole(CLIResult cliResult) {
        assertFalse(cliResult.getOutput().contains("INFO  [io.quarkus]"));
    }

    @Test
    void failUnknownHandlersInConfFile(KeycloakDistribution dist) {
        dist.copyOrReplaceFileFromClasspath("/logging/keycloak.conf", Paths.get("conf", "keycloak.conf"));
        CLIResult cliResult = dist.run("start-dev");
        cliResult.assertError("Invalid value for option 'kc.log' in keycloak.conf: foo. Expected values are: console, file, syslog");
    }

    @Test
    void failEmptyLogErrorFromConfFileError(KeycloakDistribution dist) {
        dist.copyOrReplaceFileFromClasspath("/logging/emptylog.conf", Paths.get("conf", "emptylog.conf"));
        CLIResult cliResult = dist.run(CONFIG_FILE_LONG_NAME+"=../conf/emptylog.conf", "start-dev");
        cliResult.assertError("Invalid value for option 'kc.log' in emptylog.conf: . Expected values are: console, file, syslog");
    }

    @Test
    @Launch({ "start-dev","--log=foo,bar" })
    void failUnknownHandlersInCliCommand(CLIResult cliResult) {
        cliResult.assertError("Invalid value for option '--log': foo");
    }

    @Test
    @Launch({ "start-dev","--log=" })
    void failEmptyLogValueInCliError(CLIResult cliResult) {
        cliResult.assertError("Invalid value for option '--log': .");
    }

    @Test
    @Launch({"start-dev", "--log=syslog"})
    void syslogHandler(CLIResult cliResult) {
        cliResult.assertNoMessage("org.keycloak");
        cliResult.assertNoMessage("Listening on:");
        cliResult.assertError("Error writing to TCP stream");
    }

    @Test
    @Launch({"start-dev", "--log-console-level=wrong"})
    @DryRun
    void wrongLevelForHandlers(CLIResult cliResult) {
        cliResult.assertError("Invalid value for option '--log-console-level': wrong. Expected values are (case insensitive): off, fatal, error, warn, info, debug, trace, all");
    }

    @Test
    @Launch({"start-dev", "--log-level-org.keycloak=wrong"})
    @DryRun
    void wrongLevelForCategory(CLIResult cliResult) {
        cliResult.assertError("Invalid log level: wrong. Possible values are: warn, trace, debug, error, fatal, info.");
    }

    @Test
    @Launch({"start-dev", "--log=console,file", "--log-console-level=debug", "--log-file-level=debug"})
    void levelRootDefault(CLIResult cliResult, RawDistRootPath path) {
        var output = cliResult.getOutput();

        assertThat(output, not(containsString("DEBUG [org.hibernate")));
        assertThat(output, not(containsString("DEBUG [org.keycloak")));

        var fileLog = readDefaultFileLog(path);
        assertThat(fileLog, notNullValue());
        assertFalse(fileLog.isBlank());

        assertThat(fileLog, not(containsString("DEBUG [org.hibernate")));
        assertThat(fileLog, not(containsString("DEBUG [org.keycloak")));

        assertThat(fileLog, containsString("INFO  [io.quarkus]"));
        assertThat(fileLog, containsString("INFO  [org.keycloak"));
    }

    @Test
    @Launch({"start-dev", "--log=console,file", "--log-level=org.keycloak:debug", "--log-console-level=debug", "--log-file-level=debug"})
    void levelRootCategoryDebug(CLIResult cliResult, RawDistRootPath path) {
        var output = cliResult.getOutput();

        assertThat(output, not(containsString("DEBUG [org.hibernate")));
        assertThat(output, containsString("DEBUG [org.keycloak"));

        var fileLog = readDefaultFileLog(path);
        assertThat(fileLog, notNullValue());
        assertFalse(fileLog.isBlank());

        assertThat(fileLog, not(containsString("DEBUG [org.hibernate")));
        assertThat(fileLog, containsString("DEBUG [org.keycloak"));

        assertThat(fileLog, containsString("INFO  [io.quarkus]"));
        assertThat(fileLog, containsString("INFO  [org.keycloak"));
    }

    @Test
    @Launch({"start-dev", "--log=console,file", "--log-level=info,org.keycloak:warn", "--log-console-level=off", "--log-file-level=off"})
    void levelOffHandlers(CLIResult cliResult, RawDistRootPath path) {
        var output = cliResult.getOutput();

        // log contains DB migration status + build time logs
        assertThat(output, not(containsString("DEBUG [org.hibernate")));
        assertThat(output, not(containsString("INFO  [org.keycloak")));
        assertThat(output, not(containsString("INFO  [io.quarkus")));

        var fileLog = readDefaultFileLog(path);
        assertThat(fileLog, notNullValue());
        assertTrue(fileLog.isBlank());
    }

    @Test
    @Launch({"start-dev", "--log-level=error,org.keycloak:warn,org.hibernate:debug", "--log-level-org.keycloak=trace"})
    void categoryLogLevel(CLIResult cliResult) {
        var output = cliResult.getOutput();

        assertThat(output, containsString("DEBUG [org.hibernate"));
        assertThat(output, not(containsString("TRACE [org.hibernate")));
        assertThat(output, containsString("TRACE [org.keycloak"));
        assertThat(output, not(containsString("INFO  [io.quarkus")));
    }

    @Test
    @Launch({"start-dev", "--log=console,file", "--log-console-output=json", "--log-console-json-format=ecs", "--log-file-output=json", "--log-file-json-format=ecs"})
    void ecsFormat(CLIResult cliResult, RawDistRootPath path) {
        var output = cliResult.getOutput();

        assertThat(output, containsString("ecs.version"));
        assertThat(output, containsString("@timestamp"));

        String data = readDefaultFileLog(path);
        assertThat(data, containsString("ecs.version"));
        assertThat(data, containsString("@timestamp"));
    }

    @Test
    @Launch({"start-dev", "--log-async=true"})
    void asyncLogging(CLIResult cliResult) {
        cliResult.assertStartedDevMode();
    }

    @Test
    @Launch({ "start-dev", "--features=log-mdc","--log-mdc-enabled=true", "--log-level=org.keycloak.transaction:debug" })
    void testLogMdcShowingInTheLogs(CLIResult cliResult) {

        when().get("http://127.0.0.1:8080/realms/master/.well-known/openid-configuration").then()
                .statusCode(200);
        assertThat(cliResult.getOutput(), containsString("{kc.realmName=master} DEBUG [org.keycloak."));
        cliResult.assertStartedDevMode();
    }

    protected static String readDefaultFileLog(RawDistRootPath path) {
        Path logFilePath = Paths.get(path.getDistRootPath() + File.separator + LoggingOptions.DEFAULT_LOG_PATH);
        File logFile = new File(logFilePath.toString());
        assertTrue(logFile.isFile(), "Log file does not exist!");

        try {
            return FileUtils.readFileToString(logFile, Charset.defaultCharset());
        } catch (IOException e) {
            throw new AssertionError("Cannot read default file log", e);
        }
    }

    // HTTP Access log
    @Test
    @Launch({"start-dev", "--http-access-log-enabled=true", "--http-access-log-pattern='%A %{METHOD} %{REQUEST_URL} %{i,User-Agent}'", "--http-access-log-exclude='/realms/master/clients/.*'"})
    void httpAccessLogNotNamedPattern(CLIResult cliResult) {
        cliResult.assertStartedDevMode();

        when().get("http://127.0.0.1:8080/realms/master/.well-known/openid-configuration").then()
                .statusCode(200);
        cliResult.assertMessage("[org.keycloak.http.access-log]");
        cliResult.assertMessage("127.0.0.1 GET /realms/master/.well-known/openid-configuration");

        when().get("http://127.0.0.1:8080/realms/master/clients/account/redirect").then()
                .statusCode(200);
        cliResult.assertNoMessage("http://127.0.0.1:8080/realms/master/clients/account/redirect");
    }
}
