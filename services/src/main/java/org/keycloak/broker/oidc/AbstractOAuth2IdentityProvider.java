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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.broker.provider.AbstractIdentityProvider;
import org.keycloak.broker.provider.AuthenticationRequest;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.ExchangeExternalToken;
import org.keycloak.broker.provider.ExchangeTokenToIdentityProviderToken;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.broker.provider.IdentityProvider;
import org.keycloak.broker.provider.util.IdentityBrokerState;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.util.Time;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.AsymmetricSignatureProvider;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.crypto.MacSignatureSignerContext;
import org.keycloak.crypto.SignatureSignerContext;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.models.ClientModel;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.endpoints.AuthorizationEndpoint;
import org.keycloak.protocol.oidc.utils.PkceUtils;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.services.ErrorPage;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.Urls;
import org.keycloak.services.managers.ClientSessionCode;
import org.keycloak.services.messages.Messages;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.vault.VaultStringSecret;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.ws.rs.GET;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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


    public AbstractOAuth2IdentityProvider(KeycloakSession session, C config) {
        super(session, config);

        if (config.getDefaultScope() == null || config.getDefaultScope().isEmpty()) {
            config.setDefaultScope(getDefaultScopes());
        }
    }

    @Override
    public Object callback(RealmModel realm, AuthenticationCallback callback, EventBuilder event) {
        return new Endpoint(callback, realm, event);
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

    @Override
    public Response retrieveToken(KeycloakSession session, FederatedIdentityModel identity) {
        return Response.ok(identity.getToken()).build();
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
        if (!getConfig().isStoreToken()) {
            // if token isn't stored, we need to see if this session has been linked
            String brokerId = tokenUserSession.getNote(Details.IDENTITY_PROVIDER);
            brokerId = brokerId == null ? tokenUserSession.getNote(IdentityProvider.EXTERNAL_IDENTITY_PROVIDER) : brokerId;
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

        String loginHint = request.getAuthenticationSession().getClientNote(OIDCLoginProtocol.LOGIN_HINT_PARAM);
        if (getConfig().isLoginHint() && loginHint != null) {
            uriBuilder.queryParam(OIDCLoginProtocol.LOGIN_HINT_PARAM, loginHint);
        }

        if (getConfig().isUiLocales()) {
            uriBuilder.queryParam(OIDCLoginProtocol.UI_LOCALES_PARAM, session.getContext().resolveLocale(null).toLanguageTag());
        }

        String prompt = getConfig().getPrompt();
        if (prompt == null || prompt.isEmpty()) {
            prompt = request.getAuthenticationSession().getClientNote(OAuth2Constants.PROMPT);
        }
        if (prompt != null) {
            uriBuilder.queryParam(OAuth2Constants.PROMPT, prompt);
        }

        String acr = request.getAuthenticationSession().getClientNote(OAuth2Constants.ACR_VALUES);
        if (acr != null) {
            uriBuilder.queryParam(OAuth2Constants.ACR_VALUES, acr);
        }
        String forwardParameterConfig = getConfig().getForwardParameters() != null ? getConfig().getForwardParameters(): "";
        List<String> forwardParameters = Arrays.asList(forwardParameterConfig.split("\\s*,\\s*"));
        for(String forwardParameter: forwardParameters) {
            String name = AuthorizationEndpoint.LOGIN_SESSION_NOTE_ADDITIONAL_REQ_PARAMS_PREFIX + forwardParameter.trim();
            String parameter = request.getAuthenticationSession().getClientNote(name);
            if(parameter != null && !parameter.isEmpty()) {
                uriBuilder.queryParam(forwardParameter, parameter);
            }
        }

        if (getConfig().isPkceEnabled()) {
            String codeVerifier = PkceUtils.generateCodeVerifier();
            String codeChallengeMethod = getConfig().getPkceMethod();
            request.getAuthenticationSession().setClientNote(BROKER_CODE_CHALLENGE_PARAM, codeVerifier);
            request.getAuthenticationSession().setClientNote(BROKER_CODE_CHALLENGE_METHOD_PARAM, codeChallengeMethod);

            String codeChallenge = PkceUtils.encodeCodeChallenge(codeVerifier, codeChallengeMethod);
            uriBuilder.queryParam(OAuth2Constants.CODE_CHALLENGE, codeChallenge);
            uriBuilder.queryParam(OAuth2Constants.CODE_CHALLENGE_METHOD, codeChallengeMethod);
        }

        return uriBuilder;
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

    public SimpleHttp authenticateTokenRequest(final SimpleHttp tokenRequest) {

        if (getConfig().isJWTAuthentication()) {
            String jws = new JWSBuilder().type(OAuth2Constants.JWT).jsonContent(generateToken()).sign(getSignatureContext());
            return tokenRequest
                    .param(OAuth2Constants.CLIENT_ASSERTION_TYPE, OAuth2Constants.CLIENT_ASSERTION_TYPE_JWT)
                    .param(OAuth2Constants.CLIENT_ASSERTION, jws);
        } else {
            try (VaultStringSecret vaultStringSecret = session.vault().getStringSecret(getConfig().getClientSecret())) {
                if (getConfig().isBasicAuthentication()) {
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
        jwt.id(KeycloakModelUtils.generateId());
        jwt.type(OAuth2Constants.JWT);
        jwt.issuer(getConfig().getClientId());
        jwt.subject(getConfig().getClientId());
        jwt.audience(getConfig().getTokenUrl());
        int expirationDelay = session.getContext().getRealm().getAccessCodeLifespan();
        jwt.expiration(Time.currentTime() + expirationDelay);
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
        return new AsymmetricSignatureProvider(session, alg).signer();
    }

    protected class Endpoint {
        protected AuthenticationCallback callback;
        protected RealmModel realm;
        protected EventBuilder event;

        @Context
        protected KeycloakSession session;

        @Context
        protected ClientConnection clientConnection;

        @Context
        protected HttpHeaders headers;

        @Context
        protected HttpRequest httpRequest;

        public Endpoint(AuthenticationCallback callback, RealmModel realm, EventBuilder event) {
            this.callback = callback;
            this.realm = realm;
            this.event = event;
        }

        @GET
        public Response authResponse(@QueryParam(AbstractOAuth2IdentityProvider.OAUTH2_PARAMETER_STATE) String state,
                                     @QueryParam(AbstractOAuth2IdentityProvider.OAUTH2_PARAMETER_CODE) String authorizationCode,
                                     @QueryParam(OAuth2Constants.ERROR) String error) {
            if (state == null) {
                return errorIdentityProviderLogin(Messages.IDENTITY_PROVIDER_MISSING_STATE_ERROR);
            }

            try {
                AuthenticationSessionModel authSession = this.callback.getAndVerifyAuthenticationSession(state);
                session.getContext().setAuthenticationSession(authSession);

                if (error != null) {
                    logger.error(error + " for broker login " + getConfig().getProviderId());
                    if (error.equals(ACCESS_DENIED)) {
                        return callback.cancelled();
                    } else if (error.equals(OAuthErrorException.LOGIN_REQUIRED) || error.equals(OAuthErrorException.INTERACTION_REQUIRED)) {
                        return callback.error(error);
                    } else {
                        return callback.error(Messages.IDENTITY_PROVIDER_UNEXPECTED_ERROR);
                    }
                }

                if (authorizationCode != null) {
                    String response = generateTokenRequest(authorizationCode).asString();

                    BrokeredIdentityContext federatedIdentity = getFederatedIdentity(response);

                    if (getConfig().isStoreToken()) {
                        // make sure that token wasn't already set by getFederatedIdentity();
                        // want to be able to allow provider to set the token itself.
                        if (federatedIdentity.getToken() == null)federatedIdentity.setToken(response);
                    }

                    federatedIdentity.setIdpConfig(getConfig());
                    federatedIdentity.setIdp(AbstractOAuth2IdentityProvider.this);
                    federatedIdentity.setAuthenticationSession(authSession);

                    return callback.authenticated(federatedIdentity);
                }
            } catch (WebApplicationException e) {
                return e.getResponse();
            } catch (Exception e) {
                logger.error("Failed to make identity provider oauth callback", e);
            }
            return errorIdentityProviderLogin(Messages.IDENTITY_PROVIDER_UNEXPECTED_ERROR);
        }

        private Response errorIdentityProviderLogin(String message) {
            event.event(EventType.IDENTITY_PROVIDER_LOGIN);
            event.error(Errors.IDENTITY_PROVIDER_LOGIN_FAILURE);
            return ErrorPage.error(session, null, Response.Status.BAD_GATEWAY, message);
        }

        public SimpleHttp generateTokenRequest(String authorizationCode) {
            KeycloakContext context = session.getContext();
            SimpleHttp tokenRequest = SimpleHttp.doPost(getConfig().getTokenUrl(), session)
                    .param(OAUTH2_PARAMETER_CODE, authorizationCode)
                    .param(OAUTH2_PARAMETER_REDIRECT_URI, Urls.identityProviderAuthnResponse(context.getUri().getBaseUri(),
                            getConfig().getAlias(), context.getRealm().getName()).toString())
                    .param(OAUTH2_PARAMETER_GRANT_TYPE, OAUTH2_GRANT_TYPE_AUTHORIZATION_CODE);

            if (getConfig().isPkceEnabled()) {

                // reconstruct the original code verifier that was used to generate the code challenge from the HttpRequest.
                String stateParam = session.getContext().getUri().getQueryParameters().getFirst(OAuth2Constants.STATE);
                if (stateParam == null) {
                    logger.warn("Cannot lookup PKCE code_verifier: state param is missing.");
                    return tokenRequest;
                }

                RealmModel realm = context.getRealm();

                IdentityBrokerState idpBrokerState = IdentityBrokerState.encoded(stateParam);
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

            return authenticateTokenRequest(tokenRequest);
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
        SimpleHttp.Response response = null;
        int status = 0;
        try {
            String userInfoUrl = getProfileEndpointForValidation(event);
            response = buildUserInfoRequest(subjectToken, userInfoUrl).asResponse();
            status = response.getStatus();
        } catch (IOException e) {
            logger.debug("Failed to invoke user info for external exchange", e);
        }
        if (status != 200) {
            logger.debug("Failed to invoke user info status: " + status);
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

    protected SimpleHttp buildUserInfoRequest(String subjectToken, String userInfoUrl) {
        return SimpleHttp.doGet(userInfoUrl, session)
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


    final public BrokeredIdentityContext exchangeExternal(EventBuilder event, MultivaluedMap<String, String> params) {
        if (!supportsExternalExchange()) return null;
        BrokeredIdentityContext context = exchangeExternalImpl(event, params);
        if (context != null) {
            context.setIdp(this);
            context.setIdpConfig(getConfig());
        }
        return context;
    }

    protected BrokeredIdentityContext exchangeExternalImpl(EventBuilder event, MultivaluedMap<String, String> params) {
        return exchangeExternalUserInfoValidationOnly(event, params);

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
        if (context.getContextData().containsKey(OIDCIdentityProvider.VALIDATED_ID_TOKEN))
            userSession.setNote(FEDERATED_ACCESS_TOKEN, params.getFirst(OAuth2Constants.SUBJECT_TOKEN));
        if (context.getContextData().containsKey(OIDCIdentityProvider.VALIDATED_ID_TOKEN))
            userSession.setNote(OIDCIdentityProvider.FEDERATED_ID_TOKEN, params.getFirst(OAuth2Constants.SUBJECT_TOKEN));
        userSession.setNote(OIDCIdentityProvider.EXCHANGE_PROVIDER, getConfig().getAlias());

    }


}
