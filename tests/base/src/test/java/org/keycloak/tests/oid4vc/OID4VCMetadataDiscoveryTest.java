package org.keycloak.tests.oid4vc;

import java.util.List;

import org.keycloak.protocol.oauth2.OAuth2WellKnownProviderFactory;
import org.keycloak.protocol.oid4vc.model.CredentialIssuer;
import org.keycloak.protocol.oidc.OIDCWellKnownProviderFactory;
import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests that OID4VC metadata is correctly exposed via the OAuth2 AS and OpenID well-known endpoints.
 */
@KeycloakIntegrationTest(config = OID4VCIssuerTestBase.VCTestServerConfig.class)
public class OID4VCMetadataDiscoveryTest extends OID4VCIssuerTestBase {

    private static final String AUTHORIZATION_DETAILS_TYPES_SUPPORTED = "authorization_details_types_supported";
    private static final String OPENID_CREDENTIAL = "openid_credential";

    @Test
    public void testOAuth2WellKnownMetadata() {
        verifyAuthorizationDetailsMetadata(oauthServerLegacyWellKnownPath());
        verifyAuthorizationDetailsMetadata(oauthServerWellKnownPath());
    }

    @Test
    public void testOpenIDConfigurationMetadata() {
        verifyAuthorizationDetailsMetadata(openidLegacyConfigurationPath());
    }

    @Test
    public void testCredentialIssuerAuthorizationServerMetadata() {
        CredentialIssuer credentialIssuer = oauth.oid4vc().doIssuerMetadataRequest().getMetadata();
        assertNotNull(credentialIssuer);
        assertNotNull(credentialIssuer.getAuthorizationServers());
        assertFalse(credentialIssuer.getAuthorizationServers().isEmpty());

        String authServerWellKnownPath = credentialIssuer.getAuthorizationServers().get(0)
                + "/.well-known/" + OAuth2WellKnownProviderFactory.PROVIDER_ID;
        OIDCConfigurationRepresentation authServerConfig = oauth.wellknownRequest()
                .endpoint(authServerWellKnownPath)
                .send()
                .getOidcConfiguration();
        assertNotNull(authServerConfig.getAuthorizationDetailsTypesSupported());
        assertTrue(authServerConfig.getAuthorizationDetailsTypesSupported().contains(OPENID_CREDENTIAL));
    }

    @Test
    public void testMetadataAbsentWhenOID4VCDisabled() {
        try {
            setVerifiableCredentialsEnabled(false);

            OIDCConfigurationRepresentation metadata = oauth.wellknownRequest()
                    .endpoint(oauth.getBaseUrl() + "/" + oauthServerLegacyWellKnownPath())
                    .send()
                    .getOidcConfiguration();

            assertNotNull(metadata);
            List<String> authDetailsTypes = metadata.getAuthorizationDetailsTypesSupported();
            if (authDetailsTypes != null) {
                assertFalse(authDetailsTypes.contains(OPENID_CREDENTIAL));
            }
        } finally {
            setVerifiableCredentialsEnabled(true);
        }
    }

    private String oauthServerLegacyWellKnownPath() {
        return "realms/" + testRealm.getName() + "/.well-known/" + OAuth2WellKnownProviderFactory.PROVIDER_ID;
    }

    private String oauthServerWellKnownPath() {
        return ".well-known/" + OAuth2WellKnownProviderFactory.PROVIDER_ID + "/realms/" + testRealm.getName();
    }

    private String openidLegacyConfigurationPath() {
        return "realms/" + testRealm.getName() + "/.well-known/" + OIDCWellKnownProviderFactory.PROVIDER_ID;
    }

    private void verifyAuthorizationDetailsMetadata(String path) {
        OIDCConfigurationRepresentation metadata = oauth.wellknownRequest()
                .endpoint(oauth.getBaseUrl() + "/" + path)
                .send()
                .getOidcConfiguration();

        assertNotNull(metadata, "Metadata for path " + path + " should not be null");
        List<String> authDetailsTypes = metadata.getAuthorizationDetailsTypesSupported();
        assertNotNull(authDetailsTypes, AUTHORIZATION_DETAILS_TYPES_SUPPORTED + " should be present");
        assertTrue(authDetailsTypes.contains(OPENID_CREDENTIAL));
    }
}
