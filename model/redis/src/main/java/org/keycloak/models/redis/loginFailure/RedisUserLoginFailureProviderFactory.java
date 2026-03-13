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

package org.keycloak.models.redis.loginFailure;

import org.keycloak.models.redis.AbstractRedisProviderFactory;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserLoginFailureProvider;
import org.keycloak.models.UserLoginFailureProviderFactory;

/**
 * Factory for RedisUserLoginFailureProvider.
 */
public class RedisUserLoginFailureProviderFactory extends AbstractRedisProviderFactory<UserLoginFailureProvider> 
        implements UserLoginFailureProviderFactory<UserLoginFailureProvider> {

    private static final Logger logger = Logger.getLogger(RedisUserLoginFailureProviderFactory.class);
    private static final long DEFAULT_FAILURE_LIFESPAN = 900; // 15 minutes

    private long failureLifespan = DEFAULT_FAILURE_LIFESPAN;

    @Override
    public UserLoginFailureProvider create(KeycloakSession session) {
        return new RedisUserLoginFailureProvider(session, getRedisProvider(session), failureLifespan);
    }

    @Override
    public void init(Config.Scope config) {
        failureLifespan = config.getLong("failureLifespan", DEFAULT_FAILURE_LIFESPAN);
        logger.infof("Redis UserLoginFailureProviderFactory initialized with lifespan: %d seconds", failureLifespan);
    }
}
