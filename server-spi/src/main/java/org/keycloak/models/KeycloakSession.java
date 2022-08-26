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
import org.keycloak.provider.InvalidationHandler.InvalidableObjectType;
import org.keycloak.provider.Provider;
import org.keycloak.services.clientpolicy.ClientPolicyManager;
import org.keycloak.sessions.AuthenticationSessionProvider;
import org.keycloak.storage.federated.UserFederatedStorageProvider;
import org.keycloak.vault.VaultTranscriber;

import java.util.Set;
import java.util.function.Function;

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

    /**
     * Returns a component provider for a component from the realm that is relevant to this session.
     * The relevant realm must be set prior to calling this method in the context, see {@link KeycloakContext#getRealm()}.
     * @param <T>
     * @param clazz
     * @param componentId Component configuration
     * @throws IllegalArgumentException If the realm is not set in the context.
     * @return Provider configured according to the {@param componentId}, {@code null} if it cannot be instantiated.
     */
    <T extends Provider> T getComponentProvider(Class<T> clazz, String componentId);

    /**
     * Returns a component provider for a component from the realm that is relevant to this session.
     * The relevant realm must be set prior to calling this method in the context, see {@link KeycloakContext#getRealm()}.
     * @param <T>
     * @param clazz
     * @param componentId Component configuration
     * @param modelGetter Getter to retrieve componentModel
     * @throws IllegalArgumentException If the realm is not set in the context.
     * @return Provider configured according to the {@param componentId}, {@code null} if it cannot be instantiated.
     */
    <T extends Provider> T getComponentProvider(Class<T> clazz, String componentId, Function<KeycloakSessionFactory, ComponentModel> modelGetter);

    /**
     *
     * @param <T>
     * @param clazz
     * @param componentModel
     * @return
     * @deprecated Deprecated in favor of {@link #getComponentProvider)
     */
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

    /**
     * Invalidates intermediate states of the given objects, both immediately and at the end of this session.
     * @param type Type of the objects to invalidate
     * @param params Parameters used for the invalidation
     */
    void invalidate(InvalidableObjectType type, Object... params);

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
    ClientProvider clients();

    /**
     * Returns a managed provider instance.  Will start a provider transaction.  This transaction is managed by the KeycloakSession
     * transaction.
     *
     * @return Currently used ClientScopeProvider instance.
     * @throws IllegalStateException if transaction is not active
     */
    ClientScopeProvider clientScopes();

    /**
     * Returns a managed group provider instance.
     *
     * @return Currently used GroupProvider instance.
     * @throws IllegalStateException if transaction is not active
     */
    GroupProvider groups();

    /**
     * Returns a managed provider instance.  Will start a provider transaction.  This transaction is managed by the KeycloakSession
     * transaction.
     *
     * @return
     * @throws IllegalStateException if transaction is not active
     */
    RoleProvider roles();

    /**
     * Returns a managed provider instance.  Will start a provider transaction.  This transaction is managed by the KeycloakSession
     * transaction.
     *
     * @return
     * @throws IllegalStateException if transaction is not active
     */
    UserSessionProvider sessions();

    /**
     * Returns a managed provider instance.  Will start a provider transaction.  This transaction is managed by the KeycloakSession
     * transaction.
     *
     * @return {@link UserLoginFailureProvider}
     * @throws IllegalStateException if transaction is not active
     */
    UserLoginFailureProvider loginFailures();

    AuthenticationSessionProvider authenticationSessions();



    void close();

    /**
     * The user cache
     *
     * @deprecated The access to the UserCache interface is no longer possible here, and this method is about to be removed.
     * Adjust your code according to the Keycloak 19 Upgrading Guide.
     * @return may be null if cache is disabled
     */
    @Deprecated
    UserProvider userCache();

    /**
     * A cached view of all users in system including  users loaded by UserStorageProviders
     *
     * @return UserProvider instance
     */
    UserProvider users();

    /**
     * @return ClientStorageManager instance
     */
    @Deprecated
    ClientProvider clientStorageManager();

    /**
     * @return ClientScopeStorageManager instance
     * @deprecated Use {@link #clientScopes()} instead
     */
    @Deprecated
    ClientScopeProvider clientScopeStorageManager();

    /**
     * @return RoleStorageManager instance
     */
    @Deprecated
    RoleProvider roleStorageManager();

    /**
     * @return GroupStorageManager instance
     */
    @Deprecated
    GroupProvider groupStorageManager();

    /**
     * Un-cached view of all users in system including users loaded by UserStorageProviders
     *
     * @return
     */
    @Deprecated
    UserProvider userStorageManager();

    /**
     * Service that allows you to valid and update credentials for a user
     * @deprecated Use {@link UserModel#credentialManager()} instead.
     * @return
     */
    @Deprecated
    UserCredentialManager userCredentialManager();

    /**
     * Keycloak specific local storage for users.  No cache in front, this api talks directly to database configured for Keycloak
     */
    @Deprecated
    UserProvider userLocalStorage();

    @Deprecated
    RealmProvider realmLocalStorage();

    /**
     * Keycloak specific local storage for clients.  No cache in front, this api talks directly to database configured for Keycloak
     *
     * @deprecated Access to the legacy store is no longer possible via this method. Adjust your code according to the Keycloak 19 Upgrading Guide.
     */
    @Deprecated
    ClientProvider clientLocalStorage();

    /**
     * Keycloak specific local storage for client scopes.  No cache in front, this api talks directly to database configured for Keycloak
     *
     * @deprecated Access to the legacy store is no longer possible via this method. Adjust your code according to the Keycloak 19 Upgrading Guide.
     */
    @Deprecated
    ClientScopeProvider clientScopeLocalStorage();

    /**
     * Keycloak specific local storage for groups.  No cache in front, this api talks directly to storage configured for Keycloak
     *
     * @deprecated Access to the legacy store is no longer possible via this method. Adjust your code according to the Keycloak 19 Upgrading Guide.
     */
    @Deprecated
    GroupProvider groupLocalStorage();

    /**
     * Keycloak specific local storage for roles.  No cache in front, this api talks directly to storage configured for Keycloak
     *
     * @deprecated Access to the legacy store is no longer possible via this method. Adjust your code according to the Keycloak 19 Upgrading Guide.
     */
    @Deprecated
    RoleProvider roleLocalStorage();

    /**
     * Hybrid storage for UserStorageProviders that can't store a specific piece of keycloak data in their external storage.
     * No cache in front.
     *
     * @deprecated Access to the legacy store is no longer possible via this method. Adjust your code according to the Keycloak 19 Upgrading Guide.
     */
    @Deprecated
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

    /**
     * Client Policy Manager
     */
    ClientPolicyManager clientPolicy();

}
