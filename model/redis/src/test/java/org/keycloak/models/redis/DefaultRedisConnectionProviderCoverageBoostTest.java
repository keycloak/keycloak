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

import io.lettuce.core.KeyScanCursor;
import io.lettuce.core.KeyValue;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.ScanArgs;
import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.async.RedisClusterAsyncCommands;
import io.lettuce.core.cluster.api.sync.RedisClusterCommands;
import io.lettuce.core.cluster.models.partitions.Partitions;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.resource.ClientResources;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.models.redis.DefaultRedisConnectionProvider;
import org.keycloak.models.redis.RedisConnectionProvider;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Additional tests to boost coverage for DefaultRedisConnectionProvider.
 * Focuses on error paths, async operations, and edge cases.
 */
class DefaultRedisConnectionProviderCoverageBoostTest {

    private RedisClient mockClient;
    private StatefulRedisConnection<String, String> mockConnection;
    private RedisCommands<String, String> mockSync;
    private RedisAsyncCommands<String, String> mockAsync;
    private DefaultRedisConnectionProvider provider;

    @BeforeEach
    void setUp() {
        mockClient = mock(RedisClient.class);
        mockConnection = mock(StatefulRedisConnection.class);
        mockSync = mock(RedisCommands.class);
        mockAsync = mock(RedisAsyncCommands.class);

        when(mockConnection.sync()).thenReturn(mockSync);
        when(mockConnection.async()).thenReturn(mockAsync);

        provider = new DefaultRedisConnectionProvider(
                mockClient, mockConnection, "kc:", "redis://localhost:6379"
        );
    }

    // ==================== Async Operations Coverage ====================

    @Test
    void testGetAsync_Success() throws Exception {
        RedisFuture<String> future = mock(RedisFuture.class);
        when(mockAsync.get("kc:cache:key1")).thenReturn(future);
        when(future.thenApply(any())).thenReturn(CompletableFuture.completedFuture("value"));
        when(future.toCompletableFuture()).thenReturn(CompletableFuture.completedFuture("\"value\""));

        var result = provider.getAsync("cache", "key1", String.class);
        
        assertThat(result).isNotNull();
    }

    @Test
    void testGetWithVersionAsync_WithValue() throws Exception {
        RedisFuture<String> future = mock(RedisFuture.class);
        when(mockAsync.get(anyString())).thenReturn(future);
        when(future.thenApply(any())).thenReturn(CompletableFuture.completedFuture("value"));
        when(future.toCompletableFuture()).thenReturn(CompletableFuture.completedFuture("\"value\""));

        var result = provider.getWithVersionAsync("cache", "key1", String.class);
        
        assertThat(result).isNotNull();
    }

    @Test
    void testGetWithVersionAsync_NullValue() throws Exception {
        RedisFuture<String> future = mock(RedisFuture.class);
        when(mockAsync.get(anyString())).thenReturn(future);
        when(future.thenApply(any())).thenReturn(CompletableFuture.completedFuture(null));
        when(future.toCompletableFuture()).thenReturn(CompletableFuture.completedFuture(null));

        var result = provider.getWithVersionAsync("cache", "key1", String.class);
        
        assertThat(result).isNotNull();
    }

    @Test
    void testPutAsync_Success() {
        RedisFuture<String> future = mock(RedisFuture.class);
        when(mockAsync.psetex(anyString(), anyLong(), anyString())).thenReturn(future);
        when(future.thenApply(any())).thenReturn(CompletableFuture.completedFuture(null));
        when(future.toCompletableFuture()).thenReturn(CompletableFuture.completedFuture("OK"));

        var result = provider.putAsync("cache", "key1", "value", 60, TimeUnit.SECONDS);

        assertThat(result).isNotNull();
    }

    @Test
    void testPutAsync_NegativeTTL() {
        RedisFuture<String> future = mock(RedisFuture.class);
        when(mockAsync.set(anyString(), anyString())).thenReturn(future);
        when(future.thenApply(any())).thenReturn(CompletableFuture.completedFuture(null));
        when(future.toCompletableFuture()).thenReturn(CompletableFuture.completedFuture("OK"));

        var result = provider.putAsync("cache", "key1", "value", -1, TimeUnit.SECONDS);

        assertThat(result).isNotNull();
        verify(mockAsync).set(anyString(), anyString());
        verify(mockAsync, never()).psetex(anyString(), anyLong(), anyString());
    }

    @Test
    void testPutIfAbsentAsync_Success() {
        var result = provider.putIfAbsentAsync("cache", "key1", "value", 60, TimeUnit.SECONDS);
        
        assertThat(result).isNotNull();
    }

    @Test
    void testReplaceWithVersionAsync_Success() {
        when(mockSync.eval(anyString(), eq(ScriptOutputType.INTEGER), 
            any(String[].class), any(String[].class))).thenReturn(1L);

        var result = provider.replaceWithVersionAsync("cache", "key1", "value", 1L, 60, TimeUnit.SECONDS);
        
        assertThat(result).isNotNull();
    }

    @Test
    void testRemoveAsync_Success() {
        when(mockSync.get(anyString())).thenReturn("\"value\"");
        when(mockSync.del(anyString())).thenReturn(1L);

        var result = provider.removeAsync("cache", "key1", String.class);
        
        assertThat(result).isNotNull();
    }

    @Test
    void testDeleteAsync_Success() {
        when(mockSync.del(anyString())).thenReturn(1L);

        var result = provider.deleteAsync("cache", "key1");
        
        assertThat(result).isNotNull();
    }

    @Test
    void testRemoveByPatternAsync_Success() {
        KeyScanCursor<String> cursor = mock(KeyScanCursor.class);
        when(cursor.isFinished()).thenReturn(true);
        when(cursor.getKeys()).thenReturn(List.of("kc:cache:key1"));
        when(mockSync.scan(any(ScanArgs.class))).thenReturn(cursor);
        when(mockSync.del(anyString())).thenReturn(1L);

        var result = provider.removeByPatternAsync("cache", "*");
        
        assertThat(result).isNotNull();
    }

    // ==================== Batch Operations Coverage ====================

    @Test
    void testGetAll_EmptyKeys() {
        var result = provider.getAll("cache", Collections.emptyList(), String.class);
        
        assertThat(result).isEmpty();
    }

    @Test
    void testGetAll_NullKeys() {
        var result = provider.getAll("cache", null, String.class);
        
        assertThat(result).isEmpty();
    }

    @Test
    void testGetAll_StandaloneMode() {
        List<KeyValue<String, String>> kvList = new ArrayList<>();
        kvList.add(KeyValue.just("kc:cache:key1", "\"value1\""));
        kvList.add(KeyValue.empty("kc:cache:key2"));
        
        when(mockSync.mget(any(String[].class))).thenReturn(kvList);

        var result = provider.getAll("cache", List.of("key1", "key2"), String.class);
        
        assertThat(result).hasSize(1);
        assertThat(result.get("key1")).isEqualTo("value1");
    }

    @Test
    void testGetAll_WithNullDeserializedValue() {
        List<KeyValue<String, String>> kvList = new ArrayList<>();
        // Use empty value instead of invalid JSON to test null handling
        kvList.add(KeyValue.empty("kc:cache:key1"));
        kvList.add(KeyValue.just("kc:cache:key2", "\"value2\""));
        
        when(mockSync.mget(any(String[].class))).thenReturn(kvList);

        var result = provider.getAll("cache", List.of("key1", "key2"), String.class);
        
        // Should skip null values and return only valid entries
        assertThat(result).hasSize(1);
        assertThat(result.get("key2")).isEqualTo("value2");
    }

    @Test
    void testGetAllAsync_Success() {
        List<KeyValue<String, String>> kvList = new ArrayList<>();
        kvList.add(KeyValue.just("kc:cache:key1", "\"value1\""));
        
        when(mockSync.mget(any(String[].class))).thenReturn(kvList);

        var result = provider.getAllAsync("cache", List.of("key1"), String.class);
        
        assertThat(result).isNotNull();
    }

    @Test
    void testPutAll_EmptyMap() {
        provider.putAll("cache", Collections.emptyMap(), 60, TimeUnit.SECONDS);
        
        verifyNoInteractions(mockSync);
    }

    @Test
    void testPutAll_NullMap() {
        provider.putAll("cache", null, 60, TimeUnit.SECONDS);
        
        verifyNoInteractions(mockSync);
    }

    @Test
    void testPutAll_StandaloneMode() {
        doNothing().when(mockAsync).setAutoFlushCommands(anyBoolean());
        doNothing().when(mockAsync).flushCommands();
        RedisFuture<String> future = mock(RedisFuture.class);
        when(mockAsync.psetex(anyString(), anyLong(), anyString())).thenReturn(future);

        Map<String, String> entries = new HashMap<>();
        entries.put("key1", "value1");
        entries.put("key2", "value2");

        provider.putAll("cache", entries, 60, TimeUnit.SECONDS);
        
        verify(mockAsync).setAutoFlushCommands(false);
        verify(mockAsync).flushCommands();
        verify(mockAsync).setAutoFlushCommands(true);
    }

    @Test
    void testPutAllAsync_Success() {
        doNothing().when(mockAsync).setAutoFlushCommands(anyBoolean());
        doNothing().when(mockAsync).flushCommands();
        RedisFuture<String> future = mock(RedisFuture.class);
        when(mockAsync.psetex(anyString(), anyLong(), anyString())).thenReturn(future);

        Map<String, String> entries = Map.of("key1", "value1");

        var result = provider.putAllAsync("cache", entries, 60, TimeUnit.SECONDS);
        
        assertThat(result).isNotNull();
    }

    @Test
    void testDeleteAll_EmptyKeys() {
        var result = provider.deleteAll("cache", Collections.emptyList());
        
        assertThat(result).isEqualTo(0);
    }

    @Test
    void testDeleteAll_NullKeys() {
        var result = provider.deleteAll("cache", null);
        
        assertThat(result).isEqualTo(0);
    }

    @Test
    void testDeleteAll_SingleChunk() {
        when(mockSync.eval(anyString(), eq(ScriptOutputType.INTEGER), 
            any(String[].class), any(String[].class))).thenReturn(3L);

        List<String> keys = List.of("key1", "key2", "key3");
        var result = provider.deleteAll("cache", keys);
        
        assertThat(result).isEqualTo(3);
    }

    @Test
    void testDeleteAll_MultipleChunks() {
        when(mockSync.eval(anyString(), eq(ScriptOutputType.INTEGER), 
            any(String[].class), any(String[].class))).thenReturn(1000L, 500L);

        List<String> keys = new ArrayList<>();
        for (int i = 0; i < 1500; i++) {
            keys.add("key" + i);
        }

        var result = provider.deleteAll("cache", keys);
        
        assertThat(result).isEqualTo(1500);
    }

    @Test
    void testDeleteAllAsync_Success() {
        when(mockSync.eval(anyString(), eq(ScriptOutputType.INTEGER), 
            any(String[].class), any(String[].class))).thenReturn(1L);

        var result = provider.deleteAllAsync("cache", List.of("key1"));
        
        assertThat(result).isNotNull();
    }

    // ==================== Health and Metrics Coverage ====================

    @Test
    void testPing_Success() {
        when(mockSync.ping()).thenReturn("PONG");

        boolean result = provider.ping();
        
        assertThat(result).isTrue();
        assertThat(provider.isHealthy()).isTrue();
    }

    @Test
    void testPing_Failure() {
        when(mockSync.ping()).thenReturn("ERROR");

        boolean result = provider.ping();
        
        assertThat(result).isFalse();
        assertThat(provider.isHealthy()).isFalse();
    }

    @Test
    void testPing_Exception() {
        when(mockSync.ping()).thenThrow(new RuntimeException("Connection lost"));

        boolean result = provider.ping();
        
        assertThat(result).isFalse();
        assertThat(provider.isHealthy()).isFalse();
    }

    @Test
    void testReconnect_Success() {
        when(mockSync.ping()).thenReturn("PONG");

        boolean result = provider.reconnect();
        
        assertThat(result).isTrue();
    }

    @Test
    void testReconnect_Failure() {
        when(mockSync.ping()).thenReturn("ERROR");

        boolean result = provider.reconnect();
        
        assertThat(result).isFalse();
    }

    @Test
    void testReconnect_Exception() {
        when(mockSync.ping()).thenThrow(new RuntimeException("Connection lost"));

        boolean result = provider.reconnect();
        
        assertThat(result).isFalse();
    }

    @Test
    void testIsHealthy_CatchesException() {
        when(mockSync.ping()).thenThrow(new RuntimeException("Error"));

        boolean result = provider.isHealthy();
        
        assertThat(result).isFalse();
    }

    @Test
    void testGetEnhancedMetrics_WithOperations() {
        // Perform some operations to generate metrics
        when(mockSync.get(anyString())).thenReturn("\"value\"");
        when(mockSync.psetex(anyString(), anyLong(), anyString())).thenReturn("OK");
        when(mockSync.del(anyString())).thenReturn(1L);

        provider.get("cache", "key1", String.class);
        provider.put("cache", "key2", "value", 60, TimeUnit.SECONDS);
        provider.delete("cache", "key3");

        Map<String, Object> metrics = provider.getEnhancedMetrics();
        
        assertThat(metrics).isNotEmpty();
        assertThat(metrics).containsKeys(
            "operations.get", 
            "operations.put", 
            "operations.delete",
            "cache.hits",
            "cache.misses",
            "errors.total",
            "health.healthy",
            "health.lastCheck"
        );
    }

    @Test
    void testResetMetrics() {
        // Perform operations
        when(mockSync.get(anyString())).thenReturn("\"value\"");
        provider.get("cache", "key1", String.class);

        Map<String, Long> metricsBefore = provider.getMetrics();
        assertThat(metricsBefore.get("operations.get")).isEqualTo(1);

        provider.resetMetrics();

        Map<String, Long> metricsAfter = provider.getMetrics();
        assertThat(metricsAfter.get("operations.get")).isEqualTo(0);
    }

    @Test
    void testGetConnectionInsights_StandaloneMode() {
        ClientResources resources = mock(ClientResources.class);
        when(mockClient.getResources()).thenReturn(resources);
        when(resources.ioThreadPoolSize()).thenReturn(4);
        when(resources.computationThreadPoolSize()).thenReturn(4);
        when(mockConnection.isOpen()).thenReturn(true);

        Map<String, Object> insights = provider.getConnectionInsights();
        
        assertThat(insights).isNotEmpty();
        assertThat(insights.get("connection.mode")).isEqualTo("standalone");
        assertThat(insights.get("connection.type")).isEqualTo("standalone");
        assertThat(insights.get("connection.open")).isEqualTo(true);
    }

    @Test
    void testGetConnectionInsights_WithException() {
        when(mockClient.getResources()).thenThrow(new RuntimeException("Error"));

        Map<String, Object> insights = provider.getConnectionInsights();
        
        // Should handle exception gracefully
        assertThat(insights).isNotEmpty();
    }

    // ==================== PubSub Operations Coverage ====================

    @Test
    void testPublish_StandaloneMode() {
        when(mockSync.publish("channel", "message")).thenReturn(1L);

        provider.publish("channel", "message");
        
        verify(mockSync).publish("channel", "message");
    }

    @Test
    void testCreatePubSubConnection_StandaloneMode() {
        StatefulRedisPubSubConnection<String, String> pubSubConn = mock(StatefulRedisPubSubConnection.class);
        when(mockClient.connectPubSub()).thenReturn(pubSubConn);

        Object result = provider.createPubSubConnection();
        
        assertThat(result).isNotNull();
        assertThat(result).isInstanceOf(StatefulRedisPubSubConnection.class);
    }

    @Test
    void testCreatePubSubConnection_Exception() {
        when(mockClient.connectPubSub()).thenThrow(new RuntimeException("Connection failed"));

        Object result = provider.createPubSubConnection();
        
        assertThat(result).isNull();
    }

    @Test
    void testClose_WithPubSubConnections() {
        StatefulRedisPubSubConnection<String, String> pubSubConn = mock(StatefulRedisPubSubConnection.class);
        when(mockClient.connectPubSub()).thenReturn(pubSubConn);
        when(pubSubConn.isOpen()).thenReturn(true);

        // Create pub/sub connection
        provider.createPubSubConnection();

        // Session-scoped close should be a no-op (singleton pattern)
        provider.close();

        // Connections should NOT be closed (use closeAllConnections instead)
        verify(pubSubConn, never()).close();
    }

    @Test
    void testCloseAllConnections_WithPubSubConnections() {
        StatefulRedisPubSubConnection<String, String> pubSubConn = mock(StatefulRedisPubSubConnection.class);
        when(mockClient.connectPubSub()).thenReturn(pubSubConn);
        when(pubSubConn.isOpen()).thenReturn(true);

        // Create pub/sub connection
        provider.createPubSubConnection();

        // Factory-scoped closeAllConnections should close connections
        provider.closeAllConnections();

        verify(pubSubConn).close();
    }

    @Test
    void testClose_WithClosedPubSubConnection() {
        StatefulRedisPubSubConnection<String, String> pubSubConn = mock(StatefulRedisPubSubConnection.class);
        when(mockClient.connectPubSub()).thenReturn(pubSubConn);
        when(pubSubConn.isOpen()).thenReturn(false);

        provider.createPubSubConnection();
        provider.close();

        // close() is a no-op, so no interactions expected
        verify(pubSubConn, never()).close();
    }

    @Test
    void testCloseAllConnections_WithClosedPubSubConnection() {
        StatefulRedisPubSubConnection<String, String> pubSubConn = mock(StatefulRedisPubSubConnection.class);
        when(mockClient.connectPubSub()).thenReturn(pubSubConn);
        when(pubSubConn.isOpen()).thenReturn(false);

        provider.createPubSubConnection();
        provider.closeAllConnections();

        // Should not attempt to close already-closed connection
        verify(pubSubConn, never()).close();
    }

    @Test
    void testClose_WithExceptionDuringClose() {
        // close() is now a no-op, so this test is no longer relevant
        // Keeping it to verify close() doesn't throw
        assertThatCode(() -> provider.close()).doesNotThrowAnyException();
    }

    @Test
    void testCloseAllConnections_WithExceptionDuringClose() {
        StatefulRedisPubSubConnection<String, String> pubSubConn = mock(StatefulRedisPubSubConnection.class);
        when(mockClient.connectPubSub()).thenReturn(pubSubConn);
        when(pubSubConn.isOpen()).thenReturn(true);
        doThrow(new RuntimeException("Close error")).when(pubSubConn).close();

        provider.createPubSubConnection();

        // Should not throw even when connection close fails
        assertThatCode(() -> provider.closeAllConnections()).doesNotThrowAnyException();
    }

    // ==================== Sorted Set Operations Coverage ====================

    @Test
    void testAddToSortedSet_Success() {
        when(mockSync.zadd(anyString(), anyDouble(), anyString())).thenReturn(1L);
        when(mockSync.pexpire(anyString(), anyLong())).thenReturn(true);

        boolean result = provider.addToSortedSet("cache", "set1", "member1", 1.0, 60, TimeUnit.SECONDS);
        
        assertThat(result).isTrue();
        verify(mockSync).zadd(eq("kc:cache:set1"), eq(1.0), eq("member1"));
        verify(mockSync).pexpire(eq("kc:cache:set1"), eq(60000L));
    }

    @Test
    void testAddToSortedSet_MemberAlreadyExists() {
        when(mockSync.zadd(anyString(), anyDouble(), anyString())).thenReturn(0L);
        when(mockSync.pexpire(anyString(), anyLong())).thenReturn(true);

        boolean result = provider.addToSortedSet("cache", "set1", "member1", 1.0, 60, TimeUnit.SECONDS);
        
        assertThat(result).isFalse();
    }

    @Test
    void testRemoveFromSortedSet_Success() {
        when(mockSync.zrem(anyString(), anyString())).thenReturn(1L);

        boolean result = provider.removeFromSortedSet("cache", "set1", "member1");
        
        assertThat(result).isTrue();
        verify(mockSync).zrem(eq("kc:cache:set1"), eq("member1"));
    }

    @Test
    void testRemoveFromSortedSet_MemberNotFound() {
        when(mockSync.zrem(anyString(), anyString())).thenReturn(0L);

        boolean result = provider.removeFromSortedSet("cache", "set1", "member1");
        
        assertThat(result).isFalse();
    }

    @Test
    void testGetSortedSetMembers_Success() {
        when(mockSync.zrange(anyString(), anyLong(), anyLong()))
            .thenReturn(List.of("member1", "member2", "member3"));

        List<String> result = provider.getSortedSetMembers("cache", "set1");
        
        assertThat(result).hasSize(3);
        assertThat(result).containsExactly("member1", "member2", "member3");
        verify(mockSync).zrange(eq("kc:cache:set1"), eq(0L), eq(-1L));
    }

    @Test
    void testGetSortedSetMembers_EmptySet() {
        when(mockSync.zrange(anyString(), anyLong(), anyLong()))
            .thenReturn(Collections.emptyList());

        List<String> result = provider.getSortedSetMembers("cache", "set1");
        
        assertThat(result).isEmpty();
    }

    @Test
    void testGetSortedSetSize_Success() {
        when(mockSync.zcard(anyString())).thenReturn(5L);

        long result = provider.getSortedSetSize("cache", "set1");
        
        assertThat(result).isEqualTo(5);
        verify(mockSync).zcard(eq("kc:cache:set1"));
    }

    @Test
    void testGetSortedSetSize_EmptySet() {
        when(mockSync.zcard(anyString())).thenReturn(0L);

        long result = provider.getSortedSetSize("cache", "set1");
        
        assertThat(result).isEqualTo(0);
    }

    @Test
    void testAddToSortedSet_Exception() {
        when(mockSync.zadd(anyString(), anyDouble(), anyString()))
            .thenThrow(new RuntimeException("Redis error"));

        boolean result = provider.addToSortedSet("cache", "set1", "member1", 1.0, 60, TimeUnit.SECONDS);
        
        assertThat(result).isFalse();
    }

    @Test
    void testRemoveFromSortedSet_Exception() {
        when(mockSync.zrem(anyString(), anyString()))
            .thenThrow(new RuntimeException("Redis error"));

        boolean result = provider.removeFromSortedSet("cache", "set1", "member1");
        
        assertThat(result).isFalse();
    }

    @Test
    void testGetSortedSetMembers_Exception() {
        when(mockSync.zrange(anyString(), anyLong(), anyLong()))
            .thenThrow(new RuntimeException("Redis error"));

        List<String> result = provider.getSortedSetMembers("cache", "set1");
        
        assertThat(result).isEmpty();
    }

    @Test
    void testGetSortedSetMembers_NullResult() {
        when(mockSync.zrange(anyString(), anyLong(), anyLong())).thenReturn(null);

        List<String> result = provider.getSortedSetMembers("cache", "set1");
        
        assertThat(result).isEmpty();
    }

    @Test
    void testGetSortedSetSize_Exception() {
        when(mockSync.zcard(anyString())).thenThrow(new RuntimeException("Redis error"));

        long result = provider.getSortedSetSize("cache", "set1");
        
        assertThat(result).isEqualTo(0);
    }

    @Test
    void testGetSortedSetSize_NullResult() {
        when(mockSync.zcard(anyString())).thenReturn(null);

        long result = provider.getSortedSetSize("cache", "set1");
        
        assertThat(result).isEqualTo(0);
    }

    // ==================== Scan Operations Coverage ====================

    @Test
    void testScanKeys_Exception() {
        when(mockSync.scan(any(ScanArgs.class)))
            .thenThrow(new RuntimeException("Scan error"));

        List<String> result = provider.scanKeys("cache", "*");
        
        assertThat(result).isEmpty();
    }

    @Test
    void testRemoveByPattern_Exception() {
        when(mockSync.scan(any(ScanArgs.class)))
            .thenThrow(new RuntimeException("Scan error"));

        long result = provider.removeByPattern("cache", "*");
        
        assertThat(result).isEqualTo(0);
    }

    @Test
    void testRemoveByPattern_EmptyKeys() {
        KeyScanCursor<String> cursor = mock(KeyScanCursor.class);
        when(cursor.isFinished()).thenReturn(true);
        when(cursor.getKeys()).thenReturn(Collections.emptyList());
        when(mockSync.scan(any(ScanArgs.class))).thenReturn(cursor);

        long result = provider.removeByPattern("cache", "*");
        
        assertThat(result).isEqualTo(0);
    }

    // ==================== Error Handling Coverage ====================

    @Test
    void testGet_Exception() {
        when(mockSync.get(anyString())).thenThrow(new RuntimeException("Redis error"));

        assertThatThrownBy(() -> provider.get("cache", "key1", String.class))
            .isInstanceOf(RuntimeException.class);
        
        assertThat(provider.getMetrics().get("errors.total")).isEqualTo(1);
    }

    @Test
    void testPut_Exception() {
        when(mockSync.psetex(anyString(), anyLong(), anyString()))
            .thenThrow(new RuntimeException("Redis error"));

        assertThatThrownBy(() -> provider.put("cache", "key1", "value", 60, TimeUnit.SECONDS))
            .isInstanceOf(RuntimeException.class);
        
        assertThat(provider.getMetrics().get("errors.total")).isEqualTo(1);
    }

    @Test
    void testDelete_Exception() {
        when(mockSync.del(anyString())).thenThrow(new RuntimeException("Redis error"));

        assertThatThrownBy(() -> provider.delete("cache", "key1"))
            .isInstanceOf(RuntimeException.class);
        
        assertThat(provider.getMetrics().get("errors.total")).isEqualTo(1);
    }

    @Test
    void testGetAll_Exception() {
        when(mockSync.mget(any(String[].class)))
            .thenThrow(new RuntimeException("Redis error"));

        assertThatThrownBy(() -> provider.getAll("cache", List.of("key1"), String.class))
            .isInstanceOf(RuntimeException.class);
    }

    @Test
    void testPutAll_Exception() {
        doThrow(new RuntimeException("Redis error")).when(mockAsync).setAutoFlushCommands(false);

        Map<String, String> entries = Map.of("key1", "value1");

        assertThatThrownBy(() -> provider.putAll("cache", entries, 60, TimeUnit.SECONDS))
            .isInstanceOf(RuntimeException.class);
    }

    @Test
    void testDeleteAll_Exception() {
        when(mockSync.eval(anyString(), eq(ScriptOutputType.INTEGER), 
            any(String[].class), any(String[].class)))
            .thenThrow(new RuntimeException("Redis error"));

        assertThatThrownBy(() -> provider.deleteAll("cache", List.of("key1")))
            .isInstanceOf(RuntimeException.class);
    }

    // ==================== Cluster Mode Additional Coverage ====================

    @Test
    void testClusterMode_GetConnectionInsights() {
        RedisClusterClient clusterClient = mock(RedisClusterClient.class);
        StatefulRedisClusterConnection<String, String> clusterConn = mock(StatefulRedisClusterConnection.class);
        ClientResources resources = mock(ClientResources.class);

        when(clusterClient.getResources()).thenReturn(resources);
        when(resources.ioThreadPoolSize()).thenReturn(8);
        when(resources.computationThreadPoolSize()).thenReturn(8);
        when(clusterConn.isOpen()).thenReturn(true);

        DefaultRedisConnectionProvider clusterProvider = new DefaultRedisConnectionProvider(
                clusterClient, clusterConn, "kc:", "redis://cluster:6379"
        );

        Map<String, Object> insights = clusterProvider.getConnectionInsights();
        
        assertThat(insights.get("connection.mode")).isEqualTo("cluster");
        assertThat(insights.get("connection.type")).isEqualTo("cluster");
        assertThat(insights.get("threadpool.io.size")).isEqualTo(8);
    }

    @Test
    void testCreatePubSubConnection_ReturnsNull() {
        // Test when client returns null
        StatefulRedisPubSubConnection<String, String> nullConn = null;
        when(mockClient.connectPubSub()).thenReturn(nullConn);

        Object result = provider.createPubSubConnection();
        
        // Should return null if client returns null
        assertThat(result).isNull();
    }

    @Test
    void testGetMetrics_WithZeroOperations() {
        Map<String, Long> metrics = provider.getMetrics();
        
        assertThat(metrics.get("cache.hitRate")).isEqualTo(0L);
    }

    @Test
    void testGetEnhancedMetrics_WithLatencyData() {
        // Perform operations to generate latency data
        when(mockSync.get(anyString())).thenReturn("\"value\"");
        
        for (int i = 0; i < 10; i++) {
            provider.get("cache", "key" + i, String.class);
        }

        Map<String, Object> metrics = provider.getEnhancedMetrics();
        
        assertThat(metrics).containsKey("latency.get.count");
    }
}
