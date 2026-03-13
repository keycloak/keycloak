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
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.redis.entities.RedisUserSessionEntity;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RedisUserSessionEntity.
 */
class RedisUserSessionEntityTest {

    @Test
    void testDefaultConstructor() {
        RedisUserSessionEntity entity = new RedisUserSessionEntity();
        assertThat(entity).isNotNull();
    }

    @Test
    void testCreate() {
        RealmModel realm = mock(RealmModel.class);
        when(realm.getId()).thenReturn("realm1");
        
        UserModel user = mock(UserModel.class);
        when(user.getId()).thenReturn("user1");
        
        RedisUserSessionEntity entity = RedisUserSessionEntity.create(
                "session1",
                realm,
                user,
                "testuser",
                "192.168.1.1",
                "password",
                true,
                "broker1",
                "brokerUser1"
        );
        
        assertThat(entity.getId()).isEqualTo("session1");
        assertThat(entity.getRealmId()).isEqualTo("realm1");
        assertThat(entity.getUserId()).isEqualTo("user1");
        assertThat(entity.getLoginUsername()).isEqualTo("testuser");
        assertThat(entity.getIpAddress()).isEqualTo("192.168.1.1");
        assertThat(entity.getAuthMethod()).isEqualTo("password");
        assertThat(entity.isRememberMe()).isTrue();
        assertThat(entity.getBrokerSessionId()).isEqualTo("broker1");
        assertThat(entity.getBrokerUserId()).isEqualTo("brokerUser1");
        assertThat(entity.getStarted()).isGreaterThan(0);
        assertThat(entity.getLastSessionRefresh()).isGreaterThan(0);
        assertThat(entity.getState()).isNull();
        assertThat(entity.getNotes()).isNotNull().isEmpty();
        assertThat(entity.isOffline()).isFalse();
    }

    @Test
    void testCreateFromModel() {
        UserSessionModel model = mock(UserSessionModel.class);
        RealmModel realm = mock(RealmModel.class);
        UserModel user = mock(UserModel.class);
        
        when(model.getId()).thenReturn("session1");
        when(model.getRealm()).thenReturn(realm);
        when(realm.getId()).thenReturn("realm1");
        when(model.getUser()).thenReturn(user);
        when(user.getId()).thenReturn("user1");
        when(model.getLoginUsername()).thenReturn("testuser");
        when(model.getIpAddress()).thenReturn("192.168.1.1");
        when(model.getAuthMethod()).thenReturn("password");
        when(model.isRememberMe()).thenReturn(true);
        when(model.getBrokerSessionId()).thenReturn("broker1");
        when(model.getBrokerUserId()).thenReturn("brokerUser1");
        when(model.getStarted()).thenReturn(1000);
        when(model.getLastSessionRefresh()).thenReturn(2000);
        when(model.getState()).thenReturn(UserSessionModel.State.LOGGED_IN);
        
        Map<String, String> notes = new HashMap<>();
        notes.put("key1", "value1");
        when(model.getNotes()).thenReturn(notes);
        
        RedisUserSessionEntity entity = RedisUserSessionEntity.createFromModel(model);
        
        assertThat(entity.getId()).isEqualTo("session1");
        assertThat(entity.getRealmId()).isEqualTo("realm1");
        assertThat(entity.getUserId()).isEqualTo("user1");
        assertThat(entity.getLoginUsername()).isEqualTo("testuser");
        assertThat(entity.getStarted()).isEqualTo(1000);
        assertThat(entity.getLastSessionRefresh()).isEqualTo(2000);
        assertThat(entity.getState()).isEqualTo(UserSessionModel.State.LOGGED_IN);
        assertThat(entity.getNotes()).containsEntry("key1", "value1");
    }

    @Test
    void testCreateFromModel_NullUser() {
        UserSessionModel model = mock(UserSessionModel.class);
        RealmModel realm = mock(RealmModel.class);
        
        when(model.getId()).thenReturn("session1");
        when(model.getRealm()).thenReturn(realm);
        when(realm.getId()).thenReturn("realm1");
        when(model.getUser()).thenReturn(null);
        
        RedisUserSessionEntity entity = RedisUserSessionEntity.createFromModel(model);
        
        assertThat(entity.getUserId()).isNull();
    }

    @Test
    void testCreateFromModel_NullNotes() {
        UserSessionModel model = mock(UserSessionModel.class);
        RealmModel realm = mock(RealmModel.class);
        
        when(model.getId()).thenReturn("session1");
        when(model.getRealm()).thenReturn(realm);
        when(realm.getId()).thenReturn("realm1");
        when(model.getNotes()).thenReturn(null);
        
        RedisUserSessionEntity entity = RedisUserSessionEntity.createFromModel(model);
        
        assertThat(entity.getNotes()).isNotNull().isEmpty();
    }

    @Test
    void testSettersAndGetters() {
        RedisUserSessionEntity entity = new RedisUserSessionEntity();
        
        entity.setId("id1");
        assertThat(entity.getId()).isEqualTo("id1");
        
        entity.setRealmId("realm1");
        assertThat(entity.getRealmId()).isEqualTo("realm1");
        
        entity.setUserId("user1");
        assertThat(entity.getUserId()).isEqualTo("user1");
        
        entity.setBrokerSessionId("broker1");
        assertThat(entity.getBrokerSessionId()).isEqualTo("broker1");
        
        entity.setBrokerUserId("brokerUser1");
        assertThat(entity.getBrokerUserId()).isEqualTo("brokerUser1");
        
        entity.setLoginUsername("testuser");
        assertThat(entity.getLoginUsername()).isEqualTo("testuser");
        
        entity.setIpAddress("192.168.1.1");
        assertThat(entity.getIpAddress()).isEqualTo("192.168.1.1");
        
        entity.setAuthMethod("password");
        assertThat(entity.getAuthMethod()).isEqualTo("password");
        
        entity.setRememberMe(true);
        assertThat(entity.isRememberMe()).isTrue();
        
        entity.setStarted(1000);
        assertThat(entity.getStarted()).isEqualTo(1000);
        
        entity.setLastSessionRefresh(2000);
        assertThat(entity.getLastSessionRefresh()).isEqualTo(2000);
        
        entity.setState(UserSessionModel.State.LOGGED_IN);
        assertThat(entity.getState()).isEqualTo(UserSessionModel.State.LOGGED_IN);
        
        entity.setOffline(true);
        assertThat(entity.isOffline()).isTrue();
    }

    @Test
    void testSetLastSessionRefresh_KeepsMaxValue() {
        RedisUserSessionEntity entity = new RedisUserSessionEntity();
        entity.setLastSessionRefresh(2000);
        entity.setLastSessionRefresh(1500); // Lower value
        
        assertThat(entity.getLastSessionRefresh()).isEqualTo(2000);
    }

    @Test
    void testSetLastSessionRefresh_UpdatesToHigherValue() {
        RedisUserSessionEntity entity = new RedisUserSessionEntity();
        entity.setLastSessionRefresh(1000);
        entity.setLastSessionRefresh(2000); // Higher value
        
        assertThat(entity.getLastSessionRefresh()).isEqualTo(2000);
    }

    @Test
    void testNoteOperations() {
        RedisUserSessionEntity entity = new RedisUserSessionEntity();
        
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
        RedisUserSessionEntity entity = new RedisUserSessionEntity();
        entity.setNotes(null);
        
        entity.setNote("key1", "value1");
        
        assertThat(entity.getNotes()).isNotNull();
        assertThat(entity.getNote("key1")).isEqualTo("value1");
    }

    @Test
    void testGetNote_NullNotes() {
        RedisUserSessionEntity entity = new RedisUserSessionEntity();
        entity.setNotes(null);
        
        assertThat(entity.getNote("key1")).isNull();
    }

    @Test
    void testRemoveNote_NullNotes() {
        RedisUserSessionEntity entity = new RedisUserSessionEntity();
        entity.setNotes(null);
        
        entity.removeNote("key1"); // Should not throw
    }

    @Test
    void testRestart() {
        RedisUserSessionEntity entity = new RedisUserSessionEntity();
        entity.setId("session1");
        entity.setNote("oldNote", "oldValue");
        
        entity.restart(
                "newRealm",
                "newUser",
                "newLogin",
                "10.0.0.1",
                "oauth",
                false,
                "newBroker",
                "newBrokerUser"
        );
        
        assertThat(entity.getRealmId()).isEqualTo("newRealm");
        assertThat(entity.getUserId()).isEqualTo("newUser");
        assertThat(entity.getLoginUsername()).isEqualTo("newLogin");
        assertThat(entity.getIpAddress()).isEqualTo("10.0.0.1");
        assertThat(entity.getAuthMethod()).isEqualTo("oauth");
        assertThat(entity.isRememberMe()).isFalse();
        assertThat(entity.getBrokerSessionId()).isEqualTo("newBroker");
        assertThat(entity.getBrokerUserId()).isEqualTo("newBrokerUser");
        assertThat(entity.getState()).isNull();
        assertThat(entity.getNotes()).isEmpty(); // Notes should be cleared
    }

    @Test
    void testEquals() {
        RedisUserSessionEntity entity1 = new RedisUserSessionEntity();
        entity1.setId("session1");
        
        RedisUserSessionEntity entity2 = new RedisUserSessionEntity();
        entity2.setId("session1");
        
        RedisUserSessionEntity entity3 = new RedisUserSessionEntity();
        entity3.setId("session2");
        
        assertThat(entity1).isEqualTo(entity1); // Same object
        assertThat(entity1).isEqualTo(entity2); // Same ID
        assertThat(entity1).isNotEqualTo(entity3); // Different ID
        assertThat(entity1).isNotEqualTo(null); // Null
        assertThat(entity1).isNotEqualTo("string"); // Different type
    }

    @Test
    void testHashCode() {
        RedisUserSessionEntity entity1 = new RedisUserSessionEntity();
        entity1.setId("session1");
        
        RedisUserSessionEntity entity2 = new RedisUserSessionEntity();
        entity2.setId("session1");
        
        assertThat(entity1.hashCode()).isEqualTo(entity2.hashCode());
    }

    @Test
    void testToString() {
        RedisUserSessionEntity entity = new RedisUserSessionEntity();
        entity.setId("session1");
        entity.setRealmId("realm1");
        entity.setUserId("user1");
        entity.setLoginUsername("testuser");
        entity.setOffline(true);
        
        String str = entity.toString();
        
        assertThat(str).contains("session1");
        assertThat(str).contains("realm1");
        assertThat(str).contains("user1");
        assertThat(str).contains("testuser");
        assertThat(str).contains("offline=true");
    }
}
