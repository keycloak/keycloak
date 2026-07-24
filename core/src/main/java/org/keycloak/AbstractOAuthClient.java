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

package org.keycloak;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.keycloak.common.enums.RelativeUrlsUsed;
import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.common.util.SecretGenerator;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class AbstractOAuthClient {
    private static final String OAUTH_TOKEN_REQUEST_STATE = "OAuth_Token_Request_State";
    private final AtomicLong counter = new AtomicLong();

    protected String clientId;
    protected Map<String, Object> credentials;
    protected String authUrl;
    protected String tokenUrl;
    protected RelativeUrlsUsed relativeUrlsUsed;
    protected String scope;
    protected String stateCookieName = OAUTH_TOKEN_REQUEST_STATE;
    protected String stateCookiePath;
    protected boolean isSecure;
    protected boolean publicClient;
    protected String getStateCode() {
        return counter.getAndIncrement() + "/" + SecretGenerator.getInstance().generateSecureID();
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public Map<String, Object> getCredentials() {
        return credentials;
    }

    public void setCredentials(Map<String, Object> credentials) {
        this.credentials = credentials;
    }

    public String getAuthUrl() {
        return authUrl;
    }

    public void setAuthUrl(String authUrl) {
        this.authUrl = authUrl;
    }

    public String getTokenUrl() {
        return tokenUrl;
    }

    public void setTokenUrl(String tokenUrl) {
        this.tokenUrl = tokenUrl;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getStateCookieName() {
        return stateCookieName;
    }

    public void setStateCookieName(String stateCookieName) {
        this.stateCookieName = stateCookieName;
    }

    public String getStateCookiePath() {
        return stateCookiePath;
    }

    public void setStateCookiePath(String stateCookiePath) {
        this.stateCookiePath = stateCookiePath;
    }

    public boolean isPublicClient() {
        return publicClient;
    }

    public void setPublicClient(boolean publicClient) {
        this.publicClient = publicClient;
    }

    public boolean isSecure() {
        return isSecure;
    }

    public void setSecure(boolean secure) {
        isSecure = secure;
    }

    public RelativeUrlsUsed getRelativeUrlsUsed() {
        return relativeUrlsUsed;
    }

    public void setRelativeUrlsUsed(RelativeUrlsUsed relativeUrlsUsed) {
        this.relativeUrlsUsed = relativeUrlsUsed;
    }

    protected String stripOauthParametersFromRedirect(String uri) {
        KeycloakUriBuilder builder = KeycloakUriBuilder.fromUri(uri)
                .replaceQueryParam(OAuth2Constants.CODE, null)
                .replaceQueryParam(OAuth2Constants.STATE, null);
        return builder.buildAsString();
    }

}
