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

package org.keycloak.models.redis.entities;

import org.junit.jupiter.api.Test;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.redis.entities.RedisClientSessionEntity;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RedisClientSessionEntity.
 */
class RedisClientSessionEntityTest {

    @Test
    void testDefaultConstructor() {
        RedisClientSessionEntity entity = new RedisClientSessionEntity();
        assertThat(entity).isNotNull();
    }

    @Test
    void testCreate() {
        RedisClientSessionEntity entity = RedisClientSessionEntity.create(
                "session1",
                "client1",
                "realm1"
        );
        
        assertThat(entity.getUserSessionId()).isEqualTo("session1");
        assertThat(entity.getClientId()).isEqualTo("client1");
        assertThat(entity.getRealmId()).isEqualTo("realm1");
        assertThat(entity.getTimestamp()).isGreaterThan(0);
        assertThat(entity.getNotes()).isNotNull().isEmpty();
        assertThat(entity.isOffline()).isFalse();
    }

    @Test
    void testCreateFromModel() {
        AuthenticatedClientSessionModel model = mock(AuthenticatedClientSessionModel.class);
        UserSessionModel userSession = mock(UserSessionModel.class);
        ClientModel client = mock(ClientModel.class);
        RealmModel realm = mock(RealmModel.class);
        
        when(model.getUserSession()).thenReturn(userSession);
        when(userSession.getId()).thenReturn("session1");
        when(model.getClient()).thenReturn(client);
        when(client.getId()).thenReturn("client1");
        when(model.getRealm()).thenReturn(realm);
        when(realm.getId()).thenReturn("realm1");
        when(model.getProtocol()).thenReturn("openid-connect");
        when(model.getRedirectUri()).thenReturn("http://localhost/callback");
        when(model.getTimestamp()).thenReturn(1000);
        when(model.getAction()).thenReturn("AUTHENTICATE");
        when(model.getCurrentRefreshToken()).thenReturn("refresh-token");
        when(model.getCurrentRefreshTokenUseCount()).thenReturn(3);
        
        Map<String, String> notes = new HashMap<>();
        notes.put("key1", "value1");
        when(model.getNotes()).thenReturn(notes);
        
        RedisClientSessionEntity entity = RedisClientSessionEntity.createFromModel(model);
        
        assertThat(entity.getUserSessionId()).isEqualTo("session1");
        assertThat(entity.getClientId()).isEqualTo("client1");
        assertThat(entity.getRealmId()).isEqualTo("realm1");
        assertThat(entity.getAuthMethod()).isEqualTo("openid-connect");
        assertThat(entity.getRedirectUri()).isEqualTo("http://localhost/callback");
        assertThat(entity.getTimestamp()).isEqualTo(1000);
        assertThat(entity.getAction()).isEqualTo("AUTHENTICATE");
        assertThat(entity.getCurrentRefreshToken()).isEqualTo("refresh-token");
        assertThat(entity.getCurrentRefreshTokenUseCount()).isEqualTo(3);
        assertThat(entity.getNotes()).containsEntry("key1", "value1");
    }

    @Test
    void testCreateFromModel_NullNotes() {
        AuthenticatedClientSessionModel model = mock(AuthenticatedClientSessionModel.class);
        UserSessionModel userSession = mock(UserSessionModel.class);
        ClientModel client = mock(ClientModel.class);
        RealmModel realm = mock(RealmModel.class);
        
        when(model.getUserSession()).thenReturn(userSession);
        when(userSession.getId()).thenReturn("session1");
        when(model.getClient()).thenReturn(client);
        when(client.getId()).thenReturn("client1");
        when(model.getRealm()).thenReturn(realm);
        when(realm.getId()).thenReturn("realm1");
        when(model.getNotes()).thenReturn(null);
        
        RedisClientSessionEntity entity = RedisClientSessionEntity.createFromModel(model);
        
        assertThat(entity.getNotes()).isNotNull().isEmpty();
    }

    @Test
    void testGetKey() {
        RedisClientSessionEntity entity = RedisClientSessionEntity.create("session1", "client1", "realm1");
        assertThat(entity.getKey()).isEqualTo("session1:client1");
    }

    @Test
    void testSettersAndGetters() {
        RedisClientSessionEntity entity = new RedisClientSessionEntity();
        
        entity.setUserSessionId("session1");
        assertThat(entity.getUserSessionId()).isEqualTo("session1");
        
        entity.setClientId("client1");
        assertThat(entity.getClientId()).isEqualTo("client1");
        
        entity.setRealmId("realm1");
        assertThat(entity.getRealmId()).isEqualTo("realm1");
        
        entity.setAuthMethod("oauth");
        assertThat(entity.getAuthMethod()).isEqualTo("oauth");
        
        entity.setRedirectUri("http://localhost");
        assertThat(entity.getRedirectUri()).isEqualTo("http://localhost");
        
        entity.setTimestamp(1234);
        assertThat(entity.getTimestamp()).isEqualTo(1234);
        
        entity.setAction("LOGIN");
        assertThat(entity.getAction()).isEqualTo("LOGIN");
        
        entity.setCurrentRefreshToken("token");
        assertThat(entity.getCurrentRefreshToken()).isEqualTo("token");
        
        entity.setCurrentRefreshTokenUseCount(5);
        assertThat(entity.getCurrentRefreshTokenUseCount()).isEqualTo(5);
        
        entity.setOffline(true);
        assertThat(entity.isOffline()).isTrue();
    }

    @Test
    void testNoteOperations() {
        RedisClientSessionEntity entity = new RedisClientSessionEntity();
        
        // Set notes
        Map<String, String> notes = new HashMap<>();
        notes.put("key1", "value1");
        entity.setNotes(notes);
        assertThat(entity.getNotes()).containsEntry("key1", "value1");
        
        // Get note
        assertThat(entity.getNote("key1")).isEqualTo("value1");
        assertThat(entity.getNote("nonexistent")).isNull();
        
        // Set individual note
        entity.setNote("key2", "value2");
        assertThat(entity.getNote("key2")).isEqualTo("value2");
        
        // Remove note with null value
        entity.setNote("key2", null);
        assertThat(entity.getNote("key2")).isNull();
        
        // Remove note
        entity.removeNote("key1");
        assertThat(entity.getNote("key1")).isNull();
    }

    @Test
    void testSetNote_CreatesMapIfNull() {
        RedisClientSessionEntity entity = new RedisClientSessionEntity();
        entity.setNotes(null);
        
        entity.setNote("key1", "value1");
        
        assertThat(entity.getNotes()).isNotNull();
        assertThat(entity.getNote("key1")).isEqualTo("value1");
    }

    @Test
    void testGetNote_NullNotes() {
        RedisClientSessionEntity entity = new RedisClientSessionEntity();
        entity.setNotes(null);
        
        assertThat(entity.getNote("key1")).isNull();
    }

    @Test
    void testRemoveNote_NullNotes() {
        RedisClientSessionEntity entity = new RedisClientSessionEntity();
        entity.setNotes(null);
        
        entity.removeNote("key1"); // Should not throw
    }

    @Test
    void testEquals() {
        RedisClientSessionEntity entity1 = RedisClientSessionEntity.create("session1", "client1", "realm1");
        RedisClientSessionEntity entity2 = RedisClientSessionEntity.create("session1", "client1", "realm1");
        RedisClientSessionEntity entity3 = RedisClientSessionEntity.create("session2", "client1", "realm1");
        RedisClientSessionEntity entity4 = RedisClientSessionEntity.create("session1", "client2", "realm1");
        
        assertThat(entity1).isEqualTo(entity1); // Same object
        assertThat(entity1).isEqualTo(entity2); // Same IDs
        assertThat(entity1).isNotEqualTo(entity3); // Different session ID
        assertThat(entity1).isNotEqualTo(entity4); // Different client ID
        assertThat(entity1).isNotEqualTo(null); // Null
        assertThat(entity1).isNotEqualTo("string"); // Different type
    }

    @Test
    void testHashCode() {
        RedisClientSessionEntity entity1 = RedisClientSessionEntity.create("session1", "client1", "realm1");
        RedisClientSessionEntity entity2 = RedisClientSessionEntity.create("session1", "client1", "realm1");
        
        assertThat(entity1.hashCode()).isEqualTo(entity2.hashCode());
    }

    @Test
    void testToString() {
        RedisClientSessionEntity entity = RedisClientSessionEntity.create("session1", "client1", "realm1");
        entity.setOffline(true);
        
        String str = entity.toString();
        
        assertThat(str).contains("session1");
        assertThat(str).contains("client1");
        assertThat(str).contains("realm1");
        assertThat(str).contains("offline=true");
    }
}
