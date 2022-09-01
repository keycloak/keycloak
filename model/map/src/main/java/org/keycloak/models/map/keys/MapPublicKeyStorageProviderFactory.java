/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.map.keys;

import org.keycloak.common.Profile;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.keys.PublicKeyStorageProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.common.AbstractMapProviderFactory;
import org.keycloak.provider.EnvironmentDependentProviderFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.FutureTask;

public class MapPublicKeyStorageProviderFactory extends AbstractMapProviderFactory<MapPublicKeyStorageProvider, AbstractEntity, Object>
        implements PublicKeyStorageProviderFactory<MapPublicKeyStorageProvider>, EnvironmentDependentProviderFactory {

    private final Map<String, FutureTask<Map<String, KeyWrapper>>> tasksInProgress = new ConcurrentHashMap<>();

    public MapPublicKeyStorageProviderFactory() {
        super(Object.class, MapPublicKeyStorageProvider.class);
    }

    @Override
    public MapPublicKeyStorageProvider createNew(KeycloakSession session) {
        return new MapPublicKeyStorageProvider(session, tasksInProgress);
    }

    @Override
    public boolean isSupported() {
        return Profile.isFeatureEnabled(Profile.Feature.MAP_STORAGE);
    }

    @Override
    public String getHelpText() {
        return "Public key storage provider";
    }
}
