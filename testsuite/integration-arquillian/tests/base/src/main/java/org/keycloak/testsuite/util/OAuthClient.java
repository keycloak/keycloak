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

import com.google.common.base.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.jboss.arquillian.drone.webdriver.htmlunit.DroneHtmlUnitDriver;
import org.junit.Assert;
import org.keycloak.OAuth2Constants;
import org.keycloak.TokenVerifier;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.common.VerificationException;
import org.keycloak.common.util.KeystoreUtil;
import org.keycloak.constants.AdapterConstants;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.AsymmetricSignatureSignerContext;
import org.keycloak.crypto.AsymmetricSignatureVerifierContext;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.crypto.ServerECDSASignatureSignerContext;
import org.keycloak.crypto.ServerECDSASignatureVerifierContext;
import org.keycloak.crypto.SignatureSignerContext;
import org.keycloak.jose.jwk.JSONWebKeySet;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jwk.JWKParser;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.models.Constants;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;
import org.keycloak.protocol.oidc.utils.OIDCResponseType;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.RefreshToken;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.arquillian.AuthServerTestEnricher;
import org.keycloak.testsuite.runonserver.RunOnServerException;
import org.keycloak.util.BasicAuthHelper;
import org.keycloak.util.JsonSerialization;
import org.keycloak.util.TokenUtil;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.keycloak.testsuite.admin.Users.getPasswordOf;
import static org.keycloak.testsuite.util.UIUtils.clickLink;
import static org.keycloak.testsuite.util.URLUtils.removeDefaultPorts;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
public class OAuthClient {
    public static String SERVER_ROOT;
    public static String AUTH_SERVER_ROOT;
    public static String APP_ROOT;
    public static String APP_AUTH_ROOT;
    private static final boolean sslRequired = Boolean.parseBoolean(System.getProperty("auth.server.ssl.required"));

    static {
        updateURLs(AuthServerTestEnricher.getAuthServerContextRoot());
    }

    // Workaround, but many tests directly use system properties like OAuthClient.AUTH_SERVER_ROOT instead of taking the URL from suite context
    public static void updateURLs(String serverRoot) {
        SERVER_ROOT = removeDefaultPorts(serverRoot);
        AUTH_SERVER_ROOT = SERVER_ROOT + "/auth";
        APP_ROOT = AUTH_SERVER_ROOT + "/realms/master/app";
        APP_AUTH_ROOT = APP_ROOT + "/auth";
    }

    private WebDriver driver;

    private String baseUrl = AUTH_SERVER_ROOT;

    private String realm;

    private String clientId;

    private String redirectUri;
    
    private String kcAction;

    private StateParamProvider state;

    private String scope;

    private String uiLocales;

    private String clientSessionState;

    private String clientSessionHost;

    private String maxAge;

    private String responseType;

    private String responseMode;

    private String nonce;

    private String request;

    private String requestUri;

    private Map<String, JSONWebKeySet> publicKeys = new HashMap<>();

    // https://tools.ietf.org/html/rfc7636#section-4
    private String codeVerifier;
    private String codeChallenge;
    private String codeChallengeMethod;
    private String origin;

    private Map<String, String> customParameters;

    private boolean openid = true;

    private Supplier<CloseableHttpClient> httpClient = OAuthClient::newCloseableHttpClient;

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

    public void init(WebDriver driver) {
        this.driver = driver;

        baseUrl = AUTH_SERVER_ROOT;
        realm = "test";
        clientId = "test-app";
        redirectUri = APP_ROOT + "/auth";
        state = () -> {
            return KeycloakModelUtils.generateId();
        };
        scope = null;
        uiLocales = null;
        clientSessionState = null;
        clientSessionHost = null;
        maxAge = null;
        responseType = OAuth2Constants.CODE;
        responseMode = null;
        nonce = null;
        request = null;
        requestUri = null;
        // https://tools.ietf.org/html/rfc7636#section-4
        codeVerifier = null;
        codeChallenge = null;
        codeChallengeMethod = null;
        origin = null;
        customParameters = null;
        openid = true;
    }

    public void setDriver(WebDriver driver) {
        this.driver = driver;
    }

    public AuthorizationEndpointResponse doLogin(String username, String password) {
        openLoginForm();
        fillLoginForm(username, password);

        return new AuthorizationEndpointResponse(this);
    }

    public AuthorizationEndpointResponse doLoginSocial(String brokerId, String username, String password) {
        openLoginForm();
        WaitUtils.waitForPageToLoad();

        WebElement socialButton = findSocialButton(brokerId);
        clickLink(socialButton);
        fillLoginForm(username, password);

        return new AuthorizationEndpointResponse(this);
    }

    public AuthorizationEndpointResponse doLogin(UserRepresentation user) {

        return doLogin(user.getUsername(), getPasswordOf(user));
    }

    public AuthorizationEndpointResponse doRememberMeLogin(String username, String password) {
        openLoginForm();
        fillLoginForm(username, password, true);

        return new AuthorizationEndpointResponse(this);
    }

    public void fillLoginForm(String username, String password) {
        this.fillLoginForm(username, password, false);
    }

    public void fillLoginForm(String username, String password, boolean rememberMe) {
        WaitUtils.waitForPageToLoad();
        String src = driver.getPageSource();
        try {
            driver.findElement(By.id("username")).sendKeys(username);
            driver.findElement(By.id("password")).sendKeys(password);
            if (rememberMe) {
                driver.findElement(By.id("rememberMe")).click();
            }
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

    public OAuthClient httpClient(Supplier<CloseableHttpClient> client) {
        this.httpClient = client;
        return this;
    }

    public Supplier<CloseableHttpClient> getHttpClient() {
        return httpClient;
    }

    public static CloseableHttpClient newCloseableHttpClient() {
        if (sslRequired) {
            String keyStorePath = System.getProperty("client.certificate.keystore");
            String keyStorePassword = System.getProperty("client.certificate.keystore.passphrase");
            String trustStorePath = System.getProperty("client.truststore");
            String trustStorePassword = System.getProperty("client.truststore.passphrase");
            return newCloseableHttpClientSSL(keyStorePath, keyStorePassword, trustStorePath, trustStorePassword);
        }
        return HttpClientBuilder.create().build();
    }

    public static CloseableHttpClient newCloseableHttpClientSSL(String keyStorePath,
            String keyStorePassword, String trustStorePath, String trustStorePassword) {
        KeyStore keystore = null;
        // load the keystore containing the client certificate - keystore type is probably jks or pkcs12
        try {
            keystore = KeystoreUtil.loadKeyStore(keyStorePath, keyStorePassword);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // load the trustore
        KeyStore truststore = null;
        try {
            truststore = KeystoreUtil.loadKeyStore(trustStorePath, trustStorePassword);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return (CloseableHttpClient) new org.keycloak.adapters.HttpClientBuilder()
                .keyStore(keystore, keyStorePassword)
                .trustStore(truststore)
                .hostnameVerification(org.keycloak.adapters.HttpClientBuilder.HostnameVerificationPolicy.ANY)
                .build();
    }

    public CloseableHttpResponse doPreflightRequest() {
        try (CloseableHttpClient client = httpClient.get()) {
            HttpOptions options = new HttpOptions(getAccessTokenUrl());
            options.setHeader("Origin", "http://example.com");

            return client.execute(options);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    // KEYCLOAK-6771 Certificate Bound Token
    public AccessTokenResponse doAccessTokenRequest(String code, String password) {
        try (CloseableHttpClient client = httpClient.get()) {
            return doAccessTokenRequest(code, password, client);
        }  catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    // KEYCLOAK-6771 Certificate Bound Token
    public AccessTokenResponse doAccessTokenRequest(String code, String password, CloseableHttpClient client) {
        HttpPost post = new HttpPost(getAccessTokenUrl());

        List<NameValuePair> parameters = new LinkedList<>();
        parameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, OAuth2Constants.AUTHORIZATION_CODE));

        if (origin != null) {
            post.addHeader("Origin", origin);
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

        UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(parameters, Charsets.UTF_8);
        post.setEntity(formEntity);

        try {
            return new AccessTokenResponse(client.execute(post));
        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve access token", e);
        }
    }

    // KEYCLOAK-6771 Certificate Bound Token
    public String introspectTokenWithClientCredential(String clientId, String clientSecret, String tokenType, String tokenToIntrospect) {
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            return introspectTokenWithClientCredential(clientId, clientSecret, tokenType, tokenToIntrospect, client);
        }  catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    // KEYCLOAK-6771 Certificate Bound Token
    public String introspectTokenWithClientCredential(String clientId, String clientSecret, String tokenType, String tokenToIntrospect, CloseableHttpClient client) {
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

        try (CloseableHttpResponse response = client.execute(post)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            response.getEntity().writeTo(out);
            return new String(out.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve access token", e);
        }
    }

    public String introspectAccessTokenWithClientCredential(String clientId, String clientSecret, String tokenToIntrospect) {
        return introspectTokenWithClientCredential(clientId, clientSecret, "access_token", tokenToIntrospect);
    }

    public String introspectRefreshTokenWithClientCredential(String clientId, String clientSecret, String tokenToIntrospect) {
        return introspectTokenWithClientCredential(clientId, clientSecret, "refresh_token", tokenToIntrospect);
    }

    public AccessTokenResponse doGrantAccessTokenRequest(String clientSecret, String username, String password) throws Exception {
        return doGrantAccessTokenRequest(realm, username, password, null, clientId, clientSecret);
    }

    public AccessTokenResponse doGrantAccessTokenRequest(String clientSecret, String username,  String password, String otp) throws Exception {
        return doGrantAccessTokenRequest(realm, username, password, otp, clientId, clientSecret);
    }

    public AccessTokenResponse doGrantAccessTokenRequest(String realm, String username, String password, String totp,
                                                         String clientId, String clientSecret) throws Exception {
        return doGrantAccessTokenRequest(realm, username, password, totp, clientId, clientSecret, null);
    }

    public AccessTokenResponse doGrantAccessTokenRequest(String realm, String username, String password, String totp,
                                                         String clientId, String clientSecret, String userAgent) throws Exception {
        try (CloseableHttpClient client = httpClient.get()) {
            HttpPost post = new HttpPost(getResourceOwnerPasswordCredentialGrantUrl(realm));

            List<NameValuePair> parameters = new LinkedList<>();
            parameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, OAuth2Constants.PASSWORD));
            parameters.add(new BasicNameValuePair("username", username));
            parameters.add(new BasicNameValuePair("password", password));
            if (totp != null) {
                parameters.add(new BasicNameValuePair("otp", totp));

            }
            if (clientSecret != null) {
                String authorization = BasicAuthHelper.createHeader(clientId, clientSecret);
                post.setHeader("Authorization", authorization);
            } else {
                parameters.add(new BasicNameValuePair("client_id", clientId));
            }

            if (origin != null) {
                post.addHeader("Origin", origin);
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

            if (userAgent != null) {
                post.addHeader("User-Agent", userAgent);
            }

            UrlEncodedFormEntity formEntity;
            try {
                formEntity = new UrlEncodedFormEntity(parameters, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
            post.setEntity(formEntity);

            return new AccessTokenResponse(client.execute(post));
        }
    }

    public AccessTokenResponse doTokenExchange(String realm, String token, String targetAudience,
                                               String clientId, String clientSecret) throws Exception {
        try (CloseableHttpClient client = httpClient.get()) {
            HttpPost post = new HttpPost(getResourceOwnerPasswordCredentialGrantUrl(realm));

            List<NameValuePair> parameters = new LinkedList<>();
            parameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, OAuth2Constants.TOKEN_EXCHANGE_GRANT_TYPE));
            parameters.add(new BasicNameValuePair(OAuth2Constants.SUBJECT_TOKEN, token));
            parameters.add(new BasicNameValuePair(OAuth2Constants.SUBJECT_TOKEN_TYPE, OAuth2Constants.ACCESS_TOKEN_TYPE));
            parameters.add(new BasicNameValuePair(OAuth2Constants.AUDIENCE, targetAudience));

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
        }
    }

    public AccessTokenResponse doTokenExchange(String realm, String clientId, String clientSecret, Map<String, String> params) throws Exception {
        try (CloseableHttpClient client = httpClient.get()) {
            HttpPost post = new HttpPost(getResourceOwnerPasswordCredentialGrantUrl(realm));

            List<NameValuePair> parameters = new LinkedList<>();
            parameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, OAuth2Constants.TOKEN_EXCHANGE_GRANT_TYPE));
            for (Map.Entry<String, String> entry : params.entrySet()) {
                parameters.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));

            }

            if (clientSecret != null) {
                String authorization = BasicAuthHelper.createHeader(clientId, clientSecret);
                post.setHeader("Authorization", authorization);
            } else {
                parameters.add(new BasicNameValuePair("client_id", clientId));

            }

           UrlEncodedFormEntity formEntity;
            try {
                formEntity = new UrlEncodedFormEntity(parameters, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
            post.setEntity(formEntity);

            return new AccessTokenResponse(client.execute(post));
        }
    }


    public JSONWebKeySet doCertsRequest(String realm) throws Exception {
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            HttpGet get = new HttpGet(getCertsUrl(realm));
            CloseableHttpResponse response = client.execute(get);
            return JsonSerialization.readValue(response.getEntity().getContent(), JSONWebKeySet.class);
        }
    }

    public AccessTokenResponse doClientCredentialsGrantAccessTokenRequest(String clientSecret) throws Exception {
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            HttpPost post = new HttpPost(getServiceAccountUrl());

            String authorization = BasicAuthHelper.createHeader(clientId, clientSecret);
            post.setHeader("Authorization", authorization);

            List<NameValuePair> parameters = new LinkedList<>();
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
        } 
    }

    // KEYCLOAK-6771 Certificate Bound Token
    public CloseableHttpResponse doLogout(String refreshToken, String clientSecret) {
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            return doLogout(refreshToken, clientSecret, client);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    // KEYCLOAK-6771 Certificate Bound Token
    public CloseableHttpResponse doLogout(String refreshToken, String clientSecret, CloseableHttpClient client) throws IOException {
        HttpPost post = new HttpPost(getLogoutUrl().build());

        List<NameValuePair> parameters = new LinkedList<>();
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
    }

    // KEYCLOAK-6771 Certificate Bound Token
    public AccessTokenResponse doRefreshTokenRequest(String refreshToken, String password) {
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            return doRefreshTokenRequest(refreshToken, password, client);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    // KEYCLOAK-6771 Certificate Bound Token
    public AccessTokenResponse doRefreshTokenRequest(String refreshToken, String password, CloseableHttpClient client) {
        HttpPost post = new HttpPost(getRefreshTokenUrl());

        List<NameValuePair> parameters = new LinkedList<>();
        parameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, OAuth2Constants.REFRESH_TOKEN));

        if (origin != null) {
            post.addHeader("Origin", origin);
        }
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
    }

    public OIDCConfigurationRepresentation doWellKnownRequest(String realm) {
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            return SimpleHttp.doGet(baseUrl + "/realms/" + realm + "/.well-known/openid-configuration", client).asJson(OIDCConfigurationRepresentation.class);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
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
        return verifyToken(token, AccessToken.class);
    }

    public IDToken verifyIDToken(String token) {
        return verifyToken(token, IDToken.class);
    }

    public RefreshToken parseRefreshToken(String refreshToken) {
        try {
            return new JWSInput(refreshToken).readJsonContent(RefreshToken.class);
        } catch (Exception e) {
            throw new RunOnServerException(e);
        }
    }

    public <T extends JsonWebToken> T verifyToken(String token, Class<T> clazz) {
        try {
            TokenVerifier<T> verifier = TokenVerifier.create(token, clazz);
            String kid = verifier.getHeader().getKeyId();
            String algorithm = verifier.getHeader().getAlgorithm().name();
            KeyWrapper key = getRealmPublicKey(realm, algorithm, kid);
            AsymmetricSignatureVerifierContext verifierContext;
            switch (algorithm) {
                case Algorithm.ES256:
                case Algorithm.ES384:
                case Algorithm.ES512:
                    verifierContext = new ServerECDSASignatureVerifierContext(key);
                    break;
                default:
                    verifierContext = new AsymmetricSignatureVerifierContext(key);
            }
            verifier.verifierContext(verifierContext);
            verifier.verify();
            return verifier.getToken();
        } catch (VerificationException e) {
            throw new RuntimeException("Failed to decode token", e);
        }
    }

    public SignatureSignerContext createSigner(PrivateKey privateKey, String kid, String algorithm) {
        KeyWrapper keyWrapper = new KeyWrapper();
        keyWrapper.setAlgorithm(algorithm);
        keyWrapper.setKid(kid);
        keyWrapper.setPrivateKey(privateKey);
        SignatureSignerContext signer;
        switch (algorithm) {
            case Algorithm.ES256:
            case Algorithm.ES384:
            case Algorithm.ES512:
                signer = new ServerECDSASignatureSignerContext(keyWrapper);
                break;
            default:
                signer = new AsymmetricSignatureSignerContext(keyWrapper);
        }
        return signer;
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
        Map<String, String> m = new HashMap<>();
        List<NameValuePair> pairs = URLEncodedUtils.parse(getCurrentUri(), "UTF-8");
        for (NameValuePair p : pairs) {
            m.put(p.getName(), p.getValue());
        }
        return m;
    }

    public Map<String, String> getCurrentFragment() {
        Map<String, String> m = new HashMap<>();

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
    
    /**
     * Application-initiated action.
     * 
     * @return The action name.
     */
    public String getKcAction() {
        return kcAction;
    }

    public String getState() {
        return state.getState();
    }

    public String getNonce() {
        return nonce;
    }

    public String getLoginFormUrl() {
        UriBuilder b = OIDCLoginProtocolService.authUrl(UriBuilder.fromUri(baseUrl));
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
        if (kcAction != null) {
            b.queryParam(Constants.KC_ACTION, kcAction);
        }
        String state = this.state.getState();
        if (state != null) {
            b.queryParam(OAuth2Constants.STATE, state);
        }
        if (uiLocales != null){
            b.queryParam(OAuth2Constants.UI_LOCALES_PARAM, uiLocales);
        }
        if (nonce != null){
            b.queryParam(OIDCLoginProtocol.NONCE_PARAM, nonce);
        }

        String scopeParam = openid ? TokenUtil.attachOIDCScope(scope) : scope;
        if (scopeParam != null && !scopeParam.isEmpty()) {
            b.queryParam(OAuth2Constants.SCOPE, scopeParam);
        }

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
        if (customParameters != null) {
            customParameters.keySet().stream().forEach(i -> b.queryParam(i, customParameters.get(i)));
        }

        return b.build(realm).toString();
    }

    public Entity getLoginEntityForPOST() {
        Form form = new Form()
                .param(OAuth2Constants.SCOPE, TokenUtil.attachOIDCScope(scope))
                .param(OAuth2Constants.RESPONSE_TYPE, responseType)
                .param(OAuth2Constants.CLIENT_ID, clientId)
                .param(OAuth2Constants.REDIRECT_URI, redirectUri)
                .param(OAuth2Constants.STATE, this.state.getState());
        
        return Entity.form(form);
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

    public OAuthClient baseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
        return this;
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
    
    public OAuthClient kcAction(String kcAction) {
        this.kcAction = kcAction;
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

    public OAuthClient openid(boolean openid) {
        this.openid = openid;
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
    public OAuthClient origin(String origin) {
        this.origin = origin;
        return this;
    }

    public OAuthClient addCustomerParameter(String key, String value) {
        if (customParameters == null) {
            customParameters = new HashMap<>();
        }
        customParameters.put(key, value);
        return this;
    }

    public static class AuthorizationEndpointResponse {

        private boolean isRedirected;
        private String code;
        private String state;
        private String error;
        private String errorDescription;

        private String sessionState;

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

            if ("fragment".equals(client.responseMode)) {
                fragment = true;
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
            sessionState = params.get(OAuth2Constants.SESSION_STATE);
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

        public String getSessionState() {
            return sessionState;
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
        // OIDC Financial API Read Only Profile : scope MUST be returned in the response from Token Endpoint
        private String scope;
        private String sessionState;

        private String error;
        private String errorDescription;

        private Map<String, String> headers;

        public AccessTokenResponse(CloseableHttpResponse response) throws Exception {
            try {
                statusCode = response.getStatusLine().getStatusCode();

                headers = new HashMap<>();

                for (Header h : response.getAllHeaders()) {
                    headers.put(h.getName(), h.getValue());
                }

                Header[] contentTypeHeaders = response.getHeaders("Content-Type");
                String contentType = (contentTypeHeaders != null && contentTypeHeaders.length > 0) ? contentTypeHeaders[0].getValue() : null;
                if (!"application/json".equals(contentType)) {
                    Assert.fail("Invalid content type. Status: " + statusCode + ", contentType: " + contentType);
                }

                String s = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
                Map responseJson = JsonSerialization.readValue(s, Map.class);

                if (statusCode == 200) {
                    idToken = (String) responseJson.get("id_token");
                    accessToken = (String) responseJson.get("access_token");
                    tokenType = (String) responseJson.get("token_type");
                    expiresIn = (Integer) responseJson.get("expires_in");
                    refreshExpiresIn = (Integer) responseJson.get("refresh_expires_in");
                    sessionState = (String) responseJson.get("session_state");

                    // OIDC Financial API Read Only Profile : scope MUST be returned in the response from Token Endpoint
                    if (responseJson.containsKey(OAuth2Constants.SCOPE)) {
                        scope = (String) responseJson.get(OAuth2Constants.SCOPE);
                    }

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

        // OIDC Financial API Read Only Profile : scope MUST be returned in the response from Token Endpoint
        public String getScope() {
            return scope;
        }

        public String getSessionState() {
            return sessionState;
        }

        public Map<String, String> getHeaders() {
            return headers;
        }
    }

    private KeyWrapper getRealmPublicKey(String realm, String algoritm, String kid) {
        boolean loadedKeysFromServer = false;
        JSONWebKeySet jsonWebKeySet = publicKeys.get(realm);
        if (jsonWebKeySet == null) {
            jsonWebKeySet = getRealmKeys(realm);
            publicKeys.put(realm, jsonWebKeySet);
            loadedKeysFromServer = true;
        }

        KeyWrapper key = findKey(jsonWebKeySet, algoritm, kid);

        if (key == null && !loadedKeysFromServer) {
            jsonWebKeySet = getRealmKeys(realm);
            publicKeys.put(realm, jsonWebKeySet);

            key = findKey(jsonWebKeySet, algoritm, kid);
        }

        if (key == null) {
            throw new RuntimeException("Public key for realm:" + realm + ", algorithm: " + algoritm + " not found");
        }

        return key;
    }

    private JSONWebKeySet getRealmKeys(String realm) {
        String certUrl = baseUrl + "/realms/" + realm + "/protocol/openid-connect/certs";
        try (CloseableHttpClient client = httpClient.get()){
            return SimpleHttp.doGet(certUrl, client).asJson(JSONWebKeySet.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to retrieve keys", e);
        }
    }

    private KeyWrapper findKey(JSONWebKeySet jsonWebKeySet, String algoritm, String kid) {
        for (JWK k : jsonWebKeySet.getKeys()) {
            if (k.getKeyId().equals(kid) && k.getAlgorithm().equals(algoritm)) {
                PublicKey publicKey = JWKParser.create(k).toPublicKey();

                KeyWrapper key = new KeyWrapper();
                key.setKid(k.getKeyId());
                key.setAlgorithm(k.getAlgorithm());
                key.setPublicKey(publicKey);
                key.setUse(KeyUse.SIG);

                return key;
            }
        }
        return null;
    }

    public void removeCachedPublicKeys() {
        publicKeys.clear();
    }

    public void setBrowserHeader(String name, String value) {
        if (driver instanceof DroneHtmlUnitDriver) {
            DroneHtmlUnitDriver droneDriver = (DroneHtmlUnitDriver) this.driver;
            droneDriver.getWebClient().removeRequestHeader(name);
            droneDriver.getWebClient().addRequestHeader(name, value);
        }
    }

    public WebDriver getDriver() {
        return driver;
    }

    private WebElement findSocialButton(String providerId) {
        String id = "zocial-" + providerId;
        return DroneUtils.getCurrentDriver().findElement(By.id(id));
    }
    
    private interface StateParamProvider {

        String getState();

    }

}
