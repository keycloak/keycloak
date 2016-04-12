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

import org.junit.Before;
import org.junit.Test;
import org.keycloak.client.registration.Auth;
import org.keycloak.client.registration.ClientRegistrationException;
import org.keycloak.client.registration.HttpErrorException;
import org.keycloak.representations.idm.ClientRepresentation;

import static org.junit.Assert.*;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class RegistrationAccessTokenTest extends AbstractClientRegistrationTest {

    private ClientRepresentation client;

    @Before
    public void before() throws Exception {
        super.before();

        ClientRepresentation c = new ClientRepresentation();
        c.setEnabled(true);
        c.setClientId("RegistrationAccessTokenTest");
        c.setSecret("RegistrationAccessTokenTestClientSecret");
        c.setRootUrl("http://root");

        client = createClient(c);

        reg.auth(Auth.token(client.getRegistrationAccessToken()));
    }

    private ClientRepresentation assertRead(String id, String registrationAccess, boolean expectSuccess) throws ClientRegistrationException {
        if (expectSuccess) {
            reg.auth(Auth.token(registrationAccess));
            ClientRepresentation rep = reg.get(id);
            assertNotNull(rep);
            return rep;
        } else {
            reg.auth(Auth.token(registrationAccess));
            try {
                reg.get(client.getClientId());
                fail("Expected 403");
            } catch (Exception e) {
                assertEquals(401, ((HttpErrorException) e.getCause()).getStatusLine().getStatusCode());
            }
        }
        return null;
    }

    @Test
    public void getClientWithRegistrationToken() throws ClientRegistrationException {
        ClientRepresentation rep = reg.get(client.getClientId());
        assertNotNull(rep);
        assertNotEquals(client.getRegistrationAccessToken(), rep.getRegistrationAccessToken());

        // check registration access token is updated
        assertRead(client.getClientId(), client.getRegistrationAccessToken(), false);
        assertRead(client.getClientId(), rep.getRegistrationAccessToken(), true);
    }

    @Test
    public void getClientWithBadRegistrationToken() throws ClientRegistrationException {
        reg.auth(Auth.token("invalid"));
        try {
            reg.get(client.getClientId());
            fail("Expected 401");
        } catch (ClientRegistrationException e) {
            assertEquals(401, ((HttpErrorException) e.getCause()).getStatusLine().getStatusCode());
        }
    }

    @Test
    public void updateClientWithRegistrationToken() throws ClientRegistrationException {
        client.setRootUrl("http://newroot");

        ClientRepresentation rep = reg.update(client);

        assertEquals("http://newroot", getClient(client.getId()).getRootUrl());
        assertNotEquals(client.getRegistrationAccessToken(), rep.getRegistrationAccessToken());

        // check registration access token is updated
        assertRead(client.getClientId(), client.getRegistrationAccessToken(), false);
        assertRead(client.getClientId(), rep.getRegistrationAccessToken(), true);
    }

    @Test
    public void updateClientWithBadRegistrationToken() throws ClientRegistrationException {
        client.setRootUrl("http://newroot");

        reg.auth(Auth.token("invalid"));
        try {
            reg.update(client);
            fail("Expected 401");
        } catch (ClientRegistrationException e) {
            assertEquals(401, ((HttpErrorException) e.getCause()).getStatusLine().getStatusCode());
        }

        assertEquals("http://root", getClient(client.getId()).getRootUrl());
    }

    @Test
    public void deleteClientWithRegistrationToken() throws ClientRegistrationException {
        reg.delete(client);
        assertNull(getClient(client.getId()));
    }

    @Test
    public void deleteClientWithBadRegistrationToken() throws ClientRegistrationException {
        reg.auth(Auth.token("invalid"));
        try {
            reg.delete(client);
            fail("Expected 401");
        } catch (ClientRegistrationException e) {
            assertEquals(401, ((HttpErrorException) e.getCause()).getStatusLine().getStatusCode());
        }
        assertNotNull(getClient(client.getId()));
    }

}
