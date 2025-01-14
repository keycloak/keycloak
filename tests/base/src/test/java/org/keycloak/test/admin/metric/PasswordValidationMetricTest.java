package org.keycloak.test.admin.metric;

import com.nimbusds.oauth2.sdk.AuthorizationResponse;
import com.nimbusds.oauth2.sdk.GeneralException;
import com.nimbusds.oauth2.sdk.TokenResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.keycloak.testframework.annotations.InjectHttpClient;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.nimbus.OAuthClient;
import org.keycloak.testframework.oauth.nimbus.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.realm.UserConfig;
import org.keycloak.testframework.realm.UserConfigBuilder;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.server.KeycloakUrls;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.LoginPage;
import org.openqa.selenium.WebDriver;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@KeycloakIntegrationTest(config = PasswordValidationMetricTest.ServerConfigWithMetrics.class)
public class PasswordValidationMetricTest {

    @InjectUser(config = OAuthUserConfig.class)
    ManagedUser user;

    @InjectRealm
    ManagedRealm realm;

    @InjectOAuthClient
    OAuthClient oAuthClient;

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    @InjectHttpClient
    HttpClient httpClient;

    @InjectWebDriver
    WebDriver webDriver;

    @InjectPage
    LoginPage loginPage;

    Pattern passValidationRegex = Pattern.compile("keycloak_credentials_password_hashing_validations_total\\{algorithm=\"([^\"]+)\",hashing_strength=\"([^\"]+)\",outcome=\"([^\"]+)\",realm=\"([^\"]+)\"} ([.0-9]*)");

    @Test
    void testValidAndInvalidPasswordValidation() throws GeneralException, IOException {
        runAuthorizationCodeFlow(user.getUsername(), "invalid_password");
        webDriver.manage().deleteAllCookies();
        runAuthorizationCodeFlow(user.getUsername(), user.getPassword());

        String metrics = EntityUtils.toString(httpClient.execute(new HttpGet(keycloakUrls.getMetric())).getEntity());
        Matcher matcher = passValidationRegex.matcher(metrics);

        Assertions.assertTrue(matcher.find());
        String algorithm = matcher.group(1);
        String hashing_strength = matcher.group(2);
        String outcome = matcher.group(3);
        String realmTag = matcher.group(4);
        String counterValue = matcher.group(5);

        Assertions.assertTrue("valid".equals(outcome) || "invalid".equals(outcome), "outcome tag should be valid or invalid but was " + outcome);
        Assertions.assertEquals(realm.getName(), realmTag);
        Assertions.assertEquals("1.0", counterValue);

        Assertions.assertTrue(matcher.find());
        Assertions.assertEquals(algorithm, matcher.group(1));
        Assertions.assertEquals(hashing_strength, matcher.group(2));
        Assertions.assertEquals("valid".equals(outcome) ? "invalid" : "valid", matcher.group(3));
        Assertions.assertEquals(realm.getName(), matcher.group(4));
        Assertions.assertEquals("1.0", matcher.group(5));

        Assertions.assertFalse(matcher.find());
    }

    private void runAuthorizationCodeFlow(String username, String password) throws GeneralException, IOException {
        URL authorizationRequestURL = oAuthClient.authorizationRequest();
        webDriver.navigate().to(authorizationRequestURL);
        loginPage.fillLogin(username, password);
        loginPage.submit();

        if (oAuthClient.getCallbacks().isEmpty()) {
            return;
        }

        URI callbackUri = oAuthClient.getCallbacks().remove(0);

        AuthorizationResponse authorizationResponse = AuthorizationResponse.parse(callbackUri);
        Assertions.assertTrue(authorizationResponse.indicatesSuccess());
        Assertions.assertNotNull(authorizationResponse.toSuccessResponse().getAuthorizationCode());

        TokenResponse tokenResponse = oAuthClient.tokenRequest(authorizationResponse.toSuccessResponse().getAuthorizationCode());
        Assertions.assertTrue(tokenResponse.indicatesSuccess());
        Assertions.assertNotNull(tokenResponse.toSuccessResponse().getTokens().getAccessToken());
    }

    public static class ServerConfigWithMetrics implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.option("metrics-enabled", "true");
        }
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
