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

import org.keycloak.cluster.ClusterEvent;
import org.keycloak.cluster.ClusterProvider;
import org.infinispan.context.Flag;
import org.keycloak.models.KeycloakTransaction;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.infinispan.Cache;
import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class InfinispanKeycloakTransaction implements KeycloakTransaction {

    private final static Logger log = Logger.getLogger(InfinispanKeycloakTransaction.class);

    public enum CacheOperation {
        ADD, ADD_WITH_LIFESPAN, REMOVE, REPLACE, ADD_IF_ABSENT // ADD_IF_ABSENT throws an exception if there is existing value
    }

    private boolean active;
    private boolean rollback;
    private final Map<Object, CacheTask> tasks = new LinkedHashMap<>();

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
            tasks.put(taskKey, new CacheTaskWithValue<V>(value) {
                @Override
                public void execute() {
                    decorateCache(cache).put(key, value);
                }

                @Override
                public String toString() {
                    return String.format("CacheTaskWithValue: Operation 'put' for key %s", key);
                }
            });
        }
    }

    public <K, V> void put(Cache<K, V> cache, K key, V value, long lifespan, TimeUnit lifespanUnit) {
        log.tracev("Adding cache operation: {0} on {1}", CacheOperation.ADD_WITH_LIFESPAN, key);

        Object taskKey = getTaskKey(cache, key);
        if (tasks.containsKey(taskKey)) {
            throw new IllegalStateException("Can't add session: task in progress for session");
        } else {
            tasks.put(taskKey, new CacheTaskWithValue<V>(value) {
                @Override
                public void execute() {
                    decorateCache(cache).put(key, value, lifespan, lifespanUnit);
                }

                @Override
                public String toString() {
                    return String.format("CacheTaskWithValue: Operation 'put' for key %s, lifespan %d TimeUnit %s", key, lifespan, lifespanUnit.toString());
                }
            });
        }
    }

    public <K, V> void putIfAbsent(Cache<K, V> cache, K key, V value) {
        log.tracev("Adding cache operation: {0} on {1}", CacheOperation.ADD_IF_ABSENT, key);

        Object taskKey = getTaskKey(cache, key);
        if (tasks.containsKey(taskKey)) {
            throw new IllegalStateException("Can't add session: task in progress for session");
        } else {
            tasks.put(taskKey, new CacheTaskWithValue<V>(value) {
                @Override
                public void execute() {
                    V existing = cache.putIfAbsent(key, value);
                    if (existing != null) {
                        throw new IllegalStateException("There is already existing value in cache for key " + key);
                    }
                }

                @Override
                public String toString() {
                    return String.format("CacheTaskWithValue: Operation 'putIfAbsent' for key %s", key);
                }
            });
        }
    }

    public <K, V> void replace(Cache<K, V> cache, K key, V value) {
        log.tracev("Adding cache operation: {0} on {1}", CacheOperation.REPLACE, key);

        Object taskKey = getTaskKey(cache, key);
        CacheTask current = tasks.get(taskKey);
        if (current != null) {
            if (current instanceof CacheTaskWithValue) {
                ((CacheTaskWithValue<V>) current).setValue(value);
            }
        } else {
            tasks.put(taskKey, new CacheTaskWithValue<V>(value) {
                @Override
                public void execute() {
                    decorateCache(cache).replace(key, value);
                }

                @Override
                public String toString() {
                    return String.format("CacheTaskWithValue: Operation 'replace' for key %s", key);
                }

            });
        }
    }

    public <K, V> void notify(ClusterProvider clusterProvider, String taskKey, ClusterEvent event, boolean ignoreSender) {
        log.tracev("Adding cache operation SEND_EVENT: {0}", event);

        String theTaskKey = taskKey;
        int i = 1;
        while (tasks.containsKey(theTaskKey)) {
            theTaskKey = taskKey + "-" + (i++);
        }

        tasks.put(taskKey, () -> clusterProvider.notify(taskKey, event, ignoreSender, ClusterProvider.DCNotify.ALL_DCS));
    }

    public <K, V> void remove(Cache<K, V> cache, K key) {
        log.tracev("Adding cache operation: {0} on {1}", CacheOperation.REMOVE, key);

        Object taskKey = getTaskKey(cache, key);

        // TODO:performance Eventual performance optimization could be to skip "cache.remove" if item was added in this transaction (EG. authenticationSession valid for single request due to automatic SSO login)
        tasks.put(taskKey, new CacheTask() {

            @Override
            public void execute() {
                decorateCache(cache).remove(key);
            }

            @Override
            public String toString() {
                return String.format("CacheTask: Operation 'remove' for key %s", key);
            }

        });
    }

    // This is for possibility to lookup for session by id, which was created in this transaction
    public <K, V> V get(Cache<K, V> cache, K key) {
        Object taskKey = getTaskKey(cache, key);
        CacheTask current = tasks.get(taskKey);
        if (current != null) {
            if (current instanceof CacheTaskWithValue) {
                return ((CacheTaskWithValue<V>) current).getValue();
            }
            return null;
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

    public interface CacheTask {
        void execute();
    }

    public abstract class CacheTaskWithValue<V> implements CacheTask {
        protected V value;

        public CacheTaskWithValue(V value) {
            this.value = value;
        }

        public V getValue() {
            return value;
        }

        public void setValue(V value) {
            this.value = value;
        }
    }

    // Ignore return values. Should have better performance within cluster / cross-dc env
    private static <K, V> Cache<K, V> decorateCache(Cache<K, V> cache) {
        return cache.getAdvancedCache()
                .withFlags(Flag.IGNORE_RETURN_VALUES, Flag.SKIP_REMOTE_LOOKUP);
    }
}