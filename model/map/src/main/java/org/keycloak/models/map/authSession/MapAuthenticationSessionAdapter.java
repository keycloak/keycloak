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

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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
        Map<String, ExecutionStatus> executionStatus = entity.getExecutionStatuses();
        return executionStatus == null ? Collections.emptyMap() : Collections.unmodifiableMap(executionStatus);
    }

    @Override
    public void setExecutionStatus(String authenticator, ExecutionStatus status) {
        Objects.requireNonNull(authenticator, "The provided authenticator can't be null!");
        Objects.requireNonNull(status, "The provided execution status can't be null!");
        this.entity.setExecutionStatus(authenticator, status);
    }

    @Override
    public void clearExecutionStatus() {
        entity.setExecutionStatuses(null);
    }

    @Override
    public UserModel getAuthenticatedUser() {
        return entity.getAuthUserId() == null ? null : session.users().getUserById(getRealm(), entity.getAuthUserId());
    }

    @Override
    public void setAuthenticatedUser(UserModel user) {
        String userId = (user == null) ? null : user.getId();
        entity.setAuthUserId(userId);
    }

    @Override
    public Set<String> getRequiredActions() {
        Set<String> requiredActions = entity.getRequiredActions();
        return requiredActions == null ? Collections.emptySet() : Collections.unmodifiableSet(requiredActions);
    }

    @Override
    public void addRequiredAction(String action) {
        Objects.requireNonNull(action, "The provided action can't be null!");
        entity.addRequiredAction(action);
    }

    @Override
    public void removeRequiredAction(String action) {
        Objects.requireNonNull(action, "The provided action can't be null!");
        entity.removeRequiredAction(action);
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
        entity.setUserSessionNote(name, value);
    }

    @Override
    public Map<String, String> getUserSessionNotes() {
        Map<String, String> userSessionNotes = entity.getUserSessionNotes();
        return userSessionNotes == null ? Collections.emptyMap() : Collections.unmodifiableMap(userSessionNotes);
    }

    @Override
    public void clearUserSessionNotes() {
        entity.setUserSessionNotes(null);
    }

    @Override
    public String getAuthNote(String name) {
        Map<String, String> authNotes = entity.getAuthNotes();
        return (name != null && authNotes != null) ? authNotes.get(name) : null;
    }

    @Override
    public void setAuthNote(String name, String value) {
        entity.setAuthNote(name, value);
    }

    @Override
    public void removeAuthNote(String name) {
        entity.removeAuthNote(name);
    }

    @Override
    public void clearAuthNotes() {
        entity.setAuthNotes(null);
    }

    @Override
    public String getClientNote(String name) {
        return (name != null) ? getClientNotes().get(name) : null;
    }

    @Override
    public void setClientNote(String name, String value) {
        entity.setClientNote(name, value);
    }

    @Override
    public void removeClientNote(String name) {
        entity.removeClientNote(name);
    }

    @Override
    public Map<String, String> getClientNotes() {
        Map<String, String> clientNotes = entity.getClientNotes();
        return clientNotes == null ? Collections.emptyMap() : Collections.unmodifiableMap(clientNotes);
    }

    @Override
    public void clearClientNotes() {
        entity.setClientNotes(null);
    }

    @Override
    public Set<String> getClientScopes() {
        Set<String> clientScopes = entity.getClientScopes();
        return clientScopes == null ? Collections.emptySet() : Collections.unmodifiableSet(clientScopes);
    }

    @Override
    public void setClientScopes(Set<String> clientScopes) {
        Objects.requireNonNull(clientScopes, "The provided client scopes set can't be null!");
        entity.setClientScopes(clientScopes);
    }

    @Override
    public String getRedirectUri() {
        return entity.getRedirectUri();
    }

    @Override
    public void setRedirectUri(String uri) {
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
        entity.setAction(action);
    }

    @Override
    public String getProtocol() {
        return entity.getProtocol();
    }

    @Override
    public void setProtocol(String method) {
        entity.setProtocol(method);
    }
}
