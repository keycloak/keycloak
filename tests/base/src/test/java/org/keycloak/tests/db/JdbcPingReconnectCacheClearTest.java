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

package org.keycloak.tests.db;

import java.time.Duration;

import org.keycloak.common.Profile;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.jgroups.protocol.KEYCLOAK_JDBC_PING2;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;

import org.infinispan.Cache;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that the {@code onHealthRestored} callback registered by
 * {@code DatabaseAwareClusterProviderFactory} broadcasts a {@code CLEAR_ALL_LOCAL_CACHES_EVENT}
 * through the cluster provider, causing all local caches (realm, user, authorization, keys, CRL)
 * to be cleared on every node in the cluster.
 * <p>
 * Requires the stateless feature — cache clearing on health recovery is only registered
 * when stateless mode is enabled, because in single-cluster mode cache invalidation
 * does not go through the database and a DB outage cannot cause missed events.
 */
@KeycloakIntegrationTest
public class JdbcPingReconnectCacheClearTest {

    @InjectRealm
    ManagedRealm managedRealm;

    @InjectRealm(ref = "master", attachTo = "master")
    ManagedRealm masterRealm;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @InjectRunOnServer(ref = "master", realmRef = "master")
    RunOnServerClient masterRunOnServer;

    @Test
    public void clearLocalCachesClearsRealmCache() {
        String realmId = managedRealm.getId();

        Assumptions.assumeTrue(runOnServer.fetch(s ->
            Profile.isFeatureEnabled(Profile.Feature.STATELESS), Boolean.class),
            "Cache clearing on health recovery requires stateless mode");

        // Invoke the registered onHealthRestored callback via the JDBC-PING protocol,
        // verifying the full wiring from inject() through to cache clearing.
        runOnServer.run(s -> {
            InfinispanConnectionProvider provider = s.getProvider(InfinispanConnectionProvider.class);
            assertTrue(provider.getCache("realms").containsKey(realmId), "Realm should be in cache before clearing");
            var cm = provider.getCache(InfinispanConnectionProvider.WORK_CACHE_NAME).getCacheManager();
            var transport = (org.infinispan.remoting.transport.jgroups.JGroupsTransport)
                    org.infinispan.factories.GlobalComponentRegistry
                            .componentOf(cm, org.infinispan.remoting.transport.Transport.class);
            assertNotNull(transport, "Should not run in local mode");
            var ping = (KEYCLOAK_JDBC_PING2) transport.getChannel().getProtocolStack()
                    .findProtocol(KEYCLOAK_JDBC_PING2.class);
            assertNotNull(ping, "KEYCLOAK_JDBC_PING2 protocol should be present in clustered mode");
            Runnable callback = ping.getOnHealthRestored();
            assertNotNull(callback, "onHealthRestored callback should be registered in stateless mode");
            callback.run();
        });

        // Use masterRunOnServer to avoid re-populating the managed realm's cache entry.
        // The callback uses putAsync + async listener dispatch.
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
            assertTrue(masterRunOnServer.fetch(s -> {
                InfinispanConnectionProvider provider = s.getProvider(InfinispanConnectionProvider.class);
                Cache<Object, Object> cache = provider.getCache("realms");
                return !cache.containsKey(realmId);
            }, Boolean.class), "Realm cache should be cleared after health recovery broadcast")
        );
    }
}
