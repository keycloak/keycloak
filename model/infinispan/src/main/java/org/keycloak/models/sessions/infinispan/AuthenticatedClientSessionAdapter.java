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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.sessions.infinispan.changes.InfinispanChangelogBasedTransaction;
import org.keycloak.models.sessions.infinispan.changes.SessionEntityWrapper;
import org.keycloak.models.sessions.infinispan.changes.UserSessionClientSessionUpdateTask;
import org.keycloak.models.sessions.infinispan.changes.UserSessionUpdateTask;
import org.keycloak.models.sessions.infinispan.entities.AuthenticatedClientSessionEntity;
import org.keycloak.models.sessions.infinispan.entities.UserSessionEntity;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class AuthenticatedClientSessionAdapter implements AuthenticatedClientSessionModel {

    private AuthenticatedClientSessionEntity entity;
    private final ClientModel client;
    private final InfinispanUserSessionProvider provider;
    private final InfinispanChangelogBasedTransaction updateTx;
    private UserSessionAdapter userSession;

    public AuthenticatedClientSessionAdapter(AuthenticatedClientSessionEntity entity, ClientModel client, UserSessionAdapter userSession,
                                             InfinispanUserSessionProvider provider, InfinispanChangelogBasedTransaction updateTx) {
        this.provider = provider;
        this.entity = entity;
        this.client = client;
        this.updateTx = updateTx;
        this.userSession = userSession;
    }

    private void update(UserSessionUpdateTask task) {
        updateTx.addTask(userSession.getId(), task);
    }


    @Override
    public void setUserSession(UserSessionModel userSession) {
        String clientUUID = client.getId();

        // Dettach userSession
        if (userSession == null) {
            UserSessionUpdateTask task = new UserSessionUpdateTask() {

                @Override
                public void runUpdate(UserSessionEntity sessionEntity) {
                    sessionEntity.getAuthenticatedClientSessions().remove(clientUUID);
                }

            };
            update(task);
            this.userSession = null;
        } else {
            this.userSession = (UserSessionAdapter) userSession;
            UserSessionUpdateTask task = new UserSessionUpdateTask() {

                @Override
                public void runUpdate(UserSessionEntity sessionEntity) {
                    AuthenticatedClientSessionEntity current = sessionEntity.getAuthenticatedClientSessions().putIfAbsent(clientUUID, entity);
                    if (current != null) {
                        // It may happen when 2 concurrent HTTP requests trying SSO login against same client
                        entity = current;
                    }
                }

            };
            update(task);
        }
    }

    @Override
    public UserSessionModel getUserSession() {
        return this.userSession;
    }

    @Override
    public String getRedirectUri() {
        return entity.getRedirectUri();
    }

    @Override
    public void setRedirectUri(String uri) {
        UserSessionClientSessionUpdateTask task = new UserSessionClientSessionUpdateTask(client.getId()) {

            @Override
            protected void runClientSessionUpdate(AuthenticatedClientSessionEntity entity) {
                entity.setRedirectUri(uri);
            }

        };

        update(task);
    }

    @Override
    public String getId() {
        return null;
    }

    @Override
    public RealmModel getRealm() {
        return userSession.getRealm();
    }

    @Override
    public ClientModel getClient() {
        return client;
    }

    @Override
    public int getTimestamp() {
        return entity.getTimestamp();
    }

    @Override
    public void setTimestamp(int timestamp) {
        UserSessionClientSessionUpdateTask task = new UserSessionClientSessionUpdateTask(client.getId()) {

            @Override
            protected void runClientSessionUpdate(AuthenticatedClientSessionEntity entity) {
                entity.setTimestamp(timestamp);
            }

            @Override
            public CrossDCMessageStatus getCrossDCMessageStatus(SessionEntityWrapper<UserSessionEntity> sessionWrapper) {
                // We usually update lastSessionRefresh at the same time. That would handle it.
                return CrossDCMessageStatus.NOT_NEEDED;
            }

        };

        update(task);
    }

    @Override
    public int getCurrentRefreshTokenUseCount() {
        return entity.getCurrentRefreshTokenUseCount();
    }

    @Override
    public void setCurrentRefreshTokenUseCount(int currentRefreshTokenUseCount) {
        UserSessionClientSessionUpdateTask task = new UserSessionClientSessionUpdateTask(client.getId()) {

            @Override
            protected void runClientSessionUpdate(AuthenticatedClientSessionEntity entity) {
                entity.setCurrentRefreshTokenUseCount(currentRefreshTokenUseCount);
            }

            @Override
            public CrossDCMessageStatus getCrossDCMessageStatus(SessionEntityWrapper<UserSessionEntity> sessionWrapper) {
                // We usually update lastSessionRefresh at the same time. That would handle it.
                return CrossDCMessageStatus.NOT_NEEDED;
            }

        };

        update(task);
    }

    @Override
    public String getCurrentRefreshToken() {
        return entity.getCurrentRefreshToken();
    }

    @Override
    public void setCurrentRefreshToken(String currentRefreshToken) {
        UserSessionClientSessionUpdateTask task = new UserSessionClientSessionUpdateTask(client.getId()) {

            @Override
            protected void runClientSessionUpdate(AuthenticatedClientSessionEntity entity) {
                entity.setCurrentRefreshToken(currentRefreshToken);
            }

            @Override
            public CrossDCMessageStatus getCrossDCMessageStatus(SessionEntityWrapper<UserSessionEntity> sessionWrapper) {
                // We usually update lastSessionRefresh at the same time. That would handle it.
                return CrossDCMessageStatus.NOT_NEEDED;
            }

        };

        update(task);
    }

    @Override
    public String getAction() {
        return entity.getAction();
    }

    @Override
    public void setAction(String action) {
        UserSessionClientSessionUpdateTask task = new UserSessionClientSessionUpdateTask(client.getId()) {

            @Override
            protected void runClientSessionUpdate(AuthenticatedClientSessionEntity entity) {
                entity.setAction(action);
            }

        };

        update(task);
    }

    @Override
    public String getProtocol() {
        return entity.getAuthMethod();
    }

    @Override
    public void setProtocol(String method) {
        UserSessionClientSessionUpdateTask task = new UserSessionClientSessionUpdateTask(client.getId()) {

            @Override
            protected void runClientSessionUpdate(AuthenticatedClientSessionEntity entity) {
                entity.setAuthMethod(method);
            }

        };

        update(task);
    }

    @Override
    public Set<String> getRoles() {
        return entity.getRoles();
    }

    @Override
    public void setRoles(Set<String> roles) {
        UserSessionClientSessionUpdateTask task = new UserSessionClientSessionUpdateTask(client.getId()) {

            @Override
            protected void runClientSessionUpdate(AuthenticatedClientSessionEntity entity) {
                entity.setRoles(roles); // TODO not thread-safe. But we will remove setRoles anyway...?
            }

        };

        update(task);
    }

    @Override
    public Set<String> getProtocolMappers() {
        return entity.getProtocolMappers();
    }

    @Override
    public void setProtocolMappers(Set<String> protocolMappers) {
        UserSessionClientSessionUpdateTask task = new UserSessionClientSessionUpdateTask(client.getId()) {

            @Override
            protected void runClientSessionUpdate(AuthenticatedClientSessionEntity entity) {
                entity.setProtocolMappers(protocolMappers); // TODO not thread-safe. But we will remove setProtocolMappers anyway...?
            }

        };

        update(task);
    }

    @Override
    public String getNote(String name) {
        return entity.getNotes().get(name);
    }

    @Override
    public void setNote(String name, String value) {
        UserSessionClientSessionUpdateTask task = new UserSessionClientSessionUpdateTask(client.getId()) {

            @Override
            protected void runClientSessionUpdate(AuthenticatedClientSessionEntity entity) {
                entity.getNotes().put(name, value);
            }

        };

        update(task);
    }

    @Override
    public void removeNote(String name) {
        UserSessionClientSessionUpdateTask task = new UserSessionClientSessionUpdateTask(client.getId()) {

            @Override
            protected void runClientSessionUpdate(AuthenticatedClientSessionEntity entity) {
                entity.getNotes().remove(name);
            }

        };

        update(task);
    }

    @Override
    public Map<String, String> getNotes() {
        if (entity.getNotes().isEmpty()) return Collections.emptyMap();
        Map<String, String> copy = new HashMap<>();
        copy.putAll(entity.getNotes());
        return copy;
    }

}
