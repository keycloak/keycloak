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

import java.security.PublicKey;
import java.util.List;
import java.util.Map;

import org.keycloak.TokenVerifier;
import org.keycloak.authentication.authenticators.client.AttestationBasedClientAuthenticator;
import org.keycloak.authentication.authenticators.client.AttestationBasedClientAuthenticator.ABCAConfig;
import org.keycloak.authentication.authenticators.client.AttestationBasedClientAuthenticator.ClientAttestationJwt;
import org.keycloak.authentication.authenticators.client.AttestationBasedClientAuthenticator.ClientAttestationPoPJwt;
import org.keycloak.common.VerificationException;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jwk.JWKBuilder;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oid4vc.model.CredentialResponse;
import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.annotations.TestSetup;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.tests.oid4vc.OID4VCIssuerTestBase;
import org.keycloak.tests.oid4vc.OID4VCTestContext;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;
import org.keycloak.testsuite.util.oauth.PkceGenerator;
import org.keycloak.util.JsonSerialization;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.keycloak.authentication.authenticators.client.AttestationBasedClientAuthenticator.OAUTH_CLIENT_ATTESTATION_CONFIG_ATTESTER_JWKS;
import static org.keycloak.authentication.authenticators.client.AttestationBasedClientAuthenticator.OAUTH_CLIENT_ATTESTATION_HEADER;
import static org.keycloak.authentication.authenticators.client.AttestationBasedClientAuthenticator.OAUTH_CLIENT_ATTESTATION_POP_HEADER;
import static org.keycloak.protocol.oidc.OIDCLoginProtocol.ATTEST_JWT_CLIENT_AUTH;
import static org.keycloak.tests.oid4vc.OID4VCProofTestUtils.createRsaKeyPair;
import static org.keycloak.tests.oid4vc.OID4VCTestContext.CLIENT_ATTESTER_ATTACHMENT_KEY;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


@KeycloakIntegrationTest(config = OID4VCIssuerTestBase.VCTestServerWithABCAEnabled.class)
public class OIDCAttestationBasedClientAuthenticationTest extends OID4VCIssuerTestBase {

    @InjectRunOnServer
    private RunOnServerClient runOnServer;

    private static OIDCClientAttester attester;
    private static ABCAConfig abcaConfig;

    OID4VCTestContext ctx;

    @TestSetup
    public void configure() {
        var kw = createRsaKeyPair("openid-abca-attester-key");
        JWK jwk = JWKBuilder.create()
                .kid(kw.getKid())
                .algorithm(kw.getAlgorithm())
                .rsa(kw.getPublicKey(), kw.getCertificate());
        abcaConfig = new ABCAConfig().setKeys(List.of(jwk));
        attester = new OIDCMockClientAttester(kw);
    }

    @BeforeEach
    void beforeEach() {
        String abcaConfigValue = JsonSerialization.valueAsString(abcaConfig);
        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            AuthenticatorConfigModel configModel = new AuthenticatorConfigModel();
            configModel.setAlias(AttestationBasedClientAuthenticator.PROVIDER_ID);
            configModel.setConfig(Map.of(OAUTH_CLIENT_ATTESTATION_CONFIG_ATTESTER_JWKS, abcaConfigValue));
            realm.addAuthenticatorConfig(configModel);
        });
        oauth.client(pubClient.getClientId());
        ctx = new OID4VCTestContext(pubClient, sdJwtTypeCredentialScope);
        ctx.putAttachment(CLIENT_ATTESTER_ATTACHMENT_KEY, attester);
    }

    @Test
    public void testTokenEndpointAuthMethods() {
        OIDCConfigurationRepresentation oidcConfiguration = oauth.doWellKnownRequest();
        List<String> tokenAuthMethodsSupported = oidcConfiguration.getTokenEndpointAuthMethodsSupported();
        assertTrue(tokenAuthMethodsSupported.contains(ATTEST_JWT_CLIENT_AUTH), "Should contain: " + ATTEST_JWT_CLIENT_AUTH);
    }

    @Test
    public void testClientAttestationJWT() throws VerificationException {

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
    public void testClientAttestationHappyFlow_PublicClient() {

        var kw = wallet.getRSAKeyPair(ctx);
        String attestationJwt = wallet.buildClientAttestationJWT(ctx, kw);
        String attestationPoPJwt = wallet.buildClientAttestationPoPJWT(ctx, kw);

        PkceGenerator pkce = PkceGenerator.s256();

        AuthorizationEndpointResponse authResponse = wallet.authorizationRequest()
                .scope(ctx.getScope())
                .codeChallenge(pkce)
                .send(ctx.getHolder(), TEST_PASSWORD);
        String authCode = authResponse.getCode();
        assertNotNull(authCode, "No auth code");

        AccessTokenResponse tokenResponse = wallet.accessTokenRequest(ctx, authCode)
                .header(OAUTH_CLIENT_ATTESTATION_HEADER, attestationJwt)
                .header(OAUTH_CLIENT_ATTESTATION_POP_HEADER, attestationPoPJwt)
                .codeVerifier(pkce)
                .send();
        String accessToken = wallet.validateHolderAccessToken(ctx, tokenResponse);
        assertNotNull(accessToken, "No accessToken");

        String credIdentifier = ctx.getAuthorizedCredentialIdentifier();
        CredentialResponse credResponse = wallet.credentialRequest(ctx, accessToken)
                .credentialIdentifier(credIdentifier)
                .proofs(wallet.generateJwtProof(ctx))
                .send().getCredentialResponse();

        assertFalse(credResponse.getCredentials().isEmpty(), "No credential");
    }

    @Test
    public void testClientAttestationHappyFlow_ConfidentialClient() {

        ctx = new OID4VCTestContext(client, sdJwtTypeCredentialScope);
        ctx.putAttachment(CLIENT_ATTESTER_ATTACHMENT_KEY, attester);
        oauth.client(ctx.getClient().getClientId(), ctx.getClient().getSecret());

        var kw = wallet.getRSAKeyPair(ctx);
        String attestationJwt = wallet.buildClientAttestationJWT(ctx, kw);
        String attestationPoPJwt = wallet.buildClientAttestationPoPJWT(ctx, kw);

        AuthorizationEndpointResponse authResponse = wallet.authorizationRequest()
                .scope(ctx.getScope())
                .send(ctx.getHolder(), TEST_PASSWORD);
        String authCode = authResponse.getCode();
        assertNotNull(authCode, "No auth code");

        AccessTokenResponse tokenResponse = wallet.accessTokenRequest(ctx, authCode)
                .header(OAUTH_CLIENT_ATTESTATION_HEADER, attestationJwt)
                .header(OAUTH_CLIENT_ATTESTATION_POP_HEADER, attestationPoPJwt)
                .send();
        String accessToken = wallet.validateHolderAccessToken(ctx, tokenResponse);
        assertNotNull(accessToken, "No accessToken");

        String credIdentifier = ctx.getAuthorizedCredentialIdentifier();
        CredentialResponse credResponse = wallet.credentialRequest(ctx, accessToken)
                .credentialIdentifier(credIdentifier)
                .proofs(wallet.generateJwtProof(ctx))
                .send().getCredentialResponse();

        assertFalse(credResponse.getCredentials().isEmpty(), "No credential");
    }
}
