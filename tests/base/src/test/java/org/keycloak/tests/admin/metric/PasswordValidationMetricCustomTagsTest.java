package org.keycloak.tests.admin.metric;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.keycloak.testframework.annotations.InjectHttpClient;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.realm.UserConfig;
import org.keycloak.testframework.realm.UserConfigBuilder;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.server.KeycloakUrls;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@KeycloakIntegrationTest(config = PasswordValidationMetricCustomTagsTest.ServerConfigWithMetrics.class)
public class PasswordValidationMetricCustomTagsTest {

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

    @InjectPage
    LoginPage loginPage;

    Pattern passValidationRegex = Pattern.compile("keycloak_credentials_password_hashing_validations_total\\{realm=\"([^\"]+)\"} ([.0-9]*)");

    @Test
    void testValidAndInvalidPasswordValidation() throws IOException {
        oAuthClient.openLoginForm();
        oAuthClient.fillLoginForm(user.getUsername(), "invalid_password");
        loginPage.assertCurrent();

        AuthorizationEndpointResponse authorizationEndpointResponse = oAuthClient.doLogin(user.getUsername(), user.getPassword());
        Assertions.assertTrue(oAuthClient.doAccessTokenRequest(authorizationEndpointResponse.getCode()).isSuccess());

        String metrics = EntityUtils.toString(httpClient.execute(new HttpGet(keycloakUrls.getMetric())).getEntity());
        Matcher matcher = passValidationRegex.matcher(metrics);

        Assertions.assertTrue(matcher.find());
        Assertions.assertEquals(realm.getName(), matcher.group(1));
        Assertions.assertEquals("2.0", matcher.group(2));
        Assertions.assertFalse(matcher.find());
    }

    public static class ServerConfigWithMetrics implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config
                    .option("metrics-enabled", "true")
                    .option("spi-credential-keycloak-password-validations-counter-tags", "realm");
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
