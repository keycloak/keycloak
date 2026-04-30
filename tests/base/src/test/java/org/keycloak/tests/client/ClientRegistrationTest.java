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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.client.registration.Auth;
import org.keycloak.client.registration.ClientRegistration;
import org.keycloak.client.registration.ClientRegistrationException;
import org.keycloak.client.registration.HttpErrorException;
import org.keycloak.common.constants.ServiceAccountConstants;
import org.keycloak.common.util.CollectionUtil;
import org.keycloak.events.Errors;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.representations.idm.ClientInitialAccessCreatePresentation;
import org.keycloak.representations.idm.ClientInitialAccessPresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.OAuth2ErrorRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.ResourceServerRepresentation;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.server.KeycloakUrls;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.suites.DatabaseTest;
import org.keycloak.util.JsonSerialization;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static java.util.Arrays.asList;

import static org.keycloak.services.clientregistration.ErrorCodes.INVALID_CLIENT_METADATA;
import static org.keycloak.services.clientregistration.ErrorCodes.INVALID_REDIRECT_URI;
import static org.keycloak.utils.MediaType.APPLICATION_JSON;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@KeycloakIntegrationTest
public class ClientRegistrationTest {

    private static final String CLIENT_ID = "test-client";
    private static final String CLIENT_SECRET = "test-client-secret";
    public static final String REALM_NAME = "test";

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    @InjectRealm(config = ClientRegistrationTest.ClientRegistrationRealmConfig.class)
    ManagedRealm realm;

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectAdminClient
    Keycloak adminClient;

    ClientRegistration reg;

    @BeforeEach
    public void before() throws Exception {
        reg = ClientRegistration.create()
                .url(keycloakUrls.getBase(), REALM_NAME)
                .build();
    }

    @AfterEach
    public void after() throws Exception {
        // Clean up any test client that might have been created
        try {
            if (reg != null) {
                authManageClients();
                reg.delete(CLIENT_ID);
            }
        } catch (Exception e) {
            // Ignore - client may not exist
        }

        if (reg != null) {
            reg.close();
        }
    }

    @Test
    @DatabaseTest
    public void registerClientAsAdmin() throws ClientRegistrationException {
        authManageClients();

        // Register the client
        ClientRepresentation client = buildClient();
        ClientRepresentation createdClient = registerClient(client);

        // Verify client was created
        assertNotNull(createdClient);
        assertEquals(CLIENT_ID, createdClient.getClientId());

        // Verify via admin API
        ClientRepresentation adminClient = this.adminClient.realm(REALM_NAME)
                .clients()
                .get(createdClient.getId())
                .toRepresentation();
        assertEquals(CLIENT_ID, adminClient.getClientId());

        this.adminClient.realm(REALM_NAME)
                .clients()
                .get(createdClient.getId())
                .remove();
    }

    @Test
    @DatabaseTest
    public void registerClientAsAdminWithCreateOnly() throws ClientRegistrationException {
        authCreateClients();

        ClientRepresentation client = buildClient();
        ClientRepresentation createdClient = registerClient(client);

        assertNotNull(createdClient);
        assertEquals(CLIENT_ID, createdClient.getClientId());

        this.adminClient.realm(REALM_NAME)
                .clients()
                .get(createdClient.getId())
                .remove();
    }

    // KEYCLOAK-5907
    @Test
    @DatabaseTest
    public void withServiceAccount() throws ClientRegistrationException {
        authManageClients();
        ClientRepresentation clientRep = buildClient();
        clientRep.setServiceAccountsEnabled(true);

        ClientRepresentation rep = registerClient(clientRep);
        assertNotNull(rep);
        assertEquals(CLIENT_ID, rep.getClientId());

        UserRepresentation serviceAccountUser = adminClient.realm(REALM_NAME).clients().get(rep.getId()).getServiceAccountUser();

        assertNotNull(serviceAccountUser);

        deleteClient(rep);

        try {
            adminClient.realm("test").users().get(serviceAccountUser.getId()).toRepresentation();
            fail("Expected NotFoundException");
        } catch (NotFoundException e) {
        }
    }

    @Test
    @DatabaseTest
    public void updateServiceAccount() throws Exception {
        authManageClients();
        ClientRepresentation client = buildClient();
        final ClientRepresentation createdClient = registerClient(client);

        client = reg.get(CLIENT_ID);
        assertFalse(client.isServiceAccountsEnabled());
        assertTrue(adminClient.realm(REALM_NAME).users().search(ServiceAccountConstants.SERVICE_ACCOUNT_USER_PREFIX + client.getClientId(), true).isEmpty());
        client.setServiceAccountsEnabled(true);
        client = reg.update(client);
        assertTrue(client.isServiceAccountsEnabled());
        assertFalse(adminClient.realm(REALM_NAME).users().search(ServiceAccountConstants.SERVICE_ACCOUNT_USER_PREFIX + client.getClientId(), true).isEmpty());

        client.setServiceAccountsEnabled(false);
        client = reg.update(client);
        assertFalse(client.isServiceAccountsEnabled());
        assertTrue(adminClient.realm(REALM_NAME).users().search(ServiceAccountConstants.SERVICE_ACCOUNT_USER_PREFIX + client.getClientId(), true).isEmpty());
        deleteClient(createdClient);
    }

    @Test
    @DatabaseTest
    public void registerClientInMasterRealm() throws Exception {
        ClientRegistration masterReg = ClientRegistration.create().url(keycloakUrls.getBase(), "master").build();

        String token = oauth.realm("master").client(Constants.ADMIN_CLI_CLIENT_ID).doPasswordGrantRequest( "admin", "admin").getAccessToken();
        masterReg.auth(Auth.token(token));

        ClientRepresentation client = new ClientRepresentation();
        client.setClientId(CLIENT_ID);
        client.setSecret(CLIENT_SECRET);

        ClientRepresentation createdClient = masterReg.create(client);
        assertNotNull(createdClient);

        adminClient.realm("master").clients().get(createdClient.getId()).remove();
    }

    @Test
    @DatabaseTest
    public void registerClientWithoutProtocol() throws ClientRegistrationException {
        authCreateClients();
        ClientRepresentation client = buildClient();
        ClientRepresentation clientRepresentation = registerClient(client);

        assertNotNull(clientRepresentation);
        assertEquals(CLIENT_ID, clientRepresentation.getClientId());

        assertEquals("openid-connect", clientRepresentation.getProtocol());

        // Cleanup
        authManageClients();
        deleteClient(clientRepresentation);
    }

    /**
     * OID4VC protocol is not valid for clients. It can only be used for ClientScopes.
     * Attempting to create a client with protocol "oid4vc" should be rejected.
     */
    @Test
    @DatabaseTest
    public void registerOid4vcClientShouldBeRejected() {
        authManageClients();

        ClientRepresentation client = buildClient();
        client.setProtocol("oid4vc");

        try(Response response = adminClient.realm(REALM_NAME).clients().create(client)) {
            Assertions.assertEquals(400, response.getStatus() , "Creating a client with OID4VC protocol should be rejected as it is not a valid protocol for clients.");
        }
    }

    @Test
    @DatabaseTest
    public void registerClientAsAdminWithNoAccess() {
        authNoAccess();
        try {
            ClientRepresentation client = buildClient();
            registerClient(client);
            fail("Expected 403");
        } catch (ClientRegistrationException e) {
            assertEquals(403, ((HttpErrorException) e.getCause()).getStatusLine().getStatusCode());
        }
    }

    @Test
    @DatabaseTest
    public void registerClientUsingRevokedToken() throws Exception {
        reg.auth(Auth.token(getToken("manage-clients", "password")));

        ClientRepresentation myclient = new ClientRepresentation();

        myclient.setClientId("myclient");
        myclient.setServiceAccountsEnabled(true);
        myclient.setSecret("password");
        myclient.setDirectAccessGrantsEnabled(true);

        reg.create(myclient);

        oauth.client("myclient");
        String bearerToken = getToken("myclient", "password", "manage-clients", "password");
        assertTrue(oauth.tokenRevocationRequest(bearerToken).accessToken().send().isSuccess());

        try {
            reg.auth(Auth.token(bearerToken));

            ClientRepresentation clientRep = buildClient();
            clientRep.setServiceAccountsEnabled(true);

            registerClient(clientRep);
        } catch (ClientRegistrationException cre) {
            HttpErrorException cause = (HttpErrorException) cre.getCause();
            assertEquals(401, cause.getStatusLine().getStatusCode());
            OAuth2ErrorRepresentation error = cause.toErrorRepresentation();
            assertEquals(Errors.INVALID_TOKEN, error.getError());
            assertEquals("Failed decode token", error.getErrorDescription());
        } finally {
            // Cleanup myclient
            reg.auth(Auth.token(getToken("manage-clients", "password")));
            try {
                reg.delete("myclient");
            } catch (Exception e) {
                // Ignore
            }
        }
    }

    @Test
    @DatabaseTest
    public void registerClientWithNonAsciiChars() throws ClientRegistrationException {
        authCreateClients();
        ClientRepresentation client = buildClient();
        String name = "Cli\u00EBnt";
        client.setName(name);

        ClientRepresentation createdClient = registerClient(client);
        assertEquals(name, createdClient.getName());

        authManageClients();
        deleteClient(createdClient);
    }

    @Test
    @DatabaseTest
    public void clientWithDefaultRoles() throws ClientRegistrationException {
        authCreateClients();
        ClientRepresentation client = buildClient();
        client.setDefaultRoles(new String[]{"test-default-role"});

        ClientRepresentation createdClient = registerClient(client);
        assertArrayEquals(new String[]{"test-default-role"}, createdClient.getDefaultRoles());

        authManageClients();
        ClientRepresentation obtainedClient = reg.get(CLIENT_ID);
        assertArrayEquals(new String[]{"test-default-role"}, obtainedClient.getDefaultRoles());

        client.setDefaultRoles(new String[]{"test-default-role1","test-default-role2"});
        ClientRepresentation updatedClient = reg.update(client);
        assertThat(Arrays.asList(updatedClient.getDefaultRoles()), containsInAnyOrder("test-default-role1", "test-default-role2"));
    }

    @Test
    @DatabaseTest
    public void updateClientScopes() throws ClientRegistrationException {
        authManageClients();
        ClientRepresentation client = buildClient();
        ArrayList<String> optionalClientScopes = new ArrayList<>(List.of("address"));
        client.setOptionalClientScopes(optionalClientScopes);
        ClientRepresentation createdClient = registerClient(client);
        Set<String> requestedClientScopes = new HashSet<>(optionalClientScopes);
        Set<String> registeredClientScopes = new HashSet<>(createdClient.getOptionalClientScopes());
        assertEquals(requestedClientScopes, registeredClientScopes);
        assertTrue(CollectionUtil.collectionEquals(createdClient.getDefaultClientScopes(), Set.of("basic")));

        authManageClients();
        ClientRepresentation obtainedClient = reg.get(CLIENT_ID);
        registeredClientScopes = new HashSet<>(obtainedClient.getOptionalClientScopes());
        assertEquals(requestedClientScopes, registeredClientScopes);
        assertTrue(CollectionUtil.collectionEquals(obtainedClient.getDefaultClientScopes(), Set.of("basic")));


        optionalClientScopes = new ArrayList<>(List.of("address", "phone"));
        obtainedClient.setOptionalClientScopes(optionalClientScopes);
        ClientRepresentation updatedClient = reg.update(obtainedClient);
        requestedClientScopes = new HashSet<>(optionalClientScopes);
        registeredClientScopes = new HashSet<>(updatedClient.getOptionalClientScopes());
        assertEquals(requestedClientScopes, registeredClientScopes);
        assertTrue(CollectionUtil.collectionEquals(updatedClient.getDefaultClientScopes(), Set.of("basic")));
    }

    @Test
    @DatabaseTest
    public void testInvalidUrlClientValidation() {
        testClientUriValidation("Root URL is not a valid URL",
                "Base URL is not a valid URL",
                "Backchannel logout URL is not a valid URL",
                null,
                "invalid", "myapp://some-fake-app");
    }

    @Test
    @DatabaseTest
    public void testIllegalSchemeClientValidation() {
        testClientUriValidation("Root URL uses an illegal scheme",
                "Base URL uses an illegal scheme",
                "Backchannel logout URL uses an illegal scheme",
                "A redirect URI uses an illegal scheme",
                "data:text/html;base64,PHNjcmlwdD5jb25maXJtKGRvY3VtZW50LmRvbWFpbik7PC9zY3JpcHQ+",
                "javascript:confirm(document.domain)/*"
        );
    }

    // KEYCLOAK-3421
    @Test
    @DatabaseTest
    public void testFragmentProhibitedClientValidation() {
        testClientUriValidation("Root URL must not contain an URL fragment",
                null,
                null,
                "Redirect URIs must not contain an URI fragment",
                "http://redhat.com/abcd#someFragment"
        );
    }

    @Test
    @DatabaseTest
    public void testSamlSpecificUrls() throws ClientRegistrationException {
        testSamlSpecificUrls(true, "javascript:alert('TEST')", "data:text/html;base64,PHNjcmlwdD5jb25maXJtKGRvY3VtZW50LmRvbWFpbik7PC9zY3JpcHQ+");
        testSamlSpecificUrls(false, "javascript:alert('TEST')", "data:text/html;base64,PHNjcmlwdD5jb25maXJtKGRvY3VtZW50LmRvbWFpbik7PC9zY3JpcHQ+");
    }

    @Test
    @DatabaseTest
    public void testUpdateAuthorizationSettings() throws ClientRegistrationException {
        authManageClients();
        ClientRepresentation clientRep = buildClient();
        clientRep.setAuthorizationServicesEnabled(true);

        ClientRepresentation rep = registerClient(clientRep);
        rep = adminClient.realm(REALM_NAME).clients().get(rep.getId()).toRepresentation();

        assertTrue(rep.getAuthorizationServicesEnabled());

        ResourceServerRepresentation authzSettings = new ResourceServerRepresentation();

        authzSettings.setAllowRemoteResourceManagement(false);
        authzSettings.setResources(List.of(new ResourceRepresentation("foo", "scope-a", "scope-b")));

        PolicyRepresentation permission = new PolicyRepresentation();

        permission.setName(KeycloakModelUtils.generateId());
        permission.setType("resource");
        permission.setResources(Collections.singleton("foo"));

        authzSettings.setPolicies(List.of(permission));

        rep.setAuthorizationSettings(authzSettings);

        reg.update(rep);
        authzSettings = adminClient.realm(REALM_NAME).clients().get(rep.getId()).authorization().exportSettings();

        assertFalse(authzSettings.getResources().isEmpty());
        assertFalse(authzSettings.getScopes().isEmpty());
        assertFalse(authzSettings.getPolicies().isEmpty());
    }

    @Test
    @DatabaseTest
    public void getClientAsAdmin() throws ClientRegistrationException {
        setupClientAsAdmin();
        ClientRepresentation rep = reg.get(CLIENT_ID);
        assertNotNull(rep);
    }

    @Test
    @DatabaseTest
    public void getClientAsAdminWithCreateOnly() throws ClientRegistrationException {
        setupClientAsAdmin();
        authCreateClients();
        try {
            reg.get(CLIENT_ID);
            fail("Expected 403");
        } catch (ClientRegistrationException e) {
            assertEquals(403, ((HttpErrorException) e.getCause()).getStatusLine().getStatusCode());
        }
    }

    @Test
    @DatabaseTest
    public void getClientAsAdminWithNoAccess() throws ClientRegistrationException {
        setupClientAsAdmin();
        authNoAccess();
        try {
            reg.get(CLIENT_ID);
            fail("Expected 403");
        } catch (ClientRegistrationException e) {
            assertEquals(403, ((HttpErrorException) e.getCause()).getStatusLine().getStatusCode());
        }
    }

    @Test
    public void getClientNotFound() throws ClientRegistrationException {
        authManageClients();
        Assertions.assertNull(reg.get("invalid"));
    }

    @Test
    public void getClientNotFoundNoAccess() {
        authNoAccess();
        try {
            reg.get("invalid");
            fail("Expected 403");
        } catch (ClientRegistrationException e) {
            assertEquals(403, ((HttpErrorException) e.getCause()).getStatusLine().getStatusCode());
        }
    }

    @Test
    @DatabaseTest
    public void updateClientAsAdmin() throws ClientRegistrationException {
        setupClientAsAdmin();

        authManageClients();
        updateClient();
    }

    @Test
    @DatabaseTest
    public void updateClientSecret() throws ClientRegistrationException {
        authManageClients();
        registerClient(buildClient());

        ClientRepresentation client = reg.get(CLIENT_ID);
        assertNotNull(client.getSecret());
        client.setSecret("mysecret");

        reg.update(client);

        ClientRepresentation updatedClient = reg.get(CLIENT_ID);

        assertEquals("mysecret", updatedClient.getSecret());
    }

    @Test
    @DatabaseTest
    public void addClientProtcolMappers() throws ClientRegistrationException {
        authManageClients();

        ClientRepresentation initialClient = buildClient();

        registerClient(initialClient);
        ClientRepresentation client = reg.get(CLIENT_ID);

        addProtocolMapper(client, "mapperA");
        reg.update(client);

        ClientRepresentation updatedClient = reg.get(CLIENT_ID);
        assertThat("Adding protocolMapper failed", updatedClient.getProtocolMappers().size(), is(1));
    }

    @Test
    @DatabaseTest
    public void removeClientProtcolMappers() throws ClientRegistrationException {
        authManageClients();

        ClientRepresentation initialClient = buildClient();
        addProtocolMapper(initialClient, "mapperA");
        registerClient(initialClient);
        ClientRepresentation client = reg.get(CLIENT_ID);
        client.setProtocolMappers(new ArrayList<>());
        reg.update(client);

        ClientRepresentation updatedClient = reg.get(CLIENT_ID);
        assertThat("Removing protocolMapper failed", updatedClient.getProtocolMappers(), nullValue());
    }

    @Test
    @DatabaseTest
    public void updateClientProtcolMappers() throws ClientRegistrationException {
        authManageClients();

        ClientRepresentation initialClient = buildClient();
        addProtocolMapper(initialClient, "mapperA");
        registerClient(initialClient);
        ClientRepresentation client = reg.get(CLIENT_ID);
        client.getProtocolMappers().get(0).getConfig().put("claim.name", "updatedClaimName");
        reg.update(client);

        ClientRepresentation updatedClient = reg.get(CLIENT_ID);
        assertThat("Updating protocolMapper failed", updatedClient.getProtocolMappers().get(0).getConfig().get("claim.name"), is("updatedClaimName"));
    }

    @Test
    @DatabaseTest
    public void updateClientAsAdminWithCreateOnly() {
        authCreateClients();
        try {
            updateClient();
            fail("Expected 403");
        } catch (ClientRegistrationException e) {
            assertEquals(403, ((HttpErrorException) e.getCause()).getStatusLine().getStatusCode());
        }
    }

    @Test
    @DatabaseTest
    public void updateClientAsAdminWithNoAccess() {
        authNoAccess();
        try {
            updateClient();
            fail("Expected 403");
        } catch (ClientRegistrationException e) {
            assertEquals(403, ((HttpErrorException) e.getCause()).getStatusLine().getStatusCode());
        }
    }

    @Test
    @DatabaseTest
    public void updateClientNotFound() {
        authManageClients();
        try {
            ClientRepresentation client = new ClientRepresentation();
            client.setClientId("invalid");

            reg.update(client);

            fail("Expected 404");
        } catch (ClientRegistrationException e) {
            assertEquals(404, ((HttpErrorException) e.getCause()).getStatusLine().getStatusCode());
        }
    }

    @Test
    @DatabaseTest
    public void updateClientWithNonAsciiChars() throws ClientRegistrationException {
        authCreateClients();
        registerClient(buildClient());

        authManageClients();
        ClientRepresentation client = reg.get(CLIENT_ID);
        String name = "Cli\u00EBnt";
        client.setName(name);

        ClientRepresentation updatedClient = reg.update(client);
        assertEquals(name, updatedClient.getName());
    }

    @Test
    @DatabaseTest
    public void deleteClientAsAdmin() throws ClientRegistrationException {
        authCreateClients();
        ClientRepresentation client = registerClient(buildClient());

        authManageClients();
        deleteClient(client);
    }

    @Test
    @DatabaseTest
    public void deleteClientAsAdminWithCreateOnly() throws ClientRegistrationException {
        authManageClients();
        ClientRepresentation client = registerClient(buildClient());
        try {
            authCreateClients();
            deleteClient(client);
            fail("Expected 403");
        } catch (ClientRegistrationException e) {
            assertEquals(403, ((HttpErrorException) e.getCause()).getStatusLine().getStatusCode());
        }
    }

    @Test
    @DatabaseTest
    public void deleteClientAsAdminWithNoAccess() throws ClientRegistrationException {
        authManageClients();
        ClientRepresentation client = registerClient(buildClient());
        try {
            authNoAccess();
            deleteClient(client);
            fail("Expected 403");
        } catch (ClientRegistrationException e) {
            assertEquals(403, ((HttpErrorException) e.getCause()).getStatusLine().getStatusCode());
        }
    }

    @Test
    @DatabaseTest
    public void registerClientAsAdminWithScope() throws ClientRegistrationException {
        authManageClients();
        ClientRepresentation client = new ClientRepresentation();
        client.setClientId(CLIENT_ID);
        client.setSecret(CLIENT_SECRET);
        ArrayList<String> optionalClientScopes = new ArrayList<>(Arrays.asList("address","phone"));
        client.setOptionalClientScopes(optionalClientScopes);

        ClientRepresentation createdClient = reg.create(client);
        assertEquals(CLIENT_ID, createdClient.getClientId());
        client = adminClient.realm(REALM_NAME).clients().get(createdClient.getId()).toRepresentation();
        assertEquals(CLIENT_ID, client.getClientId());
        // Remove this client after test

        deleteClient(createdClient);

        Set<String> requestedClientScopes = new HashSet<>(optionalClientScopes);
        Set<String> registeredClientScopes = new HashSet<>(client.getOptionalClientScopes());
        assertEquals(requestedClientScopes, registeredClientScopes);
        assertTrue(CollectionUtil.collectionEquals(client.getDefaultClientScopes(), Set.of("basic")));
    }

    @Test
    @DatabaseTest
    public void registerClientAsAdminWithoutScope() throws ClientRegistrationException {
        Set<String> realmDefaultClientScopes = adminClient.realm(REALM_NAME).getDefaultDefaultClientScopes().stream()
                .filter(scope -> Objects.equals(scope.getProtocol(), OIDCLoginProtocol.LOGIN_PROTOCOL))
                .map(ClientScopeRepresentation::getName).collect(Collectors.toSet());
        Set<String> realmOptionalClientScopes = adminClient.realm(REALM_NAME).getDefaultOptionalClientScopes().stream()
                .filter(scope -> Objects.equals(scope.getProtocol(), OIDCLoginProtocol.LOGIN_PROTOCOL))
                .map(ClientScopeRepresentation::getName).collect(Collectors.toSet());

        authManageClients();
        ClientRepresentation client = new ClientRepresentation();
        client.setClientId(CLIENT_ID);
        client.setSecret(CLIENT_SECRET);

        ClientRepresentation createdClient = reg.create(client);
        assertEquals(CLIENT_ID, createdClient.getClientId());
        client = adminClient.realm(REALM_NAME).clients().get(createdClient.getId()).toRepresentation();
        assertEquals(CLIENT_ID, client.getClientId());
        // Remove this client after test
        deleteClient(createdClient);

        assertEquals(realmDefaultClientScopes, new HashSet<>(client.getDefaultClientScopes()));
        assertEquals(realmOptionalClientScopes, new HashSet<>(client.getOptionalClientScopes()));
    }

    @Test
    public void registerClientAsAdminWithNotDefinedScope() {
        authManageClients();
        ClientRepresentation client = new ClientRepresentation();
        client.setClientId(CLIENT_ID);
        client.setSecret(CLIENT_SECRET);
        client.setOptionalClientScopes(new ArrayList<>(Arrays.asList("notdefinedscope","phone")));
        try {
            registerClient(client);
            fail("Expected 403");
        } catch (ClientRegistrationException e) {
            assertEquals(403, ((HttpErrorException) e.getCause()).getStatusLine().getStatusCode());
        }
    }

    @Test
    public void registerClientWithWrongCharacters() throws IOException {
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            HttpPost post = new HttpPost(keycloakUrls.getBaseBuilder().path("/realms/master/clients-registrations/openid-connect").build());
            post.setEntity(new StringEntity("{\"<img src=alert(1)>\":1}"));
            post.setHeader("Content-Type", APPLICATION_JSON);

            try (CloseableHttpResponse response = client.execute(post)) {
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
        ClientInitialAccessPresentation response = adminClient.realm(REALM_NAME).clientInitialAccess().create(clientInitialAccessCreatePresentation);

        ExecutorService threadPool = Executors.newFixedThreadPool(concurrentThreads);
        AtomicInteger createdCount = new AtomicInteger();
        try {
            Collection<Callable<Void>> futures = new LinkedList<>();
            for (int i = 0; i < iterations; i ++) {
                final int j = i;

                Callable<Void> f = () -> {
                    ClientRegistration client = ClientRegistration.create().url(keycloakUrls.getBase(), REALM_NAME).build();
                    client.auth(Auth.token(response));
                    ClientRepresentation rep = new ClientRepresentation();
                    rep.setClientId("test-" + j);
                    rep = client.create(rep);
                    if(rep.getId() != null && rep.getClientId().equals("test-" + j)) {
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

        Response response = adminClient.realm("master").clients().create(testAdminClient);
        testAdminClient = adminClient.realm("master").clients().get( ApiUtil.getCreatedId(response)).toRepresentation();

        UserRepresentation serviceAccountUser = adminClient.realm("master").clients().get(testAdminClient.getId()).getServiceAccountUser();
        RoleRepresentation adminRole =  adminClient.realm("master").roles().get("admin").toRepresentation();
        adminClient.realm("master").users().get(serviceAccountUser.getId()).roles().realmLevel().add(List.of(adminRole));

        oauth.client(TEST_ADMIN_CLIENT, TEST_ADMIN_CLIENT);
        oauth.realm("master");
        String token = oauth.doClientCredentialsGrantAccessTokenRequest().getAccessToken();
        ClientRegistration masterReg = ClientRegistration.create().url(keycloakUrls.getBase(), "master").build();
        masterReg.auth(Auth.token(token));

        ClientRepresentation client = new ClientRepresentation();
        client.setClientId(CLIENT_ID);
        client.setSecret(CLIENT_SECRET);

        ClientRepresentation createdClient = masterReg.create(client);
        assertNotNull(createdClient);

        adminClient.realm("master").clients().get(testAdminClient.getId()).remove();
        adminClient.realm("master").clients().get(createdClient.getId()).remove();
    }

    // ========== Realm Configuration ==========

    public static class ClientRegistrationRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            realm.name(REALM_NAME)
                    .clients(ClientBuilder.create("myclient-test")
                            .publicClient(true)
                            .directAccessGrantsEnabled(true));

            UserBuilder manage_client_user_builder =  UserBuilder.create()
                                                                 .username("manage-clients")
                                                                 .name("manage", "clients")
                                                                 .enabled(true)
                                                                 .password("password")
                                                                 .email("manage-clients@test.com")
                                                                 .emailVerified(true)
                                                                 .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.MANAGE_CLIENTS);

            UserBuilder create_client_user_builder =  UserBuilder.create()
                                                                 .username("create-clients")
                                                                 .name("create", "clients")
                                                                 .enabled(true)
                                                                 .password("password")
                                                                 .email("create-clients@test.com")
                                                                 .emailVerified(true)
                                                                 .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.CREATE_CLIENT);

            UserBuilder no_access_user_builder =   UserBuilder.create()
                                                              .username("no-access")
                                                              .name("no", "access")
                                                              .enabled(true)
                                                              .password("password")
                                                              .email("no-access@test.com")
                                                              .emailVerified(true);
            realm.users(manage_client_user_builder, create_client_user_builder, no_access_user_builder);
            return realm;
        }
    }

    // ========== Helper Methods ==========

    private ClientRepresentation buildClient() {
        ClientRepresentation client = new ClientRepresentation();
        client.setClientId(CLIENT_ID);
        client.setSecret(CLIENT_SECRET);
        return client;
    }

    private ClientRepresentation registerClient(ClientRepresentation client) throws ClientRegistrationException {
        ClientRepresentation createdClient = reg.create(client);
        assertEquals(CLIENT_ID, createdClient.getClientId());
        return createdClient;
    }

    private void authManageClients() {
        String token = getToken("manage-clients", "password");
        reg.auth(Auth.token(token));
    }

    private void authCreateClients() {
        String token = getToken("create-clients", "password");
        reg.auth(Auth.token(token));
    }

    private void authNoAccess() {
        String token = getToken("no-access", "password");
        reg.auth(Auth.token(token));
    }

    private String getToken(String username, String password) {
        try {
            org.keycloak.testsuite.util.oauth.AccessTokenResponse response = oauth.realm(REALM_NAME)
                    .client(Constants.ADMIN_CLI_CLIENT_ID)
                    .doPasswordGrantRequest(username, password);

            if (response.getStatusCode() != 200) {
                throw new RuntimeException("Token request failed. Status: " + response.getStatusCode() +
                        ", Error: " + response.getError() +
                        ", Description: " + response.getErrorDescription());
            }

            return response.getAccessToken();
        } catch (Exception e) {
            throw new RuntimeException("Failed to get token for user: " + username, e);
        }
    }

    private void deleteClient(ClientRepresentation client) throws ClientRegistrationException {
        reg.delete(CLIENT_ID);
        try {
            adminClient.realm(REALM_NAME).clients().get(client.getId()).toRepresentation();
            Assertions.fail("Expected 403");
        } catch (NotFoundException e) {
        }
    }

    private String getToken(String clientId, String clientSecret, String username, String password) {
        try {
            return oauth.client(clientId, clientSecret).doPasswordGrantRequest(username, password).getAccessToken();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void testClientUriValidation(String expectedRootUrlError, String expectedBaseUrlError, String expectedBackchannelLogoutUrlError, String expectedRedirectUrisError, String... testUrls) {
        testClientUriValidation(true, expectedRootUrlError, expectedBaseUrlError, expectedBackchannelLogoutUrlError, expectedRedirectUrisError, testUrls);
        testClientUriValidation(false, expectedRootUrlError, expectedBaseUrlError, expectedBackchannelLogoutUrlError, expectedRedirectUrisError, testUrls);
    }

    private void testClientUriValidation(boolean register, String expectedRootUrlError, String expectedBaseUrlError, String expectedBackchannelLogoutUrlError, String expectedRedirectUrisError, String... testUrls) {
        ClientRepresentation rep;
        if (register) {
            authCreateClients();
            rep = buildClient();
        }
        else {
            try {
                setupClientAsAdmin();
                rep = reg.get(CLIENT_ID);
            }
            catch (ClientRegistrationException e) {
                throw new RuntimeException(e);
            }
        }

        for (String testUrl : testUrls) {
            if (expectedRootUrlError != null) {
                rep.setRootUrl(testUrl);
                registerOrUpdateClientExpectingValidationErrors(rep, register, false, expectedRootUrlError);
            }
            rep.setRootUrl(null);

            if (expectedBaseUrlError != null) {
                rep.setBaseUrl(testUrl);
                registerOrUpdateClientExpectingValidationErrors(rep, register, false, expectedBaseUrlError);
            }
            rep.setBaseUrl(null);

            if (expectedBackchannelLogoutUrlError != null) {
                OIDCAdvancedConfigWrapper.fromClientRepresentation(rep).setBackchannelLogoutUrl(testUrl);
                registerOrUpdateClientExpectingValidationErrors(rep, register, false, expectedBackchannelLogoutUrlError);
            }
            OIDCAdvancedConfigWrapper.fromClientRepresentation(rep).setBackchannelLogoutUrl(null);

            if (expectedRedirectUrisError != null) {
                rep.setRedirectUris(Collections.singletonList(testUrl));
                registerOrUpdateClientExpectingValidationErrors(rep, register, true, expectedRedirectUrisError);
            }
            rep.setRedirectUris(null);

            if (expectedRootUrlError != null) rep.setRootUrl(testUrl);
            if (expectedBaseUrlError != null) rep.setBaseUrl(testUrl);
            if (expectedRedirectUrisError != null) rep.setRedirectUris(Collections.singletonList(testUrl));
            registerOrUpdateClientExpectingValidationErrors(rep, register, expectedRedirectUrisError != null, expectedRootUrlError, expectedBaseUrlError, expectedRedirectUrisError);

            rep.setRootUrl(null);
            rep.setBaseUrl(null);
            rep.setRedirectUris(null);
        }
    }

    private ClientRepresentation setupClientAsAdmin() throws ClientRegistrationException {
        authManageClients();
        ClientRepresentation client = buildClient();
        return registerClient(client);
    }

    private void registerOrUpdateClientExpectingValidationErrors(ClientRepresentation rep, boolean register, boolean redirectUris, String... expectedErrors) {
        HttpErrorException errorException = null;
        try {
            if (register) {
                registerClient(rep);
            }
            else {
                reg.update(rep);
            }
            Assertions.fail("Expected exception");
        }
        catch (ClientRegistrationException e) {
            errorException = (HttpErrorException) e.getCause();
        }

        expectedErrors = Arrays.stream(expectedErrors).filter(Objects::nonNull).toArray(String[]::new);

        assertEquals(400, errorException.getStatusLine().getStatusCode());
        OAuth2ErrorRepresentation errorRep;
        try {
            errorRep = JsonSerialization.readValue(errorException.getErrorResponse(), OAuth2ErrorRepresentation.class);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        List<String> actualErrors = asList(errorRep.getErrorDescription().split("; "));
        assertThat(actualErrors, containsInAnyOrder(expectedErrors));
        Assertions.assertEquals(redirectUris ? INVALID_REDIRECT_URI : INVALID_CLIENT_METADATA, errorRep.getError());
    }

    private void testSamlSpecificUrls(boolean register, String... testUrls) throws ClientRegistrationException {
        ClientRepresentation rep = buildClient();
        rep.setProtocol(SamlProtocol.LOGIN_PROTOCOL);
        if (register) {
            authCreateClients();
        } else {
            authManageClients();
            registerClient(rep);
            rep = reg.get(CLIENT_ID);
        }
        rep.setAttributes(new HashMap<>());

        Map<String, String> attrs = Map.of(
                SamlProtocol.SAML_ASSERTION_CONSUMER_URL_POST_ATTRIBUTE, "Assertion Consumer Service POST Binding URL",
                SamlProtocol.SAML_ASSERTION_CONSUMER_URL_REDIRECT_ATTRIBUTE, "Assertion Consumer Service Redirect Binding URL",
                SamlProtocol.SAML_ASSERTION_CONSUMER_URL_ARTIFACT_ATTRIBUTE, "Artifact Binding URL",
                SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_POST_ATTRIBUTE, "Logout Service POST Binding URL",
                SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_ARTIFACT_ATTRIBUTE, "Logout Service ARTIFACT Binding URL",
                SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_REDIRECT_ATTRIBUTE, "Logout Service Redirect Binding URL",
                SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_SOAP_ATTRIBUTE, "Logout Service SOAP Binding URL",
                SamlProtocol.SAML_ARTIFACT_RESOLUTION_SERVICE_URL_ATTRIBUTE, "Artifact Resolution Service");

        for (String testUrl : testUrls) {
            // admin url
            rep.setAdminUrl(testUrl);
            registerOrUpdateClientExpectingValidationErrors(rep, register, false, "Master SAML Processing URL uses an illegal scheme");
            rep.setAdminUrl(null);
            // attributes
            for (Map.Entry<String, String> entry : attrs.entrySet()) {
                rep.getAttributes().put(entry.getKey(), testUrl);
                registerOrUpdateClientExpectingValidationErrors(rep, register, false, entry.getValue() + " uses an illegal scheme");
                rep.getAttributes().remove(entry.getKey());
            }
        }
    }

    private void updateClient() throws ClientRegistrationException {
        ClientRepresentation client = reg.get(CLIENT_ID);
        client.setRedirectUris(Collections.singletonList("http://localhost:8080/app"));

        reg.update(client);

        ClientRepresentation updatedClient = reg.get(CLIENT_ID);

        assertEquals(1, updatedClient.getRedirectUris().size());
        assertEquals("http://localhost:8080/app", updatedClient.getRedirectUris().get(0));
    }

    private void addProtocolMapper(ClientRepresentation client, String mapperName) {
        ProtocolMapperRepresentation mapper = new ProtocolMapperRepresentation();
        mapper.setName(mapperName);
        mapper.setProtocol("openid-connect");
        mapper.setProtocolMapper("oidc-usermodel-attribute-mapper");
        mapper.getConfig().put("userinfo.token.claim", "true");
        mapper.getConfig().put("user.attribute", "someAttribute");
        mapper.getConfig().put("id.token.claim", "true");
        mapper.getConfig().put("access.token.claim", "true");
        mapper.getConfig().put("claim.name", "someClaimName");
        mapper.getConfig().put("jsonType.label", "long");

        client.setProtocolMappers(new ArrayList<>());
        client.getProtocolMappers().add(mapper);
    }
}
