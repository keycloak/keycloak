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
import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.SetArgs;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.async.RedisClusterAsyncCommands;
import io.lettuce.core.cluster.api.sync.RedisAdvancedClusterCommands;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.cluster.pubsub.StatefulRedisClusterPubSubConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.models.redis.DefaultRedisConnectionProvider;
import org.keycloak.models.redis.RedisConnectionProvider;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DefaultRedisConnectionProvider.
 */
class DefaultRedisConnectionProviderTest {

    // Test data class
    public static class TestData {
        private String name;
        private int value;

        public TestData() {}
        
        public TestData(String name, int value) {
            this.name = name;
            this.value = value;
        }
    }

    @Test
    void testStandaloneConstructor() {
        RedisClient client = mock(RedisClient.class);
        StatefulRedisConnection<String, String> conn = mock(StatefulRedisConnection.class);
        
        DefaultRedisConnectionProvider provider = new DefaultRedisConnectionProvider(
                client, conn, "test:", "redis://localhost:6379"
        );
        
        assertThat(provider).isNotNull();
        assertThat(provider.isClusterMode()).isFalse();
        assertThat(provider.getConnectionInfo()).isEqualTo("redis://localhost:6379");
    }

    @Test
    void testClusterConstructor() {
        RedisClusterClient client = mock(RedisClusterClient.class);
        StatefulRedisClusterConnection<String, String> conn = mock(StatefulRedisClusterConnection.class);
        
        DefaultRedisConnectionProvider provider = new DefaultRedisConnectionProvider(
                client, conn, "test:", "redis://cluster:6379"
        );
        
        assertThat(provider).isNotNull();
        assertThat(provider.isClusterMode()).isTrue();
        assertThat(provider.getConnectionInfo()).isEqualTo("redis://cluster:6379");
    }

    @Test
    void testGet_Standalone() {
        RedisClient client = mock(RedisClient.class);
        StatefulRedisConnection<String, String> conn = mock(StatefulRedisConnection.class);
        RedisCommands<String, String> sync = mock(RedisCommands.class);
        
        when(conn.sync()).thenReturn(sync);
        when(sync.get("kc:cache:key1")).thenReturn(null); // Return null for simplicity
        
        DefaultRedisConnectionProvider provider = new DefaultRedisConnectionProvider(
                client, conn, null, "info"
        );
        
        TestData result = provider.get("cache", "key1", TestData.class);
        
        // Verify the method was called correctly
        verify(sync).get("kc:cache:key1");
        assertThat(result).isNull(); // Will be null since we mocked null return
    }

    @Test
    void testGet_Cluster() {
        RedisClusterClient client = mock(RedisClusterClient.class);
        StatefulRedisClusterConnection<String, String> conn = mock(StatefulRedisClusterConnection.class);
        RedisAdvancedClusterCommands<String, String> sync = mock(RedisAdvancedClusterCommands.class);
        
        when(conn.sync()).thenReturn(sync);
        when(sync.get("kc:cache:{key1}")).thenReturn(null);
        
        DefaultRedisConnectionProvider provider = new DefaultRedisConnectionProvider(
                client, conn, null, "info"
        );
        
        TestData result = provider.get("cache", "key1", TestData.class);
        
        verify(sync).get("kc:cache:{key1}"); // Cluster mode uses hash tags
        assertThat(result).isNull();
    }

    @Test
    void testGet_NotFound() {
        RedisClient client = mock(RedisClient.class);
        StatefulRedisConnection<String, String> conn = mock(StatefulRedisConnection.class);
        RedisCommands<String, String> sync = mock(RedisCommands.class);
        
        when(conn.sync()).thenReturn(sync);
        when(sync.get(anyString())).thenReturn(null);
        
        DefaultRedisConnectionProvider provider = new DefaultRedisConnectionProvider(
                client, conn, null, "info"
        );
        
        TestData result = provider.get("cache", "key1", TestData.class);
        
        assertThat(result).isNull();
    }

    @Test
    void testPut_Standalone() {
        RedisClient client = mock(RedisClient.class);
        StatefulRedisConnection<String, String> conn = mock(StatefulRedisConnection.class);
        RedisCommands<String, String> sync = mock(RedisCommands.class);
        
        when(conn.sync()).thenReturn(sync);
        
        DefaultRedisConnectionProvider provider = new DefaultRedisConnectionProvider(
                client, conn, null, "info"
        );
        
        TestData data = new TestData("test", 123);
        provider.put("cache", "key1", data, 300, TimeUnit.SECONDS);
        
        verify(sync).psetex(eq("kc:cache:key1"), eq(300000L), anyString());
    }

    @Test
    void testPutIfAbsent_Success() {
        RedisClient client = mock(RedisClient.class);
        StatefulRedisConnection<String, String> conn = mock(StatefulRedisConnection.class);
        RedisCommands<String, String> sync = mock(RedisCommands.class);
        
        when(conn.sync()).thenReturn(sync);
        when(sync.set(anyString(), anyString(), any(SetArgs.class))).thenReturn("OK");
        
        DefaultRedisConnectionProvider provider = new DefaultRedisConnectionProvider(
                client, conn, null, "info"
        );
        
        TestData data = new TestData("test", 123);
        TestData result = provider.putIfAbsent("cache", "key1", data, 300, TimeUnit.SECONDS);
        
        assertThat(result).isNull(); // Success - key was absent
    }

    @Test
    void testPutIfAbsent_AlreadyExists() {
        RedisClient client = mock(RedisClient.class);
        StatefulRedisConnection<String, String> conn = mock(StatefulRedisConnection.class);
        RedisCommands<String, String> sync = mock(RedisCommands.class);
        
        when(conn.sync()).thenReturn(sync);
        when(sync.set(anyString(), anyString(), any(SetArgs.class))).thenReturn(null); // Key exists
        when(sync.get(anyString())).thenReturn(null); // Simplify
        
        DefaultRedisConnectionProvider provider = new DefaultRedisConnectionProvider(
                client, conn, null, "info"
        );
        
        TestData data = new TestData("test", 123);
        TestData result = provider.putIfAbsent("cache", "key1", data, 300, TimeUnit.SECONDS);
        
        // Returns null when key already exists (set returned null)
        verify(sync).set(anyString(), anyString(), any(SetArgs.class));
    }

    @Test
    void testRemove() {
        RedisClient client = mock(RedisClient.class);
        StatefulRedisConnection<String, String> conn = mock(StatefulRedisConnection.class);
        RedisCommands<String, String> sync = mock(RedisCommands.class);
        
        when(conn.sync()).thenReturn(sync);
        when(sync.get("kc:cache:key1")).thenReturn(null);
        when(sync.del("kc:cache:key1")).thenReturn(1L);
        
        DefaultRedisConnectionProvider provider = new DefaultRedisConnectionProvider(
                client, conn, null, "info"
        );
        
        TestData result = provider.remove("cache", "key1", TestData.class);
        
        verify(sync).get("kc:cache:key1");
        verify(sync).del("kc:cache:key1");
    }

    @Test
    void testDelete() {
        RedisClient client = mock(RedisClient.class);
        StatefulRedisConnection<String, String> conn = mock(StatefulRedisConnection.class);
        RedisCommands<String, String> sync = mock(RedisCommands.class);
        
        when(conn.sync()).thenReturn(sync);
        when(sync.del("kc:cache:key1")).thenReturn(1L);
        
        DefaultRedisConnectionProvider provider = new DefaultRedisConnectionProvider(
                client, conn, null, "info"
        );
        
        boolean result = provider.delete("cache", "key1");
        
        assertThat(result).isTrue();
    }

    @Test
    void testDelete_NotFound() {
        RedisClient client = mock(RedisClient.class);
        StatefulRedisConnection<String, String> conn = mock(StatefulRedisConnection.class);
        RedisCommands<String, String> sync = mock(RedisCommands.class);
        
        when(conn.sync()).thenReturn(sync);
        when(sync.del("kc:cache:key1")).thenReturn(0L);
        
        DefaultRedisConnectionProvider provider = new DefaultRedisConnectionProvider(
                client, conn, null, "info"
        );
        
        boolean result = provider.delete("cache", "key1");
        
        assertThat(result).isFalse();
    }

    @Test
    void testContainsKey_True() {
        RedisClient client = mock(RedisClient.class);
        StatefulRedisConnection<String, String> conn = mock(StatefulRedisConnection.class);
        RedisCommands<String, String> sync = mock(RedisCommands.class);
        
        when(conn.sync()).thenReturn(sync);
        when(sync.exists("kc:cache:key1")).thenReturn(1L);
        
        DefaultRedisConnectionProvider provider = new DefaultRedisConnectionProvider(
                client, conn, null, "info"
        );
        
        boolean result = provider.containsKey("cache", "key1");
        
        assertThat(result).isTrue();
    }

    @Test
    void testContainsKey_False() {
        RedisClient client = mock(RedisClient.class);
        StatefulRedisConnection<String, String> conn = mock(StatefulRedisConnection.class);
        RedisCommands<String, String> sync = mock(RedisCommands.class);
        
        when(conn.sync()).thenReturn(sync);
        when(sync.exists("kc:cache:key1")).thenReturn(0L);
        
        DefaultRedisConnectionProvider provider = new DefaultRedisConnectionProvider(
                client, conn, null, "info"
        );
        
        boolean result = provider.containsKey("cache", "key1");
        
        assertThat(result).isFalse();
    }

    @Test
    void testIsHealthy_True() {
        RedisClient client = mock(RedisClient.class);
        StatefulRedisConnection<String, String> conn = mock(StatefulRedisConnection.class);
        RedisCommands<String, String> sync = mock(RedisCommands.class);
        
        when(conn.sync()).thenReturn(sync);
        when(sync.ping()).thenReturn("PONG");
        
        DefaultRedisConnectionProvider provider = new DefaultRedisConnectionProvider(
                client, conn, null, "info"
        );
        
        assertThat(provider.isHealthy()).isTrue();
    }

    @Test
    void testIsHealthy_False() {
        RedisClient client = mock(RedisClient.class);
        StatefulRedisConnection<String, String> conn = mock(StatefulRedisConnection.class);
        RedisCommands<String, String> sync = mock(RedisCommands.class);
        
        when(conn.sync()).thenReturn(sync);
        when(sync.ping()).thenThrow(new RuntimeException("Connection error"));
        
        DefaultRedisConnectionProvider provider = new DefaultRedisConnectionProvider(
                client, conn, null, "info"
        );
        
        assertThat(provider.isHealthy()).isFalse();
    }

    @Test
    void testGetCacheKey_Standalone() {
        RedisClient client = mock(RedisClient.class);
        StatefulRedisConnection<String, String> conn = mock(StatefulRedisConnection.class);
        
        DefaultRedisConnectionProvider provider = new DefaultRedisConnectionProvider(
                client, conn, "prefix:", "info"
        );
        
        String key = provider.getCacheKey("cache", "key1");
        
        assertThat(key).isEqualTo("prefix:cache:key1");
    }

    @Test
    void testGetCacheKey_Cluster() {
        RedisClusterClient client = mock(RedisClusterClient.class);
        StatefulRedisClusterConnection<String, String> conn = mock(StatefulRedisClusterConnection.class);
        
        DefaultRedisConnectionProvider provider = new DefaultRedisConnectionProvider(
                client, conn, "prefix:", "info"
        );
        
        String key = provider.getCacheKey("cache", "key1");
        
        assertThat(key).isEqualTo("prefix:cache:{key1}"); // Hash tags for cluster
    }

    @Test
    void testPublish_Standalone() {
        RedisClient client = mock(RedisClient.class);
        StatefulRedisConnection<String, String> conn = mock(StatefulRedisConnection.class);
        RedisCommands<String, String> sync = mock(RedisCommands.class);
        
        when(conn.sync()).thenReturn(sync);
        
        DefaultRedisConnectionProvider provider = new DefaultRedisConnectionProvider(
                client, conn, null, "info"
        );
        
        provider.publish("channel1", "message");
        
        verify(sync).publish("channel1", "message");
    }

    @Test
    void testCreatePubSubConnection_Standalone() {
        RedisClient client = mock(RedisClient.class);
        StatefulRedisConnection<String, String> conn = mock(StatefulRedisConnection.class);
        StatefulRedisPubSubConnection<String, String> pubSubConn = mock(StatefulRedisPubSubConnection.class);
        
        when(client.connectPubSub()).thenReturn(pubSubConn);
        
        DefaultRedisConnectionProvider provider = new DefaultRedisConnectionProvider(
                client, conn, null, "info"
        );
        
        Object result = provider.createPubSubConnection();
        
        assertThat(result).isNotNull();
        verify(client).connectPubSub();
    }

    @Test
    void testCreatePubSubConnection_Cluster() {
        RedisClusterClient client = mock(RedisClusterClient.class);
        StatefulRedisClusterConnection<String, String> conn = mock(StatefulRedisClusterConnection.class);
        StatefulRedisClusterPubSubConnection<String, String> pubSubConn = mock(StatefulRedisClusterPubSubConnection.class);
        
        when(client.connectPubSub()).thenReturn(pubSubConn);
        
        DefaultRedisConnectionProvider provider = new DefaultRedisConnectionProvider(
                client, conn, null, "info"
        );
        
        Object result = provider.createPubSubConnection();
        
        assertThat(result).isNotNull();
        verify(client).connectPubSub();
    }

    @Test
    void testClose() {
        RedisClient client = mock(RedisClient.class);
        StatefulRedisConnection<String, String> conn = mock(StatefulRedisConnection.class);
        
        DefaultRedisConnectionProvider provider = new DefaultRedisConnectionProvider(
                client, conn, null, "info"
        );
        
        provider.close(); // Should not throw
    }

    @Test
    void testVersionedValue() {
        TestData data = new TestData("test", 123);
        RedisConnectionProvider.VersionedValue<TestData> vv = 
                new RedisConnectionProvider.VersionedValue<>(data, 5L);
        
        assertThat(vv.value()).isEqualTo(data);
        assertThat(vv.version()).isEqualTo(5L);
        assertThat(vv.hasValue()).isTrue();
    }

    @Test
    void testVersionedValue_NullValue() {
        RedisConnectionProvider.VersionedValue<TestData> vv = 
                new RedisConnectionProvider.VersionedValue<>(null, 0L);
        
        assertThat(vv.value()).isNull();
        assertThat(vv.hasValue()).isFalse();
    }

    @Test
    void testVersionedValue_NullValue_GetValue() {
        RedisConnectionProvider.VersionedValue<TestData> vv = 
                new RedisConnectionProvider.VersionedValue<>(null, 0L);
        
        assertThat(vv.value()).isNull();
        assertThat(vv.version()).isEqualTo(0L);
    }


    @Test
    void testGetWithVersion_NotFound() {
        RedisClient client = mock(RedisClient.class);
        StatefulRedisConnection<String, String> conn = mock(StatefulRedisConnection.class);
        RedisCommands<String, String> sync = mock(RedisCommands.class);
        
        when(conn.sync()).thenReturn(sync);
        when(sync.get(anyString())).thenReturn(null);
        
        DefaultRedisConnectionProvider provider = new DefaultRedisConnectionProvider(
                client, conn, null, "info"
        );
        
        RedisConnectionProvider.VersionedValue<TestData> result = provider.getWithVersion("cache", "key1", TestData.class);
        
        assertThat(result).isNull();
    }


    @Test
    void testPutIfAbsentAsync() throws Exception {
        RedisClient client = mock(RedisClient.class);
        StatefulRedisConnection<String, String> conn = mock(StatefulRedisConnection.class);
        RedisCommands<String, String> sync = mock(RedisCommands.class);
        
        when(conn.sync()).thenReturn(sync);
        when(sync.set(anyString(), anyString(), any(SetArgs.class))).thenReturn("OK");
        
        DefaultRedisConnectionProvider provider = new DefaultRedisConnectionProvider(
                client, conn, null, "info"
        );
        
        TestData data = new TestData("test", 42);
        TestData result = provider.putIfAbsentAsync("cache", "key1", data, 60, TimeUnit.SECONDS).toCompletableFuture().get();
        
        assertThat(result).isNull(); // Returns null when successfully set
    }

    @Test
    void testReplaceWithVersion_Success() {
        RedisClient client = mock(RedisClient.class);
        StatefulRedisConnection<String, String> conn = mock(StatefulRedisConnection.class);
        RedisCommands<String, String> sync = mock(RedisCommands.class);
        
        when(conn.sync()).thenReturn(sync);
        when(sync.eval(anyString(), any(ScriptOutputType.class), any(String[].class), any(String[].class)))
                .thenReturn(1L);
        
        DefaultRedisConnectionProvider provider = new DefaultRedisConnectionProvider(
                client, conn, null, "info"
        );
        
        TestData data = new TestData("test", 42);
        boolean result = provider.replaceWithVersion("cache", "key1", data, 5L, 60, TimeUnit.SECONDS);
        
        assertThat(result).isTrue();
        verify(sync).eval(anyString(), eq(ScriptOutputType.INTEGER), any(String[].class), any(String[].class));
    }

    @Test
    void testReplaceWithVersion_Failure() {
        RedisClient client = mock(RedisClient.class);
        StatefulRedisConnection<String, String> conn = mock(StatefulRedisConnection.class);
        RedisCommands<String, String> sync = mock(RedisCommands.class);
        
        when(conn.sync()).thenReturn(sync);
        when(sync.eval(anyString(), any(ScriptOutputType.class), any(String[].class), any(String[].class)))
                .thenReturn(0L);
        
        DefaultRedisConnectionProvider provider = new DefaultRedisConnectionProvider(
                client, conn, null, "info"
        );
        
        TestData data = new TestData("test", 42);
        boolean result = provider.replaceWithVersion("cache", "key1", data, 5L, 60, TimeUnit.SECONDS);
        
        assertThat(result).isFalse();
    }

    @Test
    void testReplaceWithVersion_Exception() {
        RedisClient client = mock(RedisClient.class);
        StatefulRedisConnection<String, String> conn = mock(StatefulRedisConnection.class);
        RedisCommands<String, String> sync = mock(RedisCommands.class);
        
        when(conn.sync()).thenReturn(sync);
        when(sync.eval(anyString(), any(ScriptOutputType.class), any(String[].class), any(String[].class)))
                .thenThrow(new RuntimeException("Redis error"));
        
        DefaultRedisConnectionProvider provider = new DefaultRedisConnectionProvider(
                client, conn, null, "info"
        );
        
        TestData data = new TestData("test", 42);
        boolean result = provider.replaceWithVersion("cache", "key1", data, 5L, 60, TimeUnit.SECONDS);
        
        assertThat(result).isFalse(); // Returns false on exception
    }

    @Test
    void testReplaceWithVersion_Cluster() {
        RedisClusterClient client = mock(RedisClusterClient.class);
        StatefulRedisClusterConnection<String, String> conn = mock(StatefulRedisClusterConnection.class);
        RedisAdvancedClusterCommands<String, String> sync = mock(RedisAdvancedClusterCommands.class);
        
        when(conn.sync()).thenReturn(sync);
        when(sync.eval(anyString(), any(ScriptOutputType.class), any(String[].class), any(String[].class)))
                .thenReturn(1L);
        
        DefaultRedisConnectionProvider provider = new DefaultRedisConnectionProvider(
                client, conn, null, "info"
        );
        
        TestData data = new TestData("test", 42);
        boolean result = provider.replaceWithVersion("cache", "key1", data, 5L, 60, TimeUnit.SECONDS);
        
        assertThat(result).isTrue();
    }

    @Test
    void testReplaceWithVersionAsync() throws Exception {
        RedisClient client = mock(RedisClient.class);
        StatefulRedisConnection<String, String> conn = mock(StatefulRedisConnection.class);
        RedisCommands<String, String> sync = mock(RedisCommands.class);
        
        when(conn.sync()).thenReturn(sync);
        when(sync.eval(anyString(), any(ScriptOutputType.class), any(String[].class), any(String[].class)))
                .thenReturn(1L);
        
        DefaultRedisConnectionProvider provider = new DefaultRedisConnectionProvider(
                client, conn, null, "info"
        );
        
        TestData data = new TestData("test", 42);
        Boolean result = provider.replaceWithVersionAsync("cache", "key1", data, 5L, 60, TimeUnit.SECONDS)
                .toCompletableFuture().get();
        
        assertThat(result).isTrue();
    }


    @Test
    void testDeleteAsync() throws Exception {
        RedisClient client = mock(RedisClient.class);
        StatefulRedisConnection<String, String> conn = mock(StatefulRedisConnection.class);
        RedisCommands<String, String> sync = mock(RedisCommands.class);
        
        when(conn.sync()).thenReturn(sync);
        when(sync.del("kc:cache:key1")).thenReturn(1L);
        
        DefaultRedisConnectionProvider provider = new DefaultRedisConnectionProvider(
                client, conn, null, "info"
        );
        
        Boolean result = provider.deleteAsync("cache", "key1").toCompletableFuture().get();
        
        assertThat(result).isTrue();
    }

    @Test
    void testScanKeys_Standalone() {
        RedisClient client = mock(RedisClient.class);
        StatefulRedisConnection<String, String> conn = mock(StatefulRedisConnection.class);
        RedisCommands<String, String> sync = mock(RedisCommands.class);
        KeyScanCursor<String> cursor = mock(KeyScanCursor.class);
        
        when(conn.sync()).thenReturn(sync);
        when(sync.scan(any(io.lettuce.core.ScanArgs.class))).thenReturn(cursor);
        when(cursor.getKeys()).thenReturn(List.of("kc:cache:key1", "kc:cache:key2"));
        when(cursor.isFinished()).thenReturn(true);
        
        DefaultRedisConnectionProvider provider = new DefaultRedisConnectionProvider(
                client, conn, null, "info"
        );
        
        List<String> keys = provider.scanKeys("cache", "*");
        
        assertThat(keys).contains("key1", "key2");
    }

    @Test
    @org.junit.jupiter.api.Disabled("Sprint 2: Cluster scan now iterates all nodes - complex mocking, tested in integration tests")
    void testScanKeys_Cluster() {
        // NOTE: Sprint 2 improvement changed cluster scan to iterate all master nodes
        // This makes unit testing complex due to Lettuce internal classes
        // The functionality is verified in integration tests with actual Redis cluster
        // See: DefaultRedisConnectionProviderFactoryIntegrationTest for cluster tests
    }

    @Test
    void testScanKeys_MultiplePages() {
        RedisClient client = mock(RedisClient.class);
        StatefulRedisConnection<String, String> conn = mock(StatefulRedisConnection.class);
        RedisCommands<String, String> sync = mock(RedisCommands.class);
        KeyScanCursor<String> cursor1 = mock(KeyScanCursor.class);
        KeyScanCursor<String> cursor2 = mock(KeyScanCursor.class);
        
        when(conn.sync()).thenReturn(sync);
        when(sync.scan(any(io.lettuce.core.ScanArgs.class))).thenReturn(cursor1);
        when(sync.scan(eq(cursor1), any(io.lettuce.core.ScanArgs.class))).thenReturn(cursor2);
        when(cursor1.getKeys()).thenReturn(List.of("kc:cache:key1"));
        when(cursor1.isFinished()).thenReturn(false);
        when(cursor2.getKeys()).thenReturn(List.of("kc:cache:key2"));
        when(cursor2.isFinished()).thenReturn(true);
        
        DefaultRedisConnectionProvider provider = new DefaultRedisConnectionProvider(
                client, conn, null, "info"
        );
        
        List<String> keys = provider.scanKeys("cache", "*");
        
        assertThat(keys).contains("key1", "key2");
    }

    @Test
    void testScanKeys_Exception() {
        RedisClient client = mock(RedisClient.class);
        StatefulRedisConnection<String, String> conn = mock(StatefulRedisConnection.class);
        RedisCommands<String, String> sync = mock(RedisCommands.class);
        
        when(conn.sync()).thenReturn(sync);
        when(sync.scan(any(io.lettuce.core.ScanArgs.class))).thenThrow(new RuntimeException("Redis error"));
        
        DefaultRedisConnectionProvider provider = new DefaultRedisConnectionProvider(
                client, conn, null, "info"
        );
        
        List<String> keys = provider.scanKeys("cache", "*");
        
        assertThat(keys).isEmpty(); // Returns empty list on exception
    }

    @Test
    void testRemoveByPattern() {
        RedisClient client = mock(RedisClient.class);
        StatefulRedisConnection<String, String> conn = mock(StatefulRedisConnection.class);
        RedisCommands<String, String> sync = mock(RedisCommands.class);
        KeyScanCursor<String> cursor = mock(KeyScanCursor.class);
        
        when(conn.sync()).thenReturn(sync);
        when(sync.scan(any(io.lettuce.core.ScanArgs.class))).thenReturn(cursor);
        when(cursor.getKeys()).thenReturn(List.of("kc:cache:key1", "kc:cache:key2"));
        when(cursor.isFinished()).thenReturn(true);
        when(sync.del(any(String[].class))).thenReturn(2L);
        
        DefaultRedisConnectionProvider provider = new DefaultRedisConnectionProvider(
                client, conn, null, "info"
        );
        
        long removed = provider.removeByPattern("cache", "key*");
        
        assertThat(removed).isEqualTo(2L);
    }

    @Test
    void testRemoveByPattern_NoMatches() {
        RedisClient client = mock(RedisClient.class);
        StatefulRedisConnection<String, String> conn = mock(StatefulRedisConnection.class);
        RedisCommands<String, String> sync = mock(RedisCommands.class);
        KeyScanCursor<String> cursor = mock(KeyScanCursor.class);
        
        when(conn.sync()).thenReturn(sync);
        when(sync.scan(any(io.lettuce.core.ScanArgs.class))).thenReturn(cursor);
        when(cursor.getKeys()).thenReturn(List.of());
        when(cursor.isFinished()).thenReturn(true);
        
        DefaultRedisConnectionProvider provider = new DefaultRedisConnectionProvider(
                client, conn, null, "info"
        );
        
        long removed = provider.removeByPattern("cache", "key*");
        
        assertThat(removed).isEqualTo(0L);
    }

    @Test
    void testRemoveByPattern_Exception() {
        RedisClient client = mock(RedisClient.class);
        StatefulRedisConnection<String, String> conn = mock(StatefulRedisConnection.class);
        RedisCommands<String, String> sync = mock(RedisCommands.class);
        
        when(conn.sync()).thenReturn(sync);
        when(sync.scan(any(io.lettuce.core.ScanArgs.class))).thenThrow(new RuntimeException("Redis error"));
        
        DefaultRedisConnectionProvider provider = new DefaultRedisConnectionProvider(
                client, conn, null, "info"
        );
        
        long removed = provider.removeByPattern("cache", "key*");
        
        assertThat(removed).isEqualTo(0L); // Returns 0 on exception
    }

    @Test
    void testRemoveByPatternAsync() throws Exception {
        RedisClient client = mock(RedisClient.class);
        StatefulRedisConnection<String, String> conn = mock(StatefulRedisConnection.class);
        RedisCommands<String, String> sync = mock(RedisCommands.class);
        KeyScanCursor<String> cursor = mock(KeyScanCursor.class);
        
        when(conn.sync()).thenReturn(sync);
        when(sync.scan(any(io.lettuce.core.ScanArgs.class))).thenReturn(cursor);
        when(cursor.getKeys()).thenReturn(List.of("kc:cache:key1"));
        when(cursor.isFinished()).thenReturn(true);
        when(sync.del(any(String[].class))).thenReturn(1L);
        
        DefaultRedisConnectionProvider provider = new DefaultRedisConnectionProvider(
                client, conn, null, "info"
        );
        
        Long removed = provider.removeByPatternAsync("cache", "key*").toCompletableFuture().get();
        
        assertThat(removed).isEqualTo(1L);
    }

    @Test
    void testPublish_Cluster() {
        RedisClusterClient client = mock(RedisClusterClient.class);
        StatefulRedisClusterConnection<String, String> conn = mock(StatefulRedisClusterConnection.class);
        RedisAdvancedClusterCommands<String, String> sync = mock(RedisAdvancedClusterCommands.class);
        
        when(conn.sync()).thenReturn(sync);
        when(sync.publish("channel1", "message")).thenReturn(1L);
        
        DefaultRedisConnectionProvider provider = new DefaultRedisConnectionProvider(
                client, conn, null, "info"
        );
        
        provider.publish("channel1", "message");
        
        verify(sync).publish("channel1", "message");
    }

    @Test
    void testPut_Cluster() {
        RedisClusterClient client = mock(RedisClusterClient.class);
        StatefulRedisClusterConnection<String, String> conn = mock(StatefulRedisClusterConnection.class);
        RedisAdvancedClusterCommands<String, String> sync = mock(RedisAdvancedClusterCommands.class);
        
        when(conn.sync()).thenReturn(sync);
        
        DefaultRedisConnectionProvider provider = new DefaultRedisConnectionProvider(
                client, conn, null, "info"
        );
        
        TestData data = new TestData("test", 42);
        provider.put("cache", "key1", data, 60, TimeUnit.SECONDS);
        
        verify(sync).psetex(eq("kc:cache:{key1}"), eq(60000L), anyString());
    }


    @Test
    void testConstructor_WithNullPrefix() {
        RedisClient client = mock(RedisClient.class);
        StatefulRedisConnection<String, String> conn = mock(StatefulRedisConnection.class);
        
        DefaultRedisConnectionProvider provider = new DefaultRedisConnectionProvider(
                client, conn, null, "redis://localhost:6379"
        );
        
        // Should use default prefix "kc:"
        String key = provider.getCacheKey("cache", "key1");
        assertThat(key).startsWith("kc:");
    }
}
