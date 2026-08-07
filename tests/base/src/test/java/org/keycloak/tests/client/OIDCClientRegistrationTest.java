package org.keycloak.tests.client;

import java.util.Collections;

import org.keycloak.client.registration.Auth;
import org.keycloak.client.registration.ClientRegistrationException;
import org.keycloak.client.registration.HttpErrorException;
import org.keycloak.representations.idm.ClientInitialAccessCreatePresentation;
import org.keycloak.representations.idm.ClientInitialAccessPresentation;
import org.keycloak.representations.oidc.OIDCClientRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@KeycloakIntegrationTest
public class OIDCClientRegistrationTest extends AbstractClientRegistrationTest {

    @BeforeEach
    public void before() throws Exception {
        super.before();

        ClientInitialAccessPresentation token = managedRealm.admin().clientInitialAccess()
                .create(new ClientInitialAccessCreatePresentation(0, 10));
        reg.auth(Auth.token(token));
    }

    private OIDCClientRepresentation createRep() {
        OIDCClientRepresentation client = new OIDCClientRepresentation();
        client.setClientName("RegistrationAccessTokenTest");
        client.setClientUri(managedRealm.getBaseUrl());
        client.setRedirectUris(Collections.singletonList(managedRealm.getBaseUrl() + "/redirect"));
        return client;
    }

    private OIDCClientRepresentation createWithCleanup(OIDCClientRepresentation client) throws ClientRegistrationException {
        OIDCClientRepresentation response = reg.oidc().create(client);
        managedRealm.cleanup().add(r -> r.clients().delete(response.getClientId()));
        return response;
    }

    private void assertCreateFail(OIDCClientRepresentation client, int expectedStatusCode, String expectedErrorContains) {
        try {
            reg.oidc().create(client);
            fail("Not expected to successfully register client");
        } catch (ClientRegistrationException expected) {
            HttpErrorException httpEx = (HttpErrorException) expected.getCause();
            assertEquals(expectedStatusCode, httpEx.getStatusLine().getStatusCode());
            if (expectedErrorContains != null) {
                assertTrue(httpEx.getErrorResponse().contains(expectedErrorContains), "Error response doesn't contain expected text");
            }
        }
    }

    @Test
    public void testApplicationTypeDefault() throws ClientRegistrationException {
        OIDCClientRepresentation clientRep = createRep();
        OIDCClientRepresentation response = createWithCleanup(clientRep);
        assertEquals("web", response.getApplicationType());
    }

    @Test
    public void testApplicationTypeWeb() throws ClientRegistrationException {
        OIDCClientRepresentation clientRep = createRep();
        clientRep.setApplicationType("web");
        OIDCClientRepresentation response = createWithCleanup(clientRep);
        assertEquals("web", response.getApplicationType());
    }

    @Test
    public void testApplicationTypeNative() throws ClientRegistrationException {
        OIDCClientRepresentation clientRep = createRep();
        clientRep.setApplicationType("native");
        OIDCClientRepresentation response = createWithCleanup(clientRep);
        assertEquals("native", response.getApplicationType());
    }

    @Test
    public void testApplicationTypeInvalid() {
        OIDCClientRepresentation clientRep = createRep();
        clientRep.setApplicationType("invalid");
        assertCreateFail(clientRep, 400, "invalid_client_metadata");
    }
}
