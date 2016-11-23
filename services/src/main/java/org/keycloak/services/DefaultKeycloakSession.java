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

import org.keycloak.component.ComponentFactory;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.UserCredentialStoreManager;
import org.keycloak.keys.DefaultKeyManager;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.KeycloakTransactionManager;
import org.keycloak.models.KeyManager;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.UserCredentialManager;
import org.keycloak.models.UserProvider;
import org.keycloak.models.UserSessionProvider;
import org.keycloak.models.cache.CacheRealmProvider;
import org.keycloak.models.cache.UserCache;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.storage.UserStorageManager;
import org.keycloak.storage.federated.UserFederatedStorageProvider;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class DefaultKeycloakSession implements KeycloakSession {

    private final DefaultKeycloakSessionFactory factory;
    private final Map<Integer, Provider> providers = new HashMap<>();
    private final List<Provider> closable = new LinkedList<Provider>();
    private final DefaultKeycloakTransactionManager transactionManager;
    private final Map<String, Object> attributes = new HashMap<>();
    private RealmProvider model;
    private UserProvider userModel;
    private UserStorageManager userStorageManager;
    private UserCredentialStoreManager userCredentialStorageManager;
    private UserSessionProvider sessionProvider;
    private UserFederatedStorageProvider userFederatedStorageProvider;
    private KeycloakContext context;
    private KeyManager keyManager;

    public DefaultKeycloakSession(DefaultKeycloakSessionFactory factory) {
        this.factory = factory;
        this.transactionManager = new DefaultKeycloakTransactionManager(this);
        context = new DefaultKeycloakContext(this);
    }

    @Override
    public KeycloakContext getContext() {
        return context;
    }

    private RealmProvider getRealmProvider() {
        CacheRealmProvider cache = getProvider(CacheRealmProvider.class);
        if (cache != null) {
            return cache;
        } else {
            return getProvider(RealmProvider.class);
        }
    }

    @Override
    public UserCache userCache() {
        return getProvider(UserCache.class);

    }

    @Override
    public void enlistForClose(Provider provider) {
        closable.add(provider);
    }

    @Override
    public Object getAttribute(String attribute) {
        return attributes.get(attribute);
    }

    @Override
    public Object removeAttribute(String attribute) {
        return attributes.remove(attribute);
    }

    @Override
    public void setAttribute(String name, Object value) {
        attributes.put(name, value);
    }

    @Override
    public KeycloakTransactionManager getTransactionManager() {
        return transactionManager;
    }

    @Override
    public KeycloakSessionFactory getKeycloakSessionFactory() {
        return factory;
    }

    @Override
    public UserFederatedStorageProvider userFederatedStorage() {
        if (userFederatedStorageProvider == null) {
            userFederatedStorageProvider = getProvider(UserFederatedStorageProvider.class);
        }
        return userFederatedStorageProvider;
    }

    @Override
    public UserProvider userLocalStorage() {
        return getProvider(UserProvider.class);
    }

    @Override
    public UserProvider userStorageManager() {
        if (userStorageManager == null) userStorageManager = new UserStorageManager(this);
        return userStorageManager;
    }

    @Override
    public UserProvider users() {
        UserCache cache = getProvider(UserCache.class);
        if (cache != null) {
            return cache;
        } else {
            return userStorageManager();
        }
    }

    @Override
    public UserCredentialManager userCredentialManager() {
        if (userCredentialStorageManager == null) userCredentialStorageManager = new UserCredentialStoreManager(this);
        return userCredentialStorageManager;
    }

    public <T extends Provider> T getProvider(Class<T> clazz) {
        Integer hash = clazz.hashCode();
        T provider = (T) providers.get(hash);
        if (provider == null) {
            ProviderFactory<T> providerFactory = factory.getProviderFactory(clazz);
            if (providerFactory != null) {
                provider = providerFactory.create(this);
                providers.put(hash, provider);
            }
        }
        return provider;
    }

    public <T extends Provider> T getProvider(Class<T> clazz, String id) {
        Integer hash = clazz.hashCode() + id.hashCode();
        T provider = (T) providers.get(hash);
        if (provider == null) {
            ProviderFactory<T> providerFactory = factory.getProviderFactory(clazz, id);

            if (providerFactory != null) {
                provider = providerFactory.create(this);
                providers.put(hash, provider);
            }
        }
        return provider;
    }

    @Override
    public <T extends Provider> T getProvider(Class<T> clazz, ComponentModel componentModel) {
        String modelId = componentModel.getProviderType() + "::" + componentModel.getId();

        Object found = getAttribute(modelId);
        if (found != null) {
            return clazz.cast(found);
        }

        ProviderFactory<T> providerFactory = factory.getProviderFactory(clazz, componentModel.getProviderId());
        if (providerFactory == null) {
            return null;
        }

        ComponentFactory<T, T> componentFactory = (ComponentFactory<T, T>) providerFactory;
        T provider = componentFactory.create(this, componentModel);
        enlistForClose(provider);
        setAttribute(modelId, provider);

        return provider;
    }

    public <T extends Provider> Set<String> listProviderIds(Class<T> clazz) {
        return factory.getAllProviderIds(clazz);
    }

    @Override
    public <T extends Provider> Set<T> getAllProviders(Class<T> clazz) {
        Set<T> providers = new HashSet<T>();
        for (String id : listProviderIds(clazz)) {
            providers.add(getProvider(clazz, id));
        }
        return providers;
    }

    @Override
    public Class<? extends Provider> getProviderClass(String providerClassName) {
        return factory.getProviderClass(providerClassName);
    }

    @Override
    public RealmProvider realms() {
        if (model == null) {
            model = getRealmProvider();
        }
        return model;
    }

    @Override
    public UserSessionProvider sessions() {
        if (sessionProvider == null) {
            sessionProvider = getProvider(UserSessionProvider.class);
        }
        return sessionProvider;
    }

    @Override
    public KeyManager keys() {
        if (keyManager == null) {
            keyManager = new DefaultKeyManager(this);
        }
        return keyManager;
    }

    public void close() {
        for (Provider p : providers.values()) {
            try {
                p.close();
            } catch (Exception e) {
            }
        }
        for (Provider p : closable) {
            try {
                p.close();
            } catch (Exception e) {
            }
        }
    }
}
