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
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakTransactionManager;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.redis.RedisConnectionProvider;
import org.keycloak.models.redis.entities.RedisClientSessionEntity;
import org.keycloak.models.redis.session.RedisClientSessionAdapter;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.keycloak.models.redis.TestConstants.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RedisClientSessionAdapter.
 */
class RedisClientSessionAdapterTest {

    private KeycloakSession session;
    private RealmModel realm;
    private ClientModel client;
    private UserSessionModel userSession;
    private RedisClientSessionEntity entity;
    private RedisConnectionProvider redis;
    private RedisClientSessionAdapter adapter;

    @BeforeEach
    void setUp() {
        session = mock(KeycloakSession.class);
        realm = mock(RealmModel.class);
        client = mock(ClientModel.class);
        userSession = mock(UserSessionModel.class);
        entity = RedisClientSessionEntity.create(TEST_SESSION_ID, TEST_CLIENT_ID, TEST_REALM_ID);
        redis = mock(RedisConnectionProvider.class);

        // Mock transaction manager to avoid NullPointerException when markModified() is called
        KeycloakTransactionManager transactionManager = mock(KeycloakTransactionManager.class);
        when(session.getTransactionManager()).thenReturn(transactionManager);

        when(realm.getId()).thenReturn(TEST_REALM_ID);
        when(client.getId()).thenReturn(TEST_CLIENT_ID);
        when(userSession.getId()).thenReturn(TEST_SESSION_ID);

        adapter = new RedisClientSessionAdapter(session, realm, client, userSession, entity, redis, false, 1L);
    }

    @Test
    void testGetId() {
        String id = adapter.getId();
        assertThat(id).isEqualTo(TEST_SESSION_ID + ":" + TEST_CLIENT_ID);
    }

    @Test
    void testGetTimestamp() {
        int timestamp = adapter.getTimestamp();
        assertThat(timestamp).isPositive();
    }

    @Test
    void testSetTimestamp() {
        int newTimestamp = 12345;
        adapter.setTimestamp(newTimestamp);
        
        assertThat(entity.getTimestamp()).isEqualTo(newTimestamp);
    }

    @Test
    void testGetUserSession() {
        assertThat(adapter.getUserSession()).isEqualTo(userSession);
    }

    @Test
    void testGetRealm() {
        assertThat(adapter.getRealm()).isEqualTo(realm);
    }

    @Test
    void testGetClient() {
        assertThat(adapter.getClient()).isEqualTo(client);
    }

    @Test
    void testCurrentRefreshToken() {
        String token = "refresh-token-123";
        adapter.setCurrentRefreshToken(token);
        
        assertThat(adapter.getCurrentRefreshToken()).isEqualTo(token);
        assertThat(entity.getCurrentRefreshToken()).isEqualTo(token);
    }

    @Test
    void testCurrentRefreshTokenUseCount() {
        adapter.setCurrentRefreshTokenUseCount(5);
        
        assertThat(adapter.getCurrentRefreshTokenUseCount()).isEqualTo(5);
        assertThat(entity.getCurrentRefreshTokenUseCount()).isEqualTo(5);
    }

    @Test
    void testNotes() {
        adapter.setNote(NOTE_KEY_1, NOTE_VALUE_1);
        assertThat(adapter.getNote(NOTE_KEY_1)).isEqualTo(NOTE_VALUE_1);

        Map<String, String> notes = adapter.getNotes();
        assertThat(notes).containsEntry(NOTE_KEY_1, NOTE_VALUE_1);
    }

    @Test
    void testRemoveNote() {
        adapter.setNote(NOTE_KEY_1, NOTE_VALUE_1);
        adapter.removeNote(NOTE_KEY_1);
        
        assertThat(adapter.getNote(NOTE_KEY_1)).isNull();
    }

    @Test
    void testGetNotes_EmptyWhenNull() {
        entity.setNotes(null);
        Map<String, String> notes = adapter.getNotes();
        
        assertThat(notes).isNotNull().isEmpty();
    }

    @Test
    void testRedirectUri() {
        adapter.setRedirectUri(REDIRECT_URI);
        
        assertThat(adapter.getRedirectUri()).isEqualTo(REDIRECT_URI);
        assertThat(entity.getRedirectUri()).isEqualTo(REDIRECT_URI);
    }

    @Test
    void testAction() {
        adapter.setAction(ACTION_VERIFY_EMAIL);
        
        assertThat(adapter.getAction()).isEqualTo(ACTION_VERIFY_EMAIL);
        assertThat(entity.getAction()).isEqualTo(ACTION_VERIFY_EMAIL);
    }

    @Test
    void testProtocol() {
        adapter.setProtocol(PROTOCOL_OPENID_CONNECT);
        
        assertThat(adapter.getProtocol()).isEqualTo(PROTOCOL_OPENID_CONNECT);
        assertThat(entity.getAuthMethod()).isEqualTo(PROTOCOL_OPENID_CONNECT);
    }

    @Test
    void testDetachFromUserSession() {
        adapter.detachFromUserSession();
        
        verify(redis).delete(
                eq(RedisConnectionProvider.CLIENT_SESSION_CACHE_NAME),
                eq(entity.getKey())
        );
    }

    @Test
    void testDetachFromUserSession_Offline() {
        adapter = new RedisClientSessionAdapter(session, realm, client, userSession, entity, redis, true, 1L);
        
        adapter.detachFromUserSession();
        
        verify(redis).delete(
                eq(RedisConnectionProvider.OFFLINE_CLIENT_SESSION_CACHE_NAME),
                eq(entity.getKey())
        );
    }

    @Test
    void testConstructor_NullParametersThrow() {
        assertThatThrownBy(() -> 
            new RedisClientSessionAdapter(null, realm, client, userSession, entity, redis, false, 1L))
            .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> 
            new RedisClientSessionAdapter(session, null, client, userSession, entity, redis, false, 1L))
            .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> 
            new RedisClientSessionAdapter(session, realm, null, userSession, entity, redis, false, 1L))
            .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> 
            new RedisClientSessionAdapter(session, realm, client, null, entity, redis, false, 1L))
            .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> 
            new RedisClientSessionAdapter(session, realm, client, userSession, null, redis, false, 1L))
            .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> 
            new RedisClientSessionAdapter(session, realm, client, userSession, entity, null, false, 1L))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void testMultipleNoteOperations() {
        adapter.setNote(NOTE_KEY_1, NOTE_VALUE_1);
        adapter.setNote(NOTE_KEY_2, NOTE_VALUE_2);
        
        Map<String, String> notes = adapter.getNotes();
        assertThat(notes).hasSize(2);
        assertThat(notes).containsEntry(NOTE_KEY_1, NOTE_VALUE_1);
        assertThat(notes).containsEntry(NOTE_KEY_2, NOTE_VALUE_2);

        adapter.removeNote(NOTE_KEY_1);
        assertThat(adapter.getNote(NOTE_KEY_1)).isNull();
        assertThat(adapter.getNote(NOTE_KEY_2)).isEqualTo(NOTE_VALUE_2);
    }

    @Test
    void testSetNote_OverwritesExisting() {
        adapter.setNote(NOTE_KEY_1, NOTE_VALUE_1);
        adapter.setNote(NOTE_KEY_1, NOTE_VALUE_2);
        
        assertThat(adapter.getNote(NOTE_KEY_1)).isEqualTo(NOTE_VALUE_2);
    }

    @Test
    void testGetNotesReturnsNewMap() {
        adapter.setNote(NOTE_KEY_1, NOTE_VALUE_1);
        
        Map<String, String> notes1 = adapter.getNotes();
        Map<String, String> notes2 = adapter.getNotes();
        
        // Should return new instances
        assertThat(notes1).isNotSameAs(notes2);
        assertThat(notes1).isEqualTo(notes2);
    }

    @Test
    void testOfflineClientSession() {
        RedisClientSessionAdapter offlineAdapter = new RedisClientSessionAdapter(
                session, realm, client, userSession, entity, redis, true, 1L);
        
        assertThat(offlineAdapter).isNotNull();
        
        // Test that detach uses offline cache
        offlineAdapter.detachFromUserSession();
        verify(redis).delete(
                eq(RedisConnectionProvider.OFFLINE_CLIENT_SESSION_CACHE_NAME),
                anyString()
        );
    }

    @Test
    void testRefreshTokenUseCount_Incremental() {
        adapter.setCurrentRefreshTokenUseCount(0);
        assertThat(adapter.getCurrentRefreshTokenUseCount()).isEqualTo(0);
        
        adapter.setCurrentRefreshTokenUseCount(1);
        assertThat(adapter.getCurrentRefreshTokenUseCount()).isEqualTo(1);
        
        adapter.setCurrentRefreshTokenUseCount(5);
        assertThat(adapter.getCurrentRefreshTokenUseCount()).isEqualTo(5);
    }

    @Test
    void testTimestamp_UpdateMultipleTimes() {
        int timestamp1 = 1000;
        int timestamp2 = 2000;
        int timestamp3 = 3000;

        adapter.setTimestamp(timestamp1);
        assertThat(adapter.getTimestamp()).isEqualTo(timestamp1);

        adapter.setTimestamp(timestamp2);
        assertThat(adapter.getTimestamp()).isEqualTo(timestamp2);

        adapter.setTimestamp(timestamp3);
        assertThat(adapter.getTimestamp()).isEqualTo(timestamp3);
    }

    // ===================== persist() tests =====================

    @Test
    void testPersist_NotModified() {
        // When not modified, persist should do nothing
        adapter.persist();
        
        verify(redis, never()).put(anyString(), anyString(), any(), anyLong(), any());
    }

    @Test
    void testPersist_Modified_ParentExists() {
        when(realm.getSsoSessionMaxLifespan()).thenReturn(3600);
        when(redis.containsKey(RedisConnectionProvider.USER_SESSION_CACHE_NAME, TEST_SESSION_ID)).thenReturn(true);
        
        adapter.setNote("key", "value"); // Mark as modified
        adapter.persist();
        
        verify(redis).put(
                eq(RedisConnectionProvider.CLIENT_SESSION_CACHE_NAME),
                eq(entity.getKey()),
                eq(entity),
                eq(3600L),
                any()
        );
    }

    @Test
    void testPersist_Modified_ParentDeleted() {
        when(redis.containsKey(RedisConnectionProvider.USER_SESSION_CACHE_NAME, TEST_SESSION_ID)).thenReturn(false);
        
        adapter.setNote("key", "value"); // Mark as modified
        adapter.persist();
        
        // Should NOT persist because parent user session doesn't exist
        verify(redis, never()).put(anyString(), anyString(), any(), anyLong(), any());
    }

    @Test
    void testPersist_Detached() {
        adapter.setNote("key", "value"); // Mark as modified
        adapter.detachFromUserSession(); // Detach the session
        
        adapter.persist();
        
        // Should NOT persist because session is detached
        verify(redis, never()).put(anyString(), anyString(), any(), anyLong(), any());
    }

    @Test
    void testPersist_Offline_ParentExists() {
        RedisClientSessionAdapter offlineAdapter = new RedisClientSessionAdapter(
                session, realm, client, userSession, entity, redis, true, 1L);
        
        when(realm.getOfflineSessionIdleTimeout()).thenReturn(86400);
        when(redis.containsKey(RedisConnectionProvider.OFFLINE_USER_SESSION_CACHE_NAME, TEST_SESSION_ID)).thenReturn(true);
        
        offlineAdapter.setNote("key", "value"); // Mark as modified
        offlineAdapter.persist();
        
        verify(redis).put(
                eq(RedisConnectionProvider.OFFLINE_CLIENT_SESSION_CACHE_NAME),
                eq(entity.getKey()),
                eq(entity),
                eq(86400L),
                any()
        );
    }

    @Test
    void testPersist_Offline_ParentDeleted() {
        RedisClientSessionAdapter offlineAdapter = new RedisClientSessionAdapter(
                session, realm, client, userSession, entity, redis, true, 1L);

        when(redis.containsKey(RedisConnectionProvider.OFFLINE_USER_SESSION_CACHE_NAME, TEST_SESSION_ID)).thenReturn(false);

        offlineAdapter.setNote("key", "value"); // Mark as modified
        offlineAdapter.persist();

        // Should NOT persist because parent offline user session doesn't exist
        verify(redis, never()).put(anyString(), anyString(), any(), anyLong(), any());
    }

    // ===================== Transaction integration tests =====================

    @Test
    void testMarkModified_EnlistsTransactionOnce() {
        // Multiple modifications should only enlist transaction once
        adapter.setNote("key1", "value1");
        adapter.setNote("key2", "value2");
        adapter.setTimestamp(5000);

        // Verify transaction was enlisted only once (batched updates)
        verify(session.getTransactionManager(), times(1)).enlist(any());
    }

    @Test
    void testMarkModified_UsesEnlistNotEnlistAfterCompletion() {
        // This is important - enlist() runs during transaction, enlistAfterCompletion() runs after
        adapter.setNote("key", "value");

        // Verify it uses enlist() not enlistAfterCompletion()
        verify(session.getTransactionManager()).enlist(any());
        verify(session.getTransactionManager(), never()).enlistAfterCompletion(any());
    }

    @Test
    void testTransactionCommit_CallsPersist() {
        KeycloakTransactionManager txManager = mock(KeycloakTransactionManager.class);
        when(session.getTransactionManager()).thenReturn(txManager);

        // Capture the transaction that gets enlisted
        org.mockito.ArgumentCaptor<org.keycloak.models.KeycloakTransaction> txCaptor =
                org.mockito.ArgumentCaptor.forClass(org.keycloak.models.KeycloakTransaction.class);

        when(realm.getSsoSessionMaxLifespan()).thenReturn(3600);
        when(redis.containsKey(RedisConnectionProvider.USER_SESSION_CACHE_NAME, TEST_SESSION_ID)).thenReturn(true);

        // Mark as modified - this should enlist a transaction
        adapter.setNote("key", "value");

        verify(txManager).enlist(txCaptor.capture());

        // Now commit the captured transaction
        org.keycloak.models.KeycloakTransaction tx = txCaptor.getValue();
        tx.commit();

        // Verify persist was called
        verify(redis).put(
                eq(RedisConnectionProvider.CLIENT_SESSION_CACHE_NAME),
                eq(entity.getKey()),
                eq(entity),
                eq(3600L),
                any()
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
        verify(redis, never()).put(anyString(), anyString(), any(), anyLong(), any());
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
        when(realm.getSsoSessionMaxLifespan()).thenReturn(3600);
        when(redis.containsKey(RedisConnectionProvider.USER_SESSION_CACHE_NAME, TEST_SESSION_ID)).thenReturn(true);
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
}
