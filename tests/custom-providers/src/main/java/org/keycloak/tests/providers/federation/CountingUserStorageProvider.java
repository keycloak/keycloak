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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.adapter.AbstractUserAdapterFederatedStorage;
import org.keycloak.storage.user.UserLookupProvider;

/**
 * Read-only, in-memory user storage that counts the lookup calls it receives, so tests can verify which lookups are
 * answered by the user cache and which ones reach the store.
 */
public class CountingUserStorageProvider implements UserStorageProvider, UserLookupProvider {

    /** username (lower case) to email */
    static final Map<String, String> USERS = new ConcurrentHashMap<>();

    public static final AtomicInteger BY_ID_CALLS = new AtomicInteger();
    public static final AtomicInteger BY_USERNAME_CALLS = new AtomicInteger();
    public static final AtomicInteger BY_EMAIL_CALLS = new AtomicInteger();

    private final KeycloakSession session;
    private final ComponentModel model;

    public CountingUserStorageProvider(KeycloakSession session, ComponentModel model) {
        this.session = session;
        this.model = model;
    }

    public static void addUser(String username, String email) {
        USERS.put(username.toLowerCase(), email.toLowerCase());
    }

    public static void reset() {
        USERS.clear();
        resetCounters();
    }

    public static void resetCounters() {
        BY_ID_CALLS.set(0);
        BY_USERNAME_CALLS.set(0);
        BY_EMAIL_CALLS.set(0);
    }

    @Override
    public UserModel getUserById(RealmModel realm, String id) {
        BY_ID_CALLS.incrementAndGet();
        String username = new StorageId(id).getExternalId();
        return USERS.containsKey(username) ? createUser(realm, username) : null;
    }

    @Override
    public UserModel getUserByUsername(RealmModel realm, String username) {
        BY_USERNAME_CALLS.incrementAndGet();
        String key = username.toLowerCase();
        return USERS.containsKey(key) ? createUser(realm, key) : null;
    }

    @Override
    public UserModel getUserByEmail(RealmModel realm, String email) {
        BY_EMAIL_CALLS.incrementAndGet();
        String key = email.toLowerCase();
        return USERS.entrySet().stream()
                .filter(e -> key.equals(e.getValue()))
                .map(e -> createUser(realm, e.getKey()))
                .findFirst()
                .orElse(null);
    }

    private UserModel createUser(RealmModel realm, String username) {
        return new AbstractUserAdapterFederatedStorage.Streams(session, realm, model) {
            @Override
            public String getUsername() {
                return username;
            }

            @Override
            public void setUsername(String username) {
                throw new UnsupportedOperationException();
            }

            @Override
            public String getEmail() {
                return USERS.get(username);
            }

            @Override
            public String getFederationLink() {
                return model.getId();
            }
        };
    }

    @Override
    public void close() {
    }
}
