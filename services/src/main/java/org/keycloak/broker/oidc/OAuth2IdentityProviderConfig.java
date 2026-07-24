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

import java.util.Arrays;

import org.keycloak.OAuth2Constants;
import org.keycloak.common.enums.SslRequired;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.IDToken;

import static org.keycloak.common.util.UriUtils.checkUrl;

/**
 * @author Pedro Igor
 */
public class OAuth2IdentityProviderConfig extends IdentityProviderModel {

    public static final String PKCE_ENABLED = "pkceEnabled";
    public static final String PKCE_METHOD = "pkceMethod";
    public static final String TOKEN_ENDPOINT_URL = "tokenUrl";
    public static final String TOKEN_INTROSPECTION_URL = "tokenIntrospectionUrl";

    public static final String JWT_X509_HEADERS_ENABLED = "jwtX509HeadersEnabled";

    public static final String REQUIRES_SHORT_STATE_PARAMETER = "requiresShortStateParameter";

    public OAuth2IdentityProviderConfig(IdentityProviderModel model) {
        super(model);
    }

    public OAuth2IdentityProviderConfig() {
        super();
    }

    public String getAuthorizationUrl() {
        return getConfig().get("authorizationUrl");
    }

    public void setAuthorizationUrl(String authorizationUrl) {
        getConfig().put("authorizationUrl", authorizationUrl);
    }

    public String getTokenUrl() {
        return getConfig().get(TOKEN_ENDPOINT_URL);
    }

    public void setTokenUrl(String tokenUrl) {
        getConfig().put(TOKEN_ENDPOINT_URL, tokenUrl);
    }

    public String getUserInfoUrl() {
        return getConfig().get("userInfoUrl");
    }

    public void setUserInfoUrl(String userInfoUrl) {
        getConfig().put("userInfoUrl", userInfoUrl);
    }

    public String getTokenIntrospectionUrl() {
        return getConfig().get(TOKEN_INTROSPECTION_URL);
    }

    public void setTokenIntrospectionUrl(String introspectionEndpointUrl) {
        getConfig().put(TOKEN_INTROSPECTION_URL, introspectionEndpointUrl);
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

    public boolean isBasicAuthenticationUnencoded(){
        return getClientAuthMethod().equals(OIDCLoginProtocol.CLIENT_SECRET_BASIC_UNENCODED);
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

    public boolean isRequiresShortStateParameter() {
        return Boolean.parseBoolean(getConfig().get(REQUIRES_SHORT_STATE_PARAMETER));
    }

    public void setRequiresShortStateParameter(boolean requiresShortStateParameter) {
        getConfig().put(REQUIRES_SHORT_STATE_PARAMETER, String.valueOf(requiresShortStateParameter));
    }

    public String getForwardParameters() {
        return getConfig().get("forwardParameters");
    }

    public void setForwardParameters(String forwardParameters) {
       getConfig().put("forwardParameters", forwardParameters);
    }

    public boolean isPkceEnabled() {
        return Boolean.parseBoolean(getConfig().getOrDefault(PKCE_ENABLED, "false"));
    }

    public void setPkceEnabled(boolean enabled) {
        getConfig().put(PKCE_ENABLED, String.valueOf(enabled));
    }

    public String getPkceMethod() {
        return getConfig().get(PKCE_METHOD);
    }

    public String setPkceMethod(String method) {
        return getConfig().put(PKCE_METHOD, method);
    }

    public String getClientAssertionSigningAlg() {
        return getConfig().get("clientAssertionSigningAlg");
    }
    
    public void setClientAssertionSigningAlg(String signingAlg) {
        getConfig().put("clientAssertionSigningAlg", signingAlg);
    }

    public String getClientAssertionAudience() {
        return getConfig().get("clientAssertionAudience");
    }

    public void setClientAssertionAudience(String audience) {
        getConfig().put("clientAssertionAudience", audience);
    }


    public boolean isJwtX509HeadersEnabled() {
        if (getClientAuthMethod().equals(OIDCLoginProtocol.PRIVATE_KEY_JWT)
            && Boolean.parseBoolean(getConfig().getOrDefault(JWT_X509_HEADERS_ENABLED, "false"))) {
            return true;
        }
        return false;
    }

    public void setJwtX509HeadersEnabled(boolean enabled) {
        getConfig().put(JWT_X509_HEADERS_ENABLED, String.valueOf(enabled));
    }

    public String getUserIDClaim() {
        return getConfig().getOrDefault("userIDClaim", IDToken.SUBJECT);
    }

    public String getUserNameClaim() {
        return getConfig().getOrDefault("userNameClaim", IDToken.PREFERRED_USERNAME);
    }

    public String getFullNameClaim() {
        return getConfig().getOrDefault("fullNameClaim", IDToken.NAME);
    }

    public String getGivenNameClaim() {
        return getConfig().getOrDefault("givenNameClaim", IDToken.GIVEN_NAME);
    }

    public String getFamilyNameClaim() {
        return getConfig().getOrDefault("familyNameClaim", IDToken.FAMILY_NAME);
    }

    public String getEmailClaim() {
        return getConfig().getOrDefault("emailClaim", IDToken.EMAIL);
    }

    @Override
    public void validate(RealmModel realm) {
        SslRequired sslRequired = realm.getSslRequired();

        checkUrl(sslRequired, getAuthorizationUrl(), "authorization_url");
        checkUrl(sslRequired, getTokenUrl(), "token_url");
        checkUrl(sslRequired, getUserInfoUrl(), "userinfo_url");
        checkUrl(sslRequired, getTokenIntrospectionUrl(), "tokenIntrospection_url");

        if (isPkceEnabled()) {
            String pkceMethod = getPkceMethod();
            if (!Arrays.asList(OAuth2Constants.PKCE_METHOD_PLAIN, OAuth2Constants.PKCE_METHOD_S256).contains(pkceMethod)) {
                throw new IllegalArgumentException("PKCE Method not supported: " + pkceMethod);
            }
        }
    }
}
