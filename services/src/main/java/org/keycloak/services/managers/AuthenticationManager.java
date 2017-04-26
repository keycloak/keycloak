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
import org.keycloak.TokenVerifier;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionContextResult;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
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
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.KeyManager;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredActionProviderModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserConsentModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.LoginProtocol;
import org.keycloak.protocol.LoginProtocol.Error;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.Urls;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.resources.IdentityBrokerService;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.services.util.CookieHelper;
import org.keycloak.services.util.P3PHelper;

import javax.crypto.SecretKey;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.security.PublicKey;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Stateless object that manages authentication
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class AuthenticationManager {
    public static final String SET_REDIRECT_URI_AFTER_REQUIRED_ACTIONS= "SET_REDIRECT_URI_AFTER_REQUIRED_ACTIONS";
    public static final String END_AFTER_REQUIRED_ACTIONS = "END_AFTER_REQUIRED_ACTIONS";

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
    public static final String CURRENT_REQUIRED_ACTION = "CURRENT_REQUIRED_ACTION";

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

            TokenVerifier verifier = TokenVerifier.create(tokenString).realmUrl(Urls.realmIssuer(uriInfo.getBaseUri(), realm.getName())).checkActive(false).checkTokenType(false);

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
        KeycloakContext ctx = session.getContext();
        backchannelLogout(session, ctx.getRealm(), userSession, ctx.getUri(), ctx.getConnection(), ctx.getRequestHeaders(), logoutBroker);
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
        userSession.setState(UserSessionModel.State.LOGGING_OUT);

        logger.debugv("Logging out: {0} ({1})", user.getUsername(), userSession.getId());
        expireUserSessionCookie(session, userSession, realm, uriInfo, headers, connection);

        for (ClientSessionModel clientSession : userSession.getClientSessions()) {
            backchannelLogoutClientSession(session, realm, clientSession, userSession, uriInfo, headers);
        }
        if (logoutBroker) {
            String brokerId = userSession.getNote(Details.IDENTITY_PROVIDER);
            if (brokerId != null) {
                IdentityProvider identityProvider = IdentityBrokerService.getIdentityProvider(session, realm, brokerId);
                try {
                    identityProvider.backchannelLogout(session, userSession, uriInfo, realm);
                } catch (Exception e) {
                }
            }
        }
        userSession.setState(UserSessionModel.State.LOGGED_OUT);
        session.sessions().removeUserSession(realm, userSession);
    }

    public static void backchannelLogoutClientSession(KeycloakSession session, RealmModel realm, ClientSessionModel clientSession, UserSessionModel userSession, UriInfo uriInfo, HttpHeaders headers) {
        ClientModel client = clientSession.getClient();
        if (client instanceof ClientModel && !client.isFrontchannelLogout() && !ClientSessionModel.Action.LOGGED_OUT.name().equals(clientSession.getAction())) {
            String authMethod = clientSession.getAuthMethod();
            if (authMethod == null) return; // must be a keycloak service like account
            LoginProtocol protocol = session.getProvider(LoginProtocol.class, authMethod);
            protocol.setRealm(realm)
                    .setHttpHeaders(headers)
                    .setUriInfo(uriInfo);
            protocol.backchannelLogout(userSession, clientSession);
            clientSession.setAction(ClientSessionModel.Action.LOGGED_OUT.name());
        }

    }

    // Logout all clientSessions of this user and client
    public static void backchannelUserFromClient(KeycloakSession session, RealmModel realm, UserModel user, ClientModel client, UriInfo uriInfo, HttpHeaders headers) {
        String clientId = client.getId();

        List<UserSessionModel> userSessions = session.sessions().getUserSessions(realm, user);
        for (UserSessionModel userSession : userSessions) {
            List<ClientSessionModel> clientSessions = userSession.getClientSessions();
            for (ClientSessionModel clientSession : clientSessions) {
                if (clientSession.getClient().getId().equals(clientId)) {
                    AuthenticationManager.backchannelLogoutClientSession(session, realm, clientSession, userSession, uriInfo, headers);
                    TokenManager.dettachClientSession(session.sessions(), realm, clientSession);
                }
            }
        }
    }

    public static Response browserLogout(KeycloakSession session, RealmModel realm, UserSessionModel userSession, UriInfo uriInfo, ClientConnection connection, HttpHeaders headers) {
        if (userSession == null) return null;
        UserModel user = userSession.getUser();

        logger.debugv("Logging out: {0} ({1})", user.getUsername(), userSession.getId());
        if (userSession.getState() != UserSessionModel.State.LOGGING_OUT) {
            userSession.setState(UserSessionModel.State.LOGGING_OUT);
        }
        List<ClientSessionModel> redirectClients = new LinkedList<ClientSessionModel>();
        for (ClientSessionModel clientSession : userSession.getClientSessions()) {
            ClientModel client = clientSession.getClient();
            if (ClientSessionModel.Action.LOGGED_OUT.name().equals(clientSession.getAction())) continue;
            if (client.isFrontchannelLogout()) {
                String authMethod = clientSession.getAuthMethod();
                if (authMethod == null) continue; // must be a keycloak service like account
                redirectClients.add(clientSession);
            } else {
                String authMethod = clientSession.getAuthMethod();
                if (authMethod == null) continue; // must be a keycloak service like account
                LoginProtocol protocol = session.getProvider(LoginProtocol.class, authMethod);
                protocol.setRealm(realm)
                        .setHttpHeaders(headers)
                        .setUriInfo(uriInfo);
                try {
                    logger.debugv("backchannel logout to: {0}", client.getClientId());
                    protocol.backchannelLogout(userSession, clientSession);
                    clientSession.setAction(ClientSessionModel.Action.LOGGED_OUT.name());
                } catch (Exception e) {
                    ServicesLogger.LOGGER.failedToLogoutClient(e);
                }
            }
        }

        for (ClientSessionModel nextRedirectClient : redirectClients) {
            String authMethod = nextRedirectClient.getAuthMethod();
            LoginProtocol protocol = session.getProvider(LoginProtocol.class, authMethod);
            protocol.setRealm(realm)
                    .setHttpHeaders(headers)
                    .setUriInfo(uriInfo);
            // setting this to logged out cuz I"m not sure protocols can always verify that the client was logged out or not
            nextRedirectClient.setAction(ClientSessionModel.Action.LOGGED_OUT.name());
            try {
                logger.debugv("frontchannel logout to: {0}", nextRedirectClient.getClient().getClientId());
                Response response = protocol.frontchannelLogout(userSession, nextRedirectClient);
                if (response != null) {
                    logger.debug("returning frontchannel logout request to client");
                    return response;
                }
            } catch (Exception e) {
                ServicesLogger.LOGGER.failedToLogoutClient(e);
            }

        }
        String brokerId = userSession.getNote(Details.IDENTITY_PROVIDER);
        if (brokerId != null) {
            IdentityProvider identityProvider = IdentityBrokerService.getIdentityProvider(session, realm, brokerId);
            Response response = identityProvider.keycloakInitiatedBrowserLogout(session, userSession, uriInfo, realm);
            if (response != null) return response;
        }
        return finishBrowserLogout(session, realm, userSession, uriInfo, connection, headers);
    }

    public static Response finishBrowserLogout(KeycloakSession session, RealmModel realm, UserSessionModel userSession, UriInfo uriInfo, ClientConnection connection, HttpHeaders headers) {
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
        if (session.isRememberMe()) {
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
                                                ClientSessionModel clientSession,
                                                HttpRequest request, UriInfo uriInfo, ClientConnection clientConnection,
                                                EventBuilder event) {
        LoginProtocol protocol = session.getProvider(LoginProtocol.class, clientSession.getAuthMethod());
        protocol.setRealm(realm)
                .setHttpHeaders(request.getHttpHeaders())
                .setUriInfo(uriInfo)
                .setEventBuilder(event);
        return redirectAfterSuccessfulFlow(session, realm, userSession, clientSession, request, uriInfo, clientConnection, event, protocol);

    }

    public static Response redirectAfterSuccessfulFlow(KeycloakSession session, RealmModel realm, UserSessionModel userSession,
                                                       ClientSessionModel clientSession,
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
        if (!isSSOAuthentication(clientSession)) {
            int authTime = Time.currentTime();
            userSession.setNote(AUTH_TIME, String.valueOf(authTime));
        }

        return protocol.authenticated(userSession, new ClientSessionCode(session, realm, clientSession));

    }

    public static boolean isSSOAuthentication(ClientSessionModel clientSession) {
        String ssoAuth = clientSession.getNote(SSO_AUTH);
        return Boolean.parseBoolean(ssoAuth);
    }


    public static Response nextActionAfterAuthentication(KeycloakSession session, UserSessionModel userSession, ClientSessionModel clientSession,
                                                  ClientConnection clientConnection,
                                                  HttpRequest request, UriInfo uriInfo, EventBuilder event) {
        Response requiredAction = actionRequired(session, userSession, clientSession, clientConnection, request, uriInfo, event);
        if (requiredAction != null) return requiredAction;
        return finishedRequiredActions(session, userSession, clientSession, clientConnection, request, uriInfo, event);

    }

    public static Response finishedRequiredActions(KeycloakSession session, UserSessionModel userSession, ClientSessionModel clientSession, ClientConnection clientConnection, HttpRequest request, UriInfo uriInfo, EventBuilder event) {
        if (clientSession.getNote(END_AFTER_REQUIRED_ACTIONS) != null) {
            LoginFormsProvider infoPage = session.getProvider(LoginFormsProvider.class)
                    .setSuccess(Messages.ACCOUNT_UPDATED);
            if (clientSession.getNote(SET_REDIRECT_URI_AFTER_REQUIRED_ACTIONS) != null) {
                if (clientSession.getRedirectUri() != null) {
                    infoPage.setAttribute("pageRedirectUri", clientSession.getRedirectUri());
                }

            } else {
                infoPage.setAttribute("skipLink", true);
            }
            Response response = infoPage
                    .createInfoPage();
            session.sessions().removeUserSession(session.getContext().getRealm(), userSession);
            return response;

        }
        event.success();
        RealmModel realm = clientSession.getRealm();
        return redirectAfterSuccessfulFlow(session, realm , userSession, clientSession, request, uriInfo, clientConnection, event);
    }

    public static boolean isActionRequired(final KeycloakSession session, final UserSessionModel userSession, final ClientSessionModel clientSession,
                                           final ClientConnection clientConnection,
                                           final HttpRequest request, final UriInfo uriInfo, final EventBuilder event) {
        final RealmModel realm = clientSession.getRealm();
        final UserModel user = userSession.getUser();
        final ClientModel client = clientSession.getClient();

        evaluateRequiredActionTriggers(session, userSession, clientSession, clientConnection, request, uriInfo, event, realm, user);

        if (!user.getRequiredActions().isEmpty() || !clientSession.getRequiredActions().isEmpty()) return true;

        if (client.isConsentRequired()) {

            UserConsentModel grantedConsent = session.users().getConsentByClient(realm, user.getId(), client.getId());

            ClientSessionCode accessCode = new ClientSessionCode(session, realm, clientSession);
            for (RoleModel r : accessCode.getRequestedRoles()) {

                // Consent already granted by user
                if (grantedConsent != null && grantedConsent.isRoleGranted(r)) {
                    continue;
                }
                return true;
             }

            for (ProtocolMapperModel protocolMapper : accessCode.getRequestedProtocolMappers()) {
                if (protocolMapper.isConsentRequired() && protocolMapper.getConsentText() != null) {
                    if (grantedConsent == null || !grantedConsent.isProtocolMapperGranted(protocolMapper)) {
                        return true;
                    }
                }
            }
            String consentDetail = (grantedConsent != null) ? Details.CONSENT_VALUE_PERSISTED_CONSENT : Details.CONSENT_VALUE_NO_CONSENT_REQUIRED;
            event.detail(Details.CONSENT, consentDetail);
        } else {
            event.detail(Details.CONSENT, Details.CONSENT_VALUE_NO_CONSENT_REQUIRED);
        }
        return false;

    }


    public static Response actionRequired(final KeycloakSession session, final UserSessionModel userSession, final ClientSessionModel clientSession,
                                                         final ClientConnection clientConnection,
                                                         final HttpRequest request, final UriInfo uriInfo, final EventBuilder event) {
        final RealmModel realm = clientSession.getRealm();
        final UserModel user = userSession.getUser();
        final ClientModel client = clientSession.getClient();

        evaluateRequiredActionTriggers(session, userSession, clientSession, clientConnection, request, uriInfo, event, realm, user);


        logger.debugv("processAccessCode: go to oauth page?: {0}", client.isConsentRequired());

        event.detail(Details.CODE_ID, clientSession.getId());

        Set<String> requiredActions = user.getRequiredActions();
        Response action = executionActions(session, userSession, clientSession, request, event, realm, user, requiredActions);
        if (action != null) return action;

        // executionActions() method should remove any duplicate actions that might be in the clientSession
        requiredActions = clientSession.getRequiredActions();
        action = executionActions(session, userSession, clientSession, request, event, realm, user, requiredActions);
        if (action != null) return action;

        if (client.isConsentRequired()) {

            UserConsentModel grantedConsent = session.users().getConsentByClient(realm, user.getId(), client.getId());

            List<RoleModel> realmRoles = new LinkedList<>();
            MultivaluedMap<String, RoleModel> resourceRoles = new MultivaluedMapImpl<>();
            ClientSessionCode accessCode = new ClientSessionCode(session, realm, clientSession);
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
                accessCode.setAction(ClientSessionModel.Action.REQUIRED_ACTIONS.name());
                clientSession.setNote(CURRENT_REQUIRED_ACTION, ClientSessionModel.Action.OAUTH_GRANT.name());

                return session.getProvider(LoginFormsProvider.class)
                        .setClientSessionCode(accessCode.getCode())
                        .setAccessRequest(realmRoles, resourceRoles, protocolMappers)
                        .createOAuthGrant(clientSession);
            } else {
                String consentDetail = (grantedConsent != null) ? Details.CONSENT_VALUE_PERSISTED_CONSENT : Details.CONSENT_VALUE_NO_CONSENT_REQUIRED;
                event.detail(Details.CONSENT, consentDetail);
            }
        } else {
            event.detail(Details.CONSENT, Details.CONSENT_VALUE_NO_CONSENT_REQUIRED);
        }
        return null;

    }

    protected static Response executionActions(KeycloakSession session, UserSessionModel userSession, ClientSessionModel clientSession,
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
            RequiredActionContextResult context = new RequiredActionContextResult(userSession, clientSession, realm, event, session, request, user, factory);
            actionProvider.requiredActionChallenge(context);

            if (context.getStatus() == RequiredActionContext.Status.FAILURE) {
                LoginProtocol protocol = context.getSession().getProvider(LoginProtocol.class, context.getClientSession().getAuthMethod());
                protocol.setRealm(context.getRealm())
                        .setHttpHeaders(context.getHttpRequest().getHttpHeaders())
                        .setUriInfo(context.getUriInfo())
                        .setEventBuilder(event);
                Response response = protocol.sendError(context.getClientSession(), Error.CONSENT_DENIED);
                event.error(Errors.REJECTED_BY_USER);
                return response;
            }
            else if (context.getStatus() == RequiredActionContext.Status.CHALLENGE) {
                clientSession.setNote(CURRENT_REQUIRED_ACTION, model.getProviderId());
                return context.getChallenge();
            }
            else if (context.getStatus() == RequiredActionContext.Status.SUCCESS) {
                event.clone().event(EventType.CUSTOM_REQUIRED_ACTION).detail(Details.CUSTOM_REQUIRED_ACTION, factory.getId()).success();
                // don't have to perform the same action twice, so remove it from both the user and session required actions
                clientSession.getUserSession().getUser().removeRequiredAction(factory.getId());
                clientSession.removeRequiredAction(factory.getId());
            }
        }
        return null;
    }

    public static void evaluateRequiredActionTriggers(final KeycloakSession session, final UserSessionModel userSession, final ClientSessionModel clientSession, final ClientConnection clientConnection, final HttpRequest request, final UriInfo uriInfo, final EventBuilder event, final RealmModel realm, final UserModel user) {

        // see if any required actions need triggering, i.e. an expired password
        for (RequiredActionProviderModel model : realm.getRequiredActionProviders()) {
            if (!model.isEnabled()) continue;
            RequiredActionFactory factory = (RequiredActionFactory)session.getKeycloakSessionFactory().getProviderFactory(RequiredActionProvider.class, model.getProviderId());
            if (factory == null) {
                throw new RuntimeException("Unable to find factory for Required Action: " + model.getProviderId() + " did you forget to declare it in a META-INF/services file?");
            }
            RequiredActionProvider provider = factory.create(session);
            RequiredActionContextResult result = new RequiredActionContextResult(userSession, clientSession, realm, event, session, request, user, factory) {
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
            TokenVerifier verifier = TokenVerifier.create(tokenString).realmUrl(Urls.realmIssuer(uriInfo.getBaseUri(), realm.getName())).checkActive(checkActive).checkTokenType(checkTokenType);
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
