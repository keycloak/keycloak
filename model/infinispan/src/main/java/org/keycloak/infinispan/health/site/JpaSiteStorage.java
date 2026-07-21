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

package org.keycloak.infinispan.health.site;

import java.io.IOException;
import java.util.Objects;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.ModelException;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.storage.configuration.ServerConfigStorageProvider;
import org.keycloak.util.JsonSerialization;

/**
 * {@link SiteStorage} implementation backed by the {@link ServerConfigStorageProvider}, which stores the
 * {@link SiteState} as a JSON-serialized row in the {@code SERVER_CONFIG} table. Each read and write runs in its own
 * transaction to ensure atomicity of compare-and-set operations across sites sharing the same database.
 */
public class JpaSiteStorage implements SiteStorage {

    private static final String STORAGE_KEY = "site.state";
    private final KeycloakSessionFactory factory;

    public JpaSiteStorage(KeycloakSessionFactory factory) {
        this.factory = Objects.requireNonNull(factory);
    }

    @Override
    public SiteState get() {
        return KeycloakModelUtils.runJobInTransactionWithResult(factory, session -> {
            var jsonState = storage(session).loadOrCreate(STORAGE_KEY, JpaSiteStorage::initialState);
            assert jsonState != null;
            return fromJson(jsonState);
        });
    }

    @Override
    public boolean compareAndSet(SiteState expectedState, SiteState newState) {
        return KeycloakModelUtils.runJobInTransactionWithResult(factory,
                session -> storage(session).replace(STORAGE_KEY, toJson(expectedState), toJson(newState)));
    }

    private static ServerConfigStorageProvider storage(KeycloakSession session) {
        return session.getProvider(ServerConfigStorageProvider.class);
    }

    private static String initialState() {
        return toJson(SiteState.healthy());
    }

    private static String toJson(SiteState state) {
        try {
            return JsonSerialization.writeValueAsString(state);
        } catch (IOException e) {
            throw new ModelException("Unable to convert site state to json", e);
        }
    }

    private static SiteState fromJson(String json) {
        try {
            return JsonSerialization.readValue(json, SiteState.class);
        } catch (IOException e) {
            throw new ModelException("Unable to convert site state from json", e);
        }
    }

}
