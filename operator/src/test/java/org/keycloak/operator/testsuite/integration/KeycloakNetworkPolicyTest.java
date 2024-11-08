/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicy;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicyIngressRule;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicyPort;
import io.quarkus.logging.Log;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.keycloak.operator.Constants;
import org.keycloak.operator.Utils;
import org.keycloak.operator.controllers.KeycloakController;
import org.keycloak.operator.crds.v2alpha1.deployment.Keycloak;
import org.keycloak.operator.crds.v2alpha1.deployment.ValueOrSecret;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.HostnameSpecBuilder;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.HttpManagementSpecBuilder;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.IngressSpecBuilder;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.NetworkPolicySpec;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.NetworkPolicySpecBuilder;
import org.keycloak.operator.testsuite.utils.CRAssert;
import org.keycloak.operator.testsuite.utils.K8sUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
public class KeycloakNetworkPolicyTest extends BaseOperatorTest {

    private static NetworkPolicy networkPolicy(Keycloak keycloak) {
        return k8sclient.network().networkPolicies()
                .inNamespace(namespaceOf(keycloak))
                .withName(NetworkPolicySpec.networkPolicyName(keycloak))
                .get();
    }

    @Test
    public void testDefaults() {
        var kc = create();
        K8sUtils.deployKeycloak(k8sclient, kc, true);
        CRAssert.awaitClusterSize(k8sclient, kc, 2);
        assertNull(networkPolicy(kc), "Expects no network policies deployed");
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void testHttpOnly(boolean randomPort) {
        var kc = create();
        enableNetworkPolicy(kc);
        disableHttps(kc);
        var httpPort = enableHttp(kc, randomPort);
        var mngtPort = configureManagement(kc, randomPort);
        K8sUtils.deployKeycloak(k8sclient, kc, true);
        CRAssert.awaitClusterSize(k8sclient, kc, 2);

        assertIngressRules(kc, httpPort, -1, mngtPort);

        CRAssert.assertKeycloakAccessibleViaService(k8sclient, kc, false, httpPort);
        CRAssert.assertManagementInterfaceAccessibleViaService(k8sclient, kc, false, mngtPort);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void testHttpsOnly(boolean randomPort) {
        var kc = create();
        enableNetworkPolicy(kc);
        var httpsPort = configureHttps(kc, randomPort);
        var mngtPort = configureManagement(kc, randomPort);

        K8sUtils.deployKeycloak(k8sclient, kc, true);
        CRAssert.awaitClusterSize(k8sclient, kc, 2);

        assertIngressRules(kc, -1, httpsPort, mngtPort);

        CRAssert.assertKeycloakAccessibleViaService(k8sclient, kc, true, httpsPort);
        CRAssert.assertManagementInterfaceAccessibleViaService(k8sclient, kc, true, mngtPort);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void testHttpAndHttps(boolean randomPort) {
        var kc = create();
        enableNetworkPolicy(kc);
        var httpPort = enableHttp(kc, randomPort);
        var httpsPort = configureHttps(kc, randomPort);
        var mngtPort = configureManagement(kc, randomPort);

        K8sUtils.deployKeycloak(k8sclient, kc, true);
        CRAssert.awaitClusterSize(k8sclient, kc, 2);

        assertIngressRules(kc, httpPort, httpsPort, mngtPort);

        CRAssert.assertKeycloakAccessibleViaService(k8sclient, kc, false, httpPort);
        CRAssert.assertKeycloakAccessibleViaService(k8sclient, kc, true, httpsPort);
        CRAssert.assertManagementInterfaceAccessibleViaService(k8sclient, kc, true, mngtPort);
    }

    @ParameterizedTest()
    @ValueSource(booleans = {true, false})
    public void testManagementDisabled(boolean legacyOption) {
        var kc = create();
        disableProbes(kc);
        enableNetworkPolicy(kc);
        disableManagement(kc, legacyOption);

        K8sUtils.deployKeycloak(k8sclient, kc, true);
        CRAssert.awaitClusterSize(k8sclient, kc, 2);

        assertIngressRules(kc, -1, Constants.KEYCLOAK_HTTPS_PORT, -1);
        CRAssert.assertKeycloakAccessibleViaService(k8sclient, kc, true, Constants.KEYCLOAK_HTTPS_PORT);
    }

    @Test
    public void testJGroupsConnectivity() {
        var kc = create();
        enableNetworkPolicy(kc);

        K8sUtils.deployKeycloak(k8sclient, kc, true);
        CRAssert.awaitClusterSize(k8sclient, kc, 2);
        assertIngressRules(kc, -1, Constants.KEYCLOAK_HTTPS_PORT, Constants.KEYCLOAK_MANAGEMENT_PORT);

        var namespace = namespaceOf(kc);
        var podIp = k8sclient.pods().inNamespace(namespace).list().getItems().get(0).getStatus().getPodIP();

        // pod in the same namespace, labels match: able to connect.
        CRAssert.assertJGroupsConnection(k8sclient, podIp, namespace, Utils.allInstanceLabels(kc), true);

        // pod in the same namespace, labels do not match: fail to connect.
        CRAssert.assertJGroupsConnection(k8sclient, podIp, namespace, Map.of(), false);

        var otherNamespace = getNewRandomNamespaceName();
        try {
            k8sclient.resource(new NamespaceBuilder().withNewMetadata().withName(otherNamespace).endMetadata().build()).create();
            // pod in a different namespace: fail to connect
            CRAssert.assertJGroupsConnection(k8sclient, podIp, otherNamespace, Utils.allInstanceLabels(kc), false);
            CRAssert.assertJGroupsConnection(k8sclient, podIp, otherNamespace, Map.of(), false);
        } finally {
            k8sclient.namespaces().withName(otherNamespace).delete();
        }
    }

    @Test
    public void testUpdate() {
        var kc = create();
        enableNetworkPolicy(kc);

        K8sUtils.deployKeycloak(k8sclient, kc, true);
        CRAssert.awaitClusterSize(k8sclient, kc, 2);
        assertIngressRules(kc, -1, Constants.KEYCLOAK_HTTPS_PORT, Constants.KEYCLOAK_MANAGEMENT_PORT);

        // disable should remove the network policy
        kc.getSpec().getNetworkPolicySpec().setNetworkPolicyEnabled(false);
        K8sUtils.deployKeycloak(k8sclient, kc, true);
        CRAssert.awaitClusterSize(k8sclient, kc, 2);
        assertNull(networkPolicy(kc), "Expects no network policies deployed");

        // disable should remove the network policy
        kc.getSpec().getNetworkPolicySpec().setNetworkPolicyEnabled(true);
        K8sUtils.deployKeycloak(k8sclient, kc, true);
        CRAssert.awaitClusterSize(k8sclient, kc, 2);
        assertIngressRules(kc, -1, Constants.KEYCLOAK_HTTPS_PORT, Constants.KEYCLOAK_MANAGEMENT_PORT);
    }

    private static void assertPodSelectorAndPolicy(Keycloak keycloak, NetworkPolicy networkPolicy) {
        assertNotNull(networkPolicy, "Expects a network policy");
        assertEquals(Utils.allInstanceLabels(keycloak), networkPolicy.getSpec().getPodSelector().getMatchLabels(), "Expects same pod match labels");
        assertTrue(networkPolicy.getSpec().getPolicyTypes().contains("Ingress"), "Expect ingress polity type present");
    }

    private static void assertManagementRulePresent(NetworkPolicy networkPolicy, int mgmtPort) {
        var rule = findIngressRuleWithPort(networkPolicy, mgmtPort);
        assertTrue(rule.isPresent(), "Management Ingress Rule is missing");
        assertTrue(rule.get().getFrom().isEmpty());
        var ports = portAndProtocol(rule.get());
        assertEquals(Map.of(mgmtPort, Constants.KEYCLOAK_SERVICE_PROTOCOL), ports);
    }

    private static void assertApplicationRulePresent(NetworkPolicy networkPolicy, int applicationPort) {
        var rule = findIngressRuleWithPort(networkPolicy, applicationPort);
        assertTrue(rule.isPresent(), "Application Ingress Rule is missing");
        assertTrue(rule.get().getFrom().isEmpty());
        var ports = portAndProtocol(rule.get());
        assertEquals(Map.of(applicationPort, Constants.KEYCLOAK_SERVICE_PROTOCOL), ports);
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

    private static void assertIngressRules(Keycloak keycloak, int httpPort, int httpsPort, int mgntPort) {
        var networkPolicy = networkPolicy(keycloak);
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
            assertApplicationRulePresent(networkPolicy, httpPort);
        }
        if (httpsPort > 0) {
            assertApplicationRulePresent(networkPolicy, httpsPort);
        }
        if (mgntPort > 0) {
            assertManagementRulePresent(networkPolicy, mgntPort);
        }
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

    private static void enableNetworkPolicy(Keycloak keycloak) {
        var builder = new NetworkPolicySpecBuilder();
        builder.withNetworkPolicyEnabled(true);
        keycloak.getSpec().setNetworkPolicySpec(builder.build());
    }

    private static int configureManagement(Keycloak keycloak, boolean randomPort) {
        if (!randomPort) {
            return Constants.KEYCLOAK_MANAGEMENT_PORT;
        }
        var port = ThreadLocalRandom.current().nextInt(10_000, 10_100);
        keycloak.getSpec().setHttpManagementSpec(new HttpManagementSpecBuilder().withPort(port).build());
        return port;
    }

    private static int enableHttp(Keycloak keycloak, boolean randomPort) {
        keycloak.getSpec().getHttpSpec().setHttpEnabled(true);
        if (randomPort) {
            var port = ThreadLocalRandom.current().nextInt(10_100, 10_200);
            keycloak.getSpec().getHttpSpec().setHttpPort(port);
            return port;
        }
        return Constants.KEYCLOAK_HTTP_PORT;
    }

    private static void disableHttps(Keycloak keycloak) {
        keycloak.getSpec().getHttpSpec().setTlsSecret(null);
    }

    private static int configureHttps(Keycloak keycloak, boolean randomPort) {
        if (randomPort) {
            var port = ThreadLocalRandom.current().nextInt(10_200, 10_300);
            keycloak.getSpec().getHttpSpec().setHttpsPort(port);
            return port;
        }
        return Constants.KEYCLOAK_HTTPS_PORT;
    }

    private static void disableManagement(Keycloak keycloak, boolean legacyOption) {
        if (legacyOption) {
            keycloak.getSpec().getAdditionalOptions().add(new ValueOrSecret("legacy-observability-interface", "true"));
        } else {
            keycloak.getSpec().getAdditionalOptions().add(new ValueOrSecret("health-enabled", "false"));
        }
        // The custom image from GitHub Actions is optimized and does not allow to change the build time attributes
        // Fallback to the default/nightly image.
        if (getTestCustomImage() != null) {
            keycloak.getSpec().setImage(null);
        }
    }

    private static Keycloak create() {
        var kc = getTestKeycloakDeployment(false);
        kc.getSpec().setInstances(2);
        var hostnameSpecBuilder = new HostnameSpecBuilder()
                .withStrict(false)
                .withStrictBackchannel(false);
        if (isOpenShift) {
            kc.getSpec().setIngressSpec(new IngressSpecBuilder().withIngressClassName(KeycloakController.OPENSHIFT_DEFAULT).build());
        }
        kc.getSpec().setHostnameSpec(hostnameSpecBuilder.build());
        return kc;
    }

}
