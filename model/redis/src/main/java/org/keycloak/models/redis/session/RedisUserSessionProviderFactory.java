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

import org.keycloak.models.redis.RedisConnectionProvider;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.UserSessionProvider;
import org.keycloak.models.UserSessionProviderFactory;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.provider.ServerInfoAwareProviderFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Factory for Redis-based UserSessionProvider.
 * This provider handles user sessions and client sessions using Redis as the backend store.
 */
public class RedisUserSessionProviderFactory implements UserSessionProviderFactory<RedisUserSessionProvider>,
        EnvironmentDependentProviderFactory, ServerInfoAwareProviderFactory {

    private static final Logger logger = Logger.getLogger(RedisUserSessionProviderFactory.class);

    public static final String PROVIDER_ID = "redis";

    private static final String CONFIG_SESSION_LIFESPAN = "sessionLifespan";
    private static final String CONFIG_OFFLINE_SESSION_LIFESPAN = "offlineSessionLifespan";

    private static final int DEFAULT_SESSION_LIFESPAN = 36000; // 10 hours
    private static final int DEFAULT_OFFLINE_SESSION_LIFESPAN = 5184000; // 60 days

    private Config.Scope config;
    private int sessionLifespan;
    private int offlineSessionLifespan;

    @Override
    public RedisUserSessionProvider create(KeycloakSession session) {
        RedisConnectionProvider redis = session.getProvider(RedisConnectionProvider.class);
        if (redis == null) {
            throw new IllegalStateException("RedisConnectionProvider not available. " +
                    "Make sure Redis connection is properly configured.");
        }
        return new RedisUserSessionProvider(session, redis, sessionLifespan, offlineSessionLifespan);
    }

    @Override
    public void init(Config.Scope config) {
        this.config = config;
        this.sessionLifespan = config.getInt(CONFIG_SESSION_LIFESPAN, DEFAULT_SESSION_LIFESPAN);
        this.offlineSessionLifespan = config.getInt(CONFIG_OFFLINE_SESSION_LIFESPAN, DEFAULT_OFFLINE_SESSION_LIFESPAN);
        logger.debugf("Redis UserSessionProviderFactory initialized: sessionLifespan=%d, offlineSessionLifespan=%d",
                sessionLifespan, offlineSessionLifespan);
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        logger.info("Redis UserSessionProvider initialized");
    }

    @Override
    public void close() {
        logger.debug("Closing Redis UserSessionProviderFactory");
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public boolean isSupported(Config.Scope config) {
        // This provider is enabled when explicitly configured
        return true;
    }

    @Override
    public int order() {
        // Higher order to take precedence over Infinispan when configured
        return 10;
    }

    @Override
    public List<ProviderConfigProperty> getConfigMetadata() {
        return ProviderConfigurationBuilder.create()
                .property()
                    .name(CONFIG_SESSION_LIFESPAN)
                    .type("int")
                    .label("Session Lifespan")
                    .helpText("Default lifespan for user sessions in seconds")
                    .defaultValue(String.valueOf(DEFAULT_SESSION_LIFESPAN))
                    .add()
                .property()
                    .name(CONFIG_OFFLINE_SESSION_LIFESPAN)
                    .type("int")
                    .label("Offline Session Lifespan")
                    .helpText("Default lifespan for offline sessions in seconds")
                    .defaultValue(String.valueOf(DEFAULT_OFFLINE_SESSION_LIFESPAN))
                    .add()
                .build();
    }

    @Override
    public Map<String, String> getOperationalInfo() {
        Map<String, String> info = new LinkedHashMap<>();
        info.put("provider", PROVIDER_ID);
        info.put("sessionLifespan", String.valueOf(sessionLifespan));
        info.put("offlineSessionLifespan", String.valueOf(offlineSessionLifespan));
        return info;
    }
}
