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

import org.apache.http.impl.client.CloseableHttpClient;
import org.keycloak.OAuth2Constants;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.ClaimsRepresentation;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.util.JsonSerialization;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.PageFactory;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
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
        clientSessionState = null;
        clientSessionHost = null;
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

    public OAuthClient clientSessionState(String client_session_state) {
        this.clientSessionState = client_session_state;
        return this;
    }

    public OAuthClient clientSessionHost(String client_session_host) {
        this.clientSessionHost = client_session_host;
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
