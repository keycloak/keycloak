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

package org.keycloak.admin.client.token;

import java.security.KeyPair;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Form;

import org.keycloak.admin.client.Config;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.BasicAuthFilter;
import org.keycloak.admin.client.resource.DPoPAuthFilter;
import org.keycloak.common.util.KeyUtils;
import org.keycloak.common.util.Time;
import org.keycloak.representations.AccessTokenResponse;

import static org.keycloak.OAuth2Constants.CLIENT_ID;
import static org.keycloak.OAuth2Constants.GRANT_TYPE;
import static org.keycloak.OAuth2Constants.PASSWORD;
import static org.keycloak.OAuth2Constants.REFRESH_TOKEN;
import static org.keycloak.OAuth2Constants.SCOPE;
import static org.keycloak.OAuth2Constants.USERNAME;

/**
 * @author rodrigo.sasaki@icarros.com.br
 */
public class TokenManager {
    private static final long DEFAULT_MIN_VALIDITY = 30;

    private AccessTokenResponse currentToken;
    private long expirationTime;
    private long refreshExpirationTime;
    private long minTokenValidity = DEFAULT_MIN_VALIDITY;
    private final Config config;
    private final TokenService tokenService;
    private final String accessTokenGrantType;
    private final KeyPair dpopKeyPair;

    public TokenManager(Config config, Client client) {
        this.config = config;
        WebTarget target = client.target(config.getServerUrl());
        if (!config.isPublicClient()) {
            target.register(new BasicAuthFilter(config.getClientId(), config.getClientSecret()));
        }

        if (this.config.isUseDPoP()) {
            this.dpopKeyPair = KeyUtils.generateRsaKeyPair(2048);
            target.register(new DPoPAuthFilter(this, true));
        } else {
            this.dpopKeyPair = null;
        }

        this.tokenService = Keycloak.getClientProvider().targetProxy(target, TokenService.class);
        this.accessTokenGrantType = config.getGrantType();
    }

    public String getAccessTokenString() {
        return getAccessToken().getToken();
    }

    public synchronized AccessTokenResponse getAccessToken() {
        if (currentToken == null) {
            grantToken();
        } else if (tokenExpired()) {
            refreshToken();
        }
        return currentToken;
    }

    public AccessTokenResponse grantToken() {
        Form form = new Form().param(GRANT_TYPE, accessTokenGrantType);
        if (PASSWORD.equals(accessTokenGrantType)) {
            form.param(USERNAME, config.getUsername())
                .param(PASSWORD, config.getPassword());
        }

        if (config.getScope() != null) {
            form.param(SCOPE, config.getScope());
        }

        if (config.isPublicClient()) {
            form.param(CLIENT_ID, config.getClientId());
        }

        int requestTime = Time.currentTime();
        synchronized (this) {
            currentToken = tokenService.grantToken(config.getRealm(), form.asMap());
            expirationTime = requestTime + currentToken.getExpiresIn();
            refreshExpirationTime = requestTime + currentToken.getRefreshExpiresIn();
        }
        return currentToken;
    }

    public synchronized AccessTokenResponse refreshToken() {
        if (currentToken.getRefreshToken() == null || refreshTokenExpired()) {
            return grantToken();
        }

        Form form = new Form().param(GRANT_TYPE, REFRESH_TOKEN)
                              .param(REFRESH_TOKEN, currentToken.getRefreshToken());

        if (config.isPublicClient()) {
            form.param(CLIENT_ID, config.getClientId());
        }

        try {
            int requestTime = Time.currentTime();

            currentToken = tokenService.refreshToken(config.getRealm(), form.asMap());
            expirationTime = requestTime + currentToken.getExpiresIn();
            return currentToken;
        } catch (BadRequestException e) {
            return grantToken();
        }
    }

    public synchronized void logout() {
        if (currentToken == null || currentToken.getRefreshToken() == null || refreshTokenExpired()) {
            return;
        }

        Form form = new Form().param(REFRESH_TOKEN, currentToken.getRefreshToken());

        if (config.isPublicClient()) {
            form.param(CLIENT_ID, config.getClientId());
        }

        tokenService.logout(config.getRealm(), form.asMap());
        currentToken = null;
    }

    public synchronized void setMinTokenValidity(long minTokenValidity) {
        this.minTokenValidity = minTokenValidity;
    }

    private synchronized boolean tokenExpired() {
        return (Time.currentTime() + minTokenValidity) >= expirationTime;
    }

    private synchronized boolean refreshTokenExpired() { return (Time.currentTime() + minTokenValidity) >= refreshExpirationTime; }

    /**
     * Invalidates the current token, but only when it is equal to the token passed as an argument.
     *
     * @param token the token to invalidate (cannot be null).
     */
    public synchronized void invalidate(String token) {
        if (currentToken == null) {
            return; // There's nothing to invalidate.
        }
        if (token.equals(currentToken.getToken())) {
            // When used next, this cause a refresh attempt, that in turn will cause a grant attempt if refreshing fails.
            expirationTime = -1;
        }
    }

    /**
     * @return dpopKeyPair if it was generated or null if DPoP is not being requested by the configuration
     */
    public KeyPair getDpopKeyPair() {
        return dpopKeyPair;
    }
}
