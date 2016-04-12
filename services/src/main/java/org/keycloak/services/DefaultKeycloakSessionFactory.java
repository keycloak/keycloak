/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.services;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderEvent;
import org.keycloak.provider.ProviderEventListener;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.ProviderManager;
import org.keycloak.provider.Spi;
import org.keycloak.services.ServicesLogger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public class DefaultKeycloakSessionFactory implements KeycloakSessionFactory {

    private static final ServicesLogger logger = ServicesLogger.ROOT_LOGGER;

    private Set<Spi> spis = new HashSet<>();
    private Map<Class<? extends Provider>, String> provider = new HashMap<Class<? extends Provider>, String>();
    private Map<Class<? extends Provider>, Map<String, ProviderFactory>> factoriesMap = new HashMap<Class<? extends Provider>, Map<String, ProviderFactory>>();
    protected CopyOnWriteArrayList<ProviderEventListener> listeners = new CopyOnWriteArrayList<ProviderEventListener>();

    // TODO: Likely should be changed to int and use Time.currentTime() to be compatible with all our "time" reps
    protected long serverStartupTimestamp;

    @Override
    public void register(ProviderEventListener listener) {
        listeners.add(listener);
    }

    @Override
    public void unregister(ProviderEventListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void publish(ProviderEvent event) {
        for (ProviderEventListener listener : listeners) {
            listener.onEvent(event);
        }
    }

    public void init() {
        serverStartupTimestamp = System.currentTimeMillis();

        ProviderManager pm = new ProviderManager(getClass().getClassLoader(), Config.scope().getArray("providers"));

        ServiceLoader<Spi> load = ServiceLoader.load(Spi.class, getClass().getClassLoader());
        loadSPIs(pm, load);
        for ( Map<String, ProviderFactory> factories : factoriesMap.values()) {
            for (ProviderFactory factory : factories.values()) {
                factory.postInit(this);
            }
        }
    }

    protected void loadSPIs(ProviderManager pm, ServiceLoader<Spi> load) {
        for (Spi spi : load) {
            spis.add(spi);

            Map<String, ProviderFactory> factories = new HashMap<String, ProviderFactory>();
            factoriesMap.put(spi.getProviderClass(), factories);

            String provider = Config.getProvider(spi.getName());
            if (provider != null) {
                this.provider.put(spi.getProviderClass(), provider);

                ProviderFactory factory = pm.load(spi, provider);
                if (factory == null) {
                    throw new RuntimeException("Failed to find provider " + provider + " for " + spi.getName());
                }

                Config.Scope scope = Config.scope(spi.getName(), provider);
                factory.init(scope);

                if (spi.isInternal() && !isInternal(factory)) {
                    logger.spiMayChange(factory.getId(), factory.getClass().getName(), spi.getName());
                }

                factories.put(factory.getId(), factory);

                logger.debugv("Loaded SPI {0} (provider = {1})", spi.getName(), provider);
            } else {
                for (ProviderFactory factory : pm.load(spi)) {
                    Config.Scope scope = Config.scope(spi.getName(), factory.getId());
                    if (scope.getBoolean("enabled", true)) {
                        factory.init(scope);

                        if (spi.isInternal() && !isInternal(factory)) {
                            logger.spiMayChange(factory.getId(), factory.getClass().getName(), spi.getName());
                        }

                        factories.put(factory.getId(), factory);
                    } else {
                        logger.debugv("SPI {0} provider {1} disabled", spi.getName(), factory.getId());
                    }
                }

                if (factories.size() == 1) {
                    provider = factories.values().iterator().next().getId();
                    this.provider.put(spi.getProviderClass(), provider);

                    logger.debugv("Loaded SPI {0} (provider = {1})", spi.getName(), provider);
                } else {
                    logger.debugv("Loaded SPI {0} (providers = {1})", spi.getName(), factories.keySet());
                }
            }
        }
    }

    public KeycloakSession create() {
        return new DefaultKeycloakSession(this);
    }

    <T extends Provider> String getDefaultProvider(Class<T> clazz) {
        return provider.get(clazz);
    }

    @Override
    public Set<Spi> getSpis() {
        return spis;
    }

    @Override
    public <T extends Provider> ProviderFactory<T> getProviderFactory(Class<T> clazz) {
         return getProviderFactory(clazz, provider.get(clazz));
    }

    @Override
    public <T extends Provider> ProviderFactory<T> getProviderFactory(Class<T> clazz, String id) {
         return factoriesMap.get(clazz).get(id);
    }

    @Override
    public List<ProviderFactory> getProviderFactories(Class<? extends Provider> clazz) {
        List<ProviderFactory> list = new LinkedList<ProviderFactory>();
        if (factoriesMap == null) return list;
        Map<String, ProviderFactory> providerFactoryMap = factoriesMap.get(clazz);
        if (providerFactoryMap == null) return list;
        list.addAll(providerFactoryMap.values());
        return list;
    }

    <T extends Provider> Set<String> getAllProviderIds(Class<T> clazz) {
        Set<String> ids = new HashSet<String>();
        for (ProviderFactory f : factoriesMap.get(clazz).values()) {
            ids.add(f.getId());
        }
        return ids;
    }

    public void close() {
        for (Map<String, ProviderFactory> factories : factoriesMap.values()) {
            for (ProviderFactory factory : factories.values()) {
                factory.close();
            }
        }
    }

    private boolean isInternal(ProviderFactory<?> factory) {
        return factory.getClass().getPackage().getName().startsWith("org.keycloak");
    }

    /**
     * @return timestamp of Keycloak server startup
     */
    @Override
    public long getServerStartupTimestamp() {
        return serverStartupTimestamp;
    }

}
