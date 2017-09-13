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
import org.jboss.resteasy.specimpl.MultivaluedMapImpl;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.OAuth2Constants;
import org.keycloak.TokenVerifier;
import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionContextResult;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.authentication.actiontoken.DefaultActionTokenKey;
import org.keycloak.broker.provider.IdentityProvider;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.VerificationException;
import org.keycloak.common.util.Time;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.jose.jws.AlgorithmType;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.models.*;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.LoginProtocol;
import org.keycloak.protocol.LoginProtocol.Error;
import org.keycloak.protocol.oidc.TokenManager;
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

import javax.crypto.SecretKey;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.security.PublicKey;
import java.util.*;
import java.util.stream.Collectors;

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

    public static boolean isSessionValid(RealmModel realm, UserSessionModel userSession) {
        if (userSession == null) {
            logger.debug("No user session");
            return false;
        }
        int currentTime = Time.currentTime();
        int max = userSession.getStarted() + realm.getSsoSessionMaxLifespan();
        return userSession.getLastSessionRefresh() + realm.getSsoSessionIdleTimeout() > currentTime && max > currentTime;
    }

    public static boolean isOfflineSessionValid(RealmModel realm, UserSessionModel userSession) {
        if (userSession == null) {
            logger.debug("No offline user session");
            return false;
        }
        int currentTime = Time.currentTime();
        return userSession.getLastSessionRefresh() + realm.getOfflineSessionIdleTimeout() > currentTime;
    }

    public static void expireUserSessionCookie(KeycloakSession session, UserSessionModel userSession, RealmModel realm, UriInfo uriInfo, HttpHeaders headers, ClientConnection connection) {
        try {
            // check to see if any identity cookie is set with the same session and expire it if necessary
            Cookie cookie = headers.getCookies().get(KEYCLOAK_IDENTITY_COOKIE);
            if (cookie == null) return;
            String tokenString = cookie.getValue();

            TokenVerifier<AccessToken> verifier = TokenVerifier.create(tokenString, AccessToken.class)
              .realmUrl(Urls.realmIssuer(uriInfo.getBaseUri(), realm.getName()))
              .checkActive(false)
              .checkTokenType(false);

            String kid = verifier.getHeader().getKeyId();
            SecretKey secretKey = session.keys().getHmacSecretKey(realm, kid);

            AccessToken token = verifier.secretKey(secretKey).verify().getToken();
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


    /**
     * Do not logout broker
     *
     * @param session
     * @param realm
     * @param userSession
     * @param uriInfo
     * @param connection
     * @param headers
     */
    public static void backchannelLogout(KeycloakSession session, RealmModel realm,
                                         UserSessionModel userSession, UriInfo uriInfo,
                                         ClientConnection connection, HttpHeaders headers,
                                         boolean logoutBroker) {
        if (userSession == null) return;
        UserModel user = userSession.getUser();
        if (userSession.getState() != UserSessionModel.State.LOGGING_OUT) {
            userSession.setState(UserSessionModel.State.LOGGING_OUT);
        }

        logger.debugv("Logging out: {0} ({1})", user.getUsername(), userSession.getId());
        expireUserSessionCookie(session, userSession, realm, uriInfo, headers, connection);

        final AuthenticationSessionManager asm = new AuthenticationSessionManager(session);
        AuthenticationSessionModel logoutAuthSession = createOrJoinLogoutSession(realm, asm, false);

        try {
            backchannelLogoutAll(session, realm, userSession, logoutAuthSession, uriInfo, headers, logoutBroker);
            checkUserSessionOnlyHasLoggedOutClients(realm, userSession, logoutAuthSession);
        } finally {
            asm.removeAuthenticationSession(realm, logoutAuthSession, false);
        }

        userSession.setState(UserSessionModel.State.LOGGED_OUT);
        session.sessions().removeUserSession(realm, userSession);
    }

    private static AuthenticationSessionModel createOrJoinLogoutSession(RealmModel realm, final AuthenticationSessionManager asm, boolean browserCookie) {
        ClientModel client = realm.getClientByClientId(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID);
        AuthenticationSessionModel logoutAuthSession = asm.getCurrentAuthenticationSession(realm);
        // Try to join existing logout session if it exists and browser session is required
        if (browserCookie && logoutAuthSession != null) {
            if (Objects.equals(AuthenticationSessionModel.Action.LOGGING_OUT.name(), logoutAuthSession.getAction())) {
                return logoutAuthSession;
            }
            logoutAuthSession.restartSession(realm, client);
        } else {
            logoutAuthSession = asm.createAuthenticationSession(realm, client, browserCookie);
        }
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
     * @param client Client. Must not be {@code null}
     * @param state
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
            AuthenticatedClientSessionModel clientSession = userSession.getAuthenticatedClientSessions().get(client.getId());
            if (clientSession != null) {
                AuthenticationManager.backchannelLogoutClientSession(session, realm, clientSession, null, uriInfo, headers);
                clientSession.setAction(AuthenticationSessionModel.Action.LOGGED_OUT.name());
                TokenManager.dettachClientSession(session.sessions(), realm, clientSession);
            }
        }
    }

    public static Response browserLogout(KeycloakSession session, RealmModel realm, UserSessionModel userSession, UriInfo uriInfo, ClientConnection connection, HttpHeaders headers) {
        if (userSession == null) return null;

        if (logger.isDebugEnabled()) {
            UserModel user = userSession.getUser();
            logger.debugv("Logging out: {0} ({1})", user.getUsername(), userSession.getId());
        }
        
        if (userSession.getState() != UserSessionModel.State.LOGGING_OUT) {
            userSession.setState(UserSessionModel.State.LOGGING_OUT);
        }

        final AuthenticationSessionManager asm = new AuthenticationSessionManager(session);
        AuthenticationSessionModel logoutAuthSession = createOrJoinLogoutSession(realm, asm, true);

        Response response = browserLogoutAllClients(userSession, session, realm, headers, uriInfo, logoutAuthSession);
        if (response != null) {
            return response;
        }

        String brokerId = userSession.getNote(Details.IDENTITY_PROVIDER);
        if (brokerId != null) {
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
        AuthenticationSessionModel logoutAuthSession = asm.getCurrentAuthenticationSession(realm);
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
        return response;
    }


    public static AccessToken createIdentityToken(RealmModel realm, UserModel user, UserSessionModel session, String issuer) {
        AccessToken token = new AccessToken();
        token.id(KeycloakModelUtils.generateId());
        token.issuedNow();
        token.subject(user.getId());
        token.issuer(issuer);
        if (session != null) {
            token.setSessionState(session.getId());
        }
        if (realm.getSsoSessionMaxLifespan() > 0) {
            token.expiration(Time.currentTime() + realm.getSsoSessionMaxLifespan());
        }
        return token;
    }

    public static void createLoginCookie(KeycloakSession keycloakSession, RealmModel realm, UserModel user, UserSessionModel session, UriInfo uriInfo, ClientConnection connection) {
        String cookiePath = getIdentityCookiePath(realm, uriInfo);
        String issuer = Urls.realmIssuer(uriInfo.getBaseUri(), realm.getName());
        AccessToken identityToken = createIdentityToken(realm, user, session, issuer);
        String encoded = encodeToken(keycloakSession, realm, identityToken);
        boolean secureOnly = realm.getSslRequired().isRequired(connection);
        int maxAge = NewCookie.DEFAULT_MAX_AGE;
        if (session != null && session.isRememberMe()) {
            maxAge = realm.getSsoSessionMaxLifespan();
        }
        logger.debugv("Create login cookie - name: {0}, path: {1}, max-age: {2}", KEYCLOAK_IDENTITY_COOKIE, cookiePath, maxAge);
        CookieHelper.addCookie(KEYCLOAK_IDENTITY_COOKIE, encoded, cookiePath, null, null, maxAge, secureOnly, true);
        //builder.cookie(new NewCookie(cookieName, encoded, cookiePath, null, null, maxAge, secureOnly));// todo httponly , true);

        String sessionCookieValue = realm.getName() + "/" + user.getId();
        if (session != null) {
            sessionCookieValue += "/" + session.getId();
        }
        // THIS SHOULD NOT BE A HTTPONLY COOKIE!  It is used for OpenID Connect Iframe Session support!
        // Max age should be set to the max lifespan of the session as it's used to invalidate old-sessions on re-login
        CookieHelper.addCookie(KEYCLOAK_SESSION_COOKIE, sessionCookieValue, cookiePath, null, null, realm.getSsoSessionMaxLifespan(), secureOnly, false);
        P3PHelper.addP3PHeader(keycloakSession);
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

    protected static String encodeToken(KeycloakSession session, RealmModel realm, Object token) {
        KeyManager.ActiveHmacKey activeKey = session.keys().getActiveHmacKey(realm);

        logger.tracef("Encoding token with kid '%s'", activeKey.getKid());

        String encodedToken = new JWSBuilder()
                .kid(activeKey.getKid())
                .jsonContent(token)
                .hmac256(activeKey.getSecretKey());
        return encodedToken;
    }

    public static void expireIdentityCookie(RealmModel realm, UriInfo uriInfo, ClientConnection connection) {
        logger.debug("Expiring identity cookie");
        String path = getIdentityCookiePath(realm, uriInfo);
        expireCookie(realm, KEYCLOAK_IDENTITY_COOKIE, path, true, connection);
        expireCookie(realm, KEYCLOAK_SESSION_COOKIE, path, false, connection);
    }
    public static void expireRememberMeCookie(RealmModel realm, UriInfo uriInfo, ClientConnection connection) {
        logger.debug("Expiring remember me cookie");
        String path = getIdentityCookiePath(realm, uriInfo);
        String cookieName = KEYCLOAK_REMEMBER_ME;
        expireCookie(realm, cookieName, path, true, connection);
    }

    protected static String getIdentityCookiePath(RealmModel realm, UriInfo uriInfo) {
        return getRealmCookiePath(realm, uriInfo);
    }

    public static String getRealmCookiePath(RealmModel realm, UriInfo uriInfo) {
        URI uri = RealmsResource.realmBaseUrl(uriInfo).build(realm.getName());
        return uri.getRawPath();
    }

    public static void expireCookie(RealmModel realm, String cookieName, String path, boolean httpOnly, ClientConnection connection) {
        logger.debugv("Expiring cookie: {0} path: {1}", cookieName, path);
        boolean secureOnly = realm.getSslRequired().isRequired(connection);;
        CookieHelper.addCookie(cookieName, "", path, null, "Expiring cookie", 0, secureOnly, httpOnly);
    }

    public AuthResult authenticateIdentityCookie(KeycloakSession session, RealmModel realm) {
        return authenticateIdentityCookie(session, realm, true);
    }

    public static AuthResult authenticateIdentityCookie(KeycloakSession session, RealmModel realm, boolean checkActive) {
        Cookie cookie = session.getContext().getRequestHeaders().getCookies().get(KEYCLOAK_IDENTITY_COOKIE);
        if (cookie == null || "".equals(cookie.getValue())) {
            logger.debugv("Could not find cookie: {0}", KEYCLOAK_IDENTITY_COOKIE);
            return null;
        }

        String tokenString = cookie.getValue();
        AuthResult authResult = verifyIdentityToken(session, realm, session.getContext().getUri(), session.getContext().getConnection(), checkActive, false, true, tokenString, session.getContext().getRequestHeaders());
        if (authResult == null) {
            expireIdentityCookie(realm, session.getContext().getUri(), session.getContext().getConnection());
            return null;
        }
        authResult.getSession().setLastSessionRefresh(Time.currentTime());
        return authResult;
    }


    public static Response redirectAfterSuccessfulFlow(KeycloakSession session, RealmModel realm, UserSessionModel userSession,
                                                AuthenticatedClientSessionModel clientSession,
                                                HttpRequest request, UriInfo uriInfo, ClientConnection clientConnection,
                                                EventBuilder event, String protocol) {
        LoginProtocol protocolImpl = session.getProvider(LoginProtocol.class, protocol);
        protocolImpl.setRealm(realm)
                .setHttpHeaders(request.getHttpHeaders())
                .setUriInfo(uriInfo)
                .setEventBuilder(event);
        return redirectAfterSuccessfulFlow(session, realm, userSession, clientSession, request, uriInfo, clientConnection, event, protocolImpl);

    }

    public static Response redirectAfterSuccessfulFlow(KeycloakSession session, RealmModel realm, UserSessionModel userSession,
                                                       AuthenticatedClientSessionModel clientSession,
                                                       HttpRequest request, UriInfo uriInfo, ClientConnection clientConnection,
                                                       EventBuilder event, LoginProtocol protocol) {
        Cookie sessionCookie = request.getHttpHeaders().getCookies().get(AuthenticationManager.KEYCLOAK_SESSION_COOKIE);
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

        // Update userSession note with authTime. But just if flag SSO_AUTH is not set
        boolean isSSOAuthentication = "true".equals(session.getAttribute(SSO_AUTH));
        if (isSSOAuthentication) {
            clientSession.setNote(SSO_AUTH, "true");
        } else {
            int authTime = Time.currentTime();
            userSession.setNote(AUTH_TIME, String.valueOf(authTime));
            clientSession.removeNote(SSO_AUTH);
        }

        return protocol.authenticated(userSession, clientSession);

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
            LoginFormsProvider infoPage = session.getProvider(LoginFormsProvider.class)
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
            return response;

            // Don't remove authentication session for now, to ensure that browser buttons (back/refresh) will still work fine.

        }
        RealmModel realm = authSession.getRealm();

        AuthenticatedClientSessionModel clientSession = AuthenticationProcessor.attachSession(authSession, userSession, session, realm, clientConnection, event);

        event.event(EventType.LOGIN);
        event.session(clientSession.getUserSession());
        event.success();
        return redirectAfterSuccessfulFlow(session, realm, clientSession.getUserSession(), clientSession, request, uriInfo, clientConnection, event, authSession.getProtocol());
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

        if (client.isConsentRequired()) {

            UserConsentModel grantedConsent = session.users().getConsentByClient(realm, user.getId(), client.getId());

            ClientSessionCode<AuthenticationSessionModel> accessCode = new ClientSessionCode<>(session, realm, authSession);
            for (RoleModel r : accessCode.getRequestedRoles()) {

                // Consent already granted by user
                if (grantedConsent != null && grantedConsent.isRoleGranted(r)) {
                    continue;
                }
                return CommonClientSessionModel.Action.OAUTH_GRANT.name();
             }

            for (ProtocolMapperModel protocolMapper : accessCode.getRequestedProtocolMappers()) {
                if (protocolMapper.isConsentRequired() && protocolMapper.getConsentText() != null) {
                    if (grantedConsent == null || !grantedConsent.isProtocolMapperGranted(protocolMapper)) {
                        return CommonClientSessionModel.Action.OAUTH_GRANT.name();
                    }
                }
            }
            String consentDetail = (grantedConsent != null) ? Details.CONSENT_VALUE_PERSISTED_CONSENT : Details.CONSENT_VALUE_NO_CONSENT_REQUIRED;
            event.detail(Details.CONSENT, consentDetail);
        } else {
            event.detail(Details.CONSENT, Details.CONSENT_VALUE_NO_CONSENT_REQUIRED);
        }
        return null;

    }


    public static Response actionRequired(final KeycloakSession session, final AuthenticationSessionModel authSession,
                                                         final ClientConnection clientConnection,
                                                         final HttpRequest request, final UriInfo uriInfo, final EventBuilder event) {
        final RealmModel realm = authSession.getRealm();
        final UserModel user = authSession.getAuthenticatedUser();
        final ClientModel client = authSession.getClient();

        evaluateRequiredActionTriggers(session, authSession, clientConnection, request, uriInfo, event, realm, user);


        logger.debugv("processAccessCode: go to oauth page?: {0}", client.isConsentRequired());

        event.detail(Details.CODE_ID, authSession.getId());

        Set<String> requiredActions = user.getRequiredActions();
        Response action = executionActions(session, authSession, request, event, realm, user, requiredActions);
        if (action != null) return action;

        // executionActions() method should remove any duplicate actions that might be in the clientSession
        requiredActions = authSession.getRequiredActions();
        action = executionActions(session, authSession, request, event, realm, user, requiredActions);
        if (action != null) return action;

        if (client.isConsentRequired()) {

            UserConsentModel grantedConsent = session.users().getConsentByClient(realm, user.getId(), client.getId());

            List<RoleModel> realmRoles = new LinkedList<>();
            MultivaluedMap<String, RoleModel> resourceRoles = new MultivaluedMapImpl<>();
            ClientSessionCode<AuthenticationSessionModel> accessCode = new ClientSessionCode<>(session, realm, authSession);
            for (RoleModel r : accessCode.getRequestedRoles()) {

                // Consent already granted by user
                if (grantedConsent != null && grantedConsent.isRoleGranted(r)) {
                    continue;
                }

                if (r.getContainer() instanceof RealmModel) {
                    realmRoles.add(r);
                } else {
                    resourceRoles.add(((ClientModel) r.getContainer()).getClientId(), r);
                }
            }

            List<ProtocolMapperModel> protocolMappers = new LinkedList<>();
            for (ProtocolMapperModel protocolMapper : accessCode.getRequestedProtocolMappers()) {
                if (protocolMapper.isConsentRequired() && protocolMapper.getConsentText() != null) {
                    if (grantedConsent == null || !grantedConsent.isProtocolMapperGranted(protocolMapper)) {
                        protocolMappers.add(protocolMapper);
                    }
                }
            }

            // Skip grant screen if everything was already approved by this user
            if (realmRoles.size() > 0 || resourceRoles.size() > 0 || protocolMappers.size() > 0) {
                String execution = AuthenticatedClientSessionModel.Action.OAUTH_GRANT.name();

                accessCode.

                        setAction(AuthenticatedClientSessionModel.Action.REQUIRED_ACTIONS.name());
                authSession.setAuthNote(AuthenticationProcessor.CURRENT_AUTHENTICATION_EXECUTION, execution);

                return session.getProvider(LoginFormsProvider.class)
                        .setExecution(execution)
                        .setClientSessionCode(accessCode.getCode())
                        .setAccessRequest(realmRoles, resourceRoles, protocolMappers)
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


    public static void setRolesAndMappersInSession(AuthenticationSessionModel authSession) {
        ClientModel client = authSession.getClient();
        UserModel user = authSession.getAuthenticatedUser();

        Set<String> requestedRoles = new HashSet<String>();
        // todo scope param protocol independent
        String scopeParam = authSession.getClientNote(OAuth2Constants.SCOPE);
        for (RoleModel r : TokenManager.getAccess(scopeParam, true, client, user)) {
            requestedRoles.add(r.getId());
        }
        authSession.setRoles(requestedRoles);

        Set<String> requestedProtocolMappers = new HashSet<String>();
        ClientTemplateModel clientTemplate = client.getClientTemplate();
        if (clientTemplate != null && client.useTemplateMappers()) {
            for (ProtocolMapperModel protocolMapper : clientTemplate.getProtocolMappers()) {
                if (protocolMapper.getProtocol().equals(authSession.getProtocol())) {
                    requestedProtocolMappers.add(protocolMapper.getId());
                }
            }

        }
        for (ProtocolMapperModel protocolMapper : client.getProtocolMappers()) {
            if (protocolMapper.getProtocol().equals(authSession.getProtocol())) {
                requestedProtocolMappers.add(protocolMapper.getId());
            }
        }
        authSession.setProtocolMappers(requestedProtocolMappers);
    }

    protected static Response executionActions(KeycloakSession session, AuthenticationSessionModel authSession,
                                               HttpRequest request, EventBuilder event, RealmModel realm, UserModel user,
                                               Set<String> requiredActions) {
        for (String action : requiredActions) {
            RequiredActionProviderModel model = realm.getRequiredActionProviderByAlias(action);
            if (model == null) {
                logger.warnv("Could not find configuration for Required Action {0}, did you forget to register it?", action);
                continue;
            }
            if (!model.isEnabled()) {
                continue;
            }

            RequiredActionFactory factory = (RequiredActionFactory)session.getKeycloakSessionFactory().getProviderFactory(RequiredActionProvider.class, model.getProviderId());
            if (factory == null) {
                throw new RuntimeException("Unable to find factory for Required Action: " + model.getProviderId() + " did you forget to declare it in a META-INF/services file?");
            }
            RequiredActionProvider actionProvider = factory.create(session);
            RequiredActionContextResult context = new RequiredActionContextResult(authSession, realm, event, session, request, user, factory);
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
            }
        }
        return null;
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
                                                    boolean isCookie, String tokenString, HttpHeaders headers) {
        try {
            TokenVerifier<AccessToken> verifier = TokenVerifier.create(tokenString, AccessToken.class)
              .withDefaultChecks()
              .realmUrl(Urls.realmIssuer(uriInfo.getBaseUri(), realm.getName()))
              .checkActive(checkActive)
              .checkTokenType(checkTokenType);
            String kid = verifier.getHeader().getKeyId();
            AlgorithmType algorithmType = verifier.getHeader().getAlgorithm().getType();

            if (AlgorithmType.RSA.equals(algorithmType)) {
                PublicKey publicKey = session.keys().getRsaPublicKey(realm, kid);
                if (publicKey == null) {
                    logger.debugf("Identity cookie signed with unknown kid '%s'", kid);
                    return null;
                }
                verifier.publicKey(publicKey);
            } else if (AlgorithmType.HMAC.equals(algorithmType)) {
                SecretKey secretKey = session.keys().getHmacSecretKey(realm, kid);
                if (secretKey == null) {
                    logger.debugf("Identity cookie signed with unknown kid '%s'", kid);
                    return null;
                }
                verifier.secretKey(secretKey);
            }

            AccessToken token = verifier.verify().getToken();
            if (checkActive) {
                if (!token.isActive() || token.getIssuedAt() < realm.getNotBefore()) {
                    logger.debug("Identity cookie expired");
                    return null;
                }
            }

            UserModel user = session.users().getUserById(token.getSubject(), realm);
            if (user == null || !user.isEnabled() ) {
                logger.debug("Unknown user in identity token");
                return null;
            }

            int userNotBefore = session.users().getNotBeforeOfUser(realm, user);
            if (token.getIssuedAt() < userNotBefore) {
                logger.debug("User notBefore newer than token");
                return null;
            }

            UserSessionModel userSession = session.sessions().getUserSession(realm, token.getSessionState());
            if (!isSessionValid(realm, userSession)) {
                // Check if accessToken was for the offline session.
                if (!isCookie) {
                    UserSessionModel offlineUserSession = session.sessions().getOfflineUserSession(realm, token.getSessionState());
                    if (isOfflineSessionValid(realm, offlineUserSession)) {
                        return new AuthResult(user, offlineUserSession, token);
                    }
                }

                if (userSession != null) backchannelLogout(session, realm, userSession, uriInfo, connection, headers, true);
                logger.debug("User session not active");
                return null;
            }

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

}
