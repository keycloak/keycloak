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

package org.keycloak.jose.jwk;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;

import org.keycloak.common.util.KeyUtils;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.JavaAlgorithm;
import org.keycloak.rule.CryptoInitRule;
import org.keycloak.util.JsonSerialization;

import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * This is not tested in keycloak-core. The subclasses should be created in the crypto modules to make sure it is tested with corresponding modules (bouncycastle VS bouncycastle-fips)
 *
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public class ServerJWKTest {

    @ClassRule
    public static CryptoInitRule cryptoInitRule = new CryptoInitRule();


    @Test
    public void publicEd25519() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance(Algorithm.Ed25519);
        KeyPair keyPair = keyGen.generateKeyPair();

        PublicKey publicKey = keyPair.getPublic();
        JWK jwk = JWKBuilder.create().kid(KeyUtils.createKeyId(keyPair.getPublic())).algorithm(Algorithm.EdDSA).okp(publicKey);

        assertEquals("OKP", jwk.getKeyType());
        assertEquals("EdDSA", jwk.getAlgorithm());
        assertEquals("sig", jwk.getPublicKeyUse());

        assertTrue(jwk instanceof OKPPublicJWK);

        OKPPublicJWK okpJwk = (OKPPublicJWK) jwk;

        assertEquals("Ed25519", okpJwk.getCrv());
        assertNotNull(okpJwk.getX());

        String jwkJson = JsonSerialization.writeValueAsString(jwk);

        JWKParser parser = JWKParser.create().parse(jwkJson);
        PublicKey publicKeyFromJwk = parser.toPublicKey();

        assertArrayEquals(publicKey.getEncoded(), publicKeyFromJwk.getEncoded());

        byte[] data = "Some test string".getBytes(StandardCharsets.UTF_8);
        byte[] sign = sign(data, JavaAlgorithm.Ed25519, keyPair.getPrivate());
        verify(data, sign, JavaAlgorithm.Ed25519, publicKeyFromJwk);
    }

    @Test
    public void publicEd448() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance(Algorithm.Ed448);
        KeyPair keyPair = keyGen.generateKeyPair();

        PublicKey publicKey = keyPair.getPublic();
        JWK jwk = JWKBuilder.create().kid(KeyUtils.createKeyId(keyPair.getPublic())).algorithm(Algorithm.EdDSA).okp(publicKey);

        assertEquals("OKP", jwk.getKeyType());
        assertEquals("EdDSA", jwk.getAlgorithm());
        assertEquals("sig", jwk.getPublicKeyUse());

        assertTrue(jwk instanceof OKPPublicJWK);

        OKPPublicJWK okpJwk = (OKPPublicJWK) jwk;

        assertEquals("Ed448", okpJwk.getCrv());
        assertNotNull(okpJwk.getX());

        String jwkJson = JsonSerialization.writeValueAsString(jwk);

        JWKParser parser = JWKParser.create().parse(jwkJson);
        PublicKey publicKeyFromJwk = parser.toPublicKey();

        assertArrayEquals(publicKey.getEncoded(), publicKeyFromJwk.getEncoded());

        byte[] data = "Some test string".getBytes(StandardCharsets.UTF_8);
        byte[] sign = sign(data, JavaAlgorithm.Ed448, keyPair.getPrivate());
        verify(data, sign, JavaAlgorithm.Ed448, publicKeyFromJwk);
    }

    private byte[] sign(byte[] data, String javaAlgorithm, PrivateKey key) throws Exception {
        Signature signature = Signature.getInstance(javaAlgorithm);
        signature.initSign(key);
        signature.update(data);
        return signature.sign();
    }

    private boolean verify(byte[] data, byte[] signature, String javaAlgorithm, PublicKey key) throws Exception {
        Signature verifier = Signature.getInstance(javaAlgorithm);
        verifier.initVerify(key);
        verifier.update(data);
        return verifier.verify(signature);
    }

}
