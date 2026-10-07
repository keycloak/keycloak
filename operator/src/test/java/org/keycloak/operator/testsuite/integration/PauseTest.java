package org.keycloak.operator.testsuite.integration;

import java.util.concurrent.TimeUnit;

import org.keycloak.operator.Constants;
import org.keycloak.operator.crds.v2beta1.deployment.Keycloak;
import org.keycloak.operator.testsuite.utils.K8sUtils;

import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.quarkus.test.junit.QuarkusTest;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;

@QuarkusTest
public class PauseTest extends BaseOperatorTest {

    @Test
    void testPause() throws Exception {
        // In remote mode, the operator runs as a Deployment — wait for it to be ready.
        // In local/local_apiserver mode, it runs in-process and is already available.
        if (operatorDeployment == OperatorDeployment.remote) {
            Awaitility.await().atMost(2, TimeUnit.MINUTES).pollInterval(1, TimeUnit.SECONDS)
                    .untilAsserted(() -> assertThat(
                            k8sclient.apps().deployments().withName(KEYCLOAK_OPERATOR).get())
                            .isNotNull()
                            .extracting(d -> d.getStatus().getReadyReplicas())
                            .isEqualTo(1));
        }

        Keycloak kc = K8sUtils.getDefaultKeycloakDeployment();
        kc.getMetadata().getAnnotations().put(Constants.KEYCLOAK_PAUSE_ANNOTATION, "true");

        k8sclient.resource(kc).serverSideApply();

        k8sclient.resource(kc)
                .informOnCondition(l -> l.stream().allMatch(
                        k -> "true".equals(k.getMetadata().getAnnotations().get(Constants.KEYCLOAK_PAUSED_ANNOTATION))))
                .get(1, TimeUnit.MINUTES);

        assertNull(k8sclient.resources(StatefulSet.class).withName(kc.getMetadata().getName()).get());

        kc.getMetadata().getAnnotations().remove(Constants.KEYCLOAK_PAUSE_ANNOTATION);
        k8sclient.resource(kc).serverSideApply();

        k8sclient.resource(kc)
                .informOnCondition(l -> l.stream().allMatch(
                        k -> k.getMetadata().getAnnotations().get(Constants.KEYCLOAK_PAUSED_ANNOTATION) == null))
                .get(1, TimeUnit.MINUTES);

        Awaitility.await().atMost(1, TimeUnit.MINUTES).until(() -> k8sclient.resources(StatefulSet.class).withName(kc.getMetadata().getName()).get() != null);
    }

}
