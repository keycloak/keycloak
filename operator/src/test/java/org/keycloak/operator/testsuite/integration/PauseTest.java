package org.keycloak.operator.testsuite.integration;

import java.util.concurrent.TimeUnit;

import org.keycloak.operator.Constants;
import org.keycloak.operator.crds.v2beta1.deployment.Keycloak;
import org.keycloak.operator.testsuite.utils.K8sUtils;

import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.quarkus.test.junit.QuarkusTest;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;

@QuarkusTest
public class PauseTest extends BaseOperatorTest {

    @Test
    void testPause() throws Exception {
        Keycloak kc = K8sUtils.getDefaultKeycloakDeployment();
        kc.getMetadata().getAnnotations().put(Constants.KEYCLOAK_PAUSE_ANNOTATION, "true");

        k8sclient.resource(kc).serverSideApply();

        k8sclient.resource(kc)
                .informOnCondition(l -> l.stream().allMatch(
                        k -> "true".equals(k.getMetadata().getAnnotations().get(Constants.KEYCLOAK_PAUSED_ANNOTATION))))
                .get(10, TimeUnit.SECONDS);
        
        assertNull(k8sclient.resources(StatefulSet.class).withName(kc.getMetadata().getName()).get());
        
        kc.getMetadata().getAnnotations().remove(Constants.KEYCLOAK_PAUSE_ANNOTATION);
        k8sclient.resource(kc).serverSideApply();
        
        k8sclient.resource(kc)
                .informOnCondition(l -> l.stream().allMatch(
                        k -> k.getMetadata().getAnnotations().get(Constants.KEYCLOAK_PAUSED_ANNOTATION) == null))
                .get(10, TimeUnit.SECONDS);
        
        Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> k8sclient.resources(StatefulSet.class).withName(kc.getMetadata().getName()).get() != null);
    }

}
