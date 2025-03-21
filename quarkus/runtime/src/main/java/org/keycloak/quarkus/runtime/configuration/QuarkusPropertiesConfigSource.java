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
import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.common.utils.ConfigSourceUtil;

/**
 * A configuration source for {@code quarkus.properties}.
 */
public final class QuarkusPropertiesConfigSource extends AbstractLocationConfigSourceLoader implements ConfigSourceProvider {

    private static final String FILE_NAME = "quarkus.properties";
    public static final String NAME = "KcQuarkusPropertiesConfigSource";

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

    private boolean loadingFile;

    @Override
    protected String[] getFileExtensions() {
        return new String[] { "properties" };
    }

    @Override
    protected ConfigSource loadConfigSource(URL url, int ordinal) throws IOException {
        String name = loadingFile ? NAME : (NAME + " " + url);
        return new PropertiesConfigSource(ConfigSourceUtil.urlToMap(url), name, ordinal) {
            @Override
            public String getValue(String propertyName) {
                if (propertyName.startsWith(NS_QUARKUS)) {
                    return super.getValue(propertyName);
                }

                return null;
            }
        };
    }

    @Override
    public synchronized List<ConfigSource> getConfigSources(final ClassLoader classLoader) {
        List<ConfigSource> configSources = new ArrayList<>();

        configSources.addAll(loadConfigSources("META-INF/services/" + FILE_NAME, 450, classLoader));

        Path configFile = getConfigurationFile();

        if (configFile != null) {
            loadingFile = true;
            try {
                configSources.addAll(loadConfigSources(configFile.toUri().toString(), KeycloakPropertiesConfigSource.PROPERTIES_FILE_ORDINAL, classLoader));
            } finally {
                loadingFile = false;
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
