package org.keycloak.tests.admin;

import java.io.IOException;

import jakarta.ws.rs.core.Response;

import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.testframework.annotations.InjectHttpClient;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RealmConfigBuilder;
import org.keycloak.testframework.server.KeycloakUrls;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 *
 * @author rmartinc
 */
@KeycloakIntegrationTest
public class AppAuthManagerTest {

    @InjectRealm(config = TestRealmConfig.class)
    ManagedRealm realm;

    @InjectOAuthClient
    OAuthClient  oAuthClient;

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    @InjectHttpClient
    CloseableHttpClient client;

    @Test
    public void testSuccess() throws IOException {
        test("Bearer ", true);
    }

    @Test
    public void testFailure_BearerLowerCase() throws IOException {
        test("bearer ", true);
    }

    @Test
    public void testFailure_BearerUpperCase() throws IOException {
        test("BEARER ", true);
    }

    @Test
    public void testFailure_BearerDiffCase() throws IOException {
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
        AccessTokenResponse response = accessToken(oAuthClient, Constants.ADMIN_CLI_CLIENT_ID, "secret", "test-admin", "password");
        Assertions.assertNotNull(response.getAccessToken());
        try (CloseableHttpResponse res = getHttpJsonResponse(whoAmiUrl(), authPrefix, response.getAccessToken())) {
            Assertions.assertEquals(
                    success ? Response.Status.OK.getStatusCode() : Response.Status.UNAUTHORIZED.getStatusCode(),
                    res.getStatusLine().getStatusCode());
        }
        oAuthClient.doLogout(response.getRefreshToken());
    }

    private AccessTokenResponse accessToken(OAuthClient oAuth, String clientId, String clientSecret, String username, String password) {
        return oAuth.client(clientId, clientSecret).doPasswordGrantRequest(username, password);
    }

    private CloseableHttpResponse getHttpJsonResponse(String url, String authPrefix, String accessToken) throws IOException{
        HttpGet httpGet = new HttpGet(url);
        httpGet.addHeader("Accept", "application/json");
        httpGet.addHeader("Authorization", authPrefix + accessToken);
        return client.execute(httpGet);
    }

    private String whoAmiUrl() {
        return new StringBuilder()
                .append(keycloakUrls.getBaseUrl())
                .append("/admin/default/console/whoami")
                .toString();
    }

    private static class TestRealmConfig implements RealmConfig {

        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder realm) {
            realm.internationalizationEnabled(false);
            realm.addUser("test-admin")
                    .password("password")
                    .name("Test", "Admin")
                    .email("locale-off@email.org")
                    .emailVerified(true)
                    .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.REALM_ADMIN);
            realm.addClient(Constants.ADMIN_CLI_CLIENT_ID)
                    .name(Constants.ADMIN_CLI_CLIENT_ID)
                    .secret("secret")
                    .attribute(Constants.SECURITY_ADMIN_CONSOLE_ATTR, "true")
                    .directAccessGrantsEnabled(true);
            return realm;
        }
    }
}
