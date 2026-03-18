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
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.keycloak.quarkus.runtime.Environment;

import io.smallrye.config.AbstractLocationConfigSourceLoader;
import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.common.utils.ConfigSourceUtil;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;

import static org.keycloak.quarkus.runtime.configuration.MicroProfileConfigProvider.NS_QUARKUS;

/**
 * A configuration source for {@code quarkus.properties}.
 */
public final class QuarkusPropertiesConfigSource extends AbstractLocationConfigSourceLoader implements ConfigSourceProvider {

    private static final String FILE_NAME = "quarkus.properties";
    public static final String NAME = "KcQuarkusPropertiesConfigSource";

    @Override
    protected String[] getFileExtensions() {
        return new String[] { "properties" };
    }

    @Override
    protected ConfigSource loadConfigSource(URL url, int ordinal) throws IOException {
        Map<String, String> map = ConfigSourceUtil.urlToMap(url);
        map.keySet().removeIf(k -> !k.startsWith(NS_QUARKUS));
        return new PropertiesConfigSource(map, NAME, ordinal);
    }

    @Override
    public synchronized List<ConfigSource> getConfigSources(final ClassLoader classLoader) {
        List<ConfigSource> configSources = new ArrayList<>();

        Environment.getHomeDir().map(p -> Paths.get(p, "conf", FILE_NAME).toFile()).filter(File::exists)
                .ifPresent(configFile -> configSources.addAll(loadConfigSources(configFile.toPath().toUri().toString(),
                        KeycloakPropertiesConfigSource.PROPERTIES_FILE_ORDINAL, classLoader)));

        return configSources;
    }

}
