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

import org.keycloak.models.*;
import org.keycloak.models.cache.CacheRealmProvider;
import org.keycloak.models.cache.CacheUserProvider;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.scripting.ScriptingProvider;
import org.keycloak.storage.UserStorageManager;
import org.keycloak.storage.federated.UserFederatedStorageProvider;

import java.util.*;

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
    private ScriptingProvider scriptingProvider;
    private UserSessionProvider sessionProvider;
    private UserFederationManager federationManager;
    private UserFederatedStorageProvider userFederatedStorageProvider;
    private KeycloakContext context;

    public DefaultKeycloakSession(DefaultKeycloakSessionFactory factory) {
        this.factory = factory;
        this.transactionManager = new DefaultKeycloakTransactionManager();
        federationManager = new UserFederationManager(this);
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

    private UserProvider getUserProvider() {
        CacheUserProvider cache = getProvider(CacheUserProvider.class);
        if (cache != null) {
            return cache;
        } else {
            return getProvider(UserProvider.class);
        }
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
    public UserProvider userStorage() {
        if (userModel == null) {
            userModel = getUserProvider();
        }
        return userModel;
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
    public RealmProvider realms() {
        if (model == null) {
            model = getRealmProvider();
        }
        return model;
    }

    @Override
    public UserFederationManager users() {
        return federationManager;
    }

    @Override
    public UserSessionProvider sessions() {
        if (sessionProvider == null) {
            sessionProvider = getProvider(UserSessionProvider.class);
        }
        return sessionProvider;
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

    @Override
    public ScriptingProvider scripting() {

        if (scriptingProvider == null) {
            scriptingProvider = getProvider(ScriptingProvider.class);
        }

        return scriptingProvider;
    }
}
