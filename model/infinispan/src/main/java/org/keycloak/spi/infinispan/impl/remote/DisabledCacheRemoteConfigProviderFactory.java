/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.spi.infinispan.impl.remote;

import java.util.Optional;

import org.keycloak.Config;
import org.keycloak.infinispan.util.InfinispanUtils;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.spi.infinispan.CacheRemoteConfigProvider;
import org.keycloak.spi.infinispan.CacheRemoteConfigProviderFactory;

import org.infinispan.client.hotrod.configuration.Configuration;

/**
 * Implementation used when an external Infinispan cluster is not configured.
 */
public class DisabledCacheRemoteConfigProviderFactory implements CacheRemoteConfigProviderFactory, CacheRemoteConfigProvider, EnvironmentDependentProviderFactory {

    private static final String PROVIDER_ID = "disabled";

    @Override
    public boolean isSupported(Config.Scope config) {
        return !InfinispanUtils.isRemoteInfinispan();
    }

    @Override
    public CacheRemoteConfigProvider create(KeycloakSession session) {
        return this;
    }

    @Override
    public void init(Config.Scope config) {
        //no-op
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        //no-op
    }

    @Override
    public Optional<Configuration> configuration() {
        // no configuration since it is disabled
        return Optional.empty();
    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
