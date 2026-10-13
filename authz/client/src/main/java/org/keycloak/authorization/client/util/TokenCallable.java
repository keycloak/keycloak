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
import org.keycloak.common.util.Time;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.RefreshToken;
import org.keycloak.util.JsonSerialization;

import org.jboss.logging.Logger;

public class TokenCallable implements Callable<String> {

    private static Logger log = Logger.getLogger(TokenCallable.class);
    private final String userName;
    private final String password;
    private final String scope;
    private final Http http;
    private final Configuration configuration;
    private final ServerConfiguration serverConfiguration;
    private AccessTokenResponse tokenResponse;

    public TokenCallable(String userName, String password, String scope, Http http, Configuration configuration,
        ServerConfiguration serverConfiguration) {
        this.userName = userName;
        this.password = password;
        this.scope = scope;
        this.http = http;
        this.configuration = configuration;
        this.serverConfiguration = serverConfiguration;
    }

    public TokenCallable(String userName, String password, Http http, Configuration configuration,
        ServerConfiguration serverConfiguration) {
        this(userName, password, null, http, configuration, serverConfiguration);
    }

    public TokenCallable(Http http, Configuration configuration, ServerConfiguration serverConfiguration) {
        this(null, null, http, configuration, serverConfiguration);
    }

    @Override
    public String call() {
        if (tokenResponse == null) {
            tokenResponse = obtainTokens();
        }

        try {
            String rawAccessToken = tokenResponse.getToken();
            AccessToken accessToken = JsonSerialization.readValue(new JWSInput(rawAccessToken).getContent(), AccessToken.class);

            if (accessToken.isActive() && this.isTokenTimeToLiveSufficient(accessToken)) {
                return rawAccessToken;
            } else {
                log.debug("Access token is expired.");
            }
        } catch (Exception cause) {
            clearTokens();
            throw new RuntimeException("Failed to parse access token", cause);
        }

        tokenResponse = tryRefreshToken();

        return tokenResponse.getToken();
    }

    private AccessTokenResponse tryRefreshToken() {
        String rawRefreshToken = tokenResponse.getRefreshToken();

        if (rawRefreshToken == null) {
            log.debug("Refresh token not found, obtaining new tokens");
            return obtainTokens();
        }

        try {
            RefreshToken refreshToken = JsonSerialization.readValue(new JWSInput(rawRefreshToken).getContent(), RefreshToken.class);
            if (!refreshToken.isActive() || !isTokenTimeToLiveSufficient(refreshToken)) {
                log.debug("Refresh token is expired.");
                return obtainTokens();
            }
        } catch (Exception cause) {
            clearTokens();
            throw new RuntimeException("Failed to parse refresh token", cause);
        }

        return refreshToken(rawRefreshToken);
    }

    public boolean isTokenTimeToLiveSufficient(AccessToken token) {
        return token != null && (token.getExp() - getConfiguration().getTokenMinimumTimeToLive()) > Time.currentTime();
    }

    /**
     * Obtains an access token using the client credentials.
     *
     * @return an {@link AccessTokenResponse}
     */
    AccessTokenResponse clientCredentialsGrant() {
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
    AccessTokenResponse resourceOwnerPasswordGrant(String userName, String password) {
        return resourceOwnerPasswordGrant(userName, password, null);
    }

    AccessTokenResponse resourceOwnerPasswordGrant(String userName, String password, String scope) {
        return this.http.<AccessTokenResponse>post(this.serverConfiguration.getTokenEndpoint()).authentication()
            .oauth2ResourceOwnerPassword(userName, password, scope).response().json(AccessTokenResponse.class).execute();
    }

    private AccessTokenResponse refreshToken(String rawRefreshToken) {
        log.debug("Refreshing tokens");
        return http.<AccessTokenResponse>post(serverConfiguration.getTokenEndpoint())
                .authentication().client()
                .form()
                .param("grant_type", "refresh_token")
                .param("refresh_token", rawRefreshToken)
                .response()
                .json(AccessTokenResponse.class)
                .execute();
    }

    private AccessTokenResponse obtainTokens() {
        if (userName == null || password == null) {
            return clientCredentialsGrant();
        } else if (scope != null) {
            return resourceOwnerPasswordGrant(userName, password, scope);
        } else {
            return resourceOwnerPasswordGrant(userName, password);
        }
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

    void clearTokens() {
        tokenResponse = null;
    }
}
