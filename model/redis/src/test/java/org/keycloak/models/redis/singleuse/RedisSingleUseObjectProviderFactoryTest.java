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

package org.keycloak.models.redis.singleuse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.SingleUseObjectProvider;
import org.keycloak.models.redis.RedisConnectionProvider;
import org.keycloak.models.redis.singleuse.RedisSingleUseObjectProviderFactory;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RedisSingleUseObjectProviderFactory.
 */
class RedisSingleUseObjectProviderFactoryTest {

    private RedisSingleUseObjectProviderFactory factory;
    private Config.Scope config;

    @BeforeEach
    void setUp() {
        factory = new RedisSingleUseObjectProviderFactory();
        config = mock(Config.Scope.class);
    }

    @Test
    void testGetId() {
        assertThat(factory.getId()).isEqualTo("redis");
    }

    @Test
    void testInit() {
        factory.init(config);
        // Should not throw
    }

    @Test
    void testPostInit() {
        KeycloakSessionFactory sessionFactory = mock(KeycloakSessionFactory.class);
        factory.postInit(sessionFactory);
        // Should not throw
    }

    @Test
    void testCreate() {
        factory.init(config);
        
        KeycloakSession session = mock(KeycloakSession.class);
        RedisConnectionProvider redis = mock(RedisConnectionProvider.class);
        
        when(session.getProvider(RedisConnectionProvider.class)).thenReturn(redis);
        
        SingleUseObjectProvider provider = factory.create(session);
        
        assertThat(provider).isNotNull();
    }

    @Test
    void testClose() {
        factory.close(); // Should not throw
    }

    @Test
    void testOrder() {
        assertThat(factory.order()).isGreaterThan(0);
    }
}
