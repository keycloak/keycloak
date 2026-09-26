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

package org.keycloak.cluster.infinispan;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.keycloak.common.Profile;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.notifications.cachemanagerlistener.event.ViewChangedEvent;
import org.infinispan.remoting.transport.Address;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class InfinispanClusterProviderFactoryTest {

    @Test
    public void leavingNodeClearsDatabaseBackedSessionCache() throws Exception {
        Profile.defaults();
        var cacheManager = new DefaultCacheManager(false);
        cacheManager.defineConfiguration(InfinispanConnectionProvider.WORK_CACHE_NAME, new ConfigurationBuilder().build());
        cacheManager.defineConfiguration(InfinispanConnectionProvider.OFFLINE_USER_SESSION_CACHE_NAME, new ConfigurationBuilder().build());
        cacheManager.start();

        Cache<String, Object> workCache = cacheManager.getCache(InfinispanConnectionProvider.WORK_CACHE_NAME);
        Cache<String, Object> sessionCache = cacheManager.getCache(InfinispanConnectionProvider.OFFLINE_USER_SESSION_CACHE_NAME);
        sessionCache.put("session", "value");

        var factory = new InfinispanClusterProviderFactory();
        setWorkCache(factory, workCache);

        Address survivor = Address.random("survivor");
        Address leaver = Address.random("leaver");
        factory.new ViewChangeListener().viewChanged(new TestViewChangedEvent(List.of(survivor, leaver), List.of(survivor)));

        try {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (!sessionCache.isEmpty() && System.nanoTime() < deadline) {
                Thread.sleep(10);
            }
            assertTrue("A member leaving must clear DB-backed session caches", sessionCache.isEmpty());
        } finally {
            factory.localExecutor.shutdownNow();
            cacheManager.stop();
            Profile.reset();
        }
    }

    private static void setWorkCache(InfinispanClusterProviderFactory factory, Cache<String, Object> cache) throws Exception {
        Field field = InfinispanClusterProviderFactory.class.getDeclaredField("workCache");
        field.setAccessible(true);
        field.set(factory, cache);
    }

    private record TestViewChangedEvent(List<Address> oldMembers, List<Address> newMembers) implements ViewChangedEvent {

        @Override
        public List<Address> getNewMembers() {
            return newMembers;
        }

        @Override
        public List<Address> getOldMembers() {
            return oldMembers;
        }

        @Override
        public Address getLocalAddress() {
            return newMembers.get(0);
        }

        @Override
        public int getViewId() {
            return 2;
        }

        @Override
        public boolean isMergeView() {
            return false;
        }

        @Override
        public Type getType() {
            return Type.VIEW_CHANGED;
        }

        @Override
        public EmbeddedCacheManager getCacheManager() {
            return null;
        }
    }
}
