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

package org.keycloak.crypto;

import java.security.Key;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderFactory;

/**
 * Utility class for common cryptographic operations and algorithm discovery.
 *
 * @author <a href="https://github.com/forkimenjeckayang">Forkim Akwichek</a>
 */
public class CryptoUtils {

    /**
     * Returns the supported asymmetric signature algorithms.
     * This method discovers all available SignatureProvider implementations and filters
     * for those that support asymmetric algorithms (RSA, EC, EdDSA, etc.).
     *
     * @param session The Keycloak session
     * @return List of asymmetric signature algorithm names
     */
    public static List<String> getSupportedAsymmetricSignatureAlgorithms(KeycloakSession session) {
        return session.getKeycloakSessionFactory().getProviderFactoriesStream(SignatureProvider.class)
                .map(ProviderFactory::getId)
                .map(algorithm -> new AbstractMap.SimpleEntry<>(algorithm, session.getProvider(SignatureProvider.class, algorithm)))
                .filter(entry -> entry.getValue() != null)
                .filter(entry -> entry.getValue().isAsymmetricAlgorithm())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * Returns the supported asymmetric encryption algorithms.
     * This method discovers all available Keys and filters
     * for those that use asymmetric algorithms (RSA, EC, EdDSA, etc.).
     *
     * @param session The Keycloak session
     * @return List of asymmetric signature algorithm names
     */
    public static List<String> getSupportedAsymmetricEncryptionAlgorithms(KeycloakSession session) {
        return session.keys()
                .getKeysStream(session.getContext().getRealm())
                .filter(key -> KeyUse.ENC.equals(key.getUse()))
                .filter(key -> {
                    Key k = key.getPublicKey();
                    // asymmetric keys will have PublicKey/PrivateKey instead of SecretKey
                    return k instanceof PublicKey || key.getPrivateKey() instanceof PrivateKey;
                })
                .map(KeyWrapper::getAlgorithm)
                .filter(algorithm -> algorithm != null && !algorithm.isEmpty())
                .distinct()
                .toList();
    }
}
