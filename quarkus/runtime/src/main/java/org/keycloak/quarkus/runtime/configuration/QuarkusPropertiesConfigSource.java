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

import static java.lang.Boolean.parseBoolean;
import static org.keycloak.quarkus.runtime.configuration.Configuration.getRawPersistedProperty;
import static org.keycloak.quarkus.runtime.configuration.MicroProfileConfigProvider.NS_QUARKUS;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;
import org.keycloak.quarkus.runtime.Environment;

import io.smallrye.config.AbstractLocationConfigSourceLoader;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.common.utils.ConfigSourceUtil;

/**
 * A configuration source for {@code quarkus.properties}.
 */
public final class QuarkusPropertiesConfigSource extends AbstractLocationConfigSourceLoader implements ConfigSourceProvider {

    private static final String NAME = "QuarkusProperties";
    private static final String FILE_NAME = "quarkus.properties";
    public static final String QUARKUS_PROPERTY_ENABLED = "kc.quarkus-properties-enabled";

    public static boolean isSameSource(ConfigValue value) {
        if (value == null) {
            return false;
        }

        return NAME.equals(value.getConfigSourceName());
    }

    public static boolean isQuarkusPropertiesEnabled() {
        return parseBoolean(getRawPersistedProperty(QUARKUS_PROPERTY_ENABLED).orElse(Boolean.FALSE.toString()));
    }

    public static Path getConfigurationFile() {
        String homeDir = Environment.getHomeDir();

        if (homeDir != null) {
            File file = Paths.get(homeDir, "conf", FILE_NAME).toFile();

            if (file.exists()) {
                return file.toPath();
            }
        }

        return null;
    }

    @Override
    protected String[] getFileExtensions() {
        return new String[] { "properties" };
    }

    @Override
    protected ConfigSource loadConfigSource(URL url, int ordinal) throws IOException {
        return new PropertiesConfigSource(ConfigSourceUtil.urlToMap(url), FILE_NAME, ordinal) {
            @Override
            public String getName() {
                return NAME;
            }

            @Override
            public String getValue(String propertyName) {
                if (propertyName.startsWith(NS_QUARKUS)) {
                    String value = super.getValue(propertyName);

                    if (value == null) {
                        return PersistedConfigSource.getInstance().getValue(propertyName);
                    }

                    return value;
                }

                return null;
            }
        };
    }

    @Override
    public List<ConfigSource> getConfigSources(final ClassLoader classLoader) {
        List<ConfigSource> configSources = new ArrayList<>();

        configSources.addAll(loadConfigSources("META-INF/services/" + FILE_NAME, 450, classLoader));

        if (Environment.isRebuild() || Environment.isRebuildCheck()) {
            Path configFile = getConfigurationFile();

            if (configFile != null) {
                configSources.addAll(loadConfigSources(configFile.toUri().toString(), 500, classLoader));
            }
        }

        return configSources;
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
}
