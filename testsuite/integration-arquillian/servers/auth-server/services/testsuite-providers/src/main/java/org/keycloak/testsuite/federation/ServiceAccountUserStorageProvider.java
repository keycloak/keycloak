/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.federation;

import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import org.keycloak.common.constants.ServiceAccountConstants;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.adapter.AbstractUserAdapterFederatedStorage;
import org.keycloak.storage.user.UserLookupProvider;
import org.keycloak.storage.user.UserServiceAccountProvider;

/**
 * In-memory service account user storage used by integration tests.
 */
public class ServiceAccountUserStorageProvider implements UserStorageProvider, UserServiceAccountProvider, UserLookupProvider {

    private final KeycloakSession session;
    private final ComponentModel model;
    private final ServiceAccountUserStorageProviderFactory.ProviderState state;

    public ServiceAccountUserStorageProvider(KeycloakSession session, ComponentModel model,
            ServiceAccountUserStorageProviderFactory.ProviderState state) {
        this.session = session;
        this.model = model;
        this.state = state;
    }

    @Override
    public UserModel addServiceAccountUser(RealmModel realm, String username) {
        if (!username.startsWith(ServiceAccountConstants.SERVICE_ACCOUNT_USER_PREFIX)) {
            return null;
        }

        state.addUser(username);
        return createUser(realm, username);
    }

    @Override
    public UserModel addUser(RealmModel realm, String username) {
        return null;
    }

    @Override
    public boolean removeUser(RealmModel realm, UserModel user) {
        return state.removeUser(user.getUsername());
    }

    @Override
    public UserModel getServiceAccount(ClientModel client) {
        String username = state.getUsernameByServiceAccountClientLink(client.getId());
        return username == null ? null : createUser(client.getRealm(), username);
    }

    @Override
    public UserModel getUserById(RealmModel realm, String id) {
        StorageId storageId = new StorageId(id);
        if (!Objects.equals(model.getId(), storageId.getProviderId())) {
            return null;
        }

        String username = storageId.getExternalId();
        return state.hasUser(username) ? createUser(realm, username) : null;
    }

    @Override
    public UserModel getUserByUsername(RealmModel realm, String username) {
        return state.hasUser(username) ? createUser(realm, username) : null;
    }

    @Override
    public UserModel getUserByEmail(RealmModel realm, String email) {
        return null;
    }

    @Override
    public void close() {
    }

    private UserModel createUser(RealmModel realm, String username) {
        AtomicReference<String> usernameRef = new AtomicReference<>(username);

        return new AbstractUserAdapterFederatedStorage.Streams(session, realm, model) {

            @Override
            public String getUsername() {
                return state.getUsername(usernameRef.get());
            }

            @Override
            public void setUsername(String newUsername) {
                state.renameUser(usernameRef.get(), newUsername);
                usernameRef.set(newUsername);
            }

            @Override
            public boolean isEnabled() {
                return state.isEnabled(usernameRef.get());
            }

            @Override
            public void setEnabled(boolean enabled) {
                state.setEnabled(usernameRef.get(), enabled);
            }

            @Override
            public String getServiceAccountClientLink() {
                return state.getServiceAccountClientLink(usernameRef.get());
            }

            @Override
            public void setServiceAccountClientLink(String clientInternalId) {
                state.setServiceAccountClientLink(usernameRef.get(), clientInternalId);
            }
        };
    }

    static String normalize(String username) {
        return username == null ? null : username.toLowerCase(Locale.ROOT);
    }
}
