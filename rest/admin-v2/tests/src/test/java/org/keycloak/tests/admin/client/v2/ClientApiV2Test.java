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

import java.util.List;
import java.util.Set;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;

import org.keycloak.admin.api.AdminApi;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.common.Profile;
import org.keycloak.representations.admin.v2.BaseClientRepresentation;
import org.keycloak.representations.admin.v2.OIDCClientRepresentation;
import org.keycloak.representations.admin.v2.SAMLClientRepresentation;
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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpMessage;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.keycloak.services.cors.Cors.ACCESS_CONTROL_ALLOW_METHODS;
import static org.keycloak.services.cors.Cors.ORIGIN_HEADER;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

@KeycloakIntegrationTest(config = ClientApiV2Test.AdminV2Config.class)
public class ClientApiV2Test {

    public static final String HOSTNAME_LOCAL_ADMIN = "http://localhost:8080/admin/api/master/clients/v2";
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
        HttpGet request = new HttpGet(HOSTNAME_LOCAL_ADMIN + "/account");
        setAuthHeader(request);
        try (var response = client.execute(request)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            OIDCClientRepresentation client = mapper.createParser(response.getEntity().getContent()).readValueAs(OIDCClientRepresentation.class);
            assertEquals("account", client.getClientId());
        }
    }

    @Test
    public void jsonPatchClient() throws Exception {
        HttpPatch request = new HttpPatch(HOSTNAME_LOCAL_ADMIN + "/account");
        setAuthHeader(request);
        request.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_PATCH_JSON);
        try (var response = client.execute(request)) {
            EntityUtils.consumeQuietly(response.getEntity());
            assertEquals(415, response.getStatusLine().getStatusCode());
        }
    }

    @Test
    public void jsonMergePatchClient() throws Exception {
        HttpPatch request = new HttpPatch(HOSTNAME_LOCAL_ADMIN + "/account");
        setAuthHeader(request);
        request.setHeader(HttpHeaders.CONTENT_TYPE, AdminApi.CONTENT_TYPE_MERGE_PATCH);

        OIDCClientRepresentation patch = new OIDCClientRepresentation();
        patch.setDescription("I'm also a description");

        request.setEntity(new StringEntity(mapper.writeValueAsString(patch)));

        try (var response = client.execute(request)) {
            assertEquals(200, response.getStatusLine().getStatusCode());

            OIDCClientRepresentation client = mapper.createParser(response.getEntity().getContent()).readValueAs(OIDCClientRepresentation.class);
            assertEquals("I'm also a description", client.getDescription());
        }
    }

    @Test
    public void putFailsWithDifferentClientId() throws Exception {
        HttpPut request = new HttpPut(HOSTNAME_LOCAL_ADMIN + "/account");
        setAuthHeader(request);
        request.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);

        OIDCClientRepresentation rep = new OIDCClientRepresentation();
        rep.setClientId("other");

        request.setEntity(new StringEntity(mapper.writeValueAsString(rep)));

        try (var response = client.execute(request)) {
            assertEquals(400, response.getStatusLine().getStatusCode());
        }
    }

    @Test
    public void putCreateOrUpdates() throws Exception {
        HttpPut request = new HttpPut(HOSTNAME_LOCAL_ADMIN + "/other");
        setAuthHeader(request);
        request.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);

        OIDCClientRepresentation rep = new OIDCClientRepresentation();
        rep.setEnabled(true);
        rep.setClientId("other");
        rep.setDescription("I'm new");

        request.setEntity(new StringEntity(mapper.writeValueAsString(rep)));

        try (var response = client.execute(request)) {
            assertEquals(201, response.getStatusLine().getStatusCode());
            OIDCClientRepresentation client = mapper.createParser(response.getEntity().getContent()).readValueAs(OIDCClientRepresentation.class);
            assertEquals("I'm new", client.getDescription());
        }

        rep.setDescription("I'm updated");
        request.setEntity(new StringEntity(mapper.writeValueAsString(rep)));

        try (var response = client.execute(request)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            OIDCClientRepresentation client = mapper.createParser(response.getEntity().getContent()).readValueAs(OIDCClientRepresentation.class);
            assertEquals("I'm updated", client.getDescription());
        }
    }

    @Test
    public void createClient() throws Exception {
        HttpPost request = new HttpPost(HOSTNAME_LOCAL_ADMIN);
        setAuthHeader(request);
        request.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);

        OIDCClientRepresentation rep = new OIDCClientRepresentation();
        rep.setEnabled(true);
        rep.setClientId("client-123");
        rep.setDescription("I'm new");

        request.setEntity(new StringEntity(mapper.writeValueAsString(rep)));

        try (var response = client.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(),is(201));
            OIDCClientRepresentation client = mapper.createParser(response.getEntity().getContent()).readValueAs(OIDCClientRepresentation.class);
            assertThat(client.getEnabled(),is(true));
            assertThat(client.getClientId(),is("client-123"));
            assertThat(client.getDescription(),is("I'm new"));
        }

        try (var response = client.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(),is(409));
        }
    }

    @Test
    public void deleteClient() throws Exception {
        HttpPut createRequest = new HttpPut(HOSTNAME_LOCAL_ADMIN + "/to-delete");
        setAuthHeader(createRequest);
        createRequest.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);

        OIDCClientRepresentation rep = new OIDCClientRepresentation();
        rep.setClientId("to-delete");
        rep.setEnabled(true);

        createRequest.setEntity(new StringEntity(mapper.writeValueAsString(rep)));

        try (var response = client.execute(createRequest)) {
            assertEquals(201, response.getStatusLine().getStatusCode());
        }

        HttpGet getRequest = new HttpGet(HOSTNAME_LOCAL_ADMIN + "/to-delete");
        setAuthHeader(getRequest);
        try (var response = client.execute(getRequest)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
        }

        HttpDelete deleteRequest = new HttpDelete(HOSTNAME_LOCAL_ADMIN + "/to-delete");
        setAuthHeader(deleteRequest);
        try (var response = client.execute(deleteRequest)) {
            assertEquals(204, response.getStatusLine().getStatusCode());
        }

        try (var response = client.execute(getRequest)) {
            assertEquals(404, response.getStatusLine().getStatusCode());
        }
    }

    @Test
    public void getClientsMixedProtocols() throws Exception {
        // Create an OIDC client with OIDC-specific fields
        HttpPost oidcRequest = new HttpPost(HOSTNAME_LOCAL_ADMIN);
        setAuthHeader(oidcRequest);
        oidcRequest.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);

        OIDCClientRepresentation oidcRep = new OIDCClientRepresentation();
        oidcRep.setEnabled(true);
        oidcRep.setClientId("mixed-test-oidc");
        oidcRep.setDescription("OIDC client for mixed protocol test");
        // OIDC-specific fields
        oidcRep.setLoginFlows(Set.of(OIDCClientRepresentation.Flow.STANDARD, OIDCClientRepresentation.Flow.DIRECT_GRANT));
        oidcRep.setWebOrigins(Set.of("http://localhost:3000", "http://localhost:4000"));

        oidcRequest.setEntity(new StringEntity(mapper.writeValueAsString(oidcRep)));

        try (var response = client.execute(oidcRequest)) {
            assertEquals(201, response.getStatusLine().getStatusCode());
        }

        // Create a SAML client with SAML-specific fields
        HttpPost samlRequest = new HttpPost(HOSTNAME_LOCAL_ADMIN);
        setAuthHeader(samlRequest);
        samlRequest.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);

        SAMLClientRepresentation samlRep = new SAMLClientRepresentation();
        samlRep.setEnabled(true);
        samlRep.setClientId("mixed-test-saml");
        samlRep.setDescription("SAML client for mixed protocol test");
        // SAML-specific fields
        samlRep.setNameIdFormat("email");
        samlRep.setSignDocuments(true);
        samlRep.setSignAssertions(true);
        samlRep.setForcePostBinding(true);
        samlRep.setFrontChannelLogout(false);

        samlRequest.setEntity(new StringEntity(mapper.writeValueAsString(samlRep)));

        try (var response = client.execute(samlRequest)) {
            assertEquals(201, response.getStatusLine().getStatusCode());
        }

        // Get all clients - this should work with mixed protocols
        HttpGet getRequest = new HttpGet(HOSTNAME_LOCAL_ADMIN);
        setAuthHeader(getRequest);

        try (var response = client.execute(getRequest)) {
            assertEquals(200, response.getStatusLine().getStatusCode());

            List<BaseClientRepresentation> clients = mapper.readValue(response.getEntity().getContent(),
                    new TypeReference<List<BaseClientRepresentation>>() {});

            // Verify OIDC client with protocol-specific fields
            OIDCClientRepresentation foundOidc = clients.stream()
                    .filter(c -> "mixed-test-oidc".equals(c.getClientId()) && c instanceof OIDCClientRepresentation)
                    .map(c -> (OIDCClientRepresentation) c)
                    .findFirst()
                    .orElse(null);

            assertThat("OIDC client should be in the list", foundOidc, is(notNullValue()));
            assertThat(foundOidc.getLoginFlows(), is(Set.of(OIDCClientRepresentation.Flow.STANDARD, OIDCClientRepresentation.Flow.DIRECT_GRANT)));
            assertThat(foundOidc.getWebOrigins(), is(Set.of("http://localhost:3000", "http://localhost:4000")));

            // Verify SAML client with protocol-specific fields
            SAMLClientRepresentation foundSaml = clients.stream()
                    .filter(c -> "mixed-test-saml".equals(c.getClientId()) && c instanceof SAMLClientRepresentation)
                    .map(c -> (SAMLClientRepresentation) c)
                    .findFirst()
                    .orElse(null);

            assertThat("SAML client should be in the list", foundSaml, is(notNullValue()));
            assertThat(foundSaml.getNameIdFormat(), is("email"));
            assertThat(foundSaml.getSignDocuments(), is(true));
            assertThat(foundSaml.getSignAssertions(), is(true));
            assertThat(foundSaml.getForcePostBinding(), is(true));
            assertThat(foundSaml.getFrontChannelLogout(), is(false));
        }

        // Get individual OIDC client and verify OIDC-specific fields
        HttpGet getOidcRequest = new HttpGet(HOSTNAME_LOCAL_ADMIN + "/mixed-test-oidc");
        setAuthHeader(getOidcRequest);

        try (var response = client.execute(getOidcRequest)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            OIDCClientRepresentation oidcClient = mapper.createParser(response.getEntity().getContent())
                    .readValueAs(OIDCClientRepresentation.class);
            assertEquals("mixed-test-oidc", oidcClient.getClientId());
            assertThat(oidcClient.getLoginFlows(), is(Set.of(OIDCClientRepresentation.Flow.STANDARD, OIDCClientRepresentation.Flow.DIRECT_GRANT)));
            assertThat(oidcClient.getWebOrigins(), is(Set.of("http://localhost:3000", "http://localhost:4000")));
        }

        // Get individual SAML client and verify SAML-specific fields
        HttpGet getSamlRequest = new HttpGet(HOSTNAME_LOCAL_ADMIN + "/mixed-test-saml");
        setAuthHeader(getSamlRequest);

        try (var response = client.execute(getSamlRequest)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            SAMLClientRepresentation samlClient = mapper.createParser(response.getEntity().getContent())
                    .readValueAs(SAMLClientRepresentation.class);
            assertEquals("mixed-test-saml", samlClient.getClientId());
            assertEquals("SAML client for mixed protocol test", samlClient.getDescription());
            assertThat(samlClient.getNameIdFormat(), is("email"));
            assertThat(samlClient.getSignDocuments(), is(true));
            assertThat(samlClient.getSignAssertions(), is(true));
            assertThat(samlClient.getForcePostBinding(), is(true));
            assertThat(samlClient.getFrontChannelLogout(), is(false));
        }

        // Cleanup
        HttpDelete deleteOidc = new HttpDelete(HOSTNAME_LOCAL_ADMIN + "/mixed-test-oidc");
        setAuthHeader(deleteOidc);
        try (var response = client.execute(deleteOidc)) {
            assertEquals(204, response.getStatusLine().getStatusCode());
        }

        HttpDelete deleteSaml = new HttpDelete(HOSTNAME_LOCAL_ADMIN + "/mixed-test-saml");
        setAuthHeader(deleteSaml);
        try (var response = client.execute(deleteSaml)) {
            assertEquals(204, response.getStatusLine().getStatusCode());
        }
    }

    @Test
    public void OIDCClientRepresentationValidation() throws Exception {
        HttpPost request = new HttpPost(HOSTNAME_LOCAL_ADMIN);
        setAuthHeader(request);
        request.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);

        request.setEntity(new StringEntity("""
                {
                    "protocol": "openid-connect",
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
                    "protocol": "openid-connect",
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
        HttpGet request = new HttpGet(HOSTNAME_LOCAL_ADMIN + "/account");
        setAuthHeader(request, noAccessAdminClient);
        try (var response = client.execute(request)) {
            assertEquals(401, response.getStatusLine().getStatusCode());
        }
    }

    @Test
    public void createFullClient() throws Exception {
        HttpPost request = new HttpPost(HOSTNAME_LOCAL_ADMIN);
        setAuthHeader(request);
        request.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);

        OIDCClientRepresentation rep = getTestingFullClientRep();
        request.setEntity(new StringEntity(mapper.writeValueAsString(rep)));

        try (var response = client.execute(request)) {
            assertEquals(201, response.getStatusLine().getStatusCode());
            OIDCClientRepresentation client = mapper.createParser(response.getEntity().getContent()).readValueAs(OIDCClientRepresentation.class);
            assertThat(client, is(rep));
        }
    }

    @Test
    public void createFullClientWrongServiceAccountRoles() throws Exception {
        HttpPost request = new HttpPost(HOSTNAME_LOCAL_ADMIN);
        setAuthHeader(request);
        request.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);

        OIDCClientRepresentation rep = getTestingFullClientRep();
        rep.setServiceAccountRoles(Set.of("non-existing", "bad-role"));
        request.setEntity(new StringEntity(mapper.writeValueAsString(rep)));

        try (var response = client.execute(request)) {
            assertEquals(400, response.getStatusLine().getStatusCode());
            assertThat(EntityUtils.toString(response.getEntity()), containsString("Cannot assign role to the service account (field 'serviceAccount.roles') as it does not exist"));
        }
    }

    @Test
    public void declarativeRoleManagement() throws Exception {
        // 1. Create a client with initial roles
        HttpPut createRequest = new HttpPut(HOSTNAME_LOCAL_ADMIN + "/declarative-role-test");
        setAuthHeader(createRequest);
        createRequest.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);

        OIDCClientRepresentation rep = new OIDCClientRepresentation();
        rep.setClientId("declarative-role-test");
        rep.setEnabled(true);
        rep.setRoles(Set.of("role1", "role2", "role3"));

        createRequest.setEntity(new StringEntity(mapper.writeValueAsString(rep)));

        try (var response = client.execute(createRequest)) {
            assertEquals(201, response.getStatusLine().getStatusCode());
            OIDCClientRepresentation created = mapper.createParser(response.getEntity().getContent()).readValueAs(OIDCClientRepresentation.class);
            assertThat(created.getRoles(), is(Set.of("role1", "role2", "role3")));
        }

        // 2. Update with completely new roles - should remove old ones and add new ones
        HttpPut updateRequest = new HttpPut(HOSTNAME_LOCAL_ADMIN + "/declarative-role-test");
        setAuthHeader(updateRequest);
        updateRequest.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);

        rep.setRoles(Set.of("new-role1", "new-role2"));
        updateRequest.setEntity(new StringEntity(mapper.writeValueAsString(rep)));

        try (var response = client.execute(updateRequest)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            OIDCClientRepresentation updated = mapper.createParser(response.getEntity().getContent()).readValueAs(OIDCClientRepresentation.class);
            assertThat(updated.getRoles(), is(Set.of("new-role1", "new-role2")));
        }

        // 3. Update with partial overlap - keep some, add some, remove some
        rep.setRoles(Set.of("new-role1", "add-role3", "add-role4"));
        updateRequest.setEntity(new StringEntity(mapper.writeValueAsString(rep)));

        try (var response = client.execute(updateRequest)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            OIDCClientRepresentation updated = mapper.createParser(response.getEntity().getContent()).readValueAs(OIDCClientRepresentation.class);
            assertThat(updated.getRoles(), is(Set.of("new-role1", "add-role3", "add-role4")));
        }

        // 4. Update with same roles - should be idempotent
        rep.setRoles(Set.of("new-role1", "add-role3", "add-role4"));
        updateRequest.setEntity(new StringEntity(mapper.writeValueAsString(rep)));

        try (var response = client.execute(updateRequest)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            OIDCClientRepresentation updated = mapper.createParser(response.getEntity().getContent()).readValueAs(OIDCClientRepresentation.class);
            assertThat(updated.getRoles(), is(Set.of("new-role1", "add-role3", "add-role4")));
        }

        // 5. Update with empty set - should remove all roles
        rep.setRoles(Set.of());
        updateRequest.setEntity(new StringEntity(mapper.writeValueAsString(rep)));

        try (var response = client.execute(updateRequest)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            OIDCClientRepresentation updated = mapper.createParser(response.getEntity().getContent()).readValueAs(OIDCClientRepresentation.class);
            assertThat(updated.getRoles(), is(Set.of()));
        }
    }

    @Test
    public void declarativeServiceAccountRoleManagement() throws Exception {
        // 1. Create a client with service account and initial realm roles
        HttpPut createRequest = new HttpPut(HOSTNAME_LOCAL_ADMIN + "/sa-declarative-test");
        setAuthHeader(createRequest);
        createRequest.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);

        OIDCClientRepresentation rep = new OIDCClientRepresentation();
        rep.setClientId("sa-declarative-test");
        rep.setEnabled(true);

        rep.setLoginFlows(Set.of(OIDCClientRepresentation.Flow.SERVICE_ACCOUNT));
        rep.setServiceAccountRoles(Set.of("default-roles-master", "offline_access"));

        createRequest.setEntity(new StringEntity(mapper.writeValueAsString(rep)));

        try (var response = client.execute(createRequest)) {
            assertEquals(201, response.getStatusLine().getStatusCode());
            OIDCClientRepresentation created = mapper.createParser(response.getEntity().getContent()).readValueAs(OIDCClientRepresentation.class);
            assertThat(created.getServiceAccountRoles(), is(Set.of("default-roles-master", "offline_access")));
        }

        // 2. Update with completely new roles - should remove old ones and add new ones
        HttpPut updateRequest = new HttpPut(HOSTNAME_LOCAL_ADMIN + "/sa-declarative-test");
        setAuthHeader(updateRequest);
        updateRequest.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);

        rep.setServiceAccountRoles(Set.of("uma_authorization", "offline_access"));
        updateRequest.setEntity(new StringEntity(mapper.writeValueAsString(rep)));

        try (var response = client.execute(updateRequest)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            OIDCClientRepresentation updated = mapper.createParser(response.getEntity().getContent()).readValueAs(OIDCClientRepresentation.class);
            assertThat(updated.getServiceAccountRoles(), is(Set.of("uma_authorization", "offline_access")));
        }

        // 3. Update with partial overlap - keep some, add some, remove some
        rep.setServiceAccountRoles(Set.of("offline_access", "default-roles-master"));
        updateRequest.setEntity(new StringEntity(mapper.writeValueAsString(rep)));

        try (var response = client.execute(updateRequest)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            OIDCClientRepresentation updated = mapper.createParser(response.getEntity().getContent()).readValueAs(OIDCClientRepresentation.class);
            assertThat(updated.getServiceAccountRoles(), is(Set.of("offline_access", "default-roles-master")));
        }

        // 4. Update with same roles - should be idempotent
        rep.setServiceAccountRoles(Set.of("offline_access", "default-roles-master"));
        updateRequest.setEntity(new StringEntity(mapper.writeValueAsString(rep)));

        try (var response = client.execute(updateRequest)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            OIDCClientRepresentation updated = mapper.createParser(response.getEntity().getContent()).readValueAs(OIDCClientRepresentation.class);
            assertThat(updated.getServiceAccountRoles(), is(Set.of("offline_access", "default-roles-master")));
        }

        // 5. Update with empty set - should remove all roles
        rep.setServiceAccountRoles(Set.of());
        updateRequest.setEntity(new StringEntity(mapper.writeValueAsString(rep)));

        try (var response = client.execute(updateRequest)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            OIDCClientRepresentation updated = mapper.createParser(response.getEntity().getContent()).readValueAs(OIDCClientRepresentation.class);
            assertThat(updated.getServiceAccountRoles(), is(Set.of()));
        }
    }

    @Test
    public void versionedClientsApi() throws Exception {
        final var ADMIN_API_URL = "http://localhost:8080/admin/api/master";

        // no version specified - default
        HttpGet request = new HttpGet(ADMIN_API_URL + "/clients");
        setAuthHeader(request);
        try (var response = client.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(), is(405)); // 405 for now due to the preflight check (needs to be fixed)
        }

        // v2 specified
        request = new HttpGet(ADMIN_API_URL + "/clients/v2");
        setAuthHeader(request);
        try (var response = client.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(), is(200));
            EntityUtils.consumeQuietly(response.getEntity());
        }

        // unknown version
        request = new HttpGet(ADMIN_API_URL + "/clients/v3");
        setAuthHeader(request);
        try (var response = client.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(), is(404));
        }

        // invalid version
        request = new HttpGet(ADMIN_API_URL + "/clients/4");
        setAuthHeader(request);
        try (var response = client.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(), is(405)); // 405 for now due to the preflight check (needs to be fixed)
        }
    }

    @Test
    public void preflight() throws Exception {
        HttpOptions request = new HttpOptions(HOSTNAME_LOCAL_ADMIN);
        request.setHeader(ORIGIN_HEADER, "http://localhost:8080");

        // we can improve preflight logic in follow-up issues
        try (var response = client.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(), is(200));
            var header = response.getFirstHeader(ACCESS_CONTROL_ALLOW_METHODS);
            assertThat(header, notNullValue());
            assertThat(header.getValue(), is("DELETE, POST, GET, PUT"));
        }
    }

    private OIDCClientRepresentation getTestingFullClientRep() {
        var rep = new OIDCClientRepresentation();
        rep.setClientId("my-client");
        rep.setDisplayName("My Client");
        rep.setDescription("This is My Client");
        rep.setEnabled(true);
        rep.setAppUrl("http://localhost:3000");
        rep.setRedirectUris(Set.of("http://localhost:3000", "http://localhost:3001"));
        var auth = new OIDCClientRepresentation.Auth();
        auth.setMethod("client-jwt");
        auth.setSecret("secret-1234");
        // no certificate inside the old rep
        // auth.setCertificate("certificate-5678");
        rep.setAuth(auth);
        rep.setWebOrigins(Set.of("http://localhost:4000", "http://localhost:4001"));
        rep.setRoles(Set.of("view-consent", "manage-account"));
        rep.setLoginFlows(Set.of(OIDCClientRepresentation.Flow.SERVICE_ACCOUNT));
        // TODO when roles are not set and SA is enabled, the default role 'default-roles-master' for the SA is used for the master realm
        rep.setServiceAccountRoles(Set.of("default-roles-master"));
        // not implemented yet
        // rep.setAdditionalFields(Map.of("key1", "val1", "key2", "val2"));
        return rep;
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
