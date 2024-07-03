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
package org.keycloak.models.sessions.infinispan.changes.remote;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import org.infinispan.client.hotrod.Flag;
import org.infinispan.client.hotrod.MetadataValue;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.commons.util.concurrent.AggregateCompletionStage;
import org.infinispan.commons.util.concurrent.CompletableFutures;
import org.infinispan.commons.util.concurrent.CompletionStages;
import org.keycloak.infinispan.util.InfinispanUtils;
import org.keycloak.models.AbstractKeycloakTransaction;
import org.keycloak.models.KeycloakTransaction;
import org.keycloak.models.sessions.infinispan.changes.remote.updater.Expiration;
import org.keycloak.models.sessions.infinispan.changes.remote.updater.Updater;
import org.keycloak.models.sessions.infinispan.changes.remote.updater.UpdaterFactory;

/**
 * A {@link KeycloakTransaction} implementation that keeps track of changes made to entities stored
 * in a Infinispan cache.
 *
 * @param <K> The type of the Infinispan cache key.
 * @param <V> The type of the Infinispan cache value.
 * @param <T> The type of the {@link Updater} implementation.
 */
public class RemoteChangeLogTransaction<K, V, T extends Updater<K, V>> extends AbstractKeycloakTransaction {

    private final Map<K, T> entityChanges;
    private final UpdaterFactory<K, V, T> factory;
    private final RemoteCache<K, V> cache;
    private RemoveEntryPredicate<K, V> removePredicate = InfinispanUtils.alwaysFalse();

    public RemoteChangeLogTransaction(UpdaterFactory<K, V, T> factory, RemoteCache<K, V> cache) {
        this.factory = Objects.requireNonNull(factory);
        this.cache = Objects.requireNonNull(cache);
        entityChanges = new ConcurrentHashMap<>(8);
    }

    @Override
    protected void commitImpl() {
        var stage = CompletionStages.aggregateCompletionStage();
        doCommit(stage);
        CompletionStages.join(stage.freeze());
        entityChanges.clear();
        removePredicate = null;
    }

    @Override
    protected void rollbackImpl() {
        entityChanges.clear();
        removePredicate = null;
    }

    public void commitAsync(AggregateCompletionStage<Void> stage) {
        if (state != TransactionState.STARTED) {
            throw new IllegalStateException("Transaction in illegal state for commit: " + state);
        }

        doCommit(stage);

        state = TransactionState.FINISHED;
    }

    private void doCommit(AggregateCompletionStage<Void> stage) {
        if (removePredicate != InfinispanUtils.alwaysFalse()) {
            // TODO [pruivo] [optimization] with protostream, use delete by query: DELETE FROM ...
            var rmStage = Flowable.fromPublisher(cache.publishEntriesWithMetadata(null, 2048))
                    .filter(removePredicate::shouldRemove)
                    .map(Map.Entry::getKey)
                    .flatMapCompletable(this::removeKey)
                    .toCompletionStage(null);
            stage.dependsOn(rmStage);
        }

        for (var updater : entityChanges.values()) {
            if (updater.isReadOnly() || updater.isTransient() || removePredicate.shouldRemove(updater)) {
                continue;
            }
            if (updater.isDeleted()) {
                stage.dependsOn(cache.removeAsync(updater.getKey()));
                continue;
            }

            var expiration = updater.computeExpiration();

            if (expiration.isExpired()) {
                stage.dependsOn(cache.removeAsync(updater.getKey()));
                continue;
            }

            if (updater.isCreated()) {
                stage.dependsOn(putIfAbsent(updater, expiration));
                continue;
            }

            stage.dependsOn(replace(updater, expiration));
        }
    }

    /**
     * @return The {@link RemoteCache} tracked by the transaction.
     */
    public RemoteCache<K, V> getCache() {
        return cache;
    }

    /**
     * Fetches the value associated to the {@code key}.
     * <p>
     * It fetches the value from the {@link RemoteCache} if a copy does not exist in the transaction.
     *
     * @param key The Infinispan cache key to fetch.
     * @return The {@link Updater} to track further changes of the Infinispan cache value.
     */
    public T get(K key) {
        var updater = entityChanges.get(key);
        if (updater != null) {
            return updater.isDeleted() ? null : updater;
        }
        return onEntityFromCache(key, cache.getWithMetadata(key));
    }

    /**
     * Nonblocking alternative of {@link #get(Object)}
     *
     * @param key The Infinispan cache key to fetch.
     * @return The {@link Updater} to track further changes of the Infinispan cache value.
     */
    public CompletionStage<T> getAsync(K key) {
        var updater = entityChanges.get(key);
        if (updater != null) {
            return updater.isDeleted() ? CompletableFutures.completedNull() : CompletableFuture.completedFuture(updater);
        }
        return cache.getWithMetadataAsync(key).thenApply(e -> onEntityFromCache(key, e));
    }

    /**
     * Tracks a new value to be created in the Infinispan cache.
     *
     * @param key    The Infinispan cache key to be associated to the value.
     * @param entity The Infinispan cache value.
     * @return The {@link Updater} to track further changes of the Infinispan cache value.
     */
    public T create(K key, V entity) {
        var updater = factory.create(key, entity);
        entityChanges.put(key, updater);
        return updater;
    }

    /**
     * Removes the {@code key} from the {@link RemoteCache}.
     *
     * @param key The Infinispan cache key to remove.
     */
    public void remove(K key) {
        var updater = entityChanges.get(key);
        if (updater != null) {
            updater.markDeleted();
            return;
        }
        entityChanges.put(key, factory.deleted(key));
    }

    /**
     * Removes all Infinispan cache values that satisfy the given predicate.
     *
     * @param predicate The {@link RemoveEntryPredicate} which returns {@code true} for elements to be removed.
     */
    public void removeIf(RemoveEntryPredicate<K, V> predicate) {
        removePredicate = removePredicate.or(predicate);
    }

    public T wrap(Map.Entry<K, MetadataValue<V>> entry) {
        return entityChanges.computeIfAbsent(entry.getKey(), k -> factory.wrapFromCache(k, entry.getValue()));
    }

    private T onEntityFromCache(K key, MetadataValue<V> entity) {
        if (entity == null) {
            return null;
        }
        var updater = factory.wrapFromCache(key, entity);
        entityChanges.put(key, updater);
        return updater.isDeleted() ? null : updater;
    }

    private CompletionStage<V> putIfAbsent(Updater<K, V> updater, Expiration expiration) {
        return cache.withFlags(Flag.FORCE_RETURN_VALUE)
                .putIfAbsentAsync(updater.getKey(), updater.getValue(), expiration.lifespan(), TimeUnit.MILLISECONDS, expiration.maxIdle(), TimeUnit.MILLISECONDS)
                .thenApply(Objects::isNull)
                .thenCompose(completed -> handleResponse(completed, updater, expiration));
    }

    private CompletionStage<V> replace(Updater<K, V> updater, Expiration expiration) {
        return cache.replaceWithVersionAsync(updater.getKey(), updater.getValue(), updater.getVersionRead(), expiration.lifespan(), TimeUnit.MILLISECONDS, expiration.maxIdle(), TimeUnit.MILLISECONDS)
                .thenCompose(completed -> handleResponse(completed, updater, expiration));
    }

    private CompletionStage<V> handleResponse(boolean completed, Updater<K, V> updater, Expiration expiration) {
        return completed ? CompletableFutures.completedNull() : merge(updater, expiration);
    }

    private CompletionStage<V> merge(Updater<K, V> updater, Expiration expiration) {
        return cache.computeIfPresentAsync(updater.getKey(), updater, expiration.lifespan(), TimeUnit.MILLISECONDS, expiration.maxIdle(), TimeUnit.MILLISECONDS);
    }

    private Completable removeKey(K key) {
        return Completable.fromCompletionStage(cache.removeAsync(key));
    }

}
