/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

/**
 * Manages revoked tokens.
 * <p>
 * When a token is revoked (e.g. via logout or token revocation endpoint), its ID is stored so that subsequent
 * validation attempts can reject it for the remainder of its natural lifespan. Expired entries are cleaned up
 * automatically.
 * <p>
 * Obtain an instance via {@link KeycloakSession#revokedTokens()}.
 */
public interface RevokedTokenProvider extends Provider {

    /**
     * Records a token as revoked.
     *
     * @param id              the unique identifier of the token (typically its {@code jti} claim).
     * @param lifespanSeconds the remaining lifespan of the token in seconds. The revocation entry will be kept for at
     *                        least this long to prevent the token from being accepted during its remaining validity.
     * @return {@code true} if the token was newly revoked; {@code false} if it was already revoked.
     */
    boolean put(String id, long lifespanSeconds);

    /**
     * Checks whether a token has been revoked.
     *
     * @param id the unique identifier of the token (typically its {@code jti} claim).
     * @return {@code true} if the token is currently revoked; {@code false} otherwise.
     */
    boolean contains(String id);

}
