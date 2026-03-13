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

package org.keycloak.models.redis.session;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.redis.RedisConnectionProvider;
import org.keycloak.models.redis.session.RedisUserSessionProvider;
import org.keycloak.models.redis.session.RedisUserSessionProviderFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.keycloak.models.redis.TestConstants.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RedisUserSessionProviderFactory.
 */
class RedisUserSessionProviderFactoryTest {

    private RedisUserSessionProviderFactory factory;
    private Config.Scope config;
    private KeycloakSession session;
    private RedisConnectionProvider redis;

    @BeforeAll
    static void initProfile() {
        // Initialize Profile with REDIS_STORAGE feature enabled
        Map<Profile.Feature, Boolean> features = new HashMap<>();
        features.put(Profile.Feature.REDIS_STORAGE, true);
        Profile.init(Profile.ProfileName.DEFAULT, features);
    }

    @AfterAll
    static void resetProfile() {
        // Clean up Profile after all tests
        Profile.reset();
    }

    @BeforeEach
    void setUp() {
        factory = new RedisUserSessionProviderFactory();
        config = mock(Config.Scope.class);
        session = mock(KeycloakSession.class);
        redis = mock(RedisConnectionProvider.class);
    }

    @Test
    void testGetId() {
        assertThat(factory.getId()).isEqualTo("redis");
    }

    @Test
    void testInit_WithDefaultValues() {
        when(config.getInt("sessionLifespan", 36000)).thenReturn(36000);
        when(config.getInt("offlineSessionLifespan", 5184000)).thenReturn(5184000);

        factory.init(config);

        // Verify initialization succeeded (no exceptions thrown)
        assertThat(factory).isNotNull();
    }

    @Test
    void testInit_WithCustomValues() {
        when(config.getInt("sessionLifespan", 36000)).thenReturn(SSO_SESSION_MAX_LIFESPAN);
        when(config.getInt("offlineSessionLifespan", 5184000)).thenReturn(OFFLINE_SESSION_IDLE_TIMEOUT);

        factory.init(config);

        // Verify factory can create provider after init
        when(session.getProvider(RedisConnectionProvider.class)).thenReturn(redis);
        RedisUserSessionProvider provider = factory.create(session);
        assertThat(provider).isNotNull();
    }

    @Test
    void testPostInit() {
        KeycloakSessionFactory sessionFactory = mock(KeycloakSessionFactory.class);
        
        // Should not throw
        factory.postInit(sessionFactory);
    }

    @Test
    void testClose() {
        // Should not throw
        factory.close();
    }

    @Test
    void testCreate_Success() {
        when(config.getInt("sessionLifespan", 36000)).thenReturn(36000);
        when(config.getInt("offlineSessionLifespan", 5184000)).thenReturn(5184000);
        factory.init(config);

        when(session.getProvider(RedisConnectionProvider.class)).thenReturn(redis);

        RedisUserSessionProvider provider = factory.create(session);

        assertThat(provider).isNotNull();
        verify(session).getProvider(RedisConnectionProvider.class);
    }

    @Test
    void testCreate_RedisProviderNotAvailable() {
        when(config.getInt("sessionLifespan", 36000)).thenReturn(36000);
        when(config.getInt("offlineSessionLifespan", 5184000)).thenReturn(5184000);
        factory.init(config);

        when(session.getProvider(RedisConnectionProvider.class)).thenReturn(null);

        assertThatThrownBy(() -> factory.create(session))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("RedisConnectionProvider not available");
    }

    @Test
    void testIsSupported() {
        boolean supported = factory.isSupported(config);
        assertThat(supported).isTrue();
    }

    @Test
    void testOrder() {
        int order = factory.order();
        assertThat(order).isEqualTo(10);
    }

    @Test
    void testGetConfigMetadata() {
        List<ProviderConfigProperty> metadata = factory.getConfigMetadata();

        assertThat(metadata).isNotNull();
        assertThat(metadata).hasSize(2);
        
        assertThat(metadata.get(0).getName()).isEqualTo("sessionLifespan");
        assertThat(metadata.get(0).getType()).isEqualTo("int");
        assertThat(metadata.get(0).getDefaultValue()).isEqualTo("36000");
        
        assertThat(metadata.get(1).getName()).isEqualTo("offlineSessionLifespan");
        assertThat(metadata.get(1).getType()).isEqualTo("int");
        assertThat(metadata.get(1).getDefaultValue()).isEqualTo("5184000");
    }

    @Test
    void testGetOperationalInfo() {
        when(config.getInt("sessionLifespan", 36000)).thenReturn(7200);
        when(config.getInt("offlineSessionLifespan", 5184000)).thenReturn(2592000);
        factory.init(config);

        Map<String, String> info = factory.getOperationalInfo();

        assertThat(info).isNotNull();
        assertThat(info).containsEntry("provider", "redis");
        assertThat(info).containsEntry("sessionLifespan", "7200");
        assertThat(info).containsEntry("offlineSessionLifespan", "2592000");
    }

    @Test
    void testGetOperationalInfo_WithDefaults() {
        when(config.getInt("sessionLifespan", 36000)).thenReturn(36000);
        when(config.getInt("offlineSessionLifespan", 5184000)).thenReturn(5184000);
        factory.init(config);

        Map<String, String> info = factory.getOperationalInfo();

        assertThat(info).containsEntry("sessionLifespan", "36000");
        assertThat(info).containsEntry("offlineSessionLifespan", "5184000");
    }
}
