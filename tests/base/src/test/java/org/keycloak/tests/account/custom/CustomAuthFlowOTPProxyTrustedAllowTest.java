package org.keycloak.tests.account.custom;

import java.util.HashMap;
import java.util.Map;

import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import static org.keycloak.authentication.authenticators.browser.ConditionalOtpFormAuthenticator.DEFAULT_OTP_OUTCOME;
import static org.keycloak.authentication.authenticators.browser.ConditionalOtpFormAuthenticator.FORCE;
import static org.keycloak.authentication.authenticators.browser.ConditionalOtpFormAuthenticator.FORCE_OTP_FOR_HTTP_HEADER;
import static org.keycloak.authentication.authenticators.browser.ConditionalOtpFormAuthenticator.SKIP;
import static org.keycloak.authentication.authenticators.browser.ConditionalOtpFormAuthenticator.SKIP_OTP_FOR_HTTP_HEADER;
import static org.keycloak.representations.idm.CredentialRepresentation.PASSWORD;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 * @author rmartinc
 */
@KeycloakIntegrationTest(config = CustomAuthFlowOTPProxyTrustedAllowTest.TrustedProxyAllowedServerConfig.class)
public class CustomAuthFlowOTPProxyTrustedAllowTest extends AbstractCustomAuthFlowOTPTest {

    @Test
    public void conditionalOTPRequestHeaderSkip() {
        //prepare config - request header skip, default to force
        Map<String, String> config = new HashMap<>();
        String port = authServerPort();
        config.put(SKIP_OTP_FOR_HTTP_HEADER, "Host: localhost:" + port);
        config.put(DEFAULT_OTP_OUTCOME, FORCE);

        setConditionalOTPForm(config);

        //test OTP is skipped
        oauth.openLoginForm();
        testRealmLoginPage.login(testUser.getUsername(), PASSWORD);
        assertTrue(driver.getCurrentUrl().startsWith(oauth.getRedirectUri()));
    }

    @Test
    public void conditionalOTPRequestHeaderForce() {
        //prepare config - equest header force, default to skip
        Map<String, String> config = new HashMap<>();
        String port = authServerPort();
        config.put(FORCE_OTP_FOR_HTTP_HEADER, "Host: localhost:" + port);
        config.put(DEFAULT_OTP_OUTCOME, SKIP);

        setConditionalOTPForm(config);

        //test OTP is required
        oauth.openLoginForm();
        testRealmLoginPage.login(testUser.getUsername(), PASSWORD);
        assertEquals("Mobile Authenticator Setup", driver.findElement(By.id("kc-page-title")).getText());

        configureOTP();
        oauth.openLoginForm();
        testRealmLoginPage.login(testUser.getUsername(), PASSWORD);

        //verify that the page is login page, not totp setup
        loginTotpPage.assertCurrent();
    }

    public static class TrustedProxyAllowedServerConfig implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.option("proxy-headers", "xforwarded")
                    .option("proxy-trusted-addresses", "127.0.0.1");
        }
    }
}
