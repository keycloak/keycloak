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
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.cluster.ClusterEvent;
import org.keycloak.cluster.ClusterProvider;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.cache.UserCache;
import org.keycloak.models.cache.UserCacheProviderFactory;
import org.keycloak.models.cache.infinispan.entities.Revisioned;
import org.keycloak.models.cache.infinispan.events.InvalidationEvent;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class InfinispanUserCacheProviderFactory implements UserCacheProviderFactory {

    private static final Logger log = Logger.getLogger(InfinispanUserCacheProviderFactory.class);
    public static final String USER_CLEAR_CACHE_EVENTS = "USER_CLEAR_CACHE_EVENTS";
    public static final String USER_INVALIDATION_EVENTS = "USER_INVALIDATION_EVENTS";

    protected volatile UserCacheManager userCache;



    @Override
    public UserCache create(KeycloakSession session) {
        lazyInit(session);
        return new UserCacheSession(userCache, session);
    }

    private void lazyInit(KeycloakSession session) {
        if (userCache == null) {
            synchronized (this) {
                if (userCache == null) {
                    Cache<String, Revisioned> cache = session.getProvider(InfinispanConnectionProvider.class).getCache(InfinispanConnectionProvider.USER_CACHE_NAME);
                    Cache<String, Long> revisions = session.getProvider(InfinispanConnectionProvider.class).getCache(InfinispanConnectionProvider.USER_REVISIONS_CACHE_NAME);
                    userCache = new UserCacheManager(cache, revisions);

                    ClusterProvider cluster = session.getProvider(ClusterProvider.class);

                    cluster.registerListener(USER_INVALIDATION_EVENTS, (ClusterEvent event) -> {

                        InvalidationEvent invalidationEvent = (InvalidationEvent) event;
                        userCache.invalidationEventReceived(invalidationEvent);

                    });

                    cluster.registerListener(USER_CLEAR_CACHE_EVENTS, (ClusterEvent event) -> {

                        userCache.clear();

                    });

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
