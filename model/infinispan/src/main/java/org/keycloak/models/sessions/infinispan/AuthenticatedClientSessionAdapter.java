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

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.keycloak.common.util.Time;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.session.UserSessionPersisterProvider;
import org.keycloak.models.sessions.infinispan.changes.ClientSessionUpdateTask;
import org.keycloak.models.sessions.infinispan.changes.Tasks;
import org.keycloak.models.sessions.infinispan.entities.AuthenticatedClientSessionEntity;
import org.keycloak.models.sessions.infinispan.entities.EmbeddedClientSessionKey;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class AuthenticatedClientSessionAdapter implements AuthenticatedClientSessionModel {

    private final KeycloakSession kcSession;
    private final AuthenticatedClientSessionEntity entity;
    private final ClientModel client;
    private final ClientSessionManager clientSessionManager;
    private UserSessionModel userSession;
    private final boolean offline;
    private final EmbeddedClientSessionKey cacheKey;

    public AuthenticatedClientSessionAdapter(KeycloakSession kcSession,
                                             AuthenticatedClientSessionEntity entity, ClientModel client, UserSessionModel userSession,
                                             ClientSessionManager clientSessionManager,
                                             EmbeddedClientSessionKey cacheKey,
                                             boolean offline) {

        this.userSession = Objects.requireNonNull(userSession, "userSession must not be null");
        this.kcSession = kcSession;
        this.entity = entity;
        this.client = client;
        this.clientSessionManager = clientSessionManager;
        this.offline = offline;
        this.cacheKey = cacheKey;
    }

    private void update(ClientSessionUpdateTask task) {
        clientSessionManager.addChange(cacheKey, task);
    }

    /**
     * Detaches the client session from its user session.
     * <p>
     * <b>This method does not delete the client session from user session records, it only removes the client session.</b>
     * The list of client sessions within user session is updated lazily for performance reasons.
     */
    @Override
    public void detachFromUserSession() {
        if (this.userSession.isOffline()) {
            kcSession.getProvider(UserSessionPersisterProvider.class).removeClientSession(userSession.getId(), client.getId(), true);
        }
        // Intentionally do not remove the clientUUID from the user session, invalid session is handled
        // as nonexistent in org.keycloak.models.sessions.infinispan.UserSessionAdapter.getAuthenticatedClientSessions()
        this.userSession = null;

        clientSessionManager.addChange(cacheKey, Tasks.removeSync(offline));
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
        ClientSessionUpdateTask task = new ClientSessionUpdateTask() {

            @Override
            public void runUpdate(AuthenticatedClientSessionEntity entity) {
                entity.setRedirectUri(uri);
            }

            @Override
            public boolean isOffline() {
                return offline;
            }
        };

        update(task);
    }

    @Override
    public String getId() {
        return cacheKey.toId();
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
        if (timestamp <= getTimestamp()) {
            return;
        }

        ClientSessionUpdateTask task = new ClientSessionUpdateTask() {

            @Override
            public void runUpdate(AuthenticatedClientSessionEntity entity) {
                if (entity.getTimestamp() >= timestamp) {
                    return;
                }
                entity.setTimestamp(timestamp);
            }

            @Override
            public boolean isOffline() {
                return offline;
            }

            @Override
            public String toString() {
                return "setTimestamp(" + timestamp + ')';
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
        ClientSessionUpdateTask task = new ClientSessionUpdateTask() {

            @Override
            public void runUpdate(AuthenticatedClientSessionEntity entity) {
                entity.setAction(action);
            }

            @Override
            public boolean isOffline() {
                return offline;
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
        ClientSessionUpdateTask task = new ClientSessionUpdateTask() {

            @Override
            public void runUpdate(AuthenticatedClientSessionEntity entity) {
                entity.setAuthMethod(method);
            }

            @Override
            public boolean isOffline() {
                return offline;
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
        ClientSessionUpdateTask task = new ClientSessionUpdateTask() {

            @Override
            public void runUpdate(AuthenticatedClientSessionEntity entity) {
                entity.getNotes().put(name, value);
            }

            @Override
            public boolean isOffline() {
                return offline;
            }
        };

        update(task);
    }

    @Override
    public void removeNote(String name) {
        ClientSessionUpdateTask task = new ClientSessionUpdateTask() {

            @Override
            public void runUpdate(AuthenticatedClientSessionEntity entity) {
                entity.getNotes().remove(name);
            }

            @Override
            public boolean isOffline() {
                return offline;
            }

        };

        update(task);
    }

    @Override
    public Map<String, String> getNotes() {
        return new ConcurrentHashMap<>(entity.getNotes());
    }

    @Override
    public void restartClientSession() {
        ClientSessionUpdateTask task = new ClientSessionUpdateTask() {

            @Override
            public void runUpdate(AuthenticatedClientSessionEntity entity) {
                UserSessionModel userSession = getUserSession();
                entity.setAction(null);
                entity.setRedirectUri(null);
                entity.setTimestamp(Time.currentTime());
                entity.getNotes().clear();
                entity.getNotes().put(AuthenticatedClientSessionModel.STARTED_AT_NOTE, String.valueOf(entity.getTimestamp()));
                entity.getNotes().put(AuthenticatedClientSessionModel.USER_SESSION_STARTED_AT_NOTE, String.valueOf(userSession.getStarted()));
                entity.getNotes().put(AuthenticatedClientSessionEntity.CLIENT_ID_NOTE, getClient().getId());
                if (userSession.isRememberMe()) {
                    entity.getNotes().put(AuthenticatedClientSessionModel.USER_SESSION_REMEMBER_ME_NOTE, "true");
                }
            }

            @Override
            public boolean isOffline() {
                return offline;
            }
        };

        clientSessionManager.restartEntity(cacheKey, task);
    }

}
