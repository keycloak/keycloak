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

package org.keycloak.models.sessions.infinispan.remote;

import java.lang.invoke.MethodHandles;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import org.infinispan.client.hotrod.MetadataValue;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.commons.util.concurrent.AggregateCompletionStage;
import org.infinispan.commons.util.concurrent.CompletionStages;
import org.jboss.logging.Logger;
import org.keycloak.models.AbstractKeycloakTransaction;

public class RemoteInfinispanKeycloakTransaction<K, V> extends AbstractKeycloakTransaction {

    private final static Logger logger = Logger.getLogger(MethodHandles.lookup().lookupClass());

    private final Map<K, Operation<K, V>> tasks = new LinkedHashMap<>();
    private final RemoteCache<K, V> cache;
    private Predicate<V> removePredicate;

    public RemoteInfinispanKeycloakTransaction(RemoteCache<K, V> cache) {
        this.cache = Objects.requireNonNull(cache);
    }

    @Override
    protected void commitImpl() {
        AggregateCompletionStage<Void> stage = CompletionStages.aggregateCompletionStage();
        if (removePredicate != null) {
            // TODO [pruivo] [optimization] with protostream, use delete by query: DELETE FROM ...
            var rmStage = Flowable.fromPublisher(cache.publishEntriesWithMetadata(null, 2048))
                    .filter(this::shouldRemoveEntry)
                    .map(Map.Entry::getKey)
                    .flatMapCompletable(this::removeKey)
                    .toCompletionStage(null);
            stage.dependsOn(rmStage);
        }
        tasks.values().stream()
                .filter(this::shouldCommitOperation)
                .map(this::commitOperation)
                .forEach(stage::dependsOn);
        CompletionStages.join(stage.freeze());
        tasks.clear();
    }

    @Override
    protected void rollbackImpl() {
        tasks.clear();
    }

    public void put(K key, V value, long lifespan, TimeUnit timeUnit) {
        logger.tracef("Adding %s.put(%S)", cache.getName(), key);

        if (tasks.containsKey(key)) {
            throw new IllegalStateException("Can't add session: task in progress for session");
        }

        tasks.put(key, new PutOperation<>(key, value, lifespan, timeUnit));
    }

    public void replace(K key, V value, int lifespan, TimeUnit timeUnit) {
        logger.tracef("Adding %s.replace(%S)", cache.getName(), key);

        Operation<K, V> existing = tasks.get(key);
        if (existing != null) {
            if (existing.hasValue()) {
                tasks.put(key, existing.update(value, lifespan, timeUnit));
            }
            return;
        }

        tasks.put(key, new ReplaceOperation<>(key, value, lifespan, timeUnit));
    }

    public void remove(K key) {
        logger.tracef("Adding %s.remove(%S)", cache.getName(), key);

        Operation<K, V> existing = tasks.get(key);
        if (existing != null && existing.canRemove()) {
            tasks.remove(key);
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

    /**
     * Removes all Infinispan cache values that satisfy the given predicate.
     *
     * @param predicate The {@link Predicate} which returns {@code true} for elements to be removed.
     */
    public void removeIf(Predicate<V> predicate) {
        if (removePredicate == null) {
            removePredicate = predicate;
            return;
        }
        removePredicate = removePredicate.or(predicate);
    }

    private Completable removeKey(K key) {
        return Completable.fromCompletionStage(cache.removeAsync(key));
    }

    private boolean shouldCommitOperation(Operation<K, V> operation) {
        // Commit if any:
        // 1. it is a removal operation (no value to test the predicate).
        // 2. remove predicate is not present.
        // 3. value does not match the remove predicate.
        return !operation.hasValue() ||
                removePredicate == null ||
                !removePredicate.test(operation.getValue());
    }

    private boolean shouldRemoveEntry(Map.Entry<K, MetadataValue<V>> entry) {
        // invoked by stream, so removePredicate is not null
        assert removePredicate != null;
        return removePredicate.test(entry.getValue().getValue());
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
    }

    private record RemoveOperation<K, V>(K key) implements Operation<K, V> {

        @Override
        public CompletionStage<?> execute(RemoteCache<K, V> cache) {
            return cache.removeAsync(key);
        }
    }
}
