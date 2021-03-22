/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.configuration;

import static org.keycloak.configuration.MicroProfileConfigProvider.NS_KEYCLOAK_PREFIX;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;
import org.jboss.logging.Logger;
import org.keycloak.platform.Platform;
import org.keycloak.util.Environment;

public class KeycloakConfigSourceProvider implements ConfigSourceProvider {

    private static final Logger log = Logger.getLogger(KeycloakConfigSourceProvider.class);

    public static final String KEYCLOAK_CONFIG_FILE_ENV = "KC_CONFIG_FILE";
    public static final String KEYCLOAK_CONFIG_FILE_PROP = NS_KEYCLOAK_PREFIX + "config.file";
    private static final List<ConfigSource> CONFIG_SOURCES = new ArrayList<>();
    public static PersistedConfigSource PERSISTED_CONFIG_SOURCE;

    // we initialize in a static block to avoid discovering the config sources multiple times when starting the application
    static {
        initializeSources();
    }

    private static void initializeSources() {
        String profile = Environment.getProfile();

        if (profile != null) {
            System.setProperty("quarkus.profile", profile);
        }

        CONFIG_SOURCES.add(new ConfigArgsConfigSource());
        PERSISTED_CONFIG_SOURCE = new PersistedConfigSource(getPersistedConfigFile());
        CONFIG_SOURCES.add(PERSISTED_CONFIG_SOURCE);
        CONFIG_SOURCES.add(new SysPropConfigSource());

        Path configFile = getConfigurationFile();

        if (configFile != null) {
            CONFIG_SOURCES.add(new KeycloakPropertiesConfigSource.InFileSystem(configFile));
        } else {
            log.debug("Loading the default server configuration");
            CONFIG_SOURCES.add(new KeycloakPropertiesConfigSource.InJar());
        }
    }

    /**
     * Mainly for test purposes as MicroProfile Config does not seem to provide a way to reload configsources when the config
     * is released
     */
    public static void reload() {
        CONFIG_SOURCES.clear();
        initializeSources();
    }

    private static Path getConfigurationFile() {
        String filePath = System.getProperty(KEYCLOAK_CONFIG_FILE_PROP);

        if (filePath == null)
            filePath = System.getenv(KEYCLOAK_CONFIG_FILE_ENV);

        if (filePath == null) {
            String homeDir = Environment.getHomeDir();

            if (homeDir != null) {
                File file = Paths.get(homeDir, "conf", KeycloakPropertiesConfigSource.KEYCLOAK_PROPERTIES).toFile();

                if (file.exists()) {
                    filePath = file.getAbsolutePath();
                }
            }
        }

        if (filePath == null) {
            return null;
        }
        
        return Paths.get(filePath);
    }

    public static Path getPersistedConfigFile() {
        String homeDir = Environment.getHomeDir();

        if (homeDir == null) {
            return Paths.get(Platform.getPlatform().getTmpDirectory().toString(), PersistedConfigSource.KEYCLOAK_PROPERTIES);
        }

        return Paths.get(homeDir, "conf", PersistedConfigSource.KEYCLOAK_PROPERTIES);
    }

    @Override
    public Iterable<ConfigSource> getConfigSources(ClassLoader forClassLoader) {
        return CONFIG_SOURCES;
    }
}
