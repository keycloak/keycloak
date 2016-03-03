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

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.keycloak.admin.client.Config;
import org.keycloak.admin.client.resource.BasicAuthFilter;
import org.keycloak.common.util.Time;
import org.keycloak.representations.AccessTokenResponse;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Form;
import java.util.Calendar;
import java.util.Date;

/**
 * @author rodrigo.sasaki@icarros.com.br
 */
public class TokenManager {

    private static final long DEFAULT_MIN_VALIDITY = 30;

    private AccessTokenResponse currentToken;
    private long expirationTime;
    private long minTokenValidity = DEFAULT_MIN_VALIDITY;
    private final Config config;
    private final ResteasyClient client;

    public TokenManager(Config config, ResteasyClient client){
        this.config = config;
        this.client = client;
    }

    public String getAccessTokenString(){
        return getAccessToken().getToken();
    }

    public AccessTokenResponse getAccessToken(){
        if(currentToken == null){
            grantToken();
        }else if(tokenExpired()){
            refreshToken();
        }
        return currentToken;
    }

    public AccessTokenResponse grantToken(){
        ResteasyWebTarget target = client.target(config.getServerUrl());

        Form form = new Form()
                .param("grant_type", "password")
                .param("username", config.getUsername())
                .param("password", config.getPassword());

        if(config.isPublicClient()){
            form.param("client_id", config.getClientId());
        } else {
            target.register(new BasicAuthFilter(config.getClientId(), config.getClientSecret()));
        }

        TokenService tokenService = target.proxy(TokenService.class);

        int requestTime = Time.currentTime();
        currentToken = tokenService.grantToken(config.getRealm(), form.asMap());
        expirationTime = requestTime + currentToken.getExpiresIn();

        return currentToken;
    }

    public AccessTokenResponse refreshToken(){
        ResteasyWebTarget target = client.target(config.getServerUrl());

        Form form = new Form()
                .param("grant_type", "refresh_token")
                .param("refresh_token", currentToken.getRefreshToken());

        if(config.isPublicClient()){
            form.param("client_id", config.getClientId());
        } else {
            target.register(new BasicAuthFilter(config.getClientId(), config.getClientSecret()));
        }

        TokenService tokenService = target.proxy(TokenService.class);

        try {
            int requestTime = Time.currentTime();
            currentToken = tokenService.refreshToken(config.getRealm(), form.asMap());
            expirationTime = requestTime + currentToken.getExpiresIn();

            return currentToken;
        } catch (BadRequestException e) {
            return grantToken();
        }
    }

    public void setMinTokenValidity(long minTokenValidity) {
        this.minTokenValidity = minTokenValidity;
    }

    private boolean tokenExpired() {
        return (Time.currentTime() + minTokenValidity) >= expirationTime;
    }

}
