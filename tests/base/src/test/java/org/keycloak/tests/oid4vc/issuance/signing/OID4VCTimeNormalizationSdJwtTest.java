package org.keycloak.tests.oid4vc.issuance.signing;

import java.util.ArrayList;
import java.util.Map;

import org.keycloak.TokenVerifier;
import org.keycloak.constants.OID4VCIConstants;
import org.keycloak.protocol.oid4vc.issuance.mappers.OID4VCIssuedAtTimeClaimMapper;
import org.keycloak.protocol.oid4vc.model.CredentialResponse;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.sdjwt.vp.SdJwtVP;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.tests.oid4vc.OID4VCIssuerTestBase;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * SD-JWT variant: ensure realm-level rounding of issuanceDate propagates to iat when mapper sources from VC.
 */
@KeycloakIntegrationTest(config = OID4VCIssuerTestBase.VCTestServerConfig.class)
public class OID4VCTimeNormalizationSdJwtTest extends OID4VCTimeNormalizationTestBase {

    @Test
    public void testSdJwtIatRoundedViaRealmNormalizedIssuanceDate() throws Exception {
        configureRoundStrategy("DAY");
        addIatFromVcMapper();

        CredentialResponse credentialResponse = issueCredentialForScope(sdJwtTypeCredentialScope);
        assertNotNull(credentialResponse, "Credential response should not be null");

        SdJwtVP sdJwtVP = SdJwtVP.of(credentialResponse.getCredentials().get(0).getCredential().toString());
        JsonWebToken jwt = TokenVerifier.create(sdJwtVP.getIssuerSignedJWT().getJws(), JsonWebToken.class).getToken();
        Long iat = jwt.getIat();
        assertNotNull(iat, "iat should be present");
        assertEquals(0, iat % 86400, "iat should be truncated to start of day (multiple of 86400)");
    }

    /**
     * Adds an {@code iat} mapper sourced from the VC issuanceDate to the SD-JWT credential scope.
     */
    private void addIatFromVcMapper() {
        ProtocolMapperRepresentation pr = new ProtocolMapperRepresentation();
        pr.setName("iat-from-vc");
        pr.setProtocol(OID4VCIConstants.OID4VC_PROTOCOL);
        pr.setProtocolMapper(OID4VCIssuedAtTimeClaimMapper.MAPPER_ID);
        pr.setConfig(Map.of(
                OID4VCIssuedAtTimeClaimMapper.CLAIM_NAME, "iat",
                OID4VCIssuedAtTimeClaimMapper.VALUE_SOURCE, "VC"
        ));

        ClientScopeRepresentation scopeRep = testRealm.admin().clientScopes()
                .get(sdJwtTypeCredentialScope.getId()).toRepresentation();
        if (scopeRep.getProtocolMappers() == null) {
            scopeRep.setProtocolMappers(new ArrayList<>());
        }
        scopeRep.getProtocolMappers().add(pr);
        testRealm.admin().clientScopes().get(sdJwtTypeCredentialScope.getId()).update(scopeRep);
    }
}
