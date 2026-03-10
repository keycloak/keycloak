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
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.common.Profile;
import org.keycloak.representations.admin.v2.OIDCClientRepresentation;
import org.keycloak.representations.admin.v2.SAMLClientRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectHttpClient;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.admin.mapper.ClientRepresentationComparator;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.Test;

import static org.keycloak.protocol.saml.SamlConfigAttributes.SAML_ASSERTION_SIGNATURE;
import static org.keycloak.protocol.saml.SamlConfigAttributes.SAML_AUTHNSTATEMENT;
import static org.keycloak.protocol.saml.SamlConfigAttributes.SAML_CLIENT_SIGNATURE_ATTRIBUTE;
import static org.keycloak.protocol.saml.SamlConfigAttributes.SAML_FORCE_NAME_ID_FORMAT_ATTRIBUTE;
import static org.keycloak.protocol.saml.SamlConfigAttributes.SAML_FORCE_POST_BINDING;
import static org.keycloak.protocol.saml.SamlConfigAttributes.SAML_NAME_ID_FORMAT_ATTRIBUTE;
import static org.keycloak.protocol.saml.SamlConfigAttributes.SAML_SERVER_SIGNATURE;
import static org.keycloak.protocol.saml.SamlConfigAttributes.SAML_SIGNATURE_ALGORITHM;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
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

    /**
     * Test: Create a client using v1 API, then assert/read using v2 API.
     */
    @Test
    public void createWithV1AssertWithV2() throws Exception {
        RealmResource realm = adminClient.realm("master");

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
        RealmResource realm = adminClient.realm("master");

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
        RealmResource realm = adminClient.realm("master");

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

    /**
     * Test: Create a SAML client using v1 API, then assert/read using v2 API.
     */
    @Test
    public void createSamlWithV1AssertWithV2() throws Exception {
        RealmResource realm = adminClient.realm("master");

        ClientRepresentation v1Client = new ClientRepresentation();
        v1Client.setClientId("v1-saml-client");
        v1Client.setName("V1 SAML Client");
        v1Client.setDescription("SAML client created via v1 API");
        v1Client.setEnabled(true);
        v1Client.setProtocol("saml");
        v1Client.setBaseUrl("http://localhost:8000/saml");
        v1Client.setRedirectUris(Arrays.asList("http://localhost:8000/saml/*"));
        v1Client.setFrontchannelLogout(true);
        v1Client.setAttributes(java.util.Map.of(
            SAML_NAME_ID_FORMAT_ATTRIBUTE, "username",
            SAML_FORCE_NAME_ID_FORMAT_ATTRIBUTE, "true",
            SAML_AUTHNSTATEMENT, "true",
            SAML_SERVER_SIGNATURE, "true",
            SAML_ASSERTION_SIGNATURE, "false",
            SAML_CLIENT_SIGNATURE_ATTRIBUTE, "false",
            SAML_FORCE_POST_BINDING, "true",
            SAML_SIGNATURE_ALGORITHM, "RSA_SHA256"
        ));

        Response response = realm.clients().create(v1Client);
        String clientUuid = ApiUtil.getCreatedId(response);
        response.close();

        ClientRepresentation createdV1Client = realm.clients().get(clientUuid).toRepresentation();

        HttpGet getRequest = new HttpGet(getClientsApiUrl() + "/v1-saml-client");
        setAuthHeader(getRequest, adminClient);

        try (var httpResponse = httpClient.execute(getRequest)) {
            assertThat(httpResponse.getStatusLine().getStatusCode(), is(200));
            SAMLClientRepresentation v2Client = mapper.createParser(httpResponse.getEntity().getContent())
                    .readValueAs(SAMLClientRepresentation.class);

            ClientRepresentationComparator.ComparisonResult result =
                ClientRepresentationComparator.compare(createdV1Client, v2Client);

            assertTrue(result.allMatch(), "V1 and V2 SAML representations should match:\n" + result);
        } finally {
            realm.clients().get(clientUuid).remove();
        }
    }

    /**
     * Test: Create a SAML client using v2 API, then assert/read using v1 API.
     */
    @Test
    public void createSamlWithV2AssertWithV1() throws Exception {
        RealmResource realm = adminClient.realm("master");

        SAMLClientRepresentation v2Client = new SAMLClientRepresentation();
        v2Client.setClientId("v2-saml-client");
        v2Client.setDisplayName("V2 SAML Client");
        v2Client.setDescription("SAML client created via v2 API");
        v2Client.setEnabled(true);
        v2Client.setAppUrl("http://localhost:9000/saml");
        v2Client.setRedirectUris(Set.of("http://localhost:9000/saml/*"));
        v2Client.setFrontChannelLogout(true);
        v2Client.setNameIdFormat("email");
        v2Client.setForceNameIdFormat(true);
        v2Client.setIncludeAuthnStatement(true);
        v2Client.setSignDocuments(true);
        v2Client.setSignAssertions(true);
        v2Client.setClientSignatureRequired(false);
        v2Client.setForcePostBinding(true);
        v2Client.setSignatureAlgorithm("RSA_SHA256");

        HttpPost createRequest = new HttpPost(getClientsApiUrl());
        setAuthHeader(createRequest, adminClient);
        createRequest.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        createRequest.setEntity(new StringEntity(mapper.writeValueAsString(v2Client)));

        try (var httpResponse = httpClient.execute(createRequest)) {
            assertThat(httpResponse.getStatusLine().getStatusCode(), is(201));
        }

        ClientRepresentation v1Client = realm.clients().findByClientId("v2-saml-client").get(0);
        String clientUuid = v1Client.getId();
        ClientRepresentation fullV1Client = realm.clients().get(clientUuid).toRepresentation();

        HttpGet getRequest = new HttpGet(getClientsApiUrl() + "/v2-saml-client");
        setAuthHeader(getRequest, adminClient);

        try (var httpResponse = httpClient.execute(getRequest)) {
            assertThat(httpResponse.getStatusLine().getStatusCode(), is(200));
            SAMLClientRepresentation fetchedV2Client = mapper.createParser(httpResponse.getEntity().getContent())
                    .readValueAs(SAMLClientRepresentation.class);

            ClientRepresentationComparator.ComparisonResult result =
                ClientRepresentationComparator.compare(fullV1Client, fetchedV2Client);

            assertTrue(result.allMatch(), "V1 and V2 SAML representations should match:\n" + result);
        } finally {
            realm.clients().get(clientUuid).remove();
        }
    }

    /**
     * Test: Create a SAML client using v1 API, update it using v2 API, then assert using v1 API.
     */
    @Test
    public void updateSamlWithV2AssertWithV1() throws Exception {
        RealmResource realm = adminClient.realm("master");

        ClientRepresentation v1Client = new ClientRepresentation();
        v1Client.setClientId("update-saml-client");
        v1Client.setName("Original SAML Name");
        v1Client.setDescription("Original SAML description");
        v1Client.setEnabled(true);
        v1Client.setProtocol("saml");
        v1Client.setRedirectUris(Arrays.asList("http://localhost:7000/saml/*"));
        v1Client.setAttributes(java.util.Map.of(
            "saml.server.signature", "false",
            "saml.force.post.binding", "false"
        ));

        Response response = realm.clients().create(v1Client);
        String clientUuid = ApiUtil.getCreatedId(response);
        response.close();

        SAMLClientRepresentation v2Update = new SAMLClientRepresentation();
        v2Update.setClientId("update-saml-client");
        v2Update.setDisplayName("Updated SAML Name via V2");
        v2Update.setDescription("Updated SAML description via V2 API");
        v2Update.setEnabled(true);
        v2Update.setAppUrl("http://localhost:7000/saml");
        v2Update.setRedirectUris(Set.of("http://localhost:7000/saml/*", "http://localhost:7001/saml/*"));
        v2Update.setFrontChannelLogout(true);
        v2Update.setSignDocuments(true);
        v2Update.setSignAssertions(true);
        v2Update.setForcePostBinding(true);
        v2Update.setSignatureAlgorithm("RSA_SHA512");

        HttpPut updateRequest = new HttpPut(getClientsApiUrl() + "/update-saml-client");
        setAuthHeader(updateRequest, adminClient);
        updateRequest.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        updateRequest.setEntity(new StringEntity(mapper.writeValueAsString(v2Update)));

        try (var httpResponse = httpClient.execute(updateRequest)) {
            assertThat(httpResponse.getStatusLine().getStatusCode(), is(200));
        }

        ClientRepresentation updatedV1Client = realm.clients().get(clientUuid).toRepresentation();

        HttpGet getRequest = new HttpGet(getClientsApiUrl() + "/update-saml-client");
        setAuthHeader(getRequest, adminClient);

        try (var httpResponse = httpClient.execute(getRequest)) {
            assertThat(httpResponse.getStatusLine().getStatusCode(), is(200));
            SAMLClientRepresentation fetchedV2Client = mapper.createParser(httpResponse.getEntity().getContent())
                    .readValueAs(SAMLClientRepresentation.class);

            ClientRepresentationComparator.ComparisonResult result =
                ClientRepresentationComparator.compare(updatedV1Client, fetchedV2Client);

            assertTrue(result.allMatch(), "V1 and V2 SAML representations should match after update:\n" + result);

            assertThat(updatedV1Client.getName(), is("Updated SAML Name via V2"));
            assertThat(updatedV1Client.getDescription(), is("Updated SAML description via V2 API"));
            assertThat(updatedV1Client.getAttributes().get("saml.server.signature"), is("true"));
            assertThat(updatedV1Client.getAttributes().get("saml.signature.algorithm"), is("RSA_SHA512"));
        } finally {
            realm.clients().get(clientUuid).remove();
        }
    }

    /**
     * Test: Create a client with roles using v2 API, then assert roles using v1 API.
     */
    @Test
    public void createWithV2RolesAssertWithV1() throws Exception {
        RealmResource realm = adminClient.realm("master");

        OIDCClientRepresentation v2Client = new OIDCClientRepresentation();
        v2Client.setClientId("v2-client-with-roles");
        v2Client.setDisplayName("V2 Client With Roles");
        v2Client.setDescription("Client with roles created via v2 API");
        v2Client.setEnabled(true);
        v2Client.setRoles(Set.of("viewer", "editor", "admin"));
        v2Client.setRedirectUris(Set.of("http://localhost:3000/*"));
        v2Client.setLoginFlows(Set.of(OIDCClientRepresentation.Flow.STANDARD));

        HttpPost createRequest = new HttpPost(getClientsApiUrl());
        setAuthHeader(createRequest, adminClient);
        createRequest.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        createRequest.setEntity(new StringEntity(mapper.writeValueAsString(v2Client)));

        try (var httpResponse = httpClient.execute(createRequest)) {
            assertThat(httpResponse.getStatusLine().getStatusCode(), is(201));
        }

        ClientRepresentation v1Client = realm.clients().findByClientId("v2-client-with-roles").get(0);
        String clientUuid = v1Client.getId();

        try {
            List<RoleRepresentation> roles = realm.clients().get(clientUuid).roles().list();
            Set<String> roleNames = roles.stream()
                .map(RoleRepresentation::getName)
                .collect(Collectors.toSet());

            assertThat(roleNames, containsInAnyOrder("viewer", "editor", "admin"));

            HttpGet getRequest = new HttpGet(getClientsApiUrl() + "/v2-client-with-roles");
            setAuthHeader(getRequest, adminClient);

            try (var httpResponse = httpClient.execute(getRequest)) {
                assertThat(httpResponse.getStatusLine().getStatusCode(), is(200));
                OIDCClientRepresentation fetchedV2Client = mapper.createParser(httpResponse.getEntity().getContent())
                        .readValueAs(OIDCClientRepresentation.class);

                assertThat(fetchedV2Client.getRoles(), containsInAnyOrder("viewer", "editor", "admin"));
            }
        } finally {
            realm.clients().get(clientUuid).remove();
        }
    }

    /**
     * Test: Create a client using v1 API, add roles via v1 API, then assert roles using v2 API.
     */
    @Test
    public void createWithV1RolesAssertWithV2() throws Exception {
        RealmResource realm = adminClient.realm("master");

        ClientRepresentation v1Client = new ClientRepresentation();
        v1Client.setClientId("v1-client-with-roles");
        v1Client.setName("V1 Client With Roles");
        v1Client.setDescription("Client with roles created via v1 API");
        v1Client.setEnabled(true);
        v1Client.setProtocol("openid-connect");
        v1Client.setRedirectUris(Arrays.asList("http://localhost:4000/*"));
        v1Client.setStandardFlowEnabled(true);

        Response response = realm.clients().create(v1Client);
        String clientUuid = ApiUtil.getCreatedId(response);
        response.close();

        try {
            RoleRepresentation role1 = new RoleRepresentation();
            role1.setName("read-access");
            role1.setDescription("Read access role");
            realm.clients().get(clientUuid).roles().create(role1);

            RoleRepresentation role2 = new RoleRepresentation();
            role2.setName("write-access");
            role2.setDescription("Write access role");
            realm.clients().get(clientUuid).roles().create(role2);

            RoleRepresentation role3 = new RoleRepresentation();
            role3.setName("delete-access");
            realm.clients().get(clientUuid).roles().create(role3);

            HttpGet getRequest = new HttpGet(getClientsApiUrl() + "/v1-client-with-roles");
            setAuthHeader(getRequest, adminClient);

            try (var httpResponse = httpClient.execute(getRequest)) {
                assertThat(httpResponse.getStatusLine().getStatusCode(), is(200));
                OIDCClientRepresentation fetchedV2Client = mapper.createParser(httpResponse.getEntity().getContent())
                        .readValueAs(OIDCClientRepresentation.class);

                assertThat(fetchedV2Client.getRoles(), containsInAnyOrder("read-access", "write-access", "delete-access"));
            }
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
