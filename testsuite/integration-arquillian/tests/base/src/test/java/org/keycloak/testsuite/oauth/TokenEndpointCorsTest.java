package org.keycloak.testsuite.oauth;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.OAuthClient;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.keycloak.testsuite.admin.AbstractAdminTest.loadJson;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer;

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
        CloseableHttpResponse response = oauth.doPreflightRequest();

        String[] methods = response.getHeaders("Access-Control-Allow-Methods")[0].getValue().split(", ");
        Set allowedMethods = new HashSet(Arrays.asList(methods));

        assertEquals(2, allowedMethods.size());
        assertTrue(allowedMethods.containsAll(Arrays.asList("POST", "OPTIONS")));
    }

    @Test
    @AuthServerContainerExclude(AuthServer.REMOTE)
    public void accessTokenCorsRequest() throws Exception {
        oauth.realm("test");
        oauth.clientId("test-app2");
        oauth.redirectUri(VALID_CORS_URL + "/realms/master/app");
        oauth.postLogoutRedirectUri(VALID_CORS_URL + "/realms/master/app");

        oauth.doLogin("test-user@localhost", "password");

        // Token request
        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        oauth.origin(VALID_CORS_URL);
        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, "password");

        assertEquals(200, response.getStatusCode());
        assertCors(response);

        // Refresh request
        response = oauth.doRefreshTokenRequest(response.getRefreshToken(), null);

        assertEquals(200, response.getStatusCode());
        assertCors(response);

        // Invalid origin
        oauth.origin(INVALID_CORS_URL);
        response = oauth.doRefreshTokenRequest(response.getRefreshToken(), "password");
        assertEquals(200, response.getStatusCode());
        assertNotCors(response);
        oauth.origin(VALID_CORS_URL);

        // No session
        oauth.idTokenHint(response.getIdToken()).openLogout();
        response = oauth.doRefreshTokenRequest(response.getRefreshToken(), null);
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
        OAuthClient.AccessTokenResponse response = oauth.doGrantAccessTokenRequest("password", "test-user@localhost", "password");

        assertEquals(200, response.getStatusCode());
        assertCors(response);

        // Invalid password
        response = oauth.doGrantAccessTokenRequest("password", "test-user@localhost", "invalid");

        assertEquals(401, response.getStatusCode());
        assertCors(response);
    }

    @Test
    public void accessTokenWithConfidentialClientCorsRequest() throws Exception {
        oauth.realm("test");
        oauth.clientId("direct-grant");
        oauth.origin(VALID_CORS_URL);

        // Successful token request with correct origin - cors should work
        OAuthClient.AccessTokenResponse response = oauth.doGrantAccessTokenRequest("password", "test-user@localhost", "password");
        assertEquals(200, response.getStatusCode());
        assertCors(response);

        // Invalid client authentication with correct origin - cors should work
        response = oauth.doGrantAccessTokenRequest("invalid", "test-user@localhost", "password");
        assertEquals(401, response.getStatusCode());
        assertCors(response);

        // Successful token request with bad origin - cors should NOT work
        oauth.origin(INVALID_CORS_URL);
        response = oauth.doGrantAccessTokenRequest("password", "test-user@localhost", "password");
        assertEquals(200, response.getStatusCode());
        assertNotCors(response);
    }

    private static void assertCors(OAuthClient.AccessTokenResponse response) {
        assertEquals("true", response.getHeaders().get("Access-Control-Allow-Credentials"));
        assertEquals(VALID_CORS_URL, response.getHeaders().get("Access-Control-Allow-Origin"));
        assertEquals("Access-Control-Allow-Methods", response.getHeaders().get("Access-Control-Expose-Headers"));
    }

    private static void assertNotCors(OAuthClient.AccessTokenResponse response) {
        assertNull(response.getHeaders().get("Access-Control-Allow-Credentials"));
        assertNull(response.getHeaders().get("Access-Control-Allow-Origin"));
        assertNull(response.getHeaders().get("Access-Control-Expose-Headers"));
    }
}
