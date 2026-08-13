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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.keycloak.models.sessions.infinispan.transaction.DatabaseUpdate;
import org.keycloak.models.sessions.infinispan.transaction.NonBlockingTransaction;

import org.infinispan.Cache;
import org.infinispan.commons.api.BasicCache;
import org.infinispan.commons.util.concurrent.AggregateCompletionStage;
import org.infinispan.context.Flag;
import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class InfinispanKeycloakTransaction implements NonBlockingTransaction {

    private final static Logger log = Logger.getLogger(InfinispanKeycloakTransaction.class);

    /**
     * Tombstone to mark an entry as already removed for the current session.
     */
    private static final CacheTask TOMBSTONE = new CacheTask() {
        @Override
        public void execute(AggregateCompletionStage<Void> stage) {
            // noop
        }

        @Override
        public String toString() {
            return "Tombstone after removal";
        }
    };

    private final Map<Object, CacheTask> tasks = new HashMap<>();

    private enum CacheOperation {
        ADD_WITH_LIFESPAN, REMOVE, REPLACE
    }

    @Override
    public void asyncCommit(AggregateCompletionStage<Void> stage, Consumer<DatabaseUpdate> databaseUpdates) {
        for (var task : tasks.values()) {
            task.execute(stage);
        }
    }

    @Override
    public void asyncRollback(AggregateCompletionStage<Void> stage) {
        tasks.clear();
    }

    public <K, V> void put(BasicCache<K, V> cache, K key, V value, long lifespan, TimeUnit lifespanUnit) {
        log.tracev("Adding cache operation: {0} on {1}", CacheOperation.ADD_WITH_LIFESPAN, key);

        Object taskKey = getTaskKey(cache, key);
        if (tasks.containsKey(taskKey)) {
            throw new IllegalStateException("Can't add session: task in progress for session");
        } else {
            tasks.put(taskKey, new CacheTaskWithValue<V>(value, lifespan, lifespanUnit) {
                @Override
                public void execute(AggregateCompletionStage<Void> stage) {
                    stage.dependsOn(decorateCache(cache).putAsync(key, value, lifespan, lifespanUnit));
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
            } else if (current != TOMBSTONE && current.getOperation() != Operation.REMOVE) {
                // A previous delete operation will take precedence over any new replace
                throw new IllegalStateException("Can't replace entry: task " + current + " in progress for session");
            }
        } else {
            tasks.put(taskKey, new CacheTaskWithValue<V>(value, lifespan, lifespanUnit) {
                @Override
                public void execute(AggregateCompletionStage<Void> stage) {
                    stage.dependsOn(decorateCache(cache).replaceAsync(key, value, lifespan, lifespanUnit));
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
            if (current instanceof CacheTaskWithValue && current.getOperation() == Operation.PUT) {
                tasks.put(taskKey, TOMBSTONE);
                return;
            }
            if (current == TOMBSTONE) {
                return;
            }
        }

        tasks.put(taskKey, new CacheTask() {

            @Override
            public void execute(AggregateCompletionStage<Void> stage) {
                stage.dependsOn(decorateCache(cache).removeAsync(key));
            }

            @Override
            public String toString() {
                return String.format("CacheTask: Operation 'remove' for key %s", key);
            }

            @Override
            public Operation getOperation() {
                return Operation.REMOVE;
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
        return key instanceof String ? cache.getName() + "::" + key : key;
    }

    private interface CacheTask {
        void execute(AggregateCompletionStage<Void> stage);

        default Operation getOperation() {
            return Operation.OTHER;
        }
    }

    private enum Operation { PUT, REMOVE, OTHER }

    private static abstract class CacheTaskWithValue<V> implements CacheTask {
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
    }

    // Ignore return values. Should have better performance within cluster
    private static <K, V> BasicCache<K, V> decorateCache(BasicCache<K, V> cache) {
        return cache instanceof Cache<K, V> c ?
                c.getAdvancedCache().withFlags(Flag.IGNORE_RETURN_VALUES, Flag.SKIP_REMOTE_LOOKUP) :
                cache;
    }
}
