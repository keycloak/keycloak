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
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.sync.RedisAdvancedClusterCommands;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.models.redis.DefaultRedisConnectionProvider;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Edge cases, error handling, and cluster-specific tests for DefaultRedisConnectionProvider.
 * Targets uncovered code paths to push coverage above 80%.
 */
class EdgeCasesAndClusterTest {

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

    // ==================== scanKeys() Edge Cases ====================

    @Test
    void testScanKeys_SinglePage() {
        KeyScanCursor<String> cursor = mock(KeyScanCursor.class);
        when(cursor.isFinished()).thenReturn(true);
        when(cursor.getKeys()).thenReturn(Arrays.asList("kc:cache:key1", "kc:cache:key2"));
        when(mockSync.scan(any(ScanArgs.class))).thenReturn(cursor);
        
        List<String> keys = provider.scanKeys("cache", "*");
        
        assertThat(keys).hasSize(2);
        assertThat(keys).containsExactlyInAnyOrder("key1", "key2");
        verify(mockSync).scan(any(ScanArgs.class));
    }

    @Test
    void testScanKeys_MultiplePages() {
        KeyScanCursor<String> cursor1 = mock(KeyScanCursor.class);
        KeyScanCursor<String> cursor2 = mock(KeyScanCursor.class);
        KeyScanCursor<String> cursor3 = mock(KeyScanCursor.class);
        
        when(cursor1.isFinished()).thenReturn(false);
        when(cursor1.getKeys()).thenReturn(Arrays.asList("kc:cache:key1", "kc:cache:key2"));
        
        when(cursor2.isFinished()).thenReturn(false);
        when(cursor2.getKeys()).thenReturn(Arrays.asList("kc:cache:key3"));
        
        when(cursor3.isFinished()).thenReturn(true);
        when(cursor3.getKeys()).thenReturn(Arrays.asList("kc:cache:key4", "kc:cache:key5"));
        
        when(mockSync.scan(any(ScanArgs.class))).thenReturn(cursor1);
        when(mockSync.scan(eq(cursor1), any(ScanArgs.class))).thenReturn(cursor2);
        when(mockSync.scan(eq(cursor2), any(ScanArgs.class))).thenReturn(cursor3);
        
        List<String> keys = provider.scanKeys("cache", "*");
        
        assertThat(keys).hasSize(5);
        assertThat(keys).contains("key1", "key2", "key3", "key4", "key5");
        verify(mockSync).scan(any(ScanArgs.class));
        verify(mockSync, times(2)).scan(any(KeyScanCursor.class), any(ScanArgs.class));
    }

    @Test
    void testScanKeys_EmptyResult() {
        KeyScanCursor<String> cursor = mock(KeyScanCursor.class);
        when(cursor.isFinished()).thenReturn(true);
        when(cursor.getKeys()).thenReturn(List.of());
        when(mockSync.scan(any(ScanArgs.class))).thenReturn(cursor);
        
        List<String> keys = provider.scanKeys("cache", "nonexistent*");
        
        assertThat(keys).isEmpty();
    }

    @Test
    void testScanKeys_WithPattern() {
        KeyScanCursor<String> cursor = mock(KeyScanCursor.class);
        when(cursor.isFinished()).thenReturn(true);
        when(cursor.getKeys()).thenReturn(Arrays.asList(
            "kc:cache:user:123",
            "kc:cache:user:456"
        ));
        when(mockSync.scan(any(ScanArgs.class))).thenReturn(cursor);
        
        List<String> keys = provider.scanKeys("cache", "user:*");
        
        assertThat(keys).hasSize(2);
        assertThat(keys).containsExactlyInAnyOrder("user:123", "user:456");
    }

    @Test
    void testScanKeys_ExceptionHandling() {
        when(mockSync.scan(any(ScanArgs.class)))
            .thenThrow(new RuntimeException("Connection lost"));
        
        List<String> keys = provider.scanKeys("cache", "*");
        
        // Should return empty list on error
        assertThat(keys).isEmpty();
    }

    // Note: Cluster mode scanning uses getPartitions() and node connections
    // which requires complex mocking of partition structures. 
    // Cluster-specific getCacheKey() test below validates cluster mode behavior.

    // ==================== containsKey() Tests ====================

    @Test
    void testContainsKey_Exists() {
        when(mockSync.exists("kc:cache:key1")).thenReturn(1L);
        
        boolean exists = provider.containsKey("cache", "key1");
        
        assertThat(exists).isTrue();
    }

    @Test
    void testContainsKey_NotExists() {
        when(mockSync.exists("kc:cache:key1")).thenReturn(0L);
        
        boolean exists = provider.containsKey("cache", "key1");
        
        assertThat(exists).isFalse();
    }

    @Test
    void testContainsKey_NullResult() {
        when(mockSync.exists("kc:cache:key1")).thenReturn(null);
        
        boolean exists = provider.containsKey("cache", "key1");
        
        assertThat(exists).isFalse();
    }

    @Test
    void testContainsKey_ClusterMode() {
        RedisClusterClient mockClusterClient = mock(RedisClusterClient.class);
        StatefulRedisClusterConnection<String, String> mockClusterConn = mock(StatefulRedisClusterConnection.class);
        RedisAdvancedClusterCommands<String, String> mockClusterSync = mock(RedisAdvancedClusterCommands.class);
        
        when(mockClusterConn.sync()).thenReturn(mockClusterSync);
        when(mockClusterSync.exists("kc:cache:{key1}")).thenReturn(1L);
        
        DefaultRedisConnectionProvider clusterProvider = new DefaultRedisConnectionProvider(
            mockClusterClient, mockClusterConn, "kc:", "redis://localhost:6379/0"
        );
        
        boolean exists = clusterProvider.containsKey("cache", "key1");
        
        assertThat(exists).isTrue();
        verify(mockClusterSync).exists("kc:cache:{key1}");
    }

    // ==================== removeByPattern() Edge Cases ====================

    @Test
    void testRemoveByPattern_Success() {
        KeyScanCursor<String> cursor = mock(KeyScanCursor.class);
        when(cursor.isFinished()).thenReturn(true);
        when(cursor.getKeys()).thenReturn(Arrays.asList("kc:cache:key1", "kc:cache:key2"));
        when(mockSync.scan(any(ScanArgs.class))).thenReturn(cursor);
        // Mock del command for standalone mode
        when(mockSync.del("kc:cache:key1", "kc:cache:key2")).thenReturn(2L);
        
        long removed = provider.removeByPattern("cache", "key*");
        
        assertThat(removed).isEqualTo(2L);
    }

    @Test
    void testRemoveByPattern_NoMatches() {
        KeyScanCursor<String> cursor = mock(KeyScanCursor.class);
        when(cursor.isFinished()).thenReturn(true);
        when(cursor.getKeys()).thenReturn(List.of());
        when(mockSync.scan(any(ScanArgs.class))).thenReturn(cursor);
        
        long removed = provider.removeByPattern("cache", "nonexistent*");
        
        assertThat(removed).isEqualTo(0L);
        verify(mockSync, never()).eval(anyString(), any(), any(), any());
    }

    @Test
    void testRemoveByPattern_LargeDataset() {
        // Simulate keys across multiple scan pages
        KeyScanCursor<String> cursor1 = mock(KeyScanCursor.class);
        KeyScanCursor<String> cursor2 = mock(KeyScanCursor.class);
        
        List<String> page1Keys = Arrays.asList("kc:cache:key1", "kc:cache:key2", "kc:cache:key3");
        List<String> page2Keys = Arrays.asList("kc:cache:key4", "kc:cache:key5");
        
        when(cursor1.isFinished()).thenReturn(false);
        when(cursor1.getKeys()).thenReturn(page1Keys);
        
        when(cursor2.isFinished()).thenReturn(true);
        when(cursor2.getKeys()).thenReturn(page2Keys);
        
        when(mockSync.scan(any(ScanArgs.class))).thenReturn(cursor1);
        when(mockSync.scan(eq(cursor1), any(ScanArgs.class))).thenReturn(cursor2);
        // Mock del for all keys
        when(mockSync.del(anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(5L);
        
        long removed = provider.removeByPattern("cache", "*");
        
        assertThat(removed).isEqualTo(5L);
    }

    @Test
    void testRemoveByPattern_ScanException() {
        when(mockSync.scan(any(ScanArgs.class)))
            .thenThrow(new RuntimeException("Scan failed"));
        
        long removed = provider.removeByPattern("cache", "*");
        
        // Should return 0 on error
        assertThat(removed).isEqualTo(0L);
    }

    // ==================== publish() Tests ====================

    @Test
    void testPublish_Standalone() {
        when(mockSync.publish("channel1", "message1")).thenReturn(1L);
        
        assertThatCode(() -> provider.publish("channel1", "message1"))
            .doesNotThrowAnyException();
        
        verify(mockSync).publish("channel1", "message1");
    }

    @Test
    void testPublish_ClusterMode() {
        RedisClusterClient mockClusterClient = mock(RedisClusterClient.class);
        StatefulRedisClusterConnection<String, String> mockClusterConn = mock(StatefulRedisClusterConnection.class);
        RedisAdvancedClusterCommands<String, String> mockClusterSync = mock(RedisAdvancedClusterCommands.class);
        
        when(mockClusterConn.sync()).thenReturn(mockClusterSync);
        when(mockClusterSync.publish("channel1", "message1")).thenReturn(1L);
        
        DefaultRedisConnectionProvider clusterProvider = new DefaultRedisConnectionProvider(
            mockClusterClient, mockClusterConn, "kc:", "redis://localhost:6379/0"
        );
        
        clusterProvider.publish("channel1", "message1");
        
        verify(mockClusterSync).publish("channel1", "message1");
    }

    // ==================== getCacheKey() Tests ====================

    @Test
    void testGetCacheKey_Standalone() {
        String key = provider.getCacheKey("cache", "mykey");
        
        assertThat(key).isEqualTo("kc:cache:mykey");
    }

    @Test
    void testGetCacheKey_ClusterMode_UsesHashTags() {
        RedisClusterClient mockClusterClient = mock(RedisClusterClient.class);
        StatefulRedisClusterConnection<String, String> mockClusterConn = mock(StatefulRedisClusterConnection.class);
        RedisAdvancedClusterCommands<String, String> mockClusterSync = mock(RedisAdvancedClusterCommands.class);
        
        when(mockClusterConn.sync()).thenReturn(mockClusterSync);
        
        DefaultRedisConnectionProvider clusterProvider = new DefaultRedisConnectionProvider(
            mockClusterClient, mockClusterConn, "kc:", "redis://localhost:6379/0"
        );
        
        String key = clusterProvider.getCacheKey("cache", "mykey");
        
        // Cluster mode should wrap key in hash tags
        assertThat(key).isEqualTo("kc:cache:{mykey}");
    }

    // ==================== isClusterMode() and getConnectionInfo() ====================

    @Test
    void testIsClusterMode_Standalone() {
        assertThat(provider.isClusterMode()).isFalse();
    }

    @Test
    void testIsClusterMode_Cluster() {
        RedisClusterClient mockClusterClient = mock(RedisClusterClient.class);
        StatefulRedisClusterConnection<String, String> mockClusterConn = mock(StatefulRedisClusterConnection.class);
        RedisAdvancedClusterCommands<String, String> mockClusterSync = mock(RedisAdvancedClusterCommands.class);
        
        when(mockClusterConn.sync()).thenReturn(mockClusterSync);
        
        DefaultRedisConnectionProvider clusterProvider = new DefaultRedisConnectionProvider(
            mockClusterClient, mockClusterConn, "kc:", "redis://cluster:6379/0"
        );
        
        assertThat(clusterProvider.isClusterMode()).isTrue();
    }

    @Test
    void testGetConnectionInfo() {
        String info = provider.getConnectionInfo();
        
        assertThat(info).isEqualTo("redis://localhost:6379/0");
    }

    @Test
    void testIsHealthy_Success() {
        when(mockSync.ping()).thenReturn("PONG");
        
        boolean healthy = provider.isHealthy();
        
        assertThat(healthy).isTrue();
    }

    @Test
    void testIsHealthy_Failure() {
        when(mockSync.ping()).thenThrow(new RuntimeException("Connection lost"));
        
        boolean healthy = provider.isHealthy();
        
        assertThat(healthy).isFalse();
    }

    // ==================== putIfAbsent() Edge Cases ====================

    @Test
    void testPutIfAbsent_Success() {
        when(mockSync.set(anyString(), anyString(), any())).thenReturn("OK");
        
        String result = provider.putIfAbsent("cache", "key1", "value1", 60, TimeUnit.SECONDS);
        
        assertThat(result).isNull(); // Returns null on successful insert
    }

    @Test
    void testPutIfAbsent_KeyExists() {
        when(mockSync.set(anyString(), anyString(), any())).thenReturn(null);
        when(mockSync.get("kc:cache:key1")).thenReturn("\"existing\"");
        
        String result = provider.putIfAbsent("cache", "key1", "value1", 60, TimeUnit.SECONDS);
        
        assertThat(result).isEqualTo("existing");
    }

    @Test
    void testPutIfAbsent_ClusterMode() {
        RedisClusterClient mockClusterClient = mock(RedisClusterClient.class);
        StatefulRedisClusterConnection<String, String> mockClusterConn = mock(StatefulRedisClusterConnection.class);
        RedisAdvancedClusterCommands<String, String> mockClusterSync = mock(RedisAdvancedClusterCommands.class);
        
        when(mockClusterConn.sync()).thenReturn(mockClusterSync);
        when(mockClusterSync.set(anyString(), anyString(), any())).thenReturn("OK");
        
        DefaultRedisConnectionProvider clusterProvider = new DefaultRedisConnectionProvider(
            mockClusterClient, mockClusterConn, "kc:", "redis://localhost:6379/0"
        );
        
        String result = clusterProvider.putIfAbsent("cache", "key1", "value1", 60, TimeUnit.SECONDS);
        
        assertThat(result).isNull();
        verify(mockClusterSync).set(eq("kc:cache:{key1}"), anyString(), any());
    }

    // ==================== close() Method ====================

    @Test
    void testClose() {
        assertThatCode(() -> provider.close()).doesNotThrowAnyException();
    }
}
