package org.keycloak.tests.oid4vc.abca;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.keycloak.TokenVerifier;
import org.keycloak.broker.trust.DefaultTrustIdentityProviderConfig;
import org.keycloak.broker.trust.DefaultTrustIdentityProviderFactory;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.jose.jwk.JSONWebKeySet;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jwk.JWKBuilder;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oid4vc.issuance.TimeClaimNormalizer;
import org.keycloak.protocol.oid4vc.model.CredentialIssuer;
import org.keycloak.protocol.oid4vc.model.CredentialIssuer.BatchCredentialIssuance;
import org.keycloak.protocol.oid4vc.model.CredentialResponse;
import org.keycloak.protocol.oid4vc.model.CredentialResponse.Credential;
import org.keycloak.protocol.oid4vc.model.CredentialScopeRepresentation;
import org.keycloak.protocol.oid4vc.model.Proofs;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.RefreshToken;
import org.keycloak.sdjwt.IssuerSignedJWT;
import org.keycloak.sdjwt.vp.SdJwtVP;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.annotations.TestSetup;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.ErrorPage;
import org.keycloak.tests.oid4vc.OID4VCBasicWallet.AuthorizationEndpointRequest;
import org.keycloak.tests.oid4vc.OID4VCIssuerTestBase;
import org.keycloak.tests.oid4vc.OID4VCTestContext;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;
import org.keycloak.testsuite.util.oauth.ParResponse;
import org.keycloak.testsuite.util.oauth.PkceGenerator;
import org.keycloak.util.JWKSUtils;
import org.keycloak.util.JsonSerialization;
import org.keycloak.util.TokenUtil;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.keycloak.OID4VCConstants.CLAIM_NAME_VCT;
import static org.keycloak.authentication.authenticators.client.AttestationBasedClientAuthenticator.ABCA_CNF_TYPE;
import static org.keycloak.authentication.authenticators.client.AttestationBasedClientAuthenticator.OAUTH_CLIENT_ATTESTATION_HEADER;
import static org.keycloak.authentication.authenticators.client.AttestationBasedClientAuthenticator.OAUTH_CLIENT_ATTESTATION_POP_HEADER;
import static org.keycloak.constants.OID4VCIConstants.BATCH_CREDENTIAL_ISSUANCE_BATCH_SIZE;
import static org.keycloak.constants.OID4VCIConstants.TIME_CLAIMS_STRATEGY;
import static org.keycloak.constants.OID4VCIConstants.TIME_RANDOMIZE_WINDOW_SECONDS;
import static org.keycloak.services.util.DPoPUtil.DPOP_CNF_TYPE;
import static org.keycloak.tests.oid4vc.OID4VCProofTestUtils.createRsaKeyPair;
import static org.keycloak.tests.oid4vc.OID4VCTestContext.CLIENT_ATTESTER_ATTACHMENT_KEY;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        jwks.setKeys(new JWK[]{jwk});
        attesterJwks = JsonSerialization.writeValueAsString(jwks);
        attester = new OIDCMockClientAttester(kw);

        setRealmAttributes(Map.of(
                BATCH_CREDENTIAL_ISSUANCE_BATCH_SIZE, "20",
                TIME_CLAIMS_STRATEGY, TimeClaimNormalizer.Strategy.RANDOMIZE.name(),
                TIME_RANDOMIZE_WINDOW_SECONDS, "300" // 5min
        ));
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
        Proofs jwtProofs = wallet.generateJwtProofs(ctx, nonce, ecKey);

        // Send Credential Request
        // Note, we use the same EC key for DPoP and Holder identity
        //
        String credentialEndpoint = oauth.getEndpoints().getOid4vcCredential();
        dpopProof = wallet.generateSignedDPoPProof(credentialEndpoint, ecKey, accessToken);

        CredentialResponse credResponse = wallet.credentialRequest(ctx, tokenType, accessToken)
                .credentialIdentifier(credentialIdentifier)
                .dpopProof(dpopProof)
                .proofs(jwtProofs)
                .send().getCredentialResponse();

        // Verify Credential Response
        //
        verifyCredentialResponse(ctx, jwtProofs, credResponse);
    }

    /**
     * oid4vci-1_0-issuer-batch-issuance
     * <p>
     * Requests multiple credentials in a single credential request, as advertised by the 'batch_credential_issuance' credential issuer metadata.
     * The wallet sends one key proof per requested credential and verifies that the issuer returns no more credentials than proofs sent,
     * that all returned credentials contain the same Credential Dataset, and that each credential is bound to a distinct proof key.
     */
    @Test
    public void testBatchIssuanceHappyFlow() throws Exception {

        var ctx = new OID4VCTestContext(abcaClient, sdJwtTypeCredentialScope);
        ctx.putAttachment(CLIENT_ATTESTER_ATTACHMENT_KEY, attester);

        // Verify Issuer Metadata
        //
        CredentialIssuer issuerMetadata = wallet.getIssuerMetadata(ctx);
        BatchCredentialIssuance bci = issuerMetadata.getBatchCredentialIssuance();
        assertEquals(20, bci.getBatchSize());

        // Generate ABCA Headers
        //
        KeyWrapper rsaKey = wallet.getRSAKeyPair(ctx);
        String attestationJwt = wallet.buildClientAttestationJWT(ctx, rsaKey);
        String attestationPoPJwt = wallet.buildClientAttestationPoPJWT(ctx, rsaKey);

        // Send PAR Request
        //
        var pkce = PkceGenerator.s256();
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

        // Proves the wallet is the legitimate holder of the DPoP-bound access token.
        // The access token is sender-constrained to the DPoP public key,
        // so every protected request must be signed with the matching private key
        KeyWrapper dpopKey = wallet.getECKeyPair(ctx, "dpopKey");

        String tokenEndpoint = oauth.getEndpoints().getToken();
        String dpopProof = wallet.generateSignedDPoPProof(tokenEndpoint, dpopKey, null);

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

        // Proves control of the key that the issued credential will be bound to.
        // Proofs must include the issuer audience and, when available, the issuer c_nonce
        KeyWrapper[] vcKeys = new KeyWrapper[bci.getBatchSize() + 2]; // two more keys than max batch-size
        for (int i = 0; i < vcKeys.length; i++) {
            vcKeys[i] = wallet.getECKeyPair(ctx, "vcKey" + (i + 1));
        }
        Proofs jwtProofs = wallet.generateJwtProofs(ctx, nonce, vcKeys);

        // Send Credential Request (with multiple proofs)
        //
        String credentialEndpoint = oauth.getEndpoints().getOid4vcCredential();
        dpopProof = wallet.generateSignedDPoPProof(credentialEndpoint, dpopKey, accessToken);

        CredentialResponse credResponse = wallet.credentialRequest(ctx, tokenType, accessToken)
                .credentialIdentifier(credentialIdentifier)
                .dpopProof(dpopProof)
                .proofs(jwtProofs)
                .send().getCredentialResponse();

        // Verify Credential Response
        //
        verifyCredentialResponse(ctx, jwtProofs, credResponse);
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

    /**
     * fapi2-security-profile-final-refresh-token
     *
     * This test obtains refresh tokens and checks that the refresh token is correctly bound to the client.
     */
    @Test
    public void testRefreshTokenHappyFlow() throws Exception {

        var ctx = new OID4VCTestContext(abcaClient, sdJwtTypeCredentialScope);
        ctx.putAttachment(CLIENT_ATTESTER_ATTACHMENT_KEY, attester);

        var pkce = PkceGenerator.s256();

        // Generate ABCA Headers
        //
        KeyWrapper abcaKey = wallet.getRSAKeyPair(ctx);
        String attestationJwt = wallet.buildClientAttestationJWT(ctx, abcaKey);
        String attestationPoPJwt = wallet.buildClientAttestationPoPJWT(ctx, abcaKey);

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

        // Send AccessToken Request
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

        // Inspect AccessToken
        //
        assertTrue(tokenResponse.isSuccess());
        String encodedAccessToken = tokenResponse.getAccessToken();
        assertNotNull(encodedAccessToken, "No access token");
        String tokenType = tokenResponse.getTokenType();
        assertEquals(TokenUtil.TOKEN_TYPE_DPOP, tokenType);

        AccessToken accessToken = new JWSInput(encodedAccessToken).readJsonContent(AccessToken.class);
        AccessToken.Confirmation accessCnf = accessToken.getConfirmation();
        assertNotNull(accessCnf, "No access confirmation");
        assertEquals(DPOP_CNF_TYPE, accessCnf.getCnfType());

        // Inspect RefreshToken
        //
        String encodedRefreshToken = tokenResponse.getRefreshToken();
        assertNotNull(encodedRefreshToken, "No refresh token");

        RefreshToken refreshToken = new JWSInput(encodedRefreshToken).readJsonContent(RefreshToken.class);
        AccessToken.Confirmation refreshCnf = refreshToken.getConfirmation();
        assertNotNull(refreshCnf, "No refresh confirmation");
        assertEquals(ABCA_CNF_TYPE, refreshCnf.getCnfType());

        JWK abcaJwk = JWKBuilder.create()
                .kid(abcaKey.getKid())
                .algorithm(abcaKey.getAlgorithm())
                .rsa(abcaKey.getPublicKey(), abcaKey.getUse());
        String abcaThumbprint = JWKSUtils.computeThumbprint(abcaJwk);
        String cnfThumbprint = refreshCnf.getKeyThumbprint();
        assertEquals(abcaThumbprint, cnfThumbprint, "Expected ABCA thumbprint in confirmation");

        // Exchange RefreshToken for new AccessToken
        //
        dpopProof = wallet.generateSignedDPoPProof(tokenEndpoint, ecKey, encodedRefreshToken);
        tokenResponse = oauth.refreshRequest(encodedRefreshToken)
                .header(OAUTH_CLIENT_ATTESTATION_HEADER, attestationJwt)
                .header(OAUTH_CLIENT_ATTESTATION_POP_HEADER, attestationPoPJwt)
                .dpopProof(dpopProof)
                .send();

        // Inspect AccessToken
        //
        assertTrue(tokenResponse.isSuccess());
        encodedAccessToken = tokenResponse.getAccessToken();
        assertNotNull(encodedAccessToken, "No access token");
        tokenType = tokenResponse.getTokenType();
        assertEquals(TokenUtil.TOKEN_TYPE_DPOP, tokenType);

        accessToken = new JWSInput(encodedAccessToken).readJsonContent(AccessToken.class);
        accessCnf = accessToken.getConfirmation();
        assertNotNull(accessCnf, "No access confirmation");
        assertEquals(DPOP_CNF_TYPE, accessCnf.getCnfType());

        // Send Nonce Request
        //
        String nonce = wallet.nonceRequest().send().getNonce();
        Proofs jwtProofs = wallet.generateJwtProofs(ctx, nonce, ecKey);

        // Send Credential Request
        // Note, we use the same EC key for DPoP and Holder identity
        //
        String credentialIdentifier = ctx.getAuthorizedCredentialIdentifier();
        assertNotNull(credentialIdentifier, "No authorized credential identifier");

        String credentialEndpoint = oauth.getEndpoints().getOid4vcCredential();
        dpopProof = wallet.generateSignedDPoPProof(credentialEndpoint, ecKey, encodedAccessToken);

        CredentialResponse credResponse = wallet.credentialRequest(ctx, tokenType, encodedAccessToken)
                .credentialIdentifier(credentialIdentifier)
                .dpopProof(dpopProof)
                .proofs(jwtProofs)
                .send().getCredentialResponse();

        // Verify Credential Response
        //
        verifyCredentialResponse(ctx, jwtProofs, credResponse);
    }

    /**
     * fapi2-security-profile-final-par-ensure-reused-request-uri-prior-to-auth-completion-succeeds
     *
     * This test checks that authorization servers that enforce one-time use of request_uri values do so at the point of authorization,
     * not at the point of visiting the authorization endpoint.
     */
    @Test
    public void testEnsureReusedRequestUriPriorToAuthCompletionSucceeds() throws Exception {

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

        // Open LoginForm and Cancel
        // This should access the request_uri for the first time
        //
        AuthorizationEndpointRequest authRequest = wallet.authorizationRequest()
                .requestUri(requestUri)
                .codeChallenge(pkce);
        assertTrue(authRequest.openLoginForm());

        // Send Authorization Request
        // This should access the request_uri for the second time
        //
        AuthorizationEndpointResponse authResponse = wallet.authorizationRequest()
                .requestUri(requestUri)
                .codeChallenge(pkce)
                .send(ctx.getHolder(), TEST_PASSWORD);
        assertTrue(authResponse.isSuccess());
        String authCode = authResponse.getCode();
        assertNotNull(authCode, "No auth code");
    }

    // Private ---------------------------------------------------------------------------------------------------------

    private void verifyCredentialResponse(OID4VCTestContext ctx, Proofs jwtProofs, CredentialResponse credResponse) throws Exception {

        CredentialScopeRepresentation credScope = ctx.getCredentialScope();
        String issuer = wallet.getIssuerMetadata(ctx).getCredentialIssuer();
        List<Credential> credentials = credResponse.getCredentials();
        assertNotNull(credentials, "No credentials");

        CredentialIssuer issuerMetadata = wallet.getIssuerMetadata(ctx);
        BatchCredentialIssuance bci = issuerMetadata.getBatchCredentialIssuance();

        List<String> allProofs = jwtProofs.getAllProofs();
        assertEquals(Math.min(allProofs.size(), bci.getBatchSize()), credentials.size(), "Number of proofs does not match number of credentials");

        List<String> expThumbprints = new ArrayList<>();
        for (String proof : allProofs) {
            JWK proofJwk = new JWSInput(proof).getHeader().getKey();
            expThumbprints.add(JWKSUtils.computeThumbprint(proofJwk));
        }

        for (int i = 0; i < credentials.size(); i++) {
            Credential credObj = credentials.get(i);
            assertNotNull(credObj, "No credential at index: " + i);

            SdJwtVP sdJwtVP = SdJwtVP.of(credObj.getCredential().toString());
            IssuerSignedJWT issuerSignedJWT = sdJwtVP.getIssuerSignedJWT();
            var cnfJwkNode = issuerSignedJWT.getCnfClaim()
                    .map(it -> it.get("jwk"))
                    .orElse(null);
            assertNotNull(cnfJwkNode, "Missing cnf.jwk claim at index: " + i);

            JWK cnfJwk = JsonSerialization.mapper.convertValue(cnfJwkNode, JWK.class);
            String wasThumbprint = JWKSUtils.computeThumbprint(cnfJwk);
            assertEquals(expThumbprints.get(i), wasThumbprint);

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
}
