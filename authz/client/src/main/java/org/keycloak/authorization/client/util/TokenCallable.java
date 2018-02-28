/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.authorization.client.util;

import java.util.concurrent.Callable;

import org.keycloak.authorization.client.Configuration;
import org.keycloak.authorization.client.representation.ServerConfiguration;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.util.JsonSerialization;

public class TokenCallable implements Callable<String> {

    private final String userName;
    private final String password;
    private final Http http;
    private final Configuration configuration;
    private final ServerConfiguration serverConfiguration;
    private AccessTokenResponse clientToken;

    public TokenCallable(String userName, String password, Http http, Configuration configuration, ServerConfiguration serverConfiguration) {
        this.userName = userName;
        this.password = password;
        this.http = http;
        this.configuration = configuration;
        this.serverConfiguration = serverConfiguration;
    }

    public TokenCallable(Http http, Configuration configuration, ServerConfiguration serverConfiguration) {
        this(null, null, http, configuration, serverConfiguration);
    }

    @Override
    public String call() {
        if (clientToken == null) {
            if (userName == null || password == null) {
                clientToken = obtainAccessToken();
            } else {
                clientToken = obtainAccessToken(userName, password);
            }
        }

        String token = clientToken.getToken();

        try {
            AccessToken accessToken = JsonSerialization.readValue(new JWSInput(token).getContent(), AccessToken.class);

            if (accessToken.isActive()) {
                return token;
            }

            clientToken = http.<AccessTokenResponse>post(serverConfiguration.getTokenEndpoint())
                    .authentication().client()
                    .form()
                    .param("grant_type", "refresh_token")
                    .param("refresh_token", clientToken.getRefreshToken())
                    .response()
                    .json(AccessTokenResponse.class)
                    .execute();
        } catch (Exception e) {
            clientToken = null;
            throw new RuntimeException(e);
        }

        return clientToken.getToken();
    }

    /**
     * Obtains an access token using the client credentials.
     *
     * @return an {@link AccessTokenResponse}
     */
    AccessTokenResponse obtainAccessToken() {
        return this.http.<AccessTokenResponse>post(this.serverConfiguration.getTokenEndpoint())
                .authentication()
                .client()
                .response()
                .json(AccessTokenResponse.class)
                .execute();
    }

    /**
     * Obtains an access token using the resource owner credentials.
     *
     * @return an {@link AccessTokenResponse}
     */
    AccessTokenResponse obtainAccessToken(String userName, String password) {
        return this.http.<AccessTokenResponse>post(this.serverConfiguration.getTokenEndpoint())
                .authentication()
                .oauth2ResourceOwnerPassword(userName, password)
                .response()
                .json(AccessTokenResponse.class)
                .execute();
    }

    Http getHttp() {
        return http;
    }

    protected boolean isRetry() {
        return true;
    }

    Configuration getConfiguration() {
        return configuration;
    }

    ServerConfiguration getServerConfiguration() {
        return serverConfiguration;
    }

    void clearToken() {
        clientToken = null;
    }
}
