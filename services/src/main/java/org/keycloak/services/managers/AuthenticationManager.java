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

import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.OAuth2Constants;
import org.keycloak.TokenVerifier;
import org.keycloak.TokenVerifier.Predicate;
import org.keycloak.TokenVerifier.TokenTypeCheck;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.AuthenticationFlowException;
import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.authentication.ConsoleDisplayMode;
import org.keycloak.authentication.DisplayTypeRequiredActionFactory;
import org.keycloak.authentication.InitiatedActionSupport;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionContextResult;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.authentication.actiontoken.DefaultActionTokenKey;
import org.keycloak.broker.provider.IdentityProvider;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.VerificationException;
import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.Time;
import org.keycloak.crypto.SignatureProvider;
import org.keycloak.crypto.SignatureVerifierContext;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.ActionTokenKeyModel;
import org.keycloak.models.ActionTokenStoreProvider;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredActionProviderModel;
import org.keycloak.models.UserConsentModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.SessionTimeoutHelper;
import org.keycloak.models.utils.SystemClientUtil;
import org.keycloak.protocol.LoginProtocol;
import org.keycloak.protocol.LoginProtocol.Error;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.Urls;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.resources.IdentityBrokerService;
import org.keycloak.services.resources.LoginActionsService;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.services.util.CookieHelper;
import org.keycloak.services.util.P3PHelper;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.sessions.CommonClientSessionModel;
import org.keycloak.sessions.RootAuthenticationSessionModel;
import org.keycloak.util.TokenUtil;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.keycloak.common.util.ServerCookie.SameSiteAttributeValue;
import static org.keycloak.services.util.CookieHelper.getCookie;

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
     * Auth session note on client logout state (when logging out)
     */
    public static final String CLIENT_LOGOUT_STATE = "logout.state.";

    // userSession note with authTime (time when authentication flow including requiredActions was finished)
    public static final String AUTH_TIME = "AUTH_TIME";
    // clientSession note with flag that clientSession was authenticated through SSO cookie
    public static final String SSO_AUTH = "SSO_AUTH";

    protected static final Logger logger = Logger.getLogger(AuthenticationManager.class);

    public static final String FORM_USERNAME = "username";
    // used for auth login
    public static final String KEYCLOAK_IDENTITY_COOKIE = "KEYCLOAK_IDENTITY";
    // used solely to determine is user is logged in
    public static final String KEYCLOAK_SESSION_COOKIE = "KEYCLOAK_SESSION";
    public static final String KEYCLOAK_REMEMBER_ME = "KEYCLOAK_REMEMBER_ME";
    public static final String KEYCLOAK_LOGOUT_PROTOCOL = "KEYCLOAK_LOGOUT_PROTOCOL";
    private static final TokenTypeCheck VALIDATE_IDENTITY_COOKIE = new TokenTypeCheck(TokenUtil.TOKEN_TYPE_KEYCLOAK_ID);

    public static boolean isSessionValid(RealmModel realm, UserSessionModel userSession) {
        if (userSession == null) {
            logger.debug("No user session");
            return false;
        }
        int currentTime = Time.currentTime();

        // Additional time window is added for the case when session was updated in different DC and the update to current DC was postponed
        int maxIdle = userSession.isRememberMe() && realm.getSsoSessionIdleTimeoutRememberMe() > 0 ?
            realm.getSsoSessionIdleTimeoutRememberMe() : realm.getSsoSessionIdleTimeout();
        int maxLifespan = userSession.isRememberMe() && realm.getSsoSessionMaxLifespanRememberMe() > 0 ?
                realm.getSsoSessionMaxLifespanRememberMe() : realm.getSsoSessionMaxLifespan();

        boolean sessionIdleOk = maxIdle > currentTime - userSession.getLastSessionRefresh() - SessionTimeoutHelper.IDLE_TIMEOUT_WINDOW_SECONDS;
        boolean sessionMaxOk = maxLifespan > currentTime - userSession.getStarted();
        return sessionIdleOk && sessionMaxOk;
    }

    public static boolean isOfflineSessionValid(RealmModel realm, UserSessionModel userSession) {
        if (userSession == null) {
            logger.debug("No offline user session");
            return false;
        }
        int currentTime = Time.currentTime();
        // Additional time window is added for the case when session was updated in different DC and the update to current DC was postponed
        int maxIdle = realm.getOfflineSessionIdleTimeout() + SessionTimeoutHelper.IDLE_TIMEOUT_WINDOW_SECONDS;

        // KEYCLOAK-7688 Offline Session Max for Offline Token
        if (realm.isOfflineSessionMaxLifespanEnabled()) {
            int max = userSession.getStarted() + realm.getOfflineSessionMaxLifespan();
            return userSession.getLastSessionRefresh() + maxIdle > currentTime && max > currentTime;
        } else {
            return userSession.getLastSessionRefresh() + maxIdle > currentTime;
        }
    }

    public static void expireUserSessionCookie(KeycloakSession session, UserSessionModel userSession, RealmModel realm, UriInfo uriInfo, HttpHeaders headers, ClientConnection connection) {
        try {
            // check to see if any identity cookie is set with the same session and expire it if necessary
            Cookie cookie = CookieHelper.getCookie(headers.getCookies(), KEYCLOAK_IDENTITY_COOKIE);
            if (cookie == null) return;
            String tokenString = cookie.getValue();

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
            if (cookieSession == null || !cookieSession.getId().equals(userSession.getId())) return;
            expireIdentityCookie(realm, uriInfo, connection);
        } catch (Exception e) {
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

    public static void backchannelLogout(KeycloakSession session, RealmModel realm,
                                         UserSessionModel userSession, UriInfo uriInfo,
                                         ClientConnection connection, HttpHeaders headers,
                                         boolean logoutBroker) {
        backchannelLogout(session, realm, userSession, uriInfo, connection, headers, logoutBroker, false);
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
     */
    public static void backchannelLogout(KeycloakSession session, RealmModel realm,
                                         UserSessionModel userSession, UriInfo uriInfo,
                                         ClientConnection connection, HttpHeaders headers,
                                         boolean logoutBroker,
                                         boolean offlineSession) {
        if (userSession == null) return;
        UserModel user = userSession.getUser();
        if (userSession.getState() != UserSessionModel.State.LOGGING_OUT) {
            userSession.setState(UserSessionModel.State.LOGGING_OUT);
        }

        logger.debugv("Logging out: {0} ({1}) offline: {2}", user.getUsername(), userSession.getId(), userSession.isOffline());
        expireUserSessionCookie(session, userSession, realm, uriInfo, headers, connection);

        final AuthenticationSessionManager asm = new AuthenticationSessionManager(session);
        AuthenticationSessionModel logoutAuthSession = createOrJoinLogoutSession(session, realm, asm, userSession, false);

        try {
            backchannelLogoutAll(session, realm, userSession, logoutAuthSession, uriInfo, headers, logoutBroker);
            checkUserSessionOnlyHasLoggedOutClients(realm, userSession, logoutAuthSession);
        } finally {
            RootAuthenticationSessionModel rootAuthSession = logoutAuthSession.getParentSession();
            rootAuthSession.removeAuthenticationSessionByTabId(logoutAuthSession.getTabId());
        }

        userSession.setState(UserSessionModel.State.LOGGED_OUT);

        if (offlineSession) {
            new UserSessionManager(session).revokeOfflineUserSession(userSession);

            // Check if "online" session still exists and remove it too
            UserSessionModel onlineUserSession = session.sessions().getUserSession(realm, userSession.getId());
            if (onlineUserSession != null) {
                session.sessions().removeUserSession(realm, onlineUserSession);
            }
        } else {
            session.sessions().removeUserSession(realm, userSession);
        }
    }

    private static AuthenticationSessionModel createOrJoinLogoutSession(KeycloakSession session, RealmModel realm, final AuthenticationSessionManager asm, UserSessionModel userSession, boolean browserCookie) {
        // Account management client is used as a placeholder
        ClientModel client = SystemClientUtil.getSystemClient(realm);

        String authSessionId;
        RootAuthenticationSessionModel rootLogoutSession = null;
        boolean browserCookiePresent = false;

        // Try to lookup current authSessionId from browser cookie. If doesn't exists, use the same as current userSession
        if (browserCookie) {
            rootLogoutSession = asm.getCurrentRootAuthenticationSession(realm);
        }
        if (rootLogoutSession != null) {
            authSessionId = rootLogoutSession.getId();
            browserCookiePresent = true;
        } else {
            authSessionId = userSession.getId();
            rootLogoutSession = session.authenticationSessions().getRootAuthenticationSession(realm, authSessionId);
        }

        if (rootLogoutSession == null) {
            rootLogoutSession = session.authenticationSessions().createRootAuthenticationSession(authSessionId, realm);
        }
        if (browserCookie && !browserCookiePresent) {
            // Update cookie if needed
            asm.setAuthSessionCookie(authSessionId, realm);
        }

        // See if we have logoutAuthSession inside current rootSession. Create new if not
        Optional<AuthenticationSessionModel> found = rootLogoutSession.getAuthenticationSessions().values().stream().filter((AuthenticationSessionModel authSession) -> {
            return client.equals(authSession.getClient()) && Objects.equals(AuthenticationSessionModel.Action.LOGGING_OUT.name(), authSession.getAction());

        }).findFirst();

        AuthenticationSessionModel logoutAuthSession = found.isPresent() ? found.get() : rootLogoutSession.createAuthenticationSession(client);
        session.getContext().setAuthenticationSession(logoutAuthSession);

        logoutAuthSession.setAction(AuthenticationSessionModel.Action.LOGGING_OUT.name());
        return logoutAuthSession;
    }

    private static void backchannelLogoutAll(KeycloakSession session, RealmModel realm,
      UserSessionModel userSession, AuthenticationSessionModel logoutAuthSession, UriInfo uriInfo,
      HttpHeaders headers, boolean logoutBroker) {
        userSession.getAuthenticatedClientSessions().values().forEach(
          clientSession -> backchannelLogoutClientSession(session, realm, clientSession, logoutAuthSession, uriInfo, headers)
        );
        if (logoutBroker) {
            String brokerId = userSession.getNote(Details.IDENTITY_PROVIDER);
            if (brokerId != null) {
                IdentityProvider identityProvider = IdentityBrokerService.getIdentityProvider(session, realm, brokerId);
                try {
                    identityProvider.backchannelLogout(session, userSession, uriInfo, realm);
                } catch (Exception e) {
                    logger.warn("Exception at broker backchannel logout for broker " + brokerId, e);
                }
            }
        }
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
          .filter(me -> Objects.nonNull(me.getValue().getProtocol()))   // Keycloak service-like accounts
          .map(Map.Entry::getValue)
          .collect(Collectors.toSet());

        boolean allClientsLoggedOut = notLoggedOutSessions.isEmpty();

        if (! allClientsLoggedOut) {
            logger.warnf("Some clients have been not been logged out for user %s in %s realm: %s",
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
     * @param session
     * @param realm
     * @param clientSession
     * @param logoutAuthSession auth session used for recording result of logout. May be {@code null}
     * @param uriInfo
     * @param headers
     * @return {@code true} if the client was or is already being logged out, {@code false} if logout failed or it is not known how to log it out.
     */
    private static boolean backchannelLogoutClientSession(KeycloakSession session, RealmModel realm,
      AuthenticatedClientSessionModel clientSession, AuthenticationSessionModel logoutAuthSession,
      UriInfo uriInfo, HttpHeaders headers) {
        UserSessionModel userSession = clientSession.getUserSession();
        ClientModel client = clientSession.getClient();

        if (client.isFrontchannelLogout() || AuthenticationSessionModel.Action.LOGGED_OUT.name().equals(clientSession.getAction())) {
            return false;
        }

        final AuthenticationSessionModel.Action logoutState = getClientLogoutAction(logoutAuthSession, client.getId());

        if (logoutState == AuthenticationSessionModel.Action.LOGGED_OUT || logoutState == AuthenticationSessionModel.Action.LOGGING_OUT) {
            return true;
        }

        try {
            setClientLogoutAction(logoutAuthSession, client.getId(), AuthenticationSessionModel.Action.LOGGING_OUT);

            String authMethod = clientSession.getProtocol();
            if (authMethod == null) return true; // must be a keycloak service like account

            logger.debugv("backchannel logout to: {0}", client.getClientId());
            LoginProtocol protocol = session.getProvider(LoginProtocol.class, authMethod);
            protocol.setRealm(realm)
                    .setHttpHeaders(headers)
                    .setUriInfo(uriInfo);
            protocol.backchannelLogout(userSession, clientSession);

            setClientLogoutAction(logoutAuthSession, client.getId(), AuthenticationSessionModel.Action.LOGGED_OUT);

            return true;
        } catch (Exception ex) {
            ServicesLogger.LOGGER.failedToLogoutClient(ex);
            return false;
        }
    }

    private static Response frontchannelLogoutClientSession(KeycloakSession session, RealmModel realm,
      AuthenticatedClientSessionModel clientSession, AuthenticationSessionModel logoutAuthSession,
      UriInfo uriInfo, HttpHeaders headers) {
        UserSessionModel userSession = clientSession.getUserSession();
        ClientModel client = clientSession.getClient();

        if (! client.isFrontchannelLogout() || AuthenticationSessionModel.Action.LOGGED_OUT.name().equals(clientSession.getAction())) {
            return null;
        }

        final AuthenticationSessionModel.Action logoutState = getClientLogoutAction(logoutAuthSession, client.getId());

        if (logoutState == AuthenticationSessionModel.Action.LOGGED_OUT || logoutState == AuthenticationSessionModel.Action.LOGGING_OUT) {
            return null;
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

                setClientLogoutAction(logoutAuthSession, client.getId(), AuthenticationSessionModel.Action.LOGGED_OUT);

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
    public static void setClientLogoutAction(AuthenticationSessionModel logoutAuthSession, String clientUuid, AuthenticationSessionModel.Action action) {
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
    public static AuthenticationSessionModel.Action getClientLogoutAction(AuthenticationSessionModel logoutAuthSession, String clientUuid) {
        if (logoutAuthSession == null || clientUuid == null) {
            return null;
        }

        String state = logoutAuthSession.getAuthNote(CLIENT_LOGOUT_STATE + clientUuid);
        return state == null ? null : AuthenticationSessionModel.Action.valueOf(state);
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
        List<UserSessionModel> userSessions = session.sessions().getUserSessions(realm, user);
        for (UserSessionModel userSession : userSessions) {
            AuthenticatedClientSessionModel clientSession = userSession.getAuthenticatedClientSessionByClient(client.getId());
            if (clientSession != null) {
                backchannelLogoutClientSession(session, realm, clientSession, null, uriInfo, headers);
                clientSession.setAction(AuthenticationSessionModel.Action.LOGGED_OUT.name());
                org.keycloak.protocol.oidc.TokenManager.dettachClientSession(session.sessions(), realm, clientSession);
            }
        }
    }

    public static Response browserLogout(KeycloakSession session,
                                         RealmModel realm,
                                         UserSessionModel userSession,
                                         UriInfo uriInfo,
                                         ClientConnection connection,
                                         HttpHeaders headers,
                                         String initiatingIdp) {
        if (userSession == null) return null;

        if (logger.isDebugEnabled()) {
            UserModel user = userSession.getUser();
            logger.debugv("Logging out: {0} ({1})", user.getUsername(), userSession.getId());
        }
        
        if (userSession.getState() != UserSessionModel.State.LOGGING_OUT) {
            userSession.setState(UserSessionModel.State.LOGGING_OUT);
        }

        final AuthenticationSessionManager asm = new AuthenticationSessionManager(session);
        AuthenticationSessionModel logoutAuthSession = createOrJoinLogoutSession(session, realm, asm, userSession, true);

        Response response = browserLogoutAllClients(userSession, session, realm, headers, uriInfo, logoutAuthSession);
        if (response != null) {
            return response;
        }

        String brokerId = userSession.getNote(Details.IDENTITY_PROVIDER);
        if (brokerId != null && !brokerId.equals(initiatingIdp)) {
            IdentityProvider identityProvider = IdentityBrokerService.getIdentityProvider(session, realm, brokerId);
            response = identityProvider.keycloakInitiatedBrowserLogout(session, userSession, uriInfo, realm);
            if (response != null) {
                return response;
            }
        }

        return finishBrowserLogout(session, realm, userSession, uriInfo, connection, headers);
    }

    private static Response browserLogoutAllClients(UserSessionModel userSession, KeycloakSession session, RealmModel realm, HttpHeaders headers, UriInfo uriInfo, AuthenticationSessionModel logoutAuthSession) {
        Map<Boolean, List<AuthenticatedClientSessionModel>> acss = userSession.getAuthenticatedClientSessions().values().stream()
          .filter(clientSession -> ! Objects.equals(AuthenticationSessionModel.Action.LOGGED_OUT.name(), clientSession.getAction()))
          .filter(clientSession -> clientSession.getProtocol() != null)
          .collect(Collectors.partitioningBy(clientSession -> clientSession.getClient().isFrontchannelLogout()));

        final List<AuthenticatedClientSessionModel> backendLogoutSessions = acss.get(false) == null ? Collections.emptyList() : acss.get(false);
        backendLogoutSessions.forEach(acs -> backchannelLogoutClientSession(session, realm, acs, logoutAuthSession, uriInfo, headers));

        final List<AuthenticatedClientSessionModel> redirectClients = acss.get(true) == null ? Collections.emptyList() : acss.get(true);
        for (AuthenticatedClientSessionModel nextRedirectClient : redirectClients) {
            Response response = frontchannelLogoutClientSession(session, realm, nextRedirectClient, logoutAuthSession, uriInfo, headers);
            if (response != null) {
                return response;
            }
        }

        return null;
    }

    public static Response finishBrowserLogout(KeycloakSession session, RealmModel realm, UserSessionModel userSession, UriInfo uriInfo, ClientConnection connection, HttpHeaders headers) {
        final AuthenticationSessionManager asm = new AuthenticationSessionManager(session);
        AuthenticationSessionModel logoutAuthSession = createOrJoinLogoutSession(session, realm, asm, userSession, true);

        checkUserSessionOnlyHasLoggedOutClients(realm, userSession, logoutAuthSession);

        expireIdentityCookie(realm, uriInfo, connection);
        expireRememberMeCookie(realm, uriInfo, connection);
        userSession.setState(UserSessionModel.State.LOGGED_OUT);
        String method = userSession.getNote(KEYCLOAK_LOGOUT_PROTOCOL);
        EventBuilder event = new EventBuilder(realm, session, connection);
        LoginProtocol protocol = session.getProvider(LoginProtocol.class, method);
        protocol.setRealm(realm)
                .setHttpHeaders(headers)
                .setUriInfo(uriInfo)
                .setEventBuilder(event);
        Response response = protocol.finishLogout(userSession);
        session.sessions().removeUserSession(realm, userSession);
        session.authenticationSessions().removeRootAuthenticationSession(realm, logoutAuthSession.getParentSession());
        return response;
    }


    public static IdentityCookieToken createIdentityToken(KeycloakSession keycloakSession, RealmModel realm, UserModel user, UserSessionModel session, String issuer) {
        IdentityCookieToken token = new IdentityCookieToken();
        token.id(KeycloakModelUtils.generateId());
        token.issuedNow();
        token.subject(user.getId());
        token.issuer(issuer);
        token.type(TokenUtil.TOKEN_TYPE_KEYCLOAK_ID);

        if (session != null) {
            token.setSessionState(session.getId());
        }

        if (session != null && session.isRememberMe() && realm.getSsoSessionMaxLifespanRememberMe() > 0) {
            token.expiration(Time.currentTime() + realm.getSsoSessionMaxLifespanRememberMe());
        } else if (realm.getSsoSessionMaxLifespan() > 0) {
            token.expiration(Time.currentTime() + realm.getSsoSessionMaxLifespan());
        }

        String stateChecker = (String) keycloakSession.getAttribute("state_checker");
        if (stateChecker == null) {
            stateChecker = Base64Url.encode(KeycloakModelUtils.generateSecret());
            keycloakSession.setAttribute("state_checker", stateChecker);
        }
        token.getOtherClaims().put("state_checker", stateChecker);

        return token;
    }

    public static void createLoginCookie(KeycloakSession keycloakSession, RealmModel realm, UserModel user, UserSessionModel session, UriInfo uriInfo, ClientConnection connection) {
        String cookiePath = getIdentityCookiePath(realm, uriInfo);
        String issuer = Urls.realmIssuer(uriInfo.getBaseUri(), realm.getName());
        IdentityCookieToken identityCookieToken = createIdentityToken(keycloakSession, realm, user, session, issuer);
        String encoded = keycloakSession.tokens().encode(identityCookieToken);
        boolean secureOnly = realm.getSslRequired().isRequired(connection);
        int maxAge = NewCookie.DEFAULT_MAX_AGE;
        if (session != null && session.isRememberMe()) {
            maxAge = realm.getSsoSessionMaxLifespanRememberMe() > 0 ? realm.getSsoSessionMaxLifespanRememberMe() : realm.getSsoSessionMaxLifespan();
        }
        logger.debugv("Create login cookie - name: {0}, path: {1}, max-age: {2}", KEYCLOAK_IDENTITY_COOKIE, cookiePath, maxAge);
        CookieHelper.addCookie(KEYCLOAK_IDENTITY_COOKIE, encoded, cookiePath, null, null, maxAge, secureOnly, true, SameSiteAttributeValue.NONE);
        //builder.cookie(new NewCookie(cookieName, encoded, cookiePath, null, null, maxAge, secureOnly));// todo httponly , true);

        String sessionCookieValue = realm.getName() + "/" + user.getId();
        if (session != null) {
            sessionCookieValue += "/" + session.getId();
        }
        // THIS SHOULD NOT BE A HTTPONLY COOKIE!  It is used for OpenID Connect Iframe Session support!
        // Max age should be set to the max lifespan of the session as it's used to invalidate old-sessions on re-login
        int sessionCookieMaxAge = session.isRememberMe() && realm.getSsoSessionMaxLifespanRememberMe() > 0 ? realm.getSsoSessionMaxLifespanRememberMe() : realm.getSsoSessionMaxLifespan();
        CookieHelper.addCookie(KEYCLOAK_SESSION_COOKIE, sessionCookieValue, cookiePath, null, null, sessionCookieMaxAge, secureOnly, false, SameSiteAttributeValue.NONE);
        P3PHelper.addP3PHeader();
    }

    public static void createRememberMeCookie(RealmModel realm, String username, UriInfo uriInfo, ClientConnection connection) {
        String path = getIdentityCookiePath(realm, uriInfo);
        boolean secureOnly = realm.getSslRequired().isRequired(connection);
        // remember me cookie should be persistent (hardcoded to 365 days for now)
        //NewCookie cookie = new NewCookie(KEYCLOAK_REMEMBER_ME, "true", path, null, null, realm.getCentralLoginLifespan(), secureOnly);// todo httponly , true);
        CookieHelper.addCookie(KEYCLOAK_REMEMBER_ME, "username:" + username, path, null, null, 31536000, secureOnly, true);
    }

    public static String getRememberMeUsername(RealmModel realm, HttpHeaders headers) {
        if (realm.isRememberMe()) {
            Cookie cookie = headers.getCookies().get(AuthenticationManager.KEYCLOAK_REMEMBER_ME);
            if (cookie != null) {
                String value = cookie.getValue();
                String[] s = value.split(":");
                if (s[0].equals("username") && s.length == 2) {
                    return s[1];
                }
            }
        }
        return null;
    }

    public static void expireIdentityCookie(RealmModel realm, UriInfo uriInfo, ClientConnection connection) {
        logger.debug("Expiring identity cookie");
        String path = getIdentityCookiePath(realm, uriInfo);
        expireCookie(realm, KEYCLOAK_IDENTITY_COOKIE, path, true, connection, SameSiteAttributeValue.NONE);
        expireCookie(realm, KEYCLOAK_SESSION_COOKIE, path, false, connection, SameSiteAttributeValue.NONE);

        String oldPath = getOldCookiePath(realm, uriInfo);
        expireCookie(realm, KEYCLOAK_IDENTITY_COOKIE, oldPath, true, connection, SameSiteAttributeValue.NONE);
        expireCookie(realm, KEYCLOAK_SESSION_COOKIE, oldPath, false, connection, SameSiteAttributeValue.NONE);
    }
    public static void expireOldIdentityCookie(RealmModel realm, UriInfo uriInfo, ClientConnection connection) {
        logger.debug("Expiring old identity cookie with wrong path");

        String oldPath = getOldCookiePath(realm, uriInfo);
        expireCookie(realm, KEYCLOAK_IDENTITY_COOKIE, oldPath, true, connection, SameSiteAttributeValue.NONE);
        expireCookie(realm, KEYCLOAK_SESSION_COOKIE, oldPath, false, connection, SameSiteAttributeValue.NONE);
    }


    public static void expireRememberMeCookie(RealmModel realm, UriInfo uriInfo, ClientConnection connection) {
        logger.debug("Expiring remember me cookie");
        String path = getIdentityCookiePath(realm, uriInfo);
        String cookieName = KEYCLOAK_REMEMBER_ME;
        expireCookie(realm, cookieName, path, true, connection, null);
    }

    public static void expireOldAuthSessionCookie(RealmModel realm, UriInfo uriInfo, ClientConnection connection) {
        logger.debugv("Expire {1} cookie .", AuthenticationSessionManager.AUTH_SESSION_ID);

        String oldPath = getOldCookiePath(realm, uriInfo);
        expireCookie(realm, AuthenticationSessionManager.AUTH_SESSION_ID, oldPath, true, connection, null);
    }

    protected static String getIdentityCookiePath(RealmModel realm, UriInfo uriInfo) {
        return getRealmCookiePath(realm, uriInfo);
    }

    public static String getRealmCookiePath(RealmModel realm, UriInfo uriInfo) {
        URI uri = RealmsResource.realmBaseUrl(uriInfo).build(realm.getName());
        // KEYCLOAK-5270
        return uri.getRawPath() + "/";
    }

    public static String getOldCookiePath(RealmModel realm, UriInfo uriInfo) {
        URI uri = RealmsResource.realmBaseUrl(uriInfo).build(realm.getName());
        return uri.getRawPath();
    }

    public static String getAccountCookiePath(RealmModel realm, UriInfo uriInfo) {
        URI uri = RealmsResource.accountUrl(uriInfo.getBaseUriBuilder()).build(realm.getName());
        return uri.getRawPath();
    }

    public static void expireCookie(RealmModel realm, String cookieName, String path, boolean httpOnly, ClientConnection connection, SameSiteAttributeValue sameSite) {
        logger.debugf("Expiring cookie: %s path: %s", cookieName, path);
        boolean secureOnly = realm.getSslRequired().isRequired(connection);;
        CookieHelper.addCookie(cookieName, "", path, null, "Expiring cookie", 0, secureOnly, httpOnly, sameSite);
    }

    public AuthResult authenticateIdentityCookie(KeycloakSession session, RealmModel realm) {
        return authenticateIdentityCookie(session, realm, true);
    }

    public static AuthResult authenticateIdentityCookie(KeycloakSession session, RealmModel realm, boolean checkActive) {
        Cookie cookie = CookieHelper.getCookie(session.getContext().getRequestHeaders().getCookies(), KEYCLOAK_IDENTITY_COOKIE);
        if (cookie == null || "".equals(cookie.getValue())) {
            logger.debugv("Could not find cookie: {0}", KEYCLOAK_IDENTITY_COOKIE);
            return null;
        }

        String tokenString = cookie.getValue();
        AuthResult authResult = verifyIdentityToken(session, realm, session.getContext().getUri(), session.getContext().getConnection(), checkActive, false, true, tokenString, session.getContext().getRequestHeaders(), VALIDATE_IDENTITY_COOKIE);
        if (authResult == null) {
            expireIdentityCookie(realm, session.getContext().getUri(), session.getContext().getConnection());
            expireOldIdentityCookie(realm, session.getContext().getUri(), session.getContext().getConnection());
            return null;
        }
        authResult.getSession().setLastSessionRefresh(Time.currentTime());
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
        Cookie sessionCookie = getCookie(request.getHttpHeaders().getCookies(), AuthenticationManager.KEYCLOAK_SESSION_COOKIE);
        if (sessionCookie != null) {

            String[] split = sessionCookie.getValue().split("/");
            if (split.length >= 3) {
                String oldSessionId = split[2];
                if (!oldSessionId.equals(userSession.getId())) {
                    UserSessionModel oldSession = session.sessions().getUserSession(realm, oldSessionId);
                    if (oldSession != null) {
                        logger.debugv("Removing old user session: session: {0}", oldSessionId);
                        session.sessions().removeUserSession(realm, oldSession);
                    }
                }
            }
        }

        // Updates users locale if required
        session.getContext().resolveLocale(userSession.getUser());

        // refresh the cookies!
        createLoginCookie(session, realm, userSession.getUser(), userSession, uriInfo, clientConnection);
        if (userSession.getState() != UserSessionModel.State.LOGGED_IN) userSession.setState(UserSessionModel.State.LOGGED_IN);
        if (userSession.isRememberMe()) {
            createRememberMeCookie(realm, userSession.getLoginUsername(), uriInfo, clientConnection);
        } else {
            expireRememberMeCookie(realm, uriInfo, clientConnection);
        }

        AuthenticatedClientSessionModel clientSession = clientSessionCtx.getClientSession();

        // Update userSession note with authTime. But just if flag SSO_AUTH is not set
        boolean isSSOAuthentication = "true".equals(session.getAttribute(SSO_AUTH));
        if (isSSOAuthentication) {
            clientSession.setNote(SSO_AUTH, "true");
        } else {
            int authTime = Time.currentTime();
            userSession.setNote(AUTH_TIME, String.valueOf(authTime));
            clientSession.removeNote(SSO_AUTH);
        }

        return protocol.authenticated(authSession, userSession, clientSessionCtx);

    }

    public static String getSessionIdFromSessionCookie(KeycloakSession session) {
        Cookie cookie = getCookie(session.getContext().getRequestHeaders().getCookies(), KEYCLOAK_SESSION_COOKIE);
        if (cookie == null || "".equals(cookie.getValue())) {
            logger.debugv("Could not find cookie: {0}", KEYCLOAK_SESSION_COOKIE);
            return null;
        }

        String[] parts = cookie.getValue().split("/", 3);
        if (parts.length != 3) {
            logger.debugv("Cannot parse session value from: {0}", KEYCLOAK_SESSION_COOKIE);
            return null;
        }
        return parts[2];
    }

    public static boolean isSSOAuthentication(AuthenticatedClientSessionModel clientSession) {
        String ssoAuth = clientSession.getNote(SSO_AUTH);
        return Boolean.parseBoolean(ssoAuth);
    }


    public static Response nextActionAfterAuthentication(KeycloakSession session, AuthenticationSessionModel authSession,
                                                  ClientConnection clientConnection,
                                                  HttpRequest request, UriInfo uriInfo, EventBuilder event) {
        Response requiredAction = actionRequired(session, authSession, clientConnection, request, uriInfo, event);
        if (requiredAction != null) return requiredAction;
        return finishedRequiredActions(session, authSession, null, clientConnection, request, uriInfo, event);

    }


    public static Response redirectToRequiredActions(KeycloakSession session, RealmModel realm, AuthenticationSessionModel authSession, UriInfo uriInfo, String requiredAction) {
        // redirect to non-action url so browser refresh button works without reposting past data
        ClientSessionCode<AuthenticationSessionModel> accessCode = new ClientSessionCode<>(session, realm, authSession);
        accessCode.setAction(AuthenticationSessionModel.Action.REQUIRED_ACTIONS.name());
        authSession.setAuthNote(AuthenticationProcessor.CURRENT_FLOW_PATH, LoginActionsService.REQUIRED_ACTION);
        authSession.setAuthNote(AuthenticationProcessor.CURRENT_AUTHENTICATION_EXECUTION, requiredAction);

        UriBuilder uriBuilder = LoginActionsService.loginActionsBaseUrl(uriInfo)
                .path(LoginActionsService.REQUIRED_ACTION);

        if (requiredAction != null) {
            uriBuilder.queryParam(Constants.EXECUTION, requiredAction);
        }

        uriBuilder.queryParam(Constants.CLIENT_ID, authSession.getClient().getClientId());
        uriBuilder.queryParam(Constants.TAB_ID, authSession.getTabId());

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
            ActionTokenKeyModel actionTokenKey = DefaultActionTokenKey.from(actionTokenKeyToInvalidate);
            
            if (actionTokenKey != null) {
                ActionTokenStoreProvider actionTokenStore = session.getProvider(ActionTokenStoreProvider.class);
                actionTokenStore.put(actionTokenKey, null); // Token is invalidated
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
                infoPage.setAttribute(Constants.SKIP_LINK, true);
            }
            Response response = infoPage
                    .createInfoPage();

            new AuthenticationSessionManager(session).removeAuthenticationSession(authSession.getRealm(), authSession, true);

            return response;
        }
        RealmModel realm = authSession.getRealm();

        ClientSessionContext clientSessionCtx = AuthenticationProcessor.attachSession(authSession, userSession, session, realm, clientConnection, event);
        userSession = clientSessionCtx.getClientSession().getUserSession();

        event.event(EventType.LOGIN);
        event.session(userSession);
        event.success();
        return redirectAfterSuccessfulFlow(session, realm, userSession, clientSessionCtx, request, uriInfo, clientConnection, event, authSession);
    }

    // Return null if action is not required. Or the name of the requiredAction in case it is required.
    public static String nextRequiredAction(final KeycloakSession session, final AuthenticationSessionModel authSession,
                                            final ClientConnection clientConnection,
                                            final HttpRequest request, final UriInfo uriInfo, final EventBuilder event) {
        final RealmModel realm = authSession.getRealm();
        final UserModel user = authSession.getAuthenticatedUser();
        final ClientModel client = authSession.getClient();

        evaluateRequiredActionTriggers(session, authSession, clientConnection, request, uriInfo, event, realm, user);

        if (!user.getRequiredActions().isEmpty()) {
            return user.getRequiredActions().iterator().next();
        }
        if (!authSession.getRequiredActions().isEmpty()) {
            return authSession.getRequiredActions().iterator().next();
        }

        String kcAction = authSession.getClientNote(Constants.KC_ACTION);
        if (kcAction != null) {
            return kcAction;
        }

        if (client.isConsentRequired()) {

            UserConsentModel grantedConsent = getEffectiveGrantedConsent(session, authSession);

            // See if any clientScopes need to be approved on consent screen
            List<ClientScopeModel> clientScopesToApprove = getClientScopesToApproveOnConsentScreen(realm, grantedConsent, authSession);
            if (!clientScopesToApprove.isEmpty()) {
                return CommonClientSessionModel.Action.OAUTH_GRANT.name();
            }

            String consentDetail = (grantedConsent != null) ? Details.CONSENT_VALUE_PERSISTED_CONSENT : Details.CONSENT_VALUE_NO_CONSENT_REQUIRED;
            event.detail(Details.CONSENT, consentDetail);
        } else {
            event.detail(Details.CONSENT, Details.CONSENT_VALUE_NO_CONSENT_REQUIRED);
        }
        return null;

    }


    private static UserConsentModel getEffectiveGrantedConsent(KeycloakSession session, AuthenticationSessionModel authSession) {
        // If prompt=consent, we ignore existing persistent consent
        String prompt = authSession.getClientNote(OIDCLoginProtocol.PROMPT_PARAM);
        if (TokenUtil.hasPrompt(prompt, OIDCLoginProtocol.PROMPT_VALUE_CONSENT)) {
            return null;
        } else {
            final RealmModel realm = authSession.getRealm();
            final UserModel user = authSession.getAuthenticatedUser();
            final ClientModel client = authSession.getClient();

            return session.users().getConsentByClient(realm, user.getId(), client.getId());
        }
    }


    public static Response actionRequired(final KeycloakSession session, final AuthenticationSessionModel authSession,
                                                         final ClientConnection clientConnection,
                                                         final HttpRequest request, final UriInfo uriInfo, final EventBuilder event) {
        final RealmModel realm = authSession.getRealm();
        final UserModel user = authSession.getAuthenticatedUser();
        final ClientModel client = authSession.getClient();

        evaluateRequiredActionTriggers(session, authSession, clientConnection, request, uriInfo, event, realm, user);


        logger.debugv("processAccessCode: go to oauth page?: {0}", client.isConsentRequired());

        event.detail(Details.CODE_ID, authSession.getParentSession().getId());

        Set<String> requiredActions = user.getRequiredActions();
        Response action = executionActions(session, authSession, request, event, realm, user, requiredActions);
        if (action != null) return action;

        // executionActions() method should remove any duplicate actions that might be in the clientSession
        requiredActions = authSession.getRequiredActions();
        action = executionActions(session, authSession, request, event, realm, user, requiredActions);
        if (action != null) return action;

        if (client.isConsentRequired()) {

            UserConsentModel grantedConsent = getEffectiveGrantedConsent(session, authSession);

            List<ClientScopeModel> clientScopesToApprove = getClientScopesToApproveOnConsentScreen(realm, grantedConsent, authSession);

            // Skip grant screen if everything was already approved by this user
            if (clientScopesToApprove.size() > 0) {
                String execution = AuthenticatedClientSessionModel.Action.OAUTH_GRANT.name();

                ClientSessionCode<AuthenticationSessionModel> accessCode = new ClientSessionCode<>(session, realm, authSession);
                accessCode.setAction(AuthenticatedClientSessionModel.Action.REQUIRED_ACTIONS.name());
                authSession.setAuthNote(AuthenticationProcessor.CURRENT_AUTHENTICATION_EXECUTION, execution);

                return session.getProvider(LoginFormsProvider.class)
                        .setAuthenticationSession(authSession)
                        .setExecution(execution)
                        .setClientSessionCode(accessCode.getOrGenerateCode())
                        .setAccessRequest(clientScopesToApprove)
                        .createOAuthGrant();
            } else {
                String consentDetail = (grantedConsent != null) ? Details.CONSENT_VALUE_PERSISTED_CONSENT : Details.CONSENT_VALUE_NO_CONSENT_REQUIRED;
                event.detail(Details.CONSENT, consentDetail);
            }
        } else {
            event.detail(Details.CONSENT, Details.CONSENT_VALUE_NO_CONSENT_REQUIRED);
        }
        return null;

    }

    private static List<ClientScopeModel> getClientScopesToApproveOnConsentScreen(RealmModel realm, UserConsentModel grantedConsent,
                                                                                  AuthenticationSessionModel authSession) {
        // Client Scopes to be displayed on consent screen
        List<ClientScopeModel> clientScopesToDisplay = new LinkedList<>();

        for (String clientScopeId : authSession.getClientScopes()) {
            ClientScopeModel clientScope = KeycloakModelUtils.findClientScopeById(realm, authSession.getClient(), clientScopeId);

            if (clientScope == null || !clientScope.isDisplayOnConsentScreen()) {
                continue;
            }

            // Check if consent already granted by user
            if (grantedConsent == null || !grantedConsent.isClientScopeGranted(clientScope)) {
                clientScopesToDisplay.add(clientScope);
            }
        }

        return clientScopesToDisplay;
    }


    public static void setClientScopesInSession(AuthenticationSessionModel authSession) {
        ClientModel client = authSession.getClient();
        UserModel user = authSession.getAuthenticatedUser();

        // todo scope param protocol independent
        String scopeParam = authSession.getClientNote(OAuth2Constants.SCOPE);

        Set<String> requestedClientScopes = new HashSet<String>();
        for (ClientScopeModel clientScope : org.keycloak.protocol.oidc.TokenManager.getRequestedClientScopes(scopeParam, client)) {
            requestedClientScopes.add(clientScope.getId());
        }
        authSession.setClientScopes(requestedClientScopes);
    }

    public static RequiredActionProvider createRequiredAction(RequiredActionContextResult context) {
        String display = context.getAuthenticationSession().getAuthNote(OAuth2Constants.DISPLAY);
        if (display == null) return context.getFactory().create(context.getSession());


        if (context.getFactory() instanceof DisplayTypeRequiredActionFactory) {
            RequiredActionProvider provider = ((DisplayTypeRequiredActionFactory)context.getFactory()).createDisplay(context.getSession(), display);
            if (provider != null) return provider;
        }
        // todo create a provider for handling lack of display support
        if (OAuth2Constants.DISPLAY_CONSOLE.equalsIgnoreCase(display)) {
            context.getAuthenticationSession().removeAuthNote(OAuth2Constants.DISPLAY);
            throw new AuthenticationFlowException(AuthenticationFlowError.DISPLAY_NOT_SUPPORTED, ConsoleDisplayMode.browserContinue(context.getSession(), context.getUriInfo().getRequestUri().toString()));

        } else {
            return context.getFactory().create(context.getSession());
        }
    }


    protected static Response executionActions(KeycloakSession session, AuthenticationSessionModel authSession,
                                               HttpRequest request, EventBuilder event, RealmModel realm, UserModel user,
                                               Set<String> requiredActions) {

        List<RequiredActionProviderModel> sortedRequiredActions = sortRequiredActionsByPriority(realm, requiredActions);

        for (RequiredActionProviderModel model : sortedRequiredActions) {
            Response response = executeAction(session, authSession, model, request, event, realm, user, false);
            if (response != null) {
                return response;
            }
        }

        String kcAction = authSession.getClientNote(Constants.KC_ACTION);
        if (kcAction != null) {
            for (RequiredActionProviderModel m : realm.getRequiredActionProviders()) {
                if (m.getProviderId().equals(kcAction)) {
                    return executeAction(session, authSession, m, request, event, realm, user, true);
                }
            }

            logger.debugv("Requested action {0} not configured for realm", kcAction);
            setKcActionStatus(kcAction, RequiredActionContext.KcActionStatus.ERROR, authSession);
        }

        return null;
    }

    private static Response executeAction(KeycloakSession session, AuthenticationSessionModel authSession, RequiredActionProviderModel model,
                                          HttpRequest request, EventBuilder event, RealmModel realm, UserModel user, boolean kcActionExecution) {
        RequiredActionFactory factory = (RequiredActionFactory) session.getKeycloakSessionFactory().getProviderFactory(RequiredActionProvider.class, model.getProviderId());
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
                return null;
            } else if (!model.isEnabled()) {
                logger.debugv("Requested action {0} is disabled and can't be invoked with kc_action", factory.getId());
                setKcActionStatus(factory.getId(), RequiredActionContext.KcActionStatus.ERROR, authSession);
                return null;
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
            Response response = protocol.sendError(context.getAuthenticationSession(), Error.CONSENT_DENIED);
            event.error(Errors.REJECTED_BY_USER);
            return response;
        }
        else if (context.getStatus() == RequiredActionContext.Status.CHALLENGE) {
            authSession.setAuthNote(AuthenticationProcessor.CURRENT_AUTHENTICATION_EXECUTION, model.getProviderId());
            return context.getChallenge();
        }
        else if (context.getStatus() == RequiredActionContext.Status.SUCCESS) {
            event.clone().event(EventType.CUSTOM_REQUIRED_ACTION).detail(Details.CUSTOM_REQUIRED_ACTION, factory.getId()).success();
            // don't have to perform the same action twice, so remove it from both the user and session required actions
            authSession.getAuthenticatedUser().removeRequiredAction(factory.getId());
            authSession.removeRequiredAction(factory.getId());
            setKcActionStatus(factory.getId(), RequiredActionContext.KcActionStatus.SUCCESS, authSession);
        }

        return null;
    }

    private static List<RequiredActionProviderModel> sortRequiredActionsByPriority(RealmModel realm, Set<String> requiredActions) {
        List<RequiredActionProviderModel> actions = new ArrayList<>();
        for (String action : requiredActions) {
            RequiredActionProviderModel model = realm.getRequiredActionProviderByAlias(action);
            if (model == null) {
                logger.warnv("Could not find configuration for Required Action {0}, did you forget to register it?", action);
                continue;
            }
            if (!model.isEnabled()) {
                continue;
            }
            actions.add(model);
        }
        Collections.sort(actions, RequiredActionProviderModel.RequiredActionComparator.SINGLETON);
        return actions;
    }
    
    public static void evaluateRequiredActionTriggers(final KeycloakSession session, final AuthenticationSessionModel authSession, final ClientConnection clientConnection, final HttpRequest request, final UriInfo uriInfo, final EventBuilder event, final RealmModel realm, final UserModel user) {

        // see if any required actions need triggering, i.e. an expired password
        for (RequiredActionProviderModel model : realm.getRequiredActionProviders()) {
            if (!model.isEnabled()) continue;
            RequiredActionFactory factory = (RequiredActionFactory)session.getKeycloakSessionFactory().getProviderFactory(RequiredActionProvider.class, model.getProviderId());
            if (factory == null) {
                throw new RuntimeException("Unable to find factory for Required Action: " + model.getProviderId() + " did you forget to declare it in a META-INF/services file?");
            }
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
                public void ignore() {
                    throw new RuntimeException("Not allowed to call ignore() within evaluateTriggers()");
                }
            };

            provider.evaluateTriggers(result);
        }
    }

    public static AuthResult verifyIdentityToken(KeycloakSession session, RealmModel realm, UriInfo uriInfo, ClientConnection connection, boolean checkActive, boolean checkTokenType,
                                                    boolean isCookie, String tokenString, HttpHeaders headers, Predicate<? super AccessToken>... additionalChecks) {
        try {
            TokenVerifier<AccessToken> verifier = TokenVerifier.create(tokenString, AccessToken.class)
              .withDefaultChecks()
              .realmUrl(Urls.realmIssuer(uriInfo.getBaseUri(), realm.getName()))
              .checkActive(checkActive)
              .checkTokenType(checkTokenType)
              .withChecks(additionalChecks);
            String kid = verifier.getHeader().getKeyId();
            String algorithm = verifier.getHeader().getAlgorithm().name();

            SignatureVerifierContext signatureVerifier = session.getProvider(SignatureProvider.class, algorithm).verifier(kid);
            verifier.verifierContext(signatureVerifier);

            AccessToken token = verifier.verify().getToken();
            if (checkActive) {
                if (!token.isActive() || token.getIssuedAt() < realm.getNotBefore()) {
                    logger.debug("Identity cookie expired");
                    return null;
                }
            }

            UserSessionModel userSession = session.sessions().getUserSession(realm, token.getSessionState());
            UserModel user = null;
            if (userSession != null) {
                user = userSession.getUser();
                if (user == null || !user.isEnabled()) {
                    logger.debug("Unknown user in identity token");
                    return null;
                }

                int userNotBefore = session.users().getNotBeforeOfUser(realm, user);
                if (token.getIssuedAt() < userNotBefore) {
                    logger.debug("User notBefore newer than token");
                    return null;
                }
            }

            if (!isSessionValid(realm, userSession)) {
                // Check if accessToken was for the offline session.
                if (!isCookie) {
                    UserSessionModel offlineUserSession = session.sessions().getOfflineUserSession(realm, token.getSessionState());
                    if (isOfflineSessionValid(realm, offlineUserSession)) {
                        user = offlineUserSession.getUser();
                        return new AuthResult(user, offlineUserSession, token);
                    }
                }

                if (userSession != null) backchannelLogout(session, realm, userSession, uriInfo, connection, headers, true);
                logger.debug("User session not active");
                return null;
            }

            session.setAttribute("state_checker", token.getOtherClaims().get("state_checker"));

            return new AuthResult(user, userSession, token);
        } catch (VerificationException e) {
            logger.debugf("Failed to verify identity token: %s", e.getMessage());
        }
        return null;
    }

    public enum AuthenticationStatus {
        SUCCESS, ACCOUNT_TEMPORARILY_DISABLED, ACCOUNT_DISABLED, ACTIONS_REQUIRED, INVALID_USER, INVALID_CREDENTIALS, MISSING_PASSWORD, MISSING_TOTP, FAILED
    }

    public static class AuthResult {
        private final UserModel user;
        private final UserSessionModel session;
        private final AccessToken token;

        public AuthResult(UserModel user, UserSessionModel session, AccessToken token) {
            this.user = user;
            this.session = session;
            this.token = token;
        }

        public UserSessionModel getSession() {
            return session;
        }

        public UserModel getUser() {
            return user;
        }

        public AccessToken getToken() {
            return token;
        }
    }

    public static void setKcActionStatus(String executedProviderId, RequiredActionContext.KcActionStatus status, AuthenticationSessionModel authSession) {
        if (executedProviderId.equals(authSession.getClientNote(Constants.KC_ACTION))) {
            authSession.setClientNote(Constants.KC_ACTION_STATUS, status.name().toLowerCase());
            authSession.removeClientNote(Constants.KC_ACTION);
            authSession.removeClientNote(Constants.KC_ACTION_EXECUTING);
        }
    }

}
