package org.keycloak.operator;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.quarkus.logging.Log;
import io.quarkus.test.junit.QuarkusTest;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.operator.v2alpha1.crds.Keycloak;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
public class ClusteringE2EIT extends ClusterOperatorTest {
    @BeforeEach
    public void cleanResources() {
        k8sclient.resources(Keycloak.class).inNamespace(namespace).delete();
    }

    @Test
    public void given_ClusterAndOperatorRunning_when_KeycloakCRCreated_Then_KeycloakStructureIsDeployedAndStatusIsOK() throws IOException {
        Log.info(((operatorDeployment == OperatorDeployment.remote) ? "Remote " : "Local ") + "Run Test :" + namespace);

        deployAndCheckKeycloak();

        // Check Keycloak has status ready
        StringBuffer podlog = new StringBuffer();
        try {
            Log.info("Checking Keycloak pod has ready replicas == 1");
            Awaitility.await()
                    .atMost(Duration.ofSeconds(180))
                    .pollDelay(Duration.ofSeconds(5))
                    .untilAsserted(() -> {
                        podlog.delete(0, podlog.length());
                        try {
                            k8sclient.pods().inNamespace(namespace).list().getItems().stream()
                                    .filter(a -> a.getMetadata().getName().startsWith("example-kc"))
                                    .forEach(a -> podlog.append(a.getMetadata().getName()).append(" : ")
                                            .append(k8sclient.pods().inNamespace(namespace).withName(a.getMetadata().getName()).getLog(true)));
                        } catch (Exception e) {
                            // swallowing exception bc the pod is not ready to give logs yet
                        }
                        assertThat(k8sclient.apps().deployments().inNamespace(namespace).withName("example-kc").get().getStatus().getReadyReplicas()).isEqualTo(1);
                    });
        } catch (ConditionTimeoutException e) {
            Log.error("On error POD LOG " + podlog, e);
            throw e;
        }


    }

    private void deployAndCheckKeycloak() {
        // DB
        Log.info("Creating new PostgreSQL deployment");
        k8sclient.load(ClusteringE2EIT.class.getResourceAsStream("/example-postgres.yaml")).inNamespace(namespace).createOrReplace();

        // Check DB has deployed and ready
        Log.info("Checking Postgres is running");
        Awaitility.await()
                .atMost(Duration.ofSeconds(60))
                .pollDelay(Duration.ofSeconds(2))
                .untilAsserted(() -> assertThat(k8sclient.apps().statefulSets().inNamespace(namespace).withName("postgresql-db").get().getStatus().getReadyReplicas()).isEqualTo(1));
        // CR
        Log.info("Creating new Keycloak CR example");
        k8sclient.load(ClusteringE2EIT.class.getResourceAsStream("/example-keycloak.yml")).inNamespace(namespace).createOrReplace();

        // Check Operator has deployed Keycloak
        Log.info("Checking Operator has deployed Keycloak deployment");
        Awaitility.await()
                .atMost(Duration.ofSeconds(60))
                .pollDelay(Duration.ofSeconds(2))
                .untilAsserted(() -> assertThat(k8sclient.apps().deployments().inNamespace(namespace).withName("example-kc").get()).isNotNull());
    }

    @Test
    public void given_ClusterAndOperatorRunning_when_KeycloakCRScaled_Then_PodsAreReady() {
        // given
        deployAndCheckKeycloak();

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
        try {
            URI uri = new URI(k8sclient.getConfiguration().getMasterUrl().replace("8443", "31999") + "realms/master") ;
            given().when().relaxedHTTPSValidation().get(uri).then().statusCode(200);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // can we check infinispan ?
    }

}