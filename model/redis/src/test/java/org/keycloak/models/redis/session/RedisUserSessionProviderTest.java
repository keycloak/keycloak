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
import org.keycloak.models.redis.entities.RedisClientSessionEntity;
import org.keycloak.models.redis.entities.RedisUserSessionEntity;
import org.keycloak.models.redis.session.RedisUserSessionProvider;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.keycloak.models.redis.TestConstants.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RedisUserSessionProvider covering all major methods.
 */
class RedisUserSessionProviderTest {

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
    void testCreateUserSession() {
        when(redis.replaceWithVersion(
                eq(RedisConnectionProvider.USER_SESSION_CACHE_NAME),
                anyString(),
                any(RedisUserSessionEntity.class),
                eq(0L),
                anyLong(),
                any()
        )).thenReturn(true);

        UserSessionModel session = provider.createUserSession(
                TEST_SESSION_ID,
                realm,
                user,
                TEST_USERNAME,
                TEST_IP_ADDRESS,
                AUTH_METHOD_PASSWORD,
                true,
                null,
                null,
                UserSessionModel.SessionPersistenceState.PERSISTENT
        );

        assertThat(session).isNotNull();
        assertThat(session.getId()).isEqualTo(TEST_SESSION_ID);
        assertThat(session.getLoginUsername()).isEqualTo(TEST_USERNAME);
        assertThat(session.isRememberMe()).isTrue();

        verify(redis).replaceWithVersion(
                eq(RedisConnectionProvider.USER_SESSION_CACHE_NAME),
                eq(TEST_SESSION_ID),
                any(RedisUserSessionEntity.class),
                eq(0L),
                anyLong(),
                any()
        );
    }

    @Test
    void testCreateUserSession_WithNullId() {
        when(redis.replaceWithVersion(
                anyString(),
                anyString(),
                any(RedisUserSessionEntity.class),
                eq(0L),
                anyLong(),
                any()
        )).thenReturn(true);

        UserSessionModel session = provider.createUserSession(
                null,  // null ID should generate one
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

        assertThat(session).isNotNull();
        assertThat(session.getId()).isNotNull();
    }

    @Test
    void testCreateUserSession_FailureWhenAlreadyExists() {
        when(redis.replaceWithVersion(anyString(), anyString(), any(), eq(0L), anyLong(), any()))
                .thenReturn(false);

        assertThatThrownBy(() -> provider.createUserSession(
                TEST_SESSION_ID,
                realm,
                user,
                TEST_USERNAME,
                TEST_IP_ADDRESS,
                AUTH_METHOD_PASSWORD,
                false,
                null,
                null,
                UserSessionModel.SessionPersistenceState.PERSISTENT
        ))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to create user session");
    }

    @Test
    void testGetUserSession_Found() {
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
        when(userProvider.getUserById(realm, TEST_USER_ID)).thenReturn(user);

        UserSessionModel session = provider.getUserSession(realm, TEST_SESSION_ID);

        assertThat(session).isNotNull();
        assertThat(session.getId()).isEqualTo(TEST_SESSION_ID);
    }

    @Test
    void testGetUserSession_NotFound() {
        when(redis.get(anyString(), eq(TEST_SESSION_ID), eq(RedisUserSessionEntity.class)))
                .thenReturn(null);

        UserSessionModel session = provider.getUserSession(realm, TEST_SESSION_ID);

        assertThat(session).isNull();
    }

    @Test
    void testGetUserSession_WithNullId() {
        UserSessionModel session = provider.getUserSession(realm, null);
        assertThat(session).isNull();
    }

    @Test
    void testGetUserSession_WrongRealm() {
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
        entity.setRealmId("different-realm");

        when(redis.get(anyString(), eq(TEST_SESSION_ID), eq(RedisUserSessionEntity.class)))
                .thenReturn(entity);

        UserSessionModel session = provider.getUserSession(realm, TEST_SESSION_ID);

        assertThat(session).isNull();
    }

    @Test
    void testGetUserSession_UserDeleted() {
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

        when(redis.get(anyString(), eq(TEST_SESSION_ID), eq(RedisUserSessionEntity.class)))
                .thenReturn(entity);
        when(userProvider.getUserById(realm, TEST_USER_ID)).thenReturn(null);

        UserSessionModel session = provider.getUserSession(realm, TEST_SESSION_ID);

        assertThat(session).isNull();
        verify(redis).delete(RedisConnectionProvider.USER_SESSION_CACHE_NAME, TEST_SESSION_ID);
    }

    @Test
    void testCreateClientSession() {
        UserSessionModel userSession = mock(UserSessionModel.class);
        when(userSession.getId()).thenReturn(TEST_SESSION_ID);

        when(redis.replaceWithVersion(
                eq(RedisConnectionProvider.CLIENT_SESSION_CACHE_NAME),
                anyString(),
                any(RedisClientSessionEntity.class),
                eq(0L),
                anyLong(),
                any()
        )).thenReturn(true);

        AuthenticatedClientSessionModel clientSession = provider.createClientSession(realm, client, userSession);

        assertThat(clientSession).isNotNull();

        verify(redis).replaceWithVersion(
                eq(RedisConnectionProvider.CLIENT_SESSION_CACHE_NAME),
                anyString(),
                any(RedisClientSessionEntity.class),
                eq(0L),
                anyLong(),
                any()
        );
    }

    @Test
    void testCreateClientSession_WithRememberMe() {
        UserSessionModel userSession = mock(UserSessionModel.class);
        when(userSession.getId()).thenReturn(TEST_SESSION_ID);
        when(userSession.getStarted()).thenReturn(1234567890);
        when(userSession.isRememberMe()).thenReturn(true);

        // Capture the entity to verify the note is set
        when(redis.replaceWithVersion(
                eq(RedisConnectionProvider.CLIENT_SESSION_CACHE_NAME),
                anyString(),
                argThat((RedisClientSessionEntity entity) -> 
                    "true".equals(entity.getNote(AuthenticatedClientSessionModel.USER_SESSION_REMEMBER_ME_NOTE))
                ),
                eq(0L),
                anyLong(),
                any()
        )).thenReturn(true);

        AuthenticatedClientSessionModel clientSession = provider.createClientSession(realm, client, userSession);

        assertThat(clientSession).isNotNull();
    }

    @Test
    void testCreateClientSession_FailureWhenAlreadyExists() {
        UserSessionModel userSession = mock(UserSessionModel.class);
        when(userSession.getId()).thenReturn(TEST_SESSION_ID);

        when(redis.replaceWithVersion(anyString(), anyString(), any(), eq(0L), anyLong(), any()))
                .thenReturn(false);

        assertThatThrownBy(() -> provider.createClientSession(realm, client, userSession))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to create client session");
    }

    @Test
    void testGetClientSession_Found() {
        UserSessionModel userSession = mock(UserSessionModel.class);
        when(userSession.getId()).thenReturn(TEST_SESSION_ID);

        RedisClientSessionEntity entity = RedisClientSessionEntity.create(
                TEST_SESSION_ID,
                TEST_CLIENT_ID,
                TEST_REALM_ID
        );

        String key = TEST_SESSION_ID + ":" + TEST_CLIENT_ID;
        when(redis.get(
                eq(RedisConnectionProvider.CLIENT_SESSION_CACHE_NAME),
                eq(key),
                eq(RedisClientSessionEntity.class)
        )).thenReturn(entity);
        when(realmProvider.getRealm(TEST_REALM_ID)).thenReturn(realm);

        AuthenticatedClientSessionModel clientSession = provider.getClientSession(userSession, client, false);

        assertThat(clientSession).isNotNull();
    }

    @Test
    void testGetClientSession_NotFound() {
        UserSessionModel userSession = mock(UserSessionModel.class);
        when(userSession.getId()).thenReturn(TEST_SESSION_ID);

        when(redis.get(anyString(), anyString(), eq(RedisClientSessionEntity.class)))
                .thenReturn(null);

        AuthenticatedClientSessionModel clientSession = provider.getClientSession(userSession, client, false);

        assertThat(clientSession).isNull();
    }

    @Test
    void testGetClientSession_Offline() {
        UserSessionModel userSession = mock(UserSessionModel.class);
        when(userSession.getId()).thenReturn(TEST_SESSION_ID);

        RedisClientSessionEntity entity = RedisClientSessionEntity.create(
                TEST_SESSION_ID,
                TEST_CLIENT_ID,
                TEST_REALM_ID
        );

        String key = TEST_SESSION_ID + ":" + TEST_CLIENT_ID;
        when(redis.get(
                eq(RedisConnectionProvider.OFFLINE_CLIENT_SESSION_CACHE_NAME),
                eq(key),
                eq(RedisClientSessionEntity.class)
        )).thenReturn(entity);
        when(realmProvider.getRealm(TEST_REALM_ID)).thenReturn(realm);

        AuthenticatedClientSessionModel clientSession = provider.getClientSession(userSession, client, true);

        assertThat(clientSession).isNotNull();
    }

    @Test
    void testRemoveUserSession() {
        UserSessionModel userSession = mock(UserSessionModel.class);
        when(userSession.getId()).thenReturn(TEST_SESSION_ID);
        when(userSession.isOffline()).thenReturn(false);

        provider.removeUserSession(realm, userSession);

        verify(redis).delete(RedisConnectionProvider.USER_SESSION_CACHE_NAME, TEST_SESSION_ID);
    }

    @Test
    void testRemoveOfflineUserSession() {
        UserSessionModel userSession = mock(UserSessionModel.class);
        when(userSession.getId()).thenReturn(TEST_SESSION_ID);
        when(userSession.isOffline()).thenReturn(true);

        provider.removeOfflineUserSession(realm, userSession);

        verify(redis).delete(RedisConnectionProvider.OFFLINE_USER_SESSION_CACHE_NAME, TEST_SESSION_ID);
    }

    @Test
    void testClose() {
        provider.close();
        // Close should not throw any exception
    }

    @Test
    void testGetOfflineUserSession() {
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
        assertThat(session.getId()).isEqualTo(TEST_SESSION_ID);
        assertThat(session.isOffline()).isTrue();
    }

    @Test
    void testRemoveExpiredUserSessions() {
        // This method doesn't do anything in the Redis implementation
        provider.removeExpired(realm);
        // Should not throw
    }

    @Test
    void testRemoveUserSessions() {
        // This method scans and removes all sessions for the realm
        provider.removeUserSessions(realm);
        // The actual implementation iterates through sessions, so we just verify it doesn't throw
    }

    @Test
    void testCreateClientSession_Success() {
        UserSessionModel userSession = mock(UserSessionModel.class);
        when(userSession.getId()).thenReturn(TEST_SESSION_ID);
        when(userSession.getRealm()).thenReturn(realm);
        
        when(redis.replaceWithVersion(
                eq(RedisConnectionProvider.CLIENT_SESSION_CACHE_NAME),
                anyString(),
                any(RedisClientSessionEntity.class),
                eq(0L),
                anyLong(),
                any()
        )).thenReturn(true);
        
        AuthenticatedClientSessionModel clientSession = provider.createClientSession(realm, client, userSession);
        
        assertThat(clientSession).isNotNull();
        verify(redis).replaceWithVersion(
                eq(RedisConnectionProvider.CLIENT_SESSION_CACHE_NAME),
                anyString(),
                any(RedisClientSessionEntity.class),
                eq(0L),
                anyLong(),
                any()
        );
    }


    @Test
    void testGetUserSessionsStream_WithUser() {
        when(redis.scanKeys(eq(RedisConnectionProvider.USER_SESSION_CACHE_NAME), eq("*")))
                .thenReturn(java.util.List.of());
        
        Stream<UserSessionModel> sessions = provider.getUserSessionsStream(realm, user);
        
        assertThat(sessions).isNotNull();
        assertThat(sessions.count()).isEqualTo(0);
    }

    @Test
    void testGetUserSessionsStream_WithClient() {
        when(redis.scanKeys(eq(RedisConnectionProvider.USER_SESSION_CACHE_NAME), eq("*")))
                .thenReturn(java.util.List.of());
        
        Stream<UserSessionModel> sessions = provider.getUserSessionsStream(realm, client);
        
        assertThat(sessions).isNotNull();
        assertThat(sessions.count()).isEqualTo(0);
    }

    @Test
    void testGetUserSessionsStream_WithClientAndPagination() {
        when(redis.scanKeys(eq(RedisConnectionProvider.USER_SESSION_CACHE_NAME), eq("*")))
                .thenReturn(java.util.List.of());
        
        Stream<UserSessionModel> sessions = provider.getUserSessionsStream(realm, client, 0, 10);
        
        assertThat(sessions).isNotNull();
        assertThat(sessions.count()).isEqualTo(0);
    }

    @Test
    void testGetUserSessionByBrokerUserIdStream() {
        when(redis.scanKeys(eq(RedisConnectionProvider.USER_SESSION_CACHE_NAME), eq("*")))
                .thenReturn(java.util.List.of());
        
        Stream<UserSessionModel> sessions = provider.getUserSessionByBrokerUserIdStream(realm, "broker-user-123");
        
        assertThat(sessions).isNotNull();
        assertThat(sessions.count()).isEqualTo(0);
    }

    @Test
    void testGetUserSessionByBrokerSessionId() {
        UserSessionModel session = provider.getUserSessionByBrokerSessionId(realm, "broker-session-123");
        
        assertThat(session).isNull(); // Current implementation returns null
    }

    @Test
    void testGetUserSessionWithPredicate_Found() {
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
        when(userProvider.getUserById(realm, TEST_USER_ID)).thenReturn(user);
        
        UserSessionModel result = provider.getUserSessionWithPredicate(
                realm,
                TEST_SESSION_ID,
                false,
                s -> true
        );
        
        assertThat(result).isNotNull();
    }

    @Test
    void testGetUserSessionWithPredicate_PredicateFails() {
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
        when(userProvider.getUserById(realm, TEST_USER_ID)).thenReturn(user);
        
        UserSessionModel result = provider.getUserSessionWithPredicate(
                realm,
                TEST_SESSION_ID,
                false,
                s -> false
        );
        
        assertThat(result).isNull();
    }

    @Test
    void testGetActiveUserSessions() {
        // Use client index instead of scanKeys
        when(redis.getSortedSetMembers(eq(RedisConnectionProvider.CLIENT_SESSION_CACHE_NAME), eq("client:" + TEST_CLIENT_ID)))
                .thenReturn(java.util.List.of("session1:" + TEST_CLIENT_ID));
        
        RedisClientSessionEntity entity = RedisClientSessionEntity.create("session1", TEST_CLIENT_ID, TEST_REALM_ID);
        // Now expects batch getAll() instead of individual get()
        when(redis.getAll(
                eq(RedisConnectionProvider.CLIENT_SESSION_CACHE_NAME),
                eq(java.util.List.of("session1:" + TEST_CLIENT_ID)),
                eq(RedisClientSessionEntity.class)
        )).thenReturn(java.util.Map.of("session1:" + TEST_CLIENT_ID, entity));
        
        long count = provider.getActiveUserSessions(realm, client);
        
        assertThat(count).isEqualTo(1);
    }

    @Test
    void testGetActiveUserSessions_Exception() {
        when(redis.getSortedSetMembers(anyString(), anyString()))
                .thenThrow(new RuntimeException("Redis error"));
        
        long count = provider.getActiveUserSessions(realm, client);
        
        assertThat(count).isEqualTo(0);
    }

    @Test
    void testGetActiveClientSessionStats() {
        // Use realm index instead of scanKeys
        when(redis.getSortedSetMembers(eq(RedisConnectionProvider.CLIENT_SESSION_CACHE_NAME), eq("realm:" + TEST_REALM_ID)))
                .thenReturn(java.util.List.of("session1:" + TEST_CLIENT_ID, "session2:client2"));
        
        RedisClientSessionEntity entity1 = RedisClientSessionEntity.create("session1", TEST_CLIENT_ID, TEST_REALM_ID);
        RedisClientSessionEntity entity2 = RedisClientSessionEntity.create("session2", "client2", TEST_REALM_ID);
        
        // Now expects batch getAll() instead of individual get() calls
        when(redis.getAll(eq(RedisConnectionProvider.CLIENT_SESSION_CACHE_NAME), 
                eq(java.util.List.of("session1:" + TEST_CLIENT_ID, "session2:client2")), 
                eq(RedisClientSessionEntity.class)))
                .thenReturn(java.util.Map.of(
                        "session1:" + TEST_CLIENT_ID, entity1,
                        "session2:client2", entity2
                ));
        
        java.util.Map<String, Long> stats = provider.getActiveClientSessionStats(realm, false);
        
        assertThat(stats).isNotNull();
        assertThat(stats.get(TEST_CLIENT_ID)).isEqualTo(1);
        assertThat(stats.get("client2")).isEqualTo(1);
    }

    @Test
    void testGetActiveClientSessionStats_Offline() {
        when(redis.scanKeys(eq(RedisConnectionProvider.OFFLINE_CLIENT_SESSION_CACHE_NAME), eq("*")))
                .thenReturn(java.util.List.of());
        
        java.util.Map<String, Long> stats = provider.getActiveClientSessionStats(realm, true);
        
        assertThat(stats).isEmpty();
    }

    @Test
    void testGetActiveClientSessionStats_Exception() {
        when(redis.scanKeys(anyString(), anyString()))
                .thenThrow(new RuntimeException("Redis error"));
        
        java.util.Map<String, Long> stats = provider.getActiveClientSessionStats(realm, false);
        
        assertThat(stats).isEmpty();
    }

    @Test
    void testCreateOfflineUserSession() {
        UserSessionModel onlineSession = mock(UserSessionModel.class);
        when(onlineSession.getId()).thenReturn(TEST_SESSION_ID);
        when(onlineSession.getRealm()).thenReturn(realm);
        when(onlineSession.getUser()).thenReturn(user);
        when(onlineSession.getLoginUsername()).thenReturn(TEST_USERNAME);
        when(onlineSession.getIpAddress()).thenReturn(TEST_IP_ADDRESS);
        when(onlineSession.getAuthMethod()).thenReturn(AUTH_METHOD_PASSWORD);
        when(onlineSession.isRememberMe()).thenReturn(false);
        when(onlineSession.getBrokerSessionId()).thenReturn(null);
        when(onlineSession.getBrokerUserId()).thenReturn(null);
        
        when(redis.replaceWithVersion(
                eq(RedisConnectionProvider.OFFLINE_USER_SESSION_CACHE_NAME),
                anyString(),
                any(RedisUserSessionEntity.class),
                eq(0L),
                anyLong(),
                any()
        )).thenReturn(true);
        
        UserSessionModel offlineSession = provider.createOfflineUserSession(onlineSession);
        
        assertThat(offlineSession).isNotNull();
        assertThat(offlineSession.isOffline()).isTrue();
    }

    @Test
    void testCreateOfflineUserSession_AlreadyExists() {
        UserSessionModel onlineSession = mock(UserSessionModel.class);
        when(onlineSession.getId()).thenReturn(TEST_SESSION_ID);
        when(onlineSession.getRealm()).thenReturn(realm);
        when(onlineSession.getUser()).thenReturn(user);
        when(onlineSession.getLoginUsername()).thenReturn(TEST_USERNAME);
        when(onlineSession.getIpAddress()).thenReturn(TEST_IP_ADDRESS);
        when(onlineSession.getAuthMethod()).thenReturn(AUTH_METHOD_PASSWORD);
        when(onlineSession.isRememberMe()).thenReturn(false);
        when(onlineSession.getBrokerSessionId()).thenReturn(null);
        when(onlineSession.getBrokerUserId()).thenReturn(null);
        
        when(redis.replaceWithVersion(anyString(), anyString(), any(), eq(0L), anyLong(), any()))
                .thenReturn(false);
        
        assertThatThrownBy(() -> provider.createOfflineUserSession(onlineSession))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to create offline user session");
    }

    @Test
    void testCreateOfflineClientSession() {
        AuthenticatedClientSessionModel onlineClientSession = mock(AuthenticatedClientSessionModel.class);
        UserSessionModel onlineUserSession = mock(UserSessionModel.class);
        UserSessionModel offlineUserSession = mock(UserSessionModel.class);
        
        when(onlineClientSession.getId()).thenReturn(TEST_SESSION_ID);
        when(onlineClientSession.getRealm()).thenReturn(realm);
        when(onlineClientSession.getClient()).thenReturn(client);
        when(onlineClientSession.getUserSession()).thenReturn(onlineUserSession);
        when(onlineUserSession.getId()).thenReturn(TEST_SESSION_ID);
        when(offlineUserSession.getId()).thenReturn(TEST_SESSION_ID);
        
        when(redis.replaceWithVersion(
                eq(RedisConnectionProvider.OFFLINE_CLIENT_SESSION_CACHE_NAME),
                anyString(),
                any(RedisClientSessionEntity.class),
                eq(0L),
                anyLong(),
                any()
        )).thenReturn(true);
        
        AuthenticatedClientSessionModel offlineClientSession = 
                provider.createOfflineClientSession(onlineClientSession, offlineUserSession);
        
        assertThat(offlineClientSession).isNotNull();
    }

    @Test
    void testCreateOfflineClientSession_WithRememberMe() {
        AuthenticatedClientSessionModel onlineClientSession = mock(AuthenticatedClientSessionModel.class);
        UserSessionModel onlineUserSession = mock(UserSessionModel.class);
        UserSessionModel offlineUserSession = mock(UserSessionModel.class);
        
        when(onlineClientSession.getId()).thenReturn(TEST_SESSION_ID);
        when(onlineClientSession.getRealm()).thenReturn(realm);
        when(onlineClientSession.getClient()).thenReturn(client);
        when(onlineClientSession.getUserSession()).thenReturn(onlineUserSession);
        when(onlineUserSession.getId()).thenReturn(TEST_SESSION_ID);
        when(offlineUserSession.getId()).thenReturn(TEST_SESSION_ID);
        when(offlineUserSession.getStarted()).thenReturn(1234567890);
        when(offlineUserSession.isRememberMe()).thenReturn(true);
        
        // Capture the entity to verify the note is set
        when(redis.replaceWithVersion(
                eq(RedisConnectionProvider.OFFLINE_CLIENT_SESSION_CACHE_NAME),
                anyString(),
                argThat((RedisClientSessionEntity entity) -> 
                    "true".equals(entity.getNote(AuthenticatedClientSessionModel.USER_SESSION_REMEMBER_ME_NOTE))
                ),
                eq(0L),
                anyLong(),
                any()
        )).thenReturn(true);
        
        AuthenticatedClientSessionModel offlineClientSession = 
                provider.createOfflineClientSession(onlineClientSession, offlineUserSession);
        
        assertThat(offlineClientSession).isNotNull();
    }

    @Test
    void testCreateOfflineClientSession_AlreadyExists() {
        AuthenticatedClientSessionModel onlineClientSession = mock(AuthenticatedClientSessionModel.class);
        UserSessionModel onlineUserSession = mock(UserSessionModel.class);
        UserSessionModel offlineUserSession = mock(UserSessionModel.class);
        
        when(onlineClientSession.getId()).thenReturn(TEST_SESSION_ID);
        when(onlineClientSession.getRealm()).thenReturn(realm);
        when(onlineClientSession.getClient()).thenReturn(client);
        when(onlineClientSession.getUserSession()).thenReturn(onlineUserSession);
        when(onlineUserSession.getId()).thenReturn(TEST_SESSION_ID);
        when(offlineUserSession.getId()).thenReturn(TEST_SESSION_ID);
        
        when(redis.replaceWithVersion(anyString(), anyString(), any(), eq(0L), anyLong(), any()))
                .thenReturn(false);
        
        assertThatThrownBy(() -> provider.createOfflineClientSession(onlineClientSession, offlineUserSession))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to create offline client session");
    }

    @Test
    void testGetOfflineUserSessionsStream_WithUser() {
        when(redis.scanKeys(eq(RedisConnectionProvider.OFFLINE_USER_SESSION_CACHE_NAME), eq("*")))
                .thenReturn(java.util.List.of());
        
        Stream<UserSessionModel> sessions = provider.getOfflineUserSessionsStream(realm, user);
        
        assertThat(sessions).isNotNull();
        assertThat(sessions.count()).isEqualTo(0);
    }

    @Test
    void testGetOfflineUserSessionByBrokerUserIdStream() {
        when(redis.scanKeys(eq(RedisConnectionProvider.OFFLINE_USER_SESSION_CACHE_NAME), eq("*")))
                .thenReturn(java.util.List.of());
        
        Stream<UserSessionModel> sessions = provider.getOfflineUserSessionByBrokerUserIdStream(realm, "broker-user-123");
        
        assertThat(sessions).isNotNull();
        assertThat(sessions.count()).isEqualTo(0);
    }

    @Test
    void testGetOfflineSessionsCount() {
        // Use client index instead of scanKeys
        when(redis.getSortedSetMembers(eq(RedisConnectionProvider.OFFLINE_CLIENT_SESSION_CACHE_NAME), eq("client:" + TEST_CLIENT_ID)))
                .thenReturn(java.util.List.of("session1:" + TEST_CLIENT_ID));
        
        RedisClientSessionEntity entity = RedisClientSessionEntity.create(TEST_SESSION_ID, TEST_CLIENT_ID, TEST_REALM_ID);
        // Now expects batch getAll() instead of individual get()
        when(redis.getAll(
                eq(RedisConnectionProvider.OFFLINE_CLIENT_SESSION_CACHE_NAME),
                eq(java.util.List.of("session1:" + TEST_CLIENT_ID)),
                eq(RedisClientSessionEntity.class)
        )).thenReturn(java.util.Map.of("session1:" + TEST_CLIENT_ID, entity));
        
        long count = provider.getOfflineSessionsCount(realm, client);
        
        assertThat(count).isEqualTo(1);
    }

    @Test
    void testGetOfflineSessionsCount_Exception() {
        when(redis.getSortedSetMembers(anyString(), anyString()))
                .thenThrow(new RuntimeException("Redis error"));
        
        long count = provider.getOfflineSessionsCount(realm, client);
        
        assertThat(count).isEqualTo(0);
    }

    @Test
    void testGetOfflineUserSessionsStream_WithClient() {
        when(redis.scanKeys(eq(RedisConnectionProvider.OFFLINE_USER_SESSION_CACHE_NAME), eq("*")))
                .thenReturn(java.util.List.of());
        
        Stream<UserSessionModel> sessions = provider.getOfflineUserSessionsStream(realm, client, 0, 10);
        
        assertThat(sessions).isNotNull();
        assertThat(sessions.count()).isEqualTo(0);
    }

    @Test
    void testGetOfflineUserSessionsStream_WithClient_Exception() {
        when(redis.scanKeys(anyString(), anyString()))
                .thenThrow(new RuntimeException("Redis error"));
        
        Stream<UserSessionModel> sessions = provider.getOfflineUserSessionsStream(realm, client, 0, 10);
        
        assertThat(sessions).isEmpty();
    }

    @Test
    void testGetStartupTime() {
        org.keycloak.cluster.ClusterProvider clusterProvider = mock(org.keycloak.cluster.ClusterProvider.class);
        when(session.getProvider(org.keycloak.cluster.ClusterProvider.class)).thenReturn(clusterProvider);
        when(clusterProvider.getClusterStartupTime()).thenReturn(12345);
        
        int startupTime = provider.getStartupTime(realm);
        
        assertThat(startupTime).isEqualTo(12345);
    }

    @Test
    void testGetStartupTime_NoClusterProvider() {
        when(session.getProvider(org.keycloak.cluster.ClusterProvider.class)).thenReturn(null);
        
        int startupTime = provider.getStartupTime(realm);
        
        assertThat(startupTime).isGreaterThan(0);
    }

    @Test
    void testGetKeycloakSession() {
        KeycloakSession result = provider.getKeycloakSession();
        
        assertThat(result).isEqualTo(session);
    }

    @Test
    void testMigrate() {
        provider.migrate("1.0");
        // Should not throw - no-op method
    }

    @Test
    void testOnRealmRemoved() {
        // Use realm index instead of scanKeys
        when(redis.getSortedSetMembers(anyString(), eq("realm:" + TEST_REALM_ID)))
                .thenReturn(java.util.List.of());
        
        provider.onRealmRemoved(realm);
        
        // Should trigger removeUserSessions with realm index lookups
        verify(redis, atLeastOnce()).getSortedSetMembers(anyString(), eq("realm:" + TEST_REALM_ID));
    }

    @Test
    void testOnClientRemoved() {
        provider.onClientRemoved(realm, client);
        
        verify(redis).removeByPattern(
                eq(RedisConnectionProvider.CLIENT_SESSION_CACHE_NAME),
                eq("*:" + TEST_CLIENT_ID)
        );
        verify(redis).removeByPattern(
                eq(RedisConnectionProvider.OFFLINE_CLIENT_SESSION_CACHE_NAME),
                eq("*:" + TEST_CLIENT_ID)
        );
    }

    @Test
    void testRemoveAllExpired() {
        provider.removeAllExpired();
        // Should not throw - Redis handles expiration via TTL
    }

    @Test
    void testRemoveUserSessions_ForUser() {
        // Now uses user index instead of scanKeys
        when(redis.getSortedSetMembers(eq(RedisConnectionProvider.USER_SESSION_CACHE_NAME), eq("user:" + TEST_USER_ID)))
                .thenReturn(java.util.List.of());
        when(redis.getSortedSetMembers(eq(RedisConnectionProvider.OFFLINE_USER_SESSION_CACHE_NAME), eq("user:" + TEST_USER_ID)))
                .thenReturn(java.util.List.of());
        
        provider.removeUserSessions(realm, user);
        
        // Verify user index lookups were called
        verify(redis, atLeast(1)).getSortedSetMembers(eq(RedisConnectionProvider.USER_SESSION_CACHE_NAME), eq("user:" + TEST_USER_ID));
        verify(redis, atLeast(1)).getSortedSetMembers(eq(RedisConnectionProvider.OFFLINE_USER_SESSION_CACHE_NAME), eq("user:" + TEST_USER_ID));
    }

    @Test
    void testGetUserSessionsStream_WithClientAndNullPagination() {
        when(redis.scanKeys(eq(RedisConnectionProvider.USER_SESSION_CACHE_NAME), eq("*")))
                .thenReturn(java.util.List.of());
        
        Stream<UserSessionModel> sessions = provider.getUserSessionsStream(realm, client, null, null);
        
        assertThat(sessions).isNotNull();
        assertThat(sessions.count()).isEqualTo(0);
    }

    @Test
    void testGetOfflineUserSessionsStream_WithClientAndNullPagination() {
        when(redis.scanKeys(eq(RedisConnectionProvider.OFFLINE_USER_SESSION_CACHE_NAME), eq("*")))
                .thenReturn(java.util.List.of());
        
        Stream<UserSessionModel> sessions = provider.getOfflineUserSessionsStream(realm, client, null, null);
        
        assertThat(sessions).isNotNull();
        assertThat(sessions.count()).isEqualTo(0);
    }

    @Test
    void testGetActiveClientSessionStats_WithErrorInEntityParsing() {
        // Use realm index instead of scanKeys
        when(redis.getSortedSetMembers(eq(RedisConnectionProvider.CLIENT_SESSION_CACHE_NAME), eq("realm:" + TEST_REALM_ID)))
                .thenReturn(java.util.List.of("session1:client1", "session2:" + TEST_CLIENT_ID));
        
        // With batch getAll(), only valid entity is returned (session1 failed to parse)
        RedisClientSessionEntity entity2 = RedisClientSessionEntity.create("session2", TEST_CLIENT_ID, TEST_REALM_ID);
        when(redis.getAll(eq(RedisConnectionProvider.CLIENT_SESSION_CACHE_NAME), 
                eq(java.util.List.of("session1:client1", "session2:" + TEST_CLIENT_ID)), 
                eq(RedisClientSessionEntity.class)))
                .thenReturn(java.util.Map.of("session2:" + TEST_CLIENT_ID, entity2));
        
        java.util.Map<String, Long> stats = provider.getActiveClientSessionStats(realm, false);
        
        // Should skip session1 due to error and count session2
        assertThat(stats).isNotNull();
        assertThat(stats.get(TEST_CLIENT_ID)).isEqualTo(1);
    }

    @Test
    void testRemoveUserSessions_ForRealm_WithIndexes() {
        // Use realm index instead of scanKeys
        when(redis.getSortedSetMembers(eq(RedisConnectionProvider.USER_SESSION_CACHE_NAME), eq("realm:" + TEST_REALM_ID)))
                .thenReturn(java.util.List.of("session1", "session2"));
        when(redis.getSortedSetMembers(eq(RedisConnectionProvider.OFFLINE_USER_SESSION_CACHE_NAME), eq("realm:" + TEST_REALM_ID)))
                .thenReturn(java.util.List.of());
        
        RedisUserSessionEntity entity1 = RedisUserSessionEntity.create(
                "session1",
                realm,
                user,
                TEST_USERNAME,
                TEST_IP_ADDRESS,
                AUTH_METHOD_PASSWORD,
                false,
                null,
                null
        );
        
        when(redis.get(eq(RedisConnectionProvider.USER_SESSION_CACHE_NAME), eq("session1"), eq(RedisUserSessionEntity.class)))
                .thenReturn(entity1);
        when(redis.get(eq(RedisConnectionProvider.USER_SESSION_CACHE_NAME), eq("session2"), eq(RedisUserSessionEntity.class)))
                .thenReturn(null);  // Session already deleted
        
        provider.removeUserSessions(realm);
        
        // Should attempt to remove both sessions from index
        verify(redis).getSortedSetMembers(eq(RedisConnectionProvider.USER_SESSION_CACHE_NAME), eq("realm:" + TEST_REALM_ID));
    }

    @Test
    void testGetUserSessionWithPredicate_Offline() {
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
        
        UserSessionModel result = provider.getUserSessionWithPredicate(
                realm,
                TEST_SESSION_ID,
                true,
                s -> true
        );
        
        assertThat(result).isNotNull();
        assertThat(result.isOffline()).isTrue();
    }

    // ===================== isUserSessionDeleted() tests =====================

    @Test
    void testIsUserSessionDeleted_NotDeleted() {
        // Initially, no sessions are deleted
        assertThat(provider.isUserSessionDeleted("session1")).isFalse();
        assertThat(provider.isUserSessionDeleted("session2")).isFalse();
    }

    @Test
    void testIsUserSessionDeleted_AfterRemove() {
        // Set up the entity to be returned
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
        when(userProvider.getUserById(realm, TEST_USER_ID)).thenReturn(user);
        
        // Get the user session so we can remove it
        UserSessionModel userSession = provider.getUserSession(realm, TEST_SESSION_ID);
        assertThat(userSession).isNotNull();
        
        // Remove the session
        provider.removeUserSession(realm, userSession);
        
        // Now it should be tracked as deleted
        assertThat(provider.isUserSessionDeleted(TEST_SESSION_ID)).isTrue();
        
        // Other sessions should not be affected
        assertThat(provider.isUserSessionDeleted("other-session")).isFalse();
    }

    @Test
    void testIsUserSessionDeleted_MultipleRemovals() {
        // Set up multiple entities
        RedisUserSessionEntity entity1 = RedisUserSessionEntity.create(
                "session1", realm, user, TEST_USERNAME, TEST_IP_ADDRESS,
                AUTH_METHOD_PASSWORD, false, null, null
        );
        RedisUserSessionEntity entity2 = RedisUserSessionEntity.create(
                "session2", realm, user, TEST_USERNAME, TEST_IP_ADDRESS,
                AUTH_METHOD_PASSWORD, false, null, null
        );
        
        when(redis.get(eq(RedisConnectionProvider.USER_SESSION_CACHE_NAME), eq("session1"), eq(RedisUserSessionEntity.class)))
                .thenReturn(entity1);
        when(redis.get(eq(RedisConnectionProvider.USER_SESSION_CACHE_NAME), eq("session2"), eq(RedisUserSessionEntity.class)))
                .thenReturn(entity2);
        when(userProvider.getUserById(realm, TEST_USER_ID)).thenReturn(user);
        
        UserSessionModel session1 = provider.getUserSession(realm, "session1");
        UserSessionModel session2 = provider.getUserSession(realm, "session2");
        
        // Remove both sessions
        provider.removeUserSession(realm, session1);
        provider.removeUserSession(realm, session2);
        
        // Both should be tracked as deleted
        assertThat(provider.isUserSessionDeleted("session1")).isTrue();
        assertThat(provider.isUserSessionDeleted("session2")).isTrue();
        assertThat(provider.isUserSessionDeleted("session3")).isFalse();
    }
}
