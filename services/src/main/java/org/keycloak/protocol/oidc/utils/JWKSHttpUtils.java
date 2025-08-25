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

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.jose.jwk.JSONWebKeySet;
import org.keycloak.models.KeycloakSession;
import org.keycloak.util.JsonSerialization;
import org.keycloak.utils.StringUtil;

import java.io.IOException;

/**
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class JWKSHttpUtils {

    /**
     * Downloads JWKS from given URI
     * @param session Keycloak session
     * @param jwksURI URI to download JWKS from
     * @return JSONWebKeySet instance
     * @throws IOException in case of network or parsing errors
     */
    public static JSONWebKeySet sendJwksRequest(KeycloakSession session, String jwksURI) throws IOException {
        return sendJwksRequest(session, jwksURI, null);
    }

    /**
     * Downloads JWKS from given URI
     * @param session Keycloak session
     * @param jwksURI URI to download JWKS from
     * @param bearerToken Token for authentication
     * @return JSONWebKeySet instance
     * @throws IOException in case of network or parsing errors
     */
    public static JSONWebKeySet sendJwksRequest(KeycloakSession session, String jwksURI, String bearerToken) throws IOException {
        HttpClient client = session.getProvider(HttpClientProvider.class).getHttpClient();
        HttpGet request = new HttpGet(jwksURI);
        if (StringUtil.isNotBlank(bearerToken)) {
            request.setHeader("Authorization", "Bearer " + bearerToken);
        }
        HttpResponse response = client.execute(request);
        String body = new BasicResponseHandler().handleResponse(response);
        return JsonSerialization.readValue(body, JSONWebKeySet.class);
    }
}
