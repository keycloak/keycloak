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

package org.keycloak.models.jpa.session;

import org.keycloak.models.session.PersistentClientSessionModel;
import org.keycloak.storage.StorageId;

/**
 * An immutable {@link PersistentClientSessionEntity} to optimize read-only queries.
 */
public record ImmutablePersistentClientSessionEntity(
        String userSessionId,
        String clientId,
        String clientStorageProvider,
        String externalClientId,
        String offline,
        String data,
        String realmId,
        int timestamp
) implements PersistentClientSessionModel {
    @Override
    public String getUserSessionId() {
        return userSessionId;
    }

    @Override
    public void setUserSessionId(String userSessionId) {
        readOnly();
    }

    @Override
    public String getClientId() {
        return externalClientId.equals(PersistentClientSessionEntity.LOCAL) ?
                clientId :
                new StorageId(clientStorageProvider, externalClientId).getId();
    }

    @Override
    public void setClientId(String clientId) {
        readOnly();
    }

    @Override
    public int getTimestamp() {
        return timestamp;
    }

    @Override
    public void setTimestamp(int timestamp) {
        readOnly();
    }

    @Override
    public String getData() {
        return data;
    }

    @Override
    public void setData(String data) {
        readOnly();
    }

    private static void readOnly() {
        throw new UnsupportedOperationException("this instance is read-only");
    }
}
