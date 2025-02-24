package org.keycloak.test.examples;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.keycloak.testframework.annotations.InjectClient;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientConfig;
import org.keycloak.testframework.realm.ClientConfigBuilder;
import org.keycloak.testframework.realm.ManagedClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.realm.UserConfig;
import org.keycloak.testframework.realm.UserConfigBuilder;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.TokenRevocationResponse;
import org.keycloak.testsuite.util.oauth.UserInfoResponse;

@KeycloakIntegrationTest
public class OAuthClientTest {

    @InjectOAuthClient
    OAuthClient oAuthClient;

    @InjectRealm
    ManagedRealm managedRealm;

    @InjectClient(config = OAuthClientConfig.class)
    ManagedClient client;

    @InjectUser(config = OAuthUserConfig.class)
    ManagedUser user;

    @Test
    public void testConfig() {
        Assertions.assertEquals(managedRealm.getName(), oAuthClient.config().getRealm());
        Assertions.assertEquals(managedRealm.getBaseUrl() + "/protocol/openid-connect/token", oAuthClient.getEndpoints().getToken());
    }

    @Test
    public void testPasswordGrant() {
        AccessTokenResponse accessTokenResponse = oAuthClient.doPasswordGrantRequest(user.getUsername(), user.getPassword());
        Assertions.assertTrue(accessTokenResponse.isSuccess());

        accessTokenResponse = oAuthClient.passwordGrantRequest(user.getUsername(), "invalid").send();
        Assertions.assertFalse(accessTokenResponse.isSuccess());
        Assertions.assertEquals("Invalid user credentials", accessTokenResponse.getErrorDescription());
    }

    @Test
    public void testClientCredential() {
        AccessTokenResponse accessTokenResponse = oAuthClient.doClientCredentialsGrantAccessTokenRequest();
        Assertions.assertTrue(accessTokenResponse.isSuccess());
    }

    @Test
    public void testUserInfo() {
        AccessTokenResponse accessTokenResponse = oAuthClient.doPasswordGrantRequest(user.getUsername(), user.getPassword());

        UserInfoResponse userInfoResponse = oAuthClient.doUserInfoRequest(accessTokenResponse.getAccessToken());
        Assertions.assertTrue(userInfoResponse.isSuccess());
        Assertions.assertEquals(user.getUsername(), userInfoResponse.getUserInfo().getPreferredUsername());
    }

    @Test
    public void testRefresh() {
        AccessTokenResponse accessTokenResponse = oAuthClient.doPasswordGrantRequest(user.getUsername(), user.getPassword());

        AccessTokenResponse refreshResponse = oAuthClient.doRefreshTokenRequest(accessTokenResponse.getRefreshToken());
        Assertions.assertTrue(refreshResponse.isSuccess());
        Assertions.assertNotEquals(accessTokenResponse.getAccessToken(), refreshResponse.getAccessToken());
    }

    @Test
    public void testRevocation() {
        AccessTokenResponse accessTokenResponse = oAuthClient.doPasswordGrantRequest(user.getUsername(), user.getPassword());

        TokenRevocationResponse tokenRevocationResponse = oAuthClient.doTokenRevoke(accessTokenResponse.getRefreshToken());
        Assertions.assertTrue(tokenRevocationResponse.isSuccess());

        AccessTokenResponse refreshResponse = oAuthClient.doRefreshTokenRequest(accessTokenResponse.getRefreshToken());
        Assertions.assertFalse(refreshResponse.isSuccess());
    }

    public static class OAuthClientConfig implements ClientConfig {

        @Override
        public ClientConfigBuilder configure(ClientConfigBuilder client) {
            return client.clientId("myclient").secret("mysecret").directAccessGrants().serviceAccount();
        }
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
