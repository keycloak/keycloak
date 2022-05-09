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
package org.keycloak.models;

import org.keycloak.provider.Provider;

import java.util.Map;

/**
 * Internal action token store provider.
 *
 * It's used for store the details about used action tokens. There is separate provider for OAuth2 codes - {@link SingleUseObjectProvider},
 * which may reuse some components (eg. same infinispan cache)
 *
 * @author hmlnarik
 */
public interface ActionTokenStoreProvider extends Provider {

    /**
     * Adds a given token to token store.
     * @param actionTokenKey key
     * @param notes Optional notes to be stored with the token. Can be {@code null} in which case it is treated as an empty map.
     */
    void put(ActionTokenKeyModel actionTokenKey, Map<String, String> notes);

    /**
     * Returns token corresponding to the given key from the internal action token store
     * @param key key
     * @return {@code null} if no token is found for given key and nonce, value otherwise
     */
    ActionTokenValueModel get(ActionTokenKeyModel key);

    /**
     * Removes token corresponding to the given key from the internal action token store, and returns the stored value
     * @param key key
     * @param nonce nonce that must match a given key
     * @return {@code null} if no token is found for given key and nonce, value otherwise
     */
    ActionTokenValueModel remove(ActionTokenKeyModel key);
}
