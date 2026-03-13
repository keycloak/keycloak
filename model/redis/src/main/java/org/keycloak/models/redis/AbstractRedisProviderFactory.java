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

import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;

/**
 * Abstract base class for Redis-based provider factories.
 * Provides common functionality for Redis provider initialization and lifecycle.
 */
public abstract class AbstractRedisProviderFactory<T extends Provider>
        implements ProviderFactory<T>, EnvironmentDependentProviderFactory {

    protected static final String PROVIDER_ID = "redis";
    protected static final int REDIS_PROVIDER_ORDER = 10;

    /**
     * Gets the Redis connection provider from the session.
     * @param session the Keycloak session
     * @return the Redis connection provider
     * @throws IllegalStateException if Redis connection provider is not available
     */
    protected RedisConnectionProvider getRedisProvider(KeycloakSession session) {
        RedisConnectionProvider redis = session.getProvider(RedisConnectionProvider.class);
        if (redis == null) {
            throw new IllegalStateException("RedisConnectionProvider not available");
        }
        return redis;
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // Default: nothing to do
    }

    @Override
    public void close() {
        // Default: nothing to close
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    /**
     * Returns the order of this provider factory.
     * Higher order gives Redis priority over Infinispan.
     * @return provider order (10 by default)
     */
    public int order() {
        return REDIS_PROVIDER_ORDER;
    }

    @Override
    public boolean isSupported(Config.Scope config) {
        return Profile.isFeatureEnabled(Profile.Feature.REDIS_STORAGE);
    }
}
