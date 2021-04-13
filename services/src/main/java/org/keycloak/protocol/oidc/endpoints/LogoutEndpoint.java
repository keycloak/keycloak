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
import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.TokenVerifier;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.VerificationException;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.headers.SecurityHeadersProvider;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.BackchannelLogoutResponse;
import org.keycloak.protocol.oidc.LogoutTokenValidationCode;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.protocol.oidc.utils.AuthorizeClientUtil;
import org.keycloak.protocol.oidc.utils.RedirectUtils;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.LogoutToken;
import org.keycloak.representations.RefreshToken;
import org.keycloak.services.CorsErrorResponseException;
import org.keycloak.services.ErrorPage;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.context.LogoutRequestContext;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.UserSessionManager;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.resources.Cors;
import org.keycloak.services.util.MtlsHoKTokenUtil;
import org.keycloak.util.TokenUtil;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.keycloak.models.UserSessionModel.State.LOGGED_OUT;
import static org.keycloak.models.UserSessionModel.State.LOGGING_OUT;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class LogoutEndpoint {
    private static final Logger logger = Logger.getLogger(LogoutEndpoint.class);

    @Context
    private KeycloakSession session;

    @Context
    private ClientConnection clientConnection;

    @Context
    private HttpRequest request;

    @Context
    private HttpHeaders headers;

    private TokenManager tokenManager;
    private RealmModel realm;
    private EventBuilder event;

    private Cors cors;

    public LogoutEndpoint(TokenManager tokenManager, RealmModel realm, EventBuilder event) {
        this.tokenManager = tokenManager;
        this.realm = realm;
        this.event = event;
    }

    @Path("/")
    @OPTIONS
    public Response issueUserInfoPreflight() {
        return Cors.add(this.request, Response.ok()).auth().preflight().build();
    }

    /**
     * Logout user session.  User must be logged in via a session cookie.
     *
     * When the logout is initiated by a remote idp, the parameter "initiating_idp" can be supplied. This param will
     * prevent upstream logout (since the logout procedure has already been started in the remote idp).
     *
     * @param redirectUri
     * @param initiatingIdp The alias of the idp initiating the logout.
     * @return
     */
    @GET
    @NoCache
    public Response logout(@QueryParam(OIDCLoginProtocol.REDIRECT_URI_PARAM) String redirectUri, // deprecated
                           @QueryParam("id_token_hint") String encodedIdToken,
                           @QueryParam("post_logout_redirect_uri") String postLogoutRedirectUri,
                           @QueryParam("state") String state,
                           @QueryParam("initiating_idp") String initiatingIdp) {
        String redirect = postLogoutRedirectUri != null ? postLogoutRedirectUri : redirectUri;
        IDToken idToken = null;
        if (encodedIdToken != null) {
            try {
                idToken = tokenManager.verifyIDTokenSignature(session, encodedIdToken);
                TokenVerifier.createWithoutSignature(idToken).tokenType(TokenUtil.TOKEN_TYPE_ID).verify();
            } catch (OAuthErrorException | VerificationException e) {
                event.event(EventType.LOGOUT);
                event.error(Errors.INVALID_TOKEN);
                return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.SESSION_NOT_ACTIVE);
            }
        }

        if (redirect != null) {
            String validatedUri;
            ClientModel client = (idToken == null || idToken.getIssuedFor() == null) ? null : realm.getClientById(idToken.getIssuedFor());
            if (client != null) {
                validatedUri = RedirectUtils.verifyRedirectUri(session, redirect, client);
            } else {
                validatedUri = RedirectUtils.verifyRealmRedirectUri(session, redirect);
            }
            if (validatedUri == null) {
                event.event(EventType.LOGOUT);
                event.detail(Details.REDIRECT_URI, redirect);
                event.error(Errors.INVALID_REDIRECT_URI);
                return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.INVALID_REDIRECT_URI);
            }
            redirect = validatedUri;
        }

        UserSessionModel userSession = null;
        if (idToken != null) {
            try {
                userSession = session.sessions().getUserSession(realm, idToken.getSessionState());

                if (userSession != null) {
                    checkTokenIssuedAt(idToken, userSession);
                }
            } catch (OAuthErrorException e) {
                event.event(EventType.LOGOUT);
                event.error(Errors.INVALID_TOKEN);
                return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.SESSION_NOT_ACTIVE);
            }
        }

        // authenticate identity cookie, but ignore an access token timeout as we're logging out anyways.
        AuthenticationManager.AuthResult authResult = AuthenticationManager.authenticateIdentityCookie(session, realm, false);
        if (authResult != null) {
            userSession = userSession != null ? userSession : authResult.getSession();
            return initiateBrowserLogout(userSession, redirect, state, initiatingIdp);
        }
        else if (userSession != null) {
            // identity cookie is missing but there's valid id_token_hint which matches session cookie => continue with browser logout
            if (idToken != null && idToken.getSessionState().equals(AuthenticationManager.getSessionIdFromSessionCookie(session))) {
                return initiateBrowserLogout(userSession, redirect, state, initiatingIdp);
            }
            // check if the user session is not logging out or already logged out
            // this might happen when a backChannelLogout is already initiated from AuthenticationManager.authenticateIdentityCookie
            if (userSession.getState() != LOGGING_OUT && userSession.getState() != LOGGED_OUT) {
                // non browser logout
                event.event(EventType.LOGOUT);
                AuthenticationManager.backchannelLogout(session, realm, userSession, session.getContext().getUri(), clientConnection, headers, true);
                event.user(userSession.getUser()).session(userSession).success();
            }
        }

        if (redirect != null) {
            UriBuilder uriBuilder = UriBuilder.fromUri(redirect);
            if (state != null) uriBuilder.queryParam(OIDCLoginProtocol.STATE_PARAM, state);
            return Response.status(302).location(uriBuilder.build()).build();
        } else {
            // TODO Empty content with ok makes no sense. Should it display a page? Or use noContent?
            session.getProvider(SecurityHeadersProvider.class).options().allowEmptyContentType();
            return Response.ok().build();
        }
    }

    /**
     * Logout a session via a non-browser invocation.  Similar signature to refresh token except there is no grant_type.
     * You must pass in the refresh token and
     * authenticate the client if it is not public.
     *
     * If the client is a confidential client
     * you must include the client-id and secret in an Basic Auth Authorization header.
     *
     * If the client is a public client, then you must include a "client_id" form parameter.
     *
     * returns 204 if successful, 400 if not with a json error response.
     *
     * @return
     */
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response logoutToken() {
        cors = Cors.add(request).auth().allowedMethods("POST").auth().exposedHeaders(Cors.ACCESS_CONTROL_ALLOW_METHODS);

        MultivaluedMap<String, String> form = request.getDecodedFormParameters();
        checkSsl();

        event.event(EventType.LOGOUT);

        ClientModel client = authorizeClient();
        String refreshToken = form.getFirst(OAuth2Constants.REFRESH_TOKEN);
        if (refreshToken == null) {
            event.error(Errors.INVALID_TOKEN);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST, "No refresh token", Response.Status.BAD_REQUEST);
        }

        try {
            session.clientPolicy().triggerOnEvent(new LogoutRequestContext(form));
        } catch (ClientPolicyException cpe) {
            throw new CorsErrorResponseException(cors, cpe.getError(), cpe.getErrorDetail(), cpe.getErrorStatus());
        }

        RefreshToken token = null;
        try {
            // KEYCLOAK-6771 Certificate Bound Token
            token = tokenManager.verifyRefreshToken(session, realm, client, request, refreshToken, false);

            boolean offline = TokenUtil.TOKEN_TYPE_OFFLINE.equals(token.getType());

            UserSessionModel userSessionModel;
            if (offline) {
                UserSessionManager sessionManager = new UserSessionManager(session);
                userSessionModel = sessionManager.findOfflineUserSession(realm, token.getSessionState());
            } else {
                userSessionModel = session.sessions().getUserSession(realm, token.getSessionState());
            }

            if (userSessionModel != null) {
                checkTokenIssuedAt(token, userSessionModel);
                logout(userSessionModel, offline);
            }
        } catch (OAuthErrorException e) {
            // KEYCLOAK-6771 Certificate Bound Token
            if (MtlsHoKTokenUtil.CERT_VERIFY_ERROR_DESC.equals(e.getDescription())) {
                event.error(Errors.NOT_ALLOWED);
                throw new CorsErrorResponseException(cors, e.getError(), e.getDescription(), Response.Status.UNAUTHORIZED);
            } else {
                event.error(Errors.INVALID_TOKEN);
                throw new CorsErrorResponseException(cors, e.getError(), e.getDescription(), Response.Status.BAD_REQUEST);
            }
        }

        return cors.builder(Response.noContent()).build();
    }

    /**
     * Backchannel logout endpoint implementation for Keycloak, which tries to logout the user from all sessions via
     * POST with a valid LogoutToken.
     *
     * Logout a session via a non-browser invocation. Will be implemented as a backchannel logout based on the
     * specification
     * https://openid.net/specs/openid-connect-backchannel-1_0.html
     *
     * @return
     */
    @Path("/backchannel-logout")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response backchannelLogout() {
        MultivaluedMap<String, String> form = request.getDecodedFormParameters();
        event.event(EventType.LOGOUT);

        String encodedLogoutToken = form.getFirst(OAuth2Constants.LOGOUT_TOKEN);
        if (encodedLogoutToken == null) {
            event.error(Errors.INVALID_TOKEN);
            throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "No logout token",
                    Response.Status.BAD_REQUEST);
        }

        LogoutTokenValidationCode validationCode = tokenManager.verifyLogoutToken(session, realm, encodedLogoutToken);
        if (!validationCode.equals(LogoutTokenValidationCode.VALIDATION_SUCCESS)) {
            event.error(Errors.INVALID_TOKEN);
            throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, validationCode.getErrorMessage(),
                    Response.Status.BAD_REQUEST);
        }

        LogoutToken logoutToken = tokenManager.toLogoutToken(encodedLogoutToken).get();

        Stream<String> identityProviderAliases = tokenManager.getValidOIDCIdentityProvidersForBackchannelLogout(realm,
                session, encodedLogoutToken, logoutToken)
                .map(idp -> idp.getConfig().getAlias());

        boolean logoutOfflineSessions = Boolean.parseBoolean(logoutToken.getEvents()
                .getOrDefault(TokenUtil.TOKEN_BACKCHANNEL_LOGOUT_EVENT_REVOKE_OFFLINE_TOKENS, false).toString());

        BackchannelLogoutResponse backchannelLogoutResponse;

        if (logoutToken.getSid() != null) {
            backchannelLogoutResponse = backchannelLogoutWithSessionId(logoutToken.getSid(), identityProviderAliases,
                    logoutOfflineSessions);
        } else {
            backchannelLogoutResponse = backchannelLogoutFederatedUserId(logoutToken.getSubject(),
                    identityProviderAliases, logoutOfflineSessions);
        }

        if (!backchannelLogoutResponse.getLocalLogoutSucceeded()) {
            event.error(Errors.LOGOUT_FAILED);
            throw new ErrorResponseException(OAuthErrorException.SERVER_ERROR,
                    "There was an error in the local logout",
                    Response.Status.NOT_IMPLEMENTED);
        }

        session.getProvider(SecurityHeadersProvider.class).options().allowEmptyContentType();

        if (oneOrMoreDownstreamLogoutsFailed(backchannelLogoutResponse)) {
            return Cors.add(request)
                    .auth()
                    .builder(Response.status(Response.Status.GATEWAY_TIMEOUT)
                            .type(MediaType.APPLICATION_JSON_TYPE))
                    .build();
        }

        return Cors.add(request)
                .auth()
                .builder(Response.ok()
                        .type(MediaType.APPLICATION_JSON_TYPE))
                .build();
    }

    private BackchannelLogoutResponse backchannelLogoutWithSessionId(String sessionId,
            Stream<String> identityProviderAliases, boolean logoutOfflineSessions) {
        AtomicReference<BackchannelLogoutResponse> backchannelLogoutResponse = new AtomicReference<>(new BackchannelLogoutResponse());
        backchannelLogoutResponse.get().setLocalLogoutSucceeded(true);
        identityProviderAliases.forEach(identityProviderAlias -> {
            UserSessionModel userSession = session.sessions().getUserSessionByBrokerSessionId(realm,
                    identityProviderAlias + "." + sessionId);

            if (logoutOfflineSessions) {
                logoutOfflineUserSession(identityProviderAlias + "." + sessionId);
            }

            if (userSession != null) {
                backchannelLogoutResponse.set(logoutUserSession(userSession));
            }
        });

        return backchannelLogoutResponse.get();
    }

    private void logoutOfflineUserSession(String brokerSessionId) {
        UserSessionModel offlineUserSession =
                session.sessions().getOfflineUserSessionByBrokerSessionId(realm, brokerSessionId);
        if (offlineUserSession != null) {
            new UserSessionManager(session).revokeOfflineUserSession(offlineUserSession);
        }
    }

    private BackchannelLogoutResponse backchannelLogoutFederatedUserId(String federatedUserId,
                                                                       Stream<String> identityProviderAliases,
                                                                       boolean logoutOfflineSessions) {
        BackchannelLogoutResponse backchannelLogoutResponse = new BackchannelLogoutResponse();
        backchannelLogoutResponse.setLocalLogoutSucceeded(true);
        identityProviderAliases.forEach(identityProviderAlias -> {

            if (logoutOfflineSessions) {
                logoutOfflineUserSessions(identityProviderAlias + "." + federatedUserId);
            }

            session.sessions().getUserSessionByBrokerUserIdStream(realm, identityProviderAlias + "." + federatedUserId)
                    .collect(Collectors.toList()) // collect to avoid concurrent modification as backchannelLogout removes the user sessions.
                    .forEach(userSession -> {
                        BackchannelLogoutResponse userBackchannelLogoutResponse = this.logoutUserSession(userSession);
                        backchannelLogoutResponse.setLocalLogoutSucceeded(backchannelLogoutResponse.getLocalLogoutSucceeded()
                                && userBackchannelLogoutResponse.getLocalLogoutSucceeded());
                        userBackchannelLogoutResponse.getClientResponses()
                                .forEach(backchannelLogoutResponse::addClientResponses);
                    });
        });

        return backchannelLogoutResponse;
    }

    private void logoutOfflineUserSessions(String brokerUserId) {
        UserSessionManager userSessionManager = new UserSessionManager(session);
        session.sessions().getOfflineUserSessionByBrokerUserIdStream(realm, brokerUserId).collect(Collectors.toList())
                .forEach(userSessionManager::revokeOfflineUserSession);
    }

    private BackchannelLogoutResponse logoutUserSession(UserSessionModel userSession) {
        BackchannelLogoutResponse backchannelLogoutResponse = AuthenticationManager.backchannelLogout(session, realm,
                userSession, session.getContext().getUri(), clientConnection, headers, false);

        if (backchannelLogoutResponse.getLocalLogoutSucceeded()) {
            event.user(userSession.getUser())
                    .session(userSession)
                    .success();
        }

        return backchannelLogoutResponse;
    }
    
    private boolean oneOrMoreDownstreamLogoutsFailed(BackchannelLogoutResponse backchannelLogoutResponse) {
        BackchannelLogoutResponse filteredBackchannelLogoutResponse = new BackchannelLogoutResponse();
        for (BackchannelLogoutResponse.DownStreamBackchannelLogoutResponse response : backchannelLogoutResponse
                .getClientResponses()) {
            if (response.isWithBackchannelLogoutUrl()) {
                filteredBackchannelLogoutResponse.addClientResponses(response);
            }
        }

        return backchannelLogoutResponse.getClientResponses().stream()
                .filter(BackchannelLogoutResponse.DownStreamBackchannelLogoutResponse::isWithBackchannelLogoutUrl)
                .anyMatch(clientResponse -> !(clientResponse.getResponseCode().isPresent() &&
                        (clientResponse.getResponseCode().get() == Response.Status.OK.getStatusCode() ||
                                clientResponse.getResponseCode().get() == Response.Status.NO_CONTENT.getStatusCode())));
    }

    private void logout(UserSessionModel userSession, boolean offline) {
        AuthenticationManager.backchannelLogout(session, realm, userSession, session.getContext().getUri(), clientConnection, headers, true, offline);
        event.user(userSession.getUser()).session(userSession).success();
    }

    private ClientModel authorizeClient() {
        ClientModel client = AuthorizeClientUtil.authorizeClient(session, event, cors).getClient();
        cors.allowedOrigins(session, client);

        if (client.isBearerOnly()) {
            throw new CorsErrorResponseException(cors, Errors.INVALID_CLIENT, "Bearer-only not allowed", Response.Status.BAD_REQUEST);
        }

        return client;
    }

    private void checkSsl() {
        if (!session.getContext().getUri().getBaseUri().getScheme().equals("https") && realm.getSslRequired().isRequired(clientConnection)) {
            throw new CorsErrorResponseException(cors.allowAllOrigins(), "invalid_request", "HTTPS required", Response.Status.FORBIDDEN);
        }
    }

    private void checkTokenIssuedAt(IDToken token, UserSessionModel userSession) throws OAuthErrorException {
        if (token.getIssuedAt() + 1 < userSession.getStarted()) {
            throw new OAuthErrorException(OAuthErrorException.INVALID_GRANT, "Refresh toked issued before the user session started");
        }
    }

    private Response initiateBrowserLogout(UserSessionModel userSession, String redirect, String state, String initiatingIdp ) {
        if (redirect != null) userSession.setNote(OIDCLoginProtocol.LOGOUT_REDIRECT_URI, redirect);
        if (state != null) userSession.setNote(OIDCLoginProtocol.LOGOUT_STATE_PARAM, state);
        userSession.setNote(AuthenticationManager.KEYCLOAK_LOGOUT_PROTOCOL, OIDCLoginProtocol.LOGIN_PROTOCOL);
        logger.debug("Initiating OIDC browser logout");
        Response response =  AuthenticationManager.browserLogout(session, realm, userSession, session.getContext().getUri(), clientConnection, headers, initiatingIdp);
        logger.debug("finishing OIDC browser logout");
        return response;
    }
}
