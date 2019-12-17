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

package org.keycloak.provider.quarkus;

import java.util.ArrayList;

import io.quarkus.runtime.configuration.DeploymentProfileConfigSource;
import io.quarkus.runtime.configuration.ExpandingConfigSource;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;

public class KeycloakConfigSourceProvider implements ConfigSourceProvider {

    private static final String KEYCLOAK_CONFIG_FILE_PROP = "keycloak.config.file";
    private static final String KEYCLOAK_CONFIG_FILE_ENV = "KEYCLOAK_CONFIG_FILE";

    @Override
    public Iterable<ConfigSource> getConfigSources(ClassLoader forClassLoader) {

        ArrayList<ConfigSource> sources = new ArrayList<>();

        sources.add(wrap(new KeycloakPropertiesConfigSource.InJar()));

        String fileName = System.getProperty(KEYCLOAK_CONFIG_FILE_PROP);

        if (fileName == null)
            fileName = System.getenv(KEYCLOAK_CONFIG_FILE_ENV);

        if (fileName != null)
            sources.add(wrap(new KeycloakPropertiesConfigSource.InFileSystem(fileName)));

        return sources;

    }

    private ConfigSource wrap(ConfigSource source) {
        return ExpandingConfigSource.wrapper(new ExpandingConfigSource.Cache())
            .compose(DeploymentProfileConfigSource.wrapper())
            .apply(source);
    }

}
