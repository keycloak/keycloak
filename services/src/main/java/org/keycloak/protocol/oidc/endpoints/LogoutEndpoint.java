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

import static org.keycloak.models.UserSessionModel.State.LOGGED_OUT;
import static org.keycloak.models.UserSessionModel.State.LOGGING_OUT;
import static org.keycloak.services.resources.LoginActionsService.SESSION_CODE;
import static org.keycloak.utils.LockObjectsForModification.lockObjectsForModification;

import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.Config;
import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.TokenVerifier;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.VerificationException;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.headers.SecurityHeadersProvider;
import org.keycloak.locale.LocaleSelectorProvider;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.utils.SystemClientUtil;
import org.keycloak.protocol.oidc.BackchannelLogoutResponse;
import org.keycloak.protocol.oidc.LogoutTokenValidationCode;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.OIDCLoginProtocolFactory;
import org.keycloak.protocol.oidc.OIDCProviderConfig;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.protocol.oidc.utils.AuthorizeClientUtil;
import org.keycloak.protocol.oidc.utils.LogoutUtil;
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
import org.keycloak.services.managers.AuthenticationSessionManager;
import org.keycloak.services.managers.ClientSessionCode;
import org.keycloak.services.managers.UserSessionManager;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.resources.Cors;
import org.keycloak.services.resources.LogoutSessionCodeChecks;
import org.keycloak.services.resources.SessionCodeChecks;
import org.keycloak.services.util.LocaleUtil;
import org.keycloak.services.util.MtlsHoKTokenUtil;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.sessions.RootAuthenticationSessionModel;
import org.keycloak.util.TokenUtil;

import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    private final TokenManager tokenManager;
    private final RealmModel realm;
    private final EventBuilder event;
    private final OIDCProviderConfig providerConfig;

    // When enabled we cannot search offline sessions by brokerSessionId. We need to search by federated userId and then filter by brokerSessionId.
    private final boolean offlineSessionsLazyLoadingEnabled;

    private Cors cors;

    public LogoutEndpoint(TokenManager tokenManager, RealmModel realm, EventBuilder event, OIDCProviderConfig providerConfig) {
        this.tokenManager = tokenManager;
        this.realm = realm;
        this.event = event;
        this.providerConfig = providerConfig;
        this.offlineSessionsLazyLoadingEnabled = !Config.scope("userSessions").scope("infinispan").getBoolean("preloadOfflineSessionsFromDatabase", false);
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
     * This endpoint is aligned with OpenID Connect RP-Initiated Logout specification https://openid.net/specs/openid-connect-rpinitiated-1_0.html#RPLogout
     *
     * All parameters are optional. Some combinations of parameters are invalid as described in the specification
     *
     * @param deprecatedRedirectUri Parameter "redirect_uri" is not supported by the specification. It is here just for the backwards compatibility
     * @param encodedIdToken Parameter "id_token_hint" as described in the specification.
     * @param clientId Parameter "client_id" as described in the specification.
     * @param postLogoutRedirectUri Parameter "post_logout_redirect_uri" as described in the specification with the URL to redirect after logout.
     * @param state Parameter "state" as described in the specification. Will be used to send "state" when redirecting back to the application after the logout
     * @param uiLocales Parameter "ui_locales" as described in the specification. Can be used by the client to display pages in specified locale (if any pages are going to be displayed to the user during logout)
     * @param initiatingIdp The alias of the idp initiating the logout.
     * @return
     */
    @GET
    @NoCache
    public Response logout(@QueryParam(OIDCLoginProtocol.REDIRECT_URI_PARAM) String deprecatedRedirectUri, // deprecated
                           @QueryParam(OIDCLoginProtocol.ID_TOKEN_HINT) String encodedIdToken,
                           @QueryParam(OIDCLoginProtocol.CLIENT_ID_PARAM) String clientId,
                           @QueryParam(OIDCLoginProtocol.POST_LOGOUT_REDIRECT_URI_PARAM) String postLogoutRedirectUri,
                           @QueryParam(OIDCLoginProtocol.STATE_PARAM) String state,
                           @QueryParam(OIDCLoginProtocol.UI_LOCALES_PARAM) String uiLocales,
                           @QueryParam(AuthenticationManager.INITIATING_IDP_PARAM) String initiatingIdp) {

        if (!providerConfig.isLegacyLogoutRedirectUri()) {
            if (deprecatedRedirectUri != null) {
                event.event(EventType.LOGOUT);
                event.error(Errors.INVALID_REQUEST);
                logger.warnf("Parameter 'redirect_uri' no longer supported. Please use 'post_logout_redirect_uri' with 'id_token_hint' for this endpoint. Alternatively you can enable backwards compatibility option '%s' of oidc login protocol in the server configuration.",
                        OIDCLoginProtocolFactory.CONFIG_LEGACY_LOGOUT_REDIRECT_URI);
                return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.INVALID_PARAMETER, OIDCLoginProtocol.REDIRECT_URI_PARAM);
            }

            if (postLogoutRedirectUri != null && encodedIdToken == null && clientId == null) {
                event.event(EventType.LOGOUT);
                event.error(Errors.INVALID_REQUEST);
                logger.warnf(
                        "Either the parameter 'client_id' or the parameter 'id_token_hint' is required when 'post_logout_redirect_uri' is used.");
                return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.MISSING_PARAMETER,
                        OIDCLoginProtocol.ID_TOKEN_HINT);
            }
        }

        deprecatedRedirectUri = providerConfig.isLegacyLogoutRedirectUri() ? deprecatedRedirectUri : null;
        final String redirectUri = postLogoutRedirectUri != null ? postLogoutRedirectUri : deprecatedRedirectUri;

        boolean confirmationNeeded = true;
        boolean forcedConfirmation = false;
        ClientModel client = clientId == null ? null : realm.getClientByClientId(clientId);
        if (clientId != null && client == null) {
            logger.warnf("Client '%s' not found.", clientId);
            forcedConfirmation = true;
        }

        IDToken idToken = null;
        if (encodedIdToken != null) {
            try {
                idToken = tokenManager.verifyIDTokenSignature(session, encodedIdToken);
                TokenVerifier.createWithoutSignature(idToken).tokenType(TokenUtil.TOKEN_TYPE_ID).verify();
            } catch (OAuthErrorException | VerificationException e) {
                event.event(EventType.LOGOUT);
                event.error(Errors.INVALID_TOKEN);
                return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.INVALID_PARAMETER, OIDCLoginProtocol.ID_TOKEN_HINT);
            }
        }

        if (clientId == null) {
            // Retrieve client from id_token_hint
            client = (idToken == null || idToken.getIssuedFor() == null) ? null : realm.getClientByClientId(idToken.getIssuedFor());
            if (client != null) {
                confirmationNeeded = false;
            }
        } else {
            // Check client_id and id_token_hint point to the same client
            if (idToken != null && idToken.getIssuedFor() != null) {
                if (!idToken.getIssuedFor().equals(clientId)) {
                    event.event(EventType.LOGOUT);
                    event.client(clientId);
                    event.error(Errors.INVALID_TOKEN);
                    logger.warnf("Parameter client_id is different than the client for which ID Token was issued. Parameter client_id: '%s', ID Token issued for: '%s'.", clientId, idToken.getIssuedFor());
                    return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.INVALID_PARAMETER, OIDCLoginProtocol.ID_TOKEN_HINT);
                } else {
                    confirmationNeeded = false;
                }
            }
        }
        if (client != null) {
            session.getContext().setClient(client);
        }

        String validatedRedirectUri = null;
        if (redirectUri != null) {
            if (client != null) {
                validatedRedirectUri = RedirectUtils.verifyRedirectUri(session, redirectUri, client);
            } else if (clientId == null) {
                /*
                 * Only call verifyRealmRedirectUri, in case both clientId and client are null - otherwise
                 * the logout uri contains a non-existing client, and we should show an INVALID_REDIRECT_URI error
                 */
                validatedRedirectUri = RedirectUtils.verifyRealmRedirectUri(session, redirectUri);
            }

            if (validatedRedirectUri == null) {
                event.event(EventType.LOGOUT);
                event.detail(Details.REDIRECT_URI, redirectUri);
                event.error(Errors.INVALID_REDIRECT_URI);
                return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.INVALID_REDIRECT_URI);
            }
        }

        AuthenticationSessionModel logoutSession = AuthenticationManager.createOrJoinLogoutSession(session, realm, new AuthenticationSessionManager(session), null, true);
        session.getContext().setAuthenticationSession(logoutSession);
        if (uiLocales != null) {
            logoutSession.setAuthNote(LocaleSelectorProvider.CLIENT_REQUEST_LOCALE, uiLocales);
        }
        if (validatedRedirectUri != null) {
            logoutSession.setAuthNote(OIDCLoginProtocol.LOGOUT_REDIRECT_URI, validatedRedirectUri);
        }
        if (state != null) {
            logoutSession.setAuthNote(OIDCLoginProtocol.LOGOUT_STATE_PARAM, state);
        }
        if (initiatingIdp != null) {
            logoutSession.setAuthNote(AuthenticationManager.LOGOUT_INITIATING_IDP, initiatingIdp);
        }
        if (idToken != null) {
            logoutSession.setAuthNote(OIDCLoginProtocol.LOGOUT_VALIDATED_ID_TOKEN_SESSION_STATE, idToken.getSessionState());
            logoutSession.setAuthNote(OIDCLoginProtocol.LOGOUT_VALIDATED_ID_TOKEN_ISSUED_AT, String.valueOf(idToken.getIat()));
        }

        LoginFormsProvider loginForm = session.getProvider(LoginFormsProvider.class)
                .setAuthenticationSession(logoutSession);

        UserSessionModel userSession = null;

        // Check if we have session in the browser. If yes and it is different session than referenced by id_token_hint, the confirmation should be displayed
        AuthenticationManager.AuthResult authResult = AuthenticationManager.authenticateIdentityCookie(session, realm, false);
        if (authResult != null) {
            userSession = authResult.getSession();
            if (idToken != null && idToken.getSessionState() != null && !idToken.getSessionState().equals(authResult.getSession().getId())) {
                forcedConfirmation = true;
            }
        } else {
            // Skip confirmation in case that valid redirect URI was setup for given client_id and there is no session in the browser as well as no id_token_hint.
            // We can do automatic redirect as there is no logout needed at all for this scenario (Session was probably already logged-out before)
            if (encodedIdToken == null && client != null && validatedRedirectUri != null) {
                confirmationNeeded = false;
            }
        }

        if (userSession == null && idToken != null && idToken.getSessionState() != null) {
            userSession = session.sessions().getUserSession(realm, idToken.getSessionState());
        }

        // Try to figure user because of localization
        if (userSession != null) {
            UserModel user = userSession.getUser();
            logoutSession.setAuthenticatedUser(user);
            loginForm.setUser(user);
        }

        // Logout confirmation screen will be displayed to the user in this case
        if (confirmationNeeded || forcedConfirmation) {
            return displayLogoutConfirmationScreen(loginForm, logoutSession);
        } else {
            return doBrowserLogout(logoutSession);
        }
    }

    private Response displayLogoutConfirmationScreen(LoginFormsProvider loginForm, AuthenticationSessionModel authSession) {
        ClientSessionCode<AuthenticationSessionModel> accessCode = new ClientSessionCode<>(session, realm, authSession);
        accessCode.setAction(AuthenticatedClientSessionModel.Action.LOGGING_OUT.name());

        return loginForm
                .setClientSessionCode(accessCode.getOrGenerateCode())
                .createLogoutConfirmPage();
    }

    /**
     * This endpoint can be used either as:
     *  - OpenID Connect RP-Initiated Logout POST endpoint according to the specification https://openid.net/specs/openid-connect-rpinitiated-1_0.html#RPLogout
     *  - Legacy Logout endpoint with refresh_token as an argument and client authentication needed. See {@link #logoutToken} for more details
     *
     * @return response
     */
    @POST
    @NoCache
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response logout() {
        MultivaluedMap<String, String> form = request.getDecodedFormParameters();
        if (form.containsKey(OAuth2Constants.REFRESH_TOKEN)) {
            return logoutToken();
        } else {
            return logout(form.getFirst(OIDCLoginProtocol.REDIRECT_URI_PARAM),
                    form.getFirst(OIDCLoginProtocol.ID_TOKEN_HINT),
                    form.getFirst(OIDCLoginProtocol.CLIENT_ID_PARAM),
                    form.getFirst(OIDCLoginProtocol.POST_LOGOUT_REDIRECT_URI_PARAM),
                    form.getFirst(OIDCLoginProtocol.STATE_PARAM),
                    form.getFirst(OIDCLoginProtocol.UI_LOCALES_PARAM),
                    form.getFirst(AuthenticationManager.INITIATING_IDP_PARAM));
        }
    }

    @Path("/logout-confirm")
    @POST
    @NoCache
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response logoutConfirmAction() {
        MultivaluedMap<String, String> formData = request.getDecodedFormParameters();
        event.event(EventType.LOGOUT);
        String code = formData.getFirst(SESSION_CODE);
        String clientId = session.getContext().getUri().getQueryParameters().getFirst(Constants.CLIENT_ID);
        String tabId = session.getContext().getUri().getQueryParameters().getFirst(Constants.TAB_ID);

        logger.tracef("Logout confirmed. sessionCode=%s, clientId=%s, tabId=%s", code, clientId, tabId);

        SessionCodeChecks checks = new LogoutSessionCodeChecks(realm, session.getContext().getUri(), request, clientConnection, session, event, code, clientId, tabId);
        checks.initialVerify();
        if (!checks.verifyActiveAndValidAction(AuthenticationSessionModel.Action.LOGGING_OUT.name(), ClientSessionCode.ActionType.USER) || !checks.isActionRequest() || !formData.containsKey("confirmLogout")) {
            AuthenticationSessionModel logoutSession = checks.getAuthenticationSession();
            logger.debugf("Failed verification during logout. logoutSessionId=%s, clientId=%s, tabId=%s",
                    logoutSession != null ? logoutSession.getParentSession().getId() : "unknown", clientId, tabId);

            if (logoutSession == null || logoutSession.getClient().equals(SystemClientUtil.getSystemClient(logoutSession.getRealm()))) {
                // Cleanup system client URL to avoid links to account management
                session.getProvider(LoginFormsProvider.class).setAttribute(Constants.SKIP_LINK, true);
            }

            return ErrorPage.error(session, logoutSession, Response.Status.BAD_REQUEST, Messages.FAILED_LOGOUT);
        }

        AuthenticationSessionModel logoutSession = checks.getAuthenticationSession();
        logger.tracef("Logout code successfully verified. Logout Session is '%s'. Client ID is '%s'.", logoutSession.getParentSession().getId(),
                logoutSession.getClient().getClientId());
        return doBrowserLogout(logoutSession);
    }


    // Typically shown when user changes localization on the logout confirmation screen
    @Path("/logout-confirm")
    @NoCache
    @GET
    public Response logoutConfirmGet() {
        event.event(EventType.LOGOUT);

        String clientId = session.getContext().getUri().getQueryParameters().getFirst(Constants.CLIENT_ID);
        String tabId = session.getContext().getUri().getQueryParameters().getFirst(Constants.TAB_ID);

        logger.tracef("Changing localization by user during logout. clientId=%s, tabId=%s, kc_locale: %s", clientId, tabId, session.getContext().getUri().getQueryParameters().getFirst(LocaleSelectorProvider.KC_LOCALE_PARAM));

        SessionCodeChecks checks = new LogoutSessionCodeChecks(realm, session.getContext().getUri(), request, clientConnection, session, event, null, clientId, tabId);
        AuthenticationSessionModel logoutSession = checks.initialVerifyAuthSession();
        if (logoutSession == null) {
            logger.debugf("Failed verification when changing locale logout. clientId=%s, tabId=%s", clientId, tabId);

            LoginFormsProvider loginForm = session.getProvider(LoginFormsProvider.class);
            if (clientId == null || clientId.equals(SystemClientUtil.getSystemClient(realm).getClientId())) {
                // Cleanup system client URL to avoid links to account management
                loginForm.setAttribute(Constants.SKIP_LINK, true);
            }

            AuthenticationManager.AuthResult authResult = AuthenticationManager.authenticateIdentityCookie(session, realm, false);
            if (authResult != null) {
                return ErrorPage.error(session, logoutSession, Response.Status.BAD_REQUEST, Messages.FAILED_LOGOUT);
            } else {
                // Probably changing locale on logout screen after logout was already performed. If there is no session in the browser, we can just display that logout was already finished
                return loginForm.setSuccess(Messages.SUCCESS_LOGOUT).createInfoPage();
            }
        }

        LocaleUtil.processLocaleParam(session, realm, logoutSession);

        LoginFormsProvider loginForm = session.getProvider(LoginFormsProvider.class)
                .setAuthenticationSession(logoutSession)
                .setUser(logoutSession.getAuthenticatedUser());

        return displayLogoutConfirmationScreen(loginForm, logoutSession);
    }


    // Method triggered after user eventually confirmed that he wants to logout and all other checks were done
    private Response doBrowserLogout(AuthenticationSessionModel logoutSession) {
        UserSessionModel userSession = null;
        String userSessionIdFromIdToken = logoutSession.getAuthNote(OIDCLoginProtocol.LOGOUT_VALIDATED_ID_TOKEN_SESSION_STATE);
        String idTokenIssuedAtStr = logoutSession.getAuthNote(OIDCLoginProtocol.LOGOUT_VALIDATED_ID_TOKEN_ISSUED_AT);
        if (userSessionIdFromIdToken != null && idTokenIssuedAtStr != null) {
            try {
                userSession = lockObjectsForModification(session, () -> session.sessions().getUserSession(realm, userSessionIdFromIdToken));

                if (userSession != null) {
                    Integer idTokenIssuedAt = Integer.parseInt(idTokenIssuedAtStr);
                    checkTokenIssuedAt(idTokenIssuedAt, userSession);
                }
            } catch (OAuthErrorException e) {
                event.event(EventType.LOGOUT);
                event.error(Errors.INVALID_TOKEN);
                return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.SESSION_NOT_ACTIVE);
            }
        }

        // authenticate identity cookie, but ignore an access token timeout as we're logging out anyways.
        AuthenticationManager.AuthResult authResult = lockObjectsForModification(session, 
                () -> AuthenticationManager.authenticateIdentityCookie(session, realm, false));
        if (authResult != null) {
            userSession = userSession != null ? userSession : authResult.getSession();
            return initiateBrowserLogout(userSession);
        } else if (userSession != null) {
            // identity cookie is missing but there's valid id_token_hint which matches session cookie => continue with browser logout
            if (userSessionIdFromIdToken.equals(AuthenticationManager.getSessionIdFromSessionCookie(session))) {
                return initiateBrowserLogout(userSession);
            }
            // check if the user session is not logging out or already logged out
            // this might happen when a backChannelLogout is already initiated from AuthenticationManager.authenticateIdentityCookie
            if (userSession.getState() != LOGGING_OUT && userSession.getState() != LOGGED_OUT) {
                // non browser logout
                event.event(EventType.LOGOUT);
                AuthenticationManager.backchannelLogout(session, realm, userSession, session.getContext().getUri(), clientConnection, headers, true);

                String redirectUri = logoutSession.getAuthNote(OIDCLoginProtocol.LOGOUT_REDIRECT_URI);
                if (redirectUri != null) {
                    event.detail(Details.REDIRECT_URI, redirectUri);
                }
                event.user(userSession.getUser()).session(userSession).success();
            }
        }

        logger.tracef("Removing logout session '%s' used during logout.", logoutSession.getParentSession().getId());
        RootAuthenticationSessionModel rootAuthSession = logoutSession.getParentSession();
        rootAuthSession.removeAuthenticationSessionByTabId(logoutSession.getTabId());
        return LogoutUtil.sendResponseAfterLogoutFinished(session, logoutSession);
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
    private Response logoutToken() {
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
                String sessionState = token.getSessionState();
                userSessionModel = lockObjectsForModification(session, () -> session.sessions().getUserSession(realm, sessionState));
            }

            if (userSessionModel != null) {
                checkTokenIssuedAt(token.getIssuedAt(), userSessionModel);
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
                    logoutOfflineSessions, logoutToken.getSubject());
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
            Stream<String> identityProviderAliases, boolean logoutOfflineSessions, String federatedUserId) {
        AtomicReference<BackchannelLogoutResponse> backchannelLogoutResponse = new AtomicReference<>(new BackchannelLogoutResponse());
        backchannelLogoutResponse.get().setLocalLogoutSucceeded(true);
        identityProviderAliases.forEach(identityProviderAlias -> {
            UserSessionModel userSession = lockObjectsForModification(session, () -> session.sessions().getUserSessionByBrokerSessionId(realm,
                    identityProviderAlias + "." + sessionId));

            if (logoutOfflineSessions) {
                if (offlineSessionsLazyLoadingEnabled) {
                    logoutOfflineUserSessionByBrokerUserId(identityProviderAlias + "." + federatedUserId, identityProviderAlias + "." + sessionId);
                } else {
                    logoutOfflineUserSession(identityProviderAlias + "." + sessionId);
                }
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

    private void logoutOfflineUserSessionByBrokerUserId(String brokerUserId, String brokerSessionId) {
        UserSessionManager userSessionManager = new UserSessionManager(session);
        if (brokerUserId != null && brokerSessionId != null) {
            session.sessions().getOfflineUserSessionByBrokerUserIdStream(realm, brokerUserId)
                    .filter(userSession -> brokerSessionId.equals(userSession.getBrokerSessionId()))
                    .forEach(userSessionManager::revokeOfflineUserSession);
        }
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

    private void checkTokenIssuedAt(int idTokenIssuedAt, UserSessionModel userSession) throws OAuthErrorException {
        if (idTokenIssuedAt + 1 < userSession.getStarted()) {
            throw new OAuthErrorException(OAuthErrorException.INVALID_GRANT, "Toked issued before the user session started");
        }
    }

    private Response initiateBrowserLogout(UserSessionModel userSession) {
        userSession.setNote(AuthenticationManager.KEYCLOAK_LOGOUT_PROTOCOL, OIDCLoginProtocol.LOGIN_PROTOCOL);
        logger.tracef("Calling initiateBrowserLogout for user session '%s'", userSession.getId());
        Response response =  AuthenticationManager.browserLogout(session, realm, userSession, session.getContext().getUri(), clientConnection, headers);
        logger.tracef("Finished call of initiateBrowserLogout for user session '%s'", userSession.getId());
        return response;
    }
}
