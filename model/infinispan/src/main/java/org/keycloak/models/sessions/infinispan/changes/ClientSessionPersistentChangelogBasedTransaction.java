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
import org.keycloak.models.session.UserSessionPersisterProvider;
import org.keycloak.models.sessions.infinispan.SessionFunction;
import org.keycloak.models.sessions.infinispan.UserSessionAdapter;
import org.keycloak.models.sessions.infinispan.entities.AuthenticatedClientSessionEntity;
import org.keycloak.models.sessions.infinispan.entities.AuthenticatedClientSessionStore;
import org.keycloak.models.sessions.infinispan.entities.UserSessionEntity;
import org.keycloak.models.sessions.infinispan.remotestore.RemoteCacheInvoker;
import org.keycloak.models.sessions.infinispan.util.InfinispanKeyGenerator;
import org.keycloak.models.sessions.infinispan.util.SessionTimeouts;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ClientSessionPersistentChangelogBasedTransaction extends PersistentSessionsChangelogBasedTransaction<UUID, AuthenticatedClientSessionEntity> {

    private static final Logger LOG = Logger.getLogger(ClientSessionPersistentChangelogBasedTransaction.class);
    private final InfinispanKeyGenerator keyGenerator;
    private final UserSessionPersistentChangelogBasedTransaction userSessionTx;

    public ClientSessionPersistentChangelogBasedTransaction(KeycloakSession session, Cache<UUID, SessionEntityWrapper<AuthenticatedClientSessionEntity>> cache, RemoteCacheInvoker remoteCacheInvoker, SessionFunction<AuthenticatedClientSessionEntity> lifespanMsLoader, SessionFunction<AuthenticatedClientSessionEntity> maxIdleTimeMsLoader, boolean offline, InfinispanKeyGenerator keyGenerator, UserSessionPersistentChangelogBasedTransaction userSessionTx) {
        super(session, cache, remoteCacheInvoker, lifespanMsLoader, maxIdleTimeMsLoader, offline);
        this.keyGenerator = keyGenerator;
        this.userSessionTx = userSessionTx;
    }

    public SessionEntityWrapper<AuthenticatedClientSessionEntity> get(RealmModel realm, ClientModel client, UserSessionModel userSession, UUID key) {
        SessionUpdatesList<AuthenticatedClientSessionEntity> myUpdates = updates.get(key);
        if (myUpdates == null) {
            SessionEntityWrapper<AuthenticatedClientSessionEntity> wrappedEntity = cache.get(key);
            if (wrappedEntity == null) {
                wrappedEntity = getSessionEntityFromPersister(realm, client, userSession);
            }

            if (wrappedEntity == null) {
                return null;
            }

            RealmModel realmFromSession = kcSession.realms().getRealm(wrappedEntity.getEntity().getRealmId());
            if (!realmFromSession.getId().equals(realm.getId())) {
                LOG.warnf("Realm mismatch for session %s. Expected realm %s, but found realm %s", wrappedEntity.getEntity(), realm.getId(), realmFromSession.getId());
                return null;
            }

            myUpdates = new SessionUpdatesList<>(realm, wrappedEntity);
            updates.put(key, myUpdates);

            return wrappedEntity;
        } else {
            AuthenticatedClientSessionEntity entity = myUpdates.getEntityWrapper().getEntity();

            // If entity is scheduled for remove, we don't return it.
            boolean scheduledForRemove = myUpdates.getUpdateTasks().stream().filter((SessionUpdateTask task) -> {

                return task.getOperation(entity) == SessionUpdateTask.CacheOperation.REMOVE;

            }).findFirst().isPresent();

            return scheduledForRemove ? null : myUpdates.getEntityWrapper();
        }
    }

    private SessionEntityWrapper<AuthenticatedClientSessionEntity> getSessionEntityFromPersister(RealmModel realm, ClientModel client, UserSessionModel userSession) {
        UserSessionPersisterProvider persister = kcSession.getProvider(UserSessionPersisterProvider.class);
        AuthenticatedClientSessionModel clientSession = persister.loadClientSession(realm, client, userSession, offline);

        if (clientSession == null) {
            return null;
        }

        SessionEntityWrapper<AuthenticatedClientSessionEntity> authenticatedClientSessionEntitySessionEntityWrapper = importClientSession(realm, client, userSession, clientSession);
        if (authenticatedClientSessionEntitySessionEntityWrapper == null) {
            persister.removeClientSession(userSession.getId(), client.getId(), offline);
        }

        return authenticatedClientSessionEntitySessionEntityWrapper;
    }

    private AuthenticatedClientSessionEntity createAuthenticatedClientSessionInstance(AuthenticatedClientSessionModel clientSession,
                                                                                      String realmId, String clientId) {
        UUID clientSessionId = null;
        if (clientSession.getId() != null) {
            clientSessionId = UUID.fromString(clientSession.getId());
        } else {
            clientSessionId = keyGenerator.generateKeyUUID(kcSession, cache);
        }

        AuthenticatedClientSessionEntity entity = new AuthenticatedClientSessionEntity(clientSessionId);
        entity.setRealmId(realmId);
        entity.setClientId(clientId);

        entity.setAction(clientSession.getAction());
        entity.setAuthMethod(clientSession.getProtocol());

        entity.setNotes(clientSession.getNotes() == null ? new ConcurrentHashMap<>() : clientSession.getNotes());
        entity.setRedirectUri(clientSession.getRedirectUri());
        entity.setTimestamp(clientSession.getTimestamp());

        return entity;
    }

    private SessionEntityWrapper<AuthenticatedClientSessionEntity> importClientSession(RealmModel realm, ClientModel client, UserSessionModel userSession, AuthenticatedClientSessionModel persistentUserSession) {
        AuthenticatedClientSessionEntity entity = createAuthenticatedClientSessionInstance(persistentUserSession,
                realm.getId(), client.getId());

        entity.setUserSessionId(userSession.getId());

        // Update timestamp to same value as userSession. LastSessionRefresh of userSession from DB will have correct value
        entity.setTimestamp(userSession.getLastSessionRefresh());

        if (maxIdleTimeMsLoader.apply(realm, client, entity) == SessionTimeouts.ENTRY_EXPIRED_FLAG
                || lifespanMsLoader.apply(realm, client, entity) == SessionTimeouts.ENTRY_EXPIRED_FLAG) {
            return null;
        }

        final UUID clientSessionId = entity.getId();

        SessionUpdateTask<AuthenticatedClientSessionEntity> createClientSessionTask = Tasks.addIfAbsentSync();
        this.addTask(entity.getId(), createClientSessionTask, entity, UserSessionModel.SessionPersistenceState.PERSISTENT);

        if (! (userSession instanceof UserSessionAdapter)) {
            throw new IllegalStateException("UserSessionModel must be instance of UserSessionAdapter");
        }

        UserSessionAdapter sessionToImportInto = (UserSessionAdapter) userSession;
        AuthenticatedClientSessionStore clientSessions = sessionToImportInto.getEntity().getAuthenticatedClientSessions();
        clientSessions.put(client.getId(), clientSessionId);

        SessionUpdateTask registerClientSessionTask = new RegisterClientSessionTask(client.getId(), clientSessionId);
        userSessionTx.addTask(sessionToImportInto.getId(), registerClientSessionTask);

        return new SessionEntityWrapper<>(entity);
    }

    private static class RegisterClientSessionTask implements SessionUpdateTask<UserSessionEntity> {

        private final String clientUuid;
        private final UUID clientSessionId;

        public RegisterClientSessionTask(String clientUuid, UUID clientSessionId) {
            this.clientUuid = clientUuid;
            this.clientSessionId = clientSessionId;
        }

        @Override
        public void runUpdate(UserSessionEntity session) {
            AuthenticatedClientSessionStore clientSessions = session.getAuthenticatedClientSessions();
            clientSessions.put(clientUuid, clientSessionId);
        }

        @Override
        public CacheOperation getOperation(UserSessionEntity session) {
            return CacheOperation.REPLACE;
        }

        @Override
        public CrossDCMessageStatus getCrossDCMessageStatus(SessionEntityWrapper<UserSessionEntity> sessionWrapper) {
            return CrossDCMessageStatus.SYNC;
        }
    }
}
