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

import org.hamcrest.Matchers;
import org.junit.Test;
import org.keycloak.client.registration.Auth;
import org.keycloak.client.registration.ClientRegistration;
import org.keycloak.client.registration.ClientRegistrationException;
import org.keycloak.client.registration.HttpErrorException;
import org.keycloak.models.Constants;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.OAuth2ErrorRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer;
import org.keycloak.util.JsonSerialization;

import javax.ws.rs.NotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import static org.keycloak.services.clientregistration.ErrorCodes.INVALID_CLIENT_METADATA;
import static org.keycloak.services.clientregistration.ErrorCodes.INVALID_REDIRECT_URI;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@AuthServerContainerExclude(AuthServer.REMOTE)
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

        String token = oauth.doGrantAccessTokenRequest("master", "admin", "admin", null, Constants.ADMIN_CLI_CLIENT_ID, null).getAccessToken();
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
        assertTrue(client.getDefaultClientScopes().isEmpty());
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

}
