package org.keycloak.testsuite.util.oauth;

import org.apache.http.impl.client.CloseableHttpClient;
import org.keycloak.OAuth2Constants;
import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AuthorizationResponseToken;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.RefreshToken;
import org.keycloak.testsuite.util.oauth.ciba.CibaClient;
import org.keycloak.testsuite.util.oauth.device.DeviceClient;
import org.openqa.selenium.WebDriver;

import java.util.Map;

public abstract class AbstractOAuthClient<T> {

    protected String baseUrl;
    protected OAuthClientConfig config;

    protected Map<String, String> customParameters;
    protected String codeChallenge;
    protected String codeChallengeMethod;
    protected String codeVerifier;
    protected String clientSessionState;
    protected String clientSessionHost;
    protected String dpopJkt;
    protected String dpopProof;
    protected String request;
    protected String requestUri;
    protected String claims;

    private final KeyManager keyManager = new KeyManager(this);
    private final TokensManager tokensManager = new TokensManager(keyManager);
    protected HttpClientManager httpClientManager;
    protected WebDriver driver;

    public AbstractOAuthClient(String baseUrl, CloseableHttpClient httpClient, WebDriver webDriver) {
        this.baseUrl = baseUrl;
        this.driver = webDriver;
        this.httpClientManager = new HttpClientManager(httpClient);
    }

    public T client(String clientId) {
        config.client(clientId);
        return (T) this;
    }

    public T client(String clientId, String clientSecret) {
        config.client(clientId, clientSecret);
        return (T) this;
    }

    public LoginUrlBuilder loginForm() {
        return new LoginUrlBuilder(this);
    }

    public void openLoginForm() {
        loginForm().open();
    }

    public AuthorizationEndpointResponse doLogin(String username, String password) {
        openLoginForm();
        fillLoginForm(username, password);
        return parseLoginResponse();
    }

    public abstract void fillLoginForm(String username, String password);

    public void openRegistrationForm() {
        driver.navigate().to(registrationForm().build());
    }

    public RegistrationUrlBuilder registrationForm() {
        return new RegistrationUrlBuilder(this);
    }

    public AuthorizationEndpointResponse parseLoginResponse() {
        return new AuthorizationEndpointResponse(this);
    }

    public PasswordGrantRequest passwordGrantRequest(String username, String password) {
        return new PasswordGrantRequest(username, password, this);
    }

    public AccessTokenResponse doPasswordGrantRequest(String username, String password) {
        return passwordGrantRequest(username, password).send();
    }

    public AccessTokenRequest accessTokenRequest(String code) {
        return new AccessTokenRequest(code, this);
    }

    public AccessTokenResponse doAccessTokenRequest(String code) {
        return accessTokenRequest(code).send();
    }

    public ClientCredentialsGrantRequest clientCredentialsGrantRequest() {
        return new ClientCredentialsGrantRequest(this);
    }

    public AccessTokenResponse doClientCredentialsGrantAccessTokenRequest() {
        return clientCredentialsGrantRequest().send();
    }

    public RefreshRequest refreshRequest(String refreshToken) {
        return new RefreshRequest(refreshToken, this);
    }

    public AccessTokenResponse doRefreshTokenRequest(String refreshToken) {
        return refreshRequest(refreshToken).send();
    }

    public LogoutUrlBuilder logoutForm() {
        return new LogoutUrlBuilder(this);
    }

    public void openLogoutForm() {
        logoutForm().open();
    }

    public LogoutRequest logoutRequest(String refreshToken) {
        return new LogoutRequest(refreshToken, this);
    }

    public LogoutResponse doLogout(String refreshToken) {
        return logoutRequest(refreshToken).send();
    }

    public BackchannelLogoutRequest backchannelLogoutRequest(String logoutToken) {
        return new BackchannelLogoutRequest(logoutToken, this);
    }

    public BackchannelLogoutResponse doBackchannelLogout(String logoutToken) {
        return backchannelLogoutRequest(logoutToken).send();
    }

    public OpenIDProviderConfigurationRequest wellknownRequest() {
        return new OpenIDProviderConfigurationRequest(this);
    }

    public OIDCConfigurationRepresentation doWellKnownRequest() {
        return wellknownRequest().send().getOidcConfiguration();
    }

    public UserInfoRequest userInfoRequest(String accessToken) {
        return new UserInfoRequest(accessToken, this);
    }

    public UserInfoResponse doUserInfoRequest(String accessToken) {
        return userInfoRequest(accessToken).send();
    }

    public IntrospectionRequest introspectionRequest(String tokenToIntrospect) {
        return new IntrospectionRequest(tokenToIntrospect, this);
    }

    public IntrospectionResponse doIntrospectionRequest(String tokenToIntrospect, String tokenType) {
        return introspectionRequest(tokenToIntrospect).tokenTypeHint(tokenType).send();
    }

    public IntrospectionResponse doIntrospectionAccessTokenRequest(String tokenToIntrospect) {
        return introspectionRequest(tokenToIntrospect).tokenTypeHint("access_token").send();
    }

    public IntrospectionResponse doIntrospectionRefreshTokenRequest(String tokenToIntrospect) {
        return introspectionRequest(tokenToIntrospect).tokenTypeHint("refresh_token").send();
    }

    public TokenRevocationRequest tokenRevocationRequest(String token) {
        return new TokenRevocationRequest(token, this);
    }

    public TokenRevocationResponse doTokenRevoke(String token) {
        return tokenRevocationRequest(token).send();
    }

    public TokenExchangeRequest tokenExchangeRequest(String subjectToken) {
        return tokenExchangeRequest(subjectToken, OAuth2Constants.ACCESS_TOKEN_TYPE);
    }

    public TokenExchangeRequest tokenExchangeRequest(String subjectToken, String subjectTokenType) {
        return new TokenExchangeRequest(subjectToken, subjectTokenType, this);
    }

    public AccessTokenResponse doTokenExchange(String subjectToken) {
        return tokenExchangeRequest(subjectToken).send();
    }

    public CibaClient ciba() {
        return new CibaClient(this);
    }

    public DeviceClient device() {
        return new DeviceClient(this);
    }

    public ParRequest pushedAuthorizationRequest() {
        return new ParRequest(this);
    }

    public ParResponse doPushedAuthorizationRequest() {
        return pushedAuthorizationRequest().send();
    }

    public <J extends JsonWebToken> J parseToken(String token, Class<J> clazz) {
        return tokensManager.parseToken(token, clazz);
    }

    public RefreshToken parseRefreshToken(String refreshToken) {
        return tokensManager.parseToken(refreshToken, RefreshToken.class);
    }

    public AccessToken verifyToken(String token) {
        return tokensManager.verifyToken(token, AccessToken.class);
    }

    public IDToken verifyIDToken(String token) {
        return tokensManager.verifyToken(token, IDToken.class);
    }

    public AuthorizationResponseToken verifyAuthorizationResponseToken(String token) {
        return tokensManager.verifyToken(token, AuthorizationResponseToken.class);
    }

    public <J extends JsonWebToken> J verifyToken(String token, Class<J> clazz) {
        return tokensManager.verifyToken(token, clazz);
    }

    public T baseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
        return (T) this;
    }

    public OAuthClientConfig config() {
        return config;
    }

    public T driver(WebDriver webDriver) {
        this.driver = webDriver;
        return (T) this;
    }

    public HttpClientManager httpClient() {
        return httpClientManager;
    }

    public KeyManager keys() {
        return keyManager;
    }

    public Endpoints getEndpoints() {
        return new Endpoints(baseUrl, config.getRealm());
    }

    public String getRealm() {
        return config.getRealm();
    }

    public String getRedirectUri() {
        return config.getRedirectUri();
    }

    String getClientSessionState() {
        return clientSessionState;
    }

    String getClientSessionHost() {
        return clientSessionHost;
    }

    public String getCodeChallenge() {
        return codeChallenge;
    }

    public String getCodeChallengeMethod() {
        return codeChallengeMethod;
    }

    public String getCodeVerifier() {
        return codeVerifier;
    }

    Map<String, String> getCustomParameters() {
        return customParameters;
    }

    String getDpopJkt() {
        return dpopJkt;
    }

    String getDpopProof() {
        return dpopProof;
    }

    public String getRequestUri() {
        return requestUri;
    }

    public String getRequest() {
        return request;
    }

    public String getClaims() {
        return claims;
    }

}
