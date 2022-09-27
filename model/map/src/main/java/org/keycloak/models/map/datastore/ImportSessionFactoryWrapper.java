/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.map.datastore;

import org.keycloak.Config;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.map.storage.MapStorageProvider;
import org.keycloak.models.map.storage.MapStorageSpi;
import org.keycloak.models.map.storage.chm.ConcurrentHashMapStorageProviderFactory;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderEvent;
import org.keycloak.provider.ProviderEventListener;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * This wraps an existing KeycloakSessionFactory and redirects all calls to a {@link MapStorageProvider} to
 * {@link org.keycloak.models.map.storage.chm.ConcurrentHashMapStorageProvider}. This allows all operations to
 * be in-memory. The final contents of the store can then be copied over to the final store once the import is complete.
 * <p/>
 * For this to work, the CHM provider needs to be registered as a provider.
 *
 * @author Alexander Schwartz
 */
public class ImportSessionFactoryWrapper implements KeycloakSessionFactory {
    private ConcurrentHashMapStorageProviderFactory concurrentHashMapStorageProviderFactory;

    @Override
    public KeycloakSession create() {
        concurrentHashMapStorageProviderFactory = new ConcurrentHashMapStorageProviderFactory();
        concurrentHashMapStorageProviderFactory.init(Config.scope(MapStorageSpi.NAME, concurrentHashMapStorageProviderFactory.getId()));
        return new ImportKeycloakSession(this, keycloakSessionFactory.create());
    }

    @Override
    public Set<Spi> getSpis() {
        return keycloakSessionFactory.getSpis();
    }

    @Override
    public Spi getSpi(Class<? extends Provider> providerClass) {
        return keycloakSessionFactory.getSpi(providerClass);
    }

    @Override
    public <T extends Provider> ProviderFactory<T> getProviderFactory(Class<T> clazz) {
        if (clazz == MapStorageProvider.class) {
            return keycloakSessionFactory.getProviderFactory(clazz, ConcurrentHashMapStorageProviderFactory.PROVIDER_ID);
        }
        return keycloakSessionFactory.getProviderFactory(clazz);
    }

    @Override
    public <T extends Provider> ProviderFactory<T> getProviderFactory(Class<T> clazz, String id) {
        if (clazz == MapStorageProvider.class) {
            return (ProviderFactory<T>) concurrentHashMapStorageProviderFactory;
        }
        return keycloakSessionFactory.getProviderFactory(clazz, id);
    }

    @Override
    public <T extends Provider> ProviderFactory<T> getProviderFactory(Class<T> clazz, String realmId, String componentId, Function<KeycloakSessionFactory, ComponentModel> modelGetter) {
        return keycloakSessionFactory.getProviderFactory(clazz, realmId, componentId, modelGetter);
    }

    @Override
    public Stream<ProviderFactory> getProviderFactoriesStream(Class<? extends Provider> clazz) {
        return keycloakSessionFactory.getProviderFactoriesStream(clazz);
    }

    @Override
    public long getServerStartupTimestamp() {
        return keycloakSessionFactory.getServerStartupTimestamp();
    }

    @Override
    public void close() {
        keycloakSessionFactory.close();
    }

    @Override
    public void invalidate(KeycloakSession session, InvalidableObjectType type, Object... params) {
        keycloakSessionFactory.invalidate(session, type, params);
    }

    @Override
    public void register(ProviderEventListener listener) {
        keycloakSessionFactory.register(listener);
    }

    @Override
    public void unregister(ProviderEventListener listener) {
        keycloakSessionFactory.unregister(listener);
    }

    @Override
    public void publish(ProviderEvent event) {
        keycloakSessionFactory.publish(event);
    }

    private final KeycloakSessionFactory keycloakSessionFactory;

    public ImportSessionFactoryWrapper(KeycloakSessionFactory keycloakSessionFactory) {
        this.keycloakSessionFactory = keycloakSessionFactory;
    }
}
