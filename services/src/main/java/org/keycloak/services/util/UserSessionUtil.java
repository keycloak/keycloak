package org.keycloak.services.util;

import java.util.Objects;

import org.jboss.logging.Logger;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.constants.ServiceAccountConstants;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ImpersonationSessionNote;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.protocol.oidc.encode.AccessTokenContext;
import org.keycloak.protocol.oidc.encode.TokenContextEncoderProvider;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.Urls;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.UserSessionManager;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.sessions.RootAuthenticationSessionModel;
import org.keycloak.utils.OAuth2Error;

public class UserSessionUtil {

    private static final Logger logger = Logger.getLogger(UserSessionUtil.class);

    public static UserSessionModel findValidSession(KeycloakSession session, RealmModel realm, AccessToken token, EventBuilder event, ClientModel client) {
        OAuth2Error error = new OAuth2Error().json(false).realm(realm);
        return findValidSession(session, realm, token, event, client, error);
    }

    public static UserSessionModel findValidSession(KeycloakSession session, RealmModel realm,
            AccessToken token, EventBuilder event, ClientModel client, OAuth2Error error) {
        if (token.getSessionId() == null) {
            return createTransientSessionForClient(session, realm, token, client, event);
        }

        var userSessionProvider = session.sessions();

        AccessTokenContext accessTokenContext = session.getProvider(TokenContextEncoderProvider.class).getTokenContextFromTokenId(token.getId());
        if (accessTokenContext.getSessionType() == AccessTokenContext.SessionType.TRANSIENT) {
            UserSessionModel userSession = userSessionProvider.getUserSession(realm, token.getSessionId());

            if (AuthenticationManager.isSessionValid(realm, userSession)) {
                checkTokenIssuedAt(realm, token, userSession, event, client);
                return createTransientSessionForClient(session, userSession, client);
            }

            logger.debug("User session not found or expired for transient token");
            event.session(userSession);
            event.error(userSession == null? Errors.USER_SESSION_NOT_FOUND : Errors.SESSION_EXPIRED);
            throw error.invalidToken(userSession == null? "Session not found" : "Session expired");
        }

        UserSessionModel userSession = userSessionProvider.getUserSessionIfClientExists(realm, token.getSessionId(), false, client.getId());
        if (userSession == null) {
            // also try to resolve sessions created during token exchange when the user is impersonated
            userSession = getUserSessionWithImpersonatorClient(session, realm, token.getSessionId(), false, client.getId());
        }

        UserSessionModel offlineUserSession;
        if (AuthenticationManager.isSessionValid(realm, userSession)) {
            checkTokenIssuedAt(realm, token, userSession, event, client);
            event.session(userSession);
            return userSession;
        } else {
            offlineUserSession = userSessionProvider.getUserSessionIfClientExists(realm, token.getSessionId(), true, client.getId());
            if (AuthenticationManager.isSessionValid(realm, offlineUserSession)) {
                checkTokenIssuedAt(realm, token, offlineUserSession, event, client);
                event.session(offlineUserSession);
                return offlineUserSession;
            }
        }

        if (userSession == null && offlineUserSession == null) {
            logger.debug("User session not found or doesn't have client attached on it");
            event.error(Errors.USER_SESSION_NOT_FOUND);
            throw error.invalidToken("User session not found or doesn't have client attached on it");
        }

        event.session(Objects.requireNonNullElse(userSession, offlineUserSession));

        logger.debug("Session expired");
        event.error(Errors.SESSION_EXPIRED);
        throw error.invalidToken("Session expired");
    }

    public static UserSessionModel createTransientUserSession(KeycloakSession session, UserSessionModel userSession) {
        UserSessionModel transientSession = new UserSessionManager(session).createUserSession(userSession.getId(), userSession.getRealm(),
                userSession.getUser(), userSession.getLoginUsername(), userSession.getIpAddress(), userSession.getAuthMethod(), userSession.isRememberMe(),
                userSession.getBrokerSessionId(), userSession.getBrokerUserId(), UserSessionModel.SessionPersistenceState.TRANSIENT);
        userSession.getNotes().entrySet().forEach(e -> transientSession.setNote(e.getKey(), e.getValue()));
        return transientSession;
    }

    private static UserSessionModel attachAuthenticationSession(KeycloakSession session, UserSessionModel userSession, ClientModel client) {
        RootAuthenticationSessionModel rootAuthSession = session.authenticationSessions().createRootAuthenticationSession(userSession.getRealm());
        AuthenticationSessionModel authSession = rootAuthSession.createAuthenticationSession(client);
        authSession.setAuthenticatedUser(userSession.getUser());
        authSession.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        authSession.setClientNote(OIDCLoginProtocol.ISSUER, Urls.realmIssuer(session.getContext().getUri().getBaseUri(), userSession.getRealm().getName()));
        AuthenticationManager.setClientScopesInSession(session, authSession);
        TokenManager.attachAuthenticationSession(session, userSession, authSession);
        return userSession;
    }

    private static UserSessionModel createTransientSessionForClient(KeycloakSession session, UserSessionModel userSession, ClientModel client) {
        UserSessionModel transientSession = createTransientUserSession(session, userSession);
        attachAuthenticationSession(session, transientSession, client);
        return transientSession;
    }

    private static UserSessionModel createTransientSessionForClient(KeycloakSession session, RealmModel realm, AccessToken token, ClientModel client, EventBuilder event) {
        OAuth2Error error = new OAuth2Error().json(false).realm(realm);
        // create a transient session
        UserModel user = TokenManager.lookupUserFromStatelessToken(session, realm, token);
        if (user == null) {
            logger.debug("Transient User not found");
            event.error(Errors.USER_NOT_FOUND);
            throw error.invalidToken("User not found");
        }
        ClientConnection clientConnection = session.getContext().getConnection();
        UserSessionModel userSession = new UserSessionManager(session).createUserSession(KeycloakModelUtils.generateId(), realm, user, user.getUsername(), clientConnection.getRemoteAddr(),
                ServiceAccountConstants.CLIENT_AUTH, false, null, null, UserSessionModel.SessionPersistenceState.TRANSIENT);
        // attach an auth session for the client
        attachAuthenticationSession(session, userSession, client);
        return userSession;
    }

    public static void checkTokenIssuedAt(RealmModel realm, AccessToken token, UserSessionModel userSession, EventBuilder event, ClientModel client) {
        OAuth2Error error = new OAuth2Error().json(false).realm(realm);
        if (token.isIssuedBeforeSessionStart(userSession.getStarted())) {
            logger.debug("Stale token for user session");
            event.error(Errors.INVALID_TOKEN);
            throw error.invalidToken("Stale token");
        }

        AuthenticatedClientSessionModel clientSession = userSession.getAuthenticatedClientSessionByClient(client.getId());
        if (clientSession != null && token.isIssuedBeforeSessionStart(clientSession.getStarted())) {
            logger.debug("Stale token for client session");
            event.error(Errors.INVALID_TOKEN);
            throw error.invalidToken("Stale token");
        }
    }

    public static UserSessionModel getUserSessionWithImpersonatorClient(KeycloakSession session, RealmModel realm, String userSessionId, boolean offline, String clientUUID) {
        return session.sessions().getUserSessionWithPredicate(realm, userSessionId, offline, userSession -> Objects.equals(clientUUID, userSession.getNote(ImpersonationSessionNote.IMPERSONATOR_CLIENT.toString())));
    }
}
