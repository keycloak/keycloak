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

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentFactoryProvider;
import org.keycloak.component.ComponentFactoryProviderFactory;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.InvalidationHandler;
import org.keycloak.provider.KeycloakDeploymentInfo;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderEvent;
import org.keycloak.provider.ProviderEventListener;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.ProviderManager;
import org.keycloak.provider.ProviderManagerDeployer;
import org.keycloak.provider.ProviderManagerRegistry;
import org.keycloak.provider.Spi;
import org.keycloak.services.resources.admin.permissions.AdminPermissions;
import org.keycloak.theme.DefaultThemeManagerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.stream.Stream;

public class DefaultKeycloakSessionFactory implements KeycloakSessionFactory, ProviderManagerDeployer {

    private static final Logger logger = Logger.getLogger(DefaultKeycloakSessionFactory.class);

    protected Set<Spi> spis = new HashSet<>();
    protected Map<Class<? extends Provider>, String> provider = new HashMap<>();
    protected volatile Map<Class<? extends Provider>, Map<String, ProviderFactory>> factoriesMap = new HashMap<>();
    protected CopyOnWriteArrayList<ProviderEventListener> listeners = new CopyOnWriteArrayList<>();

    private final DefaultThemeManagerFactory themeManagerFactory = new DefaultThemeManagerFactory();

    // TODO: Likely should be changed to int and use Time.currentTime() to be compatible with all our "time" reps
    protected long serverStartupTimestamp;

    /**
     * Timeouts are used as time boundary for obtaining models from an external storage. Default value is set
     * to 3000 milliseconds and it's configurable.
     */
    private Long clientStorageProviderTimeout;
    private Long roleStorageProviderTimeout;

    protected ComponentFactoryProviderFactory componentFactoryPF;
    
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

        ProviderManager pm = new ProviderManager(KeycloakDeploymentInfo.create().services(), getClass().getClassLoader(), Config.scope().getArray("providers"));
        for (Spi spi : pm.loadSpis()) {
            if (spi.isEnabled()) {
                spis.add(spi);
            }
        }

        factoriesMap = loadFactories(pm);

        synchronized (ProviderManagerRegistry.SINGLETON) {
            for (ProviderManager manager : ProviderManagerRegistry.SINGLETON.getPreBoot()) {
                Map<Class<? extends Provider>, Map<String, ProviderFactory>> factoryMap = loadFactories(manager);
                for (Map.Entry<Class<? extends Provider>, Map<String, ProviderFactory>> entry : factoryMap.entrySet()) {
                    Map<String, ProviderFactory> factories = factoriesMap.get(entry.getKey());
                    if (factories == null) {
                        factoriesMap.put(entry.getKey(), entry.getValue());
                    } else {
                        factories.putAll(entry.getValue());
                    }
                }
            }
            checkProvider();
            // Component factory must be initialized first, so that postInit in other factories can use component factories
            updateComponentFactoryProviderFactory();
            if (componentFactoryPF != null) {
                componentFactoryPF.postInit(this);
            }
            for (Map<String, ProviderFactory> factories : factoriesMap.values()) {
                for (ProviderFactory factory : factories.values()) {
                    if (factory != componentFactoryPF) {
                        factory.postInit(this);
                    }
                }
            }
            // make the session factory ready for hot deployment
            ProviderManagerRegistry.SINGLETON.setDeployer(this);
        }

        AdminPermissions.registerListener(this);
    }

    protected Map<Class<? extends Provider>, Map<String, ProviderFactory>> getFactoriesCopy() {
        Map<Class<? extends Provider>, Map<String, ProviderFactory>> copy = new HashMap<>();
        for (Map.Entry<Class<? extends Provider>, Map<String, ProviderFactory>> entry : factoriesMap.entrySet()) {
            Map<String, ProviderFactory> valCopy = new HashMap<>();
            valCopy.putAll(entry.getValue());
            copy.put(entry.getKey(), valCopy);
        }
        return copy;

    }

    @Override
    public void deploy(ProviderManager pm) {
        Map<Class<? extends Provider>, Map<String, ProviderFactory>> copy = getFactoriesCopy();
        Map<Class<? extends Provider>, Map<String, ProviderFactory>> newFactories = loadFactories(pm);
        List<ProviderFactory> deployed = new LinkedList<>();
        List<ProviderFactory> undeployed = new LinkedList<>();

        for (Map.Entry<Class<? extends Provider>, Map<String, ProviderFactory>> entry : newFactories.entrySet()) {
            Map<String, ProviderFactory> current = copy.get(entry.getKey());
            if (current == null) {
                copy.put(entry.getKey(), entry.getValue());
            } else {
                for (ProviderFactory f : entry.getValue().values()) {
                    deployed.add(f);
                    ProviderFactory old = current.remove(f.getId());
                    if (old != null) undeployed.add(old);
                }
                current.putAll(entry.getValue());
            }

        }
        factoriesMap = copy;
        // need to update the default provider map
        checkProvider();
        boolean cfChanged = false;
        for (ProviderFactory factory : undeployed) {
            invalidate(ObjectType.PROVIDER_FACTORY, factory.getClass());
            factory.close();
            cfChanged |= (componentFactoryPF == factory);
        }
        // Component factory must be initialized first, so that postInit in other factories can use component factories
        if (cfChanged) {
            updateComponentFactoryProviderFactory();
            if (componentFactoryPF != null) {
                componentFactoryPF.postInit(this);
            }
        }
        for (ProviderFactory factory : deployed) {
            if (factory != componentFactoryPF) {
                factory.postInit(this);
            }
        }

        if (pm.getInfo().hasThemes() || pm.getInfo().hasThemeResources()) {
            themeManagerFactory.clearCache();
        }
    }

    @Override
    public void undeploy(ProviderManager pm) {
        logger.debug("undeploy");
        // we make a copy to avoid concurrent access exceptions
        Map<Class<? extends Provider>, Map<String, ProviderFactory>> copy = getFactoriesCopy();
        MultivaluedHashMap<Class<? extends Provider>, ProviderFactory> factories = pm.getLoadedFactories();
        List<ProviderFactory> undeployed = new LinkedList<>();
        for (Map.Entry<Class<? extends Provider>, List<ProviderFactory>> entry : factories.entrySet()) {
            Map<String, ProviderFactory> registered = copy.get(entry.getKey());
            for (ProviderFactory factory : entry.getValue()) {
                undeployed.add(factory);
                logger.debugv("undeploying {0} of id {1}", factory.getClass().getName(), factory.getId());
                if (registered != null) {
                    registered.remove(factory.getId());
                }
            }
        }
        factoriesMap = copy;
        for (ProviderFactory factory : undeployed) {
            factory.close();
        }
    }

    protected DefaultThemeManagerFactory getThemeManagerFactory() {
        return themeManagerFactory;
    }

    protected void checkProvider() {
        // make sure to recreated the default providers map
        provider.clear();

        for (Spi spi : spis) {
            String defaultProvider = Config.getProvider(spi.getName());
            if (defaultProvider != null) {
                if (getProviderFactory(spi.getProviderClass(), defaultProvider) == null) {
                    throw new RuntimeException("Failed to find provider " + defaultProvider + " for " + spi.getName());
                }
            } else {
                Map<String, ProviderFactory> factories = factoriesMap.get(spi.getProviderClass());
                if (factories != null && factories.size() == 1) {
                    defaultProvider = factories.values().iterator().next().getId();
                }

                if (defaultProvider == null) {
                    Optional<ProviderFactory> highestPriority = factories.values().stream().max(Comparator.comparing(ProviderFactory::order));
                    if (highestPriority.isPresent() && highestPriority.get().order() > 0) {
                        defaultProvider = highestPriority.get().getId();
                    }
                }

                if (defaultProvider == null && factories.containsKey("default")) {
                    defaultProvider = "default";
                }
            }

            if (defaultProvider != null) {
                this.provider.put(spi.getProviderClass(), defaultProvider);
                logger.debugv("Set default provider for {0} to {1}", spi.getName(), defaultProvider);
            } else {
                logger.debugv("No default provider for {0}", spi.getName());
            }
        }
    }

    protected Map<Class<? extends Provider>, Map<String, ProviderFactory>> loadFactories(ProviderManager pm) {
        Map<Class<? extends Provider>, Map<String, ProviderFactory>> factoryMap = new HashMap<>();
        Set<Spi> spiList = spis;

        for (Spi spi : spiList) {

            Map<String, ProviderFactory> factories = new HashMap<String, ProviderFactory>();
            factoryMap.put(spi.getProviderClass(), factories);

            String provider = Config.getProvider(spi.getName());
            if (provider != null) {

                ProviderFactory factory = pm.load(spi, provider);
                if (factory == null) {
                    continue;
                }

                Config.Scope scope = Config.scope(spi.getName(), provider);
                if (isEnabled(factory, scope)) {
                    factory.init(scope);

                    if (spi.isInternal() && !isInternal(factory)) {
                        ServicesLogger.LOGGER.spiMayChange(factory.getId(), factory.getClass().getName(), spi.getName());
                    }

                    factories.put(factory.getId(), factory);

                    logger.debugv("Loaded SPI {0} (provider = {1})", spi.getName(), provider);
                }

            } else {
                for (ProviderFactory factory : pm.load(spi)) {
                    Config.Scope scope = Config.scope(spi.getName(), factory.getId());
                    if (isEnabled(factory, scope)) {
                        factory.init(scope);

                        if (spi.isInternal() && !isInternal(factory)) {
                            ServicesLogger.LOGGER.spiMayChange(factory.getId(), factory.getClass().getName(), spi.getName());
                        }
                        factories.put(factory.getId(), factory);
                    } else {
                        logger.debugv("SPI {0} provider {1} disabled", spi.getName(), factory.getId());
                    }
                }
            }
        }
        return factoryMap;
    }

    protected boolean isEnabled(ProviderFactory factory, Config.Scope scope) {
        if (!scope.getBoolean("enabled", true)) {
            return false;
        }
        if (factory instanceof EnvironmentDependentProviderFactory) {
            return ((EnvironmentDependentProviderFactory) factory).isSupported();
        }
        return true;
    }

    public KeycloakSession create() {
        KeycloakSession session =  new DefaultKeycloakSession(this);
        return session;
    }

    @Override
    public Set<Spi> getSpis() {
        return spis;
    }

    @Override
    public Spi getSpi(Class<? extends Provider> providerClass) {
        for (Spi spi : spis) {
            if (spi.getProviderClass().equals(providerClass)) return spi;
        }
        return null;
    }

    @Override
    public <T extends Provider> ProviderFactory<T> getProviderFactory(Class<T> clazz) {
         return getProviderFactory(clazz, provider.get(clazz));
    }

    @Override
    public <T extends Provider> ProviderFactory<T> getProviderFactory(Class<T> clazz, String id) {
        Map<String, ProviderFactory> map = factoriesMap.get(clazz);
        if (map == null) {
            return null;
        }
        return map.get(id);
    }

    @Override
    public <T extends Provider> ProviderFactory<T> getProviderFactory(Class<T> clazz, String realmId, String componentId, Function<KeycloakSessionFactory, ComponentModel> modelGetter) {
        return (this.componentFactoryPF == null)
          ? null
          : this.componentFactoryPF.getProviderFactory(clazz, realmId, componentId, modelGetter);
    }

    @Override
    public void invalidate(InvalidableObjectType type, Object... ids) {
        factoriesMap.values().stream()
          .map(Map::values)
          .flatMap(Collection::stream)
          .filter(InvalidationHandler.class::isInstance)
          .map(InvalidationHandler.class::cast)
          .forEach(ih -> ih.invalidate(type, ids));
    }

    @Override
    public Stream<ProviderFactory> getProviderFactoriesStream(Class<? extends Provider> clazz) {
        if (factoriesMap == null) return Stream.empty();
        Map<String, ProviderFactory> providerFactoryMap = factoriesMap.get(clazz);
        if (providerFactoryMap == null) return Stream.empty();
        return providerFactoryMap.values().stream();
    }

    <T extends Provider> Set<String> getAllProviderIds(Class<T> clazz) {
        Map<String, ProviderFactory> factoryMap = factoriesMap.get(clazz);
        if (factoryMap == null) {
            return Collections.emptySet();
        }
        Set<String> ids = new HashSet<>();
        for (ProviderFactory f : factoryMap.values()) {
            ids.add(f.getId());
        }
        return ids;
    }

    Class<? extends Provider> getProviderClass(String providerClassName) {
        for (Class<? extends Provider> clazz : factoriesMap.keySet()) {
            if (clazz.getName().equals(providerClassName)) {
                return clazz;
            }
        }
        return null;
    }

    public void close() {
        ProviderManagerRegistry.SINGLETON.setDeployer(null);
        for (Map<String, ProviderFactory> factories : factoriesMap.values()) {
            for (ProviderFactory factory : factories.values()) {
                factory.close();
            }
        }
    }

    protected boolean isInternal(ProviderFactory<?> factory) {
        String packageName = factory.getClass().getPackage().getName();
        return packageName.startsWith("org.keycloak") && !packageName.startsWith("org.keycloak.examples");
    }

    public long getClientStorageProviderTimeout() {
        if (clientStorageProviderTimeout == null) {
            clientStorageProviderTimeout = Config.scope("client").getLong("storageProviderTimeout", 3000L);
        }
        return clientStorageProviderTimeout;
    }

    public long getRoleStorageProviderTimeout() {
        if (roleStorageProviderTimeout == null) {
            roleStorageProviderTimeout = Config.scope("role").getLong("storageProviderTimeout", 3000L);
        }
        return roleStorageProviderTimeout;
    }

    /**
     * @return timestamp of Keycloak server startup
     */
    @Override
    public long getServerStartupTimestamp() {
        return serverStartupTimestamp;
    }

    protected void updateComponentFactoryProviderFactory() {
        this.componentFactoryPF = (ComponentFactoryProviderFactory) getProviderFactory(ComponentFactoryProvider.class);
    }

}
