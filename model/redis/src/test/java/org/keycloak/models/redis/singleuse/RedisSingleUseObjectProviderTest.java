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

package org.keycloak.models.redis.singleuse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.models.redis.RedisConnectionProvider;
import org.keycloak.models.redis.singleuse.RedisSingleUseObjectProvider;
import org.keycloak.models.redis.singleuse.RedisSingleUseObjectProvider.SingleUseObjectEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RedisSingleUseObjectProvider.
 */
class RedisSingleUseObjectProviderTest {

    private RedisConnectionProvider redis;
    private RedisSingleUseObjectProvider provider;

    @BeforeEach
    void setUp() {
        redis = mock(RedisConnectionProvider.class);
        provider = new RedisSingleUseObjectProvider(redis);
    }

    @Test
    void testConstructor_NullRedis() {
        assertThatThrownBy(() -> new RedisSingleUseObjectProvider(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void testPut() {
        Map<String, String> notes = new HashMap<>();
        notes.put("code", "abc123");
        
        provider.put("testKey", 300L, notes);
        
        verify(redis).put(
                eq(RedisConnectionProvider.SINGLE_USE_OBJECT_CACHE_NAME),
                eq("testKey"),
                any(SingleUseObjectEntity.class),
                eq(300L),
                eq(TimeUnit.SECONDS)
        );
    }

    @Test
    void testPut_RevokedKey() {
        String revokedKey = "testKey" + RedisSingleUseObjectProvider.REVOKED_KEY;
        
        provider.put(revokedKey, 300L, Map.of("note", "value"));
        
        verify(redis).put(
                eq(RedisConnectionProvider.SINGLE_USE_OBJECT_CACHE_NAME),
                eq(revokedKey),
                argThat((SingleUseObjectEntity entity) -> entity.getNotes().isEmpty()),
                eq(300L),
                eq(TimeUnit.SECONDS)
        );
    }

    @Test
    void testPut_NullKey() {
        assertThatThrownBy(() -> provider.put(null, 300L, Map.of()))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("key cannot be null");
    }

    @Test
    void testGet() {
        SingleUseObjectEntity entity = new SingleUseObjectEntity(Map.of("code", "abc123"));
        when(redis.get(
                eq(RedisConnectionProvider.SINGLE_USE_OBJECT_CACHE_NAME),
                eq("testKey"),
                eq(SingleUseObjectEntity.class)
        )).thenReturn(entity);
        
        Map<String, String> result = provider.get("testKey");
        
        assertThat(result).containsEntry("code", "abc123");
    }

    @Test
    void testGet_NotFound() {
        when(redis.get(
                eq(RedisConnectionProvider.SINGLE_USE_OBJECT_CACHE_NAME),
                eq("testKey"),
                eq(SingleUseObjectEntity.class)
        )).thenReturn(null);
        
        Map<String, String> result = provider.get("testKey");
        
        assertThat(result).isNull();
    }

    @Test
    void testGet_NullKey() {
        assertThatThrownBy(() -> provider.get(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("key cannot be null");
    }

    @Test
    void testRemove() {
        SingleUseObjectEntity entity = new SingleUseObjectEntity(Map.of("code", "abc123"));
        when(redis.remove(
                eq(RedisConnectionProvider.SINGLE_USE_OBJECT_CACHE_NAME),
                eq("testKey"),
                eq(SingleUseObjectEntity.class)
        )).thenReturn(entity);
        
        Map<String, String> result = provider.remove("testKey");
        
        assertThat(result).containsEntry("code", "abc123");
    }

    @Test
    void testRemove_NotFound() {
        when(redis.remove(
                eq(RedisConnectionProvider.SINGLE_USE_OBJECT_CACHE_NAME),
                eq("testKey"),
                eq(SingleUseObjectEntity.class)
        )).thenReturn(null);
        
        Map<String, String> result = provider.remove("testKey");
        
        assertThat(result).isNull();
    }

    @Test
    void testRemove_NullKey() {
        assertThatThrownBy(() -> provider.remove(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("key cannot be null");
    }

    @Test
    void testReplace_Success() {
        SingleUseObjectEntity existingEntity = new SingleUseObjectEntity(Map.of("old", "value"));
        RedisConnectionProvider.VersionedValue<SingleUseObjectEntity> versionedValue =
                new RedisConnectionProvider.VersionedValue<>(existingEntity, 1L);
        
        when(redis.getWithVersion(
                eq(RedisConnectionProvider.SINGLE_USE_OBJECT_CACHE_NAME),
                eq("testKey"),
                eq(SingleUseObjectEntity.class)
        )).thenReturn(versionedValue);
        
        when(redis.replaceWithVersion(
                eq(RedisConnectionProvider.SINGLE_USE_OBJECT_CACHE_NAME),
                eq("testKey"),
                any(SingleUseObjectEntity.class),
                eq(1L),
                eq(300L),
                eq(TimeUnit.SECONDS)
        )).thenReturn(true);
        
        Map<String, String> newNotes = Map.of("new", "value");
        boolean result = provider.replace("testKey", newNotes);
        
        assertThat(result).isTrue();
    }

    @Test
    void testReplace_NotFound() {
        when(redis.getWithVersion(
                eq(RedisConnectionProvider.SINGLE_USE_OBJECT_CACHE_NAME),
                eq("testKey"),
                eq(SingleUseObjectEntity.class)
        )).thenReturn(null);
        
        boolean result = provider.replace("testKey", Map.of("new", "value"));
        
        assertThat(result).isFalse();
    }

    @Test
    void testReplace_NoValue() {
        RedisConnectionProvider.VersionedValue<SingleUseObjectEntity> versionedValue =
                new RedisConnectionProvider.VersionedValue<>(null, 0L);
        
        when(redis.getWithVersion(
                eq(RedisConnectionProvider.SINGLE_USE_OBJECT_CACHE_NAME),
                eq("testKey"),
                eq(SingleUseObjectEntity.class)
        )).thenReturn(versionedValue);
        
        boolean result = provider.replace("testKey", Map.of("new", "value"));
        
        assertThat(result).isFalse();
    }

    @Test
    void testReplace_NullKey() {
        assertThatThrownBy(() -> provider.replace(null, Map.of()))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("key cannot be null");
    }

    @Test
    void testPutIfAbsent_Success() {
        when(redis.putIfAbsent(
                eq(RedisConnectionProvider.SINGLE_USE_OBJECT_CACHE_NAME),
                eq("testKey"),
                any(SingleUseObjectEntity.class),
                eq(300L),
                eq(TimeUnit.SECONDS)
        )).thenReturn(null);
        
        boolean result = provider.putIfAbsent("testKey", 300L);
        
        assertThat(result).isTrue();
    }

    @Test
    void testPutIfAbsent_AlreadyExists() {
        SingleUseObjectEntity existingEntity = new SingleUseObjectEntity(Map.of());
        when(redis.putIfAbsent(
                eq(RedisConnectionProvider.SINGLE_USE_OBJECT_CACHE_NAME),
                eq("testKey"),
                any(SingleUseObjectEntity.class),
                eq(300L),
                eq(TimeUnit.SECONDS)
        )).thenReturn(existingEntity);
        
        boolean result = provider.putIfAbsent("testKey", 300L);
        
        assertThat(result).isFalse();
    }

    @Test
    void testPutIfAbsent_NullKey() {
        assertThatThrownBy(() -> provider.putIfAbsent(null, 300L))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("key cannot be null");
    }

    @Test
    void testContains_True() {
        when(redis.containsKey(
                eq(RedisConnectionProvider.SINGLE_USE_OBJECT_CACHE_NAME),
                eq("testKey")
        )).thenReturn(true);
        
        boolean result = provider.contains("testKey");
        
        assertThat(result).isTrue();
    }

    @Test
    void testContains_False() {
        when(redis.containsKey(
                eq(RedisConnectionProvider.SINGLE_USE_OBJECT_CACHE_NAME),
                eq("testKey")
        )).thenReturn(false);
        
        boolean result = provider.contains("testKey");
        
        assertThat(result).isFalse();
    }

    @Test
    void testContains_NullKey() {
        assertThatThrownBy(() -> provider.contains(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("key cannot be null");
    }

    @Test
    void testClose() {
        provider.close(); // Should not throw
    }

    // Test SingleUseObjectEntity
    @Test
    void testSingleUseObjectEntity_DefaultConstructor() {
        SingleUseObjectEntity entity = new SingleUseObjectEntity();
        assertThat(entity).isNotNull();
    }

    @Test
    void testSingleUseObjectEntity_ParameterizedConstructor() {
        Map<String, String> notes = Map.of("key", "value");
        SingleUseObjectEntity entity = new SingleUseObjectEntity(notes);
        
        assertThat(entity.getNotes()).isEqualTo(notes);
    }

    @Test
    void testSingleUseObjectEntity_SetNotes() {
        SingleUseObjectEntity entity = new SingleUseObjectEntity();
        Map<String, String> notes = Map.of("key", "value");
        
        entity.setNotes(notes);
        
        assertThat(entity.getNotes()).isEqualTo(notes);
    }
}
