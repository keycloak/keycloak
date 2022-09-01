/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.dblock;

import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.EnvironmentDependentProviderFactory;

public class NoLockingDBLockProviderFactory implements DBLockProviderFactory, EnvironmentDependentProviderFactory { 

    public static final String PROVIDER_ID = "none";

    @Override
    public void setTimeouts(long lockRecheckTimeMillis, long lockWaitTimeoutMillis) {
    }

    @Override
    public DBLockProvider create(KeycloakSession session) {
        return INSTANCE;
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

    private static final DBLockProvider INSTANCE = new DBLockProvider() {
        @Override
        public void waitForLock(DBLockProvider.Namespace lock) {
        }

        @Override
        public void releaseLock() {
        }

        @Override
        public DBLockProvider.Namespace getCurrentLock() {
            return null;
        }

        @Override
        public boolean supportsForcedUnlock() {
            return false;
        }

        @Override
        public void destroyLockInfo() {
        }

        @Override
        public void close() {
        }
    };

}
