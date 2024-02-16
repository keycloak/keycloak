/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.protocol.saml;



import org.apache.xml.security.encryption.EncryptedData;
import org.apache.xml.security.encryption.EncryptedKey;
import org.apache.xml.security.encryption.EncryptionMethod;
import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.keys.KeyInfo;
import org.apache.xml.security.keys.content.KeyName;
import org.keycloak.common.util.DerUtils;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.saml.processing.core.util.XMLEncryptionUtil;

import java.security.Key;
import java.security.PrivateKey;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This implementation locates the decryption keys within realm keys.
 * It filters realm keys based on algorithm provided within {@link EncryptedData}
 *
 * Example of encrypted data:
 * <pre>
 * {@code
 * <xenc:EncryptedData Type="http://www.w3.org/2001/04/xmlenc#Element">
 *     <xenc:EncryptionMethod Algorithm="http://www.w3.org/2001/04/xmlenc#aes128-cbc"/>
 *     <ds:KeyInfo>
 *         <xenc:EncryptedKey>
 *             <xenc:EncryptionMethod Algorithm="http://www.w3.org/2001/04/xmlenc#rsa-oaep-mgf1p"/>
 *             <xenc:CipherData>
 *                 <xenc:CipherValue>
 *                     .....
 *                 </xenc:CipherValue>
 *             </xenc:CipherData>
 *         </xenc:EncryptedKey>
 *     </ds:KeyInfo>
 *     <xenc:CipherData>
 *         <xenc:CipherValue>
 *             ...
 *         </xenc:CipherValue>
 *     </xenc:CipherData>
 * </xenc:EncryptedData>
 * }
 * </pre>
 *
 */
public class SAMLDecryptionKeysLocator implements XMLEncryptionUtil.DecryptionKeyLocator {

    private final KeycloakSession session;
    private final RealmModel realm;
    private final String requestedAlgorithm;

    public SAMLDecryptionKeysLocator(KeycloakSession session, RealmModel realm, String requestedAlgorithm) {
        this.session = session;
        this.realm = realm;
        this.requestedAlgorithm = requestedAlgorithm;
    }

    private List<String> getKeyNames(KeyInfo keyInfo) {
        List<String> keyNames = new LinkedList<>();

        try {
            for (int i = 0; i < keyInfo.lengthKeyName(); i++) {
                KeyName keyName = keyInfo.itemKeyName(i);
                if (keyName != null) {
                    keyNames.add(keyName.getKeyName());
                }
            }
        } catch (XMLSecurityException e) {
            throw new IllegalStateException("Cannot load keyNames from document", e);
        }

        return keyNames;
    }

    private Predicate<KeyWrapper> hasMatchingAlgorithm(String algorithm) {
        SAMLEncryptionAlgorithms usedAlgorithm = SAMLEncryptionAlgorithms.forXMLEncIdentifier(algorithm);

        if (usedAlgorithm == null) {
            throw new IllegalStateException("Keycloak does not support encryption keys for given algorithm: " + algorithm);
        }

        return keyWrapper -> Objects.equals(keyWrapper.getAlgorithmOrDefault(), usedAlgorithm.getKeycloakIdentifier());
    }

    @Override
    public List<PrivateKey> getKeys(EncryptedData encryptedData) {
        // Check encryptedData contains keyinfo
        KeyInfo keyInfo = encryptedData.getKeyInfo();
        if (keyInfo == null) {
            throw new IllegalStateException("EncryptedData does not contain KeyInfo");
        }

        Stream<KeyWrapper> keysStream = session.keys().getKeysStream(realm)
                .filter(key -> key.getStatus().isEnabled() && KeyUse.ENC.equals(key.getUse()));

        if (requestedAlgorithm != null && !requestedAlgorithm.trim().isEmpty()) {
            keysStream = keysStream.filter(keyWrapper -> Objects.equals(keyWrapper.getAlgorithmOrDefault(), requestedAlgorithm));
        }

        // If encryptedData contains keyName we will use only for keys with given kid
        if (keyInfo.containsKeyName()) {
            List<String> keyNames = getKeyNames(keyInfo);
            keysStream = keysStream.filter(keyWrapper -> keyNames.contains(keyWrapper.getKid()));
        }

        // Look for algorithm used inside encryptedData and allow only keys generated for specific algorithm
        try {
            EncryptedKey encryptedKey = keyInfo.itemEncryptedKey(0);
            if (encryptedKey != null) {
                EncryptionMethod encryptionMethod = encryptedKey.getEncryptionMethod();

                if (encryptionMethod == null) {
                    throw new IllegalArgumentException("KeyInfo does not contain encryption method");
                }

                String algorithm = encryptionMethod.getAlgorithm();
                if (algorithm == null) {
                    throw new IllegalArgumentException("Not able to find algorithm for given encryption method");
                }
                keysStream = keysStream.filter(hasMatchingAlgorithm(algorithm));
            }
        } catch (XMLSecurityException e) {
            throw new IllegalArgumentException("EncryptedData does not contain KeyInfo ", e);
        }

        // Map keys to PrivateKey
        return keysStream
                .map(KeyWrapper::getPrivateKey)
                .filter(Objects::nonNull)
                .map(Key::getEncoded)
                .map(encoded -> {
                    try {
                        return DerUtils.decodePrivateKey(encoded);
                    } catch (Exception e) {
                        throw new RuntimeException("Could not decode private key.", e);
                    }
                })
                .collect(Collectors.toList());
    }
}
