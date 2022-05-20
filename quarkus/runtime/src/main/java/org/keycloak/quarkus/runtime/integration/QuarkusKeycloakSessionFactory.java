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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.keycloak.Config;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.ProviderManagerRegistry;
import org.keycloak.provider.Spi;
import org.keycloak.services.DefaultKeycloakSessionFactory;
import org.keycloak.services.resources.admin.permissions.AdminPermissions;

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
    private final Boolean reaugmented;
    private final Map<Spi, Map<Class<? extends Provider>, Map<String, Class<? extends ProviderFactory>>>> factories;
    private Map<String, ProviderFactory> preConfiguredProviders;

    public QuarkusKeycloakSessionFactory(
            Map<Spi, Map<Class<? extends Provider>, Map<String, Class<? extends ProviderFactory>>>> factories,
            Map<Class<? extends Provider>, String> defaultProviders,
            Map<String, ProviderFactory> preConfiguredProviders,
            Boolean reaugmented) {
        this.provider = defaultProviders;
        this.factories = factories;
        this.preConfiguredProviders = preConfiguredProviders;
        this.reaugmented = reaugmented;
        serverStartupTimestamp = System.currentTimeMillis();
        spis = factories.keySet();

        for (Spi spi : spis) {
            for (Map<String, Class<? extends ProviderFactory>> factoryClazz : factories.get(spi).values()) {
                for (Map.Entry<String, Class<? extends ProviderFactory>> entry : factoryClazz.entrySet()) {
                    ProviderFactory factory = preConfiguredProviders.get(entry.getKey());

                    if (factory == null) {
                        factory = lookupProviderFactory(entry.getValue());
                    }

                    Config.Scope scope = Config.scope(spi.getName(), factory.getId());

                    factory.init(scope);
                    factoriesMap.computeIfAbsent(spi.getProviderClass(), k -> new HashMap<>()).put(factory.getId(), factory);
                }
            }
        }
    }

    private QuarkusKeycloakSessionFactory() {
        reaugmented = false;
        factories = Collections.emptyMap();
    }

    @Override
    public void init() {
        // Component factory must be initialized first, so that postInit in other factories can use component factories
        updateComponentFactoryProviderFactory();
        if (componentFactoryPF != null) {
            componentFactoryPF.postInit(this);
        }
        for (Map<String, ProviderFactory> f : factoriesMap.values()) {
            for (ProviderFactory factory : f.values()) {
                if (factory != componentFactoryPF) {
                    factory.postInit(this);
                }
            }
        }

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
}
