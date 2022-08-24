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

import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.KeyUtils;
import org.keycloak.common.util.PemUtils;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.JavaAlgorithm;
import org.keycloak.crypto.KeyType;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.keys.PublicKeyLoader;

import java.util.Collections;
import java.util.Map;

/**
 *
 * @author hmlnarik
 */
public class HardcodedPublicKeyLoader implements PublicKeyLoader {

    private final KeyWrapper keyWrapper;

    public HardcodedPublicKeyLoader(String kid, String pem) {
        this(kid, pem, Algorithm.RS256);
    }

    public HardcodedPublicKeyLoader(String kid, String encodedKey, String algorithm) {
        if (encodedKey != null && !encodedKey.trim().isEmpty()) {
            keyWrapper = new KeyWrapper();
            keyWrapper.setKid(kid);
            keyWrapper.setUse(KeyUse.SIG);
            // depending the algorithm load the correct key from the encoded string
            if (JavaAlgorithm.isRSAJavaAlgorithm(algorithm)) {
                keyWrapper.setType(KeyType.RSA);
                keyWrapper.setPublicKey(PemUtils.decodePublicKey(encodedKey, KeyType.RSA));
            } else if (JavaAlgorithm.isECJavaAlgorithm(algorithm)) {
                keyWrapper.setType(KeyType.EC);
                keyWrapper.setPublicKey(PemUtils.decodePublicKey(encodedKey, KeyType.EC));
            } else if (JavaAlgorithm.isHMACJavaAlgorithm(algorithm)) {
                keyWrapper.setType(KeyType.OCT);
                keyWrapper.setSecretKey(KeyUtils.loadSecretKey(Base64Url.decode(encodedKey), algorithm));
            }
        } else {
            keyWrapper = null;
        }
    }

    @Override
    public Map<String, KeyWrapper> loadKeys() throws Exception {
        return keyWrapper != null
                ? Collections.unmodifiableMap(Collections.singletonMap(keyWrapper.getKid(), getSavedPublicKey()))
                : Collections.emptyMap();
    }

    protected KeyWrapper getSavedPublicKey() {
        return keyWrapper;
    }
}
