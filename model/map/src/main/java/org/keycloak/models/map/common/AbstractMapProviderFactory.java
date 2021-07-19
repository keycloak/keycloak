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

import org.keycloak.Config.Scope;
import org.keycloak.common.Profile;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.map.storage.MapStorage;
import org.keycloak.models.map.storage.MapStorageProvider;
import org.keycloak.models.map.storage.MapStorageSpi;
import org.keycloak.component.AmphibianProviderFactory;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.jboss.logging.Logger;
import static org.keycloak.models.utils.KeycloakModelUtils.getComponentFactory;

/**
 *
 * @author hmlnarik
 */
public abstract class AbstractMapProviderFactory<T extends Provider, V extends AbstractEntity, M> implements AmphibianProviderFactory<T>, EnvironmentDependentProviderFactory {

    public static final String PROVIDER_ID = "map";

    public static final String CONFIG_STORAGE = "storage";

    protected final Logger LOG = Logger.getLogger(getClass());

    protected final Class<M> modelType;

    private Scope storageConfigScope;

    @SuppressWarnings("unchecked")
    protected AbstractMapProviderFactory(Class<M> modelType) {
        this.modelType = modelType;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    protected MapStorage<V, M> getStorage(KeycloakSession session) {
        ProviderFactory<MapStorageProvider> storageProviderFactory = getComponentFactory(session.getKeycloakSessionFactory(),
          MapStorageProvider.class, storageConfigScope, MapStorageSpi.NAME);
        final MapStorageProvider factory = storageProviderFactory.create(session);

        return factory.getStorage(modelType);
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
