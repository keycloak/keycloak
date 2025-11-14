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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.quarkus.runtime.Environment;

import io.smallrye.config.AbstractLocationConfigSourceLoader;
import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.common.utils.ConfigSourceUtil;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;

import static org.keycloak.quarkus.runtime.configuration.MicroProfileConfigProvider.NS_KEYCLOAK_PREFIX;

/**
 * A configuration source for {@code keycloak.conf}.
 */
public class KeycloakPropertiesConfigSource extends AbstractLocationConfigSourceLoader {

    public static final int PROPERTIES_FILE_ORDINAL = 475;

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

    public static class InFileSystem extends KeycloakPropertiesConfigSource implements ConfigSourceProvider {

        @Override
        public List<ConfigSource> getConfigSources(final ClassLoader classLoader) {
            Path configFile = getConfigurationFile();

            if (configFile == null) {
                return Collections.emptyList();
            }

            return getConfigSources(classLoader, configFile);
        }

        public List<ConfigSource> getConfigSources(final ClassLoader classLoader, Path configFile) {
            return loadConfigSources(configFile.toUri().toString(), PROPERTIES_FILE_ORDINAL, classLoader);
        }

        @Override
        protected List<ConfigSource> tryClassPath(final URI uri, final int ordinal, final ClassLoader classLoader) {
            return Collections.emptyList();
        }

        public Path getConfigurationFile() {
            String filePath = System.getProperty(KEYCLOAK_CONFIG_FILE_PROP);

            if (filePath == null) {
                filePath = System.getenv(KEYCLOAK_CONFIG_FILE_ENV);
            }

            if (filePath == null) {
                filePath = Environment.getHomeDir()
                        .map(f -> Paths.get(f, "conf", KeycloakPropertiesConfigSource.KEYCLOAK_CONF_FILE).toFile())
                        .filter(File::exists).map(File::getAbsolutePath).orElse(null);
            }

            if (filePath == null) {
                return null;
            }

            return Paths.get(filePath);
        }
    }

    private static Map<String, String> transform(Map<String, String> properties) {
        Map<String, String> result = new HashMap<>(properties.size());

        properties.entrySet().forEach(entry -> {
            result.put(MicroProfileConfigProvider.NS_KEYCLOAK_PREFIX + entry.getKey(), entry.getValue());
        });

        return result;
    }

}
