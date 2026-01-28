package org.keycloak.testsuite.util.oauth;

import org.keycloak.util.TokenUtil;

public class OAuthClientConfig {

    private String realm;

    private String clientId;

    private String clientSecret;

    private String redirectUri;

    private String postLogoutRedirectUri;

    private String origin;

    private String scope;

    private boolean openid = true;

    private String responseType;

    private String responseMode;

    public String getRealm() {
        return realm;
    }

    public OAuthClientConfig realm(String realm) {
        this.realm = realm;
        return this;
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public OAuthClientConfig clientId(String clientId) {
        this.clientId = clientId;
        return this;
    }

    public OAuthClientConfig client(String clientId) {
        this.clientId = clientId;
        this.clientSecret = null;
        return this;
    }

    public OAuthClientConfig client(String clientId, String clientSecret) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        return this;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public OAuthClientConfig redirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
        return this;
    }

    public String getPostLogoutRedirectUri() {
        return postLogoutRedirectUri;
    }

    public OAuthClientConfig postLogoutRedirectUri(String postLogoutRedirectUri) {
        this.postLogoutRedirectUri = postLogoutRedirectUri;
        return this;
    }

    public String getOrigin() {
        return origin;
    }

    public OAuthClientConfig origin(String origin) {
        this.origin = origin;
        return this;
    }

    public String getScope() {
        return getScope(true);
    }

    public String getScope(boolean attachOpenidIfNull) {
        return openid && (scope != null || attachOpenidIfNull) ? TokenUtil.attachOIDCScope(scope) : scope;
    }

    public OAuthClientConfig scope(String scope) {
        this.scope = scope;
        return this;
    }

    public boolean isOpenid() {
        return openid;
    }

    public OAuthClientConfig openid(boolean openid) {
        this.openid = openid;
        return this;
    }

    public String getResponseMode() {
        return responseMode;
    }

    public OAuthClientConfig responseMode(String responseMode) {
        this.responseMode = responseMode;
        return this;
    }

    public String getResponseType() {
        return responseType;
    }

    public OAuthClientConfig responseType(String responseType) {
        this.responseType = responseType;
        return this;
    }

}
