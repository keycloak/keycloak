/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.keys.loader;

import java.util.Collections;

import org.keycloak.common.util.PemUtils;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.JavaAlgorithm;
import org.keycloak.crypto.KeyType;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.crypto.PublicKeysWrapper;
import org.keycloak.keys.PublicKeyLoader;

import org.jboss.logging.Logger;

/**
 *
 * @author hmlnarik
 */
public class HardcodedPublicKeyLoader implements PublicKeyLoader {

    private static final Logger logger = Logger.getLogger(HardcodedPublicKeyLoader.class);

    private final KeyWrapper keyWrapper;

    public HardcodedPublicKeyLoader(String kid, String encodedKey, String algorithm) {
        if (encodedKey != null && !encodedKey.trim().isEmpty()) {
            KeyWrapper kw = new KeyWrapper();
            kw.setKid(kid);
            kw.setUse(KeyUse.SIG);
            kw.setAlgorithm(algorithm);
            // depending the algorithm load the correct key from the encoded string
            if (JavaAlgorithm.isRSAJavaAlgorithm(algorithm)) {
                kw.setType(KeyType.RSA);
                kw.setPublicKey(PemUtils.decodePublicKey(encodedKey, KeyType.RSA));
            } else if (JavaAlgorithm.isECJavaAlgorithm(algorithm)) {
                kw.setType(KeyType.EC);
                kw.setPublicKey(PemUtils.decodePublicKey(encodedKey, KeyType.EC));
            } else if (JavaAlgorithm.isEddsaJavaAlgorithm(algorithm)) {
                kw.setType(KeyType.OKP);
                kw.setPublicKey(PemUtils.decodePublicKey(encodedKey, Algorithm.EdDSA));
                kw.setCurve(kw.getPublicKey().getAlgorithm());
            } else {
                logger.warnf("Unrecognized or invalid algorithm %s for hardcoded public key", algorithm);
                kw = null;
            }
            keyWrapper = kw;
        } else {
            keyWrapper = null;
        }
    }

    @Override
    public PublicKeysWrapper loadKeys() throws Exception {
        return keyWrapper != null
                ? new PublicKeysWrapper(Collections.singletonList(getSavedPublicKey()))
                : PublicKeysWrapper.EMPTY;
    }

    protected KeyWrapper getSavedPublicKey() {
        return keyWrapper;
    }
}
