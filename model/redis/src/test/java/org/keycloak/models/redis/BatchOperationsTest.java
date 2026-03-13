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

import io.lettuce.core.KeyValue;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.sync.RedisCommands;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.models.redis.DefaultRedisConnectionProvider;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Comprehensive tests for batch operations (getAll, putAll, deleteAll).
 * Target: >85% coverage for new batch operation code.
 */
class BatchOperationsTest {

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
            mockClient, mockConnection, "kc:", "redis://localhost:6379/0"
        );
    }

    // ==================== getAll() Tests ====================

    @Test
    void testGetAll_Success() {
        // Setup test data
        List<String> keys = Arrays.asList("key1", "key2", "key3");
        
        KeyValue<String, String> kv1 = KeyValue.just("kc:cache:key1", "\"value1\"");
        KeyValue<String, String> kv2 = KeyValue.just("kc:cache:key2", "\"value2\"");
        KeyValue<String, String> kv3 = KeyValue.just("kc:cache:key3", "\"value3\"");
        
        when(mockSync.mget(any(String[].class)))
            .thenReturn(Arrays.asList(kv1, kv2, kv3));
        
        // Execute
        Map<String, String> result = provider.getAll("cache", keys, String.class);
        
        // Verify
        assertThat(result).hasSize(3);
        assertThat(result.get("key1")).isEqualTo("value1");
        assertThat(result.get("key2")).isEqualTo("value2");
        assertThat(result.get("key3")).isEqualTo("value3");
        
        verify(mockSync).mget("kc:cache:key1", "kc:cache:key2", "kc:cache:key3");
    }

    @Test
    void testGetAll_WithNullValues() {
        List<String> keys = Arrays.asList("key1", "key2", "key3");
        
        KeyValue<String, String> kv1 = KeyValue.just("kc:cache:key1", "\"value1\"");
        KeyValue<String, String> kv2 = KeyValue.empty("kc:cache:key2"); // Missing
        KeyValue<String, String> kv3 = KeyValue.just("kc:cache:key3", "\"value3\"");
        
        when(mockSync.mget(any(String[].class)))
            .thenReturn(Arrays.asList(kv1, kv2, kv3));
        
        Map<String, String> result = provider.getAll("cache", keys, String.class);
        
        // Only keys with values should be in result
        assertThat(result).hasSize(2);
        assertThat(result.get("key1")).isEqualTo("value1");
        assertThat(result.get("key3")).isEqualTo("value3");
        assertThat(result).doesNotContainKey("key2");
    }

    @Test
    void testGetAll_EmptyKeys() {
        Map<String, String> result = provider.getAll("cache", List.of(), String.class);
        
        assertThat(result).isEmpty();
        verify(mockSync, never()).mget(any(String[].class));
    }

    @Test
    void testGetAll_NullKeys() {
        Map<String, String> result = provider.getAll("cache", null, String.class);
        
        assertThat(result).isEmpty();
        verify(mockSync, never()).mget(any(String[].class));
    }

    @Test
    void testGetAll_AllMissing() {
        List<String> keys = Arrays.asList("key1", "key2");
        
        KeyValue<String, String> kv1 = KeyValue.empty("kc:cache:key1");
        KeyValue<String, String> kv2 = KeyValue.empty("kc:cache:key2");
        
        when(mockSync.mget(any(String[].class)))
            .thenReturn(Arrays.asList(kv1, kv2));
        
        Map<String, String> result = provider.getAll("cache", keys, String.class);
        
        assertThat(result).isEmpty();
    }

    @Test
    void testGetAll_ErrorHandling() {
        List<String> keys = Arrays.asList("key1", "key2");
        
        when(mockSync.mget(any(String[].class)))
            .thenThrow(new RuntimeException("Redis error"));
        
        assertThatThrownBy(() -> provider.getAll("cache", keys, String.class))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Redis error");
    }

    @Test
    void testGetAllAsync() {
        List<String> keys = Arrays.asList("key1");
        KeyValue<String, String> kv1 = KeyValue.just("kc:cache:key1", "\"value1\"");
        
        when(mockSync.mget(any(String[].class)))
            .thenReturn(List.of(kv1));
        
        provider.getAllAsync("cache", keys, String.class)
            .thenAccept(result -> {
                assertThat(result).hasSize(1);
                assertThat(result.get("key1")).isEqualTo("value1");
            });
    }

    // ==================== putAll() Tests ====================

    @Test
    void testPutAll_Success() {
        Map<String, String> entries = new HashMap<>();
        entries.put("key1", "value1");
        entries.put("key2", "value2");
        entries.put("key3", "value3");
        
        when(mockAsync.psetex(anyString(), anyLong(), anyString()))
            .thenReturn(null);
        
        provider.putAll("cache", entries, 3600, TimeUnit.SECONDS);
        
        verify(mockAsync).setAutoFlushCommands(false);
        verify(mockAsync, times(3)).psetex(anyString(), eq(3600000L), anyString());
        verify(mockAsync).flushCommands();
        verify(mockAsync).setAutoFlushCommands(true);
    }

    @Test
    void testPutAll_EmptyEntries() {
        provider.putAll("cache", new HashMap<>(), 3600, TimeUnit.SECONDS);
        
        verify(mockAsync, never()).psetex(anyString(), anyLong(), anyString());
    }

    @Test
    void testPutAll_NullEntries() {
        provider.putAll("cache", null, 3600, TimeUnit.SECONDS);
        
        verify(mockAsync, never()).psetex(anyString(), anyLong(), anyString());
    }

    @Test
    void testPutAll_ErrorHandling() {
        Map<String, String> entries = new HashMap<>();
        entries.put("key1", "value1");
        
        when(mockAsync.psetex(anyString(), anyLong(), anyString()))
            .thenThrow(new RuntimeException("Redis error"));
        
        assertThatThrownBy(() -> provider.putAll("cache", entries, 3600, TimeUnit.SECONDS))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Redis error");
    }

    @Test
    void testPutAllAsync() {
        Map<String, String> entries = Map.of("key1", "value1");
        
        when(mockAsync.psetex(anyString(), anyLong(), anyString()))
            .thenReturn(null);
        
        provider.putAllAsync("cache", entries, 3600, TimeUnit.SECONDS)
            .thenRun(() -> {
                verify(mockAsync, times(1)).psetex(anyString(), anyLong(), anyString());
            });
    }

    // ==================== deleteAll() Tests ====================

    @Test
    void testDeleteAll_Success() {
        List<String> keys = Arrays.asList("key1", "key2", "key3");
        
        // Mock Lua script execution (now uses chunked script instead of direct DEL)
        when(mockSync.eval(anyString(), any(), any(String[].class), any(String[].class))).thenReturn(3L);
        
        long deleted = provider.deleteAll("cache", keys);
        
        assertThat(deleted).isEqualTo(3L);
        verify(mockSync).eval(anyString(), any(), any(String[].class), any(String[].class));
    }

    @Test
    void testDeleteAll_PartialDeletes() {
        List<String> keys = Arrays.asList("key1", "key2", "key3");
        
        // Mock Lua script execution - only 2 existed
        when(mockSync.eval(anyString(), any(), any(String[].class), any(String[].class))).thenReturn(2L);
        
        long deleted = provider.deleteAll("cache", keys);
        
        assertThat(deleted).isEqualTo(2L);
    }

    @Test
    void testDeleteAll_EmptyKeys() {
        long deleted = provider.deleteAll("cache", List.of());
        
        assertThat(deleted).isEqualTo(0L);
        verify(mockSync, never()).eval(anyString(), any(), any(String[].class), any(String[].class));
    }

    @Test
    void testDeleteAll_NullKeys() {
        long deleted = provider.deleteAll("cache", null);
        
        assertThat(deleted).isEqualTo(0L);
        verify(mockSync, never()).eval(anyString(), any(), any(String[].class), any(String[].class));
    }

    @Test
    void testDeleteAll_NullReturnValue() {
        List<String> keys = Arrays.asList("key1");
        
        when(mockSync.eval(anyString(), any(), any(String[].class), any(String[].class))).thenReturn(null);
        
        long deleted = provider.deleteAll("cache", keys);
        
        assertThat(deleted).isEqualTo(0L);
    }

    @Test
    void testDeleteAll_ErrorHandling() {
        List<String> keys = Arrays.asList("key1", "key2");
        
        when(mockSync.eval(anyString(), any(), any(String[].class), any(String[].class))).thenThrow(new RuntimeException("Connection lost"));
        
        assertThatThrownBy(() -> provider.deleteAll("cache", keys))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Connection lost");
    }

    @Test
    void testDeleteAllAsync() {
        List<String> keys = Arrays.asList("key1");
        
        when(mockSync.eval(anyString(), any(), any(String[].class), any(String[].class))).thenReturn(1L);
        
        provider.deleteAllAsync("cache", keys)
            .thenAccept(deleted -> {
                assertThat(deleted).isEqualTo(1L);
            });
    }

    // ==================== Metrics Tracking for Batch Ops ====================

    @Test
    void testBatchOperationsUpdateMetrics() {
        List<String> keys = Arrays.asList("key1");
        KeyValue<String, String> kv1 = KeyValue.just("kc:cache:key1", "\"value1\"");
        
        when(mockSync.mget(any(String[].class))).thenReturn(List.of(kv1));
        when(mockSync.del(any(String[].class))).thenReturn(1L);
        when(mockAsync.psetex(anyString(), anyLong(), anyString())).thenReturn(null);
        
        // Perform batch operations
        provider.getAll("cache", keys, String.class);
        provider.putAll("cache", Map.of("key1", "value1"), 60, TimeUnit.SECONDS);
        provider.deleteAll("cache", keys);
        
        // Check enhanced metrics
        Map<String, Object> metrics = provider.getEnhancedMetrics();
        
        assertThat(metrics.get("operations.batchGet")).isEqualTo(1L);
        assertThat(metrics.get("operations.batchPut")).isEqualTo(1L);
        assertThat(metrics.get("operations.batchDelete")).isEqualTo(1L);
    }
}
