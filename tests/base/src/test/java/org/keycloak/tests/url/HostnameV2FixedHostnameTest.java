package org.keycloak.tests.url;

import java.io.IOException;

import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectSimpleHttp;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.server.KeycloakUrls;

import org.junit.jupiter.api.Test;

import static org.keycloak.tests.url.HostnameV2Assertions.REALM_FRONTEND_NAME;
import static org.keycloak.tests.url.HostnameV2Assertions.REALM_FRONTEND_URL;
import static org.keycloak.tests.url.HostnameV2Assertions.assertAdminUrls;
import static org.keycloak.tests.url.HostnameV2Assertions.assertFrontendAndBackendUrls;

@KeycloakIntegrationTest(config = HostnameV2FixedHostnameTest.ServerConfig.class)
class HostnameV2FixedHostnameTest {

    private static final String HOSTNAME = "localtest.me";
    private static final String EXPECTED_URL = "http://" + HOSTNAME + ":8080";

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    @InjectOAuthClient
    OAuthClient oAuthClient;

    @InjectSimpleHttp
    SimpleHttp simpleHttp;

    @InjectRealm(config = FrontendUrlRealmConfig.class)
    ManagedRealm frontendUrlRealm;

    @Test
    void testMasterRealmFrontendAndBackendUrls() {
        assertFrontendAndBackendUrls(oAuthClient, "master", EXPECTED_URL, EXPECTED_URL);
    }

    @Test
    void testMasterRealmAdminUrls() throws IOException {
        assertAdminUrls(simpleHttp, keycloakUrls, "master", EXPECTED_URL, EXPECTED_URL);
    }

    @Test
    void testCustomRealmFrontendAndBackendUrls() {
        final String realmName = frontendUrlRealm.getName();
        assertFrontendAndBackendUrls(oAuthClient, realmName, REALM_FRONTEND_URL, REALM_FRONTEND_URL);
    }

    @Test
    void testCustomRealmAdminUrls() throws IOException {
        final String realmName = frontendUrlRealm.getName();
        assertAdminUrls(simpleHttp, keycloakUrls, realmName, REALM_FRONTEND_URL, REALM_FRONTEND_URL);
    }

    static class ServerConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.option("hostname", HOSTNAME);
        }
    }

    static class FrontendUrlRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            realm.attribute("frontendUrl", REALM_FRONTEND_URL);
            return realm.name(REALM_FRONTEND_NAME);
        }
    }
}
