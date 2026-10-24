/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.keys;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.stream.Stream;

import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.util.KeyUtils;
import org.keycloak.component.ComponentModel;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.KeyStatus;
import org.keycloak.crypto.KeyType;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.models.RealmModel;

public class GeneratedMLDSAKeyProvider implements KeyProvider {

    private final KeyWrapper key;

    public GeneratedMLDSAKeyProvider(RealmModel realm, ComponentModel model) {
        if (model.hasNote(KeyWrapper.class.getName())) {
            key = model.getNote(KeyWrapper.class.getName());
        } else {
            key = loadKey(realm, model);
            model.setNote(KeyWrapper.class.getName(), key);
        }
    }

    private static KeyWrapper loadKey(RealmModel realm, ComponentModel model) {
        String algorithm = model.get(GeneratedMLDSAKeyProviderFactory.MLDSA_ALGORITHM_KEY,
                GeneratedMLDSAKeyProviderFactory.DEFAULT_MLDSA_ALGORITHM);

        try {
            KeyFactory keyFactory = CryptoIntegration.getProvider().getKeyFactory(algorithm);
            PrivateKey privateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(
                    model.get(GeneratedMLDSAKeyProviderFactory.MLDSA_PRIVATE_KEY_KEY))));
            PublicKey publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(
                    model.get(GeneratedMLDSAKeyProviderFactory.MLDSA_PUBLIC_KEY_KEY))));

            if (!algorithm.equals(privateKey.getAlgorithm()) || !algorithm.equals(publicKey.getAlgorithm())) {
                throw new IllegalArgumentException("Stored key does not match " + algorithm);
            }

            KeyWrapper key = new KeyWrapper();
            key.setProviderId(model.getId());
            key.setProviderPriority(model.get(Attributes.PRIORITY_KEY, 0L));
            key.setKid(KeyUtils.createKeyId(publicKey));
            key.setUse(KeyUse.SIG);
            key.setType(KeyType.AKP);
            key.setAlgorithm(algorithm);
            key.setStatus(KeyStatus.from(model.get(Attributes.ACTIVE_KEY, true), model.get(Attributes.ENABLED_KEY, true)));
            key.setPrivateKey(privateKey);
            key.setPublicKey(publicKey);
            return key;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load ML-DSA key for realm " + realm.getName(), e);
        }
    }

    @Override
    public Stream<KeyWrapper> getKeysStream() {
        return Stream.of(key);
    }
}
