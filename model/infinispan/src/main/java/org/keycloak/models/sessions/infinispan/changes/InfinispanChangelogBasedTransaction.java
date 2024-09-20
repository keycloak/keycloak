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
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.infinispan.Cache;
import org.infinispan.context.Flag;
import org.jboss.logging.Logger;
import org.keycloak.common.Profile;
import org.keycloak.models.AbstractKeycloakTransaction;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.sessions.infinispan.CacheDecorators;
import org.keycloak.models.sessions.infinispan.SessionFunction;
import org.keycloak.models.sessions.infinispan.entities.SessionEntity;
import org.keycloak.models.sessions.infinispan.remotestore.RemoteCacheInvoker;
import org.keycloak.connections.infinispan.InfinispanUtil;

import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.CLIENT_SESSION_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.OFFLINE_CLIENT_SESSION_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.OFFLINE_USER_SESSION_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.USER_SESSION_CACHE_NAME;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class InfinispanChangelogBasedTransaction<K, V extends SessionEntity> extends AbstractKeycloakTransaction implements SessionsChangelogBasedTransaction<K, V> {

    public static final Logger logger = Logger.getLogger(InfinispanChangelogBasedTransaction.class);

    protected final KeycloakSession kcSession;
    private final String cacheName;
    protected final Cache<K, SessionEntityWrapper<V>> cache;
    private final RemoteCacheInvoker remoteCacheInvoker;

    protected final Map<K, SessionUpdatesList<V>> updates = new HashMap<>();

    protected final SessionFunction<V> lifespanMsLoader;
    protected final SessionFunction<V> maxIdleTimeMsLoader;
    private final SerializeExecutionsByKey<K> serializer;

    public InfinispanChangelogBasedTransaction(KeycloakSession kcSession, Cache<K, SessionEntityWrapper<V>> cache, RemoteCacheInvoker remoteCacheInvoker,
                                               SessionFunction<V> lifespanMsLoader, SessionFunction<V> maxIdleTimeMsLoader, SerializeExecutionsByKey<K> serializer) {
        this.kcSession = kcSession;
        this.cacheName = cache.getName();
        this.cache = cache;
        this.remoteCacheInvoker = remoteCacheInvoker;
        this.lifespanMsLoader = lifespanMsLoader;
        this.maxIdleTimeMsLoader = maxIdleTimeMsLoader;
        this.serializer = serializer;
    }


    @Override
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

        if (task != null) {
            // Run the update now, so reader in same transaction can see it
            task.runUpdate(entity);
            myUpdates.add(task);
        }
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
            // If entity is scheduled for remove, we don't return it.
            boolean scheduledForRemove = myUpdates.getUpdateTasks().stream().filter((SessionUpdateTask task) -> {

                return task.getOperation() == SessionUpdateTask.CacheOperation.REMOVE;

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

            long lifespanMs = lifespanMsLoader.apply(realm, sessionUpdates.getClient(), sessionWrapper.getEntity());
            long maxIdleTimeMs = maxIdleTimeMsLoader.apply(realm, sessionUpdates.getClient(), sessionWrapper.getEntity());

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
        SessionUpdateTask.CacheOperation operation = task.getOperation();

        // Don't need to run update of underlying entity. Local updates were already run
        //task.runUpdate(session);

        switch (operation) {
            case REMOVE:
                // Just remove it
                CacheDecorators.skipCacheStoreIfRemoteCacheIsEnabled(cache)
                        .withFlags(Flag.IGNORE_RETURN_VALUES)
                        .remove(key);
                break;
            case ADD:
                CacheDecorators.skipCacheStoreIfRemoteCacheIsEnabled(cache)
                        .withFlags(Flag.IGNORE_RETURN_VALUES)
                        .put(key, sessionWrapper, task.getLifespanMs(), TimeUnit.MILLISECONDS, task.getMaxIdleTimeMs(), TimeUnit.MILLISECONDS);

                logger.tracef("Added entity '%s' to the cache '%s' . Lifespan: %d ms, MaxIdle: %d ms", key, cache.getName(), task.getLifespanMs(), task.getMaxIdleTimeMs());
                break;
            case ADD_IF_ABSENT:
                SessionEntityWrapper<V> existing = CacheDecorators.skipCacheStoreIfRemoteCacheIsEnabled(cache).putIfAbsent(key, sessionWrapper, task.getLifespanMs(), TimeUnit.MILLISECONDS, task.getMaxIdleTimeMs(), TimeUnit.MILLISECONDS);
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
        serializer.runSerialized(key, () -> {
            SessionEntityWrapper<V> oldVersion = oldVersionEntity;
            SessionEntityWrapper<V> returnValue = null;
            int iteration = 0;
            V session = oldVersion.getEntity();
            var writeCache = CacheDecorators.skipCacheStoreIfRemoteCacheIsEnabled(cache);
            while (iteration++ < InfinispanUtil.MAXIMUM_REPLACE_RETRIES) {
                SessionEntityWrapper<V> newVersionEntity = generateNewVersionAndWrapEntity(session, oldVersion.getLocalMetadata());
                returnValue = writeCache.computeIfPresent(key, new ReplaceFunction<>(oldVersion.getVersion(), newVersionEntity), lifespanMs, TimeUnit.MILLISECONDS, maxIdleTimeMs, TimeUnit.MILLISECONDS);

                if (returnValue == null) {
                    logger.debugf("Entity %s not found. Maybe removed in the meantime. Replace task will be ignored", key);
                    return;
                }

                if (returnValue.getVersion().equals(newVersionEntity.getVersion())){
                    if (logger.isTraceEnabled()) {
                        logger.tracef("Replace SUCCESS for entity: %s . old version: %s, new version: %s, Lifespan: %d ms, MaxIdle: %d ms", key, oldVersion.getVersion(), newVersionEntity.getVersion(), task.getLifespanMs(), task.getMaxIdleTimeMs());
                    }
                    return;
                }

                oldVersion = returnValue;
                session = oldVersion.getEntity();
                task.runUpdate(session);
            }

            logger.warnf("Failed to replace entity '%s' in cache '%s'. Expected: %s, Current: %s", key, cache.getName(), oldVersion, returnValue);
        });
    }

    @Override
    protected void rollbackImpl() {
    }

    private SessionEntityWrapper<V> generateNewVersionAndWrapEntity(V entity, Map<String, String> localMetadata) {
        return new SessionEntityWrapper<>(localMetadata, entity);
    }

}
