package org.keycloak.tests.oid4vc.issuance.signing;

import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.tests.oid4vc.OID4VCAuthorizationCodeAuthorizationDetailsFlowTestBase;
import org.keycloak.tests.oid4vc.OID4VCIssuerTestBase;

import static org.keycloak.OID4VCConstants.SDJWT_DELIMITER;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SD-JWT-specific authorization_details tests for scope + authorization_code grant.
 */
@KeycloakIntegrationTest(config = OID4VCIssuerTestBase.VCTestServerConfig.class)
public class OID4VCSdJwtAuthorizationCodeAuthorizationDetailsFlowTest extends OID4VCAuthorizationCodeAuthorizationDetailsFlowTestBase {

    @Override
    protected ClientScopeRepresentation getCredentialClientScope() {
        return sdJwtTypeCredentialScope;
    }

    @Override
    protected void verifyCredentialStructure(Object credentialObj) {
        assertNotNull(credentialObj, "Credential object should not be null");

        assertTrue(credentialObj instanceof String, "SD-JWT credential should be a string");
        String sdJwtString = (String) credentialObj;
        assertFalse(sdJwtString.isEmpty(), "SD-JWT credential should not be empty");
        assertTrue(sdJwtString.contains("."), "SD-JWT should contain dots");
        assertTrue(sdJwtString.contains(SDJWT_DELIMITER), "SD-JWT should contain tilde");
    }
}
