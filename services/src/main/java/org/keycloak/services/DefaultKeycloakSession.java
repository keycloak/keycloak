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
import org.keycloak.jose.jws.DefaultTokenManager;
import org.keycloak.keys.DefaultKeyManager;
import org.keycloak.models.ClientProvider;
import org.keycloak.models.TokenManager;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.KeycloakTransactionManager;
import org.keycloak.models.KeyManager;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.ThemeManager;
import org.keycloak.models.UserCredentialManager;
import org.keycloak.models.UserProvider;
import org.keycloak.models.UserSessionProvider;
import org.keycloak.models.cache.CacheRealmProvider;
import org.keycloak.models.cache.UserCache;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.sessions.AuthenticationSessionProvider;
import org.keycloak.storage.ClientStorageManager;
import org.keycloak.storage.UserStorageManager;
import org.keycloak.storage.federated.UserFederatedStorageProvider;
import org.keycloak.vault.DefaultVaultTranscriber;
import org.keycloak.vault.VaultProvider;
import org.keycloak.vault.VaultTranscriber;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class DefaultKeycloakSession implements KeycloakSession {

    private final DefaultKeycloakSessionFactory factory;
    private final Map<Integer, Provider> providers = new HashMap<>();
    private final List<Provider> closable = new LinkedList<>();
    private final DefaultKeycloakTransactionManager transactionManager;
    private final Map<String, Object> attributes = new HashMap<>();
    private RealmProvider model;
    private UserStorageManager userStorageManager;
    private ClientStorageManager clientStorageManager;
    private UserCredentialStoreManager userCredentialStorageManager;
    private UserSessionProvider sessionProvider;
    private AuthenticationSessionProvider authenticationSessionProvider;
    private UserFederatedStorageProvider userFederatedStorageProvider;
    private KeycloakContext context;
    private KeyManager keyManager;
    private ThemeManager themeManager;
    private TokenManager tokenManager;
    private VaultTranscriber vaultTranscriber;

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
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String attribute, Class<T> clazz) {
        Object value = getAttribute(attribute);
        return clazz.isInstance(value) ? (T) value : null;
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
    public RealmProvider realmLocalStorage() {
        return getProvider(RealmProvider.class);
    }

    @Override
    public ClientProvider clientLocalStorage() {
        return realmLocalStorage();
    }

    @Override
    public ClientProvider clientStorageManager() {
        if (clientStorageManager == null) clientStorageManager = new ClientStorageManager(this);
        return clientStorageManager;
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

    @SuppressWarnings("unchecked")
    public <T extends Provider> T getProvider(Class<T> clazz) {
        Integer hash = clazz.hashCode();
        T provider = (T) providers.get(hash);
        // KEYCLOAK-11890 - Avoid using HashMap.computeIfAbsent() to implement logic in outer if() block below,
        // since per JDK-8071667 the remapping function should not modify the map during computation. While
        // allowed on JDK 1.8, attempt of such a modification throws ConcurrentModificationException with JDK 9+
        if (provider == null) {
            ProviderFactory<T> providerFactory = factory.getProviderFactory(clazz);
            if (providerFactory != null) {
                provider = providerFactory.create(DefaultKeycloakSession.this);
                providers.put(hash, provider);
            }
        }
        return provider;
    }

    @SuppressWarnings("unchecked")
    public <T extends Provider> T getProvider(Class<T> clazz, String id) {
        Integer hash = clazz.hashCode() + id.hashCode();
        T provider = (T) providers.get(hash);
        // KEYCLOAK-11890 - Avoid using HashMap.computeIfAbsent() to implement logic in outer if() block below,
        // since per JDK-8071667 the remapping function should not modify the map during computation. While
        // allowed on JDK 1.8, attempt of such a modification throws ConcurrentModificationException with JDK 9+
        if (provider == null) {
            ProviderFactory<T> providerFactory = factory.getProviderFactory(clazz, id);
            if (providerFactory != null) {
                provider = providerFactory.create(DefaultKeycloakSession.this);
                providers.put(hash, provider);
            }
        }
        return provider;
    }

    @Override
    public <T extends Provider> T getProvider(Class<T> clazz, ComponentModel componentModel) {
        String modelId = componentModel.getId();

        Object found = getAttribute(modelId);
        if (found != null) {
            return clazz.cast(found);
        }

        ProviderFactory<T> providerFactory = factory.getProviderFactory(clazz, componentModel.getProviderId());
        if (providerFactory == null) {
            return null;
        }

        @SuppressWarnings("unchecked")
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
        return listProviderIds(clazz).stream()
            .map(id -> getProvider(clazz, id))
            .collect(Collectors.toSet());
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
    public AuthenticationSessionProvider authenticationSessions() {
        if (authenticationSessionProvider == null) {
            authenticationSessionProvider = getProvider(AuthenticationSessionProvider.class);
        }
        return authenticationSessionProvider;
    }

    @Override
    public KeyManager keys() {
        if (keyManager == null) {
            keyManager = new DefaultKeyManager(this);
        }
        return keyManager;
    }

    @Override
    public ThemeManager theme() {
        if (themeManager == null) {
            themeManager = factory.getThemeManagerFactory().create(this);
        }
        return themeManager;
    }

    @Override
    public TokenManager tokens() {
        if (tokenManager == null) {
            tokenManager = new DefaultTokenManager(this);
        }
        return tokenManager;
    }

    @Override
    public VaultTranscriber vault() {
        if (this.vaultTranscriber == null) {
            this.vaultTranscriber = new DefaultVaultTranscriber(this.getProvider(VaultProvider.class));
        }
        return this.vaultTranscriber;
    }

    public void close() {
        Consumer<? super Provider> safeClose = p -> {
            try {
                p.close();
            } catch (Exception e) {
                // Ignore exception
            }
        };
        providers.values().forEach(safeClose);
        closable.forEach(safeClose);
    }
}
