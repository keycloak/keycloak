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

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.commons.api.BasicCache;
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

    /**
     * Tombstone to mark an entry as already removed for the current session.
     */
    private static final CacheTask TOMBSTONE = new CacheTask() {
        @Override
        public void execute() {
            // noop
        }

        @Override
        public String toString() {
            return "Tombstone after removal";
        }
    };

    public enum CacheOperation {
        ADD_WITH_LIFESPAN, REMOVE, REPLACE
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

    public <K, V> void put(BasicCache<K, V> cache, K key, V value, long lifespan, TimeUnit lifespanUnit) {
        log.tracev("Adding cache operation: {0} on {1}", CacheOperation.ADD_WITH_LIFESPAN, key);

        Object taskKey = getTaskKey(cache, key);
        if (tasks.containsKey(taskKey)) {
            throw new IllegalStateException("Can't add session: task in progress for session");
        } else {
            tasks.put(taskKey, new CacheTaskWithValue<V>(value, lifespan, lifespanUnit) {
                @Override
                public void execute() {
                    decorateCache(cache).put(key, value, lifespan, lifespanUnit);
                }

                @Override
                public String toString() {
                    return String.format("CacheTaskWithValue: Operation 'put' for key %s, lifespan %d TimeUnit %s", key, lifespan, lifespanUnit);
                }

                @Override
                public Operation getOperation() {
                    return Operation.PUT;
                }
            });
        }
    }

    public <K, V> void replace(Cache<K, V> cache, K key, V value, long lifespan, TimeUnit lifespanUnit) {
        log.tracev("Adding cache operation: {0} on {1}. Lifespan {2} {3}.", CacheOperation.REPLACE, key, lifespan, lifespanUnit);

        Object taskKey = getTaskKey(cache, key);
        CacheTask current = tasks.get(taskKey);
        if (current != null) {
            if (current instanceof CacheTaskWithValue) {
                ((CacheTaskWithValue<V>) current).setValue(value);
                ((CacheTaskWithValue<V>) current).updateLifespan(lifespan, lifespanUnit);
            }
        } else {
            tasks.put(taskKey, new CacheTaskWithValue<V>(value, lifespan, lifespanUnit) {
                @Override
                public void execute() {
                    decorateCache(cache).replace(key, value, lifespan, lifespanUnit);
                }

                @Override
                public String toString() {
                    return String.format("CacheTaskWithValue: Operation 'replace' for key %s, lifespan %d TimeUnit %s", key, lifespan, lifespanUnit);
                }

            });
        }
    }

    public <K, V> void remove(BasicCache<K, V> cache, K key) {
        log.tracev("Adding cache operation: {0} on {1}", CacheOperation.REMOVE, key);

        Object taskKey = getTaskKey(cache, key);

        CacheTask current = tasks.get(taskKey);
        if (current != null) {
            if (current instanceof CacheTaskWithValue && ((CacheTaskWithValue<?>) current).getOperation() == Operation.PUT) {
                tasks.put(taskKey, TOMBSTONE);
                return;
            }
            if (current == TOMBSTONE) {
                return;
            }
        }

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
    public <K, V> V get(BasicCache<K, V> cache, K key) {
        Object taskKey = getTaskKey(cache, key);
        CacheTask current = tasks.get(taskKey);
        if (current != null) {
            if (current instanceof CacheTaskWithValue) {
                return ((CacheTaskWithValue<V>) current).getValue();
            }
        }

        // Should we have per-transaction cache for lookups?
        return cache.get(key);
    }

    private static <K, V> Object getTaskKey(BasicCache<K, V> cache, K key) {
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

    public enum Operation { PUT, OTHER }

    public static abstract class CacheTaskWithValue<V> implements CacheTask {
        protected V value;
        protected long lifespan;
        protected TimeUnit lifespanUnit;

        public CacheTaskWithValue(V value, long lifespan, TimeUnit lifespanUnit) {
            this.value = value;
            this.lifespan = lifespan;
            this.lifespanUnit = lifespanUnit;
        }

        public V getValue() {
            return value;
        }

        public void setValue(V value) {
            this.value = value;
        }

        public void updateLifespan(long lifespan, TimeUnit lifespanUnit) {
            this.lifespan = lifespan;
            this.lifespanUnit = lifespanUnit;
        }

        public Operation getOperation() {
            return Operation.OTHER;
        }
    }

    // Ignore return values. Should have better performance within cluster / cross-dc env
    private static <K, V> BasicCache<K, V> decorateCache(BasicCache<K, V> cache) {
        if (cache instanceof RemoteCache)
            return cache;
        return ((Cache) cache).getAdvancedCache()
                .withFlags(Flag.IGNORE_RETURN_VALUES, Flag.SKIP_REMOTE_LOOKUP);
    }
}