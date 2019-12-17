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
package org.keycloak.broker.oidc;

import org.keycloak.models.IdentityProviderModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;

/**
 * @author Pedro Igor
 */
public class OAuth2IdentityProviderConfig extends IdentityProviderModel {

    public OAuth2IdentityProviderConfig(IdentityProviderModel model) {
        super(model);
    }

    public String getAuthorizationUrl() {
        return getConfig().get("authorizationUrl");
    }

    public void setAuthorizationUrl(String authorizationUrl) {
        getConfig().put("authorizationUrl", authorizationUrl);
    }

    public String getTokenUrl() {
        return getConfig().get("tokenUrl");
    }

    public void setTokenUrl(String tokenUrl) {
        getConfig().put("tokenUrl", tokenUrl);
    }

    public String getUserInfoUrl() {
        return getConfig().get("userInfoUrl");
    }

    public void setUserInfoUrl(String userInfoUrl) {
        getConfig().put("userInfoUrl", userInfoUrl);
    }

    public String getClientId() {
        return getConfig().get("clientId");
    }

    public void setClientId(String clientId) {
        getConfig().put("clientId", clientId);
    }

    public String getClientAuthMethod() {
        return getConfig().getOrDefault("clientAuthMethod", OIDCLoginProtocol.CLIENT_SECRET_POST);
    }

    public void setClientAuthMethod(String clientAuth) {
        getConfig().put("clientAuthMethod", clientAuth);
    }

    public String getClientSecret() {
        return getConfig().get("clientSecret");
    }

    public void setClientSecret(String clientSecret) {
        getConfig().put("clientSecret", clientSecret);
    }

    public String getDefaultScope() {
        return getConfig().get("defaultScope");
    }

    public void setDefaultScope(String defaultScope) {
        getConfig().put("defaultScope", defaultScope);
    }

    public boolean isLoginHint() {
        return Boolean.valueOf(getConfig().get("loginHint"));
    }

    public void setLoginHint(boolean loginHint) {
        getConfig().put("loginHint", String.valueOf(loginHint));
    }
    
    public boolean isJWTAuthentication() {
        if (getClientAuthMethod().equals(OIDCLoginProtocol.CLIENT_SECRET_JWT)
                || getClientAuthMethod().equals(OIDCLoginProtocol.PRIVATE_KEY_JWT)) {
            return true;
        }
        return false;
    }

    public boolean isBasicAuthentication(){
        return getClientAuthMethod().equals(OIDCLoginProtocol.CLIENT_SECRET_BASIC);
    }

    public boolean isUiLocales() {
        return Boolean.valueOf(getConfig().get("uiLocales"));
    }

    public void setUiLocales(boolean uiLocales) {
        getConfig().put("uiLocales", String.valueOf(uiLocales));
    }

    public String getPrompt() {
        return getConfig().get("prompt");
    }

    public String getForwardParameters() {
        return getConfig().get("forwardParameters");
    }

    public void setForwardParameters(String forwardParameters) {
       getConfig().put("forwardParameters", forwardParameters);
    }
}
