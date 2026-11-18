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
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.keycloak.common.util.KeyUtils;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.keys.PublicKeyLoader;
import org.keycloak.keys.PublicKeyStorageProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.rotation.KeyLocator;
import org.keycloak.utils.StringUtil;

import org.jboss.logging.Logger;

/**
 * <p>KeyLocator that caches the keys into a PublicKeyStorageProvider.</p>
 *
 * @author rmartinc
 */
public class SamlMetadataKeyLocator implements KeyLocator {

    private static final Logger logger = Logger.getLogger(SamlMetadataKeyLocator.class);

    // the key names are sent by the other party, limit what is written to the log
    private static final int MAX_LOGGED_KEY_NAME_LENGTH = 128;

    private final String modelKey;
    private final PublicKeyLoader loader;
    private final Function<KeycloakSession, PublicKeyLoader> backgroundLoader;
    private final PublicKeyStorageProvider keyStorage;
    private final KeyUse use;

    public SamlMetadataKeyLocator(String modelKey, PublicKeyLoader loader, KeyUse use, PublicKeyStorageProvider keyStorage) {
        this(modelKey, loader, null, use, keyStorage);
    }

    /**
     * Creates a locator that does not wait for the metadata if valid keys are cached. If a key that is not
     * in the cache is requested, the key is not found and the keys are reloaded in the background.
     *
     * @param modelKey The model key
     * @param loader The loader used when there are no valid keys in the cache
     * @param backgroundLoader Creates the loader used to reload the keys in the background, null to always
     * reload the keys in the thread that requests them
     * @param use The use of the keys
     * @param keyStorage The storage that caches the keys
     */
    public SamlMetadataKeyLocator(String modelKey, PublicKeyLoader loader, Function<KeycloakSession, PublicKeyLoader> backgroundLoader,
            KeyUse use, PublicKeyStorageProvider keyStorage) {
        this.modelKey = modelKey;
        this.loader = loader;
        this.backgroundLoader = backgroundLoader;
        this.keyStorage = keyStorage;
        this.use = use;
    }

    @Override
    public Key getKey(String kid) throws KeyManagementException {
        if (kid == null) {
            return null;
        }
        if (backgroundLoader != null) {
            return getKeyReloadingInBackground(sameKidPredicate(kid), () -> "with name '" + sanitizeKeyName(kid) + "'");
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
        if (backgroundLoader != null) {
            return getKeyReloadingInBackground(sameKeyPredicate(key), () -> "with SHA-256 thumbprint '" + thumbprint(key) + "'");
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
        return getKeys()
                .stream()
                .filter(k -> isSameUse(k) && isValidCertificate(k))
                .map(KeyWrapper::getPublicKey)
                .iterator();
    }

    private List<KeyWrapper> getKeys() {
        return backgroundLoader != null
                ? keyStorage.getKeys(modelKey, loader, backgroundLoader)
                : keyStorage.getKeys(modelKey, loader);
    }

    private Key getKeyReloadingInBackground(Predicate<KeyWrapper> predicate, Supplier<String> requestedKey) {
        // search the key without waiting for a reload if there are valid keys
        Key key = getKeys().stream().filter(predicate).map(KeyWrapper::getPublicKey).findFirst().orElse(null);
        if (key == null) {
            // the key can be a new one, the reload does not block as the requested key is not trusted
            if (keyStorage.reloadKeysInBackground(modelKey, backgroundLoader)) {
                logger.warnf("The key %s was requested but it is not in the keys for model key '%s'. Reloading the keys from the metadata in the background.",
                        requestedKey.get(), modelKey);
            } else if (logger.isDebugEnabled()) {
                logger.debugf("The key %s was requested but it is not in the keys for model key '%s'.", requestedKey.get(), modelKey);
            }
        }
        return key;
    }

    static String sanitizeKeyName(String kid) {
        return kid.length() > MAX_LOGGED_KEY_NAME_LENGTH
                ? StringUtil.removeControlCharacters(kid.substring(0, MAX_LOGGED_KEY_NAME_LENGTH)) + "..."
                : StringUtil.removeControlCharacters(kid);
    }

    private static String thumbprint(Key key) {
        return key.getEncoded() != null ? KeyUtils.createKeyId(key) : null;
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
