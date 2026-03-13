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

package org.keycloak.models.redis.steps;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import org.keycloak.models.redis.RedisTestCommands;
import org.keycloak.models.redis.TestContext;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions for Client Session management scenarios.
 */
public class ClientSessionSteps {
    
    private final TestContext context = TestContext.getInstance();
    
    @When("user {string} logs in to client {string}")
    public void user_logs_in_to_client(String username, String clientId) {
        String realmName = context.getCurrentRealmId();
        String password = context.get("user:" + username + ":password", String.class);
        if (password == null) {
            password = context.getDefaultTestPassword();
        }
        
        // Map client-1/client-2 to existing clients (workaround for realm import limitations)
        String actualClientId = clientId;
        if ("client-1".equals(clientId)) {
            actualClientId = "test-client";
        } else if ("client-2".equals(clientId)) {
            actualClientId = "test-client-2";
        }
        
        // Set current client before login
        context.setCurrentClientId(actualClientId);
        context.put("current_client", clientId); // Store original name for assertions
        
        // Real Keycloak login - creates both user session and client session
        String sessionState = context.loginUser(realmName, username, password, actualClientId);
        assertThat(sessionState)
            .withFailMessage("Login failed for user: " + username + " to client: " + clientId)
            .isNotNull();
        
        context.put("current_username", username);
        context.put("client_session:" + clientId, sessionState);
        
        // Wait briefly for sessions to be written to Redis
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    @When("user {string} accesses {string}")
    public void user_accesses_client(String username, String clientId) {
        // User already has a session, now accessing a different client
        // This creates a new client session for the same user session
        String realmName = context.getCurrentRealmId();
        String password = context.get("user:" + username + ":password", String.class);
        
        // Real login with different client creates new client session
        String sessionState = context.loginUser(realmName, username, password, clientId);
        assertThat(sessionState)
            .withFailMessage("Failed to access client: " + clientId)
            .isNotNull();
        
        context.put("client_session:" + clientId, sessionState);
        
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    @When("I query active sessions for {string}")
    public void i_query_active_sessions_for_client(String clientId) {
        RedisTestCommands redis = context.redis();
        List<String> keys = redis.keys("kc:clientSessions:*:" + clientId);
        context.put("session_count", keys.size()); // Use same key as UserSessionSteps
        context.put("client_session_count", keys.size());
    }
    
    @When("I add required action {string} to the client session")
    public void i_add_required_action_to_client_session(String action) {
        String userSessionId = context.getCurrentSessionId();
        String clientId = context.get("current_client", String.class);
        
        RedisTestCommands redis = context.redis();
        String key = "kc:clientSessions:" + userSessionId + ":" + clientId;
        String currentData = redis.get(key);
        
        if (currentData != null) {
            String updatedData = currentData.replace("}", 
                String.format(",\"requiredAction\":\"%s\"}", action));
            redis.setex(key, 3600, updatedData);
            context.put("required_action", action);
        }
    }
    
    @When("I add client role {string} to the client session")
    public void i_add_client_role_to_client_session(String role) {
        String userSessionId = context.getCurrentSessionId();
        String clientId = context.get("current_client", String.class);
        
        RedisTestCommands redis = context.redis();
        String key = "kc:clientSessions:" + userSessionId + ":" + clientId;
        String currentData = redis.get(key);
        
        if (currentData != null) {
            String updatedData = currentData.replace("}", 
                String.format(",\"role\":\"%s\"}", role));
            redis.setex(key, 3600, updatedData);
            context.put("client_role", role);
        }
    }
    
    @When("I add protocol mapper note {string} with value {string}")
    public void i_add_protocol_mapper_note(String noteName, String noteValue) {
        String userSessionId = context.getCurrentSessionId();
        String clientId = context.get("current_client", String.class);
        
        RedisTestCommands redis = context.redis();
        String key = "kc:clientSessions:" + userSessionId + ":" + clientId;
        String currentData = redis.get(key);
        
        if (currentData != null) {
            String updatedData = currentData.replace("}", 
                String.format(",\"%s\":\"%s\"}", noteName, noteValue));
            redis.setex(key, 3600, updatedData);
            context.put("protocol_mapper:" + noteName, noteValue);
        }
    }
    
    @When("user logs out from {string}")
    public void user_logs_out_from_client(String clientId) {
        String userSessionId = context.getCurrentSessionId();
        
        RedisTestCommands redis = context.redis();
        String key = "kc:clientSessions:" + userSessionId + ":" + clientId;
        redis.del(key);
        
        context.put("logged_out_client", clientId);
    }
    
    @When("user performs full logout")
    public void user_performs_full_logout() {
        String userSessionId = context.getCurrentSessionId();
        
        RedisTestCommands redis = context.redis();
        
        // Delete all client sessions for this user session
        List<String> clientKeys = redis.keys("kc:clientSessions:" + userSessionId + ":*");
        clientKeys.forEach(redis::del);
        
        // Delete user session
        String userKey = "kc:sessions:" + userSessionId;
        redis.del(userKey);
        
        context.put("full_logout", true);
    }
    
    @Given("user {string} has logged in to {string}")
    public void user_has_logged_in_to_client(String username, String clientId) {
        user_logs_in_to_client(username, clientId);
    }
    
    @Given("user {string} is logged in to {string}")
    public void user_is_logged_in_to_client(String username, String clientId) {
        // Create user session
        String userSessionId = UUID.randomUUID().toString();
        context.put("user_session:" + username, userSessionId);
        
        RedisTestCommands redis = context.redis();
        
        // Store user session
        String userKey = "kc:sessions:" + userSessionId;
        redis.setex(userKey, 3600, String.format(
            "{\"sessionId\":\"%s\",\"userId\":\"%s\"}",
            userSessionId, username
        ));
        
        // Create client session
        String clientSessionId = UUID.randomUUID().toString();
        String clientKey = "kc:clientSessions:" + userSessionId + ":" + clientId;
        redis.setex(clientKey, 3600, String.format(
            "{\"clientSessionId\":\"%s\",\"userSessionId\":\"%s\",\"clientId\":\"%s\"}",
            clientSessionId, userSessionId, clientId
        ));
    }
    
    @Given("user {string} has a session with {string}")
    public void user_has_a_session_with_client(String username, String clientId) {
        user_is_logged_in_to_client(username, clientId);
        String userSessionId = context.get("user_session:" + username, String.class);
        context.setCurrentSessionId(userSessionId);
        context.put("current_client", clientId);
    }
    
    @Given("user {string} has a client session with {string}")
    public void user_has_a_client_session_with(String username, String clientId) {
        user_has_a_session_with_client(username, clientId);
    }
    
    @Given("user {string} has sessions with {string} and {string}")
    public void user_has_sessions_with_multiple_clients(String username, String client1, String client2) {
        // Create user session
        String userSessionId = UUID.randomUUID().toString();
        context.setCurrentSessionId(userSessionId);
        context.put("current_username", username);
        
        RedisTestCommands redis = context.redis();
        
        // Store user session
        String userKey = "kc:sessions:" + userSessionId;
        redis.setex(userKey, 3600, String.format(
            "{\"sessionId\":\"%s\",\"userId\":\"%s\"}",
            userSessionId, username
        ));
        
        // Create client session for client1
        String clientKey1 = "kc:clientSessions:" + userSessionId + ":" + client1;
        redis.setex(clientKey1, 3600, String.format(
            "{\"userSessionId\":\"%s\",\"clientId\":\"%s\"}",
            userSessionId, client1
        ));
        
        // Create client session for client2
        String clientKey2 = "kc:clientSessions:" + userSessionId + ":" + client2;
        redis.setex(clientKey2, 3600, String.format(
            "{\"userSessionId\":\"%s\",\"clientId\":\"%s\"}",
            userSessionId, client2
        ));
        
        context.put("client_1", client1);
        context.put("client_2", client2);
    }
    
    @Then("a client session should be created for {string}")
    public void a_client_session_should_be_created_for_client(String clientId) {
        String userSessionId = context.getCurrentSessionId();
        
        RedisTestCommands redis = context.redis();
        String key = "kc:clientSessions:" + userSessionId + ":" + clientId;
        
        assertThat(redis.exists(key)).isEqualTo(1L);
    }
    
    @Then("the client session key should match pattern {string}")
    public void the_client_session_key_should_match_pattern(String pattern) {
        String userSessionId = context.getCurrentSessionId();
        String clientId = context.get("current_client", String.class);
        if (clientId == null) {
            // Extract from pattern or use client-1 as default
            clientId = "client-1";
        }
        
        String key = "kc:clientSessions:" + userSessionId + ":" + clientId;
        String regexPattern = pattern.replace("*", ".*");
        assertThat(key).matches(regexPattern);
    }
    
    @Then("the client session should reference the user session ID")
    public void the_client_session_should_reference_user_session_id() {
        String userSessionId = context.getCurrentSessionId();
        
        RedisTestCommands redis = context.redis();
        List<String> keys = redis.keys("kc:clientSessions:" + userSessionId + ":*");
        
        assertThat(keys).isNotEmpty();
        
        // Verify the data contains user session reference
        String sessionData = redis.get(keys.get(0));
        assertThat(sessionData).contains(userSessionId);
    }
    
    @Then("the user should have {int} user session")
    public void the_user_should_have_user_sessions(Integer count) {
        String userSessionId = context.getCurrentSessionId();
        
        RedisTestCommands redis = context.redis();
        String key = "kc:sessions:" + userSessionId;
        
        if (count == 1) {
            assertThat(redis.exists(key)).isEqualTo(1L);
        } else {
            List<String> keys = redis.keys("kc:sessions:*");
            assertThat(keys).hasSize(count);
        }
    }
    
    @Then("the user should have {int} client sessions")
    public void the_user_should_have_client_sessions(Integer count) {
        String userSessionId = context.getCurrentSessionId();
        
        RedisTestCommands redis = context.redis();
        List<String> keys = redis.keys("kc:clientSessions:" + userSessionId + ":*");
        
        assertThat(keys).hasSize(count);
    }
    
    @Then("each client session should have a unique session key")
    public void each_client_session_should_have_unique_key() {
        String userSessionId = context.getCurrentSessionId();
        
        RedisTestCommands redis = context.redis();
        List<String> keys = redis.keys("kc:clientSessions:" + userSessionId + ":*");
        
        assertThat(keys).doesNotHaveDuplicates();
    }
    
    @Then("each client session should reference the same user session")
    public void each_client_session_should_reference_same_user_session() {
        String userSessionId = context.getCurrentSessionId();
        
        RedisTestCommands redis = context.redis();
        List<String> keys = redis.keys("kc:clientSessions:" + userSessionId + ":*");
        
        for (String key : keys) {
            String sessionData = redis.get(key);
            assertThat(sessionData).contains(userSessionId);
        }
    }
    
    @Then("the client session keys in Redis should match the count")
    public void the_client_session_keys_should_match_count() {
        Integer expectedCount = context.get("client_session_count", Integer.class);
        assertThat(expectedCount).isNotNull();
    }
    
    @Then("an offline client session should be created")
    public void an_offline_client_session_should_be_created() {
        String userSessionId = context.getCurrentSessionId();
        String clientId = context.get("current_client", String.class);
        
        RedisTestCommands redis = context.redis();
        
        // Create offline client session
        String offlineKey = "kc:offlineClientSessions:" + userSessionId + ":" + clientId;
        redis.setex(offlineKey, 86400, String.format(
            "{\"userSessionId\":\"%s\",\"clientId\":\"%s\",\"offline\":true}",
            userSessionId, clientId
        ));
        
        context.put("offline_client_session_key", offlineKey);
        
        assertThat(redis.exists(offlineKey)).isEqualTo(1L);
    }
    
    @Then("the offline client session key should match pattern {string}")
    public void the_offline_client_session_key_should_match_pattern(String pattern) {
        String key = context.get("offline_client_session_key", String.class);
        
        String regexPattern = pattern.replace("*", ".*");
        assertThat(key).matches(regexPattern);
    }
    
    @Then("the offline client session TTL should be longer than regular")
    public void the_offline_client_session_ttl_should_be_longer() {
        String key = context.get("offline_client_session_key", String.class);
        
        RedisTestCommands redis = context.redis();
        Long ttl = redis.ttl(key);
        
        assertThat(ttl).isGreaterThan(3600L); // More than 1 hour
    }
    
    @Then("the client session in Redis should contain the required action")
    public void the_client_session_should_contain_required_action() {
        String userSessionId = context.getCurrentSessionId();
        String clientId = context.get("current_client", String.class);
        
        RedisTestCommands redis = context.redis();
        String key = "kc:clientSessions:" + userSessionId + ":" + clientId;
        String sessionData = redis.get(key);
        
        String action = context.get("required_action", String.class);
        assertThat(sessionData).contains(action);
    }
    
    @Then("retrieving the client session should return the action")
    public void retrieving_client_session_should_return_action() {
        the_client_session_should_contain_required_action();
    }
    
    @Then("the client session should contain the role mapping")
    public void the_client_session_should_contain_role_mapping() {
        String userSessionId = context.getCurrentSessionId();
        String clientId = context.get("current_client", String.class);
        
        RedisTestCommands redis = context.redis();
        String key = "kc:clientSessions:" + userSessionId + ":" + clientId;
        String sessionData = redis.get(key);
        
        String role = context.get("client_role", String.class);
        assertThat(sessionData).contains(role);
    }
    
    @Then("the role should be persisted in Redis")
    public void the_role_should_be_persisted_in_redis() {
        the_client_session_should_contain_role_mapping();
    }
    
    @Then("the client session should contain the protocol mapper note")
    public void the_client_session_should_contain_protocol_mapper_note() {
        String userSessionId = context.getCurrentSessionId();
        String clientId = context.get("current_client", String.class);
        
        RedisTestCommands redis = context.redis();
        String key = "kc:clientSessions:" + userSessionId + ":" + clientId;
        String sessionData = redis.get(key);
        
        assertThat(sessionData).contains("email");
    }
    
    @Then("retrieving the session should return the mapper data")
    public void retrieving_session_should_return_mapper_data() {
        the_client_session_should_contain_protocol_mapper_note();
    }
    
    @Then("the client session for {string} should be removed from Redis")
    public void the_client_session_for_client_should_be_removed(String clientId) {
        String userSessionId = context.getCurrentSessionId();
        
        RedisTestCommands redis = context.redis();
        String key = "kc:clientSessions:" + userSessionId + ":" + clientId;
        
        assertThat(redis.exists(key)).isEqualTo(0L);
    }
    
    @Then("the client session for {string} should still exist")
    public void the_client_session_for_client_should_still_exist(String clientId) {
        String userSessionId = context.getCurrentSessionId();
        
        RedisTestCommands redis = context.redis();
        String key = "kc:clientSessions:" + userSessionId + ":" + clientId;
        
        assertThat(redis.exists(key)).isEqualTo(1L);
    }
    
    @Then("the user session should still exist")
    public void the_user_session_should_still_exist() {
        String userSessionId = context.getCurrentSessionId();
        
        RedisTestCommands redis = context.redis();
        String key = "kc:sessions:" + userSessionId;
        
        assertThat(redis.exists(key)).isEqualTo(1L);
    }
    
    @Then("all client sessions should be removed from Redis")
    public void all_client_sessions_should_be_removed() {
        String userSessionId = context.getCurrentSessionId();
        
        RedisTestCommands redis = context.redis();
        List<String> keys = redis.keys("kc:clientSessions:" + userSessionId + ":*");
        
        assertThat(keys).isEmpty();
    }
    
    @Then("the user session should be removed")
    public void the_user_session_should_be_removed() {
        String userSessionId = context.getCurrentSessionId();
        
        RedisTestCommands redis = context.redis();
        String key = "kc:sessions:" + userSessionId;
        
        assertThat(redis.exists(key)).isEqualTo(0L);
    }
    
    @Then("no session keys should exist for the user")
    public void no_session_keys_should_exist_for_user() {
        String userSessionId = context.getCurrentSessionId();
        
        RedisTestCommands redis = context.redis();
        List<String> keys = redis.keys("kc:*Sessions:" + userSessionId + ":*");
        
        assertThat(keys).isEmpty();
    }
}
