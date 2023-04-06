/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.map.lock;

import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.locking.GlobalLockProvider;
import org.keycloak.models.locking.GlobalLockProviderFactory;
import org.keycloak.models.map.common.AbstractMapProviderFactory;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

import java.util.List;

/**
 * Factory to create a GlobalLockProvider backed by a Map store.
 *
 * @author Alexander Schwartz
 */
public class MapGlobalLockProviderFactory extends AbstractMapProviderFactory<GlobalLockProvider, MapLockEntity, MapLockEntity> implements GlobalLockProviderFactory, EnvironmentDependentProviderFactory {

    public static final String DEFAULT_TIMEOUT_MILLISECONDS = "defaultTimeoutMilliseconds";
    public static final long DEFAULT_VALUE = 5000L;
    private long defaultTimeoutMilliseconds;

    public MapGlobalLockProviderFactory() {
        super(MapLockEntity.class, GlobalLockProvider.class);
    }

    @Override
    public MapGlobalLockProvider createNew(KeycloakSession session) {
        return new MapGlobalLockProvider(session, defaultTimeoutMilliseconds, () -> getMapStorage(session));
    }

    @Override
    public void init(Config.Scope config) {
        super.init(config);
        defaultTimeoutMilliseconds = config.getLong(DEFAULT_TIMEOUT_MILLISECONDS, DEFAULT_VALUE);
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public boolean isSupported() {
        return Profile.isFeatureEnabled(Profile.Feature.MAP_STORAGE);
    }

    @Override
    public String getHelpText() {
        return "Lock provider";
    }

    @Override
    public List<ProviderConfigProperty> getConfigMetadata() {
        return ProviderConfigurationBuilder.create()
                .property()
                .name(DEFAULT_TIMEOUT_MILLISECONDS)
                .type("int")
                .helpText("Default timeout when waiting for a lock")
                .defaultValue(DEFAULT_VALUE)
                .add()

                .build();
    }

}
