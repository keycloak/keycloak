package org.keycloak.testsuite.client;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.client.registration.Auth;
import org.keycloak.client.registration.ClientRegistrationException;
import org.keycloak.representations.idm.ClientInitialAccessCreatePresentation;
import org.keycloak.representations.idm.ClientInitialAccessPresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.oidc.OIDCClientRepresentation;

import java.io.IOException;
import java.util.Collections;

import static org.junit.Assert.*;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class SAMLClientRegistrationTest extends AbstractClientRegistrationTest {

    @Before
    public void before() throws Exception {
        super.before();

        ClientInitialAccessPresentation token = adminClient.realm(REALM_NAME).clientInitialAccess().create(new ClientInitialAccessCreatePresentation(0, 10));
        reg.auth(Auth.token(token));
    }

    @Test
    public void createClient() throws ClientRegistrationException, IOException {
        String entityDescriptor = IOUtils.toString(getClass().getResourceAsStream("/clientreg-test/saml-entity-descriptor.xml"));
        ClientRepresentation response = reg.saml().create(entityDescriptor);

        assertNotNull(response.getRegistrationAccessToken());
        assertEquals("loadbalancer-9.siroe.com", response.getClientId());
        assertEquals(1, response.getRedirectUris().size());
        assertEquals("https://LoadBalancer-9.siroe.com:3443/federation/Consumer/metaAlias/sp", response.getRedirectUris().get(0));
    }

}
