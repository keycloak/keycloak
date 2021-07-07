/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.models.map.userSession;

import org.keycloak.common.util.Time;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.map.common.AbstractEntity;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author <a href="mailto:mkanis@redhat.com">Martin Kanis</a>
 */
public class MapUserSessionEntity<K> implements AbstractEntity<K> {
    private K id;

    private String realmId;

    /**
     * Flag signalizing that any of the setters has been meaningfully used.
     */
    protected boolean updated;

    private String userId;

    private String brokerSessionId;
    private String brokerUserId;

    private String loginUsername;

    private String ipAddress;

    private String authMethod;

    private boolean rememberMe;

    private int started;

    private int lastSessionRefresh;

    private long expiration;

    private Map<String, String> notes = new ConcurrentHashMap<>();

    private UserSessionModel.State state;

    private UserSessionModel.SessionPersistenceState persistenceState = UserSessionModel.SessionPersistenceState.PERSISTENT;

    private Map<String, String> authenticatedClientSessions = new ConcurrentHashMap<>();

    private boolean offline;

    public MapUserSessionEntity() {
        this.id = null;
        this.realmId = null;
    }

    public MapUserSessionEntity(K id, String realmId) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(realmId, "realmId");

        this.id = id;
        this.realmId = realmId;
    }

    public MapUserSessionEntity(K id, RealmModel realm, UserModel user, String loginUsername, String ipAddress,
                                     String authMethod, boolean rememberMe, String brokerSessionId, String brokerUserId,
                                     boolean offline) {
        this.id = id;
        this.realmId = realm.getId();
        this.userId = user.getId();
        this.loginUsername = loginUsername;
        this.ipAddress = ipAddress;
        this.authMethod = authMethod;
        this.rememberMe = rememberMe;
        this.brokerSessionId = brokerSessionId;
        this.brokerUserId = brokerUserId;
        this.started = Time.currentTime();
        this.lastSessionRefresh = started;
        this.offline = offline;
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

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.updated |= !Objects.equals(this.userId, userId);
        this.userId = userId;
    }

    public String getBrokerSessionId() {
        return brokerSessionId;
    }

    public void setBrokerSessionId(String brokerSessionId) {
        this.updated |= !Objects.equals(this.brokerSessionId, brokerSessionId);
        this.brokerSessionId = brokerSessionId;
    }

    public String getBrokerUserId() {
        return brokerUserId;
    }

    public void setBrokerUserId(String brokerUserId) {
        this.updated |= !Objects.equals(this.brokerUserId, brokerUserId);
        this.brokerUserId = brokerUserId;
    }

    public String getLoginUsername() {
        return loginUsername;
    }

    public void setLoginUsername(String loginUsername) {
        this.updated |= !Objects.equals(this.loginUsername, loginUsername);
        this.loginUsername = loginUsername;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.updated |= !Objects.equals(this.ipAddress, ipAddress);
        this.ipAddress = ipAddress;
    }

    public String getAuthMethod() {
        return authMethod;
    }

    public void setAuthMethod(String authMethod) {
        this.updated |= !Objects.equals(this.authMethod, authMethod);
        this.authMethod = authMethod;
    }

    public boolean isRememberMe() {
        return rememberMe;
    }

    public void setRememberMe(boolean rememberMe) {
        this.updated |= this.rememberMe != rememberMe;
        this.rememberMe = rememberMe;
    }

    public int getStarted() {
        return started;
    }

    public void setStarted(int started) {
        this.updated |= this.started != started;
        this.started = started;
    }

    public int getLastSessionRefresh() {
        return lastSessionRefresh;
    }

    public void setLastSessionRefresh(int lastSessionRefresh) {
        this.updated |= this.lastSessionRefresh != lastSessionRefresh;
        this.lastSessionRefresh = lastSessionRefresh;
    }

    public long getExpiration() {
        return expiration;
    }

    public void setExpiration(long expiration) {
        this.updated |= this.expiration != expiration;
        this.expiration = expiration;
    }

    public Map<String, String> getNotes() {
        return notes;
    }

    public String getNote(String name) {
        return notes.get(name);
    }

    public void setNotes(Map<String, String> notes) {
        this.updated |= !Objects.equals(this.notes, notes);
        this.notes = notes;
    }

    public String removeNote(String name) {
        String note = this.notes.remove(name);
        this.updated |= note != null;
        return note;
    }

    public void addNote(String name, String value) {
        this.updated |= !Objects.equals(this.notes.put(name, value), value);
    }

    public UserSessionModel.State getState() {
        return state;
    }

    public void setState(UserSessionModel.State state) {
        this.updated |= !Objects.equals(this.state, state);
        this.state = state;
    }

    public Map<String, String> getAuthenticatedClientSessions() {
        return authenticatedClientSessions;
    }

    public void setAuthenticatedClientSessions(Map<String, String> authenticatedClientSessions) {
        this.updated |= !Objects.equals(this.authenticatedClientSessions, authenticatedClientSessions);
        this.authenticatedClientSessions = authenticatedClientSessions;
    }

    public void addAuthenticatedClientSession(String clientId, String clientSessionId) {
        this.updated |= !Objects.equals(this.authenticatedClientSessions.put(clientId, clientSessionId), clientSessionId);
    }

    public String removeAuthenticatedClientSession(String clientId) {
        String entity = this.authenticatedClientSessions.remove(clientId);
        this.updated |= entity != null;
        return entity;
    }

    public void clearAuthenticatedClientSessions() {
        this.updated |= !authenticatedClientSessions.isEmpty();
        this.authenticatedClientSessions.clear();
    }

    public boolean isOffline() {
        return offline;
    }

    public void setOffline(boolean offline) {
        this.updated |= this.offline != offline;
        this.offline = offline;
    }

    public UserSessionModel.SessionPersistenceState getPersistenceState() {
        return persistenceState;
    }

    public void setPersistenceState(UserSessionModel.SessionPersistenceState persistenceState) {
        this.updated |= !Objects.equals(this.persistenceState, persistenceState);
        this.persistenceState = persistenceState;
    }

    @Override
    public String toString() {
        return String.format("%s@%08x", getId(), hashCode());
    }
}
