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

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author <a href="mailto:mkanis@redhat.com">Martin Kanis</a>
 */
public abstract class MapUserSessionAdapter extends AbstractUserSessionModel {

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
        Long started = entity.getStarted();
        return started != null ? TimeAdapter.fromLongWithTimeInSecondsToIntegerWithTimeInSeconds(started) : 0;
    }

    @Override
    public int getLastSessionRefresh() {
        Long lastSessionRefresh = entity.getLastSessionRefresh();
        return lastSessionRefresh != null ? TimeAdapter.fromLongWithTimeInSecondsToIntegerWithTimeInSeconds(lastSessionRefresh) : 0;
    }

    @Override
    public void setLastSessionRefresh(int seconds) {
        entity.setLastSessionRefresh(TimeAdapter.fromIntegerWithTimeInSecondsToLongWithTimeAsInSeconds(seconds));
    }

    @Override
    public boolean isOffline() {
        Boolean offline = entity.isOffline();
        return offline != null ? offline : false;
    }

    @Override
    public Map<String, AuthenticatedClientSessionModel> getAuthenticatedClientSessions() {
        Map<String, AuthenticatedClientSessionModel> result = new HashMap<>();
        List<String> removedClientUUIDS = new LinkedList<>();

        Map<String, String> authenticatedClientSessions = entity.getAuthenticatedClientSessions();
        if (authenticatedClientSessions == null) {
            return Collections.emptyMap();
        } else {
            // to avoid concurrentModificationException
            authenticatedClientSessions = new HashMap<>(authenticatedClientSessions);
        }

        authenticatedClientSessions.forEach((clientUUID, clientSessionId) -> {
            ClientModel client = realm.getClientById(clientUUID);

            if (client != null) {
                AuthenticatedClientSessionModel clientSession = session.sessions()
                        .getClientSession(this, client, clientSessionId, isOffline());
                if (clientSession != null) {
                    result.put(clientUUID, clientSession);
                }
            } else {
                removedClientUUIDS.add(clientUUID);
            }
        });

        removeAuthenticatedClientSessions(removedClientUUIDS);

        return Collections.unmodifiableMap(result);
    }

    @Override
    public AuthenticatedClientSessionModel getAuthenticatedClientSessionByClient(String clientUUID) {
        String clientSessionId = entity.getAuthenticatedClientSession(clientUUID);

        if (clientSessionId == null) {
            return null;
        }

        ClientModel client = realm.getClientById(clientUUID);

        if (client != null) {
            return session.sessions().getClientSession(this, client, clientSessionId, isOffline());
        }

        removeAuthenticatedClientSessions(Collections.singleton(clientUUID));

        return null;
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

        int currentTime = Time.currentTime();
        entity.setStarted(TimeAdapter.fromIntegerWithTimeInSecondsToLongWithTimeAsInSeconds(currentTime));
        entity.setLastSessionRefresh(TimeAdapter.fromIntegerWithTimeInSecondsToLongWithTimeAsInSeconds(currentTime));

        entity.setState(null);

        String correspondingSessionId = entity.getNote(CORRESPONDING_SESSION_ID);
        entity.setNotes(new ConcurrentHashMap<>());
        if (correspondingSessionId != null)
            entity.setNote(CORRESPONDING_SESSION_ID, correspondingSessionId);

        entity.setAuthenticatedClientSessions(null);
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
