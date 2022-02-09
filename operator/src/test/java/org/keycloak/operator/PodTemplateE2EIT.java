package org.keycloak.operator;

import io.fabric8.kubernetes.api.model.PodTemplateSpecBuilder;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.quarkus.logging.Log;
import io.quarkus.test.junit.QuarkusTest;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.keycloak.operator.utils.CRAssert;
import org.keycloak.operator.v2alpha1.crds.Keycloak;

import static java.util.concurrent.TimeUnit.MINUTES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.keycloak.operator.v2alpha1.crds.KeycloakStatusCondition.HAS_ERRORS;

@QuarkusTest
public class PodTemplateE2EIT extends ClusterOperatorTest {

    private Keycloak getEmptyPodTemplateKeycloak() {
        return Serialization.unmarshal(getClass().getResourceAsStream("/empty-podtemplate-keycloak.yml"), Keycloak.class);
    }

    private Resource<Keycloak> getCrSelector() {
        return k8sclient
                .resources(Keycloak.class)
                .inNamespace(namespace)
                .withName("example-podtemplate");
    }

    @Test
    public void testPodTemplateIsMerged() {
        // Arrange
        var keycloakWithPodTemplate = k8sclient
                .load(getClass().getResourceAsStream("/correct-podtemplate-keycloak.yml"));

        // Act
        keycloakWithPodTemplate.createOrReplace();

        // Assert
        Awaitility
                .await()
                .ignoreExceptions()
                .atMost(3, MINUTES).untilAsserted(() -> {
            Log.info("Getting logs from Keycloak");

            var keycloakPod = k8sclient
                    .pods()
                    .inNamespace(namespace)
                    .withLabel("app", "keycloak")
                    .list()
                    .getItems()
                    .get(0);

            var logs = k8sclient
                    .pods()
                    .inNamespace(namespace)
                    .withName(keycloakPod.getMetadata().getName())
                    .getLog();

            Log.info("Full logs are:\n" + logs);
            assertThat(logs).contains("Hello World");
            assertThat(keycloakPod.getMetadata().getLabels().get("foo")).isEqualTo("bar");
        });
    }

    @Test
    public void testPodTemplateIncorrectName() {
        // Arrange
        var plainKc = getEmptyPodTemplateKeycloak();
        var podTemplate = new PodTemplateSpecBuilder()
                .withNewMetadata()
                .withName("foo")
                .endMetadata()
                .build();
        plainKc.getSpec().getUnsupported().setPodTeplate(podTemplate);

        // Act
        k8sclient.resource(plainKc).createOrReplace();

        // Assert
        Log.info("Getting status of Keycloak");
        Awaitility
                .await()
                .ignoreExceptions()
                .atMost(3, MINUTES).untilAsserted(() -> {
                    CRAssert.assertKeycloakStatusCondition(getCrSelector().get(), HAS_ERRORS, false, "cannot be modified");
                });
    }

    @Test
    public void testPodTemplateIncorrectNamespace() {
        // Arrange
        var plainKc = getEmptyPodTemplateKeycloak();
        var podTemplate = new PodTemplateSpecBuilder()
                .withNewMetadata()
                .withNamespace("bar")
                .endMetadata()
                .build();
        plainKc.getSpec().getUnsupported().setPodTeplate(podTemplate);

        // Act
        k8sclient.resource(plainKc).createOrReplace();

        // Assert
        Log.info("Getting status of Keycloak");
        Awaitility
                .await()
                .ignoreExceptions()
                .atMost(3, MINUTES).untilAsserted(() -> {
                    CRAssert.assertKeycloakStatusCondition(getCrSelector().get(), HAS_ERRORS, false, "cannot be modified");
                });
    }

    @Test
    public void testPodTemplateIncorrectContainerName() {
        // Arrange
        var plainKc = getEmptyPodTemplateKeycloak();
        var podTemplate = new PodTemplateSpecBuilder()
                .withNewSpec()
                .addNewContainer()
                .withName("baz")
                .endContainer()
                .endSpec()
                .build();
        plainKc.getSpec().getUnsupported().setPodTeplate(podTemplate);

        // Act
        k8sclient.resource(plainKc).createOrReplace();

        // Assert
        Log.info("Getting status of Keycloak");
        Awaitility
                .await()
                .ignoreExceptions()
                .atMost(3, MINUTES).untilAsserted(() -> {
                    CRAssert.assertKeycloakStatusCondition(getCrSelector().get(), HAS_ERRORS, false, "cannot be modified");
                });
    }

    @Test
    public void testPodTemplateIncorrectDockerImage() {
        // Arrange
        var plainKc = getEmptyPodTemplateKeycloak();
        var podTemplate = new PodTemplateSpecBuilder()
                .withNewSpec()
                .addNewContainer()
                .withImage("foo")
                .endContainer()
                .endSpec()
                .build();
        plainKc.getSpec().getUnsupported().setPodTeplate(podTemplate);

        // Act
        k8sclient.resource(plainKc).createOrReplace();

        // Assert
        Log.info("Getting status of Keycloak");
        Awaitility
                .await()
                .ignoreExceptions()
                .atMost(3, MINUTES).untilAsserted(() -> {
                    CRAssert.assertKeycloakStatusCondition(getCrSelector().get(), HAS_ERRORS, false, "cannot be modified");
                });
    }

}
