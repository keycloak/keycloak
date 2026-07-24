/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.utils;

import java.util.Collection;
import java.util.Map;

import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;

/**
 * @author Alexander Schwartz
 */
public class UserSessionModelDelegate implements UserSessionModel {
    private UserSessionModel delegate;

    public UserSessionModelDelegate(UserSessionModel delegate) {
        this.delegate = delegate;
    }

    public String getId() {
        return delegate.getId();
    }

    public RealmModel getRealm() {
        return delegate.getRealm();
    }

    @Override
    public String getBrokerSessionId() {
        return delegate.getBrokerSessionId();
    }

    @Override
    public String getBrokerUserId() {
        return delegate.getBrokerUserId();
    }

    public UserModel getUser() {
        return delegate.getUser();
    }

    @Override
    public String getLoginUsername() {
        return delegate.getLoginUsername();
    }

    @Override
    public String getIpAddress() {
        return delegate.getIpAddress();
    }

    @Override
    public String getAuthMethod() {
        return delegate.getAuthMethod();
    }

    @Override
    public boolean isRememberMe() {
        return delegate.isRememberMe();
    }

    @Override
    public int getStarted() {
        return delegate.getStarted();
    }

    @Override
    public int getLastSessionRefresh() {
        return delegate.getLastSessionRefresh();
    }

    @Override
    public void setLastSessionRefresh(int seconds) {
        delegate.setLastSessionRefresh(seconds);
    }

    @Override
    public boolean isOffline() {
        return delegate.isOffline();
    }

    @Override
    public Map<String, AuthenticatedClientSessionModel> getAuthenticatedClientSessions() {
        return delegate.getAuthenticatedClientSessions();
    }

    @Override
    public AuthenticatedClientSessionModel getAuthenticatedClientSessionByClient(String clientUUID) {
        return delegate.getAuthenticatedClientSessionByClient(clientUUID);
    }

    @Override
    public void removeAuthenticatedClientSessions(Collection<String> removedClientUUIDS) {
        delegate.removeAuthenticatedClientSessions(removedClientUUIDS);
    }

    @Override
    public String getNote(String name) {
        return delegate.getNote(name);
    }

    @Override
    public void setNote(String name, String value) {
        delegate.setNote(name, value);
    }

    @Override
    public void removeNote(String name) {
        delegate.removeNote(name);
    }

    @Override
    public Map<String, String> getNotes() {
        return delegate.getNotes();
    }

    @Override
    public UserSessionModel.State getState() {
        return delegate.getState();
    }

    @Override
    public void setState(UserSessionModel.State state) {
        delegate.setState(state);
    }

    @Override
    public void restartSession(RealmModel realm, UserModel user, String loginUsername, String ipAddress, String authMethod, boolean rememberMe, String brokerSessionId, String brokerUserId) {
        delegate.restartSession(realm, user, loginUsername, ipAddress, authMethod, rememberMe, brokerSessionId, brokerUserId);
    }

    @Override
    public UserSessionModel.SessionPersistenceState getPersistenceState() {
        return delegate.getPersistenceState();
    }

}
