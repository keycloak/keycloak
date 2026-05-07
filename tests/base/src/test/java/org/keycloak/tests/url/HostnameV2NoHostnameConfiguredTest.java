package org.keycloak.tests.url;

import java.io.IOException;

import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.InjectSimpleHttp;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.server.KeycloakUrls;

import org.junit.jupiter.api.Test;

import static org.keycloak.tests.url.HostnameV2Assertions.assertAdminUrls;
import static org.keycloak.tests.url.HostnameV2Assertions.assertFrontendAndBackendUrls;

@KeycloakIntegrationTest
class HostnameV2NoHostnameConfiguredTest {

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    @InjectOAuthClient
    OAuthClient oAuthClient;

    @InjectSimpleHttp
    SimpleHttp simpleHttp;

    @Test
    void testFrontendAndBackendUrls() {
        final String baseUrl = keycloakUrls.getBase();
        assertFrontendAndBackendUrls(oAuthClient, "master", baseUrl, baseUrl);
    }

    @Test
    void testAdminUrls() throws IOException {
        final String baseUrl = keycloakUrls.getBase();
        assertAdminUrls(simpleHttp, keycloakUrls, "master", baseUrl, baseUrl);
    }
}
