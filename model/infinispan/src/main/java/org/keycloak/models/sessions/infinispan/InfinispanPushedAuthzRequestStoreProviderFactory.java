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

package org.keycloak.models.sessions.infinispan;

import org.infinispan.commons.api.BasicCache;
import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.PushedAuthzRequestStoreProvider;
import org.keycloak.models.PushedAuthzRequestStoreProviderFactory;
import org.keycloak.models.sessions.infinispan.entities.ActionTokenValueEntity;
import org.keycloak.provider.EnvironmentDependentProviderFactory;

import java.util.UUID;
import java.util.function.Supplier;

public class InfinispanPushedAuthzRequestStoreProviderFactory implements PushedAuthzRequestStoreProviderFactory, EnvironmentDependentProviderFactory {

    // Reuse "actionTokens" infinispan cache for now
    private volatile Supplier<BasicCache<UUID, ActionTokenValueEntity>> codeCache;

    @Override
    public PushedAuthzRequestStoreProvider create(KeycloakSession session) {
        lazyInit(session);
        return new InfinispanPushedAuthzRequestStoreProvider(session, codeCache);
    }

    private void lazyInit(KeycloakSession session) {
        if (codeCache == null) {
            synchronized (this) {
                if (codeCache == null) {
                    this.codeCache = InfinispanSingleUseTokenStoreProviderFactory.getActionTokenCache(session);
                }
            }
        }
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
        return "infinispan";
    }

    @Override
    public boolean isSupported() {
        return Profile.isFeatureEnabled(Profile.Feature.PAR);
    }
}
