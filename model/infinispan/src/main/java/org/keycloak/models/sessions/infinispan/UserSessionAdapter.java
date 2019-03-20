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
import java.util.stream.Collectors;

import static org.keycloak.models.sessions.infinispan.changes.sessions.CrossDCLastSessionRefreshListener.IGNORE_REMOTE_CACHE_UPDATE;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class UserSessionAdapter implements UserSessionModel {

    private final KeycloakSession session;

    private final InfinispanUserSessionProvider provider;

    private final InfinispanChangelogBasedTransaction<String, UserSessionEntity> userSessionUpdateTx;

    private final InfinispanChangelogBasedTransaction<UUID, AuthenticatedClientSessionEntity> clientSessionUpdateTx;

    private final RealmModel realm;

    private final UserSessionEntity entity;

    private final boolean offline;

    public UserSessionAdapter(KeycloakSession session, InfinispanUserSessionProvider provider, 
                              InfinispanChangelogBasedTransaction<String, UserSessionEntity> userSessionUpdateTx,
                              InfinispanChangelogBasedTransaction<UUID, AuthenticatedClientSessionEntity> clientSessionUpdateTx,
                              RealmModel realm, UserSessionEntity entity, boolean offline) {
        this.session = session;
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
                    final AuthenticatedClientSessionAdapter clientSession = provider.getClientSession(this, client, value, offline);
                    if (clientSession != null) {
                        result.put(key, clientSession);
                    }
                } else {
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
            return null;
        }

        ClientModel client = realm.getClientById(clientUUID);

        if (client != null) {
            return provider.getClientSession(this, client, clientSessionId, offline);
        }

        removeAuthenticatedClientSessions(Collections.singleton(clientUUID));
        return null;
    }

    private static final int MINIMUM_INACTIVE_CLIENT_SESSIONS_TO_CLEANUP = 5;

    @Override
    public void removeAuthenticatedClientSessions(Collection<String> removedClientUUIDS) {
        if (removedClientUUIDS == null || removedClientUUIDS.isEmpty()) {
            return;
        }

        // Performance: do not remove the clientUUIDs from the user session until there is enough of them;
        // an invalid session is handled as nonexistent in UserSessionAdapter.getAuthenticatedClientSessions()
        if (removedClientUUIDS.size() >= MINIMUM_INACTIVE_CLIENT_SESSIONS_TO_CLEANUP) {
            // Update user session
            UserSessionUpdateTask task = new UserSessionUpdateTask() {
                @Override		
                public void runUpdate(UserSessionEntity entity) {		
                    removedClientUUIDS.forEach(entity.getAuthenticatedClientSessions()::remove);		
                }		
            };		
            update(task);
        }

        // do not iterate the removedClientUUIDS and remove the clientSession directly as the addTask can manipulate
        // the collection being iterated, and that can lead to unpredictable behaviour (e.g. NPE)
        List<UUID> clientSessionUuids = removedClientUUIDS.stream()
          .map(entity.getAuthenticatedClientSessions()::get)
          .filter(Objects::nonNull)
          .collect(Collectors.toList());

        clientSessionUuids.forEach(clientSessionId -> this.clientSessionUpdateTx.addTask(clientSessionId, Tasks.removeSync()));
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
        if (entity.getLoginUsername() == null) {
            // this is a hack so that UserModel doesn't have to be available when offline token is imported.
            // see related JIRA - KEYCLOAK-5350 and corresponding test
            return getUser().getUsername();
        } else {
            return entity.getLoginUsername();
        }
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
        if (offline) {
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
                entity.setLastSessionRefresh(lastSessionRefresh);
            }

            @Override
            public CrossDCMessageStatus getCrossDCMessageStatus(SessionEntityWrapper<UserSessionEntity> sessionWrapper) {
                return new CrossDCLastSessionRefreshChecker(provider.getLastSessionRefreshStore(), provider.getOfflineLastSessionRefreshStore())
                        .shouldSaveUserSessionToRemoteCache(UserSessionAdapter.this.session, UserSessionAdapter.this.realm, sessionWrapper, offline, lastSessionRefresh);
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
        userSessionUpdateTx.addTask(getId(), task);
    }

}
