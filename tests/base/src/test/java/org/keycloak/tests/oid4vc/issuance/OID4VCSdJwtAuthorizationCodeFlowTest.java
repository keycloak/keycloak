package org.keycloak.tests.oid4vc.issuance;

import org.keycloak.protocol.oid4vc.model.CredentialScopeRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.tests.oid4vc.OID4VCIssuerTestBase;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SD-JWT-specific authorization code flow tests.
 * Extends {@link OID4VCAuthorizationCodeFlowTestBase} to inherit all common test logic while providing
 * SD-JWT-specific credential format, scope, and claim configuration.
 */
@KeycloakIntegrationTest(config = OID4VCIssuerTestBase.VCTestServerConfig.class)
public class OID4VCSdJwtAuthorizationCodeFlowTest extends OID4VCAuthorizationCodeFlowTestBase {

    @Override
    protected String getCredentialFormat() {
        return "sd_jwt_vc";
    }

    @Override
    protected CredentialScopeRepresentation getCredentialScope() {
        return sdJwtTypeCredentialScope;
    }

    @Override
    protected String getExpectedClaimPath() {
        // In sd_jwt_vc the last-name claim sits at the top level as lastName
        return "lastName";
    }

    @Override
    protected String getFirstNameProtocolMapperName() {
        // getUserAttributeMapper("firstName", "firstName") → name "firstName-mapper"
        return "firstName-mapper";
    }

    @Override
    protected void verifyCredentialStructure(Object credentialObj) {
        assertNotNull(credentialObj, "SD-JWT credential object should not be null");
        assertInstanceOf(String.class, credentialObj, "SD-JWT credential should be a string");
        String sdJwtString = (String) credentialObj;
        assertFalse(sdJwtString.isEmpty(), "SD-JWT credential should not be empty");
        // SD-JWT format: issuer-jwt ~ disclosures ~ kb-jwt  (joined by ~)
        assertTrue(sdJwtString.contains("~"), "SD-JWT credential should contain tilde (~) as delimiter");
        // The issuer-signed JWT part is still a JWS
        assertTrue(sdJwtString.contains("."), "SD-JWT credential should contain dots in the JWT portion");
    }
}
