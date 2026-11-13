package org.keycloak.tests.client;

import java.util.List;

import org.keycloak.OAuth2Constants;
import org.keycloak.client.registration.Auth;
import org.keycloak.client.registration.ClientRegistrationException;
import org.keycloak.client.registration.HttpErrorException;
import org.keycloak.representations.idm.ClientInitialAccessCreatePresentation;
import org.keycloak.representations.oidc.OIDCClientRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@KeycloakIntegrationTest
public class DcrSsfPrivilegeEscalationTest extends AbstractClientRegistrationTest {

    @Test
    public void serviceAccountsEnabledCannotBeSetViaRat() throws ClientRegistrationException {
        String iat = managedRealm.admin()
                .clientInitialAccess()
                .create(new ClientInitialAccessCreatePresentation(0,1))
                .getToken();

        reg.auth(Auth.token(iat));
        OIDCClientRepresentation oidcClient = reg.oidc().create(new OIDCClientRepresentation());
        String dclientId = oidcClient.getClientId();
        String dclientRat = oidcClient.getRegistrationAccessToken();

        managedRealm.cleanup().add(realm -> {
            var found = realm.clients().findByClientId(dclientId);
            if(!found.isEmpty()) {
                realm.clients().get(found.get(0).getId()).remove();
            }
        });

        // Attempt to flip serviceAccountsEnabled via the RAT by requesting the client_credentials grant
        reg.auth(Auth.token(dclientRat));
        OIDCClientRepresentation update = new OIDCClientRepresentation();
        update.setClientId(dclientId);
        update.setGrantTypes(List.of(OAuth2Constants.AUTHORIZATION_CODE, OAuth2Constants.CLIENT_CREDENTIALS));

        ClientRegistrationException ex = Assertions.assertThrows(ClientRegistrationException.class, () -> reg.oidc().update(update));
        assertEquals(400, ((HttpErrorException) ex.getCause())
                .getStatusLine().getStatusCode(),"DCR callers must not be permitted to enable service accounts");

        // Server-side state must not have changed
        var found = managedRealm.admin().clients().findByClientId(dclientId);
        Assertions.assertFalse(found.get(0).isServiceAccountsEnabled(),
                "serviceAccountsEnabled must remain false after rejected update");
    }
}
