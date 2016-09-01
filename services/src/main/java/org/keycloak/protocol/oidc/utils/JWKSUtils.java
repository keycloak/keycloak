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

package org.keycloak.protocol.oidc.utils;

import java.io.IOException;
import java.io.InputStream;
import java.security.PublicKey;

import org.keycloak.common.util.StreamUtil;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.jose.jwk.JSONWebKeySet;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jwk.JWKParser;
import org.keycloak.models.KeycloakSession;
import org.keycloak.util.JsonSerialization;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class JWKSUtils {

    public static JSONWebKeySet sendJwksRequest(KeycloakSession session, String jwksURI) throws IOException {
        InputStream is = session.getProvider(HttpClientProvider.class).get(jwksURI);
        String keySetString = StreamUtil.readString(is);
        return JsonSerialization.readValue(keySetString, JSONWebKeySet.class);
    }


    public static PublicKey getKeyForUse(JSONWebKeySet keySet, JWK.Use requestedUse) {
        for (JWK jwk : keySet.getKeys()) {
            JWKParser parser = JWKParser.create(jwk);
            if (parser.getJwk().getPublicKeyUse().equals(requestedUse.asString()) && parser.isAlgorithmSupported(jwk.getKeyType())) {
                return parser.toPublicKey();
            }
        }

        return null;
    }
}
