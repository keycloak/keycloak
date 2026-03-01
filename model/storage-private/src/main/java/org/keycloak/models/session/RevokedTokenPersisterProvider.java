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

package org.keycloak.models.session;

import java.util.stream.Stream;

import org.keycloak.provider.Provider;

/**
 * Use this to revoke a token, so they will be available even after the restart of Keycloak.
 * The store can be optimized in a way that entries are only added and are only removed by expiry.
 *
 * The first Keycloak instance starting up will re-load all expired tokens from it.
 *
 * @author Alexander Schwartz
 */
public interface RevokedTokenPersisterProvider extends Provider {

    /** Revoke a token with a given ID */
    void revokeToken(String tokenId, long lifetime);

    Stream<RevokedToken> getAllRevokedTokens();

    void expireTokens();
}
