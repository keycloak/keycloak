package org.keycloak.tests.cluster;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.providers.runonserver.ClusterTestTasks;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@KeycloakIntegrationTest
public class JGroupsCertificateRotationClusterTest extends AbstractClusterTest {

    @InjectRealm
    ManagedRealm managedRealm;

    @InjectRunOnServer(permittedPackages = {"org.keycloak.tests"}, ref = "cluster-run-on-server")
    RunOnServerClient clusterRunOnServer;

    @Test
    public void testRotation() {
        assumeTrue(getClusterSize() >= 2);
        assumeTrue(isMtlsEnabled());

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
        assumeTrue(getClusterSize() >= 2);
        assumeTrue(isMtlsEnabled());

        try (var revert = overwriteRotation(5, ChronoUnit.SECONDS)) {
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
        assumeTrue(getClusterSize() >= 2);
        assumeTrue(isMtlsEnabled());

        int coordinatorIdx = -1;
        for (int i = 0; i < getClusterSize(); ++i) {
            if (isCoordinator(i)) {
                assertTrue(hasRotationTask(i));
                coordinatorIdx = i;
                break;
            }
        }

        assertTrue(coordinatorIdx >= 0);
        ContainerInfo coordinatorNode = backendNode(coordinatorIdx);
        killBackendNode(coordinatorNode);
        failback();
        assertClusterSize();

        boolean foundCoordinatorWithTask = false;
        for (int i = 0; i < getClusterSize(); ++i) {
            if (isCoordinator(i) && hasRotationTask(i)) {
                foundCoordinatorWithTask = true;
                break;
            }
        }
        assertTrue(foundCoordinatorWithTask, "Expected a coordinator with scheduled rotation task after failback");
    }

    private boolean isMtlsEnabled() {
        boolean isMtlsEnabled = true;
        for (int i = 0; i < getClusterSize(); ++i) {
            var crmEnabled = testingClientFor(backendNode(i))
                    .server()
                    .fetch(new ClusterTestTasks.HasCertificateReloadManager(), Boolean.class);

            isMtlsEnabled = isMtlsEnabled && crmEnabled;
        }

        return isMtlsEnabled;
    }

    private AutoCloseable overwriteRotation(long amount, ChronoUnit timeUnit) {
        long previousRotationSeconds = 0;
        for (int i = 0; i < getClusterSize(); ++i) {
            previousRotationSeconds = testingClientFor(backendNode(i))
                    .server()
                    .fetch(new ClusterTestTasks.OverwriteRotationPeriod(amount, timeUnit), Long.class);
        }

        long finalPreviousRotationSeconds = previousRotationSeconds;
        return () -> {
            for (int i = 0; i < getClusterSize(); ++i) {
                testingClientFor(backendNode(i))
                        .server()
                        .run(new ClusterTestTasks.RestoreRotationPeriod(finalPreviousRotationSeconds));
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
        return testingClientFor(backendNode(index)).server().fetch(new ClusterTestTasks.CurrentCertificateAlias(), String.class);
    }

    private void rotateCertificate(int index) {
        testingClientFor(backendNode(index)).server().run(new ClusterTestTasks.RotateCertificate());
    }

    private boolean isCoordinator(int index) {
        return testingClientFor(backendNode(index)).server().fetch(new ClusterTestTasks.IsCoordinator(), Boolean.class);
    }

    private boolean hasRotationTask(int index) {
        return testingClientFor(backendNode(index)).server().fetch(new ClusterTestTasks.HasRotationTask(), Boolean.class);
    }

    private int fetchClusterSize(int index) {
        return testingClientFor(backendNode(index)).server().fetch(new ClusterTestTasks.ClusterMembersCount(), Integer.class);
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

    private ClusterTestingClient testingClientFor(ContainerInfo node) {
        return new ClusterTestingClient(node.getIndex(), loadBalancer, clusterRunOnServer);
    }

}
