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

package org.keycloak.crypto;

import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.util.Arrays;
import java.util.Map;

import org.keycloak.common.VerificationException;
import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.crypto.CryptoProvider;
import org.keycloak.jose.jwk.AKPPublicJWK;
import org.keycloak.jose.jwk.AKPSamples;
import org.keycloak.jose.jwk.JWKParser;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.util.JsonSerialization;
import org.keycloak.utils.AbstractUtilSessionTest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MLDSASignatureProviderTest extends AbstractUtilSessionTest {

    private static final byte[] PAYLOAD = "payload".getBytes(StandardCharsets.UTF_8);
    private static final Map<String, Integer> SIGNATURE_LENGTHS = Map.of(
            Algorithm.ML_DSA_44, 2420,
            Algorithm.ML_DSA_65, 3309,
            Algorithm.ML_DSA_87, 4627);

    @Test
    public void detectSupportedDefaultProvider() {
        for (String algorithm : SIGNATURE_LENGTHS.keySet()) {
            assertTrue(MLDSAUtils.isSignatureSupported(algorithm));
            assertTrue(MLDSAUtils.isKeyGenerationSupported(algorithm));
        }
    }

    @Test
    public void rejectProviderWithoutMLDSA() throws Exception {
        CryptoProvider original = CryptoIntegration.getProvider();
        CryptoProvider unsupported = (CryptoProvider) Proxy.newProxyInstance(
                CryptoProvider.class.getClassLoader(), new Class<?>[] { CryptoProvider.class }, (proxy, method, args) -> {
                    if ("getSignature".equals(method.getName())) {
                        throw new java.security.NoSuchAlgorithmException();
                    }
                    throw new UnsupportedOperationException(method.getName());
                });

        try {
            CryptoIntegration.setProvider(unsupported);
            assertFalse(MLDSAUtils.isSignatureSupported(Algorithm.ML_DSA_44));
            assertFalse(new MLDSA44SignatureProviderFactory().isSupported(null));
        } finally {
            CryptoIntegration.setProvider(original);
        }
    }

    @Test
    public void signAndVerifyAllParameterSets() throws Exception {
        for (Map.Entry<String, Integer> entry : SIGNATURE_LENGTHS.entrySet()) {
            KeyWrapper key = generateKey(entry.getKey());
            MLDSASignatureProvider provider = new MLDSASignatureProvider(session, entry.getKey());

            byte[] signature = provider.signer(key).sign(PAYLOAD);

            assertEquals(entry.getValue().intValue(), signature.length);
            assertTrue(provider.verifier(key).verify(PAYLOAD, signature));
        }
    }

    @Test
    public void verifyRfc9964Example() throws Exception {
        AKPPublicJWK jwk = JsonSerialization.readValue(AKPSamples.ML_DSA_44, AKPPublicJWK.class);
        JWSInput input = new JWSInput(new String(getClass().getResourceAsStream("ML_DSA_44.jws").readAllBytes(), StandardCharsets.UTF_8).trim());
        KeyWrapper key = new KeyWrapper();
        key.setKid(jwk.getKeyId());
        key.setUse(KeyUse.SIG);
        key.setType(KeyType.AKP);
        key.setAlgorithm(jwk.getAlgorithm());
        key.setPublicKey(JWKParser.create(jwk).toPublicKey());

        assertTrue(new MLDSASignatureProvider(session, Algorithm.ML_DSA_44).verifier(key)
                .verify(input.getEncodedSignatureInput().getBytes(StandardCharsets.UTF_8), input.getSignature()));
    }

    @Test
    public void rejectRsaAndOkpKeys() throws Exception {
        KeyWrapper key = generateKey(Algorithm.ML_DSA_44);
        MLDSASignatureProvider provider = new MLDSASignatureProvider(session, Algorithm.ML_DSA_44);

        key.setType(KeyType.RSA);
        assertThrows(SignatureException.class, () -> provider.signer(key));

        key.setType(KeyType.OKP);
        assertThrows(VerificationException.class, () -> provider.verifier(key));
    }

    @Test
    public void rejectCrossParameterKey() throws Exception {
        KeyWrapper key = generateKey(Algorithm.ML_DSA_65);
        MLDSASignatureProvider provider = new MLDSASignatureProvider(session, Algorithm.ML_DSA_44);

        assertThrows(SignatureException.class, () -> provider.signer(key));
        assertThrows(VerificationException.class, () -> provider.verifier(key));
    }

    @Test
    public void rejectRelabeledCrossParameterKey() throws Exception {
        KeyWrapper key = generateKey(Algorithm.ML_DSA_65);
        key.setAlgorithm(Algorithm.ML_DSA_44);
        MLDSASignatureProvider provider = new MLDSASignatureProvider(session, Algorithm.ML_DSA_44);

        assertThrows(SignatureException.class, () -> provider.signer(key));
        assertThrows(VerificationException.class, () -> provider.verifier(key));
    }

    @Test
    public void rejectMissingPrivateAndPublicKeys() throws Exception {
        KeyWrapper key = generateKey(Algorithm.ML_DSA_44);
        MLDSASignatureProvider provider = new MLDSASignatureProvider(session, Algorithm.ML_DSA_44);

        key.setPrivateKey(null);
        assertThrows(SignatureException.class, () -> provider.signer(key));

        key.setPublicKey(null);
        assertThrows(VerificationException.class, () -> provider.verifier(key));
    }

    @Test
    public void rejectAlteredPayload() throws Exception {
        KeyWrapper key = generateKey(Algorithm.ML_DSA_44);
        MLDSASignatureProvider provider = new MLDSASignatureProvider(session, Algorithm.ML_DSA_44);
        byte[] signature = provider.signer(key).sign(PAYLOAD);

        assertFalse(provider.verifier(key).verify("altered".getBytes(StandardCharsets.UTF_8), signature));
    }

    @Test
    public void rejectTruncatedSignature() throws Exception {
        KeyWrapper key = generateKey(Algorithm.ML_DSA_44);
        MLDSASignatureProvider provider = new MLDSASignatureProvider(session, Algorithm.ML_DSA_44);
        byte[] signature = provider.signer(key).sign(PAYLOAD);

        assertFalse(provider.verifier(key).verify(PAYLOAD, Arrays.copyOf(signature, signature.length - 1)));
    }

    @Test
    public void reportAkpPrivateClaim() {
        assertEquals(java.util.Set.of("priv"), new MLDSA44SignatureProviderFactory().getJwkPrivateKeyClaims());
        assertEquals(java.util.Set.of("priv"), new MLDSA65SignatureProviderFactory().getJwkPrivateKeyClaims());
        assertEquals(java.util.Set.of("priv"), new MLDSA87SignatureProviderFactory().getJwkPrivateKeyClaims());
    }

    private static KeyWrapper generateKey(String algorithm) throws Exception {
        KeyPair keyPair = CryptoIntegration.getProvider().getKeyPairGen(algorithm).generateKeyPair();
        KeyWrapper key = new KeyWrapper();
        key.setKid("kid");
        key.setUse(KeyUse.SIG);
        key.setType(KeyType.AKP);
        key.setAlgorithm(algorithm);
        key.setPublicKey(keyPair.getPublic());
        key.setPrivateKey(keyPair.getPrivate());
        return key;
    }
}
