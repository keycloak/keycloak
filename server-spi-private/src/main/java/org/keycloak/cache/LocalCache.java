package org.keycloak.cache;

/**
 * A {@link LocalCache} should be used when a local, non-clustered, cache is required to optimise data access.
 *
 * @param <K> the type of the cache Keys used for lookup
 * @param <V> the type of the cache Values to be stored
 */
public interface LocalCache<K, V> extends AutoCloseable {

    /**
     * Returns the value associated with the {@code key}, or {@code null} if there is no
     * cached value for the {@code key}.
     *
     * @param key the key whose associated value is to be returned
     * @return the value associated with the specified key or {@code null} if no value exists
     * @throws NullPointerException if the specified key is null
     */
    V get(K key);

    /**
     * Associates the value with the key in this cache.
     * If the cache previously contained a value associated with the key, the old value is replaced by the new value.
     *
     * @param key the key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @throws NullPointerException if the specified key or value is null
     */
    void put(K key, V value);

    /**
     * Removes the cached value for the specified {@code key}.
     *
     * @param key the key whose mapping is to be removed from the cache
     * @throws NullPointerException if the specified key is null
     */
    void invalidate(K key);

    /**
     * Closes all resources associated with the cache.
     */
    void close();
}
