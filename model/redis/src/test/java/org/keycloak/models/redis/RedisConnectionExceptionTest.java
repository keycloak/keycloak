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
import org.keycloak.models.redis.RedisConnectionException;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive tests for RedisConnectionException to achieve 100% coverage.
 */
class RedisConnectionExceptionTest {

    @Test
    void testExceptionWithMessage() {
        String errorMessage = "Failed to connect to Redis server";
        
        RedisConnectionException exception = new RedisConnectionException(errorMessage);
        
        assertThat(exception).isNotNull();
        assertThat(exception.getMessage()).isEqualTo(errorMessage);
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void testExceptionWithMessageAndCause() {
        String errorMessage = "Redis connection timeout";
        Throwable cause = new RuntimeException("Network error");
        
        RedisConnectionException exception = new RedisConnectionException(errorMessage, cause);
        
        assertThat(exception).isNotNull();
        assertThat(exception.getMessage()).isEqualTo(errorMessage);
        assertThat(exception.getCause()).isEqualTo(cause);
        assertThat(exception.getCause().getMessage()).isEqualTo("Network error");
    }

    @Test
    void testExceptionIsRuntimeException() {
        RedisConnectionException exception = new RedisConnectionException("test");
        
        assertThat(exception).isInstanceOf(RuntimeException.class);
    }

    @Test
    void testExceptionCanBeThrown() {
        assertThatThrownBy(() -> {
            throw new RedisConnectionException("Connection failed");
        })
        .isInstanceOf(RedisConnectionException.class)
        .hasMessage("Connection failed");
    }

    @Test
    void testExceptionWithNullMessage() {
        RedisConnectionException exception = new RedisConnectionException(null);
        
        assertThat(exception).isNotNull();
        assertThat(exception.getMessage()).isNull();
    }

    @Test
    void testExceptionWithNullCause() {
        String errorMessage = "Redis error";
        
        RedisConnectionException exception = new RedisConnectionException(errorMessage, null);
        
        assertThat(exception).isNotNull();
        assertThat(exception.getMessage()).isEqualTo(errorMessage);
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void testExceptionWithBothNull() {
        RedisConnectionException exception = new RedisConnectionException(null, null);
        
        assertThat(exception).isNotNull();
        assertThat(exception.getMessage()).isNull();
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void testExceptionStackTrace() {
        RedisConnectionException exception = new RedisConnectionException("Test error");
        
        assertThat(exception.getStackTrace()).isNotNull();
        assertThat(exception.getStackTrace()).isNotEmpty();
    }

    @Test
    void testExceptionWithChainedCause() {
        Throwable rootCause = new IllegalStateException("Root cause");
        Throwable intermediateCause = new RuntimeException("Intermediate", rootCause);
        RedisConnectionException exception = new RedisConnectionException("Top level", intermediateCause);
        
        assertThat(exception.getMessage()).isEqualTo("Top level");
        assertThat(exception.getCause()).isEqualTo(intermediateCause);
        assertThat(exception.getCause().getCause()).isEqualTo(rootCause);
    }

    @Test
    void testExceptionCanBeCaught() {
        try {
            throw new RedisConnectionException("Test exception");
        } catch (RedisConnectionException e) {
            assertThat(e.getMessage()).isEqualTo("Test exception");
        } catch (Exception e) {
            fail("Should have caught RedisConnectionException specifically");
        }
    }

    @Test
    void testExceptionWithLongMessage() {
        String longMessage = "This is a very long error message that describes in great detail ".repeat(10);
        
        RedisConnectionException exception = new RedisConnectionException(longMessage);
        
        assertThat(exception.getMessage()).isEqualTo(longMessage);
        assertThat(exception.getMessage().length()).isGreaterThan(100);
    }

    @Test
    void testExceptionWithSpecialCharacters() {
        String specialMessage = "Error: Connection failed! @host=redis://localhost:6379, timeout=5000ms, retry=3";
        
        RedisConnectionException exception = new RedisConnectionException(specialMessage);
        
        assertThat(exception.getMessage()).isEqualTo(specialMessage);
    }

    @Test
    void testExceptionEquals() {
        RedisConnectionException ex1 = new RedisConnectionException("Same message");
        RedisConnectionException ex2 = new RedisConnectionException("Same message");
        
        // Exceptions are not equal even with same message (standard Java behavior)
        assertThat(ex1).isNotEqualTo(ex2);
        assertThat(ex1.getMessage()).isEqualTo(ex2.getMessage());
    }

    @Test
    void testExceptionToString() {
        RedisConnectionException exception = new RedisConnectionException("Test error");
        
        String toString = exception.toString();
        assertThat(toString).contains("RedisConnectionException");
        assertThat(toString).contains("Test error");
    }
}
