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

import javax.ws.rs.NotFoundException;
import java.util.Collections;

import static org.junit.Assert.*;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ClientRegistrationTest extends AbstractClientRegistrationTest {

    private static final String CLIENT_ID = "test-client";
    private static final String CLIENT_SECRET = "test-client-secret";

    private ClientRepresentation registerClient() throws ClientRegistrationException {
        ClientRepresentation client = new ClientRepresentation();
        client.setClientId(CLIENT_ID);
        client.setSecret(CLIENT_SECRET);

        ClientRepresentation createdClient = reg.create(client);
        assertEquals(CLIENT_ID, createdClient.getClientId());

        client = adminClient.realm(REALM_NAME).clients().get(createdClient.getId()).toRepresentation();
        assertEquals(CLIENT_ID, client.getClientId());

        return client;
    }

    @Test
    public void registerClientAsAdmin() throws ClientRegistrationException {
        authManageClients();
        registerClient();
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
