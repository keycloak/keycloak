package org.keycloak.testsuite.oauth;

import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.UserInfoClientUtil;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
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
        realm.getClients().add(ClientBuilder.create().redirectUris(VALID_CORS_URL + "/realms/master/app").addWebOrigin(VALID_CORS_URL).id("test-app2").clientId("test-app2").publicClient().directAccessGrants().build());
        testRealms.add(realm);
    }

    @Test
    public void userInfoCorsRequestWithValidUrl() throws Exception {

        oauth.realm("test");
        oauth.clientId("test-app2");
        oauth.redirectUri(VALID_CORS_URL + "/realms/master/app");

        OAuthClient.AccessTokenResponse accessTokenResponse = oauth.doGrantAccessTokenRequest(null, "test-user@localhost", "password");

        WebTarget userInfoTarget =  UserInfoClientUtil.getUserInfoWebTarget(javax.ws.rs.client.ClientBuilder.newClient());
        Response userInfoResponse = userInfoTarget.request()
                .header(HttpHeaders.AUTHORIZATION, "bearer " + accessTokenResponse.getAccessToken())
                .header("Origin", VALID_CORS_URL) // manually trigger CORS handling
                .get();

        UserInfoClientUtil.testSuccessfulUserInfoResponse(userInfoResponse, "test-user@localhost", "test-user@localhost");

        assertCors(userInfoResponse);
    }

    @Test
    public void userInfoCorsRequestWithInvalidUrlShouldFail() throws Exception {

        oauth.realm("test");
        oauth.clientId("test-app2");
        oauth.redirectUri(VALID_CORS_URL + "/realms/master/app");

        OAuthClient.AccessTokenResponse accessTokenResponse = oauth.doGrantAccessTokenRequest(null, "test-user@localhost", "password");

        WebTarget userInfoTarget =  UserInfoClientUtil.getUserInfoWebTarget(javax.ws.rs.client.ClientBuilder.newClient());
        Response userInfoResponse = userInfoTarget.request()
                .header(HttpHeaders.AUTHORIZATION, "bearer " + accessTokenResponse.getAccessToken())
                .header("Origin", INVALID_CORS_URL) // manually trigger CORS handling
                .get();

        assertNotCors(userInfoResponse);
    }

    private static void assertCors(Response response) {
        assertEquals("true", response.getHeaders().getFirst("Access-Control-Allow-Credentials"));
        assertEquals(VALID_CORS_URL, response.getHeaders().getFirst("Access-Control-Allow-Origin"));
    }

    private static void assertNotCors(Response response) {
        assertNull(response.getHeaders().get("Access-Control-Allow-Credentials"));
        assertNull(response.getHeaders().get("Access-Control-Allow-Origin"));
        assertNull(response.getHeaders().get("Access-Control-Expose-Headers"));
    }
}
