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

import java.util.List;

/**
 * Wrapper interface for Redis commands that works with both standalone and cluster modes.
 * Provides a unified API for test steps regardless of Redis deployment mode.
 */
public interface RedisTestCommands {
    
    /**
     * Set a key with a value and expiration time in seconds
     */
    String setex(String key, long seconds, String value);
    
    /**
     * Get the value of a key
     */
    String get(String key);
    
    /**
     * Delete one or more keys
     */
    Long del(String... keys);
    
    /**
     * Set a timeout on a key
     */
    Boolean expire(String key, long seconds);
    
    /**
     * Check if a key exists
     */
    Long exists(String... keys);
    
    /**
     * Get the time to live for a key in seconds
     */
    Long ttl(String key);
    
    /**
     * Find all keys matching a pattern
     */
    List<String> keys(String pattern);
    
    /**
     * Set the value of a key
     */
    String set(String key, String value);
    
    /**
     * Set the value of a key, only if the key does not exist
     */
    Boolean setnx(String key, String value);
    
    /**
     * Get all fields and values in a hash
     */
    java.util.Map<String, String> hgetall(String key);
    
    /**
     * Determine the type stored at key
     */
    String type(String key);
}
