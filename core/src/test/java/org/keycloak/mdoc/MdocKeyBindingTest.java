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
package org.keycloak.mdoc;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.keycloak.common.util.KeyUtils;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.KeyUse;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jwk.JWKBuilder;
import org.keycloak.rule.CryptoInitRule;

import com.webauthn4j.data.attestation.authenticator.COSEKey;
import com.webauthn4j.data.attestation.authenticator.Curve;
import com.webauthn4j.data.attestation.authenticator.EC2COSEKey;
import com.webauthn4j.data.attestation.authenticator.EdDSACOSEKey;
import com.webauthn4j.data.attestation.authenticator.RSACOSEKey;
import com.webauthn4j.data.attestation.statement.COSEAlgorithmIdentifier;
import com.webauthn4j.data.attestation.statement.COSEKeyType;
import org.junit.ClassRule;
import org.junit.Test;

import static org.keycloak.common.crypto.CryptoConstants.EC_KEY_SECP256R1;
import static org.keycloak.common.crypto.CryptoConstants.EC_KEY_SECP384R1;
import static org.keycloak.common.crypto.CryptoConstants.EC_KEY_SECP521R1;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Test of proof key conversion for mDoc holder binding.
 */
public abstract class MdocKeyBindingTest {

    private static final String KEY_ID = "proof-key";

    @ClassRule
    public static CryptoInitRule cryptoInitRule = new CryptoInitRule();

    @Test
    public void testEdDSAKeyBindingWithEd25519() {
        testKeyBinding(() -> KeyUtils.generateEddsaKeyPair(Algorithm.Ed25519),
                keyPair -> JWKBuilder.create().kid(KEY_ID).algorithm(Algorithm.EdDSA)
                        .okp(keyPair.getPublic(), KeyUse.SIG),
                coseKey -> assertEdDSAKey(coseKey, Curve.ED25519));
    }

    @Test
    public void testEdDSAKeyBindingWithEd448() {
        MdocException exception = assertThrows(MdocException.class,
                () -> testKeyBinding(() -> KeyUtils.generateEddsaKeyPair(Algorithm.Ed448),
                        keyPair -> JWKBuilder.create().kid(KEY_ID).algorithm(Algorithm.EdDSA)
                                .okp(keyPair.getPublic(), KeyUse.SIG),
                        coseKey -> {}));
        assertEquals("Unsupported OKP proof key curve for mDoc COSE_Key: Ed448", exception.getMessage());
    }

    @Test
    public void testEc256KeyBinding() {
        testKeyBinding(() -> KeyUtils.generateEcKeyPair(EC_KEY_SECP256R1),
                keyPair -> JWKBuilder.create().kid(KEY_ID).algorithm(Algorithm.ES256)
                        .ec(keyPair.getPublic(), KeyUse.SIG),
                coseKey -> assertEcKey(coseKey, Curve.SECP256R1, COSEAlgorithmIdentifier.ES256));
    }

    @Test
    public void testEc384KeyBinding() {
        testKeyBinding(() -> KeyUtils.generateEcKeyPair(EC_KEY_SECP384R1),
                keyPair -> JWKBuilder.create().kid(KEY_ID).algorithm(Algorithm.ES384)
                        .ec(keyPair.getPublic(), KeyUse.SIG),
                coseKey -> assertEcKey(coseKey, Curve.SECP384R1, COSEAlgorithmIdentifier.ES384));
    }

    @Test
    public void testEc521KeyBinding() {
        testKeyBinding(() -> KeyUtils.generateEcKeyPair(EC_KEY_SECP521R1),
                keyPair -> JWKBuilder.create().kid(KEY_ID).algorithm(Algorithm.ES512)
                        .ec(keyPair.getPublic(), KeyUse.SIG),
                coseKey -> assertEcKey(coseKey, Curve.SECP521R1, COSEAlgorithmIdentifier.ES512));
    }

    @Test
    public void testEc384KeyBindingWithoutAlgorithm() {
        testKeyBinding(() -> KeyUtils.generateEcKeyPair(EC_KEY_SECP384R1),
                keyPair -> JWKBuilder.create().kid(KEY_ID).ec(keyPair.getPublic(), KeyUse.SIG),
                coseKey -> assertEcKey(coseKey, Curve.SECP384R1, null));
    }

    @Test
    public void testRSA2048KeyBinding() {
        testKeyBinding(() -> KeyUtils.generateRsaKeyPair(2048),
                keyPair -> JWKBuilder.create().kid(KEY_ID).algorithm(Algorithm.PS256)
                        .rsa(keyPair.getPublic(), KeyUse.SIG),
                coseKey -> assertRsaKey(coseKey, COSEAlgorithmIdentifier.PS256, 2048));
    }

    @Test
    public void testRSA4096KeyBinding() {
        testKeyBinding(() -> KeyUtils.generateRsaKeyPair(4096),
                keyPair -> JWKBuilder.create().kid(KEY_ID).algorithm(Algorithm.PS512)
                        .rsa(keyPair.getPublic(), KeyUse.SIG),
                coseKey -> assertRsaKey(coseKey, COSEAlgorithmIdentifier.PS512, 4096));
    }

    private void testKeyBinding(Supplier<KeyPair> keyPairSupplier, Function<KeyPair, JWK> jwkProvider,
            Consumer<COSEKey> keyFormatValidator) {
        KeyPair keyPair = keyPairSupplier.get();
        JWK proofJwk = jwkProvider.apply(keyPair);

        COSEKey coseKey = MdocDeviceKey.fromProofJwk(proofJwk).toCoseKey();

        assertArrayEquals(KEY_ID.getBytes(StandardCharsets.UTF_8), coseKey.getKeyId());
        assertTrue(coseKey.hasPublicKey());
        assertFalse(coseKey.hasPrivateKey());
        assertNotNull(coseKey.getPublicKey());

        keyFormatValidator.accept(coseKey);
    }

    private void assertEdDSAKey(COSEKey coseKey, Curve expectedCurve) {
        assertTrue(coseKey instanceof EdDSACOSEKey);
        assertEquals(COSEKeyType.OKP, coseKey.getKeyType());
        assertEquals(COSEAlgorithmIdentifier.EdDSA, coseKey.getAlgorithm());

        EdDSACOSEKey edDsaKey = (EdDSACOSEKey) coseKey;
        assertEquals(expectedCurve, edDsaKey.getCurve());
        assertEquals(expectedCurve.getSize(), edDsaKey.getX().length);
        assertNull(edDsaKey.getD());
    }

    private void assertEcKey(COSEKey coseKey, Curve expectedCurve, COSEAlgorithmIdentifier expectedAlgorithm) {
        assertTrue(coseKey instanceof EC2COSEKey);
        assertEquals(COSEKeyType.EC2, coseKey.getKeyType());
        assertEquals(expectedAlgorithm, coseKey.getAlgorithm());

        EC2COSEKey ecKey = (EC2COSEKey) coseKey;
        assertEquals(expectedCurve, ecKey.getCurve());
        assertEquals(expectedCurve.getSize(), ecKey.getX().length);
        assertEquals(expectedCurve.getSize(), ecKey.getY().length);
        assertNull(ecKey.getD());
    }

    private void assertRsaKey(COSEKey coseKey, COSEAlgorithmIdentifier expectedAlgorithm, int expectedKeySize) {
        assertTrue(coseKey instanceof RSACOSEKey);
        assertEquals(COSEKeyType.RSA, coseKey.getKeyType());
        assertEquals(expectedAlgorithm, coseKey.getAlgorithm());

        RSACOSEKey rsaKey = (RSACOSEKey) coseKey;
        assertEquals(expectedKeySize, new BigInteger(1, rsaKey.getN()).bitLength());
        assertNotNull(rsaKey.getE());
        assertNull(rsaKey.getD());
    }
}
