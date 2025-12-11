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

import java.security.Key;
import java.security.KeyManagementException;
import java.security.MessageDigest;
import java.security.cert.CertificateException;
import java.util.Iterator;
import java.util.function.Predicate;

import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.keys.PublicKeyLoader;
import org.keycloak.keys.PublicKeyStorageProvider;
import org.keycloak.rotation.KeyLocator;

/**
 * <p>KeyLocator that caches the keys into a PublicKeyStorageProvider.</p>
 *
 * @author rmartinc
 */
public class SamlMetadataKeyLocator implements KeyLocator {

    private final String modelKey;
    private final PublicKeyLoader loader;
    private final PublicKeyStorageProvider keyStorage;
    private final KeyUse use;

    public SamlMetadataKeyLocator(String modelKey, PublicKeyLoader loader, KeyUse use, PublicKeyStorageProvider keyStorage) {
        this.modelKey = modelKey;
        this.loader = loader;
        this.keyStorage = keyStorage;
        this.use = use;
    }

    @Override
    public Key getKey(String kid) throws KeyManagementException {
        if (kid == null) {
            return null;
        }
        // search the key by kid and reload if expired or null
        KeyWrapper keyWrapper = keyStorage.getFirstPublicKey(modelKey, sameKidPredicate(kid), loader);
        return keyWrapper != null? keyWrapper.getPublicKey() : null;
    }

    @Override
    public Key getKey(Key key) throws KeyManagementException {
        if (key == null) {
            return null;
        }
        // search the key and reload if expired or null
        KeyWrapper keyWrapper = keyStorage.getFirstPublicKey(modelKey, sameKeyPredicate(key), loader);
        return keyWrapper != null? keyWrapper.getPublicKey() : null;
    }

    @Override
    public void refreshKeyCache() {
        keyStorage.reloadKeys(modelKey, loader);
    }

    @Override
    public Iterator<Key> iterator() {
        // force a refresh if a certificate is expired?
        return keyStorage.getKeys(modelKey, loader)
                .stream()
                .filter(k -> isSameUse(k) && isValidCertificate(k))
                .map(KeyWrapper::getPublicKey)
                .iterator();
    }

    private Predicate<KeyWrapper> sameKidPredicate(String kid) {
        return keyWrapper -> isSameKid(keyWrapper, kid);
    }

    private boolean isSameKid(KeyWrapper keyWrapper, String kid) {
        String k = keyWrapper.getKid();
        if (k == null) {
            return false;
        }
        return k.equals(kid) && isSameUse(keyWrapper) && isValidCertificate(keyWrapper);
    }

    private Predicate<KeyWrapper> sameKeyPredicate(Key key) {
        return keyWrapper -> isSameKey(keyWrapper, key);
    }

    private boolean isSameKey(KeyWrapper keyWrapper, Key key) {
        Key k = keyWrapper.getPublicKey();
        if (k == null) {
            return false;
        }
        return isSameUse(keyWrapper)
                && key.getAlgorithm().equals(k.getAlgorithm())
                && MessageDigest.isEqual(k.getEncoded(), key.getEncoded())
                && isValidCertificate(keyWrapper);
    }

    private boolean isSameUse(KeyWrapper k) {
        if (k == null) {
            return false;
        }
        // if key use is null means it is valid for both uses
        return k.getUse() == null || k.getUse().equals(this.use);
    }

    private boolean isValidCertificate(KeyWrapper key) {
        if (key == null || key.getCertificate() == null) {
            return false;
        }
        try {
            key.getCertificate().checkValidity();
            return true;
        } catch (CertificateException e) {
            return false;
        }
    }
}
