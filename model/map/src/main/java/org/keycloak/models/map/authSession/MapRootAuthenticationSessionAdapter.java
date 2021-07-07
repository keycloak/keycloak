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

import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.Time;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.sessions.AuthenticationSessionModel;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:mkanis@redhat.com">Martin Kanis</a>
 */
public abstract class MapRootAuthenticationSessionAdapter<K> extends AbstractRootAuthenticationSessionModel<MapRootAuthenticationSessionEntity<K>> {

    public MapRootAuthenticationSessionAdapter(KeycloakSession session, RealmModel realm, MapRootAuthenticationSessionEntity<K> entity) {
        super(session, realm, entity);
    }

    @Override
    public RealmModel getRealm() {
        return session.realms().getRealm(entity.getRealmId());
    }

    @Override
    public int getTimestamp() {
        return entity.getTimestamp();
    }

    @Override
    public void setTimestamp(int timestamp) {
        entity.setTimestamp(timestamp);
    }

    @Override
    public Map<String, AuthenticationSessionModel> getAuthenticationSessions() {
        return entity.getAuthenticationSessions().entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> new MapAuthenticationSessionAdapter(session, this, entry.getKey(), entry.getValue())));
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
        Objects.requireNonNull(client, "The provided client can't be null!");

        MapAuthenticationSessionEntity authSessionEntity = new MapAuthenticationSessionEntity();
        authSessionEntity.setClientUUID(client.getId());

        int timestamp = Time.currentTime();
        authSessionEntity.setTimestamp(timestamp);

        String tabId =  generateTabId();
        entity.getAuthenticationSessions().put(tabId, authSessionEntity);

        // Update our timestamp when adding new authenticationSession
        entity.setTimestamp(timestamp);

        MapAuthenticationSessionAdapter authSession = new MapAuthenticationSessionAdapter(session, this, tabId, authSessionEntity);
        session.getContext().setAuthenticationSession(authSession);
        return authSession;
    }

    @Override
    public void removeAuthenticationSessionByTabId(String tabId) {
        if (entity.removeAuthenticationSession(tabId) != null) {
            if (entity.getAuthenticationSessions().isEmpty()) {
                session.authenticationSessions().removeRootAuthenticationSession(realm, this);
            } else {
                entity.setTimestamp(Time.currentTime());
            }
        }
    }

    @Override
    public void restartSession(RealmModel realm) {
        entity.clearAuthenticationSessions();
        entity.setTimestamp(Time.currentTime());
    }

    public void setUpdated(boolean updated) {
        entity.updated |= updated;
    }

    private String generateTabId() {
        return Base64Url.encode(KeycloakModelUtils.generateSecret(8));
    }
}
