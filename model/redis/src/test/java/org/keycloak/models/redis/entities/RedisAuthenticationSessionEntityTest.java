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
import org.keycloak.models.redis.entities.RedisAuthenticationSessionEntity;
import org.keycloak.models.redis.entities.RedisAuthenticationSessionEntity.RedisAuthenticationTabEntity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for RedisAuthenticationSessionEntity and RedisAuthenticationTabEntity.
 */
class RedisAuthenticationSessionEntityTest {

    @Test
    void testDefaultConstructor() {
        RedisAuthenticationSessionEntity entity = new RedisAuthenticationSessionEntity();
        assertThat(entity).isNotNull();
    }

    @Test
    void testParameterizedConstructor() {
        RedisAuthenticationSessionEntity entity = new RedisAuthenticationSessionEntity("root1", "realm1", 1000);
        
        assertThat(entity.getId()).isEqualTo("root1");
        assertThat(entity.getRealmId()).isEqualTo("realm1");
        assertThat(entity.getTimestamp()).isEqualTo(1000);
    }

    @Test
    void testSettersAndGetters() {
        RedisAuthenticationSessionEntity entity = new RedisAuthenticationSessionEntity();
        
        entity.setId("root1");
        assertThat(entity.getId()).isEqualTo("root1");
        
        entity.setRealmId("realm1");
        assertThat(entity.getRealmId()).isEqualTo("realm1");
        
        entity.setTimestamp(2000);
        assertThat(entity.getTimestamp()).isEqualTo(2000);
    }

    @Test
    void testAuthenticationSessionOperations() {
        RedisAuthenticationSessionEntity entity = new RedisAuthenticationSessionEntity("root1", "realm1", 1000);
        
        RedisAuthenticationTabEntity tab1 = new RedisAuthenticationTabEntity("tab1", "client1");
        entity.setAuthenticationSession("tab1", tab1);
        
        assertThat(entity.getAuthenticationSession("tab1")).isEqualTo(tab1);
        assertThat(entity.getAuthenticationSessions()).containsKey("tab1");
        assertThat(entity.getTabIds()).contains("tab1");
    }

    @Test
    void testRemoveAuthenticationSession() {
        RedisAuthenticationSessionEntity entity = new RedisAuthenticationSessionEntity("root1", "realm1", 1000);
        
        RedisAuthenticationTabEntity tab1 = new RedisAuthenticationTabEntity("tab1", "client1");
        entity.setAuthenticationSession("tab1", tab1);
        
        entity.removeAuthenticationSession("tab1");
        
        assertThat(entity.getAuthenticationSession("tab1")).isNull();
        assertThat(entity.getTabIds()).doesNotContain("tab1");
    }

    @Test
    void testSetAuthenticationSessions() {
        RedisAuthenticationSessionEntity entity = new RedisAuthenticationSessionEntity();
        
        Map<String, RedisAuthenticationTabEntity> sessions = new HashMap<>();
        RedisAuthenticationTabEntity tab1 = new RedisAuthenticationTabEntity("tab1", "client1");
        sessions.put("tab1", tab1);
        
        entity.setAuthenticationSessions(sessions);
        
        assertThat(entity.getAuthenticationSessions()).isEqualTo(sessions);
    }

    @Test
    void testGetTabIds() {
        RedisAuthenticationSessionEntity entity = new RedisAuthenticationSessionEntity("root1", "realm1", 1000);
        
        entity.setAuthenticationSession("tab1", new RedisAuthenticationTabEntity("tab1", "client1"));
        entity.setAuthenticationSession("tab2", new RedisAuthenticationTabEntity("tab2", "client2"));
        
        Set<String> tabIds = entity.getTabIds();
        
        assertThat(tabIds).containsExactlyInAnyOrder("tab1", "tab2");
    }

    // Tests for RedisAuthenticationTabEntity
    @Test
    void testTabEntityDefaultConstructor() {
        RedisAuthenticationTabEntity tab = new RedisAuthenticationTabEntity();
        assertThat(tab).isNotNull();
    }

    @Test
    void testTabEntityParameterizedConstructor() {
        RedisAuthenticationTabEntity tab = new RedisAuthenticationTabEntity("tab1", "client1");
        
        assertThat(tab.getTabId()).isEqualTo("tab1");
        assertThat(tab.getClientUUID()).isEqualTo("client1");
    }

    @Test
    void testTabEntitySettersAndGetters() {
        RedisAuthenticationTabEntity tab = new RedisAuthenticationTabEntity();
        
        tab.setTabId("tab1");
        assertThat(tab.getTabId()).isEqualTo("tab1");
        
        tab.setClientUUID("client1");
        assertThat(tab.getClientUUID()).isEqualTo("client1");
        
        tab.setAuthUserId("user1");
        assertThat(tab.getAuthUserId()).isEqualTo("user1");
        
        tab.setRedirectUri("http://localhost");
        assertThat(tab.getRedirectUri()).isEqualTo("http://localhost");
        
        tab.setAction("AUTHENTICATE");
        assertThat(tab.getAction()).isEqualTo("AUTHENTICATE");
        
        tab.setProtocol("openid-connect");
        assertThat(tab.getProtocol()).isEqualTo("openid-connect");
    }

    @Test
    void testTabEntityNotes() {
        RedisAuthenticationTabEntity tab = new RedisAuthenticationTabEntity();
        
        Map<String, String> clientNotes = new HashMap<>();
        clientNotes.put("client_key", "client_value");
        tab.setClientNotes(clientNotes);
        assertThat(tab.getClientNotes()).containsEntry("client_key", "client_value");
        
        Map<String, String> authNotes = new HashMap<>();
        authNotes.put("auth_key", "auth_value");
        tab.setAuthNotes(authNotes);
        assertThat(tab.getAuthNotes()).containsEntry("auth_key", "auth_value");
        
        Map<String, String> userSessionNotes = new HashMap<>();
        userSessionNotes.put("session_key", "session_value");
        tab.setUserSessionNotes(userSessionNotes);
        assertThat(tab.getUserSessionNotes()).containsEntry("session_key", "session_value");
    }

    @Test
    void testTabEntityRequiredActions() {
        RedisAuthenticationTabEntity tab = new RedisAuthenticationTabEntity();
        
        Set<String> actions = new HashSet<>();
        actions.add("UPDATE_PASSWORD");
        actions.add("VERIFY_EMAIL");
        
        tab.setRequiredActions(actions);
        
        assertThat(tab.getRequiredActions()).containsExactlyInAnyOrder("UPDATE_PASSWORD", "VERIFY_EMAIL");
    }

    @Test
    void testTabEntityClientScopes() {
        RedisAuthenticationTabEntity tab = new RedisAuthenticationTabEntity();
        
        Map<String, String> scopes = new HashMap<>();
        scopes.put("openid", "true");
        scopes.put("profile", "true");
        
        tab.setClientScopes(scopes);
        
        assertThat(tab.getClientScopes()).containsEntry("openid", "true");
        assertThat(tab.getClientScopes()).containsEntry("profile", "true");
    }

    @Test
    void testTabEntityExecutionStatus() {
        RedisAuthenticationTabEntity tab = new RedisAuthenticationTabEntity();
        
        Map<String, String> executionStatus = new HashMap<>();
        executionStatus.put("auth-cookie", "success");
        executionStatus.put("auth-username-form", "success");
        
        tab.setExecutionStatus(executionStatus);
        
        assertThat(tab.getExecutionStatus()).containsEntry("auth-cookie", "success");
        assertThat(tab.getExecutionStatus()).containsEntry("auth-username-form", "success");
    }
}
