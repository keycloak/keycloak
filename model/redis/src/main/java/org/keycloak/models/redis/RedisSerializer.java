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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;

import java.io.IOException;

/**
 * JSON-based serializer for Redis values using Jackson.
 */
public class RedisSerializer {

    private static final RedisSerializer INSTANCE = new RedisSerializer();

    private final ObjectMapper objectMapper;

    public RedisSerializer() {
        this.objectMapper = createObjectMapper();
    }

    public static RedisSerializer getInstance() {
        return INSTANCE;
    }

    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // Configure visibility - serialize fields only (not getters)
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

        // Configure serialization
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true);
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

        // Configure deserialization
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true);

        // Enable type information for polymorphic types with backward compatibility
        mapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL
        );


        return mapper;
    }

    /**
     * Serialize an object to bytes.
     */
    public byte[] serialize(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsBytes(value);
        } catch (JsonProcessingException e) {
            throw new RedisSerializationException("Failed to serialize object: " + value.getClass().getName(), e);
        }
    }

    /**
     * Serialize an object to a string.
     */
    public String serializeToString(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new RedisSerializationException("Failed to serialize object: " + value.getClass().getName(), e);
        }
    }

    /**
     * Deserialize bytes to an object.
     */
    public <T> T deserialize(byte[] bytes, Class<T> type) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        try {
            return objectMapper.readValue(bytes, type);
        } catch (IOException e) {
            throw new RedisSerializationException("Failed to deserialize to type: " + type.getName(), e);
        }
    }

    /**
     * Deserialize a string to an object.
     */
    public <T> T deserialize(String json, Class<T> type) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new RedisSerializationException("Failed to deserialize to type: " + type.getName(), e);
        }
    }

    /**
     * Get the underlying ObjectMapper for advanced usage.
     */
    ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    /**
     * Exception thrown when serialization or deserialization fails.
     */
    public static class RedisSerializationException extends RuntimeException {
        public RedisSerializationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

}
