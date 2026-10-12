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

import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.session.UserSessionPersisterProvider;
import org.keycloak.models.sessions.infinispan.CacheDecorators;
import org.keycloak.models.sessions.infinispan.UserSessionAdapter;
import org.keycloak.models.sessions.infinispan.entities.AuthenticatedClientSessionEntity;
import org.keycloak.models.sessions.infinispan.entities.EmbeddedClientSessionKey;
import org.keycloak.models.sessions.infinispan.entities.UserSessionEntity;
import org.keycloak.models.sessions.infinispan.util.SessionTimeouts;

import org.infinispan.Cache;
import org.infinispan.commons.util.concurrent.AggregateCompletionStage;
import org.infinispan.commons.util.concurrent.CompletionStages;
import org.jboss.logging.Logger;

import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.CLIENT_SESSION_CACHE_NAME;

public class ClientSessionPersistentChangelogBasedTransaction extends PersistentSessionsChangelogBasedTransaction<EmbeddedClientSessionKey, AuthenticatedClientSessionEntity> {

    private static final Logger LOG = Logger.getLogger(ClientSessionPersistentChangelogBasedTransaction.class);
    private final UserSessionPersistentChangelogBasedTransaction userSessionTx;
    private final boolean pessimisticLockingAuthenticationSession;

    public ClientSessionPersistentChangelogBasedTransaction(KeycloakSession session,
                                                            CacheHolder<EmbeddedClientSessionKey, AuthenticatedClientSessionEntity> cacheHolder,
                                                            CacheHolder<EmbeddedClientSessionKey, AuthenticatedClientSessionEntity> offlineCacheHolder,
                                                            UserSessionPersistentChangelogBasedTransaction userSessionTx,
                                                            boolean pessimisticLockingAuthenticationSession,
                                                            long maxCacheLifespanMs) {
        super(session, CLIENT_SESSION_CACHE_NAME, cacheHolder, offlineCacheHolder, maxCacheLifespanMs);
        this.userSessionTx = userSessionTx;
        this.pessimisticLockingAuthenticationSession = pessimisticLockingAuthenticationSession;
    }

    public void setUserSessionId(Collection<EmbeddedClientSessionKey> keys, String userSessionId, boolean offline) {
        keys.stream().map(getUpdates(offline)::get)
                .filter(Objects::nonNull)
                .map(SessionUpdatesList::getEntityWrapper)
                .map(SessionEntityWrapper::getEntity)
                .filter(Objects::nonNull)
                .forEach(authenticatedClientSessionEntity -> authenticatedClientSessionEntity.setUserSessionId(userSessionId));
    }


    public SessionEntityWrapper<AuthenticatedClientSessionEntity> get(RealmModel realm, ClientModel client, UserSessionModel userSession, EmbeddedClientSessionKey key, boolean offline) {
        if (key == null) {
            key = new EmbeddedClientSessionKey(userSession.getId(), client.getId());
        }
        SessionUpdatesList<AuthenticatedClientSessionEntity> myUpdates = getUpdates(offline).get(key);
        if (myUpdates == null) {
            SessionEntityWrapper<AuthenticatedClientSessionEntity> wrappedEntity = null;
            Cache<EmbeddedClientSessionKey, SessionEntityWrapper<AuthenticatedClientSessionEntity>> cache = getCache(offline);
            if (cache != null) {
                // Place a loading marker to prevent concurrent reads from resurrecting
                // a deleted client session via cache import.
                AuthenticatedClientSessionEntity markerEntity = new AuthenticatedClientSessionEntity();
                markerEntity.setRealmId(realm.getId());
                SessionEntityWrapper<AuthenticatedClientSessionEntity> marker = SessionEntityWrapper.createLoadingMarker(markerEntity);
                SessionEntityWrapper<AuthenticatedClientSessionEntity> existing = cache.putIfAbsent(key, marker, SessionEntityWrapper.LOADING_MARKER_LIFESPAN_MS, TimeUnit.MILLISECONDS);

                if (existing == null) {
                    storeLoadingMarker(key, marker, offline);
                } else if (!existing.isLoadingMarker()) {
                    wrappedEntity = existing;
                    LOG.tracef("Client-session found in cache. userSessionId=%s, clientSessionId=%s, clientId=%s, offline=%s",
                            userSession.getId(), key, client.getId(), offline);
                }
            }

            try {
                if (wrappedEntity == null) {
                    LOG.tracef("Client-session not found in cache, loading from persister. userSessionId=%s, clientSessionId=%s, clientId=%s, offline=%s",
                            userSession.getId(), key, client.getId(), offline);
                    if (hasStoredLoadingMarker(key, offline)) {
                        // We own the marker — load from DB and import into cache with CAS protection
                        wrappedEntity = getSessionEntityFromPersister(realm, client, userSession, key, offline);
                    } else {
                        // Another thread's marker — use data without caching
                        wrappedEntity = loadClientSessionFromPersisterWithoutCaching(realm, client, userSession, key, offline);
                    }
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
            } finally {
                cleanupLoadingMarker(key, offline);
            }
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

    /**
     * Runs cache operations after the DB commit. For REMOVE operations, the parent user session
     * cache entry is invalidated first — a concurrent reader that re-loads the user session will
     * then see the committed state (client session deleted) instead of a dangling reference.
     */
    @Override
    public void asyncPostDatabaseCommit(AggregateCompletionStage<Void> stage) {
        var removeOps = pendingCacheOps.stream()
                .filter(op -> op.merged().getOperation() == SessionUpdateTask.CacheOperation.REMOVE)
                .toList();
        var nonRemoveOps = pendingCacheOps.stream()
                .filter(op -> op.merged().getOperation() != SessionUpdateTask.CacheOperation.REMOVE)
                .toList();

        for (var op : nonRemoveOps) {
            InfinispanChangesUtils.runOperationInCluster(op.cacheHolder(), op.key(), op.merged(), op.wrapper(), stage, LOG);
        }

        if (!removeOps.isEmpty()) {
            // Evict the parent user session from cache so that concurrent readers re-load
            // from DB and see updated client-session lists. This may race with a REPLACE
            // from the user session tx's asyncPostDatabaseCommit (e.g. lastSessionRefresh
            // update); whichever wins, the next reader self-heals from DB.
            AggregateCompletionStage<Void> parentInvalidations = CompletionStages.aggregateCompletionStage();
            for (var op : removeOps) {
                String userSessionId = op.key().userSessionId();
                Cache<String, SessionEntityWrapper<UserSessionEntity>> userCache = userSessionTx.getCache(op.offline());
                if (userCache != null) {
                    parentInvalidations.dependsOn(userCache.removeAsync(userSessionId));
                }
            }

            stage.dependsOn(parentInvalidations.freeze().thenCompose(v -> {
                AggregateCompletionStage<Void> clientRemoves = CompletionStages.aggregateCompletionStage();
                for (var op : removeOps) {
                    clientRemoves.dependsOn(
                        CacheDecorators.ignoreReturnValues(op.cacheHolder().cache()).removeAsync(op.key())
                    );
                }
                return clientRemoves.freeze();
            }));
        }
    }

    @Override
    protected void prepareMarkerEntityForRemoval(EmbeddedClientSessionKey key, AuthenticatedClientSessionEntity entity) {
        entity.setUserSessionId(key.userSessionId());
        entity.setClientId(key.clientId());
    }

    @Override
    protected boolean lockDatabaseEntity(RealmModel realm, EmbeddedClientSessionKey clientSessionKey, boolean offline, SessionUpdateTask.CacheOperation operation) {
        if (operation == SessionUpdateTask.CacheOperation.ADD_IF_ABSENT) {
            // There might be concurrent inserts for the same key, which can lead to conflicts.
            // If the authentication session was locked pessimistically, we can still perform the insert safely.
            // See UserSessionConcurrencyTest#testConcurrentNotesChange for a test.
            return pessimisticLockingAuthenticationSession;
        } else {
            return kcSession.getProvider(UserSessionPersisterProvider.class).lockClientSession(realm, clientSessionKey.userSessionId(), clientSessionKey.clientId(), offline, operation == SessionUpdateTask.CacheOperation.REMOVE);
        }
    }

    private SessionEntityWrapper<AuthenticatedClientSessionEntity> loadClientSessionFromPersisterWithoutCaching(RealmModel realm, ClientModel client, UserSessionModel userSession, EmbeddedClientSessionKey key, boolean offline) {
        UserSessionPersisterProvider persister = kcSession.getProvider(UserSessionPersisterProvider.class);
        AuthenticatedClientSessionModel clientSession = persister.loadClientSession(realm, client, userSession, offline);
        if (clientSession == null) {
            return null;
        }
        AuthenticatedClientSessionEntity entity = createAuthenticatedClientSessionInstance(
                userSession.getId(), userSession.getUser().getId(), clientSession,
                realm.getId(), client.getId(), offline);
        if (offline) {
            entity.setTimestamp(userSession.getLastSessionRefresh());
        }
        entity.setUserSessionId(userSession.getId());

        long lifespan = getLifespanMsLoader(offline).apply(realm, client, entity);
        long maxIdle = getMaxIdleMsLoader(offline).apply(realm, client, entity);
        if (lifespan == SessionTimeouts.ENTRY_EXPIRED_FLAG || maxIdle == SessionTimeouts.ENTRY_EXPIRED_FLAG) {
            return null;
        }

        addTask(key, null, entity, UserSessionModel.SessionPersistenceState.PERSISTENT);
        return new SessionEntityWrapper<>(entity);
    }

    private SessionEntityWrapper<AuthenticatedClientSessionEntity> getSessionEntityFromPersister(RealmModel realm, ClientModel client, UserSessionModel userSession, EmbeddedClientSessionKey clientSessionId, boolean offline) {
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

    public static AuthenticatedClientSessionEntity createAuthenticatedClientSessionInstance(String userSessionId, String userId, AuthenticatedClientSessionModel clientSession,
                                                                                      String realmId, String clientId, boolean offline) {

        AuthenticatedClientSessionEntity entity = new AuthenticatedClientSessionEntity();
        entity.setRealmId(realmId);

        entity.setAction(clientSession.getAction());
        entity.setAuthMethod(clientSession.getProtocol());

        entity.setNotes(clientSession.getNotes() == null ? new ConcurrentHashMap<>() : clientSession.getNotes());
        entity.setClientId(clientId);
        entity.setRedirectUri(clientSession.getRedirectUri());
        entity.setTimestamp(clientSession.getTimestamp());
        entity.setOffline(offline);
        entity.setUserSessionId(userSessionId);
        entity.setUserId(userId);

        return entity;
    }

    private SessionEntityWrapper<AuthenticatedClientSessionEntity> importClientSession(RealmModel realm, ClientModel client, UserSessionModel userSession, AuthenticatedClientSessionModel persistentClientSession, EmbeddedClientSessionKey clientSessionId) {
        AuthenticatedClientSessionEntity entity = createAuthenticatedClientSessionInstance(userSession.getId(), userSession.getUser().getId(), persistentClientSession,
                realm.getId(), client.getId(), userSession.isOffline());
        boolean offline = userSession.isOffline();

        entity.setUserSessionId(userSession.getId());

        if (offline) {
            // Update timestamp to the same value as userSession. LastSessionRefresh of userSession from DB will have a correct value.
            // This is an optimization with the old code before persistent user sessions existed, and is probably valid as an offline user session is supposed to have only one client session.
            // Remove this code once this once the persistent sessions is the only way to handle sessions, and the old client sessions have been migrated to have an updated timestamp.
            entity.setTimestamp(userSession.getLastSessionRefresh());
        }

        long lifespan = getLifespanMsLoader(offline).apply(realm, client, entity);
        long maxIdle = getMaxIdleMsLoader(offline).apply(realm, client, entity);

        if (lifespan == SessionTimeouts.ENTRY_EXPIRED_FLAG || maxIdle == SessionTimeouts.ENTRY_EXPIRED_FLAG) {
            LOG.debugf("Client-session has expired, not importing it. userSessionId=%s, clientSessionId=%s, clientId=%s, offline=%s",
                    userSession.getId(), clientSessionId, client.getId(), offline);
            return null;
        }
        
        SessionEntityWrapper<AuthenticatedClientSessionEntity> wrapper = new SessionEntityWrapper<>(entity);

        SessionEntityWrapper<AuthenticatedClientSessionEntity> imported = importSession(realm, clientSessionId, wrapper, offline, lifespan, maxIdle);

        if (imported != null) {
            if (imported.isLoadingMarker()) {
                LOG.debugf("CAS failed for client-session. userSessionId=%s, clientSessionId=%s, clientId=%s, offline=%s",
                        userSession.getId(), clientSessionId, client.getId(), offline);
                return null;
            }
            LOG.debugf("Client-session already imported by another transaction. userSessionId=%s, clientSessionId=%s, clientId=%s, offline=%s",
                    userSession.getId(), clientSessionId, client.getId(), offline);
            imported.getEntity().setUserSessionId(userSession.getId());
            return imported;
        }

        // TODO do we need the code below? In theory, if we are importing a client session, it is already mapped in the user session
        if (! (userSession instanceof UserSessionAdapter<?> sessionToImportInto)) {
            throw new IllegalStateException("UserSessionModel must be instance of UserSessionAdapter");
        }

        if (sessionToImportInto.getEntity().getClientSessions().add(client.getId())) {
            userSessionTx.registerClientSession(sessionToImportInto.getId(), client.getId(), offline);
        }

        return wrapper;
    }

}
