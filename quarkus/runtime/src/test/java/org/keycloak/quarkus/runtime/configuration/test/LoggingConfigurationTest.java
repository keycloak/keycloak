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

package org.keycloak.quarkus.runtime.configuration.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.keycloak.config.LoggingOptions.DEFAULT_LOG_FORMAT;
import static org.keycloak.config.LoggingOptions.DEFAULT_SYSLOG_OUTPUT;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.keycloak.config.LoggingOptions;
import org.keycloak.quarkus.runtime.cli.PropertyException;
import org.keycloak.quarkus.runtime.configuration.ConfigArgsConfigSource;
import org.keycloak.quarkus.runtime.configuration.Configuration;

import io.smallrye.config.SmallRyeConfig;

public class LoggingConfigurationTest extends AbstractConfigurationTest {

    @Test
    public void logHandlerConfig() {
        ConfigArgsConfigSource.setCliArgs("--log=console,file");
        SmallRyeConfig config = createConfig();
        assertEquals("true", config.getConfigValue("quarkus.log.console.enable").getValue());
        assertEquals("true", config.getConfigValue("quarkus.log.file.enable").getValue());
        assertEquals("false", config.getConfigValue("quarkus.log.syslog.enable").getValue());

        ConfigArgsConfigSource.setCliArgs("--log=file");
        SmallRyeConfig config2 = createConfig();
        assertEquals("false", config2.getConfigValue("quarkus.log.console.enable").getValue());
        assertEquals("true", config2.getConfigValue("quarkus.log.file.enable").getValue());
        assertEquals("false", config2.getConfigValue("quarkus.log.syslog.enable").getValue());

        ConfigArgsConfigSource.setCliArgs("--log=console");
        SmallRyeConfig config3 = createConfig();
        assertEquals("true", config3.getConfigValue("quarkus.log.console.enable").getValue());
        assertEquals("false", config3.getConfigValue("quarkus.log.file.enable").getValue());
        assertEquals("false", config3.getConfigValue("quarkus.log.syslog.enable").getValue());

        ConfigArgsConfigSource.setCliArgs("--log=console,syslog");
        SmallRyeConfig config5 = createConfig();
        assertEquals("true", config5.getConfigValue("quarkus.log.console.enable").getValue());
        assertEquals("false", config5.getConfigValue("quarkus.log.file.enable").getValue());
        assertEquals("true", config5.getConfigValue("quarkus.log.syslog.enable").getValue());

        ConfigArgsConfigSource.setCliArgs("--log=syslog");
        SmallRyeConfig config6 = createConfig();
        assertEquals("false", config6.getConfigValue("quarkus.log.console.enable").getValue());
        assertEquals("false", config6.getConfigValue("quarkus.log.file.enable").getValue());
        assertEquals("true", config6.getConfigValue("quarkus.log.syslog.enable").getValue());
    }

    @Test
    public void syslogDefaults() {
        initConfig();

        assertConfig(Map.of(
                "log-syslog-enabled", "false",
                "log-syslog-endpoint", "localhost:514",
                "log-syslog-type", "rfc5424",
                "log-syslog-app-name", "keycloak",
                "log-syslog-protocol", "tcp",
                "log-syslog-format", DEFAULT_LOG_FORMAT,
                "log-syslog-output", DEFAULT_SYSLOG_OUTPUT.toString()
        ));
        assertThat(Configuration.getOptionalKcValue(LoggingOptions.LOG_SYSLOG_MAX_LENGTH).orElse(null), CoreMatchers.nullValue());

        assertExternalConfig(Map.of(
                "quarkus.log.syslog.enable", "false",
                "quarkus.log.syslog.endpoint", "localhost:514",
                "quarkus.log.syslog.syslog-type", "rfc5424",
                "quarkus.log.syslog.app-name", "keycloak",
                "quarkus.log.syslog.protocol", "tcp",
                "quarkus.log.syslog.format", DEFAULT_LOG_FORMAT,
                "quarkus.log.syslog.json.enabled", "false"
        ));

        // The default max-length attribute is set in the org.jboss.logmanager.handlers.SyslogHandler if not specified in config
        assertThat(Configuration.getOptionalValue("quarkus.log.syslog.max-length").orElse(null), CoreMatchers.nullValue());
    }

    @Test
    public void syslogDifferentValues() {
        putEnvVars(Map.of(
                "KC_LOG", "syslog",
                "KC_LOG_SYSLOG_ENDPOINT", "192.168.0.42:515",
                "KC_LOG_SYSLOG_TYPE", "rfc3164",
                "KC_LOG_SYSLOG_MAX_LENGTH", "4096",
                "KC_LOG_SYSLOG_APP_NAME", "keycloak2",
                "KC_LOG_SYSLOG_PROTOCOL", "udp",
                "KC_LOG_SYSLOG_FORMAT", "some format",
                "KC_LOG_SYSLOG_OUTPUT", "json"
        ));

        initConfig();

        assertConfig(Map.of(
                "log-syslog-enabled", "true",
                "log-syslog-endpoint", "192.168.0.42:515",
                "log-syslog-type", "rfc3164",
                "log-syslog-max-length", "4096",
                "log-syslog-app-name", "keycloak2",
                "log-syslog-protocol", "udp",
                "log-syslog-format", "some format",
                "log-syslog-output", "json"
        ));

        assertExternalConfig(Map.of(
                "quarkus.log.syslog.enable", "true",
                "quarkus.log.syslog.endpoint", "192.168.0.42:515",
                "quarkus.log.syslog.syslog-type", "rfc3164",
                "quarkus.log.syslog.max-length", "4096",
                "quarkus.log.syslog.app-name", "keycloak2",
                "quarkus.log.syslog.protocol", "udp",
                "quarkus.log.syslog.format", "some format",
                "quarkus.log.syslog.json.enabled", "true"
        ));
    }

    @Test
    public void syslogMaxLength() {
        // RFC3164
        putEnvVar("KC_LOG_SYSLOG_TYPE", "rfc3164");
        initConfig();

        assertConfig("log-syslog-type", "rfc3164");
        assertThat(Configuration.getOptionalKcValue(LoggingOptions.LOG_SYSLOG_MAX_LENGTH).orElse(null), CoreMatchers.nullValue());
        assertThat(Configuration.getOptionalValue("quarkus.log.syslog.max-length").orElse(null), CoreMatchers.nullValue());

        // RFC5424
        putEnvVar("KC_LOG_SYSLOG_TYPE", "rfc5424");
        initConfig();

        assertThat(Configuration.getOptionalKcValue(LoggingOptions.LOG_SYSLOG_MAX_LENGTH).orElse(null), CoreMatchers.nullValue());
        assertThat(Configuration.getOptionalValue("quarkus.log.syslog.max-length").orElse(null), CoreMatchers.nullValue());

        // Specific max-length
        removeEnvVar("KC_LOG_SYSLOG_TYPE");
        putEnvVar("KC_LOG_SYSLOG_MAX_LENGTH", "512");
        initConfig();

        assertConfig("log-syslog-max-length", "512");
        assertExternalConfig("quarkus.log.syslog.max-length", "512");
    }

    @Test
    public void logLevelsHandlers() {
        putEnvVars(Map.of(
                "KC_LOG_LEVEL", "debug",
                "KC_LOG_CONSOLE_LEVEL", "info",
                "KC_LOG_SYSLOG_LEVEL", "trace",
                "KC_LOG_FILE_LEVEL", "debug"
        ));

        initConfig();

        assertConfig(Map.of(
                "log-level", "debug",
                "log-console-level", "info",
                "log-syslog-level", "trace",
                "log-file-level", "debug"
        ));

        assertExternalConfig(Map.of(
                "quarkus.log.level", "DEBUG",
                "quarkus.log.console.level", "INFO",
                "quarkus.log.syslog.level", "TRACE",
                "quarkus.log.file.level", "DEBUG"
        ));
    }

    @Test
    public void logLevelTakesPrecedenceOverCategoryLevel() {
        ConfigArgsConfigSource.setCliArgs("--log-level=org.keycloak:error");
        SmallRyeConfig config = createConfig();
        assertEquals("INFO", config.getConfigValue("quarkus.log.level").getValue());
        assertEquals("ERROR", config.getConfigValue("quarkus.log.category.\"org.keycloak\".level").getValue());

        onAfter();
        ConfigArgsConfigSource.setCliArgs("--log-level=org.keycloak:error", "--log-level-org.keycloak=trace");
        config = createConfig();
        assertEquals("INFO", config.getConfigValue("quarkus.log.level").getValue());
        assertEquals("TRACE", config.getConfigValue("quarkus.log.category.\"org.keycloak\".level").getValue());
    }

    @Test
    public void unknownCategoryLevelIsResolvedFromRootLevel() {
        ConfigArgsConfigSource.setCliArgs("--log-level=warn,org.keycloak:error", "--log-level-org.keycloak=trace");
        SmallRyeConfig config = createConfig();
        assertEquals("WARN", config.getConfigValue("quarkus.log.level").getValue());
        assertEquals("TRACE", config.getConfigValue("quarkus.log.category.\"org.keycloak\".level").getValue());
        assertNull(config.getConfigValue("quarkus.log.category.\"foo.bar\".level").getValue());
    }

    @Test
    public void jsonDefaultFormat() {
        initConfig();

        assertConfig(Map.of(
                "log-console-json-format", "default",
                "log-file-json-format", "default",
                "log-syslog-json-format", "default"
        ));

        assertExternalConfig(Map.of(
                "quarkus.log.console.json.log-format", "default",
                "quarkus.log.file.json.log-format", "default",
                "quarkus.log.syslog.json.log-format", "default"
        ));
    }

    @Test
    public void jsonEcsFormat() {
        putEnvVars(Map.of(
                "KC_LOG_CONSOLE_OUTPUT", "json",
                "KC_LOG_CONSOLE_JSON_FORMAT", "ecs",
                "KC_LOG_FILE_OUTPUT", "json",
                "KC_LOG_FILE_JSON_FORMAT", "ecs",
                "KC_LOG_SYSLOG_OUTPUT", "json",
                "KC_LOG_SYSLOG_JSON_FORMAT", "ecs"
        ));

        initConfig();

        assertConfig(Map.of(
                "log-console-output", "json",
                "log-console-json-format", "ecs",
                "log-file-output", "json",
                "log-file-json-format", "ecs",
                "log-syslog-output", "json",
                "log-syslog-json-format", "ecs"
        ));

        assertExternalConfig(Map.of(
                "quarkus.log.console.json.enabled", "true",
                "quarkus.log.console.json.log-format", "ecs",
                "quarkus.log.file.json.enabled", "true",
                "quarkus.log.file.json.log-format", "ecs",
                "quarkus.log.syslog.json.enabled", "true",
                "quarkus.log.syslog.json.log-format", "ecs"
        ));
    }

    @Test
    public void testWildcardCliOptionCanBeMappedToQuarkusOption() {
        ConfigArgsConfigSource.setCliArgs("--log-level-org.keycloak=trace");
        SmallRyeConfig config = createConfig();
        assertEquals("TRACE", config.getConfigValue("quarkus.log.category.\"org.keycloak\".level").getValue());
        assertNull(config.getConfigValue("quarkus.log.category.\"io.quarkus\".level").getValue());
        assertNull(config.getConfigValue("quarkus.log.category.\"foo.bar\".level").getValue());
    }

    @Test
    public void testWildcardEnvVarOptionCanBeMappedToQuarkusOption() {
        putEnvVar("KC_LOG_LEVEL_IO_QUARKUS", "trace");
        SmallRyeConfig config = createConfig();
        // the default quarkus kc mapping should not be present
        Set<String> keys = StreamSupport.stream(config.getPropertyNames().spliterator(), false).collect(Collectors.toSet());
        assertFalse(keys.contains("kc.log.level.\"io.quarkus"));
        assertFalse(keys.contains("kc.log.level.io-quarkus"));
        // the default quarkus mapping should be
        assertNull(config.getConfigValue("quarkus.log.category.\"org.keycloak\".level").getValue());
        assertEquals("TRACE", config.getConfigValue("quarkus.log.category.\"io.quarkus\".level").getValue());
        assertTrue(keys.contains("kc.log-level-io.quarkus"));
        assertNull(config.getConfigValue("quarkus.log.category.\"foo.bar\".level").getValue());
    }

    @Test
    public void testWildcardOptionFromConfigFile() {
        putEnvVar("SOME_CATEGORY_LOG_LEVEL", "debug");
        SmallRyeConfig config = createConfig();
        assertEquals("DEBUG", config.getConfigValue("quarkus.log.category.\"io.k8s\".level").getValue());
    }


    @Test
    public void testLogLevelWithUnderscore() {
        ConfigArgsConfigSource.setCliArgs("--log-level=error,reproducer.not_ok:debug");
        SmallRyeConfig config = createConfig();
        assertEquals("DEBUG", config.getConfigValue("quarkus.log.category.\"reproducer.not_ok\".level").getValue());
        Set<String> keys = StreamSupport.stream(config.getPropertyNames().spliterator(), false).collect(Collectors.toSet());
        assertTrue(keys.contains("quarkus.log.category.\"reproducer.not_ok\".level"));
    }

    @Test(expected = PropertyException.class)
    public void testInvalidLogLevel() {
        ConfigArgsConfigSource.setCliArgs("--log-level=reproducer.not^ok:debug");
        createConfig();
    }

}
