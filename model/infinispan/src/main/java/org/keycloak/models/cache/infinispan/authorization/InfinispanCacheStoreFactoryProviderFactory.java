/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.cache.infinispan.authorization;

import org.keycloak.Config;
import org.keycloak.cluster.ClusterProvider;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.cache.authorization.CachedStoreFactoryProvider;
import org.keycloak.models.cache.authorization.CachedStoreProviderFactory;
import org.keycloak.models.cache.infinispan.entities.Revisioned;

import org.infinispan.Cache;
import org.jboss.logging.Logger;

import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.AUTHORIZATION_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.AUTHORIZATION_REVISIONS_CACHE_NAME;
import static org.keycloak.models.cache.infinispan.InfinispanCacheRealmProviderFactory.REALM_CLEAR_CACHE_EVENTS;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class InfinispanCacheStoreFactoryProviderFactory implements CachedStoreProviderFactory {

    private static final Logger log = Logger.getLogger(InfinispanCacheStoreFactoryProviderFactory.class);
    public static final String AUTHORIZATION_CLEAR_CACHE_EVENTS = "AUTHORIZATION_CLEAR_CACHE_EVENTS";
    public static final String AUTHORIZATION_INVALIDATION_EVENTS = "AUTHORIZATION_INVALIDATION_EVENTS";

    protected volatile StoreFactoryCacheManager storeCache;

    @Override
    public CachedStoreFactoryProvider create(KeycloakSession session) {
        lazyInit(session);
        return new StoreFactoryCacheSession(storeCache, session);
    }

    private void lazyInit(KeycloakSession session) {
        if (storeCache == null) {
            synchronized (this) {
                if (storeCache == null) {
                    var ispnProvider = session.getProvider(InfinispanConnectionProvider.class);
                    var cluster = session.getProvider(ClusterProvider.class);

                    Cache<String, Revisioned> cache = ispnProvider.getCache(AUTHORIZATION_CACHE_NAME);
                    Cache<String, Long> revisions = ispnProvider.getCache(AUTHORIZATION_REVISIONS_CACHE_NAME);
                    storeCache = new StoreFactoryCacheManager(cache, revisions);

                    cluster.registerListener(AUTHORIZATION_INVALIDATION_EVENTS, storeCache::onInvalidateEvent);
                    cluster.registerListener(AUTHORIZATION_CLEAR_CACHE_EVENTS, storeCache::onClearEvent);
                    cluster.registerListener(REALM_CLEAR_CACHE_EVENTS, storeCache::onClearEvent);
                    log.debug("Registered cluster listeners");
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
        return "default";
    }

}
