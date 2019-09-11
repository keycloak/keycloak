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

package org.keycloak.models;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.cache.UserCache;
import org.keycloak.provider.Provider;
import org.keycloak.sessions.AuthenticationSessionProvider;
import org.keycloak.storage.federated.UserFederatedStorageProvider;
import org.keycloak.vault.VaultTranscriber;

import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface KeycloakSession {

    KeycloakContext getContext();

    KeycloakTransactionManager getTransactionManager();

    /**
     * Get dedicated provider instance of provider type clazz that was created for this session.  If one hasn't been created yet,
     * find the factory and allocate by calling ProviderFactory.create(KeycloakSession).  The provider to use is determined
     * by the "provider" config entry in keycloak-server boot configuration. (keycloak-server.json)
     *
     *
     *
     * @param clazz
     * @param <T>
     * @return
     */
    <T extends Provider> T getProvider(Class<T> clazz);

    /**
     * Get dedicated provider instance for a specific provider factory of id of provider type clazz that was created for this session.
     * If one hasn't been created yet,
     * find the factory and allocate by calling ProviderFactory.create(KeycloakSession).

     * @param clazz
     * @param id
     * @param <T>
     * @return
     */
    <T extends Provider> T getProvider(Class<T> clazz, String id);

    <T extends Provider> T getProvider(Class<T> clazz, ComponentModel componentModel);

    /**
     * Get all provider factories that manage provider instances of class.
     *
     * @param clazz
     * @param <T>
     * @return
     */
    <T extends Provider> Set<String> listProviderIds(Class<T> clazz);

    <T extends Provider> Set<T> getAllProviders(Class<T> clazz);

    Class<? extends Provider> getProviderClass(String providerClassName);

    Object getAttribute(String attribute);
    <T> T getAttribute(String attribute, Class<T> clazz);
    default <T> T getAttributeOrDefault(String attribute, T defaultValue) {
        T value = (T) getAttribute(attribute);

        if (value == null) {
            return defaultValue;
        }

        return value;
    }

    Object removeAttribute(String attribute);
    void setAttribute(String name, Object value);


    void enlistForClose(Provider provider);

    KeycloakSessionFactory getKeycloakSessionFactory();

    /**
     * Returns a managed provider instance.  Will start a provider transaction.  This transaction is managed by the KeycloakSession
     * transaction.
     *
     * @return
     * @throws IllegalStateException if transaction is not active
     */
    RealmProvider realms();

    /**
     * Returns a managed provider instance.  Will start a provider transaction.  This transaction is managed by the KeycloakSession
     * transaction.
     *
     * @return
     * @throws IllegalStateException if transaction is not active
     */
    UserSessionProvider sessions();


    AuthenticationSessionProvider authenticationSessions();



    void close();

    /**
     * The user cache
     *
     * @return may be null if cache is disabled
     */
    UserCache userCache();

    /**
     * A cached view of all users in system including  users loaded by UserStorageProviders
     *
     * @return
     */
    UserProvider users();


    ClientProvider clientStorageManager();

    /**
     * Un-cached view of all users in system including users loaded by UserStorageProviders
     *
     * @return
     */
    UserProvider userStorageManager();

    /**
     * Service that allows you to valid and update credentials for a user
     *
     * @return
     */
    UserCredentialManager userCredentialManager();

    /**
     * Keycloak specific local storage for users.  No cache in front, this api talks directly to database configured for Keycloak
     *
     * @return
     */
    UserProvider userLocalStorage();

    RealmProvider realmLocalStorage();

    /**
     * Keycloak specific local storage for clients.  No cache in front, this api talks directly to database configured for Keycloak
     *
     * @return
     */
    ClientProvider clientLocalStorage();

    /**
     * Hybrid storage for UserStorageProviders that can't store a specific piece of keycloak data in their external storage.
     * No cache in front.
     *
     * @return
     */
    UserFederatedStorageProvider userFederatedStorage();

    /**
     * Key manager
     *
      * @return
     */
    KeyManager keys();

    /**
     * Theme manager
     *
     * @return
     */
    ThemeManager theme();

    /**
     * Token manager
     *
     * @return
     */
    TokenManager tokens();

    /**
     * Vault transcriber
     */
    VaultTranscriber vault();
}
