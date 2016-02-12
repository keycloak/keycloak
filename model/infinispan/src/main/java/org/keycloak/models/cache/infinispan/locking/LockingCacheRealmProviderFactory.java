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

package org.keycloak.models.cache.infinispan.locking;

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

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class LockingCacheRealmProviderFactory implements CacheRealmProviderFactory {

    private static final Logger log = Logger.getLogger(LockingCacheRealmProviderFactory.class);

    protected volatile LockingRealmCache realmCache;

    @Override
    public CacheRealmProvider create(KeycloakSession session) {
        lazyInit(session);
        return new LockingCacheRealmProvider(realmCache, session);
    }

    private void lazyInit(KeycloakSession session) {
        if (realmCache == null) {
            synchronized (this) {
                if (realmCache == null) {
                    Cache<String, Object> cache = session.getProvider(InfinispanConnectionProvider.class).getCache(InfinispanConnectionProvider.REALM_CACHE_NAME);
                    Cache<String, Long> counterCache = session.getProvider(InfinispanConnectionProvider.class).getCache(LockingConnectionProviderFactory.VERSION_CACHE_NAME);
                    cache.addListener(new CacheListener());
                    realmCache = new LockingRealmCache(cache, counterCache);
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
        return "infinispan-locking";
    }

    @Listener
    public class CacheListener {

        @CacheEntryCreated
        public void created(CacheEntryCreatedEvent<String, Object> event) {
            if (!event.isPre()) {
                Object object = event.getValue();
                if (object != null) {
                    if (object instanceof CachedRealm) {
                        CachedRealm realm = (CachedRealm) object;
                        realmCache.getRealmLookup().put(realm.getName(), realm.getId());
                        log.tracev("Realm added realm={0}", realm.getName());
                    }
                }
            }
        }

        @CacheEntryRemoved
        public void removed(CacheEntryRemovedEvent<String, Object> event) {
            if (event.isPre()) {
                Object object = event.getValue();
                if (object != null) {
                    remove(object);
                }
            }
        }

        @CacheEntryInvalidated
        public void removed(CacheEntryInvalidatedEvent<String, Object> event) {
            if (event.isPre()) {
                Object object = event.getValue();
                if (object != null) {
                    remove(object);
                }
            }
        }

        @CacheEntriesEvicted
        public void userEvicted(CacheEntriesEvictedEvent<String, Object> event) {
            for (Object object : event.getEntries().values()) {
                remove(object);
            }
        }

        private void remove(Object object) {
            if (object instanceof CachedRealm) {
                CachedRealm realm = (CachedRealm) object;

                realmCache.getRealmLookup().remove(realm.getName());

                for (String r : realm.getRealmRoles().values()) {
                    realmCache.evictRoleById(r);
                }

                for (String c : realm.getClients().values()) {
                    realmCache.evictClientById(c);
                }

                log.tracev("Realm removed realm={0}", realm.getName());
            } else if (object instanceof CachedClient) {
                CachedClient client = (CachedClient) object;

                for (String r : client.getRoles().values()) {
                    realmCache.evictRoleById(r);
                }

                log.tracev("Client removed client={0}", client.getId());
            }
        }
    }
}
