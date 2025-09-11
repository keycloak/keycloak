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
import org.infinispan.commons.util.concurrent.CompletionStages;
import org.jboss.logging.Logger;
import org.keycloak.common.util.Retry;
import org.keycloak.models.AbstractKeycloakTransaction;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.sessions.infinispan.SessionFunction;
import org.keycloak.models.sessions.infinispan.entities.SessionEntity;
import org.keycloak.models.sessions.infinispan.util.SessionTimeouts;
import org.keycloak.models.utils.KeycloakModelUtils;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

abstract public class PersistentSessionsChangelogBasedTransaction<K, V extends SessionEntity> extends AbstractKeycloakTransaction implements SessionsChangelogBasedTransaction<K, V> {

    private static final Logger LOG = Logger.getLogger(PersistentSessionsChangelogBasedTransaction.class);
    protected final KeycloakSession kcSession;
    protected final Map<K, SessionUpdatesList<V>> updates = new HashMap<>();
    protected final Map<K, SessionUpdatesList<V>> offlineUpdates = new HashMap<>();
    private final String cacheName;
    private final Cache<K, SessionEntityWrapper<V>> cache;
    private final Cache<K, SessionEntityWrapper<V>> offlineCache;
    private final SessionFunction<V> lifespanMsLoader;
    private final SessionFunction<V> maxIdleTimeMsLoader;
    private final SessionFunction<V> offlineLifespanMsLoader;
    private final SessionFunction<V> offlineMaxIdleTimeMsLoader;
    private final ArrayBlockingQueue<PersistentUpdate> batchingQueue;
    private final SerializeExecutionsByKey<K> serializerOnline;
    private final SerializeExecutionsByKey<K> serializerOffline;

    public PersistentSessionsChangelogBasedTransaction(KeycloakSession session,
                                                       String cacheName,
                                                       Cache<K, SessionEntityWrapper<V>> cache,
                                                       Cache<K, SessionEntityWrapper<V>> offlineCache,
                                                       SessionFunction<V> lifespanMsLoader,
                                                       SessionFunction<V> maxIdleTimeMsLoader,
                                                       SessionFunction<V> offlineLifespanMsLoader,
                                                       SessionFunction<V> offlineMaxIdleTimeMsLoader,
                                                       ArrayBlockingQueue<PersistentUpdate> batchingQueue,
                                                       SerializeExecutionsByKey<K> serializerOnline,
                                                       SerializeExecutionsByKey<K> serializerOffline) {
        kcSession = session;
        this.cacheName = cacheName;
        this.cache = cache;
        this.offlineCache = offlineCache;
        this.lifespanMsLoader = lifespanMsLoader;
        this.maxIdleTimeMsLoader = maxIdleTimeMsLoader;
        this.offlineLifespanMsLoader = offlineLifespanMsLoader;
        this.offlineMaxIdleTimeMsLoader = offlineMaxIdleTimeMsLoader;
        this.batchingQueue = batchingQueue;
        this.serializerOnline = serializerOnline;
        this.serializerOffline = serializerOffline;
    }

    protected Cache<K, SessionEntityWrapper<V>> getCache(boolean offline) {
        if (offline) {
            return offlineCache;
        } else {
            return cache;
        }
    }

    protected SessionFunction<V> getLifespanMsLoader(boolean offline) {
        if (offline) {
            return offlineLifespanMsLoader;
        } else {
            return lifespanMsLoader;
        }
    }

    protected SessionFunction<V> getMaxIdleMsLoader(boolean offline) {
        if (offline) {
            return offlineMaxIdleTimeMsLoader;
        } else {
            return maxIdleTimeMsLoader;
        }
    }

    protected Map<K, SessionUpdatesList<V>> getUpdates(boolean offline) {
        if (offline) {
            return offlineUpdates;
        } else {
            return updates;
        }
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

    List<SessionChangesPerformer<K, V>> prepareChangesPerformers() {
        List<SessionChangesPerformer<K, V>> changesPerformers = new LinkedList<>();

        if (batchingQueue != null) {
            changesPerformers.add(new JpaChangesPerformer<>(cacheName, batchingQueue));
        } else {
            changesPerformers.add(new JpaChangesPerformer<>(cacheName, null) {
                @Override
                public void applyChanges() {
                    Retry.executeWithBackoff(
                            iteration -> KeycloakModelUtils.runJobInTransaction(kcSession.getKeycloakSessionFactory(), super::applyChangesSynchronously),
                            (iteration, t) -> {
                                if (iteration > 20) {
                                    // Never retry more than 20 times.
                                    throw new RuntimeException("Maximum number of retries reached", t);
                                }
                            }, PersistentSessionsWorker.UPDATE_TIMEOUT, PersistentSessionsWorker.UPDATE_BASE_INTERVAL_MILLIS);
                    clear();
                }
            });
        }

        if (cache != null) {
            changesPerformers.add(new EmbeddedCachesChangesPerformer<>(cache, serializerOnline) {
                @Override
                public boolean shouldConsumeChange(V entity) {
                    return !entity.isOffline();
                }
            });
        }

        if (offlineCache != null) {
            changesPerformers.add(new EmbeddedCachesChangesPerformer<>(offlineCache, serializerOffline) {
                @Override
                public boolean shouldConsumeChange(V entity) {
                    return entity.isOffline();
                }
            });
        }

        return changesPerformers;
    }

    @Override
    protected void commitImpl() {
        List<SessionChangesPerformer<K, V>> changesPerformers = null;
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
                if (changesPerformers == null) {
                    changesPerformers = prepareChangesPerformers();
                }
                changesPerformers.stream()
                        .filter(performer -> performer.shouldConsumeChange(entity))
                        .forEach(p -> p.registerChange(entry, merged));
            }
        }

        if (changesPerformers != null) {
            changesPerformers.forEach(SessionChangesPerformer::applyChanges);
        }
    }

    @Override
    public void addTask(K key, SessionUpdateTask<V> originalTask) {
        if (!(originalTask instanceof PersistentSessionUpdateTask<V> task)) {
            throw new IllegalArgumentException("Task must be instance of PersistentSessionUpdateTask");
        }

        SessionUpdatesList<V> myUpdates = getUpdates(task.isOffline()).get(key);
        if (myUpdates == null) {
            // Lookup entity from cache
            SessionEntityWrapper<V> wrappedEntity = getCache(task.isOffline()).get(key);
            if (wrappedEntity == null) {
                LOG.tracef("Not present cache item for key %s", key);
                return;
            }
            // Cache does not contain the offline flag value so adding it
            wrappedEntity.getEntity().setOffline(task.isOffline());

            RealmModel realm = kcSession.realms().getRealm(wrappedEntity.getEntity().getRealmId());

            myUpdates = new SessionUpdatesList<>(realm, wrappedEntity);
            getUpdates(task.isOffline()).put(key, myUpdates);
        }

        // Run the update now, so reader in same transaction can see it (TODO: Rollback may not work correctly. See if it's an issue..)
        task.runUpdate(myUpdates.getEntityWrapper().getEntity());
        myUpdates.add(task);
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
            task.runUpdate(entity);
            myUpdates.add(task);
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

    @Override
    protected void rollbackImpl() {

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
