/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.models.map.authSession;

import org.keycloak.models.map.common.AbstractEntity;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author <a href="mailto:mkanis@redhat.com">Martin Kanis</a>
 */
public class MapRootAuthenticationSessionEntity<K> implements AbstractEntity<K> {

    private K id;
    private String realmId;

    /**
     * Flag signalizing that any of the setters has been meaningfully used.
     */
    protected boolean updated;
    private int timestamp;
    private Map<String, MapAuthenticationSessionEntity> authenticationSessions = new ConcurrentHashMap<>();

    protected MapRootAuthenticationSessionEntity() {
        this.id = null;
        this.realmId = null;
    }

    public MapRootAuthenticationSessionEntity(K id, String realmId) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(realmId, "realmId");

        this.id = id;
        this.realmId = realmId;
    }

    @Override
    public K getId() {
        return this.id;
    }

    @Override
    public boolean isUpdated() {
        return this.updated;
    }

    public String getRealmId() {
        return realmId;
    }

    public void setRealmId(String realmId) {
        this.updated |= !Objects.equals(this.realmId, realmId);
        this.realmId = realmId;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(int timestamp) {
        this.updated |= !Objects.equals(this.timestamp, timestamp);
        this.timestamp = timestamp;
    }

    public Map<String, MapAuthenticationSessionEntity> getAuthenticationSessions() {
        return authenticationSessions;
    }

    public void setAuthenticationSessions(Map<String, MapAuthenticationSessionEntity> authenticationSessions) {
        this.updated |= !Objects.equals(this.authenticationSessions, authenticationSessions);
        this.authenticationSessions = authenticationSessions;
    }

    public MapAuthenticationSessionEntity removeAuthenticationSession(String tabId) {
        MapAuthenticationSessionEntity entity = this.authenticationSessions.remove(tabId);
        this.updated |= entity != null;
        return entity;
    }

    public void addAuthenticationSession(String tabId, MapAuthenticationSessionEntity entity) {
        this.updated |= !Objects.equals(this.authenticationSessions.put(tabId, entity), entity);
    }

    public void clearAuthenticationSessions() {
        this.updated |= !this.authenticationSessions.isEmpty();
        this.authenticationSessions.clear();
    }
}
