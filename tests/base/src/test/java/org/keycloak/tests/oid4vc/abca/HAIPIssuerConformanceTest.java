package org.keycloak.tests.oid4vc.abca;


import java.util.Map;
import java.util.stream.Collectors;

import org.keycloak.TokenVerifier;
import org.keycloak.broker.trust.DefaultTrustIdentityProviderConfig;
import org.keycloak.broker.trust.DefaultTrustIdentityProviderFactory;
import org.keycloak.common.VerificationException;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.jose.jwk.JSONWebKeySet;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jwk.JWKBuilder;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oid4vc.model.CredentialResponse;
import org.keycloak.protocol.oid4vc.model.CredentialScopeRepresentation;
import org.keycloak.protocol.oid4vc.model.Proofs;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.sdjwt.IssuerSignedJWT;
import org.keycloak.sdjwt.vp.SdJwtVP;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.annotations.TestSetup;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.ErrorPage;
import org.keycloak.tests.oid4vc.OID4VCIssuerTestBase;
import org.keycloak.tests.oid4vc.OID4VCTestContext;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.ParResponse;
import org.keycloak.testsuite.util.oauth.PkceGenerator;
import org.keycloak.util.JsonSerialization;
import org.keycloak.util.TokenUtil;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.keycloak.OID4VCConstants.CLAIM_NAME_VCT;
import static org.keycloak.authentication.authenticators.client.AttestationBasedClientAuthenticator.OAUTH_CLIENT_ATTESTATION_HEADER;
import static org.keycloak.authentication.authenticators.client.AttestationBasedClientAuthenticator.OAUTH_CLIENT_ATTESTATION_POP_HEADER;
import static org.keycloak.tests.oid4vc.OID4VCProofTestUtils.createRsaKeyPair;
import static org.keycloak.tests.oid4vc.OID4VCTestContext.CLIENT_ATTESTER_ATTACHMENT_KEY;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Replicates various tests in oid4vci-1_0-issuer-haip-test-plan
 */
@KeycloakIntegrationTest(config = OID4VCIssuerTestBase.VCTestServerWithABCAEnabled.class)
public class HAIPIssuerConformanceTest extends OID4VCIssuerTestBase {

    @InjectPage
    ErrorPage errorPage;

    private static OIDCClientAttester attester;
    private static String attesterJwks;

    @TestSetup
    public void configure() throws Exception {
        var kw = createRsaKeyPair("openid-abca-attester-key");
        JWK jwk = JWKBuilder.create()
                .kid(kw.getKid())
                .algorithm(kw.getAlgorithm())
                .rsa(kw.getPublicKey(), kw.getCertificate());
        JSONWebKeySet jwks = new JSONWebKeySet();
        jwks.setKeys(new JWK[] { jwk });
        attesterJwks = JsonSerialization.writeValueAsString(jwks);
        attester = new OIDCMockClientAttester(kw);
    }

    @BeforeEach
    void beforeEach() {
        String jwks = attesterJwks;
        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            configureTrustIdentityProvider(realm, OAUTH_CLIENT_ATTESTATION_DEFAULT_TRUST_IDP_ALIAS,
                    DefaultTrustIdentityProviderFactory.PROVIDER_ID,
                    Map.of(DefaultTrustIdentityProviderConfig.TRUSTED_JWKS, jwks));
        });
        setClientPolicyEnabled(VCI_CLIENT_POLICY_HAIP, true);
        oauth.client(abcaClient.getClientId(), null);
    }

    /**
     * oid4vci-1_0-issuer-happy-flow
     *
     * Validates the standard credential issuance flow using an emulated wallet, as defined by OpenID4VCI.
     */
    @Test
    public void testIssuerHappyFlow() throws Exception {

        var ctx = new OID4VCTestContext(abcaClient, sdJwtTypeCredentialScope);
        ctx.putAttachment(CLIENT_ATTESTER_ATTACHMENT_KEY, attester);

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

        // Send Authorization Request
        //
        String authCode = wallet.authorizationRequest()
                .requestUri(requestUri)
                .codeChallenge(pkce)
                .send(ctx.getHolder(), TEST_PASSWORD)
                .getCode();
        assertNotNull(authCode, "No auth code");

        // Send Token Request
        //
        KeyWrapper ecKey = wallet.getECKeyPair(ctx);
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
    }

    /**
     * fapi2-security-profile-final-state-only-outside-request-object-not-used
     *
     * Uses a request object that does not contain state, but state is passed in the url parameters to the authorization endpoint
     * (hence state should be ignored, as FAPI says only parameters inside the request object should be used).
     * The expected result is a successful authentication that returns neither state nor s_hash.
     */
    @Test
    public void testOnlyParametersInsideRequestObjectAreUsed() {

        var ctx = new OID4VCTestContext(abcaClient, sdJwtTypeCredentialScope);
        ctx.putAttachment(CLIENT_ATTESTER_ATTACHMENT_KEY, attester);

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

        // Send Authorization Request
        //
        wallet.authorizationRequest()
                .requestUri(requestUri)
                .codeChallenge(pkce)
                .state("123456")
                .openLoginForm();
        errorPage.assertCurrent();
        assertEquals("PAR request did not include query parameter", errorPage.getError());

        // When error on redirect_uri is configured, we'd need to assert the JSON response
        //
        // assertFalse(authRequest.openLoginForm(), "Error expected");
        // AuthorizationEndpointResponse authResponse = authRequest.parseLoginResponse();
        //
        // assertNull(authResponse.getCode(), "Expected no auth code");
        // assertEquals("invalid_request", authResponse.getError());
        // assertEquals("PAR request did not include query parameter", authResponse.getErrorDescription());
    }

    /**
     * fapi2-security-profile-final-ensure-request-object-without-redirect-uri-fails
     *
     * This test should end with the authorization server showing an error message that the request object is invalid
     * due to the missing redirect uri.
     */
    @Test
    public void testRequestObjectWithoutRedirectUriFails() {

        var ctx = new OID4VCTestContext(abcaClient, sdJwtTypeCredentialScope);
        ctx.putAttachment(CLIENT_ATTESTER_ATTACHMENT_KEY, attester);

        var pkce = PkceGenerator.s256();

        // Generate ABCA Headers
        //
        KeyWrapper rsaKey = wallet.getRSAKeyPair(ctx);
        String attestationJwt = wallet.buildClientAttestationJWT(ctx, rsaKey);
        String attestationPoPJwt = wallet.buildClientAttestationPoPJWT(ctx, rsaKey);

        String redirectUri = oauth.config().getRedirectUri();
        try {
            oauth.config().redirectUri(null);

            // Send PAR Request (without redirect_uri)
            //
            ParResponse parResponse = oauth.pushedAuthorizationRequest()
                    .header(OAUTH_CLIENT_ATTESTATION_HEADER, attestationJwt)
                    .header(OAUTH_CLIENT_ATTESTATION_POP_HEADER, attestationPoPJwt)
                    .scopeParam(ctx.getScope())
                    .codeChallenge(pkce)
                    .send();
            assertFalse(parResponse.isSuccess());
            assertNull(parResponse.getRequestUri(), "Expected no request_uri");
            assertEquals("invalid_request", parResponse.getError());
            assertEquals("PAR is required to have a 'redirect_uri' parameter", parResponse.getErrorDescription());
        } finally {
            oauth.config().redirectUri(redirectUri);
        }
    }

    /**
     * fapi2-security-profile-final-ensure-response-type-code-idtoken-fails
     *
     * This test uses response_type=code+id_token in the authorization request, which is not permitted in FAPI2 Security Profile
     * as it would return an id_token via the browser where it may be leaked. The authorization server should show an error message
     * that the response type is unsupported or the request is invalid.
     */
    @Test
    public void testRequestObjectWithHybridResponseTypeFails() {

        var ctx = new OID4VCTestContext(abcaClient, sdJwtTypeCredentialScope);
        ctx.putAttachment(CLIENT_ATTESTER_ATTACHMENT_KEY, attester);

        var pkce = PkceGenerator.s256();

        // Generate ABCA Headers
        //
        KeyWrapper rsaKey = wallet.getRSAKeyPair(ctx);
        String attestationJwt = wallet.buildClientAttestationJWT(ctx, rsaKey);
        String attestationPoPJwt = wallet.buildClientAttestationPoPJWT(ctx, rsaKey);

        String responseType = oauth.config().getResponseType();
        try {
            oauth.config().responseType("code id_token");

            // Send PAR Request (with hybrid response type)
            //
            ParResponse parResponse = oauth.pushedAuthorizationRequest()
                    .header(OAUTH_CLIENT_ATTESTATION_HEADER, attestationJwt)
                    .header(OAUTH_CLIENT_ATTESTATION_POP_HEADER, attestationPoPJwt)
                    .scopeParam(ctx.getScope())
                    .codeChallenge(pkce)
                    .send();
            assertFalse(parResponse.isSuccess());
            assertNull(parResponse.getRequestUri(), "Expected no request_uri");
            assertEquals("invalid_request", parResponse.getError());
            String errorDescription = parResponse.getErrorDescription();
            assertEquals("Implicit/Hybrid flow is prohibited.", errorDescription);
        } finally {
            oauth.config().responseType(responseType);
        }
    }

    /**
     * fapi2-security-profile-final-ensure-holder-of-key-required
     *
     * This test ensures that all endpoints comply with the TLS version/cipher limitations and that the token endpoint
     * returns an error if a valid request is sent without a holder of key mechanism (i.e. without DPoP / MTLS).
     *
     */
    @Test
    public void testHolderOfKeyRequired() {

        var ctx = new OID4VCTestContext(abcaClient, sdJwtTypeCredentialScope);
        ctx.putAttachment(CLIENT_ATTESTER_ATTACHMENT_KEY, attester);

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

        // Send Authorization Request
        //
        String authCode = wallet.authorizationRequest()
                .requestUri(requestUri)
                .codeChallenge(pkce)
                .send(ctx.getHolder(), TEST_PASSWORD)
                .getCode();
        assertNotNull(authCode, "No auth code");

        // Send Token Request (without DPoP proof)
        //
        String clientId = oauth.config().getClientId();
        try {
            oauth.config().client(null);

            AccessTokenResponse tokenResponse = wallet.accessTokenRequest(ctx, authCode)
                    .codeVerifier(pkce)
                    .send();
            assertFalse(tokenResponse.isSuccess());
            assertEquals("invalid_request", tokenResponse.getError());
            assertEquals("DPoP proof is missing", tokenResponse.getErrorDescription());
        } finally {
            oauth.config().client(clientId);
        }
    }

    // Private ---------------------------------------------------------------------------------------------------------

    private void verifyCredentialResponse(OID4VCTestContext ctx, CredentialResponse credResponse) throws VerificationException {

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
