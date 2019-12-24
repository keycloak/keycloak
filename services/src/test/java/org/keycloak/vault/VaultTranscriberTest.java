/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.vault;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests for the {@link DefaultVaultTranscriber} implementation.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class VaultTranscriberTest {

    private final VaultTranscriber transcriber = new DefaultVaultTranscriber(new TestVaultProvider());

    private static Map<String, String> validExpressions;

    private static String[] invalidExpressions;

    @BeforeClass
    public static void init() {
        validExpressions = new HashMap<>();
        // expressions with keys that exist in the vault.
        validExpressions.put("${vault.vault_key_1}", "secret1");
        validExpressions.put("${vault.vault_key_2}", "secret2");
        // expressions with keys that don't exist in the vault.
        validExpressions.put("${vault.invalid_key}", null);
        validExpressions.put("${vault.${.id-!@#$%^&*_()}}", null);
        // invalid expressions.
        invalidExpressions = new String[]{"${vault.}","$vault.id}", "{vault.id}", "${vault.id", "${vaultid}", ""};

    }

    /**
     * Tests the retrieval of raw secrets using valid vault expressions - i.e. expressions that identify the key that should be
     * used to retrieve the secret from the vault.
     * <p/>
     * Some of the keys used in this test exist in the test vault while others, despite being valid expressions, don't identify
     * any secret in the vault. For the former, the test compares the obtained secret against the expected secret (using both
     * the buffer and array representations of the secret) and then checks if the secrets have been overridden/destroyed after
     * the try-wih-resources block. For the latter, the tests checks if an empty {@link Optional} has been returned by the
     * transcriber.
     *
     */
    @Test
    public void testGetRawSecretUsingValidExpressions() {
        ByteBuffer secretBuffer = null;
        byte[] secretArray = null;

        // attempt to obtain a secret using a proper vault expressions. The key may or may not exist in the vault, so we
        // check both cases using the returned optional and comparing against the expected secret.
        for (String key : validExpressions.keySet()) {
            String expectedSecret = validExpressions.get(key);
            try (VaultRawSecret secret = transcriber.getRawSecret(key)) {
                Optional<ByteBuffer> optional = secret.get();
                Optional<byte[]> optionalArray = secret.getAsArray();
                if (expectedSecret != null) {
                    Assert.assertTrue(optional.isPresent());
                    secretBuffer = optional.get();
                    Assert.assertArrayEquals(expectedSecret.getBytes(StandardCharsets.UTF_8), secretBuffer.array());
                    Assert.assertTrue(optionalArray.isPresent());
                    secretArray = optionalArray.get();
                    Assert.assertArrayEquals(expectedSecret.getBytes(StandardCharsets.UTF_8), secretArray);
                } else {
                    Assert.assertFalse(optional.isPresent());
                    Assert.assertFalse(optionalArray.isPresent());
                }
            }
            // after the try-with-resources block the secret should have been overridden.
            if (expectedSecret != null) {
                Assert.assertFalse(Arrays.equals(expectedSecret.getBytes(StandardCharsets.UTF_8), secretBuffer.array()));
                Assert.assertFalse(Arrays.equals(expectedSecret.getBytes(StandardCharsets.UTF_8), secretArray));
            }
        }
    }

    /**
     * Tests the retrieval of raw secrets using invalid vault expressions - i.e. expressions that identify the key that should be
     * used to retrieve the secret from the vault. When the values supplied to the transcriber are not valid vault expressions
     * the value itself is assumed to be the secret and is enclosed in the secret class that is returned. Thus this test
     * checks if the returned secret matches the specified values (using both the buffer and array representation of the value)
     * and then checks if the secrets have been overridden/destroyed after the try-wih-resources block.
     *
     */
    @Test
    public void testGetRawSecretUsingInvalidExpressions() {
        ByteBuffer secretBuffer;
        byte[] secretArray;

        // attempt to obtain a secret using invalid vault expressions - the value itself should be returned as a byte buffer.
        for (String value : this.invalidExpressions) {
            try (VaultRawSecret secret = transcriber.getRawSecret(value)) {
                Optional<ByteBuffer> optional = secret.get();
                Optional<byte[]> optionalArray = secret.getAsArray();
                Assert.assertTrue(optional.isPresent());
                secretBuffer = optional.get();
                Assert.assertArrayEquals(value.getBytes(StandardCharsets.UTF_8), secretBuffer.array());
                Assert.assertTrue(optionalArray.isPresent());
                secretArray = optionalArray.get();
                Assert.assertArrayEquals(value.getBytes(StandardCharsets.UTF_8), secretArray);
            }
            // after the try-with-resources block the secret should have been overridden.
            if (!value.isEmpty()) {
                Assert.assertFalse(Arrays.equals(value.getBytes(StandardCharsets.UTF_8), secretBuffer.array()));
                Assert.assertFalse(Arrays.equals(value.getBytes(StandardCharsets.UTF_8), secretArray));
            }
        }
    }

    /**
     * Tests that a null vault expression always returns an empty secret.
     *
     */
    @Test
    public void testGetRawSecretUsingNullExpression() {
        // check that a null expression results in an empty optional instance.
        try (VaultRawSecret secret = transcriber.getRawSecret(null)) {
            Assert.assertFalse(secret.get().isPresent());
            Assert.assertFalse(secret.getAsArray().isPresent());
        }
    }

    /**
     * Tests the retrieval of char secrets using valid vault expressions - i.e. expressions that identify the key that should be
     * used to retrieve the secret from the vault.
     * <p/>
     * Some of the keys used in this test exist in the test vault while others, despite being valid expressions, don't identify
     * any secret in the vault. For the former, the test compares the obtained secret against the expected secret (using both
     * the buffer and array representations of the secret) and then checks if the secrets have been overridden/destroyed after
     * the try-wih-resources block. For the latter, the tests checks if an empty {@link Optional} has been returned by the
     * transcriber.
     *
     */
    @Test
    public void testGetCharSecretUsingValidExpressions() {
        CharBuffer secretBuffer = null;
        char[] secretArray = null;

        // attempt to obtain a secret using a proper vault expressions. The key may or may not exist in the vault, so we
        // check both cases using the returned optional and comparing against the expected secret.
        for (String key : validExpressions.keySet()) {
            String expectedSecret = validExpressions.get(key);
            try (VaultCharSecret secret = transcriber.getCharSecret(key)) {
                Optional<CharBuffer> optional = secret.get();
                Optional<char[]> optionalArray = secret.getAsArray();
                if (expectedSecret != null) {
                    Assert.assertTrue(optional.isPresent());
                    secretBuffer = optional.get();
                    Assert.assertArrayEquals(expectedSecret.toCharArray(), secretBuffer.array());
                    Assert.assertTrue(optionalArray.isPresent());
                    secretArray = optionalArray.get();
                    Assert.assertArrayEquals(expectedSecret.toCharArray(), secretArray);
                } else {
                    Assert.assertFalse(optional.isPresent());
                    Assert.assertFalse(optionalArray.isPresent());
                }
            }
            // after the try-with-resources block the secret should have been overridden.
            if (expectedSecret != null) {
                Assert.assertFalse(Arrays.equals(expectedSecret.toCharArray(), secretBuffer.array()));
                Assert.assertFalse(Arrays.equals(expectedSecret.toCharArray(), secretArray));
            }
        }
    }

    /**
     * Tests the retrieval of char secrets using invalid vault expressions - i.e. expressions that identify the key that should be
     * used to retrieve the secret from the vault. When the values supplied to the transcriber are not valid vault expressions
     * the value itself is assumed to be the secret and is enclosed in the secret class that is returned. Thus this test
     * checks if the returned secret matches the specified values (using both the buffer and array representation of the value)
     * and then checks if the secrets have been overridden/destroyed after the try-wih-resources block.
     *
     */
    @Test
    public void testGetCharSecretUsingInvalidExpressions() {
        CharBuffer secretBuffer;
        char[] secretArray;

        // attempt to obtain a secret using invalid vault expressions - the value itself should be returned as a byte buffer.
        for (String value : this.invalidExpressions) {
            try (VaultCharSecret secret = transcriber.getCharSecret(value)) {
                Optional<CharBuffer> optional = secret.get();
                Optional<char[]> optionalArray = secret.getAsArray();
                Assert.assertTrue(optional.isPresent());
                secretBuffer = optional.get();
                Assert.assertArrayEquals(value.toCharArray(), secretBuffer.array());
                Assert.assertTrue(optionalArray.isPresent());
                secretArray = optionalArray.get();
                Assert.assertArrayEquals(value.toCharArray(), secretArray);
            }
            // after the try-with-resources block the secret should have been overridden.
            if (!value.isEmpty()) {
                Assert.assertFalse(Arrays.equals(value.toCharArray(), secretBuffer.array()));
                Assert.assertFalse(Arrays.equals(value.toCharArray(), secretArray));
            }
        }
    }

    /**
     * Tests that a null vault expression always returns an empty secret.
     *
     */
    @Test
    public void testGetCharSecretUsingNullExpression() {
        // check that a null expression results in an empty optional instance.
        try (VaultCharSecret secret = transcriber.getCharSecret(null)) {
            Assert.assertFalse(secret.get().isPresent());
            Assert.assertFalse(secret.getAsArray().isPresent());
        }
    }

    /**
     * Tests the retrieval of string secrets using valid vault expressions - i.e. expressions that identify the key that should be
     * used to retrieve the secret from the vault.
     * <p/>
     * Some of the keys used in this test exist in the test vault while others, despite being valid expressions, don't identify
     * any secret in the vault. For the former, the test compares the obtained secret against the expected secret. For the latter,
     * the tests checks if an empty {@link Optional} has been returned by the transcriber. Because strings are immutable,
     * this test doesn't verify if the secrets have been destroyed after the try-with-resources block.
     *
     */
    @Test
    public void testGetStringSecretUsingValidExpressions() {

        // attempt to obtain a secret using a proper vault expressions. The key may or may not exist in the vault, so we
        // check both cases using the returned optional and comparing against the expected secret.
        for (String key : validExpressions.keySet()) {
            String expectedSecret = validExpressions.get(key);
            try (VaultStringSecret secret = transcriber.getStringSecret(key)) {
                Optional<String> optional = secret.get();
                if (expectedSecret != null) {
                    Assert.assertTrue(optional.isPresent());
                    String secretString = optional.get();
                    Assert.assertEquals(expectedSecret, secretString);
                } else {
                    Assert.assertFalse(optional.isPresent());
                }
            }
        }
    }

    /**
     * Tests the retrieval of string secrets using invalid vault expressions - i.e. expressions that identify the key that should be
     * used to retrieve the secret from the vault. When the values supplied to the transcriber are not valid vault expressions
     * the value itself is assumed to be the secret and is enclosed in the secret class that is returned. Thus this test
     * checks if the returned secret matches the specified values. Again, due to the fact that strings are immutable, this test
     * doesn't verify if the secrets have been destroyed after the try-with-resources block.
     *
     */
    @Test
    public void testGetStringSecretUsingInvalidExpressions() {

        // attempt to obtain a secret using invalid vault expressions - the value itself should be returned as a byte buffer.
        for (String value : invalidExpressions) {
            try (VaultStringSecret secret = transcriber.getStringSecret(value)) {
                Optional<String> optional = secret.get();
                Assert.assertTrue(optional.isPresent());
                String secretString = optional.get();
                Assert.assertEquals(value, secretString);
            }
        }
    }

    /**
     * Tests that a null vault expression always returns an empty secret.
     *
     */
    @Test
    public void testGetStringSecretUsingNullExpression() {
        // check that a null expression results in an empty optional instance.
        try (VaultStringSecret secret = transcriber.getStringSecret(null)) {
            Assert.assertFalse(secret.get().isPresent());
        }
    }

    /**
     * Tests that when no {@link VaultProvider} is supplied to the transcriber it uses a default implementation that
     * always returns empty secrets.
     *
     */
    @Test
    public void testTranscriberWithNullProvider() {
        VaultTranscriber transcriber = new DefaultVaultTranscriber(null);
        // none of the valid expressions identify a key in the default vault as it always returns empty secrets.
        for (String key : validExpressions.keySet()) {
            try (VaultRawSecret secret = transcriber.getRawSecret(key)) {
                Assert.assertFalse(secret.get().isPresent());
                Assert.assertFalse(secret.getAsArray().isPresent());
            }
        }
        // for invalid expressions, the transcriber doesn't rely on the provider so it should encode the value itself.
        for (String value : invalidExpressions) {
            try (VaultStringSecret secret = transcriber.getStringSecret(value)) {
                Optional<String> optional = secret.get();
                Assert.assertTrue(optional.isPresent());
                String secretString = optional.get();
                Assert.assertEquals(value, secretString);
            }
        }
    }

    class TestVaultProvider implements VaultProvider {

        private Map<String, byte[]> secrets = new HashMap<>();

        TestVaultProvider() {
            secrets.put("vault_key_1", "secret1".getBytes());
            secrets.put("vault_key_2", "secret2".getBytes());
        }

        @Override
        public VaultRawSecret obtainSecret(String vaultSecretId) {
            if (secrets.containsKey(vaultSecretId)) {
                return DefaultVaultRawSecret.forBuffer(Optional.of(ByteBuffer.wrap(secrets.get(vaultSecretId))));
            }
            else {
                return DefaultVaultRawSecret.forBuffer(Optional.empty());
            }
        }

        @Override
        public void close() {
            // nothing to do
        }
    }
}