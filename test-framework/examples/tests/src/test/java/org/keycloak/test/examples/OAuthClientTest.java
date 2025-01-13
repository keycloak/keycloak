package org.keycloak.test.examples;

import com.nimbusds.oauth2.sdk.AuthorizationResponse;
import com.nimbusds.oauth2.sdk.TokenIntrospectionResponse;
import com.nimbusds.oauth2.sdk.TokenResponse;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.keycloak.testframework.oauth.nimbus.annotations.InjectOAuthClient;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.nimbus.OAuthClient;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.realm.UserConfig;
import org.keycloak.testframework.realm.UserConfigBuilder;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.LoginPage;
import org.openqa.selenium.WebDriver;

import java.net.URI;
import java.net.URL;

@KeycloakIntegrationTest
public class OAuthClientTest {

    @InjectUser(config = OAuthUserConfig.class)
    ManagedUser user;

    @InjectOAuthClient
    OAuthClient oAuthClient;

    @InjectWebDriver
    WebDriver webDriver;

    @InjectPage
    LoginPage loginPage;

    @Test
    public void testClientCredentials() throws Exception {
        TokenResponse tokenResponse = oAuthClient.clientCredentialGrant();
        Assertions.assertTrue(tokenResponse.indicatesSuccess());
        Assertions.assertNotNull(tokenResponse.toSuccessResponse().getTokens().getAccessToken());
    }

    @Test
    public void testIntrospection() throws Exception {
        AccessToken accessToken = oAuthClient.clientCredentialGrant().toSuccessResponse().getTokens().getAccessToken();
        TokenIntrospectionResponse introspectionResponse = oAuthClient.introspection(accessToken);
        Assertions.assertTrue(introspectionResponse.indicatesSuccess());
        Assertions.assertNotNull(introspectionResponse.toSuccessResponse().getIssuer());
    }

    @Test
    public void testAuthorizationCode() throws Exception {
        URL authorizationRequestURL = oAuthClient.authorizationRequest();
        webDriver.navigate().to(authorizationRequestURL);
        loginPage.fillLogin(user.getUsername(), user.getPassword());
        loginPage.submit();

        Assertions.assertEquals(1, oAuthClient.getCallbacks().size());

        URI callbackUri = oAuthClient.getCallbacks().remove(0);

        AuthorizationResponse authorizationResponse = AuthorizationResponse.parse(callbackUri);
        Assertions.assertTrue(authorizationResponse.indicatesSuccess());
        Assertions.assertNotNull(authorizationResponse.toSuccessResponse().getAuthorizationCode());

        TokenResponse tokenResponse = oAuthClient.tokenRequest(authorizationResponse.toSuccessResponse().getAuthorizationCode());
        Assertions.assertTrue(tokenResponse.indicatesSuccess());
        Assertions.assertNotNull(tokenResponse.toSuccessResponse().getTokens().getAccessToken());
    }

    @Test
    public void testAccessTokenRevocation() throws Exception {
        TokenResponse tokenResponse = oAuthClient.clientCredentialGrant();
        Assertions.assertTrue(tokenResponse.indicatesSuccess());
        Assertions.assertNotNull(tokenResponse.toSuccessResponse().getTokens().getAccessToken());

        AccessToken accessToken = tokenResponse.toSuccessResponse().getTokens().getAccessToken();
        TokenIntrospectionResponse introspectionResponse = oAuthClient.introspection(accessToken);
        Assertions.assertTrue(introspectionResponse.indicatesSuccess());
        Assertions.assertNotNull(introspectionResponse.toSuccessResponse().getScope());

        Assertions.assertEquals(Response.Status.OK.getStatusCode(), oAuthClient.revokeAccessToken(accessToken).getStatusCode());

        introspectionResponse = oAuthClient.introspection(accessToken);
        Assertions.assertTrue(introspectionResponse.indicatesSuccess());
        Assertions.assertNull(introspectionResponse.toSuccessResponse().getScope());
    }

    public static class OAuthUserConfig implements UserConfig {

        @Override
        public UserConfigBuilder configure(UserConfigBuilder user) {
            return user.name("First", "Last")
                    .email("test@local")
                    .password("password");
        }
    }

}
