package org.keycloak.operator;

import io.quarkus.logging.Log;
import io.quarkus.test.junit.QuarkusTest;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
public class OperatorE2EIT extends ClusterOperatorTest {
    @Test
    public void given_ClusterAndOperatorRunning_when_KeycloakCRCreated_Then_KeycloakStructureIsDeployedAndStatusIsOK() throws IOException {
        Log.info(((operatorDeployment == OperatorDeployment.remote) ? "Remote " : "Local ") + "Run Test :" + namespace);

        // DB
        Log.info("Creating new PostgreSQL deployment");
        k8sclient.load(OperatorE2EIT.class.getResourceAsStream("/example-postgres.yaml")).inNamespace(namespace).createOrReplace();

        // Check DB has deployed and ready
        Log.info("Checking Postgres is running");
        Awaitility.await()
                .atMost(Duration.ofSeconds(60))
                .pollDelay(Duration.ofSeconds(2))
                .untilAsserted(() -> assertThat(k8sclient.apps().statefulSets().inNamespace(namespace).withName("postgresql-db").get().getStatus().getReadyReplicas()).isEqualTo(1));
        // CR
        Log.info("Creating new Keycloak CR example");
        k8sclient.load(OperatorE2EIT.class.getResourceAsStream("/example-keycloak.yml")).inNamespace(namespace).createOrReplace();

        // Check Operator has deployed Keycloak
        Log.info("Checking Operator has deployed Keycloak deployment");
        Awaitility.await()
                .atMost(Duration.ofSeconds(60))
                .pollDelay(Duration.ofSeconds(2))
                .untilAsserted(() -> assertThat(k8sclient.apps().deployments().inNamespace(namespace).withName("example-kc").get()).isNotNull());

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

}
