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
import org.apache.http.impl.client.CloseableHttpClient;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.jose.jwk.JSONWebKeySet;
import org.keycloak.models.KeycloakSession;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;

/**
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class JWKSHttpUtils {

    public static JSONWebKeySet sendJwksRequest(KeycloakSession session, String jwksURI) throws IOException {
        return sendJwksRequest(session, jwksURI, null);
    }

    public static JSONWebKeySet sendJwksRequest(KeycloakSession session, String jwksURI, String bearerTokenURI) throws IOException {
        //TODO: Consider putting this logic into the HttpClientProvider. Maybe getString(string, string)?
        String bearerToken = null;
        if (bearerTokenURI != null && !bearerTokenURI.isEmpty()) {
            //read the bearer token from the URI. Assume it's a file and it might not exist
            if (!bearerTokenURI.startsWith("file://")) {
                throw new IllegalArgumentException("Bearer token URI must start with 'file://'");
            }
            //read the token from the file. Assume it's in URI format and starts with "file://"
            String tokenFilePath = bearerTokenURI.substring("file://".length());
            bearerToken = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(tokenFilePath)));
        }
        HttpClient client = session.getProvider(HttpClientProvider.class).getHttpClient();
        HttpGet request = new HttpGet(jwksURI);
        if (bearerToken != null) {
            request.setHeader("Authorization", "Bearer " + bearerToken);
        }
        HttpResponse response = client.execute(request);
        String body = new BasicResponseHandler().handleResponse(response);
        if (body == null) {
            throw new IOException("No content returned from HTTP call");
        }
        return JsonSerialization.readValue(body, JSONWebKeySet.class);

        //Old code:
//        String keySetString = session.getProvider(HttpClientProvider.class).getString(jwksURI);
//        return JsonSerialization.readValue(keySetString, JSONWebKeySet.class);
    }
}
