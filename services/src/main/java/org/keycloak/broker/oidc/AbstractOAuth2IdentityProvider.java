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

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.broker.provider.AbstractIdentityProvider;
import org.keycloak.broker.provider.AuthenticationRequest;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.ExchangeExternalToken;
import org.keycloak.broker.provider.ExchangeTokenToIdentityProviderToken;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.broker.provider.UserAuthenticationIdentityProvider;
import org.keycloak.broker.provider.util.IdentityBrokerState;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.util.SecretGenerator;
import org.keycloak.common.util.Time;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.KeyType;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.crypto.MacSignatureSignerContext;
import org.keycloak.crypto.SignatureProvider;
import org.keycloak.crypto.SignatureSignerContext;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.http.HttpRequest;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.http.simple.SimpleHttpRequest;
import org.keycloak.http.simple.SimpleHttpResponse;
import org.keycloak.jose.jwk.JWKBuilder;
import org.keycloak.jose.jwk.RSAPublicJWK;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.AccessTokenIntrospectionProviderFactory;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.protocol.oidc.TokenExchangeContext;
import org.keycloak.protocol.oidc.TokenExchangeProvider;
import org.keycloak.protocol.oidc.endpoints.AuthorizationEndpoint;
import org.keycloak.protocol.oidc.endpoints.TokenIntrospectionEndpoint;
import org.keycloak.protocol.oidc.utils.PkceUtils;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.idm.ErrorRepresentation;
import org.keycloak.representations.oidc.TokenMetadataRepresentation;
import org.keycloak.services.ErrorPage;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.Urls;
import org.keycloak.services.managers.ClientSessionCode;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.urls.UrlType;
import org.keycloak.util.Booleans;
import org.keycloak.util.JsonSerialization;
import org.keycloak.utils.StringUtil;
import org.keycloak.vault.VaultStringSecret;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.logging.Logger;

/**
 * @author Pedro Igor
 */
public abstract class AbstractOAuth2IdentityProvider<C extends OAuth2IdentityProviderConfig> extends AbstractIdentityProvider<C> implements ExchangeTokenToIdentityProviderToken, ExchangeExternalToken {
    protected static final Logger logger = Logger.getLogger(AbstractOAuth2IdentityProvider.class);

    public static final String OAUTH2_GRANT_TYPE_REFRESH_TOKEN = "refresh_token";
    public static final String OAUTH2_GRANT_TYPE_AUTHORIZATION_CODE = "authorization_code";

    public static final String FEDERATED_REFRESH_TOKEN = "FEDERATED_REFRESH_TOKEN";
    public static final String FEDERATED_TOKEN_EXPIRATION = "FEDERATED_TOKEN_EXPIRATION";
    public static final String ACCESS_DENIED = "access_denied";
    protected static ObjectMapper mapper = new ObjectMapper();

    public static final String OAUTH2_PARAMETER_ACCESS_TOKEN = "access_token";
    public static final String OAUTH2_PARAMETER_SCOPE = "scope";
    public static final String OAUTH2_PARAMETER_STATE = "state";
    public static final String OAUTH2_PARAMETER_RESPONSE_TYPE = "response_type";
    public static final String OAUTH2_PARAMETER_REDIRECT_URI = "redirect_uri";
    public static final String OAUTH2_PARAMETER_CODE = "code";
    public static final String OAUTH2_PARAMETER_CLIENT_ID = "client_id";
    public static final String OAUTH2_PARAMETER_CLIENT_SECRET = "client_secret";
    public static final String OAUTH2_PARAMETER_GRANT_TYPE = "grant_type";

    private static final String BROKER_CODE_CHALLENGE_PARAM = "BROKER_CODE_CHALLENGE";
    private static final String BROKER_CODE_CHALLENGE_METHOD_PARAM = "BROKER_CODE_CHALLENGE_METHOD";

    public static final String ACCESS_TOKEN_EXPIRATION = "accessTokenExpiration";

    public AbstractOAuth2IdentityProvider(KeycloakSession session, C config) {
        super(session, config);

        if (config.getDefaultScope() == null || config.getDefaultScope().isEmpty()) {
            config.setDefaultScope(getDefaultScopes());
        }
    }

    @Override
    public Object callback(RealmModel realm, AuthenticationCallback callback, EventBuilder event) {
        return new Endpoint(callback, realm, event, this);
    }

    @Override
    public Response performLogin(AuthenticationRequest request) {
        try {
            URI authorizationUrl = createAuthorizationUrl(request).build();

            return Response.seeOther(authorizationUrl).build();
        } catch (Exception e) {
            throw new IdentityBrokerException("Could not create authentication request.", e);
        }
    }

    /**
     * This is a custom variant of {@link AccessTokenResponse} which avoid primitives that would auto-add zero values
     * to the original responses. It also allows accessTokenExpiration to be handled as a long value.
     */
    public static class OAuthResponse {
        @JsonProperty(OAuth2Constants.ACCESS_TOKEN)
        protected String token;

        @JsonProperty(OAuth2Constants.EXPIRES_IN)
        protected Long expiresIn;

        @JsonProperty(OAuth2Constants.REFRESH_TOKEN)
        protected String refreshToken;

        @JsonProperty("refresh_expires_in")
        protected Long refreshExpiresIn;

        @JsonProperty(OAuth2Constants.ID_TOKEN)
        protected String idToken;

        @JsonProperty(ACCESS_TOKEN_EXPIRATION)
        protected Long accessTokenExpiration;

        protected Map<String, Object> otherClaims = new HashMap<>();

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public Long getExpiresIn() {
            return expiresIn;
        }

        public void setExpiresIn(Long expiresIn) {
            this.expiresIn = expiresIn;
        }

        public String getRefreshToken() {
            return refreshToken;
        }

        public void setRefreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
        }

        public Long getRefreshExpiresIn() {
            return refreshExpiresIn;
        }

        public void setRefreshExpiresIn(Long refreshExpiresIn) {
            this.refreshExpiresIn = refreshExpiresIn;
        }

        public String getIdToken() {
            return idToken;
        }

        public void setIdToken(String idToken) {
            this.idToken = idToken;
        }

        public Long getAccessTokenExpiration() {
            return accessTokenExpiration;
        }

        public void setAccessTokenExpiration(Long accessTokenExpiration) {
            this.accessTokenExpiration = accessTokenExpiration;
        }

        @JsonAnyGetter
        public Map<String, Object> getOtherClaims() {
            return otherClaims;
        }

        @JsonAnySetter
        public void setOtherClaims(String name, Object value) {
            otherClaims.put(name, value);
        }

    }

    @Override
    public Response retrieveToken(KeycloakSession session, FederatedIdentityModel identity) {
        try {
            if (identity.getToken().startsWith("{")) {
                OAuthResponse previousResponse = JsonSerialization.readValue(identity.getToken(), OAuthResponse.class);
                Long exp = previousResponse.getAccessTokenExpiration();
                if (needsRefresh(exp) && previousResponse.getRefreshToken() != null) {
                    OAuthResponse newResponse = refreshToken(previousResponse, session);
                    if (newResponse.getExpiresIn() != null && newResponse.getExpiresIn() > 0) {
                        long accessTokenExpiration = Time.currentTime() + newResponse.getExpiresIn();
                        newResponse.setAccessTokenExpiration(accessTokenExpiration);
                    }
                    identity.setToken(JsonSerialization.writeValueAsString(newResponse));
                }
            }
        } catch (IOException e) {
            ErrorRepresentation error = new ErrorRepresentation();
            error.setErrorMessage("Unable to refresh token");
            throw new WebApplicationException("Unable to refresh token", e,
                    Response.status(Response.Status.BAD_GATEWAY).entity(error).type(MediaType.APPLICATION_JSON).build());
        }
        return Response.ok(identity.getToken()).type(MediaType.APPLICATION_JSON).build();
    }

    private boolean needsRefresh(Long exp) {
        return exp != null && exp != 0 && exp < Time.currentTime() + getConfig().getMinValidityToken();
    }

    protected SimpleHttpRequest getRefreshTokenRequest(KeycloakSession session, String refreshToken, String clientId, String clientSecret) {
        SimpleHttpRequest refreshTokenRequest = SimpleHttp.create(session).doPost(getConfig().getTokenUrl())
                .param(OAUTH2_GRANT_TYPE_REFRESH_TOKEN, refreshToken)
                .param(OAUTH2_PARAMETER_GRANT_TYPE, OAUTH2_GRANT_TYPE_REFRESH_TOKEN);
        return authenticateTokenRequest(refreshTokenRequest);
    }

    private OAuthResponse refreshToken(OAuthResponse previousResponse, KeycloakSession session) throws IOException {
        try (VaultStringSecret vaultStringSecret = session.vault().getStringSecret(getConfig().getClientSecret())) {
            SimpleHttpRequest refreshTokenRequest = getRefreshTokenRequest(session, previousResponse.getRefreshToken(), getConfig().getClientId(), vaultStringSecret.get().orElse(getConfig().getClientSecret()));
            try (SimpleHttpResponse refreshTokenResponse = refreshTokenRequest.asResponse()) {
                String response = refreshTokenResponse.asString();
                if (response.contains("error")) {
                    ErrorRepresentation error = new ErrorRepresentation();
                    error.setErrorMessage("Unable to refresh token");
                    throw new WebApplicationException("Received and response code " + refreshTokenResponse.getStatus() +
                                                      " with a response '" + refreshTokenResponse.asString() + "'",
                            Response.status(Response.Status.BAD_GATEWAY).entity(error).type(MediaType.APPLICATION_JSON).build());
                }
                OAuthResponse newResponse = JsonSerialization.readValue(response, OAuthResponse.class);

                if (newResponse.getRefreshToken() == null && previousResponse.getRefreshToken() != null) {
                    newResponse.setRefreshToken(previousResponse.getRefreshToken());
                    newResponse.setRefreshExpiresIn(previousResponse.getRefreshExpiresIn());
                }
                if (newResponse.getIdToken() == null && previousResponse.getIdToken() != null) {
                    newResponse.setIdToken(previousResponse.getIdToken());
                }
                return newResponse;
            }
        }
    }

    @Override
    public C getConfig() {
        return super.getConfig();
    }

    protected String extractTokenFromResponse(String response, String tokenName) {
        if(response == null)
            return null;

        if (response.startsWith("{")) {
            try {
                JsonNode node = mapper.readTree(response);
                if(node.has(tokenName)){
                    String s = node.get(tokenName).textValue();
                    if(s == null || s.trim().isEmpty())
                        return null;
                    return s;
                } else {
                    return null;
                }
            } catch (IOException e) {
                throw new IdentityBrokerException("Could not extract token [" + tokenName + "] from response [" + response + "] due: " + e.getMessage(), e);
            }
        } else {
            Matcher matcher = Pattern.compile(tokenName + "=([^&]+)").matcher(response);

            if (matcher.find()) {
                return matcher.group(1);
            }
        }

        return null;
    }

    @Override
    public Response exchangeFromToken(UriInfo uriInfo, EventBuilder event, ClientModel authorizedClient, UserSessionModel tokenUserSession, UserModel tokenSubject, MultivaluedMap<String, String> params) {
        // check to see if we have a token exchange in session
        // in other words check to see if this session was created by an external exchange
        Response tokenResponse = hasExternalExchangeToken(event, tokenUserSession, params);
        if (tokenResponse != null) return tokenResponse;

        // going further we only support access token type?  Why?
        String requestedType = params.getFirst(OAuth2Constants.REQUESTED_TOKEN_TYPE);
        if (requestedType != null && !requestedType.equals(OAuth2Constants.ACCESS_TOKEN_TYPE)) {
            event.detail(Details.REASON, "requested_token_type unsupported");
            event.error(Errors.INVALID_REQUEST);
            return exchangeUnsupportedRequiredType();
        }
        if (Booleans.isFalse(getConfig().isStoreToken())) {
            // if token isn't stored, we need to see if this session has been linked
            String brokerId = tokenUserSession.getNote(Details.IDENTITY_PROVIDER);
            brokerId = brokerId == null ? tokenUserSession.getNote(UserAuthenticationIdentityProvider.EXTERNAL_IDENTITY_PROVIDER) : brokerId;
            if (brokerId == null || !brokerId.equals(getConfig().getAlias())) {
                event.detail(Details.REASON, "requested_issuer has not linked");
                event.error(Errors.INVALID_REQUEST);
                return exchangeNotLinkedNoStore(uriInfo, authorizedClient, tokenUserSession, tokenSubject);
            }
            return exchangeSessionToken(uriInfo, event, authorizedClient, tokenUserSession, tokenSubject);
        } else {
            return exchangeStoredToken(uriInfo, event, authorizedClient, tokenUserSession, tokenSubject);
        }
    }

    /**
     * check to see if we have a token exchange in session
     * in other words check to see if this session was created by an external exchange
     * @param tokenUserSession
     * @param params
     * @return
     */
    protected Response hasExternalExchangeToken(EventBuilder event, UserSessionModel tokenUserSession, MultivaluedMap<String, String> params) {
        if (getConfig().getAlias().equals(tokenUserSession.getNote(OIDCIdentityProvider.EXCHANGE_PROVIDER))) {

            String requestedType = params.getFirst(OAuth2Constants.REQUESTED_TOKEN_TYPE);
            if ((requestedType == null || requestedType.equals(OAuth2Constants.ACCESS_TOKEN_TYPE))) {
                String accessToken = tokenUserSession.getNote(FEDERATED_ACCESS_TOKEN);
                if (accessToken != null) {
                    AccessTokenResponse tokenResponse = new AccessTokenResponse();
                    tokenResponse.setToken(accessToken);
                    tokenResponse.setIdToken(null);
                    tokenResponse.setRefreshToken(null);
                    tokenResponse.setRefreshExpiresIn(0);
                    tokenResponse.setExpiresIn(0);
                    tokenResponse.getOtherClaims().clear();
                    tokenResponse.getOtherClaims().put(OAuth2Constants.ISSUED_TOKEN_TYPE, OAuth2Constants.ACCESS_TOKEN_TYPE);
                    event.success();
                    return Response.ok(tokenResponse).type(MediaType.APPLICATION_JSON_TYPE).build();
                }
            } else if (OAuth2Constants.ID_TOKEN_TYPE.equals(requestedType)) {
                String idToken = tokenUserSession.getNote(OIDCIdentityProvider.FEDERATED_ID_TOKEN);
                if (idToken != null) {
                    AccessTokenResponse tokenResponse = new AccessTokenResponse();
                    tokenResponse.setToken(null);
                    tokenResponse.setIdToken(idToken);
                    tokenResponse.setRefreshToken(null);
                    tokenResponse.setRefreshExpiresIn(0);
                    tokenResponse.setExpiresIn(0);
                    tokenResponse.getOtherClaims().clear();
                    tokenResponse.getOtherClaims().put(OAuth2Constants.ISSUED_TOKEN_TYPE, OAuth2Constants.ID_TOKEN_TYPE);
                    event.success();
                    return Response.ok(tokenResponse).type(MediaType.APPLICATION_JSON_TYPE).build();
                }

            }

        }
        return null;
    }

    protected Response exchangeStoredToken(UriInfo uriInfo, EventBuilder event, ClientModel authorizedClient, UserSessionModel tokenUserSession, UserModel tokenSubject) {
        FederatedIdentityModel model = session.users().getFederatedIdentity(authorizedClient.getRealm(), tokenSubject, getConfig().getAlias());
        if (model == null || model.getToken() == null) {
            event.detail(Details.REASON, "requested_issuer is not linked");
            event.error(Errors.INVALID_TOKEN);
            return exchangeNotLinked(uriInfo, authorizedClient, tokenUserSession, tokenSubject);
        }
        String accessToken = extractTokenFromResponse(model.getToken(), getAccessTokenResponseParameter());
        if (accessToken == null) {
            model.setToken(null);
            session.users().updateFederatedIdentity(authorizedClient.getRealm(), tokenSubject, model);
            event.detail(Details.REASON, "requested_issuer token expired");
            event.error(Errors.INVALID_TOKEN);
            return exchangeTokenExpired(uriInfo, authorizedClient, tokenUserSession, tokenSubject);
        }
        AccessTokenResponse tokenResponse = new AccessTokenResponse();
        tokenResponse.setToken(accessToken);
        tokenResponse.setIdToken(null);
        tokenResponse.setRefreshToken(null);
        tokenResponse.setRefreshExpiresIn(0);
        tokenResponse.getOtherClaims().clear();
        tokenResponse.getOtherClaims().put(OAuth2Constants.ISSUED_TOKEN_TYPE, OAuth2Constants.ACCESS_TOKEN_TYPE);
        tokenResponse.getOtherClaims().put(ACCOUNT_LINK_URL, getLinkingUrl(uriInfo, authorizedClient, tokenUserSession));
        event.success();
        return Response.ok(tokenResponse).type(MediaType.APPLICATION_JSON_TYPE).build();
    }

    protected Response exchangeSessionToken(UriInfo uriInfo, EventBuilder event, ClientModel authorizedClient, UserSessionModel tokenUserSession, UserModel tokenSubject) {
        String accessToken = tokenUserSession.getNote(FEDERATED_ACCESS_TOKEN);
        if (accessToken == null) {
            event.detail(Details.REASON, "requested_issuer is not linked");
            event.error(Errors.INVALID_TOKEN);
            return exchangeTokenExpired(uriInfo, authorizedClient, tokenUserSession, tokenSubject);
        }
        AccessTokenResponse tokenResponse = new AccessTokenResponse();
        tokenResponse.setToken(accessToken);
        tokenResponse.setIdToken(null);
        tokenResponse.setRefreshToken(null);
        tokenResponse.setRefreshExpiresIn(0);
        tokenResponse.getOtherClaims().clear();
        tokenResponse.getOtherClaims().put(OAuth2Constants.ISSUED_TOKEN_TYPE, OAuth2Constants.ACCESS_TOKEN_TYPE);
        tokenResponse.getOtherClaims().put(ACCOUNT_LINK_URL, getLinkingUrl(uriInfo, authorizedClient, tokenUserSession));
        event.success();
        return Response.ok(tokenResponse).type(MediaType.APPLICATION_JSON_TYPE).build();
    }


    public BrokeredIdentityContext getFederatedIdentity(String response) {
        String accessToken = extractTokenFromResponse(response, getAccessTokenResponseParameter());

        if (accessToken == null) {
            throw new IdentityBrokerException("No access token available in OAuth server response: " + response);
        }

        BrokeredIdentityContext context = doGetFederatedIdentity(accessToken);

        if (Booleans.isTrue(getConfig().isStoreToken()) && response.startsWith("{")) {
            try {
                OAuthResponse tokenResponse = JsonSerialization.readValue(response, OAuthResponse.class);
                if (tokenResponse.getExpiresIn() != null && tokenResponse.getExpiresIn() > 0) {
                    long accessTokenExpiration = Time.currentTime() + tokenResponse.getExpiresIn();
                    tokenResponse.setAccessTokenExpiration(accessTokenExpiration);
                    response = JsonSerialization.writeValueAsString(tokenResponse);
                }
                context.setToken(response);
            } catch (IOException e) {
                logger.debugf("Can't store expiration date in JSON token", e);
            }
        }

        context.getContextData().put(FEDERATED_ACCESS_TOKEN, accessToken);
        return context;
    }

    protected String getAccessTokenResponseParameter() {
        return OAUTH2_PARAMETER_ACCESS_TOKEN;
    }


    protected BrokeredIdentityContext doGetFederatedIdentity(String accessToken) {
        return null;
    }


    protected UriBuilder createAuthorizationUrl(AuthenticationRequest request) {
        final UriBuilder uriBuilder = UriBuilder.fromUri(getConfig().getAuthorizationUrl())
                .queryParam(OAUTH2_PARAMETER_SCOPE, getConfig().getDefaultScope())
                .queryParam(OAUTH2_PARAMETER_STATE, request.getState().getEncoded())
                .queryParam(OAUTH2_PARAMETER_RESPONSE_TYPE, "code")
                .queryParam(OAUTH2_PARAMETER_CLIENT_ID, getConfig().getClientId())
                .queryParam(OAUTH2_PARAMETER_REDIRECT_URI, request.getRedirectUri());
        AuthenticationSessionModel authenticationSession = request.getAuthenticationSession();
        String loginHint = authenticationSession.getClientNote(OIDCLoginProtocol.LOGIN_HINT_PARAM);

        if (Booleans.isTrue(getConfig().isLoginHint()) && loginHint != null) {
            uriBuilder.queryParam(OIDCLoginProtocol.LOGIN_HINT_PARAM, loginHint);
        }

        if (getConfig().isUiLocales()) {
            uriBuilder.queryParam(OIDCLoginProtocol.UI_LOCALES_PARAM, session.getContext().resolveLocale(null).toLanguageTag());
        }

        String prompt = getConfig().getPrompt();
        if (prompt == null || prompt.isEmpty()) {
            prompt = authenticationSession.getClientNote(OAuth2Constants.PROMPT);
        }
        if (prompt != null) {
            uriBuilder.queryParam(OAuth2Constants.PROMPT, prompt);
        }

        if (getConfig().isPkceEnabled()) {
            String codeVerifier = PkceUtils.generateCodeVerifier();
            String codeChallengeMethod = getConfig().getPkceMethod();
            authenticationSession.setClientNote(BROKER_CODE_CHALLENGE_PARAM, codeVerifier);
            authenticationSession.setClientNote(BROKER_CODE_CHALLENGE_METHOD_PARAM, codeChallengeMethod);

            String codeChallenge = PkceUtils.encodeCodeChallenge(codeVerifier, codeChallengeMethod);
            uriBuilder.queryParam(OAuth2Constants.CODE_CHALLENGE, codeChallenge);
            uriBuilder.queryParam(OAuth2Constants.CODE_CHALLENGE_METHOD, codeChallengeMethod);
        }

        appendForwardedParameters(authenticationSession, uriBuilder);

        return uriBuilder;
    }

    private void appendForwardedParameters(AuthenticationSessionModel authenticationSession, UriBuilder uriBuilder) {
        C config = getConfig();
        String forwardParameterConfig = config.getForwardParameters() != null ? config.getForwardParameters(): OAuth2Constants.ACR_VALUES;
        List<String> parameterNames = List.of(forwardParameterConfig.split("\\s*,\\s*"));
        StringBuilder query = new StringBuilder(uriBuilder.build().getRawQuery());

        for (String name: parameterNames) {
            String noteKey = AuthorizationEndpoint.LOGIN_SESSION_NOTE_ADDITIONAL_REQ_PARAMS_PREFIX + name.trim();
            String value = authenticationSession.getClientNote(noteKey);

            if (value == null) {
                // try a value set as a client note
                value = authenticationSession.getClientNote(name);
            }

            if (value != null && !value.isEmpty()) {
                if (!query.isEmpty()) {
                    query.append("&");
                }
                query.append(name).append("=").append(URLEncoder.encode(value, StandardCharsets.UTF_8));
            }
        }

        uriBuilder.replaceQuery(query.toString());
    }

    /**
     * Get JSON property as text. JSON numbers and booleans are converted to text. Empty string is converted to null.
     *
     * @param jsonNode to get property from
     * @param name of property to get
     * @return string value of the property or null.
     */
    public String getJsonProperty(JsonNode jsonNode, String name) {
        if (jsonNode.has(name) && !jsonNode.get(name).isNull()) {
            String s = jsonNode.get(name).asText();
            if(s != null && !s.isEmpty())
                return s;
            else
                return null;
        }

        return null;
    }

    public JsonNode asJsonNode(String json) throws IOException {
        return mapper.readTree(json);
    }

    protected abstract String getDefaultScopes();

    @Override
    public void authenticationFinished(AuthenticationSessionModel authSession, BrokeredIdentityContext context) {
        String token = (String) context.getContextData().get(FEDERATED_ACCESS_TOKEN);
        if (token != null) authSession.setUserSessionNote(FEDERATED_ACCESS_TOKEN, token);
    }

    public SimpleHttpRequest authenticateTokenRequest(final SimpleHttpRequest tokenRequest) {

        if (getConfig().isJWTAuthentication()) {
            String sha1x509Thumbprint = null;
            SignatureSignerContext signer = getSignatureContext();
            if (getConfig().isJwtX509HeadersEnabled()) {
                KeyWrapper key = session.keys().getKey(session.getContext().getRealm(), signer.getKid(), KeyUse.SIG, signer.getAlgorithm());
                if (key != null
                        && key.getStatus().isEnabled()
                        && key.getPublicKey() != null
                        && key.getUse().equals(KeyUse.SIG)
                        && key.getType().equals(KeyType.RSA)) {
                    JWKBuilder builder = JWKBuilder.create().kid(key.getKid()).algorithm(key.getAlgorithmOrDefault());
                    List<X509Certificate> certificates = Optional.ofNullable(key.getCertificateChain())
                            .filter(certs -> !certs.isEmpty())
                            .orElseGet(() -> Collections.singletonList(key.getCertificate()));
                    RSAPublicJWK jwk = (RSAPublicJWK) builder.rsa(key.getPublicKey(), certificates, key.getUse());
                    sha1x509Thumbprint = jwk.getSha1x509Thumbprint();
                }
            }
            String jws = new JWSBuilder().type(OAuth2Constants.JWT).x5t(sha1x509Thumbprint).jsonContent(generateToken()).sign(signer);
            return tokenRequest
                    .param(OAuth2Constants.CLIENT_ASSERTION_TYPE, OAuth2Constants.CLIENT_ASSERTION_TYPE_JWT)
                    .param(OAuth2Constants.CLIENT_ASSERTION, jws)
                    .param(OAuth2Constants.CLIENT_ID, getConfig().getClientId());
        } else {
            try (VaultStringSecret vaultStringSecret = session.vault().getStringSecret(getConfig().getClientSecret())) {
                if (getConfig().isBasicAuthentication()) {
                    String clientSecret = vaultStringSecret.get().orElse(getConfig().getClientSecret());
                    String header = org.keycloak.util.BasicAuthHelper.RFC6749.createHeader(getConfig().getClientId(), clientSecret);
                    return tokenRequest.header(HttpHeaders.AUTHORIZATION, header);
                }
                if (getConfig().isBasicAuthenticationUnencoded()) {
                    return tokenRequest.authBasic(getConfig().getClientId(), vaultStringSecret.get().orElse(getConfig().getClientSecret()));
                }
                return tokenRequest
                        .param(OAUTH2_PARAMETER_CLIENT_ID, getConfig().getClientId())
                        .param(OAUTH2_PARAMETER_CLIENT_SECRET, vaultStringSecret.get().orElse(getConfig().getClientSecret()));
            }
        }
    }

    protected JsonWebToken generateToken() {
        JsonWebToken jwt = new JsonWebToken();
        jwt.id(SecretGenerator.getInstance().generateSecureID());
        jwt.type(OAuth2Constants.JWT);
        jwt.issuer(getConfig().getClientId());
        jwt.subject(getConfig().getClientId());
        String audience = getConfig().getClientAssertionAudience();
        if (StringUtil.isBlank(audience)) {
            audience = getConfig().getTokenUrl();
        }
        jwt.audience(audience);
        long expirationDelay = session.getContext().getRealm().getAccessCodeLifespan();
        jwt.exp(Time.currentTime() + expirationDelay);
        jwt.issuedNow();
        return jwt;
    }

    protected SignatureSignerContext getSignatureContext() {
        if (getConfig().getClientAuthMethod().equals(OIDCLoginProtocol.CLIENT_SECRET_JWT)) {
            try (VaultStringSecret vaultStringSecret = session.vault().getStringSecret(getConfig().getClientSecret())) {
                KeyWrapper key = new KeyWrapper();
                String alg = getConfig().getClientAssertionSigningAlg() != null ? getConfig().getClientAssertionSigningAlg() : Algorithm.HS256;
                key.setAlgorithm(alg);
                byte[] decodedSecret = vaultStringSecret.get().orElse(getConfig().getClientSecret()).getBytes();
                SecretKey secret = new SecretKeySpec(decodedSecret, 0, decodedSecret.length, alg);
                key.setSecretKey(secret);
                return new MacSignatureSignerContext(key);
            }
        }
        String alg = getConfig().getClientAssertionSigningAlg() != null ? getConfig().getClientAssertionSigningAlg() : Algorithm.RS256;
        return session.getProvider(SignatureProvider.class, alg).signer();
    }

    protected static class Endpoint {
        protected final AuthenticationCallback callback;
        protected final RealmModel realm;
        protected final EventBuilder event;
        private final AbstractOAuth2IdentityProvider provider;

        protected final KeycloakSession session;

        protected final ClientConnection clientConnection;

        protected final HttpHeaders headers;

        protected final HttpRequest httpRequest;

        public Endpoint(AuthenticationCallback callback, RealmModel realm, EventBuilder event, AbstractOAuth2IdentityProvider provider) {
            this.callback = callback;
            this.realm = realm;
            this.event = event;
            this.provider = provider;
            this.session = provider.session;
            this.clientConnection = session.getContext().getConnection();
            this.httpRequest = session.getContext().getHttpRequest();
            this.headers = session.getContext().getRequestHeaders();
        }

        @GET
        public Response authResponse(@QueryParam(AbstractOAuth2IdentityProvider.OAUTH2_PARAMETER_STATE) String state,
                                     @QueryParam(AbstractOAuth2IdentityProvider.OAUTH2_PARAMETER_CODE) String authorizationCode,
                                     @QueryParam(OAuth2Constants.ERROR) String error,
                                     @QueryParam(OAuth2Constants.ERROR_DESCRIPTION) String errorDescription) {
            OAuth2IdentityProviderConfig providerConfig = provider.getConfig();
            
            if (state == null) {
                logErroneousRedirectUrlError("Redirection URL does not contain a state parameter", providerConfig);
                return errorIdentityProviderLogin(Messages.IDENTITY_PROVIDER_MISSING_STATE_ERROR);
            }

            try {
                AuthenticationSessionModel authSession = this.callback.getAndVerifyAuthenticationSession(state);
                session.getContext().setAuthenticationSession(authSession);

                if (error != null) {
                    logErroneousRedirectUrlError("Redirection URL contains an error", providerConfig);
                    if (error.equals(ACCESS_DENIED)) {
                        return callback.cancelled(providerConfig);
                    } else if (error.equals(OAuthErrorException.LOGIN_REQUIRED) || error.equals(OAuthErrorException.INTERACTION_REQUIRED)) {
                        return callback.error(providerConfig, error);
                    } else if (error.equals(OAuthErrorException.TEMPORARILY_UNAVAILABLE) && Constants.AUTHENTICATION_EXPIRED_MESSAGE.equals(errorDescription)) {
                        return callback.retryLogin(this.provider, authSession);
                    } else {
                        return callback.error(providerConfig, Messages.IDENTITY_PROVIDER_UNEXPECTED_ERROR);
                    }
                }

                if (authorizationCode == null) {
                    logErroneousRedirectUrlError("Redirection URL neither contains a code nor error parameter",
                            providerConfig);
                    return errorIdentityProviderLogin(Messages.IDENTITY_PROVIDER_MISSING_CODE_OR_ERROR_ERROR);
                }

                SimpleHttpRequest simpleHttp = generateTokenRequest(authorizationCode);
                String response;
                try (SimpleHttpResponse simpleResponse = simpleHttp.asResponse()) {
                    int status = simpleResponse.getStatus();
                    boolean success = status >= 200 && status < 400;
                    response = simpleResponse.asString();

                    if (!success) {
                        logger.errorf("Unexpected response from token endpoint %s. status=%s, response=%s",
                                simpleHttp.getUrl(), status, response);
                        return errorIdentityProviderLogin(Messages.IDENTITY_PROVIDER_UNEXPECTED_ERROR);
                    }
                }

                BrokeredIdentityContext federatedIdentity = provider.getFederatedIdentity(response);

                if (Booleans.isTrue(providerConfig.isStoreToken())) {
                    // make sure that token wasn't already set by getFederatedIdentity();
                    // want to be able to allow provider to set the token itself.
                    if (federatedIdentity.getToken() == null)federatedIdentity.setToken(response);
                }

                federatedIdentity.setIdp(provider);
                federatedIdentity.setAuthenticationSession(authSession);

                return callback.authenticated(federatedIdentity);
            } catch (WebApplicationException e) {
                return e.getResponse();
            } catch (IdentityBrokerException e) {
                if (e.getMessageCode() != null) {
                    return errorIdentityProviderLogin(e.getMessageCode());
                }
                logger.error("Failed to make identity provider oauth callback", e);
                return errorIdentityProviderLogin(Messages.IDENTITY_PROVIDER_UNEXPECTED_ERROR);
            } catch (Exception e) {
                logger.error("Failed to make identity provider oauth callback", e);
                return errorIdentityProviderLogin(Messages.IDENTITY_PROVIDER_UNEXPECTED_ERROR);
            }
        }

        private void logErroneousRedirectUrlError(String mainMessage, OAuth2IdentityProviderConfig providerConfig) {
            String providerId = providerConfig.getProviderId();
            String redirectionUrl = session.getContext().getUri().getRequestUri().toString();

            logger.errorf("%s. providerId=%s, redirectionUrl=%s", mainMessage, providerId, redirectionUrl);
        }

        private Response errorIdentityProviderLogin(String message) {
            event.event(EventType.IDENTITY_PROVIDER_LOGIN);
            event.error(Errors.IDENTITY_PROVIDER_LOGIN_FAILURE);
            return ErrorPage.error(session, null, Response.Status.BAD_GATEWAY, message);
        }

        public SimpleHttpRequest generateTokenRequest(String authorizationCode) {
            KeycloakContext context = session.getContext();
            OAuth2IdentityProviderConfig providerConfig = provider.getConfig();
            SimpleHttpRequest tokenRequest = SimpleHttp.create(session).doPost(providerConfig.getTokenUrl())
                    .param(OAUTH2_PARAMETER_CODE, authorizationCode)
                    .param(OAUTH2_PARAMETER_REDIRECT_URI, Urls.identityProviderAuthnResponse(context.getUri().getBaseUri(),
                            providerConfig.getAlias(), context.getRealm().getName()).toString())
                    .param(OAUTH2_PARAMETER_GRANT_TYPE, OAUTH2_GRANT_TYPE_AUTHORIZATION_CODE);

            if (providerConfig.isPkceEnabled()) {

                // reconstruct the original code verifier that was used to generate the code challenge from the HttpRequest.
                String stateParam = session.getContext().getUri().getQueryParameters().getFirst(OAuth2Constants.STATE);
                if (stateParam == null) {
                    logger.warn("Cannot lookup PKCE code_verifier: state param is missing.");
                    return tokenRequest;
                }

                RealmModel realm = context.getRealm();
                IdentityBrokerState idpBrokerState = IdentityBrokerState.encoded(stateParam, realm);
                ClientModel client = realm.getClientByClientId(idpBrokerState.getClientId());

                AuthenticationSessionModel authSession = ClientSessionCode.getClientSession(
                        idpBrokerState.getEncoded(),
                        idpBrokerState.getTabId(),
                        session,
                        realm,
                        client,
                        event,
                        AuthenticationSessionModel.class);

                if (authSession == null) {
                    logger.warnf("Cannot lookup PKCE code_verifier: authSession not found. state=%s", stateParam);
                    return tokenRequest;
                }

                String brokerCodeChallenge = authSession.getClientNote(BROKER_CODE_CHALLENGE_PARAM);
                if (brokerCodeChallenge == null) {
                    logger.warnf("Cannot lookup PKCE code_verifier: brokerCodeChallenge not found. state=%s", stateParam);
                    return tokenRequest;
                }

                tokenRequest.param(OAuth2Constants.CODE_VERIFIER, brokerCodeChallenge);
            }

            return provider.authenticateTokenRequest(tokenRequest);
        }

    }

    protected String getProfileEndpointForValidation(EventBuilder event) {
        event.detail(Details.REASON, "exchange unsupported");
        event.error(Errors.INVALID_TOKEN);
        throw new ErrorResponseException(OAuthErrorException.INVALID_TOKEN, "invalid token", Response.Status.BAD_REQUEST);
    }

    protected BrokeredIdentityContext extractIdentityFromProfile(EventBuilder event, JsonNode node) {
        return null;
    }

    protected BrokeredIdentityContext validateExternalTokenThroughUserInfo(EventBuilder event, String subjectToken, String subjectTokenType) {
        event.detail("validation_method", "user info");

        SimpleHttpResponse response = null;
        int status = 0;
        try {
            String userInfoUrl = getProfileEndpointForValidation(event);
            response = buildUserInfoRequest(subjectToken, userInfoUrl).asResponse();
            status = response.getStatus();
        } catch (IOException e) {
            logger.debug("Failed to invoke user info for external exchange", e);
        }
        if (status != 200) {
            logger.debugf("Failed to invoke user info status: %d", status);
            event.detail(Details.REASON, "user info call failure");
            event.error(Errors.INVALID_TOKEN);
            throw new ErrorResponseException(OAuthErrorException.INVALID_TOKEN, "invalid token", Response.Status.BAD_REQUEST);
        }
        JsonNode profile = null;
        try {
            profile = response.asJson();
        } catch (IOException e) {
            event.detail(Details.REASON, "user info call failure");
            event.error(Errors.INVALID_TOKEN);
            throw new ErrorResponseException(OAuthErrorException.INVALID_TOKEN, "invalid token", Response.Status.BAD_REQUEST);
        }
        BrokeredIdentityContext context = extractIdentityFromProfile(event, profile);
        if (context.getId() == null) {
            event.detail(Details.REASON, "user info call failure");
            event.error(Errors.INVALID_TOKEN);
            throw new ErrorResponseException(OAuthErrorException.INVALID_TOKEN, "invalid token", Response.Status.BAD_REQUEST);
        }
        return context;
    }

    protected SimpleHttpRequest buildUserInfoRequest(String subjectToken, String userInfoUrl) {
        return SimpleHttp.create(session).doGet(userInfoUrl)
                  .header("Authorization", "Bearer " + subjectToken);
    }


    protected boolean supportsExternalExchange() {
        return false;
    }

    @Override
    public boolean isIssuer(String issuer, MultivaluedMap<String, String> params) {
        if (!supportsExternalExchange()) return false;
        String requestedIssuer = params.getFirst(OAuth2Constants.SUBJECT_ISSUER);
        if (requestedIssuer == null) requestedIssuer = issuer;
        return requestedIssuer.equals(getConfig().getAlias());
    }

    @Override
    final public BrokeredIdentityContext exchangeExternal(TokenExchangeProvider tokenExchangeProvider, TokenExchangeContext tokenExchangeContext) {
        if (!supportsExternalExchange()) return null;

        BrokeredIdentityContext context;
        int teVersion = tokenExchangeProvider.getVersion();
        switch (teVersion) {
            case 1:
                context = exchangeExternalTokenV1Impl(tokenExchangeContext.getEvent(), tokenExchangeContext.getFormParams());
                break;
            case 2:
                context = exchangeExternalTokenV2Impl(tokenExchangeContext);
                break;
            default:
                throw new IllegalArgumentException("Unsupported token exchange version " + teVersion);
        }

        if (context != null) {
            context.setIdp(this);
        }
        return context;
    }

    /**
     * Usage with token-exchange V1
     *
     * @param event event builder
     * @param params parameters of the token-exchange request
     * @return brokered identity context with the details about user from the IDP
     */
    protected BrokeredIdentityContext exchangeExternalTokenV1Impl(EventBuilder event, MultivaluedMap<String, String> params) {
        return exchangeExternalUserInfoValidationOnly(event, params);

    }

    /**
     * Usage with external-internal token-exchange v2.
     *
     * @param tokenExchangeContext data about token-exchange request
     * @return brokered identity context with the details about user from the IDP
     */
    protected BrokeredIdentityContext exchangeExternalTokenV2Impl(TokenExchangeContext tokenExchangeContext) {
        // Needs to be properly implemented for every provider to make sure it verifies external-token in appropriate way to validate user and also if the external-token
        // was issued to the proper audience
        throw new UnsupportedOperationException("Not yet supported to verify the external token of the identity provider " + getConfig().getAlias());
    }

    /**
     * Called usually during external-internal token exchange for validation of external token, which is the token issued by the IDP.
     * The validation of external token is done by calling OAuth2 introspection endpoint on the IDP side and validate if the response contains all the necessary claims
     * and token is authorized for the token exchange (including validating of claims like aud from introspection response)
     *
     * @param tokenExchangeContext token exchange context with the external token (subject token) and other details related to token exchange
     * @throws ErrorResponseException in case that validation failed for any reason
     */
    protected void validateExternalTokenWithIntrospectionEndpoint(TokenExchangeContext tokenExchangeContext) {
        EventBuilder event = tokenExchangeContext.getEvent();

        TokenMetadataRepresentation tokenMetadata = sendTokenIntrospectionRequest(tokenExchangeContext.getParams().getSubjectToken(), event);

        boolean clientValid = false;
        String tokenClientId = tokenMetadata.getClientId();
        List<String> tokenAudiences = null;
        if (tokenClientId != null && tokenClientId.equals(getConfig().getClientId())) {
            // Consider external token valid if issued to same client, which was configured as the client on IDP side
            clientValid = true;
        } else if (tokenMetadata.getAudience() != null && tokenMetadata.getAudience().length > 0) {
            tokenAudiences = Arrays.stream(tokenMetadata.getAudience()).toList();
            if (tokenAudiences.contains(getConfig().getClientId())) {
                // Consider external token valid if client configured as the IDP client included in token audience
                clientValid = true;
            } else {
                // Consider valid introspection also if token contains audience where URL is Keycloak server (either as issuer or as token-endpoint URL).
                // Aligned with https://datatracker.ietf.org/doc/html/rfc7523#section-3 - point 3
                UriInfo frontendUriInfo = session.getContext().getUri(UrlType.FRONTEND);
                UriInfo backendUriInfo = session.getContext().getUri(UrlType.BACKEND);
                RealmModel realm = session.getContext().getRealm();
                String realmIssuer = Urls.realmIssuer(frontendUriInfo.getBaseUri(), realm.getName());
                String realmTokenUrl = RealmsResource.protocolUrl(backendUriInfo).clone()
                        .path(OIDCLoginProtocolService.class, "token")
                        .build(realm.getName(), OIDCLoginProtocol.LOGIN_PROTOCOL).toString();
                if (tokenAudiences.contains(realmIssuer) || tokenAudiences.contains(realmTokenUrl)) {
                    clientValid = true;
                }
            }
        }
        if (!clientValid) {
            logger.debugf("Token not authorized for token exchange. Token client Id: %s, Token audiences: %s", tokenClientId, tokenAudiences);
            throwErrorResponse(event, Errors.INVALID_TOKEN, OAuthErrorException.INVALID_TOKEN, "Token not authorized for token exchange");
        }
    }

    /**
     * Send introspection request as specified in the OAuth2 token introspection specification. It requires
     *
     * @param idpAccessToken access token issued by the IDP
     * @param event event builder
     * @return token metadata in case that token introspection was successful and token is valid and active
     * @throws ErrorResponseException in case that introspection response was not correct for any reason (other status than 200) or the token was not active
     */
    protected TokenMetadataRepresentation sendTokenIntrospectionRequest(String idpAccessToken, EventBuilder event) {
        String introspectionEndointUrl = getConfig().getTokenIntrospectionUrl();
        if (introspectionEndointUrl == null) {
            throwErrorResponse(event, Errors.INVALID_CONFIG, OAuthErrorException.INVALID_REQUEST, "Introspection endpoint not configured for IDP");
        }

        try {

            // Supporting only access-tokens for now
            SimpleHttpRequest introspectionRequest = SimpleHttp.create(session).doPost(introspectionEndointUrl)
                    .param(TokenIntrospectionEndpoint.PARAM_TOKEN, idpAccessToken)
                    .param(TokenIntrospectionEndpoint.PARAM_TOKEN_TYPE_HINT, AccessTokenIntrospectionProviderFactory.ACCESS_TOKEN_TYPE);
            introspectionRequest = authenticateTokenRequest(introspectionRequest);

            try (SimpleHttpResponse introspectionResponse = introspectionRequest.asResponse()) {
                int status = introspectionResponse.getStatus();

                if (status != 200) {
                    try {
                        logger.warnf("Failed to invoke introspection endpoint. Status: %d, Introspection response details: %s", status, introspectionResponse.asString());
                    } catch (Exception ioe) {
                        logger.warnf("Failed to invoke introspection endpoint. Status: %d", status);
                    }
                    throwErrorResponse(event, Errors.INVALID_REQUEST, OAuthErrorException.INVALID_REQUEST, "Introspection endpoint call failure. Introspection response status: " + status);
                }

                TokenMetadataRepresentation tokenMetadata = null;
                try {
                    tokenMetadata = introspectionResponse.asJson(TokenMetadataRepresentation.class);
                } catch (IOException e) {
                    throwErrorResponse(event, Errors.INVALID_TOKEN, OAuthErrorException.INVALID_TOKEN, "Invalid format of the introspection response");
                }

                if (!tokenMetadata.isActive()) {
                    throwErrorResponse(event, Errors.INVALID_TOKEN, OAuthErrorException.INVALID_TOKEN, "Token not active");
                }

                return tokenMetadata;
            }
        } catch (IOException e) {
            logger.debug("Failed to invoke introspection endpoint", e);
            throwErrorResponse(event, Errors.INVALID_TOKEN, OAuthErrorException.INVALID_TOKEN, "Failed to invoke introspection endpoint");
            return null; // Unreachable
        }
    }

    private void throwErrorResponse(EventBuilder event, String eventError, String oauthError, String errorDetails) {
        event.detail(Details.REASON, errorDetails);
        event.error(eventError);
        throw new ErrorResponseException(oauthError, errorDetails, Response.Status.BAD_REQUEST);
    }

    protected BrokeredIdentityContext exchangeExternalUserInfoValidationOnly(EventBuilder event, MultivaluedMap<String, String> params) {
        String subjectToken = params.getFirst(OAuth2Constants.SUBJECT_TOKEN);
        if (subjectToken == null) {
            event.detail(Details.REASON, OAuth2Constants.SUBJECT_TOKEN + " param unset");
            event.error(Errors.INVALID_TOKEN);
            throw new ErrorResponseException(OAuthErrorException.INVALID_TOKEN, "token not set", Response.Status.BAD_REQUEST);
        }
        String subjectTokenType = params.getFirst(OAuth2Constants.SUBJECT_TOKEN_TYPE);
        if (subjectTokenType == null) {
            subjectTokenType = OAuth2Constants.ACCESS_TOKEN_TYPE;
        }
        if (!OAuth2Constants.ACCESS_TOKEN_TYPE.equals(subjectTokenType)) {
            event.detail(Details.REASON, OAuth2Constants.SUBJECT_TOKEN_TYPE + " invalid");
            event.error(Errors.INVALID_TOKEN_TYPE);
            throw new ErrorResponseException(OAuthErrorException.INVALID_TOKEN, "invalid token type", Response.Status.BAD_REQUEST);
        }
        return validateExternalTokenThroughUserInfo(event, subjectToken, subjectTokenType);
    }

    @Override
    public void exchangeExternalComplete(UserSessionModel userSession, BrokeredIdentityContext context, MultivaluedMap<String, String> params) {
        if (context.getContextData().containsKey(OIDCIdentityProvider.VALIDATED_ACCESS_TOKEN))
            userSession.setNote(FEDERATED_ACCESS_TOKEN, params.getFirst(OAuth2Constants.SUBJECT_TOKEN));
        if (context.getContextData().containsKey(OIDCIdentityProvider.VALIDATED_ID_TOKEN))
            userSession.setNote(OIDCIdentityProvider.FEDERATED_ID_TOKEN, params.getFirst(OAuth2Constants.SUBJECT_TOKEN));
        userSession.setNote(OIDCIdentityProvider.EXCHANGE_PROVIDER, getConfig().getAlias());

    }

    @Override
    public boolean supportsLongStateParameter() {
        return !getConfig().isRequiresShortStateParameter();
    }
}
