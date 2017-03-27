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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.infinispan.Cache;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.sessions.infinispan.entities.ClientLoginSessionEntity;
import org.keycloak.models.sessions.infinispan.entities.SessionEntity;
import org.keycloak.models.sessions.infinispan.entities.UserSessionEntity;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class AuthenticatedClientSessionAdapter implements AuthenticatedClientSessionModel {

    private final ClientLoginSessionEntity entity;
    private final InfinispanUserSessionProvider provider;
    private final Cache<String, SessionEntity> cache;
    private UserSessionAdapter userSession;

    public AuthenticatedClientSessionAdapter(ClientLoginSessionEntity entity, UserSessionAdapter userSession, InfinispanUserSessionProvider provider, Cache<String, SessionEntity> cache) {
        this.provider = provider;
        this.entity = entity;
        this.cache = cache;
        this.userSession = userSession;
    }

    private void update() {
        provider.getTx().replace(cache, userSession.getEntity().getId(), userSession.getEntity());
    }


    @Override
    public void setUserSession(UserSessionModel userSession) {
        String clientUUID = entity.getClient();
        UserSessionEntity sessionEntity = this.userSession.getEntity();

        // Dettach userSession
        if (userSession == null) {
            if (sessionEntity.getClientLoginSessions() != null) {
                sessionEntity.getClientLoginSessions().remove(clientUUID);
                update();
                this.userSession = null;
            }
        } else {
            this.userSession = (UserSessionAdapter) userSession;

            if (sessionEntity.getClientLoginSessions() == null) {
                sessionEntity.setClientLoginSessions(new HashMap<>());
            }
            sessionEntity.getClientLoginSessions().put(clientUUID, entity);
            update();
        }
    }

    @Override
    public UserSessionModel getUserSession() {
        return this.userSession;
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
    public String getId() {
        return entity.getId();
    }

    @Override
    public RealmModel getRealm() {
        return userSession.getRealm();
    }

    @Override
    public ClientModel getClient() {
        String client = entity.getClient();
        return getRealm().getClientById(client);
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
    public String getProtocol() {
        return entity.getAuthMethod();
    }

    @Override
    public void setProtocol(String method) {
        entity.setAuthMethod(method);
        update();
    }

    @Override
    public Set<String> getRoles() {
        return entity.getRoles();
    }

    @Override
    public void setRoles(Set<String> roles) {
        entity.setRoles(roles);
        update();
    }

    @Override
    public Set<String> getProtocolMappers() {
        return entity.getProtocolMappers();
    }

    @Override
    public void setProtocolMappers(Set<String> protocolMappers) {
        entity.setProtocolMappers(protocolMappers);
        update();
    }

    @Override
    public String getNote(String name) {
        return entity.getNotes()==null ? null : entity.getNotes().get(name);
    }

    @Override
    public void setNote(String name, String value) {
        if (entity.getNotes() == null) {
            entity.setNotes(new HashMap<>());
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
}
