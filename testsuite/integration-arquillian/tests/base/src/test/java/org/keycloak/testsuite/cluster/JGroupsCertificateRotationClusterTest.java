package org.keycloak.testsuite.cluster;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.infinispan.factories.GlobalComponentRegistry;
import org.infinispan.manager.EmbeddedCacheManager;
import org.junit.Assume;
import org.junit.Test;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.infinispan.module.certificates.CertificateReloadManager;
import org.keycloak.models.KeycloakSession;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class JGroupsCertificateRotationClusterTest extends AbstractClusterTest {

    @Test
    public void testRotation() {
        Assume.assumeTrue(getClusterSize() >= 2);
        Assume.assumeTrue(isMtlsEnabled());

        assertClusterSize();

        var alias = currentCertificateAliasFor(0);
        log.infof("Current JGroups Certificate alias: %s", alias);

        // test rotation in all the nodes
        for (int i = 0; i < getClusterSize(); ++i) {
            rotateCertificate(i);
            assertAliasNotEquals(alias);

            alias = currentCertificateAliasFor(i);
            log.infof("Current JGroups Certificate alias after rotation: %s", alias);
        }
    }

    @Test
    public void testAutoRotation() {
        Assume.assumeTrue(getClusterSize() >= 2);
        Assume.assumeTrue(isMtlsEnabled());

        try (var revert = overwriteRotation(5, TimeUnit.SECONDS)) {
            assertClusterSize();

            var alias = currentCertificateAliasFor(0);
            log.infof("Current JGroups Certificate alias: %s", alias);

            // The certificate should rotate after 5 seconds
            assertAliasNotEquals(alias);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testCoordinatorHasScheduleTask() {
        Assume.assumeTrue(getClusterSize() >= 2);
        Assume.assumeTrue(isMtlsEnabled());

        var alias = currentCertificateAliasFor(0);
        log.infof("Current JGroups Certificate alias: %s", alias);

        int coordinatorIdx = -1;
        for (int i = 0; i < getClusterSize(); ++i) {
            if (isCoordinator(i)) {
                assertTrue(hasRotationTask(i));
                coordinatorIdx = i;
                break;
            }
        }

        assertTrue(coordinatorIdx >= 0);
        killBackendNode(backendNode(coordinatorIdx));
        failback();
        assertClusterSize();

        // new coordinator should be the next in line
        coordinatorIdx++;
        if (coordinatorIdx >= getClusterSize()) {
            coordinatorIdx = 0;
        }

        assertTrue(isCoordinator(coordinatorIdx));
        assertTrue(hasRotationTask(coordinatorIdx));
    }

    private boolean isMtlsEnabled() {
        boolean isMtlsEnabled = true;
        for (int i = 0; i < getClusterSize(); ++i) {
            var crmEnabled = getTestingClientFor(backendNode(i))
                    .server()
                    .fetch(session -> {
                        var crm = certificateReloadManager(session);
                        return crm != null;
                    }, Boolean.class);

            isMtlsEnabled = isMtlsEnabled && crmEnabled;
        }

        return isMtlsEnabled;
    }

    private AutoCloseable overwriteRotation(long time, TimeUnit timeUnit) {
        long previousRotationSeconds = 0;
        for (int i = 0; i < getClusterSize(); ++i) {
            previousRotationSeconds = getTestingClientFor(backendNode(i))
                    .server()
                    .fetch(session -> {
                        var crm = certificateReloadManager(session);
                        if (crm == null) {
                            throw new RuntimeException("MTLS is not enabled");
                        }
                        long originalRotation = crm.getRotationSeconds();
                        crm.setRotationSeconds(timeUnit.toSeconds(time));
                        if (crm.isCoordinator()) {
                            crm.rotateCertificate();
                        }
                        return originalRotation;
                    }, Long.class);
        }

        long finalPreviousRotationSeconds = previousRotationSeconds;
        return () -> {
            for (int i = 0; i < getClusterSize(); ++i) {
                getTestingClientFor(backendNode(i))
                        .server()
                        .run(session -> {
                            var crm = certificateReloadManager(session);
                            if (crm == null) {
                                throw new RuntimeException("MTLS is not enabled");
                            }
                            crm.setRotationSeconds(finalPreviousRotationSeconds);
                            if (crm.isCoordinator()) {
                                crm.rotateCertificate();
                            }
                        });
            }
        };
    }

    private void assertAliasNotEquals(String alias) {
        for (int i = 0; i < getClusterSize(); ++i) {
            int nodeIdx = i;
            Awaitility.waitAtMost(Duration.ofMinutes(1))
                    .pollDelay(Duration.ofSeconds(1))
                    .untilAsserted(() -> assertNotEquals(alias, currentCertificateAliasFor(nodeIdx)));
        }
    }

    private String currentCertificateAliasFor(int index) {
        return getTestingClientFor(backendNode(index)).server().fetch(JGroupsCertificateRotationClusterTest::currentCertificateAlias, String.class);
    }

    private void rotateCertificate(int index) {
        getTestingClientFor(backendNode(index)).server().run(JGroupsCertificateRotationClusterTest::rotateCertificate);
    }

    private boolean isCoordinator(int index) {
        return getTestingClientFor(backendNode(index)).server().fetch(session -> certificateReloadManager(session).isCoordinator(), Boolean.class);
    }

    private boolean hasRotationTask(int index) {
        return getTestingClientFor(backendNode(index)).server().fetch(session -> certificateReloadManager(session).hasRotationTask(), Boolean.class);
    }

    private int fetchClusterSize(int index) {
        return getTestingClientFor(backendNode(index)).server().fetch(session -> cacheManager(session).getMembers().size(), Integer.class);
    }

    private void assertClusterSize(){
        var expectedSize = getClusterSize();
        for (int i = 0; i < expectedSize; ++i) {
            var nodeIndex = i;
            Awaitility.waitAtMost(Duration.ofMinutes(1))
                    .pollDelay(Duration.ofSeconds(1))
                    .untilAsserted(() -> assertEquals(expectedSize, fetchClusterSize(nodeIndex)));
        }
    }

    private static CertificateReloadManager certificateReloadManager(KeycloakSession session) {
        return GlobalComponentRegistry.componentOf(cacheManager(session), CertificateReloadManager.class);
    }

    private static EmbeddedCacheManager cacheManager(KeycloakSession session) {
        return session.getProvider(InfinispanConnectionProvider.class)
                .getCache(InfinispanConnectionProvider.USER_CACHE_NAME)
                .getCacheManager();
    }

    private static String currentCertificateAlias(KeycloakSession session) {
        return certificateReloadManager(session)
                .currentCertificate()
                .getAlias();
    }

    private static void rotateCertificate(KeycloakSession session) {
        certificateReloadManager(session).rotateCertificate();
    }

}


