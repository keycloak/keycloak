/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.sessions.infinispan.remote;

import java.lang.invoke.MethodHandles;
import java.util.List;

import org.infinispan.client.hotrod.RemoteCache;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.infinispan.util.InfinispanUtils;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.sessions.infinispan.InfinispanAuthenticationSessionProviderFactory;
import org.keycloak.models.sessions.infinispan.entities.RootAuthenticationSessionEntity;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.sessions.AuthenticationSessionProviderFactory;

import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.AUTHENTICATION_SESSIONS_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.getRemoteCache;
import static org.keycloak.models.sessions.infinispan.InfinispanAuthenticationSessionProviderFactory.DEFAULT_AUTH_SESSIONS_LIMIT;

public class RemoteInfinispanAuthenticationSessionProviderFactory implements AuthenticationSessionProviderFactory<RemoteInfinispanAuthenticationSessionProvider>, EnvironmentDependentProviderFactory {

    private final static Logger logger = Logger.getLogger(MethodHandles.lookup().lookupClass());

    private int authSessionsLimit;
    private volatile RemoteCache<String, RootAuthenticationSessionEntity> cache;

    @Override
    public boolean isSupported(Config.Scope config) {
        return InfinispanUtils.isRemoteInfinispan();
    }

    @Override
    public RemoteInfinispanAuthenticationSessionProvider create(KeycloakSession session) {
        return new RemoteInfinispanAuthenticationSessionProvider(session, this);
    }

    @Override
    public void init(Config.Scope config) {
        authSessionsLimit = InfinispanAuthenticationSessionProviderFactory.getAuthSessionsLimit(config);
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        cache = getRemoteCache(factory, AUTHENTICATION_SESSIONS_CACHE_NAME);
        logger.debugf("Provided initialized. session limit=%s", authSessionsLimit);
    }

    @Override
    public void close() {
        cache = null;
    }

    @Override
    public List<ProviderConfigProperty> getConfigMetadata() {
        return ProviderConfigurationBuilder.create()
                .property()
                .name("authSessionsLimit")
                .type("int")
                .helpText("The maximum number of concurrent authentication sessions per RootAuthenticationSession.")
                .defaultValue(DEFAULT_AUTH_SESSIONS_LIMIT)
                .add()
                .build();
    }

    @Override
    public String getId() {
        return InfinispanUtils.REMOTE_PROVIDER_ID;
    }

    @Override
    public int order() {
        return InfinispanUtils.PROVIDER_ORDER;
    }

    public int getAuthSessionsLimit() {
        return authSessionsLimit;
    }

    public RemoteCache<String, RootAuthenticationSessionEntity> getCache() {
        return cache;
    }
}
