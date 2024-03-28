package org.keycloak.models.sessions.infinispan.remote;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.util.concurrent.AggregateCompletionStage;
import org.infinispan.util.concurrent.CompletionStages;
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakTransaction;

import java.lang.invoke.MethodHandles;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class RemoteInfinispanKeycloakTransaction<K, V> implements KeycloakTransaction {

    private final static Logger logger = Logger.getLogger(MethodHandles.lookup().lookupClass());

    private boolean active;
    private boolean rollback;
    private final Map<K, Operation<K, V>> tasks = new LinkedHashMap<>();
    private final RemoteCache<K, V> cache;

    public RemoteInfinispanKeycloakTransaction(RemoteCache<K, V> cache) {
        this.cache = Objects.requireNonNull(cache);
    }

    @Override
    public void begin() {
        active = true;
        tasks.clear();
    }

    @Override
    public void commit() {
        active = false;
        if (rollback) {
            throw new RuntimeException("Rollback only!");
        }
        AggregateCompletionStage<Void> stage = CompletionStages.aggregateCompletionStage();
        tasks.values().stream()
                .map(this::commitOperation)
                .forEach(stage::dependsOn);
        try {
            CompletionStages.await(stage.freeze());
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    @Override
    public void rollback() {
        active = false;
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

    public void put(K key, V value, int lifespan, TimeUnit timeUnit) {
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

    private record PutOperation<K, V>(K key, V value, int lifespan, TimeUnit timeUnit) implements Operation<K, V> {

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

    private record ReplaceOperation<K, V>(K key, V value, int lifespan, TimeUnit timeUnit) implements Operation<K, V> {

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
