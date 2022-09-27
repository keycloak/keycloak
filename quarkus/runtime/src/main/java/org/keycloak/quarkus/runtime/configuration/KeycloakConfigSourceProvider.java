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


import java.util.ArrayList;
import java.util.List;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;
import org.keycloak.quarkus.runtime.Environment;

public class KeycloakConfigSourceProvider implements ConfigSourceProvider {

    private static final List<ConfigSource> CONFIG_SOURCES = new ArrayList<>();

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
        CONFIG_SOURCES.add(new KcEnvConfigSource());

        CONFIG_SOURCES.addAll(new QuarkusPropertiesConfigSource().getConfigSources(Thread.currentThread().getContextClassLoader()));

        CONFIG_SOURCES.add(PersistedConfigSource.getInstance());

        CONFIG_SOURCES.addAll(new KeycloakPropertiesConfigSource.InFileSystem().getConfigSources(Thread.currentThread().getContextClassLoader()));

        // by enabling this config source we are able to rely on the default settings when running tests
        CONFIG_SOURCES.addAll(new KeycloakPropertiesConfigSource.InClassPath().getConfigSources(Thread.currentThread().getContextClassLoader()));
    }

    /**
     * Mainly for test purposes as MicroProfile Config does not seem to provide a way to reload configsources when the config
     * is released
     */
    public static void reload() {
        CONFIG_SOURCES.clear();
        initializeSources();
    }

    @Override
    public Iterable<ConfigSource> getConfigSources(ClassLoader forClassLoader) {
        if(Environment.isTestLaunchMode()) {
            reload();
        }
        return CONFIG_SOURCES;
    }
}
