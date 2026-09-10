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
package org.keycloak.tests.authzen;

import java.io.IOException;

import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectSimpleHttp;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.authzen.client.AuthZenClient;
import org.keycloak.testframework.authzen.client.AuthZenClient.WellKnownResponse;
import org.keycloak.testframework.authzen.client.annotations.InjectAuthZenClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.server.KeycloakUrls;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@KeycloakIntegrationTest(config = AuthZenServerConfig.class)
public class AuthZenWellKnownTest {

    @InjectRealm
    ManagedRealm realm;

    @InjectAuthZenClient
    AuthZenClient authZenClient;

    @InjectSimpleHttp
    SimpleHttp simpleHttp;

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    @Test
    public void testRealmWellKnownEndpoint() throws IOException {
        WellKnownResponse response = authZenClient.fetchWellKnownConfiguration();

        assertWellKnownResponse(response);
    }

    @Test
    public void testServerMetadataWellKnownEndpoint() throws IOException {
        String url = keycloakUrls.getBase() + "/.well-known/authzen-configuration/realms/" + realm.getName();
        WellKnownResponse response = simpleHttp.doGet(url).asJson(WellKnownResponse.class);

        assertWellKnownResponse(response);
    }

    private void assertWellKnownResponse(WellKnownResponse response) {
        String expectedRealmUrl = realmUrl();

        assertNotNull(response);
        assertEquals(expectedRealmUrl, response.policyDecisionPoint());
        assertEquals(expectedRealmUrl + "/authzen/access/v1/evaluation", response.accessEvaluationEndpoint());
        assertEquals(expectedRealmUrl + "/authzen/access/v1/evaluations", response.accessEvaluationsEndpoint());
    }

    private String realmUrl() {
        return keycloakUrls.getBase() + "/realms/" + realm.getName();
    }
}
