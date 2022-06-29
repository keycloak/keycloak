/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.models.map.userSession;

import org.keycloak.common.util.Time;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import org.keycloak.models.UserSessionModel;
import org.keycloak.models.map.common.TimeAdapter;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static org.keycloak.models.map.common.ExpirationUtils.isExpired;
import static org.keycloak.models.map.userSession.SessionExpiration.setUserSessionExpiration;

/**
 * @author <a href="mailto:mkanis@redhat.com">Martin Kanis</a>
 */
public class MapUserSessionAdapter extends AbstractUserSessionModel {

    public MapUserSessionAdapter(KeycloakSession session, RealmModel realm, MapUserSessionEntity entity) {
        super(session, realm, entity);
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
    public String getBrokerSessionId() {
        return entity.getBrokerSessionId();
    }

    @Override
    public String getBrokerUserId() {
        return entity.getBrokerUserId();
    }

    @Override
    public UserModel getUser() {
        return session.users().getUserById(getRealm(), entity.getUserId());
    }

    @Override
    public String getLoginUsername() {
        return entity.getLoginUsername();
    }

    @Override
    public String getIpAddress() {
        return entity.getIpAddress();
    }

    @Override
    public String getAuthMethod() {
        return entity.getAuthMethod();
    }

    @Override
    public boolean isRememberMe() {
        Boolean rememberMe = entity.isRememberMe();
        return rememberMe != null ? rememberMe : false;
    }

    @Override
    public int getStarted() {
        Long started = entity.getTimestamp();
        return started != null ? TimeAdapter.fromLongWithTimeInSecondsToIntegerWithTimeInSeconds(TimeAdapter.fromMilliSecondsToSeconds(started)) : 0;
    }

    @Override
    public int getLastSessionRefresh() {
        Long lastSessionRefresh = entity.getLastSessionRefresh();
        return lastSessionRefresh != null ? TimeAdapter.fromLongWithTimeInSecondsToIntegerWithTimeInSeconds(TimeAdapter.fromMilliSecondsToSeconds(lastSessionRefresh)) : 0;
    }

    @Override
    public void setLastSessionRefresh(int seconds) {
        entity.setLastSessionRefresh(TimeAdapter.fromSecondsToMilliseconds(seconds));

        // whenever the lastSessionRefresh is changed recompute the expiration time
        setUserSessionExpiration(entity, realm);
    }

    @Override
    public boolean isOffline() {
        Boolean offline = entity.isOffline();
        return offline != null ? offline : false;
    }

    @Override
    public Map<String, AuthenticatedClientSessionModel> getAuthenticatedClientSessions() {
        Set<MapAuthenticatedClientSessionEntity> authenticatedClientSessions = entity.getAuthenticatedClientSessions();
        if (authenticatedClientSessions == null) {
            return Collections.emptyMap();
        }

        return authenticatedClientSessions
                    .stream()
                    .filter(this::filterAndRemoveExpiredClientSessions)
                    .filter(this::matchingOfflineFlag)
                    .filter(this::filterAndRemoveClientSessionWithoutClient)
                    .collect(Collectors.toMap(MapAuthenticatedClientSessionEntity::getClientId, this::clientEntityToModel));
    }

    private AuthenticatedClientSessionModel clientEntityToModel(MapAuthenticatedClientSessionEntity clientSessionEntity) {
        return new MapAuthenticatedClientSessionAdapter(session, realm, this, clientSessionEntity) {
            @Override
            public void detachFromUserSession() {
                MapUserSessionAdapter.this.entity.removeAuthenticatedClientSession(entity.getClientId());
                this.userSession = null;
            }
        };
    }

    public boolean filterAndRemoveExpiredClientSessions(MapAuthenticatedClientSessionEntity clientSession) {
        if (isExpired(clientSession, false)) {
            entity.removeAuthenticatedClientSession(clientSession.getClientId());
            return false;
        }

        return true;
    }

    public boolean filterAndRemoveClientSessionWithoutClient(MapAuthenticatedClientSessionEntity clientSession) {
        ClientModel client = realm.getClientById(clientSession.getClientId());

        if (client == null) {
            entity.removeAuthenticatedClientSession(clientSession.getId());

            // Filter out entities that doesn't have client
            return false;
        }

        // client session has client so we do not filter it out
        return true;
    }

    public boolean matchingOfflineFlag(MapAuthenticatedClientSessionEntity clientSession) {
        Boolean isClientSessionOffline = clientSession.isOffline();

        // If client session doesn't have offline flag default to false
        if (isClientSessionOffline == null) return !isOffline();

        return isOffline() == isClientSessionOffline;
    }

    @Override
    public AuthenticatedClientSessionModel getAuthenticatedClientSessionByClient(String clientUUID) {
        return entity.getAuthenticatedClientSession(clientUUID)
                .filter(this::filterAndRemoveExpiredClientSessions)
                .filter(this::matchingOfflineFlag)
                .filter(this::filterAndRemoveClientSessionWithoutClient)
                .map(this::clientEntityToModel)
                .orElse(null);
    }
    @Override
    public void removeAuthenticatedClientSessions(Collection<String> removedClientUKS) {
        removedClientUKS.forEach(entity::removeAuthenticatedClientSession);
    }

    @Override
    public String getNote(String name) {
        return (name != null) ? entity.getNote(name) : null;
    }

    @Override
    public void setNote(String name, String value) {
        if (name != null) {
            if (value == null) {
                entity.removeNote(name);
            } else {
                entity.setNote(name, value);
            }
        }
    }

    @Override
    public void removeNote(String name) {
        if (name != null) {
            entity.removeNote(name);
        }
    }

    @Override
    public Map<String, String> getNotes() {
        Map<String, String> notes = entity.getNotes();
        return notes == null ? Collections.emptyMap() : Collections.unmodifiableMap(notes);
    }

    @Override
    public State getState() {
        return entity.getState();
    }

    @Override
    public void setState(State state) {
        entity.setState(state);
    }

    @Override
    public void restartSession(RealmModel realm, UserModel user, String loginUsername, String ipAddress, String authMethod,
                               boolean rememberMe, String brokerSessionId, String brokerUserId) {
        entity.setRealmId(realm.getId());
        entity.setUserId(user.getId());
        entity.setLoginUsername(loginUsername);
        entity.setIpAddress(ipAddress);
        entity.setAuthMethod(authMethod);
        entity.setRememberMe(rememberMe);
        entity.setBrokerSessionId(brokerSessionId);
        entity.setBrokerUserId(brokerUserId);

        long currentTime = Time.currentTimeMillis();
        entity.setTimestamp(currentTime);
        entity.setLastSessionRefresh(currentTime);

        entity.setState(null);

        String correspondingSessionId = entity.getNote(CORRESPONDING_SESSION_ID);
        entity.setNotes(new ConcurrentHashMap<>());
        if (correspondingSessionId != null)
            entity.setNote(CORRESPONDING_SESSION_ID, correspondingSessionId);

        entity.clearAuthenticatedClientSessions();
    }

    @Override
    public String toString() {
        return String.format("%s@%08x", getId(), hashCode());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserSessionModel)) return false;

        UserSessionModel that = (UserSessionModel) o;
        return Objects.equals(that.getId(), getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }
}
