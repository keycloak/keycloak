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

package org.keycloak.models.sessions.infinispan.changes;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.keycloak.marshalling.Marshalling;
import org.keycloak.models.sessions.infinispan.entities.AuthenticatedClientSessionEntity;
import org.keycloak.models.sessions.infinispan.entities.SessionEntity;
import org.keycloak.models.sessions.infinispan.entities.UserSessionEntity;

import org.infinispan.Cache;
import org.infinispan.commons.dataconversion.MediaType;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.TopologyChanged;
import org.infinispan.notifications.cachelistener.event.TopologyChangedEvent;
import org.infinispan.topology.CacheTopology;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SessionCacheTest {
    private static final String STACK = """
            <config xmlns="urn:org:jgroups">
                <SHARED_LOOPBACK/>
                <SHARED_LOOPBACK_PING/>
                <pbcast.NAKACK2 use_mcast_xmit="false"/>
                <UNICAST3/>
                <pbcast.STABLE/>
                <pbcast.GMS join_timeout="1000"/>
                <UFC/>
                <MFC/>
                <FRAG4/>
            </config>
            """;
    private final String clusterName = "session-copy-" + UUID.randomUUID();
    private final List<DefaultCacheManager> managers = new ArrayList<>();

    @After
    public void closeManagers() {
        for (int i = managers.size() - 1; i >= 0; i--) {
            managers.get(i).stop();
        }
    }

    @Test
    public void binaryValuesArePrivateOnOwnersAndRemoteReaders() throws Exception {
        var owner = addNode();
        var other = addNode();
        awaitTopology(owner, 2);
        awaitTopology(other, 2);
        String key = primaryKey(owner);
        for (SessionEntity entity : List.of(new UserSessionEntity(key), new AuthenticatedClientSessionEntity())) {
            entity.setRealmId("realm");
            var value = new SessionEntityWrapper<>(entity);
            owner.put(key, value);
            entity.setRealmId("after-put");
            for (var reader : List.of(owner, other)) {
                var result = reader.get(key);
                assertNotSame(value.getEntity(), result.getEntity());
                assertEquals("realm", result.getEntity().getRealmId());
                result.getEntity().setRealmId("request-only");
                result.getLocalMetadata().put("request", "private");
                assertEquals("realm", owner.get(key).getEntity().getRealmId());
                assertNull(owner.get(key).getLocalMetadataNote("request"));
            }
        }
    }

    @Test
    public void conditionalOperationsCompareVersionsOnRemoteOwner() throws Exception {
        var owner = addNode();
        var other = addNode();
        awaitTopology(owner, 2);
        awaitTopology(other, 2);
        String key = primaryKey(owner);
        var marker = SessionEntityWrapper.<SessionEntity>createLoadingMarker(new UserSessionEntity(key));
        owner.put(key, marker);
        // Deliberately change the expected object's bytes while retaining its CAS version.
        marker.getEntity().setRealmId("changed-after-put");
        var replacement = new SessionEntityWrapper<SessionEntity>(new UserSessionEntity(key));
        assertTrue(SessionCache.replace(other, key, marker, replacement, 30, TimeUnit.SECONDS));
        assertFalse(SessionCache.remove(other, key, marker));
        assertEquals(replacement.getVersion(), owner.get(key).getVersion());
        replacement.getEntity().setRealmId("changed-after-replace");
        assertNull(owner.get(key).getEntity().getRealmId());
        assertTrue(SessionCache.remove(other, key, replacement));
        assertNull(owner.get(key));
    }

    private Cache<String, SessionEntityWrapper<SessionEntity>> addNode() {
        var global = new GlobalConfigurationBuilder();
        global.transport().defaultTransport().clusterName(clusterName).addProperty("configurationString", STACK);
        Marshalling.configure(global);
        var manager = new DefaultCacheManager(global.build());
        managers.add(manager);
        var configuration = new ConfigurationBuilder();
        configuration.clustering().cacheMode(CacheMode.DIST_SYNC).hash().numOwners(1);
        configuration.encoding().value().mediaType(MediaType.APPLICATION_PROTOSTREAM_TYPE);
        manager.defineConfiguration("sessions", configuration.build());
        return manager.getCache("sessions");
    }

    private static String primaryKey(Cache<?, ?> cache) {
        var topology = cache.getAdvancedCache().getDistributionManager().getCacheTopology();
        for (int i = 0; i < 10_000; i++) {
            String key = "session-" + i;
            if (topology.getDistribution(key).isPrimary()) {
                return key;
            }
        }
        throw new AssertionError("No primary-owned key found");
    }

    private static void awaitTopology(Cache<?, ?> cache, int members) throws InterruptedException {
        var barrier = new TopologyBarrier(cache, members);
        cache.addListener(barrier);
        try {
            barrier.check();
            assertTrue("Cache did not reach a stable topology", barrier.ready.await(30, TimeUnit.SECONDS));
        } finally {
            cache.removeListener(barrier);
        }
    }

    @Listener
    public static class TopologyBarrier {
        final Cache<?, ?> cache;
        final int members;
        final CountDownLatch ready = new CountDownLatch(1);

        TopologyBarrier(Cache<?, ?> cache, int members) {
            this.cache = cache;
            this.members = members;
        }

        void check() {
            var topology = cache.getAdvancedCache().getDistributionManager().getCacheTopology();
            if (topology.getMembers().size() == members && topology.getPhase() == CacheTopology.Phase.NO_REBALANCE) {
                ready.countDown();
            }
        }

        @TopologyChanged
        public void topologyChanged(TopologyChangedEvent<?, ?> event) {
            if (!event.isPre()) {
                check();
            }
        }
    }

}
