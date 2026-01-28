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

package org.keycloak.quarkus.runtime.configuration;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.keycloak.config.LoggingOptions;
import org.keycloak.quarkus.runtime.Environment;
import org.keycloak.quarkus.runtime.cli.PropertyException;

import io.quarkus.runtime.logging.LogRuntimeConfig;
import io.smallrye.config.SmallRyeConfig;
import org.hamcrest.CoreMatchers;
import org.junit.Test;

import static org.keycloak.config.LoggingOptions.DEFAULT_LOG_FORMAT;
import static org.keycloak.config.LoggingOptions.DEFAULT_SYSLOG_OUTPUT;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class LoggingConfigurationTest extends AbstractConfigurationTest {

    @Test
    public void testDefaultLogColor() {
        SmallRyeConfig config = createConfig();
        assertNull(config.getConfigValue("kc.log-console-color").getValue());
        assertNotNull(config.getConfigValue("quarkus.console.color").getValue());
    }

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
                "log-syslog-output", DEFAULT_SYSLOG_OUTPUT.toString(),
                "log-syslog-counting-framing", "protocol-dependent"
        ));
        assertThat(Configuration.getOptionalKcValue(LoggingOptions.LOG_SYSLOG_MAX_LENGTH).orElse(null), CoreMatchers.nullValue());

        assertExternalConfig(Map.of(
                "quarkus.log.syslog.enable", "false",
                "quarkus.log.syslog.endpoint", "localhost:514",
                "quarkus.log.syslog.syslog-type", "rfc5424",
                "quarkus.log.syslog.app-name", "keycloak",
                "quarkus.log.syslog.protocol", "tcp",
                "quarkus.log.syslog.use-counting-framing", "protocol-dependent",
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
                "KC_LOG_SYSLOG_COUNTING_FRAMING", "false",
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
                "log-syslog-counting-framing", "false",
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
                "quarkus.log.syslog.use-counting-framing", "false",
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
    public void syslogCountingFraming() {
        assertSyslogCountingFraming(LogRuntimeConfig.SyslogConfig.CountingFraming.TRUE);
        assertSyslogCountingFraming(LogRuntimeConfig.SyslogConfig.CountingFraming.FALSE);
        assertSyslogCountingFraming(LogRuntimeConfig.SyslogConfig.CountingFraming.PROTOCOL_DEPENDENT);
    }

    protected void assertSyslogCountingFraming(LogRuntimeConfig.SyslogConfig.CountingFraming countingFraming) {
        putEnvVars(Map.of(
                "KC_LOG", "syslog",
                "KC_LOG_SYSLOG_COUNTING_FRAMING", countingFraming.toString()
        ));

        initConfig();

        assertConfig(Map.of(
                "log-syslog-enabled", "true",
                "log-syslog-counting-framing", countingFraming.toString()
        ));
        assertExternalConfig(Map.of(
                "quarkus.log.syslog.enable", "true",
                "quarkus.log.syslog.use-counting-framing", countingFraming.toString()
        ));
        onAfter();
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

    @Test
    public void testLogLevelWithUnderscoreEnv() {
        putEnvVar("KC_1", "debug");
        putEnvVar("KCKEY_1", "log-level-reproducer.not_ok");
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

    @Test
    public void testNestedBuildTimeLogging() {
        Environment.setRebuildCheck(true); // will be reset by the system properties logic
        ConfigArgsConfigSource.setCliArgs("");
        assertEquals("true", createConfig().getConfigValue("quarkus.log.console.enable").getValue());
    }

    @Test
    public void asyncDefaults() {
        initConfig();

        boolean defaultEnabled = false;
        int defaultQueueLength = 512;

        for (var handler : LoggingOptions.Handler.values()) {
            assertAsyncProperties(handler, defaultEnabled, defaultQueueLength);
        }
    }

    @Test
    public void asyncProperties() {
        boolean enabled = true;
        int queueLength = 1024;

        for (var handler : LoggingOptions.Handler.values()) {
            setAsyncProperties(handler, enabled, queueLength);
        }

        initConfig();

        for (var handler : LoggingOptions.Handler.values()) {
            assertAsyncProperties(handler, enabled, queueLength);
        }
    }

    @Test
    public void asyncPropertiesIndividual() {
        setAsyncProperties(LoggingOptions.Handler.console, true, 768);
        setAsyncProperties(LoggingOptions.Handler.file, false, 1523);
        setAsyncProperties(LoggingOptions.Handler.syslog, true, 888);

        initConfig();

        assertAsyncProperties(LoggingOptions.Handler.console, true, 768);
        assertAsyncProperties(LoggingOptions.Handler.file, false, 1523);
        assertAsyncProperties(LoggingOptions.Handler.syslog, true, 888);
    }

    @Test
    public void asyncGlobalProperty() {
        putEnvVar("KC_LOG_ASYNC", "true");

        initConfig();

        assertAsyncLoggingEnabled(LoggingOptions.Handler.console, true);
        assertAsyncLoggingEnabled(LoggingOptions.Handler.file, true);
        assertAsyncLoggingEnabled(LoggingOptions.Handler.syslog, true);

        onAfter();

        putEnvVar("KC_LOG_ASYNC", "false");

        initConfig();

        assertAsyncLoggingEnabled(LoggingOptions.Handler.console, false);
        assertAsyncLoggingEnabled(LoggingOptions.Handler.file, false);
        assertAsyncLoggingEnabled(LoggingOptions.Handler.syslog, false);
    }

    @Test
    public void asyncGlobalPropertyOverrides() {
        putEnvVar("KC_LOG_ASYNC", "true");
        setAsyncLoggingEnabled(LoggingOptions.Handler.console, false);
        initConfig();

        assertAsyncLoggingEnabled(LoggingOptions.Handler.console, false);
        assertAsyncLoggingEnabled(LoggingOptions.Handler.file, true);
        assertAsyncLoggingEnabled(LoggingOptions.Handler.syslog, true);

        setAsyncLoggingEnabled(LoggingOptions.Handler.file, false);
        initConfig();

        assertAsyncLoggingEnabled(LoggingOptions.Handler.console, false);
        assertAsyncLoggingEnabled(LoggingOptions.Handler.file, false);
        assertAsyncLoggingEnabled(LoggingOptions.Handler.syslog, true);

        setAsyncLoggingEnabled(LoggingOptions.Handler.file, false);
        initConfig();

        assertAsyncLoggingEnabled(LoggingOptions.Handler.console, false);
        assertAsyncLoggingEnabled(LoggingOptions.Handler.file, false);
        assertAsyncLoggingEnabled(LoggingOptions.Handler.syslog, true);

        onAfter();

        putEnvVar("KC_LOG_ASYNC", "false");
        setAsyncLoggingEnabled(LoggingOptions.Handler.console, true);
        initConfig();

        assertAsyncLoggingEnabled(LoggingOptions.Handler.console, true);
        assertAsyncLoggingEnabled(LoggingOptions.Handler.file, false);
        assertAsyncLoggingEnabled(LoggingOptions.Handler.syslog, false);

    }

    protected void setAsyncLoggingEnabled(LoggingOptions.Handler handler, Boolean enabled) {
        // default values
        setAsyncProperties(handler, enabled, 512);
    }

    protected void setAsyncProperties(LoggingOptions.Handler handler, Boolean enabled, Integer queueLength) {
        var handlerName = handler.name();
        putEnvVars(Map.of(
                "KC_LOG_%s_ASYNC".formatted(handlerName), enabled.toString(),
                "KC_LOG_%s_ASYNC_QUEUE_LENGTH".formatted(handlerName), queueLength.toString()
                ));
    }

    protected void assertAsyncLoggingEnabled(LoggingOptions.Handler handler, Boolean expectedEnabled) {
        var handlerName = handler.toString();
        assertConfig("log-%s-async".formatted(handlerName), expectedEnabled.toString());
        assertExternalConfig("quarkus.log.%s.async".formatted(handlerName), expectedEnabled.toString());
    }

    protected void assertAsyncProperties(LoggingOptions.Handler handler, Boolean enabled, Integer queueLength) {
        assertAsyncLoggingEnabled(handler, enabled);

        var handlerName = handler.toString();
        assertConfig("log-%s-async-queue-length".formatted(handlerName), queueLength.toString());
        assertExternalConfig("quarkus.log.%s.async.queue-length".formatted(handlerName), queueLength.toString());
    }

    // HTTP Access log
    @Test
    public void httpAccessLogDefaults() {
        initConfig();

        assertConfig(Map.of(
                "http-access-log-enabled", "false",
                "http-access-log-pattern", "common"
        ));
        assertConfigNull("http-access-log-exclude");

        assertExternalConfig(Map.of(
                "quarkus.http.access-log.enabled", "false",
                "quarkus.http.access-log.pattern", "common"
        ));
        assertExternalConfigNull("quarkus.http.access-log.exclude-pattern");
    }

    @Test
    public void httpAccessLogChanges() {
        ConfigArgsConfigSource.setCliArgs("--http-access-log-enabled=true", "--http-access-log-pattern=long", "--http-access-log-exclude=/realms/test/*");
        initConfig();

        assertConfig(Map.of(
                "http-access-log-enabled", "true",
                "http-access-log-pattern", "long",
                "http-access-log-exclude", "/realms/test/*"
        ));
        assertExternalConfig(Map.of(
                "quarkus.http.access-log.enabled", "true",
                "quarkus.http.access-log.pattern", "long",
                "quarkus.http.access-log.exclude-pattern", "/realms/test/*"
        ));
    }
}
