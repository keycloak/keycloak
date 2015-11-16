package org.keycloak.testsuite.client;

import org.junit.Before;
import org.junit.Test;
import org.keycloak.client.registration.Auth;
import org.keycloak.client.registration.ClientRegistrationException;
import org.keycloak.client.registration.HttpErrorException;
import org.keycloak.representations.adapters.config.AdapterConfig;
import org.keycloak.representations.idm.ClientRepresentation;

import javax.ws.rs.core.Response;

import static org.junit.Assert.*;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class AdapterInstallationConfigTest extends AbstractClientRegistrationTest {

    private ClientRepresentation client;
    private ClientRepresentation client2;
    private ClientRepresentation clientPublic;

    @Before
    public void before() throws Exception {
        super.before();

        client = new ClientRepresentation();
        client.setEnabled(true);
        client.setClientId("RegistrationAccessTokenTest");
        client.setSecret("RegistrationAccessTokenTestClientSecret");
        client.setPublicClient(false);
        client.setRegistrationAccessToken("RegistrationAccessTokenTestRegistrationAccessToken");
        client.setRootUrl("http://root");
        client = createClient(client);

        client2 = new ClientRepresentation();
        client2.setEnabled(true);
        client2.setClientId("RegistrationAccessTokenTest2");
        client2.setSecret("RegistrationAccessTokenTestClientSecret");
        client2.setPublicClient(false);
        client2.setRegistrationAccessToken("RegistrationAccessTokenTestRegistrationAccessToken");
        client2.setRootUrl("http://root");
        client2 = createClient(client2);

        clientPublic = new ClientRepresentation();
        clientPublic.setEnabled(true);
        clientPublic.setClientId("RegistrationAccessTokenTestPublic");
        clientPublic.setPublicClient(true);
        clientPublic.setRegistrationAccessToken("RegistrationAccessTokenTestRegistrationAccessTokenPublic");
        clientPublic.setRootUrl("http://root");
        clientPublic = createClient(clientPublic);
    }

    @Test
    public void getConfigWithRegistrationAccessToken() throws ClientRegistrationException {
        reg.auth(Auth.token(client.getRegistrationAccessToken()));

        AdapterConfig config = reg.getAdapterConfig(client.getClientId());
        assertNotNull(config);
    }

    @Test
    public void getConfig() throws ClientRegistrationException {
        reg.auth(Auth.client(client.getClientId(), client.getSecret()));

        AdapterConfig config = reg.getAdapterConfig(client.getClientId());
        assertNotNull(config);
    }

    @Test
    public void getConfigMissingSecret() throws ClientRegistrationException {
        reg.auth(null);

        try {
            reg.getAdapterConfig(client.getClientId());
            fail("Expected 403");
        } catch (ClientRegistrationException e) {
            assertEquals(403, ((HttpErrorException) e.getCause()).getStatusLine().getStatusCode());
        }
    }

    @Test
    public void getConfigWrongClient() throws ClientRegistrationException {
        reg.auth(Auth.client(client.getClientId(), client.getSecret()));

        try {
            reg.getAdapterConfig(client2.getClientId());
            fail("Expected 403");
        } catch (ClientRegistrationException e) {
            assertEquals(403, ((HttpErrorException) e.getCause()).getStatusLine().getStatusCode());
        }
    }

    @Test
    public void getConfigPublicClient() throws ClientRegistrationException {
        reg.auth(null);

        AdapterConfig config = reg.getAdapterConfig(clientPublic.getClientId());
        assertNotNull(config);
    }

}
