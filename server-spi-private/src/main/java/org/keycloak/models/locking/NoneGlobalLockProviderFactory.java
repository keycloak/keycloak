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

package org.keycloak.models.locking;

import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.KeycloakSessionTaskWithResult;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.provider.EnvironmentDependentProviderFactory;

import java.time.Duration;

public class NoneGlobalLockProviderFactory implements GlobalLockProviderFactory, EnvironmentDependentProviderFactory {

    public static final String PROVIDER_ID = "none";

    @Override
    public GlobalLockProvider create(KeycloakSession session) {
        return new GlobalLockProvider() {
            @Override
            public void close() {
            }

            @Override
            public <V> V withLock(String lockName, Duration timeToWaitForLock, KeycloakSessionTaskWithResult<V> task) {
                return KeycloakModelUtils.runJobInTransactionWithResult(session.getKeycloakSessionFactory(), task);
            }

            @Override
            public void forceReleaseAllLocks() {

            }
        };
    }

    @Override
    public void init(Config.Scope config) {

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
}
