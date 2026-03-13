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
import org.keycloak.cluster.ClusterProvider;
import org.keycloak.models.*;
import org.keycloak.models.redis.RedisConnectionProvider;
import org.keycloak.models.redis.entities.RedisClientSessionEntity;
import org.keycloak.models.redis.entities.RedisUserSessionEntity;
import org.keycloak.models.redis.session.RedisUserSessionProvider;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Additional tests to boost coverage for RedisUserSessionProvider.
 * Focuses on error paths, edge cases, and uncovered branches.
 */
class RedisUserSessionProviderCoverageBoostTest {

    private KeycloakSession session;
    private RedisConnectionProvider redis;
    private RedisUserSessionProvider provider;
    private RealmModel realm;
    private UserModel user;
    private ClientModel client;
    private UserProvider userProvider;
    private RealmProvider realmProvider;
    private ClusterProvider clusterProvider;

    private static final String TEST_REALM_ID = "test-realm";
    private static final String TEST_USER_ID = "user-123";
    private static final String TEST_CLIENT_ID = "client-123";
    private static final String TEST_SESSION_ID = "session-123";
    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_IP_ADDRESS = "192.168.1.1";
    private static final int SSO_SESSION_MAX_LIFESPAN = 3600;
    private static final int OFFLINE_SESSION_IDLE_TIMEOUT = 86400;

    @BeforeEach
    void setUp() {
        session = mock(KeycloakSession.class);
        redis = mock(RedisConnectionProvider.class);
        realm = mock(RealmModel.class);
        user = mock(UserModel.class);
        client = mock(ClientModel.class);
        userProvider = mock(UserProvider.class);
        realmProvider = mock(RealmProvider.class);
        clusterProvider = mock(ClusterProvider.class);

        when(session.users()).thenReturn(userProvider);
        when(session.realms()).thenReturn(realmProvider);
        when(session.getProvider(ClusterProvider.class)).thenReturn(clusterProvider);
        when(realm.getId()).thenReturn(TEST_REALM_ID);
        when(realm.getName()).thenReturn("test-realm");
        when(realm.getSsoSessionMaxLifespan()).thenReturn(SSO_SESSION_MAX_LIFESPAN);
        when(realm.getOfflineSessionIdleTimeout()).thenReturn(OFFLINE_SESSION_IDLE_TIMEOUT);
        when(user.getId()).thenReturn(TEST_USER_ID);
        when(client.getId()).thenReturn(TEST_CLIENT_ID);
        when(userProvider.getUserById(realm, TEST_USER_ID)).thenReturn(user);
        when(clusterProvider.getClusterStartupTime()).thenReturn(1234567890);

        provider = new RedisUserSessionProvider(session, redis, SSO_SESSION_MAX_LIFESPAN, OFFLINE_SESSION_IDLE_TIMEOUT);
    }

    // ==================== Create Session Error Paths ====================

    @Test
    void testCreateUserSession_FailsWhenReplaceWithVersionFails() {
        when(redis.replaceWithVersion(anyString(), anyString(), any(), eq(0L), anyLong(), any()))
            .thenReturn(false);

        assertThatThrownBy(() -> provider.createUserSession(
            TEST_SESSION_ID, realm, user, TEST_USERNAME, TEST_IP_ADDRESS,
            "password", false, null, null, UserSessionModel.SessionPersistenceState.PERSISTENT
        )).isInstanceOf(RuntimeException.class)
          .hasMessageContaining("Failed to create user session");
    }

    @Test
    void testCreateClientSession_FailsWhenReplaceWithVersionFails() {
        UserSessionModel userSession = mock(UserSessionModel.class);
        when(userSession.getId()).thenReturn(TEST_SESSION_ID);
        when(redis.replaceWithVersion(anyString(), anyString(), any(), eq(0L), anyLong(), any()))
            .thenReturn(false);

        assertThatThrownBy(() -> provider.createClientSession(realm, client, userSession))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Failed to create client session");
    }

    @Test
    void testCreateOfflineUserSession_FailsWhenReplaceWithVersionFails() {
        UserSessionModel userSession = mock(UserSessionModel.class);
        when(userSession.getId()).thenReturn(TEST_SESSION_ID);
        when(userSession.getRealm()).thenReturn(realm);
        when(userSession.getUser()).thenReturn(user);
        when(redis.replaceWithVersion(anyString(), anyString(), any(), eq(0L), anyLong(), any()))
            .thenReturn(false);

        assertThatThrownBy(() -> provider.createOfflineUserSession(userSession))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Failed to create offline user session");
    }

    @Test
    void testCreateOfflineClientSession_FailsWhenReplaceWithVersionFails() {
        AuthenticatedClientSessionModel clientSession = mock(AuthenticatedClientSessionModel.class);
        UserSessionModel userSession = mock(UserSessionModel.class);
        when(clientSession.getRealm()).thenReturn(realm);
        when(clientSession.getClient()).thenReturn(client);
        when(clientSession.getUserSession()).thenReturn(userSession);  // Add this mock
        when(userSession.getId()).thenReturn(TEST_SESSION_ID);
        when(redis.replaceWithVersion(anyString(), anyString(), any(), eq(0L), anyLong(), any()))
            .thenReturn(false);

        assertThatThrownBy(() -> provider.createOfflineClientSession(clientSession, userSession))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Failed to create offline client session");
    }

    // ==================== Session Creation with Index ====================

    @Test
    void testCreateUserSession_AddsToRealmIndex() {
        when(redis.replaceWithVersion(anyString(), anyString(), any(), eq(0L), anyLong(), any()))
            .thenReturn(true);
        when(redis.addToSortedSet(anyString(), anyString(), anyString(), anyDouble(), anyLong(), any()))
            .thenReturn(true);

        UserSessionModel result = provider.createUserSession(
            TEST_SESSION_ID, realm, user, TEST_USERNAME, TEST_IP_ADDRESS,
            "password", false, null, null, UserSessionModel.SessionPersistenceState.PERSISTENT
        );
        
        assertThat(result).isNotNull();
        // Verify session was added to realm index
        verify(redis).addToSortedSet(
            eq(RedisConnectionProvider.USER_SESSION_CACHE_NAME),
            eq("realm:" + TEST_REALM_ID),
            eq(TEST_SESSION_ID),
            anyDouble(),
            anyLong(),
            any()
        );
    }

    @Test
    void testCreateOfflineUserSession_AddsToRealmIndex() {
        UserSessionModel userSession = mock(UserSessionModel.class);
        when(userSession.getId()).thenReturn(TEST_SESSION_ID);
        when(userSession.getRealm()).thenReturn(realm);
        when(userSession.getUser()).thenReturn(user);
        when(userSession.getAuthenticatedClientSessions()).thenReturn(Collections.emptyMap());
        when(redis.replaceWithVersion(anyString(), anyString(), any(), eq(0L), anyLong(), any()))
            .thenReturn(true);
        when(redis.addToSortedSet(anyString(), anyString(), anyString(), anyDouble(), anyLong(), any()))
            .thenReturn(true);

        UserSessionModel result = provider.createOfflineUserSession(userSession);
        
        assertThat(result).isNotNull();
        // Verify session was added to realm index
        verify(redis).addToSortedSet(
            eq(RedisConnectionProvider.OFFLINE_USER_SESSION_CACHE_NAME),
            eq("realm:" + TEST_REALM_ID),
            eq(TEST_SESSION_ID),
            anyDouble(),
            anyLong(),
            any()
        );
    }

    @Test
    void testRemoveUserSession_WithClientSessionCleanup() {
        RedisUserSessionEntity userSessionEntity = createUserSessionEntity(TEST_SESSION_ID, TEST_USER_ID);
        UserSessionModel userSession = mock(UserSessionModel.class);
        when(userSession.getId()).thenReturn(TEST_SESSION_ID);
        
        // Setup client sessions for cleanup
        RedisClientSessionEntity clientEntity1 = RedisClientSessionEntity.create(TEST_SESSION_ID, "client-1", TEST_REALM_ID);
        RedisClientSessionEntity clientEntity2 = RedisClientSessionEntity.create(TEST_SESSION_ID, "client-2", TEST_REALM_ID);
        
        when(redis.get(eq(RedisConnectionProvider.USER_SESSION_CACHE_NAME), eq(TEST_SESSION_ID), eq(RedisUserSessionEntity.class)))
            .thenReturn(userSessionEntity);
        // Use userSession index instead of scanKeys
        when(redis.getSortedSetMembers(eq(RedisConnectionProvider.CLIENT_SESSION_CACHE_NAME), eq("userSession:" + TEST_SESSION_ID)))
            .thenReturn(List.of(TEST_SESSION_ID + ":client-1", TEST_SESSION_ID + ":client-2"));
        when(redis.get(eq(RedisConnectionProvider.CLIENT_SESSION_CACHE_NAME), eq(TEST_SESSION_ID + ":client-1"), eq(RedisClientSessionEntity.class)))
            .thenReturn(clientEntity1);
        when(redis.get(eq(RedisConnectionProvider.CLIENT_SESSION_CACHE_NAME), eq(TEST_SESSION_ID + ":client-2"), eq(RedisClientSessionEntity.class)))
            .thenReturn(clientEntity2);
        when(redis.removeFromSortedSet(anyString(), anyString(), anyString()))
            .thenReturn(true);

        provider.removeUserSession(realm, userSession);
        
        // Verify user session was deleted
        verify(redis).delete(eq(RedisConnectionProvider.USER_SESSION_CACHE_NAME), eq(TEST_SESSION_ID));
        
        // Verify client sessions were removed from realm index
        // Verify user session was removed from realm index
        verify(redis).removeFromSortedSet(
            eq(RedisConnectionProvider.USER_SESSION_CACHE_NAME),
            eq("realm:" + TEST_REALM_ID),
            eq(TEST_SESSION_ID)
        );
        
        // Verify user session was removed from user index
        verify(redis).removeFromSortedSet(
            eq(RedisConnectionProvider.USER_SESSION_CACHE_NAME),
            eq("user:" + TEST_USER_ID),
            eq(TEST_SESSION_ID)
        );
        
        // Verify userSession index was deleted
        verify(redis).delete(
            eq(RedisConnectionProvider.CLIENT_SESSION_CACHE_NAME),
            eq("userSession:" + TEST_SESSION_ID)
        );
        
        // Verify client sessions were removed from client index
        verify(redis).removeFromSortedSet(
            eq(RedisConnectionProvider.CLIENT_SESSION_CACHE_NAME),
            eq("client:client-1"),
            eq(TEST_SESSION_ID + ":client-1")
        );
        verify(redis).removeFromSortedSet(
            eq(RedisConnectionProvider.CLIENT_SESSION_CACHE_NAME),
            eq("client:client-2"),
            eq(TEST_SESSION_ID + ":client-2")
        );
        
        // Verify client sessions were removed from realm index
        verify(redis).removeFromSortedSet(
            eq(RedisConnectionProvider.CLIENT_SESSION_CACHE_NAME),
            eq("realm:" + TEST_REALM_ID),
            eq(TEST_SESSION_ID + ":client-1")
        );
        verify(redis).removeFromSortedSet(
            eq(RedisConnectionProvider.CLIENT_SESSION_CACHE_NAME),
            eq("realm:" + TEST_REALM_ID),
            eq(TEST_SESSION_ID + ":client-2")
        );
        
        // Verify user session was removed from realm index
        verify(redis).removeFromSortedSet(
            eq(RedisConnectionProvider.USER_SESSION_CACHE_NAME),
            eq("realm:" + TEST_REALM_ID),
            eq(TEST_SESSION_ID)
        );
        
        // Verify client session data was deleted
        verify(redis).removeByPattern(eq(RedisConnectionProvider.CLIENT_SESSION_CACHE_NAME), eq(TEST_SESSION_ID + ":*"));
    }

    @Test
    void testRemoveUserSession_NoClientSessions() {
        RedisUserSessionEntity userSessionEntity = createUserSessionEntity(TEST_SESSION_ID, TEST_USER_ID);
        UserSessionModel userSession = mock(UserSessionModel.class);
        when(userSession.getId()).thenReturn(TEST_SESSION_ID);
        
        when(redis.get(eq(RedisConnectionProvider.USER_SESSION_CACHE_NAME), eq(TEST_SESSION_ID), eq(RedisUserSessionEntity.class)))
            .thenReturn(userSessionEntity);
        when(redis.scanKeys(eq(RedisConnectionProvider.CLIENT_SESSION_CACHE_NAME), eq(TEST_SESSION_ID + ":*")))
            .thenReturn(Collections.emptyList());
        when(redis.removeFromSortedSet(anyString(), anyString(), anyString()))
            .thenReturn(true);

        provider.removeUserSession(realm, userSession);
        
        // Verify user session was deleted
        verify(redis).delete(eq(RedisConnectionProvider.USER_SESSION_CACHE_NAME), eq(TEST_SESSION_ID));
        
        // Verify user session was removed from realm index
        verify(redis).removeFromSortedSet(
            eq(RedisConnectionProvider.USER_SESSION_CACHE_NAME),
            eq("realm:" + TEST_REALM_ID),
            eq(TEST_SESSION_ID)
        );
        
        // Verify client sessions deletion was still attempted
        verify(redis).removeByPattern(eq(RedisConnectionProvider.CLIENT_SESSION_CACHE_NAME), eq(TEST_SESSION_ID + ":*"));
    }

    @Test
    void testRemoveUserSession_EntityNotFound() {
        UserSessionModel userSession = mock(UserSessionModel.class);
        when(userSession.getId()).thenReturn(TEST_SESSION_ID);
        
        when(redis.get(eq(RedisConnectionProvider.USER_SESSION_CACHE_NAME), eq(TEST_SESSION_ID), eq(RedisUserSessionEntity.class)))
            .thenReturn(null);  // Entity not found

        provider.removeUserSession(realm, userSession);
        
        // Verify user session deletion was still attempted
        verify(redis).delete(eq(RedisConnectionProvider.USER_SESSION_CACHE_NAME), eq(TEST_SESSION_ID));
        
        // Verify no index cleanup was attempted (entity was null)
        verify(redis, never()).removeFromSortedSet(anyString(), anyString(), anyString());
        
        // Verify client sessions deletion was still attempted
        verify(redis).removeByPattern(eq(RedisConnectionProvider.CLIENT_SESSION_CACHE_NAME), eq(TEST_SESSION_ID + ":*"));
    }

    @Test
    void testRemoveUserSession_ClientEntityNotFound() {
        RedisUserSessionEntity userSessionEntity = createUserSessionEntity(TEST_SESSION_ID, TEST_USER_ID);
        UserSessionModel userSession = mock(UserSessionModel.class);
        when(userSession.getId()).thenReturn(TEST_SESSION_ID);
        
        when(redis.get(eq(RedisConnectionProvider.USER_SESSION_CACHE_NAME), eq(TEST_SESSION_ID), eq(RedisUserSessionEntity.class)))
            .thenReturn(userSessionEntity);
        when(redis.scanKeys(eq(RedisConnectionProvider.CLIENT_SESSION_CACHE_NAME), eq(TEST_SESSION_ID + ":*")))
            .thenReturn(List.of(TEST_SESSION_ID + ":client-1"));
        when(redis.get(eq(RedisConnectionProvider.CLIENT_SESSION_CACHE_NAME), eq(TEST_SESSION_ID + ":client-1"), eq(RedisClientSessionEntity.class)))
            .thenReturn(null);  // Client entity not found
        when(redis.removeFromSortedSet(anyString(), anyString(), anyString()))
            .thenReturn(true);

        provider.removeUserSession(realm, userSession);
        
        // Verify user session was deleted
        verify(redis).delete(eq(RedisConnectionProvider.USER_SESSION_CACHE_NAME), eq(TEST_SESSION_ID));
        
        // Verify user session was removed from realm index
        verify(redis).removeFromSortedSet(
            eq(RedisConnectionProvider.USER_SESSION_CACHE_NAME),
            eq("realm:" + TEST_REALM_ID),
            eq(TEST_SESSION_ID)
        );
        
        // Verify NO client index cleanup happened (client entity was null)
        verify(redis, never()).removeFromSortedSet(
            eq(RedisConnectionProvider.CLIENT_SESSION_CACHE_NAME),
            anyString(),
            eq(TEST_SESSION_ID + ":client-1")
        );
    }

    @Test
    void testRemoveUserSession_WithRealmIdNull() {
        RedisUserSessionEntity userSessionEntity = createUserSessionEntity(TEST_SESSION_ID, TEST_USER_ID);
        userSessionEntity.setRealmId(null);  // Null realm ID
        UserSessionModel userSession = mock(UserSessionModel.class);
        when(userSession.getId()).thenReturn(TEST_SESSION_ID);
        
        when(redis.get(eq(RedisConnectionProvider.USER_SESSION_CACHE_NAME), eq(TEST_SESSION_ID), eq(RedisUserSessionEntity.class)))
            .thenReturn(userSessionEntity);

        provider.removeUserSession(realm, userSession);
        
        // Verify session was deleted
        verify(redis).delete(eq(RedisConnectionProvider.USER_SESSION_CACHE_NAME), eq(TEST_SESSION_ID));
        
        // Verify no index cleanup was attempted (realm ID was null)
        verify(redis, never()).removeFromSortedSet(anyString(), anyString(), anyString());
        
        // Verify client sessions deletion was still attempted
        verify(redis).removeByPattern(eq(RedisConnectionProvider.CLIENT_SESSION_CACHE_NAME), eq(TEST_SESSION_ID + ":*"));
    }

    @Test
    void testGetUserSessionsStream_RealmWithPagination() {
        RedisUserSessionEntity entity1 = createUserSessionEntity("session-1", TEST_USER_ID);
        RedisUserSessionEntity entity2 = createUserSessionEntity("session-2", "user-2");
        RedisUserSessionEntity entity3 = createUserSessionEntity("session-3", "user-3");
        
        UserModel user2 = mock(UserModel.class);
        UserModel user3 = mock(UserModel.class);
        when(user2.getId()).thenReturn("user-2");
        when(user3.getId()).thenReturn("user-3");
        
        UserProvider userProvider = mock(UserProvider.class);
        when(session.users()).thenReturn(userProvider);
        when(userProvider.getUserById(realm, TEST_USER_ID)).thenReturn(user);
        when(userProvider.getUserById(realm, "user-2")).thenReturn(user2);
        when(userProvider.getUserById(realm, "user-3")).thenReturn(user3);
        
        when(redis.getSortedSetMembers(anyString(), anyString()))
            .thenReturn(List.of("session-1", "session-2", "session-3"));
        when(redis.get(anyString(), eq("session-1"), eq(RedisUserSessionEntity.class)))
            .thenReturn(entity1);
        when(redis.get(anyString(), eq("session-2"), eq(RedisUserSessionEntity.class)))
            .thenReturn(entity2);
        when(redis.get(anyString(), eq("session-3"), eq(RedisUserSessionEntity.class)))
            .thenReturn(entity3);

        // Test the new getUserSessionsStream(realm, firstResult, maxResults) method
        Stream<UserSessionModel> result = provider.getUserSessionsStream(realm, 1, 1);
        
        List<UserSessionModel> sessions = result.collect(Collectors.toList());
        assertThat(sessions).hasSize(1);
        assertThat(sessions.get(0).getId()).isEqualTo("session-2");
    }

    // ==================== Client Session Stats Tests ====================

    @Test
    void testGetActiveClientSessionStats_Success() {
        RedisClientSessionEntity entity1 = RedisClientSessionEntity.create("session-1", "client-1", TEST_REALM_ID);
        
        RedisClientSessionEntity entity2 = RedisClientSessionEntity.create("session-1", "client-2", TEST_REALM_ID);
        
        RedisClientSessionEntity entity3 = RedisClientSessionEntity.create("session-2", "client-1", TEST_REALM_ID);
        
        when(redis.getSortedSetMembers(eq(RedisConnectionProvider.CLIENT_SESSION_CACHE_NAME), anyString()))
            .thenReturn(List.of("session-1:client-1", "session-1:client-2", "session-2:client-1"));
        // Batch getAll()
        when(redis.getAll(eq(RedisConnectionProvider.CLIENT_SESSION_CACHE_NAME), 
                eq(List.of("session-1:client-1", "session-1:client-2", "session-2:client-1")), 
                eq(RedisClientSessionEntity.class)))
            .thenReturn(Map.of(
                "session-1:client-1", entity1,
                "session-1:client-2", entity2,
                "session-2:client-1", entity3
            ));

        Map<String, Long> stats = provider.getActiveClientSessionStats(realm, false);
        
        assertThat(stats).hasSize(2);
        assertThat(stats.get("client-1")).isEqualTo(2L);
        assertThat(stats.get("client-2")).isEqualTo(1L);
    }

    @Test
    void testGetActiveClientSessionStats_EmptyIndex() {
        when(redis.getSortedSetMembers(anyString(), anyString()))
            .thenReturn(Collections.emptyList());

        Map<String, Long> stats = provider.getActiveClientSessionStats(realm, false);
        
        assertThat(stats).isEmpty();
    }

    @Test
    void testGetActiveClientSessionStats_WithExpiredSessions() {
        RedisClientSessionEntity entity1 = RedisClientSessionEntity.create("session-1", "client-1", TEST_REALM_ID);
        
        when(redis.getSortedSetMembers(eq(RedisConnectionProvider.CLIENT_SESSION_CACHE_NAME), anyString()))
            .thenReturn(List.of("session-1:client-1", "session-expired:client-2"));
        // Batch getAll() - expired session not in Map
        when(redis.getAll(eq(RedisConnectionProvider.CLIENT_SESSION_CACHE_NAME), 
                eq(List.of("session-1:client-1", "session-expired:client-2")), 
                eq(RedisClientSessionEntity.class)))
            .thenReturn(Map.of("session-1:client-1", entity1));
        when(redis.removeFromSortedSet(anyString(), anyString(), anyString()))
            .thenReturn(true);

        Map<String, Long> stats = provider.getActiveClientSessionStats(realm, false);
        
        assertThat(stats).hasSize(1);
        assertThat(stats.get("client-1")).isEqualTo(1L);
        // Verify cleanup of expired session from index
        verify(redis).removeFromSortedSet(
            eq(RedisConnectionProvider.CLIENT_SESSION_CACHE_NAME),
            eq("realm:" + TEST_REALM_ID),
            eq("session-expired:client-2")
        );
    }

    @Test
    void testGetActiveClientSessionStats_SkipsErrorEntries() {
        RedisClientSessionEntity entity1 = RedisClientSessionEntity.create("session-1", "client-1", TEST_REALM_ID);
        
        when(redis.getSortedSetMembers(eq(RedisConnectionProvider.CLIENT_SESSION_CACHE_NAME), anyString()))
            .thenReturn(List.of("session-1:client-1", "session-error:client-2"));
        // Batch getAll() - error entry handled gracefully, not in result
        when(redis.getAll(eq(RedisConnectionProvider.CLIENT_SESSION_CACHE_NAME), 
                eq(List.of("session-1:client-1", "session-error:client-2")), 
                eq(RedisClientSessionEntity.class)))
            .thenReturn(Map.of("session-1:client-1", entity1));

        Map<String, Long> stats = provider.getActiveClientSessionStats(realm, false);
        
        // Should still return the valid entry, skipping the error
        assertThat(stats).hasSize(1);
        assertThat(stats.get("client-1")).isEqualTo(1L);
    }

    @Test
    void testGetActiveClientSessionStats_ExceptionReturnsEmptyMap() {
        when(redis.getSortedSetMembers(anyString(), anyString()))
            .thenThrow(new RuntimeException("Redis connection error"));

        Map<String, Long> stats = provider.getActiveClientSessionStats(realm, false);
        
        assertThat(stats).isEmpty();
    }

    @Test
    void testGetActiveClientSessionStats_Offline() {
        RedisClientSessionEntity entity1 = RedisClientSessionEntity.create("offline-session", "client-1", TEST_REALM_ID);
        
        when(redis.getSortedSetMembers(eq(RedisConnectionProvider.OFFLINE_CLIENT_SESSION_CACHE_NAME), anyString()))
            .thenReturn(List.of("offline-session:client-1"));
        // Batch getAll()
        when(redis.getAll(eq(RedisConnectionProvider.OFFLINE_CLIENT_SESSION_CACHE_NAME), 
                eq(List.of("offline-session:client-1")), 
                eq(RedisClientSessionEntity.class)))
            .thenReturn(Map.of("offline-session:client-1", entity1));

        Map<String, Long> stats = provider.getActiveClientSessionStats(realm, true);
        
        assertThat(stats).hasSize(1);
        assertThat(stats.get("client-1")).isEqualTo(1L);
    }

    // ==================== Get Session Edge Cases ====================

    @Test
    void testGetUserSession_ReturnsNullForNullId() {
        UserSessionModel result = provider.getUserSession(realm, null);
        
        assertThat(result).isNull();
    }

    @Test
    void testGetUserSession_ReturnsNullWhenEntityNotFound() {
        when(redis.get(anyString(), eq(TEST_SESSION_ID), eq(RedisUserSessionEntity.class)))
            .thenReturn(null);

        UserSessionModel result = provider.getUserSession(realm, TEST_SESSION_ID);
        
        assertThat(result).isNull();
    }

    @Test
    void testGetUserSession_ReturnsNullWhenRealmMismatch() {
        RedisUserSessionEntity entity = new RedisUserSessionEntity();
        entity.setId(TEST_SESSION_ID);
        entity.setRealmId("different-realm");
        entity.setUserId(TEST_USER_ID);
        
        when(redis.get(anyString(), eq(TEST_SESSION_ID), eq(RedisUserSessionEntity.class)))
            .thenReturn(entity);

        UserSessionModel result = provider.getUserSession(realm, TEST_SESSION_ID);
        
        assertThat(result).isNull();
    }

    @Test
    void testGetUserSession_DeletesOrphanedSessionWhenUserNotFound() {
        RedisUserSessionEntity entity = new RedisUserSessionEntity();
        entity.setId(TEST_SESSION_ID);
        entity.setRealmId(TEST_REALM_ID);
        entity.setUserId("deleted-user-id");
        
        when(redis.get(anyString(), eq(TEST_SESSION_ID), eq(RedisUserSessionEntity.class)))
            .thenReturn(entity);
        when(userProvider.getUserById(realm, "deleted-user-id")).thenReturn(null);

        UserSessionModel result = provider.getUserSession(realm, TEST_SESSION_ID);
        
        assertThat(result).isNull();
        verify(redis).delete(anyString(), eq(TEST_SESSION_ID));
    }

    @Test
    void testGetClientSession_ReturnsNullWhenEntityNotFound() {
        UserSessionModel userSession = mock(UserSessionModel.class);
        when(userSession.getId()).thenReturn(TEST_SESSION_ID);
        when(redis.get(anyString(), anyString(), eq(RedisClientSessionEntity.class)))
            .thenReturn(null);

        AuthenticatedClientSessionModel result = provider.getClientSession(userSession, client, false);
        
        assertThat(result).isNull();
    }

    // ==================== Session Streams and Filtering ====================

    @Test
    void testGetUserSessionsStream_FiltersCorrectly() {
        RedisUserSessionEntity entity1 = createUserSessionEntity(TEST_SESSION_ID, TEST_USER_ID);
        
        // Now uses user index - should only return sessions for THIS user
        when(redis.getSortedSetMembers(eq(RedisConnectionProvider.USER_SESSION_CACHE_NAME), eq("user:" + TEST_USER_ID)))
            .thenReturn(List.of(TEST_SESSION_ID));
        when(redis.get(eq(RedisConnectionProvider.USER_SESSION_CACHE_NAME), eq(TEST_SESSION_ID), eq(RedisUserSessionEntity.class)))
            .thenReturn(entity1);

        Stream<UserSessionModel> result = provider.getUserSessionsStream(realm, user);
        
        assertThat(result).isNotNull();
        assertThat(result.count()).isEqualTo(1);
    }

    @Test
    void testGetUserSessionsStream_WithPagination() {
        when(redis.getSortedSetMembers(anyString(), anyString()))
            .thenReturn(Collections.emptyList());

        Stream<UserSessionModel> result = provider.getUserSessionsStream(realm, client, 10, 20);
        
        assertThat(result).isNotNull();
    }

    @Test
    void testGetUserSessionsStream_HandlesException() {
        when(redis.getSortedSetMembers(anyString(), anyString()))
            .thenReturn(List.of(TEST_SESSION_ID));
        when(redis.get(anyString(), eq(TEST_SESSION_ID), eq(RedisUserSessionEntity.class)))
            .thenThrow(new RuntimeException("Redis error"));

        Stream<UserSessionModel> result = provider.getUserSessionsStream(realm, user);
        
        // Should handle exception and continue
        assertThat(result).isNotNull();
    }

    @Test
    void testGetUserSessionWithPredicate_PredicateReturnsFalse() {
        RedisUserSessionEntity entity = createUserSessionEntity(TEST_SESSION_ID, TEST_USER_ID);
        when(redis.get(anyString(), eq(TEST_SESSION_ID), eq(RedisUserSessionEntity.class)))
            .thenReturn(entity);

        UserSessionModel result = provider.getUserSessionWithPredicate(
            realm, TEST_SESSION_ID, false, session -> false
        );
        
        assertThat(result).isNull();
    }

    @Test
    void testGetUserSessionWithPredicate_PredicateReturnsTrue() {
        RedisUserSessionEntity entity = createUserSessionEntity(TEST_SESSION_ID, TEST_USER_ID);
        when(redis.get(anyString(), eq(TEST_SESSION_ID), eq(RedisUserSessionEntity.class)))
            .thenReturn(entity);

        UserSessionModel result = provider.getUserSessionWithPredicate(
            realm, TEST_SESSION_ID, false, session -> true
        );
        
        assertThat(result).isNotNull();
    }

    // ==================== Active Session Counts ====================

    @Test
    void testGetActiveUserSessions_HandlesException() {
        when(redis.getSortedSetMembers(anyString(), anyString()))
            .thenThrow(new RuntimeException("Redis error"));

        long result = provider.getActiveUserSessions(realm, client);
        
        assertThat(result).isEqualTo(0);
    }

    @Test
    void testGetActiveUserSessions_CountsMultipleSessions() {
        when(redis.getSortedSetMembers(eq(RedisConnectionProvider.CLIENT_SESSION_CACHE_NAME), eq("client:" + TEST_CLIENT_ID)))
            .thenReturn(List.of("session1:" + TEST_CLIENT_ID, "session2:" + TEST_CLIENT_ID));
        
        RedisClientSessionEntity entity1 = RedisClientSessionEntity.create("session1", TEST_CLIENT_ID, TEST_REALM_ID);
        RedisClientSessionEntity entity2 = RedisClientSessionEntity.create("session2", TEST_CLIENT_ID, TEST_REALM_ID);
        
        // Now expects batch getAll() instead of individual get()
        when(redis.getAll(eq(RedisConnectionProvider.CLIENT_SESSION_CACHE_NAME), 
                eq(List.of("session1:" + TEST_CLIENT_ID, "session2:" + TEST_CLIENT_ID)), 
                eq(RedisClientSessionEntity.class)))
            .thenReturn(Map.of(
                "session1:" + TEST_CLIENT_ID, entity1,
                "session2:" + TEST_CLIENT_ID, entity2
            ));

        long result = provider.getActiveUserSessions(realm, client);
        
        assertThat(result).isEqualTo(2);
    }

    @Test
    void testGetActiveUserSessions_FiltersDifferentRealm() {
        when(redis.getSortedSetMembers(eq(RedisConnectionProvider.CLIENT_SESSION_CACHE_NAME), eq("client:" + TEST_CLIENT_ID)))
            .thenReturn(List.of("session1:" + TEST_CLIENT_ID, "session2:" + TEST_CLIENT_ID));
        
        RedisClientSessionEntity entity1 = RedisClientSessionEntity.create("session1", TEST_CLIENT_ID, TEST_REALM_ID);
        RedisClientSessionEntity entity2 = RedisClientSessionEntity.create("session2", TEST_CLIENT_ID, "other-realm");
        
        // Now expects batch getAll() instead of individual get()
        when(redis.getAll(eq(RedisConnectionProvider.CLIENT_SESSION_CACHE_NAME), 
                eq(List.of("session1:" + TEST_CLIENT_ID, "session2:" + TEST_CLIENT_ID)), 
                eq(RedisClientSessionEntity.class)))
            .thenReturn(Map.of(
                "session1:" + TEST_CLIENT_ID, entity1,
                "session2:" + TEST_CLIENT_ID, entity2
            ));

        long result = provider.getActiveUserSessions(realm, client);
        
        assertThat(result).isEqualTo(1);
    }

    @Test
    void testGetActiveUserSessions_EmptyIndex() {
        // Test early return when index is empty
        when(redis.getSortedSetMembers(eq(RedisConnectionProvider.CLIENT_SESSION_CACHE_NAME), eq("client:" + TEST_CLIENT_ID)))
            .thenReturn(Collections.emptyList());

        long result = provider.getActiveUserSessions(realm, client);
        
        assertThat(result).isEqualTo(0);
        // Should not attempt to get any entities
        verify(redis, never()).get(anyString(), anyString(), eq(RedisClientSessionEntity.class));
    }

    @Test
    void testGetActiveUserSessions_NullEntitiesFiltered() {
        when(redis.getSortedSetMembers(eq(RedisConnectionProvider.CLIENT_SESSION_CACHE_NAME), eq("client:" + TEST_CLIENT_ID)))
            .thenReturn(List.of("session1:" + TEST_CLIENT_ID, "session2:" + TEST_CLIENT_ID, "session3:" + TEST_CLIENT_ID));
        
        RedisClientSessionEntity entity1 = RedisClientSessionEntity.create("session1", TEST_CLIENT_ID, TEST_REALM_ID);
        // entity2 is null (expired/deleted) - omitted from Map
        RedisClientSessionEntity entity3 = RedisClientSessionEntity.create("session3", TEST_CLIENT_ID, TEST_REALM_ID);
        
        // Batch getAll() returns only non-null entities
        when(redis.getAll(eq(RedisConnectionProvider.CLIENT_SESSION_CACHE_NAME), 
                eq(List.of("session1:" + TEST_CLIENT_ID, "session2:" + TEST_CLIENT_ID, "session3:" + TEST_CLIENT_ID)), 
                eq(RedisClientSessionEntity.class)))
            .thenReturn(Map.of(
                "session1:" + TEST_CLIENT_ID, entity1,
                "session3:" + TEST_CLIENT_ID, entity3
            ));

        long result = provider.getActiveUserSessions(realm, client);
        
        // Should only count non-null entities
        assertThat(result).isEqualTo(2);
    }

    @Test
    void testGetActiveUserSessions_MixedRealmsAndNullEntities() {
        when(redis.getSortedSetMembers(eq(RedisConnectionProvider.CLIENT_SESSION_CACHE_NAME), eq("client:" + TEST_CLIENT_ID)))
            .thenReturn(List.of(
                "session1:" + TEST_CLIENT_ID, 
                "session2:" + TEST_CLIENT_ID, 
                "session3:" + TEST_CLIENT_ID,
                "session4:" + TEST_CLIENT_ID
            ));
        
        RedisClientSessionEntity entity1 = RedisClientSessionEntity.create("session1", TEST_CLIENT_ID, TEST_REALM_ID);
        // entity2 is null - omitted from Map
        RedisClientSessionEntity entity3 = RedisClientSessionEntity.create("session3", TEST_CLIENT_ID, "other-realm");
        RedisClientSessionEntity entity4 = RedisClientSessionEntity.create("session4", TEST_CLIENT_ID, TEST_REALM_ID);
        
        // Batch getAll() returns all non-null entities
        when(redis.getAll(eq(RedisConnectionProvider.CLIENT_SESSION_CACHE_NAME), 
                eq(List.of("session1:" + TEST_CLIENT_ID, "session2:" + TEST_CLIENT_ID, 
                           "session3:" + TEST_CLIENT_ID, "session4:" + TEST_CLIENT_ID)), 
                eq(RedisClientSessionEntity.class)))
            .thenReturn(Map.of(
                "session1:" + TEST_CLIENT_ID, entity1,
                "session3:" + TEST_CLIENT_ID, entity3,
                "session4:" + TEST_CLIENT_ID, entity4
            ));

        long result = provider.getActiveUserSessions(realm, client);
        
        // Should only count non-null entities in the correct realm
        assertThat(result).isEqualTo(2);
    }

    @Test
    void testGetActiveUserSessions_SingleSession() {
        when(redis.getSortedSetMembers(eq(RedisConnectionProvider.CLIENT_SESSION_CACHE_NAME), eq("client:" + TEST_CLIENT_ID)))
            .thenReturn(List.of("session1:" + TEST_CLIENT_ID));
        
        RedisClientSessionEntity entity1 = RedisClientSessionEntity.create("session1", TEST_CLIENT_ID, TEST_REALM_ID);
        
        // Batch getAll()
        when(redis.getAll(eq(RedisConnectionProvider.CLIENT_SESSION_CACHE_NAME), 
                eq(List.of("session1:" + TEST_CLIENT_ID)), 
                eq(RedisClientSessionEntity.class)))
            .thenReturn(Map.of("session1:" + TEST_CLIENT_ID, entity1));

        long result = provider.getActiveUserSessions(realm, client);
        
        assertThat(result).isEqualTo(1);
    }

    @Test
    void testGetActiveClientSessionStats_HandlesException() {
        when(redis.scanKeys(anyString(), anyString()))
            .thenThrow(new RuntimeException("Redis error"));

        Map<String, Long> result = provider.getActiveClientSessionStats(realm, false);
        
        assertThat(result).isEmpty();
    }

    @Test
    void testGetActiveClientSessionStats_MultipleSessions() {
        RedisClientSessionEntity entity1 = RedisClientSessionEntity.create("session-1", TEST_CLIENT_ID, TEST_REALM_ID);
        
        when(redis.getSortedSetMembers(eq(RedisConnectionProvider.CLIENT_SESSION_CACHE_NAME), anyString()))
            .thenReturn(List.of("session-1:" + TEST_CLIENT_ID));
        // Batch getAll()
        when(redis.getAll(eq(RedisConnectionProvider.CLIENT_SESSION_CACHE_NAME), 
                eq(List.of("session-1:" + TEST_CLIENT_ID)), 
                eq(RedisClientSessionEntity.class)))
            .thenReturn(Map.of("session-1:" + TEST_CLIENT_ID, entity1));

        Map<String, Long> result = provider.getActiveClientSessionStats(realm, false);
        
        assertThat(result).containsEntry(TEST_CLIENT_ID, 1L);
    }

    @Test
    void testGetActiveClientSessionStats_AggregatesByClient() {
        RedisClientSessionEntity entity1 = RedisClientSessionEntity.create("session-1", "client-1", TEST_REALM_ID);
        RedisClientSessionEntity entity2 = RedisClientSessionEntity.create("session-2", "client-1", TEST_REALM_ID);
        RedisClientSessionEntity entity3 = RedisClientSessionEntity.create("session-3", "client-2", TEST_REALM_ID);
        
        when(redis.getSortedSetMembers(eq(RedisConnectionProvider.CLIENT_SESSION_CACHE_NAME), anyString()))
            .thenReturn(List.of("session-1:client-1", "session-2:client-1", "session-3:client-2"));
        // Batch getAll()
        when(redis.getAll(eq(RedisConnectionProvider.CLIENT_SESSION_CACHE_NAME), 
                eq(List.of("session-1:client-1", "session-2:client-1", "session-3:client-2")), 
                eq(RedisClientSessionEntity.class)))
            .thenReturn(Map.of(
                "session-1:client-1", entity1,
                "session-2:client-1", entity2,
                "session-3:client-2", entity3
            ));

        Map<String, Long> result = provider.getActiveClientSessionStats(realm, false);
        
        assertThat(result).containsEntry("client-1", 2L);
        assertThat(result).containsEntry("client-2", 1L);
    }

    @Test
    void testGetActiveClientSessionStats_HandlesNullEntityInIndex() {
        RedisClientSessionEntity entity = RedisClientSessionEntity.create("session-2", TEST_CLIENT_ID, TEST_REALM_ID);
        
        when(redis.getSortedSetMembers(eq(RedisConnectionProvider.CLIENT_SESSION_CACHE_NAME), anyString()))
            .thenReturn(List.of("session-1:client-x", "session-2:" + TEST_CLIENT_ID));
        // Batch getAll() - null entity not returned
        when(redis.getAll(eq(RedisConnectionProvider.CLIENT_SESSION_CACHE_NAME), 
                eq(List.of("session-1:client-x", "session-2:" + TEST_CLIENT_ID)), 
                eq(RedisClientSessionEntity.class)))
            .thenReturn(Map.of("session-2:" + TEST_CLIENT_ID, entity));
        when(redis.removeFromSortedSet(anyString(), anyString(), anyString()))
            .thenReturn(true);

        Map<String, Long> result = provider.getActiveClientSessionStats(realm, false);
        
        assertThat(result).containsEntry(TEST_CLIENT_ID, 1L);
        // Verify stale entry was cleaned up
        verify(redis).removeFromSortedSet(
            eq(RedisConnectionProvider.CLIENT_SESSION_CACHE_NAME),
            eq("realm:" + TEST_REALM_ID),
            eq("session-1:client-x")
        );
    }

    // ==================== Remove Operations ====================

    @Test
    void testRemoveUserSessions_ForRealm() {
        RedisUserSessionEntity entity1 = createUserSessionEntity(TEST_SESSION_ID, TEST_USER_ID);
        RedisUserSessionEntity entity2 = createUserSessionEntity("session-456", TEST_USER_ID);
        
        // Use realm index instead of scanKeys
        when(redis.getSortedSetMembers(eq(RedisConnectionProvider.USER_SESSION_CACHE_NAME), eq("realm:" + TEST_REALM_ID)))
            .thenReturn(List.of(TEST_SESSION_ID, "session-456"));
        when(redis.getSortedSetMembers(eq(RedisConnectionProvider.OFFLINE_USER_SESSION_CACHE_NAME), eq("realm:" + TEST_REALM_ID)))
            .thenReturn(Collections.emptyList());
        when(redis.get(anyString(), eq(TEST_SESSION_ID), eq(RedisUserSessionEntity.class)))
            .thenReturn(entity1);
        when(redis.get(anyString(), eq("session-456"), eq(RedisUserSessionEntity.class)))
            .thenReturn(entity2);

        provider.removeUserSessions(realm);
        
        // Verify both sessions were deleted
        verify(redis, atLeast(2)).delete(anyString(), anyString());
    }

    @Test
    void testRemoveUserSessions_EmptyIndex() {
        // Use realm index instead of scanKeys
        when(redis.getSortedSetMembers(eq(RedisConnectionProvider.USER_SESSION_CACHE_NAME), eq("realm:" + TEST_REALM_ID)))
            .thenReturn(Collections.emptyList());
        when(redis.getSortedSetMembers(eq(RedisConnectionProvider.OFFLINE_USER_SESSION_CACHE_NAME), eq("realm:" + TEST_REALM_ID)))
            .thenReturn(Collections.emptyList());

        provider.removeUserSessions(realm);
        
        // Should not attempt to delete anything
        verify(redis, never()).delete(anyString(), anyString());
    }

    @Test
    void testRemoveUserSessions_HandlesExceptionDuringRemoval() {
        // Use realm index instead of scanKeys
        when(redis.getSortedSetMembers(eq(RedisConnectionProvider.USER_SESSION_CACHE_NAME), eq("realm:" + TEST_REALM_ID)))
            .thenReturn(List.of(TEST_SESSION_ID));
        when(redis.getSortedSetMembers(eq(RedisConnectionProvider.OFFLINE_USER_SESSION_CACHE_NAME), eq("realm:" + TEST_REALM_ID)))
            .thenReturn(Collections.emptyList());
        when(redis.get(anyString(), eq(TEST_SESSION_ID), eq(RedisUserSessionEntity.class)))
            .thenThrow(new RuntimeException("Redis error"));

        // Should not throw
        assertThatCode(() -> provider.removeUserSessions(realm))
            .doesNotThrowAnyException();
    }

    @Test
    void testRemoveUserSessions_WithOfflineSessions() {
        RedisUserSessionEntity offlineEntity1 = createUserSessionEntity("offline-1", TEST_USER_ID);
        RedisUserSessionEntity offlineEntity2 = createUserSessionEntity("offline-2", "user-2");
        
        // No regular sessions, only offline
        when(redis.getSortedSetMembers(eq(RedisConnectionProvider.USER_SESSION_CACHE_NAME), eq("realm:" + TEST_REALM_ID)))
            .thenReturn(Collections.emptyList());
        when(redis.getSortedSetMembers(eq(RedisConnectionProvider.OFFLINE_USER_SESSION_CACHE_NAME), eq("realm:" + TEST_REALM_ID)))
            .thenReturn(List.of("offline-1", "offline-2"));
        
        when(redis.get(eq(RedisConnectionProvider.OFFLINE_USER_SESSION_CACHE_NAME), eq("offline-1"), eq(RedisUserSessionEntity.class)))
            .thenReturn(offlineEntity1);
        when(redis.get(eq(RedisConnectionProvider.OFFLINE_USER_SESSION_CACHE_NAME), eq("offline-2"), eq(RedisUserSessionEntity.class)))
            .thenReturn(offlineEntity2);

        provider.removeUserSessions(realm);
        
        // Verify offline sessions were deleted
        verify(redis, atLeast(2)).delete(eq(RedisConnectionProvider.OFFLINE_USER_SESSION_CACHE_NAME), anyString());
    }

    @Test
    void testRemoveUserSessions_BothUserAndOfflineSessions() {
        RedisUserSessionEntity userEntity = createUserSessionEntity(TEST_SESSION_ID, TEST_USER_ID);
        RedisUserSessionEntity offlineEntity = createUserSessionEntity("offline-1", TEST_USER_ID);
        
        // Both user and offline sessions
        when(redis.getSortedSetMembers(eq(RedisConnectionProvider.USER_SESSION_CACHE_NAME), eq("realm:" + TEST_REALM_ID)))
            .thenReturn(List.of(TEST_SESSION_ID));
        when(redis.getSortedSetMembers(eq(RedisConnectionProvider.OFFLINE_USER_SESSION_CACHE_NAME), eq("realm:" + TEST_REALM_ID)))
            .thenReturn(List.of("offline-1"));
        
        when(redis.get(eq(RedisConnectionProvider.USER_SESSION_CACHE_NAME), eq(TEST_SESSION_ID), eq(RedisUserSessionEntity.class)))
            .thenReturn(userEntity);
        when(redis.get(eq(RedisConnectionProvider.OFFLINE_USER_SESSION_CACHE_NAME), eq("offline-1"), eq(RedisUserSessionEntity.class)))
            .thenReturn(offlineEntity);

        provider.removeUserSessions(realm);
        
        // Verify both types were deleted
        verify(redis, atLeast(1)).delete(eq(RedisConnectionProvider.USER_SESSION_CACHE_NAME), anyString());
        verify(redis, atLeast(1)).delete(eq(RedisConnectionProvider.OFFLINE_USER_SESSION_CACHE_NAME), anyString());
    }

    @Test
    void testRemoveUserSessions_ExceptionGettingUserSessionIndex() {
        // Exception when getting user session index (first try-catch)
        when(redis.getSortedSetMembers(eq(RedisConnectionProvider.USER_SESSION_CACHE_NAME), eq("realm:" + TEST_REALM_ID)))
            .thenThrow(new RuntimeException("Failed to get user session index"));
        
        // Offline sessions should still be processed
        when(redis.getSortedSetMembers(eq(RedisConnectionProvider.OFFLINE_USER_SESSION_CACHE_NAME), eq("realm:" + TEST_REALM_ID)))
            .thenReturn(Collections.emptyList());

        // Should not throw, should continue to offline sessions
        assertThatCode(() -> provider.removeUserSessions(realm))
            .doesNotThrowAnyException();
        
        // Verify offline session index was still queried
        verify(redis).getSortedSetMembers(eq(RedisConnectionProvider.OFFLINE_USER_SESSION_CACHE_NAME), eq("realm:" + TEST_REALM_ID));
    }

    @Test
    void testRemoveUserSessions_ExceptionGettingOfflineSessionIndex() {
        // User sessions succeed
        when(redis.getSortedSetMembers(eq(RedisConnectionProvider.USER_SESSION_CACHE_NAME), eq("realm:" + TEST_REALM_ID)))
            .thenReturn(Collections.emptyList());
        
        // Exception when getting offline session index (second try-catch)
        when(redis.getSortedSetMembers(eq(RedisConnectionProvider.OFFLINE_USER_SESSION_CACHE_NAME), eq("realm:" + TEST_REALM_ID)))
            .thenThrow(new RuntimeException("Failed to get offline session index"));

        // Should not throw
        assertThatCode(() -> provider.removeUserSessions(realm))
            .doesNotThrowAnyException();
    }

    @Test
    void testRemoveUserSessions_PartialFailure() {
        RedisUserSessionEntity entity1 = createUserSessionEntity("session-1", TEST_USER_ID);
        RedisUserSessionEntity entity2 = createUserSessionEntity("session-2", TEST_USER_ID);
        
        when(redis.getSortedSetMembers(eq(RedisConnectionProvider.USER_SESSION_CACHE_NAME), eq("realm:" + TEST_REALM_ID)))
            .thenReturn(List.of("session-1", "session-2", "session-3"));
        when(redis.getSortedSetMembers(eq(RedisConnectionProvider.OFFLINE_USER_SESSION_CACHE_NAME), eq("realm:" + TEST_REALM_ID)))
            .thenReturn(Collections.emptyList());
        
        when(redis.get(eq(RedisConnectionProvider.USER_SESSION_CACHE_NAME), eq("session-1"), eq(RedisUserSessionEntity.class)))
            .thenReturn(entity1);
        when(redis.get(eq(RedisConnectionProvider.USER_SESSION_CACHE_NAME), eq("session-2"), eq(RedisUserSessionEntity.class)))
            .thenReturn(entity2);
        // session-3 fails
        when(redis.get(eq(RedisConnectionProvider.USER_SESSION_CACHE_NAME), eq("session-3"), eq(RedisUserSessionEntity.class)))
            .thenReturn(null);

        provider.removeUserSessions(realm);
        
        // Should successfully remove 2 out of 3 sessions
        verify(redis, atLeast(2)).delete(eq(RedisConnectionProvider.USER_SESSION_CACHE_NAME), anyString());
    }

    @Test
    void testRemoveUserSessions_MultipleSessions() {
        List<RedisUserSessionEntity> entities = new ArrayList<>();
        List<String> sessionIds = new ArrayList<>();
        
        // Create 5 user sessions and 3 offline sessions
        for (int i = 1; i <= 5; i++) {
            String sessionId = "session-" + i;
            sessionIds.add(sessionId);
            entities.add(createUserSessionEntity(sessionId, TEST_USER_ID));
        }
        
        List<String> offlineSessionIds = List.of("offline-1", "offline-2", "offline-3");
        
        when(redis.getSortedSetMembers(eq(RedisConnectionProvider.USER_SESSION_CACHE_NAME), eq("realm:" + TEST_REALM_ID)))
            .thenReturn(sessionIds);
        when(redis.getSortedSetMembers(eq(RedisConnectionProvider.OFFLINE_USER_SESSION_CACHE_NAME), eq("realm:" + TEST_REALM_ID)))
            .thenReturn(offlineSessionIds);
        
        // Mock all session entities
        for (int i = 0; i < 5; i++) {
            when(redis.get(eq(RedisConnectionProvider.USER_SESSION_CACHE_NAME), eq(sessionIds.get(i)), eq(RedisUserSessionEntity.class)))
                .thenReturn(entities.get(i));
        }
        for (String offlineId : offlineSessionIds) {
            when(redis.get(eq(RedisConnectionProvider.OFFLINE_USER_SESSION_CACHE_NAME), eq(offlineId), eq(RedisUserSessionEntity.class)))
                .thenReturn(createUserSessionEntity(offlineId, TEST_USER_ID));
        }

        provider.removeUserSessions(realm);
        
        // Verify all 8 sessions were deleted (5 user + 3 offline)
        verify(redis, atLeast(8)).delete(anyString(), anyString());
    }

    // ==================== Offline Sessions ====================

    @Test
    void testGetOfflineSessionsCount_HandlesException() {
        when(redis.getSortedSetMembers(anyString(), anyString()))
            .thenThrow(new RuntimeException("Redis error"));

        long result = provider.getOfflineSessionsCount(realm, client);
        
        assertThat(result).isEqualTo(0);
    }

    @Test
    void testGetOfflineSessionsCount_Success() {
        RedisClientSessionEntity entity1 = RedisClientSessionEntity.create("offline-1", TEST_CLIENT_ID, TEST_REALM_ID);
        RedisClientSessionEntity entity2 = RedisClientSessionEntity.create("offline-2", TEST_CLIENT_ID, TEST_REALM_ID);
        
        when(redis.getSortedSetMembers(eq(RedisConnectionProvider.OFFLINE_CLIENT_SESSION_CACHE_NAME), eq("client:" + TEST_CLIENT_ID)))
            .thenReturn(List.of("offline-1:" + TEST_CLIENT_ID, "offline-2:" + TEST_CLIENT_ID));
        // Batch getAll()
        when(redis.getAll(eq(RedisConnectionProvider.OFFLINE_CLIENT_SESSION_CACHE_NAME), 
                eq(List.of("offline-1:" + TEST_CLIENT_ID, "offline-2:" + TEST_CLIENT_ID)), 
                eq(RedisClientSessionEntity.class)))
            .thenReturn(Map.of(
                "offline-1:" + TEST_CLIENT_ID, entity1,
                "offline-2:" + TEST_CLIENT_ID, entity2
            ));

        long result = provider.getOfflineSessionsCount(realm, client);
        
        assertThat(result).isEqualTo(2);
    }

    @Test
    void testGetOfflineSessionsCount_EmptyIndex() {
        // Test early return when index is empty
        when(redis.getSortedSetMembers(eq(RedisConnectionProvider.OFFLINE_CLIENT_SESSION_CACHE_NAME), eq("client:" + TEST_CLIENT_ID)))
            .thenReturn(Collections.emptyList());

        long result = provider.getOfflineSessionsCount(realm, client);
        
        assertThat(result).isEqualTo(0);
        // Should not attempt to get any entities
        verify(redis, never()).get(anyString(), anyString(), eq(RedisClientSessionEntity.class));
    }

    @Test
    void testGetOfflineSessionsCount_FiltersDifferentRealm() {
        RedisClientSessionEntity entity1 = RedisClientSessionEntity.create("offline-1", TEST_CLIENT_ID, TEST_REALM_ID);
        RedisClientSessionEntity entity2 = RedisClientSessionEntity.create("offline-2", TEST_CLIENT_ID, "other-realm");
        
        when(redis.getSortedSetMembers(eq(RedisConnectionProvider.OFFLINE_CLIENT_SESSION_CACHE_NAME), eq("client:" + TEST_CLIENT_ID)))
            .thenReturn(List.of("offline-1:" + TEST_CLIENT_ID, "offline-2:" + TEST_CLIENT_ID));
        // Batch getAll()
        when(redis.getAll(eq(RedisConnectionProvider.OFFLINE_CLIENT_SESSION_CACHE_NAME), 
                eq(List.of("offline-1:" + TEST_CLIENT_ID, "offline-2:" + TEST_CLIENT_ID)), 
                eq(RedisClientSessionEntity.class)))
            .thenReturn(Map.of(
                "offline-1:" + TEST_CLIENT_ID, entity1,
                "offline-2:" + TEST_CLIENT_ID, entity2
            ));

        long result = provider.getOfflineSessionsCount(realm, client);
        
        // Should only count entity1 (correct realm)
        assertThat(result).isEqualTo(1);
    }

    @Test
    void testGetOfflineSessionsCount_NullEntitiesFiltered() {
        RedisClientSessionEntity entity1 = RedisClientSessionEntity.create("offline-1", TEST_CLIENT_ID, TEST_REALM_ID);
        // entity2 is null (expired) - omitted from Map
        RedisClientSessionEntity entity3 = RedisClientSessionEntity.create("offline-3", TEST_CLIENT_ID, TEST_REALM_ID);
        
        when(redis.getSortedSetMembers(eq(RedisConnectionProvider.OFFLINE_CLIENT_SESSION_CACHE_NAME), eq("client:" + TEST_CLIENT_ID)))
            .thenReturn(List.of("offline-1:" + TEST_CLIENT_ID, "offline-2:" + TEST_CLIENT_ID, "offline-3:" + TEST_CLIENT_ID));
        // Batch getAll() - null entries omitted
        when(redis.getAll(eq(RedisConnectionProvider.OFFLINE_CLIENT_SESSION_CACHE_NAME), 
                eq(List.of("offline-1:" + TEST_CLIENT_ID, "offline-2:" + TEST_CLIENT_ID, "offline-3:" + TEST_CLIENT_ID)), 
                eq(RedisClientSessionEntity.class)))
            .thenReturn(Map.of(
                "offline-1:" + TEST_CLIENT_ID, entity1,
                "offline-3:" + TEST_CLIENT_ID, entity3
            ));

        long result = provider.getOfflineSessionsCount(realm, client);
        
        // Should only count non-null entities
        assertThat(result).isEqualTo(2);
    }

    @Test
    void testGetOfflineSessionsCount_MixedRealmsAndNullEntities() {
        RedisClientSessionEntity entity1 = RedisClientSessionEntity.create("offline-1", TEST_CLIENT_ID, TEST_REALM_ID);
        // entity2 is null - omitted from Map
        RedisClientSessionEntity entity3 = RedisClientSessionEntity.create("offline-3", TEST_CLIENT_ID, "other-realm");
        RedisClientSessionEntity entity4 = RedisClientSessionEntity.create("offline-4", TEST_CLIENT_ID, TEST_REALM_ID);
        
        when(redis.getSortedSetMembers(eq(RedisConnectionProvider.OFFLINE_CLIENT_SESSION_CACHE_NAME), eq("client:" + TEST_CLIENT_ID)))
            .thenReturn(List.of(
                "offline-1:" + TEST_CLIENT_ID, 
                "offline-2:" + TEST_CLIENT_ID, 
                "offline-3:" + TEST_CLIENT_ID,
                "offline-4:" + TEST_CLIENT_ID
            ));
        // Batch getAll()
        when(redis.getAll(eq(RedisConnectionProvider.OFFLINE_CLIENT_SESSION_CACHE_NAME), 
                eq(List.of("offline-1:" + TEST_CLIENT_ID, "offline-2:" + TEST_CLIENT_ID, 
                           "offline-3:" + TEST_CLIENT_ID, "offline-4:" + TEST_CLIENT_ID)), 
                eq(RedisClientSessionEntity.class)))
            .thenReturn(Map.of(
                "offline-1:" + TEST_CLIENT_ID, entity1,
                "offline-3:" + TEST_CLIENT_ID, entity3,
                "offline-4:" + TEST_CLIENT_ID, entity4
            ));

        long result = provider.getOfflineSessionsCount(realm, client);
        
        // Should only count non-null entities in correct realm (entity1 and entity4)
        assertThat(result).isEqualTo(2);
    }

    @Test
    void testGetOfflineSessionsCount_SingleSession() {
        RedisClientSessionEntity entity = RedisClientSessionEntity.create("offline-1", TEST_CLIENT_ID, TEST_REALM_ID);
        
        when(redis.getSortedSetMembers(eq(RedisConnectionProvider.OFFLINE_CLIENT_SESSION_CACHE_NAME), eq("client:" + TEST_CLIENT_ID)))
            .thenReturn(List.of("offline-1:" + TEST_CLIENT_ID));
        // Batch getAll()
        when(redis.getAll(eq(RedisConnectionProvider.OFFLINE_CLIENT_SESSION_CACHE_NAME), 
                eq(List.of("offline-1:" + TEST_CLIENT_ID)), 
                eq(RedisClientSessionEntity.class)))
            .thenReturn(Map.of("offline-1:" + TEST_CLIENT_ID, entity));

        long result = provider.getOfflineSessionsCount(realm, client);
        
        assertThat(result).isEqualTo(1);
    }

    @Test
    void testGetOfflineUserSessionsStream_WithPagination() {
        when(redis.getSortedSetMembers(anyString(), anyString()))
            .thenReturn(Collections.emptyList());

        Stream<UserSessionModel> result = provider.getOfflineUserSessionsStream(realm, client, 0, 10);
        
        assertThat(result).isNotNull();
    }

    @Test
    void testGetOfflineUserSessionsStream_HandlesException() {
        when(redis.getSortedSetMembers(anyString(), anyString()))
            .thenThrow(new RuntimeException("Redis error"));

        Stream<UserSessionModel> result = provider.getOfflineUserSessionsStream(realm, client, 0, 10);
        
        assertThat(result).isEmpty();
    }

    // ==================== Fallback to SCAN ====================

    @Test
    void testGetAllUserSessionsForRealm_FallsBackToEmptyList() {
        // getUserSessionsStream(realm, user) now uses user index directly
        // Empty user index means no sessions
        when(redis.getSortedSetMembers(eq(RedisConnectionProvider.USER_SESSION_CACHE_NAME), eq("user:" + TEST_USER_ID)))
            .thenReturn(Collections.emptyList());

        Stream<UserSessionModel> result = provider.getUserSessionsStream(realm, user);
        
        // Should return empty stream
        assertThat(result.count()).isEqualTo(0);
    }

    @Test
    void testGetAllUserSessionsForRealm_HandlesExceptionInIndex() {
        // getUserSessionsStream now uses user index and has fallback to getAllUserSessionsForRealm
        when(redis.getSortedSetMembers(eq(RedisConnectionProvider.USER_SESSION_CACHE_NAME), eq("user:" + TEST_USER_ID)))
            .thenThrow(new RuntimeException("Redis error"));
        
        // Fallback uses realm index, then scans if that fails
        when(redis.getSortedSetMembers(eq(RedisConnectionProvider.USER_SESSION_CACHE_NAME), eq("realm:" + TEST_REALM_ID)))
            .thenReturn(Collections.emptyList());
        when(redis.scanKeys(eq(RedisConnectionProvider.USER_SESSION_CACHE_NAME), eq("*")))
            .thenReturn(Collections.emptyList());

        Stream<UserSessionModel> result = provider.getUserSessionsStream(realm, user);
        
        // Should fall back gracefully and return empty stream
        assertThat(result.count()).isEqualTo(0);
    }

    @Test
    void testGetAllUserSessionsForRealm_CleansUpExpiredSession() {
        // getUserSessionsStream now uses user index
        String userIndexKey = "user:" + TEST_USER_ID;
        when(redis.getSortedSetMembers(eq(RedisConnectionProvider.USER_SESSION_CACHE_NAME), eq(userIndexKey)))
            .thenReturn(List.of(TEST_SESSION_ID));
        when(redis.get(eq(RedisConnectionProvider.USER_SESSION_CACHE_NAME), eq(TEST_SESSION_ID), eq(RedisUserSessionEntity.class)))
            .thenReturn(null);  // Session expired

        Stream<UserSessionModel> result = provider.getUserSessionsStream(realm, user);
        
        // Consume the stream to trigger processing
        long count = result.count();
        
        // Expired sessions are filtered out
        assertThat(count).isEqualTo(0);
        
        // Verify user index was checked
        verify(redis).getSortedSetMembers(eq(RedisConnectionProvider.USER_SESSION_CACHE_NAME), eq(userIndexKey));
    }

    @Test
    void testGetAllUserSessionsForRealm_CleansUpDeletedUser() {
        // getUserSessionsStream now uses user index - test with THIS user's session
        String userIndexKey = "user:" + TEST_USER_ID;
        RedisUserSessionEntity entity = createUserSessionEntity(TEST_SESSION_ID, TEST_USER_ID);
        
        when(redis.getSortedSetMembers(eq(RedisConnectionProvider.USER_SESSION_CACHE_NAME), eq(userIndexKey)))
            .thenReturn(List.of(TEST_SESSION_ID));
        when(redis.get(eq(RedisConnectionProvider.USER_SESSION_CACHE_NAME), eq(TEST_SESSION_ID), eq(RedisUserSessionEntity.class)))
            .thenReturn(entity);
        // User deleted
        when(userProvider.getUserById(realm, TEST_USER_ID)).thenReturn(null);

        Stream<UserSessionModel> result = provider.getUserSessionsStream(realm, user);
        
        // Consume the stream to trigger processing
        long count = result.count();
        
        // Sessions with deleted users are filtered out
        assertThat(count).isEqualTo(0);
        
        // Verify index was used
        verify(redis).getSortedSetMembers(eq(RedisConnectionProvider.USER_SESSION_CACHE_NAME), eq(userIndexKey));
    }

    @Test
    void testScanAllUserSessionsForRealm_FiltersRealmAndVersionKeys() {
        // getUserSessionsStream now uses user index directly
        RedisUserSessionEntity entity1 = createUserSessionEntity("session-1", TEST_USER_ID);
        
        // Mock user index to return this user's sessions
        when(redis.getSortedSetMembers(eq(RedisConnectionProvider.USER_SESSION_CACHE_NAME), eq("user:" + TEST_USER_ID)))
            .thenReturn(List.of("session-1"));
        when(redis.get(eq(RedisConnectionProvider.USER_SESSION_CACHE_NAME), eq("session-1"), eq(RedisUserSessionEntity.class)))
            .thenReturn(entity1);
        // Ensure user exists
        when(userProvider.getUserById(realm, TEST_USER_ID)).thenReturn(user);

        Stream<UserSessionModel> result = provider.getUserSessionsStream(realm, user);
        
        assertThat(result.count()).isEqualTo(1);
    }

    // ==================== Other Operations ====================

    @Test
    void testGetUserSessionByBrokerSessionId_ReturnsNull() {
        // Not implemented in current version
        UserSessionModel result = provider.getUserSessionByBrokerSessionId(realm, "broker-123");
        
        assertThat(result).isNull();
    }

    @Test
    void testGetStartupTime_WithNullClusterProvider() {
        when(session.getProvider(ClusterProvider.class)).thenReturn(null);

        int result = provider.getStartupTime(realm);
        
        // Should return current time if cluster provider is null
        assertThat(result).isGreaterThan(0);
    }

    @Test
    void testGetStartupTime_WithClusterProvider() {
        int result = provider.getStartupTime(realm);
        
        assertThat(result).isEqualTo(1234567890);
    }

    @Test
    void testGetKeycloakSession() {
        KeycloakSession result = provider.getKeycloakSession();
        
        assertThat(result).isEqualTo(session);
    }

    @Test
    void testClose_DoesNothing() {
        // Should not throw
        assertThatCode(() -> provider.close()).doesNotThrowAnyException();
    }

    @Test
    void testMigrate_DoesNothing() {
        // Should not throw
        assertThatCode(() -> provider.migrate("1.0.0")).doesNotThrowAnyException();
    }

    @Test
    void testRemoveAllExpired_DoesNothing() {
        // Redis handles expiration automatically
        assertThatCode(() -> provider.removeAllExpired()).doesNotThrowAnyException();
    }

    @Test
    void testRemoveExpired_DoesNothing() {
        // Redis handles expiration automatically
        assertThatCode(() -> provider.removeExpired(realm)).doesNotThrowAnyException();
    }

    @Test
    void testOnRealmRemoved_CallsRemoveUserSessions() {
        // Use realm index instead of scanKeys
        when(redis.getSortedSetMembers(anyString(), eq("realm:" + TEST_REALM_ID)))
            .thenReturn(Collections.emptyList());

        provider.onRealmRemoved(realm);
        
        // Should use realm index lookups, not scanKeys
        verify(redis, atLeast(1)).getSortedSetMembers(anyString(), eq("realm:" + TEST_REALM_ID));
    }

    @Test
    void testOnClientRemoved_RemovesClientSessions() {
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

    // ==================== Helper Methods ====================

    private RedisUserSessionEntity createUserSessionEntity(String sessionId, String userId) {
        RedisUserSessionEntity entity = new RedisUserSessionEntity();
        entity.setId(sessionId);
        entity.setRealmId(TEST_REALM_ID);
        entity.setUserId(userId);
        entity.setLoginUsername(TEST_USERNAME);
        entity.setIpAddress(TEST_IP_ADDRESS);
        entity.setStarted((int) (System.currentTimeMillis() / 1000));
        return entity;
    }

    private RedisClientSessionEntity createClientSessionEntity(String key, String clientId, String realmId) {
        RedisClientSessionEntity entity = new RedisClientSessionEntity();
        entity.setUserSessionId("session-123");
        entity.setClientId(clientId);
        entity.setRealmId(realmId);
        return entity;
    }
}
