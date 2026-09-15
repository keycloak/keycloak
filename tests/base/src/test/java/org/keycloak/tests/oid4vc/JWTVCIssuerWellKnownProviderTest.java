package org.keycloak.tests.oid4vc;

import org.keycloak.admin.client.resource.ComponentsResource;
import org.keycloak.jose.jwk.JSONWebKeySet;
import org.keycloak.protocol.oid4vc.issuance.JWTVCIssuerWellKnownProvider;
import org.keycloak.protocol.oid4vc.model.JWTVCIssuerMetadata;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.annotations.TestSetup;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Endpoint test for the JWT VC Issuer well-known provider at {@code /.well-known/jwt-vc-issuer}.
 *
 * @see <a href="https://www.ietf.org/archive/id/draft-ietf-oauth-sd-jwt-vc-03.html#name-jwt-vc-issuer-metadata">JWT VC Issuer Metadata</a>
 */
@KeycloakIntegrationTest(config = OID4VCIssuerTestBase.VCTestServerConfig.class)
public class JWTVCIssuerWellKnownProviderTest extends OID4VCIssuerTestBase {

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @TestSetup
    public void configureTestRealm() {
        super.configureTestRealm();

        // Add an RSA key so there are keys in the JWKS
        ComponentsResource components = testRealm.admin().components();
        components.add(getRsaKeyProvider(getRsaKey_Default())).close();
    }

    @Test
    public void getConfig() {
        String expectedIssuer = keycloakUrls.getBase() + "/realms/" + testRealm.getName();

        runOnServer.run(session -> {
            JWTVCIssuerWellKnownProvider provider = new JWTVCIssuerWellKnownProvider(session);
            JWTVCIssuerMetadata metadata = (JWTVCIssuerMetadata) provider.getConfig();

            assertEquals(expectedIssuer, metadata.getIssuer(), "The correct issuer should be included.");
            JSONWebKeySet jwks = metadata.getJwks();
            assertNotNull(jwks.getKeys(), "The key set shall not be null");
            assertTrue(jwks.getKeys().length > 0, "The key set shall not be empty");
        });
    }
}
