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

package org.keycloak.tests.admin.client.v2;

import java.util.List;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.authentication.authenticators.client.ClientIdAndSecretAuthenticator;
import org.keycloak.common.Profile;
import org.keycloak.events.admin.OperationType;
import org.keycloak.representations.admin.v2.OIDCClientRepresentation;
import org.keycloak.representations.admin.v2.SAMLClientRepresentation;
import org.keycloak.representations.idm.AdminEventRepresentation;
import org.keycloak.representations.idm.RealmEventsConfigRepresentation;
import org.keycloak.services.PatchTypeNames;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectHttpClient;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

import org.apache.http.HttpMessage;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.keycloak.events.admin.v2.AdminEventV2Builder.API_VERSION_DETAIL_KEY;
import static org.keycloak.events.admin.v2.AdminEventV2Builder.API_VERSION_V2;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for Admin API v2 events.
 * Verifies that v2 admin events are fired with the apiVersion=v2 detail.
 */
@KeycloakIntegrationTest(config = AdminEventV2Test.AdminV2EventConfig.class)
public class AdminEventV2Test extends AbstractClientApiV2Test {
    private static final String TEST_CLIENT_ID = "v2-rep-test-client";

    @InjectHttpClient
    CloseableHttpClient client;

    @InjectAdminClient
    Keycloak adminClient;

    @InjectRealm(attachTo = "master", ref = "master")
    ManagedRealm masterRealm;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @BeforeEach
    public void setupAndClearEvents() {
        runOnServer.run(session -> {
            // enable Admin v2 events on the server
            System.setProperty("kc.admin-v2.client-service.events.enabled", "true");
        });

        // Enable admin events on master realm
        RealmEventsConfigRepresentation eventsConfig = masterRealm.admin().getRealmEventsConfig();
        eventsConfig.setAdminEventsEnabled(true);
        eventsConfig.setAdminEventsDetailsEnabled(true);
        masterRealm.admin().updateRealmEventsConfig(eventsConfig);
        
        // Clear any existing events
        masterRealm.admin().clearAdminEvents();
    }

    @AfterEach
    public void disableAdminEventsV2() {
        runOnServer.run(session -> {
            // enable Admin v2 events on the server
            System.clearProperty("kc.admin-v2.client-service.events.enabled");
        });
    }

    @Test
    public void createClientFiresV2Event() throws Exception {
        createTestClient();
        try {
            // Verify v2 events were fired
            List<AdminEventRepresentation> events = masterRealm.admin().getAdminEvents();

            // Should have at least 1 event
            assertThat("Should have at least 1 event", events.size(), greaterThanOrEqualTo(1));

            // Find the v2 event (has apiVersion=v2 in details)
            AdminEventRepresentation v2Event = events.stream()
                    .filter(e -> e.getDetails() != null && API_VERSION_V2.equals(e.getDetails().get(API_VERSION_DETAIL_KEY)))
                    .findFirst()
                    .orElse(null);

            assertThat("V2 event should be present with apiVersion=v2 detail", v2Event, notNullValue());
            assertThat("V2 event should have CREATE operation", v2Event.getOperationType(), is(OperationType.CREATE.toString()));
            assertThat("V2 event should have resource path relative to API v2", v2Event.getResourcePath(), is("clients/v2"));
        } finally {
            deleteTestClient();
        }
    }

    @Test
    public void updateClientFiresV2Event() throws Exception {
        createTestClient();
        try {
            masterRealm.admin().clearAdminEvents();

            HttpPut updateRequest = new HttpPut(getClientsApiUrl() + "/" + TEST_CLIENT_ID);
            setAuthHeader(updateRequest);
            updateRequest.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);

            OIDCClientRepresentation rep = getTestClientRepresentation();
            // update the client
            rep.setDescription("Updated description");

            updateRequest.setEntity(new StringEntity(mapper.writeValueAsString(rep)));

            try (var response = client.execute(updateRequest)) {
                assertEquals(200, response.getStatusLine().getStatusCode());
            }

            assertUpdateEventFired("Updated description");
        } finally {
            deleteTestClient();
        }
    }

    @Test
    public void patchClientFiresV2Event() throws Exception {
        createTestClient();
        try {
            masterRealm.admin().clearAdminEvents();

            HttpPatch patchRequest = new HttpPatch(getClientsApiUrl() + "/" + TEST_CLIENT_ID);
            setAuthHeader(patchRequest);

            patchRequest.setHeader(HttpHeaders.CONTENT_TYPE, PatchTypeNames.JSON_MERGE);

            OIDCClientRepresentation patch = new OIDCClientRepresentation();
            patch.setDescription("Patched description");

            patchRequest.setEntity(new StringEntity(mapper.writeValueAsString(patch)));

            try (var response = client.execute(patchRequest)) {
                assertEquals(200, response.getStatusLine().getStatusCode());
            }

            assertUpdateEventFired("Patched description");
        } finally {
            deleteTestClient();
        }
    }

    private void assertUpdateEventFired(String newDescription){
        // Verify v2 UPDATE event was fired
        List<AdminEventRepresentation> events = masterRealm.admin().getAdminEvents();

        // Find the v2 event (has apiVersion=v2 in details)
        AdminEventRepresentation v2Event = events.stream()
                .filter(e -> e.getDetails() != null && API_VERSION_V2.equals(e.getDetails().get(API_VERSION_DETAIL_KEY)))
                .findFirst()
                .orElse(null);

        assertThat("V2 event should be present for update with apiVersion=v2 detail", v2Event, notNullValue());
        assertThat("V2 event should have UPDATE operation", v2Event.getOperationType(), is(OperationType.UPDATE.toString()));
        assertThat("V2 event should have resource path relative to API v2", v2Event.getResourcePath(), is("clients/v2/%s".formatted(TEST_CLIENT_ID)));

        var representation = v2Event.getRepresentation();
        assertThat("V2 event should have representation", representation, notNullValue());
        assertThat("V2 event should have updated client representation description", representation, containsString("\"description\":\"%s\"".formatted(newDescription)));
        assertThat("V2 event should have masked secret in representation", representation, containsString("\"secret\":\"**********\""));
    }

    @Test
    public void v2EventContainsV2Representation() throws Exception {
        createTestClient();
        try {
            // Verify v2 event contains the v2 representation format
            List<AdminEventRepresentation> events = masterRealm.admin().getAdminEvents();

            // Find the v2 event
            AdminEventRepresentation v2Event = events.stream()
                    .filter(e -> e.getDetails() != null && API_VERSION_V2.equals(e.getDetails().get(API_VERSION_DETAIL_KEY)))
                    .findFirst()
                    .orElse(null);

            assertThat("V2 event should be present", v2Event, notNullValue());
            assertThat("V2 event should have resource path relative to API v2", v2Event.getResourcePath(), is("clients/v2"));
            var representation = v2Event.getRepresentation();
            assertThat("V2 event should have representation", representation, notNullValue());
            assertThat("V2 event should have masked secret in representation", representation, containsString("\"secret\":\"**********\""));

            // The v2 representation should contain the "protocol" field (part of v2 format)
            assertTrue(v2Event.getRepresentation().contains("\"protocol\""),
                    "V2 event representation should contain protocol field");
            assertTrue(v2Event.getRepresentation().contains("\"clientId\""),
                    "V2 event representation should contain clientId field");
        } finally {
            deleteTestClient();
        }
    }

    @Test
    public void stripSamlSigningCertificateFromRepresentation() throws Exception {
        var SAML_CLIENT_ID = "saml-with-certificate";

        HttpPost request = new HttpPost(getClientsApiUrl());
        setAuthHeader(request);
        request.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);

        SAMLClientRepresentation samlRep = new SAMLClientRepresentation();
        samlRep.setEnabled(true);
        samlRep.setClientId(SAML_CLIENT_ID);
        samlRep.setSigningCertificate("""
                -----BEGIN CERTIFICATE-----
                MIIDqDCCApCgAwIBAgIUY0R7RzJQbQJx9z3Y+0l9v0E2XQkwDQYJKoZIhvcNAQEL
                BQAwgYUxCzAJBgNVBAYTAkRFMRMwEQYDVQQIDApTb21lLVN0YXRlMRMwEQYDVQQH
                DApTb21lLUNpdHkxFTATBgNVBAoMDEV4YW1wbGUgT3JnMR8wHQYDVQQLDBZJZGVu
                dGl0eSAmIEFjY2VzczE-HELLO-HOW-ARE-YOU-AwwQZXhhbXBsZS5jb20wHhcNMj
                MDAwWhcNMjYwMTAxMDAwMDAwWjCBhTELMAkGA1UEBhMCREUxEzARBgNVBAgMClNv
                bWUtU3RhdGUxEzARBgNVBAcMClNvbWUtQ2l0eTEVMBMGA1UECgwMRXhhbXBsZSBP
                cmcxHzAdBgNVBAsMFklkZW50aXR5ICYgQWNjZXNzMRkwFwYDVQQDDBBleGFtcGxl
                LmNvbTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAL+...
                -----END CERTIFICATE-----
                """);
        request.setEntity(new StringEntity(mapper.writeValueAsString(samlRep)));

        try (var response = client.execute(request)) {
            EntityUtils.consumeQuietly(response.getEntity());
            assertEquals(201, response.getStatusLine().getStatusCode());
        }

        try {
            List<AdminEventRepresentation> events = masterRealm.admin().getAdminEvents();

            // Find the v2 event
            AdminEventRepresentation v2Event = events.stream()
                    .filter(e -> e.getDetails() != null && API_VERSION_V2.equals(e.getDetails().get(API_VERSION_DETAIL_KEY)))
                    .findFirst()
                    .orElse(null);

            assertThat("V2 event should be present", v2Event, notNullValue());
            assertThat("V2 event should have resource path relative to API v2", v2Event.getResourcePath(), is("clients/v2"));
            var representation = v2Event.getRepresentation();
            assertThat("V2 event should have representation", representation, notNullValue());
            assertThat("V2 event should have masked signing certificate in representation", representation, containsString("\"signingCertificate\":\"**********\""));
        } finally {
            deleteClient(SAML_CLIENT_ID);
        }
    }

    private void createTestClient() throws Exception {
        // Create a client via v2 API (representation details already enabled in @BeforeEach)
        HttpPost request = new HttpPost(getClientsApiUrl());
        setAuthHeader(request);
        request.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);

        request.setEntity(new StringEntity(mapper.writeValueAsString(getTestClientRepresentation())));

        try (var response = client.execute(request)) {
            EntityUtils.consumeQuietly(response.getEntity());
            assertEquals(201, response.getStatusLine().getStatusCode());
        }
    }

    private void deleteTestClient() throws Exception {
        deleteClient(TEST_CLIENT_ID);
    }

    private void deleteClient(String clientId) throws Exception {
        HttpDelete deleteRequest = new HttpDelete(getClientsApiUrl() + "/" + clientId);
        setAuthHeader(deleteRequest);
        try (var response = client.execute(deleteRequest)) {
            assertEquals(204, response.getStatusLine().getStatusCode());
        }
    }

    private OIDCClientRepresentation getTestClientRepresentation() {
        OIDCClientRepresentation rep = new OIDCClientRepresentation();
        rep.setEnabled(true);
        rep.setClientId(TEST_CLIENT_ID);
        rep.setDescription("Client to test v2 representation");
        var auth = new OIDCClientRepresentation.Auth();
        auth.setMethod(ClientIdAndSecretAuthenticator.PROVIDER_ID);
        auth.setSecret("ultra-secret");
        rep.setAuth(auth);
        return rep;
    }

    private void setAuthHeader(HttpMessage request) {
        String token = adminClient.tokenManager().getAccessTokenString();
        request.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    public static class AdminV2EventConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.CLIENT_ADMIN_API_V2);
        }
    }
}
