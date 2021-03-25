/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.SamlArtifactSessionMappingStoreProvider;
import org.keycloak.models.SamlArtifactSessionMappingStoreProviderFactory;

import java.util.UUID;
import java.util.function.Supplier;
import static org.keycloak.models.sessions.infinispan.InfinispanAuthenticationSessionProviderFactory.PROVIDER_PRIORITY;

/**
 * @author mhajas
 */
public class InfinispanSamlArtifactSessionMappingStoreProviderFactory implements SamlArtifactSessionMappingStoreProviderFactory {

    private static final Logger LOG = Logger.getLogger(InfinispanSamlArtifactSessionMappingStoreProviderFactory.class);

    // Reuse "actionTokens" infinispan cache for now
    private volatile Supplier<BasicCache<UUID, String[]>> codeCache;

    @Override
    public SamlArtifactSessionMappingStoreProvider create(KeycloakSession session) {
        lazyInit(session);
        return new InfinispanSamlArtifactSessionMappingStoreProvider(codeCache);
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
    public int order() {
        return PROVIDER_PRIORITY;
    }
}
