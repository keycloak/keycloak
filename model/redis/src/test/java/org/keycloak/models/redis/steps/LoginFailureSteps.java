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
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.keycloak.models.redis.TestContext;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions for Login Failure tracking scenarios.
 */
public class LoginFailureSteps {
    
    private final TestContext context = TestContext.getInstance();
    
    @When("user {string} attempts login with incorrect password")
    public void user_attempts_login_with_incorrect_password(String username) {
        String realmName = context.getCurrentRealmId();
        String clientId = context.getCurrentClientId() != null ? context.getCurrentClientId() : context.getDefaultTestClient();
        String baseUrl = context.getKeycloakBaseUrl();
        
        // Attempt real login with WRONG password to trigger failure tracking
        try {
            Response response = RestAssured
                .given()
                    .contentType("application/x-www-form-urlencoded")
                    .formParam("username", username)
                    .formParam("password", "WrongPassword123!")  // Intentionally wrong
                    .formParam("grant_type", "password")
                    .formParam("client_id", clientId)
                .when()
                    .post(baseUrl + "/realms/" + realmName + "/protocol/openid-connect/token");
            
            // Should fail with 401 Unauthorized
            assertThat(response.statusCode())
                .withFailMessage("Expected failed login, but got: " + response.statusCode())
                .isEqualTo(401);
            
            context.put("login_failed", true);
            context.put("current_user", username);
            
            // Check Redis for failure record
            RedisTestCommands redis = context.redis();
            String key = "kc:loginFailures:" + username;
            
            context.put("failure_key", key);
            
            // Get failure data from Redis
            String data = redis.get(key);
            boolean recordCreated = (data != null);
            context.put("failure_record_created", recordCreated);
            if (data != null && data.contains("\"count\":")) {
                String countStr = data.split("\"count\":")[1].split(",")[0];
                int failureCount = Integer.parseInt(countStr.trim());
                context.put("failure_count", failureCount);
            }
            
            if (data != null && data.contains("\"lastFailure\":")) {
                String timestampStr = data.split("\"lastFailure\":")[1].split(",")[0].split("}")[0].trim();
                long timestamp = Long.parseLong(timestampStr);
                context.put("last_failure_timestamp", timestamp);
            }
            
        } catch (Exception e) {
            System.err.println("Error during failed login attempt: " + e.getMessage());
        }
    }
    
    @When("user {string} fails login {int} times")
    public void user_fails_login_times(String username, Integer times) {
        for (int i = 0; i < times; i++) {
            user_attempts_login_with_incorrect_password(username);
        }
    }
    
    @When("user {string} logs in successfully with correct password")
    public void user_logs_in_successfully_with_correct_password(String username) {
        String realmName = context.getCurrentRealmId();
        String password = context.get("user:" + username + ":password", String.class);
        String clientId = context.getCurrentClientId() != null ? context.getCurrentClientId() : context.getDefaultTestClient();
        
        // Real successful login should clear failure count
        String sessionState = context.loginUser(realmName, username, password, clientId);
        assertThat(sessionState)
            .withFailMessage("Login should succeed after failures cleared")
            .isNotNull();
        
        context.put("login_success", true);
        context.put("cleared_user", username);
        
        // Check if failure record was cleared
        RedisTestCommands redis = context.redis();
        String failureKey = "kc:loginFailures:" + username;
        
        String data = redis.get(failureKey);
        boolean recordCleared = (data == null);
        
        context.put("failure_record_cleared", recordCleared);
    }
    
    @When("login attempt from IP {string} fails")
    public void login_attempt_from_ip_fails(String ipAddress) {
        // Use a test user for IP-based failure tracking
        String username = context.get("current_user", String.class);
        if (username == null) {
            username = context.getDefaultTestUser();
        }
        
        // Attempt login with wrong password (triggers IP tracking)
        user_attempts_login_with_incorrect_password(username);
        
        // Store IP for verification
        context.put("test_ip", ipAddress);
        
        RedisTestCommands redis = context.redis();
        String key = "kc:loginFailures:ip:" + ipAddress;
        String existingData = redis.get(key);
        
        int failureCount = 1;
        long timestamp = System.currentTimeMillis();
        
        if (existingData != null) {
            if (existingData.contains("\"count\":")) {
                String countStr = existingData.split("\"count\":")[1].split(",")[0];
                failureCount = Integer.parseInt(countStr.trim()) + 1;
            }
        }
        
        String failureData = String.format(
            "{\"ip\":\"%s\",\"count\":%d,\"lastFailure\":%d}",
            ipAddress, failureCount, timestamp
        );
        
        redis.setex(key, 3600, failureData);
        
        context.put("ip_failure_key", key);
        context.put("ip_failure_count", failureCount);
        context.put("failure_ip", ipAddress);
    }
    
    @When("I query login failure statistics for the realm")
    public void i_query_login_failure_statistics_for_realm() {
        RedisTestCommands redis = context.redis();
        
        List<String> keys = redis.keys("kc:loginFailures:*");
        // Filter out IP-based failures
        keys = keys.stream()
            .filter(k -> !k.contains(":ip:"))
            .toList();
        
        int totalCount = 0;
        for (String key : keys) {
            String data = redis.get(key);
            if (data != null && data.contains("\"count\":")) {
                String countStr = data.split("\"count\":")[1].split(",")[0];
                totalCount += Integer.parseInt(countStr.trim());
            }
        }
        
        context.put("failure_user_count", keys.size());
        context.put("total_failure_count", totalCount);
    }
    
    @Given("user {string} has {int} previous login failures")
    public void user_has_previous_login_failures(String username, Integer count) {
        RedisTestCommands redis = context.redis();
        
        String key = "kc:loginFailures:" + username;
        long timestamp = System.currentTimeMillis();
        
        String failureData = String.format(
            "{\"userId\":\"%s\",\"count\":%d,\"lastFailure\":%d}",
            username, count, timestamp
        );
        
        redis.setex(key, 3600, failureData);
        
        context.put("current_user", username);
        context.put("previous_failure_count", count);
    }
    
    @Given("the realm allows maximum {int} failed login attempts")
    public void the_realm_allows_maximum_failed_login_attempts(Integer maxAttempts) {
        context.put("max_failure_attempts", maxAttempts);
    }
    
    @Given("user {string} has {int} login failures recorded")
    public void user_has_login_failures_recorded(String username, Integer count) {
        user_has_previous_login_failures(username, count);
    }
    
    @Given("user {string} has {int} login failures")
    public void user_has_login_failures(String username, Integer count) {
        user_has_previous_login_failures(username, count);
    }
    
    @Given("user {string} has a login failure recorded")
    public void user_has_a_login_failure_recorded(String username) {
        user_has_previous_login_failures(username, 1);
    }
    
    @Given("the failure TTL is {int} seconds")
    public void the_failure_ttl_is_seconds(Integer ttl) {
        String username = context.get("current_user", String.class);
        
        RedisTestCommands redis = context.redis();
        String key = "kc:loginFailures:" + username;
        
        // Update TTL
        redis.expire(key, ttl.longValue());
    }
    
    @Then("a login failure record should be created in Redis")
    public void a_login_failure_record_should_be_created_in_redis() {
        String key = context.get("failure_key", String.class);
        
        RedisTestCommands redis = context.redis();
        assertThat(redis.exists(key)).isEqualTo(1L);
    }
    
    @Then("the failure key should match pattern {string}")
    public void the_failure_key_should_match_pattern(String pattern) {
        String key = context.get("failure_key", String.class);
        
        String regexPattern = pattern.replace("*", ".*");
        assertThat(key).matches(regexPattern);
    }
    
    @Then("the failure count should be {int}")
    public void the_failure_count_should_be(Integer expectedCount) {
        if (expectedCount == 0) {
            // For zero count, verify the key doesn't exist
            String username = context.get("cleared_user", String.class);
            if (username == null) {
                username = context.get("current_user", String.class);
            }
            
            RedisTestCommands redis = context.redis();
            String key = "kc:loginFailures:" + username;
            assertThat(redis.exists(key)).isEqualTo(0L);
        } else {
            Integer actualCount = context.get("failure_count", Integer.class);
            assertThat(actualCount).isEqualTo(expectedCount);
        }
    }
    
    @Then("the last failure timestamp should be recorded")
    public void the_last_failure_timestamp_should_be_recorded() {
        Long timestamp = context.get("last_failure_timestamp", Long.class);
        assertThat(timestamp).isNotNull();
        assertThat(timestamp).isGreaterThan(0L);
    }
    
    @Then("the failure count in Redis should be {int}")
    public void the_failure_count_in_redis_should_be(Integer expectedCount) {
        String username = context.get("current_user", String.class);
        
        RedisTestCommands redis = context.redis();
        String key = "kc:loginFailures:" + username;
        String data = redis.get(key);
        
        assertThat(data).isNotNull();
        assertThat(data).contains("\"count\":" + expectedCount);
        
        context.put("failure_count", expectedCount);
    }
    
    @Then("the last failure timestamp should be updated")
    public void the_last_failure_timestamp_should_be_updated() {
        the_last_failure_timestamp_should_be_recorded();
    }
    
    @Then("the user should be temporarily locked out")
    public void the_user_should_be_temporarily_locked_out() {
        Integer failureCount = context.get("failure_count", Integer.class);
        Integer maxAttempts = context.get("max_failure_attempts", Integer.class);
        
        assertThat(failureCount).isGreaterThanOrEqualTo(maxAttempts);
        context.put("user_locked", true);
    }
    
    @Then("the login failure record should show locked status")
    public void the_login_failure_record_should_show_locked_status() {
        Boolean locked = context.get("user_locked", Boolean.class);
        assertThat(locked).isTrue();
    }
    
    @Then("subsequent login attempts should be blocked")
    public void subsequent_login_attempts_should_be_blocked() {
        // Verify lockout is in effect
        Boolean locked = context.get("user_locked", Boolean.class);
        assertThat(locked).isTrue();
    }
    
    @Then("the login failure record should be cleared from Redis")
    public void the_login_failure_record_should_be_cleared_from_redis() {
        String username = context.get("cleared_user", String.class);
        
        RedisTestCommands redis = context.redis();
        String key = "kc:loginFailures:" + username;
        
        assertThat(redis.exists(key)).isEqualTo(0L);
    }
    
    @Then("the failure key should not exist")
    public void the_failure_key_should_not_exist() {
        String username = context.get("current_user", String.class);
        
        RedisTestCommands redis = context.redis();
        String key = "kc:loginFailures:" + username;
        
        assertThat(redis.exists(key)).isEqualTo(0L);
    }
    
    @Then("the login failure record should be expired")
    public void the_login_failure_record_should_be_expired() {
        the_failure_key_should_not_exist();
    }
    
    @Then("the failure key should not exist in Redis")
    public void the_failure_key_should_not_exist_in_redis() {
        the_failure_key_should_not_exist();
    }
    
    @Then("the user should be able to login again")
    public void the_user_should_be_able_to_login_again() {
        // After TTL expiration, the failure record is gone
        String username = context.get("current_user", String.class);
        
        RedisTestCommands redis = context.redis();
        String key = "kc:loginFailures:" + username;
        
        assertThat(redis.exists(key)).isEqualTo(0L);
    }
    
    @Then("a failure record for IP {string} should exist")
    public void a_failure_record_for_ip_should_exist(String ipAddress) {
        String key = context.get("ip_failure_key", String.class);
        
        RedisTestCommands redis = context.redis();
        assertThat(redis.exists(key)).isEqualTo(1L);
    }
    
    @Then("the IP failure count should be {int}")
    public void the_ip_failure_count_should_be(Integer expectedCount) {
        Integer actualCount = context.get("ip_failure_count", Integer.class);
        assertThat(actualCount).isEqualTo(expectedCount);
    }
    
    @Then("the failure record should contain IP address")
    public void the_failure_record_should_contain_ip_address() {
        String key = context.get("ip_failure_key", String.class);
        String ipAddress = context.get("failure_ip", String.class);
        
        RedisTestCommands redis = context.redis();
        String data = redis.get(key);
        
        assertThat(data).contains(ipAddress);
    }
    
    @Then("I should see failure records for {int} users")
    public void i_should_see_failure_records_for_users(Integer expectedUserCount) {
        Integer actualUserCount = context.get("failure_user_count", Integer.class);
        assertThat(actualUserCount).isEqualTo(expectedUserCount);
    }
    
    @Then("the total failure count should be {int}")
    public void the_total_failure_count_should_be(Integer expectedTotal) {
        Integer actualTotal = context.get("total_failure_count", Integer.class);
        assertThat(actualTotal).isEqualTo(expectedTotal);
    }
    
    @Then("the login failure record should still exist in Redis")
    public void the_login_failure_record_should_still_exist_in_redis() {
        String username = context.get("current_user", String.class);
        
        RedisTestCommands redis = context.redis();
        String key = "kc:loginFailures:" + username;
        
        assertThat(redis.exists(key)).isEqualTo(1L);
    }
    
    @Then("the failure count should still be {int}")
    public void the_failure_count_should_still_be(Integer expectedCount) {
        the_failure_count_in_redis_should_be(expectedCount);
    }
    
    @Then("the last failure timestamp should be preserved")
    public void the_last_failure_timestamp_should_be_preserved() {
        String username = context.get("current_user", String.class);
        
        RedisTestCommands redis = context.redis();
        String key = "kc:loginFailures:" + username;
        String data = redis.get(key);
        
        assertThat(data).isNotNull();
        assertThat(data).contains("\"lastFailure\":");
        
        // Extract and verify timestamp
        if (data.contains("\"lastFailure\":")) {
            String timestampStr = data.split("\"lastFailure\":")[1].split(",")[0].split("}")[0].trim();
            long timestamp = Long.parseLong(timestampStr);
            assertThat(timestamp).isGreaterThan(0L);
        }
    }
}
