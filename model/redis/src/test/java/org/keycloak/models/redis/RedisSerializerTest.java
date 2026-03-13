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
import org.keycloak.models.redis.RedisSerializer;
import org.keycloak.models.redis.RedisSerializer.RedisSerializationException;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for RedisSerializer.
 */
class RedisSerializerTest {

    private final RedisSerializer serializer = RedisSerializer.getInstance();

    @Test
    void testGetInstance_ReturnsSameInstance() {
        RedisSerializer instance1 = RedisSerializer.getInstance();
        RedisSerializer instance2 = RedisSerializer.getInstance();
        assertThat(instance1).isSameAs(instance2);
    }

    @Test
    void testSerializeToBytes_SimpleObject() {
        TestObject obj = new TestObject("test", 123);
        byte[] bytes = serializer.serialize(obj);
        
        assertThat(bytes).isNotNull();
        assertThat(bytes.length).isGreaterThan(0);
    }

    @Test
    void testSerializeToBytes_NullValue() {
        byte[] bytes = serializer.serialize(null);
        assertThat(bytes).isNull();
    }

    @Test
    void testSerializeToString_SimpleObject() {
        TestObject obj = new TestObject("test", 123);
        String json = serializer.serializeToString(obj);
        
        assertThat(json).isNotNull();
        assertThat(json).contains("test");
        assertThat(json).contains("123");
    }

    @Test
    void testSerializeToString_NullValue() {
        String json = serializer.serializeToString(null);
        assertThat(json).isNull();
    }

    @Test
    void testDeserializeBytes_SimpleObject() {
        TestObject original = new TestObject("test", 123);
        byte[] bytes = serializer.serialize(original);
        
        TestObject deserialized = serializer.deserialize(bytes, TestObject.class);
        
        assertThat(deserialized).isNotNull();
        assertThat(deserialized.name).isEqualTo(original.name);
        assertThat(deserialized.value).isEqualTo(original.value);
    }

    @Test
    void testDeserializeBytes_NullBytes() {
        TestObject result = serializer.deserialize((byte[]) null, TestObject.class);
        assertThat(result).isNull();
    }

    @Test
    void testDeserializeBytes_EmptyBytes() {
        TestObject result = serializer.deserialize(new byte[0], TestObject.class);
        assertThat(result).isNull();
    }

    @Test
    void testDeserializeString_SimpleObject() {
        TestObject original = new TestObject("test", 123);
        String json = serializer.serializeToString(original);
        
        TestObject deserialized = serializer.deserialize(json, TestObject.class);
        
        assertThat(deserialized).isNotNull();
        assertThat(deserialized.name).isEqualTo(original.name);
        assertThat(deserialized.value).isEqualTo(original.value);
    }

    @Test
    void testDeserializeString_NullString() {
        TestObject result = serializer.deserialize((String) null, TestObject.class);
        assertThat(result).isNull();
    }

    @Test
    void testDeserializeString_EmptyString() {
        TestObject result = serializer.deserialize("", TestObject.class);
        assertThat(result).isNull();
    }

    @Test
    void testDeserializeString_InvalidJson() {
        assertThatThrownBy(() -> serializer.deserialize("invalid json", TestObject.class))
                .isInstanceOf(RedisSerializationException.class)
                .hasMessageContaining("Failed to deserialize");
    }

    @Test
    void testDeserializeBytes_InvalidData() {
        byte[] invalidBytes = "invalid data".getBytes();
        assertThatThrownBy(() -> serializer.deserialize(invalidBytes, TestObject.class))
                .isInstanceOf(RedisSerializationException.class)
                .hasMessageContaining("Failed to deserialize");
    }

    @Test
    void testSerialize_ComplexObject() {
        Map<String, String> map = new HashMap<>();
        map.put("key1", "value1");
        map.put("key2", "value2");
        
        ComplexObject obj = new ComplexObject("complex", map);
        String json = serializer.serializeToString(obj);
        
        ComplexObject deserialized = serializer.deserialize(json, ComplexObject.class);
        
        assertThat(deserialized).isNotNull();
        assertThat(deserialized.name).isEqualTo(obj.name);
        assertThat(deserialized.data).containsAllEntriesOf(obj.data);
    }

    @Test
    void testGetObjectMapper() {
        assertThat(serializer.getObjectMapper()).isNotNull();
    }

    @Test
    void testRedisSerializationException_Constructor() {
        Throwable cause = new RuntimeException("test cause");
        RedisSerializationException exception = new RedisSerializationException("test message", cause);
        
        assertThat(exception.getMessage()).isEqualTo("test message");
        assertThat(exception.getCause()).isEqualTo(cause);
    }

    // Test classes
    public static class TestObject {
        private String name;
        private int value;

        public TestObject() {
        }

        public TestObject(String name, int value) {
            this.name = name;
            this.value = value;
        }
    }

    public static class ComplexObject {
        private String name;
        private Map<String, String> data;

        public ComplexObject() {
        }

        public ComplexObject(String name, Map<String, String> data) {
            this.name = name;
            this.data = data;
        }
    }
}
