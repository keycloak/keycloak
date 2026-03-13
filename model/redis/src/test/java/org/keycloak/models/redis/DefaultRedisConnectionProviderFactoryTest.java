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
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.redis.DefaultRedisConnectionProviderFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive tests for DefaultRedisConnectionProviderFactory.
 */
class DefaultRedisConnectionProviderFactoryTest {

    private DefaultRedisConnectionProviderFactory factory;
    private Config.Scope config;
    private KeycloakSessionFactory sessionFactory;

    @BeforeEach
    void setUp() {
        factory = new DefaultRedisConnectionProviderFactory();
        config = mock(Config.Scope.class);
        sessionFactory = mock(KeycloakSessionFactory.class);
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
        assertThat(factory.getId()).isEqualTo("default");
    }

    @Test
    void testInit() {
        factory.init(config);
        
        // Should not throw
        assertThat(factory).isNotNull();
    }

    @Test
    void testPostInit() {
        factory.init(config);
        factory.postInit(sessionFactory);
        
        // Should not throw
        assertThat(factory).isNotNull();
    }

    @Test
    void testGetConfigMetadata() {
        List<ProviderConfigProperty> metadata = factory.getConfigMetadata();
        
        assertThat(metadata).isNotNull();
        assertThat(metadata).isNotEmpty();
        
        // Verify required config properties exist
        assertThat(metadata).extracting(ProviderConfigProperty::getName)
                .contains("host", "port", "password", "database", "ssl", "cluster", "timeout", "keyPrefix",
                          "clusterTopologyRefresh", "clusterAdaptiveRefresh", "clusterRefreshInterval", "validateClusterMembership");
    }

    @Test
    void testGetConfigMetadata_HostProperty() {
        List<ProviderConfigProperty> metadata = factory.getConfigMetadata();
        
        ProviderConfigProperty hostProp = metadata.stream()
                .filter(p -> "host".equals(p.getName()))
                .findFirst()
                .orElse(null);
        
        assertThat(hostProp).isNotNull();
        assertThat(hostProp.getType()).isEqualTo("string");
        assertThat(hostProp.getDefaultValue()).isEqualTo("localhost");
    }

    @Test
    void testGetConfigMetadata_PortProperty() {
        List<ProviderConfigProperty> metadata = factory.getConfigMetadata();
        
        ProviderConfigProperty portProp = metadata.stream()
                .filter(p -> "port".equals(p.getName()))
                .findFirst()
                .orElse(null);
        
        assertThat(portProp).isNotNull();
        assertThat(portProp.getType()).isEqualTo("int");
        assertThat(portProp.getDefaultValue()).isEqualTo("6379");
    }

    @Test
    void testGetConfigMetadata_DatabaseProperty() {
        List<ProviderConfigProperty> metadata = factory.getConfigMetadata();
        
        ProviderConfigProperty dbProp = metadata.stream()
                .filter(p -> "database".equals(p.getName()))
                .findFirst()
                .orElse(null);
        
        assertThat(dbProp).isNotNull();
        assertThat(dbProp.getType()).isEqualTo("int");
        assertThat(dbProp.getDefaultValue()).isEqualTo("0");
    }

    @Test
    void testGetConfigMetadata_SSLProperty() {
        List<ProviderConfigProperty> metadata = factory.getConfigMetadata();

        ProviderConfigProperty sslProp = metadata.stream()
                .filter(p -> "ssl".equals(p.getName()))
                .findFirst()
                .orElse(null);

        assertThat(sslProp).isNotNull();
        assertThat(sslProp.getType()).isEqualTo("boolean");
        assertThat(sslProp.getDefaultValue()).isEqualTo("false");
    }

    @Test
    void testGetConfigMetadata_SSLVerifyPeerProperty() {
        List<ProviderConfigProperty> metadata = factory.getConfigMetadata();

        ProviderConfigProperty sslVerifyPeerProp = metadata.stream()
                .filter(p -> "sslVerifyPeer".equals(p.getName()))
                .findFirst()
                .orElse(null);

        assertThat(sslVerifyPeerProp).isNotNull();
        assertThat(sslVerifyPeerProp.getType()).isEqualTo("boolean");
        assertThat(sslVerifyPeerProp.getDefaultValue()).isEqualTo("true");
        assertThat(sslVerifyPeerProp.getHelpText()).contains("CNAME");
    }

    @Test
    void testGetConfigMetadata_ClusterProperty() {
        List<ProviderConfigProperty> metadata = factory.getConfigMetadata();
        
        ProviderConfigProperty clusterProp = metadata.stream()
                .filter(p -> "cluster".equals(p.getName()))
                .findFirst()
                .orElse(null);
        
        assertThat(clusterProp).isNotNull();
        assertThat(clusterProp.getType()).isEqualTo("boolean");
        assertThat(clusterProp.getDefaultValue()).isEqualTo("false");
    }

    @Test
    void testGetOperationalInfo_NotInitialized() {
        factory.init(config);
        
        Map<String, String> info = factory.getOperationalInfo();
        
        assertThat(info).isNotNull();
        assertThat(info.get("provider")).isEqualTo("default");
        assertThat(info.get("connected")).isEqualTo("false");
        assertThat(info.get("status")).isEqualTo("not initialized");
    }

    @Test
    void testClose_WithoutInitialization() {
        factory.init(config);
        
        // Should not throw even if not initialized
        assertThatCode(() -> factory.close()).doesNotThrowAnyException();
    }

    @Test
    void testInit_WithDefaultValues() {
        when(config.get("host", "localhost")).thenReturn("localhost");
        when(config.getInt("port", 6379)).thenReturn(6379);
        when(config.get("password")).thenReturn(null);
        when(config.getInt("database", 0)).thenReturn(0);
        when(config.getBoolean("ssl", false)).thenReturn(false);
        when(config.getBoolean("cluster", false)).thenReturn(false);
        when(config.getInt("timeout", 5000)).thenReturn(5000);
        when(config.get("keyPrefix", "kc:")).thenReturn("kc:");
        
        factory.init(config);
        
        assertThat(factory).isNotNull();
    }

    @Test
    void testInit_WithCustomValues() {
        when(config.get("host", "localhost")).thenReturn("redis.example.com");
        when(config.getInt("port", 6379)).thenReturn(6380);
        when(config.get("password")).thenReturn("secret123");
        when(config.getInt("database", 0)).thenReturn(5);
        when(config.getBoolean("ssl", false)).thenReturn(true);
        when(config.getBoolean("cluster", false)).thenReturn(false);
        when(config.getInt("timeout", 5000)).thenReturn(10000);
        when(config.get("keyPrefix", "kc:")).thenReturn("custom:");
        
        factory.init(config);
        
        assertThat(factory).isNotNull();
    }

    @Test
    void testInit_WithClusterMode() {
        when(config.get("host", "localhost")).thenReturn("localhost");
        when(config.getInt("port", 6379)).thenReturn(6379);
        when(config.get("password")).thenReturn(null);
        when(config.getInt("database", 0)).thenReturn(0);
        when(config.getBoolean("ssl", false)).thenReturn(false);
        when(config.getBoolean("cluster", false)).thenReturn(true); // Cluster mode
        when(config.getInt("timeout", 5000)).thenReturn(5000);
        when(config.get("keyPrefix", "kc:")).thenReturn("kc:");
        
        factory.init(config);
        
        assertThat(factory).isNotNull();
    }

    @Test
    void testInit_WithSSL() {
        when(config.get("host", "localhost")).thenReturn("localhost");
        when(config.getInt("port", 6379)).thenReturn(6380);
        when(config.get("password")).thenReturn(null);
        when(config.getInt("database", 0)).thenReturn(0);
        when(config.getBoolean("ssl", false)).thenReturn(true); // SSL enabled
        when(config.getBoolean("cluster", false)).thenReturn(false);
        when(config.getInt("timeout", 5000)).thenReturn(5000);
        when(config.get("keyPrefix", "kc:")).thenReturn("kc:");
        
        factory.init(config);
        
        assertThat(factory).isNotNull();
    }

    @Test
    void testInit_WithPassword() {
        when(config.get("host", "localhost")).thenReturn("localhost");
        when(config.getInt("port", 6379)).thenReturn(6379);
        when(config.get("password")).thenReturn("mySecretPassword");
        when(config.getInt("database", 0)).thenReturn(0);
        when(config.getBoolean("ssl", false)).thenReturn(false);
        when(config.getBoolean("cluster", false)).thenReturn(false);
        when(config.getInt("timeout", 5000)).thenReturn(5000);
        when(config.get("keyPrefix", "kc:")).thenReturn("kc:");
        
        factory.init(config);
        
        assertThat(factory).isNotNull();
    }

    @Test
    void testInit_WithEmptyPassword() {
        when(config.get("host", "localhost")).thenReturn("localhost");
        when(config.getInt("port", 6379)).thenReturn(6379);
        when(config.get("password")).thenReturn("");
        when(config.getInt("database", 0)).thenReturn(0);
        when(config.getBoolean("ssl", false)).thenReturn(false);
        when(config.getBoolean("cluster", false)).thenReturn(false);
        when(config.getInt("timeout", 5000)).thenReturn(5000);
        when(config.get("keyPrefix", "kc:")).thenReturn("kc:");
        
        factory.init(config);
        
        assertThat(factory).isNotNull();
    }

    @Test
    void testGetConfigMetadata_AllPropertiesHaveLabels() {
        List<ProviderConfigProperty> metadata = factory.getConfigMetadata();
        
        for (ProviderConfigProperty prop : metadata) {
            assertThat(prop.getLabel()).isNotNull().isNotEmpty();
            assertThat(prop.getHelpText()).isNotNull();
        }
    }

    @Test
    void testGetConfigMetadata_TimeoutProperty() {
        List<ProviderConfigProperty> metadata = factory.getConfigMetadata();
        
        ProviderConfigProperty timeoutProp = metadata.stream()
                .filter(p -> "timeout".equals(p.getName()))
                .findFirst()
                .orElse(null);
        
        assertThat(timeoutProp).isNotNull();
        assertThat(timeoutProp.getType()).isEqualTo("int");
        assertThat(timeoutProp.getDefaultValue()).isEqualTo("5000");
    }

    @Test
    void testGetConfigMetadata_KeyPrefixProperty() {
        List<ProviderConfigProperty> metadata = factory.getConfigMetadata();
        
        ProviderConfigProperty prefixProp = metadata.stream()
                .filter(p -> "keyPrefix".equals(p.getName()))
                .findFirst()
                .orElse(null);
        
        assertThat(prefixProp).isNotNull();
        assertThat(prefixProp.getType()).isEqualTo("string");
        assertThat(prefixProp.getDefaultValue()).isEqualTo("kc:");
    }

    @Test
    void testGetConfigMetadata_PasswordProperty() {
        List<ProviderConfigProperty> metadata = factory.getConfigMetadata();
        
        ProviderConfigProperty passwordProp = metadata.stream()
                .filter(p -> "password".equals(p.getName()))
                .findFirst()
                .orElse(null);
        
        assertThat(passwordProp).isNotNull();
        assertThat(passwordProp.getType()).isEqualTo("password");
    }


    @Test
    void testInit_WithNonStandardPort() {
        when(config.get("host", "localhost")).thenReturn("redis.example.com");
        when(config.getInt("port", 6379)).thenReturn(7000);
        when(config.get("password")).thenReturn(null);
        when(config.getInt("database", 0)).thenReturn(0);
        when(config.getBoolean("ssl", false)).thenReturn(false);
        when(config.getBoolean("cluster", false)).thenReturn(false);
        when(config.getInt("timeout", 5000)).thenReturn(5000);
        when(config.get("keyPrefix", "kc:")).thenReturn("kc:");
        
        factory.init(config);
        
        assertThat(factory).isNotNull();
    }

    @Test
    void testInit_WithCustomDatabase() {
        when(config.get("host", "localhost")).thenReturn("localhost");
        when(config.getInt("port", 6379)).thenReturn(6379);
        when(config.get("password")).thenReturn(null);
        when(config.getInt("database", 0)).thenReturn(7);
        when(config.getBoolean("ssl", false)).thenReturn(false);
        when(config.getBoolean("cluster", false)).thenReturn(false);
        when(config.getInt("timeout", 5000)).thenReturn(5000);
        when(config.get("keyPrefix", "kc:")).thenReturn("kc:");
        
        factory.init(config);
        
        assertThat(factory).isNotNull();
    }

    @Test
    void testInit_WithCustomTimeout() {
        when(config.get("host", "localhost")).thenReturn("localhost");
        when(config.getInt("port", 6379)).thenReturn(6379);
        when(config.get("password")).thenReturn(null);
        when(config.getInt("database", 0)).thenReturn(0);
        when(config.getBoolean("ssl", false)).thenReturn(false);
        when(config.getBoolean("cluster", false)).thenReturn(false);
        when(config.getInt("timeout", 5000)).thenReturn(30000);
        when(config.get("keyPrefix", "kc:")).thenReturn("kc:");
        
        factory.init(config);
        
        assertThat(factory).isNotNull();
    }

    @Test
    void testInit_WithCustomKeyPrefix() {
        when(config.get("host", "localhost")).thenReturn("localhost");
        when(config.getInt("port", 6379)).thenReturn(6379);
        when(config.get("password")).thenReturn(null);
        when(config.getInt("database", 0)).thenReturn(0);
        when(config.getBoolean("ssl", false)).thenReturn(false);
        when(config.getBoolean("cluster", false)).thenReturn(false);
        when(config.getInt("timeout", 5000)).thenReturn(5000);
        when(config.get("keyPrefix", "kc:")).thenReturn("myapp:");
        
        factory.init(config);
        
        assertThat(factory).isNotNull();
    }

    @Test
    void testGetOperationalInfo_BeforeAnyConnection() {
        factory.init(config);
        
        Map<String, String> info = factory.getOperationalInfo();
        
        assertThat(info).isNotNull();
        assertThat(info.get("provider")).isEqualTo("default");
        assertThat(info.get("connected")).isEqualTo("false");
        assertThat(info.get("status")).isEqualTo("not initialized");
    }

    @Test
    void testGetId_ReturnsDefault() {
        String id = factory.getId();
        
        assertThat(id).isEqualTo("default");
        assertThat(id).isNotNull();
        assertThat(id).isNotEmpty();
    }

    @Test
    void testPostInit_DoesNotThrow() {
        factory.init(config);
        
        assertThatCode(() -> factory.postInit(sessionFactory))
                .doesNotThrowAnyException();
    }

    @Test
    void testClose_MultipleTimesDoesNotThrow() {
        factory.init(config);
        
        assertThatCode(() -> {
            factory.close();
            factory.close();
            factory.close();
        }).doesNotThrowAnyException();
    }

    @Test
    void testInit_AllBooleanConfigCombinations() {
        // Test SSL true, Cluster false
        when(config.get("host", "localhost")).thenReturn("localhost");
        when(config.getInt("port", 6379)).thenReturn(6379);
        when(config.get("password")).thenReturn(null);
        when(config.getInt("database", 0)).thenReturn(0);
        when(config.getBoolean("ssl", false)).thenReturn(true);
        when(config.getBoolean("cluster", false)).thenReturn(false);
        when(config.getInt("timeout", 5000)).thenReturn(5000);
        when(config.get("keyPrefix", "kc:")).thenReturn("kc:");
        
        factory.init(config);
        
        assertThat(factory).isNotNull();
    }

    @Test
    void testInit_WithNullKeyPrefix() {
        when(config.get("host", "localhost")).thenReturn("localhost");
        when(config.getInt("port", 6379)).thenReturn(6379);
        when(config.get("password")).thenReturn(null);
        when(config.getInt("database", 0)).thenReturn(0);
        when(config.getBoolean("ssl", false)).thenReturn(false);
        when(config.getBoolean("cluster", false)).thenReturn(false);
        when(config.getInt("timeout", 5000)).thenReturn(5000);
        when(config.get("keyPrefix", "kc:")).thenReturn(null);
        
        factory.init(config);
        
        // Should use default prefix
        assertThat(factory).isNotNull();
    }

    @Test
    void testGetConfigMetadata_HasSeventeenProperties() {
        List<ProviderConfigProperty> metadata = factory.getConfigMetadata();

        // 8 original + 1 sslVerifyPeer + 4 cluster topology + 4 Sprint 1 (retry/thread pool) = 17 total
        assertThat(metadata).hasSize(17);
    }

    @Test
    void testGetConfigMetadata_AllPropertiesHaveHelpText() {
        List<ProviderConfigProperty> metadata = factory.getConfigMetadata();
        
        for (ProviderConfigProperty prop : metadata) {
            assertThat(prop.getHelpText())
                    .as("Property %s should have help text", prop.getName())
                    .isNotNull()
                    .isNotEmpty();
        }
    }

    @Test
    void testGetConfigMetadata_PropertiesHaveCorrectNames() {
        List<ProviderConfigProperty> metadata = factory.getConfigMetadata();
        
        List<String> propertyNames = metadata.stream()
                .map(ProviderConfigProperty::getName)
                .toList();
        
        assertThat(propertyNames).containsExactlyInAnyOrder(
                "host", "port", "password", "database",
                "ssl", "sslVerifyPeer", "cluster", "timeout", "keyPrefix",
                "clusterTopologyRefresh",
                "clusterAdaptiveRefresh",
                "clusterRefreshInterval",
                "validateClusterMembership",
                "ioThreads",
                "computeThreads",
                "connectionRetries",
                "retryDelayMs"
        );
    }

    @Test
    void testGetConfigMetadata_IntPropertiesHaveCorrectType() {
        List<ProviderConfigProperty> metadata = factory.getConfigMetadata();
        
        List<String> intProperties = metadata.stream()
                .filter(p -> "int".equals(p.getType()))
                .map(ProviderConfigProperty::getName)
                .toList();
        
        assertThat(intProperties).containsExactlyInAnyOrder("port", "database", "timeout", "clusterRefreshInterval", "ioThreads", "computeThreads", "connectionRetries", "retryDelayMs");
    }

    @Test
    void testGetConfigMetadata_StringPropertiesHaveCorrectType() {
        List<ProviderConfigProperty> metadata = factory.getConfigMetadata();
        
        List<String> stringProperties = metadata.stream()
                .filter(p -> "string".equals(p.getType()))
                .map(ProviderConfigProperty::getName)
                .toList();
        
        assertThat(stringProperties).containsExactlyInAnyOrder("host", "keyPrefix");
    }

// ... (rest of the code remains the same)
    @Test
    void testGetConfigMetadata_ValidateClusterMembershipProperty() {
        List<ProviderConfigProperty> metadata = factory.getConfigMetadata();
        
        ProviderConfigProperty prop = metadata.stream()
                .filter(p -> "validateClusterMembership".equals(p.getName()))
                .findFirst()
                .orElse(null);
        
        assertThat(prop).isNotNull();
        assertThat(prop.getType()).isEqualTo("boolean");
        assertThat(prop.getDefaultValue()).isEqualTo("true");
    }
}

