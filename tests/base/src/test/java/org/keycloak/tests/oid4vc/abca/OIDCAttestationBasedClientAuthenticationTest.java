/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.tests.oid4vc.abca;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;

import org.keycloak.OAuthErrorException;
import org.keycloak.TokenVerifier;
import org.keycloak.authentication.authenticators.client.AttestationBasedClientAuthenticator.ClientAttestationJwt;
import org.keycloak.authentication.authenticators.client.AttestationBasedClientAuthenticator.ClientAttestationPoPJwt;
import org.keycloak.broker.trust.DefaultTrustIdentityProviderConfig;
import org.keycloak.broker.trust.DefaultTrustIdentityProviderFactory;
import org.keycloak.common.VerificationException;
import org.keycloak.common.util.PemUtils;
import org.keycloak.crypto.AsymmetricSignatureSignerContext;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.jose.jwk.JSONWebKeySet;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jwk.JWKBuilder;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oid4vc.model.CredentialResponse;
import org.keycloak.protocol.oid4vc.model.Proofs;
import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.annotations.TestSetup;
import org.keycloak.tests.oid4vc.OID4VCIssuerTestBase;
import org.keycloak.tests.oid4vc.OID4VCTestContext;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;
import org.keycloak.util.JsonSerialization;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.keycloak.authentication.authenticators.client.AttestationBasedClientAuthenticator.OAUTH_CLIENT_ATTESTATION_HEADER;
import static org.keycloak.authentication.authenticators.client.AttestationBasedClientAuthenticator.OAUTH_CLIENT_ATTESTATION_JWT_TYPE;
import static org.keycloak.authentication.authenticators.client.AttestationBasedClientAuthenticator.OAUTH_CLIENT_ATTESTATION_POP_HEADER;
import static org.keycloak.protocol.oidc.OIDCLoginProtocol.ATTEST_JWT_CLIENT_AUTH;
import static org.keycloak.tests.oid4vc.OID4VCProofTestUtils.createCaCertificate;
import static org.keycloak.tests.oid4vc.OID4VCProofTestUtils.createEndEntityCertificate;
import static org.keycloak.tests.oid4vc.OID4VCProofTestUtils.createRsaKeyPair;
import static org.keycloak.tests.oid4vc.OID4VCTestContext.CLIENT_ATTESTER_ATTACHMENT_KEY;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


@KeycloakIntegrationTest(config = OID4VCIssuerTestBase.VCTestServerWithABCAEnabled.class)
public class OIDCAttestationBasedClientAuthenticationTest extends OID4VCIssuerTestBase {

    private static OIDCClientAttester attester;
    private static String attesterJwks;

    @TestSetup
    public void configure() throws Exception {
        var kw = createRsaKeyPair("openid-abca-attester-key");
        JWK jwk = JWKBuilder.create()
                .kid(kw.getKid())
                .algorithm(kw.getAlgorithm())
                .rsa(kw.getPublicKey());
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
        oauth.client(abcaClient.getClientId(), null);
    }

    @Test
    public void testTokenEndpointAuthMethods() {
        OIDCConfigurationRepresentation oidcConfiguration = oauth.doWellKnownRequest();
        List<String> tokenAuthMethodsSupported = oidcConfiguration.getTokenEndpointAuthMethodsSupported();
        assertTrue(tokenAuthMethodsSupported.contains(ATTEST_JWT_CLIENT_AUTH), "Should contain: " + ATTEST_JWT_CLIENT_AUTH);
    }

    @Test
    public void testClientAttestationJWT() throws VerificationException {

        var ctx = new OID4VCTestContext(abcaClient, sdJwtTypeCredentialScope);
        ctx.putAttachment(CLIENT_ATTESTER_ATTACHMENT_KEY, attester);

        // Call the Attester to get the Client Attestation JWT
        //
        var walletKey = wallet.getRSAKeyPair(ctx);
        String attestationJwt = wallet.buildClientAttestationJWT(ctx, walletKey);

        // Verify the Client Attestation JWT
        //
        TokenVerifier.create(attestationJwt, ClientAttestationJwt.class)
                .publicKey(attester.getPublicKey())
                .withChecks(TokenVerifier.IS_ACTIVE)
                .verify().getToken();
    }

    @Test
    public void testClientAttestationPoPJWT() throws VerificationException {

        var ctx = new OID4VCTestContext(abcaClient, sdJwtTypeCredentialScope);
        ctx.putAttachment(CLIENT_ATTESTER_ATTACHMENT_KEY, attester);

        // Build Client Attestation PoP JWT
        //
        var walletKey = wallet.getRSAKeyPair(ctx);
        String attestationPoPJwt = wallet.buildClientAttestationPoPJWT(ctx, walletKey);

        // Verify the Client Attestation PoP JWT
        //
        TokenVerifier.create(attestationPoPJwt, ClientAttestationPoPJwt.class)
                .publicKey((PublicKey) walletKey.getPublicKey())
                .withChecks(TokenVerifier.IS_ACTIVE)
                .verify().getToken();
    }

    @Test
    public void testClientAttestationHappyFlow() {

        var ctx = new OID4VCTestContext(abcaClient, sdJwtTypeCredentialScope);
        ctx.putAttachment(CLIENT_ATTESTER_ATTACHMENT_KEY, attester);

        var kw = wallet.getRSAKeyPair(ctx);
        String attestationJwt = wallet.buildClientAttestationJWT(ctx, kw);
        String attestationPoPJwt = wallet.buildClientAttestationPoPJWT(ctx, kw);

        KeyWrapper ecKey = wallet.getECKeyPair(ctx);

        // Send Authorization Request
        //
        AuthorizationEndpointResponse authResponse = wallet.authorizationRequest()
                .scope(ctx.getScope())
                .send(ctx.getHolder(), TEST_PASSWORD);

        String errorDescription = authResponse.getErrorDescription();
        assertNull(errorDescription, "Authorization error: " + errorDescription);

        String authCode = authResponse.getCode();
        assertNotNull(authCode, "No auth code");

        // Send Token Request
        //
        String tokenEndpoint = oauth.getEndpoints().getToken();
        AccessTokenResponse tokenResponse = wallet.accessTokenRequest(ctx, authCode)
                .header(OAUTH_CLIENT_ATTESTATION_HEADER, attestationJwt)
                .header(OAUTH_CLIENT_ATTESTATION_POP_HEADER, attestationPoPJwt)
                .dpopProof(wallet.generateSignedDPoPProof(tokenEndpoint, ecKey, null))
                .send();

        errorDescription = tokenResponse.getErrorDescription();
        assertNull(errorDescription, "Token request error: " + errorDescription);

        String tokenType = tokenResponse.getTokenType();
        assertNotNull(tokenType, "No token type");

        String accessToken = wallet.validateHolderAccessToken(ctx, tokenResponse);
        assertNotNull(accessToken, "No access token");

        String credIdentifier = ctx.getAuthorizedCredentialIdentifier();
        assertNotNull(credIdentifier, "No credential identifier");

        // Send Nonce Request
        //
        String nonce = wallet.nonceRequest().send().getNonce();
        Proofs jwtProofs = wallet.generateJwtProofs(ctx, nonce, ecKey);

        // Send Credential Request
        //
        String credentialEndpoint = oauth.getEndpoints().getOid4vcCredential();
        CredentialResponse credResponse = wallet.credentialRequest(ctx, tokenType, accessToken)
                .credentialIdentifier(credIdentifier)
                .dpopProof(wallet.generateSignedDPoPProof(credentialEndpoint, ecKey, accessToken))
                .proofs(jwtProofs)
                .send().getCredentialResponse();

        assertFalse(credResponse.getCredentials().isEmpty(), "No credential");
    }

    @Test
    public void testClientAttestationHappyFlowWithX5c() throws Exception {
        runX5cAttestationTest("Test CA", "Test CA", true);
    }

    @Test
    public void testClientAttestationX5cUntrustedChainIsRejected() throws Exception {
        runX5cAttestationTest("Trusted CA", "Untrusted CA", false);
    }

    private void runX5cAttestationTest(String trustedCaName, String signingCaName, boolean expectSuccess) throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);

        KeyPair trustedCaKeyPair = kpg.generateKeyPair();
        X509Certificate trustedCaCert = createCaCertificate(trustedCaKeyPair, trustedCaName);

        KeyPair signingCaKeyPair = trustedCaName.equals(signingCaName)
                ? trustedCaKeyPair
                : kpg.generateKeyPair();
        X509Certificate signingCaCert = trustedCaName.equals(signingCaName)
                ? trustedCaCert
                : createCaCertificate(signingCaKeyPair, signingCaName);

        var attesterKw = createRsaKeyPair("openid-abca-attester-x5c-" + signingCaName.replace(" ", "-"));
        KeyPair attesterKeyPair = new KeyPair((PublicKey) attesterKw.getPublicKey(), (PrivateKey) attesterKw.getPrivateKey());
        X509Certificate attesterCert = createEndEntityCertificate(attesterKeyPair, signingCaKeyPair, signingCaCert, "ABCA Test Attester");
        attesterKw.setCertificate(attesterCert);

        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            configureTrustIdentityProvider(realm, OAUTH_CLIENT_ATTESTATION_DEFAULT_TRUST_IDP_ALIAS,
                    DefaultTrustIdentityProviderFactory.PROVIDER_ID,
                    Map.of(DefaultTrustIdentityProviderConfig.USE_X509, "true",
                            DefaultTrustIdentityProviderConfig.TRUSTED_CERTIFICATES,
                            PemUtils.encodeCertificate(trustedCaCert)));
        });

        var ctx = new OID4VCTestContext(abcaClient, sdJwtTypeCredentialScope);
        var walletKey = wallet.getRSAKeyPair(ctx);
        JWK walletJwk = JWKBuilder.create()
                .kid(walletKey.getKid())
                .algorithm(walletKey.getAlgorithm())
                .rsa(walletKey.getPublicKey());
        ClientAttestationJwt body = new ClientAttestationJwt()
                .issuer("https://example.com/mock-attester")
                .subject(abcaClient.getClientId())
                .confirmation(walletJwk)
                .issuedNowWithTTL(300);

        String attestationJwt = new JWSBuilder()
                .type(OAUTH_CLIENT_ATTESTATION_JWT_TYPE)
                .x5c(List.of(attesterCert))
                .jsonContent(body)
                .sign(new AsymmetricSignatureSignerContext(attesterKw));

        String attestationPoPJwt = wallet.buildClientAttestationPoPJWT(ctx, walletKey);
        KeyWrapper ecKey = wallet.getECKeyPair(ctx);

        AuthorizationEndpointResponse authResponse = wallet.authorizationRequest()
                .scope(ctx.getScope())
                .send(ctx.getHolder(), TEST_PASSWORD);
        String authCode = authResponse.getCode();
        assertNotNull(authCode, "No auth code");

        String tokenEndpoint = oauth.getEndpoints().getToken();
        AccessTokenResponse tokenResponse = wallet.accessTokenRequest(ctx, authCode)
                .header(OAUTH_CLIENT_ATTESTATION_HEADER, attestationJwt)
                .header(OAUTH_CLIENT_ATTESTATION_POP_HEADER, attestationPoPJwt)
                .dpopProof(wallet.generateSignedDPoPProof(tokenEndpoint, ecKey, null))
                .send();

        if (expectSuccess) {
            assertNull(tokenResponse.getErrorDescription(), "Token request error: " + tokenResponse.getErrorDescription());
            assertNotNull(tokenResponse.getAccessToken(), "No access token");
        } else {
            assertEquals(OAuthErrorException.INVALID_CLIENT_ATTESTATION, tokenResponse.getError(),
                    "Expected the untrusted x5c chain to be rejected with invalid_client_attestation");
            assertNull(tokenResponse.getAccessToken(), "Untrusted x5c chain must not yield an access token");
        }
    }
}
