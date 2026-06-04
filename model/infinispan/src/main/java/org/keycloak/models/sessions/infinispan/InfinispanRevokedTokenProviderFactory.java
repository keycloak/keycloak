/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

import java.util.Set;

import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.infinispan.util.InfinispanUtils;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RevokedTokenProviderFactory;
import org.keycloak.models.SingleUseObjectProvider;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.Provider;

import static org.keycloak.storage.datastore.DefaultDatastoreProviderFactory.setupClearExpiredRevokedTokensScheduledTask;

public class InfinispanRevokedTokenProviderFactory implements RevokedTokenProviderFactory<InfinispanRevokedTokenProvider>, EnvironmentDependentProviderFactory {

    @Override
    public InfinispanRevokedTokenProvider create(KeycloakSession session) {
        var singleUseObject = session.getProvider(SingleUseObjectProvider.class);
        return new InfinispanRevokedTokenProvider(singleUseObject);
    }

    @Override
    public void init(Config.Scope config) {
        //no-op
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // old expiration
        setupClearExpiredRevokedTokensScheduledTask(factory);
    }

    @Override
    public void close() {
        //no-op
    }

    @Override
    public String getId() {
        return InfinispanUtils.EMBEDDED_PROVIDER_ID;
    }

    @Override
    public int order() {
        return InfinispanUtils.PROVIDER_ORDER;
    }

    @Override
    public Set<Class<? extends Provider>> dependsOn() {
        return Set.of(SingleUseObjectProvider.class);
    }

    @Override
    public boolean isSupported(Config.Scope config) {
        return !Profile.isFeatureEnabled(Profile.Feature.CACHELESS);
    }
}
