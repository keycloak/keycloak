/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.protocol.oid4vc.presentation.verification;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.ECGenParameterSpec;
import java.util.List;
import java.util.UUID;

import org.keycloak.OID4VCConstants;
import org.keycloak.VCFormat;
import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.crypto.CryptoProvider;
import org.keycloak.common.util.Time;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.jose.jwk.JWKBuilder;
import org.keycloak.sdjwt.IssuerSignedJWT;
import org.keycloak.sdjwt.SdJwt;
import org.keycloak.sdjwt.consumer.TrustedSdJwtIssuer;
import org.keycloak.sdjwt.vp.KeyBindingJWT;
import org.keycloak.util.JsonSerialization;
import org.keycloak.util.KeyWrapperUtil;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class SdJwtCredentialVerifierTest {

    private static final String ISSUER = "https://issuer.example.org";
    private static final String AUDIENCE = "https://verifier.example.org";
    private static final String NONCE = "1234567890";
    private static final String SUBJECT = "alice";
    private static final String CREDENTIAL_TYPE = "urn:keycloak:oid4vp:credential";

    @BeforeClass
    public static void beforeClass() {
        CryptoIntegration.init(CryptoProvider.class.getClassLoader());
    }

    @Test
    public void testSupportsSdJwtCredential() {
        SdJwtCredentialVerifier verifier = new SdJwtCredentialVerifier(null, List.of());

        assertTrue(verifier.supports("issuer.jwt.signature~holder.jwt.signature"));
        assertEquals(VCFormat.SD_JWT_VC, verifier.getSupportedFormat());
    }

    @Test
    public void testVerifySdJwtCredential() throws Exception {
        KeyWrapper issuerKey = createEcKey("issuer-key");
        KeyWrapper holderKey = createEcKey("holder-key");
        SdJwtCredentialVerifier verifier = createVerifier(issuerKey);

        CredentialVerificationResult result = verifier.verify(new CredentialVerificationRequest(
                createCredential(issuerKey, holderKey, AUDIENCE, NONCE),
                AUDIENCE,
                NONCE));

        assertEquals(VCFormat.SD_JWT_VC, result.getFormat());
        assertEquals(ISSUER, result.getIssuer());
        assertEquals(CREDENTIAL_TYPE, result.getCredentialType());
        assertEquals(SUBJECT, result.getClaims().get("sub"));
        assertEquals("Alice", result.getClaims().get("given_name"));
        assertEquals("Doe", result.getClaims().get("family_name"));
        assertEquals("alice@example.org", result.getClaims().get("email"));
    }

    @Test
    public void testVerifySdJwtCredentialRejectsWrongNonce() throws Exception {
        KeyWrapper issuerKey = createEcKey("issuer-key");
        KeyWrapper holderKey = createEcKey("holder-key");
        SdJwtCredentialVerifier verifier = createVerifier(issuerKey);
        String credential = createCredential(issuerKey, holderKey, AUDIENCE, NONCE);

        assertThrows(CredentialVerificationException.class, () -> verifier.verify(new CredentialVerificationRequest(
                credential,
                AUDIENCE,
                "wrong-nonce")));
    }

    private SdJwtCredentialVerifier createVerifier(KeyWrapper issuerKey) {
        TrustedSdJwtIssuer trustedIssuer = issuerSignedJWT -> List.of(KeyWrapperUtil.createSignatureVerifierContext(issuerKey));
        return new SdJwtCredentialVerifier(null, List.of(trustedIssuer));
    }

    private String createCredential(KeyWrapper issuerKey, KeyWrapper holderKey, String audience, String nonce) {
        int now = Time.currentTime();

        ObjectNode claims = JsonSerialization.mapper.createObjectNode();
        claims.put(OID4VCConstants.CLAIM_NAME_ISSUER, ISSUER);
        claims.put("sub", SUBJECT);
        claims.put("given_name", "Alice");
        claims.put("family_name", "Doe");
        claims.put("email", "alice@example.org");
        claims.put("vct", CREDENTIAL_TYPE);

        IssuerSignedJWT issuerSignedJWT = IssuerSignedJWT.builder()
                .withClaims(claims)
                .withIat(now - 600)
                .withNbf(now - 600)
                .withExp(now + 600)
                .withKeyBindingKey(JWKBuilder.create().ec(holderKey.getPublicKey()))
                .withKid(issuerKey.getKid())
                .build();

        KeyBindingJWT keyBindingJWT = KeyBindingJWT.builder()
                .withIat(now)
                .withAudience(audience)
                .withNonce(nonce)
                .build();

        return SdJwt.builder()
                .withIssuerSignedJwt(issuerSignedJWT)
                .withKeybindingJwt(keyBindingJWT)
                .withIssuerSigningContext(KeyWrapperUtil.createSignatureSignerContext(issuerKey))
                .withKeyBindingSigningContext(KeyWrapperUtil.createSignatureSignerContext(holderKey))
                .withUseDefaultDecoys(false)
                .build()
                .toSdJwtString();
    }

    private KeyWrapper createEcKey(String prefix) throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
        keyPairGenerator.initialize(new ECGenParameterSpec("secp256r1"));
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        KeyWrapper key = new KeyWrapper();
        key.setKid(prefix + "-" + UUID.randomUUID());
        key.setUse(KeyUse.SIG);
        key.setAlgorithm("ES256");
        key.setType("EC");
        key.setCurve("P-256");
        key.setPublicKey(keyPair.getPublic());
        key.setPrivateKey(keyPair.getPrivate());
        return key;
    }
}
