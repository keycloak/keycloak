package org.keycloak.cache;

import java.time.Duration;
import java.util.Objects;
import java.util.function.Function;

/**
 * Configuration {@code record} that encapsulates all configuration required in order to create a {@link LocalCache}.
 *
 * @param name the name of the cache
 * @param maxSize the maximum size of the cache, or -1 if an unbounded cache is desired.
 * @param expiration the {@link Duration} to wait before entries are expired. A {@code null} value indicates no expiration.
 * @param loader an optional {@link Function} which is used to retrieve cache values on cache miss.
 * @param <K> the type of the cache Keys used for lookup
 * @param <V> the type of the cache Values to be stored
 */
public record LocalCacheConfiguration<K, V>(String name, int maxSize, Duration expiration, Function<K, V> loader) {

    public LocalCacheConfiguration {
        Objects.requireNonNull(name, "A cache name must be configured");
    }

    public boolean hasExpiration() {
        return expiration != null;
    }

    public boolean hasLoader() {
        return loader != null;
    }

    public static <K, V> Builder<K, V> builder() {
        return new Builder<>();
    }

    /**
     * A builder class to simplify the creation of {@link LocalCacheConfiguration} objects.
     *
     * @param <K> the type of the cache Keys used for lookup
     * @param <V> the type of the cache Values to be stored
     */
    public static class Builder<K, V> {

        private String name;
        private int maxSize = -1;
        private Duration expiration;
        private Function<K, V> loader;

        public Builder<K, V> name(String name) {
            this.name = name;
            return this;
        }

        public Builder<K, V> maxSize(int maxSize) {
            this.maxSize = maxSize;
            return this;
        }

        public Builder<K, V> expiration(Duration duration) {
            this.expiration = duration;
            return this;
        }

        public Builder<K, V> loader(Function<K, V> loader) {
            this.loader = loader;
            return this;
        }

        public LocalCacheConfiguration<K, V> build() {
            return new LocalCacheConfiguration<>(name, maxSize, expiration, loader);
        }
    }
}
