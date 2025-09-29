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

package org.keycloak.operator.testsuite.unit;

import java.util.List;

import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicy;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicyPeer;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicyPeerBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.keycloak.operator.Constants;
import org.keycloak.operator.controllers.KeycloakNetworkPolicyDependentResource;
import org.keycloak.operator.crds.v2alpha1.deployment.Keycloak;
import org.keycloak.operator.crds.v2alpha1.deployment.ValueOrSecret;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.NetworkPolicySpec;
import org.keycloak.operator.testsuite.utils.CRAssert;
import org.keycloak.operator.testsuite.utils.K8sUtils;
import org.keycloak.operator.testsuite.utils.MockController;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NetworkPolicyLogicTest {

    private static class MockKeycloakNetworkPolicy extends MockController<NetworkPolicy, KeycloakNetworkPolicyDependentResource> {

        MockKeycloakNetworkPolicy(Keycloak keycloak) {
            super(new KeycloakNetworkPolicyDependentResource(), keycloak);
        }

        @Override
        protected boolean isEnabled() {
            return NetworkPolicySpec.isNetworkPolicyEnabled(keycloak);
        }

        @Override
        protected NetworkPolicy desired() {
            return dependentResource.desired(keycloak, null);
        }
    }

    @Test
    public void testDefaults() {
        var keycloak = K8sUtils.getDefaultKeycloakDeployment();
        var controller = new MockKeycloakNetworkPolicy(keycloak);
        assertTrue(controller.isEnabled());
        assertTrue(controller.reconciled());
        assertFalse(controller.deleted());
        assertEnabledAndGet(keycloak);
    }

    @Test
    public void testNetworkPolicyDisabled() {
        var keycloak = K8sUtils.getDefaultKeycloakDeployment();
        K8sUtils.disableNetworkPolicy(keycloak);

        var controller = new MockKeycloakNetworkPolicy(keycloak);
        assertFalse(controller.isEnabled());
        assertFalse(controller.reconciled());
        assertFalse(controller.deleted());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void testHttpOnly(boolean randomPort) {
        var kc = K8sUtils.getDefaultKeycloakDeployment();
        K8sUtils.enableNetworkPolicy(kc);
        K8sUtils.disableHttps(kc);
        var httpPort = K8sUtils.enableHttp(kc, randomPort);
        var mngtPort = K8sUtils.configureManagement(kc, randomPort);
        kc.getSpec().getNetworkPolicySpec().setHttpRules(List.of(namespaceSelectorWithMatchLabel("http", "true")));
        kc.getSpec().getNetworkPolicySpec().setManagementRules(List.of(podSelectorWithMatchExpression("monitoring", "from", "1", "and", "2")));
        var networkPolicy = assertEnabledAndGet(kc);
        CRAssert.assertIngressRules(networkPolicy, kc, httpPort, -1, mngtPort);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void testHttpsOnly(boolean randomPort) {
        var kc = K8sUtils.getDefaultKeycloakDeployment();
        K8sUtils.enableNetworkPolicy(kc);
        var httpsPort = K8sUtils.configureHttps(kc, randomPort);
        var mngtPort = K8sUtils.configureManagement(kc, randomPort);
        kc.getSpec().getNetworkPolicySpec().setHttpsRules(List.of(podSelectorWithMatchLabel("https", "yes!")));
        kc.getSpec().getNetworkPolicySpec().setManagementRules(List.of(namespaceSelectorWithMatchExpressions("namespace", "in", "somewhere")));
        var networkPolicy = assertEnabledAndGet(kc);
        CRAssert.assertIngressRules(networkPolicy, kc, -1, httpsPort, mngtPort);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void testHttpAndHttps(boolean randomPort) {
        var kc = K8sUtils.getDefaultKeycloakDeployment();
        K8sUtils.enableNetworkPolicy(kc);
        var httpPort = K8sUtils.enableHttp(kc, randomPort);
        var httpsPort = K8sUtils.configureHttps(kc, randomPort);
        var mngtPort = K8sUtils.configureManagement(kc, randomPort);

        kc.getSpec().getNetworkPolicySpec().setHttpRules(List.of(
                ipBlock("127.0.0.1"),
                namespaceSelectorWithMatchExpressions("name", "in", "local", "local-2")
        ));

        kc.getSpec().getNetworkPolicySpec().setHttpsRules(List.of(
                podSelectorWithMatchExpression("app", "equals", "keycloak"),
                ipBlock("10.0.0.0/8")
        ));

        kc.getSpec().getNetworkPolicySpec().setManagementRules(List.of(
                namespaceSelectorWithMatchExpressions("monitoring", "contains", "always", "on")
        ));

        var networkPolicy = assertEnabledAndGet(kc);

        CRAssert.assertIngressRules(networkPolicy, kc, httpPort, httpsPort, mngtPort);
    }

    @ParameterizedTest()
    @ValueSource(booleans = {true, false})
    public void testManagementDisabled(boolean legacyOption) {
        var kc = K8sUtils.getDefaultKeycloakDeployment();
        K8sUtils.enableNetworkPolicy(kc);
        disableManagement(kc, legacyOption);
        kc.getSpec().getNetworkPolicySpec().setHttpsRules(List.of(
                ipBlock("127.0.0.1/15", "127.0.0.1/18", "127.0.0.1/19"),
                podSelectorWithMatchLabel("app", "keycloak"),
                namespaceSelectorWithMatchLabel("kubernetes.io/name", "keycloak")
        ));
        var networkPolicy = assertEnabledAndGet(kc);
        var mgmtPort = legacyOption ? -1 : Constants.KEYCLOAK_MANAGEMENT_PORT;
        CRAssert.assertIngressRules(networkPolicy, kc, -1, Constants.KEYCLOAK_HTTPS_PORT, mgmtPort);
    }

    @Test
    public void testUpdate() {
        var kc = K8sUtils.getDefaultKeycloakDeployment();
        K8sUtils.enableNetworkPolicy(kc);

        var controller = new MockKeycloakNetworkPolicy(kc);

        assertTrue(controller.isEnabled());
        assertTrue(controller.reconciled());
        assertFalse(controller.deleted());

        kc.getSpec().getNetworkPolicySpec().setNetworkPolicyEnabled(false);

        assertFalse(controller.isEnabled());
        assertFalse(controller.reconciled());
        assertTrue(controller.deleted());
    }

    private static NetworkPolicy assertEnabledAndGet(Keycloak keycloak) {
        var controller = new MockKeycloakNetworkPolicy(keycloak);

        assertTrue(controller.isEnabled());
        assertTrue(controller.reconciled());
        assertFalse(controller.deleted());

        var networkPolicy = controller.getReconciledResource();
        assertTrue(networkPolicy.isPresent());
        return networkPolicy.get();
    }

    private static void assertDisabled(Keycloak keycloak) {
        var controller = new MockKeycloakNetworkPolicy(keycloak);

        assertTrue(controller.isEnabled());
        assertTrue(controller.reconciled());
        assertFalse(controller.deleted());

        var networkPolicy = controller.getReconciledResource();
        assertTrue(networkPolicy.isEmpty());
    }

    private static void disableManagement(Keycloak keycloak, boolean legacyOption) {
        if (legacyOption) {
            keycloak.getSpec().getAdditionalOptions().add(new ValueOrSecret("legacy-observability-interface", "true"));
        } else {
            keycloak.getSpec().getAdditionalOptions().add(new ValueOrSecret("health-enabled", "false"));
        }
    }

    private static NetworkPolicyPeer podSelectorWithMatchLabel(String label, String value) {
        var builder = new NetworkPolicyPeerBuilder();
        builder.withNewPodSelector()
                .addToMatchLabels(label, value)
                .endPodSelector();
        return builder.build();
    }

    private static NetworkPolicyPeer podSelectorWithMatchExpression(String key, String operator, String... values) {
        var builder = new NetworkPolicyPeerBuilder();
        var selector = builder.withNewPodSelector();
        selector.addNewMatchExpression()
                .withKey(key)
                .withOperator(operator)
                .addToValues(values)
                .endMatchExpression();
        selector.endPodSelector();
        return builder.build();
    }

    private static NetworkPolicyPeer namespaceSelectorWithMatchLabel(String label, String value) {
        var builder = new NetworkPolicyPeerBuilder();
        builder.withNewPodSelector()
                .addToMatchLabels(label, value)
                .endPodSelector();
        return builder.build();
    }

    private static NetworkPolicyPeer namespaceSelectorWithMatchExpressions(String key, String operator, String... values) {
        var builder = new NetworkPolicyPeerBuilder();
        var selector = builder.withNewNamespaceSelector();
        selector.addNewMatchExpression()
                .withKey(key)
                .withOperator(operator)
                .addToValues(values)
                .endMatchExpression();
        selector.endNamespaceSelector();
        return builder.build();
    }

    private static NetworkPolicyPeer ipBlock(String cidr, String... except) {
        var builder = new NetworkPolicyPeerBuilder();
        var selector = builder.withNewIpBlock();
        selector.withCidr(cidr)
                .withExcept(except);
        selector.endIpBlock();
        return builder.build();
    }

}
