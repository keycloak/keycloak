/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.connections.httpclient;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * Comprehensive tests for RetryConfig class.
 */
public class RetryConfigTest {

    @Test
    public void testDefaultValues() {
        RetryConfig config = new RetryConfig.Builder().build();
        assertEquals(0, config.getMaxRetries());
        assertEquals(1000, config.getInitialBackoffMillis());
        assertEquals(2.0, config.getBackoffMultiplier(), 0.001);
        assertEquals(10000, config.getConnectionTimeoutMillis());
        assertEquals(10000, config.getSocketTimeoutMillis());
    }

    @Test
    public void testCustomValues() {
        RetryConfig config = new RetryConfig.Builder()
                .maxRetries(5)
                .initialBackoffMillis(2000)
                .backoffMultiplier(3.0)
                .connectionTimeoutMillis(15000)
                .socketTimeoutMillis(20000)
                .build();

        assertEquals(5, config.getMaxRetries());
        assertEquals(2000, config.getInitialBackoffMillis());
        assertEquals(3.0, config.getBackoffMultiplier(), 0.001);
        assertEquals(15000, config.getConnectionTimeoutMillis());
        assertEquals(20000, config.getSocketTimeoutMillis());
    }

    @Test
    public void testZeroRetries() {
        RetryConfig config = new RetryConfig.Builder()
                .maxRetries(0)
                .build();

        assertEquals(0, config.getMaxRetries());
    }

    @Test
    public void testNegativeRetries() {
        // Negative values should be allowed (though they don't make practical sense)
        // This tests that the builder doesn't enforce any validation
        RetryConfig config = new RetryConfig.Builder()
                .maxRetries(-1)
                .build();

        assertEquals(-1, config.getMaxRetries());
    }

    @Test
    public void testLargeNumberOfRetries() {
        // Test with a large number of retries
        RetryConfig config = new RetryConfig.Builder()
                .maxRetries(Integer.MAX_VALUE)
                .build();

        assertEquals(Integer.MAX_VALUE, config.getMaxRetries());
    }

    @Test
    public void testBuilderChaining() {
        // Test that builder methods can be chained
        RetryConfig config = new RetryConfig.Builder()
                .maxRetries(10)
                .build();

        assertEquals(10, config.getMaxRetries());
    }

    @Test
    public void testBuilderOverriding() {
        // Test that later builder calls override earlier ones
        RetryConfig config = new RetryConfig.Builder()
                .maxRetries(5)
                .maxRetries(10)
                .initialBackoffMillis(500)
                .initialBackoffMillis(1500)
                .backoffMultiplier(1.5)
                .backoffMultiplier(2.5)
                .connectionTimeoutMillis(5000)
                .connectionTimeoutMillis(8000)
                .socketTimeoutMillis(12000)
                .socketTimeoutMillis(15000)
                .build();

        assertEquals(10, config.getMaxRetries());
        assertEquals(1500, config.getInitialBackoffMillis());
        assertEquals(2.5, config.getBackoffMultiplier(), 0.001);
        assertEquals(8000, config.getConnectionTimeoutMillis());
        assertEquals(15000, config.getSocketTimeoutMillis());
    }

    @Test
    public void testExponentialBackoffSettings() {
        // Test specific exponential backoff settings
        RetryConfig config = new RetryConfig.Builder()
                .initialBackoffMillis(100) // Very short initial backoff
                .backoffMultiplier(4.0) // Aggressive multiplier
                .build();

        assertEquals(100, config.getInitialBackoffMillis());
        assertEquals(4.0, config.getBackoffMultiplier(), 0.001);
    }

    @Test
    public void testTimeoutSettings() {
        // Test specific timeout settings
        RetryConfig config = new RetryConfig.Builder()
                .connectionTimeoutMillis(30000) // 30 seconds connection timeout
                .socketTimeoutMillis(60000) // 60 seconds socket timeout
                .build();

        assertEquals(30000, config.getConnectionTimeoutMillis());
        assertEquals(60000, config.getSocketTimeoutMillis());
    }

    @Test
    public void testJitterSettings() {
        // Test jitter settings
        RetryConfig config = new RetryConfig.Builder()
                .useJitter(true)
                .jitterFactor(0.3)
                .build();

        assertTrue(config.isUseJitter());
        assertEquals(0.3, config.getJitterFactor(), 0.001);
    }

    @Test
    public void testDisableJitter() {
        // Test disabling jitter
        RetryConfig config = new RetryConfig.Builder()
                .useJitter(false)
                .build();

        assertFalse(config.isUseJitter());
        assertEquals(0.5, config.getJitterFactor(), 0.001); // Default value should still be set
    }

    @Test
    public void testEqualsWithSameValues() {
        RetryConfig config1 = new RetryConfig.Builder()
                .maxRetries(5)
                .initialBackoffMillis(2000)
                .backoffMultiplier(3.0)
                .useJitter(true)
                .jitterFactor(0.5)
                .connectionTimeoutMillis(15000)
                .socketTimeoutMillis(20000)
                .build();

        RetryConfig config2 = new RetryConfig.Builder()
                .maxRetries(5)
                .initialBackoffMillis(2000)
                .backoffMultiplier(3.0)
                .useJitter(true)
                .jitterFactor(0.5)
                .connectionTimeoutMillis(15000)
                .socketTimeoutMillis(20000)
                .build();

        assertEquals(config1, config2);
        assertEquals(config2, config1);
        assertEquals(config1.hashCode(), config2.hashCode());
    }

    @Test
    public void testEqualsWithNull() {
        RetryConfig config = new RetryConfig.Builder().build();
        assertNotEquals(config, null);
    }

    @Test
    public void testEqualsSameInstance() {
        RetryConfig config = new RetryConfig.Builder().build();
        assertEquals(config, config);
    }
}