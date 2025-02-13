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

package org.keycloak.testsuite.util.oauth;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.UriBuilder;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.jboss.arquillian.drone.webdriver.htmlunit.DroneHtmlUnitDriver;
import org.keycloak.OAuth2Constants;
import org.keycloak.TokenVerifier;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.common.VerificationException;
import org.keycloak.constants.AdapterConstants;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.AsymmetricSignatureSignerContext;
import org.keycloak.crypto.AsymmetricSignatureVerifierContext;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.crypto.ServerECDSASignatureSignerContext;
import org.keycloak.crypto.ServerECDSASignatureVerifierContext;
import org.keycloak.crypto.SignatureSignerContext;
import org.keycloak.jose.jwk.JSONWebKeySet;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.models.Constants;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.protocol.oidc.grants.ciba.CibaGrantType;
import org.keycloak.protocol.oidc.grants.ciba.channel.AuthenticationChannelResponse;
import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AuthorizationResponseToken;
import org.keycloak.representations.ClaimsRepresentation;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.RefreshToken;
import org.keycloak.representations.UserInfo;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.testsuite.broker.util.SimpleHttpDefault;
import org.keycloak.testsuite.runonserver.RunOnServerException;
import org.keycloak.testsuite.util.DroneUtils;
import org.keycloak.testsuite.util.WaitUtils;
import org.keycloak.util.BasicAuthHelper;
import org.keycloak.util.JsonSerialization;
import org.keycloak.util.TokenUtil;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.keycloak.protocol.oidc.OIDCLoginProtocol.LOGIN_HINT_PARAM;
import static org.keycloak.protocol.oidc.grants.ciba.CibaGrantType.AUTH_REQ_ID;
import static org.keycloak.protocol.oidc.grants.ciba.CibaGrantType.BINDING_MESSAGE;
import static org.keycloak.testsuite.admin.Users.getPasswordOf;
import static org.keycloak.testsuite.util.ServerURLs.getAuthServerContextRoot;
import static org.keycloak.testsuite.util.ServerURLs.removeDefaultPorts;
import static org.keycloak.testsuite.util.UIUtils.clickLink;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
public class OAuthClient {

    private static final Logger logger = LoggerFactory.getLogger(OAuthClient.class);

    public static String SERVER_ROOT;
    public static String AUTH_SERVER_ROOT;
    public static String APP_ROOT;
    public static String APP_AUTH_ROOT;
    static final boolean SSL_REQUIRED = Boolean.parseBoolean(System.getProperty("auth.server.ssl.required"));

    static {
        updateURLs(getAuthServerContextRoot());
    }

    // Workaround, but many tests directly use system properties like OAuthClient.AUTH_SERVER_ROOT instead of taking the URL from suite context
    public static void updateURLs(String serverRoot) {
        SERVER_ROOT = removeDefaultPorts(serverRoot);
        AUTH_SERVER_ROOT = SERVER_ROOT + "/auth";
        updateAppRootRealm("master");
    }

    public static void updateAppRootRealm(String realm) {
        APP_ROOT = AUTH_SERVER_ROOT + "/realms/" + realm + "/app";
        APP_AUTH_ROOT = APP_ROOT + "/auth";
    }

    public static void resetAppRootRealm() {
        updateAppRootRealm("master");
    }

    private HttpClientManager httpClientManager = new HttpClientManager();

    private KeyManager keyManager = new KeyManager(this);

    private WebDriver driver;

    private String baseUrl = AUTH_SERVER_ROOT;

    private String realm;

    private String clientId;

    private String redirectUri;

    private String postLogoutRedirectUri;

    private String idTokenHint;

    private String initiatingIDP;

    private String kcAction;

    private StateParamProvider state;

    private String scope;

    private String uiLocales;

    private String clientSessionState;

    private String clientSessionHost;

    private String maxAge;

    private String prompt;

    private String responseType;

    private String responseMode;

    private String nonce;

    private String request;

    private String requestUri;

    private String claims;

    private Map<String, String> requestHeaders;

    private String codeVerifier;
    private String codeChallenge;
    private String codeChallengeMethod;
    private String origin;
    private String dpopProof;
    private String dpopJkt;

    private Map<String, String> customParameters;

    private boolean openid = true;

    public void init(WebDriver driver) {
        this.driver = driver;

        baseUrl = AUTH_SERVER_ROOT;
        realm = "test";
        clientId = "test-app";
        redirectUri = APP_ROOT + "/auth";
        postLogoutRedirectUri = APP_ROOT + "/auth";
        state = KeycloakModelUtils::generateId;
        scope = null;
        uiLocales = null;
        clientSessionState = null;
        clientSessionHost = null;
        maxAge = null;
        prompt = null;
        responseType = OAuth2Constants.CODE;
        responseMode = null;
        nonce = null;
        request = null;
        requestUri = null;
        claims = null;
        codeVerifier = null;
        codeChallenge = null;
        codeChallengeMethod = null;
        origin = null;
        dpopProof = null;
        dpopJkt = null;
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

    public AuthorizationEndpointResponse doSilentLogin() {
        openLoginForm();
        WaitUtils.waitForPageToLoad();

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
        WebElement usernameField = driver.findElement(By.name("username"));

        try {
            usernameField.clear();
            usernameField.sendKeys(username);
        } catch (NoSuchElementException nse) {
            // we might have clicked on a social login icon and might need to wait for the login to appear.
            // avoid waiting by default to avoid the delay.
            WaitUtils.waitUntilElement(usernameField).is().present();
            usernameField.clear();
        } catch (Throwable t) {
            logger.error("Unexpected page was loaded\n{}", src);
            throw t;
        }

        WebElement passwordField = driver.findElement(By.name("password"));
        passwordField.clear();
        passwordField.sendKeys(password);

        if (rememberMe) {
            driver.findElement(By.id("rememberMe")).click();
        }

        driver.findElement(By.name("login")).click();

    }

    public void doLoginGrant(String username, String password) {
        openLoginForm();
        fillLoginForm(username, password);
    }

    public AccessTokenResponse doAccessTokenRequest(String code) {
        return new AccessTokenRequest(code, this).send();
    }

    public AccessTokenResponse doAccessTokenRequest(String code, String clientSecret) {
        return new AccessTokenRequest(code, this).clientSecret(clientSecret).send();
    }

    public String introspectTokenWithClientCredential(String clientId, String clientSecret, String tokenType, String tokenToIntrospect) {
        HttpPost post = new HttpPost(getEndpoints().getIntrospection());

        if (requestHeaders != null) {
            for (Map.Entry<String, String> header : requestHeaders.entrySet()) {
                post.addHeader(header.getKey(), header.getValue());
            }
        }

        String authorization = BasicAuthHelper.createHeader(clientId, clientSecret);
        post.setHeader("Authorization", authorization);

        List<NameValuePair> parameters = new LinkedList<>();

        parameters.add(new BasicNameValuePair("token", tokenToIntrospect));
        parameters.add(new BasicNameValuePair("token_type_hint", tokenType));

        UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(parameters, StandardCharsets.UTF_8);

        post.setEntity(formEntity);

        try (CloseableHttpResponse response = httpClientManager.get().execute(post)) {
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
        return doGrantAccessTokenRequest(username, password, clientId, clientSecret);
    }

    public AccessTokenResponse doGrantAccessTokenRequest(String username, String password, String clientId, String clientSecret) throws Exception {
        return new PasswordGrantRequest(username, password, clientId, clientSecret, this).send();
    }

    public PasswordGrantRequest passwordGrantRequest(String username, String password) {
        return new PasswordGrantRequest(username, password, clientId, null, this);
    }

    public AccessTokenResponse doTokenExchange(String realm, String token, String targetAudience,
                                               String clientId, String clientSecret) throws Exception {
        return doTokenExchange(realm, token, targetAudience, clientId, clientSecret, null);
    }

    public AccessTokenResponse doTokenExchange(String realm, String token, String targetAudience,
                                               String clientId, String clientSecret, Map<String, String> additionalParams) throws Exception {
        List<String> targetAudienceList = targetAudience == null ? null : List.of(targetAudience);
        return doTokenExchange(realm, token, targetAudienceList, clientId, clientSecret, additionalParams);
    }

    public AccessTokenResponse doTokenExchange(String realm, String token, List<String> targetAudiences,
                                               String clientId, String clientSecret, Map<String, String> additionalParams) throws Exception {
        return new TokenExchangeRequest(realm, token, clientId, clientSecret, this)
                .audience(targetAudiences)
                .additionalParams(additionalParams).send();
    }

    public JSONWebKeySet doCertsRequest() throws Exception {
        HttpGet get = new HttpGet(getEndpoints().getJwks());
        try (CloseableHttpResponse response = httpClientManager.get().execute(get)) {
            return JsonSerialization.readValue(response.getEntity().getContent(), JSONWebKeySet.class);
        }
    }

    public AccessTokenResponse doClientCredentialsGrantAccessTokenRequest(String clientSecret) throws Exception {
        HttpPost post = new HttpPost(getEndpoints().getToken());

        if (clientSecret != null) {
            String authorization = BasicAuthHelper.RFC6749.createHeader(clientId, clientSecret);
            post.setHeader("Authorization", authorization);
        }
        if (dpopProof != null) {
            post.addHeader(TokenUtil.TOKEN_TYPE_DPOP, dpopProof);
        }

        List<NameValuePair> parameters = new LinkedList<>();
        parameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, OAuth2Constants.CLIENT_CREDENTIALS));

        String scopeParam = openid ? TokenUtil.attachOIDCScope(scope) : scope;
        if (scopeParam != null && !scopeParam.isEmpty()) {
            parameters.add(new BasicNameValuePair(OAuth2Constants.SCOPE, scopeParam));
        }

        UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(parameters, StandardCharsets.UTF_8);
        post.setEntity(formEntity);

        return new AccessTokenResponse(httpClientManager.get().execute(post));
    }

    public AuthenticationRequestAcknowledgement doBackchannelAuthenticationRequest(String clientId, String clientSecret, String userid, String bindingMessage, String acrValues) throws Exception {
        return doBackchannelAuthenticationRequest(clientId, clientSecret, userid, bindingMessage, acrValues, null, null);
    }

    public AuthenticationRequestAcknowledgement doBackchannelAuthenticationRequest(String clientId, String clientSecret, String userid, String bindingMessage, String acrValues, String clientNotificationToken, Map<String, String> additionalParams) throws Exception {
        HttpPost post = new HttpPost(getEndpoints().getBackchannelAuthentication());

        String authorization = BasicAuthHelper.createHeader(clientId, clientSecret);
        post.setHeader("Authorization", authorization);

        List<NameValuePair> parameters = new LinkedList<>();
        if (userid != null) parameters.add(new BasicNameValuePair(LOGIN_HINT_PARAM, userid));
        if (bindingMessage != null) parameters.add(new BasicNameValuePair(BINDING_MESSAGE, bindingMessage));
        if (acrValues != null) parameters.add(new BasicNameValuePair(OAuth2Constants.ACR_VALUES, acrValues));
        if (clientNotificationToken != null)
            parameters.add(new BasicNameValuePair(CibaGrantType.CLIENT_NOTIFICATION_TOKEN, clientNotificationToken));
        if (scope != null) {
            parameters.add(new BasicNameValuePair(OAuth2Constants.SCOPE, OAuth2Constants.SCOPE_OPENID + " " + scope));
        } else {
            parameters.add(new BasicNameValuePair(OAuth2Constants.SCOPE, OAuth2Constants.SCOPE_OPENID));
        }
        if (requestUri != null) {
            parameters.add(new BasicNameValuePair(OIDCLoginProtocol.REQUEST_URI_PARAM, requestUri));
        }
        if (request != null) {
            parameters.add(new BasicNameValuePair(OIDCLoginProtocol.REQUEST_PARAM, request));
        }
        if (claims != null) {
            parameters.add(new BasicNameValuePair(OIDCLoginProtocol.CLAIMS_PARAM, claims));
        }
        if (additionalParams != null) {
            for (Map.Entry<String, String> entry : additionalParams.entrySet()) {
                parameters.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
            }
        }

        UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(parameters, StandardCharsets.UTF_8);
        post.setEntity(formEntity);

        return new AuthenticationRequestAcknowledgement(httpClientManager.get().execute(post));
    }

    public int doAuthenticationChannelCallback(String requestToken, AuthenticationChannelResponse.Status authStatus) throws Exception {
        HttpPost post = new HttpPost(getEndpoints().getBackchannelAuthenticationCallback());

        String authorization = TokenUtil.TOKEN_TYPE_BEARER + " " + requestToken;
        post.setHeader("Authorization", authorization);

        post.setEntity(new StringEntity(JsonSerialization.writeValueAsString(new AuthenticationChannelResponse(authStatus)), ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpClientManager.get().execute(post)) {
            return response.getStatusLine().getStatusCode();
        }
    }

    public AccessTokenResponse doBackchannelAuthenticationTokenRequest(String clientSecret, String authReqId) throws Exception {
        return doBackchannelAuthenticationTokenRequest(this.clientId, clientSecret, authReqId);
    }

    public AccessTokenResponse doBackchannelAuthenticationTokenRequest(String clientId, String clientSecret, String authReqId) throws Exception {
        HttpPost post = new HttpPost(getEndpoints().getToken());

        String authorization = BasicAuthHelper.createHeader(clientId, clientSecret);
        post.setHeader("Authorization", authorization);

        List<NameValuePair> parameters = new LinkedList<>();
        parameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, OAuth2Constants.CIBA_GRANT_TYPE));
        parameters.add(new BasicNameValuePair(AUTH_REQ_ID, authReqId));

        UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(parameters, StandardCharsets.UTF_8);
        post.setEntity(formEntity);

        return new AccessTokenResponse(httpClientManager.get().execute(post));
    }

    public LogoutResponse doLogout(String refreshToken, String clientSecret) {
        HttpPost post = new HttpPost(getEndpoints().getLogout());

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
        if (origin != null) {
            post.addHeader("Origin", origin);
        }

        UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(parameters, StandardCharsets.UTF_8);
        post.setEntity(formEntity);

        try {
            return new LogoutResponse(httpClientManager.get().execute(post));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public BackchannelLogoutResponse doBackchannelLogout(String logoutToken) {
        HttpPost post = new HttpPost(getEndpoints().getBackChannelLogout());
        List<NameValuePair> parameters = new LinkedList<>();
        if (logoutToken != null) {
            parameters.add(new BasicNameValuePair(OAuth2Constants.LOGOUT_TOKEN, logoutToken));
        }

        UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(parameters, StandardCharsets.UTF_8);
        post.setEntity(formEntity);

        try {
            return new BackchannelLogoutResponse(httpClientManager.get().execute(post));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public TokenRevocationResponse doTokenRevoke(String token, String tokenTypeHint, String clientSecret) {
        try {
            return doTokenRevoke(token, tokenTypeHint, clientSecret, httpClientManager.get());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public TokenRevocationResponse doTokenRevoke(String token, String tokenTypeHint, String clientSecret,
                                               CloseableHttpClient client) throws IOException {
        HttpPost post = new HttpPost(getEndpoints().getRevocation());

        List<NameValuePair> parameters = new LinkedList<>();
        if (token != null) {
            parameters.add(new BasicNameValuePair("token", token));
        }
        if (tokenTypeHint != null) {
            parameters.add(new BasicNameValuePair("token_type_hint", tokenTypeHint));
        }

        if (origin != null) {
            post.addHeader("Origin", origin);
        }

        if (clientId != null && clientSecret != null) {
            String authorization = BasicAuthHelper.createHeader(clientId, clientSecret);
            post.setHeader("Authorization", authorization);
        } else if (clientId != null) {
            parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ID, clientId));
        }

        if (dpopProof != null) {
            post.addHeader(TokenUtil.TOKEN_TYPE_DPOP, dpopProof);
        }

        UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(parameters, StandardCharsets.UTF_8);
        post.setEntity(formEntity);

        return new TokenRevocationResponse(client.execute(post));
    }

    public AccessTokenResponse doRefreshTokenRequest(String refreshToken, String password) {
        HttpPost post = new HttpPost(getEndpoints().getToken());

        List<NameValuePair> parameters = new LinkedList<>();
        parameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, OAuth2Constants.REFRESH_TOKEN));

        if (origin != null) {
            post.addHeader("Origin", origin);
        }
        if (refreshToken != null) {
            parameters.add(new BasicNameValuePair(OAuth2Constants.REFRESH_TOKEN, refreshToken));
        }
        if (scope != null) {
            parameters.add(new BasicNameValuePair(OAuth2Constants.SCOPE, scope));
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

        if (dpopProof != null) {
            post.addHeader(TokenUtil.TOKEN_TYPE_DPOP, dpopProof);
        }

        UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(parameters, StandardCharsets.UTF_8);
        post.setEntity(formEntity);

        try {
            return new AccessTokenResponse(httpClientManager.get().execute(post));
        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve access token", e);
        }
    }

    public DeviceAuthorizationResponse doDeviceAuthorizationRequest(String clientId, String clientSecret) throws Exception {
        HttpPost post = new HttpPost(getEndpoints().getDeviceAuthorization());

        List<NameValuePair> parameters = new LinkedList<>();
        if (clientSecret != null) {
            String authorization = BasicAuthHelper.createHeader(clientId, clientSecret);
            post.setHeader("Authorization", authorization);
        } else {
            parameters.add(new BasicNameValuePair("client_id", clientId));
        }

        if (origin != null) {
            post.addHeader("Origin", origin);
        }

        String scopeParam = openid ? TokenUtil.attachOIDCScope(scope) : scope;
        if (scopeParam != null && !scopeParam.isEmpty()) {
            parameters.add(new BasicNameValuePair(OAuth2Constants.SCOPE, scopeParam));
        }
        if (nonce != null) {
            parameters.add(new BasicNameValuePair(OIDCLoginProtocol.NONCE_PARAM, nonce));
        }
        if (codeChallenge != null) {
            parameters.add(new BasicNameValuePair(OAuth2Constants.CODE_CHALLENGE, codeChallenge));
        }
        if (codeChallengeMethod != null) {
            parameters.add(new BasicNameValuePair(OAuth2Constants.CODE_CHALLENGE_METHOD, codeChallengeMethod));
        }

        UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(parameters, StandardCharsets.UTF_8);
        post.setEntity(formEntity);

        return new DeviceAuthorizationResponse(httpClientManager.get().execute(post));
    }

    public AccessTokenResponse doDeviceTokenRequest(String clientId, String clientSecret, String deviceCode) throws Exception {
        HttpPost post = new HttpPost(getEndpoints().getToken());

        List<NameValuePair> parameters = new LinkedList<>();
        parameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, OAuth2Constants.DEVICE_CODE_GRANT_TYPE));
        parameters.add(new BasicNameValuePair("device_code", deviceCode));
        if (clientSecret != null) {
            String authorization = BasicAuthHelper.createHeader(clientId, clientSecret);
            post.setHeader("Authorization", authorization);
        } else {
            parameters.add(new BasicNameValuePair("client_id", clientId));
        }

        if (origin != null) {
            post.addHeader("Origin", origin);
        }

        if (codeVerifier != null) {
            parameters.add(new BasicNameValuePair(OAuth2Constants.CODE_VERIFIER, codeVerifier));
        }

        UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(parameters, StandardCharsets.UTF_8);
        post.setEntity(formEntity);

        return new AccessTokenResponse(httpClientManager.get().execute(post));
    }

    public OIDCConfigurationRepresentation doWellKnownRequest() {
        try {
            SimpleHttp request = SimpleHttpDefault.doGet(baseUrl + "/realms/" + realm + "/.well-known/openid-configuration",
                    httpClientManager.get());
            if (requestHeaders != null) {
                for (Map.Entry<String, String> entry : requestHeaders.entrySet()) {
                    request.header(entry.getKey(), entry.getValue());
                }
            }
            return request.asJson(OIDCConfigurationRepresentation.class);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public UserInfo doUserInfoRequest(String accessToken) {
        HttpGet get = new HttpGet(getEndpoints().getUserInfo());
        get.setHeader("Authorization", "Bearer " + accessToken);
        try {
            return new UserInfoResponse(httpClientManager.get().execute(get)).getUserInfo();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public UserInfoResponse doUserInfoRequestByGet(AccessTokenResponse accessTokenResponse) throws Exception {
        HttpGet get = new HttpGet(getEndpoints().getUserInfo());
        get.setHeader("Authorization", accessTokenResponse.getTokenType() + " " + accessTokenResponse.getAccessToken());
        if (dpopProof != null) {
            get.addHeader(TokenUtil.TOKEN_TYPE_DPOP, dpopProof);
        }
        try {
            return new UserInfoResponse(httpClientManager.get().execute(get));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public ParResponse doPushedAuthorizationRequest(String clientId, String clientSecret) throws IOException {
        return doPushedAuthorizationRequest(clientId, clientSecret, null);
    }

    public ParResponse doPushedAuthorizationRequest(String clientId, String clientSecret, String signedJwt) throws IOException {
        HttpPost post = new HttpPost(getEndpoints().getPushedAuthorizationRequest());

        List<NameValuePair> parameters = new LinkedList<>();

        if (signedJwt != null) {
            parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION_TYPE, OAuth2Constants.CLIENT_ASSERTION_TYPE_JWT));
            parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION, signedJwt));
        }

        if (origin != null) {
            post.addHeader("Origin", origin);
        }
        if (responseType != null) {
            parameters.add(new BasicNameValuePair(OAuth2Constants.RESPONSE_TYPE, responseType));
        }
        if (responseMode != null) {
            parameters.add(new BasicNameValuePair(OIDCLoginProtocol.RESPONSE_MODE_PARAM, responseMode));
        }
        if (clientId != null && clientSecret != null) {
            parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ID, clientId));
            parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_SECRET, clientSecret));
        }
        else if (clientId != null) {
            parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ID, clientId));
        }
        if (redirectUri != null) {
            parameters.add(new BasicNameValuePair(OAuth2Constants.REDIRECT_URI, redirectUri));
        }
        if (kcAction != null) {
            parameters.add(new BasicNameValuePair(Constants.KC_ACTION, kcAction));
        }
        // on authz request, state is putting automatically so that.
        // if state is put here, they are not matched.
        //String state = this.state.getState();
        //if (state != null) {
        //    parameters.add(new BasicNameValuePair(OAuth2Constants.STATE, state));
        //}
        if (uiLocales != null) {
            parameters.add(new BasicNameValuePair(OAuth2Constants.UI_LOCALES_PARAM, uiLocales));
        }
        if (nonce != null) {
            parameters.add(new BasicNameValuePair(OIDCLoginProtocol.NONCE_PARAM, nonce));
        }
        String scopeParam = openid ? TokenUtil.attachOIDCScope(scope) : scope;
        if (scopeParam != null && !scopeParam.isEmpty()) {
            parameters.add(new BasicNameValuePair(OAuth2Constants.SCOPE, scopeParam));
        }
        if (maxAge != null) {
            parameters.add(new BasicNameValuePair(OIDCLoginProtocol.MAX_AGE_PARAM, maxAge));
        }
        if (prompt != null) {
            parameters.add(new BasicNameValuePair(OIDCLoginProtocol.PROMPT_PARAM, prompt));
        }
        if (request != null) {
            parameters.add(new BasicNameValuePair(OIDCLoginProtocol.REQUEST_PARAM, request));
        }
        if (requestUri != null) {
            parameters.add(new BasicNameValuePair(OIDCLoginProtocol.REQUEST_URI_PARAM, requestUri));
        }
        if (claims != null) {
            parameters.add(new BasicNameValuePair(OIDCLoginProtocol.CLAIMS_PARAM, claims));
        }
        if (codeChallenge != null) {
            parameters.add(new BasicNameValuePair(OAuth2Constants.CODE_CHALLENGE, codeChallenge));
        }
        if (codeChallengeMethod != null) {
            parameters.add(new BasicNameValuePair(OAuth2Constants.CODE_CHALLENGE_METHOD, codeChallengeMethod));
        }
        if (customParameters != null) {
            customParameters.keySet().stream().forEach(i -> parameters.add(new BasicNameValuePair(i, customParameters.get(i))));
        }
        if (dpopJkt != null) {
            parameters.add(new BasicNameValuePair(OIDCLoginProtocol.DPOP_JKT, dpopJkt));
        }
        if (dpopProof != null) {
            post.addHeader(TokenUtil.TOKEN_TYPE_DPOP, dpopProof);
        }

        UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(parameters, StandardCharsets.UTF_8);
        post.setEntity(formEntity);
        try {
            return new ParResponse(httpClientManager.get().execute(post));
        } catch (Exception e) {
            throw new RuntimeException("Failed to do PAR request", e);
        }
    }

    public HttpClientManager httpClient() {
        return httpClientManager;
    }

    public AccessToken verifyToken(String token) {
        return verifyToken(token, AccessToken.class);
    }

    public IDToken verifyIDToken(String token) {
        return verifyToken(token, IDToken.class);
    }

    public AuthorizationResponseToken verifyAuthorizationResponseToken(String token) {
        return verifyToken(token, AuthorizationResponseToken.class);
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
            KeyWrapper key = keyManager.getPublicKey(algorithm, kid);
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
        return createSigner(privateKey, kid, algorithm, null);
    }

    public SignatureSignerContext createSigner(PrivateKey privateKey, String kid, String algorithm, String curve) {
        KeyWrapper keyWrapper = new KeyWrapper();
        keyWrapper.setAlgorithm(algorithm);
        keyWrapper.setKid(kid);
        keyWrapper.setPrivateKey(privateKey);
        keyWrapper.setCurve(curve);
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
            if (index == -1) {
                index = driver.getCurrentUrl().length();
            }
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
        List<NameValuePair> pairs = URLEncodedUtils.parse(getCurrentUri(), StandardCharsets.UTF_8);
        for (NameValuePair p : pairs) {
            m.put(p.getName(), p.getValue());
        }
        return m;
    }

    public Map<String, String> getCurrentFragment() {
        Map<String, String> m = new HashMap<>();

        String fragment = getCurrentUri().getRawFragment();
        List<NameValuePair> pairs = (fragment == null || fragment.isEmpty()) ? Collections.emptyList() : URLEncodedUtils.parse(fragment, StandardCharsets.UTF_8);

        for (NameValuePair p : pairs) {
            m.put(p.getName(), p.getValue());
        }
        return m;
    }

    public void openLoginForm() {
        driver.navigate().to(getLoginFormUrl());
    }

    public void openRegistrationForm() {
        driver.navigate().to(getRegistrationFormUrl());
    }

    public void openLogout() {
        UriBuilder b = OIDCLoginProtocolService.logoutUrl(UriBuilder.fromUri(baseUrl));
        if (postLogoutRedirectUri != null) {
            b.queryParam(OAuth2Constants.POST_LOGOUT_REDIRECT_URI, postLogoutRedirectUri);
        }
        if (idTokenHint != null) {
            b.queryParam(OAuth2Constants.ID_TOKEN_HINT, idTokenHint);
        }
        if (initiatingIDP != null) {
            b.queryParam(AuthenticationManager.INITIATING_IDP_PARAM, initiatingIDP);
        }
        driver.navigate().to(b.build(realm).toString());
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public String getState() {
        return state.getState();
    }

    public String getNonce() {
        return nonce;
    }

    public String getLoginFormUrl() {
        return this.getLoginFormUrl(this.baseUrl);
    }

    public String getResponseMode() {
        return responseMode;
    }

    public String getResponseType() {
        return responseType;
    }

    public String getCodeVerifier() {
        return codeVerifier;
    }

    public String getRegisterationsUrl() {
        return this.getLoginFormUrl(this.baseUrl).replace("openid-connect/auth", "openid-connect/registrations");
    }

    public String getLoginFormUrl(String baseUrl) {
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
        if (uiLocales != null) {
            b.queryParam(OAuth2Constants.UI_LOCALES_PARAM, uiLocales);
        }
        if (nonce != null) {
            b.queryParam(OIDCLoginProtocol.NONCE_PARAM, nonce);
        }

        String scopeParam = openid ? TokenUtil.attachOIDCScope(scope) : scope;
        if (scopeParam != null && !scopeParam.isEmpty()) {
            b.queryParam(OAuth2Constants.SCOPE, scopeParam);
        }

        if (maxAge != null) {
            b.queryParam(OIDCLoginProtocol.MAX_AGE_PARAM, maxAge);
        }
        if (prompt != null) {
            b.queryParam(OIDCLoginProtocol.PROMPT_PARAM, prompt);
        }
        if (request != null) {
            b.queryParam(OIDCLoginProtocol.REQUEST_PARAM, request);
        }
        if (requestUri != null) {
            b.queryParam(OIDCLoginProtocol.REQUEST_URI_PARAM, requestUri);
        }
        if (claims != null) {
            b.queryParam(OIDCLoginProtocol.CLAIMS_PARAM, claims);
        }
        if (codeChallenge != null) {
            b.queryParam(OAuth2Constants.CODE_CHALLENGE, codeChallenge);
        }
        if (codeChallengeMethod != null) {
            b.queryParam(OAuth2Constants.CODE_CHALLENGE_METHOD, codeChallengeMethod);
        }
        if (dpopJkt != null) {
            b.queryParam(OIDCLoginProtocol.DPOP_JKT, dpopJkt);
        }
        if (customParameters != null) {
            customParameters.keySet().stream().forEach(i -> b.queryParam(i, customParameters.get(i)));
        }

        return b.build(realm).toString();
    }

    private String getRegistrationFormUrl() {
        UriBuilder b = OIDCLoginProtocolService.registrationsUrl(UriBuilder.fromUri(baseUrl));
        if (responseType != null) {
            b.queryParam(OAuth2Constants.RESPONSE_TYPE, responseType);
        }
        if (clientId != null) {
            b.queryParam(OAuth2Constants.CLIENT_ID, clientId);
        }
        if (redirectUri != null) {
            b.queryParam(OAuth2Constants.REDIRECT_URI, redirectUri);
        }

        String scopeParam = openid ? TokenUtil.attachOIDCScope(scope) : scope;
        if (scopeParam != null && !scopeParam.isEmpty()) {
            b.queryParam(OAuth2Constants.SCOPE, scopeParam);
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

    public Endpoints getEndpoints() {
        return new Endpoints(baseUrl, realm);
    }

    public Endpoints getEndpoints(String realm) {
        return new Endpoints(baseUrl, realm);
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

    public OAuthClient postLogoutRedirectUri(String postLogoutRedirectUri) {
        this.postLogoutRedirectUri = postLogoutRedirectUri;
        return this;
    }

    public OAuthClient idTokenHint(String idTokenHint) {
        this.idTokenHint = idTokenHint;
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

    public OAuthClient uiLocales(String uiLocales) {
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

    public OAuthClient prompt(String prompt) {
        this.prompt = prompt;
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

    public OAuthClient claims(ClaimsRepresentation claims) {
        if (claims == null) {
            this.claims = null;
        } else {
            try {
                this.claims = URLEncoder.encode(JsonSerialization.writeValueAsString(claims), StandardCharsets.UTF_8);
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
        }
        return this;
    }

    public String getRealm() {
        return realm;
    }

    Map<String, String> getRequestHeaders() {
        return requestHeaders;
    }

    String getDpopProof() {
        return dpopProof;
    }

    String getOrigin() {
        return origin;
    }

    String getClientSessionState() {
        return clientSessionState;
    }

    String getClientSessionHost() {
        return clientSessionHost;
    }

    boolean isOpenid() {
        return openid;
    }

    String getScope() {
        return scope;
    }

    Map<String, String> getCustomParameters() {
        return customParameters;
    }

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

    public OAuthClient dpopProof(String dpopProof) {
        this.dpopProof = dpopProof;
        return this;
    }

    public OAuthClient dpopJkt(String dpopJkt) {
        this.dpopJkt = dpopJkt;
        return this;
    }

    public OAuthClient addCustomParameter(String key, String value) {
        if (customParameters == null) {
            customParameters = new HashMap<>();
        }
        customParameters.put(key, value);
        return this;
    }

    public OAuthClient removeCustomParameter(String key) {
        if (customParameters != null) {
            customParameters.remove(key);
        }
        return this;
    }

    public OAuthClient requestHeaders(Map<String, String> headers) {
        this.requestHeaders = headers;
        return this;
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

    private WebElement findSocialButton(String alias) {
        String id = "social-" + alias;
        return DroneUtils.getCurrentDriver().findElement(By.id(id));
    }

    private interface StateParamProvider {

        String getState();

    }

}
