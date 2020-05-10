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

import org.junit.Test;
import org.keycloak.client.registration.Auth;
import org.keycloak.client.registration.ClientRegistration;
import org.keycloak.client.registration.ClientRegistrationException;
import org.keycloak.client.registration.HttpErrorException;
import org.keycloak.models.Constants;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.OAuth2ErrorRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.services.clientregistration.ErrorCodes;
import org.keycloak.util.JsonSerialization;

import javax.ws.rs.NotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer;

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

        return client;
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
    public void registerClientValidation() throws IOException {
    	authCreateClients();
    	ClientRepresentation client = buildClient();
    	client.setRootUrl("invalid");

    	try {
            registerClient(client);
        } catch (ClientRegistrationException e) {
            HttpErrorException c = (HttpErrorException) e.getCause();
            assertEquals(400, c.getStatusLine().getStatusCode());

            OAuth2ErrorRepresentation error = JsonSerialization.readValue(c.getErrorResponse(), OAuth2ErrorRepresentation.class);

            assertEquals("invalid_client_metadata", error.getError());
            assertEquals("Invalid URL in rootUrl", error.getErrorDescription());
        }
    }

    @Test
    public void updateClientValidation() throws IOException, ClientRegistrationException {
        registerClientAsAdmin();

        ClientRepresentation client = reg.get(CLIENT_ID);
        client.setRootUrl("invalid");

    	try {
            reg.update(client);
        } catch (ClientRegistrationException e) {
            HttpErrorException c = (HttpErrorException) e.getCause();
            assertEquals(400, c.getStatusLine().getStatusCode());

            OAuth2ErrorRepresentation error = JsonSerialization.readValue(c.getErrorResponse(), OAuth2ErrorRepresentation.class);

            assertEquals("invalid_client_metadata", error.getError());
            assertEquals("Invalid URL in rootUrl", error.getErrorDescription());
        }

        ClientRepresentation updatedClient = reg.get(CLIENT_ID);
        assertNull(updatedClient.getRootUrl());
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

}
