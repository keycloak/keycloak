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

package org.keycloak.quarkus.runtime.configuration.test;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.keycloak.quarkus.runtime.configuration.ConfigArgsConfigSource;
import org.keycloak.quarkus.runtime.configuration.KeycloakConfigSourceProvider;

public class TestKeycloakConfigSourceProvider extends KeycloakConfigSourceProvider {

    private static final Map<Class<? extends ConfigSource>, Supplier<? extends ConfigSource>> REPLACEABLE_CONFIG_SOURCES = new HashMap<>();

    static {
        REPLACEABLE_CONFIG_SOURCES.put(ConfigArgsConfigSource.class, TestConfigArgsConfigSource::new);
    }

    @Override
    public Iterable<ConfigSource> getConfigSources(ClassLoader forClassLoader) {
        reload();
        return StreamSupport.stream(super.getConfigSources(forClassLoader).spliterator(), false)
                .map(new Function<ConfigSource, ConfigSource>() {
                    @Override
                    public ConfigSource apply(ConfigSource configSource) {
                        return REPLACEABLE_CONFIG_SOURCES.getOrDefault(configSource.getClass(), () -> configSource).get();
                    }
                }).collect(Collectors.toList());
    }
}
