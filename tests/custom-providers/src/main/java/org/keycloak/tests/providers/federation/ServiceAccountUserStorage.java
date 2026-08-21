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
package org.keycloak.tests.providers.federation;

import java.util.Map;
import java.util.UUID;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.adapter.AbstractUserAdapterFederatedStorage;
import org.keycloak.storage.user.UserLookupProvider;
import org.keycloak.storage.user.UserRegistrationProvider;
import org.keycloak.storage.user.UserServiceAccountProvider;

/**
 * In-memory user storage provider that handles service account users only.
 * Used to verify that {@link UserServiceAccountProvider} routes service account
 * creation, lookup and removal through external storage instead of local JPA storage.
 */
public class ServiceAccountUserStorage implements UserStorageProvider, UserLookupProvider,
        UserRegistrationProvider, UserServiceAccountProvider {

    private final KeycloakSession session;
    private final ComponentModel model;
    // externalId -> user entry (mutable username + clientLink)
    private final Map<String, UserEntry> users;

    public ServiceAccountUserStorage(KeycloakSession session, ComponentModel model, Map<String, UserEntry> users) {
        this.session = session;
        this.model = model;
        this.users = users;
    }

    static class UserEntry {
        String username;
        String serviceAccountClientLink;

        UserEntry(String username) {
            this.username = username;
            this.serviceAccountClientLink = "";
        }
    }

    private UserModel adapt(RealmModel realm, String externalId, UserEntry entry) {
        return new AbstractUserAdapterFederatedStorage(session, realm, model) {

            @Override
            public String getId() {
                return new StorageId(storageProviderModel.getId(), externalId).getId();
            }

            @Override
            public String getUsername() {
                return entry.username;
            }

            @Override
            public void setUsername(String newUsername) {
                entry.username = newUsername;
            }

            @Override
            public String getServiceAccountClientLink() {
                String link = entry.serviceAccountClientLink;
                return link == null || link.isEmpty() ? null : link;
            }

            @Override
            public void setServiceAccountClientLink(String clientInternalId) {
                entry.serviceAccountClientLink = clientInternalId;
            }
        };
    }

    @Override
    public UserModel addServiceAccountUser(RealmModel realm, String username) {
        String externalId = UUID.randomUUID().toString();
        UserEntry entry = new UserEntry(username);
        users.put(externalId, entry);
        return adapt(realm, externalId, entry);
    }

    @Override
    public UserModel getServiceAccount(ClientModel client) {
        return users.entrySet().stream()
                .filter(e -> client.getId().equals(e.getValue().serviceAccountClientLink))
                .map(e -> adapt(client.getRealm(), e.getKey(), e.getValue()))
                .findFirst()
                .orElse(null);
    }

    @Override
    public UserModel addUser(RealmModel realm, String username) {
        // Decline regular users so they fall through to local storage.
        return null;
    }

    @Override
    public boolean removeUser(RealmModel realm, UserModel user) {
        String externalId = new StorageId(user.getId()).getExternalId();
        return users.remove(externalId) != null;
    }

    @Override
    public UserModel getUserById(RealmModel realm, String id) {
        String externalId = new StorageId(id).getExternalId();
        UserEntry entry = users.get(externalId);
        return entry != null ? adapt(realm, externalId, entry) : null;
    }

    @Override
    public UserModel getUserByUsername(RealmModel realm, String username) {
        return users.entrySet().stream()
                .filter(e -> username.equals(e.getValue().username))
                .map(e -> adapt(realm, e.getKey(), e.getValue()))
                .findFirst()
                .orElse(null);
    }

    @Override
    public UserModel getUserByEmail(RealmModel realm, String email) {
        return null;
    }

    @Override
    public void close() {
    }
}
