/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

import org.junit.Test;
import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.CertificateUtils;
import org.keycloak.common.util.KeyUtils;
import org.keycloak.common.util.PemUtils;
import org.keycloak.crypto.JavaAlgorithm;
import org.keycloak.util.JsonSerialization;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;

import static org.junit.Assert.*;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class JWKTest {

    @Test
    public void publicRs256() throws Exception {
        KeyPair keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        PublicKey publicKey = keyPair.getPublic();
        X509Certificate certificate = CertificateUtils.generateV1SelfSignedCertificate(keyPair, "Test");

        JWK jwk = JWKBuilder.create().kid(KeyUtils.createKeyId(publicKey)).algorithm("RS256").rsa(publicKey, certificate);

        assertNotNull(jwk.getKeyId());
        assertEquals("RSA", jwk.getKeyType());
        assertEquals("RS256", jwk.getAlgorithm());
        assertEquals("sig", jwk.getPublicKeyUse());

        assertTrue(jwk instanceof RSAPublicJWK);
        assertNotNull(((RSAPublicJWK) jwk).getModulus());
        assertNotNull(((RSAPublicJWK) jwk).getPublicExponent());
        assertNotNull(((RSAPublicJWK) jwk).getX509CertificateChain());
        assertEquals(PemUtils.encodeCertificate(certificate), ((RSAPublicJWK) jwk).getX509CertificateChain()[0]);
        assertNotNull(((RSAPublicJWK) jwk).getSha1x509Thumbprint());
        assertEquals(PemUtils.generateThumbprint(((RSAPublicJWK) jwk).getX509CertificateChain(), "SHA-1"), ((RSAPublicJWK) jwk).getSha1x509Thumbprint());
        assertNotNull(((RSAPublicJWK) jwk).getSha256x509Thumbprint());
        assertEquals(PemUtils.generateThumbprint(((RSAPublicJWK) jwk).getX509CertificateChain(), "SHA-256"), ((RSAPublicJWK) jwk).getSha256x509Thumbprint());

        String jwkJson = JsonSerialization.writeValueAsString(jwk);

        PublicKey publicKeyFromJwk = JWKParser.create().parse(jwkJson).toPublicKey();

        // Parse
        assertArrayEquals(publicKey.getEncoded(), publicKeyFromJwk.getEncoded());

        byte[] data = "Some test string".getBytes(StandardCharsets.UTF_8);
        byte[] sign = sign(data, JavaAlgorithm.RS256, keyPair.getPrivate());
        verify(data, sign, JavaAlgorithm.RS256, publicKeyFromJwk);
    }

    @Test
    public void publicEs256() throws Exception {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
        SecureRandom randomGen = SecureRandom.getInstance("SHA1PRNG");
        ECGenParameterSpec ecSpec = new ECGenParameterSpec("secp256r1");
        keyGen.initialize(ecSpec, randomGen);
        KeyPair keyPair = keyGen.generateKeyPair();

        PublicKey publicKey = keyPair.getPublic();

        JWK jwk = JWKBuilder.create().kid(KeyUtils.createKeyId(keyPair.getPublic())).algorithm("ES256").ec(publicKey);

        assertEquals("EC", jwk.getKeyType());
        assertEquals("ES256", jwk.getAlgorithm());
        assertEquals("sig", jwk.getPublicKeyUse());

        assertTrue(jwk instanceof ECPublicJWK);

        ECPublicJWK ecJwk = (ECPublicJWK) jwk;

        assertNotNull(ecJwk.getCrv());
        assertNotNull(ecJwk.getX());
        assertNotNull(ecJwk.getY());

        byte[] xBytes = Base64Url.decode(ecJwk.getX());
        byte[] yBytes = Base64Url.decode(ecJwk.getY());

        assertEquals(256/8, xBytes.length);
        assertEquals(256/8, yBytes.length);

        String jwkJson = JsonSerialization.writeValueAsString(jwk);

        JWKParser parser = JWKParser.create().parse(jwkJson);
        PublicKey publicKeyFromJwk = parser.toPublicKey();

        assertArrayEquals(publicKey.getEncoded(), publicKeyFromJwk.getEncoded());

        byte[] data = "Some test string".getBytes(StandardCharsets.UTF_8);
        byte[] sign = sign(data, JavaAlgorithm.ES256, keyPair.getPrivate());
        verify(data, sign, JavaAlgorithm.ES256, publicKeyFromJwk);
    }

    @Test
    public void parse() {
        String jwkJson = "{" +
                "   \"kty\": \"RSA\"," +
                "   \"alg\": \"RS256\"," +
                "   \"use\": \"sig\"," +
                "   \"kid\": \"3121adaa80ace09f89d80899d4a5dc4ce33d0747\"," +
                "   \"n\": \"soFDjoZ5mQ8XAA7reQAFg90inKAHk0DXMTizo4JuOsgzUbhcplIeZ7ks83hsEjm8mP8lUVaHMPMAHEIp3gu6Xxsg-s73ofx1dtt_Fo7aj8j383MFQGl8-FvixTVobNeGeC0XBBQjN8lEl-lIwOa4ZoERNAShplTej0ntDp7TQm0=\"," +
                "   \"e\": \"AQAB\"" +
                "  }";

        PublicKey key = JWKParser.create().parse(jwkJson).toPublicKey();
        assertEquals("RSA", key.getAlgorithm());
        assertEquals("X.509", key.getFormat());
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
