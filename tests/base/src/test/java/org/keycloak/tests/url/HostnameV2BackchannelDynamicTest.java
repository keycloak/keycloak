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

@KeycloakIntegrationTest(config = HostnameV2BackchannelDynamicTest.ServerConfig.class)
class HostnameV2BackchannelDynamicTest {

    private static final String FIXED_URL = "https://localtest.me:444";

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    @InjectOAuthClient
    OAuthClient oAuthClient;

    @InjectSimpleHttp
    SimpleHttp simpleHttp;

    @Test
    void testFrontendAndBackendUrls() {
        assertFrontendAndBackendUrls(oAuthClient, "master", FIXED_URL, keycloakUrls.getBase());
    }

    @Test
    void testAdminUrls() throws IOException {
        assertAdminUrls(simpleHttp, keycloakUrls, "master", FIXED_URL, FIXED_URL);
    }

    static class ServerConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.options(Map.of(
                    "hostname", FIXED_URL,
                    "hostname-backchannel-dynamic", "true"
            ));
        }
    }
}
