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

import java.security.Key;
import java.security.PublicKey;
import java.util.List;
import java.util.Map;

import org.keycloak.TokenVerifier;
import org.keycloak.authentication.authenticators.client.AttestationBasedClientAuthenticator.ABCAConfig;
import org.keycloak.authentication.authenticators.client.AttestationBasedClientAuthenticator.ClientAttestationJwt;
import org.keycloak.common.VerificationException;
import org.keycloak.crypto.AsymmetricSignatureSignerContext;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jwk.JWKBuilder;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oid4vc.issuance.keybinding.AttestationValidatorUtil;
import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.annotations.TestSetup;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.tests.oid4vc.OID4VCIssuerTestBase;
import org.keycloak.tests.oid4vc.OID4VCTestContext;
import org.keycloak.util.JsonSerialization;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.keycloak.authentication.authenticators.client.AttestationBasedClientAuthenticator.OAUTH_CLIENT_ATTESTATION_CONFIG_ATTESTER_JWKS;
import static org.keycloak.protocol.oidc.OIDCLoginProtocol.ATTEST_JWT_CLIENT_AUTH;
import static org.keycloak.tests.oid4vc.OID4VCProofTestUtils.createRsaKeyPair;

import static org.junit.jupiter.api.Assertions.assertTrue;


@KeycloakIntegrationTest(config = OID4VCIssuerTestBase.VCTestServerWithABCAEnabled.class)
public class OIDCAttestationBasedClientAuthenticationTest extends OID4VCIssuerTestBase {

    @InjectRunOnServer
    private RunOnServerClient runOnServer;

    private static OIDCMockClientAttester attester;
    private static ABCAConfig abcaConfig;

    @TestSetup
    public void configure() {
        var kw = createRsaKeyPair("openid-abca-attester-key");
        JWK jwk = JWKBuilder.create().rsa(kw.getPublicKey(), kw.getCertificate());
        abcaConfig = new ABCAConfig().setKeys(List.of(jwk));
        attester = new OIDCMockClientAttester(kw);
    }

    @BeforeEach
    void beforeEach() {
        String abcaConfigValue = JsonSerialization.valueAsString(abcaConfig);
        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            AuthenticatorConfigModel abcaConfig = new AuthenticatorConfigModel();
            abcaConfig.setAlias(OAUTH_CLIENT_ATTESTATION_CONFIG_ATTESTER_JWKS);
            abcaConfig.setConfig(Map.of(OAUTH_CLIENT_ATTESTATION_CONFIG_ATTESTER_JWKS, abcaConfigValue));
            realm.addAuthenticatorConfig(abcaConfig);
        });
    }

    @Test
    public void testTokenEndpointAuthMethods() {
        OIDCConfigurationRepresentation oidcConfiguration = oauth.doWellKnownRequest();
        List<String> tokenAuthMethodsSupported = oidcConfiguration.getTokenEndpointAuthMethodsSupported();
        assertTrue(tokenAuthMethodsSupported.contains(ATTEST_JWT_CLIENT_AUTH), "Should contain: " + ATTEST_JWT_CLIENT_AUTH);
    }

    @Test
    public void testClientAttestationJWT() throws VerificationException {
        var ctx = new OID4VCTestContext(client, sdJwtTypeCredentialScope);

        // Call the Attester to get the Client Attestation JWT
        //
        var kw = wallet.getRSAKeyPair(ctx, "holderKey");
        String attestationJwt = attester.attestWalletKey(client.getClientId(), kw.getPublicKey());

        // Verification and Processing
        //
        TokenVerifier.create(attestationJwt, ClientAttestationJwt.class)
                .publicKey(attester.getPublicKey())
                .withChecks(TokenVerifier.IS_ACTIVE)
                .verify().getToken();
    }

    static class OIDCMockClientAttester {

        private final String issuer;
        private final KeyWrapper attesterKey;

        public OIDCMockClientAttester(KeyWrapper attesterKey) {
            this.issuer = "https://example.com/mock-attester";
            this.attesterKey = attesterKey;
        }

        public PublicKey getPublicKey() {
            return (PublicKey) attesterKey.getPublicKey();
        }

        public String attestWalletKey(String clientId, Key pubKey) {
            JWK jwk = JWKBuilder.create().rsa(pubKey);
            var body = new ClientAttestationJwt()
                    .issuedFor(issuer)
                    .subject(clientId)
                    .confirmation(jwk)
                    .issuedNowWithTTL(300); // 5min
            return new JWSBuilder()
                    .type(AttestationValidatorUtil.ATTESTATION_JWT_TYP)
                    .kid(attesterKey.getKid())
                    .jsonContent(body)
                    .sign(new AsymmetricSignatureSignerContext(attesterKey));
        }
    }
}
