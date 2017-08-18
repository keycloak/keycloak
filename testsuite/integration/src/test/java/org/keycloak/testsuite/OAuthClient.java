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
package org.keycloak.testsuite;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;
import org.junit.Assert;
import org.keycloak.OAuth2Constants;
import org.keycloak.RSATokenVerifier;
import org.keycloak.common.VerificationException;
import org.keycloak.common.util.PemUtils;
import org.keycloak.constants.AdapterConstants;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.crypto.RSAProvider;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.RefreshToken;
import org.keycloak.util.BasicAuthHelper;
import org.keycloak.util.TokenUtil;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class OAuthClient {

    private WebDriver driver;

    private String baseUrl = Constants.AUTH_SERVER_ROOT;

    private String realm = "test";

    private String clientId = "test-app";

    private String redirectUri = "http://localhost:8081/app/auth";

    private StateParamProvider state = () -> {
        return KeycloakModelUtils.generateId();
    };

    private String scope;

    private String uiLocales = null;

    private PublicKey realmPublicKey;

    private String clientSessionState;

    private String clientSessionHost;

    public OAuthClient(WebDriver driver) {
        this.driver = driver;

        try {
            JSONObject realmJson = new JSONObject(IOUtils.toString(getClass().getResourceAsStream("/testrealm.json")));
            realmPublicKey = PemUtils.decodePublicKey(realmJson.getString("publicKey"));
        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve realm public key", e);
        }
    }

    public AuthorizationCodeResponse doLogin(String username, String password) {
        openLoginForm();
        String src = driver.getPageSource();
        driver.findElement(By.id("username")).sendKeys(username);
        driver.findElement(By.id("password")).sendKeys(password);
        driver.findElement(By.name("login")).click();

        return new AuthorizationCodeResponse(this);
    }

    public void doLoginGrant(String username, String password) {
        openLoginForm();

        driver.findElement(By.id("username")).sendKeys(username);
        driver.findElement(By.id("password")).sendKeys(password);
        driver.findElement(By.name("login")).click();
    }

    public AccessTokenResponse doAccessTokenRequest(String code, String password) {
        CloseableHttpClient client = new DefaultHttpClient();
        try {
            HttpPost post = new HttpPost(getAccessTokenUrl());

            List<NameValuePair> parameters = new LinkedList<NameValuePair>();
            parameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, OAuth2Constants.AUTHORIZATION_CODE));

            if (code != null) {
                parameters.add(new BasicNameValuePair(OAuth2Constants.CODE, code));
            }
            if (redirectUri != null) {
                parameters.add(new BasicNameValuePair(OAuth2Constants.REDIRECT_URI, redirectUri));
            }
            if (clientId != null && password != null) {
                String authorization = BasicAuthHelper.createHeader(clientId, password);
                post.setHeader("Authorization", authorization);
            } else if (clientId != null) {
                parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ID, clientId));
            }

            if (clientSessionState != null) {
                parameters.add(new BasicNameValuePair(AdapterConstants.CLIENT_SESSION_STATE, clientSessionState));
            }

            if (clientSessionHost != null) {
                parameters.add(new BasicNameValuePair(AdapterConstants.CLIENT_SESSION_HOST, clientSessionHost));
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
        } finally {
            closeClient(client);
        }
    }

    public String introspectAccessTokenWithClientCredential(String clientId, String clientSecret, String tokenToIntrospect) {
        return introspectTokenWithClientCredential(clientId, clientSecret, "access_token", tokenToIntrospect);
    }

    public String introspectRefreshTokenWithClientCredential(String clientId, String clientSecret, String tokenToIntrospect) {
        return introspectTokenWithClientCredential(clientId, clientSecret, "refresh_token", tokenToIntrospect);
    }

    public String introspectTokenWithClientCredential(String clientId, String clientSecret, String tokenType, String tokenToIntrospect) {
        CloseableHttpClient client = new DefaultHttpClient();
        try {
            HttpPost post = new HttpPost(getTokenIntrospectionUrl());

            String authorization = BasicAuthHelper.createHeader(clientId, clientSecret);
            post.setHeader("Authorization", authorization);

            List<NameValuePair> parameters = new LinkedList<>();

            parameters.add(new BasicNameValuePair("token", tokenToIntrospect));
            parameters.add(new BasicNameValuePair("token_type_hint", tokenType));

            UrlEncodedFormEntity formEntity;

            try {
                formEntity = new UrlEncodedFormEntity(parameters, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }

            post.setEntity(formEntity);

            try {
                ByteArrayOutputStream out = new ByteArrayOutputStream();

                client.execute(post).getEntity().writeTo(out);

                return new String(out.toByteArray());
            } catch (Exception e) {
                throw new RuntimeException("Failed to retrieve access token", e);
            }
        } finally {
            closeClient(client);
        }
    }

    public AccessTokenResponse doGrantAccessTokenRequest(String clientSecret, String username,  String password) throws Exception {
        return doGrantAccessTokenRequest(realm, username, password, null, clientId, clientSecret);
    }

    public AccessTokenResponse doGrantAccessTokenRequest(String clientSecret, String username,  String password, String otp) throws Exception {
        return doGrantAccessTokenRequest(realm, username, password, otp, clientId, clientSecret);
    }

    public AccessTokenResponse doGrantAccessTokenRequest(String realm, String username, String password, String totp,
                                                         String clientId, String clientSecret) throws Exception {
        CloseableHttpClient client = new DefaultHttpClient();
        try {
            HttpPost post = new HttpPost(getResourceOwnerPasswordCredentialGrantUrl(realm));

            List<NameValuePair> parameters = new LinkedList<NameValuePair>();
            parameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, OAuth2Constants.PASSWORD));
            parameters.add(new BasicNameValuePair("username", username));
            parameters.add(new BasicNameValuePair("password", password));
            if (totp != null) {
                parameters.add(new BasicNameValuePair("totp", totp));

            }
            if (clientSecret != null) {
                String authorization = BasicAuthHelper.createHeader(clientId, clientSecret);
                post.setHeader("Authorization", authorization);
            } else {
                parameters.add(new BasicNameValuePair("client_id", clientId));

            }

            if (clientSessionState != null) {
                parameters.add(new BasicNameValuePair(AdapterConstants.CLIENT_SESSION_STATE, clientSessionState));
            }
            if (clientSessionHost != null) {
                parameters.add(new BasicNameValuePair(AdapterConstants.CLIENT_SESSION_HOST, clientSessionHost));
            }
            if (scope != null) {
                parameters.add(new BasicNameValuePair(OAuth2Constants.SCOPE, scope));
            }

            UrlEncodedFormEntity formEntity;
            try {
                formEntity = new UrlEncodedFormEntity(parameters, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
            post.setEntity(formEntity);

            return new AccessTokenResponse(client.execute(post));
        } finally {
            closeClient(client);
        }
    }

    public AccessTokenResponse doClientCredentialsGrantAccessTokenRequest(String clientSecret) throws Exception {
        CloseableHttpClient client = new DefaultHttpClient();
        try {
            HttpPost post = new HttpPost(getServiceAccountUrl());

            String authorization = BasicAuthHelper.createHeader(clientId, clientSecret);
            post.setHeader("Authorization", authorization);

            List<NameValuePair> parameters = new LinkedList<NameValuePair>();
            parameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, OAuth2Constants.CLIENT_CREDENTIALS));

            if (scope != null) {
                parameters.add(new BasicNameValuePair(OAuth2Constants.SCOPE, scope));
            }

            UrlEncodedFormEntity formEntity;
            try {
                formEntity = new UrlEncodedFormEntity(parameters, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
            post.setEntity(formEntity);

            return new AccessTokenResponse(client.execute(post));
        } finally {
            closeClient(client);
        }
    }


    public HttpResponse doLogout(String refreshToken, String clientSecret) throws IOException {
        CloseableHttpClient client = new DefaultHttpClient();
        try {
            HttpPost post = new HttpPost(getLogoutUrl(null, null));

            List<NameValuePair> parameters = new LinkedList<NameValuePair>();
            if (refreshToken != null) {
                parameters.add(new BasicNameValuePair(OAuth2Constants.REFRESH_TOKEN, refreshToken));
            }
            if (clientId != null && clientSecret != null) {
                String authorization = BasicAuthHelper.createHeader(clientId, clientSecret);
                post.setHeader("Authorization", authorization);
            } else if (clientId != null) {
                parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ID, clientId));
            }

            UrlEncodedFormEntity formEntity;
            try {
                formEntity = new UrlEncodedFormEntity(parameters, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
            post.setEntity(formEntity);

            return client.execute(post);
        } finally {
            closeClient(client);
        }
    }

    public AccessTokenResponse doRefreshTokenRequest(String refreshToken, String password) {
        CloseableHttpClient client = new DefaultHttpClient();
        try {
            HttpPost post = new HttpPost(getRefreshTokenUrl());

            List<NameValuePair> parameters = new LinkedList<NameValuePair>();
            parameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, OAuth2Constants.REFRESH_TOKEN));

            if (refreshToken != null) {
                parameters.add(new BasicNameValuePair(OAuth2Constants.REFRESH_TOKEN, refreshToken));
            }
            if (clientId != null && password != null) {
                String authorization = BasicAuthHelper.createHeader(clientId, password);
                post.setHeader("Authorization", authorization);
            } else if (clientId != null) {
                parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ID, clientId));
            }

            if (clientSessionState != null) {
                parameters.add(new BasicNameValuePair(AdapterConstants.CLIENT_SESSION_STATE, clientSessionState));
            }
            if (clientSessionHost != null) {
                parameters.add(new BasicNameValuePair(AdapterConstants.CLIENT_SESSION_HOST, clientSessionHost));
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
        } finally {
            closeClient(client);
        }
    }

    public void closeClient(CloseableHttpClient client) {
        try {
            client.close();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    public AccessToken verifyToken(String token) {
        try {
            return RSATokenVerifier.verifyToken(token, realmPublicKey, baseUrl + "/realms/" + realm);
        } catch (VerificationException e) {
            throw new RuntimeException("Failed to verify token", e);
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

    public String getCurrentRequest() {
        return driver.getCurrentUrl().substring(0, driver.getCurrentUrl().indexOf('?'));
    }

    public URI getCurrentUri() {
        try {
            return new URI(driver.getCurrentUrl());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, String> getCurrentQuery() {
        Map<String, String> m = new HashMap<String, String>();
        List<NameValuePair> pairs = URLEncodedUtils.parse(getCurrentUri(), "UTF-8");
        for (NameValuePair p : pairs) {
            m.put(p.getName(), p.getValue());
        }
        return m;
    }

    public void openLoginForm() {
        driver.navigate().to(getLoginFormUrl());
    }

    public void openLogout() {
        UriBuilder b = OIDCLoginProtocolService.logoutUrl(UriBuilder.fromUri(baseUrl));
        if (redirectUri != null) {
            b.queryParam(OAuth2Constants.REDIRECT_URI, redirectUri);
        }
        driver.navigate().to(b.build(realm).toString());
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public String getLoginFormUrl() {
        UriBuilder b = OIDCLoginProtocolService.authUrl(UriBuilder.fromUri(baseUrl));
        b.queryParam(OAuth2Constants.RESPONSE_TYPE, OAuth2Constants.CODE);
        if (clientId != null) {
            b.queryParam(OAuth2Constants.CLIENT_ID, clientId);
        }
        if (redirectUri != null) {
            b.queryParam(OAuth2Constants.REDIRECT_URI, redirectUri);
        }
        if (state != null) {
            b.queryParam(OAuth2Constants.STATE, state.getState());
        }
        if(uiLocales != null){
            b.queryParam(OAuth2Constants.UI_LOCALES_PARAM, uiLocales);
        }

        String scopeParam = TokenUtil.attachOIDCScope(scope);
        b.queryParam(OAuth2Constants.SCOPE, scopeParam);

        return b.build(realm).toString();
    }

    public String getAccessTokenUrl() {
        UriBuilder b = OIDCLoginProtocolService.tokenUrl(UriBuilder.fromUri(baseUrl));
        return b.build(realm).toString();
    }

    public String getTokenIntrospectionUrl() {
        UriBuilder b = OIDCLoginProtocolService.tokenIntrospectionUrl(UriBuilder.fromUri(baseUrl));
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

    public String getResourceOwnerPasswordCredentialGrantUrl(String realm) {
        UriBuilder b = OIDCLoginProtocolService.tokenUrl(UriBuilder.fromUri(baseUrl));
        return b.build(realm).toString();
    }

    public String getServiceAccountUrl() {
        return getResourceOwnerPasswordCredentialGrantUrl();
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

    public OAuthClient stateParamHardcoded(String value) {
        this.state = () -> {
            return value;
        };
        return this;
    }

    public OAuthClient stateParamRandom() {
        this.state = () -> {
            return KeycloakModelUtils.generateId();
        };
        return this;
    }

    public OAuthClient scope(String scope) {
        this.scope = scope;
        return this;
    }

    public OAuthClient uiLocales(String uiLocales){
        this.uiLocales = uiLocales;
        return this;
    }

    public OAuthClient clientSessionState(String client_session_state) {
        this.clientSessionState = client_session_state;
        return this;
    }

    public OAuthClient clientSessionHost(String client_session_host) {
        this.clientSessionHost = client_session_host;
        return this;
    }

    public String getRealm() {
        return realm;
    }

    public static class AuthorizationCodeResponse {

        private boolean isRedirected;
        private String code;
        private String state;
        private String error;

        public AuthorizationCodeResponse(OAuthClient client) {
            isRedirected = client.getCurrentRequest().equals(client.getRedirectUri());
            code = client.getCurrentQuery().get(OAuth2Constants.CODE);
            state = client.getCurrentQuery().get(OAuth2Constants.STATE);
            error = client.getCurrentQuery().get(OAuth2Constants.ERROR);
        }

        public boolean isRedirected() {
            return isRedirected;
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
        private int refreshExpiresIn;
        private String refreshToken;

        private String error;
        private String errorDescription;

        public AccessTokenResponse(HttpResponse response) throws Exception {
            statusCode = response.getStatusLine().getStatusCode();
            if (!"application/json".equals(response.getHeaders("Content-Type")[0].getValue())) {
                Assert.fail("Invalid content type");
            }

            String s = IOUtils.toString(response.getEntity().getContent());
            JSONObject responseJson = new JSONObject(s);

            if (statusCode == 200) {
                accessToken = responseJson.getString("access_token");
                tokenType = responseJson.getString("token_type");
                expiresIn = responseJson.getInt("expires_in");
                refreshExpiresIn = responseJson.getInt("refresh_expires_in");

                if (responseJson.has(OAuth2Constants.REFRESH_TOKEN)) {
                    refreshToken = responseJson.getString(OAuth2Constants.REFRESH_TOKEN);
                }
            } else {
                error = responseJson.getString(OAuth2Constants.ERROR);
                errorDescription = responseJson.has(OAuth2Constants.ERROR_DESCRIPTION) ? responseJson.getString(OAuth2Constants.ERROR_DESCRIPTION) : null;
            }
        }

        public String getAccessToken() {
            return accessToken;
        }

        public String getError() {
            return error;
        }

        public String getErrorDescription() {
            return errorDescription;
        }

        public int getExpiresIn() {
            return expiresIn;
        }

        public int getRefreshExpiresIn() {
            return refreshExpiresIn;
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
    }

    private interface StateParamProvider {

        String getState();

    }

}
