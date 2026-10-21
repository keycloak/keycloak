package org.keycloak.tests.oid4vc.issuance.signing;

import java.time.Instant;

import org.keycloak.TokenVerifier;
import org.keycloak.protocol.oid4vc.model.CredentialResponse;
import org.keycloak.protocol.oid4vc.model.VerifiableCredential;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.tests.oid4vc.OID4VCIssuerTestBase;
import org.keycloak.util.JsonSerialization;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Integration tests validating time-claim normalization configuration and effects on JWT-VC.
 */
@KeycloakIntegrationTest(config = OID4VCIssuerTestBase.VCTestServerConfig.class)
public class OID4VCTimeNormalizationTest extends OID4VCTimeNormalizationTestBase {

    @Test
    public void testJwtVcNbfRoundedToStartOfDayUtc() throws Exception {
        configureRoundStrategy("DAY");

        CredentialResponse credentialResponse = issueCredentialForScope(jwtTypeCredentialScope);

        assertNotNull(credentialResponse, "Credential response should not be null");
        assertNotNull(credentialResponse.getCredentials(), "Credentials should not be null");
        String jwtString = (String) credentialResponse.getCredentials().get(0).getCredential();

        JsonWebToken jwt = TokenVerifier.create(jwtString, JsonWebToken.class).getToken();
        assertNotNull(jwt, "JWT should not be null");

        Long nbf = jwt.getNbf();
        assertNotNull(nbf, "nbf should be present");
        assertEquals(0, nbf % 86400, "nbf should be truncated to start of day (multiple of 86400)");

        Object vcObj = jwt.getOtherClaims().get("vc");
        VerifiableCredential vc = JsonSerialization.mapper.convertValue(vcObj, VerifiableCredential.class);
        Instant issuance = vc.getIssuanceDate();
        assertNotNull(issuance, "issuanceDate should be present");
        assertEquals(0, issuance.getEpochSecond() % 86400, "issuanceDate should be truncated to start of day (multiple of 86400)");
    }
}
