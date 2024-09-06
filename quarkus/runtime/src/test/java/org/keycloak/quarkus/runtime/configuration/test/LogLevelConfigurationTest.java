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

import org.hamcrest.CoreMatchers;
import org.jboss.logmanager.Level;
import org.jboss.logmanager.LogContext;
import org.junit.Test;
import org.keycloak.quarkus.runtime.cli.PropertyException;
import org.keycloak.quarkus.runtime.configuration.ConfigArgsConfigSource;
import org.keycloak.quarkus.runtime.configuration.mappers.FeaturePropertyMappers;
import org.keycloak.quarkus.runtime.configuration.mappers.LoggingPropertyMappers;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;

public class LogLevelConfigurationTest extends AbstractConfigurationTest {

    @Test
    public void finestRootInfoSyslogDebug() {
        ConfigArgsConfigSource.setCliArgs("--log=console,syslog", "--log-level=info", "--log-syslog-level=debug");
        initConfig();

        assertConfig(Map.of(
                "log-level", "info",
                "log-syslog-level", "debug"
        ));

        assertExternalConfig("quarkus.log.level", "debug");
    }

    @Test
    public void finestRootEmptySyslogTrace() {
        ConfigArgsConfigSource.setCliArgs("--log=console,syslog", "--log-syslog-level=trace");
        initConfig();

        assertConfig(Map.of(
                "log-level", "info",
                "log-syslog-level", "trace"
        ));

        assertExternalConfig("quarkus.log.level", "trace");
    }

    @Test
    public void finestRootDebugSyslogInfo() {
        ConfigArgsConfigSource.setCliArgs("--log=console,syslog", "--log-level=debug", "--log-syslog-level=info");
        initConfig();

        assertConfig(Map.of(
                "log-level", "debug",
                "log-syslog-level", "info"
        ));

        assertExternalConfig("quarkus.log.level", "debug");
    }

    @Test
    public void finestRootTraceCategory() {
        ConfigArgsConfigSource.setCliArgs("--log=console,syslog", "--log-level=org.keycloak:debug");
        initConfig();

        assertConfig("log-level", "org.keycloak:debug");
        assertExternalConfig("quarkus.log.level", "info");

        assertThat(LogContext.getLogContext().getLogger("org.keycloak").getLevel(), CoreMatchers.is(Level.DEBUG));
    }

    @Test
    public void finestAllLevels() {
        ConfigArgsConfigSource.setCliArgs("--log-level=warn", "--log-console-level=error", "--log-file-level=trace", "--log-syslog-level=debug");
        initConfig();

        assertConfig(Map.of(
                "log-level", "warn",
                "log-console-level", "error",
                "log-file-level", "trace",
                "log-syslog-level", "debug"
        ));

        assertExternalConfig("quarkus.log.level", "trace");
    }

    @Test
    public void rootDefault() {
        ConfigArgsConfigSource.setCliArgs("--log-level=warn", "--log=console,file,syslog");
        initConfig();

        assertConfig(Map.of(
                "log-level", "warn",
                "log-console-level", "warn",
                "log-file-level", "warn",
                "log-syslog-level", "warn"
        ));

        assertExternalConfig("quarkus.log.level", "warn");
    }

    @Test
    public void multipleCategoriesOverrides() {
        ConfigArgsConfigSource.setCliArgs("--log=console,syslog", "--log-level=org.keycloak:debug",
                "--log-console-level=org.keycloak:trace",
                "--log-syslog-level=org.keycloak:off",
                "--log-file-level=org.keycloak.timer:debug");
        initConfig();

        assertConfig(Map.of(
                "log-level", "org.keycloak:debug",
                "log-console-level", "org.keycloak:trace",
                "log-syslog-level", "org.keycloak:off",
                "log-file-level", "org.keycloak.timer:debug"
        ));
        assertExternalConfig("quarkus.log.level", "info");

        // set finest level
        var logContext = LogContext.getLogContext();
        assertThat(logContext.getLogger("org.keycloak").getLevel(), CoreMatchers.is(Level.TRACE));
        assertThat(logContext.getLogger("org.keycloak.timer").getLevel(), CoreMatchers.is(Level.DEBUG));
    }
}
