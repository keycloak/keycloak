package org.keycloak.cache;

import java.util.Objects;
import java.util.function.Function;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.Scheduler;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.binder.cache.CaffeineStatsCounter;

/**
 * The default implementation for {@link LocalCacheProvider} and {@link LocalCacheProviderFactory}.
 */
public class DefaultLocalCacheProviderFactory implements LocalCacheProvider, LocalCacheProviderFactory {

    @Override
    public LocalCacheProvider create(KeycloakSession session) {
        return this;
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public String getId() {
        return "default";
    }

    @Override
    public <K, V> LocalCache<K, V> create(LocalCacheConfiguration<K, V> configuration) {
        CaffeineStatsCounter metrics = new CaffeineStatsCounter(Metrics.globalRegistry, configuration.name());
        Caffeine<Object, Object> builder = Caffeine.newBuilder().recordStats(() -> metrics);

        if (configuration.maxSize() > 0) {
            builder.maximumSize(configuration.maxSize());
        }

        if (configuration.hasRemovalListener()) {
            // run the listener on the removing thread so it has run for every value when close() returns
            builder.executor(Runnable::run).<K, V>removalListener((key, value, cause) ->
                    configuration.removalListener().accept(value));
            // expire entries without waiting for the next access so their values are released
            builder.scheduler(Scheduler.systemScheduler());
        }

        switch (configuration.expirationMode()) {
            case ACCESS -> builder.expireAfter(Expiry.accessing(configuration.expiration()));
            case CREATE -> builder.expireAfter(Expiry.creating(configuration.expiration()));
        }

        if (configuration.hasLoader()) {
            LoadingCache<K, V> cache = builder.build(k -> configuration.loader().apply(k));
            metrics.registerSizeMetric(cache);
            return new LoadingCaffeineWrapper<>(cache);
        } else {
            Cache<K, V> cache = builder.build();
            metrics.registerSizeMetric(cache);
            return new CaffeineWrapper<>(cache);
        }
    }

    @Override
    public void close() {
    }

    private static class CaffeineWrapper<K, V> implements LocalCache<K, V> {

        final Cache<K, V> cache;

        CaffeineWrapper(Cache<K, V> cache) {
            this.cache = cache;
        }

        @Override
        public V get(K key) {
            Objects.requireNonNull(key);
            return cache.getIfPresent(key);
        }

        @Override
        public void put(K key, V value) {
            Objects.requireNonNull(key);
            Objects.requireNonNull(value);
            cache.put(key, value);
        }

        @Override
        public void invalidate(K key) {
            Objects.requireNonNull(key);
            cache.invalidate(key);
        }

        @Override
        public V computeIfAbsent(K key, Function<K, V> mapper) {
            Objects.requireNonNull(key);
            return cache.get(key, mapper);
        }

        @Override
        public void close() {
            cache.invalidateAll();
            cache.cleanUp();
        }
    }

    private static class LoadingCaffeineWrapper<K, V> extends CaffeineWrapper<K, V> {

        final LoadingCache<K, V> cache;

        LoadingCaffeineWrapper(LoadingCache<K, V> cache) {
            super(cache);
            this.cache = cache;
        }

        @Override
        public V get(K key) {
            Objects.requireNonNull(key);
            return cache.get(key);
        }
    }
}
