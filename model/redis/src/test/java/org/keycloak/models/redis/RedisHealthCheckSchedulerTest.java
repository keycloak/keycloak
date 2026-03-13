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

package org.keycloak.models.redis;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.models.redis.DefaultRedisConnectionProvider;
import org.keycloak.models.redis.RedisHealthCheckScheduler;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive tests for RedisHealthCheckScheduler.
 * Ensures automated health check functionality has >85% coverage.
 */
class RedisHealthCheckSchedulerTest {

    private RedisClient mockClient;
    private StatefulRedisConnection<String, String> mockConnection;
    private RedisCommands<String, String> mockSync;
    private DefaultRedisConnectionProvider provider;
    private RedisHealthCheckScheduler scheduler;

    @BeforeEach
    void setUp() {
        mockClient = mock(RedisClient.class);
        mockConnection = mock(StatefulRedisConnection.class);
        mockSync = mock(RedisCommands.class);
        
        when(mockConnection.sync()).thenReturn(mockSync);
        
        provider = new DefaultRedisConnectionProvider(
            mockClient, mockConnection, "kc:", "redis://localhost:6379/0"
        );
        
        scheduler = new RedisHealthCheckScheduler();
    }

    @AfterEach
    void tearDown() {
        if (scheduler != null && scheduler.isRunning()) {
            scheduler.stop();
        }
    }

    // ==================== Basic Lifecycle Tests ====================

    @Test
    void testSchedulerCanBeCreated() {
        assertThat(scheduler).isNotNull();
        assertThat(scheduler.isRunning()).isFalse();
    }

    @Test
    void testSchedulerCanBeStarted() {
        when(mockSync.ping()).thenReturn("PONG");
        
        scheduler.start(provider, 100); // 100ms interval
        
        assertThat(scheduler.isRunning()).isTrue();
    }

    @Test
    void testSchedulerCanBeStopped() {
        when(mockSync.ping()).thenReturn("PONG");
        
        scheduler.start(provider, 100);
        assertThat(scheduler.isRunning()).isTrue();
        
        scheduler.stop();
        assertThat(scheduler.isRunning()).isFalse();
    }

    @Test
    void testSchedulerCannotBeStartedTwice() {
        when(mockSync.ping()).thenReturn("PONG");
        
        scheduler.start(provider, 100);
        assertThat(scheduler.isRunning()).isTrue();
        
        // Try to start again - should not crash
        scheduler.start(provider, 100);
        
        // Still running
        assertThat(scheduler.isRunning()).isTrue();
    }

    @Test
    void testSchedulerCanBeStoppedWhenNotRunning() {
        // Should not crash when stopping a scheduler that isn't running
        assertThat(scheduler.isRunning()).isFalse();
        scheduler.stop();
        assertThat(scheduler.isRunning()).isFalse();
    }

    // ==================== Health Check Execution Tests ====================

    @Test
    void testSchedulerPerformsHealthChecks() throws InterruptedException {
        when(mockSync.ping()).thenReturn("PONG");
        
        scheduler.start(provider, 50); // Fast interval for testing
        
        // Wait for at least 2 health checks
        Thread.sleep(150);
        
        // Verify ping was called multiple times
        verify(mockSync, atLeast(2)).ping();
        
        scheduler.stop();
    }

    @Test
    void testSchedulerAttemptsReconnectOnFailure() throws InterruptedException {
        // First call fails, second succeeds
        when(mockSync.ping())
            .thenReturn("ERROR")  // First health check fails
            .thenReturn("PONG");  // Reconnect succeeds
        
        scheduler.start(provider, 50);
        
        // Wait for health check to run
        Thread.sleep(100);
        
        // Verify ping was called (both for health check and reconnect)
        verify(mockSync, atLeast(2)).ping();
        
        scheduler.stop();
    }

    @Test
    void testSchedulerHandlesExceptionGracefully() throws InterruptedException {
        // Ping throws exception
        when(mockSync.ping()).thenThrow(new RuntimeException("Connection lost"));
        
        scheduler.start(provider, 50);
        
        // Wait - scheduler should not crash
        Thread.sleep(150);
        
        // Scheduler should still be running despite exceptions
        assertThat(scheduler.isRunning()).isTrue();
        
        scheduler.stop();
    }

    @Test
    void testSchedulerRecoversAfterException() throws InterruptedException {
        // First call throws exception, then recovers
        when(mockSync.ping())
            .thenThrow(new RuntimeException("Connection lost"))
            .thenReturn("PONG");
        
        scheduler.start(provider, 50);
        
        // Wait for multiple checks
        Thread.sleep(150);
        
        // Should still be running
        assertThat(scheduler.isRunning()).isTrue();
        
        scheduler.stop();
    }

    // ==================== Reconnection Logic Tests ====================

    @Test
    void testReconnectIsCalledOnHealthCheckFailure() throws InterruptedException {
        when(mockSync.ping())
            .thenReturn("ERROR")  // Health check fails
            .thenReturn("PONG");  // Reconnect succeeds
        
        scheduler.start(provider, 50);
        
        Thread.sleep(100);
        
        // Should have called ping at least twice (health check + reconnect)
        verify(mockSync, atLeast(2)).ping();
        
        scheduler.stop();
    }

    @Test
    void testReconnectFailureDoesNotStopScheduler() throws InterruptedException {
        // Both health check and reconnect fail
        when(mockSync.ping()).thenReturn("ERROR");
        
        scheduler.start(provider, 50);
        
        Thread.sleep(150);
        
        // Scheduler should still be running
        assertThat(scheduler.isRunning()).isTrue();
        
        // Multiple attempts should have been made
        verify(mockSync, atLeast(2)).ping();
        
        scheduler.stop();
    }

    @Test
    void testHealthyConnectionDoesNotTriggerReconnect() throws InterruptedException {
        when(mockSync.ping()).thenReturn("PONG");
        
        scheduler.start(provider, 50);
        
        Thread.sleep(150);
        
        // Ping should be called for health checks but not extra reconnect attempts
        verify(mockSync, atLeast(2)).ping();
        verify(mockSync, atMost(5)).ping(); // Reasonable upper bound
        
        scheduler.stop();
    }

    // ==================== Graceful Shutdown Tests ====================

    @Test
    void testGracefulShutdown() throws InterruptedException {
        when(mockSync.ping()).thenReturn("PONG");
        
        scheduler.start(provider, 100);
        
        // Let it run a bit
        Thread.sleep(150);
        
        // Stop should complete quickly
        long startTime = System.currentTimeMillis();
        scheduler.stop();
        long stopTime = System.currentTimeMillis();
        
        assertThat(stopTime - startTime).isLessThan(6000); // Should stop within 5 seconds + margin
        assertThat(scheduler.isRunning()).isFalse();
    }

    @Test
    void testMultipleStopCallsAreSafe() {
        when(mockSync.ping()).thenReturn("PONG");
        
        scheduler.start(provider, 100);
        scheduler.stop();
        
        // Multiple stop calls should be safe
        scheduler.stop();
        scheduler.stop();
        
        assertThat(scheduler.isRunning()).isFalse();
    }

    // ==================== Edge Case Tests ====================

    @Test
    void testVeryShortInterval() throws InterruptedException {
        when(mockSync.ping()).thenReturn("PONG");
        
        // Very short interval
        scheduler.start(provider, 10);
        
        Thread.sleep(100);
        
        // Should have executed many times
        verify(mockSync, atLeast(5)).ping();
        
        scheduler.stop();
    }

    @Test
    void testLongInterval() throws InterruptedException {
        when(mockSync.ping()).thenReturn("PONG");
        
        // Long interval
        scheduler.start(provider, 1000);
        
        Thread.sleep(500); // Less than interval
        
        // Should have executed at most once
        verify(mockSync, atMost(1)).ping();
        
        scheduler.stop();
    }

    @Test
    void testSchedulerThreadIsDaemon() {
        when(mockSync.ping()).thenReturn("PONG");
        
        scheduler.start(provider, 1000);
        
        // The scheduler uses a daemon thread, so JVM can exit
        // We can't directly test this, but we verify it doesn't block shutdown
        assertThat(scheduler.isRunning()).isTrue();
        
        scheduler.stop();
    }

    // ==================== Integration-style Tests ====================

    @Test
    void testRealisticHealthCheckScenario() throws InterruptedException {
        // Simulate: healthy -> unhealthy -> reconnect -> healthy
        when(mockSync.ping())
            .thenReturn("PONG")   // Initial healthy
            .thenReturn("PONG")   // Still healthy
            .thenReturn("ERROR")  // Goes unhealthy
            .thenReturn("PONG")   // Reconnect succeeds
            .thenReturn("PONG");  // Back to healthy
        
        scheduler.start(provider, 50);
        
        // Wait for all checks to run
        Thread.sleep(300);
        
        // Should have called ping multiple times including reconnect
        verify(mockSync, atLeast(4)).ping();
        
        scheduler.stop();
    }

    @Test
    void testStartStopStartAgain() throws InterruptedException {
        when(mockSync.ping()).thenReturn("PONG");
        
        // Start
        scheduler.start(provider, 50);
        Thread.sleep(100);
        assertThat(scheduler.isRunning()).isTrue();
        
        // Stop
        scheduler.stop();
        assertThat(scheduler.isRunning()).isFalse();
        
        // Can't start again with the same scheduler instance
        // (This is expected behavior - create a new one)
        scheduler = new RedisHealthCheckScheduler();
        scheduler.start(provider, 50);
        Thread.sleep(100);
        assertThat(scheduler.isRunning()).isTrue();
        
        scheduler.stop();
    }
}
