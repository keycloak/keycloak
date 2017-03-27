/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.models.sessions.infinispan;

import org.infinispan.context.Flag;
import org.keycloak.models.KeycloakTransaction;

import java.util.HashMap;
import java.util.Map;
import org.infinispan.Cache;
import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class InfinispanKeycloakTransaction implements KeycloakTransaction {

    private final static Logger log = Logger.getLogger(InfinispanKeycloakTransaction.class);

    public enum CacheOperation {
        ADD, REMOVE, REPLACE, ADD_IF_ABSENT // ADD_IF_ABSENT throws an exception if there is existing value
    }

    private boolean active;
    private boolean rollback;
    private final Map<Object, CacheTask> tasks = new HashMap<>();

    @Override
    public void begin() {
        active = true;
    }

    @Override
    public void commit() {
        if (rollback) {
            throw new RuntimeException("Rollback only!");
        }

        tasks.values().forEach(CacheTask::execute);
    }

    @Override
    public void rollback() {
        tasks.clear();
    }

    @Override
    public void setRollbackOnly() {
        rollback = true;
    }

    @Override
    public boolean getRollbackOnly() {
        return rollback;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    public <K, V> void put(Cache<K, V> cache, K key, V value) {
        log.tracev("Adding cache operation: {0} on {1}", CacheOperation.ADD, key);

        Object taskKey = getTaskKey(cache, key);
        if (tasks.containsKey(taskKey)) {
            throw new IllegalStateException("Can't add session: task in progress for session");
        } else {
            tasks.put(taskKey, new CacheTask<>(cache, CacheOperation.ADD, key, value));
        }
    }

    public <K, V> void putIfAbsent(Cache<K, V> cache, K key, V value) {
        log.tracev("Adding cache operation: {0} on {1}", CacheOperation.ADD_IF_ABSENT, key);

        Object taskKey = getTaskKey(cache, key);
        if (tasks.containsKey(taskKey)) {
            throw new IllegalStateException("Can't add session: task in progress for session");
        } else {
            tasks.put(taskKey, new CacheTask<>(cache, CacheOperation.ADD_IF_ABSENT, key, value));
        }
    }

    public <K, V> void replace(Cache<K, V> cache, K key, V value) {
        log.tracev("Adding cache operation: {0} on {1}", CacheOperation.REPLACE, key);

        Object taskKey = getTaskKey(cache, key);
        CacheTask current = tasks.get(taskKey);
        if (current != null) {
            switch (current.operation) {
                case ADD:
                case ADD_IF_ABSENT:
                case REPLACE:
                    current.value = value;
                    return;
                case REMOVE:
                    return;
            }
        } else {
            tasks.put(taskKey, new CacheTask<>(cache, CacheOperation.REPLACE, key, value));
        }
    }

    public <K, V> void remove(Cache<K, V> cache, K key) {
        log.tracev("Adding cache operation: {0} on {1}", CacheOperation.REMOVE, key);

        Object taskKey = getTaskKey(cache, key);
        tasks.put(taskKey, new CacheTask<>(cache, CacheOperation.REMOVE, key, null));
    }

    // This is for possibility to lookup for session by id, which was created in this transaction
    public <K, V> V get(Cache<K, V> cache, K key) {
        Object taskKey = getTaskKey(cache, key);
        CacheTask<K, V> current = tasks.get(taskKey);
        if (current != null) {
            switch (current.operation) {
                case ADD:
                case ADD_IF_ABSENT:
                case REPLACE:
                    return current.value;
                case REMOVE:
                    return null;
            }
        }

        // Should we have per-transaction cache for lookups?
        return cache.get(key);
    }

    private static <K, V> Object getTaskKey(Cache<K, V> cache, K key) {
        if (key instanceof String) {
            return new StringBuilder(cache.getName())
                    .append("::")
                    .append(key).toString();
        } else {
            return key;
        }
    }

    public static class CacheTask<K, V> {
        private final Cache<K, V> cache;
        private final CacheOperation operation;
        private final K key;
        private V value;

        public CacheTask(Cache<K, V> cache, CacheOperation operation, K key, V value) {
            this.cache = cache;
            this.operation = operation;
            this.key = key;
            this.value = value;
        }

        public void execute() {
            log.tracev("Executing cache operation: {0} on {1}", operation, key);

            switch (operation) {
                case ADD:
                    decorateCache().put(key, value);
                    break;
                case REMOVE:
                    decorateCache().remove(key);
                    break;
                case REPLACE:
                    decorateCache().replace(key, value);
                    break;
                case ADD_IF_ABSENT:
                    V existing = cache.putIfAbsent(key, value);
                    if (existing != null) {
                        throw new IllegalStateException("IllegalState. There is already existing value in cache for key " + key);
                    }
                    break;
            }
        }


        // Ignore return values. Should have better performance within cluster / cross-dc env
        private Cache<K, V> decorateCache() {
            return cache.getAdvancedCache()
                    .withFlags(Flag.IGNORE_RETURN_VALUES, Flag.SKIP_REMOTE_LOOKUP);
        }
    }
}