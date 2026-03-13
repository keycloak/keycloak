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

package org.keycloak.models.redis.session;

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.redis.entities.RedisAuthenticationSessionEntity;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.sessions.RootAuthenticationSessionModel;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Adapter for AuthenticationSessionModel backed by Redis.
 * Uses deferred writes to batch multiple updates into a single Redis PSETEX call.
 */
public class RedisAuthenticationSessionAdapter implements AuthenticationSessionModel {

    private final KeycloakSession session;
    private final RedisRootAuthenticationSessionAdapter parent;
    private final ClientModel client;
    private final RedisAuthenticationSessionEntity.RedisAuthenticationTabEntity entity;

    public RedisAuthenticationSessionAdapter(KeycloakSession session, RedisRootAuthenticationSessionAdapter parent,
                                             ClientModel client, RedisAuthenticationSessionEntity.RedisAuthenticationTabEntity entity) {
        this.session = session;
        this.parent = parent;
        this.client = client;
        this.entity = entity;
    }

    @Override public String getTabId() { return entity.getTabId(); }
    @Override public RootAuthenticationSessionModel getParentSession() { return parent; }
    @Override public RealmModel getRealm() { return parent.getRealm(); }
    @Override public ClientModel getClient() { return client; }

    @Override public String getRedirectUri() { return entity.getRedirectUri(); }
    @Override public void setRedirectUri(String uri) { entity.setRedirectUri(uri); markModified(); }

    @Override public String getAction() { return entity.getAction(); }
    @Override public void setAction(String action) { entity.setAction(action); markModified(); }

    @Override public String getProtocol() { return entity.getProtocol(); }
    @Override public void setProtocol(String method) { entity.setProtocol(method); markModified(); }

    @Override
    public UserModel getAuthenticatedUser() {
        String id = entity.getAuthUserId();
        return id == null ? null : session.users().getUserById(parent.getRealm(), id);
    }

    @Override
    public void setAuthenticatedUser(UserModel user) {
        entity.setAuthUserId(user == null ? null : user.getId());
        markModified();
    }

    @Override public Map<String, String> getClientNotes() { return new HashMap<>(entity.getClientNotes()); }
    @Override public void setClientNote(String name, String value) {
        if (value == null) entity.getClientNotes().remove(name);
        else entity.getClientNotes().put(name, value);
        markModified();
    }
    @Override public void removeClientNote(String name) { entity.getClientNotes().remove(name); markModified(); }
    @Override public String getClientNote(String name) { return entity.getClientNotes().get(name); }
    @Override public void clearClientNotes() { entity.getClientNotes().clear(); markModified(); }

    @Override public void setAuthNote(String name, String value) {
        if (value == null) entity.getAuthNotes().remove(name);
        else entity.getAuthNotes().put(name, value);
        markModified();
    }
    @Override public void removeAuthNote(String name) { entity.getAuthNotes().remove(name); markModified(); }
    @Override public String getAuthNote(String name) { return entity.getAuthNotes().get(name); }
    @Override public void clearAuthNotes() { entity.getAuthNotes().clear(); markModified(); }

    @Override public Map<String, String> getUserSessionNotes() { return new HashMap<>(entity.getUserSessionNotes()); }
    @Override public void setUserSessionNote(String name, String value) {
        if (value == null) entity.getUserSessionNotes().remove(name);
        else entity.getUserSessionNotes().put(name, value);
        markModified();
    }
    @Override public void clearUserSessionNotes() { entity.getUserSessionNotes().clear(); markModified(); }

    @Override public Set<String> getRequiredActions() { return new HashSet<>(entity.getRequiredActions()); }
    @Override public void addRequiredAction(String action) { entity.getRequiredActions().add(action); markModified(); }
    @Override public void removeRequiredAction(String action) { entity.getRequiredActions().remove(action); markModified(); }
    @Override public void addRequiredAction(UserModel.RequiredAction action) { addRequiredAction(action.name()); }
    @Override public void removeRequiredAction(UserModel.RequiredAction action) { removeRequiredAction(action.name()); }

    @Override public Map<String, ExecutionStatus> getExecutionStatus() {
        Map<String, ExecutionStatus> result = new HashMap<>();
        for (Map.Entry<String, String> e : entity.getExecutionStatus().entrySet()) {
            result.put(e.getKey(), ExecutionStatus.valueOf(e.getValue()));
        }
        return result;
    }
    @Override public void setExecutionStatus(String authenticator, ExecutionStatus status) {
        entity.getExecutionStatus().put(authenticator, status.name());
        markModified();
    }
    @Override public void clearExecutionStatus() { entity.getExecutionStatus().clear(); markModified(); }

    @Override public Set<String> getClientScopes() { return new HashSet<>(entity.getClientScopes().keySet()); }
    @Override public void setClientScopes(Set<String> scopes) {
        entity.getClientScopes().clear();
        if (scopes != null) scopes.forEach(s -> entity.getClientScopes().put(s, s));
        markModified();
    }

    /**
     * Marks the parent root session as modified.
     * This delegates the deferred write logic to the parent, which will batch
     * all tab changes into a single Redis PSETEX call on transaction commit.
     */
    private void markModified() {
        parent.markModified();
    }
}
