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
package org.keycloak.keys;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import org.keycloak.component.ComponentModel;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.models.RealmModel;

import org.jboss.logging.Logger;

public class GeneratedMldsaKeyProvider extends AbstractMldsaKeyProvider {
    private static final Logger logger = Logger.getLogger(GeneratedMldsaKeyProvider.class);

    public GeneratedMldsaKeyProvider(RealmModel realm, ComponentModel model) {
        super(realm, model);
    }

    @Override
    protected KeyWrapper loadKey(RealmModel realm, ComponentModel model) {
        String privateMldsaKeyBase64UrlEncoded = model.getConfig().getFirst(Attributes.PRIVATE_KEY_KEY);
        String publicMldsaKeyBase64UrlEncoded = model.getConfig().getFirst(Attributes.CERTIFICATE_KEY);

        byte[] publicEncoded = Base64.getUrlDecoder().decode(publicMldsaKeyBase64UrlEncoded);
        byte[] privateEncoded = Base64.getUrlDecoder().decode(privateMldsaKeyBase64UrlEncoded);

        try {
            KeyFactory kf = KeyFactory.getInstance(algorithm);

            X509EncodedKeySpec pubSpec = new X509EncodedKeySpec(publicEncoded);
            PublicKey publicKey = kf.generatePublic(pubSpec);

            PKCS8EncodedKeySpec privSpec = new PKCS8EncodedKeySpec(privateEncoded);
            PrivateKey privateKey = kf.generatePrivate(privSpec);

            KeyPair keyPair = new KeyPair(publicKey, privateKey);
            return createKeyWrapper(keyPair);
        } catch (Exception e) {
            logger.warnf("Exception at loadKey. %s", e.toString());
            return null;
        }
    }
}
