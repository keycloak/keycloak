package org.keycloak.operator.testsuite.integration;

import java.time.Duration;

import org.keycloak.operator.Constants;
import org.keycloak.operator.testsuite.utils.K8sUtils;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.quarkus.test.junit.QuarkusTest;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
public class CRDUpgradeTest extends BaseOperatorTest {

    @Test
    public void testKeycloakUpgrade() {
        var kc = getTestKeycloakDeployment(true);
        kc.getMetadata().getAnnotations().put(Constants.KEYCLOAK_PAUSE_ANNOTATION, "true");
        var deployedKc = k8sclient.resource(kc).create();

        Service service = Serialization.unmarshal(this.getClass().getResourceAsStream("/v2alpha1-service.yaml"), Service.class);
        service.getMetadata().setNamespace(deployedKc.getMetadata().getNamespace());
        service.addOwnerReference(deployedKc).setApiVersion("k8s.keycloak.org/v2alpha1");
        var serviceSelector = k8sclient.services().resource(service);
        serviceSelector.create();

        // now that the v2alpha1 service exists, try to reconcile - this should not fail and the owner
        // reference should be updated
        kc.getMetadata().getAnnotations().remove(Constants.KEYCLOAK_PAUSE_ANNOTATION);
        K8sUtils.deployKeycloak(k8sclient, kc, true);
        
        Awaitility.await().timeout(Duration.ofMinutes(1))
                .untilAsserted(() -> assertTrue(serviceSelector.get().getMetadata().getOwnerReferences().stream().allMatch(or -> or.getApiVersion().equals("k8s.keycloak.org/v2beta1"))));
    }
}
