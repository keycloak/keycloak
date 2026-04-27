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

package org.keycloak.keys;

import java.util.List;
import java.util.function.Predicate;

import org.keycloak.crypto.KeyWrapper;
import org.keycloak.provider.Provider;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface PublicKeyStorageProvider extends Provider {


    /**
     * Get public key to verify messages signed by particular client. Used for example during JWT client authentication
     *
     * @param modelKey
     * @param kid
     * @param algorithm The returned key must match this algorithm (unless the algorithm is not set in the JWK)
     * @param loader
     * @return
     */
	KeyWrapper getPublicKey(String modelKey, String kid, String algorithm, PublicKeyLoader loader);

    /**
     * Get first found public key to verify messages signed by particular client having several public keys. Used for example during JWT client authentication
     * or to encrypt content encryption key (CEK) by particular client. Used for example during encrypting a token in JWE
     * 
     * @param modelKey
     * @param algorithm
     * @param loader
     * @return
     */
    KeyWrapper getFirstPublicKey(String modelKey, String algorithm, PublicKeyLoader loader);

    /**
     * Get the first public key that matches the predicate. Used by SAML when fetching
     * a key via the metadata entity descriptor url.
     *
     * @param modelKey
     * @param predicate
     * @param loader
     * @return The key or null
     */
    KeyWrapper getFirstPublicKey(String modelKey, Predicate<KeyWrapper> predicate, PublicKeyLoader loader);

    /**
     * Getter for all the keys in the model key.
     *
     * @param modelKey
     * @param loader
     * @return
     */
    List<KeyWrapper> getKeys(String modelKey, PublicKeyLoader loader);

    /**
     * Reloads keys for the model key.
     *
     * @param modelKey
     * @param loader
     * @return true if reloaded, false if not
     */
    boolean reloadKeys(String modelKey, PublicKeyLoader loader);
}
