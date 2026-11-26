package org.keycloak.tests.clustering;

import java.time.Duration;

import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.jgroups.certificates.CertificateReloadManager;
import org.keycloak.models.KeycloakSession;
import org.keycloak.testframework.annotations.InjectLoadBalancer;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.clustering.LoadBalancer;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

import org.awaitility.Awaitility;
import org.infinispan.factories.GlobalComponentRegistry;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Stops the coordinator, so the cluster can't be shared with other tests. The dedicated server configuration makes
 * the framework start a new server for this class.
 */
@KeycloakIntegrationTest(config = JGroupsCertificateCoordinatorFailoverTest.FailoverServerConfig.class)
public class JGroupsCertificateCoordinatorFailoverTest {

    @InjectRealm
    ManagedRealm realm;

    @InjectLoadBalancer
    LoadBalancer loadBalancer;

    @InjectRunOnServer(permittedPackages = "org.keycloak.tests.clustering")
    RunOnServerClient runOnServer;

    @Test
    public void testNewCoordinatorHasRotationTask() {
        // stateless nodes are independent clusters
        Assumptions.assumeFalse(loadBalancer.isStateless());
        Assumptions.assumeTrue(loadBalancer.nodeCount() >= 2);

        int coordinatorIdx = -1;
        for (int i = 0; i < loadBalancer.nodeCount(); ++i) {
            loadBalancer.node(i);
            if (isCoordinator()) {
                assertTrue(hasRotationTask());
                coordinatorIdx = i;
                break;
            }
        }
        assertTrue(coordinatorIdx >= 0);

        loadBalancer.stopNode(coordinatorIdx);

        // the next node in line becomes the coordinator
        int newCoordinatorIdx = (coordinatorIdx + 1) % loadBalancer.nodeCount();
        loadBalancer.node(newCoordinatorIdx);
        Awaitility.waitAtMost(Duration.ofMinutes(1))
                .pollDelay(Duration.ofSeconds(1))
                .untilAsserted(() -> {
                    assertEquals(loadBalancer.nodeCount() - 1, memberCount());
                    assertTrue(isCoordinator());
                    assertTrue(hasRotationTask());
                });
    }

    private boolean isCoordinator() {
        return runOnServer.fetch(session -> certificateReloadManager(session).isCoordinator(), Boolean.class);
    }

    private boolean hasRotationTask() {
        return runOnServer.fetch(session -> certificateReloadManager(session).hasRotationTask(), Boolean.class);
    }

    private int memberCount() {
        return runOnServer.fetch(session -> cacheManager(session).getMembers().size(), Integer.class);
    }

    private static CertificateReloadManager certificateReloadManager(KeycloakSession session) {
        return GlobalComponentRegistry.componentOf(cacheManager(session), CertificateReloadManager.class);
    }

    private static org.infinispan.manager.EmbeddedCacheManager cacheManager(KeycloakSession session) {
        return session.getProvider(InfinispanConnectionProvider.class)
                .getCache(InfinispanConnectionProvider.USER_CACHE_NAME)
                .getCacheManager();
    }

    public static class FailoverServerConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config;
        }
    }
}
