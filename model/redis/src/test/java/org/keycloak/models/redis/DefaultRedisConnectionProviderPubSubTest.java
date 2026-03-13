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
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.models.redis.DefaultRedisConnectionProvider;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for pub/sub connection tracking and cleanup in DefaultRedisConnectionProvider.
 * Verifies Sprint 1 improvement: Fix pub/sub connection leak.
 *
 * Note: These tests verify the lifecycle-aware connection management where close() is a no-op
 * (since the provider is a singleton), and closeAllConnections() is used for actual cleanup.
 */
class DefaultRedisConnectionProviderPubSubTest {

    private RedisClient mockClient;
    private StatefulRedisConnection<String, String> mockConnection;
    private DefaultRedisConnectionProvider provider;

    @BeforeEach
    void setUp() {
        mockClient = mock(RedisClient.class);
        mockConnection = mock(StatefulRedisConnection.class);
        provider = new DefaultRedisConnectionProvider(mockClient, mockConnection, "kc:", "redis://localhost:6379/0");
    }

    @Test
    void testCreatePubSubConnection_TracksConnection() {
        @SuppressWarnings("unchecked")
        StatefulRedisPubSubConnection<String, String> mockPubSubConn = mock(StatefulRedisPubSubConnection.class);
        when(mockClient.connectPubSub()).thenReturn(mockPubSubConn);

        Object result = provider.createPubSubConnection();

        assertThat(result).isNotNull();
        assertThat(result).isSameAs(mockPubSubConn);
        verify(mockClient).connectPubSub();
    }

    @Test
    void testCreatePubSubConnection_MultipleConnectionsTracked() {
        @SuppressWarnings("unchecked")
        StatefulRedisPubSubConnection<String, String> mockPubSubConn1 = mock(StatefulRedisPubSubConnection.class);
        @SuppressWarnings("unchecked")
        StatefulRedisPubSubConnection<String, String> mockPubSubConn2 = mock(StatefulRedisPubSubConnection.class);
        @SuppressWarnings("unchecked")
        StatefulRedisPubSubConnection<String, String> mockPubSubConn3 = mock(StatefulRedisPubSubConnection.class);
        
        when(mockClient.connectPubSub())
            .thenReturn(mockPubSubConn1)
            .thenReturn(mockPubSubConn2)
            .thenReturn(mockPubSubConn3);

        provider.createPubSubConnection();
        provider.createPubSubConnection();
        provider.createPubSubConnection();

        verify(mockClient, times(3)).connectPubSub();
    }

    @Test
    void testClose_IsNoOp() {
        @SuppressWarnings("unchecked")
        StatefulRedisPubSubConnection<String, String> mockPubSubConn1 = mock(StatefulRedisPubSubConnection.class);
        @SuppressWarnings("unchecked")
        StatefulRedisPubSubConnection<String, String> mockPubSubConn2 = mock(StatefulRedisPubSubConnection.class);

        when(mockPubSubConn1.isOpen()).thenReturn(true);
        when(mockPubSubConn2.isOpen()).thenReturn(true);
        when(mockClient.connectPubSub())
            .thenReturn(mockPubSubConn1)
            .thenReturn(mockPubSubConn2);

        provider.createPubSubConnection();
        provider.createPubSubConnection();

        // Session-scoped close should be a no-op (singleton pattern)
        provider.close();

        // Connections should NOT be closed
        verify(mockPubSubConn1, never()).close();
        verify(mockPubSubConn2, never()).close();
    }

    @Test
    void testCloseAllConnections_ClosesAllPubSubConnections() {
        @SuppressWarnings("unchecked")
        StatefulRedisPubSubConnection<String, String> mockPubSubConn1 = mock(StatefulRedisPubSubConnection.class);
        @SuppressWarnings("unchecked")
        StatefulRedisPubSubConnection<String, String> mockPubSubConn2 = mock(StatefulRedisPubSubConnection.class);

        when(mockPubSubConn1.isOpen()).thenReturn(true);
        when(mockPubSubConn2.isOpen()).thenReturn(true);
        when(mockClient.connectPubSub())
            .thenReturn(mockPubSubConn1)
            .thenReturn(mockPubSubConn2);

        provider.createPubSubConnection();
        provider.createPubSubConnection();
        provider.closeAllConnections();

        verify(mockPubSubConn1).close();
        verify(mockPubSubConn2).close();
    }

    @Test
    void testCloseAllConnections_HandlesClosedConnections() {
        @SuppressWarnings("unchecked")
        StatefulRedisPubSubConnection<String, String> mockPubSubConn = mock(StatefulRedisPubSubConnection.class);

        when(mockPubSubConn.isOpen()).thenReturn(false);
        when(mockClient.connectPubSub()).thenReturn(mockPubSubConn);

        provider.createPubSubConnection();
        provider.closeAllConnections();

        // Should not attempt to close already-closed connection
        verify(mockPubSubConn, never()).close();
    }

    @Test
    void testCloseAllConnections_HandlesExceptionDuringClose() {
        @SuppressWarnings("unchecked")
        StatefulRedisPubSubConnection<String, String> mockPubSubConn1 = mock(StatefulRedisPubSubConnection.class);
        @SuppressWarnings("unchecked")
        StatefulRedisPubSubConnection<String, String> mockPubSubConn2 = mock(StatefulRedisPubSubConnection.class);

        when(mockPubSubConn1.isOpen()).thenReturn(true);
        when(mockPubSubConn2.isOpen()).thenReturn(true);
        doThrow(new RuntimeException("Close failed")).when(mockPubSubConn1).close();
        when(mockClient.connectPubSub())
            .thenReturn(mockPubSubConn1)
            .thenReturn(mockPubSubConn2);

        provider.createPubSubConnection();
        provider.createPubSubConnection();

        // Should not throw exception
        assertThatCode(() -> provider.closeAllConnections()).doesNotThrowAnyException();

        // Should still attempt to close second connection
        verify(mockPubSubConn2).close();
    }

    @Test
    void testCloseAllConnections_WithoutPubSubConnections() {
        // Should not fail when no pub/sub connections exist
        assertThatCode(() -> provider.closeAllConnections()).doesNotThrowAnyException();
    }

    @Test
    void testCloseAllConnections_ClearsConnectionList() {
        @SuppressWarnings("unchecked")
        StatefulRedisPubSubConnection<String, String> mockPubSubConn = mock(StatefulRedisPubSubConnection.class);

        when(mockPubSubConn.isOpen()).thenReturn(true);
        when(mockClient.connectPubSub()).thenReturn(mockPubSubConn);

        provider.createPubSubConnection();
        provider.closeAllConnections();

        // Second close should not attempt to close already-closed connections
        provider.closeAllConnections();

        verify(mockPubSubConn, times(1)).close();
    }

    @Test
    void testCreatePubSubConnection_ClientUnavailable() {
        // Test when client returns null for pub/sub connection
        when(mockClient.connectPubSub()).thenReturn(null);
        
        Object result = provider.createPubSubConnection();
        
        // Provider should handle null gracefully
        assertThat(result).isNull();
    }
}
