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
package org.keycloak.adapters.installed.core;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.KeycloakDeploymentBuilder;
import org.keycloak.adapters.ServerRequest;
import org.keycloak.adapters.rotation.AdapterTokenVerifier;
import org.keycloak.common.VerificationException;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.IDToken;


public abstract class AbstractKeycloakInstalled {

    private static final String KEYCLOAK_JSON = "META-INF/keycloak.json";

    private final KeycloakDeployment deployment;
    private String tokenString;
    private String refreshToken;
    private AccessTokenResponse tokenResponse;
    private IDToken idToken;
    private String idTokenString;
    private AccessToken token;

    public AbstractKeycloakInstalled() {
        this(Thread.currentThread().getContextClassLoader().getResourceAsStream(KEYCLOAK_JSON));
    }

    public AbstractKeycloakInstalled(InputStream config) {
        this(KeycloakDeploymentBuilder.build(config));
    }

    public AbstractKeycloakInstalled(KeycloakDeployment deployment) {
        this.deployment = deployment;
    }

    public final KeycloakDeployment getDeployment() {
        return deployment;
    }

    public String getTokenString() throws VerificationException, IOException, ServerRequest.HttpFailure {
        return tokenString;
    }

    public String getTokenString(long minValidity, TimeUnit unit) throws VerificationException, IOException, ServerRequest.HttpFailure {
        long expires = ((long) token.getExpiration()) * 1000 - unit.toMillis(minValidity);
        if (expires < System.currentTimeMillis()) {
            refreshToken();
        }
        return tokenString;
    }

    public void refreshToken() throws IOException, ServerRequest.HttpFailure, VerificationException {
        AccessTokenResponse tokenResponse = ServerRequest.invokeRefresh(deployment, refreshToken);
        parseAccessToken(tokenResponse);
    }

    public void refreshToken(String refreshToken) throws IOException, ServerRequest.HttpFailure, VerificationException {
        AccessTokenResponse tokenResponse = ServerRequest.invokeRefresh(deployment, refreshToken);
        parseAccessToken(tokenResponse);
    }

    protected void parseAccessToken(AccessTokenResponse tokenResponse) throws VerificationException {
        this.tokenResponse = tokenResponse;
        tokenString = tokenResponse.getToken();
        refreshToken = tokenResponse.getRefreshToken();
        idTokenString = tokenResponse.getIdToken();
        AdapterTokenVerifier.VerifiedTokens tokens = AdapterTokenVerifier.verifyTokens(tokenString, idTokenString, getDeployment());
        token = tokens.getAccessToken();
        idToken = tokens.getIdToken();
    }

    public void logout() throws IOException, InterruptedException, URISyntaxException {
        tokenString = null;
        token = null;
        idTokenString = null;
        idToken = null;
        refreshToken = null;
    }

    public IDToken getIdToken() {
        return idToken;
    }

    public String getIdTokenString() {
        return idTokenString;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public AccessToken getToken() {
        return token;
    }

    public AccessTokenResponse getTokenResponse() {
        return tokenResponse;
    }

    protected void processCode(String code, String redirectUri) throws IOException, ServerRequest.HttpFailure, VerificationException {
        AccessTokenResponse tokenResponse = ServerRequest.invokeAccessCodeToToken(getDeployment(), code, redirectUri, null);
        parseAccessToken(tokenResponse);
    }

}
