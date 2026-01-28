/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.crypto.def.test;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.InvalidKeySpecException;

import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.Environment;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.def.BCEcdhEsAlgorithmProvider;
import org.keycloak.jose.jwe.JWE;
import org.keycloak.jose.jwe.JWEConstants;
import org.keycloak.jose.jwe.JWEException;
import org.keycloak.jose.jwe.JWEHeader;
import org.keycloak.rule.CryptoInitRule;

import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.jce.spec.ECNamedCurveSpec;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

public class BCEcdhEsAlgorithmProviderTest {
    @Before
    public void before() {
        // Run this test just if java is not in FIPS mode
        Assume.assumeFalse("Java is in FIPS mode. Skipping the test.", Environment.isJavaInFipsMode());
    }

    @ClassRule
    public static CryptoInitRule cryptoInitRule = new CryptoInitRule();

    /**
     * Test ECDH-ES Key Agreement Computation.
     *
     * @see <a href=
     *      "https://datatracker.ietf.org/doc/html/rfc7518#appendix-C">Example
     *      ECDH-ES Key Agreement Computation</a>
     * @throws InvalidKeySpecException  exception
     * @throws NoSuchAlgorithmException exception
     * @throws NoSuchProviderException  exception
     * @throws IllegalStateException    exception
     * @throws InvalidKeyException      exception
     */
    @Test
    public void deriveKey() throws InvalidKeySpecException, NoSuchAlgorithmException, NoSuchProviderException,
            InvalidKeyException, IllegalStateException {
        PrivateKey ephemeralPrivateKey = getPrivateKey("P-256", "0_NxaRPUMQoAJt50Gz8YiTr8gRTwyEaCumd-MToTmIo");
        PublicKey encryptionPublicKey = getPublicKey("P-256", "weNJy2HscCSM6AEDTDg04biOvhFhyyWvOHQfeF_PxMQ",
                "e8lnCO-AlStT-NJVX-crhB7QRYhiix03illJOVAOyck");
        byte[] derivedKey = BCEcdhEsAlgorithmProvider.deriveKey(encryptionPublicKey, ephemeralPrivateKey, 128,
                "A128GCM", Base64Url.decode("QWxpY2U"), Base64Url.decode("Qm9i"));
        Assert.assertEquals("VqqN6vgjbSBcIijNcacQGg", Base64Url.encode(derivedKey));
    }

    @Test
    public void encodeDecode()
            throws JWEException, InvalidKeySpecException, NoSuchAlgorithmException, NoSuchProviderException {
        PublicKey encryptionPublicKey = getPublicKey("P-256", "weNJy2HscCSM6AEDTDg04biOvhFhyyWvOHQfeF_PxMQ",
                "e8lnCO-AlStT-NJVX-crhB7QRYhiix03illJOVAOyck");
        String content = "plaintext";
        JWE jweEncode = new JWE()
                .header(JWEHeader.builder().algorithm(Algorithm.ECDH_ES_A128KW)
                        .encryptionAlgorithm(JWEConstants.A128CBC_HS256)
                        .build())
                .content(content.getBytes(StandardCharsets.UTF_8));
        jweEncode.getKeyStorage().setEncryptionKey(encryptionPublicKey);
        String encodedJwe = jweEncode.encodeJwe();

        PrivateKey decryptionPrivateKey = getPrivateKey("P-256", "VEmDZpDXXK8p8N0Cndsxs924q6nS1RXFASRl6BfUqdw");
        JWE jweDecode = new JWE();
        jweDecode.getKeyStorage().setDecryptionKey(decryptionPrivateKey);
        jweDecode = jweDecode.verifyAndDecodeJwe(encodedJwe);
        Assert.assertArrayEquals(jweEncode.getContent(), jweDecode.getContent());
    }

    private PublicKey getPublicKey(String crv, String xStr, String yStr)
            throws InvalidKeySpecException, NoSuchAlgorithmException, NoSuchProviderException {
        BigInteger x = new BigInteger(1, Base64Url.decode(xStr));
        BigInteger y = new BigInteger(1, Base64Url.decode(yStr));
        ECPoint point = new ECPoint(x, y);
        String name = nistToSecCurveName(crv);
        ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec(name);
        ECParameterSpec params = new ECNamedCurveSpec(name, spec.getCurve(), spec.getG(), spec.getN());
        ECPublicKeySpec pubKeySpec = new ECPublicKeySpec(point, params);
        KeyFactory keyFactory = KeyFactory.getInstance("EC");
        return keyFactory.generatePublic(pubKeySpec);
    }

    private PrivateKey getPrivateKey(String crv, String dStr)
            throws InvalidKeySpecException, NoSuchAlgorithmException, NoSuchProviderException {
        BigInteger d = new BigInteger(1, Base64Url.decode(dStr));
        String name = nistToSecCurveName(crv);
        ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec(name);
        ECParameterSpec params = new ECNamedCurveSpec(name, spec.getCurve(), spec.getG(), spec.getN());
        ECPrivateKeySpec privKeySpec = new ECPrivateKeySpec(d, params);
        KeyFactory keyFactory = KeyFactory.getInstance("EC");
        return keyFactory.generatePrivate(privKeySpec);
    }

    private static String nistToSecCurveName(String nistCurveName) {
        switch (nistCurveName) {
        case "P-256":
            return "secp256r1";
        case "P-384":
            return "secp384r1";
        case "P-521":
            return "secp521r1";
        default:
            throw new IllegalArgumentException("Unsupported curve");
        }
    }
}