/*
 * Copyright 2026 Capital One Financial Corporation and/or its affiliates
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

package org.keycloak.models.redis.entities;

import org.keycloak.common.util.Time;
import org.keycloak.models.AuthenticatedClientSessionModel;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Entity representing an authenticated client session stored in Redis.
 * A client session is associated with a user session and a specific client.
 */
public class RedisClientSessionEntity {

    private String userSessionId;
    private String clientId;
    private String realmId;
    private String authMethod;
    private String redirectUri;
    private int timestamp;
    private String action;
    private Map<String, String> notes;
    private String currentRefreshToken;
    private int currentRefreshTokenUseCount;
    private boolean offline;

    // Required for deserialization
    public RedisClientSessionEntity() {
    }

    /**
     * Create a new client session entity.
     */
    public static RedisClientSessionEntity create(String userSessionId, String clientId, String realmId) {
        RedisClientSessionEntity entity = new RedisClientSessionEntity();
        entity.userSessionId = Objects.requireNonNull(userSessionId);
        entity.clientId = Objects.requireNonNull(clientId);
        entity.realmId = Objects.requireNonNull(realmId);
        entity.timestamp = Time.currentTime();
        entity.notes = new HashMap<>();
        entity.offline = false;
        return entity;
    }

    /**
     * Create entity from an existing AuthenticatedClientSessionModel.
     */
    public static RedisClientSessionEntity createFromModel(AuthenticatedClientSessionModel model) {
        RedisClientSessionEntity entity = new RedisClientSessionEntity();
        entity.userSessionId = model.getUserSession().getId();
        entity.clientId = model.getClient().getId();
        entity.realmId = model.getRealm().getId();
        entity.authMethod = model.getProtocol();
        entity.redirectUri = model.getRedirectUri();
        entity.timestamp = model.getTimestamp();
        entity.action = model.getAction();
        entity.currentRefreshToken = model.getCurrentRefreshToken();
        entity.currentRefreshTokenUseCount = model.getCurrentRefreshTokenUseCount();
        
        Map<String, String> modelNotes = model.getNotes();
        entity.notes = modelNotes != null ? new HashMap<>(modelNotes) : new HashMap<>();
        
        return entity;
    }

    /**
     * Get the composite key for this client session (userSessionId:clientId).
     */
    public String getKey() {
        return userSessionId + ":" + clientId;
    }

    // Getters and setters

    public String getUserSessionId() {
        return userSessionId;
    }

    public void setUserSessionId(String userSessionId) {
        this.userSessionId = userSessionId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getRealmId() {
        return realmId;
    }

    public void setRealmId(String realmId) {
        this.realmId = realmId;
    }

    public String getAuthMethod() {
        return authMethod;
    }

    public void setAuthMethod(String authMethod) {
        this.authMethod = authMethod;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Map<String, String> getNotes() {
        return notes;
    }

    public void setNotes(Map<String, String> notes) {
        this.notes = notes;
    }

    public String getNote(String name) {
        return notes != null ? notes.get(name) : null;
    }

    public void setNote(String name, String value) {
        if (notes == null) {
            notes = new HashMap<>();
        }
        if (value == null) {
            notes.remove(name);
        } else {
            notes.put(name, value);
        }
    }

    public void removeNote(String name) {
        if (notes != null) {
            notes.remove(name);
        }
    }

    public String getCurrentRefreshToken() {
        return currentRefreshToken;
    }

    public void setCurrentRefreshToken(String currentRefreshToken) {
        this.currentRefreshToken = currentRefreshToken;
    }

    public int getCurrentRefreshTokenUseCount() {
        return currentRefreshTokenUseCount;
    }

    public void setCurrentRefreshTokenUseCount(int currentRefreshTokenUseCount) {
        this.currentRefreshTokenUseCount = currentRefreshTokenUseCount;
    }

    public boolean isOffline() {
        return offline;
    }

    public void setOffline(boolean offline) {
        this.offline = offline;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RedisClientSessionEntity that = (RedisClientSessionEntity) o;
        return Objects.equals(userSessionId, that.userSessionId) &&
                Objects.equals(clientId, that.clientId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userSessionId, clientId);
    }

    @Override
    public String toString() {
        return "RedisClientSessionEntity{" +
                "userSessionId='" + userSessionId + '\'' +
                ", clientId='" + clientId + '\'' +
                ", realmId='" + realmId + '\'' +
                ", offline=" + offline +
                '}';
    }
}
