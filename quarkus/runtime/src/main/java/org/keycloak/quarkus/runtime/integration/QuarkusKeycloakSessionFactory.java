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

package org.keycloak.quarkus.runtime.integration;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.ProviderManagerRegistry;
import org.keycloak.provider.Spi;
import org.keycloak.quarkus.runtime.cli.Picocli;
import org.keycloak.quarkus.runtime.configuration.Configuration;
import org.keycloak.quarkus.runtime.configuration.PersistedConfigSource;
import org.keycloak.quarkus.runtime.configuration.mappers.PropertyMappers;
import org.keycloak.quarkus.runtime.themes.QuarkusJarThemeProviderFactory;
import org.keycloak.services.DefaultKeycloakSessionFactory;
import org.keycloak.services.resources.admin.permissions.AdminPermissions;
import org.keycloak.theme.ClasspathThemeProviderFactory;

public final class QuarkusKeycloakSessionFactory extends DefaultKeycloakSessionFactory {

    public static QuarkusKeycloakSessionFactory getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new QuarkusKeycloakSessionFactory();
        }

        return INSTANCE;
    }

    public static void setInstance(QuarkusKeycloakSessionFactory instance) {
        INSTANCE = instance;
    }

    private static QuarkusKeycloakSessionFactory INSTANCE;

    public QuarkusKeycloakSessionFactory(
            Map<Spi, Map<Class<? extends Provider>, Map<String, Class<? extends ProviderFactory>>>> factories,
            Map<Class<? extends Provider>, String> defaultProviders,
            Map<String, ProviderFactory> preConfiguredProviders,
            List<ClasspathThemeProviderFactory.ThemesRepresentation> themes) {
        this.provider = defaultProviders;
        serverStartupTimestamp = System.currentTimeMillis();
        spis = factories.keySet();

        Map<String, Spi> spiMapping = new HashMap<String, Spi>();

        for (Spi spi : spis) {
            spiMapping.put(Configuration.toDashCase(spi.getName()), spi);
            for (Map<String, Class<? extends ProviderFactory>> factoryClazz : factories.get(spi).values()) {
                for (Map.Entry<String, Class<? extends ProviderFactory>> entry : factoryClazz.entrySet()) {
                    ProviderFactory factory = preConfiguredProviders.get(entry.getKey());

                    if (factory == null) {
                        factory = lookupProviderFactory(entry.getValue());
                    }

                    if (factory instanceof QuarkusJarThemeProviderFactory) {
                        ((QuarkusJarThemeProviderFactory) factory).setThemes(themes);
                    }

                    Config.Scope scope = Config.scope(spi.getName(), factory.getId());

                    factory.init(scope);
                    factoriesMap.computeIfAbsent(spi.getProviderClass(), k -> new HashMap<>()).put(factory.getId(), factory);
                }
            }
        }

        if (Boolean.parseBoolean(PersistedConfigSource.getInstance().getValue(Configuration.KC_OPTIMIZED))) {
            Picocli.checkChangesInBuildOptions((key, oldValue, newValue) -> {
                if (newValue != null && key.startsWith(PropertyMappers.KC_SPI_PREFIX)) {
                    String spi;
                    if (key.endsWith("-provider")) {
                        spi = key.substring(PropertyMappers.KC_SPI_PREFIX.length(), key.length() - "-provider".length());
                    } else if (key.endsWith("-provider-default")) {
                        spi = key.substring(PropertyMappers.KC_SPI_PREFIX.length(), key.length() - "-provider-default".length());
                    } else if (key.endsWith("-enabled")) {
                        // linear scan of spi / provider combinations - seems like this information may be incomplete (if built as not enabled
                        // it won't be present
                        return;
                    }
                    // check if valid spi
                }
            });
        }
    }

    private QuarkusKeycloakSessionFactory() {
    }

    @Override
    public void init() {
        initProviderFactories();
        AdminPermissions.registerListener(this);
        // make the session factory ready for hot deployment
        ProviderManagerRegistry.SINGLETON.setDeployer(this);
    }

    private ProviderFactory lookupProviderFactory(Class<? extends ProviderFactory> factoryClazz) {
        ProviderFactory factory;

        try {
            factory = factoryClazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return factory;
    }

    @Override
    public KeycloakSession create() {
        return new QuarkusKeycloakSession(this);
    }
}
