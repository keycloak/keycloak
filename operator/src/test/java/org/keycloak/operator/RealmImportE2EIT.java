package org.keycloak.operator;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.extended.run.RunConfigBuilder;
import io.quarkus.logging.Log;
import io.quarkus.test.junit.QuarkusTest;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.keycloak.operator.v2alpha1.crds.KeycloakRealmImport;
import org.keycloak.operator.v2alpha1.crds.KeycloakRealmImportStatusCondition;

import java.util.List;
import java.util.Map;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.keycloak.operator.v2alpha1.crds.KeycloakRealmImportStatusCondition.DONE;
import static org.keycloak.operator.v2alpha1.crds.KeycloakRealmImportStatusCondition.STARTED;
import static org.keycloak.operator.v2alpha1.crds.KeycloakRealmImportStatusCondition.HAS_ERRORS;

@QuarkusTest
public class RealmImportE2EIT extends ClusterOperatorTest {

    final static String KEYCLOAK_SERVICE_NAME = "example-keycloak";
    final static int KEYCLOAK_PORT = 8080;

    private KeycloakRealmImportStatusCondition getCondition(List<KeycloakRealmImportStatusCondition> conditions, String type) {
        return conditions
                .stream()
                .filter(c -> c.getType().equals(type))
                .findFirst()
                .get();
    }

    @Test
    public void testWorkingRealmImport() {
        Log.info(((operatorDeployment == OperatorDeployment.remote) ? "Remote " : "Local ") + "Run Test :" + namespace);
        // Arrange
        k8sclient.load(getClass().getResourceAsStream("/example-postgres.yaml")).inNamespace(namespace).createOrReplace();
        k8sclient.load(getClass().getResourceAsStream("/example-keycloak.yml")).inNamespace(namespace).createOrReplace();

        k8sclient.services().inNamespace(namespace).create(
                new ServiceBuilder()
                        .withNewMetadata()
                        .withName(KEYCLOAK_SERVICE_NAME)
                        .withNamespace(namespace)
                        .endMetadata()
                        .withNewSpec()
                        .withSelector(Map.of("app", "keycloak"))
                        .addNewPort()
                        .withPort(KEYCLOAK_PORT)
                        .endPort()
                        .endSpec()
                        .build()
        );

        // Act
        k8sclient.load(getClass().getResourceAsStream("/example-realm.yaml")).inNamespace(namespace).createOrReplace();

        // Assert
        var crSelector = k8sclient
                .resources(KeycloakRealmImport.class)
                .inNamespace(namespace)
                .withName("example-count0-kc");
        Awaitility.await()
                .atMost(3, MINUTES)
                .pollDelay(5, SECONDS)
                .ignoreExceptions()
                .untilAsserted(() -> {
                    var conditions = crSelector
                            .get()
                            .getStatus()
                            .getConditions();

                    assertThat(getCondition(conditions, DONE).getStatus()).isFalse();
                    assertThat(getCondition(conditions, STARTED).getStatus()).isTrue();
                    assertThat(getCondition(conditions, HAS_ERRORS).getStatus()).isFalse();
                });

        Awaitility.await()
                .atMost(3, MINUTES)
                .pollDelay(5, SECONDS)
                .ignoreExceptions()
                .untilAsserted(() -> {
                    var conditions = crSelector
                            .get()
                            .getStatus()
                            .getConditions();

                    assertThat(getCondition(conditions, DONE).getStatus()).isTrue();
                    assertThat(getCondition(conditions, STARTED).getStatus()).isFalse();
                    assertThat(getCondition(conditions, HAS_ERRORS).getStatus()).isFalse();
                });

        String url =
                "http://" + KEYCLOAK_SERVICE_NAME + "." + namespace + ":" + KEYCLOAK_PORT + "/realms/count0";

        Awaitility.await().atMost(5, MINUTES).untilAsserted(() -> {
            try {
                Log.info("Starting curl Pod to test if the realm is available");

                Pod curlPod = k8sclient.run().inNamespace(namespace)
                        .withRunConfig(new RunConfigBuilder()
                                .withArgs("-s", "-o", "/dev/null", "-w", "%{http_code}", url)
                                .withName("curl")
                                .withImage("curlimages/curl:7.78.0")
                                .withRestartPolicy("Never")
                                .build())
                        .done();
                Log.info("Waiting for curl Pod to finish running");
                Awaitility.await().atMost(2, MINUTES)
                        .until(() -> {
                            String phase =
                                    k8sclient.pods().inNamespace(namespace).withName("curl").get()
                                            .getStatus().getPhase();
                            return phase.equals("Succeeded") || phase.equals("Failed");
                        });

                String curlOutput =
                        k8sclient.pods().inNamespace(namespace)
                                .withName(curlPod.getMetadata().getName()).getLog();
                Log.info("Output from curl: '" + curlOutput + "'");
                assertThat(curlOutput).isEqualTo("200");
            } catch (KubernetesClientException ex) {
                throw new AssertionError(ex);
            } finally {
                Log.info("Deleting curl Pod");
                k8sclient.pods().inNamespace(namespace).withName("curl").delete();
                Awaitility.await().atMost(1, MINUTES)
                        .until(() -> k8sclient.pods().inNamespace(namespace).withName("curl")
                                .get() == null);
            }
        });
    }

    @Test
    public void testNotWorkingRealmImport() {
        Log.info(((operatorDeployment == OperatorDeployment.remote) ? "Remote " : "Local ") + "Run Test :" + namespace);
        // Arrange
        k8sclient.load(getClass().getResourceAsStream("/example-postgres.yaml")).inNamespace(namespace).createOrReplace();
        k8sclient.load(getClass().getResourceAsStream("/example-keycloak.yml")).inNamespace(namespace).createOrReplace();

        // Act
        k8sclient.load(getClass().getResourceAsStream("/incorrect-realm.yaml")).inNamespace(namespace).createOrReplace();

        // Assert
        Awaitility.await()
                .atMost(3, MINUTES)
                .pollDelay(5, SECONDS)
                .ignoreExceptions()
                .untilAsserted(() -> {
                    var conditions = k8sclient
                            .resources(KeycloakRealmImport.class)
                            .inNamespace(namespace)
                            .withName("example-count0-kc")
                            .get()
                            .getStatus()
                            .getConditions();

                    assertThat(getCondition(conditions, HAS_ERRORS).getStatus()).isTrue();
                    assertThat(getCondition(conditions, DONE).getStatus()).isFalse();
                    assertThat(getCondition(conditions, STARTED).getStatus()).isFalse();
                });
    }

}
