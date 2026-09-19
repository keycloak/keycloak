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

package org.keycloak.saml.processing.core.util;

import net.jqwik.api.*;
import org.apache.xml.security.encryption.XMLCipher;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.util.Collections;

import static org.junit.Assert.assertArrayEquals;

/**
 * Property-based test verifying that {@link DefaultSamlDecryptionProvider#unwrapKey(String, byte[])}
 * produces byte-for-byte identical output to the legacy {@code javax.crypto.Cipher} UNWRAP_MODE path
 * (which simulates what XMLCipher does internally).
 */
public class DefaultSamlDecryptionProviderEquivalencePropertyTest {

    // Pre-generate RSA key pairs since they are expensive to create.
    private static final KeyPair RSA_2048;
    private static final KeyPair RSA_4096;

    static {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048);
            RSA_2048 = kpg.generateKeyPair();
            kpg.initialize(4096);
            RSA_4096 = kpg.generateKeyPair();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate RSA key pairs for testing", e);
        }
    }

    /**
     * Verifies equivalence with legacy decryption using RSA-OAEP.
     *
     * For any valid encrypted symmetric key produced by the XML Encryption standard wrapping process,
     * the DefaultSamlDecryptionProvider.unwrapKey() output is byte-for-byte identical to the
     * key bytes produced by the legacy XMLCipher.UNWRAP_MODE path using the same private key.
     */
    @Property(tries = 100)
    void equivalenceWithLegacy_RSA_OAEP(
            @ForAll("aesKeyBits") int aesKeySize,
            @ForAll("rsaKeyPairChoice") KeyPair rsaKeyPair,
            @ForAll("randomSeeds") long seed) throws Exception {

        // Generate a fresh AES key using the random seed for uniqueness
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(aesKeySize, new SecureRandom(longToBytes(seed)));
        SecretKey aesKey = keyGen.generateKey();

        // Wrap the AES key with the RSA public key using OAEP
        Cipher wrapCipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-1AndMGF1Padding");
        wrapCipher.init(Cipher.WRAP_MODE, rsaKeyPair.getPublic());
        byte[] wrappedBytes = wrapCipher.wrap(aesKey);

        // Decrypt using DefaultSamlDecryptionProvider
        DefaultSamlDecryptionProvider provider = new DefaultSamlDecryptionProvider(Collections.singletonList(rsaKeyPair.getPrivate()));
        byte[] providerResult = provider.unwrapKey(XMLCipher.RSA_OAEP, wrappedBytes, null, null);

        // Decrypt using legacy javax.crypto.Cipher in UNWRAP_MODE (simulates what XMLCipher does internally)
        Cipher legacyCipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-1AndMGF1Padding");
        legacyCipher.init(Cipher.UNWRAP_MODE, rsaKeyPair.getPrivate());
        Key legacyKey = legacyCipher.unwrap(wrappedBytes, "AES", Cipher.SECRET_KEY);
        byte[] legacyResult = legacyKey.getEncoded();

        // Assert both produce byte-for-byte identical output
        assertArrayEquals(legacyResult, providerResult);
    }

    /**
     * Verifies equivalence with legacy decryption using RSA v1.5.
     */
    @Property(tries = 100)
    void equivalenceWithLegacy_RSA_v1dot5(
            @ForAll("aesKeyBits") int aesKeySize,
            @ForAll("rsaKeyPairChoice") KeyPair rsaKeyPair,
            @ForAll("randomSeeds") long seed) throws Exception {

        // Generate a fresh AES key using the random seed for uniqueness
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(aesKeySize, new SecureRandom(longToBytes(seed)));
        SecretKey aesKey = keyGen.generateKey();

        // Wrap the AES key with the RSA public key using PKCS1
        Cipher wrapCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        wrapCipher.init(Cipher.WRAP_MODE, rsaKeyPair.getPublic());
        byte[] wrappedBytes = wrapCipher.wrap(aesKey);

        // Decrypt using DefaultSamlDecryptionProvider
        DefaultSamlDecryptionProvider provider = new DefaultSamlDecryptionProvider(Collections.singletonList(rsaKeyPair.getPrivate()));
        byte[] providerResult = provider.unwrapKey(XMLCipher.RSA_v1dot5, wrappedBytes, null, null);

        // Decrypt using legacy javax.crypto.Cipher in UNWRAP_MODE (simulates what XMLCipher does internally)
        Cipher legacyCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        legacyCipher.init(Cipher.UNWRAP_MODE, rsaKeyPair.getPrivate());
        Key legacyKey = legacyCipher.unwrap(wrappedBytes, "AES", Cipher.SECRET_KEY);
        byte[] legacyResult = legacyKey.getEncoded();

        // Assert both produce byte-for-byte identical output
        assertArrayEquals(legacyResult, providerResult);
    }

    @Provide
    Arbitrary<Integer> aesKeyBits() {
        return Arbitraries.of(128, 192, 256);
    }

    @Provide
    Arbitrary<KeyPair> rsaKeyPairChoice() {
        return Arbitraries.of(RSA_2048, RSA_4096);
    }

    @Provide
    Arbitrary<Long> randomSeeds() {
        return Arbitraries.longs();
    }

    private static byte[] longToBytes(long value) {
        byte[] bytes = new byte[8];
        for (int i = 7; i >= 0; i--) {
            bytes[i] = (byte) (value & 0xFF);
            value >>= 8;
        }
        return bytes;
    }
}
