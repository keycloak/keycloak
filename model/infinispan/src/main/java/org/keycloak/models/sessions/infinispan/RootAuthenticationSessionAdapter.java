/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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

import java.util.HashMap;
import java.util.Map;

import org.infinispan.Cache;
import org.keycloak.common.util.Time;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.sessions.infinispan.entities.AuthenticationSessionEntity;
import org.keycloak.models.sessions.infinispan.entities.RootAuthenticationSessionEntity;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.sessions.RootAuthenticationSessionModel;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class RootAuthenticationSessionAdapter implements RootAuthenticationSessionModel {

    private KeycloakSession session;
    private InfinispanAuthenticationSessionProvider provider;
    private Cache<String, RootAuthenticationSessionEntity> cache;
    private RealmModel realm;
    private RootAuthenticationSessionEntity entity;

    public RootAuthenticationSessionAdapter(KeycloakSession session, InfinispanAuthenticationSessionProvider provider,
                                            Cache<String, RootAuthenticationSessionEntity> cache, RealmModel realm,
                                            RootAuthenticationSessionEntity entity) {
        this.session = session;
        this.provider = provider;
        this.cache = cache;
        this.realm = realm;
        this.entity = entity;
    }

    void update() {
        provider.tx.replace(cache, entity.getId(), entity);
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
    public int getTimestamp() {
        return entity.getTimestamp();
    }

    @Override
    public void setTimestamp(int timestamp) {
        entity.setTimestamp(timestamp);
        update();
    }

    @Override
    public Map<String, AuthenticationSessionModel> getAuthenticationSessions() {
        Map<String, AuthenticationSessionModel> result = new HashMap<>();

        for (Map.Entry<String, AuthenticationSessionEntity> entry : entity.getAuthenticationSessions().entrySet()) {
            String tabId = entry.getKey();
            result.put(tabId , new AuthenticationSessionAdapter(session, this, tabId, entry.getValue()));
        }

        return result;
    }

    @Override
    public AuthenticationSessionModel getAuthenticationSession(ClientModel client, String tabId) {
        if (client == null || tabId == null) {
            return null;
        }

        AuthenticationSessionModel authSession = getAuthenticationSessions().get(tabId);
        if (authSession != null && client.equals(authSession.getClient())) {
            session.getContext().setAuthenticationSession(authSession);
            return authSession;
        } else {
            return null;
        }
    }

    @Override
    public AuthenticationSessionModel createAuthenticationSession(ClientModel client) {
        AuthenticationSessionEntity authSessionEntity = new AuthenticationSessionEntity();
        authSessionEntity.setClientUUID(client.getId());

        String tabId = provider.generateTabId();
        entity.getAuthenticationSessions().put(tabId, authSessionEntity);

        // Update our timestamp when adding new authenticationSession
        entity.setTimestamp(Time.currentTime());

        update();

        AuthenticationSessionAdapter authSession = new AuthenticationSessionAdapter(session, this, tabId, authSessionEntity);
        session.getContext().setAuthenticationSession(authSession);
        return authSession;
    }

    @Override
    public void removeAuthenticationSessionByTabId(String tabId) {
        if (entity.getAuthenticationSessions().remove(tabId) != null) {
            if (entity.getAuthenticationSessions().isEmpty()) {
                provider.tx.remove(cache, entity.getId());
            } else {
                entity.setTimestamp(Time.currentTime());

                update();
            }
        }
    }

    @Override
    public void restartSession(RealmModel realm) {
        entity.getAuthenticationSessions().clear();
        entity.setTimestamp(Time.currentTime());
        update();
    }
}
