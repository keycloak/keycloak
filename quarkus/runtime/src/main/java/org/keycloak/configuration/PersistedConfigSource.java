/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;

import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.common.utils.ConfigSourceUtil;
import org.keycloak.util.Environment;

/**
 * A {@link org.eclipse.microprofile.config.spi.ConfigSource} based on the configuration properties persisted into the server
 * image.
 */
public class PersistedConfigSource extends PropertiesConfigSource {

    public static final String NAME = "PersistedConfigSource";
    static final String KEYCLOAK_PROPERTIES = "persisted.properties";

    public PersistedConfigSource(Path file) {
        super(readProperties(file), "", 300);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Map<String, String> getProperties() {
        return Collections.emptyMap();
    }

    @Override
    public String getValue(String propertyName) {
        String value = super.getValue(propertyName);

        if (value != null) {
            return value;
        }

        if (propertyName.startsWith(MicroProfileConfigProvider.NS_KEYCLOAK_PREFIX)) {
            return super.getValue(PropertyMappers.toCLIFormat(propertyName));
        }

        return null;
    }

    private static Map<String, String> readProperties(Path path) {
        if (!Environment.isRebuild()) {
            File file = path.toFile();

            if (file.exists()) {
                try {
                    return ConfigSourceUtil.urlToMap(file.toURL());
                } catch (IOException e) {
                    throw new RuntimeException("Failed to load persisted properties from [" + file.getAbsolutePath() + ".", e);
                }
            }
        }

        return Collections.emptyMap();
    }
}
