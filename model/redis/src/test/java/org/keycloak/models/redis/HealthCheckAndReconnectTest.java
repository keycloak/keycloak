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
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.sync.RedisAdvancedClusterCommands;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.models.redis.DefaultRedisConnectionProvider;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive tests for health check and reconnection functionality.
 * Covers ping() and reconnect() methods.
 */
class HealthCheckAndReconnectTest {

    private RedisClient mockClient;
    private StatefulRedisConnection<String, String> mockConnection;
    private RedisCommands<String, String> mockSync;
    private DefaultRedisConnectionProvider provider;

    @BeforeEach
    void setUp() {
        mockClient = mock(RedisClient.class);
        mockConnection = mock(StatefulRedisConnection.class);
        mockSync = mock(RedisCommands.class);
        
        when(mockConnection.sync()).thenReturn(mockSync);
        
        provider = new DefaultRedisConnectionProvider(
            mockClient, mockConnection, "kc:", "redis://localhost:6379/0"
        );
    }

    // ==================== ping() Tests ====================

    @Test
    void testPing_Success_Standalone() {
        when(mockSync.ping()).thenReturn("PONG");
        
        boolean result = provider.ping();
        
        assertThat(result).isTrue();
        verify(mockSync).ping();
    }

    @Test
    void testPing_Success_CaseInsensitive() {
        when(mockSync.ping()).thenReturn("pong"); // lowercase
        
        boolean result = provider.ping();
        
        assertThat(result).isTrue();
    }

    @Test
    void testPing_Failure_WrongResponse() {
        when(mockSync.ping()).thenReturn("ERROR");
        
        boolean result = provider.ping();
        
        assertThat(result).isFalse();
    }

    @Test
    void testPing_Failure_NullResponse() {
        when(mockSync.ping()).thenReturn(null);
        
        boolean result = provider.ping();
        
        assertThat(result).isFalse();
    }

    @Test
    void testPing_Failure_Exception() {
        when(mockSync.ping()).thenThrow(new RuntimeException("Connection lost"));
        
        boolean result = provider.ping();
        
        assertThat(result).isFalse();
    }

    @Test
    void testPing_Success_ClusterMode() {
        RedisClusterClient mockClusterClient = mock(RedisClusterClient.class);
        StatefulRedisClusterConnection<String, String> mockClusterConn = mock(StatefulRedisClusterConnection.class);
        RedisAdvancedClusterCommands<String, String> mockClusterSync = mock(RedisAdvancedClusterCommands.class);
        
        when(mockClusterConn.sync()).thenReturn(mockClusterSync);
        when(mockClusterSync.ping()).thenReturn("PONG");
        
        DefaultRedisConnectionProvider clusterProvider = new DefaultRedisConnectionProvider(
            mockClusterClient, mockClusterConn, "kc:", "redis://localhost:6379/0"
        );
        
        boolean result = clusterProvider.ping();
        
        assertThat(result).isTrue();
        verify(mockClusterSync).ping();
    }

    @Test
    void testPing_Failure_ClusterMode() {
        RedisClusterClient mockClusterClient = mock(RedisClusterClient.class);
        StatefulRedisClusterConnection<String, String> mockClusterConn = mock(StatefulRedisClusterConnection.class);
        RedisAdvancedClusterCommands<String, String> mockClusterSync = mock(RedisAdvancedClusterCommands.class);
        
        when(mockClusterConn.sync()).thenReturn(mockClusterSync);
        when(mockClusterSync.ping()).thenThrow(new RuntimeException("Cluster down"));
        
        DefaultRedisConnectionProvider clusterProvider = new DefaultRedisConnectionProvider(
            mockClusterClient, mockClusterConn, "kc:", "redis://localhost:6379/0"
        );
        
        boolean result = clusterProvider.ping();
        
        assertThat(result).isFalse();
    }

    @Test
    void testPing_MultipleCalls() {
        when(mockSync.ping())
            .thenReturn("PONG")
            .thenReturn("PONG")
            .thenReturn("ERROR");
        
        assertThat(provider.ping()).isTrue();
        assertThat(provider.ping()).isTrue();
        assertThat(provider.ping()).isFalse();
        
        verify(mockSync, times(3)).ping();
    }

    // ==================== reconnect() Tests ====================

    @Test
    void testReconnect_Success_Standalone() {
        when(mockSync.ping()).thenReturn("PONG");
        
        boolean result = provider.reconnect();
        
        assertThat(result).isTrue();
        verify(mockSync, atLeastOnce()).ping();
    }

    @Test
    void testReconnect_Failure_Standalone() {
        when(mockSync.ping()).thenReturn("ERROR");
        
        boolean result = provider.reconnect();
        
        assertThat(result).isFalse();
    }

    @Test
    void testReconnect_Success_ClusterMode() {
        RedisClusterClient mockClusterClient = mock(RedisClusterClient.class);
        StatefulRedisClusterConnection<String, String> mockClusterConn = mock(StatefulRedisClusterConnection.class);
        RedisAdvancedClusterCommands<String, String> mockClusterSync = mock(RedisAdvancedClusterCommands.class);
        
        when(mockClusterConn.sync()).thenReturn(mockClusterSync);
        when(mockClusterSync.ping()).thenReturn("PONG");
        
        DefaultRedisConnectionProvider clusterProvider = new DefaultRedisConnectionProvider(
            mockClusterClient, mockClusterConn, "kc:", "redis://localhost:6379/0"
        );
        
        boolean result = clusterProvider.reconnect();
        
        assertThat(result).isTrue();
    }

    @Test
    void testReconnect_RetryLogic() {
        // First attempt fails, second succeeds
        when(mockSync.ping())
            .thenReturn("ERROR")
            .thenReturn("PONG");
        
        // First reconnect attempt
        boolean result1 = provider.reconnect();
        assertThat(result1).isFalse();
        
        // Second reconnect attempt
        boolean result2 = provider.reconnect();
        assertThat(result2).isTrue();
    }

    @Test
    void testReconnect_Exception() {
        when(mockSync.ping()).thenThrow(new RuntimeException("Connection refused"));
        
        boolean result = provider.reconnect();
        
        assertThat(result).isFalse();
    }

    // ==================== Integration Tests ====================

    @Test
    void testHealthCheckWorkflow() {
        when(mockSync.ping())
            .thenReturn("PONG")   // Healthy
            .thenReturn("ERROR")  // Unhealthy
            .thenReturn("PONG");  // Reconnected
        
        // Initially healthy
        assertThat(provider.ping()).isTrue();
        
        // Goes unhealthy
        assertThat(provider.ping()).isFalse();
        
        // Reconnect and verify
        assertThat(provider.reconnect()).isTrue();
    }

    @Test
    void testPingAfterReconnect() {
        when(mockSync.ping()).thenReturn("PONG");
        
        // Reconnect
        boolean reconnected = provider.reconnect();
        assertThat(reconnected).isTrue();
        
        // Ping should still work
        boolean healthy = provider.ping();
        assertThat(healthy).isTrue();
    }

    @Test
    void testMultipleReconnectAttempts() {
        when(mockSync.ping())
            .thenReturn("ERROR")
            .thenReturn("ERROR")
            .thenReturn("PONG");
        
        // First two attempts fail
        assertThat(provider.reconnect()).isFalse();
        assertThat(provider.reconnect()).isFalse();
        
        // Third attempt succeeds
        assertThat(provider.reconnect()).isTrue();
        
        verify(mockSync, atLeast(3)).ping();
    }
}
