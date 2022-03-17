package org.keycloak.operator;

import io.fabric8.kubernetes.api.model.ServiceSpecBuilder;
import io.quarkus.logging.Log;
import io.quarkus.test.junit.QuarkusTest;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.keycloak.operator.v2alpha1.KeycloakDiscoveryService;
import org.keycloak.operator.v2alpha1.KeycloakService;
import org.keycloak.operator.utils.K8sUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
public class KeycloakServicesE2EIT extends ClusterOperatorTest {
    @Test
    public void testMainServiceDurability() {
        var kc = K8sUtils.getDefaultKeycloakDeployment();
        K8sUtils.deployKeycloak(k8sclient, kc, true);
        var service = new KeycloakService(k8sclient, kc);
        var serviceSelector = k8sclient.services().inNamespace(namespace).withName(service.getName());

        Log.info("Trying to delete the service");
        assertThat(serviceSelector.delete()).isTrue();
        Awaitility.await()
                .untilAsserted(() -> assertThat(serviceSelector.get()).isNotNull());

        K8sUtils.waitForKeycloakToBeReady(k8sclient, kc); // wait for reconciler to calm down to avoid race condititon

        Log.info("Trying to modify the service");

        var currentService = serviceSelector.get();
        var labels = Map.of("address", "EvergreenTerrace742");
        // ignoring current IP/s
        currentService.getSpec().setClusterIP(null);
        currentService.getSpec().setClusterIPs(null);
        var origSpecs = new ServiceSpecBuilder(currentService.getSpec()).build(); // deep copy

        currentService.getMetadata().getLabels().putAll(labels);
        currentService.getSpec().setSessionAffinity("ClientIP");

        serviceSelector.createOrReplace(currentService);

        Awaitility.await()
                .untilAsserted(() -> {
                    var s = serviceSelector.get();
                    assertThat(s.getMetadata().getLabels().entrySet().containsAll(labels.entrySet())).isTrue(); // additional labels should not be overwritten
                    // ignoring assigned IP/s
                    s.getSpec().setClusterIP(null);
                    s.getSpec().setClusterIPs(null);
                    assertThat(s.getSpec()).isEqualTo(origSpecs); // specs should be reconciled back to original values
                });
    }

    @Test
    public void testDiscoveryServiceDurability() {
        var kc = K8sUtils.getDefaultKeycloakDeployment();
        K8sUtils.deployKeycloak(k8sclient, kc, true);
        var discoveryService = new KeycloakDiscoveryService(k8sclient, kc);
        var discoveryServiceSelector = k8sclient.services().inNamespace(namespace).withName(discoveryService.getName());

        Log.info("Trying to delete the discovery service");
        assertThat(discoveryServiceSelector.delete()).isTrue();
        Awaitility.await()
                .untilAsserted(() -> assertThat(discoveryServiceSelector.get()).isNotNull());

        K8sUtils.waitForKeycloakToBeReady(k8sclient, kc); // wait for reconciler to calm down to avoid race condititon

        Log.info("Trying to modify the service");

        var currentDiscoveryService = discoveryServiceSelector.get();
        var labels = Map.of("address", "EvergreenTerrace742");
        // ignoring current IP/s
        currentDiscoveryService.getSpec().setClusterIP(null);
        currentDiscoveryService.getSpec().setClusterIPs(null);
        var origDiscoverySpecs = new ServiceSpecBuilder(currentDiscoveryService.getSpec()).build(); // deep copy

        currentDiscoveryService.getMetadata().getLabels().putAll(labels);
        currentDiscoveryService.getSpec().setSessionAffinity("ClientIP");

        discoveryServiceSelector.createOrReplace(currentDiscoveryService);

        Awaitility.await()
                .untilAsserted(() -> {
                    var ds = discoveryServiceSelector.get();
                    assertThat(ds.getMetadata().getLabels().entrySet().containsAll(labels.entrySet())).isTrue(); // additional labels should not be overwritten
                    // ignoring assigned IP/s
                    ds.getSpec().setClusterIP(null);
                    ds.getSpec().setClusterIPs(null);
                    assertThat(ds.getSpec()).isEqualTo(origDiscoverySpecs); // specs should be reconciled back to original values
                });
    }
}
