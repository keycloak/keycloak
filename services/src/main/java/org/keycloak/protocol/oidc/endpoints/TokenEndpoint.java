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
import org.jboss.resteasy.spi.HttpResponse;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.authorization.AuthorizationTokenService;
import org.keycloak.authorization.util.Tokens;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.ExchangeExternalToken;
import org.keycloak.broker.provider.ExchangeTokenToIdentityProviderToken;
import org.keycloak.broker.provider.IdentityProvider;
import org.keycloak.broker.provider.IdentityProviderFactory;
import org.keycloak.broker.provider.IdentityProviderMapper;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.Profile;
import org.keycloak.common.constants.ServiceAccountConstants;
import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.constants.AdapterConstants;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.utils.AuthenticationFlowResolver;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.protocol.oidc.utils.AuthorizeClientUtil;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.idm.authorization.AuthorizationRequest.Metadata;
import org.keycloak.services.CorsErrorResponseException;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.Urls;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.AuthenticationSessionManager;
import org.keycloak.services.managers.BruteForceProtector;
import org.keycloak.services.managers.ClientManager;
import org.keycloak.protocol.oidc.utils.OAuth2Code;
import org.keycloak.protocol.oidc.utils.OAuth2CodeParser;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.Cors;
import org.keycloak.services.resources.IdentityBrokerService;
import org.keycloak.services.resources.admin.AdminAuth;
import org.keycloak.services.resources.admin.permissions.AdminPermissions;
import org.keycloak.services.util.MtlsHoKTokenUtil;
import org.keycloak.services.util.DefaultClientSessionContext;
import org.keycloak.services.validation.Validation;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.sessions.RootAuthenticationSessionModel;
import org.keycloak.util.TokenUtil;
import org.keycloak.utils.ProfileHelper;

import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.security.MessageDigest;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.keycloak.models.ImpersonationSessionNote.IMPERSONATOR_ID;
import static org.keycloak.models.ImpersonationSessionNote.IMPERSONATOR_USERNAME;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class TokenEndpoint {

    private static final Logger logger = Logger.getLogger(TokenEndpoint.class);
    private MultivaluedMap<String, String> formParams;
    private ClientModel client;
    private Map<String, String> clientAuthAttributes;

    private enum Action {
        AUTHORIZATION_CODE, REFRESH_TOKEN, PASSWORD, CLIENT_CREDENTIALS, TOKEN_EXCHANGE, PERMISSION
    }

    // https://tools.ietf.org/html/rfc7636#section-4.2
    private static final Pattern VALID_CODE_VERIFIER_PATTERN = Pattern.compile("^[0-9a-zA-Z\\-\\.~_]+$");

    @Context
    private KeycloakSession session;

    @Context
    private HttpRequest request;

    @Context
    private HttpResponse httpResponse;

    @Context
    private HttpHeaders headers;

    @Context
    private ClientConnection clientConnection;

    private final TokenManager tokenManager;
    private final RealmModel realm;
    private final EventBuilder event;

    private Action action;

    private String grantType;

    private Cors cors;

    public TokenEndpoint(TokenManager tokenManager, RealmModel realm, EventBuilder event) {
        this.tokenManager = tokenManager;
        this.realm = realm;
        this.event = event;
    }

    @POST
    public Response processGrantRequest() {
        cors = Cors.add(request).auth().allowedMethods("POST").auth().exposedHeaders(Cors.ACCESS_CONTROL_ALLOW_METHODS);

        formParams = request.getDecodedFormParameters();
        grantType = formParams.getFirst(OIDCLoginProtocol.GRANT_TYPE_PARAM);

        // https://tools.ietf.org/html/rfc6749#section-5.1
        // The authorization server MUST include the HTTP "Cache-Control" response header field
        // with a value of "no-store" as well as the "Pragma" response header field with a value of "no-cache".
        MultivaluedMap<String, Object> outputHeaders = httpResponse.getOutputHeaders();
        outputHeaders.putSingle("Cache-Control", "no-store");
        outputHeaders.putSingle("Pragma", "no-cache");

        checkSsl();
        checkRealm();
        checkGrantType();

        if (!action.equals(Action.PERMISSION)) {
            checkClient();
        }

        switch (action) {
            case AUTHORIZATION_CODE:
                return codeToToken();
            case REFRESH_TOKEN:
                return refreshTokenGrant();
            case PASSWORD:
                return resourceOwnerPasswordCredentialsGrant();
            case CLIENT_CREDENTIALS:
                return clientCredentialsGrant();
            case TOKEN_EXCHANGE:
                return tokenExchange();
            case PERMISSION:
                return permissionGrant();
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
        if (!session.getContext().getUri().getBaseUri().getScheme().equals("https") && realm.getSslRequired().isRequired(clientConnection)) {
            throw new CorsErrorResponseException(cors.allowAllOrigins(), OAuthErrorException.INVALID_REQUEST, "HTTPS required", Response.Status.FORBIDDEN);
        }
    }

    private void checkRealm() {
        if (!realm.isEnabled()) {
            throw new CorsErrorResponseException(cors.allowAllOrigins(), "access_denied", "Realm not enabled", Response.Status.FORBIDDEN);
        }
    }

    private void checkClient() {
        AuthorizeClientUtil.ClientAuthResult clientAuth = AuthorizeClientUtil.authorizeClient(session, event);
        client = clientAuth.getClient();
        clientAuthAttributes = clientAuth.getClientAuthAttributes();

        cors.allowedOrigins(session, client);

        if (client.isBearerOnly()) {
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_CLIENT, "Bearer-only not allowed", Response.Status.BAD_REQUEST);
        }


    }

    private void checkGrantType() {
        if (grantType == null) {
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST, "Missing form parameter: " + OIDCLoginProtocol.GRANT_TYPE_PARAM, Response.Status.BAD_REQUEST);
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
        } else if (grantType.equals(OAuth2Constants.UMA_GRANT_TYPE)) {
            event.event(EventType.PERMISSION_TOKEN);
            action = Action.PERMISSION;
        } else {
            throw new CorsErrorResponseException(cors, OAuthErrorException.UNSUPPORTED_GRANT_TYPE,
                "Unsupported " + OIDCLoginProtocol.GRANT_TYPE_PARAM, Response.Status.BAD_REQUEST);
        }

        event.detail(Details.GRANT_TYPE, grantType);
    }

    public Response codeToToken() {
        String code = formParams.getFirst(OAuth2Constants.CODE);
        if (code == null) {
            event.error(Errors.INVALID_CODE);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST, "Missing parameter: " + OAuth2Constants.CODE, Response.Status.BAD_REQUEST);
        }

        OAuth2CodeParser.ParseResult parseResult = OAuth2CodeParser.parseCode(session, code, realm, event);
        if (parseResult.isIllegalCode()) {
            AuthenticatedClientSessionModel clientSession = parseResult.getClientSession();

            // Attempt to use same code twice should invalidate existing clientSession
            if (clientSession != null) {
                clientSession.detachFromUserSession();
            }

            event.error(Errors.INVALID_CODE);

            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_GRANT, "Code not valid", Response.Status.BAD_REQUEST);
        }

        AuthenticatedClientSessionModel clientSession = parseResult.getClientSession();

        if (parseResult.isExpiredCode()) {
            event.error(Errors.EXPIRED_CODE);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_GRANT, "Code is expired", Response.Status.BAD_REQUEST);
        }

        UserSessionModel userSession = null;
        if (clientSession != null) {
            userSession = clientSession.getUserSession();
        }

        if (userSession == null) {
            event.error(Errors.USER_SESSION_NOT_FOUND);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_GRANT, "User session not found", Response.Status.BAD_REQUEST);
        }


        UserModel user = userSession.getUser();
        if (user == null) {
            event.error(Errors.USER_NOT_FOUND);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_GRANT, "User not found", Response.Status.BAD_REQUEST);
        }

        event.user(userSession.getUser());

        if (!user.isEnabled()) {
            event.error(Errors.USER_DISABLED);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_GRANT, "User disabled", Response.Status.BAD_REQUEST);
        }

        OAuth2Code codeData = parseResult.getCodeData();
        String redirectUri = codeData.getRedirectUriParam();
        String redirectUriParam = formParams.getFirst(OAuth2Constants.REDIRECT_URI);

        // KEYCLOAK-4478 Backwards compatibility with the adapters earlier than KC 3.4.2
        if (redirectUriParam != null && redirectUriParam.contains("session_state=") && !redirectUri.contains("session_state=")) {
            redirectUriParam = KeycloakUriBuilder.fromUri(redirectUriParam)
                    .replaceQueryParam(OAuth2Constants.SESSION_STATE, null)
                    .build().toString();
        }

        if (redirectUri != null && !redirectUri.equals(redirectUriParam)) {
            event.error(Errors.INVALID_CODE);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_GRANT, "Incorrect redirect_uri", Response.Status.BAD_REQUEST);
        }

        if (!client.getClientId().equals(clientSession.getClient().getClientId())) {
            event.error(Errors.INVALID_CODE);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_GRANT, "Auth error", Response.Status.BAD_REQUEST);
        }

        if (!client.isStandardFlowEnabled()) {
            event.error(Errors.NOT_ALLOWED);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_GRANT, "Client not allowed to exchange code", Response.Status.BAD_REQUEST);
        }

        if (!AuthenticationManager.isSessionValid(realm, userSession)) {
            event.error(Errors.USER_SESSION_NOT_FOUND);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_GRANT, "Session not active", Response.Status.BAD_REQUEST);
        }

        // https://tools.ietf.org/html/rfc7636#section-4.6
        String codeVerifier = formParams.getFirst(OAuth2Constants.CODE_VERIFIER);
        String codeChallenge = codeData.getCodeChallenge();
        String codeChallengeMethod = codeData.getCodeChallengeMethod();
        String authUserId = user.getId();
        String authUsername = user.getUsername();
        if (authUserId == null) {
            authUserId = "unknown";
        }
        if (authUsername == null) {
            authUsername = "unknown";
        }

        if (codeChallengeMethod != null && !codeChallengeMethod.isEmpty()) {
            checkParamsForPkceEnforcedClient(codeVerifier, codeChallenge, codeChallengeMethod, authUserId, authUsername);
        } else {
            // PKCE Activation is OFF, execute the codes implemented in KEYCLOAK-2604
            checkParamsForPkceNotEnforcedClient(codeVerifier, codeChallenge, codeChallengeMethod, authUserId, authUsername);
        }

        updateClientSession(clientSession);
        updateUserSessionFromClientAuth(userSession);

        // Compute client scopes again from scope parameter. Check if user still has them granted
        // (but in code-to-token request, it could just theoretically happen that they are not available)
        String scopeParam = codeData.getScope();
        Set<ClientScopeModel> clientScopes = TokenManager.getRequestedClientScopes(scopeParam, client);
        if (!TokenManager.verifyConsentStillAvailable(session, user, client, clientScopes)) {
            event.error(Errors.NOT_ALLOWED);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_SCOPE, "Client no longer has requested consent from user", Response.Status.BAD_REQUEST);
        }

        ClientSessionContext clientSessionCtx = DefaultClientSessionContext.fromClientSessionAndClientScopes(clientSession, clientScopes, session);

        // Set nonce as an attribute in the ClientSessionContext. Will be used for the token generation
        clientSessionCtx.setAttribute(OIDCLoginProtocol.NONCE_PARAM, codeData.getNonce());

        AccessToken token = tokenManager.createClientAccessToken(session, realm, client, user, userSession, clientSessionCtx);

        TokenManager.AccessTokenResponseBuilder responseBuilder = tokenManager.responseBuilder(realm, client, event, session, userSession, clientSessionCtx)
                .accessToken(token)
                .generateRefreshToken();

        // KEYCLOAK-6771 Certificate Bound Token
        // https://tools.ietf.org/html/draft-ietf-oauth-mtls-08#section-3
        if (OIDCAdvancedConfigWrapper.fromClientModel(client).isUseMtlsHokToken()) {
            AccessToken.CertConf certConf = MtlsHoKTokenUtil.bindTokenWithClientCertificate(request, session);
            if (certConf != null) {
                responseBuilder.getAccessToken().setCertConf(certConf);
                responseBuilder.getRefreshToken().setCertConf(certConf);
            } else {
                event.error(Errors.INVALID_REQUEST);
                throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST, "Client Certification missing for MTLS HoK Token Binding", Response.Status.BAD_REQUEST);
            }
        }

        if (TokenUtil.isOIDCRequest(scopeParam)) {
            responseBuilder.generateIDToken();
        }
        
        AccessTokenResponse res = null;
        try {
            res = responseBuilder.build();
        } catch (RuntimeException re) {
            if ("can not get encryption KEK".equals(re.getMessage())) {
                throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST, "can not get encryption KEK", Response.Status.BAD_REQUEST);
            } else {
                throw re;
            }
        }
        
        event.success();

        return cors.builder(Response.ok(res).type(MediaType.APPLICATION_JSON_TYPE)).build();
    }

    private void checkParamsForPkceEnforcedClient(String codeVerifier, String codeChallenge, String codeChallengeMethod, String authUserId, String authUsername) {
        // check whether code verifier is specified
        if (codeVerifier == null) {
            logger.warnf("PKCE code verifier not specified, authUserId = %s, authUsername = %s", authUserId, authUsername);
            event.error(Errors.CODE_VERIFIER_MISSING);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_GRANT, "PKCE code verifier not specified", Response.Status.BAD_REQUEST); 
        }
        verifyCodeVerifier(codeVerifier, codeChallenge, codeChallengeMethod, authUserId, authUsername);
    }

    private void checkParamsForPkceNotEnforcedClient(String codeVerifier, String codeChallenge, String codeChallengeMethod, String authUserId, String authUsername) {
        if (codeChallenge != null && codeVerifier == null) {
            logger.warnf("PKCE code verifier not specified, authUserId = %s, authUsername = %s", authUserId, authUsername);
            event.error(Errors.CODE_VERIFIER_MISSING);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_GRANT, "PKCE code verifier not specified", Response.Status.BAD_REQUEST);
        }

        if (codeChallenge != null) {
            verifyCodeVerifier(codeVerifier, codeChallenge, codeChallengeMethod, authUserId, authUsername);
        }
    }

    private void verifyCodeVerifier(String codeVerifier, String codeChallenge, String codeChallengeMethod, String authUserId, String authUsername) {
        // check whether code verifier is formatted along with the PKCE specification

        if (!isValidPkceCodeVerifier(codeVerifier)) {
            logger.infof("PKCE invalid code verifier");
            event.error(Errors.INVALID_CODE_VERIFIER);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_GRANT, "PKCE invalid code verifier", Response.Status.BAD_REQUEST);
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
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_GRANT, "PKCE code verification failed, not supported algorithm specified", Response.Status.BAD_REQUEST);
        }
        if (!codeChallenge.equals(codeVerifierEncoded)) {
            logger.warnf("PKCE verification failed. authUserId = %s, authUsername = %s", authUserId, authUsername);
            event.error(Errors.PKCE_VERIFICATION_FAILED);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_GRANT, "PKCE verification failed", Response.Status.BAD_REQUEST);
        } else {
            logger.debugf("PKCE verification success. codeVerifierEncoded = %s, codeChallenge = %s", codeVerifierEncoded, codeChallenge);
        }
    }

    public Response refreshTokenGrant() {
        String refreshToken = formParams.getFirst(OAuth2Constants.REFRESH_TOKEN);
        if (refreshToken == null) {
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST, "No refresh token", Response.Status.BAD_REQUEST);
        }

        AccessTokenResponse res;
        try {
            // KEYCLOAK-6771 Certificate Bound Token
            TokenManager.RefreshResult result = tokenManager.refreshAccessToken(session, session.getContext().getUri(), clientConnection, realm, client, refreshToken, event, headers, request);
            res = result.getResponse();

            if (!result.isOfflineToken()) {
                UserSessionModel userSession = session.sessions().getUserSession(realm, res.getSessionState());
                AuthenticatedClientSessionModel clientSession = userSession.getAuthenticatedClientSessionByClient(client.getId());
                updateClientSession(clientSession);
                updateUserSessionFromClientAuth(userSession);
            }

        } catch (OAuthErrorException e) {
            logger.trace(e.getMessage(), e);
            // KEYCLOAK-6771 Certificate Bound Token
            if (MtlsHoKTokenUtil.CERT_VERIFY_ERROR_DESC.equals(e.getDescription())) {
                event.error(Errors.NOT_ALLOWED);
                throw new CorsErrorResponseException(cors, e.getError(), e.getDescription(), Response.Status.UNAUTHORIZED);
            } else {
                event.error(Errors.INVALID_TOKEN);
                throw new CorsErrorResponseException(cors, e.getError(), e.getDescription(), Response.Status.BAD_REQUEST);
            }
        }

        event.success();

        return cors.builder(Response.ok(res, MediaType.APPLICATION_JSON_TYPE)).build();
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

            String oldClientSessionState = clientSession.getNote(AdapterConstants.CLIENT_SESSION_STATE);
            if (!adapterSessionId.equals(oldClientSessionState)) {
                clientSession.setNote(AdapterConstants.CLIENT_SESSION_STATE, adapterSessionId);
            }

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

    public Response resourceOwnerPasswordCredentialsGrant() {
        event.detail(Details.AUTH_METHOD, "oauth_credentials");

        if (!client.isDirectAccessGrantsEnabled()) {
            event.error(Errors.NOT_ALLOWED);
            throw new CorsErrorResponseException(cors, OAuthErrorException.UNAUTHORIZED_CLIENT, "Client not allowed for direct access grants", Response.Status.BAD_REQUEST);
        }

        if (client.isConsentRequired()) {
            event.error(Errors.CONSENT_DENIED);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_CLIENT, "Client requires user consent", Response.Status.BAD_REQUEST);
        }
        String scope = formParams.getFirst(OAuth2Constants.SCOPE);

        RootAuthenticationSessionModel rootAuthSession = new AuthenticationSessionManager(session).createAuthenticationSession(realm, false);
        AuthenticationSessionModel authSession = rootAuthSession.createAuthenticationSession(client);

        authSession.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        authSession.setAction(AuthenticatedClientSessionModel.Action.AUTHENTICATE.name());
        authSession.setClientNote(OIDCLoginProtocol.ISSUER, Urls.realmIssuer(session.getContext().getUri().getBaseUri(), realm.getName()));
        authSession.setClientNote(OIDCLoginProtocol.SCOPE_PARAM, scope);

        AuthenticationFlowModel flow = AuthenticationFlowResolver.resolveDirectGrantFlow(authSession);
        String flowId = flow.getId();
        AuthenticationProcessor processor = new AuthenticationProcessor();
        processor.setAuthenticationSession(authSession)
                .setFlowId(flowId)
                .setConnection(clientConnection)
                .setEventBuilder(event)
                .setRealm(realm)
                .setSession(session)
                .setUriInfo(session.getContext().getUri())
                .setRequest(request);
        Response challenge = processor.authenticateOnly();
        if (challenge != null) {
            cors.build(httpResponse);
            return challenge;
        }
        processor.evaluateRequiredActionTriggers();
        UserModel user = authSession.getAuthenticatedUser();
        if (user.getRequiredActions() != null && user.getRequiredActions().size() > 0) {
            event.error(Errors.RESOLVE_REQUIRED_ACTIONS);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_GRANT, "Account is not fully set up", Response.Status.BAD_REQUEST);

        }

        AuthenticationManager.setClientScopesInSession(authSession);

        ClientSessionContext clientSessionCtx = processor.attachSession();
        UserSessionModel userSession = processor.getUserSession();
        updateUserSessionFromClientAuth(userSession);

        TokenManager.AccessTokenResponseBuilder responseBuilder = tokenManager.responseBuilder(realm, client, event, session, userSession, clientSessionCtx)
                .generateAccessToken()
                .generateRefreshToken();

        String scopeParam = clientSessionCtx.getClientSession().getNote(OAuth2Constants.SCOPE);
        if (TokenUtil.isOIDCRequest(scopeParam)) {
            responseBuilder.generateIDToken();
        }

        // TODO : do the same as codeToToken()
        AccessTokenResponse res = responseBuilder.build();


        event.success();

        return cors.builder(Response.ok(res, MediaType.APPLICATION_JSON_TYPE)).build();
    }

    public Response clientCredentialsGrant() {
        if (client.isBearerOnly()) {
            event.error(Errors.INVALID_CLIENT);
            throw new CorsErrorResponseException(cors, OAuthErrorException.UNAUTHORIZED_CLIENT, "Bearer-only client not allowed to retrieve service account", Response.Status.UNAUTHORIZED);
        }
        if (client.isPublicClient()) {
            event.error(Errors.INVALID_CLIENT);
            throw new CorsErrorResponseException(cors, OAuthErrorException.UNAUTHORIZED_CLIENT, "Public client not allowed to retrieve service account", Response.Status.UNAUTHORIZED);
        }
        if (!client.isServiceAccountsEnabled()) {
            event.error(Errors.INVALID_CLIENT);
            throw new CorsErrorResponseException(cors, OAuthErrorException.UNAUTHORIZED_CLIENT, "Client not enabled to retrieve service account", Response.Status.UNAUTHORIZED);
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
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST, "User '" + clientUsername + "' disabled", Response.Status.UNAUTHORIZED);
        }

        String scope = formParams.getFirst(OAuth2Constants.SCOPE);

        RootAuthenticationSessionModel rootAuthSession = new AuthenticationSessionManager(session).createAuthenticationSession(realm, false);
        AuthenticationSessionModel authSession = rootAuthSession.createAuthenticationSession(client);

        authSession.setAuthenticatedUser(clientUser);
        authSession.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        authSession.setClientNote(OIDCLoginProtocol.ISSUER, Urls.realmIssuer(session.getContext().getUri().getBaseUri(), realm.getName()));
        authSession.setClientNote(OIDCLoginProtocol.SCOPE_PARAM, scope);

        UserSessionModel userSession = session.sessions().createUserSession(authSession.getParentSession().getId(), realm, clientUser, clientUsername,
                clientConnection.getRemoteAddr(), ServiceAccountConstants.CLIENT_AUTH, false, null, null);
        event.session(userSession);

        AuthenticationManager.setClientScopesInSession(authSession);
        ClientSessionContext clientSessionCtx = TokenManager.attachAuthenticationSession(session, userSession, authSession);

        // Notes about client details
        userSession.setNote(ServiceAccountConstants.CLIENT_ID, client.getClientId());
        userSession.setNote(ServiceAccountConstants.CLIENT_HOST, clientConnection.getRemoteHost());
        userSession.setNote(ServiceAccountConstants.CLIENT_ADDRESS, clientConnection.getRemoteAddr());

        updateUserSessionFromClientAuth(userSession);

        TokenManager.AccessTokenResponseBuilder responseBuilder = tokenManager.responseBuilder(realm, client, event, session, userSession, clientSessionCtx)
                .generateAccessToken()
                .generateRefreshToken();

        String scopeParam = clientSessionCtx.getClientSession().getNote(OAuth2Constants.SCOPE);
        if (TokenUtil.isOIDCRequest(scopeParam)) {
            responseBuilder.generateIDToken();
        }

        // TODO : do the same as codeToToken()
        AccessTokenResponse res = responseBuilder.build();

        event.success();

        return cors.builder(Response.ok(res, MediaType.APPLICATION_JSON_TYPE)).build();
    }

    public Response tokenExchange() {
        ProfileHelper.requireFeature(Profile.Feature.TOKEN_EXCHANGE);

        event.detail(Details.AUTH_METHOD, "token_exchange");
        event.client(client);

        UserModel tokenUser = null;
        UserSessionModel tokenSession = null;
        AccessToken token = null;

        String subjectToken = formParams.getFirst(OAuth2Constants.SUBJECT_TOKEN);
        if (subjectToken != null) {
            String subjectTokenType = formParams.getFirst(OAuth2Constants.SUBJECT_TOKEN_TYPE);
            String realmIssuerUrl = Urls.realmIssuer(session.getContext().getUri().getBaseUri(), realm.getName());
            String subjectIssuer = formParams.getFirst(OAuth2Constants.SUBJECT_ISSUER);

            if (subjectIssuer == null && OAuth2Constants.JWT_TOKEN_TYPE.equals(subjectTokenType)) {
                try {
                    JWSInput jws = new JWSInput(subjectToken);
                    JsonWebToken jwt = jws.readJsonContent(JsonWebToken.class);
                    subjectIssuer = jwt.getIssuer();
                } catch (JWSInputException e) {
                    event.detail(Details.REASON, "unable to parse jwt subject_token");
                    event.error(Errors.INVALID_TOKEN);
                    throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_TOKEN, "Invalid token type, must be access token", Response.Status.BAD_REQUEST);

                }
            }

            if (subjectIssuer != null && !realmIssuerUrl.equals(subjectIssuer)) {
                event.detail(OAuth2Constants.SUBJECT_ISSUER, subjectIssuer);
                return exchangeExternalToken(subjectIssuer, subjectToken);

            }

            if (subjectTokenType != null && !subjectTokenType.equals(OAuth2Constants.ACCESS_TOKEN_TYPE)) {
                event.detail(Details.REASON, "subject_token supports access tokens only");
                event.error(Errors.INVALID_TOKEN);
                throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_TOKEN, "Invalid token type, must be access token", Response.Status.BAD_REQUEST);

            }

            AuthenticationManager.AuthResult authResult = AuthenticationManager.verifyIdentityToken(session, realm, session.getContext().getUri(), clientConnection, true, true, false, subjectToken, headers);
            if (authResult == null) {
                event.detail(Details.REASON, "subject_token validation failure");
                event.error(Errors.INVALID_TOKEN);
                throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_TOKEN, "Invalid token", Response.Status.BAD_REQUEST);
            }

            tokenUser = authResult.getUser();
            tokenSession = authResult.getSession();
            token = authResult.getToken();
        }

        String requestedSubject = formParams.getFirst(OAuth2Constants.REQUESTED_SUBJECT);
        if (requestedSubject != null) {
            event.detail(Details.REQUESTED_SUBJECT, requestedSubject);
            UserModel requestedUser = session.users().getUserByUsername(requestedSubject, realm);
            if (requestedUser == null) {
                requestedUser = session.users().getUserById(requestedSubject, realm);
            }

            if (requestedUser == null) {
                // We always returned access denied to avoid username fishing
                event.detail(Details.REASON, "requested_subject not found");
                event.error(Errors.NOT_ALLOWED);
                throw new CorsErrorResponseException(cors, OAuthErrorException.ACCESS_DENIED, "Client not allowed to exchange", Response.Status.FORBIDDEN);

            }

            if (token != null) {
                event.detail(Details.IMPERSONATOR, tokenUser.getUsername());
                // for this case, the user represented by the token, must have permission to impersonate.
                AdminAuth auth = new AdminAuth(realm, token, tokenUser, client);
                if (!AdminPermissions.evaluator(session, realm, auth).users().canImpersonate(requestedUser)) {
                    event.detail(Details.REASON, "subject not allowed to impersonate");
                    event.error(Errors.NOT_ALLOWED);
                    throw new CorsErrorResponseException(cors, OAuthErrorException.ACCESS_DENIED, "Client not allowed to exchange", Response.Status.FORBIDDEN);
                }

            } else {
                // no token is being exchanged, this is a direct exchange.  Client must be authenticated, not public, and must be allowed
                // to impersonate
                if (client.isPublicClient()) {
                    event.detail(Details.REASON, "public clients not allowed");
                    event.error(Errors.NOT_ALLOWED);
                    throw new CorsErrorResponseException(cors, OAuthErrorException.ACCESS_DENIED, "Client not allowed to exchange", Response.Status.FORBIDDEN);

                }
                if (!AdminPermissions.management(session, realm).users().canClientImpersonate(client, requestedUser)) {
                    event.detail(Details.REASON, "client not allowed to impersonate");
                    event.error(Errors.NOT_ALLOWED);
                    throw new CorsErrorResponseException(cors, OAuthErrorException.ACCESS_DENIED, "Client not allowed to exchange", Response.Status.FORBIDDEN);
                }
            }

            tokenSession = session.sessions().createUserSession(realm, requestedUser, requestedUser.getUsername(), clientConnection.getRemoteAddr(), "impersonate", false, null, null);
            if (tokenUser != null) {
                tokenSession.setNote(IMPERSONATOR_ID.toString(), tokenUser.getId());
                tokenSession.setNote(IMPERSONATOR_USERNAME.toString(), tokenUser.getUsername());
            }

            tokenUser = requestedUser;
        }

        String requestedIssuer = formParams.getFirst(OAuth2Constants.REQUESTED_ISSUER);
        if (requestedIssuer == null) {
            return exchangeClientToClient(tokenUser, tokenSession);
        } else {
            try {
                return exchangeToIdentityProvider(tokenUser, tokenSession, requestedIssuer);
            } finally {
                if (subjectToken == null) { // we are naked! So need to clean up user session
                    try {
                        session.sessions().removeUserSession(realm, tokenSession);
                    } catch (Exception ignore) {

                    }
                }
            }
         }
    }

    public Response exchangeToIdentityProvider(UserModel targetUser, UserSessionModel targetUserSession, String requestedIssuer) {
        event.detail(Details.REQUESTED_ISSUER, requestedIssuer);
        IdentityProviderModel providerModel = realm.getIdentityProviderByAlias(requestedIssuer);
        if (providerModel == null) {
            event.detail(Details.REASON, "unknown requested_issuer");
            event.error(Errors.UNKNOWN_IDENTITY_PROVIDER);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST, "Invalid issuer", Response.Status.BAD_REQUEST);
        }

        IdentityProvider provider = IdentityBrokerService.getIdentityProvider(session, realm, requestedIssuer);
        if (!(provider instanceof ExchangeTokenToIdentityProviderToken)) {
            event.detail(Details.REASON, "exchange unsupported by requested_issuer");
            event.error(Errors.UNKNOWN_IDENTITY_PROVIDER);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST, "Issuer does not support token exchange", Response.Status.BAD_REQUEST);
        }
        if (!AdminPermissions.management(session, realm).idps().canExchangeTo(client, providerModel)) {
            event.detail(Details.REASON, "client not allowed to exchange for requested_issuer");
            event.error(Errors.NOT_ALLOWED);
            throw new CorsErrorResponseException(cors, OAuthErrorException.ACCESS_DENIED, "Client not allowed to exchange", Response.Status.FORBIDDEN);
        }
        Response response = ((ExchangeTokenToIdentityProviderToken)provider).exchangeFromToken(session.getContext().getUri(), event, client, targetUserSession, targetUser, formParams);
        return cors.builder(Response.fromResponse(response)).build();

    }

    protected Response exchangeClientToClient(UserModel targetUser, UserSessionModel targetUserSession) {
        String requestedTokenType = formParams.getFirst(OAuth2Constants.REQUESTED_TOKEN_TYPE);
        if (requestedTokenType == null) {
            requestedTokenType = OAuth2Constants.REFRESH_TOKEN_TYPE;
        } else if (!requestedTokenType.equals(OAuth2Constants.ACCESS_TOKEN_TYPE) && !requestedTokenType.equals(OAuth2Constants.REFRESH_TOKEN_TYPE)) {
            event.detail(Details.REASON, "requested_token_type unsupported");
            event.error(Errors.INVALID_REQUEST);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST, "requested_token_type unsupported", Response.Status.BAD_REQUEST);

        }
        ClientModel targetClient = client;
        String audience = formParams.getFirst(OAuth2Constants.AUDIENCE);
        if (audience != null) {
            targetClient = realm.getClientByClientId(audience);
            if (targetClient == null) {
                event.detail(Details.REASON, "audience not found");
                event.error(Errors.CLIENT_NOT_FOUND);
                throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_CLIENT, "Audience not found", Response.Status.BAD_REQUEST);

            }
        }

        if (targetClient.isConsentRequired()) {
            event.detail(Details.REASON, "audience requires consent");
            event.error(Errors.CONSENT_DENIED);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_CLIENT, "Client requires user consent", Response.Status.BAD_REQUEST);
        }

        if (!targetClient.equals(client) && !AdminPermissions.management(session, realm).clients().canExchangeTo(client, targetClient)) {
            event.detail(Details.REASON, "client not allowed to exchange to audience");
            event.error(Errors.NOT_ALLOWED);
            throw new CorsErrorResponseException(cors, OAuthErrorException.ACCESS_DENIED, "Client not allowed to exchange", Response.Status.FORBIDDEN);
        }

        String scope = formParams.getFirst(OAuth2Constants.SCOPE);

        RootAuthenticationSessionModel rootAuthSession = new AuthenticationSessionManager(session).createAuthenticationSession(realm, false);
        AuthenticationSessionModel authSession = rootAuthSession.createAuthenticationSession(targetClient);

        authSession.setAuthenticatedUser(targetUser);
        authSession.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        authSession.setClientNote(OIDCLoginProtocol.ISSUER, Urls.realmIssuer(session.getContext().getUri().getBaseUri(), realm.getName()));
        authSession.setClientNote(OIDCLoginProtocol.SCOPE_PARAM, scope);

        event.session(targetUserSession);

        AuthenticationManager.setClientScopesInSession(authSession);
        ClientSessionContext clientSessionCtx = TokenManager.attachAuthenticationSession(this.session, targetUserSession, authSession);

        updateUserSessionFromClientAuth(targetUserSession);

        TokenManager.AccessTokenResponseBuilder responseBuilder = tokenManager.responseBuilder(realm, targetClient, event, this.session, targetUserSession, clientSessionCtx)
                .generateAccessToken();
        responseBuilder.getAccessToken().issuedFor(client.getClientId());

        if (audience != null) {
            responseBuilder.getAccessToken().addAudience(audience);
        }

        if (requestedTokenType.equals(OAuth2Constants.REFRESH_TOKEN_TYPE)) {
            responseBuilder.generateRefreshToken();
            responseBuilder.getRefreshToken().issuedFor(client.getClientId());
        }

        String scopeParam = clientSessionCtx.getClientSession().getNote(OAuth2Constants.SCOPE);
        if (TokenUtil.isOIDCRequest(scopeParam)) {
            responseBuilder.generateIDToken();
        }

        AccessTokenResponse res = responseBuilder.build();
        event.detail(Details.AUDIENCE, targetClient.getClientId());

        event.success();

        return cors.builder(Response.ok(res, MediaType.APPLICATION_JSON_TYPE)).build();
    }

    public Response exchangeExternalToken(String issuer, String subjectToken) {
        ExchangeExternalToken externalIdp = null;
        IdentityProviderModel externalIdpModel = null;

        for (IdentityProviderModel idpModel : realm.getIdentityProviders()) {
            IdentityProviderFactory factory = IdentityBrokerService.getIdentityProviderFactory(session, idpModel);
            IdentityProvider idp = factory.create(session, idpModel);
            if (idp instanceof ExchangeExternalToken) {
                ExchangeExternalToken external = (ExchangeExternalToken) idp;
                if (idpModel.getAlias().equals(issuer) || external.isIssuer(issuer, formParams)) {
                    externalIdp = external;
                    externalIdpModel = idpModel;
                    break;
                }
            }
        }


        if (externalIdp == null) {
            event.error(Errors.INVALID_ISSUER);
            throw new CorsErrorResponseException(cors, Errors.INVALID_ISSUER, "Invalid " + OAuth2Constants.SUBJECT_ISSUER + " parameter", Response.Status.BAD_REQUEST);
        }
        if (!AdminPermissions.management(session, realm).idps().canExchangeTo(client, externalIdpModel)) {
            event.detail(Details.REASON, "client not allowed to exchange subject_issuer");
            event.error(Errors.NOT_ALLOWED);
            throw new CorsErrorResponseException(cors, OAuthErrorException.ACCESS_DENIED, "Client not allowed to exchange", Response.Status.FORBIDDEN);
        }
        BrokeredIdentityContext context = externalIdp.exchangeExternal(event, formParams);
        if (context == null) {
            event.error(Errors.INVALID_ISSUER);
            throw new CorsErrorResponseException(cors, Errors.INVALID_ISSUER, "Invalid " + OAuth2Constants.SUBJECT_ISSUER + " parameter", Response.Status.BAD_REQUEST);
        }

        UserModel user = importUserFromExternalIdentity(context);

        UserSessionModel userSession = session.sessions().createUserSession(realm, user, user.getUsername(), clientConnection.getRemoteAddr(), "external-exchange", false, null, null);
        externalIdp.exchangeExternalComplete(userSession, context, formParams);

        // this must exist so that we can obtain access token from user session if idp's store tokens is off
        userSession.setNote(IdentityProvider.EXTERNAL_IDENTITY_PROVIDER, externalIdpModel.getAlias());
        userSession.setNote(IdentityProvider.FEDERATED_ACCESS_TOKEN, subjectToken);

        return exchangeClientToClient(user, userSession);
    }

    protected UserModel importUserFromExternalIdentity(BrokeredIdentityContext context) {
        IdentityProviderModel identityProviderConfig = context.getIdpConfig();

        String providerId = identityProviderConfig.getAlias();

        // do we need this?
        //AuthenticationSessionModel authenticationSession = clientCode.getClientSession();
        //context.setAuthenticationSession(authenticationSession);
        //session.getContext().setClient(authenticationSession.getClient());

        context.getIdp().preprocessFederatedIdentity(session, realm, context);
        Set<IdentityProviderMapperModel> mappers = realm.getIdentityProviderMappersByAlias(context.getIdpConfig().getAlias());
        if (mappers != null) {
            KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();
            for (IdentityProviderMapperModel mapper : mappers) {
                IdentityProviderMapper target = (IdentityProviderMapper)sessionFactory.getProviderFactory(IdentityProviderMapper.class, mapper.getIdentityProviderMapper());
                target.preprocessFederatedIdentity(session, realm, mapper, context);
            }
        }

        FederatedIdentityModel federatedIdentityModel = new FederatedIdentityModel(providerId, context.getId(),
                context.getUsername(), context.getToken());

        UserModel user = this.session.users().getUserByFederatedIdentity(federatedIdentityModel, realm);

        if (user == null) {

            logger.debugf("Federated user not found for provider '%s' and broker username '%s'.", providerId, context.getUsername());

            String username = context.getModelUsername();
            if (username == null) {
                if (this.realm.isRegistrationEmailAsUsername() && !Validation.isBlank(context.getEmail())) {
                    username = context.getEmail();
                } else if (context.getUsername() == null) {
                    username = context.getIdpConfig().getAlias() + "." + context.getId();
                } else {
                    username = context.getUsername();
                }
            }
            username = username.trim();
            context.setModelUsername(username);
            if (context.getEmail() != null && !realm.isDuplicateEmailsAllowed()) {
                UserModel existingUser = session.users().getUserByEmail(context.getEmail(), realm);
                if (existingUser != null) {
                    event.error(Errors.FEDERATED_IDENTITY_EXISTS);
                    throw new CorsErrorResponseException(cors, Errors.INVALID_TOKEN, "User already exists", Response.Status.BAD_REQUEST);
                }
            }

            UserModel existingUser = session.users().getUserByUsername(username, realm);
            if (existingUser != null) {
                event.error(Errors.FEDERATED_IDENTITY_EXISTS);
                throw new CorsErrorResponseException(cors, Errors.INVALID_TOKEN, "User already exists", Response.Status.BAD_REQUEST);
            }


            user = session.users().addUser(realm, username);
            user.setEnabled(true);
            user.setEmail(context.getEmail());
            user.setFirstName(context.getFirstName());
            user.setLastName(context.getLastName());


            federatedIdentityModel = new FederatedIdentityModel(context.getIdpConfig().getAlias(), context.getId(),
                    context.getUsername(), context.getToken());
            session.users().addFederatedIdentity(realm, user, federatedIdentityModel);

            context.getIdp().importNewUser(session, realm, user, context);
            if (mappers != null) {
                KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();
                for (IdentityProviderMapperModel mapper : mappers) {
                    IdentityProviderMapper target = (IdentityProviderMapper)sessionFactory.getProviderFactory(IdentityProviderMapper.class, mapper.getIdentityProviderMapper());
                    target.importNewUser(session, realm, user, mapper, context);
                }
            }

            if (context.getIdpConfig().isTrustEmail() && !Validation.isBlank(user.getEmail())) {
                logger.debugf("Email verified automatically after registration of user '%s' through Identity provider '%s' ", user.getUsername(), context.getIdpConfig().getAlias());
                user.setEmailVerified(true);
            }
        } else {
            if (!user.isEnabled()) {
                event.error(Errors.USER_DISABLED);
                throw new CorsErrorResponseException(cors, Errors.INVALID_TOKEN, "Invalid Token", Response.Status.BAD_REQUEST);
            }
            if (realm.isBruteForceProtected()) {
                if (session.getProvider(BruteForceProtector.class).isTemporarilyDisabled(session, realm, user)) {
                    event.error(Errors.USER_TEMPORARILY_DISABLED);
                    throw new CorsErrorResponseException(cors, Errors.INVALID_TOKEN, "Invalid Token", Response.Status.BAD_REQUEST);
                }
            }

            context.getIdp().updateBrokeredUser(session, realm, user, context);
            if (mappers != null) {
                KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();
                for (IdentityProviderMapperModel mapper : mappers) {
                    IdentityProviderMapper target = (IdentityProviderMapper)sessionFactory.getProviderFactory(IdentityProviderMapper.class, mapper.getIdentityProviderMapper());
                    target.updateBrokeredUser(session, realm, user, mapper, context);
                }
            }
        }
        return user;
    }

    public Response permissionGrant() {
        event.detail(Details.AUTH_METHOD, "oauth_credentials");

        String accessTokenString = null;
        String authorizationHeader = headers.getRequestHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authorizationHeader != null && authorizationHeader.toLowerCase().startsWith("bearer")) {
            accessTokenString = new AppAuthManager().extractAuthorizationHeaderToken(headers);
        }

        // we allow public clients to authenticate using a bearer token, where the token should be a valid access token.
        // public clients don't have secret and should be able to obtain a RPT by providing an access token previously issued by the server
        if (accessTokenString != null) {
            AccessToken accessToken = Tokens.getAccessToken(session);

            if (accessToken == null) {
                throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_GRANT, "Invalid bearer token", Status.UNAUTHORIZED);
            }

            ClientModel client = realm.getClientByClientId(accessToken.getIssuedFor());

            session.getContext().setClient(client);

            cors.allowedOrigins(session, client);
        }

        String claimToken = null;

        // claim_token is optional, if provided we just grab it from the request
        if (formParams.containsKey("claim_token")) {
            claimToken = formParams.get("claim_token").get(0);
        }

        String claimTokenFormat = formParams.getFirst("claim_token_format");

        if (claimToken != null && claimTokenFormat == null) {
            claimTokenFormat = AuthorizationTokenService.CLAIM_TOKEN_FORMAT_ID_TOKEN;
        }

        String subjectToken = formParams.getFirst("subject_token");

        if (accessTokenString == null) {
            // in case no bearer token is provided, we force client authentication
            checkClient();

            // if a claim token is provided, we check if the format is a OpenID Connect IDToken and assume the token represents the identity asking for permissions
            if (AuthorizationTokenService.CLAIM_TOKEN_FORMAT_ID_TOKEN.equalsIgnoreCase(claimTokenFormat)) {
                accessTokenString = claimToken;
            } else if (subjectToken != null) {
                accessTokenString = subjectToken;
            } else {
                // Clients need to authenticate in order to obtain a RPT from the server.
                // In order to support cases where the client is obtaining permissions on its on behalf, we issue a temporary access token
                accessTokenString = AccessTokenResponse.class.cast(clientCredentialsGrant().getEntity()).getToken();
            }
        }

        AuthorizationTokenService.KeycloakAuthorizationRequest authorizationRequest = new AuthorizationTokenService.KeycloakAuthorizationRequest(session.getProvider(AuthorizationProvider.class), tokenManager, event, this.request, cors);

        authorizationRequest.setTicket(formParams.getFirst("ticket"));
        authorizationRequest.setClaimToken(claimToken);
        authorizationRequest.setClaimTokenFormat(claimTokenFormat);
        authorizationRequest.setPct(formParams.getFirst("pct"));

        String rpt = formParams.getFirst("rpt");

        if (rpt != null) {
            AccessToken accessToken = session.tokens().decode(rpt, AccessToken.class);
            if (accessToken == null) {
                throw new CorsErrorResponseException(cors, "invalid_rpt", "RPT signature is invalid", Status.FORBIDDEN);
            }

            authorizationRequest.setRpt(accessToken);
        }

        authorizationRequest.setScope(formParams.getFirst("scope"));
        authorizationRequest.setAudience(formParams.getFirst("audience"));
        authorizationRequest.setSubjectToken(accessTokenString);

        String submitRequest = formParams.getFirst("submit_request");

        authorizationRequest.setSubmitRequest(submitRequest == null ? true : Boolean.valueOf(submitRequest));

        // permissions have a format like RESOURCE#SCOPE1,SCOPE2
        List<String> permissions = formParams.get("permission");

        if (permissions != null) {
            for (String permission : permissions) {
                String[] parts = permission.split("#");
                String resource = parts[0];

                if (parts.length == 1) {
                    authorizationRequest.addPermission(resource);
                } else {
                    String[] scopes = parts[1].split(",");
                    authorizationRequest.addPermission(parts[0], scopes);
                }
            }
        }

        Metadata metadata = new Metadata();

        String responseIncludeResourceName = formParams.getFirst("response_include_resource_name");

        if (responseIncludeResourceName != null) {
            metadata.setIncludeResourceName(Boolean.parseBoolean(responseIncludeResourceName));
        }

        String responsePermissionsLimit = formParams.getFirst("response_permissions_limit");

        if (responsePermissionsLimit != null) {
            metadata.setLimit(Integer.parseInt(responsePermissionsLimit));
        }

        metadata.setResponseMode(formParams.getFirst("response_mode"));

        authorizationRequest.setMetadata(metadata);

        return AuthorizationTokenService.instance().authorize(authorizationRequest);
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
        return m.matches();
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
