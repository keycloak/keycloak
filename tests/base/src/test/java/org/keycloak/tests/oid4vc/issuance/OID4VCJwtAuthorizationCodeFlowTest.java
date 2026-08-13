package org.keycloak.tests.oid4vc.issuance;

import org.keycloak.protocol.oid4vc.model.CredentialScopeRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.tests.oid4vc.OID4VCIssuerTestBase;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JWT-specific authorization code flow tests.
 * Extends {@link OID4VCAuthorizationCodeFlowTestBase} to inherit all common test logic while providing
 * JWT-specific credential format, scope, and claim configuration.
 */
@KeycloakIntegrationTest(config = OID4VCIssuerTestBase.VCTestServerConfig.class)
public class OID4VCJwtAuthorizationCodeFlowTest extends OID4VCAuthorizationCodeFlowTestBase {

    @Override
    protected String getCredentialFormat() {
        return "jwt_vc";
    }

    @Override
    protected CredentialScopeRepresentation getCredentialScope() {
        return jwtTypeCredentialScope;
    }

    @Override
    protected String getExpectedClaimPath() {
        // In jwt_vc the last-name claim sits at vc.credentialSubject.family_name
        return "family_name";
    }

    @Override
    protected String getFirstNameProtocolMapperName() {
        // JWT scope uses test-credential-mappers.json where the firstName mapper is named "givenName"
        return "givenName";
    }

    @Override
    protected void verifyCredentialStructure(Object credentialObj) {
        assertNotNull(credentialObj, "JWT credential object should not be null");
        assertInstanceOf(String.class, credentialObj, "JWT credential should be a string");
        String jwtString = (String) credentialObj;
        assertFalse(jwtString.isEmpty(), "JWT credential should not be empty");
        assertTrue(jwtString.contains("."), "JWT credential should contain dots (header.payload.signature)");
    }
}
