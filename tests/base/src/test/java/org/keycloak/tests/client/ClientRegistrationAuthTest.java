package org.keycloak.tests.client;

import java.io.IOException;

import jakarta.ws.rs.core.Response;

import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.http.simple.SimpleHttpResponse;
import org.keycloak.representations.idm.ClientInitialAccessCreatePresentation;
import org.keycloak.representations.oidc.OIDCClientRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectSimpleHttp;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@KeycloakIntegrationTest
public class ClientRegistrationAuthTest {

    @InjectRealm
    ManagedRealm realm;

    @InjectSimpleHttp
    SimpleHttp simpleHttp;

    @Test
    public void testSuccess() throws IOException {
        test("Bearer ", true);
    }

    @Test
    public void testSuccess_BearerLowerCase() throws IOException {
        test("bearer ", true);
    }

    @Test
    public void testSuccess_BearerUpperCase() throws IOException {
        test("BEARER ", true);
    }

    @Test
    public void testSuccess_BearerMixedCase() throws IOException {
        test("BeArEr ", true);
    }

    @Test
    public void testFailure_TwoSpaces() throws IOException {
        test("Bearer  ", false);
    }

    @Test
    public void testFailure_MultiSpaces() throws IOException {
        test("Bearer     ", false);
    }

    @Test
    public void testFailure_TabSymbol() throws IOException {
        test("Bearer\t", false);
    }

    private void test(String authPrefix, boolean success) throws IOException {
        String token = createInitialAccessToken();
        OIDCClientRepresentation client = new OIDCClientRepresentation();
        client.setClientName("test-auth-client");

        try (SimpleHttpResponse response = simpleHttp.doPost(getOidcUrl())
                .json(client)
                .header("Authorization", authPrefix + token)
                .asResponse()) {
            int expectedStatus = success
                ? Response.Status.CREATED.getStatusCode()
                : Response.Status.FORBIDDEN.getStatusCode();
            Assertions.assertEquals(expectedStatus, response.getStatus());

            if (success) {
                OIDCClientRepresentation responseClient = response.asJson(OIDCClientRepresentation.class);
                String id = realm.admin().clients().findByClientId(responseClient.getClientId()).get(0).getId();
                realm.admin().clients().get(id).remove();
            }
        }
    }

    private String createInitialAccessToken() {
        ClientInitialAccessCreatePresentation initialAccessCreatePresentation = new ClientInitialAccessCreatePresentation(0, 100);
        return realm.admin().clientInitialAccess().create(initialAccessCreatePresentation).getToken();
    }

    private String getOidcUrl() {
        return realm.getBaseUrl() + "/clients-registrations/openid-connect";
    }
}
