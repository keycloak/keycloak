/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.spi.infinispan.impl.embedded;

import org.keycloak.Config;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;

import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.parsing.ConfigurationBuilderHolder;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class VolatileSessionCacheConfigurationTest {
    @Test
    public void cacheAuthoritativeSessionsAreUnboundedReplicatedAndTransferred() {
        var holder = configuration();
        holder.getNamedConfigurationBuilders().get(InfinispanConnectionProvider.OFFLINE_CLIENT_SESSION_CACHE_NAME)
                .memory().maxCount(-1).maxSize("1MB");
        CacheConfigurator.configureSessionsCachesForVolatileSessions(new Config.SystemPropertiesScope("unused."), holder, false);
        for (var name : InfinispanConnectionProvider.USER_AND_CLIENT_SESSION_CACHES) {
            var configuration = holder.getNamedConfigurationBuilders().get(name).build();
            assertEquals(-1, configuration.memory().maxCount());
            assertNull(configuration.memory().maxSize());
            assertEquals(2, configuration.clustering().hash().numOwners());
            assertTrue(configuration.clustering().stateTransfer().fetchInMemoryState());
            assertEquals(-1, configuration.expiration().lifespan());
            assertEquals(-1, configuration.expiration().maxIdle());
        }
    }

    @Test
    public void databaseBackedOfflineSessionsRemainBoundedWithOneOwner() {
        var holder = configuration();
        CacheConfigurator.configureSessionsCachesForVolatileSessions(new Config.SystemPropertiesScope("unused."), holder, true);
        for (var name : new String[] { InfinispanConnectionProvider.OFFLINE_USER_SESSION_CACHE_NAME, InfinispanConnectionProvider.OFFLINE_CLIENT_SESSION_CACHE_NAME }) {
            var configuration = holder.getNamedConfigurationBuilders().get(name).build();
            assertEquals(1, configuration.memory().maxCount());
            assertEquals(1, configuration.clustering().hash().numOwners());
            assertFalse(configuration.clustering().stateTransfer().fetchInMemoryState());
        }
    }

    private static ConfigurationBuilderHolder configuration() {
        var holder = new ConfigurationBuilderHolder();
        holder.getGlobalConfigurationBuilder().clusteredDefault();
        for (var name : InfinispanConnectionProvider.USER_AND_CLIENT_SESSION_CACHES) {
            var builder = holder.newConfigurationBuilder(name);
            builder.clustering().cacheMode(CacheMode.DIST_SYNC).hash().numOwners(1);
            builder.clustering().stateTransfer().fetchInMemoryState(false);
            builder.memory().maxCount(1);
            builder.expiration().lifespan(1000).maxIdle(500);
        }
        return holder;
    }
}
