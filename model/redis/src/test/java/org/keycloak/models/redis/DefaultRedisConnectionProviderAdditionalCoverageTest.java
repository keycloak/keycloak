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
import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.sync.RedisAdvancedClusterCommands;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.models.redis.DefaultRedisConnectionProvider;
import org.keycloak.models.redis.RedisConnectionProvider;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Additional tests to push coverage above 82%.
 * Focuses on uncovered error paths, edge cases, and cluster-specific scenarios.
 */
class DefaultRedisConnectionProviderAdditionalCoverageTest {

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

    // ==================== replaceWithVersion() Edge Cases ====================

    @Test
    void testReplaceWithVersion_Success() {
        when(mockSync.eval(anyString(), eq(ScriptOutputType.INTEGER), 
            any(String[].class), any(String[].class))).thenReturn(1L);
        
        boolean result = provider.replaceWithVersion("cache", "key1", "newValue", 5L, 60, TimeUnit.SECONDS);
        
        assertThat(result).isTrue();
    }

    @Test
    void testReplaceWithVersion_OptimisticLockFailure() {
        when(mockSync.eval(anyString(), eq(ScriptOutputType.INTEGER), 
            any(String[].class), any(String[].class))).thenReturn(0L);
        
        boolean result = provider.replaceWithVersion("cache", "key1", "newValue", 5L, 60, TimeUnit.SECONDS);
        
        assertThat(result).isFalse();
    }

    @Test
    void testReplaceWithVersion_NullResult() {
        when(mockSync.eval(anyString(), eq(ScriptOutputType.INTEGER), 
            any(String[].class), any(String[].class))).thenReturn(null);
        
        boolean result = provider.replaceWithVersion("cache", "key1", "newValue", 5L, 60, TimeUnit.SECONDS);
        
        assertThat(result).isFalse();
    }

    @Test
    void testReplaceWithVersion_ScriptException() {
        when(mockSync.eval(anyString(), eq(ScriptOutputType.INTEGER), 
            any(String[].class), any(String[].class)))
            .thenThrow(new RuntimeException("Script execution failed"));
        
        // Script exceptions are logged but return false gracefully
        assertThatCode(() -> {
            boolean result = provider.replaceWithVersion("cache", "key1", "newValue", 5L, 60, TimeUnit.SECONDS);
            assertThat(result).isFalse();
        }).doesNotThrowAnyException();
    }

    @Test
    void testReplaceWithVersion_ClusterMode() {
        RedisClusterClient mockClusterClient = mock(RedisClusterClient.class);
        StatefulRedisClusterConnection<String, String> mockClusterConn = mock(StatefulRedisClusterConnection.class);
        RedisAdvancedClusterCommands<String, String> mockClusterSync = mock(RedisAdvancedClusterCommands.class);
        
        when(mockClusterConn.sync()).thenReturn(mockClusterSync);
        when(mockClusterSync.eval(anyString(), eq(ScriptOutputType.INTEGER), 
            any(String[].class), any(String[].class))).thenReturn(1L);
        
        DefaultRedisConnectionProvider clusterProvider = new DefaultRedisConnectionProvider(
            mockClusterClient, mockClusterConn, "kc:", "redis://localhost:6379/0"
        );
        
        boolean result = clusterProvider.replaceWithVersion("cache", "key1", "value", 1L, 60, TimeUnit.SECONDS);
        
        assertThat(result).isTrue();
    }

    // ==================== getWithVersion() Tests ====================

    @Test
    void testGetWithVersion_BothExist() {
        when(mockSync.get("kc:cache:key1")).thenReturn("\"testValue\"");
        when(mockSync.get("kc:cache:_ver:key1")).thenReturn("10");
        
        RedisConnectionProvider.VersionedValue<String> result = provider.getWithVersion("cache", "key1", String.class);
        
        assertThat(result).isNotNull();
        assertThat(result.value()).isEqualTo("testValue");
        assertThat(result.version()).isEqualTo(10L);
    }

    @Test
    void testGetWithVersion_ValueExistsNoVersion() {
        when(mockSync.get("kc:cache:key1")).thenReturn("\"testValue\"");
        when(mockSync.get("kc:cache:_ver:key1")).thenReturn(null);
        
        RedisConnectionProvider.VersionedValue<String> result = provider.getWithVersion("cache", "key1", String.class);
        
        assertThat(result).isNotNull();
        assertThat(result.value()).isEqualTo("testValue");
        assertThat(result.version()).isEqualTo(0L);
    }

    @Test
    void testGetWithVersion_ClusterMode() {
        RedisClusterClient mockClusterClient = mock(RedisClusterClient.class);
        StatefulRedisClusterConnection<String, String> mockClusterConn = mock(StatefulRedisClusterConnection.class);
        RedisAdvancedClusterCommands<String, String> mockClusterSync = mock(RedisAdvancedClusterCommands.class);
        
        when(mockClusterConn.sync()).thenReturn(mockClusterSync);
        when(mockClusterSync.get("kc:cache:{key1}")).thenReturn("\"value\"");
        // Version key doesn't use hash tags in the implementation
        when(mockClusterSync.get("kc:cache:_ver:key1")).thenReturn("5");
        
        DefaultRedisConnectionProvider clusterProvider = new DefaultRedisConnectionProvider(
            mockClusterClient, mockClusterConn, "kc:", "redis://localhost:6379/0"
        );
        
        RedisConnectionProvider.VersionedValue<String> result = clusterProvider.getWithVersion("cache", "key1", String.class);
        
        assertThat(result).isNotNull();
        assertThat(result.version()).isEqualTo(5L);
    }

    // ==================== remove() Tests ====================

    @Test
    void testRemove_ValueExists() {
        when(mockSync.get("kc:cache:key1")).thenReturn("\"oldValue\"");
        when(mockSync.del("kc:cache:key1")).thenReturn(1L);
        
        String result = provider.remove("cache", "key1", String.class);
        
        assertThat(result).isEqualTo("oldValue");
    }

    @Test
    void testRemove_ValueDoesNotExist() {
        when(mockSync.get("kc:cache:key1")).thenReturn(null);
        when(mockSync.del("kc:cache:key1")).thenReturn(0L);
        
        String result = provider.remove("cache", "key1", String.class);
        
        assertThat(result).isNull();
    }

    @Test
    void testRemove_ClusterMode() {
        RedisClusterClient mockClusterClient = mock(RedisClusterClient.class);
        StatefulRedisClusterConnection<String, String> mockClusterConn = mock(StatefulRedisClusterConnection.class);
        RedisAdvancedClusterCommands<String, String> mockClusterSync = mock(RedisAdvancedClusterCommands.class);
        
        when(mockClusterConn.sync()).thenReturn(mockClusterSync);
        when(mockClusterSync.get("kc:cache:{key1}")).thenReturn("\"value\"");
        when(mockClusterSync.del("kc:cache:{key1}")).thenReturn(1L);
        
        DefaultRedisConnectionProvider clusterProvider = new DefaultRedisConnectionProvider(
            mockClusterClient, mockClusterConn, "kc:", "redis://localhost:6379/0"
        );
        
        String result = clusterProvider.remove("cache", "key1", String.class);
        
        assertThat(result).isEqualTo("value");
    }

    // ==================== delete() Tests ====================

    @Test
    void testDelete_KeyExists() {
        when(mockSync.del("kc:cache:key1")).thenReturn(1L);
        
        boolean result = provider.delete("cache", "key1");
        
        assertThat(result).isTrue();
    }

    @Test
    void testDelete_KeyDoesNotExist() {
        when(mockSync.del("kc:cache:key1")).thenReturn(0L);
        
        boolean result = provider.delete("cache", "key1");
        
        assertThat(result).isFalse();
    }

    @Test
    void testDelete_ClusterMode() {
        RedisClusterClient mockClusterClient = mock(RedisClusterClient.class);
        StatefulRedisClusterConnection<String, String> mockClusterConn = mock(StatefulRedisClusterConnection.class);
        RedisAdvancedClusterCommands<String, String> mockClusterSync = mock(RedisAdvancedClusterCommands.class);
        
        when(mockClusterConn.sync()).thenReturn(mockClusterSync);
        when(mockClusterSync.del("kc:cache:{key1}")).thenReturn(1L);
        
        DefaultRedisConnectionProvider clusterProvider = new DefaultRedisConnectionProvider(
            mockClusterClient, mockClusterConn, "kc:", "redis://localhost:6379/0"
        );
        
        boolean result = clusterProvider.delete("cache", "key1");
        
        assertThat(result).isTrue();
    }

    // ==================== put() Tests ====================

    @Test
    void testPut_Success() {
        when(mockSync.psetex(eq("kc:cache:key1"), eq(60000L), anyString())).thenReturn("OK");
        
        assertThatCode(() -> provider.put("cache", "key1", "value1", 60, TimeUnit.SECONDS))
            .doesNotThrowAnyException();
        
        verify(mockSync).psetex(eq("kc:cache:key1"), eq(60000L), anyString());
    }

    @Test
    void testPut_ClusterMode() {
        RedisClusterClient mockClusterClient = mock(RedisClusterClient.class);
        StatefulRedisClusterConnection<String, String> mockClusterConn = mock(StatefulRedisClusterConnection.class);
        RedisAdvancedClusterCommands<String, String> mockClusterSync = mock(RedisAdvancedClusterCommands.class);
        
        when(mockClusterConn.sync()).thenReturn(mockClusterSync);
        when(mockClusterSync.psetex(anyString(), anyLong(), anyString())).thenReturn("OK");
        
        DefaultRedisConnectionProvider clusterProvider = new DefaultRedisConnectionProvider(
            mockClusterClient, mockClusterConn, "kc:", "redis://localhost:6379/0"
        );
        
        clusterProvider.put("cache", "key1", "value1", 60, TimeUnit.SECONDS);
        
        verify(mockClusterSync).psetex(eq("kc:cache:{key1}"), eq(60000L), anyString());
    }

    // ==================== get() Cache Hit/Miss Tests ====================

    @Test
    void testGet_CacheHit() {
        when(mockSync.get("kc:cache:key1")).thenReturn("\"value1\"");
        
        String result = provider.get("cache", "key1", String.class);
        
        assertThat(result).isEqualTo("value1");
        
        // Verify cache hit metric
        assertThat(provider.getMetrics().get("cache.hits")).isEqualTo(1L);
        assertThat(provider.getMetrics().get("cache.misses")).isEqualTo(0L);
    }

    @Test
    void testGet_CacheMiss() {
        when(mockSync.get("kc:cache:key1")).thenReturn(null);
        
        String result = provider.get("cache", "key1", String.class);
        
        assertThat(result).isNull();
        
        // Verify cache miss metric
        assertThat(provider.getMetrics().get("cache.hits")).isEqualTo(0L);
        assertThat(provider.getMetrics().get("cache.misses")).isEqualTo(1L);
    }

    @Test
    void testGet_ClusterMode() {
        RedisClusterClient mockClusterClient = mock(RedisClusterClient.class);
        StatefulRedisClusterConnection<String, String> mockClusterConn = mock(StatefulRedisClusterConnection.class);
        RedisAdvancedClusterCommands<String, String> mockClusterSync = mock(RedisAdvancedClusterCommands.class);
        
        when(mockClusterConn.sync()).thenReturn(mockClusterSync);
        when(mockClusterSync.get("kc:cache:{key1}")).thenReturn("\"value1\"");
        
        DefaultRedisConnectionProvider clusterProvider = new DefaultRedisConnectionProvider(
            mockClusterClient, mockClusterConn, "kc:", "redis://localhost:6379/0"
        );
        
        String result = clusterProvider.get("cache", "key1", String.class);
        
        assertThat(result).isEqualTo("value1");
        verify(mockClusterSync).get("kc:cache:{key1}");
    }

    // ==================== removeByPattern() with DEL ====================

    @Test
    void testRemoveByPattern_UsesDelCommand() {
        KeyScanCursor<String> cursor = mock(KeyScanCursor.class);
        when(cursor.isFinished()).thenReturn(true);
        when(cursor.getKeys()).thenReturn(Arrays.asList("kc:cache:key1", "kc:cache:key2", "kc:cache:key3"));
        when(mockSync.scan(any(ScanArgs.class))).thenReturn(cursor);
        when(mockSync.del("kc:cache:key1", "kc:cache:key2", "kc:cache:key3")).thenReturn(3L);
        
        long removed = provider.removeByPattern("cache", "*");
        
        assertThat(removed).isEqualTo(3L);
        verify(mockSync).del("kc:cache:key1", "kc:cache:key2", "kc:cache:key3");
    }

    @Test
    void testRemoveByPattern_ClusterMode() {
        RedisClusterClient mockClusterClient = mock(RedisClusterClient.class);
        StatefulRedisClusterConnection<String, String> mockClusterConn = mock(StatefulRedisClusterConnection.class);
        RedisAdvancedClusterCommands<String, String> mockClusterSync = mock(RedisAdvancedClusterCommands.class);
        
        when(mockClusterConn.sync()).thenReturn(mockClusterSync);
        
        // Mock the cluster scanning (returns empty for simplicity)
        when(mockClusterConn.getPartitions()).thenReturn(new io.lettuce.core.cluster.models.partitions.Partitions());
        
        DefaultRedisConnectionProvider clusterProvider = new DefaultRedisConnectionProvider(
            mockClusterClient, mockClusterConn, "kc:", "redis://localhost:6379/0"
        );
        
        long removed = clusterProvider.removeByPattern("cache", "*");
        
        // Should return 0 for empty partitions
        assertThat(removed).isEqualTo(0L);
    }

    // ==================== Constructor with Custom Prefix ====================

    @Test
    void testConstructor_CustomPrefix() {
        DefaultRedisConnectionProvider customProvider = new DefaultRedisConnectionProvider(
            mockClient, mockConnection, "custom:", "redis://localhost:6379/0"
        );
        
        String key = customProvider.getCacheKey("cache", "key1");
        assertThat(key).isEqualTo("custom:cache:key1");
    }

    @Test
    void testConstructor_NullPrefix() {
        DefaultRedisConnectionProvider customProvider = new DefaultRedisConnectionProvider(
            mockClient, mockConnection, null, "redis://localhost:6379/0"
        );
        
        String key = customProvider.getCacheKey("cache", "key1");
        assertThat(key).isEqualTo("kc:cache:key1"); // Should use default
    }

    // ==================== Metrics Accumulation ====================

    @Test
    void testMetrics_MultipleOperations() {
        when(mockSync.get(anyString())).thenReturn("\"value\"");
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
        
        var metrics = provider.getMetrics();
        assertThat(metrics.get("operations.get")).isEqualTo(5L);
        assertThat(metrics.get("operations.put")).isEqualTo(3L);
        assertThat(metrics.get("operations.delete")).isEqualTo(2L);
        assertThat(metrics.get("cache.hits")).isEqualTo(5L);
    }

    // ==================== getVersionKey() Tests ====================

    @Test
    void testGetVersionKey_Standalone() {
        // Test that version key is generated correctly
        when(mockSync.get("kc:cache:key1")).thenReturn("\"value\"");
        when(mockSync.get("kc:cache:_ver:key1")).thenReturn("1");
        
        var result = provider.getWithVersion("cache", "key1", String.class);
        
        assertThat(result).isNotNull();
        verify(mockSync).get("kc:cache:_ver:key1");
    }

    // ==================== Additional Cluster Mode Tests ====================

    @Test
    void testGetCacheKey_WithHashTags() {
        RedisClusterClient mockClusterClient = mock(RedisClusterClient.class);
        StatefulRedisClusterConnection<String, String> mockClusterConn = mock(StatefulRedisClusterConnection.class);
        RedisAdvancedClusterCommands<String, String> mockClusterSync = mock(RedisAdvancedClusterCommands.class);
        
        when(mockClusterConn.sync()).thenReturn(mockClusterSync);
        
        DefaultRedisConnectionProvider clusterProvider = new DefaultRedisConnectionProvider(
            mockClusterClient, mockClusterConn, "kc:", "redis://localhost:6379/0"
        );
        
        // Verify hash tags are used in cluster mode
        String key1 = clusterProvider.getCacheKey("sessions", "abc123");
        String key2 = clusterProvider.getCacheKey("tokens", "xyz789");
        
        assertThat(key1).isEqualTo("kc:sessions:{abc123}");
        assertThat(key2).isEqualTo("kc:tokens:{xyz789}");
    }

    @Test
    void testAsyncOperations_ClusterMode() {
        RedisClusterClient mockClusterClient = mock(RedisClusterClient.class);
        StatefulRedisClusterConnection<String, String> mockClusterConn = mock(StatefulRedisClusterConnection.class);
        RedisAdvancedClusterCommands<String, String> mockClusterSync = mock(RedisAdvancedClusterCommands.class);
        
        when(mockClusterConn.sync()).thenReturn(mockClusterSync);
        
        DefaultRedisConnectionProvider clusterProvider = new DefaultRedisConnectionProvider(
            mockClusterClient, mockClusterConn, "kc:", "redis://cluster:6379/0"
        );
        
        // Verify cluster mode flag
        assertThat(clusterProvider.isClusterMode()).isTrue();
        assertThat(clusterProvider.getConnectionInfo()).isEqualTo("redis://cluster:6379/0");
    }

    @Test
    void testPutIfAbsent_ReturnsExistingValue() {
        when(mockSync.set(anyString(), anyString(), any())).thenReturn(null); // Key exists
        when(mockSync.get("kc:cache:key1")).thenReturn("\"existingValue\"");
        
        String result = provider.putIfAbsent("cache", "key1", "newValue", 60, TimeUnit.SECONDS);
        
        assertThat(result).isEqualTo("existingValue");
    }
}
