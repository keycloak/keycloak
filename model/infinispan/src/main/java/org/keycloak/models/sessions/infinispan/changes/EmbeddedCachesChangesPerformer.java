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
import org.keycloak.connections.infinispan.InfinispanUtil;
import org.keycloak.models.sessions.infinispan.CacheDecorators;
import org.keycloak.models.sessions.infinispan.entities.SessionEntity;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class EmbeddedCachesChangesPerformer<K, V extends SessionEntity> implements SessionChangesPerformer<K, V> {

    private static final Logger LOG = Logger.getLogger(EmbeddedCachesChangesPerformer.class);
    private final Cache<K, SessionEntityWrapper<V>> cache;
    private final SerializeExecutionsByKey<K> serializer;
    private final List<Runnable> changes = new LinkedList<>();

    public EmbeddedCachesChangesPerformer(Cache<K, SessionEntityWrapper<V>> cache, SerializeExecutionsByKey<K> serializer) {
        this.cache = cache;
        this.serializer = serializer;
    }

    private void runOperationInCluster(K key, MergedUpdate<V> task,  SessionEntityWrapper<V> sessionWrapper) {
        SessionUpdateTask.CacheOperation operation = task.getOperation();

        // Don't need to run update of underlying entity. Local updates were already run
        //task.runUpdate(session);

        switch (operation) {
            case REMOVE:
                // Just remove it
                CacheDecorators.ignoreReturnValues(cache).remove(key);
                break;
            case ADD:
                CacheDecorators.ignoreReturnValues(cache)
                        .put(key, sessionWrapper, task.getLifespanMs(), TimeUnit.MILLISECONDS, task.getMaxIdleTimeMs(), TimeUnit.MILLISECONDS);

                LOG.tracef("Added entity '%s' to the cache '%s' . Lifespan: %d ms, MaxIdle: %d ms", key, cache.getName(), task.getLifespanMs(), task.getMaxIdleTimeMs());
                break;
            case ADD_IF_ABSENT:
                SessionEntityWrapper<V> existing = cache.putIfAbsent(key, sessionWrapper, task.getLifespanMs(), TimeUnit.MILLISECONDS, task.getMaxIdleTimeMs(), TimeUnit.MILLISECONDS);
                if (existing != null) {
                    LOG.debugf("Existing entity in cache for key: %s . Will update it", key);

                    // Apply updates on the existing entity and replace it
                    task.runUpdate(existing.getEntity());

                    replace(key, task, existing, task.getLifespanMs(), task.getMaxIdleTimeMs());
                } else {
                    LOG.tracef("Add_if_absent successfully called for entity '%s' to the cache '%s' . Lifespan: %d ms, MaxIdle: %d ms", key, cache.getName(), task.getLifespanMs(), task.getMaxIdleTimeMs());
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
            while (iteration++ < InfinispanUtil.MAXIMUM_REPLACE_RETRIES) {
                SessionEntityWrapper<V> newVersionEntity = generateNewVersionAndWrapEntity(session, oldVersion.getLocalMetadata());
                returnValue = cache.computeIfPresent(key, new ReplaceFunction<>(oldVersion.getVersion(), newVersionEntity), lifespanMs, TimeUnit.MILLISECONDS, maxIdleTimeMs, TimeUnit.MILLISECONDS);

                if (returnValue == null) {
                    LOG.debugf("Entity %s not found. Maybe removed in the meantime. Replace task will be ignored", key);
                    return;
                }

                if (returnValue.getVersion().equals(newVersionEntity.getVersion())) {
                    if (LOG.isTraceEnabled()) {
                        LOG.tracef("Replace SUCCESS for entity: %s . old version: %s, new version: %s, Lifespan: %d ms, MaxIdle: %d ms", key, oldVersion.getVersion(), newVersionEntity.getVersion(), task.getLifespanMs(), task.getMaxIdleTimeMs());
                    }
                    return;
                }

                oldVersion = returnValue;
                session = oldVersion.getEntity();
                task.runUpdate(session);
            }

            LOG.warnf("Failed to replace entity '%s' in cache '%s'. Expected: %s, Current: %s", key, cache.getName(), oldVersion, returnValue);
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
        changes.forEach(Runnable::run);
    }
}
