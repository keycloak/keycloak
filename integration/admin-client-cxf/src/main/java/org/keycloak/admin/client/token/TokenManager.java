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

import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.keycloak.admin.client.Config;
import org.keycloak.admin.client.resource.BasicAuthFilter;
import org.keycloak.common.util.Time;
import org.keycloak.representations.AccessTokenResponse;

import javax.ws.rs.BadRequestException;

import javax.ws.rs.core.Form;


import java.util.ArrayList;
import java.util.List;

import static org.keycloak.OAuth2Constants.*;

/**
 * @author rodrigo.sasaki@icarros.com.br
 * @author Luigi De Masi luigi.demasi@extrasys.it
 */
public class TokenManager {
    private static final long DEFAULT_MIN_VALIDITY = 30;

    private AccessTokenResponse currentToken;
    private long expirationTime;
    private long minTokenValidity = DEFAULT_MIN_VALIDITY;
    private final Config config;
    private final TokenService tokenService;
    private final String accessTokenGrantType;

    public TokenManager(Config config) {
        this.config = config;
        BasicAuthFilter basicAuthFilter = new BasicAuthFilter(config.getClientId(), config.getClientSecret());

        List providers = new ArrayList();
        providers.add(basicAuthFilter);
        providers.add(new JacksonJaxbJsonProvider());

        WebClient webClient = WebClient.create(config.getServerUrl(), providers);


        this.tokenService = JAXRSClientFactory.fromClient(webClient, TokenService.class);
        this.accessTokenGrantType = config.getGrantType();

        if (CLIENT_CREDENTIALS.equals(accessTokenGrantType) && config.isPublicClient()) {
            throw new IllegalArgumentException("Can't use " + GRANT_TYPE + "=" + CLIENT_CREDENTIALS + " with public client");
        }
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
            form.param("username", config.getUsername())
                    .param("password", config.getPassword());
        }

        if (config.isPublicClient()) {
            form.param(CLIENT_ID, config.getClientId());
        }

        int requestTime = Time.currentTime();
        synchronized (this) {
            currentToken = tokenService.grantToken(config.getRealm(), form.asMap());
            expirationTime = requestTime + currentToken.getExpiresIn();
        }
        return currentToken;
    }

    public AccessTokenResponse refreshToken() {
        Form form = new Form().param(GRANT_TYPE, REFRESH_TOKEN)
                .param(REFRESH_TOKEN, currentToken.getRefreshToken());

        if (config.isPublicClient()) {
            form.param(CLIENT_ID, config.getClientId());
        }

        try {
            int requestTime = Time.currentTime();

            synchronized (this) {
                currentToken = tokenService.refreshToken(config.getRealm(), form.asMap());
                expirationTime = requestTime + currentToken.getExpiresIn();
            }
            return currentToken;
        } catch (BadRequestException e) {
            return grantToken();
        }
    }

    public synchronized void setMinTokenValidity(long minTokenValidity) {
        this.minTokenValidity = minTokenValidity;
    }

    private synchronized boolean tokenExpired() {
        return (Time.currentTime() + minTokenValidity) >= expirationTime;
    }

    /**
     * Invalidates the current token, but only when it is equal to the token passed as an argument.
     *
     * @param token the token to invalidate (cannot be null).
     */
    public void invalidate(String token) {
        if (currentToken == null) {
            return; // There's nothing to invalidate.
        }
        if (token.equals(currentToken.getToken())) {
            // When used next, this cause a refresh attempt, that in turn will cause a grant attempt if refreshing fails.
            expirationTime = -1;
        }
    }
}
