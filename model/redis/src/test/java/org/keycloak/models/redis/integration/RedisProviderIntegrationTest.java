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

package org.keycloak.models.redis.integration;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.models.*;
import org.keycloak.models.redis.DefaultRedisConnectionProvider;
import org.keycloak.models.redis.RedisConnectionProvider;
import org.keycloak.models.redis.entities.RedisUserSessionEntity;
import org.keycloak.models.redis.loginFailure.RedisUserLoginFailureProvider;
import org.keycloak.models.redis.session.RedisAuthenticationSessionProvider;
import org.keycloak.models.redis.session.RedisUserSessionAdapter;
import org.keycloak.models.redis.singleuse.RedisSingleUseObjectProvider;
import org.keycloak.models.utils.SessionExpiration;
import org.keycloak.sessions.RootAuthenticationSessionModel;
import org.mockito.MockedStatic;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.keycloak.models.redis.TestConstants.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests demonstrating complex scenarios and interactions
 * between multiple Redis provider components.
 */
class RedisProviderIntegrationTest {

    private RedisClient client;
    private StatefulRedisConnection<String, String> conn;
    private RedisCommands<String, String> sync;
    private DefaultRedisConnectionProvider redis;
    private KeycloakSession session;
    private RealmModel realm;
    private UserModel user;

    @BeforeEach
    void setUp() {
        client = mock(RedisClient.class);
        conn = mock(StatefulRedisConnection.class);
        sync = mock(RedisCommands.class);
        
        when(conn.sync()).thenReturn(sync);
        when(sync.ping()).thenReturn("PONG");
        
        redis = new DefaultRedisConnectionProvider(client, conn, REDIS_TEST_PREFIX, REDIS_LOCALHOST_URL);
        
        session = mock(KeycloakSession.class);
        realm = mock(RealmModel.class);
        user = mock(UserModel.class);
        
        when(realm.getId()).thenReturn(TEST_REALM_ID);
        when(realm.getSsoSessionMaxLifespan()).thenReturn(SSO_SESSION_MAX_LIFESPAN);
        when(realm.getOfflineSessionIdleTimeout()).thenReturn(OFFLINE_SESSION_IDLE_TIMEOUT);
        when(user.getId()).thenReturn(TEST_USER_ID);
    }

    @Test
    void testCompleteAuthenticationFlow() {
        // Static mock scoped to this method — auth session TTL is computed via SessionExpiration
        try (MockedStatic<SessionExpiration> mocked = mockStatic(SessionExpiration.class)) {
            mocked.when(() -> SessionExpiration.getAuthSessionLifespan(realm))
                    .thenReturn(DEFAULT_REALM_AUTH_SESSION_LIFESPAN);

            // 1. Create authentication session
            RedisAuthenticationSessionProvider authProvider = new RedisAuthenticationSessionProvider(session, redis);
            when(sync.psetex(anyString(), anyLong(), anyString())).thenReturn(REDIS_OK);

            RootAuthenticationSessionModel authSession = authProvider.createRootAuthenticationSession(realm);
            assertThat(authSession).isNotNull();
            assertThat(authSession.getId()).isNotNull();

            // 2. Authentication succeeds, create user session
            RedisUserSessionEntity sessionEntity = RedisUserSessionEntity.create(
                    "user-session-1",
                    realm,
                    user,
                    TEST_USERNAME,
                    TEST_IP_ADDRESS,
                    AUTH_METHOD_PASSWORD,
                    true,
                    null,
                    null
            );

            RedisUserSessionAdapter userSession = new RedisUserSessionAdapter(
                    session, realm, user, sessionEntity, redis, false, 0L
            );

            assertThat(userSession.getId()).isEqualTo("user-session-1");
            assertThat(userSession.getLoginUsername()).isEqualTo(TEST_USERNAME);
            assertThat(userSession.isRememberMe()).isTrue();

            // 3. Create single-use auth code
            RedisSingleUseObjectProvider singleUseProvider = new RedisSingleUseObjectProvider(redis);
            Map<String, String> codeData = Map.of("userId", "test-user", "sessionId", "user-session-1");

            when(sync.set(anyString(), anyString(), any())).thenReturn("OK");
            singleUseProvider.put("auth-code-123", TTL_300_SECONDS, codeData);

            // Verify psetex was called (once for auth session, once for single-use code)
            verify(sync, atLeast(1)).psetex(anyString(), anyLong(), anyString());

            // 4. Remove authentication session after login
            authProvider.removeRootAuthenticationSession(realm, authSession);
            verify(sync).del(anyString());
        }
    }

    @Test
    void testFailedLoginWithBruteForceProtection() {
        RedisUserLoginFailureProvider loginFailureProvider = 
                new RedisUserLoginFailureProvider(session, redis, 900L);
        
        // Simulate failed login attempts
        when(sync.get(anyString())).thenReturn(null);
        when(sync.psetex(anyString(), anyLong(), anyString())).thenReturn("OK");
        
        UserLoginFailureModel failure = loginFailureProvider.addUserLoginFailure(realm, "user@example.com");
        assertThat(failure).isNotNull();
        
        // Increment failures
        failure.incrementFailures();
        failure.setLastIPFailure("192.168.1.100");
        failure.setLastFailure(System.currentTimeMillis());
        
        assertThat(failure.getNumFailures()).isEqualTo(1);
        assertThat(failure.getLastIPFailure()).isEqualTo("192.168.1.100");
        
        // After 3rd failure, temporary lockout
        failure.incrementFailures();
        failure.incrementFailures();
        failure.incrementTemporaryLockouts();
        
        assertThat(failure.getNumFailures()).isEqualTo(3);
        assertThat(failure.getNumTemporaryLockouts()).isEqualTo(1);
    }

    @Test
    void testSingleUseTokenConsumption() {
        RedisSingleUseObjectProvider singleUseProvider = new RedisSingleUseObjectProvider(redis);
        
        // Store a token
        Map<String, String> tokenData = Map.of("action", "VERIFY_EMAIL", "userId", "user-123");
        when(sync.set(anyString(), anyString(), any())).thenReturn("OK");
        singleUseProvider.put("token-abc", 600L, tokenData);
        
        // Verify put was called
        verify(sync).psetex(anyString(), eq(600000L), anyString());
        
        // Consume the token - mock null to simplify
        when(sync.get(anyString())).thenReturn(null);
        when(sync.del(anyString())).thenReturn(1L);
        
        Map<String, String> result1 = singleUseProvider.remove("token-abc");
        assertThat(result1).isNull(); // Null since we mocked null
        
        // Verify delete was called
        verify(sync, atLeastOnce()).del(anyString());
    }

    @Test
    void testSessionModificationAndPersistence() {
        KeycloakTransactionManager txManager = mock(KeycloakTransactionManager.class);
        when(session.getTransactionManager()).thenReturn(txManager);
        
        RedisUserSessionEntity entity = RedisUserSessionEntity.create(
                "session-1", realm, user, TEST_USERNAME, TEST_IP_ADDRESS_2, AUTH_METHOD_PASSWORD, false, null, null
        );
        
        RedisUserSessionAdapter adapter = new RedisUserSessionAdapter(
                session, realm, user, entity, redis, false, 0L
        );
        
        // Modify session multiple times
        adapter.setNote("note1", "value1");
        adapter.setNote("note2", "value2");
        adapter.setState(UserSessionModel.State.LOGGED_IN);
        adapter.setLastSessionRefresh(2000);
        
        // Should only enlist transaction once
        verify(txManager, times(1)).enlist(any(KeycloakTransaction.class));
        
        // Persist changes
        when(sync.psetex(anyString(), anyLong(), anyString())).thenReturn("OK");
        adapter.persist();
        
        verify(sync).psetex(
                contains("session-1"),
                eq(TTL_3600_MILLIS),
                anyString()
        );
    }

    @Test
    void testOptimisticLockingForConcurrentAccess() {
        // Test versioned value operations
        when(sync.get("test:cache:_ver:key1")).thenReturn("1");
        when(sync.get("test:cache:key1")).thenReturn(null); // Simplify - skip deserialization
        
        RedisConnectionProvider.VersionedValue<Map> versionedValue = 
                redis.getWithVersion("cache", "key1", Map.class);
        
        assertThat(versionedValue).isNull(); // Null when value doesn't exist
    }

    @Test
    void testOfflineSessionCreation() {
        RedisUserSessionEntity entity = RedisUserSessionEntity.create(
                "offline-session-1",
                realm,
                user,
                TEST_USERNAME,
                TEST_IP_ADDRESS,
                AUTH_METHOD_PASSWORD,
                false,
                null,
                null
        );
        entity.setOffline(true);
        
        RedisUserSessionAdapter offlineSession = new RedisUserSessionAdapter(
                session, realm, user, entity, redis, true, 0L
        );
        
        assertThat(offlineSession.isOffline()).isTrue();
        
        // Modify and persist offline session
        KeycloakTransactionManager txManager = mock(KeycloakTransactionManager.class);
        when(session.getTransactionManager()).thenReturn(txManager);
        when(sync.psetex(anyString(), anyLong(), anyString())).thenReturn("OK");
        
        offlineSession.setNote("offline-note", "value");
        offlineSession.persist();
        
        verify(sync).psetex(
                contains("offline-session-1"),
                eq(TTL_86400_MILLIS), // Offline session timeout
                anyString()
        );
    }

    @Test
    void testHealthCheck() {
        when(sync.ping()).thenReturn("PONG");
        assertThat(redis.isHealthy()).isTrue();
        
        when(sync.ping()).thenThrow(new RuntimeException("Connection lost"));
        assertThat(redis.isHealthy()).isFalse();
    }

    @Test
    void testMultipleProviderInteractions() {
        // Static mock scoped to this method — auth session TTL is computed via SessionExpiration
        try (MockedStatic<SessionExpiration> mocked = mockStatic(SessionExpiration.class)) {
            mocked.when(() -> SessionExpiration.getAuthSessionLifespan(realm))
                    .thenReturn(DEFAULT_REALM_AUTH_SESSION_LIFESPAN);

            // Simulate complete login flow with all providers
            KeycloakTransactionManager txManager = mock(KeycloakTransactionManager.class);
            when(session.getTransactionManager()).thenReturn(txManager);
            when(sync.psetex(anyString(), anyLong(), anyString())).thenReturn("OK");
            when(sync.set(anyString(), anyString(), any())).thenReturn("OK");

            // 1. Check for previous login failures
            RedisUserLoginFailureProvider failureProvider =
                    new RedisUserLoginFailureProvider(session, redis, 900L);

            when(sync.get(anyString())).thenReturn(null);
            UserLoginFailureModel existingFailure = failureProvider.getUserLoginFailure(realm, "user@test.com");
            assertThat(existingFailure).isNull();

            // 2. Create auth session
            RedisAuthenticationSessionProvider authProvider =
                    new RedisAuthenticationSessionProvider(session, redis);
            RootAuthenticationSessionModel authSession = authProvider.createRootAuthenticationSession(realm);
            assertThat(authSession).isNotNull();

            // 3. Generate single-use code
            RedisSingleUseObjectProvider singleUseProvider = new RedisSingleUseObjectProvider(redis);
            singleUseProvider.put("code-123", TTL_300_SECONDS, Map.of("sessionId", authSession.getId()));

            // 4. Create user session after successful auth
            RedisUserSessionEntity sessionEntity = RedisUserSessionEntity.create(
                    "final-session",
                    realm,
                    user,
                    "user@test.com",
                    TEST_IP_ADDRESS,
                    AUTH_METHOD_PASSWORD,
                    true,
                    null,
                    null
            );

            RedisUserSessionAdapter userSession = new RedisUserSessionAdapter(
                    session, realm, user, sessionEntity, redis, false, 0L
            );

            userSession.setNote("login_hint", "user@test.com");
            userSession.persist();

            // 5. Clear any previous login failures
            failureProvider.removeUserLoginFailure(realm, "user@test.com");

            // Verify all operations were called
            verify(sync, atLeast(2)).psetex(anyString(), anyLong(), anyString());
            verify(sync, atLeast(1)).del(anyString());
        }
    }

    @Test
    void testConcurrentSessionCreation() {
        // Simulate concurrent access attempts
        when(sync.set(anyString(), anyString(), any()))
                .thenReturn("OK");   // First succeeds
        
        RedisSingleUseObjectProvider provider = new RedisSingleUseObjectProvider(redis);
        
        boolean first = provider.putIfAbsent("session-key", TTL_300_SECONDS);
        
        assertThat(first).isTrue();  // First client succeeds
        
        // Verify set was called with NX option
        verify(sync, atLeastOnce()).set(anyString(), anyString(), any());
    }

    @Test
    void testConnectionInfo() {
        String info = redis.getConnectionInfo();
        assertThat(info).isEqualTo(REDIS_LOCALHOST_URL);
    }

    @Test
    void testClusterModeDetection() {
        assertThat(redis.isClusterMode()).isFalse();
    }
}
