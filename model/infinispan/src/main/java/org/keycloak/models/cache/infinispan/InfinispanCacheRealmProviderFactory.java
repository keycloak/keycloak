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

package org.keycloak.models.cache.infinispan;

import org.infinispan.Cache;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntriesEvicted;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryCreated;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryInvalidated;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryRemoved;
import org.infinispan.notifications.cachelistener.event.CacheEntriesEvictedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryCreatedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryInvalidatedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryRemovedEvent;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.cache.CacheRealmProvider;
import org.keycloak.models.cache.CacheRealmProviderFactory;
import org.keycloak.models.cache.entities.CachedClient;
import org.keycloak.models.cache.entities.CachedRealm;
import org.keycloak.models.cache.infinispan.stream.StreamCacheRealmProvider;
import org.keycloak.models.cache.infinispan.stream.StreamRealmCache;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class InfinispanCacheRealmProviderFactory implements CacheRealmProviderFactory {

    private static final Logger log = Logger.getLogger(InfinispanCacheRealmProviderFactory.class);

    protected volatile StreamRealmCache realmCache;

    @Override
    public CacheRealmProvider create(KeycloakSession session) {
        lazyInit(session);
        return new StreamCacheRealmProvider(realmCache, session);
    }

    private void lazyInit(KeycloakSession session) {
        if (realmCache == null) {
            synchronized (this) {
                if (realmCache == null) {
                    Cache<String, Object> cache = session.getProvider(InfinispanConnectionProvider.class).getCache(InfinispanConnectionProvider.REALM_CACHE_NAME);
                    Cache<String, Long> revisions = session.getProvider(InfinispanConnectionProvider.class).getCache(InfinispanConnectionProvider.VERSION_CACHE_NAME);
                    realmCache = new StreamRealmCache(cache, revisions);
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
