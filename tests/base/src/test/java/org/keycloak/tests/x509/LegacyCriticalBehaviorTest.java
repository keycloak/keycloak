package org.keycloak.tests.x509;

import org.keycloak.representations.idm.AuthenticatorConfigRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 *
 * @author rmartinc
 */
@KeycloakIntegrationTest(config = LegacyCriticalBehaviorTest.LegacyCriticalBehaviorServerConfig.class)
public class LegacyCriticalBehaviorTest extends AbstractX509AuthenticationTest {

    @Test
    public void loginBrowserWithNonSupportedCertExtendedKeyUsage() throws Exception {
        x509BrowserLogin(createLoginSubjectEmailWithExtendedKeyUsage("serverAuth"), x509User.getId(), x509User.getUsername(), x509User.getUsername());
    }

    @Test
    public void loginDirectWithNonSupportedCertExtendedKeyUsage() throws Exception {
        // Set the X509 authenticator configuration
        AuthenticatorConfigRepresentation cfg = newConfig("x509-directgrant-config",
                createLoginSubjectEmailWithExtendedKeyUsage("serverAuth").getConfig());
        String cfgId = createConfig(directGrantExecution.getId(), cfg);
        Assertions.assertNotNull(cfgId);

        AccessTokenResponse response = oauth.doPasswordGrantRequest("", "");

        assertEquals(200, response.getStatusCode());
    }

    public static class LegacyCriticalBehaviorServerConfig implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            // set legacy behavior for critical extensions
            return config.option("spi-authenticator--auth-x509-client-username-form--legacy-critical-behavior", "true")
                    .option("spi-authenticator--direct-grant-auth-x509-username--legacy-critical-behavior", "true");
        }
    }
}
