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

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;

import org.keycloak.admin.api.client.ClientApi;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.common.Profile;
import org.keycloak.representations.admin.v2.ClientRepresentation;
import org.keycloak.services.error.ViolationExceptionResponse;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectHttpClient;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RealmConfigBuilder;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpMessage;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

@KeycloakIntegrationTest(config = ClientApiV2Test.AdminV2Config.class)
public class ClientApiV2Test {

    public static final String HOSTNAME_LOCAL_ADMIN = "http://localhost:8080/admin/api/v2";
    private static ObjectMapper mapper;

    @InjectHttpClient
    CloseableHttpClient client;

    @InjectAdminClient
    Keycloak adminClient;

    @InjectRealm(config = NoAccessRealmConfig.class)
    ManagedRealm testRealm;

    @InjectAdminClient(ref = "noAccessClient", client = "myclient", mode = InjectAdminClient.Mode.MANAGED_REALM)
    Keycloak noAccessAdminClient;

    @BeforeAll
    public static void setupMapper() {
        mapper = new ObjectMapper();
    }

    @Test
    public void getClient() throws Exception {
        HttpGet request = new HttpGet(HOSTNAME_LOCAL_ADMIN + "/realms/master/clients/account");
        setAuthHeader(request);
        try (var response = client.execute(request)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            ClientRepresentation client = mapper.createParser(response.getEntity().getContent()).readValueAs(ClientRepresentation.class);
            assertEquals("account", client.getClientId());
        }
    }

    @Test
    public void jsonPatchClient() throws Exception {
        HttpPatch request = new HttpPatch(HOSTNAME_LOCAL_ADMIN + "/realms/master/clients/account");
        setAuthHeader(request);
        request.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_PATCH_JSON);
        try (var response = client.execute(request)) {
            EntityUtils.consumeQuietly(response.getEntity());
            assertEquals(415, response.getStatusLine().getStatusCode());
        }
    }

    @Test
    public void jsonMergePatchClient() throws Exception {
        HttpPatch request = new HttpPatch(HOSTNAME_LOCAL_ADMIN + "/realms/master/clients/account");
        setAuthHeader(request);
        request.setHeader(HttpHeaders.CONTENT_TYPE, ClientApi.CONTENT_TYPE_MERGE_PATCH);

        ClientRepresentation patch = new ClientRepresentation();
        patch.setDescription("I'm also a description");

        request.setEntity(new StringEntity(mapper.writeValueAsString(patch)));

        try (var response = client.execute(request)) {
            assertEquals(200, response.getStatusLine().getStatusCode());

            ClientRepresentation client = mapper.createParser(response.getEntity().getContent()).readValueAs(ClientRepresentation.class);
            assertEquals("I'm also a description", client.getDescription());
        }
    }

    @Test
    public void putFailsWithDifferentClientId() throws Exception {
        HttpPut request = new HttpPut(HOSTNAME_LOCAL_ADMIN + "/realms/master/clients/account");
        setAuthHeader(request);
        request.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);

        ClientRepresentation rep = new ClientRepresentation();
        rep.setClientId("other");

        request.setEntity(new StringEntity(mapper.writeValueAsString(rep)));

        try (var response = client.execute(request)) {
            assertEquals(400, response.getStatusLine().getStatusCode());
        }
    }

    @Test
    public void putCreateOrUpdates() throws Exception {
        HttpPut request = new HttpPut(HOSTNAME_LOCAL_ADMIN + "/realms/master/clients/other");
        setAuthHeader(request);
        request.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);

        ClientRepresentation rep = new ClientRepresentation();
        rep.setEnabled(true);
        rep.setClientId("other");
        rep.setDescription("I'm new");

        request.setEntity(new StringEntity(mapper.writeValueAsString(rep)));

        try (var response = client.execute(request)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            ClientRepresentation client = mapper.createParser(response.getEntity().getContent()).readValueAs(ClientRepresentation.class);
            assertEquals("I'm new", client.getDescription());
        }

        rep.setDescription("I'm updated");
        request.setEntity(new StringEntity(mapper.writeValueAsString(rep)));

        try (var response = client.execute(request)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            ClientRepresentation client = mapper.createParser(response.getEntity().getContent()).readValueAs(ClientRepresentation.class);
            assertEquals("I'm updated", client.getDescription());
        }
    }

    @Test
    public void deleteClient() throws Exception {
        HttpPut createRequest = new HttpPut(HOSTNAME_LOCAL_ADMIN + "/realms/master/clients/to-delete");
        setAuthHeader(createRequest);
        createRequest.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);

        ClientRepresentation rep = new ClientRepresentation();
        rep.setClientId("to-delete");
        rep.setEnabled(true);

        createRequest.setEntity(new StringEntity(mapper.writeValueAsString(rep)));

        try (var response = client.execute(createRequest)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
        }

        HttpGet getRequest = new HttpGet(HOSTNAME_LOCAL_ADMIN + "/realms/master/clients/to-delete");
        setAuthHeader(getRequest);
        try (var response = client.execute(getRequest)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
        }

        HttpDelete deleteRequest = new HttpDelete(HOSTNAME_LOCAL_ADMIN + "/realms/master/clients/to-delete");
        setAuthHeader(deleteRequest);
        try (var response = client.execute(deleteRequest)) {
            assertEquals(204, response.getStatusLine().getStatusCode());
        }

        try (var response = client.execute(getRequest)) {
            assertEquals(404, response.getStatusLine().getStatusCode());
        }
    }

    @Test
    public void clientRepresentationValidation() throws Exception {
        HttpPost request = new HttpPost(HOSTNAME_LOCAL_ADMIN + "/realms/master/clients");
        setAuthHeader(request);
        request.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);

        request.setEntity(new StringEntity("""
                {
                    "displayName": "something",
                    "appUrl": "notUrl"
                }
                """));

        try (var response = client.execute(request)) {
            assertThat(response, notNullValue());
            assertThat(response.getStatusLine().getStatusCode(), is(400));

            var body = mapper.createParser(response.getEntity().getContent()).readValueAs(ViolationExceptionResponse.class);
            assertThat(body.error(), is("Provided data is invalid"));
            var violations = body.violations();
            assertThat(violations.size(), is(1));
            assertThat(violations.iterator().next(), is("clientId: must not be blank"));
        }

        request.setEntity(new StringEntity("""
                {
                    "clientId": "some-client",
                    "displayName": "something",
                    "appUrl": "notUrl",
                    "auth": {
                        "method":"missing-enabled"
                    }
                }
                """));

        try (var response = client.execute(request)) {
            assertThat(response, notNullValue());
            assertThat(response.getStatusLine().getStatusCode(), is(400));
            var body = mapper.createParser(response.getEntity().getContent()).readValueAs(ViolationExceptionResponse.class);
            assertThat(body.error(), is("Provided data is invalid"));
            var violations = body.violations();
            assertThat(violations.size(), is(1));
            assertThat(violations.iterator().next(), is("appUrl: must be a valid URL"));
        }
    }

    @Test
    public void authenticationRequired() throws Exception {
        HttpGet request = new HttpGet(HOSTNAME_LOCAL_ADMIN + "/realms/master/clients/account");
        setAuthHeader(request, noAccessAdminClient);
        try (var response = client.execute(request)) {
            assertEquals(401, response.getStatusLine().getStatusCode());
        }
    }

    // TODO Rewrite the tests to not need explicit auth. They should use the admin client directly.
    private void setAuthHeader(HttpMessage request, Keycloak adminClient) {
        String token = adminClient.tokenManager().getAccessTokenString();
        request.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    private void setAuthHeader(HttpMessage request) {
        setAuthHeader(request, this.adminClient);
    }

    public static class AdminV2Config implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.CLIENT_ADMIN_API_V2);
        }
    }

    public static class NoAccessRealmConfig implements RealmConfig {

        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder realm) {
            realm.addClient("myclient")
                    .secret("mysecret")
                    .serviceAccountsEnabled(true);
            return realm;
        }
    }
}
