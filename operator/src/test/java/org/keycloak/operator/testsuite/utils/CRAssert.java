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

package org.keycloak.operator.testsuite.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.assertj.core.api.ObjectAssert;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Assertions;
import org.keycloak.operator.Constants;
import org.keycloak.operator.Utils;
import org.keycloak.operator.controllers.KeycloakController;
import org.keycloak.operator.controllers.KeycloakDeploymentDependentResource;
import org.keycloak.operator.controllers.KeycloakServiceDependentResource;
import org.keycloak.operator.crds.v2alpha1.deployment.Keycloak;
import org.keycloak.operator.crds.v2alpha1.deployment.KeycloakStatus;
import org.keycloak.operator.crds.v2alpha1.deployment.KeycloakStatusCondition;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.NetworkPolicySpec;
import org.keycloak.operator.crds.v2alpha1.realmimport.KeycloakRealmImport;
import org.keycloak.operator.update.impl.RecreateOnImageChangeUpdateLogic;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicy;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicyIngressRule;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicyPeer;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicyPort;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.netty.util.NetUtil;
import io.quarkus.logging.Log;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public final class CRAssert {

    // ISPN000093 -> cluster view
    // ISPN000094 -> merge view
    private static final Pattern CLUSTER_SIZE_PATTERN = Pattern.compile("ISPN00009[34]: [^]]*] \\((\\d+)\\)");

    public static void assertKeycloakStatusCondition(Keycloak kc, String condition, Boolean status) {
        assertKeycloakStatusCondition(kc, condition, status, null);
    }
    public static ObjectAssert<KeycloakStatusCondition> assertKeycloakStatusCondition(Keycloak kc, String condition, Boolean status, String containedMessage) {
        Log.debugf("Asserting CR: %s, condition: %s, status: %s, message: %s", kc.getMetadata().getName(), condition, status, containedMessage);
        try {
            return assertKeycloakStatusCondition(kc.getStatus(), condition, status, containedMessage, null);
        } catch (Exception e) {
            Log.infof("Asserting CR: %s with status:\n%s", kc.getMetadata().getName(), Serialization.asYaml(kc.getStatus()));
            throw e;
        }
    }

    public static void assertKeycloakStatusCondition(KeycloakStatus kcStatus, String condition, Boolean status, String containedMessage) {
        assertKeycloakStatusCondition(kcStatus, condition, status, containedMessage, null);
    }

    public static ObjectAssert<KeycloakStatusCondition> assertKeycloakStatusCondition(KeycloakStatus kcStatus, String condition, Boolean status, String containedMessage, Long observedGeneration) {
        KeycloakStatusCondition statusCondition = kcStatus.findCondition(condition).orElseThrow();
        assertThat(statusCondition.getStatus())
                .withFailMessage(() -> "found status " + statusCondition + " and expected status " + status)
                .isEqualTo(status);
        if (containedMessage != null) {
            assertThat(statusCondition.getMessage())
                    .withFailMessage(() -> "found status " + statusCondition + " and expected it to contain " + containedMessage)
                    .contains(containedMessage);
        }
        if (observedGeneration != null) {
            assertThat(statusCondition.getObservedGeneration())
                    .withFailMessage(() -> "found status " + statusCondition + " and expected it to contain an observed generation of " + observedGeneration)
                    .isEqualTo(observedGeneration);
        }
        if (status != null) {
            assertThat(statusCondition.getLastTransitionTime())
                    .withFailMessage(() -> "found status " + statusCondition + " and expected the last transition time to not be null")
                    .isNotNull();
        }
        return assertThat(statusCondition);
    }

    public static void assertKeycloakStatusDoesNotContainMessage(KeycloakStatus kcStatus, String message) {
        assertThat(kcStatus.getConditions())
                .noneMatch(c -> c.getMessage().contains(message));
    }

    public static void assertKeycloakRealmImportStatusCondition(KeycloakRealmImport kri, String condition, Boolean status) {
        assertThat(kri.getStatus().getConditions())
                .anyMatch(c -> c.getType().equals(condition) && Objects.equals(c.getStatus(), status));
    }

    public static void awaitClusterSize(KubernetesClient client, Keycloak keycloak, int expectedSize) {
        Log.infof("Waiting for cluster size of %s", expectedSize);
        Awaitility
                .await()
                .pollInterval(1, TimeUnit.SECONDS)
                .timeout(Duration.ofMinutes(5))
                .ignoreExceptions()
                .untilAsserted(() -> client.pods()
                        .inNamespace(namespaceOf(keycloak))
                        .withLabels(Utils.allInstanceLabels(keycloak))
                        .resources()
                        .forEach(pod -> {
                            var logs = pod.getLog();
                            var matcher = CLUSTER_SIZE_PATTERN.matcher(logs);
                            int size = 0;
                            // We want the last view change.
                            // The other alternative is to reverse the string.
                            while (matcher.find()) {
                                size = Integer.parseInt(matcher.group(1));
                            }
                            Assertions.assertEquals(expectedSize, size, "Wrong cluster size in pod " + pod);
                        }));
    }

    public static void assertKeycloakAccessibleViaService(KubernetesClient client, Keycloak keycloak, boolean https, int port) {
        assertKeycloakAccessibleViaService(client, keycloak, null, Map.of(), https, port);
    }

    public static void assertKeycloakAccessibleViaService(KubernetesClient client, Keycloak keycloak, String podNamespace, Map<String, String> labels, boolean https, int port) {
        var protocol = https ? "https" : "http";
        assertServiceAccessible(client, keycloak, podNamespace, labels, protocol, protocol, port, "/admin/master/console/");
    }

    public static void assertManagementInterfaceAccessibleViaService(KubernetesClient client, Keycloak kc, boolean https) {
        assertManagementInterfaceAccessibleViaService(client, kc, https, Constants.KEYCLOAK_MANAGEMENT_PORT);
    }

    public static void assertManagementInterfaceAccessibleViaService(KubernetesClient client, Keycloak keycloak, boolean https, int port) {
        assertManagementInterfaceAccessibleViaService(client, keycloak, null, Map.of(), https, port);
    }

    public static void assertManagementInterfaceAccessibleViaService(KubernetesClient client, Keycloak keycloak, String podNamespace, Map<String, String> labels, boolean https, int port) {
        assertServiceAccessible(client, keycloak, podNamespace, labels, https ? "https" : "http", Constants.KEYCLOAK_MANAGEMENT_PORT_NAME, port, null);
    }

    private static void assertServiceAccessible(KubernetesClient client, Keycloak keycloak, String podNamespace, Map<String, String> labels, String protocol, String portName, int port, String path) {
        Awaitility.await()
                .timeout(30, TimeUnit.SECONDS)
                .ignoreExceptions()
                .untilAsserted(() -> {
                    var serviceName = KeycloakServiceDependentResource.getServiceName(keycloak);
                    var namespace = namespaceOf(keycloak);
                    assertThat(client.resources(Service.class).withName(serviceName).require().getSpec().getPorts()
                            .stream().map(ServicePort::getName).anyMatch(portName::equals)).isTrue();

                    var url = protocol + "://" + serviceName + "." + namespace + ":" + port;
                    if (path != null) {
                        url += path;
                    }
                    Log.info("Checking url: " + url);

                    var curlOutput = K8sUtils.inClusterCurl(client, podNamespace == null ? namespace : podNamespace, labels == null ? Map.of() : labels, url);
                    Log.info("Curl Output: " + curlOutput);

                    assertEquals("200", curlOutput);
                });
    }

    public static void assertKeycloakServiceBlocked(KubernetesClient client, Keycloak keycloak, String podNamespace, Map<String, String> labels, int port) {
        var serviceName = KeycloakServiceDependentResource.getServiceName(keycloak);
        var namespace = namespaceOf(keycloak);
        assertConnection(client, "%s.%s".formatted(serviceName, namespace), port, podNamespace, labels, false);
    }

    public static void assertJGroupsConnection(KubernetesClient client, String podIp, String namespace, Map<String, String> labels, boolean connects) {
        // Send a bogus command to JGroups port
        assertConnection(client, podIp ,7800, namespace, labels, connects);
    }

    public static void assertConnection(KubernetesClient client, String hostname, int port, String namespace, Map<String, String> labels, boolean connects) {
        // Send a bogus command to the port
        var result = K8sUtils.inClusterCurl(client, namespace, labels, "--telnet-option",
                "'BOGUS=1'",
                "--connect-timeout",
                "2",
                "-s",
                "telnet://%s:%s".formatted(NetUtil.isValidIpV6Address(hostname)?"["+hostname+"]":hostname, port));
        // Relevant exit codes:
        // 28-Operation timeout.
        // 48-Unknown option specified to libcurl (BOGUS=1 is not a valid option, but the connection is successful).
        assertEquals(connects ? 48 : 28, result.exitCode());
    }

    private static String namespaceOf(Keycloak keycloak) {
        return keycloak.getMetadata().getNamespace();
    }

    public static void assertIngressRules(NetworkPolicy networkPolicy, Keycloak keycloak, int httpPort, int httpsPort, int mgntPort) {
        Log.info(networkPolicy);
        var expectedNumberOfRules = IntStream.of(httpPort, httpsPort, mgntPort)
                .filter(value -> value > 0)
                .count();

        // +1 for JGRP
        ++expectedNumberOfRules;

        long numberOfRules = Optional.ofNullable(networkPolicy.getSpec())
                .map(io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicySpec::getIngress)
                .map(List::size)
                .orElse(0);

        assertEquals(expectedNumberOfRules, numberOfRules);

        // Check selector
        assertPodSelectorAndPolicy(keycloak, networkPolicy);

        // JGroups is always present
        assertJGroupsRulePresent(keycloak, networkPolicy);

        if (httpPort > 0) {
            assertKeycloakEndpointRulePresent("HTTP", networkPolicy, NetworkPolicySpec.httpRules(keycloak).orElse(null), httpPort);
        }
        if (httpsPort > 0) {
            assertKeycloakEndpointRulePresent("HTTPS", networkPolicy, NetworkPolicySpec.httpsRules(keycloak).orElse(null), httpsPort);
        }
        if (mgntPort > 0) {
            assertKeycloakEndpointRulePresent("Management", networkPolicy, NetworkPolicySpec.managementRules(keycloak).orElse(null), mgntPort);
        }
    }

    private static void assertPodSelectorAndPolicy(Keycloak keycloak, NetworkPolicy networkPolicy) {
        assertNotNull(networkPolicy, "Expects a network policy");
        assertEquals(Utils.allInstanceLabels(keycloak), networkPolicy.getSpec().getPodSelector().getMatchLabels(), "Expects same pod match labels");
        assertTrue(networkPolicy.getSpec().getPolicyTypes().contains("Ingress"), "Expect ingress polity type present");
    }

    private static void assertKeycloakEndpointRulePresent(String name, NetworkPolicy networkPolicy, List<NetworkPolicyPeer> from, int mgmtPort) {
        var rule = findIngressRuleWithPort(networkPolicy, mgmtPort);
        assertTrue(rule.isPresent(), name + " Ingress Rule is missing");
        if (from == null || from.isEmpty()) {
            assertTrue(rule.get().getFrom().isEmpty());
        } else {
            assertEquals(from, rule.get().getFrom());
        }
        var ports = portAndProtocol(rule.get());
        assertEquals(Map.of(mgmtPort, Constants.KEYCLOAK_SERVICE_PROTOCOL), ports);
    }

    private static void assertJGroupsRulePresent(Keycloak keycloak, NetworkPolicy networkPolicy) {
        var rule = findIngressRuleWithPort(networkPolicy, Constants.KEYCLOAK_JGROUPS_DATA_PORT);
        assertTrue(rule.isPresent(), "JGroups Ingress Rule is missing");

        var from = rule.get().getFrom();
        assertEquals(1, from.size(), "Incorrect 'from' list size");
        assertEquals(Utils.allInstanceLabels(keycloak), from.get(0).getPodSelector().getMatchLabels());

        var ports = portAndProtocol(rule.get());
        assertEquals(Map.of(
                Constants.KEYCLOAK_JGROUPS_DATA_PORT, Constants.KEYCLOAK_JGROUPS_PROTOCOL,
                Constants.KEYCLOAK_JGROUPS_FD_PORT, Constants.KEYCLOAK_JGROUPS_PROTOCOL
        ), ports);
    }

    private static Map<Integer, String> portAndProtocol(NetworkPolicyIngressRule rule) {
        return rule.getPorts().stream()
                .collect(Collectors.toMap(port -> port.getPort().getIntVal(), NetworkPolicyPort::getProtocol));
    }

    private static Optional<NetworkPolicyIngressRule> findIngressRuleWithPort(NetworkPolicy networkPolicy, int rulePort) {
        return networkPolicy.getSpec().getIngress().stream()
                .filter(rule -> rule.getPorts().stream().anyMatch(port -> port.getPort().getIntVal() == rulePort))
                .findFirst();
    }

    public static CompletableFuture<Void> eventuallyRollingUpdateStatus(KubernetesClient client, Keycloak keycloak, String reason) {
        // test the statefulset, rather that the keycloak status as the events with the local api server may happen too quickly and the keycloak status may not get updated
        var cf1 = client.apps().statefulSets().withName(keycloak.getMetadata().getName()).informOnCondition(ss -> {
            return !ss.isEmpty() && KeycloakController.isRolling(ss.get(0));
        });
        var cf2 = client.resource(keycloak).informOnCondition(kcs -> {
            try {
                assertKeycloakStatusCondition(kcs.get(0), KeycloakStatusCondition.UPDATE_TYPE, false, reason);
                return true;
            } catch (AssertionError e) {
                return false;
            }
        });
        return CompletableFuture.allOf(cf1, cf2);
    }

    public static CompletableFuture<Void> eventuallyRecreateUpdateStatus(KubernetesClient client, Keycloak keycloak, String reason) {
        var statefulSetResource = client.apps().statefulSets().withName(KeycloakDeploymentDependentResource.getName(keycloak));
        String oldImage = RecreateOnImageChangeUpdateLogic.extractImage(statefulSetResource.get()); // assumes a keycloak has already been deployed
        var cf1 = statefulSetResource.informOnCondition(statefulSets -> {
            try {
                StatefulSet ss = statefulSets.get(0);
                // should scale down using the old image
                assertTrue(ss.getSpec().getReplicas() == 0 && Objects.equals(RecreateOnImageChangeUpdateLogic.extractImage(ss), oldImage));
                return true;
            } catch (Error e) {
                return false;
            }
        });

        List<Consumer<Keycloak>> tests = List.of(
                kc -> CRAssert.assertKeycloakStatusCondition(kc, KeycloakStatusCondition.READY, false, null),
                kc -> assertKeycloakStatusCondition(kc, KeycloakStatusCondition.UPDATE_TYPE, true, reason));
        CompletableFuture<Void> cf2 = new CompletableFuture<Void>();
        AtomicInteger index = new AtomicInteger();

        client.resource(keycloak).inform(new ResourceEventHandler<Keycloak>() {
            @Override
            public void onUpdate(Keycloak oldObj, Keycloak newObj) {
                while (index.get() < tests.size()) {
                    try {
                        tests.get(index.get()).accept(newObj);
                        index.getAndIncrement();
                    } catch (Exception e) {

                    }
                }
                cf2.complete(null);
            }

            @Override
            public void onAdd(Keycloak obj) {
            }

            @Override
            public void onDelete(Keycloak obj, boolean deletedFinalStateUnknown) {
            }
        });
        return CompletableFuture.allOf(cf1, cf2);
    }
}
