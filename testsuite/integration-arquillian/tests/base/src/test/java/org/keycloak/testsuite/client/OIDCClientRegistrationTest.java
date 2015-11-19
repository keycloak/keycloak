package org.keycloak.testsuite.client;

import org.junit.Before;
import org.junit.Test;
import org.keycloak.client.registration.Auth;
import org.keycloak.client.registration.ClientRegistrationException;
import org.keycloak.representations.idm.ClientInitialAccessCreatePresentation;
import org.keycloak.representations.idm.ClientInitialAccessPresentation;
import org.keycloak.representations.oidc.OIDCClientRepresentation;

import java.util.Collections;

import static org.junit.Assert.*;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class OIDCClientRegistrationTest extends AbstractClientRegistrationTest {

    @Before
    public void before() throws Exception {
        super.before();

        ClientInitialAccessPresentation token = adminClient.realm(REALM_NAME).clientInitialAccess().create(new ClientInitialAccessCreatePresentation(0, 10));
        reg.auth(Auth.token(token));
    }

    public OIDCClientRepresentation create() throws ClientRegistrationException {
        OIDCClientRepresentation client = new OIDCClientRepresentation();
        client.setClientName("RegistrationAccessTokenTest");
        client.setClientUri("http://root");
        client.setRedirectUris(Collections.singletonList("http://redirect"));

        OIDCClientRepresentation response = reg.oidc().create(client);

        return response;
    }

    @Test
    public void createClient() throws ClientRegistrationException {
        OIDCClientRepresentation response = create();

        assertNotNull(response.getRegistrationAccessToken());
        assertNotNull(response.getClientIdIssuedAt());
        assertNotNull(response.getClientId());
        assertNull(response.getClientSecretExpiresAt());
        assertNotNull(response.getRegistrationClientUri());
        assertEquals("RegistrationAccessTokenTest", response.getClientName());
        assertEquals("http://root", response.getClientUri());
        assertEquals(1, response.getRedirectUris().size());
        assertEquals("http://redirect", response.getRedirectUris().get(0));
    }

    @Test
    public void getClient() throws ClientRegistrationException {
        OIDCClientRepresentation response = create();
        reg.auth(Auth.token(response));

        OIDCClientRepresentation rep = reg.oidc().get(response.getClientId());
        assertNotNull(rep);
        assertNotEquals(response.getRegistrationAccessToken(), rep.getRegistrationAccessToken());
    }

    @Test
    public void updateClient() throws ClientRegistrationException {
        OIDCClientRepresentation response = create();
        reg.auth(Auth.token(response));

        response.setRedirectUris(Collections.singletonList("http://newredirect"));

        OIDCClientRepresentation updated = reg.oidc().update(response);

        assertEquals(1, updated.getRedirectUris().size());
        assertEquals("http://newredirect", updated.getRedirectUris().get(0));
    }

    @Test
    public void deleteClient() throws ClientRegistrationException {
        OIDCClientRepresentation response = create();
        reg.auth(Auth.token(response));

        reg.oidc().delete(response);
    }

}
