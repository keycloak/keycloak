package org.keycloak.models.sessions.infinispan.changes;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.keycloak.models.sessions.infinispan.entities.SessionEntity;

import org.infinispan.Cache;
import org.infinispan.commons.util.concurrent.CompletionStages;
import org.infinispan.functional.impl.FunctionalMapImpl;
import org.infinispan.functional.impl.ReadWriteMapImpl;

/** UUID-based conditional operations, independent of the cache's binary value encoding. */
public final class SessionCache {
    private SessionCache() {
    }

    public static <K, V extends SessionEntity> CompletableFuture<Boolean> removeAsync(
            Cache<K, SessionEntityWrapper<V>> cache, K key, SessionEntityWrapper<V> expected) {
        return ReadWriteMapImpl.create(FunctionalMapImpl.create(cache.getAdvancedCache()))
                .eval(key, new SessionCompareAndSet<>(expected.getVersion(), null, -1, -1));
    }

    public static <K, V extends SessionEntity> boolean remove(
            Cache<K, SessionEntityWrapper<V>> cache, K key, SessionEntityWrapper<V> expected) {
        return CompletionStages.join(removeAsync(cache, key, expected));
    }

    public static <K, V extends SessionEntity> CompletableFuture<Boolean> replaceAsync(
            Cache<K, SessionEntityWrapper<V>> cache, K key, SessionEntityWrapper<V> expected,
            SessionEntityWrapper<V> replacement, long lifespan, TimeUnit unit, long maxIdle, TimeUnit idleUnit) {
        return ReadWriteMapImpl.create(FunctionalMapImpl.create(cache.getAdvancedCache()))
                .eval(key, new SessionCompareAndSet<>(expected.getVersion(), replacement,
                        unit.toMillis(lifespan), idleUnit.toMillis(maxIdle)));
    }

    public static <K, V extends SessionEntity> CompletableFuture<Boolean> replaceAsync(
            Cache<K, SessionEntityWrapper<V>> cache, K key, SessionEntityWrapper<V> expected,
            SessionEntityWrapper<V> replacement, long lifespan, TimeUnit unit) {
        return replaceAsync(cache, key, expected, replacement, lifespan, unit,
                cache.getCacheConfiguration().expiration().maxIdle(), TimeUnit.MILLISECONDS);
    }

    public static <K, V extends SessionEntity> boolean replace(
            Cache<K, SessionEntityWrapper<V>> cache, K key, SessionEntityWrapper<V> expected,
            SessionEntityWrapper<V> replacement, long lifespan, TimeUnit unit, long maxIdle, TimeUnit idleUnit) {
        return CompletionStages.join(replaceAsync(cache, key, expected, replacement, lifespan, unit, maxIdle, idleUnit));
    }

    public static <K, V extends SessionEntity> boolean replace(
            Cache<K, SessionEntityWrapper<V>> cache, K key, SessionEntityWrapper<V> expected,
            SessionEntityWrapper<V> replacement, long lifespan, TimeUnit unit) {
        return CompletionStages.join(replaceAsync(cache, key, expected, replacement, lifespan, unit));
    }
}
