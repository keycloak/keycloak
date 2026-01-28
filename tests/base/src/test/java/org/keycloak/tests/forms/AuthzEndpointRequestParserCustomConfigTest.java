package org.keycloak.tests.forms;

import org.keycloak.common.util.SecretGenerator;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.OIDCLoginProtocolFactory;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

import org.junit.jupiter.api.Test;

/**
 * Custom configuration of OIDC login protocol factory with some overriden config values
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@KeycloakIntegrationTest(config = AuthzEndpointRequestParserCustomConfigTest.AuthzEndpointRequestParserConfig.class)
public class AuthzEndpointRequestParserCustomConfigTest extends AuthzEndpointRequestParserTest {

    @Test
    @Override
    public void testParamsLength() {
        // Login hint with length 100 allowed, state with length 100 allowed
        String loginHint100 = SecretGenerator.getInstance().randomString(100);
        String state100 = SecretGenerator.getInstance().randomString(100);
        oauth.loginForm()
                .loginHint(loginHint100)
                .state(state100)
                .open();
        assertLogin(loginHint100, state100);

        // Login hint with length 200 not allowed, state with length 200 allowed
        String loginHint200 = SecretGenerator.getInstance().randomString(200);
        String state200 = SecretGenerator.getInstance().randomString(200);
        oauth.loginForm()
                .loginHint(loginHint200)
                .state(state200)
                .open();
        assertLogin("", state200);

        // state with length 2100 allowed
        String state2100 = SecretGenerator.getInstance().randomString(2100);
        oauth.loginForm()
                .state(state2100)
                .open();
        assertLogin("", state2100);

        // State with length 3100 not allowed
        String state3100 = SecretGenerator.getInstance().randomString(3100);
        oauth.loginForm()
                .state(state3100)
                .open();
        assertLogin("", null);
    }

    public static class AuthzEndpointRequestParserConfig implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config
                    .option("spi-login-protocol--" + OIDCLoginProtocol.LOGIN_PROTOCOL + "--" + OIDCLoginProtocolFactory.CONFIG_OIDC_REQ_PARAMS_DEFAULT_MAX_SIZE, "3000")
                    .option("spi-login-protocol--" + OIDCLoginProtocol.LOGIN_PROTOCOL + "--" + OIDCLoginProtocolFactory.CONFIG_OIDC_REQ_PARAMS_MAX_SIZE_PREFIX + "--login_hint", "100");
        }
    }
}
