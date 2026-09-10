package org.keycloak.tests.url;

import java.io.IOException;
import java.util.Map;

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

@KeycloakIntegrationTest(config = HostnameV2RealmFrontendUrlOverrideTest.ServerConfig.class)
class HostnameV2RealmFrontendUrlOverrideTest {

    private static final String FIXED_FRONTEND_URL = "https://localtest.me:444";
    private static final String FIXED_ADMIN_URL = "https://admin.localtest.me:445";

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    @InjectOAuthClient
    OAuthClient oAuthClient;

    @InjectSimpleHttp
    SimpleHttp simpleHttp;

    @InjectRealm(config = FrontendUrlRealmConfig.class)
    ManagedRealm frontendUrlRealm;

    @Test
    void testFrontendAndBackendUrls() {
        final String realmName = frontendUrlRealm.getName();
        assertFrontendAndBackendUrls(oAuthClient, realmName, REALM_FRONTEND_URL, keycloakUrls.getBase());
    }

    @Test
    void testAdminUrls() throws IOException {
        final String realmName = frontendUrlRealm.getName();
        assertAdminUrls(simpleHttp, keycloakUrls, realmName, REALM_FRONTEND_URL, FIXED_ADMIN_URL);
    }

    static class ServerConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.options(Map.of(
                    "hostname", FIXED_FRONTEND_URL,
                    "hostname-admin", FIXED_ADMIN_URL,
                    "hostname-backchannel-dynamic", "true"
            ));
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
