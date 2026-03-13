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

import org.junit.jupiter.api.Test;
import org.keycloak.models.redis.RedisConnectionProviderFactory;
import org.keycloak.models.redis.RedisConnectionSpi;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for RedisConnectionSpi.
 */
class RedisConnectionSpiTest {

    @Test
    void testGetName() {
        RedisConnectionSpi spi = new RedisConnectionSpi();
        assertThat(spi.getName()).isEqualTo("redis-connection");
    }

    @Test
    void testGetProviderClass() {
        RedisConnectionSpi spi = new RedisConnectionSpi();
        assertThat(spi.getProviderClass()).isEqualTo(org.keycloak.models.redis.RedisConnectionProvider.class);
    }

    @Test
    void testGetProviderFactoryClass() {
        RedisConnectionSpi spi = new RedisConnectionSpi();
        assertThat(spi.getProviderFactoryClass()).isEqualTo(RedisConnectionProviderFactory.class);
    }

    @Test
    void testIsInternal() {
        RedisConnectionSpi spi = new RedisConnectionSpi();
        assertThat(spi.isInternal()).isTrue();
    }
}
