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

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.authentication.authenticators.client.ClientIdAndSecretAuthenticator;
import org.keycloak.authentication.authenticators.client.JWTClientAuthenticator;
import org.keycloak.authentication.authenticators.client.JWTClientSecretAuthenticator;
import org.keycloak.common.Profile;
import org.keycloak.representations.admin.v2.BaseClientRepresentation;
import org.keycloak.representations.admin.v2.OIDCClientRepresentation;
import org.keycloak.representations.admin.v2.SAMLClientRepresentation;
import org.keycloak.services.PatchTypeNames;
import org.keycloak.services.error.ViolationExceptionResponse;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectClient;
import org.keycloak.testframework.annotations.InjectHttpClient;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RealmConfigBuilder;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.keycloak.services.cors.Cors.ACCESS_CONTROL_ALLOW_METHODS;
import static org.keycloak.services.cors.Cors.ORIGIN_HEADER;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest(config = ClientApiV2Test.AdminV2Config.class)
public class ClientApiV2Test extends AbstractClientApiV2Test{

    @InjectHttpClient
    CloseableHttpClient client;

    @InjectRealm(config = NoAccessRealmConfig.class)
    ManagedRealm testRealm;

    @InjectRealm(attachTo = "master", ref = "master")
    ManagedRealm masterRealm;

    @InjectAdminClient(ref = "noAccessClient", client = "myclient", mode = InjectAdminClient.Mode.MANAGED_REALM)
    Keycloak noAccessAdminClient;

    @InjectClient(realmRef = "master")
    ManagedClient testClient;

    @Test
    public void getClient() {
        var client = adminClient.clients(testRealm.getName()).v2().client("account").getClient();
        assertEquals("account", client.getClientId());
    }

    @Test
    public void jsonPatchClient() throws Exception {
        HttpPatch request = new HttpPatch(getClientApiUrl(testClient.getClientId()));
        setAuthHeader(request);
        request.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_PATCH_JSON);
        try (var response = client.execute(request)) {
            EntityUtils.consumeQuietly(response.getEntity());
            assertEquals(415, response.getStatusLine().getStatusCode());
        }
    }

    @Test
    public void jsonMergePatchClient() {
        OIDCClientRepresentation patch = new OIDCClientRepresentation();
        patch.setDescription("I'm also a description");
        BaseClientRepresentation baseRep = adminClient.clients(masterRealm.getName()).v2().client(testClient.getClientId()).patchClient(mapper.valueToTree(patch));
        assertEquals("I'm also a description", baseRep.getDescription());
    }

    @Test
    public void jsonMergePatchClientInvalid() throws Exception {
        HttpPatch request = new HttpPatch(getClientApiUrl(testClient.getClientId()));
        setAuthHeader(request);
        request.setHeader(HttpHeaders.CONTENT_TYPE, PatchTypeNames.JSON_MERGE);

        request.setEntity(new StringEntity("patch client invalid"));
        try (var response = client.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(),is(400));
        }

        request.setEntity(new StringEntity("{\"invalid\":\"nothing\"}"));
        try (var response = client.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(),is(400));
        }

        request.setEntity(new StringEntity("{}"));
        try (var response = client.execute(request)) {
            EntityUtils.consumeQuietly(response.getEntity());
            assertEquals(200, response.getStatusLine().getStatusCode());
        }

        request.setEntity(new StringEntity(""));
        try (var response = client.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(),is(400));
            assertThat(EntityUtils.toString(response.getEntity()), Matchers.containsString("Cannot replace client resource with null"));
        }
    }

    @Test
    public void putFailsWithDifferentClientId() {
        OIDCClientRepresentation rep = new OIDCClientRepresentation();
        rep.setClientId("other");

        try (var response = adminClient.clients(testRealm.getName()).v2().client("account").createOrUpdateClient(rep)) {
            assertEquals(400, response.getStatus());
        }
    }

    @Test
    public void putCreateOrUpdates() {
        var clientId = "other";
        OIDCClientRepresentation rep = new OIDCClientRepresentation();
        rep.setEnabled(true);
        rep.setClientId(clientId);
        rep.setDescription("I'm new");

        try (var response = adminClient.clients(testRealm.getName()).v2().client(clientId).createOrUpdateClient(rep)) {
            assertEquals(201, response.getStatus());
            OIDCClientRepresentation client = response.readEntity(OIDCClientRepresentation.class);
            assertEquals("I'm new", client.getDescription());
            assertClientUuid(client);
        }

        rep.setDescription("I'm updated");

        try (var response = adminClient.clients(testRealm.getName()).v2().client(clientId).createOrUpdateClient(rep)) {
            assertEquals(200, response.getStatus());
            OIDCClientRepresentation client = response.readEntity(OIDCClientRepresentation.class);
            assertEquals("I'm updated", client.getDescription());
            assertClientUuid(client);
        }
    }

    @Test
    public void createClient() {
        var clientId = "client-123";
        OIDCClientRepresentation rep = new OIDCClientRepresentation();
        rep.setEnabled(true);
        rep.setClientId(clientId);
        rep.setDescription("I'm new");

        try (var response = adminClient.clients(testRealm.getName()).v2().createClient(rep)) {
            assertThat(response.getStatus(), is(201));
            OIDCClientRepresentation client = response.readEntity(OIDCClientRepresentation.class);
            assertThat(client.getEnabled(), is(true));
            assertThat(client.getClientId(), is("client-123"));
            assertThat(client.getDescription(), is("I'm new"));
        }

        try (var response = adminClient.clients(testRealm.getName()).v2().createClient(rep)) {
            assertThat(response.getStatus(), is(409));
        }
    }

    @Test
    public void deleteClient() {
        var clientIdToDelete = "to-delete";
        OIDCClientRepresentation rep = new OIDCClientRepresentation();
        rep.setClientId(clientIdToDelete);
        rep.setEnabled(true);

        try (var response = adminClient.clients(testRealm.getName()).v2().createClient(rep)) {
            assertEquals(201, response.getStatus());
        }


        var baseClientRepresentation = adminClient.clients(testRealm.getName()).v2().client(clientIdToDelete).getClient();
        assertEquals(clientIdToDelete, baseClientRepresentation.getClientId());

        try (var response = adminClient.clients(testRealm.getName()).v2().client(clientIdToDelete).deleteClient()) {
            assertEquals(204, response.getStatus());
        }

        NotFoundException exception = assertThrows(
            NotFoundException.class,
            () -> adminClient.clients(testRealm.getName()).v2().client(clientIdToDelete).getClient());

        assertTrue(exception.getMessage().contains("HTTP 404 Not Found"));
    }

    @Test
    public void getClientsMixedProtocols() throws JsonProcessingException {
        // Create an OIDC client with OIDC-specific fields
        OIDCClientRepresentation oidcRep = new OIDCClientRepresentation();
        oidcRep.setEnabled(true);
        oidcRep.setClientId("mixed-test-oidc");
        oidcRep.setDescription("OIDC client for mixed protocol test");
        // OIDC-specific fields
        oidcRep.setLoginFlows(Set.of(OIDCClientRepresentation.Flow.STANDARD, OIDCClientRepresentation.Flow.DIRECT_GRANT));
        oidcRep.setWebOrigins(Set.of("http://localhost:3000", "http://localhost:4000"));

        try (var response = adminClient.clients(testRealm.getName()).v2().createClient(oidcRep)) {
            assertEquals(201, response.getStatus());
            OIDCClientRepresentation created = response.readEntity(OIDCClientRepresentation.class);
            assertThat(created, notNullValue());
            masterRealm.cleanup().add(realm -> realm.clients().delete(created.getUuid()));
        }

        // Create a SAML client with SAML-specific fields
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

        try (var response = adminClient.clients(testRealm.getName()).v2().createClient(samlRep)) {
            assertEquals(201, response.getStatus());
            SAMLClientRepresentation created = response.readEntity(SAMLClientRepresentation.class);
            assertThat(created, notNullValue());
            masterRealm.cleanup().add(realm -> realm.clients().delete(created.getUuid()));
        }

        // Get all clients - this should work with mixed protocols
        try (Stream<BaseClientRepresentation> baseClientRepresentationStream = adminClient.clients(testRealm.getName()).v2().getClients()) {
            List<BaseClientRepresentation> clients = baseClientRepresentationStream.toList();

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
        OIDCClientRepresentation oidcClient = (OIDCClientRepresentation) adminClient.clients(testRealm.getName()).v2().client(oidcRep.getClientId()).getClient();
        assertEquals("mixed-test-oidc", oidcClient.getClientId());
        assertThat(oidcClient.getLoginFlows(), is(Set.of(OIDCClientRepresentation.Flow.STANDARD, OIDCClientRepresentation.Flow.DIRECT_GRANT)));
        assertThat(oidcClient.getWebOrigins(), is(Set.of("http://localhost:3000", "http://localhost:4000")));

        // Get individual SAML client and verify SAML-specific fields
        SAMLClientRepresentation samlClient = (SAMLClientRepresentation) adminClient.clients(testRealm.getName()).v2().client(samlRep.getClientId()).getClient();
        assertEquals("mixed-test-saml", samlClient.getClientId());
        assertEquals("SAML client for mixed protocol test", samlClient.getDescription());
        assertThat(samlClient.getNameIdFormat(), is("email"));
        assertThat(samlClient.getSignDocuments(), is(true));
        assertThat(samlClient.getSignAssertions(), is(true));
        assertThat(samlClient.getForcePostBinding(), is(true));
        assertThat(samlClient.getFrontChannelLogout(), is(false));
    }

    @Test
    public void OIDCClientRepresentationValidation() {
        OIDCClientRepresentation oidcRep = new OIDCClientRepresentation();
        oidcRep.setDisplayName("something");
        oidcRep.setAppUrl("notUrl");

        try (var response = adminClient.clients(testRealm.getName()).v2().createClient(oidcRep)) {
            assertThat(response, notNullValue());
            assertThat(response.getStatus(), is(400));

            var body = response.readEntity(ViolationExceptionResponse.class);
            assertThat(body.error(), is("Provided data is invalid"));
            var violations = body.violations();
            assertThat(violations, hasSize(2));
            assertThat(violations, hasItem("clientId: must not be blank"));
            assertThat(violations, hasItem("appUrl: must be a valid URL"));
        }

        oidcRep = new OIDCClientRepresentation();
        oidcRep.setClientId("some-client");
        oidcRep.setDisplayName("something");
        oidcRep.setAppUrl("notUrl");
        var auth = new OIDCClientRepresentation.Auth();
        auth.setMethod("missing-enabled");
        oidcRep.setAuth(auth);

        try (var response = adminClient.clients(testRealm.getName()).v2().createClient(oidcRep)) {
            assertThat(response, notNullValue());
            assertThat(response.getStatus(), is(400));
            var body = response.readEntity(ViolationExceptionResponse.class);
            assertThat(body.error(), is("Provided data is invalid"));
            var violations = body.violations();
            assertThat(violations.size(), is(1));
            assertThat(violations.iterator().next(), is("appUrl: must be a valid URL"));
        }
    }

    @Test
    public void authenticationRequired() throws Exception {
        HttpGet request = new HttpGet(getClientApiUrl(testClient.getClientId()));
        setAuthHeader(request, noAccessAdminClient);
        try (var response = client.execute(request)) {
            assertEquals(403, response.getStatusLine().getStatusCode());
        }
    }

    @Test
    public void createFullClient() {
        OIDCClientRepresentation rep = getTestingFullClientRep();

        try (var response = adminClient.clients(testRealm.getName()).v2().createClient(rep);) {
            assertEquals(201, response.getStatus());
            OIDCClientRepresentation client = response.readEntity(OIDCClientRepresentation.class);
            rep.setUuid(client.getUuid()); // needed for the use of equals()
            assertThat(client, is(rep));
        }
    }

    @Test
    public void createFullClientWrongServiceAccountRoles() {
        OIDCClientRepresentation rep = getTestingFullClientRep();
        rep.setServiceAccountRoles(Set.of("non-existing", "bad-role"));

        try (var response = adminClient.clients(testRealm.getName()).v2().createClient(rep);) {
            assertEquals(400, response.getStatus());
            assertThat(response.readEntity(String.class), containsString("Cannot assign role to the service account (field 'serviceAccount.roles') as it does not exist"));
        }
    }

    @Test
    public void declarativeRoleManagement() {
        // 1. Create a client with initial roles
        OIDCClientRepresentation rep = new OIDCClientRepresentation();
        rep.setClientId("declarative-role-test");
        rep.setEnabled(true);
        rep.setRoles(Set.of("role1", "role2", "role3"));

        try (var response = adminClient.clients(testRealm.getName()).v2().createClient(rep)) {
            assertEquals(201, response.getStatus());
            OIDCClientRepresentation created = response.readEntity(OIDCClientRepresentation.class);
            assertThat(created.getRoles(), is(Set.of("role1", "role2", "role3")));
        }

        // 2. Update with completely new roles - should remove old ones and add new ones
        rep.setRoles(Set.of("new-role1", "new-role2"));
        try (var response = adminClient.clients(testRealm.getName()).v2().client("declarative-role-test").createOrUpdateClient(rep)) {
            assertEquals(200, response.getStatus());
            OIDCClientRepresentation updated = response.readEntity(OIDCClientRepresentation.class);
            assertThat(updated.getRoles(), is(Set.of("new-role1", "new-role2")));
        }

        // 3. Update with partial overlap - keep some, add some, remove some
        rep.setRoles(Set.of("new-role1", "add-role3", "add-role4"));
        try (var response = adminClient.clients(testRealm.getName()).v2().client("declarative-role-test").createOrUpdateClient(rep)) {
            assertEquals(200, response.getStatus());
            OIDCClientRepresentation updated = response.readEntity(OIDCClientRepresentation.class);
            assertThat(updated.getRoles(), is(Set.of("new-role1", "add-role3", "add-role4")));
        }

        // 4. Update with same roles - should be idempotent
        rep.setRoles(Set.of("new-role1", "add-role3", "add-role4"));
        try (var response = adminClient.clients(testRealm.getName()).v2().client("declarative-role-test").createOrUpdateClient(rep)) {
            assertEquals(200, response.getStatus());
            OIDCClientRepresentation updated = response.readEntity(OIDCClientRepresentation.class);
            assertThat(updated.getRoles(), is(Set.of("new-role1", "add-role3", "add-role4")));
        }

        // 5. Update with empty set - should remove all roles
        rep.setRoles(Set.of());
        try (var response = adminClient.clients(testRealm.getName()).v2().client("declarative-role-test").createOrUpdateClient(rep)) {
            assertEquals(200, response.getStatus());
            OIDCClientRepresentation updated = response.readEntity(OIDCClientRepresentation.class);
            assertThat(updated.getRoles(), is(Set.of()));
        }
    }

    @Test
    public void declarativeServiceAccountRoleManagement() {
        // 1. Create a client with service account and initial realm roles
        String defaultRealmRoles = "default-roles-%s".formatted(testRealm.getName());
        OIDCClientRepresentation rep = new OIDCClientRepresentation();
        rep.setClientId("sa-declarative-test");
        rep.setEnabled(true);

        rep.setLoginFlows(Set.of(OIDCClientRepresentation.Flow.SERVICE_ACCOUNT));
        rep.setServiceAccountRoles(Set.of(defaultRealmRoles, "offline_access"));

        try (var response = adminClient.clients(testRealm.getName()).v2().createClient(rep)) {
            assertEquals(201, response.getStatus());
            OIDCClientRepresentation created = response.readEntity(OIDCClientRepresentation.class);
            assertThat(created.getServiceAccountRoles(), is(Set.of(defaultRealmRoles, "offline_access")));
        }

        // 2. Update with completely new roles - should remove old ones and add new ones
        rep.setServiceAccountRoles(Set.of("uma_authorization", "offline_access"));
        try (var response = adminClient.clients(testRealm.getName()).v2().client("sa-declarative-test").createOrUpdateClient(rep)) {
            assertEquals(200, response.getStatus());
            OIDCClientRepresentation updated = response.readEntity(OIDCClientRepresentation.class);
            assertThat(updated.getServiceAccountRoles(), is(Set.of("uma_authorization", "offline_access")));
        }

        // 3. Update with partial overlap - keep some, add some, remove some
        rep.setServiceAccountRoles(Set.of("offline_access", defaultRealmRoles));
        try (var response = adminClient.clients(testRealm.getName()).v2().client("sa-declarative-test").createOrUpdateClient(rep)) {
            assertEquals(200, response.getStatus());
            OIDCClientRepresentation updated = response.readEntity(OIDCClientRepresentation.class);
            assertThat(updated.getServiceAccountRoles(), is(Set.of("offline_access", defaultRealmRoles)));
        }

        // 4. Update with same roles - should be idempotent
        rep.setServiceAccountRoles(Set.of("offline_access", defaultRealmRoles));
        try (var response = adminClient.clients(testRealm.getName()).v2().client("sa-declarative-test").createOrUpdateClient(rep)) {
            assertEquals(200, response.getStatus());
            OIDCClientRepresentation updated = response.readEntity(OIDCClientRepresentation.class);
            assertThat(updated.getServiceAccountRoles(), is(Set.of("offline_access", defaultRealmRoles)));
        }

        // 5. Update with empty set - should remove all roles
        rep.setServiceAccountRoles(Set.of());
        try (var response = adminClient.clients(testRealm.getName()).v2().client("sa-declarative-test").createOrUpdateClient(rep)) {
            assertEquals(200, response.getStatus());
            OIDCClientRepresentation updated = response.readEntity(OIDCClientRepresentation.class);
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
        HttpOptions request = new HttpOptions(getClientsApiUrl());
        request.setHeader(ORIGIN_HEADER, "http://localhost:8080");

        // we can improve preflight logic in follow-up issues
        try (var response = client.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(), is(200));
            var header = response.getFirstHeader(ACCESS_CONTROL_ALLOW_METHODS);
            assertThat(header, notNullValue());
            assertThat(header.getValue(), is("DELETE, POST, GET, PUT"));
        }
    }

    @Test
    public void createClientWithInvalidRedirectUriFragment() throws Exception {
        OIDCClientRepresentation rep = new OIDCClientRepresentation();
        rep.setEnabled(true);
        rep.setClientId("client-invalid-fragment");
        rep.setRedirectUris(Set.of("http://localhost:3000#fragment"));
        assertClientCreationFailsWithError(rep, "Redirect URIs must not contain an URI fragment");
    }

    @Test
    public void createClientWithInvalidRedirectUriScheme() throws Exception {
        OIDCClientRepresentation rep = new OIDCClientRepresentation();
        rep.setEnabled(true);
        rep.setClientId("client-invalid-scheme");
        rep.setRedirectUris(Set.of("javascript:alert(1)"));
        assertClientCreationFailsWithError(rep, "Each redirect URL must be valid");
    }

    @Test
    @Disabled("Root URL fragment validation not yet implemented in V2 API")
    public void createClientWithInvalidRootUrl() throws Exception {
        OIDCClientRepresentation rep = new OIDCClientRepresentation();
        rep.setEnabled(true);
        rep.setClientId("client-invalid-root-url");
        rep.setAppUrl("http://localhost:3000#fragment");
        assertClientCreationFailsWithError(rep, "Root URL must not contain an URL fragment");
    }

    @Test
    public void createSamlClientWithInvalidRedirectUriFragment() throws Exception {
        SAMLClientRepresentation rep = new SAMLClientRepresentation();
        rep.setEnabled(true);
        rep.setClientId("saml-client-invalid-fragment");
        rep.setRedirectUris(Set.of("http://localhost:3000#fragment"));
        assertClientCreationFailsWithError(rep, "Redirect URIs must not contain an URI fragment");
    }

    @Test
    public void createSamlClientWithInvalidRedirectUriScheme() throws Exception {
        SAMLClientRepresentation rep = new SAMLClientRepresentation();
        rep.setEnabled(true);
        rep.setClientId("saml-client-invalid-scheme");
        rep.setRedirectUris(Set.of("javascript:alert(1)"));
        assertClientCreationFailsWithError(rep, "Each redirect URL must be valid");
    }

    @Test
    @Disabled("Root URL fragment validation not yet implemented in V2 API")
    public void createSamlClientWithInvalidRootUrl() throws Exception {
        SAMLClientRepresentation rep = new SAMLClientRepresentation();
        rep.setEnabled(true);
        rep.setClientId("saml-client-invalid-root-url");
        rep.setAppUrl("http://localhost:3000#fragment");
        assertClientCreationFailsWithError(rep, "Root URL must not contain an URL fragment");
    }

    @Test
    public void updateClientWithInvalidRedirectUriFragment() throws Exception {
        OIDCClientRepresentation rep = new OIDCClientRepresentation();
        rep.setEnabled(true);
        rep.setClientId("client-update-invalid-fragment");
        rep.setRedirectUris(Set.of("http://localhost:3000#fragment"));
        assertClientUpdateFailsWithError(rep, "Redirect URIs must not contain an URI fragment");
    }

    @Test
    public void updateClientWithInvalidRedirectUriScheme() throws Exception {
        OIDCClientRepresentation rep = new OIDCClientRepresentation();
        rep.setEnabled(true);
        rep.setClientId("client-update-invalid-scheme");
        rep.setRedirectUris(Set.of("javascript:alert(1)"));
        assertClientUpdateFailsWithError(rep, "Each redirect URL must be valid");
    }

    @Test
    @Disabled("Root URL fragment validation not yet implemented in V2 API")
    public void updateClientWithInvalidRootUrl() throws Exception {
        OIDCClientRepresentation rep = new OIDCClientRepresentation();
        rep.setEnabled(true);
        rep.setClientId("client-update-invalid-root-url");
        rep.setAppUrl("http://localhost:3000#fragment");
        assertClientUpdateFailsWithError(rep, "Root URL must not contain an URL fragment");
    }

    @Test
    public void updateSamlClientWithInvalidRedirectUriFragment() throws Exception {
        SAMLClientRepresentation rep = new SAMLClientRepresentation();
        rep.setEnabled(true);
        rep.setClientId("saml-client-update-invalid-fragment");
        rep.setRedirectUris(Set.of("http://localhost:3000#fragment"));
        assertClientUpdateFailsWithError(rep, "Redirect URIs must not contain an URI fragment");
    }

    @Test
    public void updateSamlClientWithInvalidRedirectUriScheme() throws Exception {
        SAMLClientRepresentation rep = new SAMLClientRepresentation();
        rep.setEnabled(true);
        rep.setClientId("saml-client-update-invalid-scheme");
        rep.setRedirectUris(Set.of("javascript:alert(1)"));
        assertClientUpdateFailsWithError(rep, "Each redirect URL must be valid");
    }

    @Test
    @Disabled("Root URL fragment validation not yet implemented in V2 API")
    public void updateSamlClientWithInvalidRootUrl() throws Exception {
        SAMLClientRepresentation rep = new SAMLClientRepresentation();
        rep.setEnabled(true);
        rep.setClientId("saml-client-update-invalid-root-url");
        rep.setAppUrl("http://localhost:3000#fragment");
        assertClientUpdateFailsWithError(rep, "Root URL must not contain an URL fragment");
    }

    /**
     * Asserts that client secret is generated when the secret field is not set.
     */
    @ParameterizedTest
    @ValueSource(strings = {ClientIdAndSecretAuthenticator.PROVIDER_ID, JWTClientSecretAuthenticator.PROVIDER_ID})
    void createClientWithPostAndGeneratedSecret(String authenticationMethod) {
        String clientId = authenticationMethod + "-generation-post";
        OIDCClientRepresentation client = new OIDCClientRepresentation();
        client.setClientId(clientId);
        OIDCClientRepresentation.Auth auth = new OIDCClientRepresentation.Auth();
        auth.setMethod(authenticationMethod);
        auth.setSecret(null);
        client.setAuth(auth);

        try (var response = adminClient.clients(testRealm.getName()).v2().createClient(client)) {
            var createdClient = response.readEntity(OIDCClientRepresentation.class);
            assertThat(createdClient.getAuth(), notNullValue());
            assertThat(createdClient.getAuth().getSecret(), Matchers.not(emptyOrNullString()));
        }

        // make sure that the created model was persisted and GET method returns the newly generated secret
        var persistedClient = (OIDCClientRepresentation) adminClient.clients(testRealm.getName()).v2().client(clientId).getClient();
        assertEquals(clientId, persistedClient.getClientId());
        assertThat(persistedClient.getAuth().getSecret(), Matchers.not(emptyOrNullString()));
    }

    /**
     * Asserts that the client secret is not generated for authentication methods other than the client secret.
     */
    @Test
    void createJwtClientWithoutSecret() throws IOException {
        String clientId = "jwt-client-generation-post";
        OIDCClientRepresentation.Auth auth = new OIDCClientRepresentation.Auth();
        auth.setMethod(JWTClientAuthenticator.PROVIDER_ID);
        auth.setSecret(null);

        OIDCClientRepresentation.Auth createdAuth = getResultingAuthConfigPost(auth, clientId);
        assertThat(createdAuth, notNullValue());
        assertThat(createdAuth.getSecret(), nullValue());
    }

    /**
     * Asserts that the client secret is generated when a public client is patched with the client secret method.
     */
    @ParameterizedTest
    @ValueSource(strings = { ClientIdAndSecretAuthenticator.PROVIDER_ID, JWTClientSecretAuthenticator.PROVIDER_ID })
    void patchedOriginallyPublicClientHasSecretGenerated(String authenticationMethod) throws IOException {
        String clientId = authenticationMethod + "-pub-generation-patch";

        OIDCClientRepresentation.Auth createdAuth = getResultingAuthConfigPost(null, clientId);
        assertThat(createdAuth, nullValue());

        OIDCClientRepresentation.Auth authWithoutSecret = new OIDCClientRepresentation.Auth();
        authWithoutSecret.setMethod(authenticationMethod);
        authWithoutSecret.setSecret(null);
        OIDCClientRepresentation.Auth patchedAuth = getResultingAuthConfigPatch(authWithoutSecret, clientId);
        assertThat(patchedAuth, notNullValue());
        String newlyGeneratedSecret = patchedAuth.getSecret();
        assertThat(newlyGeneratedSecret, not(emptyOrNullString()));
    }

    /**
     * Asserts that the client secret is generated when a client JWT is patched with the client secret method.
     */
    @ParameterizedTest
    @ValueSource(strings = { ClientIdAndSecretAuthenticator.PROVIDER_ID, JWTClientSecretAuthenticator.PROVIDER_ID })
    void patchedOriginallyJwtClientHasSecretGenerated(String authenticationMethod) throws IOException {
        String clientId = authenticationMethod + "-jwt-generation-patch";

        OIDCClientRepresentation.Auth auth = new OIDCClientRepresentation.Auth();
        auth.setMethod(JWTClientAuthenticator.PROVIDER_ID);
        auth.setSecret("hush-hush");

        OIDCClientRepresentation.Auth createdAuth = getResultingAuthConfigPost(auth, clientId);
        assertThat(createdAuth, notNullValue());
        assertThat(createdAuth.getSecret(), is(auth.getSecret()));

        OIDCClientRepresentation.Auth authWithoutSecret = new OIDCClientRepresentation.Auth();
        authWithoutSecret.setMethod(authenticationMethod);
        authWithoutSecret.setAdditionalField("secret", null);
        OIDCClientRepresentation.Auth patchedAuth = getResultingAuthConfigPatch(authWithoutSecret, clientId);
        assertThat(patchedAuth, notNullValue());
        String newlyGeneratedSecret = patchedAuth.getSecret();
        assertThat(newlyGeneratedSecret, not(is(createdAuth.getSecret())));
    }

    /**
     * Asserts that the client secret is regenerated when a client is patched with the empty secret field.
     */
    @ParameterizedTest
    @ValueSource(strings = { ClientIdAndSecretAuthenticator.PROVIDER_ID, JWTClientSecretAuthenticator.PROVIDER_ID })
    void patchedClientSecretIsRegenerated(String authenticationMethod) throws IOException {
        String clientId = authenticationMethod + "-re-generation-patch";

        OIDCClientRepresentation.Auth auth = new OIDCClientRepresentation.Auth();
        auth.setMethod(authenticationMethod);
        auth.setSecret("shush");

        OIDCClientRepresentation.Auth createdAuth = getResultingAuthConfigPost(auth, clientId);
        assertThat(createdAuth, notNullValue());
        assertThat(createdAuth.getSecret(), is(auth.getSecret()));

        OIDCClientRepresentation.Auth authWithoutSecret = new OIDCClientRepresentation.Auth();
        authWithoutSecret.setAdditionalField("secret", null);
        OIDCClientRepresentation.Auth patchedAuth = getResultingAuthConfigPatch(authWithoutSecret, clientId);
        assertThat(patchedAuth, notNullValue());
        String newlyGeneratedSecret = patchedAuth.getSecret();
        assertThat(newlyGeneratedSecret, not(is(createdAuth.getSecret())));
    }

    /**
     * Asserts that the confidential client is turned into public one when we explicitly set auth config field with null.
     */
    @ParameterizedTest
    @ValueSource(strings = { ClientIdAndSecretAuthenticator.PROVIDER_ID, JWTClientSecretAuthenticator.PROVIDER_ID })
    void patchTurnsConfidentialClientIntoPublicOne(String authenticationMethod) throws IOException {
        String clientId = authenticationMethod + "-patch-into-public-cl";

        OIDCClientRepresentation.Auth auth = new OIDCClientRepresentation.Auth();
        auth.setMethod(authenticationMethod);
        auth.setSecret("shush");

        OIDCClientRepresentation.Auth createdAuth = getResultingAuthConfigPost(auth, clientId);
        assertThat(createdAuth, notNullValue());
        assertThat(createdAuth.getSecret(), is(auth.getSecret()));

        OIDCClientRepresentation.Auth patchedAuth = getResultingAuthConfigPatch(null, clientId, "auth", null);
        assertThat(patchedAuth, nullValue());
    }

    @Test
    void patchAuthMethodAndAssertExistingSecretDidNotChange() throws IOException {
        String clientId = "patch-auth-method-switch";

        OIDCClientRepresentation.Auth auth = new OIDCClientRepresentation.Auth();
        auth.setMethod(ClientIdAndSecretAuthenticator.PROVIDER_ID);
        auth.setSecret("shush");

        OIDCClientRepresentation.Auth createdAuth = getResultingAuthConfigPost(auth, clientId);
        assertThat(createdAuth, notNullValue());
        assertThat(createdAuth.getMethod(), is(auth.getMethod()));
        assertThat(createdAuth.getSecret(), is(auth.getSecret()));

        // just change auth method and expect that the secret is still same
        OIDCClientRepresentation.Auth authWithoutSecret = new OIDCClientRepresentation.Auth();
        authWithoutSecret.setMethod(JWTClientSecretAuthenticator.PROVIDER_ID);
        OIDCClientRepresentation.Auth patchedAuth = getResultingAuthConfigPatch(authWithoutSecret, clientId);
        assertThat(patchedAuth, notNullValue());
        assertThat(patchedAuth.getMethod(), is(authWithoutSecret.getMethod()));
        assertThat(patchedAuth.getSecret(), is(createdAuth.getSecret()));
    }

    /**
     * Asserts that the confidential client has still the client secret set if field is left out.
     */
    @ParameterizedTest
    @ValueSource(strings = { ClientIdAndSecretAuthenticator.PROVIDER_ID, JWTClientSecretAuthenticator.PROVIDER_ID })
    void patchedClientWithSecretRetainSecret(String authenticationMethod) throws IOException {
        String clientId = authenticationMethod + "-patched-other-fields";
        OIDCClientRepresentation.Auth auth = new OIDCClientRepresentation.Auth();
        auth.setMethod(authenticationMethod);
        auth.setSecret("shush");

        OIDCClientRepresentation.Auth createdAuth = getResultingAuthConfigPost(auth, clientId);
        assertThat(createdAuth, notNullValue());
        assertThat(createdAuth.getSecret(), is(auth.getSecret()));

        OIDCClientRepresentation.Auth patchedAuth = getResultingAuthConfigPatch(null, clientId);
        assertThat(patchedAuth, notNullValue());
        assertThat(patchedAuth.getSecret(), is(auth.getSecret()));
    }

    @ParameterizedTest
    @ValueSource(strings = { ClientIdAndSecretAuthenticator.PROVIDER_ID, JWTClientSecretAuthenticator.PROVIDER_ID })
    void expectValidationFailureForUpdatePutWithoutSecret(String authenticationMethod) throws IOException {
        String clientId = authenticationMethod + "-validation-update-put";
        OIDCClientRepresentation.Auth auth = new OIDCClientRepresentation.Auth();
        auth.setMethod(authenticationMethod);
        auth.setSecret(clientId);

        OIDCClientRepresentation.Auth createdAuth = getResultingAuthConfigPut(auth, clientId);
        assertThat(createdAuth, notNullValue());
        assertThat(createdAuth.getSecret(), is(auth.getSecret()));

        auth.setSecret(null);
        var assertionError = assertThrows(AssertionError.class, () -> getResultingAuthConfigPut(auth, clientId));
        assertThat(assertionError.getMessage(), Matchers.containsString("was <400>"));
    }

    @ParameterizedTest
    @ValueSource(strings = { ClientIdAndSecretAuthenticator.PROVIDER_ID, JWTClientSecretAuthenticator.PROVIDER_ID })
    void expectValidationFailureForCreatePutWithoutSecret(String authenticationMethod) {
        String clientId = authenticationMethod + "-validation-create-put";
        OIDCClientRepresentation.Auth auth = new OIDCClientRepresentation.Auth();
        auth.setMethod(authenticationMethod);
        auth.setSecret(null);

        var assertionError = assertThrows(AssertionError.class, () -> getResultingAuthConfigPut(auth, clientId));
        assertThat(assertionError.getMessage(), Matchers.containsString("was <400>"));
    }

    @ParameterizedTest
    @ValueSource(strings = { ClientIdAndSecretAuthenticator.PROVIDER_ID, JWTClientSecretAuthenticator.PROVIDER_ID })
    void usePutToTurnConfidentialClientToPublicOne(String authenticationMethod) throws IOException {
        String clientId = authenticationMethod + "-put-to-public-cl";
        OIDCClientRepresentation.Auth auth = new OIDCClientRepresentation.Auth();
        auth.setMethod(authenticationMethod);
        auth.setSecret("top-secret");

        OIDCClientRepresentation.Auth putAuth = getResultingAuthConfigPut(auth, clientId);
        assertThat(putAuth, notNullValue());
        assertThat(putAuth.getSecret(), is(auth.getSecret()));

        // now turn this client to public one
        putAuth = getResultingAuthConfigPut(null, clientId);
        assertThat(putAuth, nullValue());
    }

    private OIDCClientRepresentation.Auth getResultingAuthConfigPost(OIDCClientRepresentation.Auth auth, String clientId, String... additionalFields) throws IllegalArgumentException {
        var rep = getResultingClientRep(auth, clientId, additionalFields);
        try (var response = adminClient.clients(testRealm.getName()).v2().createClient(rep)) {
            assertThat(response.getStatus(), Matchers.anyOf(is(201), is(200)));
            OIDCClientRepresentation createdClient = response.readEntity(OIDCClientRepresentation.class);
            return assertClientEnabledIdDescriptionAndAuth(rep, createdClient);
        }
    }

    private OIDCClientRepresentation.Auth getResultingAuthConfigPut(OIDCClientRepresentation.Auth auth, String clientId, String... additionalFields) throws IllegalArgumentException {
        var rep = getResultingClientRep(auth, clientId, additionalFields);
        try (var response = adminClient.clients(testRealm.getName()).v2().client(clientId).createOrUpdateClient(rep)) {
            assertThat(response.getStatus(), Matchers.anyOf(is(201), is(200)));
            OIDCClientRepresentation createdClient = response.readEntity(OIDCClientRepresentation.class);
            return assertClientEnabledIdDescriptionAndAuth(rep, createdClient);
        }
    }

    private OIDCClientRepresentation.Auth getResultingAuthConfigPatch(OIDCClientRepresentation.Auth auth, String clientId, String... additionalFields) throws IllegalArgumentException {
        var rep = getResultingClientRep(auth, clientId, additionalFields);
        OIDCClientRepresentation createdClient = (OIDCClientRepresentation) adminClient.clients(testRealm.getName()).v2().client(clientId).patchClient(mapper.valueToTree(rep));
        return assertClientEnabledIdDescriptionAndAuth(rep, createdClient);
    }

    private OIDCClientRepresentation getResultingClientRep(OIDCClientRepresentation.Auth auth, String clientId, String... additionalFields) throws IllegalArgumentException {
        OIDCClientRepresentation rep = new OIDCClientRepresentation();
        rep.setEnabled(true);
        rep.setClientId(clientId);
        rep.setDescription("I'm OIDC Client");
        rep.setAuth(auth);

        if (additionalFields.length % 2 != 0) {
            throw new IllegalArgumentException("Additional fields must always specify both field name and key");
        }
        for (int i = 0; i < additionalFields.length; i += 2) {
            rep.setAdditionalField(additionalFields[i], additionalFields[i + 1]);
        }

        return rep;
    }

    private OIDCClientRepresentation.Auth assertClientEnabledIdDescriptionAndAuth(OIDCClientRepresentation expected, OIDCClientRepresentation actual){
        assertThat(actual.getEnabled(), is(expected.getEnabled()));
        assertThat(actual.getClientId(), is(expected.getClientId()));
        assertThat(actual.getDescription(), is(expected.getDescription()));
        if (expected.getAuth() != null) {
            assertThat(actual.getAuth(), notNullValue());
            if (expected.getAuth().getMethod() != null) {
                assertThat(actual.getAuth().getMethod(), is(expected.getAuth().getMethod()));
            }
        }
        return actual.getAuth();
    }

    /**
     * Helper method to verify that client creation fails with the expected validation error.
     * This verifies that ValidationUtil.validateClient is called after the full model is populated.
     */
    private void assertClientCreationFailsWithError(BaseClientRepresentation rep, String expectedErrorMessage) throws Exception {
        try (var response = adminClient.clients(testRealm.getName()).v2().createClient(rep)) {
            assertThat(response.getStatus(), is(400));
            String body = response.readEntity(String.class);
            assertThat(body, containsString(expectedErrorMessage));
        }
    }

    /**
     * Helper method to verify that client update fails with the expected validation error.
     * First creates a valid client, then attempts to update it with invalid data.
     */
    private void assertClientUpdateFailsWithError(BaseClientRepresentation rep, String expectedErrorMessage) throws Exception {
        String clientId = rep.getClientId();

        // First, create a valid client
        BaseClientRepresentation validRep;
        if (rep instanceof SAMLClientRepresentation) {
            validRep = new SAMLClientRepresentation();
        } else {
            validRep = new OIDCClientRepresentation();
        }
        validRep.setClientId(clientId);
        validRep.setEnabled(true);

        try (var response = adminClient.clients(testRealm.getName()).v2().createClient(validRep)) {
            assertThat(response.getStatus(), is(201));
            BaseClientRepresentation created = response.readEntity(BaseClientRepresentation.class);
            assertThat(created, notNullValue());
            masterRealm.cleanup().add(realm -> realm.clients().delete(created.getUuid()));
        }

        // Now try to update with invalid data
        try (var response = adminClient.clients(testRealm.getName()).v2().client(clientId).createOrUpdateClient(rep)) {
            assertThat(response.getStatus(), is(400));
            String body = response.readEntity(String.class);
            assertThat(body, containsString(expectedErrorMessage));
        }
    }

    private void assertClientUuid(BaseClientRepresentation client) {
        assertThat(client.getUuid(), matchesPattern("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"));
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
        rep.setServiceAccountRoles(Set.of("default-roles-%s".formatted(testRealm.getName())));
        // not implemented yet
        // rep.setAdditionalFields(Map.of("key1", "val1", "key2", "val2"));
        return rep;
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
