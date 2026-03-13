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
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.models.redis.DefaultRedisConnectionProvider;
import org.keycloak.models.redis.RedisSerializer;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.keycloak.models.redis.TestConstants.*;
import static org.mockito.Mockito.*;

/**
 * Edge case tests for DefaultRedisConnectionProvider.
 */
class DefaultRedisConnectionProviderEdgeCaseTest {

    private RedisClient client;
    private StatefulRedisConnection<String, String> connection;
    private RedisCommands<String, String> sync;
    private DefaultRedisConnectionProvider provider;

    @BeforeEach
    void setUp() {
        client = mock(RedisClient.class);
        connection = mock(StatefulRedisConnection.class);
        sync = mock(RedisCommands.class);

        when(connection.sync()).thenReturn(sync);
        
        provider = new DefaultRedisConnectionProvider(client, connection, REDIS_TEST_PREFIX, REDIS_LOCALHOST_URL);
    }

    @Test
    void testPut_WithTTL() {
        String value = "test-value";
        when(sync.psetex(anyString(), anyLong(), anyString())).thenReturn(REDIS_OK);

        provider.put("cache", "key", value, 60L, TimeUnit.SECONDS);

        verify(sync).psetex(eq(REDIS_TEST_PREFIX + "cache:key"), eq(60000L), anyString());
    }

    @Test
    void testPut_WithMilliseconds() {
        String value = "test-value";
        when(sync.psetex(anyString(), anyLong(), anyString())).thenReturn(REDIS_OK);

        provider.put("cache", "key", value, 5000L, TimeUnit.MILLISECONDS);

        verify(sync).psetex(eq(REDIS_TEST_PREFIX + "cache:key"), eq(5000L), anyString());
    }

    @Test
    void testGet_WithNullKey() {
        Object result = provider.get("cache", null, String.class);
        
        assertThat(result).isNull();
    }

    @Test
    void testGet_WithEmptyKey() {
        when(sync.get(anyString())).thenReturn(null);
        
        Object result = provider.get("cache", "", String.class);
        
        assertThat(result).isNull();
    }


    @Test
    void testDelete_Success() {
        when(sync.del(anyString())).thenReturn(1L);

        provider.delete("cache", "key");

        verify(sync).del(REDIS_TEST_PREFIX + "cache:key");
    }

    @Test
    void testDelete_KeyDoesNotExist() {
        when(sync.del(anyString())).thenReturn(0L);

        provider.delete("cache", "key");

        verify(sync).del(REDIS_TEST_PREFIX + "cache:key");
    }


    @Test
    void testPutIfAbsent_KeyDoesNotExist() {
        when(sync.set(anyString(), anyString(), any())).thenReturn(REDIS_OK);

        String result = provider.putIfAbsent("work", "lock:key", "value", 60L, TimeUnit.SECONDS);

        assertThat(result).isNull();
        verify(sync).set(anyString(), anyString(), any());
    }

    @Test
    void testPutIfAbsent_KeyExists() {
        when(sync.set(anyString(), anyString(), any())).thenReturn(null);
        when(sync.get(anyString())).thenReturn(new String(RedisSerializer.getInstance().serialize("existing-value")));

        String result = provider.putIfAbsent("work", "lock:key", "value", 60L, TimeUnit.SECONDS);

        assertThat(result).isEqualTo("existing-value");
    }







    @Test
    void testPutIfAbsent_WithMilliseconds() {
        when(sync.set(anyString(), anyString(), any())).thenReturn(REDIS_OK);

        String result = provider.putIfAbsent("work", "lock", "value", 5000L, TimeUnit.MILLISECONDS);

        assertThat(result).isNull();
        verify(sync).set(anyString(), anyString(), any());
    }

    @Test
    void testDelete_MultipleKeys() {
        provider.delete("cache", "key1");
        provider.delete("cache", "key2");
        provider.delete("cache", "key3");

        verify(sync, times(3)).del(anyString());
    }

    @Test
    void testPut_ZeroTTL() {
        when(sync.psetex(anyString(), anyLong(), anyString())).thenReturn(REDIS_OK);

        provider.put("cache", "key", "value", 0L, TimeUnit.SECONDS);

        // Should still work but with 0 TTL
        verify(sync).psetex(anyString(), eq(0L), anyString());
    }

    @Test
    void testPut_NegativeTTL() {
        when(sync.set(anyString(), anyString())).thenReturn(REDIS_OK);

        provider.put("cache", "key", "value", -1L, TimeUnit.SECONDS);

        // -1 should trigger set without expiration (not psetex)
        verify(sync).set(anyString(), anyString());
        verify(sync, never()).psetex(anyString(), anyLong(), anyString());
    }

    @Test
    void testPutIfAbsent_NegativeTTL() {
        when(sync.set(anyString(), anyString(), any())).thenReturn(REDIS_OK);

        String result = provider.putIfAbsent("cache", "key", "value", -1L, TimeUnit.SECONDS);

        // -1 should trigger set with NX but no expiration
        assertThat(result).isNull();
        verify(sync).set(anyString(), anyString(), any());
    }

}
