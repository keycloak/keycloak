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

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.ECGenParameterSpec;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.rule.CryptoInitRule;

import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public abstract class ECDSASignatureVerifierContextTest {

    @ClassRule
    public static CryptoInitRule cryptoInitRule = new CryptoInitRule();

    private static final Map<String, String> CURVE_BY_ALGORITHM = new LinkedHashMap<>();

    static {
        CURVE_BY_ALGORITHM.put(Algorithm.ES256, "secp256r1");
        CURVE_BY_ALGORITHM.put(Algorithm.ES384, "secp384r1");
        CURVE_BY_ALGORITHM.put(Algorithm.ES512, "secp521r1");
    }

    @Test
    public void verifiesExactLengthSignature() throws Exception {
        for (Map.Entry<String, String> entry : CURVE_BY_ALGORITHM.entrySet()) {
            String algorithm = entry.getKey();
            KeyWrapper key = generateKey(algorithm, entry.getValue());
            byte[] data = "sample-signature-input".getBytes(StandardCharsets.UTF_8);

            byte[] signature = new ECDSASignatureSignerContext(key).sign(data);

            assertEquals(algorithm, ECDSAAlgorithm.getSignatureLength(algorithm), signature.length);
            assertTrue(algorithm, new ECDSASignatureVerifierContext(key).verify(data, signature));
        }
    }

    @Test
    public void rejectsSignatureWithTrailingBytes() throws Exception {
        for (Map.Entry<String, String> entry : CURVE_BY_ALGORITHM.entrySet()) {
            String algorithm = entry.getKey();
            KeyWrapper key = generateKey(algorithm, entry.getValue());
            byte[] data = "sample-signature-input".getBytes(StandardCharsets.UTF_8);
            byte[] signature = new ECDSASignatureSignerContext(key).sign(data);

            // RFC 7518 section 3.4 fixes the JWS signature to exactly 2 * curve-size octets. A valid
            // R || S with data appended must not verify, otherwise accepted signatures are malleable.
            byte[] oneTrailingByte = Arrays.copyOf(signature, signature.length + 1);
            assertFalse(algorithm, new ECDSASignatureVerifierContext(key).verify(data, oneTrailingByte));

            byte[] trailingJunk = Arrays.copyOf(signature, signature.length + 17);
            Arrays.fill(trailingJunk, signature.length, trailingJunk.length, (byte) 0x5a);
            assertFalse(algorithm, new ECDSASignatureVerifierContext(key).verify(data, trailingJunk));
        }
    }

    @Test
    public void rejectsTruncatedSignature() throws Exception {
        for (Map.Entry<String, String> entry : CURVE_BY_ALGORITHM.entrySet()) {
            String algorithm = entry.getKey();
            KeyWrapper key = generateKey(algorithm, entry.getValue());
            byte[] data = "sample-signature-input".getBytes(StandardCharsets.UTF_8);
            byte[] signature = new ECDSASignatureSignerContext(key).sign(data);

            assertFalse(algorithm, new ECDSASignatureVerifierContext(key).verify(data, Arrays.copyOf(signature, signature.length - 1)));
        }
    }

    private KeyWrapper generateKey(String algorithm, String curve) throws Exception {
        KeyPairGenerator kpg = CryptoIntegration.getProvider().getKeyPairGen(KeyType.EC);
        kpg.initialize(new ECGenParameterSpec(curve));
        KeyPair keyPair = kpg.generateKeyPair();

        KeyWrapper key = new KeyWrapper();
        key.setKid("test-kid");
        key.setAlgorithm(algorithm);
        key.setType(KeyType.EC);
        key.setUse(KeyUse.SIG);
        key.setPrivateKey(keyPair.getPrivate());
        key.setPublicKey(keyPair.getPublic());
        return key;
    }
}
