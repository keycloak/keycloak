/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.models.authorization.infinispan;

import java.util.List;
import java.util.Map;

import org.infinispan.Cache;
import org.keycloak.Config;
import org.keycloak.cluster.ClusterProvider;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.authorization.infinispan.events.AuthorizationInvalidationEvent;
import org.keycloak.models.cache.authorization.CachedStoreFactoryProvider;
import org.keycloak.models.cache.authorization.CachedStoreProviderFactory;
import org.keycloak.provider.EnvironmentDependentProviderFactory;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class InfinispanStoreProviderFactory implements CachedStoreProviderFactory, EnvironmentDependentProviderFactory {

    private StoreFactoryCacheManager cacheManager;

    @Override
    public CachedStoreFactoryProvider create(KeycloakSession session) {
        return new InfinispanStoreFactoryProvider(session, cacheManager);
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        KeycloakSession session = factory.create();

        try {
            InfinispanConnectionProvider provider = session.getProvider(InfinispanConnectionProvider.class);
            Cache<String, Map<String, List<Object>>> cache = provider.getCache(InfinispanConnectionProvider.AUTHORIZATION_CACHE_NAME);
            ClusterProvider clusterProvider = session.getProvider(ClusterProvider.class);

            cacheManager = new StoreFactoryCacheManager(cache);

            clusterProvider.registerListener(ClusterProvider.ALL, event -> {
                if (event instanceof AuthorizationInvalidationEvent) {
                    cacheManager.invalidate(AuthorizationInvalidationEvent.class.cast(event));
                }
            });
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return "infinispan-authz-store-factory";
    }

    @Override
    public boolean isSupported() {
        return true;
    }
}
