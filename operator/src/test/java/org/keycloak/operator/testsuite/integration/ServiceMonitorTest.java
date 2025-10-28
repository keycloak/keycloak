package org.keycloak.operator.testsuite.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.keycloak.operator.controllers.KeycloakServiceMonitorDependentResource;
import org.keycloak.operator.crds.v2alpha1.deployment.Keycloak;
import org.keycloak.operator.crds.v2alpha1.deployment.KeycloakStatusCondition;
import org.keycloak.operator.crds.v2alpha1.deployment.ValueOrSecret;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.ServiceMonitorSpecBuilder;
import org.keycloak.operator.testsuite.utils.CRAssert;
import org.keycloak.operator.testsuite.utils.K8sUtils;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.openshift.api.model.monitoring.v1.ServiceMonitor;
import io.quarkus.test.junit.QuarkusTest;

@Tag(BaseOperatorTest.SLOW)
@QuarkusTest
public class ServiceMonitorTest extends BaseOperatorTest {

    @Test
    public void testServiceMonitorDisabledNoMetrics() {
        Assumptions.assumeTrue(isServiceMonitorAvailable(k8sclient));
        var kc = getTestKeycloakDeployment(true, false);
        kc.getSpec().setAdditionalOptions(List.of(new ValueOrSecret("metrics-enabled", "false")));
        K8sUtils.deployKeycloak(k8sclient, kc, true);

        ServiceMonitor sm = getServiceMonitor(kc);
        assertThat(sm).isNull();

        CRAssert.assertKeycloakStatusCondition(
                k8sclient.resources(Keycloak.class).withName(kc.getMetadata().getName()).get(),
                KeycloakStatusCondition.HAS_ERRORS, false,
                KeycloakServiceMonitorDependentResource.WARN_METRICS_NOT_ENABLED);
    }

    @Test
    public void testServiceMonitorCreatedWithMetricsEnabled() {
        Assumptions.assumeTrue(isServiceMonitorAvailable(k8sclient));
        var kc = getTestKeycloakDeployment(true, false);
        K8sUtils.deployKeycloak(k8sclient, kc, true);

        Awaitility.await().untilAsserted(() -> {
            var sm = getServiceMonitor(kc);
            assertThat(sm).isNotNull();
            assertThat(sm.getSpec().getEndpoints()).hasSize(1);
        });

        // make sure the desired state is maintained
        var sm = getServiceMonitor(kc);
        sm.getMetadata().setResourceVersion(null);
        sm.getSpec().setScrapeProtocols(List.of("PrometheusText0.0.4"));
        assertThat(k8sclient.resource(sm).patch().getSpec().getScrapeProtocols())
                .doesNotContain(KeycloakServiceMonitorDependentResource.OPEN_METRICS_PROTOCOL);

        Awaitility.await().untilAsserted(() -> {
            assertThat(getServiceMonitor(kc).getSpec().getScrapeProtocols())
                    .contains(KeycloakServiceMonitorDependentResource.OPEN_METRICS_PROTOCOL);
        });
    }

    @Test
    public void testServiceMonitorDisabledExplicitly() {
        Assumptions.assumeTrue(isServiceMonitorAvailable(k8sclient));
        var kc = getTestKeycloakDeployment(true, false);
        kc.getSpec().setServiceMonitorSpec(
              new ServiceMonitorSpecBuilder()
                    .withEnabled(false)
                    .build()
        );
        K8sUtils.deployKeycloak(k8sclient, kc, true);

        ServiceMonitor sm = getServiceMonitor(kc);
        assertThat(sm).isNull();
    }

    @Test
    public void testServiceMonitorLegacyManagement() {
        Assumptions.assumeTrue(isServiceMonitorAvailable(k8sclient));
        var kc = getTestKeycloakDeployment(true, false);
        kc.getSpec().getAdditionalOptions().add(new ValueOrSecret("legacy-observability-interface", "true"));
        K8sUtils.deployKeycloak(k8sclient, kc, true);

        Awaitility.await().untilAsserted(() -> {
            var sm = getServiceMonitor(kc);
            assertThat(sm).isNotNull();
            assertThat(sm.getSpec().getEndpoints()).hasSize(1);
            assertThat(sm.getSpec().getEndpoints().get(0).getPort()).isEqualTo("https");
        });
    }

    @Test
    public void testServiceMonitorConfigProperties() {
        Assumptions.assumeTrue(isServiceMonitorAvailable(k8sclient));
        var kc = getTestKeycloakDeployment(true, false);
        kc.getSpec().setServiceMonitorSpec(
              new ServiceMonitorSpecBuilder()
                    .withInterval("1s")
                    .withScrapeTimeout("2s")
                    .build()
        );
        K8sUtils.deployKeycloak(k8sclient, kc, true);

        Awaitility.await().untilAsserted(() -> {
            var sm = getServiceMonitor(kc);
            assertThat(sm).isNotNull();
            assertThat(sm.getSpec().getEndpoints()).hasSize(1);
            assertThat(sm.getSpec().getEndpoints().get(0).getInterval()).isEqualTo("1s");
            assertThat(sm.getSpec().getEndpoints().get(0).getScrapeTimeout()).isEqualTo("2s");
        });
    }

    static ServiceMonitor getServiceMonitor(Keycloak kc) {
        return k8sclient.resources(ServiceMonitor.class)
              .inNamespace(kc.getMetadata().getNamespace())
              .withName(kc.getMetadata().getName())
              .get();
    }

    static boolean isServiceMonitorAvailable(KubernetesClient client) {
        return client
              .apiextensions()
              .v1()
              .customResourceDefinitions()
              .withName(new ServiceMonitor().getFullResourceName())
              .get() != null;
    }
}
