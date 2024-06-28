/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.rotation;

import java.security.Key;
import java.security.KeyException;
import java.security.KeyManagementException;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import javax.xml.crypto.XMLStructure;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyName;
import javax.xml.crypto.dsig.keyinfo.KeyValue;
import javax.xml.crypto.dsig.keyinfo.X509Data;

/**
 * This interface defines a method for obtaining a security key by ID.
 * <p>
 * If the {@code KeyLocator} implementor wants to make all its keys available for iteration,
 * it should implement {@link Iterable}&lt;{@code T extends }{@link Key}&gt; interface.
 * The base {@code KeyLocator} does not extend this interface to enable {@code KeyLocators}
 * that do not support listing their keys.
 *
 * @author <a href="mailto:hmlnarik@redhat.com">Hynek Mlnařík</a>
 */
public interface KeyLocator extends Iterable<Key> {

    /**
     * Returns a key with a particular ID.
     * @param kid Key ID
     * @return key, which should be used for verify signature on given "input"
     * @throws KeyManagementException
     */
    Key getKey(String kid) throws KeyManagementException;

    /**
     * Method that checks if the key passed is inside the locator.
     * @param key The key to search
     * @return The same key or null if it's not in the locator
     * @throws KeyManagementException
     */
    default Key getKey(Key key) throws KeyManagementException {
        if (key == null) {
            return null;
        }
        for (Key k : this) {
            if (k.getAlgorithm().equals(key.getAlgorithm()) && MessageDigest.isEqual(k.getEncoded(), key.getEncoded())) {
                return key;
            }
        }
        return null;
    }

    /**
     * Returns the key in the locator that is represented by the KeyInfo
     * dsig structure. The default implementation just iterates and returns
     * the first KeyName, X509Data or PublicKey that is in the locator.
     * @param info The KeyInfo to search
     * @return The key found or null
     * @throws KeyManagementException
     */
    default Key getKey(KeyInfo info) throws KeyManagementException {
        if (info == null) {
            return null;
        }
        Key key = null;
        for (XMLStructure xs : (List<XMLStructure>) info.getContent()) {
            if (xs instanceof KeyName) {
                key = getKey(((KeyName) xs).getName());
            } else if (xs instanceof X509Data) {
                for (Object content : ((X509Data) xs).getContent()) {
                    if (content instanceof X509Certificate) {
                        key = getKey(((X509Certificate) content).getPublicKey());
                        if (key != null) {
                            return key;
                        }
                        // only the first X509Certificate is the signer
                        // the rest are just part of the chain
                        break;
                    }
                }
            } else if (xs instanceof KeyValue) {
                try {
                    key = getKey(((KeyValue) xs).getPublicKey());
                } catch (KeyException e) {
                    throw new KeyManagementException(e);
                }
            }
            if (key != null) {
                return key;
            }
        }
        return null;
    }

    /**
     * If this key locator caches keys in any way, forces this cache cleanup
     * and refreshing the keys.
     */
    void refreshKeyCache();

    /**
     * Helper class that facilitates the hash of a Key to be located easier.
     */
    public static class KeyHash {
        private final Key key;
        private final int keyHash;

        public KeyHash(Key key) {
            this.key = key;
            this.keyHash = Arrays.hashCode(key.getEncoded());
        }

        @Override
        public int hashCode() {
            return keyHash;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof KeyHash) {
                KeyHash other = (KeyHash) o;
                return keyHash == other.keyHash &&
                        key.getAlgorithm().equals(other.key.getAlgorithm()) &&
                        MessageDigest.isEqual(key.getEncoded(), other.key.getEncoded());
            }
            return false;
        }
    }
}
