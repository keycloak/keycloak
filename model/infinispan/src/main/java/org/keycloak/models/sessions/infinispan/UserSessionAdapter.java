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

import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.sessions.infinispan.changes.InfinispanChangelogBasedTransaction;
import org.keycloak.models.sessions.infinispan.changes.sessions.LastSessionRefreshChecker;
import org.keycloak.models.sessions.infinispan.changes.SessionEntityWrapper;
import org.keycloak.models.sessions.infinispan.changes.UserSessionUpdateTask;
import org.keycloak.models.sessions.infinispan.entities.AuthenticatedClientSessionEntity;
import org.keycloak.models.sessions.infinispan.entities.UserSessionEntity;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class UserSessionAdapter implements UserSessionModel {

    private final KeycloakSession session;

    private final InfinispanUserSessionProvider provider;

    private final InfinispanChangelogBasedTransaction updateTx;

    private final RealmModel realm;

    private final UserSessionEntity entity;

    private final boolean offline;

    public UserSessionAdapter(KeycloakSession session, InfinispanUserSessionProvider provider, InfinispanChangelogBasedTransaction updateTx, RealmModel realm,
                              UserSessionEntity entity, boolean offline) {
        this.session = session;
        this.provider = provider;
        this.updateTx = updateTx;
        this.realm = realm;
        this.entity = entity;
        this.offline = offline;
    }

    @Override
    public Map<String, AuthenticatedClientSessionModel> getAuthenticatedClientSessions() {
        Map<String, AuthenticatedClientSessionEntity> clientSessionEntities = entity.getAuthenticatedClientSessions();
        Map<String, AuthenticatedClientSessionModel> result = new HashMap<>();

        List<String> removedClientUUIDS = new LinkedList<>();

        if (clientSessionEntities != null) {
            clientSessionEntities.forEach((String key, AuthenticatedClientSessionEntity value) -> {
                // Check if client still exists
                ClientModel client = realm.getClientById(key);
                if (client != null) {
                    result.put(key, new AuthenticatedClientSessionAdapter(value, client, this, provider, updateTx));
                } else {
                    removedClientUUIDS.add(key);
                }
            });
        }

        removeAuthenticatedClientSessions(removedClientUUIDS);

        return Collections.unmodifiableMap(result);
    }

    @Override
    public void removeAuthenticatedClientSessions(Iterable<String> removedClientUUIDS) {
        if (removedClientUUIDS == null || ! removedClientUUIDS.iterator().hasNext()) {
            return;
        }

        // Update user session
        UserSessionUpdateTask task = new UserSessionUpdateTask() {
            @Override
            public void runUpdate(UserSessionEntity entity) {
                removedClientUUIDS.forEach(entity.getAuthenticatedClientSessions()::remove);
            }
        };

        update(task);
    }

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
    public UserModel getUser() {
        return session.users().getUserById(entity.getUser(), realm);
    }

    @Override
    public String getLoginUsername() {
        return entity.getLoginUsername();
    }

    public String getIpAddress() {
        return entity.getIpAddress();
    }

    @Override
    public String getAuthMethod() {
        return entity.getAuthMethod();
    }

    @Override
    public boolean isRememberMe() {
        return entity.isRememberMe();
    }

    public int getStarted() {
        return entity.getStarted();
    }

    public int getLastSessionRefresh() {
        return entity.getLastSessionRefresh();
    }

    public void setLastSessionRefresh(int lastSessionRefresh) {
        UserSessionUpdateTask task = new UserSessionUpdateTask() {

            @Override
            public void runUpdate(UserSessionEntity entity) {
                entity.setLastSessionRefresh(lastSessionRefresh);
            }

            @Override
            public CrossDCMessageStatus getCrossDCMessageStatus(SessionEntityWrapper<UserSessionEntity> sessionWrapper) {
                return new LastSessionRefreshChecker(provider.getLastSessionRefreshStore(), provider.getOfflineLastSessionRefreshStore())
                        .getCrossDCMessageStatus(UserSessionAdapter.this.session, UserSessionAdapter.this.realm, sessionWrapper, offline, lastSessionRefresh);
            }

            @Override
            public String toString() {
                return "setLastSessionRefresh(" + lastSessionRefresh + ')';
            }
        };

        update(task);
    }

    @Override
    public String getNote(String name) {
        return entity.getNotes() != null ? entity.getNotes().get(name) : null;
    }

    @Override
    public void setNote(String name, String value) {
        UserSessionUpdateTask task = new UserSessionUpdateTask() {

            @Override
            public void runUpdate(UserSessionEntity entity) {
                if (value == null) {
                    if (entity.getNotes().containsKey(name)) {
                        removeNote(name);
                    }
                    return;
                }
                entity.getNotes().put(name, value);
            }

        };

        update(task);
    }

    @Override
    public void removeNote(String name) {
        UserSessionUpdateTask task = new UserSessionUpdateTask() {

            @Override
            public void runUpdate(UserSessionEntity entity) {
                entity.getNotes().remove(name);
            }

        };

        update(task);
    }

    @Override
    public Map<String, String> getNotes() {
        return entity.getNotes();
    }

    @Override
    public State getState() {
        return entity.getState();
    }

    @Override
    public void setState(State state) {
        UserSessionUpdateTask task = new UserSessionUpdateTask() {

            @Override
            public void runUpdate(UserSessionEntity entity) {
                entity.setState(state);
            }

        };

        update(task);
    }

    @Override
    public void restartSession(RealmModel realm, UserModel user, String loginUsername, String ipAddress, String authMethod, boolean rememberMe, String brokerSessionId, String brokerUserId) {
        UserSessionUpdateTask task = new UserSessionUpdateTask() {

            @Override
            public void runUpdate(UserSessionEntity entity) {
                provider.updateSessionEntity(entity, realm, user, loginUsername, ipAddress, authMethod, rememberMe, brokerSessionId, brokerUserId);

                entity.setState(null);
                entity.getNotes().clear();
                entity.getAuthenticatedClientSessions().clear();
            }

        };

        update(task);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof UserSessionModel)) return false;

        UserSessionModel that = (UserSessionModel) o;
        return that.getId().equals(getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    UserSessionEntity getEntity() {
        return entity;
    }

    void update(UserSessionUpdateTask task) {
        updateTx.addTask(getId(), task);
    }

}
