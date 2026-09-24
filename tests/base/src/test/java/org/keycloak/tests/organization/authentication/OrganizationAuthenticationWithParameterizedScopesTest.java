package org.keycloak.tests.organization.authentication;

import org.keycloak.common.Profile;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

/**
 *
 * @author rmartinc
 */
@KeycloakIntegrationTest(config = OrganizationAuthenticationWithParameterizedScopesTest.ServerConfig.class)
public class OrganizationAuthenticationWithParameterizedScopesTest extends OrganizationAuthenticationTest {

    static class ServerConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.PARAMETERIZED_SCOPES);
        }
    }
}
