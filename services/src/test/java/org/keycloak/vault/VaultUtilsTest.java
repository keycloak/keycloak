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

import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for the {@link VaultUtils} class.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class VaultUtilsTest {

    @Test
    public void testDetectVaultExpressions() throws Exception {
        String[] validExpressions = new String[]{"${vault.vault-id}", "${vault.${.id-!@#$%^&*_()}}"};
        for (String expression : validExpressions) {
            Assert.assertTrue(VaultUtils.isVaultExpression(expression));
        }

        String[] invalidExpressions = new String[]{"${vault.}","$vault.id}", "{vault.id}", "${vault.id", "${vaultid}", ""};
        for (String expression : invalidExpressions) {
            Assert.assertFalse(VaultUtils.isVaultExpression(expression));
        }
    }

    @Test
    public void testExtractVaultEntryId() throws Exception {
        // entry containing simple text.
        String entryId = VaultUtils.getVaultEntryKey("${vault.entrytext}");
        Assert.assertNotNull(entryId);
        Assert.assertEquals("entrytext", entryId);
        // entry with underscores.
        entryId = VaultUtils.getVaultEntryKey("${vault.entry_id_with_underscore}");
        Assert.assertNotNull(entryId);
        Assert.assertEquals("entry_id_with_underscore", entryId);
        // entry with special characters, starting with ${ and ending in }.
        entryId = VaultUtils.getVaultEntryKey("${vault.${.id-!@#$%^&*_()}}");
        Assert.assertNotNull(entryId);
        Assert.assertEquals("${.id-!@#$%^&*_()}", entryId);
    }

    @Test
    public void testRetrieveRawSecret() throws Exception {
        TestVaultProvider provider = new TestVaultProvider();
        ByteBuffer secretBuffer;

        // first attempt to obtain a secret using a proper vault string with an existing key.
        try (VaultRawSecret secret = VaultUtils.getRawSecret(provider, "${vault.vault_key_1}")) {
            Optional<ByteBuffer> optional = secret.getRawSecret();
            Assert.assertTrue(optional.isPresent());
            secretBuffer = optional.get();
            Assert.assertTrue(Arrays.equals("secret1".getBytes(StandardCharsets.UTF_8), secretBuffer.array()));
        }
        // after the try-with-resources block the secret should have been overridden
        Assert.assertFalse(Arrays.equals("secret1".getBytes(StandardCharsets.UTF_8), secretBuffer.array()));

        // now attempt to obtain a secret using a proper vault string with a non-existing key.
        try (VaultRawSecret secret = VaultUtils.getRawSecret(provider, "${vault.non_existing_key}")) {
            Optional<ByteBuffer> optional = secret.getRawSecret();
            Assert.assertFalse(optional.isPresent());
        }

        // finally try to obtain a secret using a string that is not a vault string.
        try (VaultRawSecret secret = VaultUtils.getRawSecret(provider, "non_vault_secret")) {
            Assert.fail();
        } catch(IllegalArgumentException expected) {
            Assert.assertEquals("Value non_vault_secret is not a valid vault string. Expected format: ${vault.entry_key}",
                    expected.getMessage());
        }
    }

    @Test
    public void testRetrieveCharSecret() throws Exception {
        TestVaultProvider provider = new TestVaultProvider();
        CharBuffer secretBuffer;

        // first attempt to obtain a secret using a proper vault string with an existing key.
        try (VaultCharSecret secret = VaultUtils.getCharSecret(provider, "${vault.vault_key_2}")) {
            Optional<CharBuffer> optional = secret.getCharSecret();
            Assert.assertTrue(optional.isPresent());
            secretBuffer = optional.get();
            Assert.assertTrue(Arrays.equals("secret2".toCharArray(), secretBuffer.array()));
        }
        // after the try-with-resources block the secret should have been overridden
        Assert.assertFalse(Arrays.equals("secret2".toCharArray(), secretBuffer.array()));

        // now attempt to obtain a secret using a proper vault string with a non-existing key.
        try (VaultCharSecret secret = VaultUtils.getCharSecret(provider, "${vault.non_existing_key}")) {
            Optional<CharBuffer> optional = secret.getCharSecret();
            Assert.assertFalse(optional.isPresent());
        }

        // finally try to obtain a secret using a string that is not a vault string. The string itself should be encoded.
        try (VaultCharSecret secret = VaultUtils.getCharSecret(provider, "non_vault_secret")) {
            Assert.fail();
        } catch(IllegalArgumentException expected) {
            Assert.assertEquals("Value non_vault_secret is not a valid vault string. Expected format: ${vault.entry_key}",
                    expected.getMessage());
        }
    }

    @Test
    public void testRetrieveStringSecret() throws Exception {
        TestVaultProvider provider = new TestVaultProvider();

        // first attempt to obtain a secret using a proper vault string with an existing key.
        Optional<WeakReference<String>> optional = VaultUtils.getStringSecret(provider, "${vault.vault_key_1}");
        Assert.assertTrue(optional.isPresent());
        WeakReference<String> stringSecret = optional.get();
        Assert.assertEquals("secret1", stringSecret.get());

        // now attempt to obtain a secret using a proper vault string with a non-existing key.
        optional = VaultUtils.getStringSecret(provider, "${vault.non_existing_key}");
        Assert.assertFalse(optional.isPresent());

        // finally try to obtain a secret using a string that is not a vault string. The string itself should be encoded.
        try {
            optional = VaultUtils.getStringSecret(provider, "non_vault_secret");
            Assert.fail();
        } catch(IllegalArgumentException expected) {
            Assert.assertEquals("Value non_vault_secret is not a valid vault string. Expected format: ${vault.entry_key}",
                    expected.getMessage());
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
