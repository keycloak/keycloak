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
import java.util.function.Function;
import java.util.function.Predicate;

import org.keycloak.crypto.KeyWrapper;
import org.keycloak.models.KeycloakSession;
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
     * Getter for all the keys in the model key that does not make the caller wait for the loader if valid keys
     * are already stored. It is intended for callers that need the keys to process input that is not trusted yet,
     * for example an incoming message whose signature is going to be validated.
     *
     * <p>Missing or expired keys are loaded by the caller, exactly as in {@link #getKeys(String, PublicKeyLoader)}.
     * If valid keys are stored they are returned straight away and, if they are due for a refresh, they are
     * reloaded in the background.</p>
     *
     * <p>The default implementation just calls {@link #getKeys(String, PublicKeyLoader)}.</p>
     *
     * @param modelKey The model key
     * @param loader The loader used when the caller has to wait for the keys
     * @param backgroundLoader Creates the loader for a reload in the background. It receives the session that
     * runs the reload, because the session of the caller can be closed at that point
     * @return The keys in the model
     */
    default List<KeyWrapper> getKeys(String modelKey, PublicKeyLoader loader, Function<KeycloakSession, PublicKeyLoader> backgroundLoader) {
        return getKeys(modelKey, loader);
    }

    /**
     * Reloads keys for the model key.
     *
     * @param modelKey
     * @param loader
     * @return true if reloaded, false if not
     */
    boolean reloadKeys(String modelKey, PublicKeyLoader loader);

    /**
     * Requests a reload of the valid keys stored for the model key without waiting for it. The stored keys
     * remain in use until the reload finishes successfully. A reload is not requested more often than
     * {@link #reloadKeys(String, PublicKeyLoader)} allows, or while a previously requested one is not finished.
     *
     * <p>The default implementation does nothing and returns false.</p>
     *
     * @param modelKey The model key
     * @param backgroundLoader Creates the loader for the reload. It receives the session that runs the reload,
     * because the session of the caller can be closed at that point
     * @return true if a reload was started, false if not
     */
    default boolean reloadKeysInBackground(String modelKey, Function<KeycloakSession, PublicKeyLoader> backgroundLoader) {
        return false;
    }
}
