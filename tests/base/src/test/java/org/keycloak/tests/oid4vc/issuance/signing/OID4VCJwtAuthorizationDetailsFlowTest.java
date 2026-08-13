package org.keycloak.tests.oid4vc.issuance.signing;

import org.keycloak.protocol.oid4vc.model.CredentialScopeRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.tests.oid4vc.OID4VCIssuerTestBase;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JWT-specific authorization_details tests for scope + authorization_code grant.
 */
@KeycloakIntegrationTest(config = OID4VCIssuerTestBase.VCTestServerConfig.class)
public class OID4VCJwtAuthorizationDetailsFlowTest extends OID4VCAuthorizationDetailsFlowTestBase {

    @Override
    protected CredentialScopeRepresentation getCredentialScope() {
        return jwtTypeCredentialScope;
    }

    @Override
    protected void verifyCredentialStructure(Object credentialObj) {
        assertNotNull(credentialObj, "Credential object should not be null");

        assertInstanceOf(String.class, credentialObj, "JWT credential should be a string");
        String jwtString = (String) credentialObj;
        assertFalse(jwtString.isEmpty(), "JWT credential should not be empty");
        assertTrue(jwtString.contains("."), "JWT should contain dots");
    }
}
