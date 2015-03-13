package org.keycloak.testsuite.performance.web;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.jboss.resteasy.security.PemUtils;
import org.json.JSONObject;
import org.keycloak.OAuth2Constants;
import org.keycloak.RSATokenVerifier;
import org.keycloak.VerificationException;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.crypto.RSAProvider;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.RefreshToken;
import org.keycloak.util.BasicAuthHelper;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.PublicKey;
import java.util.LinkedList;
import java.util.List;

/**
 * TODO: Remove from here and instead merge with org.keycloak.testsuite.OAuthClient
 *
 *@author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class OAuthClient {

    private String baseUrl;

    private String realm = "perf-realm";

    private String responseType = OAuth2Constants.CODE;

    private String grantType = "authorization_code";

    private String clientId = "perf-app";

    private String redirectUri;

    private String state = "123";

    private PublicKey realmPublicKey;

    public OAuthClient(String baseUrl) {
        try {
            JSONObject realmJson = new JSONObject(IOUtils.toString(getClass().getResourceAsStream("/perfrealm.json")));
            realmPublicKey = PemUtils.decodePublicKey(realmJson.getString("publicKey"));

            this.baseUrl = (baseUrl != null) ? baseUrl + "/auth" : "http://localhost:8081/auth";
            this.redirectUri = baseUrl + "/perf-app/perf-servlet";
        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve realm public key", e);
        }
    }

    public AccessTokenResponse doAccessTokenRequest(String code, String password) {
        HttpClient client = new DefaultHttpClient();
        HttpPost post = new HttpPost(getAccessTokenUrl());

        List<NameValuePair> parameters = new LinkedList<NameValuePair>();
        if (grantType != null) {
            parameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, grantType));
        }
        if (code != null) {
            parameters.add(new BasicNameValuePair(OAuth2Constants.CODE, code));
        }
        if (redirectUri != null) {
            parameters.add(new BasicNameValuePair(OAuth2Constants.REDIRECT_URI, redirectUri));
        }
        if (clientId != null && password != null) {
            String authorization = BasicAuthHelper.createHeader(clientId, password);
            post.setHeader("Authorization", authorization);
        }
        else if (clientId != null) {
            parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ID, clientId));
        }

        UrlEncodedFormEntity formEntity = null;
        try {
            formEntity = new UrlEncodedFormEntity(parameters, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        post.setEntity(formEntity);

        try {
            return new AccessTokenResponse(client.execute(post));
        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve access token", e);
        }
    }

    public AccessTokenResponse doGrantAccessTokenRequest(String clientSecret, String username,  String password) throws Exception {
        HttpClient client = new DefaultHttpClient();
        HttpPost post = new HttpPost(getResourceOwnerPasswordCredentialGrantUrl());

        String authorization = BasicAuthHelper.createHeader(clientId, clientSecret);
        post.setHeader("Authorization", authorization);

        List<NameValuePair> parameters = new LinkedList<NameValuePair>();
        parameters.add(new BasicNameValuePair("username", username));
        parameters.add(new BasicNameValuePair("password", password));

        UrlEncodedFormEntity formEntity;
        try {
            formEntity = new UrlEncodedFormEntity(parameters, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        post.setEntity(formEntity);

        return new AccessTokenResponse(client.execute(post));
    }

    public HttpResponse doLogout(String redirectUri, String sessionState) throws IOException {
        HttpClient client = new DefaultHttpClient();
        HttpGet get = new HttpGet(getLogoutUrl(redirectUri, sessionState));

        return client.execute(get);
    }

    public AccessTokenResponse doRefreshTokenRequest(String refreshToken, String password) {
        HttpClient client = new DefaultHttpClient();
        HttpPost post = new HttpPost(getRefreshTokenUrl());

        List<NameValuePair> parameters = new LinkedList<NameValuePair>();
        if (grantType != null) {
            parameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, grantType));
        }
        if (refreshToken != null) {
            parameters.add(new BasicNameValuePair(OAuth2Constants.REFRESH_TOKEN, refreshToken));
        }
        if (clientId != null && password != null) {
            String authorization = BasicAuthHelper.createHeader(clientId, password);
            post.setHeader("Authorization", authorization);
        }
        else if (clientId != null) {
            parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ID, clientId));
        }

        UrlEncodedFormEntity formEntity;
        try {
            formEntity = new UrlEncodedFormEntity(parameters, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        post.setEntity(formEntity);

        try {
            return new AccessTokenResponse(client.execute(post));
        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve access token", e);
        }
    }

    public AccessToken verifyToken(String token) {
        try {
            return RSATokenVerifier.verifyToken(token, realmPublicKey, realm);
        } catch (VerificationException e) {
            throw new RuntimeException("Failed to verify token", e);
        }
    }

    public void verifyCode(String code) {
        if (!RSAProvider.verify(new JWSInput(code), realmPublicKey)) {
            throw new RuntimeException("Failed to verify code");
        }
    }

    public RefreshToken verifyRefreshToken(String refreshToken) {
        try {
            JWSInput jws = new JWSInput(refreshToken);
            if (!RSAProvider.verify(jws, realmPublicKey)) {
                throw new RuntimeException("Invalid refresh token");
            }
            return jws.readJsonContent(RefreshToken.class);
        } catch (Exception e) {
            throw new RuntimeException("Invalid refresh token", e);
        }
    }

    public String getClientId() {
        return clientId;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public String getLoginFormUrl() {
        UriBuilder b = OIDCLoginProtocolService.authUrl(UriBuilder.fromUri(baseUrl));
        if (responseType != null) {
            b.queryParam(OAuth2Constants.RESPONSE_TYPE, responseType);
        }
        if (clientId != null) {
            b.queryParam(OAuth2Constants.CLIENT_ID, clientId);
        }
        if (redirectUri != null) {
            b.queryParam(OAuth2Constants.REDIRECT_URI, redirectUri);
        }
        if (state != null) {
            b.queryParam(OAuth2Constants.STATE, state);
        }
        return b.build(realm).toString();
    }

    public String getAccessTokenUrl() {
        UriBuilder b = OIDCLoginProtocolService.tokenUrl(UriBuilder.fromUri(baseUrl));
        return b.build(realm).toString();
    }

    public String getLogoutUrl(String redirectUri, String sessionState) {
        UriBuilder b = OIDCLoginProtocolService.logoutUrl(UriBuilder.fromUri(baseUrl));
        if (redirectUri != null) {
            b.queryParam(OAuth2Constants.REDIRECT_URI, redirectUri);
        }
        if (sessionState != null) {
            b.queryParam("session_state", sessionState);
        }
        return b.build(realm).toString();
    }

    public String getResourceOwnerPasswordCredentialGrantUrl() {
        UriBuilder b = OIDCLoginProtocolService.tokenUrl(UriBuilder.fromUri(baseUrl));
        return b.build(realm).toString();
    }

    public String getRefreshTokenUrl() {
        UriBuilder b = OIDCLoginProtocolService.tokenUrl(UriBuilder.fromUri(baseUrl));
        return b.build(realm).toString();
    }

    public OAuthClient realm(String realm) {
        this.realm = realm;
        return this;
    }
    public OAuthClient realmPublicKey(PublicKey key) {
        this.realmPublicKey = key;
        return this;
    }

    public OAuthClient clientId(String clientId) {
        this.clientId = clientId;
        return this;
    }

    public OAuthClient redirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
        return this;
    }

    public OAuthClient responseType(String responseType) {
        this.responseType = responseType;
        return this;
    }

    public OAuthClient state(String state) {
        this.state = state;
        return this;
    }

    public String getRealm() {
        return realm;
    }

    public static class AuthorizationCodeResponse {

        private String code;
        private String state;
        private String error;

        public AuthorizationCodeResponse(OAuthClient client, HttpServletRequest req) {
            code = req.getParameter(OAuth2Constants.CODE);
            state = req.getParameter(OAuth2Constants.STATE);
            error = req.getParameter(OAuth2Constants.ERROR);
        }

        public String getCode() {
            return code;
        }

        public String getState() {
            return state;
        }

        public String getError() {
            return error;
        }

    }

    public static class AccessTokenResponse {
        private int statusCode;

        private String accessToken;
        private String tokenType;
        private int expiresIn;
        private String refreshToken;
        private String idToken;
        private String sessionState;

        private String error;

        public AccessTokenResponse(HttpResponse response) throws Exception {
            statusCode = response.getStatusLine().getStatusCode();
            if (!"application/json".equals(response.getHeaders("Content-Type")[0].getValue())) {
                throw new RuntimeException("Invalid content type");
            }

            String s = IOUtils.toString(response.getEntity().getContent());
            JSONObject responseJson = new JSONObject(s);

            if (statusCode == 200) {
                accessToken = responseJson.getString("access_token");
                tokenType = responseJson.getString("token_type");
                expiresIn = responseJson.getInt("expires_in");
                idToken = responseJson.optString("id_token");
                sessionState = responseJson.optString("session-state");

                if (responseJson.has(OAuth2Constants.REFRESH_TOKEN)) {
                    refreshToken = responseJson.getString(OAuth2Constants.REFRESH_TOKEN);
                }
            } else {
                error = responseJson.getString(OAuth2Constants.ERROR);
            }
        }

        public String getAccessToken() {
            return accessToken;
        }

        public String getError() {
            return error;
        }

        public int getExpiresIn() {
            return expiresIn;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public String getRefreshToken() {
            return refreshToken;
        }

        public String getTokenType() {
            return tokenType;
        }

        public String getIdToken() {
            return idToken;
        }

        public String getSessionState() {
            return sessionState;
        }
    }


}
