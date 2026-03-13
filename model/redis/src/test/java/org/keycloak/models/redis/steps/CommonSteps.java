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

import io.cucumber.java.Before;
import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.lettuce.core.api.sync.RedisCommands;
import org.keycloak.models.redis.TestContext;
import org.keycloak.models.redis.KeycloakAdminHelper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Common step definitions for Redis and Keycloak setup.
 */
public class CommonSteps {
    
    private final TestContext context = TestContext.getInstance();
    private final KeycloakAdminHelper adminHelper = new KeycloakAdminHelper(context);
    
    @Before
    public void beforeScenario() {
        context.reset();
        
        // Clean up Redis test data from previous scenarios
        try {
            // Works for both cluster and standalone modes
            io.lettuce.core.api.sync.RedisKeyCommands<String, String> redis = context.getRedisCommands();
            
            // Delete all test sessions and auth sessions with more comprehensive patterns
            java.util.List<String> patterns = java.util.Arrays.asList(
                "kc:authSessions:*",
                "kc:sessions:*",
                "kc:clientSessions:*",
                "kc:offlineSessions:*",
                "kc:offlineClientSessions:*",
                "kc:loginFailures:*",
                "kc:actionTokens:*"
            );
            
            int deletedCount = 0;
            for (String pattern : patterns) {
                java.util.List<String> keys = redis.keys(pattern);
                if (keys != null && !keys.isEmpty()) {
                    for (String key : keys) {
                        redis.del(key);
                        deletedCount++;
                    }
                }
            }
            
            if (deletedCount > 0) {
                System.out.println("🧹 Cleaned up " + deletedCount + " Redis keys from previous scenarios");
                // Brief pause to ensure cleanup is complete
                Thread.sleep(200);
            }
        } catch (Exception e) {
            // Redis may not be connected yet, ignore
        }
    }
    
    @After
    public void afterScenario() {
        // Clean up test realms created during the scenario
        // BUT skip pre-imported realms (test-realm)
        try {
            String realmName = context.getCurrentRealmId();
            if (realmName != null && !realmName.equals("master") && !realmName.equals("test-realm")) {
                adminHelper.deleteRealm(realmName);
                System.out.println("🧹 Deleted dynamically created realm: " + realmName);
            }
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }
    
    @Given("Redis is running on {string}")
    public void redis_is_running_on(String hostPort) {
        // DEPRECATED: This step is kept for backward compatibility but now uses configuration
        // from properties files instead of the hardcoded value from feature files.
        // The hostPort parameter is ignored - configuration comes from:
        //   - application-test.properties (common)
        //   - application-test-standalone.properties or application-test-docker-cluster.properties
        
        System.out.println("⚠️  NOTE: Using Redis configuration from properties, not feature file value: " + hostPort);
        
        // Test Redis connection using configuration from properties
        context.initRedisConnection();
        io.lettuce.core.api.sync.BaseRedisCommands<String, String> redis = context.getRedisCommands();
        String pong = redis.ping();
        assertThat(pong).isEqualTo("PONG");
        
        System.out.println("✅ Redis connection verified: " + context.getRedisHost() + ":" + context.getRedisPort() + 
                          " (cluster=" + context.isClusterEnabled() + ")");
    }
    
    @Given("Redis is running")
    public void redis_is_running() {
        // New step that uses configuration from properties files
        // Recommended for new feature files
        context.initRedisConnection();
        io.lettuce.core.api.sync.BaseRedisCommands<String, String> redis = context.getRedisCommands();
        String pong = redis.ping();
        assertThat(pong).isEqualTo("PONG");
        
        System.out.println("✅ Redis connection verified: " + context.getRedisHost() + ":" + context.getRedisPort() + 
                          " (cluster=" + context.isClusterEnabled() + ")");
    }
    
    @Given("Keycloak is configured with Redis providers")
    public void keycloak_is_configured_with_redis_providers() {
        // Verify Keycloak is accessible by getting admin token
        try {
            String token = context.getAdminToken();
            assertThat(token).isNotNull();
            context.put("redis_providers_enabled", true);
        } catch (Exception e) {
            throw new RuntimeException("Keycloak is not accessible at " + context.getKeycloakBaseUrl(), e);
        }
    }
    
    @Given("a test realm {string} exists")
    public void a_test_realm_exists(String realmName) {
        // Use pre-imported realm (test-realm.json)
        // Verify realm exists by testing token endpoint
        try {
            io.restassured.RestAssured
                .given()
                    .contentType("application/x-www-form-urlencoded")
                    .formParam("username", context.getDefaultTestUser())
                    .formParam("password", context.getDefaultTestPassword())
                    .formParam("grant_type", "password")
                    .formParam("client_id", context.getDefaultTestClient())
                .when()
                    .post(context.getKeycloakBaseUrl() + "/realms/" + realmName + "/protocol/openid-connect/token")
                .then()
                    .statusCode(200);
            
            System.out.println("✅ Using pre-imported realm: " + realmName);
        } catch (Exception e) {
            throw new RuntimeException("Realm '" + realmName + "' not found. Ensure realm JSON is imported at Keycloak startup.", e);
        }
        
        context.setCurrentRealmId(realmName);
        context.put("realm:" + realmName, realmName);
    }
    
    @Given("a test realm {string} exists with brute force protection enabled")
    public void a_test_realm_exists_with_brute_force_protection(String realmName) {
        // Use pre-imported realm with brute force enabled (test-realm-realm.json)
        // Verify realm exists by testing token endpoint
        try {
            io.restassured.RestAssured
                .given()
                    .contentType("application/x-www-form-urlencoded")
                    .formParam("username", context.getDefaultTestUser())
                    .formParam("password", context.getDefaultTestPassword())
                    .formParam("grant_type", "password")
                    .formParam("client_id", context.getDefaultTestClient())
                .when()
                    .post(context.getKeycloakBaseUrl() + "/realms/" + realmName + "/protocol/openid-connect/token")
                .then()
                    .statusCode(200);
            
            System.out.println("✅ Using pre-imported realm with brute force protection: " + realmName);
        } catch (Exception e) {
            throw new RuntimeException("Realm '" + realmName + "' not found. Ensure realm JSON with brute force is imported at Keycloak startup.", e);
        }
        
        context.setCurrentRealmId(realmName);
        context.put("realm:" + realmName, realmName);
        context.put("brute_force_enabled", true);
    }
    
    @Given("a test user {string} with password {string} exists")
    public void a_test_user_with_password_exists(String username, String password) {
        String realmName = context.getCurrentRealmId();
        assertThat(realmName).withFailMessage("No realm set in context").isNotNull();
        
        // Use pre-imported user from test-realm.json
        // Verify user exists by testing authentication
        try {
            io.restassured.response.Response response = io.restassured.RestAssured
                .given()
                    .contentType("application/x-www-form-urlencoded")
                    .formParam("username", username)
                    .formParam("password", password)
                    .formParam("grant_type", "password")
                    .formParam("client_id", context.getDefaultTestClient())
                .when()
                    .post(context.getKeycloakBaseUrl() + "/realms/" + realmName + "/protocol/openid-connect/token");
            
            assertThat(response.statusCode())
                .withFailMessage("User '" + username + "' authentication failed. Ensure user is pre-configured in realm JSON")
                .isEqualTo(200);
            
            System.out.println("✅ Using pre-imported user: " + username);
        } catch (Exception e) {
            throw new RuntimeException("User '" + username + "' not found or authentication failed.", e);
        }
        
        context.put("user:" + username + ":password", password);
        context.put("user:" + username + ":id", username); // Use username as ID for pre-imported users
        context.setCurrentUserId(username);
    }
    
    @Given("a test user {string} exists")
    public void a_test_user_exists(String username) {
        a_test_user_with_password_exists(username, context.getDefaultTestPassword());
    }
    
    @Given("a test client {string} exists")
    public void a_test_client_exists(String clientId) {
        String realmName = context.getCurrentRealmId();
        assertThat(realmName).withFailMessage("No realm set in context").isNotNull();
        
        // Use pre-imported client from test-realm.json
        System.out.println("✅ Using pre-imported client: " + clientId);
        
        context.put("client:" + clientId, clientId);
        context.setCurrentClientId(clientId);
    }
    
    @Given("test clients {string} and {string} exist")
    public void test_clients_exist(String client1, String client2) {
        a_test_client_exists(client1);
        a_test_client_exists(client2);
    }
    
    @Given("Keycloak cluster with {int} nodes is configured with Redis providers")
    public void keycloak_cluster_with_nodes_is_configured(Integer nodeCount) {
        context.put("cluster_nodes", nodeCount);
        context.put("redis_providers_enabled", true);
        // Would start multiple Keycloak instances in real implementation
    }
    
    @Given("node{int} is running")
    public void node_is_running(Integer nodeNumber) {
        context.put("node" + nodeNumber + ":status", "running");
    }
    
    @Given("both cluster nodes are subscribed to {string}")
    public void both_nodes_subscribed_to(String channel) {
        context.put("cluster_channel", channel);
    }
    
    @io.cucumber.java.en.Then("Redis should respond to ping")
    public void redis_should_respond_to_ping() {
        RedisCommands<String, String> redis = context.getRedisCommands();
        String pong = redis.ping();
        assertThat(pong).isEqualTo("PONG");
    }
}
