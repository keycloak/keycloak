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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for health check, reconnect, and enhanced metrics features.
 * Target: >85% coverage for new health check and metrics code.
 */
class HealthCheckAndMetricsTest {

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

    // ==================== Ping Tests ====================

    @Test
    void testPing_Success() {
        when(mockSync.ping()).thenReturn("PONG");
        
        boolean result = provider.ping();
        
        assertThat(result).isTrue();
        verify(mockSync).ping();
    }

    @Test
    void testPing_SuccessCaseInsensitive() {
        when(mockSync.ping()).thenReturn("pong"); // lowercase
        
        boolean result = provider.ping();
        
        assertThat(result).isTrue();
    }

    @Test
    void testPing_Failure() {
        when(mockSync.ping()).thenReturn("ERROR");
        
        boolean result = provider.ping();
        
        assertThat(result).isFalse();
    }

    @Test
    void testPing_Exception() {
        when(mockSync.ping()).thenThrow(new RuntimeException("Connection lost"));
        
        boolean result = provider.ping();
        
        assertThat(result).isFalse();
    }

    @Test
    void testPing_UpdatesHealthStatus() {
        when(mockSync.ping()).thenReturn("PONG");
        
        provider.ping();
        
        Map<String, Object> metrics = provider.getEnhancedMetrics();
        assertThat(metrics.get("health.healthy")).isEqualTo(true);
        assertThat(metrics.get("health.lastCheck")).isInstanceOf(Long.class);
    }

    // ==================== Reconnect Tests ====================

    @Test
    void testReconnect_Success() {
        when(mockSync.ping()).thenReturn("PONG");
        
        boolean result = provider.reconnect();
        
        assertThat(result).isTrue();
        verify(mockSync).ping(); // Should ping to verify connection
    }

    @Test
    void testReconnect_Failure() {
        when(mockSync.ping()).thenThrow(new RuntimeException("Connection failed"));
        
        boolean result = provider.reconnect();
        
        assertThat(result).isFalse();
    }

    @Test
    void testReconnect_PingReturnsFalse() {
        when(mockSync.ping()).thenReturn("ERROR");
        
        boolean result = provider.reconnect();
        
        assertThat(result).isFalse();
    }

    @Test
    void testReconnect_UpdatesHealthStatus() {
        when(mockSync.ping()).thenReturn("PONG");
        
        provider.reconnect();
        
        Map<String, Object> metrics = provider.getEnhancedMetrics();
        assertThat(metrics.get("health.healthy")).isEqualTo(true);
    }

    // ==================== Enhanced Metrics Tests ====================

    @Test
    void testGetEnhancedMetrics_InitialState() {
        Map<String, Object> metrics = provider.getEnhancedMetrics();
        
        // Basic counters
        assertThat(metrics.get("operations.get")).isEqualTo(0L);
        assertThat(metrics.get("operations.put")).isEqualTo(0L);
        assertThat(metrics.get("operations.delete")).isEqualTo(0L);
        assertThat(metrics.get("operations.scan")).isEqualTo(0L);
        
        // Batch counters
        assertThat(metrics.get("operations.batchGet")).isEqualTo(0L);
        assertThat(metrics.get("operations.batchPut")).isEqualTo(0L);
        assertThat(metrics.get("operations.batchDelete")).isEqualTo(0L);
        
        // Cache metrics
        assertThat(metrics.get("cache.hits")).isEqualTo(0L);
        assertThat(metrics.get("cache.misses")).isEqualTo(0L);
        assertThat(metrics.get("cache.hitRate")).isEqualTo(0.0);
        
        // Errors
        assertThat(metrics.get("errors.total")).isEqualTo(0L);
        
        // Health
        assertThat(metrics.get("health.healthy")).isEqualTo(true);
        assertThat(metrics.get("health.lastCheck")).isInstanceOf(Long.class);
    }

    @Test
    void testGetEnhancedMetrics_WithOperations() {
        when(mockSync.get(anyString())).thenReturn("\"value\"", (String) null);
        when(mockSync.psetex(anyString(), anyLong(), anyString())).thenReturn("OK");
        when(mockSync.del(anyString())).thenReturn(1L);
        
        // Perform operations
        provider.get("cache", "key1", String.class); // Hit
        provider.get("cache", "key2", String.class); // Miss
        provider.put("cache", "key3", "value", 60, java.util.concurrent.TimeUnit.SECONDS);
        provider.delete("cache", "key4");
        
        Map<String, Object> metrics = provider.getEnhancedMetrics();
        
        assertThat(metrics.get("operations.get")).isEqualTo(2L);
        assertThat(metrics.get("operations.put")).isEqualTo(1L);
        assertThat(metrics.get("operations.delete")).isEqualTo(1L);
        assertThat(metrics.get("cache.hits")).isEqualTo(1L);
        assertThat(metrics.get("cache.misses")).isEqualTo(1L);
        
        Double hitRate = (Double) metrics.get("cache.hitRate");
        assertThat(hitRate).isEqualTo(50.0); // 1 hit / 2 gets = 50%
    }

    @Test
    void testGetEnhancedMetrics_LatencyTracking() {
        when(mockSync.get(anyString())).thenReturn("\"value\"");
        
        // Perform operations to generate latency data
        provider.get("cache", "key1", String.class);
        provider.get("cache", "key2", String.class);
        provider.get("cache", "key3", String.class);
        
        Map<String, Object> metrics = provider.getEnhancedMetrics();
        
        // Check that latency metrics exist (HdrHistogram provides mean instead of avg)
        assertThat(metrics).containsKey("latency.get.min");
        assertThat(metrics).containsKey("latency.get.max");
        assertThat(metrics).containsKey("latency.get.mean");
        assertThat(metrics).containsKey("latency.get.p50");
        assertThat(metrics).containsKey("latency.get.p95");
        assertThat(metrics).containsKey("latency.get.p99");
        assertThat(metrics).containsKey("latency.get.count");
        
        // Verify values are in milliseconds (should be small numbers)
        Double minLatency = (Double) metrics.get("latency.get.min");
        assertThat(minLatency).isNotNull().isGreaterThanOrEqualTo(0.0);
        
        Integer count = (Integer) metrics.get("latency.get.count");
        assertThat(count).isEqualTo(3);
    }

    @Test
    void testGetEnhancedMetrics_NoLatencyWhenNoOperations() {
        Map<String, Object> metrics = provider.getEnhancedMetrics();
        
        // No latency metrics should exist for operations that haven't been called
        assertThat(metrics).doesNotContainKey("latency.get.min");
        assertThat(metrics).doesNotContainKey("latency.put.min");
    }

    // ==================== Reset Metrics Tests ====================

    @Test
    void testResetMetrics() {
        when(mockSync.get(anyString())).thenReturn("\"value\"");
        when(mockSync.psetex(anyString(), anyLong(), anyString())).thenReturn("OK");
        
        // Perform operations
        provider.get("cache", "key1", String.class);
        provider.put("cache", "key2", "value", 60, java.util.concurrent.TimeUnit.SECONDS);
        
        // Verify metrics exist
        Map<String, Object> metricsBefore = provider.getEnhancedMetrics();
        assertThat(metricsBefore.get("operations.get")).isEqualTo(1L);
        assertThat(metricsBefore.get("operations.put")).isEqualTo(1L);
        
        // Reset
        provider.resetMetrics();
        
        // Verify all counters are reset
        Map<String, Object> metricsAfter = provider.getEnhancedMetrics();
        assertThat(metricsAfter.get("operations.get")).isEqualTo(0L);
        assertThat(metricsAfter.get("operations.put")).isEqualTo(0L);
        assertThat(metricsAfter.get("operations.delete")).isEqualTo(0L);
        assertThat(metricsAfter.get("operations.scan")).isEqualTo(0L);
        assertThat(metricsAfter.get("operations.batchGet")).isEqualTo(0L);
        assertThat(metricsAfter.get("operations.batchPut")).isEqualTo(0L);
        assertThat(metricsAfter.get("operations.batchDelete")).isEqualTo(0L);
        assertThat(metricsAfter.get("cache.hits")).isEqualTo(0L);
        assertThat(metricsAfter.get("cache.misses")).isEqualTo(0L);
        assertThat(metricsAfter.get("errors.total")).isEqualTo(0L);
        
        // Latency metrics should be cleared
        assertThat(metricsAfter).doesNotContainKey("latency.get.min");
        assertThat(metricsAfter).doesNotContainKey("latency.put.min");
    }

    @Test
    void testResetMetrics_CanCollectNewMetrics() {
        when(mockSync.get(anyString())).thenReturn("\"value\"");
        
        // Perform operation
        provider.get("cache", "key1", String.class);
        
        // Reset
        provider.resetMetrics();
        
        // Perform new operation
        provider.get("cache", "key2", String.class);
        
        Map<String, Object> metrics = provider.getEnhancedMetrics();
        assertThat(metrics.get("operations.get")).isEqualTo(1L); // Only the new operation
    }

    // ==================== Latency Bucket Management Tests ====================

    @Test
    void testLatencyBuckets_HandlesLargeVolume() {
        when(mockSync.get(anyString())).thenReturn("\"value\"");
        
        // Perform more than 1000 operations
        for (int i = 0; i < 1500; i++) {
            provider.get("cache", "key" + i, String.class);
        }
        
        Map<String, Object> metrics = provider.getEnhancedMetrics();
        Integer count = (Integer) metrics.get("latency.get.count");
        
        // HdrHistogram keeps all samples in histogram buckets (not limited to 1000)
        // Verify all operations were recorded
        assertThat(count).isEqualTo(1500);
    }

    @Test
    void testMetrics_HitRateCalculation() {
        when(mockSync.get(anyString()))
            .thenReturn("\"value\"", "\"value\"", "\"value\"", (String) null);
        
        // 3 hits, 1 miss = 75% hit rate
        provider.get("cache", "key1", String.class);
        provider.get("cache", "key2", String.class);
        provider.get("cache", "key3", String.class);
        provider.get("cache", "key4", String.class);
        
        Map<String, Object> metrics = provider.getEnhancedMetrics();
        Double hitRate = (Double) metrics.get("cache.hitRate");
        
        assertThat(hitRate).isEqualTo(75.0); // 3/4 = 75%
    }

    @Test
    void testMetrics_ErrorTracking() {
        when(mockSync.get(anyString())).thenThrow(new RuntimeException("Error"));
        
        // Perform operation that fails
        try {
            provider.get("cache", "key1", String.class);
        } catch (Exception e) {
            // Expected
        }
        
        Map<String, Object> metrics = provider.getEnhancedMetrics();
        assertThat(metrics.get("errors.total")).isEqualTo(1L);
    }

    // ==================== Integration Tests ====================

    @Test
    void testHealthAndMetricsTogether() {
        when(mockSync.ping()).thenReturn("PONG");
        when(mockSync.get(anyString())).thenReturn("\"value\"");
        
        // Check health
        boolean healthy = provider.ping();
        assertThat(healthy).isTrue();
        
        // Perform operation
        provider.get("cache", "key1", String.class);
        
        // Get metrics
        Map<String, Object> metrics = provider.getEnhancedMetrics();
        
        // Verify both health and operation metrics
        assertThat(metrics.get("health.healthy")).isEqualTo(true);
        assertThat(metrics.get("operations.get")).isEqualTo(1L);
        assertThat(metrics.get("cache.hits")).isEqualTo(1L);
    }
}
