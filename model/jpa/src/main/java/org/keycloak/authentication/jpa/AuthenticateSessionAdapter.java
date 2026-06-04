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

package org.keycloak.authentication.jpa;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.keycloak.common.Profile;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.light.LightweightUserAdapter;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.sessions.RootAuthenticationSessionModel;

import static org.keycloak.models.Constants.SESSION_NOTE_LIGHTWEIGHT_USER;
import static org.keycloak.models.light.LightweightUserAdapter.isLightweightUser;

/**
 * Adapter for {@link AuthenticationSessionEntity}. All mutations are applied directly to the underlying JPA entity,
 * with collection fields (notes, execution status, required actions, etc.) lazily deserialized from JSON on first
 * access and re-serialized on every write.
 */
class AuthenticateSessionAdapter implements AuthenticationSessionModel {

    private final AuthenticationSessionEntity entity;
    private final RootAuthenticationSessionAdapter parentSession;
    private final KeycloakSession session;

    private Map<String, ExecutionStatus> executionStatus;
    private Map<String, String> clientNotes;
    private Map<String, String> authNotes;
    private Map<String, String> userSessionNotes;
    private Set<String> requiredActions;
    private Set<String> clientScopes;

    public AuthenticateSessionAdapter(AuthenticationSessionEntity entity, RootAuthenticationSessionAdapter parentSession, KeycloakSession session) {
        this.entity = Objects.requireNonNull(entity);
        this.parentSession = Objects.requireNonNull(parentSession);
        this.session = Objects.requireNonNull(session);
    }

    public static AuthenticateSessionAdapter create(RootAuthenticationSessionAdapter parentSession, KeycloakSession session, String tabId, String clientUUID, int timestamp) {
        var authEntity = new AuthenticationSessionEntity();
        authEntity.setTabId(tabId);
        authEntity.setRootAuthenticationSession(parentSession.getEntity());
        authEntity.setClientUUID(clientUUID);
        authEntity.setTimestamp(timestamp);
        AuthenticationSessionSerialization.setClientScopes(authEntity, Set.of());
        AuthenticationSessionSerialization.setExecutionStatus(authEntity, Map.of());
        AuthenticationSessionSerialization.setClientNotes(authEntity, Map.of());
        AuthenticationSessionSerialization.setAuthNotes(authEntity, Map.of());
        AuthenticationSessionSerialization.setRequiredActions(authEntity, Set.of());
        AuthenticationSessionSerialization.setUserSessionNotes(authEntity, Map.of());
        return new AuthenticateSessionAdapter(authEntity, parentSession, session);
    }

    @Override
    public String getTabId() {
        return entity.getTabId();
    }

    @Override
    public RootAuthenticationSessionModel getParentSession() {
        return parentSession;
    }

    @Override
    public RealmModel getRealm() {
        return parentSession.getRealm();
    }

    @Override
    public ClientModel getClient() {
        return parentSession.getRealm().getClientById(entity.getClientUUID());
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

    @Override
    public UserModel getAuthenticatedUser() {
        if (entity.getAuthUserId() == null) {
            return null;
        }
        if (Profile.isFeatureEnabled(Profile.Feature.TRANSIENT_USERS)) {
            var luaSerialization = userSessionNotes().get(SESSION_NOTE_LIGHTWEIGHT_USER);
            if (luaSerialization != null) {
                LightweightUserAdapter cachedUser = session.getAttribute("authSession.user." + parentSession.getId(), LightweightUserAdapter.class);
                if (cachedUser != null) {
                    return cachedUser;
                }
                LightweightUserAdapter lua = LightweightUserAdapter.fromString(session, getRealm(), luaSerialization);
                session.setAttribute("authSession.user." + parentSession.getId(), lua);
                lua.setUpdateHandler(lua1 -> {
                    if (lua == lua1) {
                        setUserSessionNote(SESSION_NOTE_LIGHTWEIGHT_USER, lua1.serialize());
                    }
                });
                return lua;
            }
        }
        return session.users().getUserById(getRealm(), entity.getAuthUserId());
    }

    @Override
    public void setAuthenticatedUser(UserModel user) {
        if (user == null) {
            entity.setAuthUserId(null);
            setUserSessionNote(SESSION_NOTE_LIGHTWEIGHT_USER, null);
            return;
        }
        entity.setAuthUserId(user.getId());
        if (isLightweightUser(user)) {
            LightweightUserAdapter lua = (LightweightUserAdapter) user;
            setUserSessionNote(SESSION_NOTE_LIGHTWEIGHT_USER, lua.serialize());
            lua.setUpdateHandler(lua1 -> {
                if (lua == lua1) {
                    setUserSessionNote(SESSION_NOTE_LIGHTWEIGHT_USER, lua1.serialize());
                }
            });
        }
    }

    @Override
    public Map<String, ExecutionStatus> getExecutionStatus() {
        return Collections.unmodifiableMap(executionStatus());
    }

    @Override
    public void setExecutionStatus(String authenticator, ExecutionStatus status) {
        executionStatus().put(authenticator, status);
        AuthenticationSessionSerialization.setExecutionStatus(entity, executionStatus);
    }

    @Override
    public void clearExecutionStatus() {
        executionStatus = new HashMap<>();
        AuthenticationSessionSerialization.setExecutionStatus(entity, executionStatus);
    }

    @Override
    public Set<String> getRequiredActions() {
        return Collections.unmodifiableSet(requiredActions());
    }

    @Override
    public void addRequiredAction(String action) {
        requiredActions().add(action);
        AuthenticationSessionSerialization.setRequiredActions(entity, requiredActions);
    }

    @Override
    public void removeRequiredAction(String action) {
        requiredActions().remove(action);
        AuthenticationSessionSerialization.setRequiredActions(entity, requiredActions);
    }

    @Override
    public void addRequiredAction(UserModel.RequiredAction action) {
        addRequiredAction(action.name());
    }

    @Override
    public void removeRequiredAction(UserModel.RequiredAction action) {
        removeRequiredAction(action.name());
    }

    @Override
    public void setUserSessionNote(String name, String value) {
        if (name == null) {
            return;
        }
        if (value == null) {
            userSessionNotes().remove(name);
        } else {
            userSessionNotes().put(name, value);
        }
        AuthenticationSessionSerialization.setUserSessionNotes(entity, userSessionNotes);
    }

    @Override
    public Map<String, String> getUserSessionNotes() {
        return Collections.unmodifiableMap(userSessionNotes());
    }

    @Override
    public void clearUserSessionNotes() {
        userSessionNotes = new HashMap<>();
        AuthenticationSessionSerialization.setUserSessionNotes(entity, userSessionNotes);
    }

    @Override
    public String getAuthNote(String name) {
        return name == null ? null : authNotes().get(name);
    }

    @Override
    public void setAuthNote(String name, String value) {
        if (name == null) {
            return;
        }
        if (value == null) {
            authNotes().remove(name);
        } else {
            authNotes().put(name, value);
        }
        AuthenticationSessionSerialization.setAuthNotes(entity, authNotes);
    }

    @Override
    public void removeAuthNote(String name) {
        if (name == null) {
            return;
        }
        authNotes().remove(name);
        AuthenticationSessionSerialization.setAuthNotes(entity, authNotes);
    }

    @Override
    public void clearAuthNotes() {
        authNotes = new HashMap<>();
        AuthenticationSessionSerialization.setAuthNotes(entity, authNotes);
    }

    @Override
    public String getClientNote(String name) {
        return name == null ? null : clientNotes().get(name);
    }

    @Override
    public void setClientNote(String name, String value) {
        if (name == null) {
            return;
        }
        if (value == null) {
            clientNotes().remove(name);
        } else {
            clientNotes().put(name, value);
        }
        AuthenticationSessionSerialization.setClientNotes(entity, clientNotes);
    }

    @Override
    public void removeClientNote(String name) {
        if (name == null) {
            return;
        }
        clientNotes().remove(name);
        AuthenticationSessionSerialization.setClientNotes(entity, clientNotes);
    }

    @Override
    public Map<String, String> getClientNotes() {
        return Collections.unmodifiableMap(clientNotes());
    }

    @Override
    public void clearClientNotes() {
        clientNotes = new HashMap<>();
        AuthenticationSessionSerialization.setClientNotes(entity, clientNotes);
    }

    @Override
    public Set<String> getClientScopes() {
        return Collections.unmodifiableSet(clientScopes());
    }

    @Override
    public void setClientScopes(Set<String> clientScopes) {
        this.clientScopes = new HashSet<>(clientScopes);
        AuthenticationSessionSerialization.setClientScopes(entity, this.clientScopes);
    }

    @Override
    public String toString() {
        return "AuthenticateSessionAdapter{" +
                "entity=" + entity +
                '}';
    }

    public AuthenticationSessionEntity getEntity() {
        return entity;
    }

    private Map<String, ExecutionStatus> executionStatus() {
        if (executionStatus == null) {
            executionStatus = nullSafeMap(AuthenticationSessionSerialization.getExecutionStatus(entity));
        }
        return executionStatus;
    }

    private Map<String, String> clientNotes() {
        if (clientNotes == null) {
            clientNotes = nullSafeMap(AuthenticationSessionSerialization.getClientNotes(entity));
        }
        return clientNotes;
    }

    private Map<String, String> authNotes() {
        if (authNotes == null) {
            authNotes = nullSafeMap(AuthenticationSessionSerialization.getAuthNotes(entity));
        }
        return authNotes;
    }

    private Map<String, String> userSessionNotes() {
        if (userSessionNotes == null) {
            userSessionNotes = nullSafeMap(AuthenticationSessionSerialization.getUserSessionNotes(entity));
        }
        return userSessionNotes;
    }

    private Set<String> requiredActions() {
        if (requiredActions == null) {
            requiredActions = nullSafeSet(AuthenticationSessionSerialization.getRequiredActions(entity));
        }
        return requiredActions;
    }

    private Set<String> clientScopes() {
        if (clientScopes == null) {
            clientScopes = nullSafeSet(AuthenticationSessionSerialization.getClientScopes(entity));
        }
        return clientScopes;
    }

    private static <K, V> Map<K, V> nullSafeMap(Map<K, V> map) {
        return new HashMap<>(Objects.requireNonNullElse(map, Map.of()));
    }

    private static <V> Set<V> nullSafeSet(Set<V> set) {
        return new HashSet<>(Objects.requireNonNullElse(set, Set.of()));
    }
}
