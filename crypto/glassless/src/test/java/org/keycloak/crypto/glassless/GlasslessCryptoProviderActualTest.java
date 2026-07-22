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

package org.keycloak.crypto.glassless;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Provider;
import java.security.Security;
import java.security.Signature;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.crypto.FipsMode;
import org.keycloak.jose.jwe.JWE;
import org.keycloak.jose.jwe.JWEConstants;
import org.keycloak.jose.jwe.JWEHeader;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

public class GlasslessCryptoProviderActualTest {

    private static final byte[] DATA = "Keycloak Glassless integration".getBytes(StandardCharsets.UTF_8);

    @After
    public void cleanup() {
        CryptoIntegration.setProvider(null);
        Security.removeProvider("GlaSSLess");
    }

    @Test
    public void shouldPerformKeycloakCryptographicOperations() throws Exception {
        assumeGlasslessAvailable();

        CryptoIntegration.init(Thread.currentThread().getContextClassLoader(), "glassless", FipsMode.NON_STRICT);
        GlasslessCryptoProvider provider = (GlasslessCryptoProvider) CryptoIntegration.getProvider();
        Provider glassless = provider.getBouncyCastleProvider();
        assertEquals("GlaSSLess", glassless.getName());
        assertEquals(glassless, Security.getProviders()[0]);

        KeyPairGenerator keyPairGenerator = provider.getKeyPairGen("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        assertEquals(glassless, keyPairGenerator.getProvider());

        assertSignature(provider, keyPair, "RS256");
        assertSignature(provider, keyPair, "PS256");

        SecretKeySpec key = new SecretKeySpec(new byte[16], "AES");
        IvParameterSpec iv = new IvParameterSpec(new byte[16]);
        Cipher encrypt = provider.getAesCbcCipher();
        encrypt.init(Cipher.ENCRYPT_MODE, key, iv);
        byte[] encrypted = encrypt.doFinal(DATA);
        Cipher decrypt = provider.getAesCbcCipher();
        decrypt.init(Cipher.DECRYPT_MODE, key, iv);
        assertArrayEquals(DATA, decrypt.doFinal(encrypted));
        assertEquals(glassless, encrypt.getProvider());

        Cipher keyWrap = Cipher.getInstance("AESWrap_128", glassless);
        keyWrap.init(Cipher.ENCRYPT_MODE, key);
        byte[] keyMaterial = new byte[32];
        byte[] wrappedKey = keyWrap.doFinal(keyMaterial);
        Cipher keyUnwrap = Cipher.getInstance("AESWrap_128", glassless);
        keyUnwrap.init(Cipher.DECRYPT_MODE, key);
        assertArrayEquals(keyMaterial, keyUnwrap.doFinal(wrappedKey));

        SecretKeySpec hmacKey = new SecretKeySpec(new byte[16], "HmacSHA256");
        Mac glasslessMac = Mac.getInstance("HmacSHA256", glassless);
        glasslessMac.init(hmacKey);
        byte[] glasslessTag = glasslessMac.doFinal(DATA);
        Mac referenceMac = Mac.getInstance("HmacSHA256", "SunJCE");
        referenceMac.init(hmacKey);
        assertArrayEquals(referenceMac.doFinal(DATA), glasslessTag);

        SecretKeyFactory secretKeyFactory = provider.getSecretKeyFact("PBKDF2WithHmacSHA256");
        assertEquals(glassless, secretKeyFactory.getProvider());
        assertEquals(32, secretKeyFactory.generateSecret(
                new PBEKeySpec("glassless-keycloak-password".toCharArray(), new byte[16], 1_000, 256)).getEncoded().length);

        assertTrue(provider.createECParams("secp256r1").getOrder().bitLength() >= 256);

        JWE jwe = new JWE()
                .header(new JWEHeader(JWEConstants.A128KW, JWEConstants.A128CBC_HS256, null))
                .content(DATA);
        jwe.getKeyStorage().setEncryptionKey(key);
        String encoded = jwe.encodeJwe();
        JWE decoded = new JWE();
        decoded.getKeyStorage().setDecryptionKey(key);
        decoded.verifyAndDecodeJwe(encoded);
        assertArrayEquals(DATA, decoded.getContent());
    }

    @Test
    public void shouldRequireOpenSslFipsForStrictMode() throws Exception {
        assumeGlasslessAvailable();

        GlasslessCryptoProvider provider = new GlasslessCryptoProvider();
        boolean fipsMode = (boolean) provider.getBouncyCastleProvider().getClass().getMethod("isFIPSMode")
                .invoke(provider.getBouncyCastleProvider());
        Class<?> status = Class.forName("net.glassless.provider.FIPSStatus");
        boolean fipsProviderAvailable = (boolean) status.getMethod("isFIPSProviderAvailable").invoke(null);
        boolean openSslFipsEnabled = GlasslessCryptoProvider.isOpenSslDefaultContextFipsEnabled(
                Thread.currentThread().getContextClassLoader());

        if (fipsMode && fipsProviderAvailable && openSslFipsEnabled) {
            assertArrayEquals(new String[] { "2048", "3072", "4096" },
                    new GlasslessStrictCryptoProvider().getSupportedRsaKeySizes());
        } else {
            assertThrows(IllegalStateException.class, GlasslessStrictCryptoProvider::new);
        }
    }

    private static void assertSignature(GlasslessCryptoProvider provider, KeyPair keyPair, String algorithm) throws Exception {
        Signature signer = provider.getSignature(algorithm);
        signer.initSign(keyPair.getPrivate());
        signer.update(DATA);
        byte[] signature = signer.sign();

        Signature verifier = provider.getSignature(algorithm);
        verifier.initVerify(keyPair.getPublic());
        verifier.update(DATA);
        assertTrue(verifier.verify(signature));
        assertEquals("GlaSSLess", signer.getProvider().getName());
    }

    private static void assumeGlasslessAvailable() {
        assumeTrue("Glassless requires Java 25", Runtime.version().feature() >= 25);
        try {
            Class.forName("net.glassless.provider.GlaSSLessProvider");
        } catch (ClassNotFoundException cause) {
            assumeTrue("Glassless test dependency is not available", false);
        }
    }
}
