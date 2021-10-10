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
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

import org.infinispan.Cache;
import org.infinispan.context.Flag;
import org.jboss.logging.Logger;
import org.keycloak.models.AbstractKeycloakTransaction;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.sessions.infinispan.CacheDecorators;
import org.keycloak.models.sessions.infinispan.entities.SessionEntity;
import org.keycloak.models.sessions.infinispan.remotestore.RemoteCacheInvoker;
import org.keycloak.connections.infinispan.InfinispanUtil;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class InfinispanChangelogBasedTransaction<K, V extends SessionEntity> extends AbstractKeycloakTransaction {

    public static final Logger logger = Logger.getLogger(InfinispanChangelogBasedTransaction.class);

    private final KeycloakSession kcSession;
    private final String cacheName;
    private final Cache<K, SessionEntityWrapper<V>> cache;
    private final RemoteCacheInvoker remoteCacheInvoker;

    private final Map<K, SessionUpdatesList<V>> updates = new HashMap<>();

    private final BiFunction<RealmModel, V, Long> lifespanMsLoader;
    private final BiFunction<RealmModel, V, Long> maxIdleTimeMsLoader;

    public InfinispanChangelogBasedTransaction(KeycloakSession kcSession, Cache<K, SessionEntityWrapper<V>> cache, RemoteCacheInvoker remoteCacheInvoker,
                                               BiFunction<RealmModel, V, Long> lifespanMsLoader, BiFunction<RealmModel, V, Long> maxIdleTimeMsLoader) {
        this.kcSession = kcSession;
        this.cacheName = cache.getName();
        this.cache = cache;
        this.remoteCacheInvoker = remoteCacheInvoker;
        this.lifespanMsLoader = lifespanMsLoader;
        this.maxIdleTimeMsLoader = maxIdleTimeMsLoader;
    }


    public void addTask(K key, SessionUpdateTask<V> task) {
        SessionUpdatesList<V> myUpdates = updates.get(key);
        if (myUpdates == null) {
            // Lookup entity from cache
            SessionEntityWrapper<V> wrappedEntity = cache.get(key);
            if (wrappedEntity == null) {
                logger.tracef("Not present cache item for key %s", key);
                return;
            }

            RealmModel realm = kcSession.realms().getRealm(wrappedEntity.getEntity().getRealmId());

            myUpdates = new SessionUpdatesList<>(realm, wrappedEntity);
            updates.put(key, myUpdates);
        }

        // Run the update now, so reader in same transaction can see it (TODO: Rollback may not work correctly. See if it's an issue..)
        task.runUpdate(myUpdates.getEntityWrapper().getEntity());
        myUpdates.add(task);
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

        // Run the update now, so reader in same transaction can see it
        task.runUpdate(entity);
        myUpdates.add(task);
    }


    public void reloadEntityInCurrentTransaction(RealmModel realm, K key, SessionEntityWrapper<V> entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Null entity not allowed");
        }

        SessionEntityWrapper<V> latestEntity = cache.get(key);
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
            SessionEntityWrapper<V> wrappedEntity = cache.get(key);
            if (wrappedEntity == null) {
                return null;
            }

            RealmModel realm = kcSession.realms().getRealm(wrappedEntity.getEntity().getRealmId());

            myUpdates = new SessionUpdatesList<>(realm, wrappedEntity);
            updates.put(key, myUpdates);

            return wrappedEntity;
        } else {
            V entity = myUpdates.getEntityWrapper().getEntity();

            // If entity is scheduled for remove, we don't return it.
            boolean scheduledForRemove = myUpdates.getUpdateTasks().stream().filter((SessionUpdateTask task) -> {

                return task.getOperation(entity) == SessionUpdateTask.CacheOperation.REMOVE;

            }).findFirst().isPresent();

            return scheduledForRemove ? null : myUpdates.getEntityWrapper();
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

            long lifespanMs = lifespanMsLoader.apply(realm, sessionWrapper.getEntity());
            long maxIdleTimeMs = maxIdleTimeMsLoader.apply(realm, sessionWrapper.getEntity());

            MergedUpdate<V> merged = MergedUpdate.computeUpdate(sessionUpdates.getUpdateTasks(), sessionWrapper, lifespanMs, maxIdleTimeMs);

            if (merged != null) {
                // Now run the operation in our cluster
                runOperationInCluster(entry.getKey(), merged, sessionWrapper);

                // Check if we need to send message to second DC
                remoteCacheInvoker.runTask(kcSession, realm, cacheName, entry.getKey(), merged, sessionWrapper);
            }
        }
    }


    private void runOperationInCluster(K key, MergedUpdate<V> task,  SessionEntityWrapper<V> sessionWrapper) {
        V session = sessionWrapper.getEntity();
        SessionUpdateTask.CacheOperation operation = task.getOperation(session);

        // Don't need to run update of underlying entity. Local updates were already run
        //task.runUpdate(session);

        switch (operation) {
            case REMOVE:
                // Just remove it
                CacheDecorators.skipCacheStore(cache)
                        .getAdvancedCache().withFlags(Flag.IGNORE_RETURN_VALUES)
                        .remove(key);
                break;
            case ADD:
                CacheDecorators.skipCacheStore(cache)
                        .getAdvancedCache().withFlags(Flag.IGNORE_RETURN_VALUES)
                        .put(key, sessionWrapper, task.getLifespanMs(), TimeUnit.MILLISECONDS, task.getMaxIdleTimeMs(), TimeUnit.MILLISECONDS);

                logger.tracef("Added entity '%s' to the cache '%s' . Lifespan: %d ms, MaxIdle: %d ms", key, cache.getName(), task.getLifespanMs(), task.getMaxIdleTimeMs());
                break;
            case ADD_IF_ABSENT:
                SessionEntityWrapper<V> existing = CacheDecorators.skipCacheStore(cache).putIfAbsent(key, sessionWrapper, task.getLifespanMs(), TimeUnit.MILLISECONDS, task.getMaxIdleTimeMs(), TimeUnit.MILLISECONDS);
                if (existing != null) {
                    logger.debugf("Existing entity in cache for key: %s . Will update it", key);

                    // Apply updates on the existing entity and replace it
                    task.runUpdate(existing.getEntity());

                    replace(key, task, existing, task.getLifespanMs(), task.getMaxIdleTimeMs());
                } else {
                    logger.tracef("Add_if_absent successfully called for entity '%s' to the cache '%s' . Lifespan: %d ms, MaxIdle: %d ms", key, cache.getName(), task.getLifespanMs(), task.getMaxIdleTimeMs());
                }
                break;
            case REPLACE:
                replace(key, task, sessionWrapper, task.getLifespanMs(), task.getMaxIdleTimeMs());
                break;
            default:
                throw new IllegalStateException("Unsupported state " +  operation);
        }

    }


    private void replace(K key, MergedUpdate<V> task, SessionEntityWrapper<V> oldVersionEntity, long lifespanMs, long maxIdleTimeMs) {
        boolean replaced = false;
        int iteration = 0;
        V session = oldVersionEntity.getEntity();

        while (!replaced && iteration < InfinispanUtil.MAXIMUM_REPLACE_RETRIES) {
            iteration++;

            SessionEntityWrapper<V> newVersionEntity = generateNewVersionAndWrapEntity(session, oldVersionEntity.getLocalMetadata());

            // Atomic cluster-aware replace
            replaced = CacheDecorators.skipCacheStore(cache).replace(key, oldVersionEntity, newVersionEntity, lifespanMs, TimeUnit.MILLISECONDS, maxIdleTimeMs, TimeUnit.MILLISECONDS);

            // Replace fail. Need to load latest entity from cache, apply updates again and try to replace in cache again
            if (!replaced) {
                if (logger.isDebugEnabled()) {
                    logger.debugf("Replace failed for entity: %s, old version %s, new version %s. Will try again", key, oldVersionEntity.getVersion(), newVersionEntity.getVersion());
                }

                oldVersionEntity = cache.get(key);

                if (oldVersionEntity == null) {
                    logger.debugf("Entity %s not found. Maybe removed in the meantime. Replace task will be ignored", key);
                    return;
                }

                session = oldVersionEntity.getEntity();

                task.runUpdate(session);
            } else {
                if (logger.isTraceEnabled()) {
                    logger.tracef("Replace SUCCESS for entity: %s . old version: %s, new version: %s, Lifespan: %d ms, MaxIdle: %d ms", key, oldVersionEntity.getVersion(), newVersionEntity.getVersion(), task.getLifespanMs(), task.getMaxIdleTimeMs());
                }
            }
        }

        if (!replaced) {
            logger.warnf("Failed to replace entity '%s' in cache '%s'", key, cache.getName());
        }

    }


    @Override
    protected void rollbackImpl() {
    }

    private SessionEntityWrapper<V> generateNewVersionAndWrapEntity(V entity, Map<String, String> localMetadata) {
        return new SessionEntityWrapper<>(localMetadata, entity);
    }

}
