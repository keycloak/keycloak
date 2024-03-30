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
import org.keycloak.common.Profile;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.UserSessionProvider;
import org.keycloak.models.session.UserSessionPersisterProvider;
import org.keycloak.models.sessions.infinispan.PersistentUserSessionProvider;
import org.keycloak.models.sessions.infinispan.SessionFunction;
import org.keycloak.models.sessions.infinispan.entities.SessionEntity;
import org.keycloak.models.sessions.infinispan.entities.UserSessionEntity;
import org.keycloak.models.sessions.infinispan.remotestore.RemoteCacheInvoker;

import java.util.Collections;
import java.util.Objects;

import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.CLIENT_SESSION_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.OFFLINE_CLIENT_SESSION_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.OFFLINE_USER_SESSION_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.USER_SESSION_CACHE_NAME;

public class UserSessionPersistentChangelogBasedTransaction extends PersistentSessionsChangelogBasedTransaction<String, UserSessionEntity> {

    private static final Logger LOG = Logger.getLogger(UserSessionPersistentChangelogBasedTransaction.class);
    public UserSessionPersistentChangelogBasedTransaction(KeycloakSession session, Cache<String, SessionEntityWrapper<UserSessionEntity>> cache, RemoteCacheInvoker remoteCacheInvoker, SessionFunction<UserSessionEntity> lifespanMsLoader, SessionFunction<UserSessionEntity> maxIdleTimeMsLoader, boolean offline) {
        super(session, cache, remoteCacheInvoker, lifespanMsLoader, maxIdleTimeMsLoader, offline);
    }

    public SessionEntityWrapper<UserSessionEntity> get(RealmModel realm, String key) {
        SessionUpdatesList<UserSessionEntity> myUpdates = updates.get(key);
        if (myUpdates == null) {
            SessionEntityWrapper<UserSessionEntity> wrappedEntity = null;
            if (!((Objects.equals(cache.getName(), USER_SESSION_CACHE_NAME) || Objects.equals(cache.getName(), CLIENT_SESSION_CACHE_NAME) || Objects.equals(cache.getName(), OFFLINE_USER_SESSION_CACHE_NAME) || Objects.equals(cache.getName(), OFFLINE_CLIENT_SESSION_CACHE_NAME)) && Profile.isFeatureEnabled(Profile.Feature.PERSISTENT_USER_SESSIONS_NO_CACHE))) {
                wrappedEntity = cache.get(key);
            }
            if (wrappedEntity == null) {
                wrappedEntity = getSessionEntityFromPersister(realm, key);
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
            UserSessionEntity entity = myUpdates.getEntityWrapper().getEntity();

            // If entity is scheduled for remove, we don't return it.
            boolean scheduledForRemove = myUpdates.getUpdateTasks().stream().filter((SessionUpdateTask task) -> {

                return task.getOperation(entity) == SessionUpdateTask.CacheOperation.REMOVE;

            }).findFirst().isPresent();

            return scheduledForRemove ? null : myUpdates.getEntityWrapper();
        }
    }

    public SessionEntityWrapper<UserSessionEntity> getSessionEntityFromPersister(RealmModel realm, String key) {
        UserSessionPersisterProvider persister = kcSession.getProvider(UserSessionPersisterProvider.class);
        UserSessionModel persistentUserSession = persister.loadUserSession(realm, key, offline);

        if (persistentUserSession == null) {
            return null;
        }

        SessionEntityWrapper<UserSessionEntity> userSessionEntitySessionEntityWrapper = importUserSession(persistentUserSession);
        if (userSessionEntitySessionEntityWrapper == null) {
            removeSessionEntityFromPersister(key);
        }

        return userSessionEntitySessionEntityWrapper;
    }

    private void removeSessionEntityFromPersister(String key) {
        UserSessionPersisterProvider persister = kcSession.getProvider(UserSessionPersisterProvider.class);
        persister.removeUserSession(key, offline);
    }

    private SessionEntityWrapper<UserSessionEntity> importUserSession(UserSessionModel persistentUserSession) {
        String sessionId = persistentUserSession.getId();

        if (isScheduledForRemove(sessionId)) {
            return null;
        }

        if (Profile.isFeatureEnabled(Profile.Feature.PERSISTENT_USER_SESSIONS_NO_CACHE) && (cache.getName().equals(USER_SESSION_CACHE_NAME) || cache.getName().equals(CLIENT_SESSION_CACHE_NAME) || cache.getName().equals(OFFLINE_USER_SESSION_CACHE_NAME) || cache.getName().equals(OFFLINE_CLIENT_SESSION_CACHE_NAME))) {
            return ((PersistentUserSessionProvider) kcSession.getProvider(UserSessionProvider.class)).wrapPersistentEntity(persistentUserSession.getRealm(), offline, persistentUserSession);
        }

        LOG.debugf("Attempting to import user-session for sessionId=%s offline=%s", sessionId, offline);
        kcSession.sessions().importUserSessions(Collections.singleton(persistentUserSession), offline);
        LOG.debugf("user-session imported, trying another lookup for sessionId=%s offline=%s", sessionId, offline);

        SessionEntityWrapper<UserSessionEntity> ispnUserSessionEntity = cache.get(sessionId);

        if (ispnUserSessionEntity != null) {
            LOG.debugf("user-session found after import for sessionId=%s offline=%s", sessionId, offline);
            return ispnUserSessionEntity;
        }

        LOG.debugf("user-session could not be found after import for sessionId=%s offline=%s", sessionId, offline);
        return null;
    }
    public boolean isScheduledForRemove(String key) {
        return isScheduledForRemove(updates.get(key));
    }

    private static <V extends SessionEntity> boolean isScheduledForRemove(SessionUpdatesList<V> myUpdates) {
        if (myUpdates == null) {
            return false;
        }

        V entity = myUpdates.getEntityWrapper().getEntity();

        // If entity is scheduled for remove, we don't return it.
        boolean scheduledForRemove = myUpdates.getUpdateTasks()
                .stream()
                .anyMatch(task -> task.getOperation(entity) == SessionUpdateTask.CacheOperation.REMOVE);

        return scheduledForRemove;
    }

}
