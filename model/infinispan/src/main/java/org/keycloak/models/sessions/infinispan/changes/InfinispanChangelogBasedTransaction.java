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

package org.keycloak.models.sessions.infinispan.changes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

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

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class InfinispanChangelogBasedTransaction<K, V extends SessionEntity> implements SessionsChangelogBasedTransaction<K, V>, NonBlockingTransaction {

    public static final Logger logger = Logger.getLogger(InfinispanChangelogBasedTransaction.class);

    protected final KeycloakSession kcSession;
    protected final Map<K, SessionUpdatesList<V>> updates = new HashMap<>();
    protected final CacheHolder<K, V> cacheHolder;

    public InfinispanChangelogBasedTransaction(KeycloakSession kcSession, CacheHolder<K, V> cacheHolder) {
        this.kcSession = kcSession;
        this.cacheHolder = cacheHolder;
    }


    @Override
    public void addTask(K key, SessionUpdateTask<V> task) {
        SessionUpdatesList<V> myUpdates = updates.get(key);
        if (myUpdates != null) {
            myUpdates.addAndExecute(task);
            return;
        }
        lookupAndAndExecuteTask(key, task);
    }

    @Override
    public void restartEntity(K key, SessionUpdateTask<V> restartTask) {
        SessionUpdatesList<V> myUpdates = updates.get(key);
        if (myUpdates != null) {
            myUpdates.getUpdateTasks().clear();
            myUpdates.addAndExecute(restartTask);
            return;
        }
        lookupAndAndExecuteTask(key, restartTask);
    }


    // Create entity and new version for it
    public void addTask(K key, SessionUpdateTask<V> task, V entity, UserSessionModel.SessionPersistenceState persistenceState) {
        if (entity == null) {
            throw new IllegalArgumentException("Null entity not allowed");
        }

        RealmModel realm = kcSession.realms().getRealm(entity.getRealmId());
        SessionEntityWrapper<V> wrappedEntity = new SessionEntityWrapper<>(entity);
        SessionUpdatesList<V> myUpdates = new SessionUpdatesList<>(realm, wrappedEntity, persistenceState);
        updates.put(key, myUpdates);

        if (task != null) {
            // Run the update now, so reader in same transaction can see it
            myUpdates.addAndExecute(task);
        }
    }

    @Deprecated(since = "26.4", forRemoval = true)
    //unused method
    public void reloadEntityInCurrentTransaction(RealmModel realm, K key, SessionEntityWrapper<V> entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Null entity not allowed");
        }

        SessionEntityWrapper<V> latestEntity = cacheHolder.cache().get(key);
        if (latestEntity == null) {
            return;
        }

        SessionUpdatesList<V> newUpdates = new SessionUpdatesList<>(realm, latestEntity);

        SessionUpdatesList<V> existingUpdates = updates.get(key);
        if (existingUpdates != null) {
            newUpdates.setUpdateTasks(existingUpdates.getUpdateTasks());
        }

        updates.put(key, newUpdates);
    }


    public SessionEntityWrapper<V> get(K key) {
        SessionUpdatesList<V> myUpdates = updates.get(key);
        if (myUpdates == null) {
            SessionEntityWrapper<V> wrappedEntity = cacheHolder.cache().get(key);
            if (wrappedEntity == null) {
                return null;
            }

            RealmModel realm = kcSession.realms().getRealm(wrappedEntity.getEntity().getRealmId());

            myUpdates = new SessionUpdatesList<>(realm, wrappedEntity);
            updates.put(key, myUpdates);

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
        for (Map.Entry<K, SessionUpdatesList<V>> entry : updates.entrySet()) {
            SessionUpdatesList<V> sessionUpdates = entry.getValue();
            SessionEntityWrapper<V> sessionWrapper = sessionUpdates.getEntityWrapper();
            List<SessionUpdateTask<V>> updateTasks = sessionUpdates.getUpdateTasks();

            if (updateTasks.isEmpty()) {
                // no changes tracked, moving on.
                continue;
            }

            // Don't save transient entities to infinispan. They are valid just for current transaction
            if (sessionUpdates.getPersistenceState() == UserSessionModel.SessionPersistenceState.TRANSIENT) continue;

            // Don't save entities in infinispan that are both added and removed within the same transaction.
            if (updateTasks.get(0).getOperation().equals(SessionUpdateTask.CacheOperation.ADD_IF_ABSENT)
                    && updateTasks.get(updateTasks.size() - 1).getOperation().equals(SessionUpdateTask.CacheOperation.REMOVE)) {
                continue;
            }

            RealmModel realm = sessionUpdates.getRealm();

            long lifespanMs = cacheHolder.lifespanFunction().apply(realm, sessionUpdates.getClient(), sessionWrapper.getEntity());
            long maxIdleTimeMs = cacheHolder.maxIdleFunction().apply(realm, sessionUpdates.getClient(), sessionWrapper.getEntity());

            MergedUpdate<V> merged = MergedUpdate.computeUpdate(updateTasks, sessionWrapper, lifespanMs, maxIdleTimeMs);

            if (merged != null) {
                // Now run the operation in our cluster
                InfinispanChangesUtils.runOperationInCluster(cacheHolder, entry.getKey(), merged, sessionWrapper, stage, logger);
            }
        }
    }

    @Override
    public void asyncRollback(AggregateCompletionStage<Void> stage) {
        updates.clear();
    }

    /**
     * @return The {@link Cache} backing up this transaction.
     */
    public Cache<K, SessionEntityWrapper<V>> getCache() {
        return cacheHolder.cache();
    }

    public K generateKey() {
        assert cacheHolder.keyGenerator() != null;
        return cacheHolder.keyGenerator().get();
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
     * @return The existing cached session. If it returns {@code null}, it means the {@code session} used in the
     * parameters was cached.
     */
    public V importSession(RealmModel realmModel, K key, SessionEntityWrapper<V> session, long lifespan, long maxIdle) {
        SessionUpdatesList<V> updatesList = updates.get(key);
        if (updatesList != null) {
            // exists in transaction, avoid cache operation
            return updatesList.getEntityWrapper().getEntity();
        }
        SessionEntityWrapper<V> existing = cacheHolder.cache().putIfAbsent(key, session, lifespan, TimeUnit.MILLISECONDS, maxIdle, TimeUnit.MILLISECONDS);
        if (existing == null) {
            // keep track of the imported session for updates
            updates.put(key, new SessionUpdatesList<>(realmModel, session));
            return null;
        }
        updates.put(key, new SessionUpdatesList<>(realmModel, existing));
        return existing.getEntity();
    }

    /**
     * Imports multiple sessions from an external source into the {@link Cache}.
     * <p>
     * If the {@code lifespanFunction} or {@code maxIdleFunction} returns {@link SessionTimeouts#ENTRY_EXPIRED_FLAG},
     * the session is considered expired and not stored in the cache.
     * <p>
     * Also, if one or more sessions already exist in the {@link Cache}, it will not be imported.
     * <p>
     * This transaction will keep track of further changes in the sessions.
     *
     * @param realmModel       The {@link RealmModel} where the sessions belong to.
     * @param sessions         The {@link Map} with the cache's key/session mapping to be imported.
     * @param lifespanFunction The {@link java.util.function.Function} to compute the lifespan of the session. It
     *                         defines how long the session should be stored in the cache until it is removed.
     * @param maxIdleFunction  The {@link java.util.function.Function} to compute the max-idle of the session. It
     *                         defines how long the session will be idle before it is removed.
     */
    public void importSessionsConcurrently(RealmModel realmModel, Map<K, SessionEntityWrapper<V>> sessions, SessionFunction<V> lifespanFunction, SessionFunction<V> maxIdleFunction) {
        if (sessions.isEmpty()) {
            //nothing to import
            return;
        }
        var stage = CompletionStages.aggregateCompletionStage();
        var allSessions = new ConcurrentHashMap<K, SessionEntityWrapper<V>>();
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
            var future = cacheHolder.cache().putIfAbsentAsync(key, session, lifespan, TimeUnit.MILLISECONDS, maxIdle, TimeUnit.MILLISECONDS);
            // write result into concurrent hash map because the consumer is invoked in a different thread each time.
            stage.dependsOn(future.thenAccept(existing -> allSessions.put(key, existing == null ? session : existing)));
        });

        CompletionStages.join(stage.freeze());
        allSessions.forEach((key, wrapper) -> updates.put(key, new SessionUpdatesList<>(realmModel, wrapper)));
    }

    private void lookupAndAndExecuteTask(K key, SessionUpdateTask<V> task) {
        // Lookup entity from cache
        SessionEntityWrapper<V> wrappedEntity = cacheHolder.cache().get(key);
        if (wrappedEntity == null) {
            logger.tracef("Not present cache item for key %s", key);
            return;
        }

        RealmModel realm = kcSession.realms().getRealm(wrappedEntity.getEntity().getRealmId());

        SessionUpdatesList<V> myUpdates = new SessionUpdatesList<>(realm, wrappedEntity);
        updates.put(key, myUpdates);

        // Run the update now, so reader in same transaction can see it (TODO: Rollback may not work correctly. See if it's an issue..)
        myUpdates.addAndExecute(task);
    }
}
