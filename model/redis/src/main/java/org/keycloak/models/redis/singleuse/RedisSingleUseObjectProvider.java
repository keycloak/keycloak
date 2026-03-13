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

import org.keycloak.models.redis.RedisConnectionProvider;

import org.jboss.logging.Logger;
import org.keycloak.models.SingleUseObjectProvider;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Redis-based implementation of SingleUseObjectProvider.
 * Handles authorization codes, one-time tokens, and other single-use objects.
 * 
 * This is critical for OIDC authorization code flow - codes must be:
 * 1. Used only once (atomic remove operation)
 * 2. Expire after a configured time
 * 3. Be unique across the cluster
 */
public class RedisSingleUseObjectProvider implements SingleUseObjectProvider {

    private static final Logger logger = Logger.getLogger(RedisSingleUseObjectProvider.class);

    private final RedisConnectionProvider redis;

    public RedisSingleUseObjectProvider(RedisConnectionProvider redis) {
        this.redis = Objects.requireNonNull(redis);
    }

    @Override
    public void put(String key, long lifespanSeconds, Map<String, String> notes) {
        Objects.requireNonNull(key, "key cannot be null");
        
        if (key.endsWith(REVOKED_KEY)) {
            // Token revocation - store with empty notes
            SingleUseObjectEntity entity = new SingleUseObjectEntity(Collections.emptyMap());
            redis.put(RedisConnectionProvider.SINGLE_USE_OBJECT_CACHE_NAME, key, entity, lifespanSeconds, TimeUnit.SECONDS);
            logger.debugf("Token revoked: %s", key);
            return;
        }
        
        SingleUseObjectEntity entity = new SingleUseObjectEntity(notes);
        redis.put(RedisConnectionProvider.SINGLE_USE_OBJECT_CACHE_NAME, key, entity, lifespanSeconds, TimeUnit.SECONDS);
        
        if (logger.isDebugEnabled()) {
            logger.debugf("Stored single-use object: %s with lifespan %d seconds", key, lifespanSeconds);
        }
    }

    @Override
    public Map<String, String> get(String key) {
        Objects.requireNonNull(key, "key cannot be null");
        
        SingleUseObjectEntity entity = redis.get(
                RedisConnectionProvider.SINGLE_USE_OBJECT_CACHE_NAME,
                key,
                SingleUseObjectEntity.class
        );
        
        return entity != null ? entity.getNotes() : null;
    }

    @Override
    public Map<String, String> remove(String key) {
        Objects.requireNonNull(key, "key cannot be null");
        
        // Atomic get-and-delete to ensure single-use semantics
        // This is critical for authorization codes - they must only be used once
        SingleUseObjectEntity entity = redis.remove(
                RedisConnectionProvider.SINGLE_USE_OBJECT_CACHE_NAME,
                key,
                SingleUseObjectEntity.class
        );
        
        if (entity != null) {
            if (logger.isDebugEnabled()) {
                logger.debugf("Removed single-use object: %s", key);
            }
            return entity.getNotes();
        }
        
        return null;
    }

    @Override
    public boolean replace(String key, Map<String, String> notes) {
        Objects.requireNonNull(key, "key cannot be null");
        
        // Check if key exists first
        RedisConnectionProvider.VersionedValue<SingleUseObjectEntity> existing = redis.getWithVersion(
                RedisConnectionProvider.SINGLE_USE_OBJECT_CACHE_NAME,
                key,
                SingleUseObjectEntity.class
        );
        
        if (existing == null || !existing.hasValue()) {
            return false;
        }
        
        // Replace with version check for optimistic locking
        SingleUseObjectEntity newEntity = new SingleUseObjectEntity(notes);
        boolean replaced = redis.replaceWithVersion(
                RedisConnectionProvider.SINGLE_USE_OBJECT_CACHE_NAME,
                key,
                newEntity,
                existing.version(),
                // Keep same TTL - we can't easily get remaining TTL in Redis without extra call
                // Using a reasonable default; in production, consider using PTTL command
                300,
                TimeUnit.SECONDS
        );
        
        if (replaced && logger.isDebugEnabled()) {
            logger.debugf("Replaced single-use object: %s", key);
        }
        
        return replaced;
    }

    @Override
    public boolean putIfAbsent(String key, long lifespanInSeconds) {
        Objects.requireNonNull(key, "key cannot be null");
        
        // Atomic conditional put - critical for ensuring uniqueness
        SingleUseObjectEntity entity = new SingleUseObjectEntity(null);
        SingleUseObjectEntity previous = redis.putIfAbsent(
                RedisConnectionProvider.SINGLE_USE_OBJECT_CACHE_NAME,
                key,
                entity,
                lifespanInSeconds,
                TimeUnit.SECONDS
        );
        
        boolean wasAbsent = previous == null;
        
        if (wasAbsent && logger.isDebugEnabled()) {
            logger.debugf("Created single-use object (if absent): %s", key);
        }
        
        return wasAbsent;
    }

    @Override
    public boolean contains(String key) {
        Objects.requireNonNull(key, "key cannot be null");
        return redis.containsKey(RedisConnectionProvider.SINGLE_USE_OBJECT_CACHE_NAME, key);
    }

    @Override
    public void close() {
        // Connection is managed by the factory
    }

    /**
     * Entity class for storing single-use object data.
     */
    public static class SingleUseObjectEntity {
        private Map<String, String> notes;

        // Required for deserialization
        public SingleUseObjectEntity() {
        }

        public SingleUseObjectEntity(Map<String, String> notes) {
            this.notes = notes;
        }

        public Map<String, String> getNotes() {
            return notes;
        }

        public void setNotes(Map<String, String> notes) {
            this.notes = notes;
        }
    }
}
