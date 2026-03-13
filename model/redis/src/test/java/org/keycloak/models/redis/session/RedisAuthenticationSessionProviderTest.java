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

package org.keycloak.models.redis.session;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.redis.RedisConnectionProvider;
import org.keycloak.models.redis.entities.RedisAuthenticationSessionEntity;
import org.keycloak.models.redis.session.RedisAuthenticationSessionProvider;
import org.keycloak.models.utils.SessionExpiration;
import org.keycloak.sessions.RootAuthenticationSessionModel;
import org.mockito.MockedStatic;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.keycloak.models.redis.TestConstants.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RedisAuthenticationSessionProvider.
 *
 * SessionExpiration.getAuthSessionLifespan() is a static method from keycloak-server-spi-private.
 * Because the majority of tests in this class exercise code paths that call it (create/get/calculate),
 * we open a class-level MockedStatic in {@link #setUp()} and close it in {@link #tearDown()}.
 * Individual tests can re-stub the return value when they need a different TTL (e.g. 0, -1, or
 * a custom realm value). Tests that don't touch auth-session creation/retrieval simply ignore the mock.
 */
class RedisAuthenticationSessionProviderTest {

    private KeycloakSession session;
    private RedisConnectionProvider redis;
    private RealmModel realm;
    private RedisAuthenticationSessionProvider provider;
    private MockedStatic<SessionExpiration> sessionExpirationMock;

    @BeforeEach
    void setUp() {
        session = mock(KeycloakSession.class);
        redis = mock(RedisConnectionProvider.class);
        realm = mock(RealmModel.class);

        when(realm.getId()).thenReturn(TEST_REALM_ID);

        provider = new RedisAuthenticationSessionProvider(session, redis);

        sessionExpirationMock = mockStatic(SessionExpiration.class);
        sessionExpirationMock.when(() -> SessionExpiration.getAuthSessionLifespan(realm))
                .thenReturn(DEFAULT_REALM_AUTH_SESSION_LIFESPAN);
    }

    @AfterEach
    void tearDown() {
        sessionExpirationMock.close();
    }

    @Test
    void testCreateRootAuthenticationSession_WithoutId() {
        RootAuthenticationSessionModel result = provider.createRootAuthenticationSession(realm);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isNotNull();

        verify(redis).put(
                eq(RedisConnectionProvider.CACHE_AUTHENTICATION_SESSIONS),
                anyString(),
                any(RedisAuthenticationSessionEntity.class),
                eq((long) DEFAULT_REALM_AUTH_SESSION_LIFESPAN),
                eq(TIME_UNIT_SECONDS)
        );
    }

    @Test
    void testCreateRootAuthenticationSession_WithId() {
        String customId = CUSTOM_SESSION_ID;

        RootAuthenticationSessionModel result = provider.createRootAuthenticationSession(realm, customId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(customId);

        verify(redis).put(
                eq(RedisConnectionProvider.CACHE_AUTHENTICATION_SESSIONS),
                eq(customId),
                any(RedisAuthenticationSessionEntity.class),
                eq((long) DEFAULT_REALM_AUTH_SESSION_LIFESPAN),
                eq(TIME_UNIT_SECONDS)
        );
    }

    @Test
    void testCreateRootAuthenticationSession_DefaultOverload() {
        RootAuthenticationSessionModel result = provider.createRootAuthenticationSession(realm);

        assertThat(result).isNotNull();
        verify(redis).put(
                anyString(),
                anyString(),
                any(RedisAuthenticationSessionEntity.class),
                anyLong(),
                any(TimeUnit.class)
        );
    }

    @Test
    void testCreateRootAuthenticationSession_UsesRealmTTL() {
        provider.createRootAuthenticationSession(realm);

        verify(redis).put(
                eq(RedisConnectionProvider.CACHE_AUTHENTICATION_SESSIONS),
                anyString(),
                any(RedisAuthenticationSessionEntity.class),
                eq((long) DEFAULT_REALM_AUTH_SESSION_LIFESPAN),
                eq(TIME_UNIT_SECONDS)
        );
    }

    @Test
    void testCreateRootAuthenticationSession_DifferentRealms() {
        RealmModel realm2 = mock(RealmModel.class);
        when(realm2.getId()).thenReturn("realm-2");

        sessionExpirationMock.when(() -> SessionExpiration.getAuthSessionLifespan(realm2))
                .thenReturn(CUSTOM_REALM_AUTH_SESSION_LIFESPAN);

        provider.createRootAuthenticationSession(realm);
        provider.createRootAuthenticationSession(realm2);

        verify(redis).put(
                eq(RedisConnectionProvider.CACHE_AUTHENTICATION_SESSIONS),
                anyString(),
                any(RedisAuthenticationSessionEntity.class),
                eq((long) DEFAULT_REALM_AUTH_SESSION_LIFESPAN),
                eq(TIME_UNIT_SECONDS)
        );
        verify(redis).put(
                eq(RedisConnectionProvider.CACHE_AUTHENTICATION_SESSIONS),
                anyString(),
                any(RedisAuthenticationSessionEntity.class),
                eq((long) CUSTOM_REALM_AUTH_SESSION_LIFESPAN),
                eq(TIME_UNIT_SECONDS)
        );
    }

    @Test
    void testCreateRootAuthenticationSession_FallbackWhenRealmReturnsZero() {
        sessionExpirationMock.when(() -> SessionExpiration.getAuthSessionLifespan(realm))
                .thenReturn(0);

        provider.createRootAuthenticationSession(realm);

        verify(redis).put(
                eq(RedisConnectionProvider.CACHE_AUTHENTICATION_SESSIONS),
                anyString(),
                any(RedisAuthenticationSessionEntity.class),
                eq((long) RedisAuthenticationSessionProvider.FALLBACK_AUTH_SESSION_LIFESPAN),
                eq(TIME_UNIT_SECONDS)
        );
    }

    @Test
    void testCreateRootAuthenticationSession_FallbackWhenRealmReturnsNegative() {
        sessionExpirationMock.when(() -> SessionExpiration.getAuthSessionLifespan(realm))
                .thenReturn(-1);

        provider.createRootAuthenticationSession(realm);

        verify(redis).put(
                eq(RedisConnectionProvider.CACHE_AUTHENTICATION_SESSIONS),
                anyString(),
                any(RedisAuthenticationSessionEntity.class),
                eq((long) RedisAuthenticationSessionProvider.FALLBACK_AUTH_SESSION_LIFESPAN),
                eq(TIME_UNIT_SECONDS)
        );
    }

    @Test
    void testGetRootAuthenticationSession_Found() {
        String sessionId = "session1";
        RedisAuthenticationSessionEntity entity = new RedisAuthenticationSessionEntity(sessionId, TEST_REALM_ID, 1000);

        when(redis.get(
                eq(RedisConnectionProvider.CACHE_AUTHENTICATION_SESSIONS),
                eq(sessionId),
                eq(RedisAuthenticationSessionEntity.class)
        )).thenReturn(entity);

        RootAuthenticationSessionModel result = provider.getRootAuthenticationSession(realm, sessionId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(sessionId);
    }

    @Test
    void testGetRootAuthenticationSession_UsesRealmTTL() {
        sessionExpirationMock.when(() -> SessionExpiration.getAuthSessionLifespan(realm))
                .thenReturn(CUSTOM_REALM_AUTH_SESSION_LIFESPAN);

        String sessionId = "session1";
        RedisAuthenticationSessionEntity entity = new RedisAuthenticationSessionEntity(sessionId, TEST_REALM_ID, 1000);

        when(redis.get(
                eq(RedisConnectionProvider.CACHE_AUTHENTICATION_SESSIONS),
                eq(sessionId),
                eq(RedisAuthenticationSessionEntity.class)
        )).thenReturn(entity);

        RootAuthenticationSessionModel result = provider.getRootAuthenticationSession(realm, sessionId);

        assertThat(result).isNotNull();
        // The adapter receives the realm-based TTL for use in persist() calls
        sessionExpirationMock.verify(() -> SessionExpiration.getAuthSessionLifespan(realm));
    }

    @Test
    void testGetRootAuthenticationSession_NotFound() {
        String sessionId = "session1";

        when(redis.get(
                eq(RedisConnectionProvider.CACHE_AUTHENTICATION_SESSIONS),
                eq(sessionId),
                eq(RedisAuthenticationSessionEntity.class)
        )).thenReturn(null);

        RootAuthenticationSessionModel result = provider.getRootAuthenticationSession(realm, sessionId);

        assertThat(result).isNull();
    }

    @Test
    void testGetRootAuthenticationSession_NullId() {
        RootAuthenticationSessionModel result = provider.getRootAuthenticationSession(realm, null);

        assertThat(result).isNull();
        verify(redis, never()).get(anyString(), anyString(), any());
    }

    @Test
    void testGetRootAuthenticationSession_WrongRealm() {
        String sessionId = "session1";
        RedisAuthenticationSessionEntity entity = new RedisAuthenticationSessionEntity(sessionId, "realm2", 1000);

        when(redis.get(
                eq(RedisConnectionProvider.CACHE_AUTHENTICATION_SESSIONS),
                eq(sessionId),
                eq(RedisAuthenticationSessionEntity.class)
        )).thenReturn(entity);

        RootAuthenticationSessionModel result = provider.getRootAuthenticationSession(realm, sessionId);

        assertThat(result).isNull();
    }

    @Test
    void testRemoveRootAuthenticationSession() {
        RootAuthenticationSessionModel authSession = mock(RootAuthenticationSessionModel.class);
        when(authSession.getId()).thenReturn("session1");

        provider.removeRootAuthenticationSession(realm, authSession);

        verify(redis).delete(RedisConnectionProvider.CACHE_AUTHENTICATION_SESSIONS, "session1");
    }

    @Test
    void testRemoveRootAuthenticationSession_Null() {
        provider.removeRootAuthenticationSession(realm, null);

        verify(redis, never()).delete(anyString(), anyString());
    }

    @Test
    void testRemoveAllExpired() {
        // Should not throw
        provider.removeAllExpired();

        // Redis handles expiration via TTL, so no Redis operations expected
        verify(redis, never()).removeByPattern(anyString(), anyString());
    }

    @Test
    void testRemoveExpired() {
        provider.removeExpired(realm);

        // Redis handles expiration via TTL, so no Redis operations expected
        verify(redis, never()).removeByPattern(anyString(), anyString());
    }

    @Test
    void testOnRealmRemoved() {
        provider.onRealmRemoved(realm);

        // Should not throw and no Redis operations in current implementation
    }

    @Test
    void testOnClientRemoved() {
        org.keycloak.models.ClientModel client = mock(org.keycloak.models.ClientModel.class);
        when(client.getClientId()).thenReturn(TEST_CLIENT_ID);

        provider.onClientRemoved(realm, client);

        // Should not throw
    }

    @Test
    void testClose() {
        provider.close(); // Should not throw
    }

    @Test
    void testCalculateAuthSessionLifespan_ReturnsRealmValue() {
        int lifespan = provider.calculateAuthSessionLifespan(realm);

        assertThat(lifespan).isEqualTo(DEFAULT_REALM_AUTH_SESSION_LIFESPAN);
    }

    @Test
    void testCalculateAuthSessionLifespan_FallbackOnZero() {
        sessionExpirationMock.when(() -> SessionExpiration.getAuthSessionLifespan(realm))
                .thenReturn(0);

        int lifespan = provider.calculateAuthSessionLifespan(realm);

        assertThat(lifespan).isEqualTo(RedisAuthenticationSessionProvider.FALLBACK_AUTH_SESSION_LIFESPAN);
    }

    @Test
    void testCalculateAuthSessionLifespan_FallbackOnNegative() {
        sessionExpirationMock.when(() -> SessionExpiration.getAuthSessionLifespan(realm))
                .thenReturn(-5);

        int lifespan = provider.calculateAuthSessionLifespan(realm);

        assertThat(lifespan).isEqualTo(RedisAuthenticationSessionProvider.FALLBACK_AUTH_SESSION_LIFESPAN);
    }
}
