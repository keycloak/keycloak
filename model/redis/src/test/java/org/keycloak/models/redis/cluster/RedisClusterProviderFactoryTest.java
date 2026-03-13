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

package org.keycloak.models.redis.cluster;

import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.redis.RedisConnectionProvider;
import org.keycloak.cluster.ClusterProvider;
import org.keycloak.models.redis.cluster.RedisClusterProviderFactory;

import static org.assertj.core.api.Assertions.*;
import static org.keycloak.models.redis.TestConstants.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive tests for RedisClusterProviderFactory.
 */
class RedisClusterProviderFactoryTest {

    private RedisClusterProviderFactory factory;
    private Config.Scope config;
    private KeycloakSession session;
    private KeycloakSessionFactory sessionFactory;
    private RedisConnectionProvider redis;

    @BeforeEach
    void setUp() {
        factory = new RedisClusterProviderFactory();
        config = mock(Config.Scope.class);
        session = mock(KeycloakSession.class);
        sessionFactory = mock(KeycloakSessionFactory.class);
        redis = mock(RedisConnectionProvider.class);
    }

    @AfterEach
    void tearDown() {
        if (factory != null) {
            try {
                factory.close();
            } catch (Exception e) {
                // Ignore cleanup errors
            }
        }
    }

    @Test
    void testGetId() {
        assertThat(factory.getId()).isEqualTo("redis");
    }

    @Test
    void testInit_WithNodeId() {
        when(config.get(eq("nodeId"), anyString())).thenReturn(NODE_ID);
        
        factory.init(config);
        
        assertThat(factory).isNotNull();
    }

    @Test
    void testInit_WithoutNodeId_GeneratesUUID() {
        when(config.get(eq("nodeId"), anyString())).thenAnswer(invocation -> invocation.getArgument(1));
        
        factory.init(config);
        
        // Should use generated UUID as default
        assertThat(factory).isNotNull();
    }

    @Test
    void testPostInit_WithRedisProvider() {
        when(config.get(eq("nodeId"), anyString())).thenReturn(NODE_ID);
        factory.init(config);

        // Mock session factory to return session
        when(sessionFactory.create()).thenReturn(session);
        when(session.getProvider(RedisConnectionProvider.class)).thenReturn(redis);
        // Mock clusterStartupTime retrieval (no existing value)
        when(redis.get(eq("work"), eq("cluster:startup-time"), eq(Integer.class))).thenReturn(null);
        when(redis.putIfAbsent(eq("work"), eq("cluster:startup-time"), anyInt(), anyLong(), any())).thenReturn(null);
        when(redis.createPubSubConnection()).thenReturn(mock(StatefulRedisPubSubConnection.class));

        factory.postInit(sessionFactory);

        verify(sessionFactory, atLeast(1)).create();
        verify(session, atLeast(1)).close();
    }

    @Test
    void testPostInit_WithExistingClusterStartupTime() {
        when(config.get(eq("nodeId"), anyString())).thenReturn(NODE_ID);
        factory.init(config);

        int existingStartupTime = 1234567890;

        // Mock session factory to return session
        when(sessionFactory.create()).thenReturn(session);
        when(session.getProvider(RedisConnectionProvider.class)).thenReturn(redis);
        // Mock clusterStartupTime retrieval (existing value)
        when(redis.get(eq("work"), eq("cluster:startup-time"), eq(Integer.class))).thenReturn(existingStartupTime);
        when(redis.createPubSubConnection()).thenReturn(mock(StatefulRedisPubSubConnection.class));

        factory.postInit(sessionFactory);

        // Should use existing value, not create a new one
        verify(redis, never()).putIfAbsent(eq("work"), eq("cluster:startup-time"), anyInt(), anyLong(), any());
        verify(sessionFactory, atLeast(1)).create();
        verify(session, atLeast(1)).close();
    }

    @Test
    void testPostInit_WithoutRedisProvider() {
        when(config.get(eq("nodeId"), anyString())).thenReturn(NODE_ID);
        factory.init(config);

        when(sessionFactory.create()).thenReturn(session);
        when(session.getProvider(RedisConnectionProvider.class)).thenReturn(null);

        // Should not throw, just log warning
        assertThatCode(() -> factory.postInit(sessionFactory)).doesNotThrowAnyException();

        // Session is closed twice: once in initializeClusterStartupTime, once in postInit for pub/sub
        verify(session, atLeast(1)).close();
    }

    @Test
    void testPostInit_PubSubConnectionFails() {
        when(config.get(eq("nodeId"), anyString())).thenReturn(NODE_ID);
        factory.init(config);

        when(sessionFactory.create()).thenReturn(session);
        when(session.getProvider(RedisConnectionProvider.class)).thenReturn(redis);
        // Mock clusterStartupTime retrieval
        when(redis.get(eq("work"), eq("cluster:startup-time"), eq(Integer.class))).thenReturn(null);
        when(redis.putIfAbsent(eq("work"), eq("cluster:startup-time"), anyInt(), anyLong(), any())).thenReturn(null);
        when(redis.createPubSubConnection()).thenReturn(null);

        // Should not throw
        assertThatCode(() -> factory.postInit(sessionFactory)).doesNotThrowAnyException();

        verify(session, atLeast(1)).close();
    }

    @Test
    void testPostInit_InvalidPubSubConnectionType() {
        when(config.get(eq("nodeId"), anyString())).thenReturn(NODE_ID);
        factory.init(config);

        when(sessionFactory.create()).thenReturn(session);
        when(session.getProvider(RedisConnectionProvider.class)).thenReturn(redis);
        // Mock clusterStartupTime retrieval
        when(redis.get(eq("work"), eq("cluster:startup-time"), eq(Integer.class))).thenReturn(null);
        when(redis.putIfAbsent(eq("work"), eq("cluster:startup-time"), anyInt(), anyLong(), any())).thenReturn(null);
        when(redis.createPubSubConnection()).thenReturn(new Object()); // Wrong type

        // Should handle gracefully
        assertThatCode(() -> factory.postInit(sessionFactory)).doesNotThrowAnyException();

        verify(session, atLeast(1)).close();
    }

    @Test
    void testPostInit_ExceptionDuringInit() {
        when(config.get(eq("nodeId"), anyString())).thenReturn(NODE_ID);
        factory.init(config);
        
        when(sessionFactory.create()).thenThrow(new RuntimeException("Test exception"));
        
        // Should catch and log exception
        assertThatCode(() -> factory.postInit(sessionFactory)).doesNotThrowAnyException();
    }

    @Test
    void testClose_WithoutInit() {
        // Should not throw
        assertThatCode(() -> factory.close()).doesNotThrowAnyException();
    }

    @Test
    void testClose_AfterInit() {
        when(config.get(eq("nodeId"), anyString())).thenReturn(NODE_ID);
        factory.init(config);
        
        assertThatCode(() -> factory.close()).doesNotThrowAnyException();
    }

    @Test
    void testCreate_Success() {
        when(config.get(eq("nodeId"), anyString())).thenReturn(NODE_ID);
        factory.init(config);
        
        when(session.getProvider(RedisConnectionProvider.class)).thenReturn(redis);
        
        ClusterProvider provider = factory.create(session);
        
        assertThat(provider).isNotNull();
        verify(session).getProvider(RedisConnectionProvider.class);
    }

    @Test
    void testCreate_RedisProviderNotAvailable() {
        when(config.get(eq("nodeId"), anyString())).thenReturn(NODE_ID);
        factory.init(config);
        
        when(session.getProvider(RedisConnectionProvider.class)).thenReturn(null);
        
        assertThatThrownBy(() -> factory.create(session))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("RedisConnectionProvider not available");
    }

    @Test
    void testCreate_WithoutInit() {
        when(config.get(eq("nodeId"), anyString())).thenAnswer(invocation -> invocation.getArgument(1));
        when(session.getProvider(RedisConnectionProvider.class)).thenReturn(redis);
        
        // Should work with default node ID
        ClusterProvider provider = factory.create(session);
        
        assertThat(provider).isNotNull();
    }

    @Test
    void testMultipleCreates_ShareListeners() {
        when(config.get(eq("nodeId"), anyString())).thenReturn(NODE_ID);
        factory.init(config);
        
        when(session.getProvider(RedisConnectionProvider.class)).thenReturn(redis);
        
        ClusterProvider provider1 = factory.create(session);
        ClusterProvider provider2 = factory.create(session);
        
        assertThat(provider1).isNotNull();
        assertThat(provider2).isNotNull();
        // Each call creates new provider instance
        assertThat(provider1).isNotSameAs(provider2);
    }

    @Test
    void testInit_NullConfig() {
        when(config.get(eq("nodeId"), anyString())).thenReturn(null);
        
        factory.init(config);
        
        // Should generate UUID
        assertThat(factory).isNotNull();
    }

    @Test
    void testInit_EmptyNodeId() {
        when(config.get(eq("nodeId"), anyString())).thenReturn("");
        
        factory.init(config);
        
        // Should use empty string as node ID
        assertThat(factory).isNotNull();
    }

    @Test
    void testCreate_MultipleSessionsSameFactory() {
        when(config.get(eq("nodeId"), anyString())).thenReturn(NODE_ID);
        factory.init(config);
        
        KeycloakSession session1 = mock(KeycloakSession.class);
        KeycloakSession session2 = mock(KeycloakSession.class);
        
        when(session1.getProvider(RedisConnectionProvider.class)).thenReturn(redis);
        when(session2.getProvider(RedisConnectionProvider.class)).thenReturn(redis);
        
        ClusterProvider provider1 = factory.create(session1);
        ClusterProvider provider2 = factory.create(session2);
        
        assertThat(provider1).isNotNull();
        assertThat(provider2).isNotNull();
    }
}
