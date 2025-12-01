package org.keycloak.tests.compatibility;

import org.keycloak.testframework.annotations.InjectLoadBalancer;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.clustering.LoadBalancer;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.realm.UserConfig;
import org.keycloak.testframework.realm.UserConfigBuilder;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;

import org.htmlunit.WebClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

@KeycloakIntegrationTest
public class ClusteredOAuthClientTest {

    @InjectUser(config = OAuthUserConfig.class)
    ManagedUser user;

    @InjectLoadBalancer
    LoadBalancer loadBalancer;

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectWebDriver
    WebDriver driver;

    @AfterEach
    public void cleanup() {
        loadBalancer.node(0);
        driver.navigate().to("about:blank");
        if (driver instanceof HtmlUnitDriver htmlUnitDriver) {
            WebClient webClient = htmlUnitDriver.getWebClient();
            webClient.getCache().clear();
            webClient.getCookieManager().clearCookies();
            webClient.reset();
        }
    }

    @ParameterizedTest
    @CsvSource({"0, 1", "1, 0"})
    public void testAccessTokenRefresh(int grantNode, int refreshNode) {
        loadBalancer.node(grantNode);
        AccessTokenResponse accessTokenResponse = oauth.doPasswordGrantRequest(user.getUsername(), user.getPassword());

        loadBalancer.node(refreshNode);
        AccessTokenResponse refreshResponse = oauth.doRefreshTokenRequest(accessTokenResponse.getRefreshToken());
        Assertions.assertTrue(refreshResponse.isSuccess());
        Assertions.assertNotEquals(accessTokenResponse.getAccessToken(), refreshResponse.getAccessToken());
    }

    @ParameterizedTest
    @CsvSource({
          "0, 0, 1",
          "0, 1, 0",
          "0, 1, 1",
          "1, 0, 0",
          "1, 1, 0",
          "1, 0, 1",
    })
    public void testLoginLogout(int loginNode, int tokenNode, int logoutNode) {
        loadBalancer.node(loginNode);
        AuthorizationEndpointResponse authResponse = oauth.doLogin(user.getUsername(), user.getPassword());
        Assertions.assertTrue(authResponse.isRedirected());

        loadBalancer.node(tokenNode);
        AccessTokenResponse accessTokenResponse = oauth.doPasswordGrantRequest(user.getUsername(), user.getPassword());
        Assertions.assertTrue(accessTokenResponse.isSuccess());

        loadBalancer.node(logoutNode);
        oauth.logoutForm().idTokenHint(accessTokenResponse.getIdToken()).open();
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
