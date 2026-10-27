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

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Property-based test for {@link DefaultSamlDecryptionProvider#unwrapKey(String, byte[])}
 * verifying that invalid inputs produce descriptive errors.
 */
public class DefaultSamlDecryptionProviderInvalidInputPropertyTest {

    private static final KeyPair RSA_2048;
    private static final DefaultSamlDecryptionProvider PROVIDER;

    private static final Set<String> VALID_ALGORITHM_URIS = new HashSet<>(Arrays.asList(
            XMLCipher.RSA_OAEP,
            XMLCipher.RSA_OAEP_11,
            XMLCipher.RSA_v1dot5
    ));

    static {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048);
            RSA_2048 = kpg.generateKeyPair();
            PROVIDER = new DefaultSamlDecryptionProvider(Collections.singletonList(RSA_2048.getPrivate()));
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate RSA key pair for testing", e);
        }
    }

    /**
     * Null algorithm URI with valid encrypted bytes produces a RuntimeException
     * with a message about "null or empty".
     */
    @Property(tries = 100)
    void nullAlgorithmUri_throwsDescriptiveError(
            @ForAll("validEncryptedBytes") byte[] encryptedBytes) {
        try {
            PROVIDER.unwrapKey(null, encryptedBytes, null, null);
            fail("Expected RuntimeException for null algorithm URI");
        } catch (RuntimeException e) {
            assertNotNull("Exception message should not be null", e.getMessage());
            assertFalse("Exception message should not be empty", e.getMessage().isEmpty());
            assertTrue("Exception message should mention 'Algorithm URI', got: " + e.getMessage(),
                    e.getMessage().contains("Algorithm URI"));
            assertTrue("Exception message should mention 'null or empty', got: " + e.getMessage(),
                    e.getMessage().toLowerCase().contains("null or empty"));
        }
    }

    /**
     * Empty algorithm URI with valid encrypted bytes produces a RuntimeException
     * with a message about "null or empty".
     */
    @Property(tries = 100)
    void emptyAlgorithmUri_throwsDescriptiveError(
            @ForAll("validEncryptedBytes") byte[] encryptedBytes) {
        try {
            PROVIDER.unwrapKey("", encryptedBytes, null, null);
            fail("Expected RuntimeException for empty algorithm URI");
        } catch (RuntimeException e) {
            assertNotNull("Exception message should not be null", e.getMessage());
            assertFalse("Exception message should not be empty", e.getMessage().isEmpty());
            assertTrue("Exception message should mention 'Algorithm URI', got: " + e.getMessage(),
                    e.getMessage().contains("Algorithm URI"));
            assertTrue("Exception message should mention 'null or empty', got: " + e.getMessage(),
                    e.getMessage().toLowerCase().contains("null or empty"));
        }
    }

    /**
     * Unsupported algorithm URI produces a RuntimeException with a message
     * about "Unsupported".
     */
    @Property(tries = 100)
    void unsupportedAlgorithmUri_throwsDescriptiveError(
            @ForAll("unsupportedAlgorithmUris") String unsupportedUri,
            @ForAll("validEncryptedBytes") byte[] encryptedBytes) {
        try {
            PROVIDER.unwrapKey(unsupportedUri, encryptedBytes, null, null);
            fail("Expected RuntimeException for unsupported algorithm URI: " + unsupportedUri);
        } catch (RuntimeException e) {
            assertNotNull("Exception message should not be null", e.getMessage());
            assertFalse("Exception message should not be empty", e.getMessage().isEmpty());
            assertTrue("Exception message should mention 'Unsupported', got: " + e.getMessage(),
                    e.getMessage().contains("Unsupported"));
            assertTrue("Exception message should contain the offending URI, got: " + e.getMessage(),
                    e.getMessage().contains(unsupportedUri));
        }
    }

    /**
     * Null encrypted bytes with a valid algorithm URI produces a RuntimeException
     * with a message about "null or empty".
     */
    @Property(tries = 100)
    void nullEncryptedBytes_throwsDescriptiveError(
            @ForAll("validAlgorithmUris") String algorithmUri) {
        try {
            PROVIDER.unwrapKey(algorithmUri, null, null, null);
            fail("Expected RuntimeException for null encrypted bytes");
        } catch (RuntimeException e) {
            assertNotNull("Exception message should not be null", e.getMessage());
            assertFalse("Exception message should not be empty", e.getMessage().isEmpty());
            assertTrue("Exception message should mention 'Encrypted key bytes', got: " + e.getMessage(),
                    e.getMessage().contains("Encrypted key bytes"));
            assertTrue("Exception message should mention 'null or empty', got: " + e.getMessage(),
                    e.getMessage().toLowerCase().contains("null or empty"));
        }
    }

    /**
     * Empty encrypted bytes with a valid algorithm URI produces a RuntimeException
     * with a message about "null or empty".
     */
    @Property(tries = 100)
    void emptyEncryptedBytes_throwsDescriptiveError(
            @ForAll("validAlgorithmUris") String algorithmUri) {
        try {
            PROVIDER.unwrapKey(algorithmUri, new byte[0], null, null);
            fail("Expected RuntimeException for empty encrypted bytes");
        } catch (RuntimeException e) {
            assertNotNull("Exception message should not be null", e.getMessage());
            assertFalse("Exception message should not be empty", e.getMessage().isEmpty());
            assertTrue("Exception message should mention 'Encrypted key bytes', got: " + e.getMessage(),
                    e.getMessage().contains("Encrypted key bytes"));
            assertTrue("Exception message should mention 'null or empty', got: " + e.getMessage(),
                    e.getMessage().toLowerCase().contains("null or empty"));
        }
    }

    /**
     * Corrupted encrypted bytes (random non-zero-length byte arrays that are NOT valid
     * wrapped keys) with a valid algorithm URI produces a RuntimeException with a message
     * about "Failed to unwrap".
     */
    @Property(tries = 100)
    void corruptedEncryptedBytes_throwsDescriptiveError(
            @ForAll("validAlgorithmUris") String algorithmUri,
            @ForAll("corruptedBytes") byte[] corruptedBytes) {
        try {
            PROVIDER.unwrapKey(algorithmUri, corruptedBytes, null, null);
            fail("Expected RuntimeException for corrupted encrypted bytes");
        } catch (RuntimeException e) {
            assertNotNull("Exception message should not be null", e.getMessage());
            assertFalse("Exception message should not be empty", e.getMessage().isEmpty());
            assertTrue("Exception message should mention 'Failed to unwrap', got: " + e.getMessage(),
                    e.getMessage().contains("Failed to unwrap"));
            assertTrue("Exception message should contain the algorithm URI, got: " + e.getMessage(),
                    e.getMessage().contains(algorithmUri));
        }
    }

    // --- Arbitraries ---

    @Provide
    Arbitrary<byte[]> validEncryptedBytes() {
        return Arbitraries.bytes().array(byte[].class).ofMinSize(1).ofMaxSize(50);
    }

    @Provide
    Arbitrary<String> unsupportedAlgorithmUris() {
        return Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(100)
                .filter(s -> !VALID_ALGORITHM_URIS.contains(s));
    }

    @Provide
    Arbitrary<String> validAlgorithmUris() {
        return Arbitraries.of(XMLCipher.RSA_OAEP, XMLCipher.RSA_OAEP_11, XMLCipher.RSA_v1dot5);
    }

    @Provide
    Arbitrary<byte[]> corruptedBytes() {
        return Arbitraries.bytes().array(byte[].class).ofMinSize(1).ofMaxSize(100);
    }
}
