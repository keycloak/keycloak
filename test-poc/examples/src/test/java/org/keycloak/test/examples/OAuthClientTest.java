package org.keycloak.test.examples;

import com.nimbusds.oauth2.sdk.AuthorizationResponse;
import com.nimbusds.oauth2.sdk.TokenIntrospectionResponse;
import com.nimbusds.oauth2.sdk.TokenResponse;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.test.framework.annotations.InjectOAuthClient;
import org.keycloak.test.framework.annotations.InjectPage;
import org.keycloak.test.framework.annotations.InjectRealm;
import org.keycloak.test.framework.annotations.InjectUser;
import org.keycloak.test.framework.annotations.InjectWebDriver;
import org.keycloak.test.framework.annotations.KeycloakIntegrationTest;
import org.keycloak.test.framework.oauth.OAuthClient;
import org.keycloak.test.framework.page.LoginPage;
import org.keycloak.test.framework.realm.ManagedRealm;
import org.keycloak.test.framework.realm.ManagedUser;
import org.openqa.selenium.WebDriver;

import java.net.URI;
import java.net.URL;

@KeycloakIntegrationTest
public class OAuthClientTest {

    @InjectUser(config = UserConfig.class)
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

    public static class UserConfig implements org.keycloak.test.framework.realm.UserConfig {

        @Override
        public UserRepresentation getRepresentation() {
            return builder()
                    .name("First", "Last")
                    .email("test@local")
                    .password("password")
                    .build();
        }
    }

}
