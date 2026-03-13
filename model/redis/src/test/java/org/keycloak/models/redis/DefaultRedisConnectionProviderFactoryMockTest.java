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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.redis.DefaultRedisConnectionProviderFactory;
import org.keycloak.models.redis.RedisClientFactory;
import org.keycloak.models.redis.RedisConnectionException;
import org.keycloak.models.redis.RedisConnectionProvider;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for DefaultRedisConnectionProviderFactory using mock Redis clients.
 * Achieves high coverage without requiring actual Redis connections.
 */
@SuppressWarnings({"unchecked", "rawtypes"})
class DefaultRedisConnectionProviderFactoryMockTest {

    private DefaultRedisConnectionProviderFactory factory;
    private Config.Scope config;
    private KeycloakSession session;
    private KeycloakSessionFactory sessionFactory;
    private RedisClientFactory mockClientFactory;
    private RedisClient mockRedisClient;
    private StatefulRedisConnection<String, String> mockConnection;
    private RedisCommands<String, String> mockCommands;
    private RedisClusterClient mockClusterClient;
    private StatefulRedisClusterConnection<String, String> mockClusterConnection;
    private RedisAdvancedClusterCommands<String, String> mockClusterCommands;

    @BeforeEach
    void setUp() {
        factory = new DefaultRedisConnectionProviderFactory();
        config = mock(Config.Scope.class);
        session = mock(KeycloakSession.class);
        sessionFactory = mock(KeycloakSessionFactory.class);
        
        // Create mocks
        mockClientFactory = mock(RedisClientFactory.class);
        mockRedisClient = mock(RedisClient.class);
        mockConnection = mock(StatefulRedisConnection.class);
        mockCommands = mock(RedisCommands.class);
        mockClusterClient = mock(RedisClusterClient.class);
        mockClusterConnection = mock(StatefulRedisClusterConnection.class);
        mockClusterCommands = mock(RedisAdvancedClusterCommands.class);
        
        // Setup default mock behavior
        when(mockConnection.sync()).thenReturn(mockCommands);
        when(mockCommands.ping()).thenReturn("PONG");
        when(mockClusterConnection.sync()).thenReturn(mockClusterCommands);
        when(mockClusterCommands.ping()).thenReturn("PONG");
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

    private void setupStandaloneConfig() {
        when(config.get("host", "localhost")).thenReturn("localhost");
        when(config.getInt("port", 6379)).thenReturn(6379);
        when(config.get("password")).thenReturn(null);
        when(config.getInt("database", 0)).thenReturn(0);
        when(config.getBoolean("ssl", false)).thenReturn(false);
        when(config.getBoolean("sslVerifyPeer", true)).thenReturn(true);
        when(config.getBoolean("cluster", false)).thenReturn(false);
        when(config.getInt("timeout", 5000)).thenReturn(5000);
        when(config.get("keyPrefix", "kc:")).thenReturn("kc:");
        // New Sprint 1 properties
        when(config.getInt("ioThreads", 4)).thenReturn(4);
        when(config.getInt("computeThreads", 4)).thenReturn(4);
        when(config.getInt("connectionRetries", 3)).thenReturn(3);
        when(config.getInt("retryDelayMs", 1000)).thenReturn(1000);
    }

    private void setupClusterConfig() {
        when(config.get("host", "localhost")).thenReturn("cluster.example.com");
        when(config.getInt("port", 6379)).thenReturn(6379);
        when(config.getInt("database", 0)).thenReturn(0);
        when(config.getBoolean("ssl", false)).thenReturn(false);
        when(config.getBoolean("sslVerifyPeer", true)).thenReturn(true);
        when(config.getBoolean("cluster", false)).thenReturn(true);
        when(config.getInt("timeout", 5000)).thenReturn(5000);
        when(config.get("keyPrefix", "kc:")).thenReturn("kc:");
        when(config.getBoolean("clusterTopologyRefresh", true)).thenReturn(true);
        when(config.getBoolean("clusterAdaptiveRefresh", true)).thenReturn(true);
        when(config.getInt("clusterRefreshInterval", 300)).thenReturn(300);
        when(config.getBoolean("validateClusterMembership", true)).thenReturn(true);
        // New Sprint 1 properties
        when(config.getInt("ioThreads", 4)).thenReturn(4);
        when(config.getInt("computeThreads", 4)).thenReturn(4);
        when(config.getInt("connectionRetries", 3)).thenReturn(3);
        when(config.getInt("retryDelayMs", 1000)).thenReturn(1000);
    }

    // ========== Lazy Initialization Tests ==========

    @Test
    void testCreate_TriggersLazyInitialization_StandaloneMode() {
        setupStandaloneConfig();
        when(mockClientFactory.createStandaloneClient(any(), any())).thenReturn(mockRedisClient);
        when(mockClientFactory.connectStandalone(any())).thenReturn(mockConnection);
        
        factory.setClientFactory(mockClientFactory);
        factory.init(config);

        RedisConnectionProvider provider = factory.create(session);

        assertThat(provider).isNotNull();
        verify(mockClientFactory).createStandaloneClient(any(), any());
        verify(mockClientFactory).connectStandalone(mockRedisClient);
    }

    @Test
    void testCreate_MultipleCalls_OnlyInitializesOnce() {
        setupStandaloneConfig();
        when(mockClientFactory.createStandaloneClient(any(), any())).thenReturn(mockRedisClient);
        when(mockClientFactory.connectStandalone(any())).thenReturn(mockConnection);
        
        factory.setClientFactory(mockClientFactory);
        factory.init(config);

        RedisConnectionProvider provider1 = factory.create(session);
        RedisConnectionProvider provider2 = factory.create(session);
        RedisConnectionProvider provider3 = factory.create(session);

        assertThat(provider1).isSameAs(provider2).isSameAs(provider3);
        verify(mockClientFactory, times(1)).createStandaloneClient(any(), any());
    }

    @Test
    void testCreate_WithPassword() {
        setupStandaloneConfig();
        when(config.get("password")).thenReturn("mySecretPassword");
        when(mockClientFactory.createStandaloneClient(any(), any())).thenReturn(mockRedisClient);
        when(mockClientFactory.connectStandalone(any())).thenReturn(mockConnection);
        
        factory.setClientFactory(mockClientFactory);
        factory.init(config);

        RedisConnectionProvider provider = factory.create(session);

        assertThat(provider).isNotNull();
        verify(mockClientFactory).createStandaloneClient(any(), any());
    }

    @Test
    void testCreate_WithEmptyPassword() {
        setupStandaloneConfig();
        when(config.get("password")).thenReturn("");
        when(mockClientFactory.createStandaloneClient(any(), any())).thenReturn(mockRedisClient);
        when(mockClientFactory.connectStandalone(any())).thenReturn(mockConnection);
        
        factory.setClientFactory(mockClientFactory);
        factory.init(config);

        RedisConnectionProvider provider = factory.create(session);

        assertThat(provider).isNotNull();
    }

    @Test
    void testCreate_WithSSL() {
        setupStandaloneConfig();
        when(config.getBoolean("ssl", false)).thenReturn(true);
        when(mockClientFactory.createStandaloneClient(any(), any())).thenReturn(mockRedisClient);
        when(mockClientFactory.connectStandalone(any())).thenReturn(mockConnection);

        factory.setClientFactory(mockClientFactory);
        factory.init(config);

        RedisConnectionProvider provider = factory.create(session);

        assertThat(provider).isNotNull();
    }

    @Test
    void testCreate_WithSSLAndVerifyPeerEnabled() {
        setupStandaloneConfig();
        when(config.getBoolean("ssl", false)).thenReturn(true);
        when(config.getBoolean("sslVerifyPeer", true)).thenReturn(true);
        when(mockClientFactory.createStandaloneClient(any(), any())).thenReturn(mockRedisClient);
        when(mockClientFactory.connectStandalone(any())).thenReturn(mockConnection);

        factory.setClientFactory(mockClientFactory);
        factory.init(config);

        RedisConnectionProvider provider = factory.create(session);

        assertThat(provider).isNotNull();
        verify(config).getBoolean("sslVerifyPeer", true);
    }

    @Test
    void testCreate_WithSSLAndVerifyPeerDisabled() {
        setupStandaloneConfig();
        when(config.getBoolean("ssl", false)).thenReturn(true);
        when(config.getBoolean("sslVerifyPeer", true)).thenReturn(false);
        when(mockClientFactory.createStandaloneClient(any(), any())).thenReturn(mockRedisClient);
        when(mockClientFactory.connectStandalone(any())).thenReturn(mockConnection);

        factory.setClientFactory(mockClientFactory);
        factory.init(config);

        RedisConnectionProvider provider = factory.create(session);

        assertThat(provider).isNotNull();
        verify(config).getBoolean("sslVerifyPeer", true);
    }

    @Test
    void testCreate_WithoutSSL_VerifyPeerIgnored() {
        setupStandaloneConfig();
        when(config.getBoolean("ssl", false)).thenReturn(false);
        when(config.getBoolean("sslVerifyPeer", true)).thenReturn(false);
        when(mockClientFactory.createStandaloneClient(any(), any())).thenReturn(mockRedisClient);
        when(mockClientFactory.connectStandalone(any())).thenReturn(mockConnection);

        factory.setClientFactory(mockClientFactory);
        factory.init(config);

        RedisConnectionProvider provider = factory.create(session);

        // Should still work - sslVerifyPeer is only relevant when SSL is enabled
        assertThat(provider).isNotNull();
    }

    @Test
    void testCreate_WithCustomDatabase() {
        setupStandaloneConfig();
        when(config.getInt("database", 0)).thenReturn(5);
        when(mockClientFactory.createStandaloneClient(any(), any())).thenReturn(mockRedisClient);
        when(mockClientFactory.connectStandalone(any())).thenReturn(mockConnection);
        
        factory.setClientFactory(mockClientFactory);
        factory.init(config);

        RedisConnectionProvider provider = factory.create(session);

        assertThat(provider).isNotNull();
    }

    @Test
    void testCreate_WithCustomTimeout() {
        setupStandaloneConfig();
        when(config.getInt("timeout", 5000)).thenReturn(15000);
        when(mockClientFactory.createStandaloneClient(any(), any())).thenReturn(mockRedisClient);
        when(mockClientFactory.connectStandalone(any())).thenReturn(mockConnection);
        
        factory.setClientFactory(mockClientFactory);
        factory.init(config);

        RedisConnectionProvider provider = factory.create(session);

        assertThat(provider).isNotNull();
    }

    @Test
    void testCreate_WithCustomKeyPrefix() {
        setupStandaloneConfig();
        when(config.get("keyPrefix", "kc:")).thenReturn("custom:");
        when(mockClientFactory.createStandaloneClient(any(), any())).thenReturn(mockRedisClient);
        when(mockClientFactory.connectStandalone(any())).thenReturn(mockConnection);
        
        factory.setClientFactory(mockClientFactory);
        factory.init(config);

        RedisConnectionProvider provider = factory.create(session);

        assertThat(provider).isNotNull();
    }

    // ========== Cluster Mode Tests ==========

    @Test
    void testCreate_ClusterMode_WithAllOptionsEnabled() {
        setupClusterConfig();
        when(mockClientFactory.createClusterClient(any(), any())).thenReturn(mockClusterClient);
        when(mockClientFactory.connectCluster(any())).thenReturn(mockClusterConnection);
        
        factory.setClientFactory(mockClientFactory);
        factory.init(config);

        RedisConnectionProvider provider = factory.create(session);

        assertThat(provider).isNotNull();
        verify(mockClientFactory).createClusterClient(any(), any());
        verify(mockClientFactory).setClusterOptions(eq(mockClusterClient), any());
        verify(mockClientFactory).connectCluster(mockClusterClient);
    }

    @Test
    void testCreate_ClusterMode_WithOnlyTopologyRefresh() {
        setupClusterConfig();
        when(config.getBoolean("clusterTopologyRefresh", true)).thenReturn(true);
        when(config.getBoolean("clusterAdaptiveRefresh", true)).thenReturn(false);
        when(mockClientFactory.createClusterClient(any(), any())).thenReturn(mockClusterClient);
        when(mockClientFactory.connectCluster(any())).thenReturn(mockClusterConnection);
        
        factory.setClientFactory(mockClientFactory);
        factory.init(config);

        RedisConnectionProvider provider = factory.create(session);

        assertThat(provider).isNotNull();
        verify(mockClientFactory).setClusterOptions(eq(mockClusterClient), any());
    }

    @Test
    void testCreate_ClusterMode_WithOnlyAdaptiveRefresh() {
        setupClusterConfig();
        when(config.getBoolean("clusterTopologyRefresh", true)).thenReturn(false);
        when(config.getBoolean("clusterAdaptiveRefresh", true)).thenReturn(true);
        when(mockClientFactory.createClusterClient(any(), any())).thenReturn(mockClusterClient);
        when(mockClientFactory.connectCluster(any())).thenReturn(mockClusterConnection);
        
        factory.setClientFactory(mockClientFactory);
        factory.init(config);

        RedisConnectionProvider provider = factory.create(session);

        assertThat(provider).isNotNull();
    }

    @Test
    void testCreate_ClusterMode_WithNoRefresh() {
        setupClusterConfig();
        when(config.getBoolean("clusterTopologyRefresh", true)).thenReturn(false);
        when(config.getBoolean("clusterAdaptiveRefresh", true)).thenReturn(false);
        when(mockClientFactory.createClusterClient(any(), any())).thenReturn(mockClusterClient);
        when(mockClientFactory.connectCluster(any())).thenReturn(mockClusterConnection);
        
        factory.setClientFactory(mockClientFactory);
        factory.init(config);

        RedisConnectionProvider provider = factory.create(session);

        assertThat(provider).isNotNull();
    }

    @Test
    void testCreate_ClusterMode_WithCustomRefreshInterval() {
        setupClusterConfig();
        when(config.getInt("clusterRefreshInterval", 300)).thenReturn(600);
        when(mockClientFactory.createClusterClient(any(), any())).thenReturn(mockClusterClient);
        when(mockClientFactory.connectCluster(any())).thenReturn(mockClusterConnection);
        
        factory.setClientFactory(mockClientFactory);
        factory.init(config);

        RedisConnectionProvider provider = factory.create(session);

        assertThat(provider).isNotNull();
    }

    @Test
    void testCreate_ClusterMode_WithValidateMembershipDisabled() {
        setupClusterConfig();
        when(config.getBoolean("validateClusterMembership", true)).thenReturn(false);
        when(mockClientFactory.createClusterClient(any(), any())).thenReturn(mockClusterClient);
        when(mockClientFactory.connectCluster(any())).thenReturn(mockClusterConnection);
        
        factory.setClientFactory(mockClientFactory);
        factory.init(config);

        RedisConnectionProvider provider = factory.create(session);

        assertThat(provider).isNotNull();
    }

    // ========== Connection Info Tests ==========

    @Test
    void testConnectionInfo_StandaloneMode_NoSSL() {
        setupStandaloneConfig();
        when(mockClientFactory.createStandaloneClient(any(), any())).thenReturn(mockRedisClient);
        when(mockClientFactory.connectStandalone(any())).thenReturn(mockConnection);
        
        factory.setClientFactory(mockClientFactory);
        factory.init(config);
        factory.create(session);

        Map<String, String> info = factory.getOperationalInfo();

        assertThat(info.get("connectionInfo")).startsWith("redis://");
        assertThat(info.get("connectionInfo")).contains("localhost:6379");
        assertThat(info.get("connectionInfo")).contains("/0");
    }

    @Test
    void testConnectionInfo_StandaloneMode_WithSSL() {
        setupStandaloneConfig();
        when(config.getBoolean("ssl", false)).thenReturn(true);
        when(mockClientFactory.createStandaloneClient(any(), any())).thenReturn(mockRedisClient);
        when(mockClientFactory.connectStandalone(any())).thenReturn(mockConnection);
        
        factory.setClientFactory(mockClientFactory);
        factory.init(config);
        factory.create(session);

        Map<String, String> info = factory.getOperationalInfo();

        assertThat(info.get("connectionInfo")).startsWith("rediss://");
    }

    @Test
    void testConnectionInfo_WithCustomDatabase() {
        setupStandaloneConfig();
        when(config.getInt("database", 0)).thenReturn(7);
        when(mockClientFactory.createStandaloneClient(any(), any())).thenReturn(mockRedisClient);
        when(mockClientFactory.connectStandalone(any())).thenReturn(mockConnection);
        
        factory.setClientFactory(mockClientFactory);
        factory.init(config);
        factory.create(session);

        Map<String, String> info = factory.getOperationalInfo();

        assertThat(info.get("connectionInfo")).contains("/7");
    }

    // ========== Operational Info Tests ==========

    @Test
    void testGetOperationalInfo_AfterInitialization_Standalone() {
        setupStandaloneConfig();
        when(mockClientFactory.createStandaloneClient(any(), any())).thenReturn(mockRedisClient);
        when(mockClientFactory.connectStandalone(any())).thenReturn(mockConnection);
        
        factory.setClientFactory(mockClientFactory);
        factory.init(config);
        factory.create(session);

        Map<String, String> info = factory.getOperationalInfo();

        assertThat(info).isNotNull();
        assertThat(info.get("provider")).isEqualTo("default");
        assertThat(info.get("clusterMode")).isEqualTo("false");
        assertThat(info.get("connectionInfo")).isNotNull();
        assertThat(info).containsKey("connected");
    }

    @Test
    void testGetOperationalInfo_AfterInitialization_Cluster() {
        setupClusterConfig();
        when(mockClientFactory.createClusterClient(any(), any())).thenReturn(mockClusterClient);
        when(mockClientFactory.connectCluster(any())).thenReturn(mockClusterConnection);
        
        factory.setClientFactory(mockClientFactory);
        factory.init(config);
        factory.create(session);

        Map<String, String> info = factory.getOperationalInfo();

        assertThat(info).isNotNull();
        assertThat(info.get("provider")).isEqualTo("default");
        assertThat(info.get("clusterMode")).isEqualTo("true");
        assertThat(info.get("connectionInfo")).isNotNull();
    }

    // ========== Close and Cleanup Tests ==========

    @Test
    void testClose_AfterStandaloneInit_CleansUpResources() {
        setupStandaloneConfig();
        when(mockClientFactory.createStandaloneClient(any(), any())).thenReturn(mockRedisClient);
        when(mockClientFactory.connectStandalone(any())).thenReturn(mockConnection);
        
        factory.setClientFactory(mockClientFactory);
        factory.init(config);
        factory.create(session);

        assertThatCode(() -> factory.close()).doesNotThrowAnyException();

        verify(mockConnection).close();
        verify(mockRedisClient).shutdown();
    }

    @Test
    void testClose_AfterClusterInit_CleansUpResources() {
        setupClusterConfig();
        when(mockClientFactory.createClusterClient(any(), any())).thenReturn(mockClusterClient);
        when(mockClientFactory.connectCluster(any())).thenReturn(mockClusterConnection);
        
        factory.setClientFactory(mockClientFactory);
        factory.init(config);
        factory.create(session);

        assertThatCode(() -> factory.close()).doesNotThrowAnyException();

        verify(mockClusterConnection).close();
        verify(mockClusterClient).shutdown();
    }

    @Test
    void testClose_MultipleTimes_DoesNotThrow() {
        setupStandaloneConfig();
        when(mockClientFactory.createStandaloneClient(any(), any())).thenReturn(mockRedisClient);
        when(mockClientFactory.connectStandalone(any())).thenReturn(mockConnection);
        
        factory.setClientFactory(mockClientFactory);
        factory.init(config);
        factory.create(session);

        assertThatCode(() -> {
            factory.close();
            factory.close();
            factory.close();
        }).doesNotThrowAnyException();
    }

    @Test
    void testClose_WithException_DoesNotThrow() {
        setupStandaloneConfig();
        when(mockClientFactory.createStandaloneClient(any(), any())).thenReturn(mockRedisClient);
        when(mockClientFactory.connectStandalone(any())).thenReturn(mockConnection);
        doThrow(new RuntimeException("Close error")).when(mockConnection).close();
        
        factory.setClientFactory(mockClientFactory);
        factory.init(config);
        factory.create(session);

        assertThatCode(() -> factory.close()).doesNotThrowAnyException();
    }

    @Test
    void testGetOperationalInfo_AfterClose_ShowsDisconnected() {
        setupStandaloneConfig();
        when(mockClientFactory.createStandaloneClient(any(), any())).thenReturn(mockRedisClient);
        when(mockClientFactory.connectStandalone(any())).thenReturn(mockConnection);
        
        factory.setClientFactory(mockClientFactory);
        factory.init(config);
        factory.create(session);
        factory.close();

        Map<String, String> info = factory.getOperationalInfo();

        assertThat(info.get("connected")).isEqualTo("false");
        assertThat(info.get("status")).isEqualTo("not initialized");
    }

    // ========== PostInit Tests ==========

    @Test
    void testPostInit_BeforeCreate() {
        setupStandaloneConfig();
        when(mockClientFactory.createStandaloneClient(any(), any())).thenReturn(mockRedisClient);
        when(mockClientFactory.connectStandalone(any())).thenReturn(mockConnection);
        
        factory.setClientFactory(mockClientFactory);
        factory.init(config);
        factory.postInit(sessionFactory);

        RedisConnectionProvider provider = factory.create(session);

        assertThat(provider).isNotNull();
    }

    // ========== All Configuration Combinations ==========

    @Test
    void testCreate_AllCustomValues() {
        setupStandaloneConfig();
        when(config.get("host", "localhost")).thenReturn("custom.redis.com");
        when(config.getInt("port", 6379)).thenReturn(7000);
        when(config.get("password")).thenReturn("customPassword");
        when(config.getInt("database", 0)).thenReturn(9);
        when(config.getBoolean("ssl", false)).thenReturn(true);
        when(config.getInt("timeout", 5000)).thenReturn(20000);
        when(config.get("keyPrefix", "kc:")).thenReturn("myapp:");
        when(mockClientFactory.createStandaloneClient(any(), any())).thenReturn(mockRedisClient);
        when(mockClientFactory.connectStandalone(any())).thenReturn(mockConnection);
        
        factory.setClientFactory(mockClientFactory);
        factory.init(config);

        RedisConnectionProvider provider = factory.create(session);

        assertThat(provider).isNotNull();
        
        Map<String, String> info = factory.getOperationalInfo();
        assertThat(info.get("connectionInfo")).contains("custom.redis.com");
        assertThat(info.get("connectionInfo")).contains("7000");
        assertThat(info.get("connectionInfo")).contains("/9");
        assertThat(info.get("connectionInfo")).startsWith("rediss://");
    }

    // ========== Sprint 1 Improvements: Retry Logic Tests ==========

    @Test
    void testConnectionRetry_SucceedsOnSecondAttempt() {
        setupStandaloneConfig();
        when(config.getInt("connectionRetries", 3)).thenReturn(3);
        when(config.getInt("retryDelayMs", 1000)).thenReturn(100); // Fast retry for testing
        
        // First attempt fails, second succeeds
        when(mockClientFactory.createStandaloneClient(any(), any()))
            .thenThrow(new RuntimeException("Connection failed"))
            .thenReturn(mockRedisClient);
        when(mockClientFactory.connectStandalone(any())).thenReturn(mockConnection);
        
        factory.setClientFactory(mockClientFactory);
        factory.init(config);

        RedisConnectionProvider provider = factory.create(session);

        assertThat(provider).isNotNull();
        verify(mockClientFactory, times(2)).createStandaloneClient(any(), any());
    }

    @Test
    void testConnectionRetry_FailsAfterMaxRetries() {
        setupStandaloneConfig();
        when(config.getInt("connectionRetries", 3)).thenReturn(2);
        when(config.getInt("retryDelayMs", 1000)).thenReturn(10);
        
        // All attempts fail
        when(mockClientFactory.createStandaloneClient(any(), any()))
            .thenThrow(new RuntimeException("Connection failed"));
        
        factory.setClientFactory(mockClientFactory);
        factory.init(config);

        assertThatThrownBy(() -> factory.create(session))
            .isInstanceOf(RedisConnectionException.class)
            .hasMessageContaining("Failed to initialize Redis connection after 2 attempts");
        
        verify(mockClientFactory, times(2)).createStandaloneClient(any(), any());
    }

    @Test
    void testConnectionRetry_CustomRetryConfig() {
        setupStandaloneConfig();
        when(config.getInt("connectionRetries", 3)).thenReturn(5);
        when(config.getInt("retryDelayMs", 1000)).thenReturn(50);
        when(mockClientFactory.createStandaloneClient(any(), any())).thenReturn(mockRedisClient);
        when(mockClientFactory.connectStandalone(any())).thenReturn(mockConnection);
        
        factory.setClientFactory(mockClientFactory);
        factory.init(config);

        RedisConnectionProvider provider = factory.create(session);

        assertThat(provider).isNotNull();
        verify(mockClientFactory, times(1)).createStandaloneClient(any(), any());
    }

    // ========== Sprint 1 Improvements: Thread Pool Configuration Tests ==========

    @Test
    void testThreadPoolConfiguration_DefaultValues() {
        setupStandaloneConfig();
        when(config.getInt("ioThreads", 4)).thenReturn(4);
        when(config.getInt("computeThreads", 4)).thenReturn(4);
        when(mockClientFactory.createStandaloneClient(any(), any())).thenReturn(mockRedisClient);
        when(mockClientFactory.connectStandalone(any())).thenReturn(mockConnection);
        
        factory.setClientFactory(mockClientFactory);
        factory.init(config);

        RedisConnectionProvider provider = factory.create(session);

        assertThat(provider).isNotNull();
        verify(config).getInt("ioThreads", 4);
        verify(config).getInt("computeThreads", 4);
    }

    @Test
    void testThreadPoolConfiguration_CustomValues() {
        setupStandaloneConfig();
        when(config.getInt("ioThreads", 4)).thenReturn(8);
        when(config.getInt("computeThreads", 4)).thenReturn(16);
        when(mockClientFactory.createStandaloneClient(any(), any())).thenReturn(mockRedisClient);
        when(mockClientFactory.connectStandalone(any())).thenReturn(mockConnection);
        
        factory.setClientFactory(mockClientFactory);
        factory.init(config);

        RedisConnectionProvider provider = factory.create(session);

        assertThat(provider).isNotNull();
        verify(config).getInt("ioThreads", 4);
        verify(config).getInt("computeThreads", 4);
    }

    @Test
    void testConfigMetadata_IncludesNewProperties() {
        List<ProviderConfigProperty> metadata = factory.getConfigMetadata();
        
        List<String> propertyNames = metadata.stream()
            .map(ProviderConfigProperty::getName)
            .toList();
        
        assertThat(propertyNames).contains(
            "ioThreads",
            "computeThreads",
            "connectionRetries",
            "retryDelayMs"
        );
    }

    @Test
    void testConfigMetadata_NewPropertiesHaveDefaults() {
        List<ProviderConfigProperty> metadata = factory.getConfigMetadata();
        
        metadata.stream()
            .filter(p -> p.getName().equals("ioThreads"))
            .findFirst()
            .ifPresent(p -> assertThat(p.getDefaultValue()).isEqualTo("4"));
        
        metadata.stream()
            .filter(p -> p.getName().equals("connectionRetries"))
            .findFirst()
            .ifPresent(p -> assertThat(p.getDefaultValue()).isEqualTo("3"));
    }
}
