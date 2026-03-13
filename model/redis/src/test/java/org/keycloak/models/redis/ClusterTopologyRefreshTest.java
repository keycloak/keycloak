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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.Config;
import org.keycloak.models.redis.DefaultRedisConnectionProviderFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for Redis Cluster topology refresh features added in Lettuce 6.5.1 upgrade.
 * Tests new configuration options:
 * - clusterTopologyRefresh
 * - clusterAdaptiveRefresh
 * - clusterRefreshInterval
 * - validateClusterMembership
 */
class ClusterTopologyRefreshTest {

    private DefaultRedisConnectionProviderFactory factory;
    private Config.Scope config;

    @BeforeEach
    void setUp() {
        factory = new DefaultRedisConnectionProviderFactory();
        config = mock(Config.Scope.class);
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

    // ========== Configuration Metadata Tests ==========

    @Test
    void testGetConfigMetadata_HasClusterTopologyRefreshProperty() {
        List<ProviderConfigProperty> metadata = factory.getConfigMetadata();

        ProviderConfigProperty prop = metadata.stream()
                .filter(p -> "clusterTopologyRefresh".equals(p.getName()))
                .findFirst()
                .orElse(null);

        assertThat(prop).isNotNull();
        assertThat(prop.getType()).isEqualTo("boolean");
        assertThat(prop.getLabel()).contains("Topology Refresh");
        assertThat(prop.getDefaultValue()).isEqualTo("true");
        assertThat(prop.getHelpText()).isNotEmpty();
    }

    @Test
    void testGetConfigMetadata_HasClusterAdaptiveRefreshProperty() {
        List<ProviderConfigProperty> metadata = factory.getConfigMetadata();

        ProviderConfigProperty prop = metadata.stream()
                .filter(p -> "clusterAdaptiveRefresh".equals(p.getName()))
                .findFirst()
                .orElse(null);

        assertThat(prop).isNotNull();
        assertThat(prop.getType()).isEqualTo("boolean");
        assertThat(prop.getLabel()).contains("Adaptive");
        assertThat(prop.getDefaultValue()).isEqualTo("true");
        assertThat(prop.getHelpText()).containsAnyOf("MOVED", "redirect", "ASK");
    }

    @Test
    void testGetConfigMetadata_HasClusterRefreshIntervalProperty() {
        List<ProviderConfigProperty> metadata = factory.getConfigMetadata();

        ProviderConfigProperty prop = metadata.stream()
                .filter(p -> "clusterRefreshInterval".equals(p.getName()))
                .findFirst()
                .orElse(null);

        assertThat(prop).isNotNull();
        assertThat(prop.getType()).isEqualTo("int");
        assertThat(prop.getLabel()).contains("Refresh Interval");
        assertThat(prop.getDefaultValue()).isEqualTo("300");
        assertThat(prop.getHelpText()).containsAnyOf("refresh", "topology", "periodic");
    }

    @Test
    void testGetConfigMetadata_HasValidateClusterMembershipProperty() {
        List<ProviderConfigProperty> metadata = factory.getConfigMetadata();

        ProviderConfigProperty prop = metadata.stream()
                .filter(p -> "validateClusterMembership".equals(p.getName()))
                .findFirst()
                .orElse(null);

        assertThat(prop).isNotNull();
        assertThat(prop.getType()).isEqualTo("boolean");
        assertThat(prop.getLabel()).contains("Validate");
        assertThat(prop.getDefaultValue()).isEqualTo("true");
        assertThat(prop.getHelpText()).isNotEmpty();
    }

    @Test
    void testGetConfigMetadata_HasCorrectNumberOfProperties() {
        List<ProviderConfigProperty> metadata = factory.getConfigMetadata();

        // Original 8 + 1 sslVerifyPeer + 4 cluster properties + 4 Sprint 1 properties = 17 total
        assertThat(metadata).hasSize(17);
    }

    @Test
    void testGetConfigMetadata_AllNewPropertiesHaveHelpText() {
        List<ProviderConfigProperty> metadata = factory.getConfigMetadata();

        List<String> newProperties = List.of(
                "clusterTopologyRefresh",
                "clusterAdaptiveRefresh",
                "clusterRefreshInterval",
                "validateClusterMembership"
        );

        for (String propertyName : newProperties) {
            ProviderConfigProperty prop = metadata.stream()
                    .filter(p -> propertyName.equals(p.getName()))
                    .findFirst()
                    .orElse(null);

            assertThat(prop)
                    .as("Property %s should exist", propertyName)
                    .isNotNull();

            assertThat(prop.getHelpText())
                    .as("Property %s should have help text", propertyName)
                    .isNotNull()
                    .isNotEmpty();
        }
    }

    // ========== Cluster Mode with Topology Refresh Tests ==========

    @Test
    void testInit_ClusterModeWithDefaultTopologyRefresh() {
        setupClusterConfig();

        factory.init(config);

        // Should not throw - validates that cluster mode works with default topology refresh settings
        assertThat(factory).isNotNull();
    }

    @Test
    void testInit_ClusterModeWithTopologyRefreshEnabled() {
        setupClusterConfig();
        when(config.getBoolean("clusterTopologyRefresh", true)).thenReturn(true);
        when(config.getBoolean("clusterAdaptiveRefresh", true)).thenReturn(true);
        when(config.getInt("clusterRefreshInterval", 300)).thenReturn(300);
        when(config.getBoolean("validateClusterMembership", true)).thenReturn(true);

        factory.init(config);

        assertThat(factory).isNotNull();
    }

    @Test
    void testInit_ClusterModeWithTopologyRefreshDisabled() {
        setupClusterConfig();
        when(config.getBoolean("clusterTopologyRefresh", true)).thenReturn(false);
        when(config.getBoolean("clusterAdaptiveRefresh", true)).thenReturn(false);
        when(config.getInt("clusterRefreshInterval", 300)).thenReturn(300);
        when(config.getBoolean("validateClusterMembership", true)).thenReturn(true);

        factory.init(config);

        assertThat(factory).isNotNull();
    }

    @Test
    void testInit_ClusterModeWithCustomRefreshInterval() {
        setupClusterConfig();
        when(config.getBoolean("clusterTopologyRefresh", true)).thenReturn(true);
        when(config.getBoolean("clusterAdaptiveRefresh", true)).thenReturn(true);
        when(config.getInt("clusterRefreshInterval", 300)).thenReturn(60); // 1 minute
        when(config.getBoolean("validateClusterMembership", true)).thenReturn(true);

        factory.init(config);

        assertThat(factory).isNotNull();
    }

    @Test
    void testInit_ClusterModeWithVeryShortRefreshInterval() {
        setupClusterConfig();
        when(config.getBoolean("clusterTopologyRefresh", true)).thenReturn(true);
        when(config.getBoolean("clusterAdaptiveRefresh", true)).thenReturn(true);
        when(config.getInt("clusterRefreshInterval", 300)).thenReturn(10); // 10 seconds
        when(config.getBoolean("validateClusterMembership", true)).thenReturn(true);

        factory.init(config);

        assertThat(factory).isNotNull();
    }

    @Test
    void testInit_ClusterModeWithLongRefreshInterval() {
        setupClusterConfig();
        when(config.getBoolean("clusterTopologyRefresh", true)).thenReturn(true);
        when(config.getBoolean("clusterAdaptiveRefresh", true)).thenReturn(true);
        when(config.getInt("clusterRefreshInterval", 300)).thenReturn(3600); // 1 hour
        when(config.getBoolean("validateClusterMembership", true)).thenReturn(true);

        factory.init(config);

        assertThat(factory).isNotNull();
    }

    @Test
    void testInit_ClusterModeWithValidateMembershipDisabled() {
        setupClusterConfig();
        when(config.getBoolean("clusterTopologyRefresh", true)).thenReturn(true);
        when(config.getBoolean("clusterAdaptiveRefresh", true)).thenReturn(true);
        when(config.getInt("clusterRefreshInterval", 300)).thenReturn(300);
        when(config.getBoolean("validateClusterMembership", true)).thenReturn(false); // For SSH tunnels

        factory.init(config);

        assertThat(factory).isNotNull();
    }

    @Test
    void testInit_ClusterModeWithPeriodicOnlyRefresh() {
        setupClusterConfig();
        when(config.getBoolean("clusterTopologyRefresh", true)).thenReturn(true);
        when(config.getBoolean("clusterAdaptiveRefresh", true)).thenReturn(false); // Periodic only
        when(config.getInt("clusterRefreshInterval", 300)).thenReturn(300);
        when(config.getBoolean("validateClusterMembership", true)).thenReturn(true);

        factory.init(config);

        assertThat(factory).isNotNull();
    }

    @Test
    void testInit_ClusterModeWithAdaptiveOnlyRefresh() {
        setupClusterConfig();
        when(config.getBoolean("clusterTopologyRefresh", true)).thenReturn(false); // No periodic
        when(config.getBoolean("clusterAdaptiveRefresh", true)).thenReturn(true); // Adaptive only
        when(config.getInt("clusterRefreshInterval", 300)).thenReturn(300);
        when(config.getBoolean("validateClusterMembership", true)).thenReturn(true);

        factory.init(config);

        assertThat(factory).isNotNull();
    }

    @Test
    void testInit_ClusterModeWithAllRefreshDisabled() {
        setupClusterConfig();
        when(config.getBoolean("clusterTopologyRefresh", true)).thenReturn(false);
        when(config.getBoolean("clusterAdaptiveRefresh", true)).thenReturn(false);
        when(config.getInt("clusterRefreshInterval", 300)).thenReturn(300);
        when(config.getBoolean("validateClusterMembership", true)).thenReturn(true);

        factory.init(config);

        assertThat(factory).isNotNull();
    }

    // ========== Standalone Mode Should Ignore Cluster Settings Tests ==========

    @Test
    void testInit_StandaloneModeIgnoresTopologyRefreshSettings() {
        setupStandaloneConfig();
        when(config.getBoolean("clusterTopologyRefresh", true)).thenReturn(true);
        when(config.getBoolean("clusterAdaptiveRefresh", true)).thenReturn(true);
        when(config.getInt("clusterRefreshInterval", 300)).thenReturn(300);
        when(config.getBoolean("validateClusterMembership", true)).thenReturn(true);

        factory.init(config);

        // Should not throw - topology settings should be ignored in standalone mode
        assertThat(factory).isNotNull();
    }

    // ========== Edge Case Tests ==========

    @Test
    void testInit_ClusterModeWithZeroRefreshInterval() {
        setupClusterConfig();
        when(config.getBoolean("clusterTopologyRefresh", true)).thenReturn(true);
        when(config.getBoolean("clusterAdaptiveRefresh", true)).thenReturn(true);
        when(config.getInt("clusterRefreshInterval", 300)).thenReturn(0); // Edge case
        when(config.getBoolean("validateClusterMembership", true)).thenReturn(true);

        factory.init(config);

        assertThat(factory).isNotNull();
    }

    @Test
    void testInit_ClusterModeWithNegativeRefreshInterval() {
        setupClusterConfig();
        when(config.getBoolean("clusterTopologyRefresh", true)).thenReturn(true);
        when(config.getBoolean("clusterAdaptiveRefresh", true)).thenReturn(true);
        when(config.getInt("clusterRefreshInterval", 300)).thenReturn(-1); // Edge case
        when(config.getBoolean("validateClusterMembership", true)).thenReturn(true);

        factory.init(config);

        assertThat(factory).isNotNull();
    }

    @Test
    void testInit_ClusterModeWithSSLAndTopologyRefresh() {
        setupClusterConfig();
        when(config.getBoolean("ssl", false)).thenReturn(true); // SSL enabled
        when(config.getBoolean("clusterTopologyRefresh", true)).thenReturn(true);
        when(config.getBoolean("clusterAdaptiveRefresh", true)).thenReturn(true);
        when(config.getInt("clusterRefreshInterval", 300)).thenReturn(300);
        when(config.getBoolean("validateClusterMembership", true)).thenReturn(true);

        factory.init(config);

        assertThat(factory).isNotNull();
    }

    @Test
    void testInit_ClusterModeWithPasswordAndTopologyRefresh() {
        setupClusterConfig();
        when(config.get("password")).thenReturn("secretPassword123");
        when(config.getBoolean("clusterTopologyRefresh", true)).thenReturn(true);
        when(config.getBoolean("clusterAdaptiveRefresh", true)).thenReturn(true);
        when(config.getInt("clusterRefreshInterval", 300)).thenReturn(300);
        when(config.getBoolean("validateClusterMembership", true)).thenReturn(true);

        factory.init(config);

        assertThat(factory).isNotNull();
    }

    // ========== Configuration Combination Tests ==========

    @Test
    void testInit_AllClusterFeaturesEnabled() {
        setupClusterConfig();
        when(config.getBoolean("ssl", false)).thenReturn(true);
        when(config.get("password")).thenReturn("password123");
        when(config.getBoolean("clusterTopologyRefresh", true)).thenReturn(true);
        when(config.getBoolean("clusterAdaptiveRefresh", true)).thenReturn(true);
        when(config.getInt("clusterRefreshInterval", 300)).thenReturn(180);
        when(config.getBoolean("validateClusterMembership", true)).thenReturn(false);

        factory.init(config);

        assertThat(factory).isNotNull();
    }

    @Test
    void testInit_MinimalClusterConfiguration() {
        setupClusterConfig();
        when(config.getBoolean("clusterTopologyRefresh", true)).thenReturn(false);
        when(config.getBoolean("clusterAdaptiveRefresh", true)).thenReturn(false);
        when(config.getInt("clusterRefreshInterval", 300)).thenReturn(300);
        when(config.getBoolean("validateClusterMembership", true)).thenReturn(false);

        factory.init(config);

        assertThat(factory).isNotNull();
    }

    // ========== Helper Methods ==========

    private void setupClusterConfig() {
        when(config.get("host", "localhost")).thenReturn("redis-cluster.example.com");
        when(config.getInt("port", 6379)).thenReturn(6379);
        when(config.get("password")).thenReturn(null);
        when(config.getInt("database", 0)).thenReturn(0);
        when(config.getBoolean("ssl", false)).thenReturn(false);
        when(config.getBoolean("cluster", false)).thenReturn(true); // CLUSTER MODE
        when(config.getInt("timeout", 5000)).thenReturn(5000);
        when(config.get("keyPrefix", "kc:")).thenReturn("kc:");
    }

    private void setupStandaloneConfig() {
        when(config.get("host", "localhost")).thenReturn("localhost");
        when(config.getInt("port", 6379)).thenReturn(6379);
        when(config.get("password")).thenReturn(null);
        when(config.getInt("database", 0)).thenReturn(0);
        when(config.getBoolean("ssl", false)).thenReturn(false);
        when(config.getBoolean("cluster", false)).thenReturn(false); // STANDALONE MODE
        when(config.getInt("timeout", 5000)).thenReturn(5000);
        when(config.get("keyPrefix", "kc:")).thenReturn("kc:");
    }
}
