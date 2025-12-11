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

import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;

import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.quarkus.runtime.Environment;
import org.keycloak.quarkus.runtime.cli.ExecutionExceptionHandler;
import org.keycloak.quarkus.runtime.configuration.mappers.PropertyMappers;

import io.smallrye.config.ConfigValue;
import io.smallrye.config.ConfigValue.ConfigValueBuilder;
import io.smallrye.config.SmallRyeConfig;
import org.junit.After;
import org.junit.BeforeClass;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public abstract class AbstractConfigurationTest {

    private static final Properties SYSTEM_PROPERTIES = (Properties) System.getProperties().clone();

    public static void putEnvVar(String name, String value) {
        assertNotNull(name);
        assertNotNull(value);
        KcEnvConfigSource.ENV_OVERRIDE.put(name, value);
    }

    public static void putEnvVars(Map<String, String> map) {
        map.forEach(ConfigurationTest::putEnvVar);
    }

    @SuppressWarnings("unchecked")
    public static void removeEnvVar(String name) {
        assertTrue(KcEnvConfigSource.ENV_OVERRIDE.remove(name) != null);
    }

    public static void setSystemProperty(String key, String value, Runnable runnable) {
        System.setProperty(key, value);
        try {
            runnable.run();
        } finally {
            System.clearProperty(key);
        }
    }

    @BeforeClass
    public static void resetConfiguration() {
        System.setProperties((Properties) SYSTEM_PROPERTIES.clone());
        Environment.setHomeDir(Paths.get("src/test/resources/"));

        KcEnvConfigSource.ENV_OVERRIDE.clear();

        PropertyMappers.reset();
        ConfigArgsConfigSource.setCliArgs();
        PersistedConfigSource.getInstance().getConfigValueProperties().clear();
        Profile.reset();
        Configuration.resetConfig();
        ExecutionExceptionHandler.resetExceptionTransformers();
    }

    @After
    public void onAfter() {
        resetConfiguration();
    }

    protected static Config.Scope initConfig(String... scope) {
        Config.init(new MicroProfileConfigProvider(createConfig()));
        return Config.scope(scope);
    }

    static protected SmallRyeConfig createConfig() {
        Configuration.resetConfig();
        KeycloakConfigSourceProvider.reload();
        Environment.getCurrentOrCreateFeatureProfile();
        return Configuration.getConfig();
    }

    protected void assertConfig(String key, String expectedValue, boolean isExternal) {
        Function<String, ConfigValue> getConfig = isExternal ? Configuration::getConfigValue : Configuration::getKcConfigValue;
        var value = getConfig.apply(key).getValue();
        assertThat(String.format("Value is null for key '%s'", key), value, notNullValue());
        assertThat(String.format("Different value for key '%s'", key), value, is(expectedValue));
    }

    protected void assertConfig(String key, String expectedValue) {
        assertConfig(key, expectedValue, false);
    }

    protected void assertConfig(Map<String, String> expectedValues) {
        expectedValues.forEach(this::assertConfig);
    }

    protected void assertExternalConfig(String key, String expectedValue) {
        assertConfig(key, expectedValue, true);
    }

    protected void assertExternalConfig(Map<String, String> expectedValues) {
        expectedValues.forEach(this::assertExternalConfig);
    }

    protected void assertConfigNull(String key, boolean isExternal) {
        Function<String, ConfigValue> getConfig = isExternal ? Configuration::getConfigValue : Configuration::getKcConfigValue;
        assertThat(String.format("We expect that the value is null for key '%s'", key), getConfig.apply(key).getValue(), nullValue());
    }

    protected void assertConfigNull(String key) {
        assertConfigNull(key, false);
    }

    protected void assertExternalConfigNull(String key) {
        assertConfigNull(key, true);
    }

    protected static void addPersistedConfigValues(Map<String, String> values) {
        var configValueProps = PersistedConfigSource.getInstance().getConfigValueProperties();
        values.forEach((k, v) -> configValueProps.put(k,
                new ConfigValueBuilder().withName(k).withValue(v).withRawValue(v)
                        .withConfigSourceName(PersistedConfigSource.getInstance().getName())
                        .withConfigSourceOrdinal(PersistedConfigSource.getInstance().getOrdinal()).build()));
    }
}
