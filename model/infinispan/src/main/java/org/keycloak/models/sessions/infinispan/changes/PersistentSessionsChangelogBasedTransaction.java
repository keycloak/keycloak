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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.sessions.infinispan.SessionFunction;
import org.keycloak.models.sessions.infinispan.entities.SessionEntity;
import org.keycloak.models.sessions.infinispan.transaction.DatabaseUpdate;
import org.keycloak.models.sessions.infinispan.transaction.NonBlockingTransaction;
import org.keycloak.models.sessions.infinispan.util.SessionTimeouts;

import org.infinispan.Cache;
import org.infinispan.commons.util.concurrent.AggregateCompletionStage;
import org.infinispan.commons.util.concurrent.CompletionStages;
import org.jboss.logging.Logger;

abstract public class PersistentSessionsChangelogBasedTransaction<K, V extends SessionEntity> implements SessionsChangelogBasedTransaction<K, V>, NonBlockingTransaction {

    private static final Logger LOG = Logger.getLogger(PersistentSessionsChangelogBasedTransaction.class);
    protected final KeycloakSession kcSession;
    protected final Map<K, SessionUpdatesList<V>> updates = new HashMap<>();
    protected final Map<K, SessionUpdatesList<V>> offlineUpdates = new HashMap<>();
    private final String cacheName;
    private final ArrayBlockingQueue<PersistentUpdate> batchingQueue;
    private final CacheHolder<K, V> cacheHolder;
    private final CacheHolder<K, V> offlineCacheHolder;

    public PersistentSessionsChangelogBasedTransaction(KeycloakSession session,
                                                       String cacheName,
                                                       ArrayBlockingQueue<PersistentUpdate> batchingQueue,
                                                       CacheHolder<K, V> cacheHolder,
                                                       CacheHolder<K, V> offlineCacheHolder) {
        kcSession = session;
        this.cacheName = cacheName;
        this.batchingQueue = batchingQueue;
        this.cacheHolder = cacheHolder;
        this.offlineCacheHolder = offlineCacheHolder;
    }

    public Cache<K, SessionEntityWrapper<V>> getCache(boolean offline) {
        return offline ? offlineCacheHolder.cache() : cacheHolder.cache();
    }

    protected SessionFunction<V> getLifespanMsLoader(boolean offline) {
        return offline ? offlineCacheHolder.lifespanFunction() : cacheHolder.lifespanFunction();
    }

    protected SessionFunction<V> getMaxIdleMsLoader(boolean offline) {
        return offline ? offlineCacheHolder.maxIdleFunction() : cacheHolder.maxIdleFunction();
    }

    protected Map<K, SessionUpdatesList<V>> getUpdates(boolean offline) {
        return offline ? offlineUpdates : updates;
    }

    public K generateKey() {
        assert cacheHolder.keyGenerator() != null;
        return cacheHolder.keyGenerator().get();
    }

    public SessionEntityWrapper<V> get(K key, boolean offline) {
        SessionUpdatesList<V> myUpdates = getUpdates(offline).get(key);
        if (myUpdates == null) {
            SessionEntityWrapper<V> wrappedEntity = getCache(offline).get(key);
            if (wrappedEntity == null) {
                return null;
            }
            wrappedEntity.getEntity().setOffline(offline);

            RealmModel realm = kcSession.realms().getRealm(wrappedEntity.getEntity().getRealmId());

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

    @Override
    public void asyncCommit(AggregateCompletionStage<Void> stage, Consumer<DatabaseUpdate> databaseUpdates) {
        JpaChangesPerformer<K, V> persister = null;
        for (Map.Entry<K, SessionUpdatesList<V>> entry : Stream.concat(updates.entrySet().stream(), offlineUpdates.entrySet().stream()).toList()) {
            SessionUpdatesList<V> sessionUpdates = entry.getValue();
            if (sessionUpdates.getUpdateTasks().isEmpty()) {
                continue;
            }
            SessionEntityWrapper<V> sessionWrapper = sessionUpdates.getEntityWrapper();
            V entity = sessionWrapper.getEntity();
            boolean isOffline = entity.isOffline();

            // Don't save transient entities to infinispan. They are valid just for current transaction
            if (sessionUpdates.getPersistenceState() == UserSessionModel.SessionPersistenceState.TRANSIENT) continue;

            RealmModel realm = sessionUpdates.getRealm();

            long lifespanMs = getLifespanMsLoader(isOffline).apply(realm, sessionUpdates.getClient(), entity);
            long maxIdleTimeMs = getMaxIdleMsLoader(isOffline).apply(realm, sessionUpdates.getClient(), entity);

            MergedUpdate<V> merged = MergedUpdate.computeUpdate(sessionUpdates.getUpdateTasks(), sessionWrapper, lifespanMs, maxIdleTimeMs);

            if (merged != null) {
                var c = isOffline ? offlineCacheHolder : cacheHolder;
                if (c.cache() != null) {
                    // Update cache. It is non-blocking.
                    InfinispanChangesUtils.runOperationInCluster(c, entry.getKey(), merged, entry.getValue().getEntityWrapper(), stage, LOG);
                }

                if (persister == null) {
                    persister =new JpaChangesPerformer<>(cacheName, batchingQueue);
                    if (!persister.isNonBlocking()) {
                        databaseUpdates.accept(persister::write);
                    }
                }
                if (persister.isNonBlocking()) {
                    // batching enabled, another thread will commit the changes.
                    persister.asyncWrite(stage, entry, merged);
                } else {
                    // batching disabled, we queue, and we will execute the update later.
                    persister.registerChange(entry, merged);
                }
            }
        }
    }

    @Override
    public void asyncRollback(AggregateCompletionStage<Void> stage) {
        updates.clear();
        offlineUpdates.clear();
    }

    @Override
    public void addTask(K key, SessionUpdateTask<V> originalTask) {
        if (!(originalTask instanceof PersistentSessionUpdateTask<V> task)) {
            throw new IllegalArgumentException("Task must be instance of PersistentSessionUpdateTask");
        }

        SessionUpdatesList<V> myUpdates = getUpdates(task.isOffline()).get(key);
        if (myUpdates != null) {
            myUpdates.addAndExecute(task);
            return;
        }
        lookupAndAndExecuteTask(key, task);
    }

    @Override
    public void restartEntity(K key, SessionUpdateTask<V> restartTask) {
        if (!(restartTask instanceof PersistentSessionUpdateTask<V> task)) {
            throw new IllegalArgumentException("Task must be instance of PersistentSessionUpdateTask");
        }
        var myUpdates = getUpdates(task.isOffline()).get(key);
        if (myUpdates != null) {
            myUpdates.getUpdateTasks().clear();
            myUpdates.addAndExecute(task);
            return;
        }
        lookupAndAndExecuteTask(key, task);
    }

    private void lookupAndAndExecuteTask(K key, PersistentSessionUpdateTask<V> task) {
        // Lookup entity from cache
        SessionEntityWrapper<V> wrappedEntity = getCache(task.isOffline()).get(key);
        if (wrappedEntity == null) {
            LOG.tracef("Not present cache item for key %s", key);
            return;
        }
        // Cache does not contain the offline flag value so adding it
        wrappedEntity.getEntity().setOffline(task.isOffline());

        RealmModel realm = kcSession.realms().getRealm(wrappedEntity.getEntity().getRealmId());

        SessionUpdatesList<V> myUpdates = new SessionUpdatesList<>(realm, wrappedEntity);
        getUpdates(task.isOffline()).put(key, myUpdates);

        // Run the update now, so reader in same transaction can see it (TODO: Rollback may not work correctly. See if it's an issue..)
        myUpdates.addAndExecute(task);
    }

    public void addTask(K key, SessionUpdateTask<V> task, V entity, UserSessionModel.SessionPersistenceState persistenceState) {
        if (entity == null) {
            throw new IllegalArgumentException("Null entity not allowed");
        }

        RealmModel realm = kcSession.realms().getRealm(entity.getRealmId());
        SessionEntityWrapper<V> wrappedEntity = new SessionEntityWrapper<>(entity);
        SessionUpdatesList<V> myUpdates = new SessionUpdatesList<>(realm, wrappedEntity, persistenceState);
        getUpdates(entity.isOffline()).put(key, myUpdates);

        if (task != null) {
            // Run the update now, so reader in same transaction can see it
            myUpdates.addAndExecute(task);
        }
    }

    // method not currently in use, remove in the next major.
    @Deprecated(forRemoval = true, since = "26.4")
    public void reloadEntityInCurrentTransaction(RealmModel realm, K key, SessionEntityWrapper<V> entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Null entity not allowed");
        }
        boolean offline = entity.getEntity().isOffline();

        SessionEntityWrapper<V> latestEntity = getCache(offline).get(key);
        if (latestEntity == null) {
            return;
        }

        SessionUpdatesList<V> newUpdates = new SessionUpdatesList<>(realm, latestEntity);

        SessionUpdatesList<V> existingUpdates = getUpdates(entity.getEntity().isOffline()).get(key);
        if (existingUpdates != null) {
            newUpdates.setUpdateTasks(existingUpdates.getUpdateTasks());
        }

        getUpdates(entity.getEntity().isOffline()).put(key, newUpdates);
    }

    /**
     * Imports a session from an external source into the {@link Cache}.
     * <p>
     * If a session already exists in the cache, this method does not insert the {@code session}. The invoker should use
     * the session returned by this method invocation. When the session is successfully imported, this method returns
     * null and the {@code session} can be used by the transaction.
     * <p>
     * This transaction will keep track of further changes in the session.
     *
     * @param realmModel The {@link RealmModel} where the session belong to.
     * @param key        The cache's key.
     * @param session    The session to import.
     * @param lifespan   How long the session stays cached until it is expired and removed.
     * @param maxIdle    How long the session can be idle (without reading or writing) before being removed.
     * @param offline    {@code true} if it is an offline session.
     * @return The existing cached session. If it returns {@code null}, it means the {@code session} used in the
     * parameters was cached.
     */
    public SessionEntityWrapper<V> importSession(RealmModel realmModel, K key, SessionEntityWrapper<V> session, boolean offline, long lifespan, long maxIdle) {
        var updates = getUpdates(offline);
        var updatesList = updates.get(key);
        if (updatesList != null) {
            // exists in transaction, avoid import operation
            return updatesList.getEntityWrapper();
        }
        SessionEntityWrapper<V> existing = null;
        try {
            if (getCache(offline) != null) {
                existing = getCache(offline).putIfAbsent(key, session, lifespan, TimeUnit.MILLISECONDS, maxIdle, TimeUnit.MILLISECONDS);
            }
        } catch (RuntimeException exception) {
            // If the import fails, the transaction can continue with the data from the database.
            LOG.debugf(exception, "Failed to import session %s", session);
        }
        if (existing == null) {
            // keep track of the imported session for updates
            updates.put(key, new SessionUpdatesList<>(realmModel, session));
            return null;
        }
        updates.put(key, new SessionUpdatesList<>(realmModel, existing));
        return existing;
    }

    /**
     * Imports multiple sessions from an external source into the {@link Cache}.
     * <p>
     * If one or more sessions already exist in the {@link Cache}, or is expired, it will not be imported.
     * <p>
     * This transaction will keep track of further changes in the sessions.
     *
     * @param realmModel The {@link RealmModel} where the sessions belong to.
     * @param sessions   The {@link Map} with the cache's key/session mapping to be imported.
     * @param offline    {@code true} if it is an offline session.
     */
    public void importSessionsConcurrently(RealmModel realmModel, Map<K, SessionEntityWrapper<V>> sessions, boolean offline) {
        var cache = getCache(offline);
        if (sessions.isEmpty() || cache == null) {
            //nothing to import
            return;
        }
        var stage = CompletionStages.aggregateCompletionStage();
        var allSessions = new ConcurrentHashMap<K, SessionEntityWrapper<V>>();
        var updates = getUpdates(offline);
        var lifespanFunction = getLifespanMsLoader(offline);
        var maxIdleFunction = getMaxIdleMsLoader(offline);
        sessions.forEach((key, session) -> {
            if (updates.containsKey(key)) {
                //nothing to import, already exists in transaction
                return;
            }
            var clientModel = session.getClientIfNeeded(realmModel);
            var sessionEntity = session.getEntity();
            var lifespan = lifespanFunction.apply(realmModel, clientModel, sessionEntity);
            var maxIdle = maxIdleFunction.apply(realmModel, clientModel, sessionEntity);
            if (lifespan == SessionTimeouts.ENTRY_EXPIRED_FLAG || maxIdle == SessionTimeouts.ENTRY_EXPIRED_FLAG) {
                //nothing to import, already expired
                return;
            }
            var future = cache.putIfAbsentAsync(key, session, lifespan, TimeUnit.MILLISECONDS, maxIdle, TimeUnit.MILLISECONDS)
                    .exceptionally(throwable -> {
                        // If the import fails, the transaction can continue with the data from the database.
                        LOG.debugf(throwable, "Failed to import session %s", session);
                        return null;
                    });
            // write result into concurrent hash map because the consumer is invoked in a different thread each time.
            stage.dependsOn(future.thenAccept(existing -> allSessions.put(key, existing == null ? session : existing)));
        });

        CompletionStages.join(stage.freeze());
        allSessions.forEach((key, wrapper) -> updates.put(key, new SessionUpdatesList<>(realmModel, wrapper)));
    }
}
