/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import org.infinispan.Cache;
import org.infinispan.commons.util.concurrent.AggregateCompletionStage;
import org.infinispan.commons.util.concurrent.CompletableFutures;
import org.infinispan.util.concurrent.BlockingManager;
import org.jboss.logging.Logger;
import org.keycloak.connections.infinispan.InfinispanUtil;
import org.keycloak.models.sessions.infinispan.CacheDecorators;
import org.keycloak.models.sessions.infinispan.entities.SessionEntity;

class InfinispanChangesUtils {

    private InfinispanChangesUtils() {
    }

    public static <K, V extends SessionEntity> void runOperationInCluster(
            Cache<K, SessionEntityWrapper<V>> cache,
            K key,
            MergedUpdate<V> task,
            SessionEntityWrapper<V> sessionWrapper,
            AggregateCompletionStage<Void> stage,
            SerializeExecutionsByKey<K> serializer,
            BlockingManager blockingManager,
            Logger logger
    ) {
        SessionUpdateTask.CacheOperation operation = task.getOperation();

        // Don't need to run update of underlying entity. Local updates were already run
        //task.runUpdate(session);

        switch (operation) {
            case REMOVE:
                // Just remove it
                stage.dependsOn(CacheDecorators.ignoreReturnValues(cache).removeAsync(key));
                break;
            case ADD:
                CompletableFuture<?> future = CacheDecorators.ignoreReturnValues(cache)
                        .putAsync(key, sessionWrapper, task.getLifespanMs(), TimeUnit.MILLISECONDS, task.getMaxIdleTimeMs(), TimeUnit.MILLISECONDS);
                if (logger.isTraceEnabled()) {
                    future = future.thenRun(() -> logger.tracef("Added entity '%s' to the cache '%s' . Lifespan: %d ms, MaxIdle: %d ms", key, cache.getName(), task.getLifespanMs(), task.getMaxIdleTimeMs()));
                }
                stage.dependsOn(future);
                break;
            case ADD_IF_ABSENT:
                CompletableFuture<Void> putIfAbsentFuture = cache.putIfAbsentAsync(key, sessionWrapper, task.getLifespanMs(), TimeUnit.MILLISECONDS, task.getMaxIdleTimeMs(), TimeUnit.MILLISECONDS)
                        .thenCompose(existing -> handlePutIfAbsentResponse(cache, existing, key, task, serializer, blockingManager, logger));
                stage.dependsOn(putIfAbsentFuture);
                break;
            case REPLACE:
                stage.dependsOn(replace(cache, key, task, sessionWrapper, task.getLifespanMs(), task.getMaxIdleTimeMs(), serializer, blockingManager, logger));
                break;
            default:
                throw new IllegalStateException("Unsupported state " + operation);
        }

    }

    private static <K, V extends SessionEntity> CompletionStage<Void> handlePutIfAbsentResponse(
            Cache<K, SessionEntityWrapper<V>> cache,
            SessionEntityWrapper<V> existing,
            K key,
            MergedUpdate<V> task,
            SerializeExecutionsByKey<K> serializer,
            BlockingManager blockingManager, Logger logger
    ) {
        if (existing == null) {
            if (logger.isTraceEnabled()) {
                logger.tracef("Add_if_absent successfully called for entity '%s' to the cache '%s' . Lifespan: %d ms, MaxIdle: %d ms", key, cache.getName(), task.getLifespanMs(), task.getMaxIdleTimeMs());
            }
            return CompletableFutures.completedNull();
        }
        if (logger.isDebugEnabled()) {
            logger.debugf("Existing entity in cache for key: %s . Will update it", key);
        }

        // Apply updates on the existing entity and replace it
        task.runUpdate(existing.getEntity());

        return replace(cache, key, task, existing, task.getLifespanMs(), task.getMaxIdleTimeMs(), serializer, blockingManager, logger);
    }

    private static <K, V extends SessionEntity> CompletionStage<Void> replace(
            Cache<K, SessionEntityWrapper<V>> cache,
            K key,
            MergedUpdate<V> task,
            SessionEntityWrapper<V> oldVersionEntity,
            long lifespanMs,
            long maxIdleTimeMs,
            SerializeExecutionsByKey<K> serializer,
            BlockingManager blockingManager,
            Logger logger) {
        // Ugly!! We cannot have a full non-blocking implementation without getting rid of "serializer".
        // It may have a negative impact in low core count instances.
        // TODO see org.infinispan.util.concurrent.ActionSequencer
        return blockingManager.runBlocking(() -> serialReplace(cache, key, task, oldVersionEntity, lifespanMs, maxIdleTimeMs, serializer, logger), "replace");
    }

    private static <K, V extends SessionEntity> void serialReplace(
            Cache<K, SessionEntityWrapper<V>> cache,
            K key,
            MergedUpdate<V> task,
            SessionEntityWrapper<V> oldVersionEntity,
            long lifespanMs,
            long maxIdleTimeMs,
            SerializeExecutionsByKey<K> serializer,
            Logger logger
    ) {
        serializer.runSerialized(key, () -> {
            SessionEntityWrapper<V> oldVersion = oldVersionEntity;
            SessionEntityWrapper<V> returnValue = null;
            int iteration = 0;
            V session = oldVersion.getEntity();
            while (iteration++ < InfinispanUtil.MAXIMUM_REPLACE_RETRIES) {

                if (session.shouldEvaluateRemoval() && task.shouldRemove(session)) {
                    logger.debugf("Entity %s removed after evaluation", key);
                    CacheDecorators.ignoreReturnValues(cache).remove(key);
                    return;
                }

                SessionEntityWrapper<V> newVersionEntity = generateNewVersionAndWrapEntity(session, oldVersion.getLocalMetadata());
                returnValue = cache.computeIfPresent(key, new ReplaceFunction<>(oldVersion.getVersion(), newVersionEntity), lifespanMs, TimeUnit.MILLISECONDS, maxIdleTimeMs, TimeUnit.MILLISECONDS);

                if (returnValue == null) {
                    logger.debugf("Entity %s not found. Maybe removed in the meantime. Replace task will be ignored", key);
                    return;
                }

                if (returnValue.getVersion().equals(newVersionEntity.getVersion())) {
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

    private static <V extends SessionEntity> SessionEntityWrapper<V> generateNewVersionAndWrapEntity(V entity, Map<String, String> localMetadata) {
        return new SessionEntityWrapper<>(localMetadata, entity);
    }

}
