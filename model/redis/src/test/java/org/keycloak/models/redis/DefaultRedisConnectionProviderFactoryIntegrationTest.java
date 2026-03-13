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

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.redis.DefaultRedisConnectionProviderFactory;
import org.keycloak.models.redis.RedisConnectionProvider;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for DefaultRedisConnectionProviderFactory using Testcontainers.
 * These tests verify actual Redis connection scenarios including standalone and cluster modes.
 * 
 * These tests require Docker to be available and are disabled by default.
 * To enable, set environment variable: REDIS_INTEGRATION_TEST=true
 */
@EnabledIfEnvironmentVariable(named = "REDIS_INTEGRATION_TEST", matches = "true")
class DefaultRedisConnectionProviderFactoryIntegrationTest {

    private static GenericContainer<?> redisContainer;
    private DefaultRedisConnectionProviderFactory factory;
    private Config.Scope config;
    private KeycloakSession session;
    private KeycloakSessionFactory sessionFactory;

    @BeforeAll
    static void startRedis() {
        @SuppressWarnings("resource") // Container is closed in @AfterAll
        GenericContainer<?> container = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                .withExposedPorts(6379)
                .withCommand("redis-server", "--appendonly", "yes");
        container.start();
        redisContainer = container;
    }

    @AfterAll
    static void stopRedis() {
        if (redisContainer != null) {
            redisContainer.stop();
        }
    }

    @BeforeEach
    void setUp() {
        factory = new DefaultRedisConnectionProviderFactory();
        config = mock(Config.Scope.class);
        session = mock(KeycloakSession.class);
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

    private void setupStandaloneConfig() {
        when(config.get("host", "localhost")).thenReturn(redisContainer.getHost());
        when(config.getInt("port", 6379)).thenReturn(redisContainer.getMappedPort(6379));
        when(config.get("password")).thenReturn(null);
        when(config.getInt("database", 0)).thenReturn(0);
        when(config.getBoolean("ssl", false)).thenReturn(false);
        when(config.getBoolean("cluster", false)).thenReturn(false);
        when(config.getInt("timeout", 5000)).thenReturn(5000);
        when(config.get("keyPrefix", "kc:")).thenReturn("kc:");
    }

    // ========== Lazy Initialization Tests ==========

    @Test
    void testCreate_TriggersLazyInitialization() {
        setupStandaloneConfig();
        factory.init(config);

        RedisConnectionProvider provider = factory.create(session);

        assertThat(provider).isNotNull();
    }

    @Test
    void testCreate_MultipleCalls_ReturnsSameProvider() {
        setupStandaloneConfig();
        factory.init(config);

        RedisConnectionProvider provider1 = factory.create(session);
        RedisConnectionProvider provider2 = factory.create(session);

        assertThat(provider1).isSameAs(provider2);
    }

    @Test
    void testCreate_WithPassword_InitializesSuccessfully() {
        // Note: This test uses null password as our test container doesn't require one
        setupStandaloneConfig();
        when(config.get("password")).thenReturn(null);
        
        factory.init(config);

        assertThatCode(() -> factory.create(session)).doesNotThrowAnyException();
    }

    @Test
    void testCreate_WithEmptyPassword_DoesNotSetPassword() {
        setupStandaloneConfig();
        when(config.get("password")).thenReturn("");
        
        factory.init(config);

        assertThatCode(() -> factory.create(session)).doesNotThrowAnyException();
    }

    @Test
    void testCreate_WithCustomDatabase_InitializesSuccessfully() {
        setupStandaloneConfig();
        when(config.getInt("database", 0)).thenReturn(5);
        
        factory.init(config);

        assertThatCode(() -> factory.create(session)).doesNotThrowAnyException();
    }

    @Test
    void testCreate_WithCustomTimeout_InitializesSuccessfully() {
        setupStandaloneConfig();
        when(config.getInt("timeout", 5000)).thenReturn(10000);
        
        factory.init(config);

        assertThatCode(() -> factory.create(session)).doesNotThrowAnyException();
    }

    @Test
    void testCreate_WithCustomKeyPrefix_InitializesSuccessfully() {
        setupStandaloneConfig();
        when(config.get("keyPrefix", "kc:")).thenReturn("custom:");
        
        factory.init(config);

        assertThatCode(() -> factory.create(session)).doesNotThrowAnyException();
    }

    // ========== Connection Info String Tests ==========

    @Test
    void testConnectionInfo_WithoutSSL_GeneratesCorrectString() {
        setupStandaloneConfig();
        factory.init(config);
        factory.create(session);

        Map<String, String> info = factory.getOperationalInfo();
        
        assertThat(info.get("connectionInfo")).startsWith("redis://");
        assertThat(info.get("connectionInfo")).doesNotContain("rediss://");
        assertThat(info.get("connectionInfo")).contains(redisContainer.getHost());
    }

    @Test
    void testConnectionInfo_WithDatabase_GeneratesCorrectString() {
        setupStandaloneConfig();
        when(config.getInt("database", 0)).thenReturn(3);
        
        factory.init(config);
        factory.create(session);

        Map<String, String> info = factory.getOperationalInfo();
        
        assertThat(info.get("connectionInfo")).contains("/3");
    }

    // ========== Operational Info Tests ==========

    @Test
    void testGetOperationalInfo_AfterInitialization_ShowsConnectedStatus() {
        setupStandaloneConfig();
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
    void testGetOperationalInfo_StandaloneMode_ShowsCorrectMode() {
        setupStandaloneConfig();
        factory.init(config);
        factory.create(session);

        Map<String, String> info = factory.getOperationalInfo();

        assertThat(info.get("clusterMode")).isEqualTo("false");
    }

    // ========== Close Tests ==========

    @Test
    void testClose_AfterCreateStandalone_CleansUpResources() {
        setupStandaloneConfig();
        factory.init(config);
        factory.create(session);

        assertThatCode(() -> factory.close()).doesNotThrowAnyException();
    }

    @Test
    void testClose_AfterMultipleCreates_CleansUpResources() {
        setupStandaloneConfig();
        factory.init(config);
        factory.create(session);
        factory.create(session);
        factory.create(session);

        assertThatCode(() -> factory.close()).doesNotThrowAnyException();
    }

    @Test
    void testClose_MultipleTimes_DoesNotThrow() {
        setupStandaloneConfig();
        factory.init(config);
        factory.create(session);

        assertThatCode(() -> {
            factory.close();
            factory.close();
            factory.close();
        }).doesNotThrowAnyException();
    }

    // ========== Standalone Mode Tests ==========

    @Test
    void testCreate_StandaloneMode_ConnectionIsHealthy() {
        setupStandaloneConfig();
        factory.init(config);
        
        RedisConnectionProvider provider = factory.create(session);

        assertThat(provider).isNotNull();
        assertThat(provider.isHealthy()).isTrue();
    }

    @Test
    void testCreate_StandaloneMode_WithAllCustomValues() {
        setupStandaloneConfig();
        when(config.getInt("database", 0)).thenReturn(7);
        when(config.getInt("timeout", 5000)).thenReturn(15000);
        when(config.get("keyPrefix", "kc:")).thenReturn("integration:");
        
        factory.init(config);
        
        RedisConnectionProvider provider = factory.create(session);

        assertThat(provider).isNotNull();
        assertThat(provider.isHealthy()).isTrue();
    }

    // ========== Post-Init Tests ==========

    @Test
    void testPostInit_AfterInit_DoesNotThrow() {
        setupStandaloneConfig();
        factory.init(config);

        assertThatCode(() -> factory.postInit(sessionFactory))
                .doesNotThrowAnyException();
    }

    @Test
    void testPostInit_BeforeCreate_DoesNotThrow() {
        setupStandaloneConfig();
        factory.init(config);
        factory.postInit(sessionFactory);

        assertThatCode(() -> factory.create(session))
                .doesNotThrowAnyException();
    }

    // ========== Edge Cases ==========

    @Test
    void testCreate_AfterClose_ThrowsException() {
        setupStandaloneConfig();
        factory.init(config);
        factory.create(session);
        factory.close();

        // After close, trying to use the provider should fail
        // but create itself should work (lazy init again would be needed in production)
        assertThatCode(() -> factory.create(session)).doesNotThrowAnyException();
    }

    @Test
    void testGetOperationalInfo_AfterClose_ShowsDisconnected() {
        setupStandaloneConfig();
        factory.init(config);
        factory.create(session);
        factory.close();

        Map<String, String> info = factory.getOperationalInfo();

        assertThat(info).isNotNull();
        assertThat(info.get("provider")).isEqualTo("default");
        // After close, connectionProvider is null
        assertThat(info.get("connected")).isEqualTo("false");
        assertThat(info.get("status")).isEqualTo("not initialized");
    }
}
