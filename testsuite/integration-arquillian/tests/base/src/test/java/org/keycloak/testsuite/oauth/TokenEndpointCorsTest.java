package org.keycloak.testsuite.oauth;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.cors.Cors;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.OAuthClient;

import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpOptions;
import org.junit.Rule;
import org.junit.Test;

import static org.keycloak.testsuite.AbstractAdminTest.loadJson;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:mkanis@redhat.com">Martin Kanis</a>
 */
public class TokenEndpointCorsTest extends AbstractKeycloakTest {

    private static final String VALID_CORS_URL = "http://localtest.me:8180";
    private static final String INVALID_CORS_URL = "http://invalid.localtest.me:8180";

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Override
    public void beforeAbstractKeycloakTest() throws Exception {
        super.beforeAbstractKeycloakTest();
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation realm = loadJson(getClass().getResourceAsStream("/testrealm.json"), RealmRepresentation.class);
        realm.getClients().add(ClientBuilder.create().redirectUris(VALID_CORS_URL + "/realms/master/app").addWebOrigin(VALID_CORS_URL).clientId("test-app2").publicClient().directAccessGrants().build());
        testRealms.add(realm);
    }

    @Test
    public void preflightRequest() throws Exception {
        Map<String, String> responseHeaders = getTokenEndpointPreflightResponseHeaders(oauth);
        Set<String> allowedMethods = Arrays.stream(responseHeaders.get(Cors.ACCESS_CONTROL_ALLOW_METHODS).split(", ")).collect(Collectors.toSet());

        assertEquals(2, allowedMethods.size());
        assertTrue(allowedMethods.containsAll(Arrays.asList("POST", "OPTIONS")));
    }

    @Test
    public void accessTokenCorsRequest() throws Exception {
        oauth.realm("test");
        oauth.client("test-app2", "password");
        oauth.redirectUri(VALID_CORS_URL + "/realms/master/app");

        oauth.doLogin("test-user@localhost", "password");

        // Token request
        String code = oauth.parseLoginResponse().getCode();
        oauth.origin(VALID_CORS_URL);
        AccessTokenResponse response = oauth.doAccessTokenRequest(code);

        assertEquals(200, response.getStatusCode());
        assertCors(response);

        // Refresh request
        response = oauth.doRefreshTokenRequest(response.getRefreshToken());

        assertEquals(200, response.getStatusCode());
        assertCors(response);

        // Invalid origin
        oauth.origin(INVALID_CORS_URL);
        response = oauth.doRefreshTokenRequest(response.getRefreshToken());
        assertEquals(200, response.getStatusCode());
        assertNotCors(response);
        oauth.origin(VALID_CORS_URL);

        // No session
        oauth.logoutForm().idTokenHint(response.getIdToken()).open();
        response = oauth.doRefreshTokenRequest(response.getRefreshToken());
        assertEquals(400, response.getStatusCode());
        assertCors(response);
        assertEquals("invalid_grant", response.getError());
        assertEquals("Session not active", response.getErrorDescription());
    }

    @Test
    public void accessTokenResourceOwnerCorsRequest() throws Exception {
        oauth.realm("test");
        oauth.clientId("test-app2");
        oauth.origin(VALID_CORS_URL);

        // Token request
        AccessTokenResponse response = oauth.doPasswordGrantRequest("test-user@localhost", "password");

        assertEquals(200, response.getStatusCode());
        assertCors(response);

        // Invalid password
        response = oauth.doPasswordGrantRequest("test-user@localhost", "invalid");

        assertEquals(401, response.getStatusCode());
        assertCors(response);
    }

    @Test
    public void accessTokenWithConfidentialClientCorsRequest() throws Exception {
        oauth.realm("test");
        oauth.client("direct-grant", "password");
        oauth.origin(VALID_CORS_URL);

        // Successful token request with correct origin - cors should work
        AccessTokenResponse response = oauth.doPasswordGrantRequest("test-user@localhost", "password");
        assertEquals(200, response.getStatusCode());
        assertCors(response);

        oauth.client("direct-grant", "invalid");

        // Invalid client authentication with correct origin - cors should work
        response = oauth.doPasswordGrantRequest("test-user@localhost", "password");
        assertEquals(401, response.getStatusCode());
        assertCors(response);

        oauth.client("direct-grant", "password");

        // Successful token request with bad origin - cors should NOT work
        oauth.origin(INVALID_CORS_URL);
        response = oauth.doPasswordGrantRequest("test-user@localhost", "password");
        assertEquals(200, response.getStatusCode());
        assertNotCors(response);
    }

    private static void assertCors(AccessTokenResponse response) {
        assertEquals("true", response.getHeaders().get("Access-Control-Allow-Credentials"));
        assertEquals(VALID_CORS_URL, response.getHeaders().get("Access-Control-Allow-Origin"));
        assertEquals("Access-Control-Allow-Methods", response.getHeaders().get("Access-Control-Expose-Headers"));
    }

    private static void assertNotCors(AccessTokenResponse response) {
        assertNull(response.getHeaders().get("Access-Control-Allow-Credentials"));
        assertNull(response.getHeaders().get("Access-Control-Allow-Origin"));
        assertNull(response.getHeaders().get("Access-Control-Expose-Headers"));
    }

    public static Map<String, String> getTokenEndpointPreflightResponseHeaders(OAuthClient oAuthClient) {
        HttpOptions options = new HttpOptions(oAuthClient.getEndpoints().getToken());
        options.setHeader("Origin", "http://example.com");
        try (CloseableHttpResponse response = oAuthClient.httpClient().get().execute(options)) {
            return Arrays.stream(response.getAllHeaders()).collect(Collectors.toMap(Header::getName, Header::getValue));
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

}
