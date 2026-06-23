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
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.SecretGenerator;
import org.keycloak.common.util.Time;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.sessions.RootAuthenticationSessionModel;

/**
 * Adapter for {@link RootAuthenticationSessionEntity}. All mutations are applied directly to the underlying JPA
 * entity. Child {@link AuthenticateSessionAdapter} instances are lazily created and cached on first access.
 */
class RootAuthenticationSessionAdapter implements RootAuthenticationSessionModel {

    private final RootAuthenticationSessionEntity entity;
    private final RealmModel realm;
    private final KeycloakSession session;
    private final int authSessionsLimit;
    private Map<String, AuthenticateSessionAdapter> adapters;

    public static RootAuthenticationSessionAdapter create(KeycloakSession session, RealmModel realm, String id, int timestamp, int authSessionsLimit) {
        assert session != null;
        assert realm != null;
        var entity = new RootAuthenticationSessionEntity();
        entity.setId(id);
        entity.setTimestamp(timestamp);
        entity.setAuthenticationSessions(new HashMap<>());
        entity.setRealmId(realm.getId());
        return new RootAuthenticationSessionAdapter(entity, realm, session, authSessionsLimit);
    }

    public static RootAuthenticationSessionAdapter wrapEntity(KeycloakSession session, RealmModel realm, RootAuthenticationSessionEntity entity, int authSessionsLimit) {
        assert session != null;
        assert realm != null;
        assert entity != null;
        assert Objects.equals(realm.getId(), entity.getRealmId());

        return new RootAuthenticationSessionAdapter(entity, realm, session, authSessionsLimit);
    }

    private RootAuthenticationSessionAdapter(RootAuthenticationSessionEntity entity, RealmModel realm, KeycloakSession session, int authSessionsLimit) {
        this.entity = Objects.requireNonNull(entity);
        this.realm = Objects.requireNonNull(realm);
        this.session = Objects.requireNonNull(session);
        this.authSessionsLimit = authSessionsLimit;
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
        return Math.toIntExact(entity.getTimestamp());
    }

    @Override
    public void setTimestamp(int timestamp) {
        entity.setTimestamp(timestamp);
    }

    @Override
    public Map<String, AuthenticationSessionModel> getAuthenticationSessions() {
        return Collections.unmodifiableMap(adapters());
    }

    @Override
    public AuthenticationSessionModel getAuthenticationSession(ClientModel client, String tabId) {
        if (client == null || tabId == null) {
            return null;
        }
        var adapter = adapters().get(tabId);
        if (adapter == null || !Objects.equals(client, adapter.getClient())) {
            return null;
        }
        session.getContext().setAuthenticationSession(adapter);
        return adapter;
    }

    @Override
    public AuthenticationSessionModel createAuthenticationSession(ClientModel client) {
        var tabId = Base64Url.encode(SecretGenerator.getInstance().randomBytes(8));
        var timestamp = Time.currentTime();

        if (entity.getAuthenticationSessions().size() >= authSessionsLimit) {
            entity.getAuthenticationSessions().values().stream()
                    .min(Comparator.comparingLong(AuthenticationSessionEntity::getTimestamp))
                    .map(AuthenticationSessionEntity::getTabId)
                    .ifPresent(this::removeAuthenticationSessionByTabId);
        }

        var adapter = AuthenticateSessionAdapter.create(this, session, tabId, client.getId(), timestamp);
        entity.getAuthenticationSessions().put(tabId, adapter.getEntity());

        if (adapters != null) {
            adapters.put(tabId, adapter);
        }
        session.getContext().setAuthenticationSession(adapter);
        return adapter;
    }

    @Override
    public void removeAuthenticationSessionByTabId(String tabId) {
        session.getProvider(JpaConnectionProvider.class).getEntityManager().remove(entity.getAuthenticationSessions().remove(tabId));
        if (adapters != null) {
            adapters.remove(tabId);
        }
        if (entity.getAuthenticationSessions().isEmpty()) {
            session.getProvider(JpaConnectionProvider.class).getEntityManager().remove(entity);
        }
    }

    @Override
    public void restartSession(RealmModel realm) {
        entity.getAuthenticationSessions().clear();
        entity.setTimestamp(Time.currentTime());
        if (adapters != null) {
            adapters.clear();
        }
    }

    @Override
    public String toString() {
        return "RootAuthenticationSessionAdapter{" +
                "entity=" + entity +
                '}';
    }

    public RootAuthenticationSessionEntity getEntity() {
        return entity;
    }

    private Map<String, AuthenticateSessionAdapter> adapters() {
        if (adapters == null) {
            adapters = new HashMap<>();
            entity.getAuthenticationSessions().forEach((tabId, authEntity) -> {
                var client = realm.getClientById(authEntity.getClientUUID());
                if (client != null) {
                    adapters.put(tabId, new AuthenticateSessionAdapter(authEntity, this, session));
                }
            });
        }
        return adapters;
    }


}
