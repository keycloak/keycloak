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
package org.keycloak.services;

import org.keycloak.Config;
import org.keycloak.Config.Scope;
import org.keycloak.cluster.ClusterProvider;
import org.keycloak.common.util.StackUtil;
import org.keycloak.component.ComponentFactoryProviderFactory;
import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentModelScope;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.provider.InvalidationHandler;
import org.keycloak.provider.InvalidationHandler.InvalidableObjectType;
import org.keycloak.provider.InvalidationHandler.ObjectType;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Stream;
import org.jboss.logging.Logger;

/**
 * @author hmlnarik
 */
public class DefaultComponentFactoryProviderFactory implements ComponentFactoryProviderFactory {

    private static final Logger LOG = Logger.getLogger(DefaultComponentFactoryProviderFactory.class);
    public static final String PROVIDER_ID = "default";

    private final AtomicReference<ConcurrentMap<String, ProviderFactory>> componentsMap = new AtomicReference<>(new ConcurrentHashMap<>());

    /**
     * Should an ID in the key be invalidated, it would invalidate also all the IDs in the values
     */
    private final ConcurrentMap<Object, Set<String>> dependentInvalidations = new ConcurrentHashMap<>();

    private KeycloakSessionFactory factory;
    private boolean componentCachingAvailable;
    private boolean componentCachingEnabled;
    private Boolean componentCachingForced;

    @Override
    public void init(Scope config) {
        this.componentCachingEnabled = config.getBoolean("cachingEnabled", true);
        this.componentCachingForced = config.getBoolean("cachingForced", false);
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        this.factory = factory;
        this.componentCachingAvailable = this.componentCachingEnabled && this.factory.getProviderFactory(ClusterProvider.class) != null;
        if (! componentCachingEnabled) {
            LOG.warn("Caching of components disabled by the configuration which may have performance impact.");
        } else if (! componentCachingAvailable) {
            if (Objects.equals(componentCachingForced, Boolean.TRUE)) {
                LOG.warn("Component caching forced even though no system-wide ClusterProviderFactory found. This would be only reliable in single-node deployment.");
                this.componentCachingAvailable = true;
            } else {
                LOG.warn("No system-wide ClusterProviderFactory found. Cannot send messages across cluster, thus disabling caching of components. Consider setting cachingForced option in single-node deployment.");
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Provider> ProviderFactory<T> getProviderFactory(Class<T> clazz, String realmId, String componentId, Function<KeycloakSessionFactory, ComponentModel> modelGetter) {
        ProviderFactory res = componentsMap.get().get(componentId);
        if (res != null) {
            LOG.tracef("Found cached ProviderFactory for %s in (%s, %s)", clazz, realmId, componentId);
            return res;
        }

        // Apply the expensive operation before putting it into the cache
        final ComponentModel cm;
        if (modelGetter == null) {
            LOG.debugf("Getting component configuration for component (%s, %s) from realm configuration", clazz, realmId, componentId);
            cm = KeycloakModelUtils.getComponentModel(factory, realmId, componentId);
        } else {
            LOG.debugf("Getting component configuration for component (%s, %s) via provided method", realmId, componentId);
            cm = modelGetter.apply(factory);
        }

        if (cm == null) {
            return null;
        }

        final String provider = cm.getProviderId();
        ProviderFactory<T> pf = provider == null
          ? factory.getProviderFactory(clazz)
          : factory.getProviderFactory(clazz, provider);

        if (pf == null) {   // Either not found or not enabled
            LOG.debugf("ProviderFactory for %s in (%s, %s) not found", clazz, realmId, componentId);
            return null;
        }

        final ProviderFactory newFactory;
        try {
            newFactory = pf.getClass().getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException ex) {
            LOG.warn("Cannot instantiate factory", ex);
            return null;
        }

        Scope scope = Config.scope(factory.getSpi(clazz).getName(), provider);
        ComponentModelScope configScope = new ComponentModelScope(scope, cm);

        ProviderFactory<T> providerFactory;
        if (this.componentCachingAvailable) {
            providerFactory = componentsMap.get().computeIfAbsent(componentId, cId -> initializeFactory(clazz, realmId, componentId, newFactory, configScope));
        } else {
            providerFactory = initializeFactory(clazz, realmId, componentId, newFactory, configScope);
        }
        return providerFactory;
    }

    @SuppressWarnings("unchecked")
    protected <T extends Provider> ProviderFactory<T> initializeFactory(Class<T> clazz, String realmId, String componentId, final ProviderFactory newFactory, ComponentModelScope configScope) {
        LOG.debugf("Initializing ProviderFactory for %s in (%s, %s)", clazz, realmId, componentId);

        newFactory.init(configScope);
        newFactory.postInit(factory);

        dependentInvalidations.computeIfAbsent(realmId, k -> ConcurrentHashMap.newKeySet()).add(componentId);
        dependentInvalidations.computeIfAbsent(newFactory.getClass(), k -> ConcurrentHashMap.newKeySet()).add(componentId);

        return newFactory;
    }

    @Override
    public void invalidate(InvalidableObjectType type, Object... ids) {
        if (LOG.isDebugEnabled()) {
            LOG.debugf("Invalidating %s: %s", type, Arrays.asList(ids));
        }
        LOG.tracef("invalidate(%s)%s", type, StackUtil.getShortStackTrace());

        if (type == ObjectType._ALL_) {
            final ConcurrentMap<String, ProviderFactory> cm = componentsMap.getAndSet(new ConcurrentHashMap<>());
            dependentInvalidations.clear();
            cm.values().forEach(ProviderFactory::close);
        } else if (type == ObjectType.COMPONENT) {
            Stream.of(ids)
              .map(componentsMap.get()::remove).filter(Objects::nonNull)
              .forEach(ProviderFactory::close);
            propagateInvalidation(componentsMap.get(), type, ids);
        } else if (type == ObjectType.REALM || type == ObjectType.PROVIDER_FACTORY) {
            Stream.of(ids)
              .map(dependentInvalidations::get).filter(Objects::nonNull).flatMap(Collection::stream)
              .map(componentsMap.get()::remove).filter(Objects::nonNull)
              .forEach(ProviderFactory::close);
            Stream.of(ids).forEach(dependentInvalidations::remove);
            propagateInvalidation(componentsMap.get(), type, ids);
        } else {
            propagateInvalidation(componentsMap.get(), type, ids);
        }
    }

    private void propagateInvalidation(ConcurrentMap<String, ProviderFactory> componentsMap, InvalidableObjectType type, Object[] ids) {
        componentsMap.values()
          .stream()
          .filter(InvalidationHandler.class::isInstance)
          .map(InvalidationHandler.class::cast)
          .forEach(ih -> ih.invalidate(type, ids));
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public void close() {
        componentsMap.get().values().forEach(ProviderFactory::close);
    }

}
