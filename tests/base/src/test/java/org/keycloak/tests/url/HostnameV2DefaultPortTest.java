package org.keycloak.tests.url;

import java.io.IOException;
import java.util.Map;

import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.InjectSimpleHttp;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.server.KeycloakUrls;

import org.junit.jupiter.api.Test;

import static org.keycloak.tests.url.HostnameV2Assertions.assertAdminUrls;
import static org.keycloak.tests.url.HostnameV2Assertions.assertFrontendAndBackendUrls;

@KeycloakIntegrationTest(config = HostnameV2DefaultPortTest.ServerConfig.class)
class HostnameV2DefaultPortTest {

    private static final String EXPECTED_FRONTEND_URL = "https://localtest.me";
    private static final String EXPECTED_ADMIN_URL = "https://admin.localtest.me";

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    @InjectOAuthClient
    OAuthClient oAuthClient;

    @InjectSimpleHttp
    SimpleHttp simpleHttp;

    @Test
    void testFrontendAndBackendUrls() {
        assertFrontendAndBackendUrls(oAuthClient, "master", EXPECTED_FRONTEND_URL, EXPECTED_FRONTEND_URL);
    }

    @Test
    void testAdminUrls() throws IOException {
        assertAdminUrls(simpleHttp, keycloakUrls, "master", EXPECTED_FRONTEND_URL, EXPECTED_ADMIN_URL);
    }

    static class ServerConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.options(Map.of(
                    "hostname", "https://localtest.me:443",
                    "hostname-admin", "https://admin.localtest.me:443"
            ));
        }
    }
}
