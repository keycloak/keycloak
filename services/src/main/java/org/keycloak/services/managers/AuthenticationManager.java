package org.keycloak.services.managers;

import org.jboss.logging.Logger;
import org.keycloak.RSATokenVerifier;
import org.keycloak.VerificationException;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredCredentialModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.ClientConnection;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.services.util.CookieHelper;
import org.keycloak.util.Time;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.HashSet;
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
    protected static Logger logger = Logger.getLogger(AuthenticationManager.class);
    public static final String FORM_USERNAME = "username";
    // used for auth login
    public static final String KEYCLOAK_IDENTITY_COOKIE = "KEYCLOAK_IDENTITY";
    // used solely to determine is user is logged in
    public static final String KEYCLOAK_SESSION_COOKIE = "KEYCLOAK_SESSION";
    public static final String KEYCLOAK_REMEMBER_ME = "KEYCLOAK_REMEMBER_ME";

    protected BruteForceProtector protector;

    public AuthenticationManager() {
    }

    public AuthenticationManager(BruteForceProtector protector) {
        this.protector = protector;
    }

    public static boolean isSessionValid(RealmModel realm, UserSessionModel userSession) {
        if (userSession == null) return false;
        int currentTime = Time.currentTime();
        int max = userSession.getStarted() + realm.getSsoSessionMaxLifespan();
        boolean valid = userSession != null && userSession.getLastSessionRefresh() + realm.getSsoSessionIdleTimeout() > currentTime && max > currentTime;
        return valid;
    }

    public static void logout(KeycloakSession session, RealmModel realm, UserSessionModel userSession, UriInfo uriInfo, ClientConnection connection) {
        if (userSession == null) return;
        UserModel user = userSession.getUser();

        logger.infov("Logging out: {0} ({1})", user.getUsername(), userSession.getId());

        session.sessions().removeUserSession(realm, userSession);
        expireIdentityCookie(realm, uriInfo, connection);
        expireRememberMeCookie(realm, uriInfo, connection);

        new ResourceAdminManager().logoutUser(uriInfo.getRequestUri(), realm, user.getId(), userSession.getId());

    }


    public AccessToken createIdentityToken(RealmModel realm, UserModel user, UserSessionModel session) {
        logger.info("createIdentityToken");
        AccessToken token = new AccessToken();
        token.id(KeycloakModelUtils.generateId());
        token.issuedNow();
        token.subject(user.getId());
        token.audience(realm.getName());
        if (session != null) {
            token.setSessionState(session.getId());
        }
        if (realm.getSsoSessionIdleTimeout() > 0) {
            token.expiration(Time.currentTime() + realm.getSsoSessionIdleTimeout());
        }
        return token;
    }

    public void createLoginCookie(RealmModel realm, UserModel user, UserSessionModel session, UriInfo uriInfo, ClientConnection connection) {
        logger.info("createLoginCookie");
        String cookiePath = getIdentityCookiePath(realm, uriInfo);
        AccessToken identityToken = createIdentityToken(realm, user, session);
        String encoded = encodeToken(realm, identityToken);
        boolean secureOnly = realm.getSslRequired().isRequired(connection);
        logger.debugv("creatingLoginCookie - name: {0} path: {1}", KEYCLOAK_IDENTITY_COOKIE, cookiePath);
        int maxAge = NewCookie.DEFAULT_MAX_AGE;
        if (session.isRememberMe()) {
            maxAge = realm.getSsoSessionIdleTimeout();
            logger.info("createLoginCookie maxAge: " + maxAge);
        }
        CookieHelper.addCookie(KEYCLOAK_IDENTITY_COOKIE, encoded, cookiePath, null, null, maxAge, secureOnly, true);
        //builder.cookie(new NewCookie(cookieName, encoded, cookiePath, null, null, maxAge, secureOnly));// todo httponly , true);

        String sessionCookieValue = realm.getName() + "/" + user.getId();
        if (session != null) {
            sessionCookieValue += "/" + session.getId();
        }
        // THIS SHOULD NOT BE A HTTPONLY COOKIE!  It is used for OpenID Connect Iframe Session support!
        // Max age should be set to the max lifespan of the session as it's used to invalidate old-sessions on re-login
        CookieHelper.addCookie(KEYCLOAK_SESSION_COOKIE, sessionCookieValue, cookiePath, null, null, realm.getSsoSessionMaxLifespan(), secureOnly, false);

    }

    public void createRememberMeCookie(RealmModel realm, UriInfo uriInfo, ClientConnection connection) {
        String path = getIdentityCookiePath(realm, uriInfo);
        boolean secureOnly = realm.getSslRequired().isRequired(connection);
        // remember me cookie should be persistent
        //NewCookie cookie = new NewCookie(KEYCLOAK_REMEMBER_ME, "true", path, null, null, realm.getCentralLoginLifespan(), secureOnly);// todo httponly , true);
        CookieHelper.addCookie(KEYCLOAK_REMEMBER_ME, "true", path, null, null, realm.getSsoSessionIdleTimeout(), secureOnly, true);
    }

    protected String encodeToken(RealmModel realm, Object token) {
        String encodedToken = new JWSBuilder()
                .jsonContent(token)
                .rsa256(realm.getPrivateKey());
        return encodedToken;
    }

    public static void expireIdentityCookie(RealmModel realm, UriInfo uriInfo, ClientConnection connection) {
        logger.debug("Expiring identity cookie");
        String path = getIdentityCookiePath(realm, uriInfo);
        expireCookie(realm, KEYCLOAK_IDENTITY_COOKIE, path, true, connection);
        expireCookie(realm, KEYCLOAK_SESSION_COOKIE, path, false, connection);
        expireRememberMeCookie(realm, uriInfo, connection);
    }
    public static void expireRememberMeCookie(RealmModel realm, UriInfo uriInfo, ClientConnection connection) {
        logger.debug("Expiring remember me cookie");
        String path = getIdentityCookiePath(realm, uriInfo);
        String cookieName = KEYCLOAK_REMEMBER_ME;
        expireCookie(realm, cookieName, path, true, connection);
    }

    protected static String getIdentityCookiePath(RealmModel realm, UriInfo uriInfo) {
        URI uri = RealmsResource.realmBaseUrl(uriInfo).build(realm.getName());
        return uri.getRawPath();
    }

    public static void expireCookie(RealmModel realm, String cookieName, String path, boolean httpOnly, ClientConnection connection) {
        logger.debugv("Expiring cookie: {0} path: {1}", cookieName, path);
        boolean secureOnly = realm.getSslRequired().isRequired(connection);;
        CookieHelper.addCookie(cookieName, "", path, null, "Expiring cookie", 0, secureOnly, httpOnly);
    }

    public AuthResult authenticateIdentityCookie(KeycloakSession session, RealmModel realm, UriInfo uriInfo, ClientConnection connection, HttpHeaders headers) {
        return authenticateIdentityCookie(session, realm, uriInfo, connection, headers, true);
    }

    public AuthResult authenticateIdentityCookie(KeycloakSession session, RealmModel realm, UriInfo uriInfo, ClientConnection connection, HttpHeaders headers, boolean checkActive) {
        logger.info("authenticateIdentityCookie");
        Cookie cookie = headers.getCookies().get(KEYCLOAK_IDENTITY_COOKIE);
        if (cookie == null) {
            logger.infov("authenticateCookie could not find cookie: {0}", KEYCLOAK_IDENTITY_COOKIE);
            return null;
        }

        String tokenString = cookie.getValue();
        AuthResult authResult = verifyIdentityToken(session, realm, uriInfo, connection, checkActive, tokenString);
        if (authResult == null) {
            expireIdentityCookie(realm, uriInfo, connection);
            return null;
        }
        authResult.getSession().setLastSessionRefresh(Time.currentTime());
        return authResult;
    }

    protected AuthResult verifyIdentityToken(KeycloakSession session, RealmModel realm, UriInfo uriInfo, ClientConnection connection, boolean checkActive, String tokenString) {
        try {
            AccessToken token = RSATokenVerifier.verifyToken(tokenString, realm.getPublicKey(), realm.getName(), checkActive);
            logger.info("identity token verified");
            if (checkActive) {
                logger.info("Checking if identity token is active");
                if (!token.isActive() || token.getIssuedAt() < realm.getNotBefore()) {
                    logger.info("identity cookie expired");
                    return null;
                } else {
                    logger.info("token.isActive() : " + token.isActive());
                    logger.info("token.issuedAt: " + token.getIssuedAt());
                    logger.info("real.notbefore: " + realm.getNotBefore());
                }
            }

            UserModel user = session.users().getUserById(token.getSubject(), realm);
            if (user == null || !user.isEnabled() ) {
                logger.info("Unknown user in identity token");
                return null;
            }

            UserSessionModel userSession = session.sessions().getUserSession(realm, token.getSessionState());
            if (!isSessionValid(realm, userSession)) {
                if (userSession != null) logout(session, realm, userSession, uriInfo, connection);
                logger.info("User session not active");
                return null;
            }

            return new AuthResult(user, userSession, token);
        } catch (VerificationException e) {
            logger.info("Failed to verify identity token", e);
        }
        return null;
    }

    public AuthenticationStatus authenticateForm(KeycloakSession session, ClientConnection clientConnection, RealmModel realm, MultivaluedMap<String, String> formData) {
        String username = formData.getFirst(FORM_USERNAME);
        if (username == null) {
            logger.warn("Username not provided");
            return AuthenticationStatus.INVALID_USER;
        }

        if (realm.isBruteForceProtected()) {
            if (protector.isTemporarilyDisabled(session, realm, username)) {
                return AuthenticationStatus.ACCOUNT_TEMPORARILY_DISABLED;
            }
        }

        AuthenticationStatus status = authenticateInternal(session, realm, formData, username);
        if (realm.isBruteForceProtected()) {
            switch (status) {
                case SUCCESS:
                    protector.successfulLogin(realm, username, clientConnection);
                    break;
                case FAILED:
                case MISSING_TOTP:
                case MISSING_PASSWORD:
                case INVALID_CREDENTIALS:
                    protector.failedLogin(realm, username, clientConnection);
                    break;
                case INVALID_USER:
                    protector.invalidUser(realm, username, clientConnection);
                    break;
                default:
                    break;
            }
        }

        return status;
    }

    protected AuthenticationStatus authenticateInternal(KeycloakSession session, RealmModel realm, MultivaluedMap<String, String> formData, String username) {
        UserModel user = KeycloakModelUtils.findUserByNameOrEmail(session, realm, username);

        if (user == null) {
            logger.warn("User " + username + " not found");
            return AuthenticationStatus.INVALID_USER;
        }

        if (!checkEnabled(user)) {
            return AuthenticationStatus.ACCOUNT_DISABLED;
        }

        Set<String> types = new HashSet<String>();

        for (RequiredCredentialModel credential : realm.getRequiredCredentials()) {
            types.add(credential.getType());
        }

        if (types.contains(CredentialRepresentation.PASSWORD)) {
            List<UserCredentialModel> credentials = new LinkedList<UserCredentialModel>();
            String password = formData.getFirst(CredentialRepresentation.PASSWORD);
            if (password == null) {
                logger.warn("Password not provided");
                return AuthenticationStatus.MISSING_PASSWORD;
            }
            credentials.add(UserCredentialModel.password(password));

            if (user.isTotp()) {
                String token = formData.getFirst(CredentialRepresentation.TOTP);
                if (token == null) {
                    logger.warn("TOTP token not provided");
                    return AuthenticationStatus.MISSING_TOTP;
                }
                credentials.add(UserCredentialModel.totp(token));

             }

            logger.debug("validating password for user: " + username);

            if (!session.users().validCredentials(realm, user, credentials)) {
                return AuthenticationStatus.INVALID_CREDENTIALS;
            }

            if (!user.getRequiredActions().isEmpty()) {
                return AuthenticationStatus.ACTIONS_REQUIRED;
            } else {
                return AuthenticationStatus.SUCCESS;
            }
        } else if (types.contains(CredentialRepresentation.SECRET)) {
            String secret = formData.getFirst(CredentialRepresentation.SECRET);
            if (secret == null) {
                logger.warn("Secret not provided");
                return AuthenticationStatus.MISSING_PASSWORD;
            }
            if (!session.users().validCredentials(realm, user, UserCredentialModel.secret(secret))) {
                return AuthenticationStatus.INVALID_CREDENTIALS;
            }
            if (!user.getRequiredActions().isEmpty()) {
                return AuthenticationStatus.ACTIONS_REQUIRED;
            } else {
                return AuthenticationStatus.SUCCESS;
            }
        } else {
            logger.warn("Do not know how to authenticate user");
            return AuthenticationStatus.FAILED;
        }
    }

    private boolean checkEnabled(UserModel user) {
        if (!user.isEnabled()) {
            logger.warn("AccountProvider is disabled, contact admin. " + user.getUsername());
            return false;
        } else {
            return true;
        }
    }

    public enum AuthenticationStatus {
        SUCCESS, ACCOUNT_TEMPORARILY_DISABLED, ACCOUNT_DISABLED, ACTIONS_REQUIRED, INVALID_USER, INVALID_CREDENTIALS, MISSING_PASSWORD, MISSING_TOTP, FAILED
    }

    public class AuthResult {
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
