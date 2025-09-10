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

package org.keycloak.models.sessions.infinispan.changes;

import org.infinispan.Cache;
import org.jboss.logging.Logger;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.UserSessionProvider;
import org.keycloak.models.session.UserSessionPersisterProvider;
import org.keycloak.models.sessions.infinispan.PersistentUserSessionProvider;
import org.keycloak.models.sessions.infinispan.SessionFunction;
import org.keycloak.models.sessions.infinispan.UserSessionAdapter;
import org.keycloak.models.sessions.infinispan.entities.AuthenticatedClientSessionEntity;
import org.keycloak.models.sessions.infinispan.entities.AuthenticatedClientSessionStore;
import org.keycloak.models.sessions.infinispan.entities.UserSessionEntity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.CLIENT_SESSION_CACHE_NAME;

public class ClientSessionPersistentChangelogBasedTransaction extends PersistentSessionsChangelogBasedTransaction<UUID, AuthenticatedClientSessionEntity> {

    private static final Logger LOG = Logger.getLogger(ClientSessionPersistentChangelogBasedTransaction.class);
    private final UserSessionPersistentChangelogBasedTransaction userSessionTx;

    public ClientSessionPersistentChangelogBasedTransaction(KeycloakSession session,
                                                            Cache<UUID, SessionEntityWrapper<AuthenticatedClientSessionEntity>> cache,
                                                            Cache<UUID, SessionEntityWrapper<AuthenticatedClientSessionEntity>> offlineCache,
                                                            SessionFunction<AuthenticatedClientSessionEntity> lifespanMsLoader,
                                                            SessionFunction<AuthenticatedClientSessionEntity> maxIdleTimeMsLoader,
                                                            SessionFunction<AuthenticatedClientSessionEntity> offlineLifespanMsLoader,
                                                            SessionFunction<AuthenticatedClientSessionEntity> offlineMaxIdleTimeMsLoader,
                                                            UserSessionPersistentChangelogBasedTransaction userSessionTx,
                                                            ArrayBlockingQueue<PersistentUpdate> batchingQueue,
                                                            SerializeExecutionsByKey<UUID> serializerOnline,
                                                            SerializeExecutionsByKey<UUID> serializerOffline) {
        super(session, CLIENT_SESSION_CACHE_NAME, cache, offlineCache, lifespanMsLoader, maxIdleTimeMsLoader, offlineLifespanMsLoader, offlineMaxIdleTimeMsLoader, batchingQueue, serializerOnline, serializerOffline);
        this.userSessionTx = userSessionTx;
    }

    public SessionEntityWrapper<AuthenticatedClientSessionEntity> get(RealmModel realm, ClientModel client, UserSessionModel userSession, UUID key, boolean offline) {
        SessionUpdatesList<AuthenticatedClientSessionEntity> myUpdates = getUpdates(offline).get(key);
        if (myUpdates == null) {
            SessionEntityWrapper<AuthenticatedClientSessionEntity> wrappedEntity = null;
            Cache<UUID, SessionEntityWrapper<AuthenticatedClientSessionEntity>> cache = getCache(offline);
            if (cache != null) {
                wrappedEntity = cache.get(key);
            }

            if (wrappedEntity == null) {
                LOG.tracef("Client-session not found in cache, loading from persister. userSessionId=%s, clientSessionId=%s, clientId=%s, offline=%s",
                        userSession.getId(), key, client.getId(), offline);
                wrappedEntity = getSessionEntityFromPersister(realm, client, userSession, key, offline);
            } else {
                LOG.tracef("Client-session found in cache. userSessionId=%s, clientSessionId=%s, clientId=%s, offline=%s",
                        userSession.getId(), key, client.getId(), offline);
            }

            if (wrappedEntity == null) {
                LOG.debugf("Client-session not found in persister. userSessionId=%s, clientSessionId=%s, clientId=%s, offline=%s",
                        userSession.getId(), key, client.getId(), offline);
                return null;
            }

            // Cache does not contain the offline flag value so adding it
            wrappedEntity.getEntity().setOffline(offline);
            wrappedEntity.getEntity().setUserSessionId(userSession.getId());

            RealmModel realmFromSession = kcSession.realms().getRealm(wrappedEntity.getEntity().getRealmId());
            if (!realmFromSession.getId().equals(realm.getId())) {
                LOG.warnf("Realm mismatch for session %s. Expected realm %s, but found realm %s", wrappedEntity.getEntity(), realm.getId(), realmFromSession.getId());
                return null;
            }

            myUpdates = new SessionUpdatesList<>(realm, wrappedEntity);
            getUpdates(offline).put(key, myUpdates);

            return wrappedEntity;
        } else {

            // If entity is scheduled for remove, we don't return it.
            boolean scheduledForRemove = myUpdates.getUpdateTasks().stream()
                    .map(SessionUpdateTask::getOperation)
                    .anyMatch(SessionUpdateTask.CacheOperation.REMOVE::equals);

            if (scheduledForRemove) {
                LOG.debugf("Client-session scheduled for removal in transaction. userSessionId=%s, clientSessionId=%s, clientId=%s, offline=%s",
                        userSession.getId(), key, client.getId(), offline);
            }

            return scheduledForRemove ? null : myUpdates.getEntityWrapper();
        }
    }

    private SessionEntityWrapper<AuthenticatedClientSessionEntity> getSessionEntityFromPersister(RealmModel realm, ClientModel client, UserSessionModel userSession, UUID clientSessionId, boolean offline) {
        UserSessionPersisterProvider persister = kcSession.getProvider(UserSessionPersisterProvider.class);
        AuthenticatedClientSessionModel clientSession = persister.loadClientSession(realm, client, userSession, offline);

        if (clientSession == null) {
            LOG.debugf("Client-session not loaded from persister. userSessionId=%s, clientSessionId=%s, clientId=%s, offline=%s",
                    userSession.getId(), clientSessionId, client.getId(), offline);
            return null;
        }

        SessionEntityWrapper<AuthenticatedClientSessionEntity> authenticatedClientSessionEntitySessionEntityWrapper = importClientSession(realm, client, userSession, clientSession, clientSessionId);
        if (authenticatedClientSessionEntitySessionEntityWrapper == null) {
            LOG.debugf("Client-session not imported from persister. userSessionId=%s, clientSessionId=%s, clientId=%s, offline=%s",
                    userSession.getId(), clientSessionId, client.getId(), offline);
        }

        return authenticatedClientSessionEntitySessionEntityWrapper;
    }

    private AuthenticatedClientSessionEntity createAuthenticatedClientSessionInstance(String userSessionId, AuthenticatedClientSessionModel clientSession,
                                                                                      String realmId, String clientId) {
        UUID clientSessionId = PersistentUserSessionProvider.createClientSessionUUID(userSessionId, clientId);

        AuthenticatedClientSessionEntity entity = new AuthenticatedClientSessionEntity(clientSessionId);
        entity.setRealmId(realmId);

        entity.setAction(clientSession.getAction());
        entity.setAuthMethod(clientSession.getProtocol());

        entity.setNotes(clientSession.getNotes() == null ? new ConcurrentHashMap<>() : clientSession.getNotes());
        entity.setClientId(clientId);
        entity.setRedirectUri(clientSession.getRedirectUri());
        entity.setTimestamp(clientSession.getTimestamp());
        entity.setOffline(clientSession.getUserSession().isOffline());
        entity.setUserSessionId(userSessionId);

        return entity;
    }

    private SessionEntityWrapper<AuthenticatedClientSessionEntity> importClientSession(RealmModel realm, ClientModel client, UserSessionModel userSession, AuthenticatedClientSessionModel persistentClientSession, UUID clientSessionId) {
        AuthenticatedClientSessionEntity entity = createAuthenticatedClientSessionInstance(userSession.getId(), persistentClientSession,
                realm.getId(), client.getId());
        boolean offline = userSession.isOffline();

        entity.setUserSessionId(userSession.getId());

        if (offline) {
            // Update timestamp to the same value as userSession. LastSessionRefresh of userSession from DB will have a correct value.
            // This is an optimization with the old code before persistent user sessions existed, and is probably valid as an offline user session is supposed to have only one client session.
            // Remove this code once this once the persistent sessions is the only way to handle sessions, and the old client sessions have been migrated to have an updated timestamp.
            entity.setTimestamp(userSession.getLastSessionRefresh());
        }

        SessionEntityWrapper<AuthenticatedClientSessionEntity> wrapper = new SessionEntityWrapper<>(entity);
        Map<UUID, SessionEntityWrapper<AuthenticatedClientSessionEntity>> imported = ((PersistentUserSessionProvider) kcSession.getProvider(UserSessionProvider.class)).importSessionsWithExpiration(Map.of(clientSessionId, wrapper), getCache(offline),
                getLifespanMsLoader(offline),
                getMaxIdleMsLoader(offline));

        if (imported.isEmpty()) {
            LOG.debugf("Client-session has expired, not importing it. userSessionId=%s, clientSessionId=%s, clientId=%s, offline=%s",
                    userSession.getId(), clientSessionId, client.getId(), offline);
            return null;
        }

        SessionUpdateTask<AuthenticatedClientSessionEntity> createClientSessionTask = Tasks.addIfAbsentSync();
        this.addTask(entity.getId(), createClientSessionTask, entity, UserSessionModel.SessionPersistenceState.PERSISTENT);

        if (! (userSession instanceof UserSessionAdapter<?> sessionToImportInto)) {
            throw new IllegalStateException("UserSessionModel must be instance of UserSessionAdapter");
        }

        AuthenticatedClientSessionStore clientSessions = sessionToImportInto.getEntity().getAuthenticatedClientSessions();
        clientSessions.put(client.getId(), clientSessionId);

        SessionUpdateTask<UserSessionEntity> registerClientSessionTask = new RegisterClientSessionTask(client.getId(), clientSessionId, offline);
        userSessionTx.addTask(sessionToImportInto.getId(), registerClientSessionTask);

        return wrapper;
    }

    public static class RegisterClientSessionTask implements PersistentSessionUpdateTask<UserSessionEntity> {

        private final String clientUuid;
        private final UUID clientSessionId;
        private final boolean offline;

        public RegisterClientSessionTask(String clientUuid, UUID clientSessionId, boolean offline) {
            this.clientUuid = clientUuid;
            this.clientSessionId = clientSessionId;
            this.offline = offline;
        }

        @Override
        public void runUpdate(UserSessionEntity session) {
            AuthenticatedClientSessionStore clientSessions = session.getAuthenticatedClientSessions();
            clientSessions.put(clientUuid, clientSessionId);
        }

        @Override
        public CacheOperation getOperation() {
            return CacheOperation.REPLACE;
        }

        @Override
        public boolean isOffline() {
            return offline;
        }
    }
}
