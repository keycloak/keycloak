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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;
import org.keycloak.quarkus.runtime.Environment;
import org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper;
import org.keycloak.quarkus.runtime.configuration.mappers.PropertyMappers;

import io.smallrye.config.AbstractLocationConfigSourceLoader;
import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.common.utils.ConfigSourceUtil;

import static org.keycloak.common.util.StringPropertyReplacer.replaceProperties;
import static org.keycloak.quarkus.runtime.configuration.Configuration.getMappedPropertyName;
import static org.keycloak.quarkus.runtime.configuration.MicroProfileConfigProvider.NS_KEYCLOAK;
import static org.keycloak.quarkus.runtime.configuration.MicroProfileConfigProvider.NS_KEYCLOAK_PREFIX;
import static org.keycloak.quarkus.runtime.configuration.MicroProfileConfigProvider.NS_QUARKUS;

/**
 * A configuration source for {@code keycloak.conf}.
 */
public class KeycloakPropertiesConfigSource extends AbstractLocationConfigSourceLoader {

    private static final Pattern DOT_SPLIT = Pattern.compile("\\.");
    private static final String KEYCLOAK_CONFIG_FILE_ENV = "KC_CONFIG_FILE";
    private static final String KEYCLOAK_CONF_FILE = "keycloak.conf";
    public static final String KEYCLOAK_CONFIG_FILE_PROP = NS_KEYCLOAK_PREFIX + "config.file";

    @Override
    protected String[] getFileExtensions() {
        return new String[] { "conf" };
    }

    @Override
    protected ConfigSource loadConfigSource(URL url, int ordinal) throws IOException {
        return new PropertiesConfigSource(transform(ConfigSourceUtil.urlToMap(url)), url.toString(), ordinal);
    }

    public static class InClassPath extends KeycloakPropertiesConfigSource implements ConfigSourceProvider {

        @Override
        public List<ConfigSource> getConfigSources(final ClassLoader classLoader) {
            return loadConfigSources("META-INF/" + KEYCLOAK_CONF_FILE, 150, classLoader);
        }

        @Override
        protected List<ConfigSource> tryClassPath(URI uri, int ordinal, ClassLoader classLoader) {
            try {
                return super.tryClassPath(uri, ordinal, classLoader);
            } catch (RuntimeException e) {
                Throwable cause = e.getCause();

                if (cause instanceof NoSuchFileException) {
                    // configuration step happens before classpath is updated, and it might happen that
                    // provider JARs are still in classpath index but removed from the providers dir
                    return Collections.emptyList();
                }

                throw e;
            }
        }

        @Override
        protected List<ConfigSource> tryFileSystem(final URI uri, final int ordinal) {
            return Collections.emptyList();
        }
    }

    public static class InFileSystem extends KeycloakPropertiesConfigSource implements ConfigSourceProvider {

        @Override
        public List<ConfigSource> getConfigSources(final ClassLoader classLoader) {
            Path configFile = getConfigurationFile();

            if (configFile == null) {
                return Collections.emptyList();
            }

            return loadConfigSources(configFile.toUri().toString(), 450, classLoader);
        }

        @Override
        protected List<ConfigSource> tryClassPath(final URI uri, final int ordinal, final ClassLoader classLoader) {
            return Collections.emptyList();
        }

        private Path getConfigurationFile() {
            String filePath = System.getProperty(KEYCLOAK_CONFIG_FILE_PROP);

            if (filePath == null)
                filePath = System.getenv(KEYCLOAK_CONFIG_FILE_ENV);

            if (filePath == null) {
                String homeDir = Environment.getHomeDir();

                if (homeDir != null) {
                    File file = Paths.get(homeDir, "conf", KeycloakPropertiesConfigSource.KEYCLOAK_CONF_FILE).toFile();

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
    }

    private static Map<String, String> transform(Map<String, String> properties) {
        Map<String, String> result = new HashMap<>(properties.size());
        properties.keySet().forEach(k -> {
            String key = transformKey(k);
            PropertyMapper mapper = PropertyMappers.getMapper(key);

            //TODO: remove explicit checks for spi and feature options once we have proper support in our config mappers
            if (mapper != null
                    || key.contains(NS_KEYCLOAK_PREFIX + "spi")
                    || key.contains(NS_KEYCLOAK_PREFIX + "feature")) {
                String value = replaceProperties(properties.get(k));

                result.put(key, value);

                if (mapper != null && key.charAt(0) != '%') {
                    result.put(getMappedPropertyName(key), value);
                }
            }
        });

        return result;
    }

    /**
     * We need a better namespace resolution so that we don't need to add Quarkus extensions manually. Maybe the easiest 
     * path is to just have the "kc" namespace for Keycloak-specific properties.
     * 
     * @param key the key to transform
     * @return the same key but prefixed with the namespace
     */
    private static String transformKey(String key) {
        String namespace;
        String[] keyParts = DOT_SPLIT.split(key);
        String extension = keyParts[0];
        String profile = "";
        String transformed = key;

        if (extension.startsWith("%")) {
            profile = String.format("%s.", keyParts[0]);
            extension = keyParts[1];
            transformed = key.substring(key.indexOf('.') + 1);
        }

        if (extension.equalsIgnoreCase(NS_QUARKUS)) {
            return key;
        } else {
            namespace = NS_KEYCLOAK;
        }

        return profile + namespace + "." + transformed;

    }
}
