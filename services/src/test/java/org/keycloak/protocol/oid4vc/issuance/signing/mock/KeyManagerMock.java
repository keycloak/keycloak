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

package org.keycloak.protocol.oid4vc.issuance.signing.mock;

import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.keys.RsaKeyMetadata;
import org.keycloak.keys.SecretKeyMetadata;
import org.keycloak.models.KeyManager;
import org.keycloak.models.RealmModel;

import javax.crypto.SecretKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class KeyManagerMock implements KeyManager {

    private final Map<KeyMapKey, KeyWrapper> keyMap = new HashMap<>();

    public void addKey(RealmModel realm, String kid, KeyUse use, String algorithm, KeyWrapper keyWrapper) {
        keyMap.put(new KeyMapKey(realm, kid, use, algorithm), keyWrapper);
    }

    @Override
    public KeyWrapper getActiveKey(RealmModel realm, KeyUse use, String algorithm) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public KeyWrapper getKey(RealmModel realm, String kid, KeyUse use, String algorithm) {
        try {
            return keyMap.get(new KeyMapKey(realm, kid, use, algorithm));
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public Stream<KeyWrapper> getKeysStream(RealmModel realm) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public Stream<KeyWrapper> getKeysStream(RealmModel realm, KeyUse use, String algorithm) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public ActiveRsaKey getActiveRsaKey(RealmModel realm) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public PublicKey getRsaPublicKey(RealmModel realm, String kid) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public Certificate getRsaCertificate(RealmModel realm, String kid) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public List<RsaKeyMetadata> getRsaKeys(RealmModel realm) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public ActiveHmacKey getActiveHmacKey(RealmModel realm) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public SecretKey getHmacSecretKey(RealmModel realm, String kid) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public List<SecretKeyMetadata> getHmacKeys(RealmModel realm) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public ActiveAesKey getActiveAesKey(RealmModel realm) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public SecretKey getAesSecretKey(RealmModel realm, String kid) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    @Override
    public List<SecretKeyMetadata> getAesKeys(RealmModel realm) {
        throw new UnsupportedOperationException("Not supported by the mock.");
    }

    record KeyMapKey(RealmModel realmModel, String kid, KeyUse keyUse, String algorithm) {
    }
}
