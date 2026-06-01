package org.keycloak.tests.ssf.transmitter;

import java.io.IOException;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.common.Profile;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.http.simple.SimpleHttpResponse;
import org.keycloak.ssf.Ssf;
import org.keycloak.ssf.event.caep.CaepCredentialChange;
import org.keycloak.ssf.event.caep.CaepSessionRevoked;
import org.keycloak.ssf.metadata.TransmitterMetadata;
import org.keycloak.ssf.transmitter.SsfTransmitterConfig;
import org.keycloak.ssf.transmitter.admin.SsfConfigRepresentation;
import org.keycloak.ssf.transmitter.support.SsfTransmitterUrls;
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
 * Tests for the SSF Transmitter metadata/discovery surface exposed at
 * <ul>
 *     <li>{@code /realms/{realm}/.well-known/ssf-configuration}</li>
 *     <li>{@code /admin/realms/{realm}/ssf/config}</li>
 * </ul>
 *
 * These tests run against the default transmitter SPI configuration, i.e.
 * they assert that the values returned by the two endpoints match the
 * defaults declared in {@link SsfTransmitterConfig}. Tests that override
 * these defaults via SPI configuration live in
 * {@link SsfTransmitterConfigOverrideTests}.
 */
@KeycloakIntegrationTest(config = SsfTransmitterMetadataTests.SsfTransmitterKeycloakServerConfig.class)
public class SsfTransmitterMetadataTests {

    @InjectRealm(config = SsfTransmitterRealm.class)
    ManagedRealm realm;

    @InjectSimpleHttp
    SimpleHttp http;

    @InjectAdminClient
    Keycloak adminClient;

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    @Test
    public void testWellKnownMetadata() throws IOException {

        String wellKnownUrl = realm.getBaseUrl() + "/" + Ssf.SSF_WELL_KNOWN_METADATA_PATH;

        try (SimpleHttpResponse response = http.doGet(wellKnownUrl).asResponse()) {
            Assertions.assertEquals(200, response.getStatus(),
                    "Well-known endpoint should return 200");

            TransmitterMetadata metadata = response.asJson(TransmitterMetadata.class);

            Assertions.assertEquals("1_0", metadata.getSpecVersion(),
                    "spec_version should be 1_0");
            Assertions.assertEquals(realm.getBaseUrl(), metadata.getIssuer(),
                    "issuer should match the realm base URL");
            Assertions.assertEquals(realm.getBaseUrl() + "/protocol/openid-connect/certs",
                    metadata.getJwksUri(),
                    "jwks_uri should point at the realm's OIDC JWKS");

            Assertions.assertNotNull(metadata.getDeliveryMethodSupported());
            Assertions.assertTrue(
                    metadata.getDeliveryMethodSupported().contains(Ssf.DELIVERY_METHOD_PUSH_URI),
                    "delivery_methods_supported should contain the SSF PUSH URI");
            Assertions.assertTrue(
                    metadata.getDeliveryMethodSupported().contains(Ssf.DELIVERY_METHOD_RISC_PUSH_URI),
                    "delivery_methods_supported should contain the legacy RISC PUSH URI");

            String transmitterBase = SsfTransmitterUrls.getSsfTransmitterBasePath(realm.getBaseUrl());
            Assertions.assertEquals(transmitterBase + "/streams",
                    metadata.getConfigurationEndpoint(),
                    "configuration_endpoint should point at the transmitter streams endpoint");
            Assertions.assertEquals(transmitterBase + "/streams/status",
                    metadata.getStatusEndpoint(),
                    "status_endpoint should point at the transmitter streams/status endpoint");
            Assertions.assertEquals(transmitterBase + "/verify",
                    metadata.getVerificationEndpoint(),
                    "verification_endpoint should point at the transmitter verify endpoint");

            Assertions.assertNotNull(metadata.getAuthorizationSchemes(),
                    "authorization_schemes should be populated");
            Assertions.assertFalse(metadata.getAuthorizationSchemes().isEmpty(),
                    "authorization_schemes should contain at least one scheme");
            Assertions.assertEquals("urn:ietf:rfc:6749",
                    metadata.getAuthorizationSchemes().get(0).get("spec_urn"),
                    "authorization_schemes should advertise OAuth 2.0 (RFC 6749)");
        }
    }

    @Test
    public void testAdminConfigEndpointExposesDefaults() throws IOException {

        String adminConfigUrl = keycloakUrls.getAdmin() + "/realms/" + realm.getName() + "/ssf/config";

        try (SimpleHttpResponse response = http.doGet(adminConfigUrl)
                .auth(adminClient.tokenManager().getAccessTokenString())
                .asResponse()) {
            Assertions.assertEquals(200, response.getStatus(),
                    "Admin /ssf/config should return 200 for realm-admin");

            SsfConfigRepresentation config = response.asJson(SsfConfigRepresentation.class);

            // Both defaultSupportedEvents and availableSupportedEvents are returned
            // as event aliases so the admin UI can render them directly and
            // pre-select the defaults against the same option values.
            Assertions.assertNotNull(config.getDefaultSupportedEvents(),
                    "defaultSupportedEvents should be populated");
            Assertions.assertFalse(config.getDefaultSupportedEvents().isEmpty(),
                    "defaultSupportedEvents should contain at least one event");
            Assertions.assertTrue(
                    config.getDefaultSupportedEvents().contains(CaepCredentialChange.class.getSimpleName()),
                    "defaultSupportedEvents should include the CaepCredentialChange alias");
            Assertions.assertTrue(
                    config.getDefaultSupportedEvents().contains(CaepSessionRevoked.class.getSimpleName()),
                    "defaultSupportedEvents should include the CaepSessionRevoked alias");

            Assertions.assertNotNull(config.getAvailableSupportedEvents(),
                    "availableSupportedEvents should be populated");
            Assertions.assertTrue(
                    config.getAvailableSupportedEvents().contains(CaepCredentialChange.class.getSimpleName()),
                    "availableSupportedEvents should include the CaepCredentialChange alias");
            Assertions.assertTrue(
                    config.getAvailableSupportedEvents().contains(CaepSessionRevoked.class.getSimpleName()),
                    "availableSupportedEvents should include the CaepSessionRevoked alias");

            Assertions.assertEquals(
                    SsfTransmitterConfig.DEFAULT_PUSH_ENDPOINT_CONNECT_TIMEOUT_MILLIS,
                    config.getDefaultPushEndpointConnectTimeoutMillis(),
                    "defaultPushEndpointConnectTimeoutMillis should match the SPI default");
            Assertions.assertEquals(
                    SsfTransmitterConfig.DEFAULT_PUSH_ENDPOINT_SOCKET_TIMEOUT_MILLIS,
                    config.getDefaultPushEndpointSocketTimeoutMillis(),
                    "defaultPushEndpointSocketTimeoutMillis should match the SPI default");
        }
    }

    @Test
    public void testAdminConfigEndpointRequiresAuthentication() throws IOException {

        String adminConfigUrl = keycloakUrls.getAdmin() + "/realms/" + realm.getName() + "/ssf/config";

        try (SimpleHttpResponse response = http.doGet(adminConfigUrl).asResponse()) {
            Assertions.assertEquals(401, response.getStatus(),
                    "Unauthenticated requests should be rejected with 401");
        }
    }

    public static class SsfTransmitterKeycloakServerConfig extends DefaultKeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            KeycloakServerConfigBuilder configured = super.configure(config);
            config.features(Profile.Feature.SSF);
            config.log().categoryLevel("org.keycloak.protocol.ssf", "DEBUG");
            return configured;
        }
    }

    public static class SsfTransmitterRealm implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            realm.name("ssf-transmitter-test");
            realm.attribute(Ssf.SSF_TRANSMITTER_ENABLED_KEY, "true");
            return realm;
        }
    }
}
