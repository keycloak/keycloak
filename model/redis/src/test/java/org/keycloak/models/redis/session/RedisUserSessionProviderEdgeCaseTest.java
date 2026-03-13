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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.models.*;
import org.keycloak.models.redis.RedisConnectionProvider;
import org.keycloak.models.redis.entities.RedisUserSessionEntity;
import org.keycloak.models.redis.session.RedisUserSessionProvider;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.keycloak.models.redis.TestConstants.*;
import static org.mockito.Mockito.*;

/**
 * Edge case and error condition tests for RedisUserSessionProvider.
 */
class RedisUserSessionProviderEdgeCaseTest {

    private KeycloakSession session;
    private RedisConnectionProvider redis;
    private RedisUserSessionProvider provider;
    private RealmModel realm;
    private UserModel user;
    private ClientModel client;
    private UserProvider userProvider;
    private RealmProvider realmProvider;

    @BeforeEach
    void setUp() {
        session = mock(KeycloakSession.class);
        redis = mock(RedisConnectionProvider.class);
        realm = mock(RealmModel.class);
        user = mock(UserModel.class);
        client = mock(ClientModel.class);
        userProvider = mock(UserProvider.class);
        realmProvider = mock(RealmProvider.class);

        when(session.users()).thenReturn(userProvider);
        when(session.realms()).thenReturn(realmProvider);
        when(realm.getId()).thenReturn(TEST_REALM_ID);
        when(realm.getSsoSessionMaxLifespan()).thenReturn(SSO_SESSION_MAX_LIFESPAN);
        when(realm.getOfflineSessionIdleTimeout()).thenReturn(OFFLINE_SESSION_IDLE_TIMEOUT);
        when(user.getId()).thenReturn(TEST_USER_ID);
        when(client.getId()).thenReturn(TEST_CLIENT_ID);

        provider = new RedisUserSessionProvider(session, redis, SSO_SESSION_MAX_LIFESPAN, OFFLINE_SESSION_IDLE_TIMEOUT);
    }

    @Test
    void testCreateUserSession_WithNullId_GeneratesId() {
        when(redis.replaceWithVersion(anyString(), anyString(), any(), eq(0L), anyLong(), any()))
                .thenReturn(true);

        UserSessionModel userSession = provider.createUserSession(
                null,  // null ID
                realm,
                user,
                TEST_USERNAME,
                TEST_IP_ADDRESS,
                AUTH_METHOD_PASSWORD,
                false,
                null,
                null,
                UserSessionModel.SessionPersistenceState.PERSISTENT
        );

        assertThat(userSession).isNotNull();
        assertThat(userSession.getId()).isNotNull();
        assertThat(userSession.getId()).isNotEmpty();
    }

    @Test
    void testCreateUserSession_WithBrokerInfo() {
        when(redis.replaceWithVersion(anyString(), anyString(), any(), eq(0L), anyLong(), any()))
                .thenReturn(true);

        String brokerSessionId = "broker-session-123";
        String brokerUserId = "broker-user-456";

        UserSessionModel userSession = provider.createUserSession(
                TEST_SESSION_ID,
                realm,
                user,
                TEST_USERNAME,
                TEST_IP_ADDRESS,
                AUTH_METHOD_PASSWORD,
                true,
                brokerSessionId,
                brokerUserId,
                UserSessionModel.SessionPersistenceState.PERSISTENT
        );

        assertThat(userSession).isNotNull();
        assertThat(userSession.isRememberMe()).isTrue();
    }

    @Test
    void testGetUserSession_EntityExistsButUserDeleted() {
        RedisUserSessionEntity entity = RedisUserSessionEntity.create(
                TEST_SESSION_ID,
                realm,
                user,
                TEST_USERNAME,
                TEST_IP_ADDRESS,
                AUTH_METHOD_PASSWORD,
                false,
                null,
                null
        );

        when(redis.get(
                eq(RedisConnectionProvider.USER_SESSION_CACHE_NAME),
                eq(TEST_SESSION_ID),
                eq(RedisUserSessionEntity.class)
        )).thenReturn(entity);
        when(userProvider.getUserById(realm, TEST_USER_ID)).thenReturn(null);

        UserSessionModel session = provider.getUserSession(realm, TEST_SESSION_ID);

        assertThat(session).isNull();
        // Verify orphaned session was deleted
        verify(redis).delete(RedisConnectionProvider.USER_SESSION_CACHE_NAME, TEST_SESSION_ID);
    }

    @Test
    void testGetOfflineUserSession_NotFound() {
        when(redis.get(anyString(), eq(TEST_SESSION_ID), eq(RedisUserSessionEntity.class)))
                .thenReturn(null);

        UserSessionModel session = provider.getOfflineUserSession(realm, TEST_SESSION_ID);

        assertThat(session).isNull();
    }

    @Test
    void testGetOfflineUserSession_Found() {
        RedisUserSessionEntity entity = RedisUserSessionEntity.create(
                TEST_SESSION_ID,
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

        when(redis.get(
                eq(RedisConnectionProvider.OFFLINE_USER_SESSION_CACHE_NAME),
                eq(TEST_SESSION_ID),
                eq(RedisUserSessionEntity.class)
        )).thenReturn(entity);
        when(userProvider.getUserById(realm, TEST_USER_ID)).thenReturn(user);

        UserSessionModel session = provider.getOfflineUserSession(realm, TEST_SESSION_ID);

        assertThat(session).isNotNull();
        assertThat(session.isOffline()).isTrue();
    }

    @Test
    void testRemoveUserSessions_ForUser() {
        provider.removeUserSessions(realm, user);
        
        // Method should complete without errors
        // In a real implementation, this would iterate and remove sessions
    }

    @Test
    void testRemoveAllExpired() {
        provider.removeAllExpired();
        
        // Should not throw - Redis handles expiration automatically
    }

    @Test
    void testRemoveExpired() {
        provider.removeExpired(realm);
        
        // Should not throw - Redis handles expiration automatically
    }

    @Test
    void testOnRealmRemoved() {
        provider.onRealmRemoved(realm);
        
        // Should trigger removal of all sessions for realm
        // Actual removal happens via removeUserSessions
    }

    @Test
    void testOnClientRemoved() {
        provider.onClientRemoved(realm, client);
        
        // Should not throw
    }


    @Test
    void testGetUserSessionsStream_EmptyResult() {
        Stream<UserSessionModel> sessions = provider.getUserSessionsStream(realm, user);

        assertThat(sessions).isNotNull();
        // Stream should be empty when no sessions exist
        assertThat(sessions.count()).isEqualTo(0);
    }

    @Test
    void testGetUserSessionsStream_ForClient() {
        Stream<UserSessionModel> sessions = provider.getUserSessionsStream(realm, client);

        assertThat(sessions).isNotNull();
        assertThat(sessions.count()).isEqualTo(0);
    }

    @Test
    void testLoadOfflineUserSessions() {
        Stream<UserSessionModel> sessions = provider.getOfflineUserSessionsStream(realm, user);

        assertThat(sessions).isNotNull();
        assertThat(sessions.count()).isEqualTo(0);
    }

    @Test
    void testImportUserSessions() {
        // Test that provider handles import operations
        provider.importUserSessions(null, true);
        
        // Should not throw
    }



}
