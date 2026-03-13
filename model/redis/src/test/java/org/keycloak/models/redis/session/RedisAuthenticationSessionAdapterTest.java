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
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.models.redis.entities.RedisAuthenticationSessionEntity;
import org.keycloak.models.redis.session.RedisAuthenticationSessionAdapter;
import org.keycloak.models.redis.session.RedisRootAuthenticationSessionAdapter;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.keycloak.models.redis.TestConstants.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RedisAuthenticationSessionAdapter.
 */
class RedisAuthenticationSessionAdapterTest {

    private KeycloakSession session;
    private RedisRootAuthenticationSessionAdapter parent;
    private ClientModel client;
    private RedisAuthenticationSessionEntity.RedisAuthenticationTabEntity entity;
    private RedisAuthenticationSessionAdapter adapter;
    private RealmModel realm;
    private UserProvider userProvider;

    @BeforeEach
    void setUp() {
        session = mock(KeycloakSession.class);
        parent = mock(RedisRootAuthenticationSessionAdapter.class);
        client = mock(ClientModel.class);
        realm = mock(RealmModel.class);
        userProvider = mock(UserProvider.class);

        entity = new RedisAuthenticationSessionEntity.RedisAuthenticationTabEntity();
        entity.setTabId(TAB_ID_1);

        when(session.users()).thenReturn(userProvider);
        when(parent.getRealm()).thenReturn(realm);
        when(realm.getId()).thenReturn(TEST_REALM_ID);
        when(client.getId()).thenReturn(TEST_CLIENT_ID);

        adapter = new RedisAuthenticationSessionAdapter(session, parent, client, entity);
    }

    @Test
    void testGetTabId() {
        assertThat(adapter.getTabId()).isEqualTo(TAB_ID_1);
    }

    @Test
    void testGetParentSession() {
        assertThat(adapter.getParentSession()).isEqualTo(parent);
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
    void testRedirectUri() {
        adapter.setRedirectUri(REDIRECT_URI);
        assertThat(adapter.getRedirectUri()).isEqualTo(REDIRECT_URI);
    }

    @Test
    void testAction() {
        adapter.setAction(ACTION_VERIFY_EMAIL);
        assertThat(adapter.getAction()).isEqualTo(ACTION_VERIFY_EMAIL);
    }

    @Test
    void testProtocol() {
        adapter.setProtocol(PROTOCOL_OPENID_CONNECT);
        assertThat(adapter.getProtocol()).isEqualTo(PROTOCOL_OPENID_CONNECT);
    }

    @Test
    void testAuthenticatedUser() {
        UserModel user = mock(UserModel.class);
        when(user.getId()).thenReturn(TEST_USER_ID);
        when(userProvider.getUserById(realm, TEST_USER_ID)).thenReturn(user);

        adapter.setAuthenticatedUser(user);
        assertThat(entity.getAuthUserId()).isEqualTo(TEST_USER_ID);

        UserModel retrieved = adapter.getAuthenticatedUser();
        assertThat(retrieved).isEqualTo(user);
    }

    @Test
    void testAuthenticatedUser_SetToNull() {
        adapter.setAuthenticatedUser(null);
        assertThat(entity.getAuthUserId()).isNull();
        assertThat(adapter.getAuthenticatedUser()).isNull();
    }

    @Test
    void testClientNotes() {
        adapter.setClientNote(NOTE_KEY_1, NOTE_VALUE_1);
        assertThat(adapter.getClientNote(NOTE_KEY_1)).isEqualTo(NOTE_VALUE_1);

        Map<String, String> notes = adapter.getClientNotes();
        assertThat(notes).containsEntry(NOTE_KEY_1, NOTE_VALUE_1);
    }

    @Test
    void testClientNote_SetNullRemoves() {
        adapter.setClientNote(NOTE_KEY_1, NOTE_VALUE_1);
        adapter.setClientNote(NOTE_KEY_1, null);
        assertThat(adapter.getClientNote(NOTE_KEY_1)).isNull();
    }

    @Test
    void testRemoveClientNote() {
        adapter.setClientNote(NOTE_KEY_1, NOTE_VALUE_1);
        adapter.removeClientNote(NOTE_KEY_1);
        assertThat(adapter.getClientNote(NOTE_KEY_1)).isNull();
    }

    @Test
    void testClearClientNotes() {
        adapter.setClientNote(NOTE_KEY_1, NOTE_VALUE_1);
        adapter.setClientNote(NOTE_KEY_2, NOTE_VALUE_2);
        adapter.clearClientNotes();

        assertThat(adapter.getClientNotes()).isEmpty();
    }

    @Test
    void testAuthNotes() {
        adapter.setAuthNote(NOTE_KEY_1, NOTE_VALUE_1);
        assertThat(adapter.getAuthNote(NOTE_KEY_1)).isEqualTo(NOTE_VALUE_1);
    }

    @Test
    void testAuthNote_SetNullRemoves() {
        adapter.setAuthNote(NOTE_KEY_1, NOTE_VALUE_1);
        adapter.setAuthNote(NOTE_KEY_1, null);
        assertThat(adapter.getAuthNote(NOTE_KEY_1)).isNull();
    }

    @Test
    void testRemoveAuthNote() {
        adapter.setAuthNote(NOTE_KEY_1, NOTE_VALUE_1);
        adapter.removeAuthNote(NOTE_KEY_1);
        assertThat(adapter.getAuthNote(NOTE_KEY_1)).isNull();
    }

    @Test
    void testClearAuthNotes() {
        adapter.setAuthNote(NOTE_KEY_1, NOTE_VALUE_1);
        adapter.setAuthNote(NOTE_KEY_2, NOTE_VALUE_2);
        adapter.clearAuthNotes();

        assertThat(adapter.getAuthNote(NOTE_KEY_1)).isNull();
        assertThat(adapter.getAuthNote(NOTE_KEY_2)).isNull();
    }

    @Test
    void testUserSessionNotes() {
        adapter.setUserSessionNote(NOTE_KEY_1, NOTE_VALUE_1);
        Map<String, String> notes = adapter.getUserSessionNotes();
        assertThat(notes).containsEntry(NOTE_KEY_1, NOTE_VALUE_1);
    }

    @Test
    void testUserSessionNote_SetNullRemoves() {
        adapter.setUserSessionNote(NOTE_KEY_1, NOTE_VALUE_1);
        adapter.setUserSessionNote(NOTE_KEY_1, null);
        Map<String, String> notes = adapter.getUserSessionNotes();
        assertThat(notes).doesNotContainKey(NOTE_KEY_1);
    }

    @Test
    void testClientScopes() {
        Set<String> scopes = Set.of(PROTOCOL_OPENID_CONNECT, PROTOCOL_SAML);
        adapter.setClientScopes(scopes);
        
        Set<String> retrieved = adapter.getClientScopes();
        assertThat(retrieved).contains(PROTOCOL_OPENID_CONNECT, PROTOCOL_SAML);
    }

    @Test
    void testRequiredActions() {
        adapter.addRequiredAction(ACTION_VERIFY_EMAIL);
        assertThat(adapter.getRequiredActions()).contains(ACTION_VERIFY_EMAIL);
    }

    @Test
    void testRemoveRequiredAction() {
        adapter.addRequiredAction(ACTION_VERIFY_EMAIL);
        adapter.removeRequiredAction(ACTION_VERIFY_EMAIL);

        assertThat(adapter.getRequiredActions()).doesNotContain(ACTION_VERIFY_EMAIL);
    }

    @Test
    void testAddRequiredAction_WithUserAction() {
        UserModel.RequiredAction action = UserModel.RequiredAction.VERIFY_EMAIL;
        adapter.addRequiredAction(action);

        assertThat(adapter.getRequiredActions()).contains(action.name());
    }

    @Test
    void testRemoveRequiredAction_WithUserAction() {
        UserModel.RequiredAction action = UserModel.RequiredAction.VERIFY_EMAIL;
        adapter.addRequiredAction(action);
        adapter.removeRequiredAction(action);

        assertThat(adapter.getRequiredActions()).doesNotContain(action.name());
    }

}
