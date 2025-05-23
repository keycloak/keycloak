/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.operator.testsuite.integration;

import io.fabric8.kubernetes.api.model.ServiceSpecBuilder;
import io.quarkus.logging.Log;
import io.quarkus.test.junit.QuarkusTest;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.keycloak.operator.controllers.KeycloakDiscoveryServiceDependentResource;
import org.keycloak.operator.controllers.KeycloakServiceDependentResource;
import org.keycloak.operator.testsuite.utils.K8sUtils;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@QuarkusTest
public class KeycloakServicesTest extends BaseOperatorTest {
    @Test
    public void testMainServiceDurability() {
        var kc = getTestKeycloakDeployment(true);
        kc.getSpec().getHttpSpec().setLabels(Map.of("foo","bar"));
        K8sUtils.deployKeycloak(k8sclient, kc, true);
        String serviceName = KeycloakServiceDependentResource.getServiceName(kc);
        var serviceSelector = k8sclient.services().inNamespace(namespace).withName(serviceName);

        Log.info("Trying to delete the service");
        assertThat(serviceSelector.delete()).isNotNull();
        Awaitility.await()
                .timeout(Duration.ofMinutes(1))
                .untilAsserted(() -> assertThat(serviceSelector.get()).isNotNull());

        K8sUtils.waitForKeycloakToBeReady(k8sclient, kc); // wait for reconciler to calm down to avoid race condition

        Log.info("Trying to modify the service");

        var currentService = serviceSelector.get();
        var labels = Map.of("address", "EvergreenTerrace742");
        // ignoring current IP/s
        currentService.getSpec().setClusterIP(null);
        currentService.getSpec().setClusterIPs(null);

        // an unmanaged change
        currentService.getSpec().setSessionAffinity("ClientIP");
        var origSpecs = new ServiceSpecBuilder(currentService.getSpec()).build(); // deep copy

        currentService.getMetadata().getLabels().putAll(labels);

        currentService.getMetadata().setResourceVersion(null);
        k8sclient.resource(currentService).update();

        Awaitility.await()
                .timeout(Duration.ofMinutes(1))
                .untilAsserted(() -> {
                    var s = serviceSelector.get();
                    assertThat(s.getMetadata().getLabels().entrySet().containsAll(labels.entrySet())).isTrue(); // additional labels should not be overwritten
                    assertEquals("bar", s.getMetadata().getLabels().get("foo"));
                    // ignoring assigned IP/s and generated config
                    s.getSpec().setClusterIP(null);
                    s.getSpec().setClusterIPs(null);
                    s.getSpec().setSessionAffinityConfig(null);
                    assertThat(s.getSpec()).isEqualTo(origSpecs); // managed spec fields should be reconciled back to original values
                });
    }

    @Test
    public void testCustomServiceAnnotations() {
        var kc = getTestKeycloakDeployment(true);

        // set 'a'
        kc.getSpec().getHttpSpec().setAnnotations(Map.of("a", "b"));
        K8sUtils.deployKeycloak(k8sclient, kc, true);

        String serviceName = KeycloakServiceDependentResource.getServiceName(kc);
        var serviceSelector = k8sclient.services().inNamespace(namespace).withName(serviceName);

        Awaitility.await()
                .ignoreExceptions()
                .untilAsserted(() -> {
                    var s = serviceSelector.get();
                    assertEquals("b", s.getMetadata().getAnnotations().get("a"));
                });

        // update 'a'
        kc.getSpec().getHttpSpec().setAnnotations(Map.of("a", "bb"));
        K8sUtils.deployKeycloak(k8sclient, kc, true);

        Awaitility.await()
                .ignoreExceptions()
                .untilAsserted(() -> {
                    var s = serviceSelector.get();
                    assertEquals("bb", s.getMetadata().getAnnotations().get("a"));
                });

        // remove 'a' and add 'c'
        kc.getSpec().getHttpSpec().setAnnotations(Map.of("c", "d"));
        K8sUtils.deployKeycloak(k8sclient, kc, true);

        Awaitility.await()
                .ignoreExceptions()
                .untilAsserted(() -> {
                    var s = serviceSelector.get();
                    assertFalse(s.getMetadata().getAnnotations().containsKey("a"));
                    assertEquals("d", s.getMetadata().getAnnotations().get("c"));
                });

        // remove all
        kc.getSpec().getHttpSpec().setAnnotations(null);
        K8sUtils.deployKeycloak(k8sclient, kc, true);

        Awaitility.await()
                .ignoreExceptions()
                .untilAsserted(() -> {
                    var s = serviceSelector.get();
                    assertFalse(s.getMetadata().getAnnotations().containsKey("a"));
                    assertFalse(s.getMetadata().getAnnotations().containsKey("c"));
                });
    }

    @Test
    public void testDiscoveryServiceDurability() {
        var kc = getTestKeycloakDeployment(true);
        K8sUtils.deployKeycloak(k8sclient, kc, true);
        var discoveryServiceSelector = k8sclient.services().inNamespace(namespace).withName(KeycloakDiscoveryServiceDependentResource.getName(kc));

        Log.info("Trying to delete the discovery service");
        assertThat(discoveryServiceSelector.delete()).isNotNull();
        Awaitility.await()
                .timeout(Duration.ofMinutes(1))
                .untilAsserted(() -> assertThat(discoveryServiceSelector.get()).isNotNull());

        K8sUtils.waitForKeycloakToBeReady(k8sclient, kc); // wait for reconciler to calm down to avoid race condititon

        Log.info("Trying to modify the service");

        var currentDiscoveryService = discoveryServiceSelector.get();
        // check publishNotReadyAddresses is set
        assertThat(currentDiscoveryService.getSpec().getPublishNotReadyAddresses()).isTrue();
        var labels = Map.of("address", "EvergreenTerrace742");
        // ignoring current IP/s
        currentDiscoveryService.getSpec().setClusterIP(null);
        currentDiscoveryService.getSpec().setClusterIPs(null);

        currentDiscoveryService.getMetadata().getLabels().putAll(labels);
        // an unmanaged change
        currentDiscoveryService.getSpec().setSessionAffinity("ClientIP");
        var origDiscoverySpecs = new ServiceSpecBuilder(currentDiscoveryService.getSpec()).build(); // deep copy

        // a managed change
        currentDiscoveryService.getSpec().getPorts().get(0).setName(null);

        currentDiscoveryService.getMetadata().setResourceVersion(null);
        k8sclient.resource(currentDiscoveryService).update();

        Awaitility.await()
                .timeout(Duration.ofMinutes(1))
                .untilAsserted(() -> {
                    var ds = discoveryServiceSelector.get();
                    assertThat(ds.getMetadata().getLabels().entrySet().containsAll(labels.entrySet())).isTrue(); // additional labels should not be overwritten
                    // ignoring assigned IP/s and generated config
                    ds.getSpec().setClusterIP(null);
                    ds.getSpec().setClusterIPs(null);
                    ds.getSpec().setSessionAffinityConfig(null);
                    assertThat(ds.getSpec()).isEqualTo(origDiscoverySpecs); // specs should be reconciled back to original values
                });
    }
}
