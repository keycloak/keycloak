/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.protocol.oid4vc.issuance.signing.mock;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.ClientProvider;
import org.keycloak.models.ClientScopeProvider;
import org.keycloak.models.GroupProvider;
import org.keycloak.models.KeyManager;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.KeycloakTransactionManager;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.RoleProvider;
import org.keycloak.models.SingleUseObjectProvider;
import org.keycloak.models.ThemeManager;
import org.keycloak.models.TokenManager;
import org.keycloak.models.UserLoginFailureProvider;
import org.keycloak.models.UserProvider;
import org.keycloak.models.UserSessionProvider;
import org.keycloak.provider.InvalidationHandler;
import org.keycloak.provider.Provider;
import org.keycloak.services.DefaultKeycloakContext;
import org.keycloak.services.clientpolicy.ClientPolicyManager;
import org.keycloak.sessions.AuthenticationSessionProvider;
import org.keycloak.vault.VaultTranscriber;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class KeycloakSessionMock implements KeycloakSession {

    private final KeycloakContext keycloakContext;
    private final KeyManager keyManager;
    private final RealmModel realmModel;

    public KeycloakSessionMock(RealmModel realmModel, KeyManager keyManager) {
        this.keycloakContext = new DefaultKeycloakContext(this);
        this.realmModel = realmModel;
        this.keyManager = keyManager;
        this.keycloakContext.setRealm(this.realmModel);
    }

    @Override
    public KeycloakContext getContext() {
        return keycloakContext;
    }

    @Override
    public KeycloakTransactionManager getTransactionManager() {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public <T extends Provider> T getProvider(Class<T> clazz) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public <T extends Provider> T getProvider(Class<T> clazz, String id) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public <T extends Provider> T getComponentProvider(Class<T> clazz, String componentId) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public <T extends Provider> T getComponentProvider(Class<T> clazz, String componentId, Function<KeycloakSessionFactory, ComponentModel> modelGetter) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public <T extends Provider> T getProvider(Class<T> clazz, ComponentModel componentModel) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public <T extends Provider> Set<String> listProviderIds(Class<T> clazz) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public <T extends Provider> Set<T> getAllProviders(Class<T> clazz) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public Class<? extends Provider> getProviderClass(String providerClassName) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public Object getAttribute(String attribute) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public <T> T getAttribute(String attribute, Class<T> clazz) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public Object removeAttribute(String attribute) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public void setAttribute(String name, Object value) {

    }

    @Override
    public Map<String, Object> getAttributes() {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public void invalidate(InvalidationHandler.InvalidableObjectType type, Object... params) {

    }

    @Override
    public void enlistForClose(Provider provider) {

    }

    @Override
    public KeycloakSessionFactory getKeycloakSessionFactory() {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public RealmProvider realms() {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public ClientProvider clients() {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public ClientScopeProvider clientScopes() {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public GroupProvider groups() {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public RoleProvider roles() {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public UserSessionProvider sessions() {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public UserLoginFailureProvider loginFailures() {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public AuthenticationSessionProvider authenticationSessions() {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public SingleUseObjectProvider singleUseObjects() {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public void close() {

    }

    @Override
    public UserProvider users() {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public KeyManager keys() {
        return keyManager;
    }

    @Override
    public ThemeManager theme() {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public TokenManager tokens() {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public VaultTranscriber vault() {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public ClientPolicyManager clientPolicy() {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }
}
