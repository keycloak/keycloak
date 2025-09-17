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
package org.keycloak.models;

import java.util.function.BiConsumer;

import org.keycloak.Token;
import org.keycloak.TokenCategory;
import org.keycloak.jose.JOSE;
import org.keycloak.jose.jws.Algorithm;
import org.keycloak.representations.LogoutToken;
import org.keycloak.representations.openid_federation.EntityStatement;

public interface TokenManager {

    BiConsumer<JOSE, ClientModel> DEFAULT_VALIDATOR = (jwt, client) -> {
        String rawAlgorithm = jwt.getHeader().getRawAlgorithm();

        if (rawAlgorithm.equalsIgnoreCase(Algorithm.none.name())) {
            throw new RuntimeException("Algorithm none not supported");
        }
    };

    /**
     * Encodes the supplied token
     *
     * @param token the token to encode
     * @return The encoded token
     */
    String encode(Token token);

    /**
     * Encodes the supplied token for OpenID Federation
     *
     * @param token the token to encode
     * @return The encoded token
     */
    String encodeForOpenIdFederation(EntityStatement token);

    /**
     * Decodes and verifies the token, or <code>null</code> if the token was invalid
     *
     * @param token the token to decode
     * @param clazz the token type to return
     * @param <T>
     * @return The decoded token, or <code>null</code> if the token was not valid
     */
    <T extends Token> T decode(String token, Class<T> clazz);

    String signatureAlgorithm(TokenCategory category);

    /**
     *
     *
     * @param token
     * @param client
     * @param clazz
     * @param <T>
     * @return
     */
    default <T> T decodeClientJWT(String token, ClientModel client, Class<T> clazz) {
        return decodeClientJWT(token, client, DEFAULT_VALIDATOR, clazz);
    }

    <T> T decodeClientJWT(String token, ClientModel client, BiConsumer<JOSE, ClientModel> jwtValidator,
            Class<T> clazz);

    String encodeAndEncrypt(Token token);
    String cekManagementAlgorithm(TokenCategory category);
    String encryptAlgorithm(TokenCategory category);

    LogoutToken initLogoutToken(ClientModel client, UserModel user, AuthenticatedClientSessionModel clientSessionModel);
}
