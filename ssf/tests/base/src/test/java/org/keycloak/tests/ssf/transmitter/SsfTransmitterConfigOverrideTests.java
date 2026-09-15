package org.keycloak.tests.ssf.transmitter;

import java.io.IOException;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.common.Profile;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.http.simple.SimpleHttpResponse;
import org.keycloak.ssf.Ssf;
import org.keycloak.ssf.transmitter.SsfTransmitterConfig;
import org.keycloak.ssf.transmitter.admin.SsfConfigRepresentation;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectSimpleHttp;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.server.DefaultKeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.server.KeycloakUrls;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Validates that the transmitter SPI configuration actually feeds the values
 * returned by {@code /admin/realms/{realm}/ssf/config} by overriding each
 * property to a non-default value via {@code --spi-ssf-transmitter--default--*}.
 */
@KeycloakIntegrationTest(config = SsfTransmitterConfigOverrideTests.ConfigOverrideServerConfig.class)
public class SsfTransmitterConfigOverrideTests {

    static final int OVERRIDDEN_CONNECT_TIMEOUT_MILLIS = 2345;
    static final int OVERRIDDEN_SOCKET_TIMEOUT_MILLIS = 3456;
    static final int OVERRIDDEN_VERIFICATION_DELAY_MILLIS = 250;

    @InjectRealm(config = SsfOverrideRealm.class)
    ManagedRealm realm;

    @InjectSimpleHttp
    SimpleHttp http;

    @InjectAdminClient
    Keycloak adminClient;

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    @Test
    public void testAdminConfigEndpointReflectsSpiOverrides() throws IOException {

        String adminConfigUrl = keycloakUrls.getAdmin() + "/realms/" + realm.getName() + "/ssf/config";

        try (SimpleHttpResponse response = http.doGet(adminConfigUrl)
                .auth(adminClient.tokenManager().getAccessTokenString())
                .asResponse()) {
            Assertions.assertEquals(200, response.getStatus());

            SsfConfigRepresentation config = response.asJson(SsfConfigRepresentation.class);

            Assertions.assertEquals(OVERRIDDEN_CONNECT_TIMEOUT_MILLIS,
                    config.getDefaultPushEndpointConnectTimeoutMillis(),
                    "defaultPushEndpointConnectTimeoutMillis should reflect the SPI override");
            Assertions.assertEquals(OVERRIDDEN_SOCKET_TIMEOUT_MILLIS,
                    config.getDefaultPushEndpointSocketTimeoutMillis(),
                    "defaultPushEndpointSocketTimeoutMillis should reflect the SPI override");
        }
    }

    public static class ConfigOverrideServerConfig extends DefaultKeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            KeycloakServerConfigBuilder configured = super.configure(config);
            config.features(Profile.Feature.SSF);
            config.log().categoryLevel("org.keycloak.protocol.ssf", "DEBUG");
            config.spiOption("ssf-transmitter", "default",
                    SsfTransmitterConfig.CONFIG_PUSH_ENDPOINT_CONNECT_TIMEOUT_MILLIS,
                    String.valueOf(OVERRIDDEN_CONNECT_TIMEOUT_MILLIS));
            config.spiOption("ssf-transmitter", "default",
                    SsfTransmitterConfig.CONFIG_PUSH_ENDPOINT_SOCKET_TIMEOUT_MILLIS,
                    String.valueOf(OVERRIDDEN_SOCKET_TIMEOUT_MILLIS));
            config.spiOption("ssf-transmitter", "default",
                    SsfTransmitterConfig.CONFIG_TRANSMITTER_INITIATED_VERIFICATION_DELAY_MILLIS,
                    String.valueOf(OVERRIDDEN_VERIFICATION_DELAY_MILLIS));
            return configured;
        }
    }

    public static class SsfOverrideRealm implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            realm.name("ssf-transmitter-override");
            // Opt the realm into SSF so SsfAdminRealmResourceProviderFactory
            // actually registers the /ssf/* admin routes — without it, the
            // factory's isTransmitterEnabled gate returns null and every
            // request to /admin/realms/<realm>/ssf/config is a 404.
            realm.attribute(Ssf.SSF_TRANSMITTER_ENABLED_KEY, "true");
            return realm;
        }
    }
}
