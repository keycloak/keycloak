package org.keycloak.test.examples;

import java.io.IOException;

import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;
import org.keycloak.representations.AccessToken;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.realm.UserConfig;
import org.keycloak.testframework.realm.UserConfigBuilder;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;
import org.keycloak.testsuite.util.oauth.IntrospectionResponse;
import org.keycloak.testsuite.util.oauth.TokenRevocationResponse;
import org.keycloak.testsuite.util.oauth.UserInfoResponse;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@KeycloakIntegrationTest
public class OAuthClientTest {

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectRealm
    ManagedRealm managedRealm;

    @InjectUser(config = OAuthUserConfig.class)
    ManagedUser user;

    @InjectPage
    LoginPage loginPage;

    @Test
    public void testConfig() {
        Assertions.assertEquals(managedRealm.getName(), oauth.config().getRealm());
        Assertions.assertEquals(managedRealm.getBaseUrl() + "/protocol/openid-connect/token", oauth.getEndpoints().getToken());
    }

    @Test
    public void testLogin() {
        AuthorizationEndpointResponse response = oauth.doLogin(user.getUsername(), user.getPassword());
        Assertions.assertTrue(response.isRedirected());

        oauth.logoutForm().idTokenHint(oauth.doAccessTokenRequest(response.getCode()).getIdToken()).open();
    }

    @Test
    public void testPasswordGrant() {
        AccessTokenResponse accessTokenResponse = oauth.doPasswordGrantRequest(user.getUsername(), user.getPassword());
        Assertions.assertTrue(accessTokenResponse.isSuccess());

        accessTokenResponse = oauth.passwordGrantRequest(user.getUsername(), "invalid").send();
        Assertions.assertFalse(accessTokenResponse.isSuccess());
        Assertions.assertEquals("Invalid user credentials", accessTokenResponse.getErrorDescription());
    }

    @Test
    public void testClientCredential() {
        AccessTokenResponse accessTokenResponse = oauth.doClientCredentialsGrantAccessTokenRequest();
        Assertions.assertTrue(accessTokenResponse.isSuccess());
    }

    @Test
    public void testUserInfo() {
        AccessTokenResponse accessTokenResponse = oauth.doPasswordGrantRequest(user.getUsername(), user.getPassword());

        UserInfoResponse userInfoResponse = oauth.doUserInfoRequest(accessTokenResponse.getAccessToken());
        Assertions.assertTrue(userInfoResponse.isSuccess());
        Assertions.assertEquals(user.getUsername(), userInfoResponse.getUserInfo().getPreferredUsername());
    }

    @Test
    public void testRefresh() {
        AccessTokenResponse accessTokenResponse = oauth.doPasswordGrantRequest(user.getUsername(), user.getPassword());

        AccessTokenResponse refreshResponse = oauth.doRefreshTokenRequest(accessTokenResponse.getRefreshToken());
        Assertions.assertTrue(refreshResponse.isSuccess());
        Assertions.assertNotEquals(accessTokenResponse.getAccessToken(), refreshResponse.getAccessToken());
    }

    @Test
    public void testOpenIDConfiguration() {
        OIDCConfigurationRepresentation oidcConfiguration = oauth.doWellKnownRequest();
        Assertions.assertNotNull(oidcConfiguration);
    }

    @Test
    public void testIntrospection() throws IOException {
        AccessTokenResponse accessTokenResponse = oauth.doPasswordGrantRequest(user.getUsername(), user.getPassword());

        IntrospectionResponse introspectionResponse = oauth.doIntrospectionAccessTokenRequest(accessTokenResponse.getAccessToken());
        Assertions.assertTrue(introspectionResponse.isSuccess());
        Assertions.assertTrue(introspectionResponse.asTokenMetadata().isActive());
    }

    @Test
    public void testRevocation() {
        AccessTokenResponse accessTokenResponse = oauth.doPasswordGrantRequest(user.getUsername(), user.getPassword());

        TokenRevocationResponse tokenRevocationResponse = oauth.doTokenRevoke(accessTokenResponse.getRefreshToken());
        Assertions.assertTrue(tokenRevocationResponse.isSuccess());

        AccessTokenResponse refreshResponse = oauth.doRefreshTokenRequest(accessTokenResponse.getRefreshToken());
        Assertions.assertFalse(refreshResponse.isSuccess());
    }

    @Test
    public void testParseToken() {
        AccessTokenResponse accessTokenResponse = oauth.doPasswordGrantRequest(user.getUsername(), user.getPassword());

        AccessToken accessToken = oauth.parseToken(accessTokenResponse.getAccessToken(), AccessToken.class);
        Assertions.assertEquals(user.getUsername(), accessToken.getPreferredUsername());
    }

    @Test
    public void testVerifyToken() {
        AccessTokenResponse accessTokenResponse = oauth.doPasswordGrantRequest(user.getUsername(), user.getPassword());

        AccessToken accessToken = oauth.verifyToken(accessTokenResponse.getAccessToken(), AccessToken.class);
        Assertions.assertEquals(user.getUsername(), accessToken.getPreferredUsername());
    }

    @Test
    public void testLogout() {
        AuthorizationEndpointResponse authzResponse = oauth.doLogin(user.getUsername(), user.getPassword());
        AccessTokenResponse accessTokenResponse = oauth.doAccessTokenRequest(authzResponse.getCode());
        oauth.logoutForm().idTokenHint(accessTokenResponse.getIdToken()).open();
        oauth.loginForm().open();
        loginPage.assertCurrent();
    }

    public static class OAuthUserConfig implements UserConfig {

        @Override
        public UserConfigBuilder configure(UserConfigBuilder user) {
            return user.username("myuser").name("First", "Last")
                    .email("test@local")
                    .password("password");
        }
    }

}
