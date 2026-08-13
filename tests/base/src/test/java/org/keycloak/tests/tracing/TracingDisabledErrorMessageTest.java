package org.keycloak.tests.tracing;

import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.ErrorPage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

@KeycloakIntegrationTest(config = TracingDisabledErrorMessageTest.ServerConfigWithoutTracing.class)
public class TracingDisabledErrorMessageTest {

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectPage
    ErrorPage errorPage;

    @Test
    public void traceIdAbsentInErrorPage() {
        oauth.redirectUri("http://invalid");
        oauth.openLoginForm();
        errorPage.assertCurrent();

        assertFalse(errorPage.isTraceIdPresent());
    }

    public static class ServerConfigWithoutTracing implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config
                    .option("tracing-enabled", "false");
        }
    }

}
