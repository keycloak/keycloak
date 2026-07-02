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

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.keycloak.Config;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.storage.UserStorageProviderFactory;

public class ServiceAccountUserStorageProviderFactory implements UserStorageProviderFactory<ServiceAccountUserStorageProvider> {

    public static final String PROVIDER_ID = "service-account-user-storage";

    private final Map<String, ProviderState> states = new ConcurrentHashMap<>();

    @Override
    public ServiceAccountUserStorageProvider create(KeycloakSession session, ComponentModel model) {
        return new ServiceAccountUserStorageProvider(session, model, getState(model.getId()));
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return List.of();
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

    public ProviderState getState(String componentId) {
        return states.computeIfAbsent(componentId, ignored -> new ProviderState());
    }

    public void clear() {
        states.clear();
    }

    public static class ProviderState {

        private final Map<String, StoredUser> users = new ConcurrentHashMap<>();

        public void addUser(String username) {
            users.put(ServiceAccountUserStorageProvider.normalize(username), new StoredUser(username));
        }

        public boolean removeUser(String username) {
            return users.remove(ServiceAccountUserStorageProvider.normalize(username)) != null;
        }

        public boolean hasUser(String username) {
            return users.containsKey(ServiceAccountUserStorageProvider.normalize(username));
        }

        public String getUsername(String username) {
            StoredUser user = users.get(ServiceAccountUserStorageProvider.normalize(username));
            return user == null ? null : user.username;
        }

        public void renameUser(String username, String newUsername) {
            String oldKey = ServiceAccountUserStorageProvider.normalize(username);
            StoredUser user = users.remove(oldKey);
            if (user == null) {
                return;
            }

            user.username = newUsername;
            users.put(ServiceAccountUserStorageProvider.normalize(newUsername), user);
        }

        public boolean isEnabled(String username) {
            StoredUser user = users.get(ServiceAccountUserStorageProvider.normalize(username));
            return user != null && user.enabled;
        }

        public void setEnabled(String username, boolean enabled) {
            StoredUser user = users.get(ServiceAccountUserStorageProvider.normalize(username));
            if (user != null) {
                user.enabled = enabled;
            }
        }

        public String getServiceAccountClientLink(String username) {
            StoredUser user = users.get(ServiceAccountUserStorageProvider.normalize(username));
            return user == null ? null : user.serviceAccountClientLink;
        }

        public void setServiceAccountClientLink(String username, String clientInternalId) {
            StoredUser user = users.get(ServiceAccountUserStorageProvider.normalize(username));
            if (user != null) {
                user.serviceAccountClientLink = clientInternalId;
            }
        }

        public String getUsernameByServiceAccountClientLink(String clientInternalId) {
            return users.values().stream()
                    .filter(user -> clientInternalId.equals(user.serviceAccountClientLink))
                    .map(user -> user.username)
                    .findFirst()
                    .orElse(null);
        }

        public int size() {
            return users.size();
        }
    }

    private static class StoredUser {
        private String username;
        private boolean enabled;
        private String serviceAccountClientLink;

        private StoredUser(String username) {
            this.username = username;
        }
    }
}
