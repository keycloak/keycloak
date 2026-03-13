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

package org.keycloak.models.redis.entities;

import org.junit.jupiter.api.Test;
import org.keycloak.models.redis.entities.RedisLoginFailureEntity;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for RedisLoginFailureEntity.
 */
class RedisLoginFailureEntityTest {

    @Test
    void testDefaultConstructor() {
        RedisLoginFailureEntity entity = new RedisLoginFailureEntity();
        assertThat(entity).isNotNull();
        assertThat(entity.getRealmId()).isNull();
        assertThat(entity.getUserId()).isNull();
    }

    @Test
    void testParameterizedConstructor() {
        RedisLoginFailureEntity entity = new RedisLoginFailureEntity("realm1", "user1");
        
        assertThat(entity.getRealmId()).isEqualTo("realm1");
        assertThat(entity.getUserId()).isEqualTo("user1");
    }

    @Test
    void testGettersAndSetters() {
        RedisLoginFailureEntity entity = new RedisLoginFailureEntity();
        
        entity.setRealmId("realm123");
        assertThat(entity.getRealmId()).isEqualTo("realm123");
        
        entity.setUserId("user456");
        assertThat(entity.getUserId()).isEqualTo("user456");
        
        entity.setFailedLoginNotBefore(100);
        assertThat(entity.getFailedLoginNotBefore()).isEqualTo(100);
        
        entity.setNumFailures(5);
        assertThat(entity.getNumFailures()).isEqualTo(5);
        
        entity.setNumTemporaryLockouts(2);
        assertThat(entity.getNumTemporaryLockouts()).isEqualTo(2);
        
        entity.setLastFailure(1234567890L);
        assertThat(entity.getLastFailure()).isEqualTo(1234567890L);
        
        entity.setLastIPFailure("192.168.1.1");
        assertThat(entity.getLastIPFailure()).isEqualTo("192.168.1.1");
    }

    @Test
    void testClearFailures() {
        RedisLoginFailureEntity entity = new RedisLoginFailureEntity();
        entity.setNumFailures(10);
        entity.setLastFailure(12345L);
        entity.setLastIPFailure("10.0.0.1");
        
        entity.clearFailures();
        
        assertThat(entity.getNumFailures()).isZero();
        assertThat(entity.getLastFailure()).isZero();
        assertThat(entity.getLastIPFailure()).isNull();
    }

    @Test
    void testCreateKey() {
        String key = RedisLoginFailureEntity.createKey("realm1", "user1");
        assertThat(key).isEqualTo("realm1:user1");
    }

    @Test
    void testCreateKey_WithSpecialCharacters() {
        String key = RedisLoginFailureEntity.createKey("realm:special", "user@domain");
        assertThat(key).isEqualTo("realm:special:user@domain");
    }
}
