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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.redis.AbstractRedisProviderFactory;
import org.keycloak.models.redis.RedisConnectionProvider;
import org.keycloak.provider.Provider;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AbstractRedisProviderFactory.
 */
class AbstractRedisProviderFactoryTest {

    // Concrete test implementation of the abstract class
    private static class TestRedisProviderFactory extends AbstractRedisProviderFactory<TestProvider> {
        
        private boolean initCalled = false;
        private boolean createCalled = false;
        
        @Override
        public TestProvider create(KeycloakSession session) {
            createCalled = true;
            RedisConnectionProvider redis = getRedisProvider(session);
            return new TestProvider(redis);
        }

        @Override
        public void init(Config.Scope config) {
            initCalled = true;
        }
        
        // Expose protected method for testing
        public RedisConnectionProvider testGetRedisProvider(KeycloakSession session) {
            return getRedisProvider(session);
        }
        
        // Expose protected constants for testing
        public static String getProviderId() {
            return PROVIDER_ID;
        }
        
        public static int getProviderOrder() {
            return REDIS_PROVIDER_ORDER;
        }
    }

    // Test provider implementation
    private static class TestProvider implements Provider {
        private final RedisConnectionProvider redis;

        TestProvider(RedisConnectionProvider redis) {
            this.redis = redis;
        }

        @Override
        public void close() {
            // Test implementation
        }

        public RedisConnectionProvider getRedis() {
            return redis;
        }
    }

    private TestRedisProviderFactory factory;
    private KeycloakSession session;
    private KeycloakSessionFactory sessionFactory;
    private RedisConnectionProvider redisProvider;
    private Config.Scope config;

    @BeforeEach
    void setUp() {
        factory = new TestRedisProviderFactory();
        session = mock(KeycloakSession.class);
        sessionFactory = mock(KeycloakSessionFactory.class);
        redisProvider = mock(RedisConnectionProvider.class);
        config = mock(Config.Scope.class);
    }

    @Test
    void testGetId() {
        String id = factory.getId();
        
        assertThat(id).isEqualTo("redis");
        assertThat(id).isNotNull();
        assertThat(id).isNotEmpty();
    }

    @Test
    void testGetId_ReturnsConstantValue() {
        // Verify getId is consistent
        String id1 = factory.getId();
        String id2 = factory.getId();
        
        assertThat(id1).isEqualTo(id2);
        assertThat(id1).isEqualTo("redis");
    }

    @Test
    void testOrder() {
        int order = factory.order();
        
        assertThat(order).isEqualTo(10);
    }

    @Test
    void testOrder_HigherThanDefault() {
        // Verify Redis provider has higher priority than default (0)
        assertThat(factory.order()).isGreaterThan(0);
    }

    @Test
    void testPostInit_DoesNotThrow() {
        assertThatCode(() -> factory.postInit(sessionFactory))
                .doesNotThrowAnyException();
    }

    @Test
    void testPostInit_WithNullFactory() {
        assertThatCode(() -> factory.postInit(null))
                .doesNotThrowAnyException();
    }

    @Test
    void testClose_DoesNotThrow() {
        assertThatCode(() -> factory.close())
                .doesNotThrowAnyException();
    }

    @Test
    void testClose_MultipleTimes() {
        assertThatCode(() -> {
            factory.close();
            factory.close();
            factory.close();
        }).doesNotThrowAnyException();
    }

    @Test
    void testGetRedisProvider_Success() {
        when(session.getProvider(RedisConnectionProvider.class)).thenReturn(redisProvider);
        
        RedisConnectionProvider result = factory.testGetRedisProvider(session);
        
        assertThat(result).isNotNull();
        assertThat(result).isSameAs(redisProvider);
        verify(session).getProvider(RedisConnectionProvider.class);
    }

    @Test
    void testGetRedisProvider_ThrowsWhenNotAvailable() {
        when(session.getProvider(RedisConnectionProvider.class)).thenReturn(null);
        
        assertThatThrownBy(() -> factory.testGetRedisProvider(session))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("RedisConnectionProvider not available");
    }

    @Test
    void testGetRedisProvider_ThrowsWithNullSession() {
        assertThatThrownBy(() -> factory.testGetRedisProvider(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void testCreate_UsesGetRedisProvider() {
        when(session.getProvider(RedisConnectionProvider.class)).thenReturn(redisProvider);
        
        TestProvider provider = factory.create(session);
        
        assertThat(provider).isNotNull();
        assertThat(provider.getRedis()).isSameAs(redisProvider);
        assertThat(factory.createCalled).isTrue();
    }

    @Test
    void testCreate_FailsWhenRedisNotAvailable() {
        when(session.getProvider(RedisConnectionProvider.class)).thenReturn(null);
        
        assertThatThrownBy(() -> factory.create(session))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("RedisConnectionProvider not available");
    }

    @Test
    void testInit_CanBeOverridden() {
        factory.init(config);
        
        assertThat(factory.initCalled).isTrue();
    }

    @Test
    void testInit_WithNullConfig() {
        assertThatCode(() -> factory.init(null))
                .doesNotThrowAnyException();
    }

    @Test
    void testProviderIdConstant() {
        // Verify the constant is accessible through the base class
        assertThat(TestRedisProviderFactory.getProviderId()).isEqualTo("redis");
    }

    @Test
    void testProviderOrderConstant() {
        // Verify the order constant is accessible through the base class
        assertThat(TestRedisProviderFactory.getProviderOrder()).isEqualTo(10);
    }

    @Test
    void testLifecycleFlow() {
        // Test typical lifecycle: init -> create -> close
        factory.init(config);
        assertThat(factory.initCalled).isTrue();
        
        when(session.getProvider(RedisConnectionProvider.class)).thenReturn(redisProvider);
        TestProvider provider = factory.create(session);
        assertThat(provider).isNotNull();
        
        assertThatCode(() -> factory.close()).doesNotThrowAnyException();
    }

    @Test
    void testMultipleCreateCalls() {
        when(session.getProvider(RedisConnectionProvider.class)).thenReturn(redisProvider);
        
        TestProvider provider1 = factory.create(session);
        TestProvider provider2 = factory.create(session);
        
        assertThat(provider1).isNotNull();
        assertThat(provider2).isNotNull();
        // Each create should return a new instance
        assertThat(provider1).isNotSameAs(provider2);
    }

    @Test
    void testGetRedisProvider_ErrorMessageIsDescriptive() {
        when(session.getProvider(RedisConnectionProvider.class)).thenReturn(null);
        
        assertThatThrownBy(() -> factory.testGetRedisProvider(session))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("RedisConnectionProvider not available");
    }

    @Test
    void testPostInit_DoesNotModifyState() {
        factory.init(config);
        boolean initStateBeforePostInit = factory.initCalled;
        
        factory.postInit(sessionFactory);
        
        // postInit should not change init state
        assertThat(factory.initCalled).isEqualTo(initStateBeforePostInit);
    }

    @Test
    void testClose_DoesNotAffectFutureOperations() {
        when(session.getProvider(RedisConnectionProvider.class)).thenReturn(redisProvider);
        
        factory.close();
        
        // After close, create should still work
        assertThatCode(() -> factory.create(session)).doesNotThrowAnyException();
    }

    @Test
    void testGetId_UsesProviderId() {
        // Verify getId returns the PROVIDER_ID constant
        assertThat(factory.getId()).isEqualTo(TestRedisProviderFactory.getProviderId());
    }

    @Test
    void testOrder_UsesOrderConstant() {
        // Verify order() returns the REDIS_PROVIDER_ORDER constant
        assertThat(factory.order()).isEqualTo(TestRedisProviderFactory.getProviderOrder());
    }

    @Test
    void testGetRedisProvider_CalledMultipleTimes() {
        when(session.getProvider(RedisConnectionProvider.class)).thenReturn(redisProvider);
        
        RedisConnectionProvider result1 = factory.testGetRedisProvider(session);
        RedisConnectionProvider result2 = factory.testGetRedisProvider(session);
        
        assertThat(result1).isSameAs(redisProvider);
        assertThat(result2).isSameAs(redisProvider);
        verify(session, times(2)).getProvider(RedisConnectionProvider.class);
    }

    @Test
    void testProtectedConstantsAccessibility() {
        // Verify protected constants are accessible in subclasses
        TestRedisProviderFactory testFactory = new TestRedisProviderFactory();
        
        // These should be accessible through the concrete class
        assertThat(TestRedisProviderFactory.getProviderId()).isNotNull();
        assertThat(TestRedisProviderFactory.getProviderOrder()).isGreaterThan(0);
    }
}
