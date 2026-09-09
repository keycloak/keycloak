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

import org.apache.xml.security.encryption.XMLCipher;

import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import java.security.PrivateKey;
import java.security.spec.MGF1ParameterSpec;
import java.util.Collections;
import java.util.List;

/**
 * Default implementation of {@link SamlDecryptionProvider} that performs local
 * key unwrapping using one or more {@link PrivateKey} instances from JKS/PEM configuration.
 *
 * <p>This provider uses the standard JCE {@link Cipher} in {@code UNWRAP_MODE}
 * to decrypt the encrypted symmetric key bytes. When multiple keys are provided,
 * it tries each in order until one succeeds (supporting key rotation scenarios).</p>
 *
 * <p>For RSA-OAEP algorithms, the provider supports configurable digest and MGF
 * algorithms as specified in XML Encryption 1.1.</p>
 */
public class DefaultSamlDecryptionProvider implements SamlDecryptionProvider {

    private final List<PrivateKey> privateKeys;

    public DefaultSamlDecryptionProvider(List<PrivateKey> privateKeys) {
        if (privateKeys == null || privateKeys.isEmpty()) {
            throw new RuntimeException("Decryption private keys must not be null or empty");
        }
        this.privateKeys = Collections.unmodifiableList(privateKeys);
    }

    @Override
    public byte[] unwrapKey(String algorithmUri, byte[] encryptedKeyBytes, String digestAlgorithm, String mgfAlgorithm) {
        if (algorithmUri == null || algorithmUri.isEmpty()) {
            throw new RuntimeException("Algorithm URI must not be null or empty");
        }
        if (encryptedKeyBytes == null || encryptedKeyBytes.length == 0) {
            throw new RuntimeException("Encrypted key bytes must not be null or empty");
        }

        Exception lastException = null;

        for (PrivateKey privateKey : privateKeys) {
            try {
                Cipher cipher = createCipher(algorithmUri, privateKey, digestAlgorithm, mgfAlgorithm);
                java.security.Key unwrappedKey = cipher.unwrap(
                    encryptedKeyBytes, "AES", Cipher.SECRET_KEY);
                return unwrappedKey.getEncoded();
            } catch (Exception e) {
                lastException = e;
            }
        }

        throw new RuntimeException(
            "Failed to unwrap key with algorithm " + algorithmUri
                + " using " + privateKeys.size() + " available key(s): "
                + (lastException != null ? lastException.getMessage() : "unknown error"),
            lastException);
    }

    @Override
    public void close() {
        // No resources to release for local key unwrapping
    }

    private Cipher createCipher(String algorithmUri, PrivateKey privateKey, String digestAlgorithm, String mgfAlgorithm) throws Exception {
        switch (algorithmUri) {
            case XMLCipher.RSA_OAEP:
            case XMLCipher.RSA_OAEP_11: {
                // Build OAEPParameterSpec from the digest and MGF algorithms
                String digestName = mapDigestUri(digestAlgorithm);
                MGF1ParameterSpec mgfSpec = mapMgfUri(mgfAlgorithm, digestName);
                OAEPParameterSpec oaepSpec = new OAEPParameterSpec(
                        digestName, "MGF1", mgfSpec, PSource.PSpecified.DEFAULT);
                Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPPadding");
                cipher.init(Cipher.UNWRAP_MODE, privateKey, oaepSpec);
                return cipher;
            }
            case XMLCipher.RSA_v1dot5: {
                Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                cipher.init(Cipher.UNWRAP_MODE, privateKey);
                return cipher;
            }
            default:
                throw new RuntimeException(
                    "Unsupported key encryption algorithm: " + algorithmUri);
        }
    }

    /**
     * Maps an XML Encryption digest algorithm URI to a JCE digest name.
     */
    private String mapDigestUri(String digestUri) {
        if (digestUri == null || digestUri.isEmpty()) {
            return "SHA-1"; // Default for RSA-OAEP when not specified
        }
        switch (digestUri) {
            case "http://www.w3.org/2000/09/xmldsig#sha1":
                return "SHA-1";
            case "http://www.w3.org/2001/04/xmlenc#sha256":
                return "SHA-256";
            case "http://www.w3.org/2001/04/xmldsig-more#sha384":
                return "SHA-384";
            case "http://www.w3.org/2001/04/xmlenc#sha512":
                return "SHA-512";
            default:
                return "SHA-1";
        }
    }

    /**
     * Maps an XML Encryption MGF algorithm URI to a JCE MGF1ParameterSpec.
     * If not specified, defaults to using the same digest as the OAEP digest.
     */
    private MGF1ParameterSpec mapMgfUri(String mgfUri, String defaultDigest) {
        if (mgfUri == null || mgfUri.isEmpty()) {
            // Default: MGF1 with the same digest as OAEP
            return new MGF1ParameterSpec(defaultDigest);
        }
        switch (mgfUri) {
            case "http://www.w3.org/2009/xmlenc11#mgf1sha1":
                return MGF1ParameterSpec.SHA1;
            case "http://www.w3.org/2009/xmlenc11#mgf1sha256":
                return MGF1ParameterSpec.SHA256;
            case "http://www.w3.org/2009/xmlenc11#mgf1sha384":
                return new MGF1ParameterSpec("SHA-384");
            case "http://www.w3.org/2009/xmlenc11#mgf1sha512":
                return MGF1ParameterSpec.SHA512;
            default:
                return new MGF1ParameterSpec(defaultDigest);
        }
    }
}
