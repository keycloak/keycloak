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
import org.keycloak.common.util.SecretGenerator;
import org.keycloak.common.util.Time;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.map.common.TimeAdapter;
import org.keycloak.models.utils.SessionExpiration;
import org.keycloak.sessions.AuthenticationSessionModel;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.keycloak.models.utils.SessionExpiration.getAuthSessionLifespan;

/**
 * @author <a href="mailto:mkanis@redhat.com">Martin Kanis</a>
 */
public class MapRootAuthenticationSessionAdapter extends AbstractRootAuthenticationSessionModel<MapRootAuthenticationSessionEntity> {

    public MapRootAuthenticationSessionAdapter(KeycloakSession session, RealmModel realm, MapRootAuthenticationSessionEntity entity) {
        super(session, realm, entity);
    }

    @Override
    public String getId() {
        return entity.getId();
    }

    @Override
    public RealmModel getRealm() {
        return session.realms().getRealm(entity.getRealmId());
    }

    @Override
    public int getTimestamp() {
        return TimeAdapter.fromLongWithTimeInSecondsToIntegerWithTimeInSeconds(TimeAdapter.fromMilliSecondsToSeconds(entity.getTimestamp()));
    }

    @Override
    public void setTimestamp(int timestamp) {
        entity.setTimestamp(TimeAdapter.fromSecondsToMilliseconds(timestamp));
        entity.setExpiration(TimeAdapter.fromSecondsToMilliseconds(SessionExpiration.getAuthSessionExpiration(realm, timestamp)));
    }

    @Override
    public Map<String, AuthenticationSessionModel> getAuthenticationSessions() {
        return Optional.ofNullable(entity.getAuthenticationSessions()).orElseGet(Collections::emptySet).stream()
                .collect(Collectors.toMap(MapAuthenticationSessionEntity::getTabId, this::toAdapter));
    }

    @Override
    public AuthenticationSessionModel getAuthenticationSession(ClientModel client, String tabId) {
        if (client == null || tabId == null) {
            return null;
        }

        return entity.getAuthenticationSession(tabId).map(this::toAdapter).map(this::setAuthContext).orElse(null);
    }

    @Override
    public AuthenticationSessionModel createAuthenticationSession(ClientModel client) {
        Objects.requireNonNull(client, "The provided client can't be null!");

        MapAuthenticationSessionEntity authSessionEntity = new MapAuthenticationSessionEntityImpl();
        authSessionEntity.setClientUUID(client.getId());

        long timestamp = Time.currentTimeMillis();
        authSessionEntity.setTimestamp(timestamp);
        String tabId = generateTabId();
        authSessionEntity.setTabId(tabId);

        entity.addAuthenticationSession(authSessionEntity);

        // Update our timestamp when adding new authenticationSession
        entity.setTimestamp(timestamp);

        int authSessionLifespanSeconds = getAuthSessionLifespan(realm);
        entity.setExpiration(timestamp + TimeAdapter.fromSecondsToMilliseconds(authSessionLifespanSeconds));

        return entity.getAuthenticationSession(tabId).map(this::toAdapter).map(this::setAuthContext).orElse(null);
    }

    @Override
    public void removeAuthenticationSessionByTabId(String tabId) {
        Boolean result = entity.removeAuthenticationSession(tabId);
        if (result == null || result) {
            if (entity.getAuthenticationSessions().isEmpty()) {
                session.authenticationSessions().removeRootAuthenticationSession(realm, this);
            } else {
                long timestamp = Time.currentTimeMillis();
                entity.setTimestamp(timestamp);
                int authSessionLifespanSeconds = getAuthSessionLifespan(realm);
                entity.setExpiration(timestamp + TimeAdapter.fromSecondsToMilliseconds(authSessionLifespanSeconds));
            }
        }
    }

    @Override
    public void restartSession(RealmModel realm) {
        entity.setAuthenticationSessions(null);
        long timestamp = Time.currentTimeMillis();
        entity.setTimestamp(timestamp);
        int authSessionLifespanSeconds = getAuthSessionLifespan(realm);
        entity.setExpiration(timestamp + TimeAdapter.fromSecondsToMilliseconds(authSessionLifespanSeconds));
    }

    private String generateTabId() {
        return Base64Url.encode(SecretGenerator.getInstance().randomBytes(8));
    }

    private MapAuthenticationSessionAdapter toAdapter(MapAuthenticationSessionEntity entity) {
        return new MapAuthenticationSessionAdapter(session, this, entity.getTabId(), entity);
    }

    private MapAuthenticationSessionAdapter setAuthContext(MapAuthenticationSessionAdapter adapter) {
        session.getContext().setAuthenticationSession(adapter);
        return adapter;
    }
}
