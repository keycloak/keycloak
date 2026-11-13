/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.tests.client;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.client.registration.Auth;
import org.keycloak.client.registration.ClientRegistration;
import org.keycloak.client.registration.ClientRegistrationException;
import org.keycloak.client.registration.HttpErrorException;
import org.keycloak.common.constants.ServiceAccountConstants;
import org.keycloak.events.Errors;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.representations.idm.ClientInitialAccessCreatePresentation;
import org.keycloak.representations.idm.ClientInitialAccessPresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.OAuth2ErrorRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.oidc.OIDCClientRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.suites.DatabaseTest;
import org.keycloak.util.JsonSerialization;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;

import static org.keycloak.services.clientregistration.ErrorCodes.INVALID_CLIENT_METADATA;
import static org.keycloak.services.clientregistration.ErrorCodes.INVALID_REDIRECT_URI;
import static org.keycloak.tests.utils.admin.AdminApiUtil.findClientByClientId;
import static org.keycloak.utils.MediaType.APPLICATION_JSON;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@KeycloakIntegrationTest
public class ClientRegistrationTest extends AbstractClientRegistrationTest {

    private OIDCClientRepresentation buildOidcClient() {
        OIDCClientRepresentation client = new OIDCClientRepresentation();
        client.setClientName(CLIENT_ID);
        client.setRedirectUris(Collections.singletonList("http://localhost:8080/app"));
        return client;
    }

    private OIDCClientRepresentation registerClient(boolean cleanup) throws ClientRegistrationException {
        return registerClient(buildOidcClient(), cleanup);
    }

    private OIDCClientRepresentation registerClient(OIDCClientRepresentation client, boolean cleanup) throws ClientRegistrationException {
        OIDCClientRepresentation createdClient = reg.oidc().create(client);
        assertNotNull(createdClient.getClientId());

        // Remove this client after test
        if (cleanup) {
            String clientUuid = findClientByClientId(managedRealm.admin(), createdClient.getClientId()).toRepresentation().getId();
            managedRealm.cleanup().add(r -> r.clients().get(clientUuid).remove());
        }

        return createdClient;
    }

    private void assertStatus(int expectedStatus, ClientRegistrationException e) {
        assertEquals(expectedStatus, ((HttpErrorException) e.getCause()).getStatusLine().getStatusCode());
    }

    @Test
    @DatabaseTest
    public void registerClientAsAdmin() throws ClientRegistrationException {
        authManageClients();
        OIDCClientRepresentation createdClient = registerClient(true);

        ClientRepresentation client = findClientByClientId(managedRealm.admin(), createdClient.getClientId()).toRepresentation();
        assertEquals(OIDCLoginProtocol.LOGIN_PROTOCOL, client.getProtocol());
        assertFalse(client.isFullScopeAllowed());
    }

    // KEYCLOAK-5907
    @Test
    public void withServiceAccount() throws ClientRegistrationException {
        authManageClients();
        OIDCClientRepresentation clientRep = buildOidcClient();
        clientRep.setGrantTypes(Arrays.asList(OAuth2Constants.AUTHORIZATION_CODE, OAuth2Constants.CLIENT_CREDENTIALS));

        OIDCClientRepresentation rep = registerClient(clientRep, false);
        ClientRepresentation client = findClientByClientId(managedRealm.admin(), rep.getClientId()).toRepresentation();

        String serviceAccountUserId = client.getServiceAccountUserId();
        assertNotNull(serviceAccountUserId);

        reg.oidc().delete(rep.getClientId());
        try {
            managedRealm.admin().clients().get(client.getId()).toRepresentation();
            fail("Expected NotFoundException");
        } catch (NotFoundException e) {
        }

        try {
            managedRealm.admin().users().get(serviceAccountUserId).toRepresentation();
            fail("Expected NotFoundException");
        } catch (NotFoundException e) {
        }
    }

    @Test
    @DatabaseTest
    public void updateServiceAccount() throws Exception {
        authManageClients();
        OIDCClientRepresentation created = registerClient(true);
        String serviceAccountUsername = ServiceAccountConstants.SERVICE_ACCOUNT_USER_PREFIX + created.getClientId();

        OIDCClientRepresentation client = reg.oidc().get(created.getClientId());
        assertFalse(client.getGrantTypes().contains(OAuth2Constants.CLIENT_CREDENTIALS));
        assertTrue(managedRealm.admin().users().search(serviceAccountUsername, true).isEmpty());

        client.setGrantTypes(Arrays.asList(OAuth2Constants.AUTHORIZATION_CODE, OAuth2Constants.CLIENT_CREDENTIALS));
        client = reg.oidc().update(client);
        assertTrue(client.getGrantTypes().contains(OAuth2Constants.CLIENT_CREDENTIALS));
        assertFalse(managedRealm.admin().users().search(serviceAccountUsername, true).isEmpty());

        client.setGrantTypes(Collections.singletonList(OAuth2Constants.AUTHORIZATION_CODE));
        client = reg.oidc().update(client);
        assertFalse(client.getGrantTypes().contains(OAuth2Constants.CLIENT_CREDENTIALS));
        assertTrue(managedRealm.admin().users().search(serviceAccountUsername, true).isEmpty());
    }

    @Test
    @DatabaseTest
    public void registerClientInMasterRealm() throws Exception {
        ClientRegistration masterReg = ClientRegistration.create().url(keycloakUrls.getBase(), masterRealm.getName()).build();

        String token = oauth.realm(masterRealm.getName()).client(Constants.ADMIN_CLI_CLIENT_ID).doPasswordGrantRequest( "admin", "admin").getAccessToken();
        masterReg.auth(Auth.token(token));

        OIDCClientRepresentation createdClient = masterReg.oidc().create(buildOidcClient());
        assertNotNull(createdClient);

        masterRealm.admin().clients().get(findClientByClientId(masterRealm.admin(), createdClient.getClientId()).toRepresentation().getId()).remove();
    }

    @Test
    public void registerClientAsAdminWithCreateOnly() throws ClientRegistrationException {
        authCreateClients();
        registerClient(true);
    }

    @Test
    public void registerClientAsAdminWithNoAccess() throws ClientRegistrationException {
        authNoAccess();
        try {
            registerClient(true);
            fail("Expected 403");
        } catch (ClientRegistrationException e) {
            assertStatus(403, e);
        }
    }

    @Test
    @DatabaseTest
    public void registerClientUsingRevokedToken() throws Exception {
        ClientRepresentation myclient = new ClientRepresentation();

        myclient.setClientId("myclient");
        myclient.setServiceAccountsEnabled(true);
        myclient.setSecret("password");
        myclient.setDirectAccessGrantsEnabled(true);

        ClientRepresentation createdMyClient = createClient(myclient);
        managedRealm.cleanup().add(r -> r.clients().get(createdMyClient.getId()).remove());

        oauth.client("myclient");
        String bearerToken = getToken("myclient", "password", "manage-clients", "password");
        assertTrue(oauth.tokenRevocationRequest(bearerToken).accessToken().send().isSuccess());

        try {
            reg.auth(Auth.token(bearerToken));
            registerClient(true);
            fail("Expected 401");
        } catch (ClientRegistrationException cre) {
            HttpErrorException cause = (HttpErrorException) cre.getCause();
            assertEquals(401, cause.getStatusLine().getStatusCode());
            OAuth2ErrorRepresentation error = cause.toErrorRepresentation();
            assertEquals(Errors.INVALID_TOKEN, error.getError());
            assertEquals("Failed decode token", error.getErrorDescription());
        }
    }

    @Test
    @DatabaseTest
    public void registerClientWithNonAsciiChars() throws ClientRegistrationException {
        authCreateClients();
        OIDCClientRepresentation client = buildOidcClient();
        String name = "Cli\u00EBnt";
        client.setClientName(name);

        OIDCClientRepresentation createdClient = registerClient(client, true);
        assertEquals(name, createdClient.getClientName());
    }

    @Test
    @DatabaseTest
    public void testInvalidUrlClientValidation() throws ClientRegistrationException {
        for (boolean register : new boolean[] {true, false}) {
            OIDCClientRepresentation rep = prepareForValidation(register);
            rep.setClientUri("invalid");
            assertRejected(rep, register, INVALID_CLIENT_METADATA, "Base URL is not a valid URL");
        }
    }

    @Test
    @DatabaseTest
    public void testIllegalSchemeClientValidation() throws ClientRegistrationException {
        for (String url : List.of("data:text/html;base64,PHNjcmlwdD5jb25maXJtKGRvY3VtZW50LmRvbWFpbik7PC9zY3JpcHQ+", "javascript:confirm(document.domain)/*")) {
            for (boolean register : new boolean[] {true, false}) {
                OIDCClientRepresentation rep = prepareForValidation(register);
                rep.setClientUri(url);
                assertRejected(rep, register, INVALID_CLIENT_METADATA, "Base URL uses an illegal scheme");

                rep = prepareForValidation(register);
                rep.setBackchannelLogoutUri(url);
                assertRejected(rep, register, INVALID_CLIENT_METADATA, "Backchannel logout URL uses an illegal scheme");

                rep = prepareForValidation(register);
                rep.setRedirectUris(Collections.singletonList(url));
                assertRejected(rep, register, INVALID_REDIRECT_URI, "A redirect URI uses an illegal scheme");
            }
        }
    }

    // KEYCLOAK-3421
    @Test
    @DatabaseTest
    public void testFragmentProhibitedClientValidation() throws ClientRegistrationException {
        for (boolean register : new boolean[] {true, false}) {
            OIDCClientRepresentation rep = prepareForValidation(register);
            rep.setRedirectUris(Collections.singletonList("http://redhat.com/abcd#someFragment"));
            assertRejected(rep, register, INVALID_REDIRECT_URI, "A redirect URI must not contain an URL fragment");
        }
    }

    /**
     * Returns the representation to be sent to the registration endpoint. When updating, an existing valid client is
     * registered first and the returned representation targets it.
     */
    private OIDCClientRepresentation prepareForValidation(boolean register) throws ClientRegistrationException {
        if (register) {
            authCreateClients();
            return buildOidcClient();
        }
        authManageClients();
        OIDCClientRepresentation created = registerClient(true);
        authManageClients();
        return reg.oidc().get(created.getClientId());
    }

    private void assertRejected(OIDCClientRepresentation rep, boolean register, String expectedError, String expectedErrorDescription) {
        try {
            if (register) {
                registerClient(rep, true);
            } else {
                reg.oidc().update(rep);
            }
            fail("Expected exception");
        } catch (ClientRegistrationException e) {
            HttpErrorException cause = (HttpErrorException) e.getCause();
            assertEquals(400, cause.getStatusLine().getStatusCode());
            OAuth2ErrorRepresentation errorRep;
            try {
                errorRep = JsonSerialization.readValue(cause.getErrorResponse(), OAuth2ErrorRepresentation.class);
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
            assertEquals(expectedError, errorRep.getError());
            assertThat(errorRep.getErrorDescription(), CoreMatchers.containsString(expectedErrorDescription));
        }
    }

    @Test
    public void samlClientCannotBeUpdatedOrDeletedViaOidcEndpointWithRegistrationToken() throws ClientRegistrationException {
        // Create a SAML client; capture the RAT.
        ClientRepresentation samlClient = buildClient();
        samlClient.setProtocol(SamlProtocol.LOGIN_PROTOCOL);
        ClientRepresentation created = createClient(samlClient);
        managedRealm.cleanup().add(r -> r.clients().get(created.getId()).remove());
        assertThat(created.getProtocol(), is(SamlProtocol.LOGIN_PROTOCOL));

        // Present the SAML client's RAT to the OIDC registration endpoint.
        reg.auth(Auth.token(created.getRegistrationAccessToken()));

        OIDCClientRepresentation update = new OIDCClientRepresentation();
        update.setClientId(created.getClientId());
        update.setRedirectUris(Collections.singletonList("http://localhost:8080/callback"));

        assertWrongClientProtocol(() -> reg.oidc().update(update));
        assertWrongClientProtocol(() -> {
            reg.oidc().delete(created.getClientId());
            return null;
        });

        // The client must survive both attempts, unchanged.
        ClientRepresentation afterAttempt = managedRealm.admin().clients().get(created.getId()).toRepresentation();
        assertThat(afterAttempt.getProtocol(), is(SamlProtocol.LOGIN_PROTOCOL));
    }

    private void assertWrongClientProtocol(Callable<?> request) {
        try {
            request.call();
            fail("Expected ClientRegistrationException — registration token must not be accepted by an endpoint of a different protocol");
        } catch (Exception e) {
            assertThat(e, org.hamcrest.Matchers.instanceOf(ClientRegistrationException.class));
            HttpErrorException cause = (HttpErrorException) e.getCause();
            assertThat(cause.getStatusLine().getStatusCode(), is(400));

            OAuth2ErrorRepresentation errorRep;
            try {
                errorRep = JsonSerialization.readValue(cause.getErrorResponse(), OAuth2ErrorRepresentation.class);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
            assertThat(errorRep.getError(), is(Errors.INVALID_CLIENT));
            assertThat(errorRep.getErrorDescription(), CoreMatchers.containsString("Wrong client protocol"));
        }
    }

    @Test
    public void getClientAsAdmin() throws ClientRegistrationException {
        authManageClients();
        OIDCClientRepresentation created = registerClient(true);
        OIDCClientRepresentation rep = reg.oidc().get(created.getClientId());
        assertNotNull(rep);
    }

    @Test
    public void getClientAsAdminWithCreateOnly() throws ClientRegistrationException {
        authManageClients();
        OIDCClientRepresentation created = registerClient(true);
        authCreateClients();
        try {
            reg.oidc().get(created.getClientId());
            fail("Expected 403");
        } catch (ClientRegistrationException e) {
            assertStatus(403, e);
        }
    }

    @Test
    public void getClientAsAdminWithNoAccess() throws ClientRegistrationException {
        authManageClients();
        OIDCClientRepresentation created = registerClient(true);
        authNoAccess();
        try {
            reg.oidc().get(created.getClientId());
            fail("Expected 403");
        } catch (ClientRegistrationException e) {
            assertStatus(403, e);
        }
    }

    @Test
    public void getClientNotFoundNoAccess() {
        authNoAccess();
        try {
            reg.oidc().get("invalid");
            fail("Expected 403");
        } catch (ClientRegistrationException e) {
            assertStatus(403, e);
        }
    }

    private void updateClient(String clientId) throws ClientRegistrationException {
        OIDCClientRepresentation client = reg.oidc().get(clientId);
        client.setRedirectUris(Collections.singletonList("http://localhost:8080/updated"));

        reg.oidc().update(client);

        OIDCClientRepresentation updatedClient = reg.oidc().get(clientId);

        assertEquals(1, updatedClient.getRedirectUris().size());
        assertEquals("http://localhost:8080/updated", updatedClient.getRedirectUris().get(0));
    }

    @Test
    @DatabaseTest
    public void updateClientAsAdmin() throws ClientRegistrationException {
        authManageClients();
        OIDCClientRepresentation created = registerClient(true);

        authManageClients();
        updateClient(created.getClientId());
    }

    @Test
    public void updateClientAsAdminWithCreateOnly() throws ClientRegistrationException {
        authManageClients();
        OIDCClientRepresentation created = registerClient(true);

        authCreateClients();
        try {
            updateClient(created.getClientId());
            fail("Expected 403");
        } catch (ClientRegistrationException e) {
            assertStatus(403, e);
        }
    }

    @Test
    public void updateClientAsAdminWithNoAccess() throws ClientRegistrationException {
        authManageClients();
        OIDCClientRepresentation created = registerClient(true);

        authNoAccess();
        try {
            updateClient(created.getClientId());
            fail("Expected 403");
        } catch (ClientRegistrationException e) {
            assertStatus(403, e);
        }
    }

    @Test
    @DatabaseTest
    public void updateClientNotFound() {
        authManageClients();
        try {
            OIDCClientRepresentation client = new OIDCClientRepresentation();
            client.setClientId("invalid");

            reg.oidc().update(client);

            fail("Expected 404");
        } catch (ClientRegistrationException e) {
            assertStatus(404, e);
        }
    }

    @Test
    @DatabaseTest
    public void updateClientWithNonAsciiChars() throws ClientRegistrationException {
        authCreateClients();
        OIDCClientRepresentation created = registerClient(true);

        authManageClients();
        OIDCClientRepresentation client = reg.oidc().get(created.getClientId());
        String name = "Cli\u00EBnt";
        client.setClientName(name);

        OIDCClientRepresentation updatedClient = reg.oidc().update(client);
        assertEquals(name, updatedClient.getClientName());
    }

    private void deleteClient(OIDCClientRepresentation client) throws ClientRegistrationException {
        reg.oidc().delete(client.getClientId());
        assertTrue(managedRealm.admin().clients().findByClientId(client.getClientId()).isEmpty(),
                "Expected client to be deleted");
    }

    @Test
    @DatabaseTest
    public void deleteClientAsAdmin() throws ClientRegistrationException {
        authCreateClients();
        OIDCClientRepresentation client = registerClient(false);

        authManageClients();
        deleteClient(client);
    }

    @Test
    public void deleteClientAsAdminWithCreateOnly() throws ClientRegistrationException {
        authManageClients();
        OIDCClientRepresentation client = registerClient(true);
        try {
            authCreateClients();
            deleteClient(client);
            fail("Expected 403");
        } catch (ClientRegistrationException e) {
            assertStatus(403, e);
        }
    }

    @Test
    public void deleteClientAsAdminWithNoAccess() throws ClientRegistrationException {
        authManageClients();
        OIDCClientRepresentation client = registerClient(true);
        try {
            authNoAccess();
            deleteClient(client);
            fail("Expected 403");
        } catch (ClientRegistrationException e) {
            assertStatus(403, e);
        }
    }

    @Test
    public void registerClientWithWrongCharacters() throws IOException {
        HttpPost post = new HttpPost(keycloakUrls.getBaseBuilder().path("/realms/master/clients-registrations/openid-connect").build());
        post.setEntity(new StringEntity("{\"<img src=alert(1)>\":1}"));
        post.setHeader("Content-Type", APPLICATION_JSON);

        try (CloseableHttpResponse response = closeableHttpClient.execute(post)) {
            assertThat(response.getStatusLine().getStatusCode(),is(400));

            Header header = response.getFirstHeader("Content-Type");
            assertThat(header, notNullValue());

            // Verify the Content-Type is not text/html
            assertThat(Arrays.stream(header.getElements())
                    .map(HeaderElement::getName)
                    .filter(Objects::nonNull)
                    .anyMatch(f -> f.equals(APPLICATION_JSON)), is(true));

            // The alert is not executed
            assertThat(EntityUtils.toString(response.getEntity()), CoreMatchers.containsString("Unrecognized field \\\"<img src=alert(1)>\\\""));
        }
    }

    @Test
    public void defaultRegistrationEndpointIsNotAvailable() throws IOException {
        String base = keycloakUrls.getBase() + "/realms/" + managedRealm.getName() + "/clients-registrations/default";

        HttpPost post = new HttpPost(base);
        post.setEntity(new StringEntity("{\"clientId\":\"" + CLIENT_ID + "\"}"));
        post.setHeader("Content-Type", APPLICATION_JSON);

        for (HttpUriRequest request : List.of(
                post,
                new HttpGet(base + "/" + CLIENT_ID),
                new HttpPut(base + "/" + CLIENT_ID),
                new HttpDelete(base + "/" + CLIENT_ID))) {
            try (CloseableHttpResponse response = closeableHttpClient.execute(request)) {
                assertThat(request.getMethod(), response.getStatusLine().getStatusCode(), is(404));
            }
        }
    }

    @Test
    @DatabaseTest
    public void registerMultipleClients() {

        int concurrentThreads = 5;
        int iterations = 10;
        int initialTokenCounts = 2;

        ClientInitialAccessCreatePresentation clientInitialAccessCreatePresentation = new ClientInitialAccessCreatePresentation();
        clientInitialAccessCreatePresentation.setCount(initialTokenCounts);
        clientInitialAccessCreatePresentation.setExpiration(10000);
        ClientInitialAccessPresentation response = managedRealm.admin().clientInitialAccess().create(clientInitialAccessCreatePresentation);

        ExecutorService threadPool = Executors.newFixedThreadPool(concurrentThreads);
        AtomicInteger createdCount = new AtomicInteger();
        try {
            Collection<Callable<Void>> futures = new LinkedList<>();
            for (int i = 0; i < iterations; i ++) {
                final int j = i;

                Callable<Void> f = () -> {
                    ClientRegistration client = ClientRegistration.create().url(keycloakUrls.getBase(), managedRealm.getName()).build();
                    client.auth(Auth.token(response));
                    OIDCClientRepresentation rep = new OIDCClientRepresentation();
                    rep.setClientName("test-" + j);
                    rep.setRedirectUris(Collections.singletonList("http://localhost:8080/app"));
                    rep = client.oidc().create(rep);
                    if (rep.getClientId() != null && ("test-" + j).equals(rep.getClientName())) {
                        createdCount.getAndIncrement();
                    }
                    return null;
                };
                futures.add(f);
            }
            threadPool.invokeAll(futures);

        } catch (Exception ex) {
            fail(ex.getMessage());
        }

        //controls the number of uses of the initial access token
        assertEquals(initialTokenCounts, createdCount.get());
    }

    @Test
    @DatabaseTest
    public void registerWithLightweightAccessTokenAndTransientSession() throws Exception {

        String TEST_ADMIN_CLIENT = "test-admin-client";
        ClientRepresentation testAdminClient = new ClientRepresentation();
        testAdminClient.setClientId(TEST_ADMIN_CLIENT);
        testAdminClient.setSecret(TEST_ADMIN_CLIENT);
        testAdminClient.setServiceAccountsEnabled(true);
        testAdminClient.setAttributes(new HashMap<>());
        testAdminClient.getAttributes().put(Constants.USE_LIGHTWEIGHT_ACCESS_TOKEN_ENABLED, Boolean.TRUE.toString());

        Response response = masterRealm.admin().clients().create(testAdminClient);
        testAdminClient = masterRealm.admin().clients().get( ApiUtil.getCreatedId(response)).toRepresentation();

        String serviceAccountUserId = testAdminClient.getServiceAccountUserId();
        RoleRepresentation adminRole =  masterRealm.admin().roles().get(AdminRoles.ADMIN).toRepresentation();
        masterRealm.admin().users().get(serviceAccountUserId).roles().realmLevel().add(List.of(adminRole));

        oauth.client(TEST_ADMIN_CLIENT, TEST_ADMIN_CLIENT);
        oauth.realm(masterRealm.getName());
        String token = oauth.doClientCredentialsGrantAccessTokenRequest().getAccessToken();
        ClientRegistration masterReg = ClientRegistration.create().url(keycloakUrls.getBase(), masterRealm.getName()).build();
        masterReg.auth(Auth.token(token));

        OIDCClientRepresentation createdClient = masterReg.oidc().create(buildOidcClient());
        assertNotNull(createdClient);

        masterRealm.admin().clients().get(testAdminClient.getId()).remove();
        masterRealm.admin().clients().get(findClientByClientId(masterRealm.admin(), createdClient.getClientId()).toRepresentation().getId()).remove();
    }
}
