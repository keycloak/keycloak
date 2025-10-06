package org.keycloak.operator.testsuite.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.keycloak.operator.controllers.KeycloakServiceMonitorDependentResource;
import org.keycloak.operator.crds.v2alpha1.deployment.Keycloak;
import org.keycloak.operator.crds.v2alpha1.deployment.KeycloakStatusCondition;
import org.keycloak.operator.testsuite.utils.CRAssert;
import org.keycloak.operator.testsuite.utils.K8sUtils;

import io.fabric8.openshift.api.model.monitoring.v1.ServiceMonitor;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class ServiceMonitorUninstalledTest extends BaseOperatorTest {

    @BeforeAll
    public static void beforeAll() {
        k8sclient.apiextensions().v1().customResourceDefinitions().withName(new ServiceMonitor().getFullResourceName())
                .withTimeout(10, TimeUnit.SECONDS).delete();
    }

    @Test
    public void testServiceMonitorNoCRD() {
        Assumptions.assumeFalse(ServiceMonitorTest.isServiceMonitorAvailable(k8sclient));
        var kc = getTestKeycloakDeployment(true, false);
        K8sUtils.deployKeycloak(k8sclient, kc, true);

        ServiceMonitor sm = ServiceMonitorTest.getServiceMonitor(kc);
        assertThat(sm).isNull();

        CRAssert.assertKeycloakStatusCondition(
                k8sclient.resources(Keycloak.class).withName(kc.getMetadata().getName()).get(),
                KeycloakStatusCondition.HAS_ERRORS, false,
                KeycloakServiceMonitorDependentResource.WARN_CRD_NOT_INSTALLED);
    }

}
