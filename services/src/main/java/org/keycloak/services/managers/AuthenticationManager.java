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
package org.keycloak.services.managers;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;

import org.keycloak.OAuth2Constants;
import org.keycloak.Token;
import org.keycloak.TokenCategory;
import org.keycloak.TokenVerifier;
import org.keycloak.TokenVerifier.TokenTypeCheck;
import org.keycloak.authentication.AuthenticationFlowException;
import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.authentication.AuthenticatorUtil;
import org.keycloak.authentication.InitiatedActionSupport;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionContextResult;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.authentication.authenticators.browser.AbstractUsernameFormAuthenticator;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.broker.provider.UserAuthenticationIdentityProvider;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.Profile;
import org.keycloak.common.VerificationException;
import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.SecretGenerator;
import org.keycloak.common.util.Time;
import org.keycloak.cookie.CookieProvider;
import org.keycloak.cookie.CookieType;
import org.keycloak.crypto.SignatureProvider;
import org.keycloak.crypto.SignatureVerifierContext;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.http.HttpRequest;
import org.keycloak.jose.jws.crypto.HashUtils;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.Constants;
import org.keycloak.models.DefaultActionTokenKey;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredActionProviderModel;
import org.keycloak.models.SingleUseObjectKeyModel;
import org.keycloak.models.SingleUseObjectProvider;
import org.keycloak.models.UserConsentModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.utils.DefaultRequiredActions;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.SessionExpirationUtils;
import org.keycloak.models.utils.SessionTimeoutHelper;
import org.keycloak.models.utils.SystemClientUtil;
import org.keycloak.protocol.LoginProtocol;
import org.keycloak.protocol.LoginProtocol.Error;
import org.keycloak.protocol.oidc.BackchannelLogoutResponse;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.protocol.oidc.encode.AccessTokenContext;
import org.keycloak.protocol.oidc.encode.TokenContextEncoderProvider;
import org.keycloak.rar.AuthorizationDetails;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.Urls;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.context.LogoutRequestContext;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.resources.IdentityBrokerService;
import org.keycloak.services.resources.LoginActionsService;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.services.util.AuthorizationContextUtil;
import org.keycloak.services.util.DefaultClientSessionContext;
import org.keycloak.services.util.UserSessionUtil;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.sessions.CommonClientSessionModel;
import org.keycloak.sessions.CommonClientSessionModel.Action;
import org.keycloak.sessions.RootAuthenticationSessionModel;
import org.keycloak.util.TokenUtil;
import org.keycloak.utils.RoleResolveUtil;

import org.jboss.logging.Logger;

import static org.keycloak.models.UserSessionModel.CORRESPONDING_SESSION_ID;
import static org.keycloak.protocol.oidc.grants.device.DeviceGrantType.isOAuth2DeviceVerificationFlow;

/**
 * Stateless object that manages authentication
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class AuthenticationManager {
    public static final String SET_REDIRECT_URI_AFTER_REQUIRED_ACTIONS= "SET_REDIRECT_URI_AFTER_REQUIRED_ACTIONS";
    public static final String END_AFTER_REQUIRED_ACTIONS = "END_AFTER_REQUIRED_ACTIONS";
    public static final String INVALIDATE_ACTION_TOKEN = "INVALIDATE_ACTION_TOKEN";

    /**
     * Auth session note, which indicates if user session will be persistent (Saved to real persistent store) or
     * transient (transient session will be scoped to single request and hence there is no need to save it in the underlying store)
     */
    public static final String USER_SESSION_PERSISTENT_STATE = "USER_SESSION_PERSISTENT_STATE";

    /**
     * Auth session note on client logout state (when logging out)
     */
    public static final String CLIENT_LOGOUT_STATE = "logout.state.";

    // userSession note with authTime (time when authentication flow including requiredActions was finished)
    public static final String AUTH_TIME = "AUTH_TIME";

    // authSession client note set during brokering indicating the time when the authentication happened at the IdP
    public static final String AUTH_TIME_BROKER = "AUTH_TIME_BROKER";

    // clientSession note with flag that clientSession was authenticated through SSO cookie
    public static final String SSO_AUTH = "SSO_AUTH";

    // authSession note with flag that is true if user is forced to re-authenticate by client (EG. in case of OIDC client by sending "prompt=login")
    public static final String FORCED_REAUTHENTICATION = "FORCED_REAUTHENTICATION";

    // authSession note with flag that is true if the user's password has been correctly validated
    public static final String PASSWORD_VALIDATED = "PASSWORD_VALIDATED";

    // state checker identity token claim
    private static final String STATE_CHECKER = "state_checker";

    protected static final Logger logger = Logger.getLogger(AuthenticationManager.class);

    public static final String FORM_USERNAME = "username";
    // used solely to determine is user is logged in
    public static final String KEYCLOAK_SESSION_COOKIE = "KEYCLOAK_SESSION";

    // Protocol of the client, which initiated logout
    public static final String KEYCLOAK_LOGOUT_PROTOCOL = "KEYCLOAK_LOGOUT_PROTOCOL";
    // Filled in case that logout was triggered with "initiating idp"
    public static final String LOGOUT_INITIATING_IDP = "LOGOUT_INITIATING_IDP";

    // Parameter of LogoutEndpoint
    public static final String INITIATING_IDP_PARAM = "initiating_idp";

    private static final TokenTypeCheck VALIDATE_IDENTITY_COOKIE = new TokenTypeCheck(List.of(TokenUtil.TOKEN_TYPE_KEYCLOAK_ID));

    public static boolean isSessionValid(RealmModel realm, UserSessionModel userSession) {
        if (userSession == null) {
            logger.debug("No user session");
            return false;
        }
        if (userSession.isRememberMe() && !realm.isRememberMe()) {
            logger.debugv("Session {0} invalid: created with remember me but remember me is disabled for the realm.", userSession.getId());
            return false;
        }
        if (userSession.getNote(Details.IDENTITY_PROVIDER) != null) {
            String brokerAlias = userSession.getNote(Details.IDENTITY_PROVIDER);
            if (realm.getIdentityProviderByAlias(brokerAlias) == null) {
                // associated idp was removed, invalidate the session.
                return false;
            }
        }
        long currentTime = Time.currentTimeMillis();
        long lifespan = SessionExpirationUtils.calculateUserSessionMaxLifespanTimestamp(userSession.isOffline(),
                userSession.isRememberMe(), TimeUnit.SECONDS.toMillis(userSession.getStarted()), realm);
        long idle = SessionExpirationUtils.calculateUserSessionIdleTimestamp(userSession.isOffline(),
                userSession.isRememberMe(), TimeUnit.SECONDS.toMillis(userSession.getLastSessionRefresh()), realm);

        boolean sessionIdleOk = idle > currentTime -
                                       ((Profile.isFeatureEnabled(Profile.Feature.PERSISTENT_USER_SESSIONS) || Profile.isFeatureEnabled(Profile.Feature.CLUSTERLESS)) ? 0 : TimeUnit.SECONDS.toMillis(SessionTimeoutHelper.IDLE_TIMEOUT_WINDOW_SECONDS));
        boolean sessionMaxOk = lifespan == -1L || lifespan > currentTime;
        return sessionIdleOk && sessionMaxOk;
    }

    public static boolean isClientSessionValid(RealmModel realm, ClientModel client,
            UserSessionModel userSession, AuthenticatedClientSessionModel clientSession) {
        if (userSession == null || clientSession == null) {
            logger.debug("No user session");
            return false;
        }
        long currentTime = Time.currentTimeMillis();
        long lifespan = SessionExpirationUtils.calculateClientSessionMaxLifespanTimestamp(userSession.isOffline(),
                userSession.isRememberMe(), TimeUnit.SECONDS.toMillis(clientSession.getStarted()),
                TimeUnit.SECONDS.toMillis(userSession.getStarted()), realm, client);
        long idle = SessionExpirationUtils.calculateClientSessionIdleTimestamp(userSession.isOffline(),
                userSession.isRememberMe(), TimeUnit.SECONDS.toMillis(clientSession.getTimestamp()), realm, client);

        boolean sessionIdleOk = idle > currentTime -
                                       ((Profile.isFeatureEnabled(Profile.Feature.PERSISTENT_USER_SESSIONS) || Profile.isFeatureEnabled(Profile.Feature.CLUSTERLESS)) ? 0 : TimeUnit.SECONDS.toMillis(SessionTimeoutHelper.IDLE_TIMEOUT_WINDOW_SECONDS));
        boolean sessionMaxOk = lifespan == -1L || lifespan > currentTime;
        return sessionIdleOk && sessionMaxOk;
    }


    /**
     * @deprecated Use {@link #expireUserSessionCookie(KeycloakSession session, UserSessionModel userSession, RealmModel realm, UriInfo uriInfo)} instead.
     */
    @Deprecated
    public static boolean expireUserSessionCookie(KeycloakSession session, UserSessionModel userSession, RealmModel realm, UriInfo uriInfo, HttpHeaders headers, ClientConnection connection) {
        return expireUserSessionCookie(session, userSession, realm, uriInfo);
    }

    public static boolean expireUserSessionCookie(KeycloakSession session, UserSessionModel userSession, RealmModel realm, UriInfo uriInfo) {
        try {
            // check to see if any identity cookie is set with the same session and expire it if necessary
            String tokenString = session.getProvider(CookieProvider.class).get(CookieType.IDENTITY);
            if (tokenString == null) return true;

            TokenVerifier<AccessToken> verifier = TokenVerifier.create(tokenString, AccessToken.class)
              .realmUrl(Urls.realmIssuer(uriInfo.getBaseUri(), realm.getName()))
              .checkActive(false)
              .checkTokenType(false)
              .withChecks(VALIDATE_IDENTITY_COOKIE);

            String kid = verifier.getHeader().getKeyId();
            String algorithm = verifier.getHeader().getAlgorithm().name();

            SignatureVerifierContext signatureVerifier = session.getProvider(SignatureProvider.class, algorithm).verifier(kid);
            verifier.verifierContext(signatureVerifier);

            AccessToken token = verifier.verify().getToken();
            UserSessionModel cookieSession = session.sessions().getUserSession(realm, token.getSessionState());
            if (cookieSession == null || !cookieSession.getId().equals(userSession.getId())) return true;
            expireIdentityCookie(session);
            return true;
        } catch (Exception e) {
            return false;
        }

    }

    public static void backchannelLogout(KeycloakSession session, UserSessionModel userSession, boolean logoutBroker) {
        backchannelLogout(
                session,
                session.getContext().getRealm(),
                userSession,
                session.getContext().getUri(),
                session.getContext().getConnection(),
                session.getContext().getRequestHeaders(),
                logoutBroker
        );
    }

    public static BackchannelLogoutResponse backchannelLogout(KeycloakSession session, RealmModel realm,
            UserSessionModel userSession, UriInfo uriInfo,
            ClientConnection connection, HttpHeaders headers,
            boolean logoutBroker) {

        return backchannelLogout(session, realm, userSession, uriInfo, connection, headers, logoutBroker, userSession != null && userSession.isOffline());
    }

    /**
     *
     * @param session
     * @param realm
     * @param userSession
     * @param uriInfo
     * @param connection
     * @param headers
     * @param logoutBroker
     * @param offlineSession
     *
     * @return BackchannelLogoutResponse with logout information
     */
    public static BackchannelLogoutResponse backchannelLogout(KeycloakSession session, RealmModel realm,
            UserSessionModel userSession, UriInfo uriInfo,
            ClientConnection connection, HttpHeaders headers,
            boolean logoutBroker,
            boolean offlineSession) {
        BackchannelLogoutResponse backchannelLogoutResponse = new BackchannelLogoutResponse();

        if (userSession == null) {
            backchannelLogoutResponse.setLocalLogoutSucceeded(true);
            return backchannelLogoutResponse;
        }
        if (userSession.getState() != UserSessionModel.State.LOGGING_OUT) {
            userSession.setState(UserSessionModel.State.LOGGING_OUT);
        }

        if (logger.isDebugEnabled()) {
            UserModel user = userSession.getUser();
            String username = user == null ? null : user.getUsername();
            logger.debugv("Logging out: {0} ({1}) offline: {2}", username, userSession.getId(),
                    userSession.isOffline());
        }

        boolean expireUserSessionCookieSucceeded =
                expireUserSessionCookie(session, userSession, realm, uriInfo);

        final AuthenticationSessionManager asm = new AuthenticationSessionManager(session);
        AuthenticationSessionModel logoutAuthSession =
                createOrJoinLogoutSession(session, realm, asm, userSession, false, false);

        boolean userSessionOnlyHasLoggedOutClients = false;
        try {
            backchannelLogoutResponse = backchannelLogoutAll(session, realm, userSession, logoutAuthSession, uriInfo,
                    headers, logoutBroker);
            userSessionOnlyHasLoggedOutClients =
                    checkUserSessionOnlyHasLoggedOutClients(realm, userSession, logoutAuthSession);
        } finally {
            logger.tracef("Removing logout session '%s' after backchannel logout", logoutAuthSession.getParentSession().getId());
            session.authenticationSessions().removeRootAuthenticationSession(realm, logoutAuthSession.getParentSession());
        }

        userSession.setState(UserSessionModel.State.LOGGED_OUT);

        if (offlineSession) {
            new UserSessionManager(session).revokeOfflineUserSession(userSession);

            // Check if "online" session still exists and remove it too
            String onlineUserSessionId = userSession.getNote(CORRESPONDING_SESSION_ID);
            UserSessionModel onlineUserSession = onlineUserSessionId != null ?
                    session.sessions().getUserSession(realm, onlineUserSessionId) :
                    session.sessions().getUserSession(realm, userSession.getId());

            if (onlineUserSession != null) {
                session.sessions().removeUserSession(realm, onlineUserSession);
            }
        } else {
            session.sessions().removeUserSession(realm, userSession);
        }
        backchannelLogoutResponse
                .setLocalLogoutSucceeded(expireUserSessionCookieSucceeded && userSessionOnlyHasLoggedOutClients);
        return backchannelLogoutResponse;
    }

    public static AuthenticationSessionModel createOrJoinLogoutSession(KeycloakSession session, RealmModel realm,
            final AuthenticationSessionManager asm, UserSessionModel userSession, boolean browserCookie, boolean initiateLogout) {
        AuthenticationSessionModel logoutSession = session.getContext().getAuthenticationSession();
        if (logoutSession != null && AuthenticationSessionModel.Action.LOGGING_OUT.name().equals(logoutSession.getAction())) {
            return logoutSession;
        }

        ClientModel client = session.getContext().getClient();
        if (client == null) {
            // Account management client is used as a placeholder
            client = SystemClientUtil.getSystemClient(realm);
        }

        String authSessionId;
        RootAuthenticationSessionModel rootLogoutSession = null;
        boolean browserCookiePresent = false;

        // Try to lookup current authSessionId from browser cookie. If doesn't exist, use the same as current userSession
        if (browserCookie) {
            rootLogoutSession = asm.getCurrentRootAuthenticationSession(realm);
        }
        if (rootLogoutSession != null) {
            authSessionId = rootLogoutSession.getId();
            browserCookiePresent = true;
        } else if (userSession != null) {
            authSessionId = userSession.getId();
            rootLogoutSession = session.authenticationSessions().getRootAuthenticationSession(realm, authSessionId);
        } else {
            authSessionId = KeycloakModelUtils.generateId();
        }

        if (rootLogoutSession == null) {
            rootLogoutSession = session.authenticationSessions().createRootAuthenticationSession(realm, authSessionId);
        }
        if (browserCookie && !browserCookiePresent) {
            // Update cookie if needed
            asm.setAuthSessionCookie(authSessionId);
        }

        // See if we have logoutAuthSession inside current rootSession. Create new if not
        Optional<AuthenticationSessionModel> found = rootLogoutSession.getAuthenticationSessions().values().stream()
                .filter( authSession -> AuthenticationSessionModel.Action.LOGGING_OUT.name().equals(authSession.getAction()))
                .findFirst();

        AuthenticationSessionModel logoutAuthSession = null;
        AuthenticationSessionModel prevAuthSession = null;
        if (found.isPresent()) {
            prevAuthSession = found.get();
            if (!initiateLogout || client.getId().equals(prevAuthSession.getClient().getId())) {
                logoutAuthSession = prevAuthSession;
                logger.tracef("Found existing logout session for client '%s'. Authentication session id: %s", client.getClientId(), rootLogoutSession.getId());
            }
        }

        if (logoutAuthSession == null) {
            logoutAuthSession = rootLogoutSession.createAuthenticationSession(client);
            logoutAuthSession.setAction(AuthenticationSessionModel.Action.LOGGING_OUT.name());
            logger.tracef("Creating logout session for client '%s'. Authentication session id: %s", client.getClientId(), rootLogoutSession.getId());
            if (prevAuthSession != null) {
                // remove previous logout session for the other client
                rootLogoutSession.removeAuthenticationSessionByTabId(prevAuthSession.getTabId());
                logger.tracef("Removing previous logout session for client '%s' in %s", prevAuthSession.getClient().getClientId(), rootLogoutSession.getId());
            }
        }
        session.getContext().setAuthenticationSession(logoutAuthSession);
        session.getContext().setClient(client);

        return logoutAuthSession;
    }

    private static BackchannelLogoutResponse backchannelLogoutAll(KeycloakSession session, RealmModel realm,
            UserSessionModel userSession, AuthenticationSessionModel logoutAuthSession, UriInfo uriInfo,
            HttpHeaders headers, boolean logoutBroker) {
        BackchannelLogoutResponse backchannelLogoutResponse = new BackchannelLogoutResponse();

        for (AuthenticatedClientSessionModel clientSession : userSession.getAuthenticatedClientSessions().values()) {
            Response clientSessionLogoutResponse =
                    backchannelLogoutClientSession(session, realm, clientSession, logoutAuthSession, uriInfo, headers);

            String backchannelLogoutUrl =
                    OIDCAdvancedConfigWrapper.fromClientModel(clientSession.getClient()).getBackchannelLogoutUrl();

            BackchannelLogoutResponse.DownStreamBackchannelLogoutResponse downStreamBackchannelLogoutResponse =
                    new BackchannelLogoutResponse.DownStreamBackchannelLogoutResponse();
            downStreamBackchannelLogoutResponse.setWithBackchannelLogoutUrl(backchannelLogoutUrl != null);

            if (clientSessionLogoutResponse != null) {
                downStreamBackchannelLogoutResponse.setResponseCode(clientSessionLogoutResponse.getStatus());
            } else {
                downStreamBackchannelLogoutResponse.setResponseCode(null);
            }
            backchannelLogoutResponse.addClientResponses(downStreamBackchannelLogoutResponse);
        }
        if (logoutBroker) {
            String brokerId = userSession.getNote(Details.IDENTITY_PROVIDER);
            if (brokerId != null) {
                UserAuthenticationIdentityProvider<?> identityProvider = null;
                try {
                    identityProvider = IdentityBrokerService.getIdentityProvider(session, brokerId);
                } catch (IdentityBrokerException e) {
                    logger.warn("Skipping backchannel logout for broker " + brokerId + " - not found");
                }
                if (identityProvider != null) {
                    try {
                        identityProvider.backchannelLogout(session, userSession, uriInfo, realm);
                    } catch (Exception e) {
                        logger.warn("Exception at broker backchannel logout for broker " + brokerId, e);
                        backchannelLogoutResponse.setLocalLogoutSucceeded(false);
                    }
                }
            }
        }

        return backchannelLogoutResponse;
    }

    /**
     * Checks that all sessions have been removed from the user session. The list of logged out clients is determined from
     * the {@code logoutAuthSession} auth session notes.
     * @param realm
     * @param userSession
     * @param logoutAuthSession
     * @return {@code true} when all clients have been logged out, {@code false} otherwise
     */
    private static boolean checkUserSessionOnlyHasLoggedOutClients(RealmModel realm,
      UserSessionModel userSession, AuthenticationSessionModel logoutAuthSession) {
        final Map<String, AuthenticatedClientSessionModel> acs = userSession.getAuthenticatedClientSessions();
        Set<AuthenticatedClientSessionModel> notLoggedOutSessions = acs.entrySet().stream()
          .filter(me -> ! Objects.equals(AuthenticationSessionModel.Action.LOGGED_OUT, getClientLogoutAction(logoutAuthSession, me.getKey())))
          .filter(me -> ! Objects.equals(AuthenticationSessionModel.Action.LOGGED_OUT.name(), me.getValue().getAction()))
          .filter(me -> ! Objects.equals(AuthenticationSessionModel.Action.LOGGING_OUT.name(), me.getValue().getAction()))
          .filter(me -> Objects.nonNull(me.getValue().getProtocol()))   // Keycloak service-like accounts
          .map(Map.Entry::getValue)
          .collect(Collectors.toSet());

        boolean allClientsLoggedOut = notLoggedOutSessions.isEmpty();

        if (! allClientsLoggedOut) {
            logger.warnf("Some clients have not been logged out for user %s in %s realm: %s",
              userSession.getUser().getUsername(), realm.getName(),
              notLoggedOutSessions.stream()
                .map(AuthenticatedClientSessionModel::getClient)
                .map(ClientModel::getClientId)
                .sorted()
                .collect(Collectors.joining(", "))
            );
        } else if (logger.isDebugEnabled()) {
            logger.debugf("All clients have been logged out for user %s in %s realm, session %s",
              userSession.getUser().getUsername(), realm.getName(), userSession.getId());
        }

        return allClientsLoggedOut;
    }

    /**
     * Logs out the given client session and records the result into {@code logoutAuthSession} if set.
     *
     * @param session
     * @param realm
     * @param clientSession
     * @param logoutAuthSession auth session used for recording result of logout. May be {@code null}
     * @param uriInfo
     * @param headers
     * @return {@code http status OK} if the client was or is already being logged out, {@code null} if it is
     *         not known how to log it out and no request is made, otherwise the response of the logout request.
     */
    private static Response backchannelLogoutClientSession(KeycloakSession session, RealmModel realm,
            AuthenticatedClientSessionModel clientSession, AuthenticationSessionModel logoutAuthSession,
            UriInfo uriInfo, HttpHeaders headers) {
        UserSessionModel userSession = clientSession.getUserSession();
        ClientModel client = clientSession.getClient();

        if (client.isFrontchannelLogout()
                || AuthenticationSessionModel.Action.LOGGED_OUT.name().equals(clientSession.getAction())) {
            return null;
        }

        final Action logoutState = getClientLogoutAction(logoutAuthSession, client.getId());

        if (logoutState == AuthenticationSessionModel.Action.LOGGED_OUT
                || logoutState == AuthenticationSessionModel.Action.LOGGING_OUT) {
            return Response.ok().build();
        }

        if (!client.isEnabled()) {
            return null;
        }

        try {
            setClientLogoutAction(logoutAuthSession, client.getId(), AuthenticationSessionModel.Action.LOGGING_OUT);

            String authMethod = clientSession.getProtocol();
            if (authMethod == null) return Response.ok().build(); // must be a keycloak service like account

            logger.debugv("backchannel logout to: {0}", client.getClientId());
            LoginProtocol protocol = session.getProvider(LoginProtocol.class, authMethod);
            protocol.setRealm(realm)
                    .setHttpHeaders(headers)
                    .setUriInfo(uriInfo);

            Response clientSessionLogout = protocol.backchannelLogout(userSession, clientSession);

            setClientLogoutAction(logoutAuthSession, client.getId(), AuthenticationSessionModel.Action.LOGGED_OUT);

            return clientSessionLogout;
        } catch (Exception ex) {
            ServicesLogger.LOGGER.failedToLogoutClient(ex);
            return Response.serverError().build();
        }
    }

    private static Response frontchannelLogoutClientSession(KeycloakSession session, RealmModel realm,
      AuthenticatedClientSessionModel clientSession, AuthenticationSessionModel logoutAuthSession,
      UriInfo uriInfo, HttpHeaders headers, EventBuilder event) {
        UserSessionModel userSession = clientSession.getUserSession();
        ClientModel client = clientSession.getClient();

        if (!client.isFrontchannelLogout() || AuthenticationSessionModel.Action.LOGGED_OUT.name().equals(clientSession.getAction())) {
            return null;
        }

        final Action logoutState = getClientLogoutAction(logoutAuthSession, client.getId());

        if (logoutState == AuthenticationSessionModel.Action.LOGGED_OUT || logoutState == AuthenticationSessionModel.Action.LOGGING_OUT) {
            return null;
        }

        try {
            session.clientPolicy().triggerOnEvent(new LogoutRequestContext());
        } catch (ClientPolicyException cpe) {
            event.event(EventType.LOGOUT);
            event.detail(Details.REASON, Details.CLIENT_POLICY_ERROR);
            event.detail(Details.CLIENT_POLICY_ERROR, cpe.getError());
            event.detail(Details.CLIENT_POLICY_ERROR_DETAIL, cpe.getErrorDetail());
            event.error(cpe.getError());
            throw new ErrorResponseException(cpe.getError(), cpe.getErrorDetail(), cpe.getErrorStatus());
        }

        try {
            setClientLogoutAction(logoutAuthSession, client.getId(), AuthenticationSessionModel.Action.LOGGING_OUT);

            String authMethod = clientSession.getProtocol();
            if (authMethod == null) return null; // must be a keycloak service like account

            logger.debugv("frontchannel logout to: {0}", client.getClientId());
            LoginProtocol protocol = session.getProvider(LoginProtocol.class, authMethod);
            protocol.setRealm(realm)
                    .setHttpHeaders(headers)
                    .setUriInfo(uriInfo);

            Response response = protocol.frontchannelLogout(userSession, clientSession);
            if (response != null) {
                logger.debug("returning frontchannel logout request to client");
                // setting this to logged out cuz I'm not sure protocols can always verify that the client was logged out or not

                if (!AuthenticationSessionModel.Action.LOGGING_OUT.name().equals(clientSession.getAction())) {
                    setClientLogoutAction(logoutAuthSession, client.getId(), AuthenticationSessionModel.Action.LOGGED_OUT);
                }

                return response;
            }
        } catch (Exception e) {
            ServicesLogger.LOGGER.failedToLogoutClient(e);
        }

        return null;
    }

    /**
     * Sets logout state of the particular client into the {@code logoutAuthSession}
     * @param logoutAuthSession logoutAuthSession. May be {@code null} in which case this is a no-op.
     * @param clientUuid Client. Must not be {@code null}
     * @param action
     */
    public static void setClientLogoutAction(AuthenticationSessionModel logoutAuthSession, String clientUuid, Action action) {
        if (logoutAuthSession != null && clientUuid != null) {
            logoutAuthSession.setAuthNote(CLIENT_LOGOUT_STATE + clientUuid, action.name());
        }
    }

    /**
     * Returns the logout state of the particular client as per the {@code logoutAuthSession}
     * @param logoutAuthSession logoutAuthSession. May be {@code null} in which case this is a no-op.
     * @param clientUuid Internal ID of the client. Must not be {@code null}
     * @return State if it can be determined, {@code null} otherwise.
     */
    public static Action getClientLogoutAction(AuthenticationSessionModel logoutAuthSession, String clientUuid) {
        if (logoutAuthSession == null || clientUuid == null) {
            return null;
        }

        String state = logoutAuthSession.getAuthNote(CLIENT_LOGOUT_STATE + clientUuid);
        return state == null ? null : Action.valueOf(state);
    }

    /**
     * Logout all clientSessions belonging to the the given user session
     * @param session
     * @param realm
     * @param userSession
     * @param client
     * @param uriInfo
     * @param headers
     */
    public static void backchannelLogoutUserSessionFromClient(KeycloakSession session, RealmModel realm, UserSessionModel userSession, ClientModel client, UriInfo uriInfo, HttpHeaders headers) {
        AuthenticatedClientSessionModel clientSession = userSession.getAuthenticatedClientSessionByClient(client.getId());
        if(clientSession!=null) {
            backchannelLogoutClientSession(session,
                    realm,
                    clientSession,
                    null,
                    uriInfo,
                    headers);
            clientSession.setAction(AuthenticationSessionModel.Action.LOGGED_OUT.name());
            TokenManager.dettachClientSession(clientSession);
        }
    }

    /**
     * Logout all clientSessions of this user and client
     * @param session
     * @param realm
     * @param user
     * @param client
     * @param uriInfo
     * @param headers
     */
    public static void backchannelLogoutUserFromClient(KeycloakSession session, RealmModel realm, UserModel user, ClientModel client, UriInfo uriInfo, HttpHeaders headers) {
        session.sessions().getUserSessionsStream(realm, user)
                .toList() // collect to avoid concurrent modification.
                .forEach(userSession ->
                                backchannelLogoutUserSessionFromClient(session, realm, userSession, client, uriInfo, headers)
                        );
    }

    public static Response browserLogout(KeycloakSession session,
                                         RealmModel realm,
                                         UserSessionModel userSession,
                                         UriInfo uriInfo,
                                         ClientConnection connection,
                                         HttpHeaders headers) {
        if (userSession == null) return null;

        if (logger.isDebugEnabled()) {
            UserModel user = userSession.getUser();
            logger.debugv("Logging out: {0} ({1})", user.getUsername(), userSession.getId());
        }

        if (userSession.getState() != UserSessionModel.State.LOGGING_OUT) {
            userSession.setState(UserSessionModel.State.LOGGING_OUT);
        }

        final AuthenticationSessionManager asm = new AuthenticationSessionManager(session);
        AuthenticationSessionModel logoutAuthSession = createOrJoinLogoutSession(session, realm, asm, userSession, true, false);

        String brokerId = userSession.getNote(Details.IDENTITY_PROVIDER);
        String initiatingIdp = logoutAuthSession.getAuthNote(AuthenticationManager.LOGOUT_INITIATING_IDP);
        if (brokerId != null && !brokerId.equals(initiatingIdp)) {
            UserAuthenticationIdentityProvider<?> identityProvider = IdentityBrokerService.getIdentityProvider(session, brokerId);
            Response response = identityProvider.keycloakInitiatedBrowserLogout(session, userSession, uriInfo, realm);
            if (response != null) {
                return response;
            }
        }

        return finishBrowserLogout(session, realm, userSession, uriInfo, connection, headers);
    }

    private static Response browserLogoutAllClients(UserSessionModel userSession, KeycloakSession session, RealmModel realm, HttpHeaders headers, UriInfo uriInfo, AuthenticationSessionModel logoutAuthSession, EventBuilder event) {
        Map<Boolean, List<AuthenticatedClientSessionModel>> acss = userSession.getAuthenticatedClientSessions().values().stream()
          .filter(clientSession -> !Objects.equals(AuthenticationSessionModel.Action.LOGGED_OUT.name(), clientSession.getAction())
                                && !Objects.equals(AuthenticationSessionModel.Action.LOGGING_OUT.name(), clientSession.getAction()))
          .filter(clientSession -> clientSession.getProtocol() != null)
          .collect(Collectors.partitioningBy(clientSession -> clientSession.getClient().isFrontchannelLogout()));

        final List<AuthenticatedClientSessionModel> backendLogoutSessions = acss.get(false) == null ? Collections.emptyList() : acss.get(false);
        backendLogoutSessions.forEach(acs -> backchannelLogoutClientSession(session, realm, acs, logoutAuthSession, uriInfo, headers));

        final List<AuthenticatedClientSessionModel> redirectClients = acss.get(true) == null ? Collections.emptyList() : acss.get(true);
        for (AuthenticatedClientSessionModel nextRedirectClient : redirectClients) {
            Response response = frontchannelLogoutClientSession(session, realm, nextRedirectClient, logoutAuthSession, uriInfo, headers, event);
            if (response != null) {
                return response;
            }
        }

        return null;
    }

    public static Response finishBrowserLogout(KeycloakSession session, RealmModel realm, UserSessionModel userSession, UriInfo uriInfo, ClientConnection connection, HttpHeaders headers) {
        final AuthenticationSessionManager asm = new AuthenticationSessionManager(session);
        AuthenticationSessionModel logoutAuthSession = createOrJoinLogoutSession(session, realm, asm, userSession, true, false);
        EventBuilder event = new EventBuilder(realm, session, connection);
        Response response = browserLogoutAllClients(userSession, session, realm, headers, uriInfo, logoutAuthSession, event);
        if (response != null) {
            return response;
        }

        checkUserSessionOnlyHasLoggedOutClients(realm, userSession, logoutAuthSession);

        // For resolving artifact we don't need any cookie, all details are stored in session storage so we can remove
        expireIdentityCookie(session);
        expireRememberMeCookie(session);

        String method = userSession.getNote(KEYCLOAK_LOGOUT_PROTOCOL);
        LoginProtocol protocol = session.getProvider(LoginProtocol.class, method);
        protocol.setRealm(realm)
                .setHttpHeaders(headers)
                .setUriInfo(uriInfo)
                .setEventBuilder(event);

        response = protocol.finishBrowserLogout(userSession, logoutAuthSession);

        // It may be possible that there are some client sessions that are still in LOGGING_OUT state
        long numberOfUnconfirmedSessions = userSession.getAuthenticatedClientSessions().values().stream()
                .filter(clientSessionModel -> CommonClientSessionModel.Action.LOGGING_OUT.name().equals(clientSessionModel.getAction()))
                .count();

        // If logout flow end up correctly there should be at maximum 1 client session in LOGGING_OUT action, if there are more, something went wrong
        if (numberOfUnconfirmedSessions > 1) {
            logger.warnf("There are more than one clientSession in logging_out state. Perhaps some client did not finish logout flow correctly.");
        }

        // By setting LOGGED_OUT_UNCONFIRMED state we are saying that anybody who will turn the last client session into
        // LOGGED_OUT action can remove UserSession
        if (numberOfUnconfirmedSessions >= 1) {
            userSession.setState(UserSessionModel.State.LOGGED_OUT_UNCONFIRMED);
        } else {
            userSession.setState(UserSessionModel.State.LOGGED_OUT);
        }

        // Do not remove user session, it will be removed when last clientSession will be logged out
        if (numberOfUnconfirmedSessions < 1) {
            session.sessions().removeUserSession(realm, userSession);
        }

        logger.tracef("Removing logout session '%s'.", logoutAuthSession.getParentSession().getId());
        session.authenticationSessions().removeRootAuthenticationSession(realm, logoutAuthSession.getParentSession());

        return response;
    }

    public static void finishUnconfirmedUserSession(KeycloakSession session, RealmModel realm, UserSessionModel userSessionModel) {
        if (userSessionModel.getAuthenticatedClientSessions().values().stream().anyMatch(cs -> !CommonClientSessionModel.Action.LOGGED_OUT.name().equals(cs.getAction()))) {
            logger.warnf("UserSession with id %s is removed while there are still some user sessions that are not logged out properly.", userSessionModel.getId());
            if (logger.isTraceEnabled()) {
                logger.trace("Client sessions with their states:");
                userSessionModel.getAuthenticatedClientSessions().values()
                        .forEach(clientSessionModel -> logger.tracef("Client session for clientId: %s has action: %s", clientSessionModel.getClient().getClientId(), clientSessionModel.getAction()));
            }
        }

        session.sessions().removeUserSession(realm, userSessionModel);
    }


    public static IdentityCookieToken createIdentityToken(KeycloakSession keycloakSession, RealmModel realm, UserModel user, UserSessionModel session, String issuer) {
        IdentityCookieToken token = new IdentityCookieToken();
        token.id(SecretGenerator.getInstance().generateSecureID());
        token.issuedNow();
        token.subject(user.getId());
        token.issuer(issuer);
        token.type(TokenUtil.TOKEN_TYPE_KEYCLOAK_ID);

        if (session != null) {
            token.setSessionId(session.getId());
        }

        if (session != null && session.isRememberMe() && realm.getSsoSessionMaxLifespanRememberMe() > 0) {
            token.exp((long) Time.currentTime() + realm.getSsoSessionMaxLifespanRememberMe());
        } else if (realm.getSsoSessionMaxLifespan() > 0) {
            token.exp((long) Time.currentTime() + realm.getSsoSessionMaxLifespan());
        }

        String stateChecker = (String) keycloakSession.getAttribute(STATE_CHECKER);
        if (stateChecker == null) {
            stateChecker = Base64Url.encode(SecretGenerator.getInstance().randomBytes());
            keycloakSession.setAttribute(STATE_CHECKER, stateChecker);
        }
        token.getOtherClaims().put(STATE_CHECKER, stateChecker);

        return token;
    }

    public static void createLoginCookie(KeycloakSession keycloakSession, RealmModel realm, UserModel user, UserSessionModel session, UriInfo uriInfo, ClientConnection connection) {
        Objects.requireNonNull(session, "User session cannot be null");
        String issuer = Urls.realmIssuer(uriInfo.getBaseUri(), realm.getName());
        IdentityCookieToken identityCookieToken = createIdentityToken(keycloakSession, realm, user, session, issuer);
        String encoded = keycloakSession.tokens().encode(identityCookieToken);
        int maxAge = NewCookie.DEFAULT_MAX_AGE;
        if (session.isRememberMe()) {
            maxAge = realm.getSsoSessionMaxLifespanRememberMe() > 0 ? realm.getSsoSessionMaxLifespanRememberMe() : realm.getSsoSessionMaxLifespan();
        }
        keycloakSession.getProvider(CookieProvider.class).set(CookieType.IDENTITY, encoded, maxAge);

        String sessionCookieValue = sha256UrlEncodedHash(session.getId());

        // THIS SHOULD NOT BE A HTTPONLY COOKIE!  It is used for OpenID Connect Iframe Session support!
        // Max age should be set to the max lifespan of the session as it's used to invalidate old-sessions on re-login
        int sessionCookieMaxAge = session.isRememberMe() && realm.getSsoSessionMaxLifespanRememberMe() > 0 ? realm.getSsoSessionMaxLifespanRememberMe() : realm.getSsoSessionMaxLifespan();
        keycloakSession.getProvider(CookieProvider.class).set(CookieType.SESSION, sessionCookieValue, sessionCookieMaxAge);
    }

    public static void createRememberMeCookie(String username, UriInfo uriInfo, KeycloakSession session) {
        session.getProvider(CookieProvider.class).set(CookieType.LOGIN_HINT, "username:" + URLEncoder.encode(username, StandardCharsets.UTF_8));
    }

    public static String getRememberMeUsername(KeycloakSession session) {
        if (session.getContext().getRealm().isRememberMe()) {
            String value = session.getProvider(CookieProvider.class).get(CookieType.LOGIN_HINT);
            if (value != null) {
                String[] s = value.split(":");
                if (s[0].equals("username") && s.length == 2) {
                    return URLDecoder.decode(s[1], StandardCharsets.UTF_8);
                }
            }
        }
        return null;
    }

    public static void expireIdentityCookie(KeycloakSession session) {
        session.getProvider(CookieProvider.class).expire(CookieType.IDENTITY);
        session.getProvider(CookieProvider.class).expire(CookieType.SESSION);
    }

    public static void expireRememberMeCookie(KeycloakSession session) {
        session.getProvider(CookieProvider.class).expire(CookieType.LOGIN_HINT);
    }

    public static void expireAuthSessionCookie(KeycloakSession session) {
        session.getProvider(CookieProvider.class).expire(CookieType.AUTH_SESSION_ID);
    }

    public static String getRealmCookiePath(RealmModel realm, UriInfo uriInfo) {
        URI uri = RealmsResource.realmBaseUrl(uriInfo).build(realm.getName());
        // KEYCLOAK-5270
        return uri.getRawPath() + "/";
    }

    public AuthResult authenticateIdentityCookie(KeycloakSession session, RealmModel realm) {
        return authenticateIdentityCookie(session, realm, true);
    }

    public static AuthResult authenticateIdentityCookie(KeycloakSession session, RealmModel realm, boolean checkActive) {
        String tokenString = session.getProvider(CookieProvider.class).get(CookieType.IDENTITY);
        if (tokenString == null || tokenString.isEmpty()) {
            logger.debugv("Could not find cookie: {0}", CookieType.IDENTITY.getName());
            return null;
        }

        AuthResult authResult = verifyIdentityToken(session, realm, session.getContext().getUri(), session.getContext().getConnection(), checkActive, false, null, true, tokenString,
                session.getContext().getRequestHeaders(), verifier -> verifier.withChecks(VALIDATE_IDENTITY_COOKIE));
        if (authResult == null || authResult.session() == null) {
            expireIdentityCookie(session);
            return null;
        }
        authResult.session().setLastSessionRefresh(Time.currentTime());
        return authResult;
    }


    public static Response redirectAfterSuccessfulFlow(KeycloakSession session, RealmModel realm, UserSessionModel userSession,
                                                       ClientSessionContext clientSessionCtx,
                                                HttpRequest request, UriInfo uriInfo, ClientConnection clientConnection,
                                                EventBuilder event, AuthenticationSessionModel authSession) {
        LoginProtocol protocolImpl = session.getProvider(LoginProtocol.class, authSession.getProtocol());
        protocolImpl.setRealm(realm)
                .setHttpHeaders(request.getHttpHeaders())
                .setUriInfo(uriInfo)
                .setEventBuilder(event);
        return redirectAfterSuccessfulFlow(session, realm, userSession, clientSessionCtx, request, uriInfo, clientConnection, event, authSession, protocolImpl);

    }

    public static Response redirectAfterSuccessfulFlow(KeycloakSession session, RealmModel realm, UserSessionModel userSession,
                                                       ClientSessionContext clientSessionCtx,
                                                       HttpRequest request, UriInfo uriInfo, ClientConnection clientConnection,
                                                       EventBuilder event, AuthenticationSessionModel authSession, LoginProtocol protocol) {
        if (!compareSessionIdWithSessionCookie(session, userSession.getId())) {
            AuthResult result = authenticateIdentityCookie(session, realm, false);
            if (result != null) {
                UserSessionModel oldSession = result.session();
                if (oldSession != null && !oldSession.getId().equals(userSession.getId())) {
                    logger.debugv("Removing old user session: session: {0}", oldSession.getId());
                    session.sessions().removeUserSession(realm, oldSession);
                }
            }
        }

        // Updates users locale if required
        session.getContext().resolveLocale(userSession.getUser());

        // refresh the cookies!
        createLoginCookie(session, realm, userSession.getUser(), userSession, uriInfo, clientConnection);
        if (userSession.getState() != UserSessionModel.State.LOGGED_IN) userSession.setState(UserSessionModel.State.LOGGED_IN);
        if (userSession.isRememberMe()) {
            createRememberMeCookie(userSession.getLoginUsername(), uriInfo, session);
        } else {
            expireRememberMeCookie(session);
        }

        AuthenticatedClientSessionModel clientSession = clientSessionCtx.getClientSession();

        // Update userSession note with authTime. But just if flag SSO_AUTH is not set
        boolean isSSOAuthentication = AuthenticatorUtil.isSSOAuthentication(authSession);
        if (isSSOAuthentication) {
            clientSession.setNote(SSO_AUTH, "true");
            authSession.removeAuthNote(SSO_AUTH);
        } else {
            int authTime = Optional.ofNullable(authSession.getClientNote(AUTH_TIME_BROKER)).map(Integer::parseInt).orElse(Time.currentTime());
            userSession.setNote(AUTH_TIME, String.valueOf(authTime));
            clientSession.removeNote(SSO_AUTH);
        }

        // The user has successfully logged in and we can clear his/her previous login failure attempts.
        logSuccess(session, authSession);

        return protocol.authenticated(authSession, userSession, clientSessionCtx);

    }

    /**
     * @param session keycloak session
     * @param sessionId in plain-text
     * @return true if sessionId matches with the session from KEYCLOAK_SESSION_COOKIE
     */
    public static boolean compareSessionIdWithSessionCookie(KeycloakSession session, String sessionId) {
        Objects.requireNonNull(sessionId, "Session id cannot be null");

        String cookie = session.getProvider(CookieProvider.class).get(CookieType.SESSION);
        if (cookie == null || cookie.isEmpty()) {
            logger.debugv("Could not find cookie: {0}", KEYCLOAK_SESSION_COOKIE);
            return false;
        }

        if (cookie.equals(sha256UrlEncodedHash(sessionId))) return true;

        // Backwards compatibility
        String[] split = cookie.split("/");
        if (split.length >= 3) {
            String oldSessionId = split[2];
            return !sessionId.equals(oldSessionId);
        }
        return false;
    }

    public static boolean isSSOAuthentication(AuthenticatedClientSessionModel clientSession) {
        String ssoAuth = clientSession.getNote(SSO_AUTH);
        return Boolean.parseBoolean(ssoAuth);
    }


    public static Response nextActionAfterAuthentication(KeycloakSession session, AuthenticationSessionModel authSession,
                                                  ClientConnection clientConnection,
                                                  HttpRequest request, UriInfo uriInfo, EventBuilder event) {
        return nextActionAfterAuthentication(session, authSession, clientConnection, request, uriInfo, event, new HashSet<>());
    }

    private static Response nextActionAfterAuthentication(KeycloakSession session, AuthenticationSessionModel authSession,
                                                  ClientConnection clientConnection,
                                                  HttpRequest request, UriInfo uriInfo, EventBuilder event,
                                                  Set<String> ignoredActions) {
        Response requiredAction = actionRequired(session, authSession, request, event, ignoredActions);
        if (requiredAction != null) return requiredAction;
        return finishedRequiredActions(session, authSession, null, clientConnection, request, uriInfo, event);

    }


    public static Response redirectToRequiredActions(KeycloakSession session, RealmModel realm, AuthenticationSessionModel authSession, UriInfo uriInfo, String requiredAction) {
        // redirect to non-action url so browser refresh button works without reposting past data
        ClientSessionCode<AuthenticationSessionModel> accessCode = new ClientSessionCode<>(session, realm, authSession);
        accessCode.setAction(Action.REQUIRED_ACTIONS.name());
        authSession.setAuthNote(AuthenticationProcessor.CURRENT_FLOW_PATH, LoginActionsService.REQUIRED_ACTION);
        authSession.setAuthNote(AuthenticationProcessor.CURRENT_AUTHENTICATION_EXECUTION, requiredAction);

        UriBuilder uriBuilder = LoginActionsService.loginActionsBaseUrl(uriInfo)
                .path(LoginActionsService.REQUIRED_ACTION);

        if (requiredAction != null) {
            uriBuilder.queryParam(Constants.EXECUTION, requiredAction);
        }

        uriBuilder.queryParam(Constants.CLIENT_ID, authSession.getClient().getClientId());
        uriBuilder.queryParam(Constants.TAB_ID, authSession.getTabId());
        uriBuilder.queryParam(Constants.CLIENT_DATA, AuthenticationProcessor.getClientData(session, authSession));

        if (uriInfo.getQueryParameters().containsKey(LoginActionsService.AUTH_SESSION_ID)) {
            uriBuilder.queryParam(LoginActionsService.AUTH_SESSION_ID, authSession.getParentSession().getId());

        }

        URI redirect = uriBuilder.build(realm.getName());
        return Response.status(302).location(redirect).build();

    }


    public static Response finishedRequiredActions(KeycloakSession session, AuthenticationSessionModel authSession, UserSessionModel userSession,
                                                   ClientConnection clientConnection, HttpRequest request, UriInfo uriInfo, EventBuilder event) {
        String actionTokenKeyToInvalidate = authSession.getAuthNote(INVALIDATE_ACTION_TOKEN);
        if (actionTokenKeyToInvalidate != null) {
            SingleUseObjectKeyModel actionTokenKey = DefaultActionTokenKey.from(actionTokenKeyToInvalidate);
            if (actionTokenKey != null) {
                SingleUseObjectProvider singleUseObjectProvider = session.singleUseObjects();
                singleUseObjectProvider.put(actionTokenKeyToInvalidate, actionTokenKey.getExp() - Time.currentTime(), null); // Token is invalidated
            }
        }

        if (authSession.getAuthNote(END_AFTER_REQUIRED_ACTIONS) != null) {
            LoginFormsProvider infoPage = session.getProvider(LoginFormsProvider.class).setAuthenticationSession(authSession)
                    .setSuccess(Messages.ACCOUNT_UPDATED);
            if (authSession.getAuthNote(SET_REDIRECT_URI_AFTER_REQUIRED_ACTIONS) != null) {
                if (authSession.getRedirectUri() != null) {
                    infoPage.setAttribute("pageRedirectUri", authSession.getRedirectUri());
                }

            } else {
                SystemClientUtil.checkSkipLink(session, authSession);
            }
            Response response = infoPage
                    .setDetachedAuthSession()
                    .createInfoPage();

            new AuthenticationSessionManager(session).removeAuthenticationSession(authSession.getRealm(), authSession, true);

            return response;
        }
        RealmModel realm = authSession.getRealm();

        ClientSessionContext clientSessionCtx = AuthenticationProcessor.attachSession(authSession, userSession, session, realm, clientConnection, event);
        userSession = clientSessionCtx.getClientSession().getUserSession();

        event.event(EventType.LOGIN);
        event.session(userSession);
        Response response = redirectAfterSuccessfulFlow(session, realm, userSession, clientSessionCtx, request, uriInfo, clientConnection, event, authSession);
        event.success();
        return response;
    }

    // Return null if action is not required. Or the alias of the requiredAction in case it is required.
    public static String nextRequiredAction(final KeycloakSession session, final AuthenticationSessionModel authSession,
            final HttpRequest request, final EventBuilder event) {
        final var realm = authSession.getRealm();
        final var user = authSession.getAuthenticatedUser();

        evaluateRequiredActionTriggers(session, authSession, request, event, realm, user, new HashSet<>());

        final var kcAction = authSession.getClientNote(Constants.KC_ACTION);
        final var nextApplicableAction =
                getFirstApplicableRequiredAction(realm, authSession, user, kcAction, new HashSet<>());
        if (nextApplicableAction != null) {
            return nextApplicableAction.getAlias();
        }

        final var client = authSession.getClient();
        if (client.isConsentRequired() || isOAuth2DeviceVerificationFlow(authSession)) {

            UserConsentModel grantedConsent = getEffectiveGrantedConsent(session, authSession);

            // See if any clientScopes need to be approved on consent screen
            List<AuthorizationDetails> clientScopesToApprove =
                    getClientScopesToApproveOnConsentScreen(grantedConsent, session, authSession);
            if (!clientScopesToApprove.isEmpty()) {
                return Action.OAUTH_GRANT.name();
            }

            String consentDetail = (grantedConsent != null) ? Details.CONSENT_VALUE_PERSISTED_CONSENT
                    : Details.CONSENT_VALUE_NO_CONSENT_REQUIRED;
            event.detail(Details.CONSENT, consentDetail);
        } else {
            event.detail(Details.CONSENT, Details.CONSENT_VALUE_NO_CONSENT_REQUIRED);
        }
        return null;
    }


    private static UserConsentModel getEffectiveGrantedConsent(KeycloakSession session, AuthenticationSessionModel authSession) {
        // https://tools.ietf.org/html/draft-ietf-oauth-device-flow-15#section-5.4
        // The spec says "The authorization server SHOULD display information about the device",
        // so we ignore existing persistent consent to display the consent screen always.
        if (isOAuth2DeviceVerificationFlow(authSession)) {
            return null;
        }

        // If prompt=consent, we ignore existing persistent consent
        String prompt = authSession.getClientNote(OIDCLoginProtocol.PROMPT_PARAM);
        if (TokenUtil.hasPrompt(prompt, OIDCLoginProtocol.PROMPT_VALUE_CONSENT)) {
            return null;
        } else {
            final RealmModel realm = authSession.getRealm();
            final UserModel user = authSession.getAuthenticatedUser();
            final ClientModel client = authSession.getClient();

            return UserConsentManager.getConsentByClient(session, realm, user, client.getId());
        }
    }

    private static Response actionRequired(final KeycloakSession session, final AuthenticationSessionModel authSession,
            final HttpRequest request, final EventBuilder event, Set<String> ignoredActions) {
        final var realm = authSession.getRealm();
        final var user = authSession.getAuthenticatedUser();

        evaluateRequiredActionTriggers(session, authSession, request, event, realm, user, ignoredActions);

        event.detail(Details.CODE_ID, authSession.getParentSession().getId());

        final var actionResponse = executionActions(session, authSession, request, event, realm, user, ignoredActions);
        if (actionResponse != null) {
            return actionResponse;
        }

        final var client = authSession.getClient();
        logger.debugv("processAccessCode: go to oauth page?: {0}", client.isConsentRequired());

        // https://tools.ietf.org/html/draft-ietf-oauth-device-flow-15#section-5.4
        // The spec says "The authorization server SHOULD display information about the device",
        // so the consent is required when running a verification flow of OAuth 2.0 Device Authorization Grant.
        if (client.isConsentRequired() || isOAuth2DeviceVerificationFlow(authSession)) {

            UserConsentModel grantedConsent = getEffectiveGrantedConsent(session, authSession);

            List<AuthorizationDetails> clientScopesToApprove =
                    getClientScopesToApproveOnConsentScreen(grantedConsent, session, authSession);

            // Skip grant screen if everything was already approved by this user
            if (!clientScopesToApprove.isEmpty()) {
                String execution = AuthenticatedClientSessionModel.Action.OAUTH_GRANT.name();

                ClientSessionCode<AuthenticationSessionModel> accessCode =
                        new ClientSessionCode<>(session, realm, authSession);
                accessCode.setAction(Action.REQUIRED_ACTIONS.name());
                authSession.setAuthNote(AuthenticationProcessor.CURRENT_AUTHENTICATION_EXECUTION, execution);

                return session.getProvider(LoginFormsProvider.class)
                        .setAuthenticationSession(authSession)
                        .setExecution(execution)
                        .setClientSessionCode(accessCode.getOrGenerateCode())
                        .setAccessRequest(clientScopesToApprove)
                        .createOAuthGrant();
            } else {
                String consentDetail = (grantedConsent != null) ? Details.CONSENT_VALUE_PERSISTED_CONSENT
                        : Details.CONSENT_VALUE_NO_CONSENT_REQUIRED;
                event.detail(Details.CONSENT, consentDetail);
            }
        } else {
            event.detail(Details.CONSENT, Details.CONSENT_VALUE_NO_CONSENT_REQUIRED);
        }
        return null;

    }

    private static List<AuthorizationDetails> getClientScopesToApproveOnConsentScreen(UserConsentModel grantedConsent, KeycloakSession session, AuthenticationSessionModel authSession) {
        // Client Scopes to be displayed on consent screen
        List<AuthorizationDetails> clientScopesToDisplay = new LinkedList<>();

        // AuthorizationDetails are going to be returned regardless of the Dynamic Scope feature state
        for (AuthorizationDetails authDetails : getClientScopeModelStream(session).toList()) {
            ClientScopeModel clientScope = authDetails.getClientScope();
            if (clientScope == null || !clientScope.isDisplayOnConsentScreen()) {
                continue;
            }

            // we need to add dynamic scopes with params to the scopes to consent every time for now
            if (grantedConsent == null || !grantedConsent.isClientScopeGranted(clientScope) || isDynamicScopeWithParam(authDetails)) {
                clientScopesToDisplay.add(authDetails);
            }
        }
        //force consent when running a verification flow of OAuth 2.0 Device Authorization Grant
        if(clientScopesToDisplay.isEmpty() && isOAuth2DeviceVerificationFlow(authSession)) {
            clientScopesToDisplay.add(new AuthorizationDetails(authSession.getClient()));
        }

        return clientScopesToDisplay;
    }

    private static boolean isDynamicScopeWithParam(AuthorizationDetails authorizationDetails) {
        boolean dynamicScopeWithParam = authorizationDetails.getClientScope().isDynamicScope()
                && authorizationDetails.getAuthorizationDetails() != null;
        if (dynamicScopeWithParam) {
            logger.debugf("Scope %1s is a dynamic scope with param: %2s",
                    authorizationDetails.getAuthorizationDetails().getScopeNameFromCustomData(),
                    authorizationDetails.getDynamicScopeParam());
        }
        return dynamicScopeWithParam;
    }


    private static Stream<AuthorizationDetails> getClientScopeModelStream(KeycloakSession session) {
        AuthenticationSessionModel authSession = session.getContext().getAuthenticationSession();
        //if Dynamic Scopes are enabled, get the scopes from the AuthorizationRequestContext, passing the session and scopes as parameters
        // then concat a Stream with the ClientModel, as it's discarded in the getAuthorizationRequestContext method
        if (Profile.isFeatureEnabled(Profile.Feature.DYNAMIC_SCOPES)) {
            return AuthorizationContextUtil.getAuthorizationRequestsStreamFromScopesWithClient(session, authSession.getClientNote(OAuth2Constants.SCOPE));
        }
        // if dynamic scopes are not enabled, we retain the old behaviour, but the ClientScopes will be wrapped in
        // AuthorizationRequest objects to standardize the code handling these.
        return authSession.getClientScopes().stream()
                .map(scopeId -> KeycloakModelUtils.findClientScopeById(authSession.getRealm(), authSession.getClient(), scopeId))
                .map(AuthorizationDetails::new);
    }


    public static void setClientScopesInSession(KeycloakSession session, AuthenticationSessionModel authSession) {
        ClientModel client = authSession.getClient();
        UserModel user = authSession.getAuthenticatedUser();

        // todo scope param protocol independent
        String scopeParam = authSession.getClientNote(OAuth2Constants.SCOPE);

        Set<String> requestedClientScopes = TokenManager.getRequestedClientScopes(session, scopeParam, client, user)
                .map(ClientScopeModel::getId).collect(Collectors.toSet());

        authSession.setClientScopes(requestedClientScopes);
    }

    public static RequiredActionProvider createRequiredAction(RequiredActionContextResult context) {
        return context.getFactory().create(context.getSession());
    }


    protected static Response executionActions(KeycloakSession session, AuthenticationSessionModel authSession,
            HttpRequest request, EventBuilder event, RealmModel realm, UserModel user, Set<String> ignoredActions) {
        final String kcAction = authSession.getClientNote(Constants.KC_ACTION);
        final RequiredActionProviderModel firstApplicableRequiredAction =
                getFirstApplicableRequiredAction(realm, authSession, user, kcAction, ignoredActions);
        boolean kcActionExecution = kcAction != null && kcAction.equals(firstApplicableRequiredAction.getProviderId());

        if (firstApplicableRequiredAction != null) {
            return executeAction(session, authSession, firstApplicableRequiredAction, request, event, realm, user,
                    kcActionExecution, ignoredActions);
        }

        return null;
    }

    private static Response executeAction(KeycloakSession session, AuthenticationSessionModel authSession, RequiredActionProviderModel model,
                                          HttpRequest request, EventBuilder event, RealmModel realm, UserModel user, boolean kcActionExecution,
                                          Set<String> ignoredActions) {
        RequiredActionFactory factory = (RequiredActionFactory) session.getKeycloakSessionFactory()
                .getProviderFactory(RequiredActionProvider.class, model.getProviderId());
        if (factory == null) {
            throw new RuntimeException("Unable to find factory for Required Action: " + model.getProviderId() + " did you forget to declare it in a META-INF/services file?");
        }
        RequiredActionContextResult context = new RequiredActionContextResult(authSession, realm, event, session, request, user, factory);
        RequiredActionProvider actionProvider = null;
        try {
            actionProvider = createRequiredAction(context);
        } catch (AuthenticationFlowException e) {
            if (e.getResponse() != null) {
                return e.getResponse();
            }
            throw e;
        }

        if (kcActionExecution) {
            if (actionProvider.initiatedActionSupport() == InitiatedActionSupport.NOT_SUPPORTED) {
                logger.debugv("Requested action {0} does not support being invoked with kc_action", factory.getId());
                setKcActionStatus(factory.getId(), RequiredActionContext.KcActionStatus.ERROR, authSession);
                ignoredActions.add(factory.getId());
                return nextActionAfterAuthentication(session, authSession, session.getContext().getConnection(), request, session.getContext().getUri(), event, ignoredActions);
            } else if (!model.isEnabled()) {
                logger.debugv("Requested action {0} is disabled and can't be invoked with kc_action", factory.getId());
                setKcActionStatus(factory.getId(), RequiredActionContext.KcActionStatus.ERROR, authSession);
                ignoredActions.add(factory.getId());
                return nextActionAfterAuthentication(session, authSession, session.getContext().getConnection(), request, session.getContext().getUri(), event, ignoredActions);
            } else {
                authSession.setClientNote(Constants.KC_ACTION_EXECUTING, factory.getId());
            }
        }

        actionProvider.requiredActionChallenge(context);

        if (context.getStatus() == RequiredActionContext.Status.FAILURE) {
            LoginProtocol protocol = context.getSession().getProvider(LoginProtocol.class, context.getAuthenticationSession().getProtocol());
            protocol.setRealm(context.getRealm())
                    .setHttpHeaders(context.getHttpRequest().getHttpHeaders())
                    .setUriInfo(context.getUriInfo())
                    .setEventBuilder(event);
            Response response = protocol.sendError(context.getAuthenticationSession(), Error.CONSENT_DENIED, null);
            event.error(Errors.REJECTED_BY_USER);
            return response;
        }
        else if (context.getStatus() == RequiredActionContext.Status.CHALLENGE) {
            authSession.setAuthNote(AuthenticationProcessor.CURRENT_AUTHENTICATION_EXECUTION, model.getProviderId());
            return context.getChallenge();
        }
        else if (context.getStatus() == RequiredActionContext.Status.IGNORE) {
            setKcActionStatus(factory.getId(), RequiredActionContext.KcActionStatus.ERROR, authSession);
            ignoredActions.add(factory.getId());
            return nextActionAfterAuthentication(session, authSession, session.getContext().getConnection(), request, session.getContext().getUri(), event, ignoredActions);
        }
        else if (context.getStatus() == RequiredActionContext.Status.SUCCESS) {
            event.clone().event(EventType.CUSTOM_REQUIRED_ACTION).detail(Details.CUSTOM_REQUIRED_ACTION, factory.getId()).success();
            // don't have to perform the same action twice, so remove it from both the user and session required actions
            authSession.getAuthenticatedUser().removeRequiredAction(factory.getId());
            authSession.removeRequiredAction(factory.getId());
            setKcActionStatus(factory.getId(), RequiredActionContext.KcActionStatus.SUCCESS, authSession);
            return nextActionAfterAuthentication(session, authSession, session.getContext().getConnection(), request, session.getContext().getUri(), event, ignoredActions);
        }

        return null;
    }

    private static RequiredActionProviderModel getFirstApplicableRequiredAction(final RealmModel realm,
            final AuthenticationSessionModel authSession, final UserModel user, final String kcAction, final Set<String> ignoredActions) {
        final var applicableRequiredActionsSorted =
                getApplicableRequiredActionsSorted(realm, authSession, user, kcAction, ignoredActions);

        final RequiredActionProviderModel firstApplicableRequiredAction;
        if (applicableRequiredActionsSorted.isEmpty()) {
            firstApplicableRequiredAction = null;
            logger.debugv("Did not find applicable required action");
        } else {
            firstApplicableRequiredAction = applicableRequiredActionsSorted.iterator().next();
            logger.debugv("first applicable required action: {0}", firstApplicableRequiredAction.getAlias());
        }

        return firstApplicableRequiredAction;
    }

    private static List<RequiredActionProviderModel> getApplicableRequiredActionsSorted(final RealmModel realm,
            final AuthenticationSessionModel authSession, final UserModel user, final String kcActionAlias, final Set<String> ignoredActions) {
        final Set<String> nonInitiatedActionAliases = new HashSet<>();
        nonInitiatedActionAliases.addAll(user.getRequiredActionsStream().toList());
        nonInitiatedActionAliases.addAll(authSession.getRequiredActions());

        final Map<String, RequiredActionProviderModel> applicableNonInitiatedActions = nonInitiatedActionAliases.stream()
                .map(alias -> getApplicableRequiredAction(realm, alias))
                .filter(Objects::nonNull)
                .filter(model -> !ignoredActions.contains(model.getProviderId()))
                .collect(Collectors.toMap(RequiredActionProviderModel::getAlias, Function.identity()));

        RequiredActionProviderModel kcAction = null;
        if (kcActionAlias != null) {
            kcAction = getApplicableRequiredAction(realm, kcActionAlias);
            if (kcAction == null) {
                logger.debugv("Requested action {0} not configured for realm", kcActionAlias);
                setKcActionStatus(kcActionAlias, RequiredActionContext.KcActionStatus.ERROR, authSession);
            } else {
                if (applicableNonInitiatedActions.containsKey(kcActionAlias)) {
                    setKcActionToEnforced(kcActionAlias, authSession);
                }
            }
        }

        final List<RequiredActionProviderModel> applicableActionsSorted = applicableNonInitiatedActions.values().stream()
                .sorted(RequiredActionProviderModel.RequiredActionComparator.SINGLETON)
                .collect(Collectors.toList());

        // Insert "kc_action" as last action (unless present in required actions)
        if (kcAction != null && !applicableNonInitiatedActions.containsKey(kcActionAlias)) {
            applicableActionsSorted.add(kcAction);
        }

        if (logger.isDebugEnabled()) {
            logger.debugv("applicable required actions (sorted): {0}",
                    applicableActionsSorted.stream().map(RequiredActionProviderModel::getAlias).toList());
        }

        return applicableActionsSorted;
    }

    private static RequiredActionProviderModel getApplicableRequiredAction(final RealmModel realm, final String alias) {
        final var model = realm.getRequiredActionProviderByAlias(alias);
        if (model == null) {
            logger.warnv(
                    "Could not find configuration for Required Action {0}, did you forget to register it?",
                    alias);
            return null;
        }

        if (!model.isEnabled()) {
            return null;
        }

        return model;
    }

    public static void evaluateRequiredActionTriggers(final KeycloakSession session, final AuthenticationSessionModel authSession,
                                                      final HttpRequest request, final EventBuilder event,
                                                      final RealmModel realm, final UserModel user) {
        evaluateRequiredActionTriggers(session, authSession, request, event, realm, user, new HashSet<>());
    }

    private static void evaluateRequiredActionTriggers(final KeycloakSession session, final AuthenticationSessionModel authSession,
                                                      final HttpRequest request, final EventBuilder event,
                                                      final RealmModel realm, final UserModel user, Set<String> ignoredActions) {
        // see if any required actions need triggering, i.e. an expired password
        realm.getRequiredActionProvidersStream()
                .filter(RequiredActionProviderModel::isEnabled)
                .filter(model -> !ignoredActions.contains(model.getProviderId()))
                .map(model -> toRequiredActionFactory(session, model, realm))
                .filter(Objects::nonNull)
                .forEachOrdered(f -> evaluateRequiredAction(session, authSession, request, event, realm, user, f));
    }

    private static void evaluateRequiredAction(final KeycloakSession session, final AuthenticationSessionModel authSession,
                                        final HttpRequest request, final EventBuilder event, final RealmModel realm,
                                        final UserModel user, RequiredActionFactory factory) {
        RequiredActionProvider provider = factory.create(session);
        RequiredActionContextResult result = new RequiredActionContextResult(authSession, realm, event, session, request, user, factory) {
            @Override
            public void challenge(Response response) {
                throw new RuntimeException("Not allowed to call challenge() within evaluateTriggers()");
            }

            @Override
            public void failure() {
                throw new RuntimeException("Not allowed to call failure() within evaluateTriggers()");
            }

            @Override
            public void success() {
                throw new RuntimeException("Not allowed to call success() within evaluateTriggers()");
            }

            @Override
            public void cancel() {
                throw new RuntimeException("Not allowed to call cancel() within evaluateTriggers()");
            }

            @Override
            public void ignore() {
                throw new RuntimeException("Not allowed to call ignore() within evaluateTriggers()");
            }
        };

        provider.evaluateTriggers(result);
    }

    private static RequiredActionFactory toRequiredActionFactory(KeycloakSession session, RequiredActionProviderModel model, RealmModel realm) {
        RequiredActionFactory factory = (RequiredActionFactory) session.getKeycloakSessionFactory()
                .getProviderFactory(RequiredActionProvider.class, model.getProviderId());
        if (factory == null) {
            if (!DefaultRequiredActions.isActionAvailable(model)) {
                logger.warnf("Required action provider factory '%s' configured in the realm '%s' is not available. " +
                        "Provider not found or feature is disabled.", model.getProviderId(), realm.getName());
            } else {
                throw new RuntimeException(String.format("Unable to find factory for Required Action '%s' configured in the realm '%s'. " +
                        "Did you forget to declare it in a META-INF/services file?", model.getProviderId(), realm.getName()));
            }
        }
        return factory;
    }

    public static AuthResult verifyIdentityToken(KeycloakSession session, RealmModel realm, UriInfo uriInfo, ClientConnection connection, boolean checkActive, boolean checkTokenType,
                                                 String checkAudience, boolean isCookie, String tokenString, HttpHeaders headers, Consumer<TokenVerifier<AccessToken>> verifierConsumer) {
        try {
            TokenVerifier<AccessToken> verifier = TokenVerifier.create(tokenString, AccessToken.class)
              .withDefaultChecks()
              .realmUrl(Urls.realmIssuer(uriInfo.getBaseUri(), realm.getName()))
              .checkActive(checkActive)
              .checkTokenType(checkTokenType);

            verifierConsumer.accept(verifier);

            if (checkAudience != null) {
                verifier.audience(checkAudience);
            }

            // Check token revocation in case of access token
            if (checkTokenType) {
                verifier.withChecks(new TokenManager.TokenRevocationCheck(session));
            }

            String kid = verifier.getHeader().getKeyId();
            String algorithm = verifier.getHeader().getAlgorithm().name();

            SignatureVerifierContext signatureVerifier = session.getProvider(SignatureProvider.class, algorithm).verifier(kid);
            verifier.verifierContext(signatureVerifier);

            AccessToken token = verifier.verify().getToken();
            if (checkActive && (!token.isActive() || token.getIat() < realm.getNotBefore())) {
                logger.debugf("Identity cookie expired. Token expiration: %d, Current Time: %d. token issued at: %d, realm not before: %d", token.getExp(), Time.currentTime(), token.getIat(), realm.getNotBefore());
                return null;
            }

            KeycloakContext context = session.getContext();
            Consumer<UserSessionModel> invalidUserSessionCallback = (userSession) -> {
                // Ignored for offline session for now?
                if (userSession.isOffline()) return;

                String userSessionId = userSession.getId();
                KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), context, newSession -> {
                    RealmModel realmModel = newSession.realms().getRealm(realm.getId());
                    UserSessionModel userSessionModel = newSession.sessions().getUserSession(realmModel, userSessionId);
                    backchannelLogout(newSession, realmModel, userSessionModel, uriInfo, connection, headers, true);
                });
                // remove the user session here so that the external persistent session tx becomes aware of the removal that happened
                // during the backchannel logout.
                session.sessions().removeUserSession(realm, userSession);
            };

            UserSessionModel userSession = null;
            ClientModel client = null;
            session.setAttribute("state_checker", token.getOtherClaims().get("state_checker"));

            if (isCookie) {
                UserSessionUtil.UserSessionValidationResult validationResult = UserSessionUtil.findValidSessionForIdentityCookie(session, realm, token, invalidUserSessionCallback);
                if (validationResult.getError() != null) {
                    return null;
                }
                userSession = validationResult.getUserSession();
            } else {
                client = realm.getClientByClientId(token.getIssuedFor());
                if (client == null) {
                    return null;
                }
                UserSessionUtil.UserSessionValidationResult validationResult = UserSessionUtil.findValidSessionForAccessToken(session, realm, token, client, invalidUserSessionCallback);
                if (validationResult.getError() != null) {
                    return null;
                }
                userSession = validationResult.getUserSession();
                if (!isClientValid(userSession, client, token)) {
                    return null;
                }
            }

            UserModel user = userSession.getUser();
            if (!TokenManager.isUserValid(session, realm, token, user)) {
                return null;
            }

            if (!isCookie) {
                context.setClient(client);
                context.setBearerToken(token);
            }
            context.setUserSession(userSession);

            return new AuthResult(user, userSession, token, client);
        } catch (VerificationException e) {
            logger.debugf("Failed to verify identity token: %s", e.getMessage());
        }
        return null;
    }

    // Verify client and whether clientSession exists
    private static boolean isClientValid(UserSessionModel userSession, ClientModel client, AccessToken token) {
        if (client == null || !client.isEnabled()) {
            logger.debugf("Identity token issued for unknown or disabled client '%s'", token.getIssuedFor());
            return false;
        }

        if (token.getIat() < client.getNotBefore()) {
            logger.debug("Client notBefore newer than token");
            return false;
        }

        // User session may not exists for example during client credentials auth
        if (userSession == null) return true;

        AuthenticatedClientSessionModel clientSession = userSession.getAuthenticatedClientSessionByClient(client.getId());
        if (clientSession == null) {
            logger.debugf("Client session for client '%s' not present in user session '%s'", client.getClientId(), userSession.getId());
            return false;
        }
        return true;
    }

    public static void resolveLightweightAccessTokenRoles(KeycloakSession session, AccessToken accessToken, RealmModel realm) {
        final String issuedFor = accessToken.getIssuedFor();
        ClientModel client = realm.getClientByClientId(issuedFor);
        if(client == null) {
            return;
        }

        TokenContextEncoderProvider encoder = session.getProvider(TokenContextEncoderProvider.class);
        AccessTokenContext subjectTokenContext = encoder.getTokenContextFromTokenId(accessToken.getId());
        boolean isAccessTokenLightweight = AccessTokenContext.TokenType.LIGHTWEIGHT.equals(subjectTokenContext.getTokenType());
        if (isAccessTokenLightweight || accessToken.getSubject() == null || (accessToken.getSessionId() == null && accessToken.getResourceAccess().isEmpty() && accessToken.getRealmAccess() == null)) {
            //get user session
            UserSessionModel userSession = UserSessionUtil.findValidSessionForAccessToken(session,realm, accessToken, client, (invalidUserSession -> {})).getUserSession();

            if (userSession != null) {
                //get client session
                AuthenticatedClientSessionModel clientSession = userSession.getAuthenticatedClientSessionByClient(client.getId());
                //set realm roles
                ClientSessionContext clientSessionCtx = DefaultClientSessionContext.fromClientSessionAndScopeParameter(clientSession, accessToken.getScope(), session);
                AccessToken.Access realmAccess = RoleResolveUtil.getResolvedRealmRoles(session, clientSessionCtx, false);
                Map<String, AccessToken.Access> clientAccess = RoleResolveUtil.getAllResolvedClientRoles(session, clientSessionCtx);
                accessToken.subject(userSession.getUser().getId());
                accessToken.setRealmAccess(realmAccess);
                accessToken.setResourceAccess(clientAccess);
            }
        }
    }

    public enum AuthenticationStatus {
        SUCCESS, ACCOUNT_TEMPORARILY_DISABLED, ACCOUNT_DISABLED, ACTIONS_REQUIRED, INVALID_USER, INVALID_CREDENTIALS, MISSING_PASSWORD, MISSING_TOTP, FAILED
    }

    public record AuthResult(UserModel user, UserSessionModel session, AccessToken token, ClientModel client) {
        /**
         * @deprecated use {@link #session()} instead.
         */
        @Deprecated(since = "26.5", forRemoval = true)
        public UserSessionModel getSession() {
            return session;
        }

        /**
         * @deprecated use {@link #user()} instead.
         */
        @Deprecated(since = "26.5", forRemoval = true)
        public UserModel getUser() {
            return user;
        }

        /**
         * @deprecated use {@link #token()} instead.
         */
        @Deprecated(since = "26.5", forRemoval = true)
        public AccessToken getToken() {
            return token;
        }

        /**
         * @deprecated use {@link #client()} instead.
         */
        @Deprecated(since = "26.5", forRemoval = true)
        public ClientModel getClient() {
            return client;
        }
    }

    public static void setKcActionStatus(String executedProviderId, RequiredActionContext.KcActionStatus status, AuthenticationSessionModel authSession) {
        if (executedProviderId.equals(authSession.getClientNote(Constants.KC_ACTION))) {
            authSession.setClientNote(Constants.KC_ACTION_STATUS, status.name().toLowerCase());
            authSession.removeClientNote(Constants.KC_ACTION);
            authSession.removeClientNote(Constants.KC_ACTION_EXECUTING);
        }
    }

    public static void setKcActionToEnforced(String executedProviderId, AuthenticationSessionModel authSession) {
        if (executedProviderId.equals(authSession.getClientNote(Constants.KC_ACTION))) {
            authSession.setClientNote(Constants.KC_ACTION_ENFORCED, Boolean.TRUE.toString());
        }
    }

    public static void logSuccess(KeycloakSession session, AuthenticationSessionModel authSession) {
        RealmModel realm = session.getContext().getRealm();
        if (realm.isBruteForceProtected()) {
            UserModel user = lookupUserForBruteForceLog(session, realm, authSession);
            if (user != null) {
                BruteForceProtector bruteForceProtector = session.getProvider(BruteForceProtector.class);
                bruteForceProtector.successfulLogin(realm, user, session.getContext().getConnection(), session.getContext().getHttpRequest().getUri());
            }
        }
    }

    public static UserModel lookupUserForBruteForceLog(KeycloakSession session, RealmModel realm, AuthenticationSessionModel authenticationSession) {
        UserModel user = authenticationSession.getAuthenticatedUser();
        if (user != null) return user;

        String username = authenticationSession.getAuthNote(AbstractUsernameFormAuthenticator.ATTEMPTED_USERNAME);
        if (username != null) {
            return KeycloakModelUtils.findUserByNameOrEmail(session, realm, username);
        }

        return null;
    }

    public static String sha256UrlEncodedHash(String input) {
        return HashUtils.sha256UrlEncodedHash(input, StandardCharsets.ISO_8859_1);
    }

    public static String getRequestedScopes(KeycloakSession session) {
        return getRequestedScopes(session, session.getContext().getClient());
    }

    public static String getRequestedScopes(KeycloakSession session, ClientModel client) {
        KeycloakContext context = session.getContext();
        Token bearerToken = context.getBearerToken();

        if (bearerToken != null && TokenCategory.ACCESS.equals(bearerToken.getCategory())) {
            return AccessToken.class.cast(bearerToken).getScope();
        }

        AuthenticationSessionModel authenticationSession = context.getAuthenticationSession();

        if (authenticationSession != null) {
            return authenticationSession.getClientNote(OIDCLoginProtocol.SCOPE_PARAM);
        }

        UserSessionModel userSession = context.getUserSession();

        if (userSession == null) {
            return null;
        }

        Map<String, AuthenticatedClientSessionModel> clientSessions = userSession.getAuthenticatedClientSessions();

        return clientSessions.values().stream().filter(c -> c.getClient().equals(client))
                .map((c) -> c.getNote(OIDCLoginProtocol.SCOPE_PARAM))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }
}
