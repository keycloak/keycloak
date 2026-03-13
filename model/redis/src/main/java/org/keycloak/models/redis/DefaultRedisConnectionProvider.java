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

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.SetArgs;
import io.lettuce.core.ScanArgs;
import io.lettuce.core.KeyScanCursor;
import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.SlotHash;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import org.HdrHistogram.Histogram;
import org.HdrHistogram.Recorder;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ConcurrentHashMap;
public class DefaultRedisConnectionProvider implements RedisConnectionProvider {

    private static final Logger logger = Logger.getLogger(DefaultRedisConnectionProvider.class);

    private final RedisClient standaloneClient;
    private final RedisClusterClient clusterClient;
    private final StatefulRedisConnection<String, String> standaloneConnection;
    private final StatefulRedisClusterConnection<String, String> clusterConnection;
    private final RedisSerializer serializer;
    private final String keyPrefix;
    private final boolean clusterMode;
    private final String connectionInfo;
    private final List<StatefulRedisPubSubConnection<String, String>> pubSubConnections = new CopyOnWriteArrayList<>();
    
    // Metrics tracking
    private final AtomicLong getOperations = new AtomicLong(0);
    private final AtomicLong putOperations = new AtomicLong(0);
    private final AtomicLong deleteOperations = new AtomicLong(0);
    private final AtomicLong scanOperations = new AtomicLong(0);
    private final AtomicLong cacheHits = new AtomicLong(0);
    private final AtomicLong cacheMisses = new AtomicLong(0);
    private final AtomicLong errorCount = new AtomicLong(0);
    
    // Enhanced metrics with latency tracking using HdrHistogram for efficiency
    private final ConcurrentHashMap<String, Recorder> latencyRecorders = new ConcurrentHashMap<>();
    private final AtomicLong batchGetOperations = new AtomicLong(0);
    private final AtomicLong batchPutOperations = new AtomicLong(0);
    private final AtomicLong batchDeleteOperations = new AtomicLong(0);
    
    // Health tracking
    private volatile boolean healthy = true;
    private volatile long lastHealthCheck = System.currentTimeMillis();

    // Lua script for atomic compare-and-set with version
    // Executes atomically in both standalone and cluster modes (with hash tags)
    private static final String CAS_SCRIPT =
            "local current_version = redis.call('GET', KEYS[2]) " +
            "if current_version == ARGV[1] or current_version == false then " +
            "  redis.call('PSETEX', KEYS[1], ARGV[3], ARGV[2]) " +
            "  redis.call('SET', KEYS[2], tonumber(ARGV[1]) + 1) " +
            "  redis.call('PEXPIRE', KEYS[2], ARGV[3]) " +
            "  return 1 " +
            "else " +
            "  return 0 " +
            "end";
    
    // Lua script for bulk delete to prevent timeouts with large key sets
    // Processes keys in the script rather than a single DEL command
    private static final String BULK_DELETE_SCRIPT =
            "local deleted = 0 " +
            "for i, key in ipairs(KEYS) do " +
            "    deleted = deleted + redis.call('DEL', key) " +
            "end " +
            "return deleted";
    
    // Chunk size for bulk operations to prevent Redis timeouts
    private static final int BULK_DELETE_CHUNK_SIZE = 1000;
    
    // Metric key constants
    private static final String METRIC_CACHE_HIT_RATE = "cache.hitRate";
    private static final String METRIC_LATENCY_PREFIX = "latency.";

    public DefaultRedisConnectionProvider(RedisClient client, StatefulRedisConnection<String, String> conn, String prefix, String info) {
        this.standaloneClient = Objects.requireNonNull(client);
        this.clusterClient = null;
        this.standaloneConnection = Objects.requireNonNull(conn);
        this.clusterConnection = null;
        this.serializer = RedisSerializer.getInstance();
        this.keyPrefix = prefix != null ? prefix : DEFAULT_KEY_PREFIX;
        this.clusterMode = false;
        this.connectionInfo = info;
    }

    public DefaultRedisConnectionProvider(RedisClusterClient client, StatefulRedisClusterConnection<String, String> conn, String prefix, String info) {
        this.standaloneClient = null;
        this.clusterClient = Objects.requireNonNull(client);
        this.standaloneConnection = null;
        this.clusterConnection = Objects.requireNonNull(conn);
        this.serializer = RedisSerializer.getInstance();
        this.keyPrefix = prefix != null ? prefix : DEFAULT_KEY_PREFIX;
        this.clusterMode = true;
        this.connectionInfo = info;
    }

    @Override
    public <V> V get(String cacheName, String key, Class<V> type) {
        long start = System.nanoTime();
        getOperations.incrementAndGet();
        
        // Structured logging context
        if (logger.isTraceEnabled()) {
            logger.tracef("Redis GET: cacheName=%s, key=%s, type=%s", cacheName, key, type.getSimpleName());
        }
        
        try {
            String fullKey = getCacheKey(cacheName, key);
            String json = clusterMode ? clusterConnection.sync().get(fullKey) : standaloneConnection.sync().get(fullKey);
            if (json != null) {
                cacheHits.incrementAndGet();
                if (logger.isTraceEnabled()) {
                    logger.tracef("Redis GET hit: cacheName=%s, key=%s", cacheName, key);
                }
            } else {
                cacheMisses.incrementAndGet();
                if (logger.isTraceEnabled()) {
                    logger.tracef("Redis GET miss: cacheName=%s, key=%s", cacheName, key);
                }
            }
            V result = deserialize(json, type);
            long latency = System.nanoTime() - start;
            recordLatency("get", latency);
            
            if (logger.isTraceEnabled()) {
                logger.tracef("Redis GET complete: cacheName=%s, key=%s, latencyMs=%.2f, hit=%s", 
                    cacheName, key, latency / 1_000_000.0, (json != null));
            }
            
            return result;
        } catch (Exception e) {
            errorCount.incrementAndGet();
            logger.errorf(e, "Redis GET error: cacheName=%s, key=%s, error=%s", cacheName, key, e.getMessage());
            throw e;
        }
    }

    @Override
    public <V> CompletionStage<V> getAsync(String cacheName, String key, Class<V> type) {
        String fullKey = getCacheKey(cacheName, key);
        var future = clusterMode ? clusterConnection.async().get(fullKey) : standaloneConnection.async().get(fullKey);
        return future.thenApply(json -> deserialize(json, type)).toCompletableFuture();
    }

    @Override
    public <V> VersionedValue<V> getWithVersion(String cacheName, String key, Class<V> type) {
        String fullKey = getCacheKey(cacheName, key);
        String versionKey = keyPrefix + cacheName + ":_ver:" + key;
        String json = clusterMode ? clusterConnection.sync().get(fullKey) : standaloneConnection.sync().get(fullKey);
        if (json == null) return null;
        String vs = clusterMode ? clusterConnection.sync().get(versionKey) : standaloneConnection.sync().get(versionKey);
        long ver = vs != null ? Long.parseLong(vs) : 0L;
        return new VersionedValue<>(deserialize(json, type), ver);
    }

    @Override
    public <V> CompletionStage<VersionedValue<V>> getWithVersionAsync(String cacheName, String key, Class<V> type) {
        return getAsync(cacheName, key, type).thenApply(v -> v != null ? new VersionedValue<>(v, 0L) : null);
    }

    @Override
    public <V> void put(String cacheName, String key, V value, long lifespan, TimeUnit unit) {
        long start = System.nanoTime();
        putOperations.incrementAndGet();
        try {
            String fullKey = getCacheKey(cacheName, key);
            String json = serialize(value);
            if (lifespan == -1) {
                // No expiration
                if (clusterMode) clusterConnection.sync().set(fullKey, json);
                else standaloneConnection.sync().set(fullKey, json);
            } else {
                long ms = unit.toMillis(lifespan);
                if (clusterMode) clusterConnection.sync().psetex(fullKey, ms, json);
                else standaloneConnection.sync().psetex(fullKey, ms, json);
            }
            recordLatency("put", System.nanoTime() - start);
        } catch (Exception e) {
            errorCount.incrementAndGet();
            throw e;
        }
    }

    @Override
    public <V> CompletionStage<Void> putAsync(String cacheName, String key, V value, long lifespan, TimeUnit unit) {
        String fullKey = getCacheKey(cacheName, key);
        String json = serialize(value);
        RedisFuture<?> f;
        if (lifespan == -1) {
            // No expiration
            f = clusterMode ? clusterConnection.async().set(fullKey, json) : standaloneConnection.async().set(fullKey, json);
        } else {
            long ms = unit.toMillis(lifespan);
            f = clusterMode ? clusterConnection.async().psetex(fullKey, ms, json) : standaloneConnection.async().psetex(fullKey, ms, json);
        }
        return f.thenApply(ok -> (Void) null).toCompletableFuture();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <V> V putIfAbsent(String cacheName, String key, V value, long lifespan, TimeUnit unit) {
        String fullKey = getCacheKey(cacheName, key);
        String json = serialize(value);
        SetArgs args = lifespan == -1 ? SetArgs.Builder.nx() : SetArgs.Builder.nx().px(unit.toMillis(lifespan));
        String result = clusterMode ? clusterConnection.sync().set(fullKey, json, args) : standaloneConnection.sync().set(fullKey, json, args);
        if (result != null) return null;
        String existing = clusterMode ? clusterConnection.sync().get(fullKey) : standaloneConnection.sync().get(fullKey);
        return deserialize(existing, (Class<V>) value.getClass());
    }

    @Override
    public <V> CompletionStage<V> putIfAbsentAsync(String cacheName, String key, V value, long lifespan, TimeUnit unit) {
        return CompletableFuture.supplyAsync(() -> putIfAbsent(cacheName, key, value, lifespan, unit));
    }

    @Override
    public <V> boolean replaceWithVersion(String cacheName, String key, V value, long version, long lifespan, TimeUnit unit) {
        String fullKey = getCacheKey(cacheName, key);
        String versionKey = getVersionKey(cacheName, key);
        String json = serialize(value);
        long ms = unit.toMillis(lifespan);

        try {
            // Execute Lua script for atomic compare-and-set
            // Works atomically in both standalone and cluster modes (with hash tags)
            Long result = executeScript(CAS_SCRIPT,
                    new String[]{fullKey, versionKey},
                    new String[]{String.valueOf(version), json, String.valueOf(ms)});

            boolean success = result != null && result == 1;

            if (!success && logger.isDebugEnabled()) {
                logger.debugf("Optimistic lock failure for key %s (expected version %d)", key, version);
            }

            return success;
        } catch (Exception e) {
            logger.errorf(e, "Failed to execute replaceWithVersion for key %s", key);
            return false;
        }
    }

    @Override
    public <V> CompletionStage<Boolean> replaceWithVersionAsync(String cacheName, String key, V value, long version, long lifespan, TimeUnit unit) {
        return CompletableFuture.supplyAsync(() -> replaceWithVersion(cacheName, key, value, version, lifespan, unit));
    }

    @Override
    public <V> V remove(String cacheName, String key, Class<V> type) {
        String fullKey = getCacheKey(cacheName, key);
        String json = clusterMode ? clusterConnection.sync().get(fullKey) : standaloneConnection.sync().get(fullKey);
        V prev = deserialize(json, type);
        if (clusterMode) clusterConnection.sync().del(fullKey);
        else standaloneConnection.sync().del(fullKey);
        return prev;
    }

    @Override
    public <V> CompletionStage<V> removeAsync(String cacheName, String key, Class<V> type) {
        return CompletableFuture.supplyAsync(() -> remove(cacheName, key, type));
    }

    @Override
    public boolean delete(String cacheName, String key) {
        long start = System.nanoTime();
        deleteOperations.incrementAndGet();
        try {
            String fullKey = getCacheKey(cacheName, key);
            Long del = clusterMode ? clusterConnection.sync().del(fullKey) : standaloneConnection.sync().del(fullKey);
            boolean result = del != null && del > 0;
            recordLatency("delete", System.nanoTime() - start);
            return result;
        } catch (Exception e) {
            errorCount.incrementAndGet();
            throw e;
        }
    }

    @Override
    public CompletionStage<Boolean> deleteAsync(String cacheName, String key) {
        return CompletableFuture.supplyAsync(() -> delete(cacheName, key));
    }

    @Override
    public boolean containsKey(String cacheName, String key) {
        String fullKey = getCacheKey(cacheName, key);
        Long ex = clusterMode ? clusterConnection.sync().exists(fullKey) : standaloneConnection.sync().exists(fullKey);
        return ex != null && ex > 0;
    }

    @Override
    public long removeByPattern(String cacheName, String pattern) {
        try {
            List<String> keys = scanKeys(cacheName, pattern);
            
            if (keys.isEmpty()) {
                return 0;
            }
            
            // Convert to full keys and delete in batch
            String[] fullKeys = keys.stream()
                    .map(key -> getCacheKey(cacheName, key))
                    .toArray(String[]::new);
            
            Long deleted = clusterMode ? 
                    clusterConnection.sync().del(fullKeys) : 
                    standaloneConnection.sync().del(fullKeys);
            
            if (logger.isDebugEnabled()) {
                logger.debugf("Removed %d keys matching pattern %s in cache %s", deleted, pattern, cacheName);
            }
            
            return deleted != null ? deleted : 0;
        } catch (Exception e) {
            logger.errorf(e, "Failed to remove keys by pattern %s in cache %s", pattern, cacheName);
            return 0;
        }
    }

    @Override
    public List<String> scanKeys(String cacheName, String pattern) {
        scanOperations.incrementAndGet();
        String fullPattern = keyPrefix + cacheName + ":" + pattern;
        
        try {
            List<String> rawKeys = clusterMode ? 
                scanClusterNodes(fullPattern) : 
                scanStandaloneNode(fullPattern);
            
            return cleanKeyNames(rawKeys, cacheName);
        } catch (Exception e) {
            logger.errorf(e, "Failed to scan keys with pattern %s", fullPattern);
            return new ArrayList<>();
        }
    }
    
    private List<String> scanClusterNodes(String fullPattern) {
        if (logger.isDebugEnabled()) {
            logger.debugf("Scanning all cluster nodes for pattern: %s", fullPattern);
        }
        
        List<String> keys = new ArrayList<>();
        clusterConnection.getPartitions().forEach(partition -> {
            if (partition.getRole().isUpstream()) {
                scanSingleClusterNode(partition, fullPattern, keys);
            }
        });
        
        if (logger.isDebugEnabled()) {
            logger.debugf("Cluster scan completed: found %d keys across all master nodes", keys.size());
        }
        
        return keys;
    }
    
    private void scanSingleClusterNode(
            io.lettuce.core.cluster.models.partitions.RedisClusterNode partition,
            String fullPattern,
            List<String> keys) {
        try {
            var nodeConnection = clusterConnection.getConnection(partition.getNodeId());
            if (nodeConnection != null) {
                KeyScanCursor<String> cursor = nodeConnection.sync().scan(ScanArgs.Builder.matches(fullPattern));
                keys.addAll(cursor.getKeys());
                
                while (!cursor.isFinished()) {
                    cursor = nodeConnection.sync().scan(cursor, ScanArgs.Builder.matches(fullPattern));
                    keys.addAll(cursor.getKeys());
                }
                
                logger.debugf("Scanned node %s, found %d keys", partition.getNodeId(), cursor.getKeys().size());
            }
        } catch (Exception e) {
            logger.warnf(e, "Error scanning cluster node %s", partition.getNodeId());
            errorCount.incrementAndGet();
        }
    }
    
    private List<String> scanStandaloneNode(String fullPattern) {
        List<String> keys = new ArrayList<>();
        KeyScanCursor<String> cursor = standaloneConnection.sync().scan(ScanArgs.Builder.matches(fullPattern));
        keys.addAll(cursor.getKeys());
        
        while (!cursor.isFinished()) {
            cursor = standaloneConnection.sync().scan(cursor, ScanArgs.Builder.matches(fullPattern));
            keys.addAll(cursor.getKeys());
        }
        
        return keys;
    }
    
    private List<String> cleanKeyNames(List<String> rawKeys, String cacheName) {
        String prefix = keyPrefix + cacheName + ":";
        return rawKeys.stream()
                .filter(key -> key.startsWith(prefix))
                .map(key -> key.substring(prefix.length()))
                .toList();
    }

    @Override
    public CompletionStage<Long> removeByPatternAsync(String cacheName, String pattern) {
        return CompletableFuture.supplyAsync(() -> removeByPattern(cacheName, pattern));
    }

    // ==================== Sorted Set Operations ====================
    
    @Override
    public boolean addToSortedSet(String cacheName, String setKey, String member, double score, long lifespan, TimeUnit unit) {
        String fullKey = getCacheKey(cacheName, setKey);
        long lifespanMs = unit.toMillis(lifespan);
        
        try {
            long startTime = System.nanoTime();
            
            if (clusterMode) {
                // Add member to sorted set
                Long added = clusterConnection.sync().zadd(fullKey, score, member);
                // Set expiration on the sorted set
                clusterConnection.sync().pexpire(fullKey, lifespanMs);
                
                recordLatency("addToSortedSet", System.nanoTime() - startTime);
                return added != null && added > 0;
            } else {
                Long added = standaloneConnection.sync().zadd(fullKey, score, member);
                standaloneConnection.sync().pexpire(fullKey, lifespanMs);
                
                recordLatency("addToSortedSet", System.nanoTime() - startTime);
                return added != null && added > 0;
            }
        } catch (Exception e) {
            errorCount.incrementAndGet();
            logger.errorf(e, "Failed to add to sorted set %s", fullKey);
            return false;
        }
    }
    
    @Override
    public boolean removeFromSortedSet(String cacheName, String setKey, String member) {
        String fullKey = getCacheKey(cacheName, setKey);
        
        try {
            long startTime = System.nanoTime();
            
            if (clusterMode) {
                Long removed = clusterConnection.sync().zrem(fullKey, member);
                recordLatency("removeFromSortedSet", System.nanoTime() - startTime);
                return removed != null && removed > 0;
            } else {
                Long removed = standaloneConnection.sync().zrem(fullKey, member);
                recordLatency("removeFromSortedSet", System.nanoTime() - startTime);
                return removed != null && removed > 0;
            }
        } catch (Exception e) {
            errorCount.incrementAndGet();
            logger.errorf(e, "Failed to remove from sorted set %s", fullKey);
            return false;
        }
    }
    
    @Override
    public List<String> getSortedSetMembers(String cacheName, String setKey) {
        String fullKey = getCacheKey(cacheName, setKey);
        
        try {
            long startTime = System.nanoTime();
            
            logger.debugf("getSortedSetMembers: cacheName=%s, setKey=%s, fullKey=%s, clusterMode=%s",
                    cacheName, setKey, fullKey, clusterMode);
            
            List<String> members;
            if (clusterMode) {
                members = clusterConnection.sync().zrange(fullKey, 0, -1);
            } else {
                members = standaloneConnection.sync().zrange(fullKey, 0, -1);
            }
            
            logger.debugf("getSortedSetMembers result: fullKey=%s, size=%d",
                    fullKey, members != null ? members.size() : 0);
            
            recordLatency("getSortedSetMembers", System.nanoTime() - startTime);
            return members != null ? members : new ArrayList<>();
        } catch (Exception e) {
            errorCount.incrementAndGet();
            logger.errorf(e, "Failed to get sorted set members from %s", fullKey);
            return new ArrayList<>();
        }
    }
    
    @Override
    public long getSortedSetSize(String cacheName, String setKey) {
        String fullKey = getCacheKey(cacheName, setKey);
        
        try {
            long startTime = System.nanoTime();
            
            Long size;
            if (clusterMode) {
                size = clusterConnection.sync().zcard(fullKey);
            } else {
                size = standaloneConnection.sync().zcard(fullKey);
            }
            
            recordLatency("getSortedSetSize", System.nanoTime() - startTime);
            return size != null ? size : 0;
        } catch (Exception e) {
            errorCount.incrementAndGet();
            logger.errorf(e, "Failed to get sorted set size from %s", fullKey);
            return 0;
        }
    }

    @Override
    public void publish(String channel, String message) {
        if (clusterMode) clusterConnection.sync().publish(channel, message);
        else standaloneConnection.sync().publish(channel, message);
    }

    @Override
    public Object createPubSubConnection() {
        try {
            StatefulRedisPubSubConnection<String, String> pubSubConn;
            
            if (clusterMode) {
                if (clusterClient == null) {
                    logger.warnf("Cannot create Pub/Sub connection - cluster client not available");
                    return null;
                }
                pubSubConn = clusterClient.connectPubSub();
                logger.debugf("Created Redis cluster Pub/Sub connection (total connections: %d)", pubSubConnections.size() + 1);
            } else {
                if (standaloneClient == null) {
                    logger.warnf("Cannot create Pub/Sub connection - standalone client not available");
                    return null;
                }
                pubSubConn = standaloneClient.connectPubSub();
                logger.debugf("Created Redis standalone Pub/Sub connection (total connections: %d)", pubSubConnections.size() + 1);
            }
            
            // Track connection for cleanup
            pubSubConnections.add(pubSubConn);
            
            if (logger.isDebugEnabled()) {
                logger.debugf("Active Pub/Sub connections: %d", pubSubConnections.size());
            }
            
            return pubSubConn;
        } catch (Exception e) {
            logger.errorf(e, "Failed to create Redis Pub/Sub connection");
            return null;
        }
    }

    @Override
    public String getCacheKey(String cacheName, String key) {
        // Use hash tags in cluster mode to ensure related keys hash to the same slot
        // This is critical for Lua scripts to work atomically in cluster mode
        if (clusterMode) {
            return keyPrefix + cacheName + ":{" + key + "}";
        }
        return keyPrefix + cacheName + ":" + key;
    }

    /**
     * Get the version key for optimistic locking.
     * Uses hash tags in cluster mode to ensure version key is on same slot as data key.
     */
    private String getVersionKey(String cacheName, String key) {
        if (clusterMode) {
            // Hash tag ensures version key is on same slot as data key
            return keyPrefix + cacheName + ":{" + key + "}:_ver";
        }
        return keyPrefix + cacheName + ":_ver:" + key;
    }

    @Override
    public boolean isClusterMode() { return clusterMode; }

    @Override
    public boolean isHealthy() {
        try {
            String pong = clusterMode ? clusterConnection.sync().ping() : standaloneConnection.sync().ping();
            return "PONG".equalsIgnoreCase(pong);
        } catch (Exception e) { return false; }
    }

    @Override
    public String getConnectionInfo() { return connectionInfo; }

    /**
     * Get operational metrics for monitoring and observability.
     * Provides basic metrics tracking for operations, cache performance, and errors.
     * 
     * @return Map of metric names to values
     */
    public java.util.Map<String, Long> getMetrics() {
        java.util.Map<String, Long> metrics = new java.util.LinkedHashMap<>();
        metrics.put("operations.get", getOperations.get());
        metrics.put("operations.put", putOperations.get());
        metrics.put("operations.delete", deleteOperations.get());
        metrics.put("operations.scan", scanOperations.get());
        metrics.put("cache.hits", cacheHits.get());
        metrics.put("cache.misses", cacheMisses.get());
        metrics.put("errors.total", errorCount.get());
        
        // Calculate hit rate
        long totalGets = getOperations.get();
        if (totalGets > 0) {
            long hitRate = (cacheHits.get() * 100) / totalGets;
            metrics.put(METRIC_CACHE_HIT_RATE, hitRate);
        } else {
            metrics.put(METRIC_CACHE_HIT_RATE, 0L);
        }
        
        return metrics;
    }

    // ==================== Batch Operations ====================
    
    @Override
    public <V> Map<String, V> getAll(String cacheName, List<String> keys, Class<V> type) {
        long start = System.nanoTime();
        batchGetOperations.incrementAndGet();
        
        try {
            if (keys == null || keys.isEmpty()) {
                return new HashMap<>();
            }
            
            Map<String, V> resultMap = new HashMap<>();
            
            if (clusterMode) {
                // Cluster mode: Group keys by slot to avoid CROSSSLOT errors
                Map<Integer, List<String>> keysBySlot = groupKeysBySlot(cacheName, keys);
                
                for (Map.Entry<Integer, List<String>> entry : keysBySlot.entrySet()) {
                    List<String> slotKeys = entry.getValue();
                    String[] fullKeys = slotKeys.stream()
                        .map(k -> getCacheKey(cacheName, k))
                        .toArray(String[]::new);
                    
                    List<io.lettuce.core.KeyValue<String, String>> results = 
                        clusterConnection.sync().mget(fullKeys);
                    
                    processGetAllResults(slotKeys, results, resultMap, type);
                }
            } else {
                // Standalone mode: Single MGET call
                String[] fullKeys = keys.stream()
                    .map(k -> getCacheKey(cacheName, k))
                    .toArray(String[]::new);
                
                List<io.lettuce.core.KeyValue<String, String>> results = 
                    standaloneConnection.sync().mget(fullKeys);
                
                processGetAllResults(keys, results, resultMap, type);
            }
            
            recordLatency("getAll", System.nanoTime() - start);
            return resultMap;
        } catch (Exception e) {
            errorCount.incrementAndGet();
            logger.errorf(e, "Failed to getAll from cache %s", cacheName);
            throw e;
        }
    }
    
    /**
     * Process results from MGET command and populate result map.
     */
    private <V> void processGetAllResults(List<String> keys, 
                                           List<io.lettuce.core.KeyValue<String, String>> results,
                                           Map<String, V> resultMap,
                                           Class<V> type) {
        for (int i = 0; i < results.size(); i++) {
            io.lettuce.core.KeyValue<String, String> kv = results.get(i);
            if (kv.hasValue()) {
                String originalKey = keys.get(i);
                V value = deserialize(kv.getValue(), type);
                if (value != null) {
                    resultMap.put(originalKey, value);
                    cacheHits.incrementAndGet();
                } else {
                    cacheMisses.incrementAndGet();
                }
            } else {
                cacheMisses.incrementAndGet();
            }
        }
    }
    
    /**
     * Group keys by Redis Cluster slot to enable efficient batching.
     * Keys in the same slot can be fetched together without CROSSSLOT errors.
     */
    private Map<Integer, List<String>> groupKeysBySlot(String cacheName, List<String> keys) {
        Map<Integer, List<String>> keysBySlot = new HashMap<>();
        
        for (String key : keys) {
            String fullKey = getCacheKey(cacheName, key);
            int slot = SlotHash.getSlot(fullKey);
            keysBySlot.computeIfAbsent(slot, k -> new ArrayList<>()).add(key);
        }
        
        return keysBySlot;
    }
    
    @Override
    public <V> CompletionStage<Map<String, V>> getAllAsync(String cacheName, List<String> keys, Class<V> type) {
        return CompletableFuture.supplyAsync(() -> getAll(cacheName, keys, type));
    }
    
    @Override
    public <V> void putAll(String cacheName, Map<String, V> entries, long lifespan, TimeUnit unit) {
        long start = System.nanoTime();
        batchPutOperations.incrementAndGet();
        
        try {
            if (entries == null || entries.isEmpty()) {
                return;
            }
            
            long lifespanMs = TimeUnit.MILLISECONDS.convert(lifespan, unit);
            
            // Use pipeline for efficiency
            if (clusterMode) {
                entries.forEach((key, value) -> {
                    String fullKey = getCacheKey(cacheName, key);
                    String json = serialize(value);
                    clusterConnection.sync().psetex(fullKey, lifespanMs, json);
                });
            } else {
                var async = standaloneConnection.async();
                async.setAutoFlushCommands(false);
                
                entries.forEach((key, value) -> {
                    String fullKey = getCacheKey(cacheName, key);
                    String json = serialize(value);
                    async.psetex(fullKey, lifespanMs, json);
                });
                
                async.flushCommands();
                async.setAutoFlushCommands(true);
            }
            
            recordLatency("putAll", System.nanoTime() - start);
        } catch (Exception e) {
            errorCount.incrementAndGet();
            logger.errorf(e, "Failed to putAll to cache %s", cacheName);
            throw e;
        }
    }
    
    @Override
    public <V> CompletionStage<Void> putAllAsync(String cacheName, Map<String, V> entries, long lifespan, TimeUnit unit) {
        return CompletableFuture.runAsync(() -> putAll(cacheName, entries, lifespan, unit));
    }
    
    @Override
    public long deleteAll(String cacheName, List<String> keys) {
        long start = System.nanoTime();
        batchDeleteOperations.incrementAndGet();
        
        if (logger.isDebugEnabled()) {
            logger.debugf("Redis DELETEALL: cacheName=%s, keyCount=%d", cacheName, keys != null ? keys.size() : 0);
        }
        
        try {
            if (keys == null || keys.isEmpty()) {
                return 0;
            }
            
            long totalDeleted = 0;
            
            // Convert to full keys
            List<String> fullKeys = keys.stream()
                .map(k -> getCacheKey(cacheName, k))
                .collect(java.util.stream.Collectors.toList());
            
            // Process in chunks to prevent Redis timeouts
            for (int i = 0; i < fullKeys.size(); i += BULK_DELETE_CHUNK_SIZE) {
                int end = Math.min(i + BULK_DELETE_CHUNK_SIZE, fullKeys.size());
                List<String> chunk = fullKeys.subList(i, end);
                
                // Use Lua script for atomic deletion within chunk
                String[] chunkArray = chunk.toArray(new String[0]);
                Long deleted = executeScript(BULK_DELETE_SCRIPT, chunkArray, new String[0]);
                
                totalDeleted += (deleted != null ? deleted : 0);
                
                if (logger.isTraceEnabled()) {
                    logger.tracef("Redis DELETEALL chunk: cacheName=%s, chunkSize=%d, deleted=%d", 
                        cacheName, chunk.size(), deleted);
                }
            }
            
            long latency = System.nanoTime() - start;
            recordLatency("deleteAll", latency);
            
            if (logger.isDebugEnabled()) {
                logger.debugf("Redis DELETEALL complete: cacheName=%s, totalKeys=%d, totalDeleted=%d, latencyMs=%.2f", 
                    cacheName, keys.size(), totalDeleted, latency / 1_000_000.0);
            }
            
            return totalDeleted;
        } catch (Exception e) {
            errorCount.incrementAndGet();
            logger.errorf(e, "Redis DELETEALL error: cacheName=%s, error=%s", cacheName, e.getMessage());
            throw e;
        }
    }
    
    @Override
    public CompletionStage<Long> deleteAllAsync(String cacheName, List<String> keys) {
        return CompletableFuture.supplyAsync(() -> deleteAll(cacheName, keys));
    }
    
    // ==================== Health Check & Reconnect ====================
    
    @Override
    public boolean ping() {
        try {
            String pong = clusterMode ?
                clusterConnection.sync().ping() :
                standaloneConnection.sync().ping();
            
            boolean pingSuccess = "PONG".equalsIgnoreCase(pong);
            if (pingSuccess) {
                healthy = true;
                lastHealthCheck = System.currentTimeMillis();
            } else {
                healthy = false;
            }
            return pingSuccess;
        } catch (Exception e) {
            logger.warnf(e, "Ping failed");
            healthy = false;
            return false;
        }
    }
    
    @Override
    public boolean reconnect() {
        logger.infof("Attempting to reconnect to Redis...");
        
        try {
            // Test connection with ping
            boolean pingSuccess = ping();
            
            if (pingSuccess) {
                logger.infof("Reconnect successful");
                healthy = true;
                return true;
            } else {
                logger.warnf("Reconnect failed - ping unsuccessful");
                healthy = false;
                return false;
            }
        } catch (Exception e) {
            logger.errorf(e, "Reconnect failed");
            healthy = false;
            return false;
        }
    }
    
    // ==================== Enhanced Metrics ====================
    
    @Override
    public Map<String, Object> getEnhancedMetrics() {
        Map<String, Object> metrics = new LinkedHashMap<>();
        
        // Basic metrics
        metrics.put("operations.get", getOperations.get());
        metrics.put("operations.put", putOperations.get());
        metrics.put("operations.delete", deleteOperations.get());
        metrics.put("operations.scan", scanOperations.get());
        metrics.put("operations.batchGet", batchGetOperations.get());
        metrics.put("operations.batchPut", batchPutOperations.get());
        metrics.put("operations.batchDelete", batchDeleteOperations.get());
        
        metrics.put("cache.hits", cacheHits.get());
        metrics.put("cache.misses", cacheMisses.get());
        metrics.put("errors.total", errorCount.get());
        
        // Calculate hit rate
        long totalGets = getOperations.get();
        if (totalGets > 0) {
            double hitRate = (cacheHits.get() * 100.0) / totalGets;
            metrics.put(METRIC_CACHE_HIT_RATE, hitRate);
        } else {
            metrics.put(METRIC_CACHE_HIT_RATE, 0.0);
        }
        
        // Latency statistics from HdrHistogram
        latencyRecorders.forEach((operation, recorder) -> {
            Histogram histogram = recorder.getIntervalHistogram();
            if (histogram.getTotalCount() > 0) {
                String prefix = METRIC_LATENCY_PREFIX + operation + ".";
                metrics.put(prefix + "count", (int) histogram.getTotalCount());
                metrics.put(prefix + "min", histogram.getMinValue() / 1_000_000.0); // Convert to ms
                metrics.put(prefix + "max", histogram.getMaxValue() / 1_000_000.0);
                metrics.put(prefix + "mean", histogram.getMean() / 1_000_000.0);
                metrics.put(prefix + "p50", histogram.getValueAtPercentile(50.0) / 1_000_000.0);
                metrics.put(prefix + "p95", histogram.getValueAtPercentile(95.0) / 1_000_000.0);
                metrics.put(prefix + "p99", histogram.getValueAtPercentile(99.0) / 1_000_000.0);
                metrics.put(prefix + "p999", histogram.getValueAtPercentile(99.9) / 1_000_000.0);
            }
        });
        
        // Health status
        metrics.put("health.healthy", healthy);
        metrics.put("health.lastCheck", lastHealthCheck);
        
        return metrics;
    }
    
    @Override
    public void resetMetrics() {
        getOperations.set(0);
        putOperations.set(0);
        deleteOperations.set(0);
        scanOperations.set(0);
        batchGetOperations.set(0);
        batchPutOperations.set(0);
        batchDeleteOperations.set(0);
        cacheHits.set(0);
        cacheMisses.set(0);
        errorCount.set(0);
        latencyRecorders.clear();
        
        logger.debugf("Metrics reset");
    }
    
    @Override
    public Map<String, Object> getConnectionInsights() {
        Map<String, Object> insights = new LinkedHashMap<>();
        
        // Connection mode
        insights.put("connection.mode", clusterMode ? "cluster" : "standalone");
        insights.put("connection.info", connectionInfo);
        
        // Connection state
        if (clusterMode && clusterConnection != null) {
            insights.put("connection.open", clusterConnection.isOpen());
            insights.put("connection.type", "cluster");
        } else if (standaloneConnection != null) {
            insights.put("connection.open", standaloneConnection.isOpen());
            insights.put("connection.type", "standalone");
        }
        
        // Pub/Sub connections
        insights.put("pubsub.connections.count", pubSubConnections.size());
        insights.put("pubsub.connections.tracked", !pubSubConnections.isEmpty());
        
        // Thread pool info from client resources
        if (clusterMode && clusterClient != null) {
            try {
                io.lettuce.core.resource.ClientResources resources = clusterClient.getResources();
                insights.put("threadpool.io.size", resources.ioThreadPoolSize());
                insights.put("threadpool.computation.size", resources.computationThreadPoolSize());
            } catch (Exception e) {
                logger.debugf(e, "Could not retrieve client resources info");
            }
        } else if (standaloneClient != null) {
            try {
                io.lettuce.core.resource.ClientResources resources = standaloneClient.getResources();
                insights.put("threadpool.io.size", resources.ioThreadPoolSize());
                insights.put("threadpool.computation.size", resources.computationThreadPoolSize());
            } catch (Exception e) {
                logger.debugf(e, "Could not retrieve client resources info");
            }
        }
        
        // Latency recorders state
        insights.put("latency.trackers.count", latencyRecorders.size());
        insights.put("latency.trackers.types", new ArrayList<>(latencyRecorders.keySet()));
        
        // Health tracking
        insights.put("health.status", healthy ? "healthy" : "unhealthy");
        insights.put("health.lastCheckAge", System.currentTimeMillis() - lastHealthCheck);
        
        return insights;
    }
    
    /**
     * Record latency for an operation using HdrHistogram.
     * HdrHistogram provides efficient percentile calculation with minimal memory overhead.
     * Configured to track latencies from 1 microsecond to 60 seconds with 3 significant digits.
     */
    private void recordLatency(String operation, long latencyNs) {
        latencyRecorders.computeIfAbsent(operation, k -> 
            // Track from 1us to 60s with 3 significant digits of precision
            new Recorder(TimeUnit.MICROSECONDS.toNanos(1), TimeUnit.SECONDS.toNanos(60), 3)
        ).recordValue(latencyNs);
    }

    @Override
    public void close() {
        // This provider is a singleton shared across sessions, so individual session
        // close() calls should NOT close the shared Pub/Sub connections.
        // Pub/Sub connections are long-lived and should only be closed when the
        // factory itself is closed (via closeAllConnections()).
        // This is a no-op to prevent premature cleanup during session lifecycle.
        logger.tracef("Session-scoped close() called - connections remain open (singleton pattern)");
    }

    /**
     * Close all connections including Pub/Sub connections.
     * This should only be called by the factory during shutdown.
     */
    void closeAllConnections() {
        // Close all tracked Pub/Sub connections
        if (!pubSubConnections.isEmpty()) {
            logger.infof("Closing %d Pub/Sub connections", pubSubConnections.size());

            for (StatefulRedisPubSubConnection<String, String> conn : pubSubConnections) {
                try {
                    if (conn != null && conn.isOpen()) {
                        conn.close();
                        logger.debugf("Closed Pub/Sub connection");
                    }
                } catch (Exception e) {
                    logger.warnf(e, "Error closing Pub/Sub connection");
                }
            }

            pubSubConnections.clear();
            logger.debugf("All Pub/Sub connections closed");
        }
    }

    /**
     * Execute a Lua script on Redis for atomic operations.
     * Scripts execute atomically in both standalone and cluster modes.
     * 
     * @param script The Lua script to execute
     * @param keys Array of Redis keys for the script (KEYS array in Lua)
     * @param args Array of arguments for the script (ARGV array in Lua)
     * @return Result of script execution
     */
    private Long executeScript(String script, String[] keys, String[] args) {
        try {
            if (clusterMode) {
                return clusterConnection.sync().eval(
                        script,
                        ScriptOutputType.INTEGER,
                        keys,
                        args
                );
            } else {
                return standaloneConnection.sync().eval(
                        script,
                        ScriptOutputType.INTEGER,
                        keys,
                        args
                );
            }
        } catch (Exception e) {
            logger.errorf(e, "Failed to execute Lua script");
            throw e;
        }
    }

    private <V> String serialize(V value) { return serializer.serializeToString(value); }
    private <V> V deserialize(String json, Class<V> type) { return serializer.deserialize(json, type); }
}
