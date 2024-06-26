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

package org.keycloak.quarkus.runtime.configuration;


import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.quarkus.runtime.configuration.ConfigBuilder;
import io.smallrye.config.SmallRyeConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;
import org.keycloak.quarkus.runtime.Environment;

public class KeycloakConfigSourceProvider implements ConfigSourceProvider, ConfigBuilder {

    private static final List<ConfigSource> CONFIG_SOURCES = new ArrayList<>();
    private static final Map<String, String> CONFIG_SOURCE_DISPLAY_NAMES = new HashMap<>();

    // we initialize in a static block to avoid discovering the config sources multiple times when starting the application
    static {
        initializeSources();
    }

    private static void initializeSources() {
        String profile = org.keycloak.common.util.Environment.getProfile();

        if (profile != null) {
            System.setProperty("quarkus.profile", profile);
        }

        addConfigSources("CLI", List.of(new ConfigArgsConfigSource()));

        addConfigSources("ENV", List.of(new KcEnvConfigSource()));

        addConfigSources("quarkus.properties", new QuarkusPropertiesConfigSource().getConfigSources(Thread.currentThread().getContextClassLoader()));

        addConfigSources("Persisted", List.of(PersistedConfigSource.getInstance()));

        KeycloakPropertiesConfigSource.InFileSystem inFileSystem = new KeycloakPropertiesConfigSource.InFileSystem();
        Path path = inFileSystem.getConfigurationFile();
        if (path != null) {
            addConfigSources(path.getFileName().toString(), inFileSystem.getConfigSources(Thread.currentThread().getContextClassLoader(), path));
        }

        // by enabling this config source we are able to rely on the default settings when running tests
        addConfigSources("classpath keycloak.conf", new KeycloakPropertiesConfigSource.InClassPath().getConfigSources(Thread.currentThread().getContextClassLoader()));
    }

    private static void addConfigSources(String displayName, Collection<ConfigSource> configSources) {
        for (ConfigSource cs : configSources) {
            CONFIG_SOURCES.add(cs);
            CONFIG_SOURCE_DISPLAY_NAMES.put(cs.getName(), displayName);
        }
    }

    /**
     * Mainly for test purposes as MicroProfile Config does not seem to provide a way to reload configsources when the config
     * is released
     */
    public static void reload() {
        CONFIG_SOURCES.clear();
        CONFIG_SOURCE_DISPLAY_NAMES.clear();
        initializeSources();
    }

    @Override
    public Iterable<ConfigSource> getConfigSources(ClassLoader forClassLoader) {
        if(Environment.isTestLaunchMode()) {
            reload();
        }
        return CONFIG_SOURCES;
    }

    @Override
    public SmallRyeConfigBuilder configBuilder(SmallRyeConfigBuilder builder) {
        return builder.withSources(CONFIG_SOURCES);
    }

    public static String getConfigSourceDisplayName(String configSource) {
        if (configSource == null) {
            return "Derived";
        }
        if (isKeyStoreConfigSource(configSource)) {
            return "config-keystore";
        }
        return CONFIG_SOURCE_DISPLAY_NAMES.getOrDefault(configSource, configSource);
    }

    public static boolean isKeyStoreConfigSource(String configSourceName) {
        return configSourceName.contains("KeyStoreConfigSource");
    }
}
