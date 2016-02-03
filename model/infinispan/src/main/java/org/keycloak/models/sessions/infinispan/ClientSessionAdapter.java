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

import org.infinispan.Cache;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.sessions.infinispan.entities.ClientSessionEntity;
import org.keycloak.models.sessions.infinispan.entities.SessionEntity;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ClientSessionAdapter implements ClientSessionModel {

    private KeycloakSession session;
    private InfinispanUserSessionProvider provider;
    private Cache<String, SessionEntity> cache;
    private RealmModel realm;
    private ClientSessionEntity entity;
    private boolean offline;

    public ClientSessionAdapter(KeycloakSession session, InfinispanUserSessionProvider provider, Cache<String, SessionEntity> cache, RealmModel realm,
                                ClientSessionEntity entity, boolean offline) {
        this.session = session;
        this.provider = provider;
        this.cache = cache;
        this.realm = realm;
        this.entity = entity;
        this.offline = offline;
    }

    @Override
    public String getId() {
        return entity.getId();
    }

    @Override
    public RealmModel getRealm() {
        return realm;
    }

    @Override
    public ClientModel getClient() {
        return realm.getClientById(entity.getClient());
    }

    @Override
    public UserSessionAdapter getUserSession() {
        return entity.getUserSession() != null ? provider.getUserSession(realm, entity.getUserSession(), offline) : null;
    }

    @Override
    public void setUserSession(UserSessionModel userSession) {
        if (userSession == null) {
            if (entity.getUserSession() != null) {
                provider.dettachSession(getUserSession(), this);
            }
            entity.setUserSession(null);
        } else {
            UserSessionAdapter userSessionAdapter = (UserSessionAdapter) userSession;
            if (entity.getUserSession() != null) {
                if (entity.getUserSession().equals(userSession.getId())) {
                    return;
                } else {
                    provider.dettachSession(userSessionAdapter, this);
                }
            } else {
                provider.attachSession(userSessionAdapter, this);
            }

            entity.setUserSession(userSession.getId());
        }
        update();
    }

    @Override
    public String getRedirectUri() {
        return entity.getRedirectUri();
    }

    @Override
    public void setRedirectUri(String uri) {
        entity.setRedirectUri(uri);
        update();
    }

    @Override
    public int getTimestamp() {
        return entity.getTimestamp();
    }

    @Override
    public void setTimestamp(int timestamp) {
        entity.setTimestamp(timestamp);
        update();
    }

    @Override
    public String getAction() {
        return entity.getAction();
    }

    @Override
    public void setAction(String action) {
        entity.setAction(action);
        update();
    }

    @Override
    public Set<String> getRoles() {
        if (entity.getRoles() == null || entity.getRoles().isEmpty()) return Collections.emptySet();
        return new HashSet<>(entity.getRoles());
    }

    @Override
    public void setRoles(Set<String> roles) {
        entity.setRoles(roles);
        update();
    }

    @Override
    public Set<String> getProtocolMappers() {
        if (entity.getProtocolMappers() == null || entity.getProtocolMappers().isEmpty()) return Collections.emptySet();
        return new HashSet<>(entity.getProtocolMappers());
    }

    @Override
    public void setProtocolMappers(Set<String> protocolMappers) {
        entity.setProtocolMappers(protocolMappers);
        update();
    }

    @Override
    public String getAuthMethod() {
        return entity.getAuthMethod();
    }

    @Override
    public void setAuthMethod(String authMethod) {
        entity.setAuthMethod(authMethod);
        update();
    }

    @Override
    public String getNote(String name) {
        return entity.getNotes() != null ? entity.getNotes().get(name) : null;
    }

    @Override
    public void setNote(String name, String value) {
        if (entity.getNotes() == null) {
            entity.setNotes(new HashMap<String, String>());
        }
        entity.getNotes().put(name, value);
        update();
    }

    @Override
    public void removeNote(String name) {
        if (entity.getNotes() != null) {
            entity.getNotes().remove(name);
            update();
        }
    }

    @Override
    public Map<String, String> getNotes() {
        if (entity.getNotes() == null || entity.getNotes().isEmpty()) return Collections.emptyMap();
        Map<String, String> copy = new HashMap<>();
        copy.putAll(entity.getNotes());
        return copy;
    }

    @Override
    public void setUserSessionNote(String name, String value) {
        if (entity.getUserSessionNotes() == null) {
            entity.setUserSessionNotes(new HashMap<String, String>());
        }
        entity.getUserSessionNotes().put(name, value);
        update();

    }

    @Override
    public Map<String, String> getUserSessionNotes() {
        if (entity.getUserSessionNotes() == null) {
            return Collections.EMPTY_MAP;
        }
        HashMap<String, String> copy = new HashMap<>();
        copy.putAll(entity.getUserSessionNotes());
        return copy;
    }

    @Override
    public void clearUserSessionNotes() {
        entity.setUserSessionNotes(new HashMap<String, String>());
        update();

    }

    @Override
    public Set<String> getRequiredActions() {
        Set<String> copy = new HashSet<>();
        copy.addAll(entity.getRequiredActions());
        return copy;
    }

    @Override
    public void addRequiredAction(String action) {
        entity.getRequiredActions().add(action);
        update();

    }

    @Override
    public void removeRequiredAction(String action) {
        entity.getRequiredActions().remove(action);
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

    void update() {
        provider.getTx().replace(cache, entity.getId(), entity);
    }
    @Override
    public Map<String, ExecutionStatus> getExecutionStatus() {
        return entity.getAuthenticatorStatus();
    }

    @Override
    public void setExecutionStatus(String authenticator, ExecutionStatus status) {
        entity.getAuthenticatorStatus().put(authenticator, status);
        update();

    }

    @Override
    public void clearExecutionStatus() {
        entity.getAuthenticatorStatus().clear();
        update();
    }

    @Override
    public UserModel getAuthenticatedUser() {
        return entity.getAuthUserId() == null ? null : session.users().getUserById(entity.getAuthUserId(), realm);    }

    @Override
    public void setAuthenticatedUser(UserModel user) {
        if (user == null) entity.setAuthUserId(null);
        else entity.setAuthUserId(user.getId());
        update();

    }

}
