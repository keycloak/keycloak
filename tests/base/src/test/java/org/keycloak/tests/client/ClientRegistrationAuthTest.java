package org.keycloak.tests.client;

import java.io.IOException;

import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.core.Response;

import org.keycloak.OAuthErrorException;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.http.simple.SimpleHttpRequest;
import org.keycloak.http.simple.SimpleHttpResponse;
import org.keycloak.representations.idm.ClientInitialAccessCreatePresentation;
import org.keycloak.representations.idm.OAuth2ErrorRepresentation;
import org.keycloak.representations.oidc.OIDCClientRepresentation;
import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectSimpleHttp;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.events.Events;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@KeycloakIntegrationTest
public class ClientRegistrationAuthTest {

    @InjectRealm(config = ClientRegistrationAuthRealmConfig.class)
    ManagedRealm realm;

    @InjectEvents
    Events events;

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

    @Test
    public void testInvalidToken_Get() throws IOException {
        testInvalidToken(HttpMethod.GET, EventType.CLIENT_INFO_ERROR);
    }

    @Test
    public void testInvalidToken_Post() throws IOException {
        testInvalidToken(HttpMethod.POST, EventType.CLIENT_REGISTER_ERROR);
    }

    @Test
    public void testInvalidToken_Put() throws IOException {
        testInvalidToken(HttpMethod.PUT, EventType.CLIENT_UPDATE_ERROR);
    }

    @Test
    public void testInvalidToken_Delete() throws IOException {
        testInvalidToken(HttpMethod.DELETE, EventType.CLIENT_DELETE_ERROR);
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

    private void testInvalidToken(String method, EventType expectedEventType) throws IOException {
        try (SimpleHttpResponse response = createInvalidTokenRequest(method)
                .header("Authorization", "Bearer test")
                .asResponse()) {
            Assertions.assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());

            OAuth2ErrorRepresentation error = response.asJson(OAuth2ErrorRepresentation.class);
            Assertions.assertEquals(OAuthErrorException.INVALID_TOKEN, error.getError());
            Assertions.assertNotNull(error.getErrorDescription());
            Assertions.assertFalse(error.getErrorDescription().contains("IllegalStateException"));
        }

        EventAssertion.assertError(events.poll())
                .type(expectedEventType)
                .error(Errors.INVALID_TOKEN);
    }

    private SimpleHttpRequest createInvalidTokenRequest(String method) {
        return switch (method) {
            case HttpMethod.GET -> simpleHttp.doGet(getOidcUrl() + "/test-auth-client");
            case HttpMethod.POST -> simpleHttp.doPost(getOidcUrl()).json(new OIDCClientRepresentation());
            case HttpMethod.PUT -> simpleHttp.doPut(getOidcUrl() + "/test-auth-client").json(new OIDCClientRepresentation());
            case HttpMethod.DELETE -> simpleHttp.doDelete(getOidcUrl() + "/test-auth-client");
            default -> throw new IllegalArgumentException("Unsupported method: " + method);
        };
    }

    private String createInitialAccessToken() {
        ClientInitialAccessCreatePresentation initialAccessCreatePresentation = new ClientInitialAccessCreatePresentation(0, 100);
        return realm.admin().clientInitialAccess().create(initialAccessCreatePresentation).getToken();
    }

    private String getOidcUrl() {
        return realm.getBaseUrl() + "/clients-registrations/openid-connect";
    }

    private static class ClientRegistrationAuthRealmConfig implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm.eventsEnabled(true)
                    .enabledEventTypes(EventType.CLIENT_INFO_ERROR.name(), EventType.CLIENT_REGISTER_ERROR.name(),
                            EventType.CLIENT_UPDATE_ERROR.name(), EventType.CLIENT_DELETE_ERROR.name());
        }
    }
}
