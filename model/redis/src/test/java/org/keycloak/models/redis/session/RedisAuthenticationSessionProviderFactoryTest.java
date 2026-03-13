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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.redis.RedisConnectionProvider;
import org.keycloak.models.redis.session.RedisAuthenticationSessionProviderFactory;
import org.keycloak.sessions.AuthenticationSessionProvider;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RedisAuthenticationSessionProviderFactory.
 */
class RedisAuthenticationSessionProviderFactoryTest {

    private RedisAuthenticationSessionProviderFactory factory;
    private Config.Scope config;
    private KeycloakSession session;
    private RedisConnectionProvider redis;

    @BeforeEach
    void setUp() {
        factory = new RedisAuthenticationSessionProviderFactory();
        config = mock(Config.Scope.class);
        session = mock(KeycloakSession.class);
        redis = mock(RedisConnectionProvider.class);
    }

    @Test
    void testGetId() {
        assertThat(factory.getId()).isEqualTo("redis");
    }

    @Test
    void testInit() {
        // init() is now a no-op for lifespan config; just verify it doesn't throw
        factory.init(config);

        // No config reads expected — TTL is realm-based now
        verifyNoInteractions(config);
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
        factory.init(config);

        when(session.getProvider(RedisConnectionProvider.class)).thenReturn(redis);

        AuthenticationSessionProvider provider = factory.create(session);

        assertThat(provider).isNotNull();
        verify(session).getProvider(RedisConnectionProvider.class);
    }

    @Test
    void testCreate_RedisProviderNotAvailable() {
        factory.init(config);

        when(session.getProvider(RedisConnectionProvider.class)).thenReturn(null);

        assertThatThrownBy(() -> factory.create(session))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("RedisConnectionProvider not available");
    }

    @Test
    void testOrder() {
        int order = factory.order();
        assertThat(order).isEqualTo(10);
    }

    @Test
    void testCreate_WithoutInit() {
        // Factory should work even without explicit init
        when(session.getProvider(RedisConnectionProvider.class)).thenReturn(redis);

        AuthenticationSessionProvider provider = factory.create(session);

        assertThat(provider).isNotNull();
    }
}
