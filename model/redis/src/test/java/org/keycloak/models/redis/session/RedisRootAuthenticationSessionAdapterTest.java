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
import org.keycloak.common.util.Time;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.KeycloakTransaction;
import org.keycloak.models.KeycloakTransactionManager;
import org.keycloak.models.redis.RedisConnectionProvider;
import org.keycloak.models.redis.entities.RedisAuthenticationSessionEntity;
import org.keycloak.models.redis.session.RedisRootAuthenticationSessionAdapter;
import org.keycloak.sessions.AuthenticationSessionModel;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.keycloak.models.redis.TestConstants.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RedisRootAuthenticationSessionAdapter.
 * Tests verify deferred write pattern where changes are batched and persisted at transaction commit.
 */
class RedisRootAuthenticationSessionAdapterTest {

    private KeycloakSession session;
    private KeycloakTransactionManager transactionManager;
    private RealmModel realm;
    private RedisAuthenticationSessionEntity entity;
    private RedisConnectionProvider redis;
    private RedisRootAuthenticationSessionAdapter adapter;
    private ClientModel client;

    @BeforeEach
    void setUp() {
        session = mock(KeycloakSession.class);
        realm = mock(RealmModel.class);
        transactionManager = mock(KeycloakTransactionManager.class);
        entity = new RedisAuthenticationSessionEntity();
        redis = mock(RedisConnectionProvider.class);
        client = mock(ClientModel.class);

        entity.setId(CUSTOM_SESSION_ID);
        entity.setRealmId(TEST_REALM_ID);
        entity.setTimestamp(Time.currentTime());

        when(session.getTransactionManager()).thenReturn(transactionManager);
        when(realm.getId()).thenReturn(TEST_REALM_ID);
        when(client.getId()).thenReturn(TEST_CLIENT_ID);

        adapter = new RedisRootAuthenticationSessionAdapter(session, realm, entity, redis, AUTH_SESSION_TTL);
    }

    @Test
    void testGetId() {
        assertThat(adapter.getId()).isEqualTo(CUSTOM_SESSION_ID);
    }

    @Test
    void testGetRealm() {
        assertThat(adapter.getRealm()).isEqualTo(realm);
    }

    @Test
    void testGetTimestamp() {
        int timestamp = adapter.getTimestamp();
        assertThat(timestamp).isPositive();
    }

    @Test
    void testSetTimestamp() {
        int newTimestamp = Time.currentTime() + 100;
        
        adapter.setTimestamp(newTimestamp);
        
        assertThat(entity.getTimestamp()).isEqualTo(newTimestamp);
        // With deferred writes, should register transaction callback instead of immediate write
        verify(transactionManager).enlistAfterCompletion(any(KeycloakTransaction.class));
        verify(redis, never()).put(anyString(), anyString(), any(), anyLong(), any());
    }

    @Test
    void testGetAuthenticationSessions_Empty() {
        Map<String, AuthenticationSessionModel> sessions = adapter.getAuthenticationSessions();
        
        assertThat(sessions).isEmpty();
    }

    @Test
    void testCreateAuthenticationSession() {
        when(realm.getClientById(TEST_CLIENT_ID)).thenReturn(client);

        AuthenticationSessionModel authSession = adapter.createAuthenticationSession(client);

        assertThat(authSession).isNotNull();
        assertThat(authSession.getClient()).isEqualTo(client);
        assertThat(authSession.getTabId()).isNotNull();

        // With deferred writes, should register transaction callback
        verify(transactionManager).enlistAfterCompletion(any(KeycloakTransaction.class));
        verify(redis, never()).put(anyString(), anyString(), any(), anyLong(), any());
    }

    @Test
    void testGetAuthenticationSession_Found() {
        // Create a tab first
        RedisAuthenticationSessionEntity.RedisAuthenticationTabEntity tab =
                new RedisAuthenticationSessionEntity.RedisAuthenticationTabEntity(TAB_ID_1, TEST_CLIENT_ID);
        entity.setAuthenticationSession(TAB_ID_1, tab);

        AuthenticationSessionModel session = adapter.getAuthenticationSession(client, TAB_ID_1);

        assertThat(session).isNotNull();
        assertThat(session.getTabId()).isEqualTo(TAB_ID_1);
        assertThat(session.getClient()).isEqualTo(client);
    }

    @Test
    void testGetAuthenticationSession_NotFound() {
        AuthenticationSessionModel session = adapter.getAuthenticationSession(client, TAB_ID_1);

        assertThat(session).isNull();
    }

    @Test
    void testGetAuthenticationSession_NullClient() {
        AuthenticationSessionModel session = adapter.getAuthenticationSession(null, TAB_ID_1);

        assertThat(session).isNull();
    }

    @Test
    void testGetAuthenticationSession_NullTabId() {
        AuthenticationSessionModel session = adapter.getAuthenticationSession(client, null);

        assertThat(session).isNull();
    }

    @Test
    void testGetAuthenticationSession_WrongClient() {
        RedisAuthenticationSessionEntity.RedisAuthenticationTabEntity tab =
                new RedisAuthenticationSessionEntity.RedisAuthenticationTabEntity(TAB_ID_1, TEST_CLIENT_ID);
        entity.setAuthenticationSession(TAB_ID_1, tab);

        ClientModel differentClient = mock(ClientModel.class);
        when(differentClient.getId()).thenReturn(TEST_CLIENT_ID_2);

        AuthenticationSessionModel session = adapter.getAuthenticationSession(differentClient, TAB_ID_1);

        assertThat(session).isNull();
    }

    @Test
    void testRemoveAuthenticationSessionByTabId() {
        RedisAuthenticationSessionEntity.RedisAuthenticationTabEntity tab =
                new RedisAuthenticationSessionEntity.RedisAuthenticationTabEntity(TAB_ID_1, TEST_CLIENT_ID);
        entity.setAuthenticationSession(TAB_ID_1, tab);

        adapter.removeAuthenticationSessionByTabId(TAB_ID_1);

        assertThat(entity.getAuthenticationSession(TAB_ID_1)).isNull();
        // With deferred writes, should register transaction callback
        verify(transactionManager).enlistAfterCompletion(any(KeycloakTransaction.class));
        verify(redis, never()).put(anyString(), anyString(), any(), anyLong(), any());
    }

    @Test
    void testRestartSession() {
        // Add some authentication sessions
        RedisAuthenticationSessionEntity.RedisAuthenticationTabEntity tab1 =
                new RedisAuthenticationSessionEntity.RedisAuthenticationTabEntity(TAB_ID_1, TEST_CLIENT_ID);
        RedisAuthenticationSessionEntity.RedisAuthenticationTabEntity tab2 =
                new RedisAuthenticationSessionEntity.RedisAuthenticationTabEntity(TAB_ID_2, TEST_CLIENT_ID_2);
        entity.setAuthenticationSession(TAB_ID_1, tab1);
        entity.setAuthenticationSession(TAB_ID_2, tab2);

        int oldTimestamp = entity.getTimestamp();

        adapter.restartSession(realm);

        assertThat(entity.getAuthenticationSessions()).isEmpty();
        assertThat(entity.getTimestamp()).isGreaterThanOrEqualTo(oldTimestamp);

        // With deferred writes, should register transaction callback
        verify(transactionManager).enlistAfterCompletion(any(KeycloakTransaction.class));
        verify(redis, never()).put(anyString(), anyString(), any(), anyLong(), any());
    }

    @Test
    void testGetAuthenticationSessions_WithMultipleSessions() {
        // Add multiple tabs
        RedisAuthenticationSessionEntity.RedisAuthenticationTabEntity tab1 =
                new RedisAuthenticationSessionEntity.RedisAuthenticationTabEntity(TAB_ID_1, TEST_CLIENT_ID);
        RedisAuthenticationSessionEntity.RedisAuthenticationTabEntity tab2 =
                new RedisAuthenticationSessionEntity.RedisAuthenticationTabEntity(TAB_ID_2, TEST_CLIENT_ID_2);
        entity.setAuthenticationSession(TAB_ID_1, tab1);
        entity.setAuthenticationSession(TAB_ID_2, tab2);

        ClientModel client2 = mock(ClientModel.class);
        when(client2.getId()).thenReturn(TEST_CLIENT_ID_2);
        
        when(realm.getClientById(TEST_CLIENT_ID)).thenReturn(client);
        when(realm.getClientById(TEST_CLIENT_ID_2)).thenReturn(client2);

        Map<String, AuthenticationSessionModel> sessions = adapter.getAuthenticationSessions();

        assertThat(sessions).hasSize(2);
        assertThat(sessions).containsKey(TAB_ID_1);
        assertThat(sessions).containsKey(TAB_ID_2);
    }

    @Test
    void testGetAuthenticationSessions_ClientNotFound() {
        // Add a tab
        RedisAuthenticationSessionEntity.RedisAuthenticationTabEntity tab =
                new RedisAuthenticationSessionEntity.RedisAuthenticationTabEntity(TAB_ID_1, TEST_CLIENT_ID);
        entity.setAuthenticationSession(TAB_ID_1, tab);

        // Client not found in realm
        when(realm.getClientById(TEST_CLIENT_ID)).thenReturn(null);

        Map<String, AuthenticationSessionModel> sessions = adapter.getAuthenticationSessions();

        // Should skip tabs with missing clients
        assertThat(sessions).isEmpty();
    }

    @Test
    void testCreateMultipleAuthenticationSessions() {
        ClientModel client2 = mock(ClientModel.class);
        when(client2.getId()).thenReturn(TEST_CLIENT_ID_2);

        AuthenticationSessionModel session1 = adapter.createAuthenticationSession(client);
        AuthenticationSessionModel session2 = adapter.createAuthenticationSession(client2);

        assertThat(session1).isNotNull();
        assertThat(session2).isNotNull();
        assertThat(session1.getTabId()).isNotEqualTo(session2.getTabId());

        // With deferred writes, should register transaction callback only once (batched)
        verify(transactionManager, times(1)).enlistAfterCompletion(any(KeycloakTransaction.class));
        verify(redis, never()).put(anyString(), anyString(), any(), anyLong(), any());
    }

    // ===================== Transaction integration tests =====================

    @Test
    void testMarkModified_EnlistsTransactionOnce() {
        // Multiple modifications should only enlist transaction once
        adapter.setTimestamp(5000);
        adapter.createAuthenticationSession(client);

        RedisAuthenticationSessionEntity.RedisAuthenticationTabEntity tab =
                new RedisAuthenticationSessionEntity.RedisAuthenticationTabEntity(TAB_ID_1, TEST_CLIENT_ID);
        entity.setAuthenticationSession(TAB_ID_1, tab);
        adapter.removeAuthenticationSessionByTabId(TAB_ID_1);

        // Verify transaction was enlisted only once (batched updates)
        verify(transactionManager, times(1)).enlistAfterCompletion(any());
    }

    @Test
    void testMarkModified_UsesEnlistAfterCompletion() {
        // RootAuthenticationSession uses enlistAfterCompletion (unlike Client/UserSession which use enlist)
        adapter.setTimestamp(5000);

        // Verify it uses enlistAfterCompletion()
        verify(transactionManager).enlistAfterCompletion(any());
        verify(transactionManager, never()).enlist(any());
    }

    @Test
    void testTransactionCommit_CallsPersist() {
        KeycloakTransactionManager txManager = mock(KeycloakTransactionManager.class);
        when(session.getTransactionManager()).thenReturn(txManager);

        // Capture the transaction that gets enlisted
        org.mockito.ArgumentCaptor<KeycloakTransaction> txCaptor =
                org.mockito.ArgumentCaptor.forClass(KeycloakTransaction.class);

        // Mark as modified - this should enlist a transaction
        adapter.setTimestamp(5000);

        verify(txManager).enlistAfterCompletion(txCaptor.capture());

        // Now commit the captured transaction
        KeycloakTransaction tx = txCaptor.getValue();
        tx.commit();

        // Verify persist was called
        verify(redis).put(
                eq(RedisConnectionProvider.CACHE_AUTHENTICATION_SESSIONS),
                eq(entity.getId()),
                eq(entity),
                eq((long) AUTH_SESSION_TTL),
                eq(TimeUnit.SECONDS)
        );
    }

    @Test
    void testTransactionRollback_ResetsModifiedFlag() {
        KeycloakTransactionManager txManager = mock(KeycloakTransactionManager.class);
        when(session.getTransactionManager()).thenReturn(txManager);

        // Capture the transaction
        org.mockito.ArgumentCaptor<KeycloakTransaction> txCaptor =
                org.mockito.ArgumentCaptor.forClass(KeycloakTransaction.class);

        adapter.setTimestamp(5000);
        verify(txManager).enlistAfterCompletion(txCaptor.capture());

        KeycloakTransaction tx = txCaptor.getValue();

        // Transaction should be active before rollback
        assertThat(tx.isActive()).isTrue();

        // Rollback
        tx.rollback();

        // Transaction should no longer be active
        assertThat(tx.isActive()).isFalse();

        // Verify persist is NOT called after rollback
        verify(redis, never()).put(anyString(), anyString(), any(), anyLong(), any());
    }

    @Test
    void testTransactionIsActive_ReflectsModifiedState() {
        KeycloakTransactionManager txManager = mock(KeycloakTransactionManager.class);
        when(session.getTransactionManager()).thenReturn(txManager);

        org.mockito.ArgumentCaptor<KeycloakTransaction> txCaptor =
                org.mockito.ArgumentCaptor.forClass(KeycloakTransaction.class);

        adapter.setTimestamp(5000);
        verify(txManager).enlistAfterCompletion(txCaptor.capture());

        KeycloakTransaction tx = txCaptor.getValue();

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

        org.mockito.ArgumentCaptor<KeycloakTransaction> txCaptor =
                org.mockito.ArgumentCaptor.forClass(KeycloakTransaction.class);

        adapter.setTimestamp(5000);
        verify(txManager).enlistAfterCompletion(txCaptor.capture());

        KeycloakTransaction tx = txCaptor.getValue();

        assertThat(tx.getRollbackOnly()).isFalse();
        tx.setRollbackOnly();
        assertThat(tx.getRollbackOnly()).isFalse(); // Still false
    }

    @Test
    void testTransactionBegin_DoesNothing() {
        KeycloakTransactionManager txManager = mock(KeycloakTransactionManager.class);
        when(session.getTransactionManager()).thenReturn(txManager);

        org.mockito.ArgumentCaptor<KeycloakTransaction> txCaptor =
                org.mockito.ArgumentCaptor.forClass(KeycloakTransaction.class);

        adapter.setTimestamp(5000);
        verify(txManager).enlistAfterCompletion(txCaptor.capture());

        KeycloakTransaction tx = txCaptor.getValue();

        // begin() should not throw and have no side effects
        assertThatCode(tx::begin).doesNotThrowAnyException();
        verify(redis, never()).put(anyString(), anyString(), any(), anyLong(), any());
    }
}
