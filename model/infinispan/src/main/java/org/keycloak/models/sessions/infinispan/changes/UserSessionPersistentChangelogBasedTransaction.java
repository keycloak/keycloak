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
import org.infinispan.util.concurrent.BlockingManager;
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.UserSessionProvider;
import org.keycloak.models.session.UserSessionPersisterProvider;
import org.keycloak.models.sessions.infinispan.PersistentUserSessionProvider;
import org.keycloak.models.sessions.infinispan.SessionFunction;
import org.keycloak.models.sessions.infinispan.entities.SessionEntity;
import org.keycloak.models.sessions.infinispan.entities.UserSessionEntity;

import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;

import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.USER_SESSION_CACHE_NAME;

public class UserSessionPersistentChangelogBasedTransaction extends PersistentSessionsChangelogBasedTransaction<String, UserSessionEntity> {

    private static final Logger LOG = Logger.getLogger(UserSessionPersistentChangelogBasedTransaction.class);

    public UserSessionPersistentChangelogBasedTransaction(KeycloakSession session,
                                                          Cache<String, SessionEntityWrapper<UserSessionEntity>> cache,
                                                          Cache<String, SessionEntityWrapper<UserSessionEntity>> offlineCache,
                                                          SessionFunction<UserSessionEntity> lifespanMsLoader,
                                                          SessionFunction<UserSessionEntity> maxIdleTimeMsLoader,
                                                          SessionFunction<UserSessionEntity> offlineLifespanMsLoader,
                                                          SessionFunction<UserSessionEntity> offlineMaxIdleTimeMsLoader,
                                                          ArrayBlockingQueue<PersistentUpdate> batchingQueue,
                                                          SerializeExecutionsByKey<String> serializerOnline,
                                                          SerializeExecutionsByKey<String> serializerOffline,
                                                          BlockingManager blockingManager) {
        super(session, USER_SESSION_CACHE_NAME, cache, offlineCache, lifespanMsLoader, maxIdleTimeMsLoader, offlineLifespanMsLoader, offlineMaxIdleTimeMsLoader, batchingQueue, serializerOnline, serializerOffline, blockingManager);
    }

    public SessionEntityWrapper<UserSessionEntity> get(RealmModel realm, String key, UserSessionModel userSession, boolean offline) {
        SessionUpdatesList<UserSessionEntity> myUpdates = getUpdates(offline).get(key);
        if (myUpdates == null) {
            SessionEntityWrapper<UserSessionEntity> wrappedEntity = null;
            Cache<String, SessionEntityWrapper<UserSessionEntity>> cache = getCache(offline);
            if (cache != null) {
                wrappedEntity = cache.get(key);
            }

            if (wrappedEntity == null) {
                LOG.debugf("user-session not found in cache for sessionId=%s offline=%s, loading from persister", key, offline);
                wrappedEntity = getSessionEntityFromPersister(realm, key, userSession, offline);
            } else {
                LOG.debugf("user-session found in cache for sessionId=%s offline=%s %s", key, offline, wrappedEntity.getEntity().getLastSessionRefresh());
            }

            if (wrappedEntity == null) {
                LOG.debugf("user-session not found in persister for sessionId=%s offline=%s", key, offline);
                return null;
            }

            // Cache does not contain the offline flag value so adding it
            wrappedEntity.getEntity().setOffline(offline);

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

            return scheduledForRemove ? null : myUpdates.getEntityWrapper();
        }
    }

    private SessionEntityWrapper<UserSessionEntity> getSessionEntityFromPersister(RealmModel realm, String key, UserSessionModel userSession, boolean offline) {
        if (userSession == null) {
            UserSessionPersisterProvider persister = kcSession.getProvider(UserSessionPersisterProvider.class);
            userSession = persister.loadUserSession(realm, key, offline);
        }

        if (userSession == null) {
            return null;
        }

        return importUserSession(userSession);
    }

    private SessionEntityWrapper<UserSessionEntity> importUserSession(UserSessionModel persistentUserSession) {
        String sessionId = persistentUserSession.getId();
        boolean offline = persistentUserSession.isOffline();

        if (isScheduledForRemove(sessionId, offline)) {
            return null;
        }

        if (getCache(offline) == null) {
            return ((PersistentUserSessionProvider) kcSession.getProvider(UserSessionProvider.class)).wrapPersistentEntity(persistentUserSession.getRealm(), offline, persistentUserSession);
        }

        LOG.debugf("Attempting to import user-session for sessionId=%s offline=%s", sessionId, offline);
        SessionEntityWrapper<UserSessionEntity> ispnUserSessionEntity = ((PersistentUserSessionProvider) kcSession.getProvider(UserSessionProvider.class)).importUserSession(persistentUserSession, offline);

        if (ispnUserSessionEntity != null) {
            LOG.debugf("user-session found after import for sessionId=%s offline=%s", sessionId, offline);
            return ispnUserSessionEntity;
        }

        LOG.debugf("user-session could not be found after import for sessionId=%s offline=%s", sessionId, offline);
        return null;
    }

    public boolean isScheduledForRemove(String key, boolean offline) {
        return isScheduledForRemove(getUpdates(offline).get(key));
    }

    public void registerClientSession(String userSessionId, String clientId, UUID clientSessionId, boolean offline) {
        addTask(userSessionId, new PersistentSessionUpdateTask<>() {
            @Override
            public boolean isOffline() {
                return offline;
            }

            @Override
            public void runUpdate(UserSessionEntity entity) {
                entity.getAuthenticatedClientSessions().put(clientId, clientSessionId);
            }

            @Override
            public CacheOperation getOperation() {
                return CacheOperation.REPLACE;
            }
        });
    }

    private static <V extends SessionEntity> boolean isScheduledForRemove(SessionUpdatesList<V> myUpdates) {
        if (myUpdates == null) {
            return false;
        }
        // If entity is scheduled for remove, we don't return it.

        return myUpdates.getUpdateTasks()
                .stream()
                .anyMatch(task -> task.getOperation() == SessionUpdateTask.CacheOperation.REMOVE);
    }
}
