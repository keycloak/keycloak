package org.keycloak.operator;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.quarkus.test.junit.QuarkusTest;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.operator.v2alpha1.crds.Keycloak;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.keycloak.operator.utils.K8sUtils.deployKeycloak;
import static org.keycloak.operator.utils.K8sUtils.getDefaultKeycloakDeployment;

@QuarkusTest
public class ClusteringE2EIT extends ClusterOperatorTest {
    @BeforeEach
    public void cleanResources() {
        k8sclient.resources(Keycloak.class).inNamespace(namespace).delete();
    }

    @Test
    public void given_ClusterAndOperatorRunning_when_KeycloakCRScaled_Then_PodsAreReady() throws URISyntaxException {
        // given
        var kc = getDefaultKeycloakDeployment();
        var deploymentName = kc.getMetadata().getName();
        deployKeycloak(k8sclient, kc, true);

        Keycloak keycloak = k8sclient.resources(Keycloak.class)
                .inNamespace(namespace)
                .list().getItems().get(0);

        // when scale it to 10
        keycloak.getSpec().setInstances(10);
        k8sclient.resources(Keycloak.class).inNamespace(namespace).createOrReplace(keycloak);
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> assertThat(k8sclient.pods().inNamespace(namespace).withLabel("app", "keycloak").list().getItems().size()).isEqualTo(10));

        // when scale it down to 2
        keycloak.getSpec().setInstances(2);
        k8sclient.resources(Keycloak.class).inNamespace(namespace).createOrReplace(keycloak);
        Awaitility.await()
                .atMost(Duration.ofSeconds(180))
                .untilAsserted(() -> assertThat(k8sclient.pods().inNamespace(namespace).withLabel("app", "keycloak").list().getItems().size()).isEqualTo(2));

        // get the service
        Service service = k8sclient.services().inNamespace(namespace).withName(keycloak.getMetadata().getName() + "-service").get();

        // clone the service but NodePort
        Service nodeportTestService = new ServiceBuilder(service).build();
        nodeportTestService.getMetadata().setName(keycloak.getMetadata().getName()+ "-servicenodeport");
        nodeportTestService.getSpec().getPorts().get(0).setNodePort(31999);
        nodeportTestService.getSpec().setType("NodePort");
        nodeportTestService.getSpec().setClusterIP(null);
        nodeportTestService.getSpec().setClusterIPs(null);
        k8sclient.services().inNamespace(namespace).createOrReplace(nodeportTestService);

        // check we can reach Keycloak
        URI uri = new URI(k8sclient.getConfiguration()
                    .getMasterUrl()
                    .replace("8443", "31999")
                    .replace("https", "http") + "realms/master")
                    ;
        Awaitility.await()
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> given().when().relaxedHTTPSValidation().get(uri).then().extract().statusCode() == 200);

        // can we check infinispan ?
    }

}
