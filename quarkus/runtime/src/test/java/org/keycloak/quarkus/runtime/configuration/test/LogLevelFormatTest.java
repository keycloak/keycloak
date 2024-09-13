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

import org.junit.Test;
import org.keycloak.config.FeatureOptions;
import org.keycloak.quarkus.runtime.cli.PropertyException;
import org.keycloak.quarkus.runtime.configuration.ConfigArgsConfigSource;
import org.keycloak.quarkus.runtime.configuration.LogLevelFormat;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;

public class LogLevelFormatTest extends AbstractConfigurationTest {

    @Test
    public void nonExistingProperty() {
        var format = LogLevelFormat.parseFromProperty("not-existing");
        assertThat(format, notNullValue());
        assertThat(format.getRootLevel().isEmpty(), is(true));
        assertThat(format.getCategories(), is(Collections.emptyMap()));
    }

    @Test
    public void notLogLevelFormatProperty() {
        ConfigArgsConfigSource.setCliArgs("--features=opentelemetry");
        initConfig();

        var ex = assertThrows(PropertyException.class, () -> LogLevelFormat.parseFromProperty(FeatureOptions.FEATURES));
        assertThat(ex.getMessage(), is("Invalid log category format: opentelemetry. The format is 'category:level' such as 'org.keycloak:debug'."));
    }

    @Test
    public void parseValue() {
        assertLogLevelFormatValue("all", org.jboss.logmanager.Level.ALL);
        assertLogLevelFormatValue("fatal", org.jboss.logmanager.Level.FATAL);
        assertLogLevelFormatValue("error", org.jboss.logmanager.Level.ERROR);
        assertLogLevelFormatValue("warn", org.jboss.logmanager.Level.WARN);
        assertLogLevelFormatValue("info", org.jboss.logmanager.Level.INFO);
        assertLogLevelFormatValue("debug", org.jboss.logmanager.Level.DEBUG);
        assertLogLevelFormatValue("trace", org.jboss.logmanager.Level.TRACE);
        assertLogLevelFormatValue("off", org.jboss.logmanager.Level.OFF);

        // even the java.util.logging levels are accepted
        assertLogLevelFormatValue("finest", Level.FINEST);
        assertLogLevelFormatValue("finer", Level.FINER);
        assertLogLevelFormatValue("severe", Level.SEVERE);

        // last occurrence takes precedence
        assertLogLevelFormatValue("debug,info", Level.INFO);
        assertLogLevelFormatValue("debug,debug", org.jboss.logmanager.Level.DEBUG);

        assertLogLevelFormatValue("warn,org.keycloak:debug,org.keycloak.timer:trace", org.jboss.logmanager.Level.WARN, Map.of(
                "org.keycloak", org.jboss.logmanager.Level.DEBUG,
                "org.keycloak.timer", org.jboss.logmanager.Level.TRACE)
        );

        assertLogLevelFormatValue("all,org.keycloak:trace,org.keycloak:debug", org.jboss.logmanager.Level.ALL, Map.of(
                "org.keycloak", org.jboss.logmanager.Level.DEBUG)
        );
    }

    @Test
    public void logValidation() {
        var ex = assertThrows(PropertyException.class, () -> assertLogLevelFormatValue("all,org.keycloak:bad", Level.ALL));
        assertThat(ex.getMessage(), is("Invalid log category format: org.keycloak:bad. The format is 'category:level' such as 'org.keycloak:debug'."));

        ex = assertThrows(PropertyException.class, () -> assertLogLevelFormatValue("org.keycloak.bad", Level.ALL));
        assertThat(ex.getMessage(), is("Invalid log category format: org.keycloak.bad. The format is 'category:level' such as 'org.keycloak:debug'."));

        ex = assertThrows(PropertyException.class, () -> assertLogLevelFormatValue("org.keycloak:trace,io.quarkus:trace;", Level.ALL));
        assertThat(ex.getMessage(), is("Invalid log category format: io.quarkus:trace;. The format is 'category:level' such as 'org.keycloak:debug'."));
    }

    @Test
    public void levelConversions() {
        assertLogLevelFormatValue("org.keycloak:severe,io.quarkus:finer", Optional.empty(), Map.of(
                "org.keycloak", Level.SEVERE,
                "io.quarkus", Level.FINER
        ));
    }

    protected void assertLogLevelFormatValue(String value, Level level) {
        assertLogLevelFormatValue(value, Optional.of(level), Collections.emptyMap());
    }

    protected void assertLogLevelFormatValue(String value, Level level, Map<String, Level> categories) {
        assertLogLevelFormatValue(value, Optional.of(level), categories);
    }

    protected void assertLogLevelFormatValue(String value, Optional<Level> level, Map<String, Level> categories) {
        var format = LogLevelFormat.parseValue(value);
        assertThat(format, is(notNullValue()));
        assertThat(format.getRootLevel(), is(level));
        assertThat(format.getCategories(), is(categories));
    }
}
