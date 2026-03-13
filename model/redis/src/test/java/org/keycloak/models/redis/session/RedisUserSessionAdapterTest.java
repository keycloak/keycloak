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
import org.keycloak.models.redis.session.RedisUserSessionAdapter;
import org.keycloak.models.redis.session.RedisUserSessionProvider;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RedisUserSessionAdapter.
 */
class RedisUserSessionAdapterTest {

    private KeycloakSession session;
    private RealmModel realm;
    private UserModel user;
    private RedisUserSessionEntity entity;
    private RedisConnectionProvider redis;
    private KeycloakTransactionManager transactionManager;
    private RedisUserSessionAdapter adapter;

    @BeforeEach
    void setUp() {
        session = mock(KeycloakSession.class);
        realm = mock(RealmModel.class);
        user = mock(UserModel.class);
        entity = new RedisUserSessionEntity();
        redis = mock(RedisConnectionProvider.class);
        transactionManager = mock(KeycloakTransactionManager.class);
        
        entity.setId("session1");
        entity.setRealmId("realm1");
        entity.setUserId("user1");
        entity.setLoginUsername("testuser");
        entity.setIpAddress("192.168.1.1");
        entity.setAuthMethod("password");
        entity.setRememberMe(true);
        entity.setStarted(1000);
        entity.setLastSessionRefresh(2000);
        entity.setState(UserSessionModel.State.LOGGED_IN);
        
        when(session.getTransactionManager()).thenReturn(transactionManager);
        when(realm.getId()).thenReturn("realm1");
        when(realm.getSsoSessionMaxLifespan()).thenReturn(3600);
        when(realm.getOfflineSessionIdleTimeout()).thenReturn(86400);
        
        adapter = new RedisUserSessionAdapter(session, realm, user, entity, redis, false, 1L);
    }

    @Test
    void testGetId() {
        assertThat(adapter.getId()).isEqualTo("session1");
    }

    @Test
    void testGetRealm() {
        assertThat(adapter.getRealm()).isEqualTo(realm);
    }

    @Test
    void testGetBrokerSessionId() {
        entity.setBrokerSessionId("broker123");
        assertThat(adapter.getBrokerSessionId()).isEqualTo("broker123");
    }

    @Test
    void testGetBrokerUserId() {
        entity.setBrokerUserId("brokerUser");
        assertThat(adapter.getBrokerUserId()).isEqualTo("brokerUser");
    }

    @Test
    void testGetUser() {
        assertThat(adapter.getUser()).isEqualTo(user);
    }

    @Test
    void testGetLoginUsername() {
        assertThat(adapter.getLoginUsername()).isEqualTo("testuser");
    }

    @Test
    void testGetIpAddress() {
        assertThat(adapter.getIpAddress()).isEqualTo("192.168.1.1");
    }

    @Test
    void testGetAuthMethod() {
        assertThat(adapter.getAuthMethod()).isEqualTo("password");
    }

    @Test
    void testIsRememberMe() {
        assertThat(adapter.isRememberMe()).isTrue();
    }

    @Test
    void testGetStarted() {
        assertThat(adapter.getStarted()).isEqualTo(1000);
    }

    @Test
    void testGetLastSessionRefresh() {
        assertThat(adapter.getLastSessionRefresh()).isEqualTo(2000);
    }

    @Test
    void testSetLastSessionRefresh() {
        adapter.setLastSessionRefresh(3000);
        
        assertThat(adapter.getLastSessionRefresh()).isEqualTo(3000);
        verify(transactionManager).enlist(any(KeycloakTransaction.class));
    }

    @Test
    void testIsOffline_False() {
        assertThat(adapter.isOffline()).isFalse();
    }

    @Test
    void testIsOffline_True() {
        RedisUserSessionAdapter offlineAdapter = new RedisUserSessionAdapter(
                session, realm, user, entity, redis, true, 1L
        );
        assertThat(offlineAdapter.isOffline()).isTrue();
    }

    @Test
    void testGetAuthenticatedClientSessions() {
        // Setup: Mock the index and client session data
        String userSessionIndexKey = "userSession:session1";
        List<String> clientSessionKeys = List.of("session1:client1", "session1:client2");
        
        RedisClientSessionEntity clientEntity1 = RedisClientSessionEntity.create("session1", "client1", "realm1");
        RedisClientSessionEntity clientEntity2 = RedisClientSessionEntity.create("session1", "client2", "realm1");
        
        ClientModel client1 = mock(ClientModel.class);
        ClientModel client2 = mock(ClientModel.class);
        when(client1.getId()).thenReturn("client1");
        when(client2.getId()).thenReturn("client2");
        
        when(redis.getSortedSetMembers(eq(RedisConnectionProvider.CLIENT_SESSION_CACHE_NAME), eq(userSessionIndexKey)))
                .thenReturn(clientSessionKeys);
        when(redis.getAll(eq(RedisConnectionProvider.CLIENT_SESSION_CACHE_NAME), eq(clientSessionKeys), eq(RedisClientSessionEntity.class)))
                .thenReturn(Map.of("session1:client1", clientEntity1, "session1:client2", clientEntity2));
        when(realm.getClientById("client1")).thenReturn(client1);
        when(realm.getClientById("client2")).thenReturn(client2);
        
        Map<String, AuthenticatedClientSessionModel> sessions = adapter.getAuthenticatedClientSessions();
        
        assertThat(sessions).hasSize(2);
        assertThat(sessions).containsKey("client1");
        assertThat(sessions).containsKey("client2");
    }

    @Test
    void testGetAuthenticatedClientSessions_EmptyIndex() {
        when(redis.getSortedSetMembers(anyString(), anyString())).thenReturn(Collections.emptyList());
        
        Map<String, AuthenticatedClientSessionModel> sessions = adapter.getAuthenticatedClientSessions();
        
        assertThat(sessions).isEmpty();
    }

    @Test
    void testGetAuthenticatedClientSessions_ClientDeleted() {
        // Test graceful handling when a client no longer exists
        String userSessionIndexKey = "userSession:session1";
        List<String> clientSessionKeys = List.of("session1:deleted-client");
        
        RedisClientSessionEntity clientEntity = RedisClientSessionEntity.create("session1", "deleted-client", "realm1");
        
        when(redis.getSortedSetMembers(eq(RedisConnectionProvider.CLIENT_SESSION_CACHE_NAME), eq(userSessionIndexKey)))
                .thenReturn(clientSessionKeys);
        when(redis.getAll(eq(RedisConnectionProvider.CLIENT_SESSION_CACHE_NAME), eq(clientSessionKeys), eq(RedisClientSessionEntity.class)))
                .thenReturn(Map.of("session1:deleted-client", clientEntity));
        when(realm.getClientById("deleted-client")).thenReturn(null);  // Client was deleted
        
        Map<String, AuthenticatedClientSessionModel> sessions = adapter.getAuthenticatedClientSessions();
        
        assertThat(sessions).isEmpty();  // Gracefully returns empty, no exception
    }

    @Test
    void testGetAuthenticatedClientSessions_Offline() {
        RedisUserSessionAdapter offlineAdapter = new RedisUserSessionAdapter(
                session, realm, user, entity, redis, true, 1L
        );
        
        when(redis.getSortedSetMembers(eq(RedisConnectionProvider.OFFLINE_CLIENT_SESSION_CACHE_NAME), anyString()))
                .thenReturn(Collections.emptyList());
        
        Map<String, AuthenticatedClientSessionModel> sessions = offlineAdapter.getAuthenticatedClientSessions();
        
        assertThat(sessions).isEmpty();
        // Verify it used OFFLINE cache, not regular cache
        verify(redis).getSortedSetMembers(eq(RedisConnectionProvider.OFFLINE_CLIENT_SESSION_CACHE_NAME), anyString());
        verify(redis, never()).getSortedSetMembers(eq(RedisConnectionProvider.CLIENT_SESSION_CACHE_NAME), anyString());
    }

    @Test
    void testGetAuthenticatedClientSessionByClient_NotFound() {
        ClientModel client = mock(ClientModel.class);
        when(realm.getClientById("client1")).thenReturn(client);
        when(redis.get(anyString(), eq("session1:client1"), eq(RedisClientSessionEntity.class)))
                .thenReturn(null);
        
        AuthenticatedClientSessionModel result = adapter.getAuthenticatedClientSessionByClient("client1");
        
        assertThat(result).isNull();
    }

    @Test
    void testGetAuthenticatedClientSessionByClient_ClientNotFound() {
        when(realm.getClientById("client1")).thenReturn(null);
        
        AuthenticatedClientSessionModel result = adapter.getAuthenticatedClientSessionByClient("client1");
        
        assertThat(result).isNull();
    }

    @Test
    void testGetAuthenticatedClientSessionByClient_Found() {
        ClientModel client = mock(ClientModel.class);
        RedisClientSessionEntity clientEntity = RedisClientSessionEntity.create("session1", "client1", "realm1");
        
        when(realm.getClientById("client1")).thenReturn(client);
        when(redis.get(
                eq(RedisConnectionProvider.CLIENT_SESSION_CACHE_NAME),
                eq("session1:client1"),
                eq(RedisClientSessionEntity.class)
        )).thenReturn(clientEntity);
        
        AuthenticatedClientSessionModel result = adapter.getAuthenticatedClientSessionByClient("client1");
        
        assertThat(result).isNotNull();
    }

    @Test
    void testRemoveAuthenticatedClientSessions() {
        List<String> clientIds = List.of("client1", "client2");
        
        adapter.removeAuthenticatedClientSessions(clientIds);
        
        verify(redis).delete(RedisConnectionProvider.CLIENT_SESSION_CACHE_NAME, "session1:client1");
        verify(redis).delete(RedisConnectionProvider.CLIENT_SESSION_CACHE_NAME, "session1:client2");
    }

    @Test
    void testRemoveAuthenticatedClientSessions_Offline() {
        RedisUserSessionAdapter offlineAdapter = new RedisUserSessionAdapter(
                session, realm, user, entity, redis, true, 1L
        );
        
        List<String> clientIds = List.of("client1");
        offlineAdapter.removeAuthenticatedClientSessions(clientIds);
        
        verify(redis).delete(RedisConnectionProvider.OFFLINE_CLIENT_SESSION_CACHE_NAME, "session1:client1");
    }

    @Test
    void testGetNote() {
        entity.setNote("key1", "value1");
        assertThat(adapter.getNote("key1")).isEqualTo("value1");
    }

    @Test
    void testSetNote() {
        adapter.setNote("key1", "value1");
        
        assertThat(entity.getNote("key1")).isEqualTo("value1");
        verify(transactionManager).enlist(any(KeycloakTransaction.class));
    }

    @Test
    void testRemoveNote() {
        entity.setNote("key1", "value1");
        
        adapter.removeNote("key1");
        
        assertThat(entity.getNote("key1")).isNull();
        verify(transactionManager).enlist(any(KeycloakTransaction.class));
    }

    @Test
    void testGetNotes() {
        entity.setNote("key1", "value1");
        entity.setNote("key2", "value2");
        
        Map<String, String> notes = adapter.getNotes();
        
        assertThat(notes).containsEntry("key1", "value1");
        assertThat(notes).containsEntry("key2", "value2");
    }

    @Test
    void testGetNotes_EmptyWhenNull() {
        entity.setNotes(null);
        
        Map<String, String> notes = adapter.getNotes();
        
        assertThat(notes).isNotNull().isEmpty();
    }

    @Test
    void testGetState() {
        assertThat(adapter.getState()).isEqualTo(UserSessionModel.State.LOGGED_IN);
    }

    @Test
    void testSetState() {
        adapter.setState(UserSessionModel.State.LOGGING_OUT);
        
        assertThat(entity.getState()).isEqualTo(UserSessionModel.State.LOGGING_OUT);
        verify(transactionManager).enlist(any(KeycloakTransaction.class));
    }

    @Test
    void testRestartSession() {
        RealmModel newRealm = mock(RealmModel.class);
        UserModel newUser = mock(UserModel.class);
        when(newRealm.getId()).thenReturn("realm2");
        when(newUser.getId()).thenReturn("user2");
        
        adapter.restartSession(newRealm, newUser, "newuser", "10.0.0.1", 
                "oauth", false, "broker2", "brokerUser2");
        
        assertThat(entity.getRealmId()).isEqualTo("realm2");
        assertThat(entity.getUserId()).isEqualTo("user2");
        assertThat(entity.getLoginUsername()).isEqualTo("newuser");
        assertThat(entity.getIpAddress()).isEqualTo("10.0.0.1");
        assertThat(entity.getAuthMethod()).isEqualTo("oauth");
        assertThat(entity.isRememberMe()).isFalse();
        assertThat(entity.getBrokerSessionId()).isEqualTo("broker2");
        assertThat(entity.getBrokerUserId()).isEqualTo("brokerUser2");
        verify(transactionManager).enlist(any(KeycloakTransaction.class));
    }

    @Test
    void testGetPersistenceState() {
        assertThat(adapter.getPersistenceState())
                .isEqualTo(UserSessionModel.SessionPersistenceState.PERSISTENT);
    }

    @Test
    void testPersist_NotModified() {
        adapter.persist();
        
        verify(redis, never()).put(anyString(), anyString(), any(), anyLong(), any(TimeUnit.class));
    }

    @Test
    void testPersist_Modified() {
        adapter.setNote("key", "value"); // Mark as modified
        
        adapter.persist();
        
        verify(redis).put(
                eq(RedisConnectionProvider.USER_SESSION_CACHE_NAME),
                eq("session1"),
                eq(entity),
                eq(3600L),
                eq(TimeUnit.SECONDS)
        );
    }

    @Test
    void testPersist_Offline() {
        RedisUserSessionAdapter offlineAdapter = new RedisUserSessionAdapter(
                session, realm, user, entity, redis, true, 1L
        );
        
        offlineAdapter.setNote("key", "value"); // Mark as modified
        offlineAdapter.persist();
        
        verify(redis).put(
                eq(RedisConnectionProvider.OFFLINE_USER_SESSION_CACHE_NAME),
                eq("session1"),
                eq(entity),
                eq(86400L),
                eq(TimeUnit.SECONDS)
        );
    }

    @Test
    void testMultipleModifications_SingleTransactionEnlistment() {
        adapter.setNote("key1", "value1");
        adapter.setNote("key2", "value2");
        adapter.setState(UserSessionModel.State.LOGGING_OUT);
        
        // Should only enlist once
        verify(transactionManager, times(1)).enlist(any(KeycloakTransaction.class));
    }

    // ===================== persist() deletion tracking tests =====================

    @Test
    void testPersist_SessionMarkedAsDeleted() {
        RedisUserSessionProvider provider = mock(RedisUserSessionProvider.class);
        when(session.sessions()).thenReturn(provider);
        when(provider.isUserSessionDeleted("session1")).thenReturn(true);
        
        adapter.setNote("key", "value"); // Mark as modified
        adapter.persist();
        
        // Should NOT persist because session was marked as deleted
        verify(redis, never()).put(anyString(), anyString(), any(), anyLong(), any(TimeUnit.class));
    }

    @Test
    void testPersist_SessionNotDeleted() {
        RedisUserSessionProvider provider = mock(RedisUserSessionProvider.class);
        when(session.sessions()).thenReturn(provider);
        when(provider.isUserSessionDeleted("session1")).thenReturn(false);
        
        adapter.setNote("key", "value"); // Mark as modified
        adapter.persist();
        
        // Should persist because session is not deleted
        verify(redis).put(
                eq(RedisConnectionProvider.USER_SESSION_CACHE_NAME),
                eq("session1"),
                eq(entity),
                eq(3600L),
                eq(TimeUnit.SECONDS)
        );
    }

    @Test
    void testPersist_ProviderNull_ShouldStillPersist() {
        // session.sessions() returns null (e.g., in some test scenarios)
        when(session.sessions()).thenReturn(null);

        adapter.setNote("key", "value"); // Mark as modified
        adapter.persist();

        // Should persist because null provider should not block persist
        verify(redis).put(
                eq(RedisConnectionProvider.USER_SESSION_CACHE_NAME),
                eq("session1"),
                eq(entity),
                eq(3600L),
                eq(TimeUnit.SECONDS)
        );
    }

    // ===================== Transaction integration tests =====================

    @Test
    void testMarkModified_EnlistsTransactionOnce() {
        // Multiple modifications should only enlist transaction once
        adapter.setNote("key1", "value1");
        adapter.setNote("key2", "value2");
        adapter.setLastSessionRefresh(3000);
        adapter.setState(UserSessionModel.State.LOGGING_OUT);

        // Verify transaction was enlisted only once (batched updates)
        verify(transactionManager, times(1)).enlist(any());
    }

    @Test
    void testMarkModified_UsesEnlistNotEnlistAfterCompletion() {
        // This is important - enlist() runs during transaction, enlistAfterCompletion() runs after
        adapter.setNote("key", "value");

        // Verify it uses enlist() not enlistAfterCompletion()
        verify(transactionManager).enlist(any());
        verify(transactionManager, never()).enlistAfterCompletion(any());
    }

    @Test
    void testTransactionCommit_CallsPersist() {
        KeycloakTransactionManager txManager = mock(KeycloakTransactionManager.class);
        when(session.getTransactionManager()).thenReturn(txManager);

        // Capture the transaction that gets enlisted
        org.mockito.ArgumentCaptor<org.keycloak.models.KeycloakTransaction> txCaptor =
                org.mockito.ArgumentCaptor.forClass(org.keycloak.models.KeycloakTransaction.class);

        // Mark as modified - this should enlist a transaction
        adapter.setNote("key", "value");

        verify(txManager).enlist(txCaptor.capture());

        // Now commit the captured transaction
        org.keycloak.models.KeycloakTransaction tx = txCaptor.getValue();
        tx.commit();

        // Verify persist was called
        verify(redis).put(
                eq(RedisConnectionProvider.USER_SESSION_CACHE_NAME),
                eq("session1"),
                eq(entity),
                eq(3600L),
                eq(TimeUnit.SECONDS)
        );
    }

    @Test
    void testTransactionRollback_ResetsModifiedFlag() {
        KeycloakTransactionManager txManager = mock(KeycloakTransactionManager.class);
        when(session.getTransactionManager()).thenReturn(txManager);

        // Capture the transaction
        org.mockito.ArgumentCaptor<org.keycloak.models.KeycloakTransaction> txCaptor =
                org.mockito.ArgumentCaptor.forClass(org.keycloak.models.KeycloakTransaction.class);

        adapter.setNote("key", "value");
        verify(txManager).enlist(txCaptor.capture());

        org.keycloak.models.KeycloakTransaction tx = txCaptor.getValue();

        // Transaction should be active before rollback
        assertThat(tx.isActive()).isTrue();

        // Rollback
        tx.rollback();

        // Transaction should no longer be active
        assertThat(tx.isActive()).isFalse();

        // Persist should not be called after rollback
        adapter.persist();
        verify(redis, never()).put(anyString(), anyString(), any(), anyLong(), any(TimeUnit.class));
    }

    @Test
    void testTransactionIsActive_ReflectsModifiedState() {
        KeycloakTransactionManager txManager = mock(KeycloakTransactionManager.class);
        when(session.getTransactionManager()).thenReturn(txManager);

        org.mockito.ArgumentCaptor<org.keycloak.models.KeycloakTransaction> txCaptor =
                org.mockito.ArgumentCaptor.forClass(org.keycloak.models.KeycloakTransaction.class);

        adapter.setNote("key", "value");
        verify(txManager).enlist(txCaptor.capture());

        org.keycloak.models.KeycloakTransaction tx = txCaptor.getValue();

        // Should be active when modified
        assertThat(tx.isActive()).isTrue();

        // After commit, persist() clears the modified flag
        tx.commit();

        // Should no longer be active after persist completes
        assertThat(tx.isActive()).isFalse();
    }

    @Test
    void testTransactionGetRollbackOnly_AlwaysReturnsFalse() {
        KeycloakTransactionManager txManager = mock(KeycloakTransactionManager.class);
        when(session.getTransactionManager()).thenReturn(txManager);

        org.mockito.ArgumentCaptor<org.keycloak.models.KeycloakTransaction> txCaptor =
                org.mockito.ArgumentCaptor.forClass(org.keycloak.models.KeycloakTransaction.class);

        adapter.setNote("key", "value");
        verify(txManager).enlist(txCaptor.capture());

        org.keycloak.models.KeycloakTransaction tx = txCaptor.getValue();

        assertThat(tx.getRollbackOnly()).isFalse();
        tx.setRollbackOnly();
        assertThat(tx.getRollbackOnly()).isFalse(); // Still false
    }

    @Test
    void testTransactionBegin_DoesNothing() {
        KeycloakTransactionManager txManager = mock(KeycloakTransactionManager.class);
        when(session.getTransactionManager()).thenReturn(txManager);

        org.mockito.ArgumentCaptor<org.keycloak.models.KeycloakTransaction> txCaptor =
                org.mockito.ArgumentCaptor.forClass(org.keycloak.models.KeycloakTransaction.class);

        adapter.setNote("key", "value");
        verify(txManager).enlist(txCaptor.capture());

        org.keycloak.models.KeycloakTransaction tx = txCaptor.getValue();

        // begin() should not throw and have no side effects
        assertThatCode(tx::begin).doesNotThrowAnyException();
        verify(redis, never()).put(anyString(), anyString(), any(), anyLong(), any(TimeUnit.class));
    }
}
