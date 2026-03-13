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

import org.keycloak.models.redis.AbstractRedisProviderFactory;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.SingleUseObjectProviderFactory;
import org.keycloak.provider.EnvironmentDependentProviderFactory;

/**
 * Factory for Redis-based SingleUseObjectProvider.
 * This provider handles authorization codes, one-time tokens, and similar single-use objects.
 */
public class RedisSingleUseObjectProviderFactory extends AbstractRedisProviderFactory<RedisSingleUseObjectProvider> 
        implements SingleUseObjectProviderFactory<RedisSingleUseObjectProvider>, EnvironmentDependentProviderFactory {

    private static final Logger logger = Logger.getLogger(RedisSingleUseObjectProviderFactory.class);

    @Override
    public RedisSingleUseObjectProvider create(KeycloakSession session) {
        return new RedisSingleUseObjectProvider(getRedisProvider(session));
    }

    @Override
    public void init(Config.Scope config) {
        logger.debug("Initializing Redis SingleUseObjectProviderFactory");
    }

    @Override
    public boolean isSupported(Config.Scope config) {
        return true; // Will fail gracefully if Redis is not configured
    }
}
