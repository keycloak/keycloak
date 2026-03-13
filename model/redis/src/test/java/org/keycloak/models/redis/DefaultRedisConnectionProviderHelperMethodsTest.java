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
import io.lettuce.core.RedisClient;
import io.lettuce.core.ScanArgs;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.models.redis.DefaultRedisConnectionProvider;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for helper methods in DefaultRedisConnectionProvider (Sprint 2 improvements).
 * Tests the refactored scanKeys() method and its extracted helpers.
 */
class DefaultRedisConnectionProviderHelperMethodsTest {

    private RedisClient mockClient;
    private StatefulRedisConnection<String, String> mockConnection;
    private RedisCommands<String, String> mockSync;
    private DefaultRedisConnectionProvider provider;

    @BeforeEach
    void setUp() {
        mockClient = mock(RedisClient.class);
        mockConnection = mock(StatefulRedisConnection.class);
        mockSync = mock(RedisCommands.class);
        
        when(mockConnection.sync()).thenReturn(mockSync);
        
        provider = new DefaultRedisConnectionProvider(
            mockClient, mockConnection, "kc:", "redis://localhost:6379/0"
        );
    }

    @Test
    void testScanKeys_StandaloneWithResults() {
        KeyScanCursor<String> cursor = mock(KeyScanCursor.class);
        
        when(mockSync.scan(any(ScanArgs.class))).thenReturn(cursor);
        when(cursor.getKeys()).thenReturn(List.of("kc:cache:key1", "kc:cache:key2", "kc:cache:key3"));
        when(cursor.isFinished()).thenReturn(true);
        
        List<String> keys = provider.scanKeys("cache", "*");
        
        assertThat(keys).hasSize(3);
        assertThat(keys).containsExactlyInAnyOrder("key1", "key2", "key3");
    }

    @Test
    void testScanKeys_StandaloneWithMultiplePages() {
        KeyScanCursor<String> cursor1 = mock(KeyScanCursor.class);
        KeyScanCursor<String> cursor2 = mock(KeyScanCursor.class);
        
        when(mockSync.scan(any(ScanArgs.class))).thenReturn(cursor1);
        when(mockSync.scan(eq(cursor1), any(ScanArgs.class))).thenReturn(cursor2);
        
        when(cursor1.getKeys()).thenReturn(List.of("kc:cache:key1", "kc:cache:key2"));
        when(cursor1.isFinished()).thenReturn(false);
        
        when(cursor2.getKeys()).thenReturn(List.of("kc:cache:key3"));
        when(cursor2.isFinished()).thenReturn(true);
        
        List<String> keys = provider.scanKeys("cache", "*");
        
        assertThat(keys).hasSize(3);
        assertThat(keys).containsExactlyInAnyOrder("key1", "key2", "key3");
    }

    @Test
    void testScanKeys_StandaloneWithNoResults() {
        KeyScanCursor<String> cursor = mock(KeyScanCursor.class);
        
        when(mockSync.scan(any(ScanArgs.class))).thenReturn(cursor);
        when(cursor.getKeys()).thenReturn(List.of());
        when(cursor.isFinished()).thenReturn(true);
        
        List<String> keys = provider.scanKeys("cache", "*");
        
        assertThat(keys).isEmpty();
    }

    @Test
    void testScanKeys_WithDifferentCacheNames() {
        KeyScanCursor<String> cursor = mock(KeyScanCursor.class);
        
        when(mockSync.scan(any(ScanArgs.class))).thenReturn(cursor);
        when(cursor.getKeys()).thenReturn(List.of(
            "kc:sessions:session1",
            "kc:sessions:session2",
            "kc:cache:cached1"  // Different cache, should be filtered
        ));
        when(cursor.isFinished()).thenReturn(true);
        
        List<String> keys = provider.scanKeys("sessions", "*");
        
        // Should only return keys from "sessions" cache
        assertThat(keys).hasSize(2);
        assertThat(keys).containsExactlyInAnyOrder("session1", "session2");
    }

    @Test
    void testScanKeys_HandlesException() {
        when(mockSync.scan(any(ScanArgs.class))).thenThrow(new RuntimeException("Redis error"));
        
        List<String> keys = provider.scanKeys("cache", "*");
        
        // Should return empty list on error
        assertThat(keys).isEmpty();
    }

    @Test
    void testGetMetrics_InitialState() {
        var metrics = provider.getMetrics();
        
        assertThat(metrics).isNotNull();
        assertThat(metrics.get("operations.get")).isEqualTo(0L);
        assertThat(metrics.get("operations.put")).isEqualTo(0L);
        assertThat(metrics.get("operations.delete")).isEqualTo(0L);
        assertThat(metrics.get("operations.scan")).isEqualTo(0L);
        assertThat(metrics.get("cache.hits")).isEqualTo(0L);
        assertThat(metrics.get("cache.misses")).isEqualTo(0L);
        assertThat(metrics.get("errors.total")).isEqualTo(0L);
        assertThat(metrics.get("cache.hitRate")).isEqualTo(0L);
    }

    @Test
    void testGetMetrics_AfterOperations() {
        // Use proper JSON strings for serialization
        when(mockSync.get(anyString())).thenReturn("\"value\"", (String) null);
        when(mockSync.psetex(anyString(), anyLong(), anyString())).thenReturn("OK");
        when(mockSync.del(anyString())).thenReturn(1L);
        
        // Perform operations
        provider.get("cache", "key1", String.class);  // Hit
        provider.get("cache", "key2", String.class);  // Miss
        provider.put("cache", "key3", "value", 60, java.util.concurrent.TimeUnit.SECONDS);
        provider.delete("cache", "key4");
        
        var metrics = provider.getMetrics();
        
        assertThat(metrics.get("operations.get")).isEqualTo(2L);
        assertThat(metrics.get("operations.put")).isEqualTo(1L);
        assertThat(metrics.get("operations.delete")).isEqualTo(1L);
        assertThat(metrics.get("cache.hits")).isEqualTo(1L);
        assertThat(metrics.get("cache.misses")).isEqualTo(1L);
        assertThat(metrics.get("cache.hitRate")).isEqualTo(50L); // 1/2 = 50%
    }

    @Test
    void testGetMetrics_HitRateCalculation() {
        // Use proper JSON strings
        when(mockSync.get(anyString())).thenReturn("\"value\"");
        
        // 9 hits
        for (int i = 0; i < 9; i++) {
            provider.get("cache", "key" + i, String.class);
        }
        // 1 miss
        when(mockSync.get(anyString())).thenReturn((String) null);
        provider.get("cache", "key10", String.class);
        
        var metrics = provider.getMetrics();
        
        assertThat(metrics.get("operations.get")).isEqualTo(10L);
        assertThat(metrics.get("cache.hits")).isEqualTo(9L);
        assertThat(metrics.get("cache.misses")).isEqualTo(1L);
        assertThat(metrics.get("cache.hitRate")).isEqualTo(90L); // 9/10 = 90%
    }

    @Test
    void testErrorTracking() {
        when(mockSync.get(anyString())).thenThrow(new RuntimeException("Redis error"));
        
        // Perform operation that will fail
        assertThatThrownBy(() -> provider.get("cache", "key1", String.class))
            .isInstanceOf(RuntimeException.class);
        
        var metrics = provider.getMetrics();
        
        assertThat(metrics.get("errors.total")).isEqualTo(1L);
    }
}
