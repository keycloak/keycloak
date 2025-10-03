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

import org.jboss.logging.Logger;
import org.keycloak.common.util.MultiSiteUtils;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.UserSessionProvider;
import org.keycloak.models.sessions.infinispan.changes.SessionsChangelogBasedTransaction;
import org.keycloak.models.sessions.infinispan.changes.sessions.CrossDCLastSessionRefreshChecker;
import org.keycloak.models.sessions.infinispan.changes.SessionEntityWrapper;
import org.keycloak.models.sessions.infinispan.changes.Tasks;
import org.keycloak.models.sessions.infinispan.changes.UserSessionUpdateTask;
import org.keycloak.models.sessions.infinispan.changes.sessions.CrossDCLastSessionRefreshListener;
import org.keycloak.models.sessions.infinispan.entities.AuthenticatedClientSessionEntity;
import org.keycloak.models.sessions.infinispan.entities.AuthenticatedClientSessionStore;
import org.keycloak.models.sessions.infinispan.entities.UserSessionEntity;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class UserSessionAdapter<T extends SessionRefreshStore & UserSessionProvider> implements UserSessionModel {

    private static final Logger logger = Logger.getLogger(UserSessionAdapter.class);

    private final KeycloakSession session;

    private final T provider;

    private final SessionsChangelogBasedTransaction<String, UserSessionEntity> userSessionUpdateTx;

    private final SessionsChangelogBasedTransaction<UUID, AuthenticatedClientSessionEntity> clientSessionUpdateTx;

    private final RealmModel realm;

    private final UserModel user;

    private final UserSessionEntity entity;

    private final boolean offline;

    private SessionPersistenceState persistenceState;

    public UserSessionAdapter(KeycloakSession session, UserModel user, T provider,
                              SessionsChangelogBasedTransaction<String, UserSessionEntity> userSessionUpdateTx,
                              SessionsChangelogBasedTransaction<UUID, AuthenticatedClientSessionEntity> clientSessionUpdateTx,
                              RealmModel realm, UserSessionEntity entity, boolean offline) {
        this.session = session;
        this.user = user;
        this.provider = provider;
        this.userSessionUpdateTx = userSessionUpdateTx;
        this.clientSessionUpdateTx = clientSessionUpdateTx;
        this.realm = realm;
        this.entity = entity;
        this.offline = offline;
    }

    @Override
    public Map<String, AuthenticatedClientSessionModel> getAuthenticatedClientSessions() {
        AuthenticatedClientSessionStore clientSessionEntities = entity.getAuthenticatedClientSessions();
        Map<String, AuthenticatedClientSessionModel> result = new HashMap<>();

        List<String> removedClientUUIDS = new LinkedList<>();

        if (clientSessionEntities != null) {
            clientSessionEntities.forEach((String key, UUID value) -> {
                // Check if client still exists
                ClientModel client = realm.getClientById(key);
                if (client != null) {
                    final AuthenticatedClientSessionModel clientSession = provider.getClientSession(this, client, value.toString(), offline);
                    if (clientSession != null) {
                        result.put(key, clientSession);
                    } else {
                        // Either the client session has expired, or it hasn't been added by a concurrently running login yet.
                        // So it is unsafe to clear it, so we need to keep it for now. Otherwise, the test ConcurrentLoginTest.concurrentLoginSingleUser will fail.
                        // removedClientUUIDS.add(key);
                    }
                } else {
                    // client does no longer exist
                    removedClientUUIDS.add(key);
                }
            });
        }

        removeAuthenticatedClientSessions(removedClientUUIDS);

        return Collections.unmodifiableMap(result);
    }

    @Override
    public AuthenticatedClientSessionModel getAuthenticatedClientSessionByClient(String clientUUID) {
        AuthenticatedClientSessionStore clientSessionEntities = entity.getAuthenticatedClientSessions();
        final UUID clientSessionId = clientSessionEntities.get(clientUUID);

        if (clientSessionId == null) {
            logger.debugf("Client to client session mapping not found. userSessionId=%s, clientId=%s, offline=%s, mappings=%s",
                    getId(), clientUUID, offline, clientSessionEntities);
            return null;
        }

        ClientModel client = realm.getClientById(clientUUID);

        if (client != null) {
            // Might return null either the client session has expired, or it hasn't been added by a concurrently running login yet.
            // So it is unsafe to clear it, so we need to keep it for now. Otherwise, the test ConcurrentLoginTest.concurrentLoginSingleUser will fail.
            return provider.getClientSession(this, client, clientSessionId.toString(), offline);
        }

        logger.debugf("Client not found. Removing from mappings. userSessionId=%s, clientId=%s, clientSessionId=%s, offline=%s",
                getId(), clientUUID, clientSessionId, offline);
        removeAuthenticatedClientSessions(Collections.singleton(clientUUID));
        return null;
    }

    @Override
    public void removeAuthenticatedClientSessions(Collection<String> removedClientUUIDS) {
        if (removedClientUUIDS == null || removedClientUUIDS.isEmpty()) {
            return;
        }
        logger.debugf("Removing client sessions. clients=%s, offline=%s", removedClientUUIDS, offline);

        // do not iterate the removedClientUUIDS and remove the clientSession directly as the addTask can manipulate
        // the collection being iterated, and that can lead to unpredictable behaviour (e.g. NPE)
        List<UUID> clientSessionUuids = removedClientUUIDS.stream()
                .map(entity.getAuthenticatedClientSessions()::get)
                .filter(Objects::nonNull)
                .toList();

        // Update user session
        UserSessionUpdateTask task = new UserSessionUpdateTask() {
            @Override
            public void runUpdate(UserSessionEntity entity) {
                removedClientUUIDS.forEach(entity.getAuthenticatedClientSessions()::remove);
            }

            @Override
            public boolean isOffline() {
                return offline;
            }
        };
        update(task);

        clientSessionUuids.forEach(clientSessionId -> this.clientSessionUpdateTx.addTask(clientSessionId, Tasks.removeSync(offline)));
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
        return this.user;
    }

    @Override
    public String getLoginUsername() {
        if (entity.getLoginUsername() == null) {
            // this is a hack so that UserModel doesn't have to be available when offline token is imported.
            // see related JIRA - KEYCLOAK-5350 and corresponding test
            return getUser().getUsername();
        } else {
            return entity.getLoginUsername();
        }
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
        return entity.isRememberMe();
    }

    @Override
    public int getStarted() {
        return entity.getStarted();
    }

    @Override
    public int getLastSessionRefresh() {
        return entity.getLastSessionRefresh();
    }

    @Override
    public void setLastSessionRefresh(int lastSessionRefresh) {
        if (lastSessionRefresh <= entity.getLastSessionRefresh()) {
            return;
        }

        if (!MultiSiteUtils.isPersistentSessionsEnabled() && offline) {
            // Received the message from the other DC that we should update the lastSessionRefresh in local cluster. Don't update DB in that case.
            // The other DC already did.
            Boolean ignoreRemoteCacheUpdate = (Boolean) session.getAttribute(CrossDCLastSessionRefreshListener.IGNORE_REMOTE_CACHE_UPDATE);
            if (ignoreRemoteCacheUpdate == null || !ignoreRemoteCacheUpdate) {
                provider.getPersisterLastSessionRefreshStore().putLastSessionRefresh(session, entity.getId(), realm.getId(), lastSessionRefresh);
            }
        }

        UserSessionUpdateTask task = new UserSessionUpdateTask() {

            @Override
            public void runUpdate(UserSessionEntity entity) {
                if (entity.getLastSessionRefresh() >= lastSessionRefresh) {
                    return;
                }
                entity.setLastSessionRefresh(lastSessionRefresh);
            }

            @Override
            public CrossDCMessageStatus getCrossDCMessageStatus(SessionEntityWrapper<UserSessionEntity> sessionWrapper) {
                return new CrossDCLastSessionRefreshChecker(provider.getLastSessionRefreshStore(), provider.getOfflineLastSessionRefreshStore())
                        .shouldSaveUserSessionToRemoteCache(UserSessionAdapter.this.session, UserSessionAdapter.this.realm, sessionWrapper, offline, lastSessionRefresh);
            }

            @Override
            public boolean isOffline() {
                return offline;
            }

            @Override
            public String toString() {
                return "setLastSessionRefresh(" + lastSessionRefresh + ')';
            }
        };

        update(task);
    }

    @Override
    public boolean isOffline() {
        return offline;
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

            @Override
            public boolean isOffline() {
                return offline;
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

            @Override
            public boolean isOffline() {
                return offline;
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
            public boolean isOffline() {
                return offline;
            }

            @Override
            public void runUpdate(UserSessionEntity entity) {
                entity.setState(state);
            }

        };

        update(task);
    }

    @Override
    public SessionPersistenceState getPersistenceState() {
        return persistenceState;
    }

    public void setPersistenceState(SessionPersistenceState persistenceState) {
        this.persistenceState = persistenceState;
    }

    @Override
    public void restartSession(RealmModel realm, UserModel user, String loginUsername, String ipAddress, String authMethod, boolean rememberMe, String brokerSessionId, String brokerUserId) {
        // Sending a delete statement for each client session may have a performance impact.
        // The update task will clear the entity.getAuthenticatedClientSessions().
        entity.getAuthenticatedClientSessions()
                .forEach((ignored, clientSessionId) -> this.clientSessionUpdateTx.addTask(clientSessionId, Tasks.removeSync(offline)));
        UserSessionUpdateTask task = new UserSessionUpdateTask() {

            @Override
            public boolean isOffline() {
                return offline;
            }

            @Override
            public void runUpdate(UserSessionEntity entity) {
                UserSessionEntity.updateSessionEntity(entity, realm, user, loginUsername, ipAddress, authMethod, rememberMe, brokerSessionId, brokerUserId);

                entity.setState(null);
                entity.getNotes().clear();
                entity.getAuthenticatedClientSessions().clear();
            }

        };

        update(task);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !(o instanceof UserSessionModel)) {
            return false;
        }

        UserSessionModel that = (UserSessionModel) o;
        return that.getId().equals(getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    // TODO: This should not be public
    public UserSessionEntity getEntity() {
        return entity;
    }

    void update(UserSessionUpdateTask task) {
        userSessionUpdateTx.addTask(getId(), task);
    }

}
