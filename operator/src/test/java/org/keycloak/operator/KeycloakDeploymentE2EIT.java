package org.keycloak.operator;

import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpecBuilder;
import io.quarkus.logging.Log;
import io.quarkus.test.junit.QuarkusTest;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.keycloak.operator.v2alpha1.crds.Keycloak;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.keycloak.operator.Constants.DEFAULT_LABELS;
import static org.keycloak.operator.utils.K8sUtils.deployKeycloak;
import static org.keycloak.operator.utils.K8sUtils.getDefaultKeycloakDeployment;
import static org.keycloak.operator.utils.K8sUtils.waitForKeycloakToBeReady;

@QuarkusTest
public class KeycloakDeploymentE2EIT extends ClusterOperatorTest {
    @Test
    public void testBasicKeycloakDeploymentAndDeletion() {
        try {
            // CR
            Log.info("Creating new Keycloak CR example");
            var kc = getDefaultKeycloakDeployment();
            var deploymentName = kc.getMetadata().getName();
            deployKeycloak(k8sclient, kc, true);

            // Check Operator has deployed Keycloak
            Log.info("Checking Operator has deployed Keycloak deployment");
            assertThat(k8sclient.apps().deployments().inNamespace(namespace).withName(deploymentName).get()).isNotNull();

            // Check Keycloak has correct replicas
            Log.info("Checking Keycloak pod has ready replicas == 1");
            assertThat(k8sclient.apps().deployments().inNamespace(namespace).withName(deploymentName).get().getStatus().getReadyReplicas()).isEqualTo(1);

            // Delete CR
            Log.info("Deleting Keycloak CR and watching cleanup");
            k8sclient.resources(Keycloak.class).delete(kc);
            Awaitility.await()
                    .untilAsserted(() -> assertThat(k8sclient.apps().deployments().inNamespace(namespace).withName(deploymentName).get()).isNull());
        } catch (Exception e) {
            savePodLogs();
            throw e;
        }
    }

    @Test
    public void testCRFields() {
        try {
            var kc = getDefaultKeycloakDeployment();
            var deploymentName = kc.getMetadata().getName();
            deployKeycloak(k8sclient, kc, true);

            kc.getSpec().setImage("quay.io/keycloak/non-existing-keycloak");
            kc.getSpec().getServerConfiguration().put("KC_DB_PASSWORD", "Ay Caramba!");
            deployKeycloak(k8sclient, kc, false);

            Awaitility.await()
                    .during(Duration.ofSeconds(15)) // check if the Deployment is stable
                    .untilAsserted(() -> {
                        var c = k8sclient.apps().deployments().inNamespace(namespace).withName(deploymentName).get()
                                .getSpec().getTemplate().getSpec().getContainers().get(0);
                        assertThat(c.getImage()).isEqualTo("quay.io/keycloak/non-existing-keycloak");
                        assertThat(c.getEnv().stream()
                                .anyMatch(e -> e.getName().equals("KC_DB_PASSWORD") && e.getValue().equals("Ay Caramba!")))
                                .isTrue();
                    });

        } catch (Exception e) {
            savePodLogs();
            throw e;
        }
    }

    @Test
    public void testDeploymentDurability() {
        try {
            var kc = getDefaultKeycloakDeployment();
            var deploymentName = kc.getMetadata().getName();
            deployKeycloak(k8sclient, kc, true);

            Log.info("Trying to delete deployment");
            assertThat(k8sclient.apps().deployments().withName(deploymentName).delete()).isTrue();
            Awaitility.await()
                    .untilAsserted(() -> assertThat(k8sclient.apps().deployments().withName(deploymentName).get()).isNotNull());

            waitForKeycloakToBeReady(k8sclient, kc); // wait for reconciler to calm down to avoid race condititon

            Log.info("Trying to modify deployment");

            var deployment = k8sclient.apps().deployments().withName(deploymentName).get();
            var labels = Map.of("address", "EvergreenTerrace742");
            var flandersEnvVar = new EnvVarBuilder().withName("NEIGHBOR").withValue("Stupid Flanders!").build();
            var origSpecs = new DeploymentSpecBuilder(deployment.getSpec()).build(); // deep copy

            deployment.getMetadata().getLabels().putAll(labels);
            deployment.getSpec().getTemplate().getSpec().getContainers().get(0).setEnv(List.of(flandersEnvVar));
            k8sclient.apps().deployments().createOrReplace(deployment);

            Awaitility.await()
                    .untilAsserted(() -> {
                        var d = k8sclient.apps().deployments().withName(deploymentName).get();
                        assertThat(d.getMetadata().getLabels().entrySet().containsAll(labels.entrySet())).isTrue(); // additional labels should not be overwritten
                        assertThat(d.getSpec()).isEqualTo(origSpecs); // specs should be reconciled back to original values
                    });
        } catch (Exception e) {
            savePodLogs();
            throw e;
        }
    }

    @Test
    public void testExtensions() {
        try {
            var kc = getDefaultKeycloakDeployment();
            kc.getSpec().setExtensions(
                    Collections.singletonList(
                            "https://github.com/aerogear/keycloak-metrics-spi/releases/download/2.5.3/keycloak-metrics-spi-2.5.3.jar"));
            deployKeycloak(k8sclient, kc, true);

            var kcPod = k8sclient
                    .pods()
                    .inNamespace(namespace)
                    .withLabels(DEFAULT_LABELS)
                    .list()
                    .getItems()
                    .get(0);

            Awaitility.await()
                    .ignoreExceptions()
                    .untilAsserted(() -> {
                        var logs = k8sclient.pods().inNamespace(namespace).withName(kcPod.getMetadata().getName()).getLog();

                        assertTrue(logs.contains("metrics-listener (org.jboss.aerogear.keycloak.metrics.MetricsEventListenerFactory) is implementing the internal SPI"));
                    });
        } catch (Exception e) {
            savePodLogs();
            throw e;
        }
    }

}
