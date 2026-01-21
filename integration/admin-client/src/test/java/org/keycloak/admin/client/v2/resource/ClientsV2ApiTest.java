/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.admin.client.v2.resource;

import java.net.Socket;
import java.net.URI;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


class ClientsV2ApiTest {

    private static final String SERVER_URL = System.getProperty("keycloak.url", "http://localhost:8080");
    private static final String REALM = "master";
    private static final String USER = System.getProperty("keycloak.user", "admin");
    private static final String PASSWORD = System.getProperty("keycloak.password", "admin");

    @Test
    @DisplayName("Should retrieve clients list from V2 endpoint successfully")
    void testGetClientsV2_EndToEnd() {
        Assumptions.assumeTrue(isServerUp(SERVER_URL), "Test skipped: Keycloak server is not reachable at " + SERVER_URL);

        try (Keycloak legacyClient = KeycloakBuilder.builder()
                .serverUrl(SERVER_URL)
                .realm(REALM)
                .username(USER)
                .password(PASSWORD)
                .clientId("admin-cli")
                .build()) {

            String token = legacyClient.tokenManager().getAccessToken().getToken();

            try (Client client = ClientBuilder.newClient()) {

                Response response = client.target(SERVER_URL)
                        .path("/admin-v2/realms/" + REALM + "/clients")
                        .request()
                        .header("Authorization", "Bearer " + token)
                        .get();

                Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus(),
                        "The V2 endpoint should return 200 OK status");

                String jsonResponse = response.readEntity(String.class);

                Assertions.assertNotNull(jsonResponse, "Response payload should not be empty");
                Assertions.assertTrue(jsonResponse.contains("admin-cli"),
                        "The clients list must include the 'admin-cli' client");
            }
        } catch (Exception e) {
            Assertions.fail("End-to-End test failed due to an unexpected exception: " + e.getMessage(), e);
        }
    }

    private boolean isServerUp(String urlStr) {
        try {
            URI uri = URI.create(urlStr);
            try (Socket s = new Socket(uri.getHost(), uri.getPort())) {
                return true;
            }
        } catch (Exception e) {
            return false;
        }
    }
}
