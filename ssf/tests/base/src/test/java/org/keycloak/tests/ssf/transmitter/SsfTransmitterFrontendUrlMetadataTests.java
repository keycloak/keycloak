package org.keycloak.tests.ssf.transmitter;

import java.io.IOException;

import org.keycloak.common.Profile;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.http.simple.SimpleHttpResponse;
import org.keycloak.ssf.Ssf;
import org.keycloak.ssf.metadata.TransmitterMetadata;
import org.keycloak.ssf.transmitter.support.SsfTransmitterUrls;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectSimpleHttp;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.server.DefaultKeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Regression test for keycloak/keycloak#53107: when a realm has the
 * {@code frontendUrl} attribute set, the SSF discovery document must include
 * the {@code /realms/{realm}} path segment in every advertised URL, matching
 * the realm's standard OIDC issuer.
 *
 * <p>The {@code frontendUrl} value used here ({@code https://example.com}) is
 * intentionally unreachable — the test only inspects the string values in the
 * JSON response, it does not follow the advertised URLs.
 */
@KeycloakIntegrationTest(config = SsfTransmitterFrontendUrlMetadataTests.SsfServerConfig.class)
public class SsfTransmitterFrontendUrlMetadataTests {

    static final String FRONTEND_URL = "https://example.com";

    @InjectRealm(config = FrontendUrlRealm.class)
    ManagedRealm realm;

    @InjectSimpleHttp
    SimpleHttp http;

    /**
     * Verifies that all URL fields in the SSF discovery document contain the
     * {@code /realms/{realm}} path segment when the realm's {@code frontendUrl}
     * attribute is set (regression: previously they were missing it).
     */
    @Test
    public void testWellKnownMetadataUrlsIncludeRealmPath() throws IOException {

        String wellKnownUrl = realm.getBaseUrl() + "/" + Ssf.SSF_WELL_KNOWN_METADATA_PATH;

        try (SimpleHttpResponse response = http.doGet(wellKnownUrl).asResponse()) {
            Assertions.assertEquals(200, response.getStatus(),
                    "Well-known endpoint should return 200");

            TransmitterMetadata metadata = response.asJson(TransmitterMetadata.class);

            String expectedIssuer = FRONTEND_URL + "/realms/" + realm.getName();

            Assertions.assertEquals(expectedIssuer, metadata.getIssuer(),
                    "issuer must include /realms/<realm> when frontendUrl is set");
            Assertions.assertEquals(expectedIssuer + "/protocol/openid-connect/certs",
                    metadata.getJwksUri(),
                    "jwks_uri must include /realms/<realm> when frontendUrl is set");

            String expectedTransmitterBase = SsfTransmitterUrls.getSsfTransmitterBasePath(expectedIssuer);
            Assertions.assertEquals(expectedTransmitterBase + "/streams",
                    metadata.getConfigurationEndpoint(),
                    "configuration_endpoint must include /realms/<realm> when frontendUrl is set");
            Assertions.assertEquals(expectedTransmitterBase + "/streams/status",
                    metadata.getStatusEndpoint(),
                    "status_endpoint must include /realms/<realm> when frontendUrl is set");
            Assertions.assertEquals(expectedTransmitterBase + "/verify",
                    metadata.getVerificationEndpoint(),
                    "verification_endpoint must include /realms/<realm> when frontendUrl is set");
        }
    }

    public static class SsfServerConfig extends DefaultKeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            super.configure(config);
            config.features(Profile.Feature.SSF);
            return config;
        }
    }

    public static class FrontendUrlRealm implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            realm.name("ssf-frontend-url-test");
            realm.attribute(Ssf.SSF_TRANSMITTER_ENABLED_KEY, "true");
            realm.attribute("frontendUrl", FRONTEND_URL);
            return realm;
        }
    }
}
