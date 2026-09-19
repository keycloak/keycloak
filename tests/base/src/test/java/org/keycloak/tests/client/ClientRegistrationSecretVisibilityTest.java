package org.keycloak.tests.client;

import org.keycloak.client.registration.Auth;
import org.keycloak.client.registration.ClientRegistrationException;
import org.keycloak.representations.idm.ClientInitialAccessCreatePresentation;
import org.keycloak.representations.idm.ClientInitialAccessPresentation;
import org.keycloak.representations.oidc.OIDCClientRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@KeycloakIntegrationTest
public class ClientRegistrationSecretVisibilityTest extends AbstractClientRegistrationTest {

    @BeforeEach
    @Override
    public void before() throws Exception {
        super.before();

        ClientInitialAccessPresentation token =
                managedRealm.admin().clientInitialAccess()
                        .create(new ClientInitialAccessCreatePresentation(0, 10));

        reg.auth(Auth.token(token));
    }

    @Test
    public void viewClients_getOIDC_secretIsNotExposed() throws ClientRegistrationException {
        OIDCClientRepresentation created = createConfidentialClient();

        reg.auth(Auth.token(getToken("view-clients", "password")));

        OIDCClientRepresentation response = reg.oidc().get(created.getClientId());

        Assertions.assertNotNull(response);
        Assertions.assertEquals("**********", response.getClientSecret());
    }

    @Test
    public void manageClients_getOIDC_secretIsVisible() throws ClientRegistrationException {
        OIDCClientRepresentation created = createConfidentialClient();

        reg.auth(Auth.token(getToken("manage-clients", "password")));

        OIDCClientRepresentation response = reg.oidc().get(created.getClientId());

        Assertions.assertNotNull(response);
        Assertions.assertEquals(created.getClientSecret(), response.getClientSecret());
    }

    @Test
    public void registrationAccessToken_getOIDC_secretIsVisible() throws ClientRegistrationException {
        OIDCClientRepresentation created = createConfidentialClient();

        reg.auth(Auth.token(created));

        OIDCClientRepresentation response = reg.oidc().get(created.getClientId());

        Assertions.assertNotNull(response);
        Assertions.assertEquals(created.getClientSecret(), response.getClientSecret());
    }

    private OIDCClientRepresentation createConfidentialClient() throws ClientRegistrationException {
        OIDCClientRepresentation client = new OIDCClientRepresentation();
        client.setClientName("SecretVisibilityTest");
        return reg.oidc().create(client);
    }
}
