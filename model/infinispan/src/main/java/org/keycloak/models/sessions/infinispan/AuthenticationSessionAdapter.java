/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.sessions.infinispan;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.keycloak.common.Profile;
import org.keycloak.common.Profile.Feature;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.light.LightweightUserAdapter;
import org.keycloak.models.sessions.infinispan.entities.AuthenticationSessionEntity;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.sessions.RootAuthenticationSessionModel;

import static org.keycloak.models.Constants.SESSION_NOTE_LIGHTWEIGHT_USER;
import static org.keycloak.models.light.LightweightUserAdapter.isLightweightUser;

/**
 * NOTE: Calling setter doesn't automatically enlist for update
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class AuthenticationSessionAdapter implements AuthenticationSessionModel {

    private final KeycloakSession session;
    private final RootAuthenticationSessionModel parent;
    private final  SessionEntityUpdater<AuthenticationSessionEntity> updater;
    private final String tabId;

    public AuthenticationSessionAdapter(KeycloakSession session, RootAuthenticationSessionModel parent, SessionEntityUpdater<AuthenticationSessionEntity> updater, String tabId) {
        this.session = session;
        this.parent = parent;
        this.updater = updater;
        this.tabId = tabId;
    }

    private void update() {
        updater.onEntityUpdated();
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
    public RealmModel getRealm() {
        return parent.getRealm();
    }

    @Override
    public ClientModel getClient() {
        return getRealm().getClientById(updater.getEntity().getClientUUID());
    }

    @Override
    public String getRedirectUri() {
        return updater.getEntity().getRedirectUri();
    }

    @Override
    public void setRedirectUri(String uri) {
        updater.getEntity().setRedirectUri(uri);
        update();
    }


    @Override
    public String getAction() {
        return updater.getEntity().getAction();
    }

    @Override
    public void setAction(String action) {
        updater.getEntity().setAction(action);
        update();
    }

    @Override
    public Set<String> getClientScopes() {
        if (updater.getEntity().getClientScopes() == null || updater.getEntity().getClientScopes().isEmpty()) {
            return Collections.emptySet();
        }
        return new HashSet<>(updater.getEntity().getClientScopes());
    }

    @Override
    public void setClientScopes(Set<String> clientScopes) {
        updater.getEntity().setClientScopes(clientScopes);
        update();
    }

    @Override
    public String getProtocol() {
        return updater.getEntity().getProtocol();
    }

    @Override
    public void setProtocol(String protocol) {
        updater.getEntity().setProtocol(protocol);
        update();
    }

    @Override
    public String getClientNote(String name) {
        return (updater.getEntity().getClientNotes() != null && name != null) ? updater.getEntity().getClientNotes().get(name) : null;
    }

    @Override
    public void setClientNote(String name, String value) {
        if (updater.getEntity().getClientNotes() == null) {
            updater.getEntity().setClientNotes(new ConcurrentHashMap<>());
        }
        if (name != null) {
            if (value == null) {
                updater.getEntity().getClientNotes().remove(name);
            } else {
                updater.getEntity().getClientNotes().put(name, value);
            }
        }
        update();
    }

    @Override
    public void removeClientNote(String name) {
        if (updater.getEntity().getClientNotes() != null && name != null) {
            updater.getEntity().getClientNotes().remove(name);
        }
        update();
    }

    @Override
    public Map<String, String> getClientNotes() {
        if (updater.getEntity().getClientNotes() == null || updater.getEntity().getClientNotes().isEmpty()) {
            return Collections.emptyMap();
        }
        return new ConcurrentHashMap<>(updater.getEntity().getClientNotes());
    }

    @Override
    public void clearClientNotes() {
        updater.getEntity().setClientNotes(new ConcurrentHashMap<>());
        update();
    }

    @Override
    public String getAuthNote(String name) {
        return (updater.getEntity().getAuthNotes() != null && name != null) ? updater.getEntity().getAuthNotes().get(name) : null;
    }

    @Override
    public void setAuthNote(String name, String value) {
        if (updater.getEntity().getAuthNotes() == null) {
            updater.getEntity().setAuthNotes(new ConcurrentHashMap<>());
        }
        if (name != null) {
            if (value == null) {
                updater.getEntity().getAuthNotes().remove(name);
            } else {
                updater.getEntity().getAuthNotes().put(name, value);
            }
        }
        update();
    }

    @Override
    public void removeAuthNote(String name) {
        if (updater.getEntity().getAuthNotes() != null && name != null) {
            updater.getEntity().getAuthNotes().remove(name);
        }
        update();
    }

    @Override
    public void clearAuthNotes() {
        updater.getEntity().setAuthNotes(new ConcurrentHashMap<>());
        update();
    }

    @Override
    public void setUserSessionNote(String name, String value) {
        if (updater.getEntity().getUserSessionNotes() == null) {
            updater.getEntity().setUserSessionNotes(new ConcurrentHashMap<>());
        }
        if (name != null) {
            if (value == null) {
                updater.getEntity().getUserSessionNotes().remove(name);
            } else {
                updater.getEntity().getUserSessionNotes().put(name, value);
            }
        }
        update();

    }

    @Override
    public Map<String, String> getUserSessionNotes() {
        if (updater.getEntity().getUserSessionNotes() == null) {
            return Collections.emptyMap();
        }
        return new ConcurrentHashMap<>(updater.getEntity().getUserSessionNotes());
    }

    @Override
    public void clearUserSessionNotes() {
        updater.getEntity().setUserSessionNotes(new ConcurrentHashMap<>());
        update();

    }

    @Override
    public Set<String> getRequiredActions() {
        return new HashSet<>(updater.getEntity().getRequiredActions());
    }

    @Override
    public void addRequiredAction(String action) {
        updater.getEntity().getRequiredActions().add(action);
        update();

    }

    @Override
    public void removeRequiredAction(String action) {
        updater.getEntity().getRequiredActions().remove(action);
        update();

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
    public Map<String, AuthenticationSessionModel.ExecutionStatus> getExecutionStatus() {

        return updater.getEntity().getExecutionStatus();
    }

    @Override
    public void setExecutionStatus(String authenticator, AuthenticationSessionModel.ExecutionStatus status) {
        updater.getEntity().getExecutionStatus().put(authenticator, status);
        update();

    }

    @Override
    public void clearExecutionStatus() {
        updater.getEntity().getExecutionStatus().clear();
        update();
    }

    @Override
    public UserModel getAuthenticatedUser() {
        if (updater.getEntity().getAuthUserId() == null) {
            return null;
        }

        if (Profile.isFeatureEnabled(Feature.TRANSIENT_USERS) && getUserSessionNotes().containsKey(SESSION_NOTE_LIGHTWEIGHT_USER)) {
            LightweightUserAdapter cachedUser = session.getAttribute("authSession.user." + parent.getId(), LightweightUserAdapter.class);

            if (cachedUser != null) {
                return cachedUser;
            }

            LightweightUserAdapter lua = LightweightUserAdapter.fromString(session, parent.getRealm(), getUserSessionNotes().get(SESSION_NOTE_LIGHTWEIGHT_USER));
            session.setAttribute("authSession.user." + parent.getId(), lua);
            lua.setUpdateHandler(lua1 -> {
                if (lua == lua1) {  // Ensure there is no conflicting user model, only the latest lightweight user can be used
                    setUserSessionNote(SESSION_NOTE_LIGHTWEIGHT_USER, lua1.serialize());
                }
            });

            return lua;
        } else {
            return session.users().getUserById(getRealm(), updater.getEntity().getAuthUserId());
        }
    }

    @Override
    public void setAuthenticatedUser(UserModel user) {
        if (user == null) {
            updater.getEntity().setAuthUserId(null);
            setUserSessionNote(SESSION_NOTE_LIGHTWEIGHT_USER, null);
        } else {
            updater.getEntity().setAuthUserId(user.getId());

            if (isLightweightUser(user)) {
                LightweightUserAdapter lua = (LightweightUserAdapter) user;
                setUserSessionNote(SESSION_NOTE_LIGHTWEIGHT_USER, lua.serialize());
                lua.setUpdateHandler(lua1 -> {
                    if (lua == lua1) {  // Ensure there is no conflicting user model, only the latest lightweight user can be used
                        setUserSessionNote(SESSION_NOTE_LIGHTWEIGHT_USER, lua1.serialize());
                    }
                });
            }
        }
        update();
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof AuthenticationSessionModel that && that.getTabId().equals(getTabId());

    }

    @Override
    public int hashCode() {
        return getTabId().hashCode();
    }

}
