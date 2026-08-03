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
import org.keycloak.broker.oidc.mtls.IdpClientCertificateResolver;
import org.keycloak.common.enums.SslRequired;
import org.keycloak.component.ComponentModel;
import org.keycloak.keys.KeyProvider;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
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

    public static final String CLIENT_CERT_KEY_PROVIDER_ID = "clientCertKeyProviderId";

    // RFC 8705 mtls_endpoint_aliases: certificate-authenticated endpoints published by the IdP.
    // When tls_client_auth is used, backchannel requests must target these instead of the regular endpoints.
    public static final String MTLS_TOKEN_ENDPOINT_URL = "mtlsTokenUrl";
    public static final String MTLS_USER_INFO_URL = "mtlsUserInfoUrl";
    public static final String MTLS_TOKEN_INTROSPECTION_URL = "mtlsTokenIntrospectionUrl";

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

    public boolean isTlsClientAuth() {
        return getClientAuthMethod().equals(OIDCLoginProtocol.TLS_CLIENT_AUTH);
    }

    public String getClientCertKeyProviderId() {
        return getConfig().get(CLIENT_CERT_KEY_PROVIDER_ID);
    }

    public void setClientCertKeyProviderId(String keyProviderId) {
        getConfig().put(CLIENT_CERT_KEY_PROVIDER_ID, keyProviderId);
    }

    public String getMtlsTokenUrl() {
        return getConfig().get(MTLS_TOKEN_ENDPOINT_URL);
    }

    public void setMtlsTokenUrl(String mtlsTokenUrl) {
        getConfig().put(MTLS_TOKEN_ENDPOINT_URL, mtlsTokenUrl);
    }

    public String getMtlsUserInfoUrl() {
        return getConfig().get(MTLS_USER_INFO_URL);
    }

    public void setMtlsUserInfoUrl(String mtlsUserInfoUrl) {
        getConfig().put(MTLS_USER_INFO_URL, mtlsUserInfoUrl);
    }

    public String getMtlsTokenIntrospectionUrl() {
        return getConfig().get(MTLS_TOKEN_INTROSPECTION_URL);
    }

    public void setMtlsTokenIntrospectionUrl(String mtlsTokenIntrospectionUrl) {
        getConfig().put(MTLS_TOKEN_INTROSPECTION_URL, mtlsTokenIntrospectionUrl);
    }

    /**
     * RFC 8705: when the IdP is configured for {@code tls_client_auth} and published a
     * {@code mtls_endpoint_aliases} token endpoint, certificate-authenticated token requests must be sent
     * there. Falls back to the regular token endpoint when no alias is configured or mTLS is not used.
     */
    public String getTokenUrlForClientAuth() {
        return selectEndpointForClientAuth(getMtlsTokenUrl(), getTokenUrl());
    }

    /**
     * See {@link #getTokenUrlForClientAuth()} — the userinfo variant.
     */
    public String getUserInfoUrlForClientAuth() {
        return selectEndpointForClientAuth(getMtlsUserInfoUrl(), getUserInfoUrl());
    }

    /**
     * See {@link #getTokenUrlForClientAuth()} — the token-introspection variant.
     */
    public String getTokenIntrospectionUrlForClientAuth() {
        return selectEndpointForClientAuth(getMtlsTokenIntrospectionUrl(), getTokenIntrospectionUrl());
    }

    private String selectEndpointForClientAuth(String mtlsEndpoint, String defaultEndpoint) {
        if (isTlsClientAuth() && mtlsEndpoint != null && !mtlsEndpoint.trim().isEmpty()) {
            return mtlsEndpoint;
        }
        return defaultEndpoint;
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
    public void validate(KeycloakSession session, RealmModel realm) {
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

        if (isTlsClientAuth()) {
            String keyProviderId = getClientCertKeyProviderId();
            if (keyProviderId == null || keyProviderId.trim().isEmpty()) {
                throw new IllegalArgumentException(
                        "A client certificate key provider is required when using tls_client_auth.");
            }
            ComponentModel component = realm.getComponent(keyProviderId);
            if (component == null || !KeyProvider.class.getName().equals(component.getProviderType())) {
                throw new IllegalArgumentException(
                        "The client certificate key provider referenced for tls_client_auth does not exist: " + keyProviderId);
            }
            // Resolve the actual key material now so that create/update enforces the same requirements as
            // the runtime backchannel calls: the provider must expose an enabled key with a private key and
            // a certificate. Otherwise creation would succeed but IdpClientCertificateResolver would then
            // deterministically fail on the first mTLS request.
            try {
                new IdpClientCertificateResolver(session).resolve(realm, keyProviderId);
            } catch (RuntimeException e) {
                throw new IllegalArgumentException(
                        "The client certificate key provider referenced for tls_client_auth cannot be used: "
                                + e.getMessage(), e);
            }
        }
    }
}
