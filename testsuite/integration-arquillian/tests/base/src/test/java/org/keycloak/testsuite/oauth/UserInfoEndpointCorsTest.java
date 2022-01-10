package org.keycloak.testsuite.oauth;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.junit.Test;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.util.AdminClientUtil;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.UserInfoClientUtil;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.keycloak.testsuite.admin.AbstractAdminTest.loadJson;

public class UserInfoEndpointCorsTest extends AbstractKeycloakTest {

    private static final String VALID_CORS_URL = "http://localtest.me:8180";
    private static final String INVALID_CORS_URL = "http://invalid.localtest.me:8180";

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation realm = loadJson(getClass().getResourceAsStream("/testrealm.json"), RealmRepresentation.class);
        realm.getClients().add(ClientBuilder.create().redirectUris(VALID_CORS_URL + "/realms/master/app").addWebOrigin(VALID_CORS_URL).clientId("test-app2").publicClient().directAccessGrants().build());
        testRealms.add(realm);
    }

    @Test
    public void userInfoCorsValidRequestWithValidUrl() throws Exception {

        oauth.realm("test");
        oauth.clientId("test-app2");
        oauth.redirectUri(VALID_CORS_URL + "/realms/master/app");

        OAuthClient.AccessTokenResponse accessTokenResponse = oauth.doGrantAccessTokenRequest(null, "test-user@localhost", "password");

        ResteasyClient resteasyClient = AdminClientUtil.createResteasyClient();
        try {
            WebTarget userInfoTarget = UserInfoClientUtil.getUserInfoWebTarget(resteasyClient);
            Response userInfoResponse = userInfoTarget.request()
                    .header(HttpHeaders.AUTHORIZATION, "bearer " + accessTokenResponse.getAccessToken())
                    .header("Origin", VALID_CORS_URL) // manually trigger CORS handling
                    .get();

            UserInfoClientUtil.testSuccessfulUserInfoResponse(userInfoResponse, "test-user@localhost", "test-user@localhost");

            assertCors(userInfoResponse);
        } finally {
            resteasyClient.close();
        }
    }

    // KEYCLOAK-15719 error response should still contain CORS headers
    @Test
    public void userInfoCorsInvalidRequestWithValidUrl() throws Exception {

        oauth.realm("test");
        oauth.clientId("test-app2");
        oauth.redirectUri(VALID_CORS_URL + "/realms/master/app");

        OAuthClient.AccessTokenResponse accessTokenResponse = oauth.doGrantAccessTokenRequest(null, "test-user@localhost", "password");

        // Set time offset to make sure that userInfo request will be invalid due the expired token
        setTimeOffset(600);

        ResteasyClient resteasyClient = AdminClientUtil.createResteasyClient();
        try {
            WebTarget userInfoTarget = UserInfoClientUtil.getUserInfoWebTarget(resteasyClient);
            Response userInfoResponse = userInfoTarget.request()
                    .header(HttpHeaders.AUTHORIZATION, "bearer " + accessTokenResponse.getAccessToken())
                    .header("Origin", VALID_CORS_URL) // manually trigger CORS handling
                    .get();

            // We should have errorResponse, but CORS headers should be there as origin was valid
            assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), userInfoResponse.getStatus());

            assertCors(userInfoResponse);
        } finally {
            resteasyClient.close();
        }
    }

    @Test
    public void userInfoCorsValidRequestWithInvalidUrlShouldFail() throws Exception {

        oauth.realm("test");
        oauth.clientId("test-app2");
        oauth.redirectUri(VALID_CORS_URL + "/realms/master/app");

        OAuthClient.AccessTokenResponse accessTokenResponse = oauth.doGrantAccessTokenRequest(null, "test-user@localhost", "password");

        ResteasyClient resteasyClient = AdminClientUtil.createResteasyClient();
        try {
            WebTarget userInfoTarget =  UserInfoClientUtil.getUserInfoWebTarget(resteasyClient);
            Response userInfoResponse = userInfoTarget.request()
                    .header(HttpHeaders.AUTHORIZATION, "bearer " + accessTokenResponse.getAccessToken())
                    .header("Origin", INVALID_CORS_URL) // manually trigger CORS handling
                    .get();

            UserInfoClientUtil.testSuccessfulUserInfoResponse(userInfoResponse, "test-user@localhost", "test-user@localhost");

            assertNotCors(userInfoResponse);
        } finally {
            resteasyClient.close();
        }
    }

    private static void assertCors(Response response) {
        assertEquals("true", response.getHeaders().getFirst("Access-Control-Allow-Credentials"));
        assertEquals(VALID_CORS_URL, response.getHeaders().getFirst("Access-Control-Allow-Origin"));
        response.close();
    }

    private static void assertNotCors(Response response) {
        assertNull(response.getHeaders().get("Access-Control-Allow-Credentials"));
        assertNull(response.getHeaders().get("Access-Control-Allow-Origin"));
        assertNull(response.getHeaders().get("Access-Control-Expose-Headers"));
        response.close();
    }
}
