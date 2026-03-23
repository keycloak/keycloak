package org.keycloak.tests.oid4vc.issuance.signing;

import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.tests.oid4vc.OID4VCAuthorizationCodeAuthorizationDetailsFlowTestBase;
import org.keycloak.tests.oid4vc.OID4VCIssuerTestBase;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JWT-specific authorization_details tests for scope + authorization_code grant.
 */
@KeycloakIntegrationTest(config = OID4VCIssuerTestBase.VCTestServerConfig.class)
public class OID4VCJwtAuthorizationCodeAuthorizationDetailsFlowTest extends OID4VCAuthorizationCodeAuthorizationDetailsFlowTestBase {

    @Override
    protected ClientScopeRepresentation getCredentialClientScope() {
        return jwtTypeCredentialScope;
    }

    @Override
    protected void verifyCredentialStructure(Object credentialObj) {
        assertNotNull(credentialObj, "Credential object should not be null");

        assertTrue(credentialObj instanceof String, "JWT credential should be a string");
        String jwtString = (String) credentialObj;
        assertFalse(jwtString.isEmpty(), "JWT credential should not be empty");
        assertTrue(jwtString.contains("."), "JWT should contain dots");
    }
}
