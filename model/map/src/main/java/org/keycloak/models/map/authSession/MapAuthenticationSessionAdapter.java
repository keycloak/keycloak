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

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.sessions.RootAuthenticationSessionModel;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author <a href="mailto:mkanis@redhat.com">Martin Kanis</a>
 */
public class MapAuthenticationSessionAdapter implements AuthenticationSessionModel {

    private final KeycloakSession session;
    private final MapRootAuthenticationSessionAdapter parent;
    private final String tabId;
    private final MapAuthenticationSessionEntity entity;

    public MapAuthenticationSessionAdapter(KeycloakSession session, MapRootAuthenticationSessionAdapter parent,
                                           String tabId, MapAuthenticationSessionEntity entity) {
        this.session = session;
        this.parent = parent;
        this.tabId = tabId;
        this.entity = entity;
    }

    @Override
    public String getTabId() {
        return tabId;
    }

    @Override
    public RootAuthenticationSessionModel getParentSession() {
        return parent;
    }

    @Override
    public Map<String, ExecutionStatus> getExecutionStatus() {
        return entity.getExecutionStatus();
    }

    @Override
    public void setExecutionStatus(String authenticator, ExecutionStatus status) {
        Objects.requireNonNull(authenticator, "The provided authenticator can't be null!");
        Objects.requireNonNull(status, "The provided execution status can't be null!");
        parent.setUpdated(!Objects.equals(entity.getExecutionStatus().put(authenticator, status), status));
    }

    @Override
    public void clearExecutionStatus() {
        parent.setUpdated(!entity.getExecutionStatus().isEmpty());
        entity.getExecutionStatus().clear();
    }

    @Override
    public UserModel getAuthenticatedUser() {
        return entity.getAuthUserId() == null ? null : session.users().getUserById(getRealm(), entity.getAuthUserId());
    }

    @Override
    public void setAuthenticatedUser(UserModel user) {
        String userId = (user == null) ? null : user.getId();
        parent.setUpdated(!Objects.equals(userId, entity.getAuthUserId()));
        entity.setAuthUserId(userId);
    }

    @Override
    public Set<String> getRequiredActions() {
        return new HashSet<>(entity.getRequiredActions());
    }

    @Override
    public void addRequiredAction(String action) {
        Objects.requireNonNull(action, "The provided action can't be null!");
        parent.setUpdated(entity.getRequiredActions().add(action));
    }

    @Override
    public void removeRequiredAction(String action) {
        Objects.requireNonNull(action, "The provided action can't be null!");
        parent.setUpdated(entity.getRequiredActions().remove(action));
    }

    @Override
    public void addRequiredAction(UserModel.RequiredAction action) {
        Objects.requireNonNull(action, "The provided action can't be null!");
        addRequiredAction(action.name());
    }

    @Override
    public void removeRequiredAction(UserModel.RequiredAction action) {
        Objects.requireNonNull(action, "The provided action can't be null!");
        removeRequiredAction(action.name());
    }

    @Override
    public void setUserSessionNote(String name, String value) {
        if (name != null) {
            if (value == null) {
                parent.setUpdated(entity.getUserSessionNotes().remove(name) != null);
            } else {
                parent.setUpdated(!Objects.equals(entity.getUserSessionNotes().put(name, value), value));
            }
        }
    }

    @Override
    public Map<String, String> getUserSessionNotes() {
        return new ConcurrentHashMap<>(entity.getUserSessionNotes());
    }

    @Override
    public void clearUserSessionNotes() {
        parent.setUpdated(!entity.getUserSessionNotes().isEmpty());
        entity.getUserSessionNotes().clear();
    }

    @Override
    public String getAuthNote(String name) {
        return (name != null) ? entity.getAuthNotes().get(name) : null;
    }

    @Override
    public void setAuthNote(String name, String value) {
        if (name != null) {
            if (value == null) {
                parent.setUpdated(entity.getAuthNotes().remove(name) != null);
            } else {
                parent.setUpdated(!Objects.equals(entity.getAuthNotes().put(name, value), value));
            }
        }
    }

    @Override
    public void removeAuthNote(String name) {
        if (name != null) {
            parent.setUpdated(entity.getAuthNotes().remove(name) != null);
        }
    }

    @Override
    public void clearAuthNotes() {
        parent.setUpdated(!entity.getAuthNotes().isEmpty());
        entity.getAuthNotes().clear();
    }

    @Override
    public String getClientNote(String name) {
        return (name != null) ? entity.getClientNotes().get(name) : null;
    }

    @Override
    public void setClientNote(String name, String value) {
        if (name != null) {
            if (value == null) {
                parent.setUpdated(entity.getClientNotes().remove(name) != null);
            } else {
                parent.setUpdated(!Objects.equals(entity.getClientNotes().put(name, value), value));
            }
        }
    }

    @Override
    public void removeClientNote(String name) {
        if (name != null) {
            parent.setUpdated(entity.getClientNotes().remove(name) != null);
        }
    }

    @Override
    public Map<String, String> getClientNotes() {
        return new ConcurrentHashMap<>(entity.getClientNotes());
    }

    @Override
    public void clearClientNotes() {
        parent.setUpdated(!entity.getClientNotes().isEmpty());
        entity.getClientNotes().clear();
    }

    @Override
    public Set<String> getClientScopes() {
        return new HashSet<>(entity.getClientScopes());
    }

    @Override
    public void setClientScopes(Set<String> clientScopes) {
        Objects.requireNonNull(clientScopes, "The provided client scopes set can't be null!");
        parent.setUpdated(!Objects.equals(entity.getClientScopes(), clientScopes));
        entity.setClientScopes(new HashSet<>(clientScopes));
    }

    @Override
    public String getRedirectUri() {
        return entity.getRedirectUri();
    }

    @Override
    public void setRedirectUri(String uri) {
        parent.setUpdated(!Objects.equals(entity.getRedirectUri(), uri));
        entity.setRedirectUri(uri);
    }

    @Override
    public RealmModel getRealm() {
        return parent.getRealm();
    }

    @Override
    public ClientModel getClient() {
        return parent.getRealm().getClientById(entity.getClientUUID());
    }

    @Override
    public String getAction() {
        return entity.getAction();
    }

    @Override
    public void setAction(String action) {
        parent.setUpdated(!Objects.equals(entity.getAction(), action));
        entity.setAction(action);
    }

    @Override
    public String getProtocol() {
        return entity.getProtocol();
    }

    @Override
    public void setProtocol(String method) {
        parent.setUpdated(!Objects.equals(entity.getProtocol(), method));
        entity.setProtocol(method);
    }
}
