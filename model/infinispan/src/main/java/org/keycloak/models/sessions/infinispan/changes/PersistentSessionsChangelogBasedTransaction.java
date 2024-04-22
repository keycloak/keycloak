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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;

import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.CLIENT_SESSION_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.OFFLINE_CLIENT_SESSION_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.OFFLINE_USER_SESSION_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.USER_SESSION_CACHE_NAME;

public class PersistentSessionsChangelogBasedTransaction<K, V extends SessionEntity> extends InfinispanChangelogBasedTransaction<K, V> {

    private final List<SessionChangesPerformer<K, V>> changesPerformers;
    protected final boolean offline;
    private final ArrayBlockingQueue<PersistentDeferredElement<K, V>> asyncQueue;
    private final boolean batchAllWrites;
    private Collection<PersistentDeferredElement<K, V>> batch;

    public PersistentSessionsChangelogBasedTransaction(KeycloakSession session, Cache<K, SessionEntityWrapper<V>> cache, RemoteCacheInvoker remoteCacheInvoker, SessionFunction<V> lifespanMsLoader, SessionFunction<V> maxIdleTimeMsLoader, boolean offline, SerializeExecutionsByKey<K> serializer, ArrayBlockingQueue<PersistentDeferredElement<K, V>> asyncQueue, boolean batchAllWrites) {
        super(session, cache, remoteCacheInvoker, lifespanMsLoader, maxIdleTimeMsLoader, serializer);
        this.offline = offline;
        this.asyncQueue = asyncQueue;
        this.batchAllWrites = batchAllWrites;

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
                    new EmbeddedCachesChangesPerformer<>(cache, serializer),
                    new RemoteCachesChangesPerformer<>(session, cache, remoteCacheInvoker)
            );
        }
    }

    @Override
    protected void commitImpl() {
        List<CompletableFuture<Void>> futures = new ArrayList<>(updates.size());
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
                if (batchAllWrites) {
                    // We will batch the inserts and updates and deletes (although deletes have shown deadlocks if there are concurrent updates).
                    // We'll also wait for all deferrable items, as doing so will reduce the likelihood of concurrent requests later
                    // which might lead to deadlocks.
                    // Important to memorize the future first before adding it to the queue,
                    // as the future will only be created only when necessary.
                    futures.add(merged.result());
                    addEntryToQueue(entry, merged);
                } else if (merged.isDeferrable()) {
                    // This is deferrable, no need to memorize the future
                    addEntryToQueue(entry, merged);
                } else {
                    changesPerformers.forEach(p -> p.registerChange(entry, merged));
                }
            }
        }

        if (batch != null) {
            batch.forEach(o -> {
                changesPerformers.forEach(p -> p.registerChange(o.getEntry(), o.getMerged()));
            });
        }

        changesPerformers.forEach(SessionChangesPerformer::applyChanges);

        // If we enqueued any items that we wait for in the background tasks, this is now the moment
        if (!futures.isEmpty()) {
            List<Throwable> exceptions = new ArrayList<>();
            CompletableFuture.allOf(futures.stream().map(f -> f.exceptionally(throwable -> {
                exceptions.add(throwable);
                return null;
            })).toArray(CompletableFuture[]::new)).join();
            // If any of those futures has failed, add the exceptions as suppressed exceptions to our runtime exception
            if (!exceptions.isEmpty()) {
                RuntimeException ex = new RuntimeException("unable to complete the session updates");
                exceptions.forEach(ex::addSuppressed);
                throw ex;
            }
        }
    }

    private void addEntryToQueue(Map.Entry<K, SessionUpdatesList<V>> entry, MergedUpdate<V> merged) {
        if (!asyncQueue.offer(new PersistentDeferredElement<>(entry, merged))) {
            logger.warnf("Queue for cache %s is full, will block", cache.getName());
            try {
                // this will block until there is a free spot in the queue
                asyncQueue.put(new PersistentDeferredElement<>(entry, merged));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    protected void rollbackImpl() {

    }

    public void applyDeferredBatch(Collection<PersistentDeferredElement<K, V>> batchToApply) {
        if (this.batch == null) {
            this.batch = new ArrayList<>(batchToApply.size());
        }
        batch.addAll(batchToApply);
    }

}
