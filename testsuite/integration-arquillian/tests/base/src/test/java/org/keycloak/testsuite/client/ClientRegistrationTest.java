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

package org.keycloak.testsuite.client;

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

import org.keycloak.client.registration.Auth;
import org.keycloak.client.registration.ClientRegistration;
import org.keycloak.client.registration.ClientRegistrationException;
import org.keycloak.client.registration.HttpErrorException;
import org.keycloak.common.util.CollectionUtil;
import org.keycloak.events.Errors;
import org.keycloak.models.Constants;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.representations.idm.ClientInitialAccessCreatePresentation;
import org.keycloak.representations.idm.ClientInitialAccessPresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.OAuth2ErrorRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.ResourceServerRepresentation;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.annotation.UncaughtServerErrorExpected;
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
import org.hamcrest.Matchers;
import org.junit.Test;

import static java.util.Arrays.asList;

import static org.keycloak.services.clientregistration.ErrorCodes.INVALID_CLIENT_METADATA;
import static org.keycloak.services.clientregistration.ErrorCodes.INVALID_REDIRECT_URI;
import static org.keycloak.utils.MediaType.APPLICATION_JSON;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ClientRegistrationTest extends AbstractClientRegistrationTest {

    private static final String CLIENT_ID = "test-client";
    private static final String CLIENT_SECRET = "test-client-secret";

    private ClientRepresentation buildClient() {
    	ClientRepresentation client = new ClientRepresentation();
        client.setClientId(CLIENT_ID);
        client.setSecret(CLIENT_SECRET);

        return client;
    }

    private ClientRepresentation registerClient() throws ClientRegistrationException {
    	return registerClient(buildClient());
    }

    private ClientRepresentation registerClient(ClientRepresentation client) throws ClientRegistrationException {
        ClientRepresentation createdClient = reg.create(client);
        assertEquals(CLIENT_ID, createdClient.getClientId());

        client = adminClient.realm(REALM_NAME).clients().get(createdClient.getId()).toRepresentation();
        assertEquals(CLIENT_ID, client.getClientId());

        // Remove this client after test
        getCleanup().addClientUuid(createdClient.getId());

        return createdClient;
    }

    @Test
    public void registerClientAsAdmin() throws ClientRegistrationException {
        authManageClients();
        registerClient();
    }

    // KEYCLOAK-5907
    @Test
    public void withServiceAccount() throws ClientRegistrationException {
        authManageClients();
        ClientRepresentation clientRep = buildClient();
        clientRep.setServiceAccountsEnabled(true);

        ClientRepresentation rep = registerClient(clientRep);

        UserRepresentation serviceAccountUser = adminClient.realm("test").clients().get(rep.getId()).getServiceAccountUser();

        assertNotNull(serviceAccountUser);

        deleteClient(rep);

        try {
            adminClient.realm("test").users().get(serviceAccountUser.getId()).toRepresentation();
            fail("Expected NotFoundException");
        } catch (NotFoundException e) {
        }
    }

    @Test
    public void registerClientInMasterRealm() throws Exception {
        ClientRegistration masterReg = ClientRegistration.create().url(suiteContext.getAuthServerInfo().getContextRoot() + "/auth", "master").build();

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
    public void registerClientWithoutProtocol() throws ClientRegistrationException {
        authCreateClients();
        ClientRepresentation clientRepresentation = registerClient();

        assertEquals("openid-connect", clientRepresentation.getProtocol());
    }

    @Test
    public void registerClientAsAdminWithCreateOnly() throws ClientRegistrationException {
        authCreateClients();
        registerClient();
    }

    @Test
    public void registerClientAsAdminWithNoAccess() throws ClientRegistrationException {
        authNoAccess();
        try {
            registerClient();
            fail("Expected 403");
        } catch (ClientRegistrationException e) {
            assertEquals(403, ((HttpErrorException) e.getCause()).getStatusLine().getStatusCode());
        }
    }

    @Test
    public void registerClientUsingRevokedToken() throws Exception {
        reg.auth(Auth.token(getToken("manage-clients", "password")));

        ClientRepresentation myclient = new ClientRepresentation();

        myclient.setClientId("myclient");
        myclient.setServiceAccountsEnabled(true);
        myclient.setSecret("password");
        myclient.setDirectAccessGrantsEnabled(true);

        reg.create(myclient);

        oauth.clientId("myclient");
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
        }
    }

    @Test
    public void registerClientWithNonAsciiChars() throws ClientRegistrationException {
    	authCreateClients();
    	ClientRepresentation client = buildClient();
    	String name = "Cli\u00EBnt";
		client.setName(name);

    	ClientRepresentation createdClient = registerClient(client);
    	assertEquals(name, createdClient.getName());
    }

    @Test
    public void clientWithDefaultRoles() throws ClientRegistrationException {
        authCreateClients();
        ClientRepresentation client = buildClient();
        client.setDefaultRoles(new String[]{"test-default-role"});

        ClientRepresentation createdClient = registerClient(client);
        assertThat(createdClient.getDefaultRoles(), Matchers.arrayContaining("test-default-role"));

        authManageClients();
        ClientRepresentation obtainedClient = reg.get(CLIENT_ID);
        assertThat(obtainedClient.getDefaultRoles(), Matchers.arrayContaining("test-default-role"));

        client.setDefaultRoles(new String[]{"test-default-role1","test-default-role2"});
        ClientRepresentation updatedClient = reg.update(client);
        assertThat(updatedClient.getDefaultRoles(), Matchers.arrayContainingInAnyOrder("test-default-role1","test-default-role2"));
    }

    @Test
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
    public void testInvalidUrlClientValidation() {
        testClientUriValidation("Root URL is not a valid URL",
                "Base URL is not a valid URL",
                "Backchannel logout URL is not a valid URL",
                null,
                "invalid", "myapp://some-fake-app");
    }

    @Test
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
    public void testFragmentProhibitedClientValidation() {
        testClientUriValidation("Root URL must not contain an URL fragment",
                null,
                null,
                "Redirect URIs must not contain an URI fragment",
                "http://redhat.com/abcd#someFragment"
        );
    }

    @Test
    public void testSamlSpecificUrls() throws ClientRegistrationException {
        testSamlSpecificUrls(true, "javascript:alert('TEST')", "data:text/html;base64,PHNjcmlwdD5jb25maXJtKGRvY3VtZW50LmRvbWFpbik7PC9zY3JpcHQ+");
        testSamlSpecificUrls(false, "javascript:alert('TEST')", "data:text/html;base64,PHNjcmlwdD5jb25maXJtKGRvY3VtZW50LmRvbWFpbik7PC9zY3JpcHQ+");
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

    @Test
    public void testUpdateAuthorizationSettings() throws ClientRegistrationException {
        authManageClients();
        ClientRepresentation clientRep = buildClient();
        clientRep.setAuthorizationServicesEnabled(true);

        ClientRepresentation rep = registerClient(clientRep);
        rep = adminClient.realm("test").clients().get(rep.getId()).toRepresentation();

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
        authzSettings = adminClient.realm("test").clients().get(rep.getId()).authorization().exportSettings();

        assertFalse(authzSettings.getResources().isEmpty());
        assertFalse(authzSettings.getScopes().isEmpty());
        assertFalse(authzSettings.getPolicies().isEmpty());
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
                registerClientAsAdmin();
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

    private void registerOrUpdateClientExpectingValidationErrors(ClientRepresentation rep, boolean register, boolean redirectUris, String... expectedErrors) {
        HttpErrorException errorException = null;
        try {
            if (register) {
                registerClient(rep);
            }
            else {
                reg.update(rep);
            }
            fail("Expected exception");
        }
        catch (ClientRegistrationException e) {
            errorException = (HttpErrorException) e.getCause();
        }

        expectedErrors = Arrays.stream(expectedErrors).filter(Objects::nonNull).toArray(String[]::new);

        assertEquals(errorException.getStatusLine().getStatusCode(), 400);
        OAuth2ErrorRepresentation errorRep;
        try {
            errorRep = JsonSerialization.readValue(errorException.getErrorResponse(), OAuth2ErrorRepresentation.class);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        List<String> actualErrors = asList(errorRep.getErrorDescription().split("; "));
        assertThat(actualErrors, containsInAnyOrder(expectedErrors));
        assertEquals(redirectUris ? INVALID_REDIRECT_URI : INVALID_CLIENT_METADATA, errorRep.getError());
    }

    @Test
    public void getClientAsAdmin() throws ClientRegistrationException {
        registerClientAsAdmin();
        ClientRepresentation rep = reg.get(CLIENT_ID);
        assertNotNull(rep);
    }

    @Test
    public void getClientAsAdminWithCreateOnly() throws ClientRegistrationException {
        registerClientAsAdmin();
        authCreateClients();
        try {
            reg.get(CLIENT_ID);
            fail("Expected 403");
        } catch (ClientRegistrationException e) {
            assertEquals(403, ((HttpErrorException) e.getCause()).getStatusLine().getStatusCode());
        }
    }

    @Test
    public void getClientAsAdminWithNoAccess() throws ClientRegistrationException {
        registerClientAsAdmin();
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
        assertNull(reg.get("invalid"));
    }

    @Test
    public void getClientNotFoundNoAccess() throws ClientRegistrationException {
        authNoAccess();
        try {
            reg.get("invalid");
            fail("Expected 403");
        } catch (ClientRegistrationException e) {
            assertEquals(403, ((HttpErrorException) e.getCause()).getStatusLine().getStatusCode());
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


    @Test
    public void updateClientAsAdmin() throws ClientRegistrationException {
        registerClientAsAdmin();

        authManageClients();
        updateClient();
    }

    @Test
    public void updateClientSecret() throws ClientRegistrationException {
        authManageClients();

        registerClient();

        ClientRepresentation client = reg.get(CLIENT_ID);
        assertNotNull(client.getSecret());
        client.setSecret("mysecret");

        reg.update(client);

        ClientRepresentation updatedClient = reg.get(CLIENT_ID);

        assertEquals("mysecret", updatedClient.getSecret());
    }

    @Test
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

    @Test
    public void updateClientAsAdminWithCreateOnly() throws ClientRegistrationException {
        authCreateClients();
        try {
            updateClient();
            fail("Expected 403");
        } catch (ClientRegistrationException e) {
            assertEquals(403, ((HttpErrorException) e.getCause()).getStatusLine().getStatusCode());
        }
    }

    @Test
    public void updateClientAsAdminWithNoAccess() throws ClientRegistrationException {
        authNoAccess();
        try {
            updateClient();
            fail("Expected 403");
        } catch (ClientRegistrationException e) {
            assertEquals(403, ((HttpErrorException) e.getCause()).getStatusLine().getStatusCode());
        }
    }

    @Test
    public void updateClientNotFound() throws ClientRegistrationException {
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
    public void updateClientWithNonAsciiChars() throws ClientRegistrationException {
    	authCreateClients();
    	registerClient();

    	authManageClients();
    	ClientRepresentation client = reg.get(CLIENT_ID);
    	String name = "Cli\u00EBnt";
		client.setName(name);

    	ClientRepresentation updatedClient = reg.update(client);
    	assertEquals(name, updatedClient.getName());
    }

    private void deleteClient(ClientRepresentation client) throws ClientRegistrationException {
        reg.delete(CLIENT_ID);
        try {
            adminClient.realm("test").clients().get(client.getId()).toRepresentation();
            fail("Expected 403");
        } catch (NotFoundException e) {
        }
    }

    @Test
    public void deleteClientAsAdmin() throws ClientRegistrationException {
        authCreateClients();
        ClientRepresentation client = registerClient();

        authManageClients();
        deleteClient(client);
    }

    @Test
    public void deleteClientAsAdminWithCreateOnly() throws ClientRegistrationException {
        authManageClients();
        ClientRepresentation client = registerClient();
        try {
            authCreateClients();
            deleteClient(client);
            fail("Expected 403");
        } catch (ClientRegistrationException e) {
            assertEquals(403, ((HttpErrorException) e.getCause()).getStatusLine().getStatusCode());
        }
    }

    @Test
    public void deleteClientAsAdminWithNoAccess() throws ClientRegistrationException {
        authManageClients();
        ClientRepresentation client = registerClient();
        try {
            authNoAccess();
            deleteClient(client);
            fail("Expected 403");
        } catch (ClientRegistrationException e) {
            assertEquals(403, ((HttpErrorException) e.getCause()).getStatusLine().getStatusCode());
        }
    }

    @Test
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
        getCleanup().addClientUuid(createdClient.getId());

        Set<String> requestedClientScopes = new HashSet<>(optionalClientScopes);
        Set<String> registeredClientScopes = new HashSet<>(client.getOptionalClientScopes());
        assertTrue(requestedClientScopes.equals(registeredClientScopes));
        assertTrue(CollectionUtil.collectionEquals(client.getDefaultClientScopes(), Set.of("basic")));
    }

    @Test
    public void registerClientAsAdminWithoutScope() throws ClientRegistrationException {
        Set<String> realmDefaultClientScopes = new HashSet<>(adminClient.realm(REALM_NAME).getDefaultDefaultClientScopes().stream()
                .filter(scope -> Objects.equals(scope.getProtocol(), OIDCLoginProtocol.LOGIN_PROTOCOL))
                .map(i->i.getName()).collect(Collectors.toList()));
        Set<String> realmOptionalClientScopes = new HashSet<>(adminClient.realm(REALM_NAME).getDefaultOptionalClientScopes().stream()
                .filter(scope -> Objects.equals(scope.getProtocol(), OIDCLoginProtocol.LOGIN_PROTOCOL))
                .map(i->i.getName()).collect(Collectors.toList()));

        authManageClients();
        ClientRepresentation client = new ClientRepresentation();
        client.setClientId(CLIENT_ID);
        client.setSecret(CLIENT_SECRET);

        ClientRepresentation createdClient = reg.create(client);
        assertEquals(CLIENT_ID, createdClient.getClientId());
        client = adminClient.realm(REALM_NAME).clients().get(createdClient.getId()).toRepresentation();
        assertEquals(CLIENT_ID, client.getClientId());
        // Remove this client after test
        getCleanup().addClientUuid(createdClient.getId());

        assertTrue(realmDefaultClientScopes.equals(new HashSet<>(client.getDefaultClientScopes())));
        assertTrue(realmOptionalClientScopes.equals(new HashSet<>(client.getOptionalClientScopes())));
    }

    @Test
    public void registerClientAsAdminWithNotDefinedScope() throws ClientRegistrationException {
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
    @UncaughtServerErrorExpected
    public void registerClientWithWrongCharacters() throws IOException {
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            HttpPost post = new HttpPost(suiteContext.getAuthServerInfo().getUriBuilder().path("/auth/realms/master/clients-registrations/openid-connect").build());
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
                    ClientRegistration client = ClientRegistration.create().url(suiteContext.getAuthServerInfo().getContextRoot() + "/auth", "test").build();
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
        ClientRegistration masterReg = ClientRegistration.create().url(suiteContext.getAuthServerInfo().getContextRoot() + "/auth", "master").build();
        masterReg.auth(Auth.token(token));

        ClientRepresentation client = new ClientRepresentation();
        client.setClientId(CLIENT_ID);
        client.setSecret(CLIENT_SECRET);

        ClientRepresentation createdClient = masterReg.create(client);
        assertNotNull(createdClient);

        adminClient.realm("master").clients().get(testAdminClient.getId()).remove();
        adminClient.realm("master").clients().get(createdClient.getId()).remove();
    }
}
