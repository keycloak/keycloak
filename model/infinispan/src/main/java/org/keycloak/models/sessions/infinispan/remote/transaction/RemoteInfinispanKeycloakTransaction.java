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

package org.keycloak.models.sessions.infinispan.remote.transaction;

import java.lang.invoke.MethodHandles;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.keycloak.models.sessions.infinispan.changes.remote.remover.ConditionalRemover;
import org.keycloak.models.sessions.infinispan.transaction.DatabaseUpdate;
import org.keycloak.models.sessions.infinispan.transaction.NonBlockingTransaction;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.commons.util.concurrent.AggregateCompletionStage;
import org.infinispan.commons.util.concurrent.CompletableFutures;
import org.jboss.logging.Logger;

class RemoteInfinispanKeycloakTransaction<K, V, R extends ConditionalRemover<K, V>> implements NonBlockingTransaction {

    private final static Logger logger = Logger.getLogger(MethodHandles.lookup().lookupClass());

    private final Map<K, Operation<K, V>> tasks = new LinkedHashMap<>();
    private final RemoteCache<K, V> cache;
    private final R conditionalRemover;

    RemoteInfinispanKeycloakTransaction(RemoteCache<K, V> cache, R conditionalRemover) {
        this.cache = Objects.requireNonNull(cache);
        this.conditionalRemover = Objects.requireNonNull(conditionalRemover);
    }

    @Override
    public void asyncCommit(AggregateCompletionStage<Void> stage, Consumer<DatabaseUpdate> databaseUpdates) {
        conditionalRemover.executeRemovals(cache, stage);
        tasks.values().stream()
                .filter(this::shouldCommitOperation)
                .map(this::commitOperation)
                .forEach(stage::dependsOn);
    }

    @Override
    public void asyncRollback(AggregateCompletionStage<Void> stage) {
        tasks.clear();
    }

    public void put(K key, V value, long lifespan, TimeUnit timeUnit) {
        logger.tracef("Adding %s.put(%S)", cache.getName(), key);

        if (tasks.containsKey(key)) {
            throw new IllegalStateException("Can't add entry: task " + tasks.get(key) + " in progress for session");
        }

        tasks.put(key, new PutOperation<>(key, value, lifespan, timeUnit));
    }

    public void replace(K key, V value, int lifespan, TimeUnit timeUnit) {
        logger.tracef("Adding %s.replace(%S)", cache.getName(), key);

        Operation<K, V> existing = tasks.get(key);
        if (existing != null) {
            if (existing.hasValue()) {
                tasks.put(key, existing.update(value, lifespan, timeUnit));
            } else if (!(existing == TOMBSTONE || existing instanceof RemoveOperation<K, V>)) {
                // A previous delete operation will take precedence over any new replace
                throw new IllegalStateException("Can't replace entry: task " + existing + " in progress for session");
            }
            return;
        }

        tasks.put(key, new ReplaceOperation<>(key, value, lifespan, timeUnit));
    }

    public void remove(K key) {
        logger.tracef("Adding %s.remove(%S)", cache.getName(), key);

        Operation<K, V> existing = tasks.get(key);
        if (existing != null && existing.canRemove()) {
            //noinspection unchecked
            tasks.put(key, (Operation<K, V>) TOMBSTONE);
            return;
        }

        tasks.put(key, new RemoveOperation<>(key));
    }

    public V get(K key) {
        var existing = tasks.get(key);

        if (existing != null && existing.hasValue()) {
            return existing.getValue();
        }

        // Should we have per-transaction cache for lookups?
        return cache.get(key);
    }

    public RemoteCache<K, V> getCache() {
        return cache;
    }

    R getConditionalRemover() {
        return conditionalRemover;
    }

    private boolean shouldCommitOperation(Operation<K, V> operation) {
        // Commit if any:
        // 1. it is a removal operation (no value to test the predicate).
        // 2. remove predicate is not present.
        // 3. value does not match the remove predicate.
        return !operation.hasValue() || !conditionalRemover.willRemove(operation.getCacheKey(), operation.getValue());
    }

    private CompletionStage<?> commitOperation(Operation<K, V> operation) {
        try {
            return operation.execute(cache);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    private interface Operation<K, V> {
        CompletionStage<?> execute(RemoteCache<K, V> cache);

        /**
         * Updates the operation with a new value and lifespan only if {@link #hasValue()} returns {@code true}.
         */
        default Operation<K, V> update(V newValue, int newLifespan, TimeUnit newTimeUnit) {
            return null;
        }

        /**
         * @return {@code true} if the operation can be removed from the tasks map. It will skip the {@link RemoteCache} removal.
         */
        default boolean canRemove() {
            return false;
        }

        /**
         * @return {@code true} if the operation has a value associated
         */
        default boolean hasValue() {
            return false;
        }

        default V getValue() {
            return null;
        }

        K getCacheKey();
    }

    private record PutOperation<K, V>(K key, V value, long lifespan, TimeUnit timeUnit) implements Operation<K, V> {

        @Override
        public CompletionStage<?> execute(RemoteCache<K, V> cache) {
            return cache.putAsync(key, value, lifespan, timeUnit);
        }

        @Override
        public Operation<K, V> update(V newValue, int newLifespan, TimeUnit newTimeUnit) {
            return new PutOperation<>(key, newValue, newLifespan, newTimeUnit);
        }

        @Override
        public boolean canRemove() {
            // since it is new entry in the cache, it can be removed form the tasks map.
            return true;
        }

        @Override
        public boolean hasValue() {
            return true;
        }

        @Override
        public V getValue() {
            return value;
        }

        @Override
        public K getCacheKey() {
            return key;
        }
    }

    private record ReplaceOperation<K, V>(K key, V value, long lifespan, TimeUnit timeUnit) implements Operation<K, V> {

        @Override
        public CompletionStage<?> execute(RemoteCache<K, V> cache) {
            return cache.replaceAsync(key, value, lifespan, timeUnit);
        }

        @Override
        public Operation<K, V> update(V newValue, int newLifespan, TimeUnit newTimeUnit) {
            return new ReplaceOperation<>(key, newValue, newLifespan, newTimeUnit);
        }

        @Override
        public boolean hasValue() {
            return true;
        }

        @Override
        public V getValue() {
            return value;
        }

        @Override
        public K getCacheKey() {
            return key;
        }
    }

    private record RemoveOperation<K, V>(K key) implements Operation<K, V> {

        @Override
        public CompletionStage<?> execute(RemoteCache<K, V> cache) {
            return cache.removeAsync(key);
        }

        @Override
        public K getCacheKey() {
            return key;
        }
    }

    private static final Operation<?, ?> TOMBSTONE = new Operation<>() {

        @Override
        public boolean canRemove() {
            return true;
        }

        @Override
        public Object getCacheKey() {
            return null;
        }

        @Override
        public CompletionStage<?> execute(RemoteCache<Object, Object> cache) {
            return CompletableFutures.completedNull();
        }
    };

}
