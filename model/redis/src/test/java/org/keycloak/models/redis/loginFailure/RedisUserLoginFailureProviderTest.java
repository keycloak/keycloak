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

package org.keycloak.models.redis.loginFailure;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserLoginFailureModel;
import org.keycloak.models.redis.RedisConnectionProvider;
import org.keycloak.models.redis.entities.RedisLoginFailureEntity;
import org.keycloak.models.redis.loginFailure.RedisUserLoginFailureProvider;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RedisUserLoginFailureProvider.
 */
class RedisUserLoginFailureProviderTest {

    private KeycloakSession session;
    private RedisConnectionProvider redis;
    private RealmModel realm;
    private RedisUserLoginFailureProvider provider;
    
    private static final long FAILURE_LIFESPAN = 3600L;

    @BeforeEach
    void setUp() {
        session = mock(KeycloakSession.class);
        redis = mock(RedisConnectionProvider.class);
        realm = mock(RealmModel.class);
        when(realm.getId()).thenReturn("realm1");
        
        provider = new RedisUserLoginFailureProvider(session, redis, FAILURE_LIFESPAN);
    }

    @Test
    void testGetUserLoginFailure_Found() {
        String userId = "user1";
        String key = "realm1:user1";
        RedisLoginFailureEntity entity = new RedisLoginFailureEntity("realm1", userId);
        entity.setNumFailures(3);
        
        when(redis.get(
                eq(RedisConnectionProvider.CACHE_LOGIN_FAILURES),
                eq(key),
                eq(RedisLoginFailureEntity.class)
        )).thenReturn(entity);
        
        UserLoginFailureModel result = provider.getUserLoginFailure(realm, userId);
        
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getNumFailures()).isEqualTo(3);
    }

    @Test
    void testGetUserLoginFailure_NotFound() {
        String userId = "user1";
        String key = "realm1:user1";
        
        when(redis.get(
                eq(RedisConnectionProvider.CACHE_LOGIN_FAILURES),
                eq(key),
                eq(RedisLoginFailureEntity.class)
        )).thenReturn(null);
        
        UserLoginFailureModel result = provider.getUserLoginFailure(realm, userId);
        
        assertThat(result).isNull();
    }

    @Test
    void testAddUserLoginFailure_New() {
        String userId = "user1";
        String key = "realm1:user1";
        
        when(redis.get(
                eq(RedisConnectionProvider.CACHE_LOGIN_FAILURES),
                eq(key),
                eq(RedisLoginFailureEntity.class)
        )).thenReturn(null);
        
        UserLoginFailureModel result = provider.addUserLoginFailure(realm, userId);
        
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);
        
        verify(redis).put(
                eq(RedisConnectionProvider.CACHE_LOGIN_FAILURES),
                eq(key),
                any(RedisLoginFailureEntity.class),
                eq(FAILURE_LIFESPAN),
                eq(TimeUnit.SECONDS)
        );
    }

    @Test
    void testAddUserLoginFailure_Existing() {
        String userId = "user1";
        String key = "realm1:user1";
        RedisLoginFailureEntity existingEntity = new RedisLoginFailureEntity("realm1", userId);
        existingEntity.setNumFailures(2);
        
        when(redis.get(
                eq(RedisConnectionProvider.CACHE_LOGIN_FAILURES),
                eq(key),
                eq(RedisLoginFailureEntity.class)
        )).thenReturn(existingEntity);
        
        UserLoginFailureModel result = provider.addUserLoginFailure(realm, userId);
        
        assertThat(result).isNotNull();
        assertThat(result.getNumFailures()).isEqualTo(2);
        
        // Should not call put again since entity already exists
        verify(redis, never()).put(
                eq(RedisConnectionProvider.CACHE_LOGIN_FAILURES),
                eq(key),
                any(RedisLoginFailureEntity.class),
                anyLong(),
                any(TimeUnit.class)
        );
    }

    @Test
    void testRemoveUserLoginFailure() {
        String userId = "user1";
        String key = "realm1:user1";
        
        provider.removeUserLoginFailure(realm, userId);
        
        verify(redis).delete(
                eq(RedisConnectionProvider.CACHE_LOGIN_FAILURES),
                eq(key)
        );
    }

    @Test
    void testRemoveAllUserLoginFailures() {
        // Should not throw and log only
        provider.removeAllUserLoginFailures(realm);
        
        // Verify no Redis operations called (entries expire via TTL)
        verify(redis, never()).delete(anyString(), anyString());
        verify(redis, never()).removeByPattern(anyString(), anyString());
    }

    @Test
    void testClose() {
        provider.close(); // Should not throw
    }

    // Test the inner adapter class
    @Test
    void testAdapter_GetId() {
        String userId = "user1";
        RedisLoginFailureEntity entity = new RedisLoginFailureEntity("realm1", userId);
        
        when(redis.get(
                anyString(),
                anyString(),
                eq(RedisLoginFailureEntity.class)
        )).thenReturn(entity);
        
        UserLoginFailureModel model = provider.addUserLoginFailure(realm, userId);
        
        assertThat(model.getId()).isEqualTo("realm1:user1");
    }

    @Test
    void testAdapter_GetUserId() {
        String userId = "user1";
        RedisLoginFailureEntity entity = new RedisLoginFailureEntity("realm1", userId);
        
        when(redis.get(
                anyString(),
                anyString(),
                eq(RedisLoginFailureEntity.class)
        )).thenReturn(entity);
        
        UserLoginFailureModel model = provider.addUserLoginFailure(realm, userId);
        
        assertThat(model.getUserId()).isEqualTo(userId);
    }

    @Test
    void testAdapter_FailedLoginNotBefore() {
        String userId = "user1";
        RedisLoginFailureEntity entity = new RedisLoginFailureEntity("realm1", userId);
        
        when(redis.get(
                anyString(),
                anyString(),
                eq(RedisLoginFailureEntity.class)
        )).thenReturn(entity);
        
        UserLoginFailureModel model = provider.addUserLoginFailure(realm, userId);
        
        model.setFailedLoginNotBefore(100);
        assertThat(model.getFailedLoginNotBefore()).isEqualTo(100);
        
        verify(redis, atLeastOnce()).put(
                eq(RedisConnectionProvider.CACHE_LOGIN_FAILURES),
                anyString(),
                any(RedisLoginFailureEntity.class),
                anyLong(),
                any(TimeUnit.class)
        );
    }

    @Test
    void testAdapter_IncrementFailures() {
        String userId = "user1";
        RedisLoginFailureEntity entity = new RedisLoginFailureEntity("realm1", userId);
        entity.setNumFailures(5);
        
        when(redis.get(
                anyString(),
                anyString(),
                eq(RedisLoginFailureEntity.class)
        )).thenReturn(entity);
        
        UserLoginFailureModel model = provider.addUserLoginFailure(realm, userId);
        
        model.incrementFailures();
        assertThat(model.getNumFailures()).isEqualTo(6);
    }

    @Test
    void testAdapter_ClearFailures() {
        String userId = "user1";
        RedisLoginFailureEntity entity = new RedisLoginFailureEntity("realm1", userId);
        entity.setNumFailures(5);
        entity.setLastFailure(12345L);
        entity.setLastIPFailure("192.168.1.1");
        
        when(redis.get(
                anyString(),
                anyString(),
                eq(RedisLoginFailureEntity.class)
        )).thenReturn(entity);
        
        UserLoginFailureModel model = provider.addUserLoginFailure(realm, userId);
        
        model.clearFailures();
        
        assertThat(model.getNumFailures()).isZero();
        assertThat(model.getLastFailure()).isZero();
        assertThat(model.getLastIPFailure()).isNull();
    }

    @Test
    void testAdapter_LastFailure() {
        String userId = "user1";
        RedisLoginFailureEntity entity = new RedisLoginFailureEntity("realm1", userId);
        
        when(redis.get(
                anyString(),
                anyString(),
                eq(RedisLoginFailureEntity.class)
        )).thenReturn(entity);
        
        UserLoginFailureModel model = provider.addUserLoginFailure(realm, userId);
        
        model.setLastFailure(1234567890L);
        assertThat(model.getLastFailure()).isEqualTo(1234567890L);
    }

    @Test
    void testAdapter_LastIPFailure() {
        String userId = "user1";
        RedisLoginFailureEntity entity = new RedisLoginFailureEntity("realm1", userId);
        
        when(redis.get(
                anyString(),
                anyString(),
                eq(RedisLoginFailureEntity.class)
        )).thenReturn(entity);
        
        UserLoginFailureModel model = provider.addUserLoginFailure(realm, userId);
        
        model.setLastIPFailure("10.0.0.1");
        assertThat(model.getLastIPFailure()).isEqualTo("10.0.0.1");
    }

    @Test
    void testAdapter_IncrementTemporaryLockouts() {
        String userId = "user1";
        RedisLoginFailureEntity entity = new RedisLoginFailureEntity("realm1", userId);
        entity.setNumTemporaryLockouts(2);
        
        when(redis.get(
                anyString(),
                anyString(),
                eq(RedisLoginFailureEntity.class)
        )).thenReturn(entity);
        
        UserLoginFailureModel model = provider.addUserLoginFailure(realm, userId);
        
        model.incrementTemporaryLockouts();
        assertThat(model.getNumTemporaryLockouts()).isEqualTo(3);
    }
}
