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

import org.keycloak.provider.Provider;

/**
 * Provider responsible for unwrapping (decrypting) an encrypted symmetric key.
 *
 * <p>The private key material NEVER leaves the provider. For vault-backed
 * implementations, the encrypted bytes are sent to the vault which performs
 * the RSA decryption internally and returns the decrypted symmetric key bytes.</p>
 */
public interface SamlDecryptionProvider extends Provider {

    /**
     * Unwraps an encrypted symmetric key.
     *
     * @param algorithmUri the XML Encryption algorithm URI
     *                     (e.g., "http://www.w3.org/2001/04/xmlenc#rsa-oaep-mgf1p"
     *                     or "http://www.w3.org/2009/xmlenc11#rsa-oaep")
     * @param encryptedKeyBytes the encrypted symmetric key bytes (from EncryptedKey/CipherValue)
     * @param digestAlgorithm the digest algorithm URI from the EncryptionMethod (may be null,
     *                        defaults to SHA-1 for OAEP). Example:
     *                        "http://www.w3.org/2001/04/xmlenc#sha256"
     * @param mgfAlgorithm the mask generation function algorithm URI (may be null,
     *                     defaults to MGF1 with the digest algorithm). Example:
     *                     "http://www.w3.org/2009/xmlenc11#mgf1sha256"
     * @return the decrypted symmetric key bytes
     * @throws RuntimeException if the unwrapping operation fails
     */
    byte[] unwrapKey(String algorithmUri, byte[] encryptedKeyBytes, String digestAlgorithm, String mgfAlgorithm);
}
