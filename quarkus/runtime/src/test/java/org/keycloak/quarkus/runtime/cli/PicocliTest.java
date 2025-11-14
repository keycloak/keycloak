/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.quarkus.runtime.cli;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.keycloak.common.Profile;
import org.keycloak.config.LoggingOptions;
import org.keycloak.quarkus.runtime.Environment;
import org.keycloak.quarkus.runtime.KeycloakMain;
import org.keycloak.quarkus.runtime.cli.command.AbstractAutoBuildCommand;
import org.keycloak.quarkus.runtime.cli.command.AbstractCommand;
import org.keycloak.quarkus.runtime.configuration.AbstractConfigurationTest;
import org.keycloak.quarkus.runtime.configuration.Configuration;
import org.keycloak.quarkus.runtime.configuration.KeycloakConfigSourceProvider;
import org.keycloak.quarkus.runtime.configuration.PersistedConfigSource;

import io.smallrye.config.SmallRyeConfig;
import org.apache.commons.io.FileUtils;
import org.junit.Ignore;
import org.junit.Test;
import picocli.CommandLine;
import picocli.CommandLine.Help;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class PicocliTest extends AbstractConfigurationTest {

    // TODO: could utilize CLIResult
    private class NonRunningPicocli extends Picocli {

        final StringWriter err = new StringWriter();
        final StringWriter out = new StringWriter();
        SmallRyeConfig config;
        int exitCode = Integer.MAX_VALUE;
        boolean reaug;
        private Properties buildProps;

        String getErrString() {
            return normalize(err);
        }

        // normalize line endings - TODO: could also normalize non-printable chars
        // but for now those are part of the expected output
        String normalize(StringWriter writer) {
            return System.lineSeparator().equals("\n") ? writer.toString()
                    : writer.toString().replace(System.lineSeparator(), "\n");
        }

        String getOutString() {
            return normalize(out);
        }

        @Override
        public PrintWriter getErrWriter() {
            return new PrintWriter(err, true);
        }

        @Override
        public PrintWriter getOutWriter() {
            return new PrintWriter(out, true);
        }

        @Override
        public void exit(int exitCode) {
            this.exitCode = exitCode;
        }

        @Override
        public void start() {
            // skip
        }

        @Override
        public void initConfig(List<String> cliArgs, AbstractCommand command) {
            KeycloakConfigSourceProvider.reload();
            boolean checkBuild = Environment.isRebuildCheck();
            super.initConfig(cliArgs, command);
            if (!checkBuild && PersistedConfigSource.getInstance().getConfigValueProperties().isEmpty()) {
                System.getProperties().remove(Environment.KC_CONFIG_REBUILD_CHECK);
            }
            config = Configuration.getConfig();
        }

        @Override
        public void build() throws Throwable {
            reaug = true;
            this.buildProps = getNonPersistedBuildTimeOptions();
        }

    };

    NonRunningPicocli pseudoLaunch(String... args) {
        NonRunningPicocli nonRunningPicocli = new NonRunningPicocli();
        KeycloakMain.main(args, nonRunningPicocli);
        return nonRunningPicocli;
    }

    @Test
    public void testUnbuiltHelp() {
        NonRunningPicocli nonRunningPicocli = pseudoLaunch("bootstrap-admin");
        assertTrue(nonRunningPicocli.getErrString().contains("Missing required subcommand"));
    }

    @Test
    public void testProfileForHelp() {
        NonRunningPicocli nonRunningPicocli = pseudoLaunch("-pf=dev", "bootstrap-admin", "-h");
        assertEquals("dev", nonRunningPicocli.config.getConfigValue("kc.profile").getValue());
    }

    @Test
    public void testCleanStartDev() {
        NonRunningPicocli nonRunningPicocli = pseudoLaunch("start-dev");
        assertFalse(nonRunningPicocli.getOutString(), nonRunningPicocli.getOutString().toUpperCase().contains("WARN"));
        assertFalse(nonRunningPicocli.getOutString(), nonRunningPicocli.getOutString().toUpperCase().contains("ERROR"));
    }

    @Test
    public void testNegativeArgument() {
        NonRunningPicocli nonRunningPicocli = pseudoLaunch("start-dev");
        assertEquals(CommandLine.ExitCode.OK, nonRunningPicocli.exitCode);
        assertEquals("1h",
                nonRunningPicocli.config.getConfigValue("quarkus.http.ssl.certificate.reload-period").getValue());
        assertEquals("1h",
                nonRunningPicocli.config.getConfigValue("quarkus.management.ssl.certificate.reload-period").getValue());

        onAfter();
        nonRunningPicocli = pseudoLaunch("start-dev", "--https-certificates-reload-period=-1");
        assertEquals(CommandLine.ExitCode.OK, nonRunningPicocli.exitCode);
        assertNull(nonRunningPicocli.config.getConfigValue("quarkus.http.ssl.certificate.reload-period").getValue());
        assertNull(nonRunningPicocli.config.getConfigValue("quarkus.management.ssl.certificate.reload-period").getValue());
    }

    @Test
    public void testNegativeArgumentMgmtInterfaceCertReload() {
        NonRunningPicocli nonRunningPicocli = pseudoLaunch("start-dev");
        assertEquals(CommandLine.ExitCode.OK, nonRunningPicocli.exitCode);
        assertEquals("1h",
                nonRunningPicocli.config.getConfigValue("quarkus.management.ssl.certificate.reload-period").getValue());

        onAfter();
        nonRunningPicocli = pseudoLaunch("start-dev", "--https-management-certificates-reload-period=-1");
        assertEquals(CommandLine.ExitCode.OK, nonRunningPicocli.exitCode);
        assertNull(nonRunningPicocli.config.getConfigValue("quarkus.management.ssl.certificate.reload-period").getValue());

        onAfter();
        nonRunningPicocli = pseudoLaunch("start-dev", "--https-certificates-reload-period=5m");
        assertEquals(CommandLine.ExitCode.OK, nonRunningPicocli.exitCode);
        assertEquals("5m",
                nonRunningPicocli.config.getConfigValue("quarkus.management.ssl.certificate.reload-period").getValue());
    }

    @Test
    public void testInvalidArgumentType() {
        NonRunningPicocli nonRunningPicocli = pseudoLaunch("start-dev", "--http-port=a");
        assertEquals(CommandLine.ExitCode.USAGE, nonRunningPicocli.exitCode);
        assertThat(nonRunningPicocli.getErrString(),
                containsString("Invalid value for option '--http-port': Expected an integer value, got \"a\""));
    }

    @Test
    public void testInvalidArgumentTypeEnv() {
        putEnvVar("KC_HTTP_PORT", "a");
        NonRunningPicocli nonRunningPicocli = pseudoLaunch("start-dev");
        assertEquals(CommandLine.ExitCode.USAGE, nonRunningPicocli.exitCode);
        assertThat(nonRunningPicocli.getErrString(),
                containsString("Invalid value for option 'KC_HTTP_PORT': Expected an integer value, got \"a\""));
    }

    @Test
    public void testEmptyValue() {
        NonRunningPicocli nonRunningPicocli = pseudoLaunch("start-dev", "--http-port=");
        assertEquals(CommandLine.ExitCode.USAGE, nonRunningPicocli.exitCode);
        assertThat(nonRunningPicocli.getErrString(),
                containsString("Invalid empty value for option '--http-port'"));
    }

    @Test
    public void failWrongEnumValue() {
        NonRunningPicocli nonRunningPicocli = pseudoLaunch("start-dev", "--log-console-level=wrong");
        assertEquals(CommandLine.ExitCode.USAGE, nonRunningPicocli.exitCode);
        assertThat(nonRunningPicocli.getErrString(), containsString(
                "Invalid value for option '--log-console-level': wrong. Expected values are (case insensitive): off, fatal, error, warn, info, debug, trace, all"));
    }

    @Test
    public void passUpperCaseLogValue() {
        NonRunningPicocli nonRunningPicocli = pseudoLaunch("start-dev", "--log-console-level=INFO");
        assertEquals(CommandLine.ExitCode.OK, nonRunningPicocli.exitCode);
    }

    @Test
    public void passMixedCaseLogValue() {
        NonRunningPicocli nonRunningPicocli = pseudoLaunch("start-dev", "--log-console-level=Info");
        assertEquals(CommandLine.ExitCode.OK, nonRunningPicocli.exitCode);
        assertEquals("INFO", nonRunningPicocli.config.getConfigValue("quarkus.log.console.level").getValue());
    }

    @Test
    public void failMissingOptionValue() {
        NonRunningPicocli nonRunningPicocli = pseudoLaunch("start-dev", "--db");
        assertEquals(CommandLine.ExitCode.USAGE, nonRunningPicocli.exitCode);
        assertThat(nonRunningPicocli.getErrString(), containsString(
                "Option '--db' (vendor) expects a single value. Expected values are: dev-file, dev-mem, mariadb, mssql, mysql, oracle, postgres"));
    }

    @Test
    public void failMultipleOptionValue() {
        NonRunningPicocli nonRunningPicocli = pseudoLaunch("build", "--db", "mysql", "postgres");
        assertEquals(CommandLine.ExitCode.USAGE, nonRunningPicocli.exitCode);
        assertThat(nonRunningPicocli.getErrString(), containsString("Unknown option: 'postgres'"));
    }

    @Test
    public void failMultipleMultiOptionValue() {
        NonRunningPicocli nonRunningPicocli = pseudoLaunch("build", "--features", "linkedin-oauth", "account3");
        assertEquals(CommandLine.ExitCode.USAGE, nonRunningPicocli.exitCode);
        assertThat(nonRunningPicocli.getErrString(), containsString("Unknown option: 'account3'"));
    }

    @Test
    public void failMissingMultiOptionValue() {
        NonRunningPicocli nonRunningPicocli = pseudoLaunch("build", "--features");
        assertEquals(CommandLine.ExitCode.USAGE, nonRunningPicocli.exitCode);
        assertThat(nonRunningPicocli.getErrString(), containsString(
                "Option '--features' (feature) expects one or more comma separated values without whitespace. Expected values are:"));
    }

    @Test
    public void failInvalidMultiOptionValue() {
        NonRunningPicocli nonRunningPicocli = pseudoLaunch("build", "--features", "xyz,account3");
        assertEquals(CommandLine.ExitCode.USAGE, nonRunningPicocli.exitCode);
        assertThat(nonRunningPicocli.getErrString(),
                containsString("'xyz' is an unrecognized feature, it should be one of"));
    }

    @Test
    public void failUnknownOption() {
        NonRunningPicocli nonRunningPicocli = pseudoLaunch("build", "--nosuch");
        assertEquals(CommandLine.ExitCode.USAGE, nonRunningPicocli.exitCode);
        assertThat(nonRunningPicocli.getErrString(), containsString("Unknown option: '--nosuch'"));
    }

    @Test
    public void failUnknownOptionWhitespaceSeparatorNotShowingValue() {
        NonRunningPicocli nonRunningPicocli = pseudoLaunch("start", "--db-pasword", "mytestpw");
        assertEquals(CommandLine.ExitCode.USAGE, nonRunningPicocli.exitCode);
        assertUnknownOption(nonRunningPicocli);
    }

    private void assertUnknownOption(NonRunningPicocli nonRunningPicocli) {
        assertThat(nonRunningPicocli.getErrString(),
                containsString(Help.defaultColorScheme(nonRunningPicocli.getColorMode())
                        .errorText("Unknown option: '--db-pasword'").toString()));
        assertThat(nonRunningPicocli.getErrString(), containsString(
                "Possible solutions: --db-url, --db-url-host, --db-url-database, --db-url-port, --db-url-properties, --db-username, --db-password, --db-schema, --db-pool-initial-size, --db-pool-min-size, --db-pool-max-size, --db-pool-max-lifetime, --db-debug-jpql, --db-log-slow-queries-threshold, --db-driver, --db"));
    }

    @Test
    public void failUnknownOptionEqualsSeparatorNotShowingValue() {
        NonRunningPicocli nonRunningPicocli = pseudoLaunch("start", "--db-pasword=mytestpw");
        assertEquals(CommandLine.ExitCode.USAGE, nonRunningPicocli.exitCode);
        assertUnknownOption(nonRunningPicocli);
    }

    @Test
    public void failWithFirstOptionOnMultipleUnknownOptions() {
        NonRunningPicocli nonRunningPicocli = pseudoLaunch("start", "--db-username=foobar", "--db-pasword=mytestpw",
                "--foobar=barfoo");
        assertEquals(CommandLine.ExitCode.USAGE, nonRunningPicocli.exitCode);
        assertUnknownOption(nonRunningPicocli);
    }

    @Test
    public void testShowConfigHidesSystemProperties() {
        setSystemProperty("kc.something", "password", () -> {
            NonRunningPicocli nonRunningPicocli = pseudoLaunch("show-config");
            // the command line should now show up within the output
            assertThat(nonRunningPicocli.getOutString(), not(containsString("show-config")));
            // arbitrary kc system properties should not show up either
            assertThat(nonRunningPicocli.getOutString(), not(containsString("kc.something")));
        });
    }

    @Test
    public void testShowConfigDisplaysPrimaryValue() {
        build("build", "--db=postgres");
        NonRunningPicocli nonRunningPicocli = pseudoLaunch("show-config");
        assertThat(nonRunningPicocli.getOutString(), containsString("postgres (Persisted)"));
    }

    @Test
    public void failSingleParamWithSpace() {
        NonRunningPicocli nonRunningPicocli = pseudoLaunch("start", "--db postgres");
        assertEquals(CommandLine.ExitCode.USAGE, nonRunningPicocli.exitCode);
        assertThat(nonRunningPicocli.getErrString(), containsString(
                "Option: '--db postgres' is not expected to contain whitespace, please remove any unnecessary quoting/escaping"));
    }

    @Test
    public void spiRuntimeAllowedWithStart() {
        NonRunningPicocli nonRunningPicocli = pseudoLaunch("start", "--db=dev-file", "--http-enabled=true", "--spi-something-pass=changeme");
        assertEquals(CommandLine.ExitCode.OK, nonRunningPicocli.exitCode);
        assertThat(nonRunningPicocli.getOutString(), not(containsString("kc.spi-something-pass")));
    }

    @Test
    public void spiRuntimeWarnWithBuild() {
        NonRunningPicocli nonRunningPicocli = pseudoLaunch("build", "--db=dev-file", "--spi-something-pass=changeme");
        assertEquals(CommandLine.ExitCode.OK, nonRunningPicocli.exitCode);
        assertThat(nonRunningPicocli.getOutString(), containsString("The following run time options were found, but will be ignored during build time: kc.spi-something-pass"));
    }

    @Test
    public void failBuildDev() {
        NonRunningPicocli nonRunningPicocli = pseudoLaunch("--profile=dev", "build", "--verbose");
        assertThat(nonRunningPicocli.getErrString(), containsString("You can not 'build' the server in development mode."));
        assertEquals(CommandLine.ExitCode.SOFTWARE, nonRunningPicocli.exitCode);
    }

    @Test
    public void failStartBuildDev() {
        NonRunningPicocli nonRunningPicocli = pseudoLaunch("--profile=dev", "start");
        assertThat(nonRunningPicocli.getErrString(), containsString("You can not 'start' the server in development mode."));
        assertEquals(CommandLine.ExitCode.USAGE, nonRunningPicocli.exitCode);
    }

    @Test
    public void failIfOptimizedUsedForFirstStartupExport() {
        NonRunningPicocli nonRunningPicocli = pseudoLaunch("export", "--optimized", "--dir=data");
        assertEquals(CommandLine.ExitCode.USAGE, nonRunningPicocli.exitCode);
        assertThat(nonRunningPicocli.getErrString(), containsString("The '--optimized' flag was used for first ever server start."));
    }

    @Test
    public void optimizedExport() {
        build("build", "--db=dev-file");

        NonRunningPicocli nonRunningPicocli = pseudoLaunch("export", "--optimized", "--dir=data");
        assertEquals(CommandLine.ExitCode.OK, nonRunningPicocli.exitCode);
    }

    @Test
    public void testReaugFromProdToDev() {
        build("build", "--db=dev-file");

        NonRunningPicocli nonRunningPicocli = pseudoLaunch("start-dev", "--hostname=name", "--http-enabled=true");
        assertEquals(AbstractAutoBuildCommand.REBUILT_EXIT_CODE, nonRunningPicocli.exitCode);
        assertTrue(nonRunningPicocli.reaug);
        assertEquals("dev", nonRunningPicocli.buildProps.getProperty(org.keycloak.common.util.Environment.PROFILE));
    }

    /**
     * Runs a fake build to setup the state of the persisted build properties
     */
    private NonRunningPicocli build(String... args) {
        return build(out -> {
            assertFalse(out, out.contains("first-class"));
            assertFalse(out, out.toUpperCase().contains("WARN"));
        }, args);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private NonRunningPicocli build(Consumer<String> outChecker, String... args) {
        int code = CommandLine.ExitCode.OK;
        if (Stream.of(args).anyMatch("start-dev"::equals)) {
            Environment.setRebuildCheck(true);
            code = AbstractAutoBuildCommand.REBUILT_EXIT_CODE;
        }
        NonRunningPicocli nonRunningPicocli = pseudoLaunch(args);
        assertTrue(nonRunningPicocli.getErrString(), nonRunningPicocli.reaug);
        assertEquals(nonRunningPicocli.getErrString(), code, nonRunningPicocli.exitCode);
        outChecker.accept(nonRunningPicocli.getOutString());
        onAfter();
        addPersistedConfigValues((Map)nonRunningPicocli.buildProps);
        return nonRunningPicocli;
    }

    @Test
    public void testReaugFromProdToDevExport() {
        build("build", "--db=dev-file");

        NonRunningPicocli nonRunningPicocli = pseudoLaunch("--profile=dev", "export", "--file=file");
        assertEquals(AbstractAutoBuildCommand.REBUILT_EXIT_CODE, nonRunningPicocli.exitCode);
        assertTrue(nonRunningPicocli.reaug);
    }

    @Test
    public void testNoReaugFromProdToExport() {
        build("build", "--db=dev-file");

        NonRunningPicocli nonRunningPicocli = pseudoLaunch("export", "--db=dev-file", "--file=file");
        assertEquals(CommandLine.ExitCode.OK, nonRunningPicocli.exitCode);
        assertFalse(nonRunningPicocli.reaug);
    }

    @Ignore("Not valid until db is required for production")
    @Test
    public void testDBRequiredAutoBuild() {
        build("build", "--db=dev-file");

        NonRunningPicocli nonRunningPicocli = pseudoLaunch("export", "--file=file");
        assertEquals(CommandLine.ExitCode.USAGE, nonRunningPicocli.exitCode);
    }

    @Test
    public void testReaugFromDevToProd() {
        build("start-dev");

        NonRunningPicocli nonRunningPicocli = pseudoLaunch("start", "--db=dev-file", "--hostname=name", "--http-enabled=true");
        assertEquals(AbstractAutoBuildCommand.REBUILT_EXIT_CODE, nonRunningPicocli.exitCode);
        assertTrue(nonRunningPicocli.reaug);
    }

    @Test
    public void testNoReaugFromDevToDevExport() {
        build("start-dev");

        NonRunningPicocli nonRunningPicocli = pseudoLaunch("--profile=dev", "export", "--file=file");
        assertEquals(CommandLine.ExitCode.OK, nonRunningPicocli.exitCode);
        assertFalse(nonRunningPicocli.reaug);
    }

    @Test
    public void testReaugFromDevToProdExport() {
        build("start-dev", "-v");

        NonRunningPicocli nonRunningPicocli = pseudoLaunch("export", "--db=dev-file", "--file=file");
        assertEquals(AbstractAutoBuildCommand.REBUILT_EXIT_CODE, nonRunningPicocli.exitCode);
        assertTrue(nonRunningPicocli.reaug);
        assertEquals("prod", nonRunningPicocli.buildProps.getProperty(org.keycloak.common.util.Environment.PROFILE));
    }

    @Test
    public void testOptimizedReaugmentationMessage() {
        build("build", "--db=dev-file");

        NonRunningPicocli nonRunningPicocli = pseudoLaunch("start", "--db=dev-file", "--features=docker", "--hostname=name", "--http-enabled=true");
        assertEquals(AbstractAutoBuildCommand.REBUILT_EXIT_CODE, nonRunningPicocli.exitCode);
        assertThat(nonRunningPicocli.getOutString(), containsString("features=<unset> > features=docker"));
        assertTrue(nonRunningPicocli.reaug);
    }

    @Test
    public void startOptimizedSucceeds() {
        build("build", "--db=dev-file");

        System.setProperty("kc.http-enabled", "true");
        System.setProperty("kc.hostname-strict", "false");

        NonRunningPicocli nonRunningPicocli = pseudoLaunch("start", "--optimized");
        assertEquals(CommandLine.ExitCode.OK, nonRunningPicocli.exitCode);
    }

    @Test
    public void invalidImportRealmArgument() {
        NonRunningPicocli nonRunningPicocli = pseudoLaunch("start-dev", "--import-realm", "some-file");
        assertEquals(CommandLine.ExitCode.USAGE, nonRunningPicocli.exitCode);
        assertTrue(nonRunningPicocli.getErrString(), nonRunningPicocli.getErrString().contains("Unknown option: 'some-file'"));
    }

    @Test
    public void invalidImportRealmEqualsArgument() {
        NonRunningPicocli nonRunningPicocli = pseudoLaunch("start-dev", "--import-realm=some-file");
        assertEquals(CommandLine.ExitCode.USAGE, nonRunningPicocli.exitCode);
        assertTrue(nonRunningPicocli.getErrString(), nonRunningPicocli.getErrString().contains("option '--import-realm' should be specified without 'some-file' parameter"));
    }

    @Test
    public void wrongLevelForCategory() {
        NonRunningPicocli nonRunningPicocli = pseudoLaunch("start-dev", "--log-level-org.keycloak", "wrong");
        assertEquals(CommandLine.ExitCode.USAGE, nonRunningPicocli.exitCode);
        assertTrue(nonRunningPicocli.getErrString().contains("Invalid log level: wrong. Possible values are: warn, trace, debug, error, fatal, info."));
    }

    @Test
    public void wildcardLevelForCategory() {
        NonRunningPicocli nonRunningPicocli = pseudoLaunch("start-dev", "--log-level-org.keycloak=warn");
        assertEquals(CommandLine.ExitCode.OK, nonRunningPicocli.exitCode);
        var value = nonRunningPicocli.config.getConfigValue("quarkus.log.category.\"org.keycloak\".level");
        assertEquals("quarkus.log.category.\"org.keycloak\".level", value.getName());
        assertEquals("WARN", value.getValue());
        value = nonRunningPicocli.config.getConfigValue("quarkus.log.category.\"org.keycloak1\".level");
        assertEquals("quarkus.log.category.\"org.keycloak1\".level", value.getName());
        assertNull(value.getValue());
    }

    @Test
    public void wildcardLevelFromParent() {
        NonRunningPicocli nonRunningPicocli = pseudoLaunch("start-dev", "--log-level=org.keycloak:warn");
        assertEquals(CommandLine.ExitCode.OK, nonRunningPicocli.exitCode);
        var value = nonRunningPicocli.config.getConfigValue("quarkus.log.category.\"org.keycloak\".level");
        assertEquals("quarkus.log.category.\"org.keycloak\".level", value.getName());
        assertEquals("WARN", value.getValue());
        value = nonRunningPicocli.config.getConfigValue("quarkus.log.category.\"org.keycloak1\".level");
        assertEquals("quarkus.log.category.\"org.keycloak1\".level", value.getName());
        assertNull(value.getValue());
    }

    @Test
    public void warnDBRequired() {
        // dev profile has a default
        NonRunningPicocli nonRunningPicocli = pseudoLaunch("start-dev");
        assertEquals(CommandLine.ExitCode.OK, nonRunningPicocli.exitCode);
        assertThat(nonRunningPicocli.getOutString(),
                not(containsString("Usage of the default value for the db option")));
        onAfter();

        // prod profiles warn about db
        nonRunningPicocli = pseudoLaunch("build");
        assertEquals(CommandLine.ExitCode.OK, nonRunningPicocli.exitCode);
        assertThat(nonRunningPicocli.getOutString(),
                containsString("Usage of the default value for the db option in the production profile is deprecated. Please explicitly set the db instead."));
    }

    @Test
    public void syslogMaxLengthMemorySize() {
        NonRunningPicocli nonRunningPicocli = pseudoLaunch("start-dev", "--log=syslog", "--log-syslog-max-length=60k");
        assertEquals(CommandLine.ExitCode.OK, nonRunningPicocli.exitCode);
        assertEquals("60k", nonRunningPicocli.config.getConfigValue("quarkus.log.syslog.max-length").getValue());
        onAfter();

        nonRunningPicocli = pseudoLaunch("start-dev", "--log=syslog");
        assertEquals(CommandLine.ExitCode.OK, nonRunningPicocli.exitCode);
        assertThat(nonRunningPicocli.config.getConfigValue("quarkus.log.syslog.max-length").getValue(), nullValue());
        onAfter();

        nonRunningPicocli = pseudoLaunch("start-dev", "--log=syslog", "--log-syslog-max-length=wrong");
        assertEquals(CommandLine.ExitCode.USAGE, nonRunningPicocli.exitCode);
        assertThat(nonRunningPicocli.getErrString(), containsString(
                "Invalid value for option '--log-syslog-max-length': value wrong not in correct format (regular expression): [0-9]+[BbKkMmGgTtPpEeZzYy]?"));
    }

    @Test
    public void syslogCountingFraming() {
        NonRunningPicocli nonRunningPicocli = pseudoLaunch("start-dev", "--log=syslog", "--log-syslog-counting-framing=TRUE");
        assertThat(nonRunningPicocli.exitCode, is(CommandLine.ExitCode.USAGE));
        assertThat(nonRunningPicocli.getErrString(), containsString(
                "Invalid value for option '--log-syslog-counting-framing': TRUE. Expected values are: true, false, protocol-dependent"));

        onAfter();
        nonRunningPicocli = pseudoLaunch("start-dev", "--log=syslog", "--log-syslog-counting-framing=true");
        assertThat(nonRunningPicocli.exitCode, is(CommandLine.ExitCode.OK));
        assertThat(nonRunningPicocli.config.getConfigValue("quarkus.log.syslog.use-counting-framing").getValue(), is("true"));

        onAfter();
        nonRunningPicocli = pseudoLaunch("start-dev", "--log=syslog", "--log-syslog-counting-framing=false");
        assertThat(nonRunningPicocli.exitCode, is(CommandLine.ExitCode.OK));
        assertThat(nonRunningPicocli.config.getConfigValue("quarkus.log.syslog.use-counting-framing").getValue(), is("false"));

        onAfter();
        nonRunningPicocli = pseudoLaunch("start-dev", "--log=syslog", "--log-syslog-protocol=ssl-tcp", "--log-syslog-counting-framing=protocol-dependent");
        assertThat(nonRunningPicocli.exitCode, is(CommandLine.ExitCode.OK));
        assertThat(nonRunningPicocli.config.getConfigValue("quarkus.log.syslog.use-counting-framing").getValue(), is("protocol-dependent"));

        onAfter();
        nonRunningPicocli = pseudoLaunch("start-dev", "--log=syslog", "--log-syslog-counting-framing=wrong");
        assertThat(nonRunningPicocli.exitCode, is(CommandLine.ExitCode.USAGE));
        assertThat(nonRunningPicocli.getErrString(), containsString(
                "Invalid value for option '--log-syslog-counting-framing': wrong. Expected values are: true, false, protocol-dependent"));
    }

    @Test
    public void providerChanged() {
        build("build", "--db=dev-file");

        addPersistedConfigValues(Map.of(Picocli.KC_PROVIDER_FILE_PREFIX + "fake", "value"));

        NonRunningPicocli nonRunningPicocli = pseudoLaunch("start", "--optimized", "--http-enabled=true", "--hostname-strict=false");
        assertEquals(CommandLine.ExitCode.USAGE, nonRunningPicocli.exitCode);
        assertTrue(nonRunningPicocli.getErrString().contains(Picocli.PROVIDER_TIMESTAMP_ERROR));
    }

    @Test
    public void warnProviderChanged() {
        build("build", "--db=dev-file");

        putEnvVar("KC_RUN_IN_CONTAINER", "true");
        String key = PersistedConfigSource.getInstance().getConfigValueProperties().keySet().stream().filter(k -> k.startsWith(Picocli.KC_PROVIDER_FILE_PREFIX)).findAny().orElseThrow();
        addPersistedConfigValues(Map.of(key, "1")); // change to a fake timestamp

        NonRunningPicocli nonRunningPicocli = pseudoLaunch("start", "--optimized", "--http-enabled=true", "--hostname-strict=false");
        assertEquals(nonRunningPicocli.getErrString(), CommandLine.ExitCode.OK, nonRunningPicocli.exitCode);
        assertTrue(nonRunningPicocli.getOutString().contains(Picocli.PROVIDER_TIMESTAMP_WARNING));
    }

    @Test
    public void buildOptionChangedWithOptimized() {
        build("build", "--db=dev-file");

        NonRunningPicocli nonRunningPicocli = pseudoLaunch("start", "--optimized", "--db=dev-mem");
        assertEquals(CommandLine.ExitCode.USAGE, nonRunningPicocli.exitCode);
        assertTrue(nonRunningPicocli.getErrString().contains("Build time option: '--db' not usable with pre-built image and --optimized"));
    }

    @Test
    public void buildOptionWithOptimized() {
        build("build", "--metrics-enabled=true", "--db=dev-file");

        NonRunningPicocli nonRunningPicocli = pseudoLaunch("start", "--optimized", "--http-enabled=true", "--hostname-strict=false");
        assertEquals(CommandLine.ExitCode.OK, nonRunningPicocli.exitCode);
    }

    @Test
    public void testHiddenCliConfigValueWithNoDescription() {
        NonRunningPicocli nonRunningPicocli = pseudoLaunch("start-dev", "--db-dialect=user-defined");
        assertEquals(CommandLine.ExitCode.OK, nonRunningPicocli.exitCode);
        assertEquals("user-defined", nonRunningPicocli.config.getConfigValue("kc.db-dialect").getValue());
    }

    @Test
    public void buildDBWithOptimized() {
        build("build", "--db=mariadb");

        NonRunningPicocli nonRunningPicocli = pseudoLaunch("import", "--optimized", "--dir=./", "--override=false");
        assertEquals(CommandLine.ExitCode.OK, nonRunningPicocli.exitCode);
    }

    @Test
    public void logConsoleJsonFormatDisabled() {
        NonRunningPicocli nonRunningPicocli = pseudoLaunch("start-dev", "--log-console-json-format=ecs");
        assertEquals(CommandLine.ExitCode.USAGE, nonRunningPicocli.exitCode);
        assertThat(nonRunningPicocli.getErrString(), containsString("Disabled option: '--log-console-json-format'. Available only when Console log handler is activated and output is set to 'json'"));
    }

    @Test
    public void logConsoleJsonFormat() {
        NonRunningPicocli nonRunningPicocli = pseudoLaunch("start-dev", "--log-console-output=json", "--log-console-json-format=invalid");
        assertEquals(CommandLine.ExitCode.USAGE, nonRunningPicocli.exitCode);
        assertThat(nonRunningPicocli.getErrString(), containsString("Invalid value for option '--log-console-json-format': invalid. Expected values are: default, ecs"));
        onAfter();

        nonRunningPicocli = pseudoLaunch("start-dev", "--log-console-output=json", "--log-console-json-format=ecs");
        assertEquals(CommandLine.ExitCode.OK, nonRunningPicocli.exitCode);
    }

    @Test
    public void logFileJsonFormatDisabled() {
        NonRunningPicocli nonRunningPicocli = pseudoLaunch("start-dev", "--log=file", "--log-file-json-format=ecs");
        assertEquals(CommandLine.ExitCode.USAGE, nonRunningPicocli.exitCode);
        assertThat(nonRunningPicocli.getErrString(), containsString("Disabled option: '--log-file-json-format'. Available only when File log handler is activated and output is set to 'json'"));
    }

    @Test
    public void logFileJsonFormat() {
        NonRunningPicocli nonRunningPicocli = pseudoLaunch("start-dev", "--log-console-output=json", "--log-console-json-format=invalid");
        assertEquals(CommandLine.ExitCode.USAGE, nonRunningPicocli.exitCode);
        assertThat(nonRunningPicocli.getErrString(), containsString("Invalid value for option '--log-console-json-format': invalid. Expected values are: default, ecs"));
        onAfter();

        nonRunningPicocli = pseudoLaunch("start-dev", "--log-console-output=json", "--log-console-json-format=ecs");
        assertEquals(CommandLine.ExitCode.OK, nonRunningPicocli.exitCode);
    }

    @Test
    public void logSyslogJsonFormatDisabled() {
        NonRunningPicocli nonRunningPicocli = pseudoLaunch("start-dev", "--log=syslog", "--log-syslog-json-format=ecs");
        assertEquals(CommandLine.ExitCode.USAGE, nonRunningPicocli.exitCode);
        assertThat(nonRunningPicocli.getErrString(), containsString("Disabled option: '--log-syslog-json-format'. Available only when Syslog is activated and output is set to 'json'"));
    }

    @Test
    public void logSyslogJsonFormat() {
        NonRunningPicocli nonRunningPicocli = pseudoLaunch("start-dev", "--log=syslog", "--log-syslog-output=json", "--log-syslog-json-format=invalid");
        assertEquals(CommandLine.ExitCode.USAGE, nonRunningPicocli.exitCode);
        assertThat(nonRunningPicocli.getErrString(), containsString("Invalid value for option '--log-syslog-json-format': invalid. Expected values are: default, ecs"));
        onAfter();

        nonRunningPicocli = pseudoLaunch("start-dev", "--log=syslog", "--log-syslog-output=json", "--log-syslog-json-format=ecs");
        assertEquals(CommandLine.ExitCode.OK, nonRunningPicocli.exitCode);
    }

    @Test
    public void proxyProtolNotAllowedWithProxyHeaders() {
        NonRunningPicocli nonRunningPicocli = pseudoLaunch("start-dev", "--proxy-headers=forwarded", "--proxy-protocol-enabled=true");
        assertEquals(CommandLine.ExitCode.USAGE, nonRunningPicocli.exitCode);
        assertThat(nonRunningPicocli.getErrString(), containsString(" protocol cannot be enabled when using the `proxy-headers` option"));
    }

    @Test
    public void hostnameProxyValidation() {
        NonRunningPicocli nonRunningPicocli = pseudoLaunch("start", "--hostname=foo", "--http-enabled=true");
        assertEquals(CommandLine.ExitCode.OK, nonRunningPicocli.exitCode);
        assertThat(nonRunningPicocli.getOutString(), containsString("Likely misconfiguration detected"));
    }

    @Test
    public void hostnameProxyValidationStrictFalse() {
        NonRunningPicocli nonRunningPicocli = pseudoLaunch("start", "--hostname-strict=false", "--http-enabled=true");
        assertEquals(CommandLine.ExitCode.OK, nonRunningPicocli.exitCode);
        assertThat(nonRunningPicocli.getOutString(), containsString("With HTTPS not enabled"));
    }

    @Test
    public void hostnameValidationHttp() {
        NonRunningPicocli nonRunningPicocli = pseudoLaunch("start", "--hostname=http://host", "--http-enabled=true");
        assertEquals(CommandLine.ExitCode.OK, nonRunningPicocli.exitCode);
        assertThat(nonRunningPicocli.getOutString(), containsString("Likely misconfiguration detected"));
    }

    @Test
    public void derivedPropertyUsage() {
        NonRunningPicocli nonRunningPicocli = pseudoLaunch("start-dev", "--hostname=localhost", "--spi-hostname-v2-hostname=second-class");
        assertEquals(CommandLine.ExitCode.OK, nonRunningPicocli.exitCode);
        assertThat(nonRunningPicocli.getOutString(), containsString("Please use the first-class option `kc.hostname` instead of `kc.spi-hostname-v2-hostname`"));
    }

    @Test
    public void testAmbiguousSpiOption() {
        NonRunningPicocli nonRunningPicocli = pseudoLaunch("start-dev", "--spi-x-y-enabled=true");
        assertEquals(CommandLine.ExitCode.OK, nonRunningPicocli.exitCode);
        assertThat(nonRunningPicocli.getOutString(), containsString("The following SPI options are using the legacy format and are not being treated as build time options. Please use the new format with the appropriate -- separators to resolve this ambiguity: kc.spi-x-y-enabled"));
    }

    @Test
    public void testAmbiguousSpiOptionBuild() {
        NonRunningPicocli nonRunningPicocli = pseudoLaunch("build", "--db=dev-file", "--spi-x-y-enabled=true");
        assertEquals(CommandLine.ExitCode.OK, nonRunningPicocli.exitCode);
        assertThat(nonRunningPicocli.getOutString(), containsString("The following SPI options are using the legacy format and are not being treated as build time options. Please use the new format with the appropriate -- separators to resolve this ambiguity: kc.spi-x-y-enabled"));
    }

    @Test
    public void testDerivedShowConfig() {
        NonRunningPicocli nonRunningPicocli = build("build", "--metrics-enabled=true", "--features=user-event-metrics", "--event-metrics-user-enabled=true", "--db=dev-file");

        nonRunningPicocli = pseudoLaunch("show-config");
        // first class kc form should show up
        assertThat(nonRunningPicocli.getOutString(), containsString("kc.event-metrics-user-enabled"));
        // second class kc form should not
        assertThat(nonRunningPicocli.getOutString(), not(containsString("kc.spi-events-listener-micrometer-user-event-metrics-enabled")));
    }

    @Test
    public void logAsyncDisabledParent() {
        NonRunningPicocli nonRunningPicocli = pseudoLaunch("start-dev", "--log=file", "--log-console-async=true");
        assertEquals(CommandLine.ExitCode.USAGE, nonRunningPicocli.exitCode);
        assertThat(nonRunningPicocli.getErrString(), containsString("Disabled option: '--log-console-async'. Available only when Console log handler is activated"));

        onAfter();
        nonRunningPicocli = pseudoLaunch("start-dev", "--log=console", "--log-file-async=true");
        assertEquals(CommandLine.ExitCode.USAGE, nonRunningPicocli.exitCode);
        assertThat(nonRunningPicocli.getErrString(), containsString("Disabled option: '--log-file-async'. Available only when File log handler is activated"));

        onAfter();
        nonRunningPicocli = pseudoLaunch("start-dev", "--log=file", "--log-syslog-async=true");
        assertEquals(CommandLine.ExitCode.USAGE, nonRunningPicocli.exitCode);
        assertThat(nonRunningPicocli.getErrString(), containsString("Disabled option: '--log-syslog-async'. Available only when Syslog is activated"));
    }

    @Test
    public void logAsyncGlobal() {
        NonRunningPicocli nonRunningPicocli = pseudoLaunch("start-dev", "--log-async=true", "--log-console-async=false", "--log-console-async-queue-length=222");
        assertEquals(CommandLine.ExitCode.USAGE, nonRunningPicocli.exitCode);
        assertThat(nonRunningPicocli.getErrString(), containsString("Disabled option: '--log-console-async-queue-length'. Available only when Console log handler is activated"));
        onAfter();

        nonRunningPicocli = pseudoLaunch("start-dev", "--log-async=true", "--log-console-async-queue-length=222");
        assertEquals(CommandLine.ExitCode.OK, nonRunningPicocli.exitCode);

        onAfter();

        nonRunningPicocli = pseudoLaunch("start-dev", "--log=console,file", "--log-async=true", "--log-console-async=false", "--log-console-async-queue-length=222");
        assertEquals(CommandLine.ExitCode.USAGE, nonRunningPicocli.exitCode);
        assertThat(nonRunningPicocli.getErrString(), containsString("Disabled option: '--log-console-async-queue-length'. Available only when Console log handler is activated"));
        onAfter();

        nonRunningPicocli = pseudoLaunch("start-dev", "--log=console,file", "--log-async=true", "--log-console-async=false", "--log-file-async-queue-length=222");
        assertEquals(CommandLine.ExitCode.OK, nonRunningPicocli.exitCode);
    }

    @Test
    public void logAsyncConsoleDisabledOptions() {
        assertLogAsyncHandlerDisabledOptions(LoggingOptions.Handler.file, LoggingOptions.Handler.console, "Console log handler");
    }

    @Test
    public void logAsyncFileDisabledOptions() {
        assertLogAsyncHandlerDisabledOptions(LoggingOptions.Handler.console, LoggingOptions.Handler.file, "File log handler");
    }

    @Test
    public void logAsyncSyslogDisabledOptions() {
        assertLogAsyncHandlerDisabledOptions(LoggingOptions.Handler.console, LoggingOptions.Handler.syslog, "Syslog");
    }

    private void assertLogAsyncHandlerDisabledOptions(LoggingOptions.Handler logHandler, LoggingOptions.Handler logHandlerOptions, String logHandlerFullName) {
        var logHandlerName = logHandler.toString();
        var logHandlerOptionsName = logHandlerOptions.toString();

        NonRunningPicocli nonRunningPicocli = pseudoLaunch("start-dev", "--log=%s".formatted(logHandlerOptionsName), "--log-%s-async=true".formatted(logHandlerOptionsName), "--log-%s-async-queue-length=invalid".formatted(logHandlerOptionsName));
        assertEquals(CommandLine.ExitCode.USAGE, nonRunningPicocli.exitCode);
        assertThat(nonRunningPicocli.getErrString(), containsString("Invalid value for option '--log-%s-async-queue-length': Expected an integer value, got \"invalid\"".formatted(logHandlerOptionsName)));

        onAfter();
        nonRunningPicocli = pseudoLaunch("start-dev", "--log=%s".formatted(logHandlerName), "--log-%s-async-queue-length=768".formatted(logHandlerOptionsName));
        assertEquals(CommandLine.ExitCode.USAGE, nonRunningPicocli.exitCode);
        assertThat(nonRunningPicocli.getErrString(), containsString("Disabled option: '--log-%s-async-queue-length'. Available only when %s is activated and asynchronous logging is enabled".formatted(logHandlerOptionsName, logHandlerFullName)));

    }

    @Test
    public void logAsyncConsoleInvalidValues() {
        assertLogAsyncHandlerInvalidValues(LoggingOptions.Handler.console);
    }

    @Test
    public void logAsyncFileInvalidValues() {
        assertLogAsyncHandlerInvalidValues(LoggingOptions.Handler.file);
    }

    @Test
    public void logAsyncSyslogInvalidValues() {
        assertLogAsyncHandlerInvalidValues(LoggingOptions.Handler.syslog);
    }

    @Test
    public void timestampChanged() {
        assertTrue(Picocli.timestampChanged("12345", "12346"));
        assertTrue(Picocli.timestampChanged("12000", "12346"));
        // new is truncated - should not be a change
        assertFalse(Picocli.timestampChanged("12345", "12000"));
    }

    @Test
    public void quarkusRuntimeChangeNoError() throws IOException {
        Path conf = Paths.get("src/test/resources/");
        Path tmp = Paths.get("target/home-tmp");
        FileUtils.copyDirectory(conf.toFile(), tmp.toFile());
        Files.delete(tmp.resolve("conf/quarkus.properties"));
        Environment.setHomeDir(tmp);
        try {
            build("build", "--db=dev-file");
        } finally {
            Environment.setHomeDir(conf);
        }

        var nonRunningPicocli = pseudoLaunch("start", "--optimized", "--http-enabled=true", "--hostname=foo");
        assertEquals(CommandLine.ExitCode.OK, nonRunningPicocli.exitCode);
    }

    protected void assertLogAsyncHandlerInvalidValues(LoggingOptions.Handler handler) {
        var handlerName = handler.toString();

        var nonRunningPicocli = pseudoLaunch("start-dev", "--log=%s".formatted(handlerName), "--log-%s-async=true".formatted(handlerName), "--log-%s-async-queue-length=invalid".formatted(handlerName));
        assertEquals(CommandLine.ExitCode.USAGE, nonRunningPicocli.exitCode);
        assertThat(nonRunningPicocli.getErrString(), containsString("Invalid value for option '--log-%s-async-queue-length': Expected an integer value, got \"invalid\"".formatted(handlerName)));
    }

    @Test
    public void testImportHelpAllSucceeds() {
        NonRunningPicocli nonRunningPicocli = pseudoLaunch("import", "--help-all");
        assertEquals(CommandLine.ExitCode.OK, nonRunningPicocli.exitCode);
        assertTrue(nonRunningPicocli.getOutString().contains("--db"));
    }

    @Test
    public void testUnaryBooleanFails() {
        NonRunningPicocli nonRunningPicocli = pseudoLaunch("start-dev", "--health-enabled");
        assertEquals(CommandLine.ExitCode.USAGE, nonRunningPicocli.exitCode);
        assertThat(nonRunningPicocli.getErrString(), containsString("Option '--health-enabled' (true|false) expects a single value. Expected values are: true, false"));
    }

    @Test
    public void datasourcesNotAllowedChar(){
        NonRunningPicocli nonRunningPicocli = pseudoLaunch("start-dev","--db-kind-<default>=postgres");
        assertEquals(CommandLine.ExitCode.USAGE, nonRunningPicocli.exitCode);
        assertThat(nonRunningPicocli.getErrString(), containsString("Unknown option: '--db-kind-<default>'"));
    }

    @Test
    public void updateCommandValidation(){
        NonRunningPicocli nonRunningPicocli = pseudoLaunch("update-compatibility","check");
        assertEquals(CommandLine.ExitCode.USAGE, nonRunningPicocli.exitCode);
        assertThat(nonRunningPicocli.getErrString(), containsString("Missing required argument: --file"));
    }

    @Test
    public void testNoKcDirWarning() {
        putEnvVar("KC_DIR", "dir");
        putEnvVar("KC_LOG_LEVEL", "debug");
        var picocli = build("build", "--db=dev-file");
        assertFalse(picocli.getOutString(), picocli.getOutString().contains("kc.dir"));
    }

    @Test
    public void testUpdatesFileValidation() {
        NonRunningPicocli picocli = pseudoLaunch("update-compatibility","check", "--file=not-found");
        assertTrue(picocli.getErrString().contains("Incorrect argument --file."));
    }

    @Test
    public void errorSpiBuildtimeChanged() {
        putEnvVar("KC_SPI_EVENTS_LISTENER__PROVIDER", "jboss-logging");
        build("build", "--db=dev-file");

        putEnvVar("KC_SPI_EVENTS_LISTENER__PROVIDER", "new-jboss-logging");

        NonRunningPicocli nonRunningPicocli = pseudoLaunch("start", "--optimized", "--http-enabled=true", "--hostname-strict=false");
        assertEquals(CommandLine.ExitCode.USAGE, nonRunningPicocli.exitCode);
        assertThat(nonRunningPicocli.getErrString(), containsString("The following build time options have values that differ from what is persisted - the new values will NOT be used until another build is run: kc.spi-events-listener--provider"));
    }

    @Test
    public void spiAmbiguousSpiAutoBuild() {
        putEnvVar("KC_SPI_EVENTS_LISTENER_PROVIDER", "jboss-logging");
        NonRunningPicocli nonRunningPicocli = build(out -> assertThat(out, containsString("The following SPI options")), "build", "--db=dev-file");

        putEnvVar("KC_SPI_EVENTS_LISTENER_PROVIDER", "new-jboss-logging");
        nonRunningPicocli = pseudoLaunch("start", "--http-enabled=true", "--hostname-strict=false");
        assertEquals(AbstractAutoBuildCommand.REBUILT_EXIT_CODE, nonRunningPicocli.exitCode);
        assertTrue(nonRunningPicocli.reaug);
        assertThat(nonRunningPicocli.getOutString(), containsString("The following SPI options"));
    }

    @Test
    public void httpAccessLog() {
        // http-access-log-pattern disabled
        NonRunningPicocli nonRunningPicocli = pseudoLaunch("start-dev", "--http-access-log-pattern=long");
        assertEquals(CommandLine.ExitCode.USAGE, nonRunningPicocli.exitCode);
        assertThat(nonRunningPicocli.getErrString(), containsString("Available only when HTTP Access log is enabled"));
        onAfter();

        // http-access-log-pattern disabled
        nonRunningPicocli = pseudoLaunch("start-dev", "--http-access-log-exclude=something/");
        assertEquals(CommandLine.ExitCode.USAGE, nonRunningPicocli.exitCode);
        assertThat(nonRunningPicocli.getErrString(), containsString("Available only when HTTP Access log is enabled"));
        onAfter();

        // accept other patterns - error will be thrown on Quarkus side
        nonRunningPicocli = pseudoLaunch("start-dev", "--http-access-log-enabled=true", "--http-access-log-pattern=not-named-pattern");
        assertEquals(CommandLine.ExitCode.OK, nonRunningPicocli.exitCode);
        onAfter();

        // accept other patterns
        nonRunningPicocli = pseudoLaunch("start-dev", "--http-access-log-enabled=true", "--http-access-log-pattern='%r n%{ALL_REQUEST_HEADERS}'");
        assertEquals(CommandLine.ExitCode.OK, nonRunningPicocli.exitCode);
        onAfter();

        // exclude
        nonRunningPicocli = pseudoLaunch("start-dev", "--http-access-log-enabled=true", "--http-access-log-exclude=something.*");
        assertEquals(CommandLine.ExitCode.OK, nonRunningPicocli.exitCode);
        onAfter();

        nonRunningPicocli = pseudoLaunch("start-dev", "--http-access-log-enabled=true", "--http-access-log-exclude='/realms/my-realm/.*");
        assertEquals(CommandLine.ExitCode.OK, nonRunningPicocli.exitCode);
    }

    @Test
    public void healthEnabledRequired() {
        var nonRunningPicocli = pseudoLaunch("start-dev", "--http-management-health-enabled=false");
        assertEquals(CommandLine.ExitCode.USAGE, nonRunningPicocli.exitCode);
        assertThat(nonRunningPicocli.getErrString(), containsString("Available only when health is enabled"));
    }

    @Test
    public void duplicatedCliOptions() {
        var nonRunningPicocli = pseudoLaunch("start-dev", "--http-access-log-enabled=true", "--http-access-log-enabled=false");
        assertEquals(CommandLine.ExitCode.OK, nonRunningPicocli.exitCode);
        assertThat(nonRunningPicocli.getOutString(), containsString("WARNING: Duplicated options present in CLI: --http-access-log-enabled"));
        onAfter();

        nonRunningPicocli = pseudoLaunch("start-dev", "--http-access-log-enabled=true", "--http-access-log-enabled=false", "--db=postgres", "--db=dev-mem");
        assertEquals(CommandLine.ExitCode.OK, nonRunningPicocli.exitCode);
        assertThat(nonRunningPicocli.getOutString(), containsString("WARNING: Duplicated options present in CLI: --http-access-log-enabled, --db"));
        onAfter();

        nonRunningPicocli = pseudoLaunch("start-dev",
                "--http-access-log-enabled=true", "--http-access-log-enabled=false",
                "--db=postgres", "--db=dev-mem",
                "--db-kind-my-store=mariadb", "--db-kind-my-store=dev-mem");
        assertEquals(CommandLine.ExitCode.OK, nonRunningPicocli.exitCode);
        assertThat(nonRunningPicocli.getOutString(), containsString("Duplicated options present in CLI: --db-kind-my-store, --http-access-log-enabled, --db"));
        onAfter();

        nonRunningPicocli = pseudoLaunch("start-dev", "--non-existing=yes", "--non-existing=no");
        assertEquals(CommandLine.ExitCode.USAGE, nonRunningPicocli.exitCode);
        assertThat(nonRunningPicocli.getOutString(), not(containsString("WARNING: Duplicated options present in CLI: --non-existing")));
        assertThat(nonRunningPicocli.getErrString(), containsString("Unknown option: '--non-existing'"));
        onAfter();

        nonRunningPicocli = pseudoLaunch("start-dev", "-Dsome.property=123", "-Dsome.property=456");
        assertEquals(CommandLine.ExitCode.OK, nonRunningPicocli.exitCode);
        assertThat(nonRunningPicocli.getOutString(), containsString("WARNING: Duplicated options present in CLI: -Dsome.property"));
        onAfter();

        nonRunningPicocli = pseudoLaunch("start-dev", "something-wrong=asdf", "something-wrong=not-here");
        assertEquals(CommandLine.ExitCode.USAGE, nonRunningPicocli.exitCode);
        assertThat(nonRunningPicocli.getOutString(), not(containsString("WARNING: Duplicated options present in CLI: something-wrong")));
        assertThat(nonRunningPicocli.getErrString(), containsString("Unknown option: 'something-wrong'"));
    }

    @Test
    public void httpOptimizedSerializers() {
        var nonRunningPicocli = pseudoLaunch("start-dev");
        assertEquals(CommandLine.ExitCode.OK, nonRunningPicocli.exitCode);
        assertExternalConfigNull("quarkus.rest.jackson.optimization.enable-reflection-free-serializers");
        onAfter();

        nonRunningPicocli = pseudoLaunch("start-dev", "--features=http-optimized-serializers");
        assertEquals(CommandLine.ExitCode.OK, nonRunningPicocli.exitCode);
        assertExternalConfig("quarkus.rest.jackson.optimization.enable-reflection-free-serializers", "true");
    }

    @Test
    public void tracingHiddenParentHeaders() {
        var nonRunningPicocli = pseudoLaunch("start-dev", "--tracing-headers=Authorization=Bearer asdlkfjadsflkj");
        assertEquals(CommandLine.ExitCode.USAGE, nonRunningPicocli.exitCode);
        assertThat(nonRunningPicocli.getErrString(), containsString("Disabled option: '--tracing-headers'. Available only when Tracing is enabled"));
        onAfter();

        nonRunningPicocli = pseudoLaunch("start-dev", "--tracing-enabled=true", "--tracing-headers=Authorization=Bearer asdlkfjadsflkj");
        assertEquals(CommandLine.ExitCode.OK, nonRunningPicocli.exitCode);
        assertExternalConfig("quarkus.otel.exporter.otlp.traces.headers", "Authorization=Bearer asdlkfjadsflkj");
        onAfter();

        nonRunningPicocli = pseudoLaunch("start-dev", "--tracing-enabled=true", "--tracing-headers=Authorization=Bearer asdlkfjadsflkj,Host=localhost:8080");
        assertEquals(CommandLine.ExitCode.OK, nonRunningPicocli.exitCode);
        assertExternalConfig("quarkus.otel.exporter.otlp.traces.headers", "Authorization=Bearer asdlkfjadsflkj,Host=localhost:8080");
    }

    @Test
    public void tracingHeaders() {
        // tracing is disabled
        var nonRunningPicocli = pseudoLaunch("start-dev", "--tracing-header-Authorization=Bearer asdlkfjadsflkj");
        assertEquals(CommandLine.ExitCode.USAGE, nonRunningPicocli.exitCode);
        onAfter();

        // basic
        nonRunningPicocli = pseudoLaunch("start-dev", "--tracing-enabled=true", "--tracing-header-Authorization=Bearer asdlkfjadsflkj");
        assertEquals(CommandLine.ExitCode.OK, nonRunningPicocli.exitCode);
        assertExternalConfig("quarkus.otel.exporter.otlp.traces.headers", "Authorization=Bearer asdlkfjadsflkj");
        onAfter();

        // multiple
        nonRunningPicocli = pseudoLaunch("start-dev", "--tracing-enabled=true", "--tracing-header-Authorization=Bearer asdlkfjadsflkj", "--tracing-header-Host=localhost:8080");
        assertEquals(CommandLine.ExitCode.OK, nonRunningPicocli.exitCode);
        assertExternalConfig("quarkus.otel.exporter.otlp.traces.headers", "Authorization=Bearer asdlkfjadsflkj,Host=localhost:8080");
        onAfter();

        // other header
        nonRunningPicocli = pseudoLaunch("start-dev", "--tracing-enabled=true", "--tracing-header-Content-length=300");
        assertEquals(CommandLine.ExitCode.OK, nonRunningPicocli.exitCode);
        assertExternalConfig("quarkus.otel.exporter.otlp.traces.headers", "Content-length=300");
        onAfter();

        // duplicated headers
        nonRunningPicocli = pseudoLaunch("start-dev", "--tracing-enabled=true", "--tracing-header-Content-Language=en-US", "--tracing-header-Content-Language=de-DE");
        assertEquals(CommandLine.ExitCode.OK, nonRunningPicocli.exitCode);
        // the last is accepted
        assertExternalConfig("quarkus.otel.exporter.otlp.traces.headers", "Content-Language=de-DE");
        onAfter();

        // Hidden 'tracing-headers' takes precedence
        nonRunningPicocli = pseudoLaunch("start-dev", "--tracing-enabled=true", "--tracing-headers=Overridden-by-me=yes", "--tracing-header-Authorization=Bearer asdlkfjadsflkj", "--tracing-header-Host=localhost:8080");
        assertEquals(CommandLine.ExitCode.OK, nonRunningPicocli.exitCode);
        assertExternalConfig("quarkus.otel.exporter.otlp.traces.headers", "Overridden-by-me=yes");
    }

    @Test
    public void singleFeatureFlag() {
        var nonRunningPicocli = pseudoLaunch("start-dev", "--feature-impersonation=disabled");
        assertEquals(CommandLine.ExitCode.OK, nonRunningPicocli.exitCode);
        assertThat(Profile.isFeatureEnabled(Profile.Feature.IMPERSONATION), is(false));
        onAfter();

        nonRunningPicocli = pseudoLaunch("start-dev", "--feature-ipa-tuura-federation=enabled");
        assertEquals(CommandLine.ExitCode.OK, nonRunningPicocli.exitCode);
        assertThat(Profile.isFeatureEnabled(Profile.Feature.IPA_TUURA_FEDERATION), is(true));
        onAfter();

        nonRunningPicocli = pseudoLaunch("start-dev", "--feature-ipa-tuura-federation=ENABLED");
        assertEquals(CommandLine.ExitCode.USAGE, nonRunningPicocli.exitCode);
        assertThat(nonRunningPicocli.getErrString(), containsString("Wrong value for feature 'ipa-tuura-federation': ENABLED. You can specify either 'enabled', 'disabled', or specific version (lowercase) that will be enabled"));
        assertThat(Profile.isFeatureEnabled(Profile.Feature.IPA_TUURA_FEDERATION), is(false));
        onAfter();

        nonRunningPicocli = pseudoLaunch("start-dev", "--feature-ipa-tuura-federation=v1");
        assertEquals(CommandLine.ExitCode.OK, nonRunningPicocli.exitCode);
        assertThat(Profile.isFeatureEnabled(Profile.Feature.IPA_TUURA_FEDERATION), is(true));
        onAfter();

        nonRunningPicocli = pseudoLaunch("start-dev", "--feature-ipa-tuura-federation=V1");
        assertEquals(CommandLine.ExitCode.USAGE, nonRunningPicocli.exitCode);
        assertThat(nonRunningPicocli.getErrString(), containsString("Wrong value for feature 'ipa-tuura-federation': V1. You can specify either 'enabled', 'disabled', or specific version (lowercase) that will be enabled"));
        assertThat(Profile.isFeatureEnabled(Profile.Feature.IPA_TUURA_FEDERATION), is(false));
        onAfter();

        nonRunningPicocli = pseudoLaunch("start-dev", "--feature-admin-fine-grained-authz=v1");
        assertEquals(CommandLine.ExitCode.OK, nonRunningPicocli.exitCode);
        assertThat(Profile.isFeatureEnabled(Profile.Feature.ADMIN_FINE_GRAINED_AUTHZ), is(true));
        assertThat(Profile.isFeatureEnabled(Profile.Feature.ADMIN_FINE_GRAINED_AUTHZ_V2), is(false));
        onAfter();

        nonRunningPicocli = pseudoLaunch("start-dev", "--feature-admin-fine-grained-authz=disabled");
        assertEquals(CommandLine.ExitCode.OK, nonRunningPicocli.exitCode);
        assertThat(Profile.isFeatureEnabled(Profile.Feature.ADMIN_FINE_GRAINED_AUTHZ), is(false));
        assertThat(Profile.isFeatureEnabled(Profile.Feature.ADMIN_FINE_GRAINED_AUTHZ_V2), is(false));
        onAfter();

        nonRunningPicocli = pseudoLaunch("start-dev", "--feature-admin-fine-grained-authz=enabled");
        assertEquals(CommandLine.ExitCode.OK, nonRunningPicocli.exitCode);
        assertThat(Profile.isFeatureEnabled(Profile.Feature.ADMIN_FINE_GRAINED_AUTHZ), is(false));
        assertThat(Profile.isFeatureEnabled(Profile.Feature.ADMIN_FINE_GRAINED_AUTHZ_V2), is(true));
        onAfter();

        // duplicates
        nonRunningPicocli = pseudoLaunch("start-dev", "--feature-passkeys=enabled", "--feature-passkeys=disabled");
        assertEquals(CommandLine.ExitCode.OK, nonRunningPicocli.exitCode);
        assertThat(nonRunningPicocli.getOutString(), containsString("WARNING: Duplicated options present in CLI: --feature-passkeys"));
        assertThat(Profile.isFeatureEnabled(Profile.Feature.PASSKEYS), is(false));
        onAfter();

        nonRunningPicocli = pseudoLaunch("start-dev", "--feature-passkeys=enabled", "--feature-passkeys=disabled", "--feature-spiffe=v1", "--feature-spiffe=disabled");
        assertEquals(CommandLine.ExitCode.OK, nonRunningPicocli.exitCode);
        assertThat(nonRunningPicocli.getOutString(), containsString("WARNING: Duplicated options present in CLI: --feature-spiffe, --feature-passkeys"));
        assertThat(Profile.isFeatureEnabled(Profile.Feature.PASSKEYS), is(false));
        assertThat(Profile.isFeatureEnabled(Profile.Feature.SPIFFE), is(false));
        onAfter();

        nonRunningPicocli = pseudoLaunch("start-dev", "--feature-passkeys=enabled", "--feature-passkeys=disabled", "--feature-spiffe=v1", "--feature-spiffe=");
        assertEquals(CommandLine.ExitCode.USAGE, nonRunningPicocli.exitCode);
        assertThat(nonRunningPicocli.getErrString(), containsString("Missing value for feature 'spiffe'"));
        onAfter();

        // Non-existing
        nonRunningPicocli = pseudoLaunch("start-dev", "--feature-not-here=enabled");
        assertEquals(CommandLine.ExitCode.USAGE, nonRunningPicocli.exitCode);
        assertThat(nonRunningPicocli.getErrString(), containsString("'not-here' is an unrecognized feature, it should be one of"));
        assertThat(nonRunningPicocli.getErrString(), not(containsString("preview")));
        onAfter();

        nonRunningPicocli = pseudoLaunch("start-dev", "--feature-non-existing=v2");
        assertEquals(CommandLine.ExitCode.USAGE, nonRunningPicocli.exitCode);
        assertThat(nonRunningPicocli.getErrString(), containsString("'non-existing' is an unrecognized feature, it should be one of"));
        assertThat(nonRunningPicocli.getErrString(), not(containsString("preview")));
        onAfter();

        nonRunningPicocli = pseudoLaunch("start-dev", "--feature-non-existing-feature=disabled");
        assertEquals(CommandLine.ExitCode.USAGE, nonRunningPicocli.exitCode);
        assertThat(nonRunningPicocli.getErrString(), containsString("'non-existing-feature' is an unrecognized feature, it should be one of"));
        assertThat(nonRunningPicocli.getErrString(), not(containsString("preview")));
        onAfter();

        // wrong value
        nonRunningPicocli = pseudoLaunch("start-dev", "--feature-impersonation=false");
        assertEquals(CommandLine.ExitCode.USAGE, nonRunningPicocli.exitCode);
        assertThat(nonRunningPicocli.getErrString(), containsString("Wrong value for feature 'impersonation': false. You can specify either 'enabled', 'disabled', or specific version (lowercase) that will be enabled"));
        onAfter();

        nonRunningPicocli = pseudoLaunch("start-dev", "--feature-impersonation=v3");
        assertEquals(CommandLine.ExitCode.USAGE, nonRunningPicocli.exitCode);
        assertThat(nonRunningPicocli.getErrString(), containsString("Feature 'impersonation' has an unrecognized feature version, it should be one of [1]"));
        onAfter();

        nonRunningPicocli = pseudoLaunch("start-dev", "--feature-impersonation=");
        assertEquals(CommandLine.ExitCode.USAGE, nonRunningPicocli.exitCode);
        assertThat(nonRunningPicocli.getErrString(), containsString("Missing value for feature 'impersonation'"));
        onAfter();

        // ENV variables
        putEnvVar("KC_FEATURE_IPA_TUURA_FEDERATION", "v1");
        nonRunningPicocli = pseudoLaunch("start-dev");
        assertEquals(CommandLine.ExitCode.OK, nonRunningPicocli.exitCode);
        assertThat(Profile.isFeatureEnabled(Profile.Feature.IPA_TUURA_FEDERATION), is(true));
        onAfter();

        putEnvVar("KC_FEATURE_TRANSIENT_USERS", "enabled");
        nonRunningPicocli = pseudoLaunch("start-dev");
        assertEquals(CommandLine.ExitCode.OK, nonRunningPicocli.exitCode);
        assertThat(Profile.isFeatureEnabled(Profile.Feature.TRANSIENT_USERS), is(true));
        onAfter();

        putEnvVar("KC_FEATURE_TRANSIENT_USERS", "");
        nonRunningPicocli = pseudoLaunch("start-dev");
        assertEquals(CommandLine.ExitCode.USAGE, nonRunningPicocli.exitCode);
        assertThat(nonRunningPicocli.getErrString(), containsString("Missing value for feature 'transient-users'"));
        onAfter();

        putEnvVar("KC_FEATURE_TRANSIENT_USERS", "v1");
        nonRunningPicocli = pseudoLaunch("start-dev");
        assertEquals(CommandLine.ExitCode.OK, nonRunningPicocli.exitCode);
        assertThat(Profile.isFeatureEnabled(Profile.Feature.TRANSIENT_USERS), is(true));
        onAfter();

        putEnvVar("KC_FEATURE_DPOP", "disabled");
        nonRunningPicocli = pseudoLaunch("start-dev");
        assertEquals(CommandLine.ExitCode.OK, nonRunningPicocli.exitCode);
        assertThat(Profile.isFeatureEnabled(Profile.Feature.DPOP), is(false));
        onAfter();

        putEnvVar("KC_FEATURE_DPOP", "wrong");
        nonRunningPicocli = pseudoLaunch("start-dev");
        assertEquals(CommandLine.ExitCode.USAGE, nonRunningPicocli.exitCode);
        assertThat(nonRunningPicocli.getErrString(), containsString("Wrong value for feature 'dpop': wrong. You can specify either 'enabled', 'disabled', or specific version (lowercase) that will be enabled"));
    }
}
