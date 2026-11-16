package org.keycloak.tests.clustering;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.jgroups.certificates.CertificateReloadManager;
import org.keycloak.jgroups.certificates.DatabaseJGroupsCertificateProvider;
import org.keycloak.jgroups.certificates.JGroupsCertificate;
import org.keycloak.jgroups.certificates.KeySpec;
import org.keycloak.jgroups.certificates.Utils;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.spi.infinispan.JGroupsCertificateProvider;
import org.keycloak.storage.configuration.ServerConfigStorageProvider;
import org.keycloak.testframework.annotations.InjectLoadBalancer;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.clustering.LoadBalancer;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;

import org.awaitility.Awaitility;
import org.infinispan.factories.GlobalComponentRegistry;
import org.infinispan.manager.EmbeddedCacheManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests rotation and startup migration of the generated JGroups mTLS certificate. The code is executed on a specific
 * node by selecting it in the load balancer before running on the server.
 */
@KeycloakIntegrationTest
public class JGroupsCertificateRotationClusterTest {

    @InjectRealm
    ManagedRealm realm;

    @InjectLoadBalancer
    LoadBalancer loadBalancer;

    @InjectRunOnServer(permittedPackages = "org.keycloak.tests.clustering")
    RunOnServerClient runOnServer;

    @BeforeEach
    public void checkMtlsEnabled() {
        // stateless nodes are independent clusters
        Assumptions.assumeFalse(loadBalancer.isStateless());
        Assumptions.assumeTrue(loadBalancer.nodeCount() >= 2);
        for (int i = 0; i < loadBalancer.nodeCount(); ++i) {
            loadBalancer.node(i);
            Assumptions.assumeTrue(runOnServer.fetch(session -> certificateReloadManager(session) != null, Boolean.class));
        }
        assertClusterSize();
    }

    @AfterEach
    public void cleanup() {
        loadBalancer.node(0);
    }

    @Test
    public void testRotation() {
        var alias = currentCertificateAlias(0);

        // test rotation in all the nodes
        for (int i = 0; i < loadBalancer.nodeCount(); ++i) {
            loadBalancer.node(i);
            runOnServer.run(session -> certificateReloadManager(session).rotateCertificate());
            assertAliasNotEquals(alias);
            alias = currentCertificateAlias(i);
        }
    }

    @Test
    public void testAutoRotation() {
        setRotationPeriod(5);
        try {
            var alias = currentCertificateAlias(0);
            // The certificate should rotate after 5 seconds
            assertAliasNotEquals(alias);
        } finally {
            setRotationPeriod(Duration.ofDays(30).toSeconds());
        }
    }

    @ParameterizedTest
    @CsvSource({"RSA:2048, RSA:3072", "RSA:3072, RSA:2048", "RSA:2048, ECDSA:secp256r1"})
    public void testStartupReplacesCertificateWithDifferentKey(String storedSpec, String configuredSpec) {
        assertStartupMigration(storedSpec, configuredSpec, true);
    }

    @Test
    public void testStartupKeepsCertificateWithUnchangedKey() {
        assertStartupMigration("RSA:2048", "RSA:2048", false);
    }

    @Test
    public void testConcurrentStartupConvergesOnCommittedCertificate() {
        loadBalancer.node(0);
        try {
            var converged = runOnServer.fetch(session -> {
                var factory = session.getKeycloakSessionFactory();
                seedCertificate(factory, keySpec("RSA:2048"));
                var spec = keySpec("RSA:3072");
                var starts = 4;
                var executor = Executors.newFixedThreadPool(starts);
                try {
                    // release all starts at once so they overlap
                    var go = new CountDownLatch(1);
                    List<Future<String>> futures = new ArrayList<>();
                    for (int i = 0; i < starts; i++) {
                        futures.add(executor.submit(() -> {
                            go.await();
                            return DatabaseJGroupsCertificateProvider.create(factory, Duration.ofDays(30), spec)
                                    .getCurrentCertificate().getAlias();
                        }));
                    }
                    go.countDown();
                    var aliases = new ArrayList<String>();
                    for (var future : futures) {
                        aliases.add(future.get());
                    }
                    var stored = storedCertificate(factory);
                    return spec.matches(stored.getCertificate().getPublicKey())
                            && aliases.stream().allMatch(stored.getAlias()::equals);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    executor.shutdownNow();
                }
            }, Boolean.class);
            assertTrue(converged, "All concurrently started nodes must use the committed certificate");
        } finally {
            restoreCertificate();
        }
    }

    private void assertStartupMigration(String storedSpec, String configuredSpec, boolean expectReplacement) {
        loadBalancer.node(0);
        try {
            var result = runOnServer.fetch(session -> {
                var factory = session.getKeycloakSessionFactory();
                var configured = keySpec(configuredSpec);
                var seeded = seedCertificate(factory, keySpec(storedSpec)).getAlias();
                var provider = DatabaseJGroupsCertificateProvider.create(factory, Duration.ofDays(30), configured);
                var committed = storedCertificate(factory);
                boolean keyMatches = configured.matches(committed.getCertificate().getPublicKey());
                boolean aliasChanged = !seeded.equals(committed.getAlias());
                boolean providerUsesCommitted = committed.getAlias().equals(provider.getCurrentCertificate().getAlias());
                return keyMatches && providerUsesCommitted && aliasChanged == expectReplacement;
            }, Boolean.class);
            assertTrue(result);
        } finally {
            restoreCertificate();
        }
    }

    // The tests overwrite the stored certificate, rotate it so that the nodes continue with a certificate matching their configuration.
    private void restoreCertificate() {
        // the coordinator rotates only if it knows the alias in the database, so make every node load the seeded certificate first
        for (int i = 0; i < loadBalancer.nodeCount(); ++i) {
            loadBalancer.node(i);
            runOnServer.run(session -> databaseJGroupsCertificateProvider(session).reloadCertificate());
        }
        for (int i = 0; i < loadBalancer.nodeCount(); ++i) {
            loadBalancer.node(i);
            runOnServer.run(session -> {
                if (certificateReloadManager(session).isCoordinator()) {
                    certificateReloadManager(session).rotateCertificate();
                }
            });
        }
        assertClusterSize();
    }

    private void setRotationPeriod(long seconds) {
        for (int i = 0; i < loadBalancer.nodeCount(); ++i) {
            loadBalancer.node(i);
            runOnServer.run(session -> {
                databaseJGroupsCertificateProvider(session).setRotationPeriod(Duration.ofSeconds(seconds));
                if (certificateReloadManager(session).isCoordinator()) {
                    certificateReloadManager(session).rotateCertificate();
                }
            });
        }
    }

    private void assertAliasNotEquals(String alias) {
        for (int i = 0; i < loadBalancer.nodeCount(); ++i) {
            int nodeIdx = i;
            Awaitility.waitAtMost(Duration.ofMinutes(1))
                    .pollDelay(Duration.ofSeconds(1))
                    .untilAsserted(() -> assertNotEquals(alias, currentCertificateAlias(nodeIdx)));
        }
    }

    private String currentCertificateAlias(int index) {
        loadBalancer.node(index);
        return runOnServer.fetch(session -> databaseJGroupsCertificateProvider(session).getCurrentCertificate().getAlias(), String.class);
    }

    private void assertClusterSize() {
        var expectedSize = loadBalancer.nodeCount();
        for (int i = 0; i < expectedSize; ++i) {
            var nodeIndex = i;
            Awaitility.waitAtMost(Duration.ofMinutes(1))
                    .pollDelay(Duration.ofSeconds(1))
                    .untilAsserted(() -> {
                        loadBalancer.node(nodeIndex);
                        assertEquals(expectedSize, runOnServer.fetch(session -> cacheManager(session).getMembers().size(), Integer.class));
                    });
        }
    }

    // Only serializable values are captured by the lambdas run on the server, therefore the spec is "RSA:<bits>" or "ECDSA:<curve>".
    private static KeySpec keySpec(String spec) {
        var parts = spec.split(":");
        return "RSA".equals(parts[0]) ? KeySpec.of("RSA", Integer.valueOf(parts[1]), null) : KeySpec.of("ECDSA", null, parts[1]);
    }

    private static JGroupsCertificate seedCertificate(KeycloakSessionFactory factory, KeySpec spec) {
        var certificate = Utils.generateSelfSignedCertificate(Duration.ofDays(60), spec);
        KeycloakModelUtils.runJobInTransaction(factory, s -> s.getProvider(ServerConfigStorageProvider.class)
                .replace(DatabaseJGroupsCertificateProvider.CERTIFICATE_ID, json -> true, () -> JGroupsCertificate.toJson(certificate)));
        return certificate;
    }

    private static JGroupsCertificate storedCertificate(KeycloakSessionFactory factory) {
        return KeycloakModelUtils.runJobInTransactionWithResult(factory, s -> s.getProvider(ServerConfigStorageProvider.class)
                        .find(DatabaseJGroupsCertificateProvider.CERTIFICATE_ID))
                .map(JGroupsCertificate::fromJson)
                .orElseThrow();
    }

    private static CertificateReloadManager certificateReloadManager(KeycloakSession session) {
        return GlobalComponentRegistry.componentOf(cacheManager(session), CertificateReloadManager.class);
    }

    private static DatabaseJGroupsCertificateProvider databaseJGroupsCertificateProvider(KeycloakSession session) {
        return (DatabaseJGroupsCertificateProvider) session.getProvider(JGroupsCertificateProvider.class);
    }

    private static EmbeddedCacheManager cacheManager(KeycloakSession session) {
        return session.getProvider(InfinispanConnectionProvider.class)
                .getCache(InfinispanConnectionProvider.USER_CACHE_NAME)
                .getCacheManager();
    }
}
