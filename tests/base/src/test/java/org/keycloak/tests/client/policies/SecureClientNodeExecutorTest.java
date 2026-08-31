/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.tests.client.policies;

import java.util.List;
import java.util.Map;

import jakarta.ws.rs.BadRequestException;

import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.idm.ClientPolicyConditionConfigurationRepresentation;
import org.keycloak.services.clientpolicy.condition.AnyClientConditionFactory;
import org.keycloak.services.clientpolicy.executor.SecureClientNodeExecutor;
import org.keycloak.services.clientpolicy.executor.SecureClientNodeExecutorFactory;
import org.keycloak.testframework.annotations.InjectClient;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ClientConfig;
import org.keycloak.testframework.realm.ManagedClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testsuite.util.oauth.RegisterNodeResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@KeycloakIntegrationTest
public class SecureClientNodeExecutorTest extends AbstractClientPoliciesTest {

    private static final String PROVIDER_ID   = SecureClientNodeExecutorFactory.PROVIDER_ID;

    private static final String PATTERN_INTERNAL  = "^app-node-\\d+\\.internal\\.example\\.com$";
    private static final String PATTERN_LEGACY    = "^legacy\\.prod\\.example\\.com$";
    private static final String VALID_HOST        = "app-node-1.internal.example.com";
    private static final String VALID_HOST_LEGACY = "legacy.prod.example.com";
    private static final String INVALID_HOST      = "evil.attacker.com";
    private static final String PORT_HOST         = "app-node-1.internal.example.com:8443";

    @InjectRealm
    ManagedRealm realm;

    @InjectClient(config = ConfidentialClientConfig.class)
    ManagedClient confidentialClient;

    @InjectOAuthClient
    OAuthClient oauth;

    @AfterEach
    void resetPolicies() {
        realm.updateWithCleanup(r -> r.resetClientProfiles().resetClientPolicies());
    }

    @Test
    void registerNode_adminPath_validHostname_persistedAndNodePresent() throws Exception {
        setupPolicy(List.of(PATTERN_INTERNAL));

        realm.admin().clients().get(confidentialClient.getId())
                .registerNode(Map.of("node", VALID_HOST));

        Map<String, Integer> nodes = realm.admin().clients().get(confidentialClient.getId())
                .toRepresentation().getRegisteredNodes();
        Assertions.assertNotNull(nodes);
        Assertions.assertTrue(nodes.containsKey(VALID_HOST),
                "Expected " + VALID_HOST + " to be persisted in registeredNodes");

        realm.admin().clients().get(confidentialClient.getId()).unregisterNode(VALID_HOST);
    }

    @Test
    void registerNode_adminPath_invalidHostname_nothingPersisted() throws Exception {
        setupPolicy(List.of(PATTERN_INTERNAL));

        Assertions.assertThrows(BadRequestException.class, () ->
                realm.admin().clients().get(confidentialClient.getId())
                        .registerNode(Map.of("node", INVALID_HOST)));

        Map<String, Integer> nodes = realm.admin().clients().get(confidentialClient.getId())
                .toRepresentation().getRegisteredNodes();
        Assertions.assertTrue(nodes == null || !nodes.containsKey(INVALID_HOST),
                "Rejected hostname must not be written to registeredNodes");
    }

    @Test
    void registerNode_adminPath_portSuffixedHostname_rejectedAndNotPersisted() throws Exception {
        setupPolicy(List.of(PATTERN_INTERNAL));

        Assertions.assertThrows(BadRequestException.class, () ->
                realm.admin().clients().get(confidentialClient.getId())
                        .registerNode(Map.of("node", PORT_HOST)));

        Map<String, Integer> nodes = realm.admin().clients().get(confidentialClient.getId())
                .toRepresentation().getRegisteredNodes();
        Assertions.assertTrue(nodes == null || !nodes.containsKey(PORT_HOST),
                "Port-suffixed hostname must not be written to registeredNodes");
    }

    @Test
    void registerNode_adminPath_multiplePatterns_secondPatternAllowsHost() throws Exception {
        setupPolicy(List.of(PATTERN_INTERNAL, PATTERN_LEGACY));

        realm.admin().clients().get(confidentialClient.getId())
                .registerNode(Map.of("node", VALID_HOST_LEGACY));

        Map<String, Integer> nodes = realm.admin().clients().get(confidentialClient.getId())
                .toRepresentation().getRegisteredNodes();
        Assertions.assertNotNull(nodes);
        Assertions.assertTrue(nodes.containsKey(VALID_HOST_LEGACY),
                "Expected " + VALID_HOST_LEGACY + " to be persisted via second pattern");

        realm.admin().clients().get(confidentialClient.getId()).unregisterNode(VALID_HOST_LEGACY);
    }

    @Test
    void registerNode_adminPath_noExecutorConfigured_anyhostPassesThrough() {
        // No policy attached, all registrations should pass through.
        realm.admin().clients().get(confidentialClient.getId())
                .registerNode(Map.of("node", INVALID_HOST));

        Map<String, Integer> nodes = realm.admin().clients().get(confidentialClient.getId())
                .toRepresentation().getRegisteredNodes();
        Assertions.assertNotNull(nodes);
        Assertions.assertTrue(nodes.containsKey(INVALID_HOST));

        realm.admin().clients().get(confidentialClient.getId()).unregisterNode(INVALID_HOST);
    }

    @Test
    void registerNode_adapterPath_validHostname_returns204() throws Exception {
        setupPolicy(List.of(PATTERN_INTERNAL));

        RegisterNodeResponse response = oauth
                .registerNodeRequest()
                .client(confidentialClient.getClientId(), confidentialClient.getSecret())
                .clientClusterHost(VALID_HOST)
                .send();

        Assertions.assertTrue(response.isSuccess(),
                "Expected 204 for a valid hostname, got: " + response.getStatusCode());

        realm.admin().clients().get(confidentialClient.getId()).unregisterNode(VALID_HOST);
    }

    @Test
    void registerNode_adapterPath_invalidHostname_returns400AndNotPersisted() throws Exception {
        setupPolicy(List.of(PATTERN_INTERNAL));

        RegisterNodeResponse response = oauth
                .registerNodeRequest()
                .client(confidentialClient.getClientId(), confidentialClient.getSecret())
                .clientClusterHost(INVALID_HOST)
                .send();

        Assertions.assertFalse(response.isSuccess(),
                "Expected rejection for an invalid hostname");
        Assertions.assertEquals(400, response.getStatusCode());

        Map<String, Integer> nodes = realm.admin().clients().get(confidentialClient.getId())
                .toRepresentation().getRegisteredNodes();
        Assertions.assertTrue(nodes == null || !nodes.containsKey(INVALID_HOST),
                "Rejected hostname must not be written to registeredNodes");
    }

    @Test
    void registerNode_adapterPath_portSuffixedHostname_returns400AndNotPersisted() throws Exception {
        setupPolicy(List.of(PATTERN_INTERNAL));

        RegisterNodeResponse response = oauth
                .registerNodeRequest()
                .client(confidentialClient.getClientId(), confidentialClient.getSecret())
                .clientClusterHost(PORT_HOST)
                .send();

        Assertions.assertFalse(response.isSuccess(),
                "Expected rejection for a port-suffixed hostname");
        Assertions.assertEquals(400, response.getStatusCode());

        Map<String, Integer> nodes = realm.admin().clients().get(confidentialClient.getId())
                .toRepresentation().getRegisteredNodes();
        Assertions.assertTrue(nodes == null || !nodes.containsKey(PORT_HOST),
                "Port-suffixed hostname must not be written to registeredNodes");
    }

    @Test
    void registerNode_adapterPath_noExecutorConfigured_anyHostPassesThrough() throws Exception {
        RegisterNodeResponse response = oauth
                .registerNodeRequest()
                .client(confidentialClient.getClientId(), confidentialClient.getSecret())
                .clientClusterHost(INVALID_HOST)
                .send();

        Assertions.assertTrue(response.isSuccess(),
                "Expected passthrough (no executor configured), got: " + response.getStatusCode());

        realm.admin().clients().get(confidentialClient.getId()).unregisterNode(INVALID_HOST);
    }

    @Test
    void unregisterNode_adminPath_succeeds() throws Exception {
        realm.admin().clients().get(confidentialClient.getId())
                .registerNode(Map.of("node", VALID_HOST));

        setupPolicy(List.of(PATTERN_INTERNAL));

        realm.admin().clients().get(confidentialClient.getId()).unregisterNode(VALID_HOST);

        Map<String, Integer> nodes = realm.admin().clients().get(confidentialClient.getId())
                .toRepresentation().getRegisteredNodes();
        Assertions.assertTrue(nodes == null || !nodes.containsKey(VALID_HOST),
                "Node should have been removed");
    }

    @Test
    void unregisterNode_adminPath_afterExecutorGatedRegistration_succeeds() throws Exception {
        setupPolicy(List.of(PATTERN_INTERNAL));

        realm.admin().clients().get(confidentialClient.getId())
                .registerNode(Map.of("node", VALID_HOST));

        realm.admin().clients().get(confidentialClient.getId()).unregisterNode(VALID_HOST);

        Map<String, Integer> nodes = realm.admin().clients().get(confidentialClient.getId())
                .toRepresentation().getRegisteredNodes();
        Assertions.assertTrue(nodes == null || !nodes.containsKey(VALID_HOST),
                "Node should have been removed after executor-gated registration");
    }

    private void setupPolicy(List<String> patterns) throws Exception {
        SecureClientNodeExecutor.Configuration config = new SecureClientNodeExecutor.Configuration();
        config.setHostnameAllowedPatterns(patterns);
        setupPolicy(realm, PROVIDER_ID, config,
                AnyClientConditionFactory.PROVIDER_ID,
                new ClientPolicyConditionConfigurationRepresentation());
    }

    public static class ConfidentialClientConfig implements ClientConfig {
        @Override
        public ClientBuilder configure(ClientBuilder client) {
            return client
                    .clientId("node-test-client")
                    .secret("node-test-secret")
                    .protocol(OIDCLoginProtocol.LOGIN_PROTOCOL)
                    .publicClient(false);
        }
    }
}
