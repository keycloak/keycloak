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
import org.keycloak.common.Profile;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.sessions.infinispan.SessionFunction;
import org.keycloak.models.sessions.infinispan.entities.SessionEntity;
import org.keycloak.models.sessions.infinispan.remotestore.RemoteCacheInvoker;

import java.util.List;
import java.util.Map;

import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.CLIENT_SESSION_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.OFFLINE_CLIENT_SESSION_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.OFFLINE_USER_SESSION_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.USER_SESSION_CACHE_NAME;

public class PersistentSessionsChangelogBasedTransaction<K, V extends SessionEntity> extends InfinispanChangelogBasedTransaction<K, V> {

    private final List<SessionChangesPerformer<K, V>> changesPerformers;
    protected final boolean offline;

    public PersistentSessionsChangelogBasedTransaction(KeycloakSession session, Cache<K, SessionEntityWrapper<V>> cache, RemoteCacheInvoker remoteCacheInvoker, SessionFunction<V> lifespanMsLoader, SessionFunction<V> maxIdleTimeMsLoader, boolean offline) {
        super(session, cache, remoteCacheInvoker, lifespanMsLoader, maxIdleTimeMsLoader);
        this.offline = offline;

        if (!Profile.isFeatureEnabled(Profile.Feature.PERSISTENT_USER_SESSIONS)) {
            throw new IllegalStateException("Persistent user sessions are not enabled");
        }

        if (Profile.isFeatureEnabled(Profile.Feature.PERSISTENT_USER_SESSIONS_NO_CACHE) &&
            (cache.getName().equals(USER_SESSION_CACHE_NAME) || cache.getName().equals(CLIENT_SESSION_CACHE_NAME) || cache.getName().equals(OFFLINE_USER_SESSION_CACHE_NAME) || cache.getName().equals(OFFLINE_CLIENT_SESSION_CACHE_NAME))) {
            changesPerformers = List.of(
                    new JpaChangesPerformer<>(session, cache.getName(), offline)
            );
        } else {
            changesPerformers = List.of(
                    new JpaChangesPerformer<>(session, cache.getName(), offline),
                    new EmbeddedCachesChangesPerformer<>(cache),
                    new RemoteCachesChangesPerformer<>(session, cache, remoteCacheInvoker)
            );
        }
    }

    @Override
    protected void commitImpl() {
        for (Map.Entry<K, SessionUpdatesList<V>> entry : updates.entrySet()) {
            SessionUpdatesList<V> sessionUpdates = entry.getValue();
            SessionEntityWrapper<V> sessionWrapper = sessionUpdates.getEntityWrapper();

            // Don't save transient entities to infinispan. They are valid just for current transaction
            if (sessionUpdates.getPersistenceState() == UserSessionModel.SessionPersistenceState.TRANSIENT) continue;

            RealmModel realm = sessionUpdates.getRealm();

            long lifespanMs = lifespanMsLoader.apply(realm, sessionUpdates.getClient(), sessionWrapper.getEntity());
            long maxIdleTimeMs = maxIdleTimeMsLoader.apply(realm, sessionUpdates.getClient(), sessionWrapper.getEntity());

            MergedUpdate<V> merged = MergedUpdate.computeUpdate(sessionUpdates.getUpdateTasks(), sessionWrapper, lifespanMs, maxIdleTimeMs);

            if (merged != null) {
                changesPerformers.forEach(p -> p.registerChange(entry, merged));
            }
        }

        changesPerformers.forEach(SessionChangesPerformer::applyChanges);
    }

    @Override
    protected void rollbackImpl() {

    }
}
