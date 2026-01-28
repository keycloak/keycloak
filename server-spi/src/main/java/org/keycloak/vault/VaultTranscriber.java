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

/**
 * A facade to the configured vault provider that exposes utility methods for obtaining the vault secrets in different
 * formats (such as {@link VaultRawSecret}, {@link VaultCharSecret} or {@link VaultStringSecret}).
 *
 * @see VaultRawSecret
 * @see VaultCharSecret
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public interface VaultTranscriber {

    /**
     * Obtains the raw secret from the vault that matches the entry in the specified value string. The value must follow
     * the format {@code ${vault.<KEY>}} where {@code <KEY>} identifies the entry in the vault. If the value doesn't follow
     * the vault expression format, it is assumed to be the secret itself and is encoded into a {@link VaultRawSecret}.
     * <p/>
     * The returned {@link VaultRawSecret} extends {@link AutoCloseable} and it is strongly recommended that it is used in
     * try-with-resources blocks to ensure the raw secret is overridden (destroyed) when the calling code is finished using
     * it.
     *
     * @param value a {@link String} that might be a vault expression containing a vault entry key.
     * @return a {@link VaultRawSecret} representing the secret that was read from the vault. If the specified value is not
     *         a vault expression then the returned secret is the value itself encoded as a {@link VaultRawSecret}.
     */
    VaultRawSecret getRawSecret(final String value);

    /**
     * Obtains the secret represented as a {@link VaultCharSecret} from the vault that matches the entry in the specified
     * value string. The value must follow the format {@code ${vault.<KEY>}} where {@code <KEY>} identifies the entry in
     * the vault. If the value doesn't follow the vault expression format, it is assumed to be the secret itself and is
     * encoded into a {@link VaultCharSecret}.
     * <p/>
     * The returned {@link VaultCharSecret} extends {@link AutoCloseable} and it is strongly recommended that it is used in
     * try-with-resources blocks to ensure the raw secret is overridden (destroyed) when the calling code is finished using
     * it.
     *
     * @param value a {@link String} that might be a vault expression containing a vault entry key.
     * @return a {@link VaultRawSecret} representing the secret that was read from the vault. If the specified value is not
     *         a vault expression then the returned secret is the value itself encoded as a {@link VaultRawSecret}.
     */
    VaultCharSecret getCharSecret(final String value);

    /**
     * Obtains the secret represented as a {@link String} from the vault that matches the entry in the specified value.
     * The value must follow the format {@code ${vault.<KEY>}} where {@code <KEY>} identifies the entry in the vault. If
     * the value doesn't follow the vault expression format, it is assumed to be the secret itself.
     * <p/>
     * Due to the immutable nature of strings and the way the JVM handles them internally, implementations that keep a reference
     * to the secret string might consider doing so using a {@link WeakReference} that can be cleared in the {@link AutoCloseable#close()}
     * method. Being immutable, such strings cannot be overridden (destroyed) by the implementation, but using a {@link WeakReference}
     * guarantees that at least no hard references to the secret are held by the implementation class itself (which would
     * prevent proper GC disposal of the secrets).
     * <p/>
     * <b>WARNING:</b> It is strongly recommended that callers of this method use the returned secret in try-with-resources
     * blocks and they should strive not to keep hard references to the enclosed secret string for any longer than necessary
     * so that the secret becomes available for GC as soon as possible. These measures help shorten the window of time when
     * the secret strings are readable from memory.
     *
     * @param value a {@link String} that might be a vault expression containing a vault entry key.
     * @return a {@link VaultStringSecret} representing the secret that was read from the vault. If the specified value is not
     *         a vault expression then the returned secret is the value itself encoded as a {@link VaultStringSecret}.
     */
    VaultStringSecret getStringSecret(final String value);

}