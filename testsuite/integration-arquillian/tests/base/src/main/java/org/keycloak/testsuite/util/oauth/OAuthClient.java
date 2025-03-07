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

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.keycloak.OAuth2Constants;
import org.keycloak.models.Constants;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.grants.ciba.channel.AuthenticationChannelResponse;
import org.keycloak.representations.ClaimsRepresentation;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.util.BasicAuthHelper;
import org.keycloak.util.JsonSerialization;
import org.keycloak.util.TokenUtil;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.PageFactory;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.keycloak.testsuite.util.ServerURLs.getAuthServerContextRoot;
import static org.keycloak.testsuite.util.ServerURLs.removeDefaultPorts;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
public class OAuthClient extends AbstractOAuthClient<OAuthClient> {

    public static String SERVER_ROOT;
    public static String AUTH_SERVER_ROOT;
    public static String APP_ROOT;
    public static String APP_AUTH_ROOT;

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

    public OAuthClient(CloseableHttpClient httpClient, WebDriver webDriver) {
        super(AUTH_SERVER_ROOT, httpClient, webDriver);
        init();
    }

    public OAuthClient newConfig() {
        OAuthClient newClient = new OAuthClient(httpClientManager.get(), driver);
        newClient.init();
        return newClient;
    }

    public void init() {
        config = new OAuthClientConfig()
                .realm("test")
                .client("test-app", "password")
                .redirectUri(APP_ROOT + "/auth")
                .postLogoutRedirectUri(APP_ROOT + "/auth")
                .responseType(OAuth2Constants.CODE);

        state = KeycloakModelUtils::generateId;
        uiLocales = null;
        clientSessionState = null;
        clientSessionHost = null;
        maxAge = null;
        prompt = null;
        nonce = null;
        request = null;
        requestUri = null;
        claims = null;
        codeVerifier = null;
        codeChallenge = null;
        codeChallengeMethod = null;
        dpopProof = null;
        dpopJkt = null;
        customParameters = null;
    }

    public void setDriver(WebDriver driver) {
        this.driver = driver;
    }

    public void fillLoginForm(String username, String password) {
        LoginPage loginPage = new LoginPage();
        PageFactory.initElements(driver, loginPage);
        loginPage.login(username, password);
    }

    public TokenExchangeRequest tokenExchangeRequest(String subjectToken) {
        return new TokenExchangeRequest(subjectToken, this);
    }

    public TokenExchangeRequest tokenExchangeRequest(String subjectToken, String subjectTokenType) {
        return new TokenExchangeRequest(subjectToken, subjectTokenType, this);
    }

    /**
     * @deprecated Set clientId and clientSecret using {@link #client(String, String)} and use {@link #tokenExchangeRequest(String)}
     */
    @Deprecated
    public AccessTokenResponse doTokenExchange(String subjectToken, String targetAudience,
                                               String clientId, String clientSecret) throws Exception {
        return doTokenExchange(subjectToken, targetAudience, clientId, clientSecret, null);
    }

    /**
     * @deprecated Set clientId and clientSecret using {@link #client(String, String)} and use {@link #tokenExchangeRequest(String)}
     */
    @Deprecated
    public AccessTokenResponse doTokenExchange(String subjectToken, String targetAudience,
                                               String clientId, String clientSecret, Map<String, String> additionalParams) throws Exception {
        List<String> targetAudienceList = targetAudience == null ? null : List.of(targetAudience);
        return doTokenExchange(subjectToken, targetAudienceList, clientId, clientSecret, additionalParams);
    }

    /**
     * @deprecated Set clientId and clientSecret using {@link #client(String, String)} and use {@link #tokenExchangeRequest(String)}
     */
    @Deprecated
    public AccessTokenResponse doTokenExchange(String subjectToken, List<String> targetAudiences,
                                               String clientId, String clientSecret, Map<String, String> additionalParams) throws Exception {
        return tokenExchangeRequest(subjectToken)
                .client(clientId, clientSecret)
                .audience(targetAudiences)
                .additionalParams(additionalParams).send();
    }

    // TODO Extract into request class
    public DeviceAuthorizationResponse doDeviceAuthorizationRequest(String clientId, String clientSecret) throws Exception {
        HttpPost post = new HttpPost(getEndpoints().getDeviceAuthorization());

        List<NameValuePair> parameters = new LinkedList<>();
        if (clientSecret != null) {
            String authorization = BasicAuthHelper.createHeader(clientId, clientSecret);
            post.setHeader("Authorization", authorization);
        } else {
            parameters.add(new BasicNameValuePair("client_id", clientId));
        }

        if (config.getOrigin() != null) {
            post.addHeader("Origin", config.getOrigin());
        }

        String scopeParam = config.getScope();
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

    // TODO Extract into request class
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

        if (config.getOrigin() != null) {
            post.addHeader("Origin", config.getOrigin());
        }

        if (codeVerifier != null) {
            parameters.add(new BasicNameValuePair(OAuth2Constants.CODE_VERIFIER, codeVerifier));
        }

        UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(parameters, StandardCharsets.UTF_8);
        post.setEntity(formEntity);

        return new AccessTokenResponse(httpClientManager.get().execute(post));
    }



    // TODO Deprecate
    public ParResponse doPushedAuthorizationRequest(String clientId, String clientSecret) throws IOException {
        return doPushedAuthorizationRequest(clientId, clientSecret, null);
    }

    // TODO Extract into request class
    public ParResponse doPushedAuthorizationRequest(String clientId, String clientSecret, String signedJwt) throws IOException {
        HttpPost post = new HttpPost(getEndpoints().getPushedAuthorizationRequest());

        List<NameValuePair> parameters = new LinkedList<>();

        if (signedJwt != null) {
            parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION_TYPE, OAuth2Constants.CLIENT_ASSERTION_TYPE_JWT));
            parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION, signedJwt));
        }

        if (config.getOrigin() != null) {
            post.addHeader("Origin", config.getOrigin());
        }
        if (config.getResponseType() != null) {
            parameters.add(new BasicNameValuePair(OAuth2Constants.RESPONSE_TYPE, config.getResponseType()));
        }
        if (config.getResponseMode() != null) {
            parameters.add(new BasicNameValuePair(OIDCLoginProtocol.RESPONSE_MODE_PARAM, config.getResponseMode()));
        }
        if (clientId != null && clientSecret != null) {
            parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ID, clientId));
            parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_SECRET, clientSecret));
        }
        else if (clientId != null) {
            parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ID, clientId));
        }
        if (config.getRedirectUri() != null) {
            parameters.add(new BasicNameValuePair(OAuth2Constants.REDIRECT_URI, config.getRedirectUri()));
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
        String scopeParam = config.getScope();
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

    public String getClientId() {
        return config.getClientId();
    }

    public String getScope() {
        return config.getScope();
    }

    public String getState() {
        return state.getState();
    }

    public String getNonce() {
        return nonce;
    }

    public OAuthClient realm(String realm) {
        config.realm(realm);
        return this;
    }

    /**
     * @deprecated This method is deprecated, use {@link OAuthClient#client(String)} for public clients,
     * or {@link OAuthClient#client(String, String)} for confidential clients
     */
    @Deprecated
    public OAuthClient clientId(String clientId) {
        config.clientId(clientId);
        return this;
    }

    public OAuthClient redirectUri(String redirectUri) {
        config.redirectUri(redirectUri);
        return this;
    }

    public OAuthClient postLogoutRedirectUri(String postLogoutRedirectUri) {
        config.postLogoutRedirectUri(postLogoutRedirectUri);
        return this;
    }

    public OAuthClient kcAction(String kcAction) {
        this.kcAction = kcAction;
        return this;
    }

    public OAuthClient stateParamHardcoded(String value) {
        this.state = () -> value;
        return this;
    }

    public OAuthClient stateParamRandom() {
        this.state = KeycloakModelUtils::generateId;
        return this;
    }

    public OAuthClient scope(String scope) {
        config.scope(scope);
        return this;
    }

    public OAuthClient openid(boolean openid) {
        config.openid(openid);
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
        config.responseType(responseType);
        return this;
    }

    public OAuthClient responseMode(String responseMode) {
        config.responseMode(responseMode);
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
        config.origin(origin);
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

    public WebDriver getDriver() {
        return driver;
    }

}
