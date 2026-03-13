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
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Entity representing a user session stored in Redis.
 * Mirrors the structure of RemoteUserSessionEntity from Infinispan implementation.
 */
public class RedisUserSessionEntity {

    // Immutable fields
    private String id;
    
    // Mutable fields
    private String realmId;
    private String userId;
    private String brokerSessionId;
    private String brokerUserId;
    private String loginUsername;
    private String ipAddress;
    private String authMethod;
    private boolean rememberMe;
    private int started;
    private int lastSessionRefresh;
    private UserSessionModel.State state;
    private Map<String, String> notes;
    private boolean offline;

    // Required for deserialization
    public RedisUserSessionEntity() {
    }

    private RedisUserSessionEntity(String id) {
        this.id = Objects.requireNonNull(id);
    }

    /**
     * Create a new user session entity.
     */
    public static RedisUserSessionEntity create(String id, RealmModel realm, UserModel user,
                                                  String loginUsername, String ipAddress,
                                                  String authMethod, boolean rememberMe,
                                                  String brokerSessionId, String brokerUserId) {
        RedisUserSessionEntity entity = new RedisUserSessionEntity(id);
        entity.realmId = realm.getId();
        entity.userId = user.getId();
        entity.loginUsername = loginUsername;
        entity.ipAddress = ipAddress;
        entity.authMethod = authMethod;
        entity.rememberMe = rememberMe;
        entity.brokerSessionId = brokerSessionId;
        entity.brokerUserId = brokerUserId;
        entity.started = Time.currentTime();
        entity.lastSessionRefresh = entity.started;
        entity.state = null;
        entity.notes = new HashMap<>();
        entity.offline = false;
        return entity;
    }

    /**
     * Create entity from an existing UserSessionModel.
     */
    public static RedisUserSessionEntity createFromModel(UserSessionModel model) {
        RedisUserSessionEntity entity = new RedisUserSessionEntity(model.getId());
        entity.realmId = model.getRealm().getId();
        entity.userId = model.getUser() != null ? model.getUser().getId() : null;
        entity.loginUsername = model.getLoginUsername();
        entity.ipAddress = model.getIpAddress();
        entity.authMethod = model.getAuthMethod();
        entity.rememberMe = model.isRememberMe();
        entity.brokerSessionId = model.getBrokerSessionId();
        entity.brokerUserId = model.getBrokerUserId();
        entity.started = model.getStarted();
        entity.lastSessionRefresh = model.getLastSessionRefresh();
        entity.state = model.getState();
        
        Map<String, String> modelNotes = model.getNotes();
        entity.notes = modelNotes != null ? new HashMap<>(modelNotes) : new HashMap<>();
        
        return entity;
    }

    // Getters and setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRealmId() {
        return realmId;
    }

    public void setRealmId(String realmId) {
        this.realmId = realmId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getBrokerSessionId() {
        return brokerSessionId;
    }

    public void setBrokerSessionId(String brokerSessionId) {
        this.brokerSessionId = brokerSessionId;
    }

    public String getBrokerUserId() {
        return brokerUserId;
    }

    public void setBrokerUserId(String brokerUserId) {
        this.brokerUserId = brokerUserId;
    }

    public String getLoginUsername() {
        return loginUsername;
    }

    public void setLoginUsername(String loginUsername) {
        this.loginUsername = loginUsername;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getAuthMethod() {
        return authMethod;
    }

    public void setAuthMethod(String authMethod) {
        this.authMethod = authMethod;
    }

    public boolean isRememberMe() {
        return rememberMe;
    }

    public void setRememberMe(boolean rememberMe) {
        this.rememberMe = rememberMe;
    }

    public int getStarted() {
        return started;
    }

    public void setStarted(int started) {
        this.started = started;
    }

    public int getLastSessionRefresh() {
        return lastSessionRefresh;
    }

    public void setLastSessionRefresh(int lastSessionRefresh) {
        this.lastSessionRefresh = Math.max(this.lastSessionRefresh, lastSessionRefresh);
    }

    public UserSessionModel.State getState() {
        return state;
    }

    public void setState(UserSessionModel.State state) {
        this.state = state;
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

    public boolean isOffline() {
        return offline;
    }

    public void setOffline(boolean offline) {
        this.offline = offline;
    }

    /**
     * Restart the session (used when session is restarted during authentication).
     */
    public void restart(String realmId, String userId, String loginUsername,
                        String ipAddress, String authMethod, boolean rememberMe,
                        String brokerSessionId, String brokerUserId) {
        int currentTime = Time.currentTime();
        this.realmId = realmId;
        this.userId = userId;
        this.loginUsername = loginUsername;
        this.ipAddress = ipAddress;
        this.authMethod = authMethod;
        this.rememberMe = rememberMe;
        this.brokerSessionId = brokerSessionId;
        this.brokerUserId = brokerUserId;
        this.started = currentTime;
        this.lastSessionRefresh = currentTime;
        this.state = null;
        this.notes = new HashMap<>();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RedisUserSessionEntity that = (RedisUserSessionEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "RedisUserSessionEntity{" +
                "id='" + id + '\'' +
                ", realmId='" + realmId + '\'' +
                ", userId='" + userId + '\'' +
                ", loginUsername='" + loginUsername + '\'' +
                ", offline=" + offline +
                '}';
    }
}
