package org.keycloak.tests.oid4vc.haip;


import org.keycloak.TokenVerifier;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.protocol.oid4vc.model.CredentialResponse;
import org.keycloak.protocol.oid4vc.model.CredentialScopeRepresentation;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.sdjwt.IssuerSignedJWT;
import org.keycloak.sdjwt.vp.SdJwtVP;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.tests.oid4vc.OID4VCIssuerTestBase;
import org.keycloak.tests.oid4vc.OID4VCTestContext;
import org.keycloak.testsuite.util.oauth.PkceGenerator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static org.keycloak.OID4VCConstants.CLAIM_NAME_VCT;
import static org.keycloak.authentication.authenticators.client.AttestationBasedClientAuthenticator.OAUTH_CLIENT_ATTESTATION_HEADER;
import static org.keycloak.authentication.authenticators.client.AttestationBasedClientAuthenticator.OAUTH_CLIENT_ATTESTATION_POP_HEADER;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Mirrors oid4vci-1_0-issuer-haip-test-plan:oid4vci-1_0-issuer-happy-flow
 */
@KeycloakIntegrationTest(config = OID4VCIssuerTestBase.VCTestServerWithHAIPEnabled.class)
public class HAIPIssuerConformanceTest extends OID4VCIssuerTestBase {

    @BeforeEach
    void beforeEach() {
        oauth.client(pubClient.getClientId());
        setClientPolicyEnabled(VCI_CLIENT_POLICY_HAIP, true);
    }

    @Test
    public void testIssuerHappyFlow() {

        var ctx = new OID4VCTestContext(pubClient, sdJwtTypeCredentialScope);

        var pkce = PkceGenerator.s256();

        // Generate ABCA Headers
        //
        KeyWrapper rsaKey = wallet.getRSAKeyPair(ctx);
        String attestationJwt = wallet.buildClientAttestationJWT(ctx, rsaKey);
        String attestationPoPJwt = wallet.buildClientAttestationPoPJWT(ctx, rsaKey);

        // Send PAR Request
        //
        String requestUri = oauth.pushedAuthorizationRequest()
                .header(OAUTH_CLIENT_ATTESTATION_HEADER, attestationJwt)
                .header(OAUTH_CLIENT_ATTESTATION_POP_HEADER, attestationPoPJwt)
                .scopeParam(ctx.getScope())
                .codeChallenge(pkce)
                .send().getRequestUri();
        assertNotNull(requestUri, "No requestUri");

        /* Send Authorization Request
        //
        KeyWrapper ecKey = wallet.getECKeyPair(ctx);
        JWK jwkEc = wallet.getECJwk(ecKey);

        String authCode = wallet.authorizationRequest()
                .requestUri(requestUri)
                .codeChallenge(pkce)
                .dpopJkt(JWKSUtils.computeThumbprint(jwkEc))
                .send(ctx.getHolder(), TEST_PASSWORD)
                .getCode();
        assertNotNull(authCode, "No auth code");

        // Send Token Request
        //
        String tokenEndpoint = oauth.getEndpoints().getToken();
        String dpopProof = wallet.generateSignedDPoPProof(tokenEndpoint, ecKey, null);

        AccessTokenResponse tokenResponse = wallet.accessTokenRequest(ctx, authCode)
                .header(OAUTH_CLIENT_ATTESTATION_HEADER, attestationJwt)
                .header(OAUTH_CLIENT_ATTESTATION_POP_HEADER, attestationPoPJwt)
                .codeVerifier(pkce)
                .dpopProof(dpopProof)
                .send();
        String tokenType = tokenResponse.getTokenType();
        String accessToken = tokenResponse.getAccessToken();
        assertEquals(TokenUtil.TOKEN_TYPE_DPOP, tokenType);
        assertNotNull(accessToken, "No access token");

        String credentialIdentifier = ctx.getAuthorizedCredentialIdentifier();
        assertNotNull(credentialIdentifier, "No authorized credential identifier");

        // Send Nonce Request
        //
        String nonce = wallet.nonceRequest().send().getNonce();
        Proofs jwtProof = wallet.generateJwtProof(ctx, ecKey, nonce);

        // Send Credential Request
        // Note, we use the same EC key for DPoP and Holder identity
        //
        String credentialEndpoint = oauth.getEndpoints().getOid4vcCredential();
        dpopProof = wallet.generateSignedDPoPProof(credentialEndpoint, ecKey, accessToken);

        CredentialResponse credResponse = wallet.credentialRequest(ctx, tokenType, accessToken)
                .credentialIdentifier(credentialIdentifier)
                .dpopProof(dpopProof)
                .proofs(jwtProof)
                .send().getCredentialResponse();

        // Verify Credential Response
        //
        verifyCredentialResponse(ctx, credResponse);
        */
    }

    private void verifyCredentialResponse(OID4VCTestContext ctx, CredentialResponse credResponse) throws Exception {

        CredentialScopeRepresentation credScope = ctx.getCredentialScope();
        String issuer = wallet.getIssuerMetadata(ctx).getCredentialIssuer();
        CredentialResponse.Credential credObj = credResponse.getCredentials().get(0);
        assertNotNull(credObj, "The first credential in the array should not be null");

        SdJwtVP sdJwtVP = SdJwtVP.of(credObj.getCredential().toString());
        IssuerSignedJWT issuerSignedJWT = sdJwtVP.getIssuerSignedJWT();
        JsonWebToken vcSdJwt = TokenVerifier.create(issuerSignedJWT.getJws(), JsonWebToken.class).getToken();
        Map<String, Object> otherClaims = vcSdJwt.getOtherClaims();
        assertEquals(issuer, vcSdJwt.getIssuer());
        assertEquals(credScope.getVct(), otherClaims.get(CLAIM_NAME_VCT));

        Map<String, String> claims = sdJwtVP.getClaims().values().stream().collect(Collectors.toMap(
                arrayNode -> arrayNode.get(1).asText(),
                arrayNode -> arrayNode.get(2).asText()
        ));
        assertEquals("Alice", claims.get("firstName"));
        assertEquals("Wonderland", claims.get("lastName"));
        assertEquals("alice@email.cz", claims.get("email"));
    }
}
