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

package org.keycloak.models.sessions.infinispan;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;

import static org.keycloak.models.sessions.infinispan.ImmutableSession.readOnly;

/**
 * An immutable {@link UserSessionModel} implementation.
 * <p>
 * All setters throw a {@link UnsupportedOperationException}.
 */
record ImmutableUserSessionModel(
        String id,
        RealmModel realm,
        UserModel user,
        String brokerSessionId,
        String brokerUserId,
        String loginUserName,
        String ipAddress,
        String authMethod,
        Map<String, AuthenticatedClientSessionModel> clientSessions,
        Map<String, String> notes,
        State state,
        int started,
        int lastSessionRefresh,
        boolean rememberMe,
        boolean offline
) implements UserSessionModel {

    @Override
    public String getId() {
        return id;
    }

    @Override
    public RealmModel getRealm() {
        return realm;
    }

    @Override
    public String getBrokerSessionId() {
        return brokerSessionId;
    }

    @Override
    public String getBrokerUserId() {
        return brokerUserId;
    }

    @Override
    public UserModel getUser() {
        return user;
    }

    @Override
    public String getLoginUsername() {
        return loginUserName;
    }

    @Override
    public String getIpAddress() {
        return ipAddress;
    }

    @Override
    public String getAuthMethod() {
        return authMethod;
    }

    @Override
    public boolean isRememberMe() {
        return rememberMe;
    }

    @Override
    public int getStarted() {
        return started;
    }

    @Override
    public int getLastSessionRefresh() {
        return lastSessionRefresh;
    }

    @Override
    public void setLastSessionRefresh(int seconds) {
        readOnly();
    }

    @Override
    public boolean isOffline() {
        return offline;
    }

    @Override
    public Map<String, AuthenticatedClientSessionModel> getAuthenticatedClientSessions() {
        return Collections.unmodifiableMap(clientSessions);
    }

    @Override
    public void removeAuthenticatedClientSessions(Collection<String> removedClientUUIDS) {
        readOnly();
    }

    @Override
    public String getNote(String name) {
        return notes.get(name);
    }

    @Override
    public void setNote(String name, String value) {
        readOnly();
    }

    @Override
    public void removeNote(String name) {
        readOnly();
    }

    @Override
    public Map<String, String> getNotes() {
        return Collections.unmodifiableMap(notes);
    }

    @Override
    public State getState() {
        return state;
    }

    @Override
    public void setState(State state) {
        readOnly();
    }

    @Override
    public void restartSession(RealmModel realm, UserModel user, String loginUsername, String ipAddress, String authMethod, boolean rememberMe, String brokerSessionId, String brokerUserId) {
        readOnly();
    }
}
