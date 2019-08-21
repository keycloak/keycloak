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
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class that exposes methods for obtaining the vault secrets in different formats. By default, the {@link VaultProvider}
 * provides raw secrets through a {@link ByteBuffer}. This class offers methods to convert the raw secrets into other types
 * (such as {@link VaultCharSecret} or {@link WeakReference<String>}).
 *
 * @see VaultRawSecret
 * @see VaultCharSecret
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class VaultUtils {

    private static final Pattern pattern = Pattern.compile("^\\$\\{vault\\.(.+)}$");

    /**
     * Obtains the raw secret from the provider that matches the entry key in the specified value string. The value
     * must follow the format {@code ${vault.entry_key}} where {@code entry_key} identifies the entry in the vault. If
     * the value doesn't follow the vault string format, an {@link IllegalArgumentException} is thrown.
     *
     * It is recommended that this method is used together with {@link #isVaultExpression(String)} so that the value string
     * is verified to be a valid vault string before this method is invoked.
     *
     * @param provider the {@link VaultProvider} that will be used to retrieve the raw secret. Must not be {@code null}.
     * @param value a {@link String} that might be a vault string containing a vault entry key.
     * @return a {@link VaultRawSecret} representing the secret that was read from the vault.
     * @throws {@link IllegalArgumentException} if the specified value doesn't follow the vault string format.
     */
    public static VaultRawSecret getRawSecret(final VaultProvider provider, final String value) {
        String entryId = getVaultEntryKey(value);
        if (entryId != null) {
            // we have a valid ${vault.entry_id} string, use the provider to retrieve the entry.
            return provider.obtainSecret(entryId);
        } else {
            throw new IllegalArgumentException("Value " + value + " is not a valid vault string. Expected format: ${vault.entry_key}");
        }
    }

    /**
     * Obtains the raw secret from the provider that matches the entry key in the specified value string and converts it into
     * a {@link VaultCharSecret}. The value must follow the format {@code ${vault.entry_key}} where {@code entry_key} identifies
     * the entry in the vault. If the value doesn't follow the vault string format, an {@link IllegalArgumentException} is
     * thrown.
     *
     * It is recommended that this method is used together with {@link #isVaultExpression(String)} so that the value string
     * is verified to be a valid vault string before this method is invoked.
     *
     * This method retrieves the raw secret from the vault and uses it in a try-with-resources construct to convert the secret
     * into a {@link VaultCharSecret}, which, just like {@link VaultRawSecret}, is also {@link AutoCloseable}. This means
     * that the original raw secret is discarded (overridden) when this method returns and the newly constructed secret should
     * also be used in try-with-resources blocks to make sure the content of the {@code CharBuffer} is properly discarded.
     *
     * @param provider the {@link VaultProvider} that will be used to retrieve the raw secret. Must not be {@code null}.
     * @param value a {@link String} that might be a vault string containing a vault entry key.
     * @return a {@link VaultCharSecret} representing the secret that was read from the vault.
     * @throws {@link IllegalArgumentException} if the specified value doesn't follow the vault string format.
     */
    public static VaultCharSecret getCharSecret(final VaultProvider provider, final String value) {
        try (VaultRawSecret rawSecret = getRawSecret(provider, value)) {
            if (!rawSecret.getRawSecret().isPresent()) {
                return DefaultVaultCharSecret.forBuffer(Optional.empty());
            }
            ByteBuffer rawSecretBuffer = rawSecret.getRawSecret().get();
            CharBuffer charSecretBuffer = StandardCharsets.UTF_8.decode(rawSecretBuffer);
            return DefaultVaultCharSecret.forBuffer(Optional.ofNullable(charSecretBuffer));
        }
    }

    /**
     * Obtains the raw secret from the provider that matches the entry key in the specified value string and converts it into
     * a {@link WeakReference<String>}. The value must follow the format {@code ${vault.entry_key}} where {@code entry_key} identifies
     * the entry in the vault. If the value doesn't follow the vault string format, an {@link IllegalArgumentException} is
     * thrown.
     *
     * It is recommended that this method is used together with {@link #isVaultExpression(String)} so that the value string
     * is verified to be a valid vault string before this method is invoked.
     *
     * This method retrieves the raw secret from the vault and uses it in a try-with-resources construct to convert the secret
     * into a {@link WeakReference<String>}. This means the original raw secret is discarded (overridden) when this method
     * returns. Notice we don't have an {@link AutoCloseable} type for the {@code String} variant because strings can't be
     * overridden. We do, however, return the constructed string as a {@code WeakReference} to convey that the secret string
     * shouldn't be held longer than needed and to make sure it is available for the GC as soon as possible.
     *
     * @param provider the {@link VaultProvider} that will be used to retrieve the raw secret. Must not be {@code null}.
     * @param value a {@link String} that might be a vault string containing a vault entry key.
     * @return an {@link Optional} holding the weak reference to the constructed secret string. If the original raw secret
     *          was empty this method returns an empty {@code Optional}.
     * @throws {@link IllegalArgumentException} if the specified value doesn't follow the vault string format.
     */
    public static Optional<WeakReference<String>> getStringSecret(final VaultProvider provider, final String value) {
        try (VaultRawSecret rawSecret = getRawSecret(provider, value)) {
            if (!rawSecret.getRawSecret().isPresent()) {
                return Optional.empty();
            }
            ByteBuffer rawSecretBuffer = rawSecret.getRawSecret().get();
            return Optional.of(new WeakReference<>(StandardCharsets.UTF_8.decode(rawSecretBuffer).toString()));
        }
    }

    /**
     * Determines if the specified value is a vault string referencing a vault entry key or not. A vault entry key is specified
     * using the format {@code ${vault.entry_key}}. For example, the string {@code ${vault.smtp_secret}} identifies an entry
     * in the vault whose key is {@code smtp_secret}.
     *
     * @param value the {@code String} to be checked.
     * @return {@code true} if the given value contains a vault entry key following the {@code ${vault.entry_key}} format;
     *          {@code false} otherwise.
     */
    public static boolean isVaultExpression(final String value) {
        return getVaultEntryKey(value) != null;
    }

    /**
     * Obtains the vault entry key from the specified value when the value follows the {@code ${vault.entry_key}} format.
     * For example, calling this method with the {@code ${vault.smtp_secret}} argument results in the string {@code smtp_secret}
     * being returned.
     *
     * @param value a {@code String} that might contain a vault entry key.
     * @return the extracted entry key if the value follows the {@code ${vault.entry_key}} format; null otherwise.
     */
    public static String getVaultEntryKey(final String value) {
        if (value != null) {
            Matcher matcher = pattern.matcher(value);
            if (matcher.matches()) {
                return matcher.group(1);
            }
        }
        return null;
    }
}
