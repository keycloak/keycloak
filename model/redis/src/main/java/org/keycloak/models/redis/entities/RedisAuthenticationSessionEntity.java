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

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashSet;

/**
 * Entity representing a root authentication session stored in Redis.
 */
public class RedisAuthenticationSessionEntity {

    private String id;
    private String realmId;
    private int timestamp;
    private Map<String, RedisAuthenticationTabEntity> authenticationSessions = new ConcurrentHashMap<>();

    public RedisAuthenticationSessionEntity() {}

    public RedisAuthenticationSessionEntity(String id, String realmId, int timestamp) {
        this.id = id;
        this.realmId = realmId;
        this.timestamp = timestamp;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getRealmId() { return realmId; }
    public void setRealmId(String realmId) { this.realmId = realmId; }

    public int getTimestamp() { return timestamp; }
    public void setTimestamp(int timestamp) { this.timestamp = timestamp; }

    public Map<String, RedisAuthenticationTabEntity> getAuthenticationSessions() { return authenticationSessions; }
    public void setAuthenticationSessions(Map<String, RedisAuthenticationTabEntity> authenticationSessions) {
        this.authenticationSessions = authenticationSessions;
    }

    public RedisAuthenticationTabEntity getAuthenticationSession(String tabId) {
        return authenticationSessions.get(tabId);
    }

    public void setAuthenticationSession(String tabId, RedisAuthenticationTabEntity session) {
        authenticationSessions.put(tabId, session);
    }

    public void removeAuthenticationSession(String tabId) {
        authenticationSessions.remove(tabId);
    }

    Set<String> getTabIds() {
        return authenticationSessions.keySet();
    }

    /**
     * Entity representing an authentication tab (sub-session) within a root authentication session.
     */
    public static class RedisAuthenticationTabEntity {
        private String tabId;
        private String clientUUID;
        private String authUserId;
        private String redirectUri;
        private String action;
        private String protocol;
        private Map<String, String> clientNotes = new ConcurrentHashMap<>();
        private Map<String, String> authNotes = new ConcurrentHashMap<>();
        private Map<String, String> userSessionNotes = new ConcurrentHashMap<>();
        private Set<String> requiredActions = new HashSet<>();
        private Map<String, String> clientScopes = new ConcurrentHashMap<>();
        private Map<String, String> executionStatus = new ConcurrentHashMap<>();

        public RedisAuthenticationTabEntity() {}

        public RedisAuthenticationTabEntity(String tabId, String clientUUID) {
            this.tabId = tabId;
            this.clientUUID = clientUUID;
        }

        public String getTabId() { return tabId; }
        public void setTabId(String tabId) { this.tabId = tabId; }

        public String getClientUUID() { return clientUUID; }
        public void setClientUUID(String clientUUID) { this.clientUUID = clientUUID; }

        public String getAuthUserId() { return authUserId; }
        public void setAuthUserId(String authUserId) { this.authUserId = authUserId; }

        public String getRedirectUri() { return redirectUri; }
        public void setRedirectUri(String redirectUri) { this.redirectUri = redirectUri; }

        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }

        public String getProtocol() { return protocol; }
        public void setProtocol(String protocol) { this.protocol = protocol; }

        public Map<String, String> getClientNotes() { return clientNotes; }
        public void setClientNotes(Map<String, String> clientNotes) { this.clientNotes = clientNotes; }

        public Map<String, String> getAuthNotes() { return authNotes; }
        public void setAuthNotes(Map<String, String> authNotes) { this.authNotes = authNotes; }

        public Map<String, String> getUserSessionNotes() { return userSessionNotes; }
        public void setUserSessionNotes(Map<String, String> userSessionNotes) { this.userSessionNotes = userSessionNotes; }

        public Set<String> getRequiredActions() { return requiredActions; }
        public void setRequiredActions(Set<String> requiredActions) { this.requiredActions = requiredActions; }

        public Map<String, String> getClientScopes() { return clientScopes; }
        public void setClientScopes(Map<String, String> clientScopes) { this.clientScopes = clientScopes; }

        public Map<String, String> getExecutionStatus() { return executionStatus; }
        public void setExecutionStatus(Map<String, String> executionStatus) { this.executionStatus = executionStatus; }
    }
}
