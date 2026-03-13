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

package org.keycloak.models.redis.session;

import org.keycloak.models.redis.AbstractRedisProviderFactory;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.sessions.AuthenticationSessionProvider;
import org.keycloak.sessions.AuthenticationSessionProviderFactory;

/**
 * Factory for RedisAuthenticationSessionProvider.
 */
public class RedisAuthenticationSessionProviderFactory extends AbstractRedisProviderFactory<AuthenticationSessionProvider>
        implements AuthenticationSessionProviderFactory<AuthenticationSessionProvider> {

    private static final Logger logger = Logger.getLogger(RedisAuthenticationSessionProviderFactory.class);

    @Override
    public AuthenticationSessionProvider create(KeycloakSession session) {
        logger.debug("Creating a RedisAuthenticationSessionProvider instance...");
        return new RedisAuthenticationSessionProvider(session, getRedisProvider(session));
    }

    @Override
    public void init(Config.Scope config) {
        logger.info("Redis AuthenticationSessionProviderFactory initialized");
    }
}
