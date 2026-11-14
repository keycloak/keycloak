package org.keycloak.cache;

import java.time.Duration;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Configuration {@code record} that encapsulates all configuration required in order to create a {@link LocalCache}.
 *
 * @param name the name of the cache
 * @param maxSize the maximum size of the cache, or -1 if an unbounded cache is desired.
 * @param expiration the {@link Duration} to wait before entries are expired. A {@code null} value indicates no expiration.
 * @param loader an optional {@link Function} which is used to retrieve cache values on cache miss.
 * @param expirationMode the expiration mode
 * @param removalListener an optional {@link Consumer} which is called with the value of every entry that is removed.
 * @param <K> the type of the cache Keys used for lookup
 * @param <V> the type of the cache Values to be stored
 */
public record LocalCacheConfiguration<K, V>(String name, int maxSize, BiFunction<K, V, Duration> expiration,
                                            Function<K, V> loader, ExpirationMode expirationMode,
                                            Consumer<V> removalListener) {

    public LocalCacheConfiguration {
        Objects.requireNonNull(name, "A cache name must be configured");
        Objects.requireNonNull(expirationMode, "A cache expiration mode must be configured");
    }

    public boolean hasLoader() {
        return loader != null;
    }

    public boolean hasRemovalListener() {
        return removalListener != null;
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
        private BiFunction<K, V, Duration> expiration;
        private Function<K, V> loader;
        private ExpirationMode expirationMode = ExpirationMode.DISABLED;
        private Consumer<V> removalListener;

        public Builder<K, V> name(String name) {
            this.name = name;
            return this;
        }

        public Builder<K, V> maxSize(int maxSize) {
            this.maxSize = maxSize;
            return this;
        }

        /**
         * @deprecated use {@link #expirationAfterAccess(Duration)} instead
         */
        @Deprecated(since = "26.7", forRemoval = true)
        public Builder<K, V> expiration(Duration duration) {
            Objects.requireNonNull(duration, "A duration must be configured");
            return expirationAfterAccess((k, v) -> duration);
        }

        public Builder<K, V> expirationAfterAccess(Duration duration) {
            Objects.requireNonNull(duration, "A duration must be configured");
            return expirationAfterAccess((k, v) -> duration);
        }

        public Builder<K, V> expirationAfterAccess(BiFunction<K, V, Duration> variableDuration) {
            this.expiration = Objects.requireNonNull(variableDuration, "Variable duration must not be null");
            this.expirationMode = ExpirationMode.ACCESS;
            return this;
        }

        public Builder<K, V> expirationAfterCreate(Duration duration) {
            Objects.requireNonNull(duration, "A duration must be configured");
            return expirationAfterAccess((k, v) -> duration);
        }

        public Builder<K, V> expirationAfterCreate(BiFunction<K, V, Duration> variableDuration) {
            this.expiration = Objects.requireNonNull(variableDuration, "Variable duration must not be null");
            this.expirationMode = ExpirationMode.CREATE;
            return this;
        }

        public Builder<K, V> loader(Function<K, V> loader) {
            this.loader = loader;
            return this;
        }

        /**
         * Sets a listener called with the value of every removed entry, including when the cache is closed, to release
         * values that hold resources.
         */
        public Builder<K, V> removalListener(Consumer<V> removalListener) {
            this.removalListener = removalListener;
            return this;
        }

        public LocalCacheConfiguration<K, V> build() {
            return new LocalCacheConfiguration<>(name, maxSize, expiration, loader, expirationMode, removalListener);
        }
    }

    public enum ExpirationMode {
        DISABLED,
        CREATE,
        ACCESS
    }
}
