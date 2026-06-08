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

import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.storage.UserStorageProviderFactory;

public class ServiceAccountUserStorageFactory implements UserStorageProviderFactory<ServiceAccountUserStorage> {

    public static final String PROVIDER_ID = "service-account-user-storage";

    private final Map<String, String> users = new ConcurrentHashMap<>();

    @Override
    public ServiceAccountUserStorage create(KeycloakSession session, ComponentModel model) {
        return new ServiceAccountUserStorage(session, model, users);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    public boolean contains(String username) {
        return users.containsKey(username);
    }

    public void clear() {
        users.clear();
    }
}
