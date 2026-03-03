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
package org.keycloak.tests.admin.client.v2;

import java.util.Arrays;
import java.util.Set;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.common.Profile;
import org.keycloak.representations.admin.v2.OIDCClientRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectHttpClient;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.admin.mapper.ClientRepresentationComparator;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This class tests the interoperability between v1 and v2 admin APIs.
 * It verifies that clients created/updated via one API version can be correctly
 * read and validated via the other API version.
 */
@KeycloakIntegrationTest(config = InteropTest.ServerConfig.class)
public class InteropTest extends AbstractClientApiV2Test {


    @InjectHttpClient
    CloseableHttpClient httpClient;

    @InjectAdminClient
    Keycloak adminClient;

    @InjectRealm
    ManagedRealm managedRealm;

    @Override
    public String getRealmName() {
        return managedRealm.getName();
    }

    /**
     * Test: Create a client using v1 API, then assert/read using v2 API.
     */
    @Test
    public void createWithV1AssertWithV2() throws Exception {
        RealmResource realm = managedRealm.admin();

        ClientRepresentation v1Client = new ClientRepresentation();
        v1Client.setClientId("v1-created-client");
        v1Client.setName("V1 Created Client");
        v1Client.setDescription("Client created via v1 API");
        v1Client.setEnabled(true);
        v1Client.setPublicClient(false);
        v1Client.setProtocol("openid-connect");
        v1Client.setBaseUrl("http://localhost:3000");
        v1Client.setRedirectUris(Arrays.asList("http://localhost:3000/*", "http://localhost:3001/*"));
        v1Client.setWebOrigins(Arrays.asList("http://localhost:3000", "http://localhost:3001"));
        v1Client.setStandardFlowEnabled(true);
        v1Client.setDirectAccessGrantsEnabled(true);
        v1Client.setServiceAccountsEnabled(false);
        v1Client.setClientAuthenticatorType("client-secret");
        v1Client.setSecret("test-secret-123");

        Response response = realm.clients().create(v1Client);
        String clientUuid = ApiUtil.getCreatedId(response);
        response.close();

        ClientRepresentation createdV1Client = realm.clients().get(clientUuid).toRepresentation();

        HttpGet getRequest = new HttpGet(getClientsApiUrl() + "/v1-created-client");
        setAuthHeader(getRequest, adminClient);

        try (var httpResponse = httpClient.execute(getRequest)) {
            assertThat(httpResponse.getStatusLine().getStatusCode(), is(200));
            OIDCClientRepresentation v2Client = mapper.createParser(httpResponse.getEntity().getContent())
                    .readValueAs(OIDCClientRepresentation.class);

            ClientRepresentationComparator.ComparisonResult result = 
                ClientRepresentationComparator.compare(createdV1Client, v2Client);

            assertTrue(result.allMatch(), "V1 and V2 representations should match:\n" + result);
        } finally {
            realm.clients().get(clientUuid).remove();
        }
    }

    /**
     * Test: Create a client using v2 API, then assert/read using v1 API.
     */
    @Test
    public void createWithV2AssertWithV1() throws Exception {
        RealmResource realm = managedRealm.admin();

        OIDCClientRepresentation v2Client = new OIDCClientRepresentation();
        v2Client.setClientId("v2-created-client");
        v2Client.setDisplayName("V2 Created Client");
        v2Client.setDescription("Client created via v2 API");
        v2Client.setEnabled(true);
        v2Client.setAppUrl("http://localhost:4000");
        v2Client.setRedirectUris(Set.of("http://localhost:4000/*", "http://localhost:4001/*"));
        v2Client.setWebOrigins(Set.of("http://localhost:4000", "http://localhost:4001"));
        v2Client.setLoginFlows(Set.of(
            OIDCClientRepresentation.Flow.STANDARD, 
            OIDCClientRepresentation.Flow.DIRECT_GRANT
        ));
        OIDCClientRepresentation.Auth auth = new OIDCClientRepresentation.Auth();
        auth.setMethod("client-secret");
        auth.setSecret("v2-secret-456");
        v2Client.setAuth(auth);

        HttpPost createRequest = new HttpPost(getClientsApiUrl());
        setAuthHeader(createRequest, adminClient);
        createRequest.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        createRequest.setEntity(new StringEntity(mapper.writeValueAsString(v2Client)));

        try (var httpResponse = httpClient.execute(createRequest)) {
            assertThat(httpResponse.getStatusLine().getStatusCode(), is(201));
        }

        ClientRepresentation v1Client = realm.clients().findByClientId("v2-created-client").get(0);
        String clientUuid = v1Client.getId();
        ClientRepresentation fullV1Client = realm.clients().get(clientUuid).toRepresentation();

        HttpGet getRequest = new HttpGet(getClientsApiUrl() + "/v2-created-client");
        setAuthHeader(getRequest, adminClient);

        try (var httpResponse = httpClient.execute(getRequest)) {
            assertThat(httpResponse.getStatusLine().getStatusCode(), is(200));
            OIDCClientRepresentation fetchedV2Client = mapper.createParser(httpResponse.getEntity().getContent())
                    .readValueAs(OIDCClientRepresentation.class);

            ClientRepresentationComparator.ComparisonResult result = 
                ClientRepresentationComparator.compare(fullV1Client, fetchedV2Client);

            assertTrue(result.allMatch(), "V1 and V2 representations should match:\n" + result);
        } finally {
            realm.clients().get(clientUuid).remove();
        }

    }

    /**
     * Test: Create a client using v1 API, update it using v2 API, then assert using v1 API.
     */
    @Test
    public void updateWithV2AssertWithV1() throws Exception {
        RealmResource realm = managedRealm.admin();

        ClientRepresentation v1Client = new ClientRepresentation();
        v1Client.setClientId("update-test-client");
        v1Client.setName("Original Name");
        v1Client.setDescription("Original description");
        v1Client.setEnabled(true);
        v1Client.setPublicClient(false);
        v1Client.setProtocol("openid-connect");
        v1Client.setRedirectUris(Arrays.asList("http://localhost:5000/*"));
        v1Client.setStandardFlowEnabled(true);

        Response response = realm.clients().create(v1Client);
        String clientUuid = ApiUtil.getCreatedId(response);
        response.close();

        OIDCClientRepresentation v2Update = new OIDCClientRepresentation();
        v2Update.setClientId("update-test-client");
        v2Update.setDisplayName("Updated Name via V2");
        v2Update.setDescription("Updated description via V2 API");
        v2Update.setEnabled(true);
        v2Update.setAppUrl("http://localhost:5000");
        v2Update.setRedirectUris(Set.of("http://localhost:5000/*", "http://localhost:5001/*"));
        v2Update.setWebOrigins(Set.of("http://localhost:5000"));
        v2Update.setLoginFlows(Set.of(
            OIDCClientRepresentation.Flow.STANDARD,
            OIDCClientRepresentation.Flow.DIRECT_GRANT,
            OIDCClientRepresentation.Flow.SERVICE_ACCOUNT
        ));
        OIDCClientRepresentation.Auth auth = new OIDCClientRepresentation.Auth();
        auth.setMethod("client-secret");
        auth.setSecret("updated-secret");
        v2Update.setAuth(auth);

        HttpPut updateRequest = new HttpPut(getClientsApiUrl() + "/update-test-client");
        setAuthHeader(updateRequest, adminClient);
        updateRequest.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        updateRequest.setEntity(new StringEntity(mapper.writeValueAsString(v2Update)));

        try (var httpResponse = httpClient.execute(updateRequest)) {
            assertThat(httpResponse.getStatusLine().getStatusCode(), is(200));
        }

        ClientRepresentation updatedV1Client = realm.clients().get(clientUuid).toRepresentation();

        HttpGet getRequest = new HttpGet(getClientsApiUrl() + "/update-test-client");
        setAuthHeader(getRequest, adminClient);

        try (var httpResponse = httpClient.execute(getRequest)) {
            assertThat(httpResponse.getStatusLine().getStatusCode(), is(200));
            OIDCClientRepresentation fetchedV2Client = mapper.createParser(httpResponse.getEntity().getContent())
                    .readValueAs(OIDCClientRepresentation.class);

            ClientRepresentationComparator.ComparisonResult result = 
                ClientRepresentationComparator.compare(updatedV1Client, fetchedV2Client);

            assertTrue(result.allMatch(), "V1 and V2 representations should match after update:\n" + result);

            assertThat(updatedV1Client.getName(), is("Updated Name via V2"));
            assertThat(updatedV1Client.getDescription(), is("Updated description via V2 API"));
            assertThat(updatedV1Client.isServiceAccountsEnabled(), is(true));
        } finally {
            realm.clients().get(clientUuid).remove();
        }
}

    public static class ServerConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.CLIENT_ADMIN_API_V2);
        }
    }
}
