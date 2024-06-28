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
 * Default {@link VaultTranscriber} implementation that uses the configured {@link VaultProvider} to obtain raw secrets
 * and convert them into other types. By default, the {@link VaultProvider} provides raw secrets through a {@link ByteBuffer}.
 * This class offers methods to convert the raw secrets into other types (such as {@link VaultCharSecret} or {@link WeakReference<String>}).
 *
 * @see VaultRawSecret
 * @see VaultCharSecret
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class DefaultVaultTranscriber implements VaultTranscriber {

    private static final Pattern pattern = Pattern.compile("^\\$\\{vault\\.(.+?)}$");

    private final VaultProvider provider;

    public DefaultVaultTranscriber(final VaultProvider provider) {
        if (provider == null) {
            this.provider = new VaultProvider() {
                @Override
                public VaultRawSecret obtainSecret(String vaultSecretId) {
                    return DefaultVaultRawSecret.forBuffer(null);
                }

                @Override
                public void close() {
                }
            };
        } else {
            this.provider = provider;
        }
    }

    @Override
    public VaultRawSecret getRawSecret(final String value) {
        String entryId = this.getVaultEntryKey(value);
        if (entryId != null) {
            // we have a valid ${vault.<KEY>} string, use the provider to retrieve the entry.
            return this.provider.obtainSecret(entryId);
        } else {
            // not a vault expression - encode the value itself as a byte buffer.
            ByteBuffer buffer = value != null ? ByteBuffer.wrap(value.getBytes(StandardCharsets.UTF_8)) : null;
            return DefaultVaultRawSecret.forBuffer(Optional.ofNullable(buffer));
        }
    }

    @Override
    public VaultCharSecret getCharSecret(final String value) {
        // obtain the raw secret and convert it into a char secret.
        try (VaultRawSecret rawSecret = this.getRawSecret(value)) {
            if (!rawSecret.get().isPresent()) {
                return DefaultVaultCharSecret.forBuffer(Optional.empty());
            }
            ByteBuffer rawSecretBuffer = rawSecret.get().get();
            CharBuffer charSecretBuffer = StandardCharsets.UTF_8.decode(rawSecretBuffer);
            return DefaultVaultCharSecret.forBuffer(Optional.of(charSecretBuffer));
        }
    }

    @Override
    public VaultStringSecret getStringSecret(final String value) {
        // obtain the raw secret and convert it into a string string.
        try (VaultRawSecret rawSecret = this.getRawSecret(value)) {
            if (!rawSecret.get().isPresent()) {
                return DefaultVaultStringSecret.forString(Optional.empty());
            }
            ByteBuffer rawSecretBuffer = rawSecret.get().get();
            return DefaultVaultStringSecret.forString(Optional.of(StandardCharsets.UTF_8.decode(rawSecretBuffer).toString()));
        }
    }

    /**
     * Obtains the vault entry key from the specified value if the value is a valid {@code ${vault.<KEY>}} expression.
     * For example, calling this method with the {@code ${vault.smtp_secret}} argument results in the string {@code smtp_secret}
     * being returned.
     *
     * @param value a {@code String} that might contain a vault entry key.
     * @return the extracted entry key if the value follows the {@code ${vault.<KEY>}} format; null otherwise.
     */
    private String getVaultEntryKey(final String value) {
        if (value != null) {
            Matcher matcher = pattern.matcher(value);
            if (matcher.matches()) {
                return matcher.group(1);
            }
        }
        return null;
    }
}
