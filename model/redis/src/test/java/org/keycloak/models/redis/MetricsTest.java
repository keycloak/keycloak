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

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.models.redis.DefaultRedisConnectionProvider;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Comprehensive tests for metrics tracking functionality.
 * Covers getMetrics() and getEnhancedMetrics() methods.
 */
class MetricsTest {

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

    // ==================== getMetrics() Tests ====================

    @Test
    void testGetMetrics_InitialState() {
        Map<String, Long> metrics = provider.getMetrics();
        
        assertThat(metrics).isNotNull();
        assertThat(metrics).containsKeys(
            "operations.get",
            "operations.put",
            "operations.delete",
            "operations.scan",
            "cache.hits",
            "cache.misses"
        );
        
        // All should be 0 initially
        assertThat(metrics.get("operations.get")).isEqualTo(0L);
        assertThat(metrics.get("operations.put")).isEqualTo(0L);
        assertThat(metrics.get("operations.delete")).isEqualTo(0L);
        assertThat(metrics.get("cache.hits")).isEqualTo(0L);
        assertThat(metrics.get("cache.misses")).isEqualTo(0L);
    }

    @Test
    void testGetMetrics_AfterGetOperations() {
        when(mockSync.get(anyString())).thenReturn("\"test\"");
        
        provider.get("cache", "key1", String.class);
        provider.get("cache", "key2", String.class);
        provider.get("cache", "key3", String.class);
        
        Map<String, Long> metrics = provider.getMetrics();
        
        assertThat(metrics.get("operations.get")).isEqualTo(3L);
        assertThat(metrics.get("cache.hits")).isEqualTo(3L);
        assertThat(metrics.get("cache.misses")).isEqualTo(0L);
    }

    @Test
    void testGetMetrics_AfterPutOperations() {
        when(mockSync.psetex(anyString(), anyLong(), anyString())).thenReturn("OK");
        
        provider.put("cache", "key1", "value1", 60, TimeUnit.SECONDS);
        provider.put("cache", "key2", "value2", 60, TimeUnit.SECONDS);
        
        Map<String, Long> metrics = provider.getMetrics();
        
        assertThat(metrics.get("operations.put")).isEqualTo(2L);
    }

    @Test
    void testGetMetrics_AfterDeleteOperations() {
        when(mockSync.del(anyString())).thenReturn(1L);
        
        provider.delete("cache", "key1");
        
        Map<String, Long> metrics = provider.getMetrics();
        
        assertThat(metrics.get("operations.delete")).isEqualTo(1L);
    }

    @Test
    void testGetMetrics_CacheHitRate() {
        when(mockSync.get(anyString()))
            .thenReturn("\"value1\"")  // Hit
            .thenReturn(null)           // Miss
            .thenReturn("\"value2\"")  // Hit
            .thenReturn(null);          // Miss
        
        provider.get("cache", "key1", String.class);
        provider.get("cache", "key2", String.class);
        provider.get("cache", "key3", String.class);
        provider.get("cache", "key4", String.class);
        
        Map<String, Long> metrics = provider.getMetrics();
        
        assertThat(metrics.get("cache.hits")).isEqualTo(2L);
        assertThat(metrics.get("cache.misses")).isEqualTo(2L);
        
        // Hit rate should be 50% (2/4)
        long hits = metrics.get("cache.hits");
        long misses = metrics.get("cache.misses");
        double hitRate = (double) hits / (hits + misses) * 100;
        assertThat(hitRate).isCloseTo(50.0, within(0.1));
    }

    @Test
    void testGetMetrics_AllCacheMisses() {
        when(mockSync.get(anyString())).thenReturn(null);
        
        provider.get("cache", "key1", String.class);
        provider.get("cache", "key2", String.class);
        
        Map<String, Long> metrics = provider.getMetrics();
        
        assertThat(metrics.get("cache.hits")).isEqualTo(0L);
        assertThat(metrics.get("cache.misses")).isEqualTo(2L);
    }

    @Test
    void testGetMetrics_AllCacheHits() {
        when(mockSync.get(anyString())).thenReturn("\"value\"");
        
        provider.get("cache", "key1", String.class);
        provider.get("cache", "key2", String.class);
        provider.get("cache", "key3", String.class);
        
        Map<String, Long> metrics = provider.getMetrics();
        
        assertThat(metrics.get("cache.hits")).isEqualTo(3L);
        assertThat(metrics.get("cache.misses")).isEqualTo(0L);
    }

    @Test
    void testGetMetrics_CountersIncrement() {
        when(mockSync.get(anyString())).thenReturn("\"test\"");
        when(mockSync.psetex(anyString(), anyLong(), anyString())).thenReturn("OK");
        when(mockSync.del(anyString())).thenReturn(1L);
        
        // Perform multiple operations
        for (int i = 0; i < 5; i++) {
            provider.get("cache", "key" + i, String.class);
        }
        
        for (int i = 0; i < 3; i++) {
            provider.put("cache", "key" + i, "value" + i, 60, TimeUnit.SECONDS);
        }
        
        for (int i = 0; i < 2; i++) {
            provider.delete("cache", "key" + i);
        }
        
        Map<String, Long> metrics = provider.getMetrics();
        
        assertThat(metrics.get("operations.get")).isEqualTo(5L);
        assertThat(metrics.get("operations.put")).isEqualTo(3L);
        assertThat(metrics.get("operations.delete")).isEqualTo(2L);
    }

    // ==================== getEnhancedMetrics() Tests ====================

    @Test
    void testGetEnhancedMetrics_ContainsBasicMetrics() {
        Map<String, Object> enhanced = provider.getEnhancedMetrics();
        
        assertThat(enhanced).isNotNull();
        assertThat(enhanced).containsKeys(
            "operations.get",
            "operations.put",
            "operations.delete"
        );
    }

    @Test
    void testGetEnhancedMetrics_ContainsLatency() {
        when(mockSync.get(anyString())).thenReturn("\"test\"");
        
        provider.get("cache", "key1", String.class);
        
        Map<String, Object> enhanced = provider.getEnhancedMetrics();
        
        // Should contain latency metrics
        assertThat(enhanced).containsKeys(
            "latency.get.mean",
            "latency.get.max",
            "latency.get.min"
        );
        
        // Latency values should be present and reasonable
        Object avgLatency = enhanced.get("latency.get.mean");
        assertThat(avgLatency).isNotNull();
    }

    @Test
    void testGetEnhancedMetrics_LatencyTracking() {
        when(mockSync.get(anyString())).thenReturn("\"test\"");
        when(mockSync.psetex(anyString(), anyLong(), anyString())).thenReturn("OK");
        when(mockSync.del(anyString())).thenReturn(1L);
        
        // Perform operations
        provider.get("cache", "key1", String.class);
        provider.put("cache", "key1", "value1", 60, TimeUnit.SECONDS);
        provider.delete("cache", "key1");
        
        Map<String, Object> enhanced = provider.getEnhancedMetrics();
        
        // Should have latency metrics for all operations
        assertThat(enhanced).containsKey("latency.get.mean");
        assertThat(enhanced).containsKey("latency.put.mean");
        assertThat(enhanced).containsKey("latency.delete.mean");
        
        // Latency should be positive
        double getLatency = ((Number) enhanced.get("latency.get.mean")).doubleValue();
        assertThat(getLatency).isGreaterThanOrEqualTo(0.0);
    }

    @Test
    void testGetEnhancedMetrics_ZeroOperations() {
        Map<String, Object> enhanced = provider.getEnhancedMetrics();
        
        // With zero operations, counters should be 0
        assertThat(enhanced).isNotNull();
        assertThat(enhanced.get("operations.get")).isEqualTo(0L);
        assertThat(enhanced.get("operations.put")).isEqualTo(0L);
        assertThat(enhanced.get("operations.delete")).isEqualTo(0L);
    }

    @Test
    void testGetEnhancedMetrics_AfterManyOperations() {
        when(mockSync.get(anyString())).thenReturn("\"test\"");
        
        // Perform many operations
        for (int i = 0; i < 100; i++) {
            provider.get("cache", "key" + i, String.class);
        }
        
        Map<String, Object> enhanced = provider.getEnhancedMetrics();
        
        assertThat(enhanced.get("operations.get")).isEqualTo(100L);
        
        // Latency metrics should be present
        assertThat(enhanced).containsKey("latency.get.mean");
        assertThat(enhanced).containsKey("latency.get.max");
        assertThat(enhanced).containsKey("latency.get.min");
    }

    // ==================== Integration Tests ====================

    @Test
    void testMetrics_CompleteWorkflow() {
        // Setup mocks
        when(mockSync.get(anyString()))
            .thenReturn("\"value1\"")
            .thenReturn(null)
            .thenReturn("\"value2\"");
        when(mockSync.psetex(anyString(), anyLong(), anyString())).thenReturn("OK");
        when(mockSync.del(anyString())).thenReturn(1L);
        
        // Perform various operations
        provider.get("cache", "key1", String.class);  // Hit
        provider.get("cache", "key2", String.class);  // Miss
        provider.get("cache", "key3", String.class);  // Hit
        provider.put("cache", "key4", "value4", 60, TimeUnit.SECONDS);
        provider.delete("cache", "key5");
        
        // Get basic metrics
        Map<String, Long> metrics = provider.getMetrics();
        
        assertThat(metrics.get("operations.get")).isEqualTo(3L);
        assertThat(metrics.get("operations.put")).isEqualTo(1L);
        assertThat(metrics.get("operations.delete")).isEqualTo(1L);
        assertThat(metrics.get("cache.hits")).isEqualTo(2L);
        assertThat(metrics.get("cache.misses")).isEqualTo(1L);
        
        // Get enhanced metrics with latency
        Map<String, Object> enhanced = provider.getEnhancedMetrics();
        
        assertThat(enhanced).containsKey("latency.get.mean");
        assertThat(enhanced).containsKey("latency.put.mean");
        assertThat(enhanced).containsKey("latency.delete.mean");
    }

    @Test
    void testMetrics_ConcurrentOperations() {
        when(mockSync.get(anyString())).thenReturn("\"test\"");
        when(mockSync.psetex(anyString(), anyLong(), anyString())).thenReturn("OK");
        
        // Simulate concurrent operations
        for (int i = 0; i < 10; i++) {
            provider.get("cache", "key" + i, String.class);
            provider.put("cache", "key" + i, "value" + i, 60, TimeUnit.SECONDS);
        }
        
        Map<String, Long> metrics = provider.getMetrics();
        
        assertThat(metrics.get("operations.get")).isEqualTo(10L);
        assertThat(metrics.get("operations.put")).isEqualTo(10L);
    }

    @Test
    void testMetrics_ReturnsCopyNotReference() {
        when(mockSync.get(anyString())).thenReturn("\"test\"");
        
        provider.get("cache", "key1", String.class);
        
        Map<String, Long> metrics1 = provider.getMetrics();
        Map<String, Long> metrics2 = provider.getMetrics();
        
        // Should be separate instances
        assertThat(metrics1).isNotSameAs(metrics2);
        
        // But should have the same values
        assertThat(metrics1).isEqualTo(metrics2);
    }
}
