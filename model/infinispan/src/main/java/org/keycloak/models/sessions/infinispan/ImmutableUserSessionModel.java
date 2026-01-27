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
import java.util.Objects;

import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;

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

    ImmutableUserSessionModel {
        Objects.requireNonNull(id, "id cannot be null");
    }

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
        throw new UnsupportedOperationException();
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
        throw new UnsupportedOperationException();
    }

    @Override
    public String getNote(String name) {
        return notes.get(name);
    }

    @Override
    public void setNote(String name, String value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeNote(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, String> getNotes() {
        return notes;
    }

    @Override
    public State getState() {
        return state;
    }

    @Override
    public void setState(State state) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void restartSession(RealmModel realm, UserModel user, String loginUsername, String ipAddress, String authMethod, boolean rememberMe, String brokerSessionId, String brokerUserId) {
        throw new UnsupportedOperationException();
    }
}
