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

package org.keycloak.protocol.oidc.endpoints;

import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.broker.provider.IdentityProvider;
import org.keycloak.broker.provider.ExchangeTokenToIdentityProviderToken;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.constants.ServiceAccountConstants;
import org.keycloak.common.util.Base64Url;
import org.keycloak.constants.AdapterConstants;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.protocol.oidc.utils.AuthorizeClientUtil;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.Urls;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.AuthenticationSessionManager;
import org.keycloak.services.managers.ClientManager;
import org.keycloak.services.managers.ClientSessionCode;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.Cors;
import org.keycloak.services.resources.IdentityBrokerService;
import org.keycloak.services.resources.admin.permissions.AdminPermissions;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.util.TokenUtil;

import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.security.MessageDigest;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class TokenEndpoint {

    private static final Logger logger = Logger.getLogger(TokenEndpoint.class);
    private MultivaluedMap<String, String> formParams;
    private ClientModel client;
    private Map<String, String> clientAuthAttributes;

    private enum Action {
        AUTHORIZATION_CODE, REFRESH_TOKEN, PASSWORD, CLIENT_CREDENTIALS, TOKEN_EXCHANGE
    }

    // https://tools.ietf.org/html/rfc7636#section-4.2
    private static final Pattern VALID_CODE_VERIFIER_PATTERN = Pattern.compile("^[0-9a-zA-Z\\-\\.~_]+$");

    @Context
    private KeycloakSession session;

    @Context
    private HttpRequest request;

    @Context
    private HttpHeaders headers;

    @Context
    private UriInfo uriInfo;

    @Context
    private ClientConnection clientConnection;

    private final TokenManager tokenManager;
    private final RealmModel realm;
    private final EventBuilder event;

    private Action action;

    private String grantType;

    public TokenEndpoint(TokenManager tokenManager, RealmModel realm, EventBuilder event) {
        this.tokenManager = tokenManager;
        this.realm = realm;
        this.event = event;
    }

    @POST
    public Response build() {
        formParams = request.getDecodedFormParameters();
        grantType = formParams.getFirst(OIDCLoginProtocol.GRANT_TYPE_PARAM);

        checkSsl();
        checkRealm();
        checkGrantType();
        checkClient();

        switch (action) {
            case AUTHORIZATION_CODE:
                return buildAuthorizationCodeAccessTokenResponse();
            case REFRESH_TOKEN:
                return buildRefreshToken();
            case PASSWORD:
                return buildResourceOwnerPasswordCredentialsGrant();
            case CLIENT_CREDENTIALS:
                return buildClientCredentialsGrant();
            case TOKEN_EXCHANGE:
                return buildTokenExchange();
        }

        throw new RuntimeException("Unknown action " + action);
    }

    @Path("introspect")
    public Object introspect() {
        TokenIntrospectionEndpoint tokenIntrospectionEndpoint = new TokenIntrospectionEndpoint(this.realm, this.event);

        ResteasyProviderFactory.getInstance().injectProperties(tokenIntrospectionEndpoint);

        return tokenIntrospectionEndpoint;
    }

    @OPTIONS
    public Response preflight() {
        if (logger.isDebugEnabled()) {
            logger.debugv("CORS preflight from: {0}", headers.getRequestHeaders().getFirst("Origin"));
        }
        return Cors.add(request, Response.ok()).auth().preflight().allowedMethods("POST", "OPTIONS").build();
    }

    private void checkSsl() {
        if (!uriInfo.getBaseUri().getScheme().equals("https") && realm.getSslRequired().isRequired(clientConnection)) {
            throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "HTTPS required", Response.Status.FORBIDDEN);
        }
    }

    private void checkRealm() {
        if (!realm.isEnabled()) {
            throw new ErrorResponseException("access_denied", "Realm not enabled", Response.Status.FORBIDDEN);
        }
    }

    private void checkClient() {
        AuthorizeClientUtil.ClientAuthResult clientAuth = AuthorizeClientUtil.authorizeClient(session, event);
        client = clientAuth.getClient();
        clientAuthAttributes = clientAuth.getClientAuthAttributes();

        if (client.isBearerOnly()) {
            throw new ErrorResponseException(OAuthErrorException.INVALID_CLIENT, "Bearer-only not allowed", Response.Status.BAD_REQUEST);
        }


    }

    private void checkGrantType() {
        if (grantType == null) {
            throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "Missing form parameter: " + OIDCLoginProtocol.GRANT_TYPE_PARAM, Response.Status.BAD_REQUEST);
        }

        if (grantType.equals(OAuth2Constants.AUTHORIZATION_CODE)) {
            event.event(EventType.CODE_TO_TOKEN);
            action = Action.AUTHORIZATION_CODE;
        } else if (grantType.equals(OAuth2Constants.REFRESH_TOKEN)) {
            event.event(EventType.REFRESH_TOKEN);
            action = Action.REFRESH_TOKEN;
        } else if (grantType.equals(OAuth2Constants.PASSWORD)) {
            event.event(EventType.LOGIN);
            action = Action.PASSWORD;
        } else if (grantType.equals(OAuth2Constants.CLIENT_CREDENTIALS)) {
            event.event(EventType.CLIENT_LOGIN);
            action = Action.CLIENT_CREDENTIALS;
        } else if (grantType.equals(OAuth2Constants.TOKEN_EXCHANGE_GRANT_TYPE)) {
            event.event(EventType.TOKEN_EXCHANGE);
            action = Action.TOKEN_EXCHANGE;

        } else {
            throw new ErrorResponseException(Errors.INVALID_REQUEST, "Invalid " + OIDCLoginProtocol.GRANT_TYPE_PARAM, Response.Status.BAD_REQUEST);
        }

        event.detail(Details.GRANT_TYPE, grantType);
    }

    public Response buildAuthorizationCodeAccessTokenResponse() {
        String code = formParams.getFirst(OAuth2Constants.CODE);
        if (code == null) {
            event.error(Errors.INVALID_CODE);
            throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "Missing parameter: " + OAuth2Constants.CODE, Response.Status.BAD_REQUEST);
        }

        String[] parts = code.split("\\.");
        if (parts.length == 4) {
            event.detail(Details.CODE_ID, parts[2]);
        }

        ClientSessionCode.ParseResult<AuthenticatedClientSessionModel> parseResult = ClientSessionCode.parseResult(code, session, realm, AuthenticatedClientSessionModel.class);
        if (parseResult.isAuthSessionNotFound() || parseResult.isIllegalHash()) {
            event.error(Errors.INVALID_CODE);

            // Attempt to use same code twice should invalidate existing clientSession
            AuthenticatedClientSessionModel clientSession = parseResult.getClientSession();
            if (clientSession != null) {
                clientSession.setUserSession(null);
            }

            throw new ErrorResponseException(OAuthErrorException.INVALID_GRANT, "Code not valid", Response.Status.BAD_REQUEST);
        }

        AuthenticatedClientSessionModel clientSession = parseResult.getClientSession();

        if (!parseResult.getCode().isValid(AuthenticatedClientSessionModel.Action.CODE_TO_TOKEN.name(), ClientSessionCode.ActionType.CLIENT)) {
            event.error(Errors.INVALID_CODE);
            throw new ErrorResponseException(OAuthErrorException.INVALID_GRANT, "Code is expired", Response.Status.BAD_REQUEST);
        }

        // TODO: This shouldn't be needed to write into the AuthenticatedClientSessionModel itself
        parseResult.getCode().setAction(null);

        // TODO: Maybe rather create userSession even at this stage?
        UserSessionModel userSession = clientSession.getUserSession();

        if (userSession == null) {
            event.error(Errors.USER_SESSION_NOT_FOUND);
            throw new ErrorResponseException(OAuthErrorException.INVALID_GRANT, "User session not found", Response.Status.BAD_REQUEST);
        }

        UserModel user = userSession.getUser();
        if (user == null) {
            event.error(Errors.USER_NOT_FOUND);
            throw new ErrorResponseException(OAuthErrorException.INVALID_GRANT, "User not found", Response.Status.BAD_REQUEST);
        }
        if (!user.isEnabled()) {
            event.error(Errors.USER_DISABLED);
            throw new ErrorResponseException(OAuthErrorException.INVALID_GRANT, "User disabled", Response.Status.BAD_REQUEST);
        }

        event.user(userSession.getUser());

        event.session(userSession.getId());

        String redirectUri = clientSession.getNote(OIDCLoginProtocol.REDIRECT_URI_PARAM);
        String formParam = formParams.getFirst(OAuth2Constants.REDIRECT_URI);
        if (redirectUri != null && !redirectUri.equals(formParam)) {
            event.error(Errors.INVALID_CODE);
            throw new ErrorResponseException(OAuthErrorException.INVALID_GRANT, "Incorrect redirect_uri", Response.Status.BAD_REQUEST);
        }

        if (!client.getClientId().equals(clientSession.getClient().getClientId())) {
            event.error(Errors.INVALID_CODE);
            throw new ErrorResponseException(OAuthErrorException.INVALID_GRANT, "Auth error", Response.Status.BAD_REQUEST);
        }

        if (!client.isStandardFlowEnabled()) {
            event.error(Errors.NOT_ALLOWED);
            throw new ErrorResponseException(OAuthErrorException.INVALID_GRANT, "Client not allowed to exchange code", Response.Status.BAD_REQUEST);
        }

        if (!AuthenticationManager.isSessionValid(realm, userSession)) {
            event.error(Errors.USER_SESSION_NOT_FOUND);
            throw new ErrorResponseException(OAuthErrorException.INVALID_GRANT, "Session not active", Response.Status.BAD_REQUEST);
        }

        // https://tools.ietf.org/html/rfc7636#section-4.6
        String codeVerifier = formParams.getFirst(OAuth2Constants.CODE_VERIFIER);
        String codeChallenge = clientSession.getNote(OIDCLoginProtocol.CODE_CHALLENGE_PARAM);
        String codeChallengeMethod = clientSession.getNote(OIDCLoginProtocol.CODE_CHALLENGE_METHOD_PARAM);
        String authUserId = user.getId();
        String authUsername = user.getUsername();
        if (authUserId == null) {
            authUserId = "unknown";
        }
        if (authUsername == null) {
            authUsername = "unknown";
        }
        if (codeChallenge != null && codeVerifier == null) {
            logger.warnf("PKCE code verifier not specified, authUserId = %s, authUsername = %s", authUserId, authUsername);
            event.error(Errors.CODE_VERIFIER_MISSING);
            throw new ErrorResponseException(OAuthErrorException.INVALID_GRANT, "PKCE code verifier not specified", Response.Status.BAD_REQUEST);
        }

        if (codeChallenge != null) {
            // based on whether code_challenge has been stored at corresponding authorization code request previously
        	// decide whether this client(RP) supports PKCE
            if (!isValidPkceCodeVerifier(codeVerifier)) {
                logger.infof("PKCE invalid code verifier");
                event.error(Errors.INVALID_CODE_VERIFIER);
                throw new ErrorResponseException(OAuthErrorException.INVALID_GRANT, "PKCE invalid code verifier", Response.Status.BAD_REQUEST);
            }
            
            logger.debugf("PKCE supporting Client, codeVerifier = %s", codeVerifier);
            String codeVerifierEncoded = codeVerifier;
            try {
            	// https://tools.ietf.org/html/rfc7636#section-4.2
            	// plain or S256
                if (codeChallengeMethod != null && codeChallengeMethod.equals(OAuth2Constants.PKCE_METHOD_S256)) {
                    logger.debugf("PKCE codeChallengeMethod = %s", codeChallengeMethod);                    
                    codeVerifierEncoded = generateS256CodeChallenge(codeVerifier);
                } else {
                    logger.debug("PKCE codeChallengeMethod is plain");
                    codeVerifierEncoded = codeVerifier;
                }
            } catch (Exception nae) {
                logger.infof("PKCE code verification failed, not supported algorithm specified");
                event.error(Errors.PKCE_VERIFICATION_FAILED);
                throw new ErrorResponseException(OAuthErrorException.INVALID_GRANT, "PKCE code verification failed, not supported algorithm specified", Response.Status.BAD_REQUEST);
            }
            if (!codeChallenge.equals(codeVerifierEncoded)) {
                logger.warnf("PKCE verification failed. authUserId = %s, authUsername = %s", authUserId, authUsername);
                event.error(Errors.PKCE_VERIFICATION_FAILED);
                throw new ErrorResponseException(OAuthErrorException.INVALID_GRANT, "PKCE verification failed", Response.Status.BAD_REQUEST);
            } else {
                logger.debugf("PKCE verification success. codeVerifierEncoded = %s, codeChallenge = %s", codeVerifierEncoded, codeChallenge);
            }
        }


        updateClientSession(clientSession);
        updateUserSessionFromClientAuth(userSession);

        AccessToken token = tokenManager.createClientAccessToken(session, parseResult.getCode().getRequestedRoles(), realm, client, user, userSession, clientSession);

        TokenManager.AccessTokenResponseBuilder responseBuilder = tokenManager.responseBuilder(realm, client, event, session, userSession, clientSession)
                .accessToken(token)
                .generateRefreshToken();

        String scopeParam = clientSession.getNote(OAuth2Constants.SCOPE);
        if (TokenUtil.isOIDCRequest(scopeParam)) {
            responseBuilder.generateIDToken();
        }

        AccessTokenResponse res = responseBuilder.build();

        event.success();

        return Cors.add(request, Response.ok(res).type(MediaType.APPLICATION_JSON_TYPE)).auth().allowedOrigins(uriInfo, client).allowedMethods("POST").exposedHeaders(Cors.ACCESS_CONTROL_ALLOW_METHODS).build();
    }

    public Response buildRefreshToken() {
        String refreshToken = formParams.getFirst(OAuth2Constants.REFRESH_TOKEN);
        if (refreshToken == null) {
            throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "No refresh token", Response.Status.BAD_REQUEST);
        }

        AccessTokenResponse res;
        try {
            TokenManager.RefreshResult result = tokenManager.refreshAccessToken(session, uriInfo, clientConnection, realm, client, refreshToken, event, headers);
            res = result.getResponse();

            if (!result.isOfflineToken()) {
                UserSessionModel userSession = session.sessions().getUserSession(realm, res.getSessionState());
                AuthenticatedClientSessionModel clientSession = userSession.getAuthenticatedClientSessions().get(client.getId());
                updateClientSession(clientSession);
                updateUserSessionFromClientAuth(userSession);
            }

        } catch (OAuthErrorException e) {
            logger.trace(e.getMessage(), e);
            event.error(Errors.INVALID_TOKEN);
            throw new ErrorResponseException(e.getError(), e.getDescription(), Response.Status.BAD_REQUEST);
        }

        event.success();

        return Cors.add(request, Response.ok(res, MediaType.APPLICATION_JSON_TYPE)).auth().allowedOrigins(uriInfo, client).allowedMethods("POST").exposedHeaders(Cors.ACCESS_CONTROL_ALLOW_METHODS).build();
    }

    private void updateClientSession(AuthenticatedClientSessionModel clientSession) {

        if(clientSession == null) {
            ServicesLogger.LOGGER.clientSessionNull();
            return;
        }

        String adapterSessionId = formParams.getFirst(AdapterConstants.CLIENT_SESSION_STATE);
        if (adapterSessionId != null) {
            String adapterSessionHost = formParams.getFirst(AdapterConstants.CLIENT_SESSION_HOST);
            logger.debugf("Adapter Session '%s' saved in ClientSession for client '%s'. Host is '%s'", adapterSessionId, client.getClientId(), adapterSessionHost);

            event.detail(AdapterConstants.CLIENT_SESSION_STATE, adapterSessionId);
            String oldClientSessionState = clientSession.getNote(AdapterConstants.CLIENT_SESSION_STATE);
            if (!adapterSessionId.equals(oldClientSessionState)) {
                clientSession.setNote(AdapterConstants.CLIENT_SESSION_STATE, adapterSessionId);
            }

            event.detail(AdapterConstants.CLIENT_SESSION_HOST, adapterSessionHost);
            String oldClientSessionHost = clientSession.getNote(AdapterConstants.CLIENT_SESSION_HOST);
            if (!Objects.equals(adapterSessionHost, oldClientSessionHost)) {
                clientSession.setNote(AdapterConstants.CLIENT_SESSION_HOST, adapterSessionHost);
            }
        }
    }

    private void updateUserSessionFromClientAuth(UserSessionModel userSession) {
        for (Map.Entry<String, String> attr : clientAuthAttributes.entrySet()) {
            userSession.setNote(attr.getKey(), attr.getValue());
        }
    }

    public Response buildResourceOwnerPasswordCredentialsGrant() {
        event.detail(Details.AUTH_METHOD, "oauth_credentials");

        if (!client.isDirectAccessGrantsEnabled()) {
            event.error(Errors.NOT_ALLOWED);
            throw new ErrorResponseException(OAuthErrorException.INVALID_GRANT, "Client not allowed for direct access grants", Response.Status.BAD_REQUEST);
        }

        if (client.isConsentRequired()) {
            event.error(Errors.CONSENT_DENIED);
            throw new ErrorResponseException(OAuthErrorException.INVALID_CLIENT, "Client requires user consent", Response.Status.BAD_REQUEST);
        }
        String scope = formParams.getFirst(OAuth2Constants.SCOPE);

        AuthenticationSessionModel authSession = new AuthenticationSessionManager(session).createAuthenticationSession(realm, client, false);
        authSession.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        authSession.setAction(AuthenticatedClientSessionModel.Action.AUTHENTICATE.name());
        authSession.setClientNote(OIDCLoginProtocol.ISSUER, Urls.realmIssuer(uriInfo.getBaseUri(), realm.getName()));
        authSession.setClientNote(OIDCLoginProtocol.SCOPE_PARAM, scope);

        AuthenticationFlowModel flow = realm.getDirectGrantFlow();
        String flowId = flow.getId();
        AuthenticationProcessor processor = new AuthenticationProcessor();
        processor.setAuthenticationSession(authSession)
                .setFlowId(flowId)
                .setConnection(clientConnection)
                .setEventBuilder(event)
                .setRealm(realm)
                .setSession(session)
                .setUriInfo(uriInfo)
                .setRequest(request);
        Response challenge = processor.authenticateOnly();
        if (challenge != null) return challenge;
        processor.evaluateRequiredActionTriggers();
        UserModel user = authSession.getAuthenticatedUser();
        if (user.getRequiredActions() != null && user.getRequiredActions().size() > 0) {
            event.error(Errors.RESOLVE_REQUIRED_ACTIONS);
            throw new ErrorResponseException(OAuthErrorException.INVALID_GRANT, "Account is not fully set up", Response.Status.BAD_REQUEST);

        }

        AuthenticationManager.setRolesAndMappersInSession(authSession);

        AuthenticatedClientSessionModel clientSession = processor.attachSession();
        UserSessionModel userSession = processor.getUserSession();
        updateUserSessionFromClientAuth(userSession);

        TokenManager.AccessTokenResponseBuilder responseBuilder = tokenManager.responseBuilder(realm, client, event, session, userSession, clientSession)
                .generateAccessToken()
                .generateRefreshToken();

        String scopeParam = clientSession.getNote(OAuth2Constants.SCOPE);
        if (TokenUtil.isOIDCRequest(scopeParam)) {
            responseBuilder.generateIDToken();
        }

        AccessTokenResponse res = responseBuilder.build();


        event.success();

        return Cors.add(request, Response.ok(res, MediaType.APPLICATION_JSON_TYPE)).auth().allowedOrigins(uriInfo, client).allowedMethods("POST").exposedHeaders(Cors.ACCESS_CONTROL_ALLOW_METHODS).build();
    }

    public Response buildClientCredentialsGrant() {
        if (client.isBearerOnly()) {
            event.error(Errors.INVALID_CLIENT);
            throw new ErrorResponseException(OAuthErrorException.UNAUTHORIZED_CLIENT, "Bearer-only client not allowed to retrieve service account", Response.Status.UNAUTHORIZED);
        }
        if (client.isPublicClient()) {
            event.error(Errors.INVALID_CLIENT);
            throw new ErrorResponseException(OAuthErrorException.UNAUTHORIZED_CLIENT, "Public client not allowed to retrieve service account", Response.Status.UNAUTHORIZED);
        }
        if (!client.isServiceAccountsEnabled()) {
            event.error(Errors.INVALID_CLIENT);
            throw new ErrorResponseException(OAuthErrorException.UNAUTHORIZED_CLIENT, "Client not enabled to retrieve service account", Response.Status.UNAUTHORIZED);
        }

        UserModel clientUser = session.users().getServiceAccount(client);

        if (clientUser == null || client.getProtocolMapperByName(OIDCLoginProtocol.LOGIN_PROTOCOL, ServiceAccountConstants.CLIENT_ID_PROTOCOL_MAPPER) == null) {
            // May need to handle bootstrap here as well
            logger.debugf("Service account user for client '%s' not found or default protocol mapper for service account not found. Creating now", client.getClientId());
            new ClientManager(new RealmManager(session)).enableServiceAccount(client);
            clientUser = session.users().getServiceAccount(client);
        }

        String clientUsername = clientUser.getUsername();
        event.detail(Details.USERNAME, clientUsername);
        event.user(clientUser);

        if (!clientUser.isEnabled()) {
            event.error(Errors.USER_DISABLED);
            throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "User '" + clientUsername + "' disabled", Response.Status.UNAUTHORIZED);
        }

        String scope = formParams.getFirst(OAuth2Constants.SCOPE);

        AuthenticationSessionModel authSession = new AuthenticationSessionManager(session).createAuthenticationSession(realm, client, false);
        authSession.setAuthenticatedUser(clientUser);
        authSession.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        authSession.setClientNote(OIDCLoginProtocol.ISSUER, Urls.realmIssuer(uriInfo.getBaseUri(), realm.getName()));
        authSession.setClientNote(OIDCLoginProtocol.SCOPE_PARAM, scope);

        UserSessionModel userSession = session.sessions().createUserSession(authSession.getId(), realm, clientUser, clientUsername, clientConnection.getRemoteAddr(), ServiceAccountConstants.CLIENT_AUTH, false, null, null);
        event.session(userSession);

        AuthenticationManager.setRolesAndMappersInSession(authSession);
        AuthenticatedClientSessionModel clientSession = TokenManager.attachAuthenticationSession(session, userSession, authSession);

        // Notes about client details
        userSession.setNote(ServiceAccountConstants.CLIENT_ID, client.getClientId());
        userSession.setNote(ServiceAccountConstants.CLIENT_HOST, clientConnection.getRemoteHost());
        userSession.setNote(ServiceAccountConstants.CLIENT_ADDRESS, clientConnection.getRemoteAddr());

        updateUserSessionFromClientAuth(userSession);

        TokenManager.AccessTokenResponseBuilder responseBuilder = tokenManager.responseBuilder(realm, client, event, session, userSession, clientSession)
                .generateAccessToken()
                .generateRefreshToken();

        String scopeParam = clientSession.getNote(OAuth2Constants.SCOPE);
        if (TokenUtil.isOIDCRequest(scopeParam)) {
            responseBuilder.generateIDToken();
        }

        AccessTokenResponse res = responseBuilder.build();

        event.success();

        return Cors.add(request, Response.ok(res, MediaType.APPLICATION_JSON_TYPE)).auth().allowedOrigins(uriInfo, client).allowedMethods("POST").exposedHeaders(Cors.ACCESS_CONTROL_ALLOW_METHODS).build();
    }

    public Response buildTokenExchange() {
        event.detail(Details.AUTH_METHOD, "oauth_credentials");

        String subjectToken = formParams.getFirst(OAuth2Constants.SUBJECT_TOKEN);
        String subjectTokenType = formParams.getFirst(OAuth2Constants.SUBJECT_TOKEN_TYPE);
        if (subjectTokenType != null && !subjectTokenType.equals(OAuth2Constants.ACCESS_TOKEN_TYPE)) {
            event.error(Errors.INVALID_TOKEN);
            throw new ErrorResponseException(OAuthErrorException.INVALID_TOKEN, "Invalid token type, must be access token", Response.Status.BAD_REQUEST);

        }


        AuthenticationManager.AuthResult authResult = AuthenticationManager.verifyIdentityToken(session, realm, uriInfo, clientConnection, true, true, false, subjectToken, headers);
        if (authResult == null) {
            event.error(Errors.INVALID_TOKEN);
            throw new ErrorResponseException(OAuthErrorException.INVALID_TOKEN, "Invalid token", Response.Status.BAD_REQUEST);
        }

        String requestedIssuer = formParams.getFirst(OAuth2Constants.REQUESTED_ISSUER);

        if (requestedIssuer == null) {
            return exchangeClientToClient(authResult.getUser(), authResult.getSession());
        } else {
            return exchangeToIdentityProvider(authResult, requestedIssuer);
        }
    }

    public Response exchangeToIdentityProvider(AuthenticationManager.AuthResult authResult, String requestedIssuer) {
        IdentityProviderModel providerModel = realm.getIdentityProviderByAlias(requestedIssuer);
        if (providerModel == null) {
            event.error(Errors.UNKNOWN_IDENTITY_PROVIDER);
            throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "Invalid issuer", Response.Status.BAD_REQUEST);
        }

        IdentityProvider provider = IdentityBrokerService.getIdentityProvider(session, realm, requestedIssuer);
        if (!(provider instanceof ExchangeTokenToIdentityProviderToken)) {
            event.error(Errors.UNKNOWN_IDENTITY_PROVIDER);
            throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "Issuer does not support token exchange", Response.Status.BAD_REQUEST);
        }
        if (!AdminPermissions.management(session, realm).idps().canExchangeTo(client, providerModel)) {
            logger.debug("Client not allowed to exchange for linked token");
            event.error(Errors.NOT_ALLOWED);
            throw new ErrorResponseException(OAuthErrorException.ACCESS_DENIED, "Client not allowed to exchange", Response.Status.FORBIDDEN);
        }
        Response response = ((ExchangeTokenToIdentityProviderToken)provider).exchangeFromToken(uriInfo, client, authResult.getSession(), authResult.getUser(), authResult.getToken(), formParams);
        return Cors.add(request, Response.fromResponse(response)).auth().allowedOrigins(uriInfo, client).allowedMethods("POST").exposedHeaders(Cors.ACCESS_CONTROL_ALLOW_METHODS).build();

    }

    protected Response exchangeClientToClient(UserModel targetUser, UserSessionModel targetUserSession) {
        String requestedTokenType = formParams.getFirst(OAuth2Constants.REQUESTED_TOKEN_TYPE);
        if (requestedTokenType == null) {
            requestedTokenType = OAuth2Constants.REFRESH_TOKEN_TYPE;
        } else if (!requestedTokenType.equals(OAuth2Constants.ACCESS_TOKEN_TYPE) && !requestedTokenType.equals(OAuth2Constants.REFRESH_TOKEN_TYPE)) {
            event.error(Errors.INVALID_REQUEST);
            throw new ErrorResponseException("unsupported_requested_token_type", "Unsupported requested token type", Response.Status.BAD_REQUEST);

        }
        String audience = formParams.getFirst(OAuth2Constants.AUDIENCE);
        if (audience == null) {
            event.error(Errors.INVALID_REQUEST);
            throw new ErrorResponseException("invalid_audience", "Audience parameter required", Response.Status.BAD_REQUEST);

        }
        ClientModel targetClient = null;
        if (audience != null) {
            targetClient = realm.getClientByClientId(audience);
        }
        if (targetClient == null) {
            event.error(Errors.INVALID_CLIENT);
            throw new ErrorResponseException("invalid_client", "Client authentication ended, but client is null", Response.Status.BAD_REQUEST);
        }

        if (targetClient.isConsentRequired()) {
            event.error(Errors.CONSENT_DENIED);
            throw new ErrorResponseException(OAuthErrorException.INVALID_CLIENT, "Client requires user consent", Response.Status.BAD_REQUEST);
        }

        if (!AdminPermissions.management(session, realm).clients().canExchangeTo(client, targetClient)) {
            logger.debug("Client does not have exchange rights for target audience");
            event.error(Errors.NOT_ALLOWED);
            throw new ErrorResponseException(OAuthErrorException.ACCESS_DENIED, "Client not allowed to exchange", Response.Status.FORBIDDEN);
        }

        String scope = formParams.getFirst(OAuth2Constants.SCOPE);

        AuthenticationSessionModel authSession = new AuthenticationSessionManager(session).createAuthenticationSession(realm, targetClient, false);
        authSession.setAuthenticatedUser(targetUser);
        authSession.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        authSession.setClientNote(OIDCLoginProtocol.ISSUER, Urls.realmIssuer(uriInfo.getBaseUri(), realm.getName()));
        authSession.setClientNote(OIDCLoginProtocol.SCOPE_PARAM, scope);

        event.session(targetUserSession);

        AuthenticationManager.setRolesAndMappersInSession(authSession);
        AuthenticatedClientSessionModel clientSession = TokenManager.attachAuthenticationSession(this.session, targetUserSession, authSession);

        updateUserSessionFromClientAuth(targetUserSession);

        TokenManager.AccessTokenResponseBuilder responseBuilder = tokenManager.responseBuilder(realm, targetClient, event, this.session, targetUserSession, clientSession)
                .generateAccessToken();
        responseBuilder.getAccessToken().issuedFor(client.getClientId());

        if (requestedTokenType.equals(OAuth2Constants.REFRESH_TOKEN_TYPE)) {
            responseBuilder.generateRefreshToken();
            responseBuilder.getRefreshToken().issuedFor(client.getClientId());
        }

        String scopeParam = clientSession.getNote(OAuth2Constants.SCOPE);
        if (TokenUtil.isOIDCRequest(scopeParam)) {
            responseBuilder.generateIDToken();
        }

        AccessTokenResponse res = responseBuilder.build();

        event.success();

        return Cors.add(request, Response.ok(res, MediaType.APPLICATION_JSON_TYPE)).auth().allowedOrigins(uriInfo, client).allowedMethods("POST").exposedHeaders(Cors.ACCESS_CONTROL_ALLOW_METHODS).build();
    }


    // https://tools.ietf.org/html/rfc7636#section-4.1
    private boolean isValidPkceCodeVerifier(String codeVerifier) {
        if (codeVerifier.length() < OIDCLoginProtocol.PKCE_CODE_VERIFIER_MIN_LENGTH) {
            logger.infof(" Error: PKCE codeVerifier length under lower limit , codeVerifier = %s", codeVerifier);
            return false;
        }
        if (codeVerifier.length() > OIDCLoginProtocol.PKCE_CODE_VERIFIER_MAX_LENGTH) {
            logger.infof(" Error: PKCE codeVerifier length over upper limit , codeVerifier = %s", codeVerifier);
            return false;
        }
        Matcher m = VALID_CODE_VERIFIER_PATTERN.matcher(codeVerifier);
        return m.matches() ? true : false;
    }

    // https://tools.ietf.org/html/rfc7636#section-4.6
    private String generateS256CodeChallenge(String codeVerifier) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(codeVerifier.getBytes("ISO_8859_1"));
        byte[] digestBytes = md.digest();
        String codeVerifierEncoded = Base64Url.encode(digestBytes);
        return codeVerifierEncoded;
    }
 
}
