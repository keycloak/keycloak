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

import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.component.ComponentModel;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.models.RealmModel;
import org.jboss.logging.Logger;

public class GeneratedMlDsaKeyProvider extends AbstractMlDsaKeyProvider {
    private static final Logger logger = Logger.getLogger(GeneratedMlDsaKeyProvider.class);

    public GeneratedMlDsaKeyProvider(RealmModel realm, ComponentModel model) {
        super(realm, model);
    }

    @Override
    protected KeyWrapper loadKey(RealmModel realm, ComponentModel model) {
        String privateKeyBase64 = model.getConfig().getFirst(GeneratedMlDsaKeyProviderFactory.MLDSA_PRIVATE_KEY_KEY);
        String publicKeyBase64 = model.getConfig().getFirst(GeneratedMlDsaKeyProviderFactory.MLDSA_PUBLIC_KEY_KEY);
        String algorithm = model.getConfig().getFirst(GeneratedMlDsaKeyProviderFactory.MLDSA_ALGORITHM_KEY);

        try {
            PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(Base64.getMimeDecoder().decode(privateKeyBase64));
            KeyFactory kf = CryptoIntegration.getProvider().getKeyFactory(algorithm);
            PrivateKey decodedPrivateKey = kf.generatePrivate(privateKeySpec);

            X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(Base64.getMimeDecoder().decode(publicKeyBase64));
            PublicKey decodedPublicKey = kf.generatePublic(publicKeySpec);

            KeyPair keyPair = new KeyPair(decodedPublicKey, decodedPrivateKey);

            return createKeyWrapper(keyPair, algorithm);
        } catch (Exception e) {
            logger.warnf("Exception loading ML-DSA public key. %s", e.toString());
            return null;
        }
    }
}
