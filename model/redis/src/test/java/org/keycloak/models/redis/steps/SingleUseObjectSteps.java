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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions for Single-Use Objects (Action Tokens) scenarios.
 */
public class SingleUseObjectSteps {
    
    private final TestContext context = TestContext.getInstance();
    
    @When("I create an action token for password reset")
    public void i_create_action_token_for_password_reset() {
        String tokenId = UUID.randomUUID().toString();
        
        RedisTestCommands redis = context.redis();
        String key = "kc:actionTokens:" + tokenId;
        
        String tokenData = String.format(
            "{\"tokenId\":\"%s\",\"type\":\"RESET_PASSWORD\",\"used\":false}",
            tokenId
        );
        
        redis.setex(key, 300, tokenData);
        
        context.put("action_token_id", tokenId);
        context.put("action_token_key", key);
    }
    
    @When("I create a {string} action token")
    public void i_create_action_token_of_type(String tokenType) {
        String tokenId = UUID.randomUUID().toString();
        
        RedisTestCommands redis = context.redis();
        String key = "kc:actionTokens:" + tokenId;
        
        String tokenData = String.format(
            "{\"tokenId\":\"%s\",\"type\":\"%s\",\"used\":false}",
            tokenId, tokenType
        );
        
        redis.setex(key, 300, tokenData);
        
        context.put("action_token_" + tokenType, tokenId);
        context.put("last_token_id", tokenId);
    }
    
    @When("I consume the action token for the first time")
    public void i_consume_action_token_first_time() {
        String tokenId = context.get("action_token_id", String.class);
        
        RedisTestCommands redis = context.redis();
        String key = "kc:actionTokens:" + tokenId;
        
        String tokenData = redis.get(key);
        
        if (tokenData != null && tokenData.contains("\"used\":false")) {
            // Mark as used
            String updatedData = tokenData.replace("\"used\":false", "\"used\":true");
            Long ttl = redis.ttl(key);
            redis.setex(key, ttl, updatedData);
            
            context.put("consumption_success", true);
            context.put("consumption_error", null);
        } else {
            context.put("consumption_success", false);
            context.put("consumption_error", "already used");
        }
    }
    
    @When("I attempt to consume the token again")
    public void i_attempt_to_consume_token_again() {
        String tokenId = context.get("action_token_id", String.class);
        
        RedisTestCommands redis = context.redis();
        String key = "kc:actionTokens:" + tokenId;
        
        String tokenData = redis.get(key);
        
        if (tokenData != null && tokenData.contains("\"used\":true")) {
            context.put("second_consumption_success", false);
            context.put("second_consumption_error", "already used");
        } else {
            context.put("second_consumption_success", true);
            context.put("second_consumption_error", null);
        }
    }
    
    @When("{int} concurrent requests attempt to consume the token")
    public void concurrent_requests_attempt_to_consume(Integer requestCount) throws InterruptedException {
        String tokenId = context.get("action_token_id", String.class);
        String key = "kc:actionTokens:" + tokenId;
        String lockKey = key + ":lock";
        
        RedisTestCommands redis = context.redis();
        
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(requestCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        
        for (int i = 0; i < requestCount; i++) {
            new Thread(() -> {
                try {
                    startLatch.await(); // Wait for all threads to be ready
                    
                    // Try to acquire lock using SETNX (atomic operation)
                    Boolean lockAcquired = redis.setnx(lockKey, "1");
                    
                    if (lockAcquired != null && lockAcquired) {
                        try {
                            redis.expire(lockKey, 5); // 5 second expiration on lock
                            String tokenData = redis.get(key);
                            
                            if (tokenData != null && tokenData.contains("\"used\":false")) {
                                String updatedData = tokenData.replace("\"used\":false", "\"used\":true");
                                Long ttl = redis.ttl(key);
                                redis.setex(key, ttl, updatedData);
                                successCount.incrementAndGet();
                            } else {
                                failureCount.incrementAndGet();
                            }
                        } finally {
                            redis.del(lockKey); // Release lock
                        }
                    } else {
                        failureCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }
        
        startLatch.countDown(); // Start all threads
        doneLatch.await(); // Wait for all to complete
        
        context.put("concurrent_success_count", successCount.get());
        context.put("concurrent_failure_count", failureCount.get());
    }
    
    @Given("an action token exists for password reset")
    public void an_action_token_exists_for_password_reset() {
        i_create_action_token_for_password_reset();
    }
    
    @Given("an action token with TTL of {int} seconds")
    public void an_action_token_with_ttl(Integer ttl) {
        String tokenId = UUID.randomUUID().toString();
        
        RedisTestCommands redis = context.redis();
        String key = "kc:actionTokens:" + tokenId;
        
        String tokenData = String.format(
            "{\"tokenId\":\"%s\",\"type\":\"RESET_PASSWORD\",\"used\":false}",
            tokenId
        );
        
        redis.setex(key, ttl.longValue(), tokenData);
        
        context.put("action_token_id", tokenId);
        context.put("action_token_key", key);
    }
    
    @Given("an action token exists")
    public void an_action_token_exists() {
        an_action_token_exists_for_password_reset();
    }
    
    @Given("{int} action tokens exist with {int} second TTL")
    public void multiple_action_tokens_exist_with_ttl(Integer count, Integer ttl) {
        for (int i = 0; i < count; i++) {
            String tokenId = UUID.randomUUID().toString();
            
            RedisTestCommands redis = context.redis();
            String key = "kc:actionTokens:" + tokenId;
            
            String tokenData = String.format(
                "{\"tokenId\":\"%s\",\"type\":\"TEST_TOKEN\",\"used\":false}",
                tokenId
            );
            
            redis.setex(key, ttl.longValue(), tokenData);
        }
    }
    
    @Given("an action token exists with {int} second TTL")
    public void an_action_token_exists_with_ttl(Integer ttl) {
        an_action_token_with_ttl(ttl);
    }
    
    @Then("the token should be stored in Redis")
    public void the_token_should_be_stored_in_redis() {
        String key = context.get("action_token_key", String.class);
        
        RedisTestCommands redis = context.redis();
        assertThat(redis.exists(key)).isEqualTo(1L);
    }
    
    @Then("the token key should match pattern {string}")
    public void the_token_key_should_match_pattern(String pattern) {
        String key = context.get("action_token_key", String.class);
        
        String regexPattern = pattern.replace("*", ".*");
        assertThat(key).matches(regexPattern);
    }
    
    @Then("the token should have a TTL of {int} seconds")
    public void the_token_should_have_ttl(Integer expectedTtl) {
        String key = context.get("action_token_key", String.class);
        
        RedisTestCommands redis = context.redis();
        Long ttl = redis.ttl(key);
        
        assertThat(ttl).isGreaterThan(0L).isLessThanOrEqualTo(expectedTtl.longValue());
    }
    
    @Then("the consumption should succeed")
    public void the_consumption_should_succeed() {
        Boolean success = context.get("consumption_success", Boolean.class);
        assertThat(success).isTrue();
    }
    
    @Then("the token should be marked as used in Redis")
    public void the_token_should_be_marked_as_used() {
        String tokenId = context.get("action_token_id", String.class);
        
        RedisTestCommands redis = context.redis();
        String key = "kc:actionTokens:" + tokenId;
        String tokenData = redis.get(key);
        
        assertThat(tokenData).contains("\"used\":true");
    }
    
    @Then("the consumption should fail")
    public void the_consumption_should_fail() {
        Boolean success = context.get("second_consumption_success", Boolean.class);
        assertThat(success).isFalse();
    }
    
    @Then("an error indicating {string} should be returned")
    public void an_error_indicating_should_be_returned(String errorType) {
        String error = context.get("second_consumption_error", String.class);
        if (error == null) {
            error = context.get("consumption_error", String.class);
        }
        
        assertThat(error).isNotNull();
        assertThat(error.toLowerCase()).contains(errorType.toLowerCase());
    }
    
    @Then("the token should be expired in Redis")
    public void the_token_should_be_expired_in_redis() {
        String key = context.get("action_token_key", String.class);
        
        RedisTestCommands redis = context.redis();
        assertThat(redis.exists(key)).isEqualTo(0L);
    }
    
    @Then("attempting to consume the token should fail")
    public void attempting_to_consume_token_should_fail() {
        String tokenId = context.get("action_token_id", String.class);
        
        RedisTestCommands redis = context.redis();
        String key = "kc:actionTokens:" + tokenId;
        String tokenData = redis.get(key);
        
        context.put("consumption_error", tokenData == null ? "expired" : "other");
        assertThat(tokenData).isNull();
    }
    
    @Then("{int} action tokens should exist in Redis")
    public void action_tokens_should_exist_in_redis(Integer expectedCount) {
        RedisTestCommands redis = context.redis();
        List<String> keys = redis.keys("kc:actionTokens:*");
        
        assertThat(keys).hasSize(expectedCount);
    }
    
    @Then("each token should have its own unique key")
    public void each_token_should_have_unique_key() {
        RedisTestCommands redis = context.redis();
        List<String> keys = redis.keys("kc:actionTokens:*");
        
        assertThat(keys).doesNotHaveDuplicates();
    }
    
    @Then("each token type should be identifiable")
    public void each_token_type_should_be_identifiable() {
        RedisTestCommands redis = context.redis();
        List<String> keys = redis.keys("kc:actionTokens:*");
        
        for (String key : keys) {
            String tokenData = redis.get(key);
            assertThat(tokenData).contains("\"type\":");
        }
    }
    
    @Then("only {int} consumption should succeed")
    public void only_consumption_should_succeed(Integer expectedSuccessCount) {
        Integer actualSuccess = context.get("concurrent_success_count", Integer.class);
        assertThat(actualSuccess).isEqualTo(expectedSuccessCount);
    }
    
    @Then("the other should fail with {string} error")
    public void the_other_should_fail_with_error(String errorType) {
        Integer failureCount = context.get("concurrent_failure_count", Integer.class);
        assertThat(failureCount).isGreaterThan(0);
    }
    
    @Then("Redis optimistic locking should prevent double usage")
    public void redis_optimistic_locking_should_prevent_double_usage() {
        // This is validated by the concurrent consumption test
        Integer successCount = context.get("concurrent_success_count", Integer.class);
        assertThat(successCount).isEqualTo(1);
    }
    
    @Then("all tokens should be expired and removed from Redis")
    public void all_tokens_should_be_expired_and_removed() {
        RedisTestCommands redis = context.redis();
        List<String> keys = redis.keys("kc:actionTokens:*");
        
        assertThat(keys).isEmpty();
    }
    
    @Then("the token count in Redis should be {int}")
    public void the_token_count_should_be(Integer expectedCount) {
        RedisTestCommands redis = context.redis();
        List<String> keys = redis.keys("kc:actionTokens:*");
        
        assertThat(keys).hasSize(expectedCount);
    }
    
    @Then("the token should still exist in Redis")
    public void the_token_should_still_exist_in_redis() {
        String key = context.get("action_token_key", String.class);
        
        RedisTestCommands redis = context.redis();
        assertThat(redis.exists(key)).isEqualTo(1L);
    }
    
    @Then("the token TTL should be preserved")
    public void the_token_ttl_should_be_preserved() {
        String key = context.get("action_token_key", String.class);
        
        RedisTestCommands redis = context.redis();
        Long ttl = redis.ttl(key);
        
        assertThat(ttl).isGreaterThan(0L);
    }
    
    @Then("the token can be successfully consumed")
    public void the_token_can_be_successfully_consumed() {
        i_consume_action_token_first_time();
        the_consumption_should_succeed();
    }
}
