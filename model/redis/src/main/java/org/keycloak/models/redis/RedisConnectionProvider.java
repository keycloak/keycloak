/*
 * Copyright 2026 Capital One Financial Corporation and/or its affiliates
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

package org.keycloak.models.redis;

import org.keycloak.models.redis.RedisConnectionProvider;

import org.keycloak.provider.Provider;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

/**
 * Provider for Redis connections and operations.
 * This interface abstracts Redis operations to allow both standalone and cluster mode support.
 */
public interface RedisConnectionProvider extends Provider {

    // Cache name constants - matching Infinispan cache names for compatibility
    String USER_SESSION_CACHE_NAME = "sessions";
    String OFFLINE_USER_SESSION_CACHE_NAME = "offlineSessions";
    String CLIENT_SESSION_CACHE_NAME = "clientSessions";
    String OFFLINE_CLIENT_SESSION_CACHE_NAME = "offlineClientSessions";
    String AUTHENTICATION_SESSIONS_CACHE_NAME = "authenticationSessions";
    String LOGIN_FAILURE_CACHE_NAME = "loginFailures";
    String SINGLE_USE_OBJECT_CACHE_NAME = "actionTokens";
    String WORK_CACHE_NAME = "work";

    // Aliases for convenience
    String CACHE_AUTHENTICATION_SESSIONS = AUTHENTICATION_SESSIONS_CACHE_NAME;
    String CACHE_LOGIN_FAILURES = LOGIN_FAILURE_CACHE_NAME;

    // Default key prefix
    String DEFAULT_KEY_PREFIX = "kc:";

    /**
     * Get a value from Redis.
     *
     * @param cacheName The logical cache name
     * @param key       The key
     * @param type      The expected value type
     * @return The value or null if not found
     */
    <V> V get(String cacheName, String key, Class<V> type);

    /**
     * Async version of get.
     */
    <V> CompletionStage<V> getAsync(String cacheName, String key, Class<V> type);

    /**
     * Get a value with its version for optimistic locking.
     *
     * @param cacheName The logical cache name
     * @param key       The key
     * @param type      The expected value type
     * @return The value with version metadata, or null if not found
     */
    <V> VersionedValue<V> getWithVersion(String cacheName, String key, Class<V> type);

    /**
     * Async version of getWithVersion.
     */
    <V> CompletionStage<VersionedValue<V>> getWithVersionAsync(String cacheName, String key, Class<V> type);

    /**
     * Put a value in Redis with TTL.
     *
     * @param cacheName  The logical cache name
     * @param key        The key
     * @param value      The value
     * @param lifespan   The lifespan
     * @param unit       The time unit
     */
    <V> void put(String cacheName, String key, V value, long lifespan, TimeUnit unit);

    /**
     * Async version of put.
     */
    <V> CompletionStage<Void> putAsync(String cacheName, String key, V value, long lifespan, TimeUnit unit);

    /**
     * Put a value only if the key doesn't exist.
     *
     * @param cacheName  The logical cache name
     * @param key        The key
     * @param value      The value
     * @param lifespan   The lifespan
     * @param unit       The time unit
     * @return The previous value if key existed, null if put was successful
     */
    <V> V putIfAbsent(String cacheName, String key, V value, long lifespan, TimeUnit unit);

    /**
     * Async version of putIfAbsent.
     */
    <V> CompletionStage<V> putIfAbsentAsync(String cacheName, String key, V value, long lifespan, TimeUnit unit);

    /**
     * Replace a value only if the version matches (optimistic locking).
     *
     * @param cacheName The logical cache name
     * @param key       The key
     * @param value     The new value
     * @param version   The expected version
     * @param lifespan  The lifespan
     * @param unit      The time unit
     * @return true if replaced, false if version mismatch or key doesn't exist
     */
    <V> boolean replaceWithVersion(String cacheName, String key, V value, long version, long lifespan, TimeUnit unit);

    /**
     * Async version of replaceWithVersion.
     */
    <V> CompletionStage<Boolean> replaceWithVersionAsync(String cacheName, String key, V value, long version, long lifespan, TimeUnit unit);

    /**
     * Remove a key from Redis.
     *
     * @param cacheName The logical cache name
     * @param key       The key
     * @return The previous value, or null if key didn't exist
     */
    <V> V remove(String cacheName, String key, Class<V> type);

    /**
     * Async version of remove.
     */
    <V> CompletionStage<V> removeAsync(String cacheName, String key, Class<V> type);

    /**
     * Remove a key without returning the value.
     */
    boolean delete(String cacheName, String key);

    /**
     * Async version of delete.
     */
    CompletionStage<Boolean> deleteAsync(String cacheName, String key);

    /**
     * Check if a key exists.
     */
    boolean containsKey(String cacheName, String key);

    // ==================== Batch Operations ====================
    
    /**
     * Get multiple values in a single operation (batch GET).
     * More efficient than multiple individual gets.
     *
     * @param cacheName The logical cache name
     * @param keys      List of keys to retrieve
     * @param type      The expected value type
     * @return Map of key -> value (null values are excluded)
     */
    <V> Map<String, V> getAll(String cacheName, List<String> keys, Class<V> type);
    
    /**
     * Async version of getAll.
     */
    <V> CompletionStage<Map<String, V>> getAllAsync(String cacheName, List<String> keys, Class<V> type);
    
    /**
     * Put multiple values in a single operation (batch PUT).
     * More efficient than multiple individual puts.
     *
     * @param cacheName The logical cache name
     * @param entries   Map of key -> value to store
     * @param lifespan  The lifespan for all entries
     * @param unit      The time unit
     */
    <V> void putAll(String cacheName, Map<String, V> entries, long lifespan, TimeUnit unit);
    
    /**
     * Async version of putAll.
     */
    <V> CompletionStage<Void> putAllAsync(String cacheName, Map<String, V> entries, long lifespan, TimeUnit unit);
    
    /**
     * Delete multiple keys in a single operation (batch DELETE).
     * More efficient than multiple individual deletes.
     *
     * @param cacheName The logical cache name
     * @param keys      List of keys to delete
     * @return The number of keys actually deleted
     */
    long deleteAll(String cacheName, List<String> keys);
    
    /**
     * Async version of deleteAll.
     */
    CompletionStage<Long> deleteAllAsync(String cacheName, List<String> keys);
    
    /**
     * Remove all keys matching a pattern.
     *
     * @param cacheName The logical cache name
     * @param pattern   The pattern (e.g., "realm:*")
     * @return The number of keys removed
     */
    long removeByPattern(String cacheName, String pattern);

    /**
     * Scan for keys matching a pattern.
     * Uses Redis SCAN command for efficient key enumeration.
     *
     * @param cacheName The logical cache name
     * @param pattern   The pattern (e.g., "*" for all keys)
     * @return List of matching keys (without cache prefix)
     */
    List<String> scanKeys(String cacheName, String pattern);

    /**
     * Async version of removeByPattern.
     */
    CompletionStage<Long> removeByPatternAsync(String cacheName, String pattern);

    // ==================== Sorted Set Operations (for Indexes) ====================
    
    /**
     * Add a member to a sorted set with a score.
     * Used for maintaining session indexes (e.g., realm -> [session-ids]).
     * Automatically handles cluster mode with hash tags.
     *
     * @param cacheName The logical cache name
     * @param setKey    The sorted set key
     * @param member    The member to add (e.g., session ID)
     * @param score     The score (e.g., timestamp)
     * @param lifespan  The lifespan for the sorted set
     * @param unit      The time unit
     * @return true if added, false if already exists
     */
    boolean addToSortedSet(String cacheName, String setKey, String member, double score, long lifespan, TimeUnit unit);
    
    /**
     * Remove a member from a sorted set.
     *
     * @param cacheName The logical cache name
     * @param setKey    The sorted set key
     * @param member    The member to remove
     * @return true if removed, false if didn't exist
     */
    boolean removeFromSortedSet(String cacheName, String setKey, String member);
    
    /**
     * Get all members from a sorted set.
     * Returns members ordered by score.
     *
     * @param cacheName The logical cache name
     * @param setKey    The sorted set key
     * @return List of members (empty if set doesn't exist)
     */
    List<String> getSortedSetMembers(String cacheName, String setKey);
    
    /**
     * Get the size of a sorted set.
     *
     * @param cacheName The logical cache name
     * @param setKey    The sorted set key
     * @return The number of members in the set
     */
    long getSortedSetSize(String cacheName, String setKey);

    /**
     * Publish an event to a Redis channel.
     *
     * @param channel The channel name
     * @param message The message to publish
     */
    void publish(String channel, String message);

    /**
     * Create a dedicated Pub/Sub connection for cluster event distribution.
     * This connection should be separate from regular operations to avoid blocking.
     * 
     * @return A Pub/Sub connection object, or null if not supported
     */
    Object createPubSubConnection();

    /**
     * Get the full Redis key for a cache entry.
     */
    String getCacheKey(String cacheName, String key);

    /**
     * Check if running in cluster mode.
     */
    boolean isClusterMode();

    /**
     * Check if the connection is healthy.
     */
    boolean isHealthy();

    /**
     * Get connection info for operational monitoring.
     */
    String getConnectionInfo();
    
    // ==================== Health Check & Reconnect ====================
    
    /**
     * Attempt to reconnect to Redis.
     * Used when connection is unhealthy or after Redis restart.
     *
     * @return true if reconnection succeeded, false otherwise
     */
    boolean reconnect();
    
    /**
     * Ping Redis to check connectivity.
     *
     * @return true if ping successful, false otherwise
     */
    boolean ping();
    
    // ==================== Enhanced Metrics ====================
    
    /**
     * Get basic metrics (operations counters, cache hits/misses, errors).
     * Provides counters for operations, cache performance, and errors.
     *
     * @return Map of metric name -> value
     */
    Map<String, Long> getMetrics();
    
    /**
     * Get enhanced metrics including latency statistics.
     * Includes percentiles (p50, p95, p99) for operation latencies.
     *
     * @return Map of metric name -> value (includes latency percentiles)
     */
    Map<String, Object> getEnhancedMetrics();
    
    /**
     * Reset all metrics counters.
     * Useful for testing or periodic metric collection.
     */
    void resetMetrics();
    
    /**
     * Get connection pool and client insights.
     * Provides visibility into Lettuce's internal connection state,
     * thread pools, and configuration.
     *
     * @return Map of connection insight metrics
     */
    Map<String, Object> getConnectionInsights();

    /**
     * Value wrapper that includes version information for optimistic locking.
     */
    record VersionedValue<V>(V value, long version) {
        public boolean hasValue() {
            return value != null;
        }
    }
}
