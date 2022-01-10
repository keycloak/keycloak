/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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
 *
 */

package org.keycloak.models;

import java.util.UUID;

import org.keycloak.provider.Provider;

/**
 *  Provides the cache for store revoked tokens.
 *
 *  For now, it is separate provider as it is bit different use-case that existing providers like {@link CodeToTokenStoreProvider},
 *  {@link SingleUseTokenStoreProvider} and {@link ActionTokenStoreProvider}
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface TokenRevocationStoreProvider extends Provider {

    /**
     * Mark given token as revoked. Parameter "lifespanSeconds" is the time for which the token is considered revoked. After this time, it may be removed from this store,
     * which means that {@link #isRevoked} method will return false. In reality, the token will usually still be invalid due the "expiration" claim on it, however
     * that is out of scope of this provider.
     *
     * @param tokenId
     * @oaran lifespanSeconds
     */
    void putRevokedToken(String tokenId, long lifespanSeconds);

    /**
     * @param tokenId
     * @return true if token exists in the store, which indicates that it is revoked.
     */
    boolean isRevoked(String tokenId);
}
