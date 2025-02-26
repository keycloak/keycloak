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
        var mtlsEnabled = assumeEnabledAndOverwriteRotation(1, TimeUnit.DAYS);
        Assume.assumeTrue(mtlsEnabled);
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
        var mtlsEnabled = assumeEnabledAndOverwriteRotation(5, TimeUnit.SECONDS);
        Assume.assumeTrue(mtlsEnabled);
        assertClusterSize();

        var alias = currentCertificateAliasFor(0);
        log.infof("Current JGroups Certificate alias: %s", alias);

        // The certificate should rotate after 5 seconds
        assertAliasNotEquals(alias);
    }

    @Test
    public void testCoordinatorHasScheduleTask() {
        Assume.assumeTrue(getClusterSize() >= 2);
        var mtlsEnabled = assumeEnabledAndOverwriteRotation(1, TimeUnit.DAYS);
        Assume.assumeTrue(mtlsEnabled);

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

    private boolean assumeEnabledAndOverwriteRotation(long time, TimeUnit timeUnit) {
        boolean enabled = false;
        for (int i = 0; i < getClusterSize(); ++i) {
            var crmEnabled = getTestingClientFor(backendNode(i))
                    .server()
                    .fetch(session -> {
                        var crm = certificateReloadManager(session);
                        if (crm == null) {
                            return false;
                        }
                        crm.setRotationSeconds(timeUnit.toSeconds(time));
                        if (crm.isCoordinator()) {
                            crm.rotateCertificate();
                        }
                        return true;
                    }, Boolean.class);
            if (crmEnabled) {
                enabled = true;
            }
        }
        return enabled;
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


