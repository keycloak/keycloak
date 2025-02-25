package org.keycloak.testsuite.cluster;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.infinispan.factories.GlobalComponentRegistry;
import org.infinispan.manager.EmbeddedCacheManager;
import org.junit.Assume;
import org.junit.Test;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.infinispan.module.certificates.CertificateReloadManager;
import org.keycloak.models.KeycloakSession;

public class JGroupsCertificateRotationClusterTest extends AbstractClusterTest {

    @Test
    public void testRotation() {
        Assume.assumeTrue(getClusterSize() >= 2);
        var mtlsEnabled = assumeEnabledAndOverwriteRotation(1, TimeUnit.DAYS);
        Assume.assumeTrue(mtlsEnabled);

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

        var alias = currentCertificateAliasFor(0);
        log.infof("Current JGroups Certificate alias: %s", alias);

        // The certificate should rotate after 5 seconds
        assertAliasNotEquals(alias);
    }

    private boolean assumeEnabledAndOverwriteRotation(long time, TimeUnit timeUnit) {
        boolean enabled = false;
        for (int i = 0; i < getClusterSize(); ++i) {
            enabled = enabled || getTestingClientFor(backendNode(0))
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
        }
        return enabled;
    }

    private void assertAliasNotEquals(String alias) {
        for (int i = 0; i < getClusterSize(); ++i) {
            int nodeIdx = i;
            Awaitility.waitAtMost(Duration.ofMinutes(1))
                    .pollDelay(Duration.ofSeconds(1))
                    .until(() -> !Objects.equals(alias, currentCertificateAliasFor(nodeIdx)));
        }
    }

    private String currentCertificateAliasFor(int index) {
        return getTestingClientFor(backendNode(index)).server().fetch(JGroupsCertificateRotationClusterTest::currentCertificateAlias, String.class);
    }

    private void rotateCertificate(int index) {
        getTestingClientFor(backendNode(index)).server().run(JGroupsCertificateRotationClusterTest::rotateCertificate);
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


