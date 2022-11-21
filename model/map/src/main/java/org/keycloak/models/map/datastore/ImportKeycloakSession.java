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

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.ClientProvider;
import org.keycloak.models.ClientScopeProvider;
import org.keycloak.models.ClientScopeSpi;
import org.keycloak.models.ClientSpi;
import org.keycloak.models.GroupProvider;
import org.keycloak.models.GroupSpi;
import org.keycloak.models.KeyManager;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.KeycloakTransactionManager;
import org.keycloak.models.ModelException;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.RealmSpi;
import org.keycloak.models.RoleProvider;
import org.keycloak.models.RoleSpi;
import org.keycloak.models.ThemeManager;
import org.keycloak.models.TokenManager;
import org.keycloak.models.UserCredentialManager;
import org.keycloak.models.UserLoginFailureProvider;
import org.keycloak.models.UserProvider;
import org.keycloak.models.UserSessionProvider;
import org.keycloak.models.UserSpi;
import org.keycloak.models.map.client.MapClientProvider;
import org.keycloak.models.map.client.MapClientProviderFactory;
import org.keycloak.models.map.clientscope.MapClientScopeProvider;
import org.keycloak.models.map.clientscope.MapClientScopeProviderFactory;
import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.common.AbstractMapProviderFactory;
import org.keycloak.models.map.group.MapGroupProvider;
import org.keycloak.models.map.group.MapGroupProviderFactory;
import org.keycloak.models.map.realm.MapRealmProvider;
import org.keycloak.models.map.realm.MapRealmProviderFactory;
import org.keycloak.models.map.role.MapRoleProvider;
import org.keycloak.models.map.role.MapRoleProviderFactory;
import org.keycloak.models.map.user.MapUserProvider;
import org.keycloak.models.map.user.MapUserProviderFactory;
import org.keycloak.provider.InvalidationHandler;
import org.keycloak.provider.Provider;
import org.keycloak.provider.Spi;
import org.keycloak.services.clientpolicy.ClientPolicyManager;
import org.keycloak.sessions.AuthenticationSessionProvider;
import org.keycloak.vault.VaultTranscriber;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

import static org.keycloak.common.util.StackUtil.getShortStackTrace;

/**
 * This implementation of {@link KeycloakSession} wraps an existing session and directs all calls to the datastore provider
 * to a separate {@link KeycloakSessionFactory}.
 * This allows it to create instantiate different storage providers during import.
 *
 * @author Alexander Schwartz
 */
public class ImportKeycloakSession implements KeycloakSession {

    private static final Logger LOG = Logger.getLogger(ImportKeycloakSession.class);

    private final KeycloakSessionFactory factory;
    private final KeycloakSession session;
    private final MapRealmProvider realmProvider;
    private final MapClientProvider clientProvider;
    private final MapClientScopeProvider clientScopeProvider;
    private final MapGroupProvider groupProvider;
    private final MapRoleProvider roleProvider;
    private final MapUserProvider userProvider;

    private final Set<AbstractMapProviderFactory<?, ?, ?>> providerFactories = new HashSet<>();

    public ImportKeycloakSession(ImportSessionFactoryWrapper factory, KeycloakSession session) {
        this.factory = factory;
        this.session = session;
        realmProvider = createProvider(RealmSpi.class, MapRealmProviderFactory.class);
        clientProvider = createProvider(ClientSpi.class, MapClientProviderFactory.class);
        clientScopeProvider = createProvider(ClientScopeSpi.class, MapClientScopeProviderFactory.class);
        groupProvider = createProvider(GroupSpi.class, MapGroupProviderFactory.class);
        roleProvider = createProvider(RoleSpi.class, MapRoleProviderFactory.class);
        userProvider = createProvider(UserSpi.class, MapUserProviderFactory.class);
    }

    private <P extends Provider, V extends AbstractEntity, M> P createProvider(Class<? extends Spi> spi, Class<? extends AbstractMapProviderFactory<P, V, M>> providerFactoryClass) {
        try {
            AbstractMapProviderFactory<P, V, M> providerFactory = providerFactoryClass.getConstructor().newInstance();
            providerFactory.init(Config.scope(spi.getDeclaredConstructor().newInstance().getName(), providerFactory.getId()));
            providerFactories.add(providerFactory);
            return providerFactory.create(this);
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException |
                 InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public KeycloakContext getContext() {
        return session.getContext();
    }

    @Override
    public KeycloakTransactionManager getTransactionManager() {
        return session.getTransactionManager();
    }

    @Override
    public <T extends Provider> T getProvider(Class<T> clazz) {
        LOG.warnf("Calling getProvider(%s) on the parent session. Revisit this to ensure it doesn't have side effects on the parent session.", clazz.getName(), getShortStackTrace());
        return session.getProvider(clazz);
    }

    @Override
    public <T extends Provider> T getProvider(Class<T> clazz, String id) {
        LOG.warnf("Calling getProvider(%s, %s) on the parent session. Revisit this to ensure it doesn't have side effects on the parent session.", clazz.getName(), id, getShortStackTrace());
        return session.getProvider(clazz, id);
    }

    @Override
    public <T extends Provider> T getComponentProvider(Class<T> clazz, String componentId) {
        LOG.warnf("Calling getComponentProvider(%s, %s) on the parent session. Revisit this to ensure it doesn't have side effects on the parent session.", clazz.getName(), componentId, getShortStackTrace());
        return session.getComponentProvider(clazz, componentId);
    }

    @Override
    public <T extends Provider> T getComponentProvider(Class<T> clazz, String componentId, Function<KeycloakSessionFactory, ComponentModel> modelGetter) {
        LOG.warnf("Calling getComponentProvider(%s, %s, ...) on the parent session. Revisit this to ensure it doesn't have side effects on the parent session.", clazz.getName(), componentId, getShortStackTrace());
        return session.getComponentProvider(clazz, componentId, modelGetter);
    }

    @Override
    public <T extends Provider> T getProvider(Class<T> clazz, ComponentModel componentModel) {
        LOG.warnf("Calling getProvider(%s, ...) on the parent session. Revisit this to ensure it doesn't have side effects on the parent session.", clazz.getName(), getShortStackTrace());
        return session.getProvider(clazz, componentModel);
    }

    @Override
    public <T extends Provider> Set<String> listProviderIds(Class<T> clazz) {
        LOG.warnf("Calling listProviderIds(%s, ...) on the parent session. Revisit this to ensure it doesn't have side effects on the parent session.", clazz.getName(), getShortStackTrace());
        return session.listProviderIds(clazz);
    }

    @Override
    public <T extends Provider> Set<T> getAllProviders(Class<T> clazz) {
        LOG.warnf("Calling getAllProviders(%s) on the parent session. Revisit this to ensure it doesn't have side effects on the parent session.", clazz.getName(), getShortStackTrace());
        return session.getAllProviders(clazz);
    }

    @Override
    public Class<? extends Provider> getProviderClass(String providerClassName) {
        LOG.warnf("Calling getProviderClass(%s) on the parent session. Revisit this to ensure it doesn't have side effects on the parent session.", providerClassName, getShortStackTrace());
        return session.getProviderClass(providerClassName);
    }

    @Override
    public Object getAttribute(String attribute) {
        return session.getAttribute(attribute);
    }

    @Override
    public <T> T getAttribute(String attribute, Class<T> clazz) {
        return session.getAttribute(attribute, clazz);
    }

    @Override
    public <T> T getAttributeOrDefault(String attribute, T defaultValue) {
        return session.getAttributeOrDefault(attribute, defaultValue);
    }

    @Override
    public Object removeAttribute(String attribute) {
        return session.removeAttribute(attribute);
    }

    @Override
    public void setAttribute(String name, Object value) {
        session.setAttribute(name, value);
    }

    @Override
    public void invalidate(InvalidationHandler.InvalidableObjectType type, Object... ids) {
        // For now, forward the invalidations only to those providers created specifically for this import session.
        providerFactories.stream()
                .filter(InvalidationHandler.class::isInstance)
                .map(InvalidationHandler.class::cast)
                .forEach(ih -> ih.invalidate(this, type, ids));
    }

    @Override
    public void enlistForClose(Provider provider) {
        session.enlistForClose(provider);
    }

    @Override
    public KeycloakSessionFactory getKeycloakSessionFactory() {
        return factory;
    }

    @Override
    public RealmProvider realms() {
        return realmProvider;
    }

    @Override
    public ClientProvider clients() {
        return clientProvider;
    }

    @Override
    public ClientScopeProvider clientScopes() {
        return clientScopeProvider;
    }

    @Override
    public GroupProvider groups() {
        return groupProvider;
    }

    @Override
    public RoleProvider roles() {
        return roleProvider;
    }

    @Override
    public UserSessionProvider sessions() {
        throw new ModelException("not supported yet");
    }

    @Override
    public UserLoginFailureProvider loginFailures() {
        throw new ModelException("not supported yet");
    }

    @Override
    public AuthenticationSessionProvider authenticationSessions() {
        throw new ModelException("not supported yet");
    }

    @Override
    public void close() {
        session.close();
    }

    @Override
    @Deprecated
    public UserProvider userCache() {
        throw new ModelException("not supported");
    }

    @Override
    public UserProvider users() {
        return userProvider;
    }

    @Override
    @Deprecated
    public ClientProvider clientStorageManager() {
        throw new ModelException("not supported");
    }

    @Override
    @Deprecated
    public ClientScopeProvider clientScopeStorageManager() {
        throw new ModelException("not supported");
    }

    @Override
    @Deprecated
    public RoleProvider roleStorageManager() {
        throw new ModelException("not supported");
    }

    @Override
    @Deprecated
    public GroupProvider groupStorageManager() {
        throw new ModelException("not supported");
    }

    @Override
    @Deprecated
    public UserProvider userStorageManager() {
        throw new ModelException("not supported");
    }

    @Override
    @Deprecated
    public UserCredentialManager userCredentialManager() {
        throw new ModelException("not supported");
    }

    @Override
    @Deprecated
    public UserProvider userLocalStorage() {
        throw new ModelException("not supported");
    }

    @Override
    @Deprecated
    public RealmProvider realmLocalStorage() {
        throw new ModelException("not supported");
    }

    @Override
    @Deprecated
    public ClientProvider clientLocalStorage() {
        throw new ModelException("not supported");
    }

    @Override
    @Deprecated
    public ClientScopeProvider clientScopeLocalStorage() {
        throw new ModelException("not supported");
    }

    @Override
    @Deprecated
    public GroupProvider groupLocalStorage() {
        throw new ModelException("not supported");
    }

    @Override
    @Deprecated
    public RoleProvider roleLocalStorage() {
        throw new ModelException("not supported");
    }

    @Override
    public KeyManager keys() {
        throw new ModelException("not supported");
    }

    @Override
    public ThemeManager theme() {
        throw new ModelException("not supported");
    }

    @Override
    public TokenManager tokens() {
        throw new ModelException("not supported");
    }

    @Override
    public VaultTranscriber vault() {
        throw new ModelException("not supported");
    }

    @Override
    public ClientPolicyManager clientPolicy() {
        return session.clientPolicy();
    }
}
