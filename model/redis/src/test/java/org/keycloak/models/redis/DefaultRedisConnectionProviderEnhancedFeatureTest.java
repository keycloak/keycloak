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
import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.sync.RedisAdvancedClusterCommands;
import io.lettuce.core.resource.ClientResources;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.models.redis.DefaultRedisConnectionProvider;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for newly added features: connection insights, structured logging, bulk delete with Lua.
 */
class DefaultRedisConnectionProviderEnhancedFeatureTest {

    private RedisClient mockClient;
    private StatefulRedisConnection<String, String> mockConnection;
    private RedisCommands<String, String> mockSync;
    private DefaultRedisConnectionProvider standaloneProvider;
    
    private RedisClusterClient mockClusterClient;
    private StatefulRedisClusterConnection<String, String> mockClusterConnection;
    private RedisAdvancedClusterCommands<String, String> mockClusterSync;
    private DefaultRedisConnectionProvider clusterProvider;

    @BeforeEach
    void setUp() {
        // Standalone setup
        mockClient = mock(RedisClient.class);
        mockConnection = mock(StatefulRedisConnection.class);
        mockSync = mock(RedisCommands.class);
        
        when(mockConnection.sync()).thenReturn(mockSync);
        when(mockConnection.isOpen()).thenReturn(true);
        
        ClientResources standaloneResources = mock(ClientResources.class);
        when(standaloneResources.ioThreadPoolSize()).thenReturn(4);
        when(standaloneResources.computationThreadPoolSize()).thenReturn(4);
        when(mockClient.getResources()).thenReturn(standaloneResources);
        
        standaloneProvider = new DefaultRedisConnectionProvider(mockClient, mockConnection, "kc:", "redis://localhost:6379/0");
        
        // Cluster setup
        mockClusterClient = mock(RedisClusterClient.class);
        mockClusterConnection = mock(StatefulRedisClusterConnection.class);
        mockClusterSync = mock(RedisAdvancedClusterCommands.class);
        
        when(mockClusterConnection.sync()).thenReturn(mockClusterSync);
        when(mockClusterConnection.isOpen()).thenReturn(true);
        
        ClientResources clusterResources = mock(ClientResources.class);
        when(clusterResources.ioThreadPoolSize()).thenReturn(8);
        when(clusterResources.computationThreadPoolSize()).thenReturn(8);
        when(mockClusterClient.getResources()).thenReturn(clusterResources);
        
        clusterProvider = new DefaultRedisConnectionProvider(mockClusterClient, mockClusterConnection, "kc:", "redis://cluster:6379");
    }

    @AfterEach
    void tearDown() {
        if (standaloneProvider != null) {
            standaloneProvider.close();
        }
        if (clusterProvider != null) {
            clusterProvider.close();
        }
    }

    // ==================== Connection Insights Tests ====================

    @Test
    void testGetConnectionInsights_Standalone() {
        Map<String, Object> insights = standaloneProvider.getConnectionInsights();
        
        assertThat(insights).isNotNull();
        assertThat(insights.get("connection.mode")).isEqualTo("standalone");
        assertThat(insights.get("connection.info")).isEqualTo("redis://localhost:6379/0");
        assertThat(insights.get("connection.open")).isEqualTo(true);
        assertThat(insights.get("connection.type")).isEqualTo("standalone");
        assertThat(insights.get("threadpool.io.size")).isEqualTo(4);
        assertThat(insights.get("threadpool.computation.size")).isEqualTo(4);
        assertThat(insights.get("pubsub.connections.count")).isEqualTo(0);
        assertThat(insights.get("pubsub.connections.tracked")).isEqualTo(false);
        assertThat(insights.get("latency.trackers.count")).isEqualTo(0);
        assertThat(insights.get("health.status")).isEqualTo("healthy");
        assertThat(insights.get("health.lastCheckAge")).isInstanceOf(Long.class);
    }

    @Test
    void testGetConnectionInsights_Cluster() {
        Map<String, Object> insights = clusterProvider.getConnectionInsights();
        
        assertThat(insights).isNotNull();
        assertThat(insights.get("connection.mode")).isEqualTo("cluster");
        assertThat(insights.get("connection.info")).isEqualTo("redis://cluster:6379");
        assertThat(insights.get("connection.open")).isEqualTo(true);
        assertThat(insights.get("connection.type")).isEqualTo("cluster");
        assertThat(insights.get("threadpool.io.size")).isEqualTo(8);
        assertThat(insights.get("threadpool.computation.size")).isEqualTo(8);
    }

    @Test
    void testGetConnectionInsights_WithPubSubConnections() {
        // Create a Pub/Sub connection
        standaloneProvider.createPubSubConnection();
        
        Map<String, Object> insights = standaloneProvider.getConnectionInsights();
        
        assertThat(insights.get("pubsub.connections.count")).isEqualTo(1);
        assertThat(insights.get("pubsub.connections.tracked")).isEqualTo(true);
    }

    @Test
    void testGetConnectionInsights_WithLatencyTracking() {
        // Perform an operation to create latency tracker
        when(mockSync.get(anyString())).thenReturn("\"test\"");
        standaloneProvider.get("cache", "key1", String.class);
        
        Map<String, Object> insights = standaloneProvider.getConnectionInsights();
        
        assertThat(insights.get("latency.trackers.count")).isEqualTo(1);
        @SuppressWarnings("unchecked")
        List<String> trackerTypes = (List<String>) insights.get("latency.trackers.types");
        assertThat(trackerTypes).contains("get");
    }

    @Test
    void testGetConnectionInsights_UnhealthyStatus() {
        // Make provider unhealthy
        when(mockSync.ping()).thenReturn("ERROR");
        standaloneProvider.ping();
        
        Map<String, Object> insights = standaloneProvider.getConnectionInsights();
        
        assertThat(insights.get("health.status")).isEqualTo("unhealthy");
    }

    @Test
    void testGetConnectionInsights_ExceptionInResourceRetrieval() {
        // Create provider with null client to test exception handling
        RedisClient mockClientWithError = mock(RedisClient.class);
        when(mockClientWithError.getResources()).thenThrow(new RuntimeException("Resource error"));
        
        StatefulRedisConnection<String, String> mockConn = mock(StatefulRedisConnection.class);
        when(mockConn.isOpen()).thenReturn(true);
        when(mockConn.sync()).thenReturn(mockSync);
        
        DefaultRedisConnectionProvider provider = new DefaultRedisConnectionProvider(
            mockClientWithError, mockConn, "kc:", "redis://test:6379"
        );
        
        // Should not throw, should handle gracefully
        Map<String, Object> insights = provider.getConnectionInsights();
        
        assertThat(insights).isNotNull();
        assertThat(insights.get("connection.mode")).isEqualTo("standalone");
        // Thread pool info should be missing due to exception
        assertThat(insights.containsKey("threadpool.io.size")).isFalse();
    }

    // ==================== Structured Logging Tests ====================

    @Test
    void testStructuredLogging_GetOperation_Hit() {
        when(mockSync.get("kc:cache:key1")).thenReturn("\"value1\"");
        
        String result = standaloneProvider.get("cache", "key1", String.class);
        
        assertThat(result).isEqualTo("value1");
        verify(mockSync).get("kc:cache:key1");
        
        // Verify metrics are tracked
        Map<String, Long> metrics = standaloneProvider.getMetrics();
        assertThat(metrics.get("cache.hits")).isEqualTo(1L);
        assertThat(metrics.get("cache.misses")).isEqualTo(0L);
    }

    @Test
    void testStructuredLogging_GetOperation_Miss() {
        when(mockSync.get("kc:cache:key1")).thenReturn(null);
        
        String result = standaloneProvider.get("cache", "key1", String.class);
        
        assertThat(result).isNull();
        
        Map<String, Long> metrics = standaloneProvider.getMetrics();
        assertThat(metrics.get("cache.hits")).isEqualTo(0L);
        assertThat(metrics.get("cache.misses")).isEqualTo(1L);
    }

    @Test
    void testStructuredLogging_GetOperation_Error() {
        when(mockSync.get(anyString())).thenThrow(new RuntimeException("Connection lost"));
        
        assertThatThrownBy(() -> standaloneProvider.get("cache", "key1", String.class))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Connection lost");
        
        Map<String, Long> metrics = standaloneProvider.getMetrics();
        assertThat(metrics.get("errors.total")).isEqualTo(1L);
    }

    // ==================== Bulk Delete with Lua Script Tests ====================

    @Test
    void testBulkDelete_SmallBatch_UsesLuaScript() {
        List<String> keys = Arrays.asList("key1", "key2", "key3");
        
        // Mock Lua script execution
        when(mockSync.eval(anyString(), eq(ScriptOutputType.INTEGER), any(String[].class), any(String[].class)))
            .thenReturn(3L);
        
        long deleted = standaloneProvider.deleteAll("cache", keys);
        
        assertThat(deleted).isEqualTo(3L);
        verify(mockSync).eval(anyString(), eq(ScriptOutputType.INTEGER), any(String[].class), any(String[].class));
    }

    @Test
    void testBulkDelete_LargeBatch_ChunksCorrectly() {
        // Create 2500 keys (should be split into 3 chunks: 1000, 1000, 500)
        List<String> keys = new java.util.ArrayList<>();
        for (int i = 0; i < 2500; i++) {
            keys.add("key" + i);
        }
        
        when(mockSync.eval(anyString(), eq(ScriptOutputType.INTEGER), any(String[].class), any(String[].class)))
            .thenReturn(1000L, 1000L, 500L);
        
        long deleted = standaloneProvider.deleteAll("cache", keys);
        
        assertThat(deleted).isEqualTo(2500L);
        // Should be called 3 times (3 chunks)
        verify(mockSync, times(3)).eval(anyString(), eq(ScriptOutputType.INTEGER), any(String[].class), any(String[].class));
    }

    @Test
    void testBulkDelete_EmptyList() {
        long deleted = standaloneProvider.deleteAll("cache", List.of());
        
        assertThat(deleted).isEqualTo(0L);
        verify(mockSync, never()).eval(anyString(), any(), any(String[].class), any(String[].class));
    }

    @Test
    void testBulkDelete_NullList() {
        long deleted = standaloneProvider.deleteAll("cache", null);
        
        assertThat(deleted).isEqualTo(0L);
        verify(mockSync, never()).eval(anyString(), any(), any(String[].class), any(String[].class));
    }

    @Test
    void testBulkDelete_ClusterMode() {
        List<String> keys = Arrays.asList("key1", "key2", "key3");
        
        when(mockClusterSync.eval(anyString(), eq(ScriptOutputType.INTEGER), any(String[].class), any(String[].class)))
            .thenReturn(3L);
        
        long deleted = clusterProvider.deleteAll("cache", keys);
        
        assertThat(deleted).isEqualTo(3L);
        verify(mockClusterSync).eval(anyString(), eq(ScriptOutputType.INTEGER), any(String[].class), any(String[].class));
    }

    @Test
    void testBulkDelete_PartialSuccess() {
        List<String> keys = Arrays.asList("key1", "key2", "key3");
        
        // Only 2 keys actually deleted (one didn't exist)
        when(mockSync.eval(anyString(), eq(ScriptOutputType.INTEGER), any(String[].class), any(String[].class)))
            .thenReturn(2L);
        
        long deleted = standaloneProvider.deleteAll("cache", keys);
        
        assertThat(deleted).isEqualTo(2L);
    }

    @Test
    void testBulkDelete_NullReturnFromScript() {
        List<String> keys = Arrays.asList("key1");
        
        when(mockSync.eval(anyString(), eq(ScriptOutputType.INTEGER), any(String[].class), any(String[].class)))
            .thenReturn(null);
        
        long deleted = standaloneProvider.deleteAll("cache", keys);
        
        assertThat(deleted).isEqualTo(0L);
    }

    @Test
    void testBulkDelete_ErrorDuringExecution() {
        List<String> keys = Arrays.asList("key1", "key2");
        
        when(mockSync.eval(anyString(), eq(ScriptOutputType.INTEGER), any(String[].class), any(String[].class)))
            .thenThrow(new RuntimeException("Script execution failed"));
        
        assertThatThrownBy(() -> standaloneProvider.deleteAll("cache", keys))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Script execution failed");
        
        Map<String, Long> metrics = standaloneProvider.getMetrics();
        assertThat(metrics.get("errors.total")).isEqualTo(1L);
    }

    @Test
    void testBulkDelete_ExactlyOneChunk() {
        // Exactly 1000 keys - should be single chunk
        List<String> keys = new java.util.ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            keys.add("key" + i);
        }
        
        when(mockSync.eval(anyString(), eq(ScriptOutputType.INTEGER), any(String[].class), any(String[].class)))
            .thenReturn(1000L);
        
        long deleted = standaloneProvider.deleteAll("cache", keys);
        
        assertThat(deleted).isEqualTo(1000L);
        verify(mockSync, times(1)).eval(anyString(), eq(ScriptOutputType.INTEGER), any(String[].class), any(String[].class));
    }

    @Test
    void testBulkDelete_JustOverOneChunk() {
        // 1001 keys - should be 2 chunks (1000 + 1)
        List<String> keys = new java.util.ArrayList<>();
        for (int i = 0; i < 1001; i++) {
            keys.add("key" + i);
        }
        
        when(mockSync.eval(anyString(), eq(ScriptOutputType.INTEGER), any(String[].class), any(String[].class)))
            .thenReturn(1000L, 1L);
        
        long deleted = standaloneProvider.deleteAll("cache", keys);
        
        assertThat(deleted).isEqualTo(1001L);
        verify(mockSync, times(2)).eval(anyString(), eq(ScriptOutputType.INTEGER), any(String[].class), any(String[].class));
    }

    // ==================== Enhanced Metrics with Latency Tests ====================

    @Test
    void testEnhancedMetrics_IncludesLatencyData() {
        // Perform some operations to generate latency data
        when(mockSync.get(anyString())).thenReturn("\"value\"");
        when(mockSync.psetex(anyString(), anyLong(), anyString())).thenReturn("OK");
        when(mockSync.del(anyString())).thenReturn(1L);
        
        standaloneProvider.get("cache", "key1", String.class);
        standaloneProvider.put("cache", "key2", "value", 300, java.util.concurrent.TimeUnit.SECONDS);
        standaloneProvider.delete("cache", "key3");
        
        Map<String, Object> metrics = standaloneProvider.getEnhancedMetrics();
        
        assertThat(metrics).isNotNull();
        assertThat(metrics.get("operations.get")).isEqualTo(1L);
        assertThat(metrics.get("operations.put")).isEqualTo(1L);
        assertThat(metrics.get("operations.delete")).isEqualTo(1L);
        
        // Should have latency metrics for operations
        assertThat(metrics.keySet()).anyMatch(key -> key.startsWith("latency.get."));
        assertThat(metrics.keySet()).anyMatch(key -> key.startsWith("latency.put."));
        assertThat(metrics.keySet()).anyMatch(key -> key.startsWith("latency.delete."));
    }

    @Test
    void testEnhancedMetrics_LatencyPercentiles() {
        when(mockSync.get(anyString())).thenReturn("\"value\"");
        
        // Generate multiple operations to get meaningful percentiles
        for (int i = 0; i < 100; i++) {
            standaloneProvider.get("cache", "key" + i, String.class);
        }
        
        Map<String, Object> metrics = standaloneProvider.getEnhancedMetrics();
        
        // Check all percentile metrics exist
        assertThat(metrics.get("latency.get.count")).isNotNull();
        assertThat(metrics.get("latency.get.min")).isNotNull();
        assertThat(metrics.get("latency.get.max")).isNotNull();
        assertThat(metrics.get("latency.get.mean")).isNotNull();
        assertThat(metrics.get("latency.get.p50")).isNotNull();
        assertThat(metrics.get("latency.get.p95")).isNotNull();
        assertThat(metrics.get("latency.get.p99")).isNotNull();
        assertThat(metrics.get("latency.get.p999")).isNotNull();
        
        // Verify count
        assertThat(metrics.get("latency.get.count")).isEqualTo(100);
    }

    @Test
    void testResetMetrics_ClearsLatencyRecorders() {
        when(mockSync.get(anyString())).thenReturn("\"value\"");
        
        standaloneProvider.get("cache", "key1", String.class);
        
        Map<String, Object> metricsBefore = standaloneProvider.getEnhancedMetrics();
        assertThat(metricsBefore.get("operations.get")).isEqualTo(1L);
        
        standaloneProvider.resetMetrics();
        
        Map<String, Object> metricsAfter = standaloneProvider.getEnhancedMetrics();
        assertThat(metricsAfter.get("operations.get")).isEqualTo(0L);
        assertThat(metricsAfter.keySet()).noneMatch(key -> key.startsWith("latency.get."));
    }
}
