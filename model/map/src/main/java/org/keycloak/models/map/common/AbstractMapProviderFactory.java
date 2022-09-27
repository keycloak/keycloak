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
package org.keycloak.models.map.common;

import java.util.concurrent.atomic.AtomicInteger;
import org.keycloak.Config.Scope;
import org.keycloak.common.Profile;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelException;
import org.keycloak.models.map.storage.MapStorage;
import org.keycloak.models.map.storage.MapStorageProvider;
import org.keycloak.component.AmphibianProviderFactory;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.InvalidationHandler;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.jboss.logging.Logger;

/**
 *
 * @author hmlnarik
 */
public abstract class AbstractMapProviderFactory<T extends Provider, V extends AbstractEntity, M> implements AmphibianProviderFactory<T>, EnvironmentDependentProviderFactory {

    public static final String PROVIDER_ID = "map";

    public static final String CONFIG_STORAGE = "storage";

    protected final Logger LOG = Logger.getLogger(getClass());

    public static final AtomicInteger uniqueCounter = new AtomicInteger();
    private final String uniqueKey = getClass().getName() + uniqueCounter.incrementAndGet();

    protected final Class<M> modelType;
    private final Class<T> providerType;

    private Scope storageConfigScope;

    protected AbstractMapProviderFactory(Class<M> modelType, Class<T> providerType) {
        this.modelType = modelType;
        this.providerType = providerType;
    }

    public enum MapProviderObjectType implements InvalidationHandler.InvalidableObjectType {
        CLIENT_BEFORE_REMOVE,
        CLIENT_AFTER_REMOVE,
        CLIENT_SCOPE_BEFORE_REMOVE,
        CLIENT_SCOPE_AFTER_REMOVE,
        GROUP_BEFORE_REMOVE,
        GROUP_AFTER_REMOVE,
        REALM_BEFORE_REMOVE,
        REALM_AFTER_REMOVE,
        RESOURCE_SERVER_BEFORE_REMOVE,
        RESOURCE_SERVER_AFTER_REMOVE,
        ROLE_BEFORE_REMOVE,
        ROLE_AFTER_REMOVE,
        USER_BEFORE_REMOVE,
        USER_AFTER_REMOVE
    }

    /**
     * Creates new instance of a provider.
     *
     * @param session
     * @return See description.
     */
    public abstract T createNew(KeycloakSession session);

    /**
     * Returns instance of a provider. If the instance is already created within 
     * the session (it's found in session attributes), it's returned from there, 
     * otherwise new instance is created (and stored among the session attributes).
     *
     * @param session
     * @return See description.
     */
    @Override
    public T create(KeycloakSession session) {
        T provider = session.getAttribute(uniqueKey, providerType);
        if (provider != null) {
            return provider;
        }
        provider = createNew(session);
        session.setAttribute(uniqueKey, provider);
        return provider;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    protected MapStorage<V, M> getStorage(KeycloakSession session) {
        ProviderFactory<MapStorageProvider> storageProviderFactory = getProviderFactoryOrComponentFactory(session, storageConfigScope);
        final MapStorageProvider factory = storageProviderFactory.create(session);
        return factory.getStorage(modelType);
    }

    public static ProviderFactory<MapStorageProvider> getProviderFactoryOrComponentFactory(KeycloakSession session, Scope storageConfigScope) {
        ProviderFactory<MapStorageProvider> storageProviderFactory;
        if (!hasRealmSpecificStorage(session, storageConfigScope)) {
            String provider = storageConfigScope.get("provider");
            if (provider == null) {
                storageProviderFactory = session.getKeycloakSessionFactory().getProviderFactory(MapStorageProvider.class);
            } else {
                storageProviderFactory = session.getKeycloakSessionFactory().getProviderFactory(MapStorageProvider.class, provider);
            }
        } else {
            // If this is being implemented, make sure that the factory is being closed eventually.
            // When no cluster provider is available, the componentFactory will not be cached and a new instance is being returned all the time
            // when calling `getComponentFactory(session.getKeycloakSessionFactory(), MapStorageProvider.class, storageConfigScope, MapStorageSpi.NAME)`.
            throw new ModelException("not supported yet");
        }
        return storageProviderFactory;
    }

    private static boolean hasRealmSpecificStorage(KeycloakSession session, Scope storageConfigScope) {
        // Once there is functionality for a realm-specific storage, implement the logic on how to detect it here.
        return false;
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void init(Scope config) {
        // Implementation of the map storage SPI
        this.storageConfigScope = config.scope(CONFIG_STORAGE);
    }

    @Override
    public boolean isSupported() {
        return Profile.isFeatureEnabled(Profile.Feature.MAP_STORAGE);
    }
}
