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

import org.keycloak.client.registration.Auth;
import org.keycloak.client.registration.ClientRegistrationException;
import org.keycloak.client.registration.HttpErrorException;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.oidc.OIDCClientRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.remote.timeoffset.InjectTimeOffSet;
import org.keycloak.testframework.remote.timeoffset.TimeOffSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@KeycloakIntegrationTest
public class RegistrationAccessTokenTest extends AbstractClientRegistrationTest {

    @InjectTimeOffSet
    TimeOffSet timeOffSet;

    private ClientRepresentation client;

    @BeforeEach
    @Override
    public void before() throws Exception {
        super.before();

        ClientRepresentation c = ClientBuilder.create("RegistrationAccessTokenTest")
                .secret("RegistrationAccessTokenTestClientSecret")
                .rootUrl("http://root")
                .build();

        client = createClient(c);
        String clientId = client.getId();
        managedRealm.cleanup().add(r -> r.clients().delete(clientId).close());

        c = ClientBuilder.create("SomeOtherClient")
                .secret("RegistrationAccessTokenTestClientSecret")
                .rootUrl("http://root")
                .build();

        c = createClient(c);
        String cId = c.getId();
        managedRealm.cleanup().add(r -> r.clients().get(cId).remove());

        reg.auth(Auth.token(client.getRegistrationAccessToken()));
    }

    private OIDCClientRepresentation updateRep(String baseUrl) {
        OIDCClientRepresentation rep = new OIDCClientRepresentation();
        rep.setClientId(client.getClientId());
        rep.setClientUri(baseUrl);
        return rep;
    }

    private OIDCClientRepresentation assertRead(String id, String registrationAccess, boolean expectSuccess) throws ClientRegistrationException {
        if (expectSuccess) {
            reg.auth(Auth.token(registrationAccess));
            OIDCClientRepresentation rep = reg.oidc().get(id);
            assertNotNull(rep);
            return rep;
        } else {
            reg.auth(Auth.token(registrationAccess));
            try {
                reg.oidc().get(client.getClientId());
                fail("Expected 401");
            } catch (Exception e) {
                assertEquals(401, ((HttpErrorException) e.getCause()).getStatusLine().getStatusCode());
            }
        }
        return null;
    }

    @Test
    public void getClientWithRegistrationToken() throws ClientRegistrationException {
        timeOffSet.set(10);

        OIDCClientRepresentation rep = reg.oidc().get(client.getClientId());
        assertNotNull(rep);

        assertEquals(client.getRegistrationAccessToken(), rep.getRegistrationAccessToken());
        assertNotNull(rep.getRegistrationAccessToken());

        // KEYCLOAK-4984 check registration access token is not updated
        assertRead(client.getClientId(), client.getRegistrationAccessToken(), true);
    }

    @Test
    public void getClientWrongClient() {
        ClientRegistrationException e = assertThrows(ClientRegistrationException.class, () -> reg.oidc().get("SomeOtherClient"));
        assertEquals(401, ((HttpErrorException) e.getCause()).getStatusLine().getStatusCode());
    }

    @Test
    public void getClientMissingClient() {
        ClientRegistrationException e = assertThrows(ClientRegistrationException.class, () -> reg.oidc().get("nosuch"));
        assertEquals(401, ((HttpErrorException) e.getCause()).getStatusLine().getStatusCode());
    }

    @Test
    public void getClientWithBadRegistrationToken() throws ClientRegistrationException {
        String oldToken = client.getRegistrationAccessToken();
        reg.oidc().update(updateRep("http://newbase"));
        reg.auth(Auth.token(oldToken));
        try {
            reg.oidc().get(client.getClientId());
            fail("Expected 401");
        } catch (ClientRegistrationException e) {
            assertEquals(401, ((HttpErrorException) e.getCause()).getStatusLine().getStatusCode());
        }
    }

    @Test
    public void updateClientWithRegistrationToken() throws ClientRegistrationException {
        OIDCClientRepresentation rep = reg.oidc().update(updateRep("http://newbase"));

        assertEquals("http://newbase", getClient(client.getId()).getBaseUrl());
        assertNotEquals(client.getRegistrationAccessToken(), rep.getRegistrationAccessToken());

        // check registration access token is updated
        assertRead(client.getClientId(), client.getRegistrationAccessToken(), false);
        assertRead(client.getClientId(), rep.getRegistrationAccessToken(), true);
    }

    @Test
    public void updateClientWithBadRegistrationToken() throws ClientRegistrationException {
        String oldToken = client.getRegistrationAccessToken();
        reg.oidc().update(updateRep("http://newbase"));
        reg.auth(Auth.token(oldToken));
        try {
            reg.oidc().update(updateRep("http://otherbase"));
            fail("Expected 401");
        } catch (ClientRegistrationException e) {
            assertEquals(401, ((HttpErrorException) e.getCause()).getStatusLine().getStatusCode());
        }

        assertEquals("http://newbase", getClient(client.getId()).getBaseUrl());
    }

    @Test
    public void deleteClientWithRegistrationToken() throws ClientRegistrationException {
        reg.oidc().delete(client.getClientId());
        assertNull(getClient(client.getId()));
    }

    @Test
    public void deleteClientWithBadRegistrationToken() throws ClientRegistrationException {
        String oldToken = client.getRegistrationAccessToken();
        reg.oidc().update(updateRep("http://newbase"));
        reg.auth(Auth.token(oldToken));
        try {
            reg.oidc().delete(client.getClientId());
            fail("Expected 401");
        } catch (ClientRegistrationException e) {
            assertEquals(401, ((HttpErrorException) e.getCause()).getStatusLine().getStatusCode());
        }
        assertNotNull(getClient(client.getId()));
    }

    @Test
    public void testDisabledClientWithRegistrationToken() {
        // Admin disables the client
        ClientRepresentation adminRep = adminClient.realm(REALM_NAME).clients().get(client.getId()).toRepresentation();
        adminRep.setEnabled(false);
        adminClient.realm(REALM_NAME).clients().get(client.getId()).update(adminRep);

        //get
        try {
            reg.oidc().get(client.getClientId());
            fail("Should not be able to get a disabled client using a registration access token");
        } catch (ClientRegistrationException e) {
            assertEquals(401, ((HttpErrorException) e.getCause()).getStatusLine().getStatusCode());
        }

        //update
        try {
            reg.oidc().update(updateRep("http://newbase"));
            fail("Should not be able to update a disabled client using a registration access token");
        } catch (ClientRegistrationException e) {
            assertEquals(401, ((HttpErrorException) e.getCause()).getStatusLine().getStatusCode());
        }

        //delete
        try {
            reg.oidc().delete(client.getClientId());
            fail("Should not be able to delete a disabled client using a registration access token");
        } catch (ClientRegistrationException e) {
            assertEquals(401, ((HttpErrorException) e.getCause()).getStatusLine().getStatusCode());
        }

        ClientRepresentation updatedClient = adminClient.realm(REALM_NAME).clients().get(client.getId()).toRepresentation();
        assertNotNull(updatedClient);
        assertFalse(updatedClient.isEnabled());
    }

}
