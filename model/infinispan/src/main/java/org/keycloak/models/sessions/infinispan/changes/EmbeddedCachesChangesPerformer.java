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

import org.infinispan.AdvancedCache;
import org.infinispan.Cache;
import org.infinispan.context.Flag;
import org.jboss.logging.Logger;
import org.keycloak.connections.infinispan.InfinispanUtil;
import org.keycloak.models.sessions.infinispan.CacheDecorators;
import org.keycloak.models.sessions.infinispan.entities.SessionEntity;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class EmbeddedCachesChangesPerformer<K, V extends SessionEntity> implements SessionChangesPerformer<K, V> {

    private static final Logger LOG = Logger.getLogger(EmbeddedCachesChangesPerformer.class);
    private final Cache<K, SessionEntityWrapper<V>> cache;
    private final List<Supplier<CompletableFuture<?>>> changes = new LinkedList<>();

    public EmbeddedCachesChangesPerformer(Cache<K, SessionEntityWrapper<V>> cache) {
        this.cache = cache;
    }

    private CompletableFuture<?> runOperationInCluster(K key, MergedUpdate<V> task, SessionEntityWrapper<V> sessionWrapper) {
        V session = sessionWrapper.getEntity();
        SessionUpdateTask.CacheOperation operation = task.getOperation(session);

        // Don't need to run update of underlying entity. Local updates were already run
        //task.runUpdate(session);

        switch (operation) {
            case REMOVE:
                // Just remove it
                return CacheDecorators.skipCacheStoreIfRemoteCacheIsEnabled(cache)
                        .withFlags(Flag.IGNORE_RETURN_VALUES)
                        .removeAsyncEntry(key);
            case ADD:
                return CacheDecorators.skipCacheStoreIfRemoteCacheIsEnabled(cache)
                        .withFlags(Flag.IGNORE_RETURN_VALUES)
                        .putAsync(key, sessionWrapper, task.getLifespanMs(), TimeUnit.MILLISECONDS, task.getMaxIdleTimeMs(), TimeUnit.MILLISECONDS)
                        .thenAcceptAsync(v -> LOG.tracef("Added entity '%s' to the cache '%s' . Lifespan: %d ms, MaxIdle: %d ms", key, cache.getName(), task.getLifespanMs(), task.getMaxIdleTimeMs()));
            case ADD_IF_ABSENT:
                return CacheDecorators.skipCacheStoreIfRemoteCacheIsEnabled(cache).putIfAbsentAsync(key, sessionWrapper, task.getLifespanMs(), TimeUnit.MILLISECONDS, task.getMaxIdleTimeMs(), TimeUnit.MILLISECONDS)
                        .thenAccept(existing -> {
                            if (existing != null) {
                                LOG.debugf("Existing entity in cache for key: %s . Will update it", key);

                                // Apply updates on the existing entity and replace it
                                task.runUpdate(existing.getEntity());

                                replace(key, task, existing, task.getLifespanMs(), task.getMaxIdleTimeMs()).join();
                            } else {
                                LOG.tracef("Add_if_absent successfully called for entity '%s' to the cache '%s' . Lifespan: %d ms, MaxIdle: %d ms", key, cache.getName(), task.getLifespanMs(), task.getMaxIdleTimeMs());
                            }
                        });
            case REPLACE:
                return replace(key, task, sessionWrapper, task.getLifespanMs(), task.getMaxIdleTimeMs());
            default:
                throw new IllegalStateException("Unsupported state " +  operation);
        }
    }

    private CompletableFuture<?> replace(K key, MergedUpdate<V> task, SessionEntityWrapper<V> oldVersionEntityFirst, long lifespanMs, long maxIdleTimeMs) {
        AdvancedCache<K, SessionEntityWrapper<V>> writeCache = CacheDecorators.skipCacheStoreIfRemoteCacheIsEnabled(cache);
        // make one async attempt
        SessionEntityWrapper<V> newVersionEntityFirst = generateNewVersionAndWrapEntity(oldVersionEntityFirst.getEntity(), oldVersionEntityFirst.getLocalMetadata());
        return writeCache.computeIfPresentAsync(key, new ReplaceFunction<>(oldVersionEntityFirst.getVersion(), newVersionEntityFirst), lifespanMs, TimeUnit.MILLISECONDS, maxIdleTimeMs, TimeUnit.MILLISECONDS)
                .thenAccept(returnValue -> {
                    int iteration = 0;
                    SessionEntityWrapper<V> newVersionEntity = newVersionEntityFirst;
                    SessionEntityWrapper<V> oldVersion = oldVersionEntityFirst;

                    while (true) {
                        if (returnValue == null) {
                            LOG.debugf("Entity %s not found. Maybe removed in the meantime. Replace task will be ignored", key);
                            return;
                        }

                        if (returnValue.getVersion().equals(newVersionEntity.getVersion())){
                            if (LOG.isTraceEnabled()) {
                                LOG.tracef("Replace SUCCESS for entity: %s . old version: %s, new version: %s, Lifespan: %d ms, MaxIdle: %d ms", key, oldVersion.getVersion(), newVersionEntity.getVersion(), task.getLifespanMs(), task.getMaxIdleTimeMs());
                            }

                            return;
                        } else {
                            if (LOG.isTraceEnabled()) {
                                LOG.tracef("Replace failed for entity: %s, old version %s, new version %s. Will try again", key, oldVersion.getVersion(), newVersionEntity.getVersion());
                            }
                        }

                        if (++iteration >= InfinispanUtil.MAXIMUM_REPLACE_RETRIES) {
                            LOG.warnf("Failed to replace entity '%s' in cache '%s'. Expected: %s, Current: %s", key, cache.getName(), oldVersion, returnValue);
                            return;
                        }

                        oldVersion = returnValue;
                        V session = oldVersion.getEntity();
                        task.runUpdate(session);
                        newVersionEntity = generateNewVersionAndWrapEntity(session, oldVersion.getLocalMetadata());

                        returnValue = writeCache.computeIfPresent(key, new ReplaceFunction<>(oldVersion.getVersion(), newVersionEntity), lifespanMs, TimeUnit.MILLISECONDS, maxIdleTimeMs, TimeUnit.MILLISECONDS);
                    }
                });
    }

    private SessionEntityWrapper<V> generateNewVersionAndWrapEntity(V entity, Map<String, String> localMetadata) {
        return new SessionEntityWrapper<>(localMetadata, entity);
    }

    @Override
    public void registerChange(Map.Entry<K, SessionUpdatesList<V>> entry, MergedUpdate<V> merged) {
        changes.add(() -> runOperationInCluster(entry.getKey(), merged, entry.getValue().getEntityWrapper()));
    }

    @Override
    public void applyChanges() {
        if (!changes.isEmpty()) {
            List<Throwable> exceptions = new ArrayList<>();
            CompletableFuture.allOf(changes.stream().map(s -> s.get().exceptionally(throwable -> {
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
}
