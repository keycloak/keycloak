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

package org.keycloak.testsuite.util;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.junit.Assert;
import org.keycloak.OAuth2Constants;
import org.keycloak.RSATokenVerifier;
import org.keycloak.adapters.HttpClientBuilder;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.common.VerificationException;
import org.keycloak.common.util.KeystoreUtil;
import org.keycloak.common.util.PemUtils;
import org.keycloak.constants.AdapterConstants;
import org.keycloak.jose.jwk.JSONWebKeySet;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.crypto.RSAProvider;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.protocol.oidc.utils.OIDCResponseType;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.RefreshToken;
import org.keycloak.representations.idm.KeysMetadataRepresentation;
import org.keycloak.testsuite.arquillian.AuthServerTestEnricher;
import org.keycloak.util.BasicAuthHelper;
import org.keycloak.util.JsonSerialization;
import org.keycloak.util.TokenUtil;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import javax.ws.rs.core.UriBuilder;

import java.io.Closeable;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.security.KeyStore;
import java.security.PublicKey;
import java.util.*;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
public class OAuthClient {
    public static final String SERVER_ROOT = AuthServerTestEnricher.getAuthServerContextRoot();
    public static final String AUTH_SERVER_ROOT = SERVER_ROOT + "/auth";
    public static final String APP_ROOT = AUTH_SERVER_ROOT + "/realms/master/app";
    private static final boolean sslRequired = Boolean.parseBoolean(System.getProperty("auth.server.ssl.required"));

    private Keycloak adminClient;

    private WebDriver driver;

    private String baseUrl = AUTH_SERVER_ROOT;

    private String realm;

    private String clientId;

    private String redirectUri;

    private String state;

    private String scope;

    private String uiLocales;

    private String clientSessionState;

    private String clientSessionHost;

    private String maxAge;

    private String responseType = OAuth2Constants.CODE;

    private String responseMode;

    private String nonce;

    private String request;

    private String requestUri;

    private Map<String, PublicKey> publicKeys = new HashMap<>();

    // https://tools.ietf.org/html/rfc7636#section-4
    private String codeVerifier;
    private String codeChallenge;
    private String codeChallengeMethod;

    public class LogoutUrlBuilder {
        private final UriBuilder b = OIDCLoginProtocolService.logoutUrl(UriBuilder.fromUri(baseUrl));

        public LogoutUrlBuilder idTokenHint(String idTokenHint) {
            if (idTokenHint != null) {
                b.queryParam("id_token_hint", idTokenHint);
            }
            return this;
        }

        public LogoutUrlBuilder postLogoutRedirectUri(String redirectUri) {
            if (redirectUri != null) {
                b.queryParam("post_logout_redirect_uri", redirectUri);
            }
            return this;
        }

        public LogoutUrlBuilder redirectUri(String redirectUri) {
            if (redirectUri != null) {
                b.queryParam(OAuth2Constants.REDIRECT_URI, redirectUri);
            }
            return this;
        }

        public LogoutUrlBuilder sessionState(String sessionState) {
            if (sessionState != null) {
                b.queryParam("session_state", sessionState);
            }
            return this;
        }

        public String build() {
            return b.build(realm).toString();
        }
    }

    public void init(Keycloak adminClient, WebDriver driver) {
        this.adminClient = adminClient;
        this.driver = driver;

        baseUrl = AUTH_SERVER_ROOT;
        realm = "test";
        clientId = "test-app";
        redirectUri = APP_ROOT + "/auth";
        state = "mystate";
        scope = null;
        uiLocales = null;
        clientSessionState = null;
        clientSessionHost = null;
        maxAge = null;
        nonce = null;
        request = null;
        requestUri = null;
        // https://tools.ietf.org/html/rfc7636#section-4
        codeVerifier = null;
        codeChallenge = null;
        codeChallengeMethod = null;
    }

    public AuthorizationEndpointResponse doLogin(String username, String password) {
        openLoginForm();
        fillLoginForm(username, password);

        return new AuthorizationEndpointResponse(this);
    }

    public void fillLoginForm(String username, String password) {
        String src = driver.getPageSource();
        try {
            driver.findElement(By.id("username")).sendKeys(username);
            driver.findElement(By.id("password")).sendKeys(password);
            driver.findElement(By.name("login")).click();
        } catch (Throwable t) {
            System.err.println(src);
            throw t;
        }
    }

    public void doLoginGrant(String username, String password) {
        openLoginForm();
        fillLoginForm(username, password);
    }

    private static CloseableHttpClient newCloseableHttpClient() {
        if (sslRequired) {
            KeyStore keystore = null;
            // load the keystore containing the client certificate - keystore type is probably jks or pkcs12
            String keyStorePath = System.getProperty("client.certificate.keystore");
            String keyStorePassword = System.getProperty("client.certificate.keystore.passphrase");
            try {
                keystore = KeystoreUtil.loadKeyStore(keyStorePath, keyStorePassword);
            } catch (Exception e) {
                e.printStackTrace();
            }

            // load the trustore
            KeyStore truststore = null;
            String trustStorePath = System.getProperty("client.truststore");
            String trustStorePassword = System.getProperty("client.truststore.passphrase");
            try {
                truststore = KeystoreUtil.loadKeyStore(trustStorePath, trustStorePassword);
            } catch(Exception e) {
                e.printStackTrace();
            }
            return (DefaultHttpClient)new HttpClientBuilder()
                    .keyStore(keystore, keyStorePassword)
                    .trustStore(truststore)
                    .hostnameVerification(HttpClientBuilder.HostnameVerificationPolicy.ANY)
                    .build();
        }
        return new DefaultHttpClient();
    }

    public AccessTokenResponse doAccessTokenRequest(String code, String password) {
        CloseableHttpClient client = newCloseableHttpClient();
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

            // https://tools.ietf.org/html/rfc7636#section-4.5
            if (codeVerifier != null) {
                parameters.add(new BasicNameValuePair(OAuth2Constants.CODE_VERIFIER, codeVerifier));
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

                CloseableHttpResponse response = client.execute(post);
                response.getEntity().writeTo(out);
                response.close();

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
        CloseableHttpClient client = newCloseableHttpClient();
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

    public JSONWebKeySet doCertsRequest(String realm) throws Exception {
        CloseableHttpClient client = new DefaultHttpClient();
        try {
            HttpGet get = new HttpGet(getCertsUrl(realm));
            CloseableHttpResponse response = client.execute(get);
            return JsonSerialization.readValue(response.getEntity().getContent(), JSONWebKeySet.class);
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


    public CloseableHttpResponse doLogout(String refreshToken, String clientSecret) throws IOException {
        CloseableHttpClient client = new DefaultHttpClient();
        try {
            HttpPost post = new HttpPost(getLogoutUrl().build());

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
            return RSATokenVerifier.verifyToken(token, getRealmPublicKey(realm), baseUrl + "/realms/" + realm);
        } catch (VerificationException e) {
            throw new RuntimeException("Failed to verify token", e);
        }
    }

    public IDToken verifyIDToken(String token) {
        try {
            IDToken idToken = RSATokenVerifier.verifyToken(token, getRealmPublicKey(realm), baseUrl + "/realms/" + realm, true, false);
            Assert.assertEquals(TokenUtil.TOKEN_TYPE_ID, idToken.getType());
            return idToken;
        } catch (VerificationException e) {
            throw new RuntimeException("Failed to verify token", e);
        }
    }

    public RefreshToken verifyRefreshToken(String refreshToken) {
        try {
            JWSInput jws = new JWSInput(refreshToken);
            if (!RSAProvider.verify(jws, getRealmPublicKey(realm))) {
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
        int index = driver.getCurrentUrl().indexOf('?');
        if (index == -1) {
            index = driver.getCurrentUrl().indexOf('#');
        }
        return driver.getCurrentUrl().substring(0, index);
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

    public Map<String, String> getCurrentFragment() {
        Map<String, String> m = new HashMap<String, String>();

        String fragment = getCurrentUri().getRawFragment();
        List<NameValuePair> pairs = (fragment == null || fragment.isEmpty()) ? Collections.emptyList() : URLEncodedUtils.parse(fragment, Charset.forName("UTF-8"));

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
        UriBuilder b = OIDCLoginProtocolService.authUrl(UriBuilder.fromUri(AUTH_SERVER_ROOT));
        if (responseType != null) {
            b.queryParam(OAuth2Constants.RESPONSE_TYPE, responseType);
        }
        if (responseMode != null) {
            b.queryParam(OIDCLoginProtocol.RESPONSE_MODE_PARAM, responseMode);
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
        if (uiLocales != null){
            b.queryParam(OAuth2Constants.UI_LOCALES_PARAM, uiLocales);
        }
        if (nonce != null){
            b.queryParam(OIDCLoginProtocol.NONCE_PARAM, nonce);
        }

        String scopeParam = TokenUtil.attachOIDCScope(scope);
        b.queryParam(OAuth2Constants.SCOPE, scopeParam);

        if (maxAge != null) {
            b.queryParam(OIDCLoginProtocol.MAX_AGE_PARAM, maxAge);
        }
        if (request != null) {
            b.queryParam(OIDCLoginProtocol.REQUEST_PARAM, request);
        }
        if (requestUri != null) {
            b.queryParam(OIDCLoginProtocol.REQUEST_URI_PARAM, requestUri);
        }
        // https://tools.ietf.org/html/rfc7636#section-4.3
        if (codeChallenge != null) {
            b.queryParam(OAuth2Constants.CODE_CHALLENGE, codeChallenge);
        }
        if (codeChallengeMethod != null) {
            b.queryParam(OAuth2Constants.CODE_CHALLENGE_METHOD, codeChallengeMethod);
        }  
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

    public LogoutUrlBuilder getLogoutUrl() {
        return new LogoutUrlBuilder();
    }

    public String getResourceOwnerPasswordCredentialGrantUrl() {
        UriBuilder b = OIDCLoginProtocolService.tokenUrl(UriBuilder.fromUri(baseUrl));
        return b.build(realm).toString();
    }

    public String getResourceOwnerPasswordCredentialGrantUrl(String realm) {
        UriBuilder b = OIDCLoginProtocolService.tokenUrl(UriBuilder.fromUri(baseUrl));
        return b.build(realm).toString();
    }

    public String getCertsUrl(String realm) {
        UriBuilder b = OIDCLoginProtocolService.certsUrl(UriBuilder.fromUri(baseUrl));
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

    public OAuthClient clientId(String clientId) {
        this.clientId = clientId;
        return this;
    }

    public OAuthClient redirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
        return this;
    }

    public OAuthClient state(String state) {
        this.state = state;
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

    public OAuthClient maxAge(String maxAge) {
        this.maxAge = maxAge;
        return this;
    }

    public OAuthClient responseType(String responseType) {
        this.responseType = responseType;
        return this;
    }

    public OAuthClient responseMode(String responseMode) {
        this.responseMode = responseMode;
        return this;
    }

    public OAuthClient nonce(String nonce) {
        this.nonce = nonce;
        return this;
    }

    public OAuthClient request(String request) {
        this.request = request;
        return this;
    }

    public OAuthClient requestUri(String requestUri) {
        this.requestUri = requestUri;
        return this;
    }

    public String getRealm() {
        return realm;
    }

    // https://tools.ietf.org/html/rfc7636#section-4
    public OAuthClient codeVerifier(String codeVerifier) {
    	this.codeVerifier = codeVerifier;
    	return this;
    }
    public OAuthClient codeChallenge(String codeChallenge) {
    	this.codeChallenge = codeChallenge;
    	return this;
    }
    public OAuthClient codeChallengeMethod(String codeChallengeMethod) {
    	this.codeChallengeMethod = codeChallengeMethod;
    	return this;
    }

    public static class AuthorizationEndpointResponse {

        private boolean isRedirected;
        private String code;
        private String state;
        private String error;
        private String errorDescription;

        // Just during OIDC implicit or hybrid flow
        private String accessToken;
        private String idToken;

        public AuthorizationEndpointResponse(OAuthClient client) {
            boolean fragment;
            try {
                fragment = client.responseType != null && OIDCResponseType.parse(client.responseType).isImplicitOrHybridFlow();
            } catch (IllegalArgumentException iae) {
                fragment = false;
            }
            init (client, fragment);
        }

        public AuthorizationEndpointResponse(OAuthClient client, boolean fragment) {
            init(client, fragment);
        }

        private void init(OAuthClient client, boolean fragment) {
            isRedirected = client.getCurrentRequest().equals(client.getRedirectUri());
            Map<String, String> params = fragment ? client.getCurrentFragment() : client.getCurrentQuery();

            code = params.get(OAuth2Constants.CODE);
            state = params.get(OAuth2Constants.STATE);
            error = params.get(OAuth2Constants.ERROR);
            errorDescription = params.get(OAuth2Constants.ERROR_DESCRIPTION);
            accessToken = params.get(OAuth2Constants.ACCESS_TOKEN);
            idToken = params.get(OAuth2Constants.ID_TOKEN);
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

        public String getErrorDescription() {
            return errorDescription;
        }

        public String getAccessToken() {
            return accessToken;
        }

        public String getIdToken() {
            return idToken;
        }
    }

    public static class AccessTokenResponse {
        private int statusCode;

        private String idToken;
        private String accessToken;
        private String tokenType;
        private int expiresIn;
        private int refreshExpiresIn;
        private String refreshToken;

        private String error;
        private String errorDescription;

        public AccessTokenResponse(CloseableHttpResponse response) throws Exception {
            try {
                statusCode = response.getStatusLine().getStatusCode();
                if (!"application/json".equals(response.getHeaders("Content-Type")[0].getValue())) {
                    Assert.fail("Invalid content type");
                }

                String s = IOUtils.toString(response.getEntity().getContent());
                Map responseJson = JsonSerialization.readValue(s, Map.class);

                if (statusCode == 200) {
                    idToken = (String) responseJson.get("id_token");
                    accessToken = (String) responseJson.get("access_token");
                    tokenType = (String) responseJson.get("token_type");
                    expiresIn = (Integer) responseJson.get("expires_in");
                    refreshExpiresIn = (Integer) responseJson.get("refresh_expires_in");

                    if (responseJson.containsKey(OAuth2Constants.REFRESH_TOKEN)) {
                        refreshToken = (String) responseJson.get(OAuth2Constants.REFRESH_TOKEN);
                    }
                } else {
                    error = (String) responseJson.get(OAuth2Constants.ERROR);
                    errorDescription = responseJson.containsKey(OAuth2Constants.ERROR_DESCRIPTION) ? (String) responseJson.get(OAuth2Constants.ERROR_DESCRIPTION) : null;
                }
            } finally {
                response.close();
            }
        }

        public String getIdToken() {
            return idToken;
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

    public PublicKey getRealmPublicKey(String realm) {
        if (!publicKeys.containsKey(realm)) {
            KeysMetadataRepresentation keyMetadata = adminClient.realms().realm(realm).keys().getKeyMetadata();
            String activeKid = keyMetadata.getActive().get("RSA");
            PublicKey publicKey = null;
            for (KeysMetadataRepresentation.KeyMetadataRepresentation rep : keyMetadata.getKeys()) {
                if (rep.getKid().equals(activeKid)) {
                    publicKey = PemUtils.decodePublicKey(rep.getPublicKey());
                }
            }
            publicKeys.put(realm, publicKey);
        }

        return publicKeys.get(realm);
    }

}