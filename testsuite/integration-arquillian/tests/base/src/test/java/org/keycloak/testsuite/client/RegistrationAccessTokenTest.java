package org.keycloak.testsuite.client;

import org.junit.Before;
import org.junit.Test;
import org.keycloak.client.registration.Auth;
import org.keycloak.client.registration.ClientRegistrationException;
import org.keycloak.client.registration.HttpErrorException;
import org.keycloak.representations.idm.ClientRepresentation;

import javax.ws.rs.core.Response;

import static org.junit.Assert.*;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class RegistrationAccessTokenTest extends AbstractClientRegistrationTest {

    private ClientRepresentation client;

    @Before
    public void before() throws Exception {
        super.before();

        client = new ClientRepresentation();
        client.setEnabled(true);
        client.setClientId("RegistrationAccessTokenTest");
        client.setSecret("RegistrationAccessTokenTestClientSecret");
        client.setRegistrationAccessToken("RegistrationAccessTokenTestRegistrationAccessToken");
        client.setRootUrl("http://root");
        client = createClient(client);

        reg.auth(Auth.token(client.getRegistrationAccessToken()));
    }

    @Test
    public void getClientWithRegistrationToken() throws ClientRegistrationException {
        ClientRepresentation rep = reg.get(client.getClientId());
        assertNotNull(rep);
    }

    @Test
    public void getClientWithBadRegistrationToken() throws ClientRegistrationException {
        reg.auth(Auth.token("invalid"));
        try {
            reg.get(client.getClientId());
            fail("Expected 403");
        } catch (ClientRegistrationException e) {
            assertEquals(403, ((HttpErrorException) e.getCause()).getStatusLine().getStatusCode());
        }
    }

    @Test
    public void updateClientWithRegistrationToken() throws ClientRegistrationException {
        client.setRootUrl("http://newroot");
        reg.update(client);

        assertEquals("http://newroot", getClient(client.getId()).getRootUrl());
    }

    @Test
    public void updateClientWithBadRegistrationToken() throws ClientRegistrationException {
        client.setRootUrl("http://newroot");

        reg.auth(Auth.token("invalid"));
        try {
            reg.update(client);
            fail("Expected 403");
        } catch (ClientRegistrationException e) {
            assertEquals(403, ((HttpErrorException) e.getCause()).getStatusLine().getStatusCode());
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
            fail("Expected 403");
        } catch (ClientRegistrationException e) {
            assertEquals(403, ((HttpErrorException) e.getCause()).getStatusLine().getStatusCode());
        }
        assertNotNull(getClient(client.getId()));
    }

}
