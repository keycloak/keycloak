package org.keycloak.cache;

import org.keycloak.provider.Provider;

/**
 * A {@link Provider} to abstract the creation of local, non-clustered, in-memory caches from the underlying cache implementation.
 */
public interface LocalCacheProvider extends Provider {
    /**
     * Creates a new {@link LocalCache} instance for local caching. {@link LocalCacheProvider} implementations
     * are not responsible for managing the lifecycle of created {@link LocalCache} instances. It is the responsibility
     * of {@link LocalCache} consumers to ensure that {@link LocalCache#close()} is called when the cache is no longer
     * required.
     *
     * @param configuration the desired cache configuration
     * @return {@link LocalCache} a newly created cache
     * @param <K> the type of the cache Keys used for lookup
     * @param <V> the type of the cache Values to be stored
     */
    <K, V> LocalCache<K, V> create(LocalCacheConfiguration<K, V> configuration);
}
