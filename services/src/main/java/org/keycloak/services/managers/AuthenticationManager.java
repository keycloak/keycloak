package org.keycloak.services.managers;

import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.HttpResponse;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.RSATokenVerifier;
import org.keycloak.VerificationException;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.models.AuthenticationLinkModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.provider.ProviderSession;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.services.ClientConnection;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.authentication.AuthProviderStatus;
import org.keycloak.authentication.AuthUser;
import org.keycloak.authentication.AuthenticationProviderManager;
import org.keycloak.services.util.CookieHelper;
import org.keycloak.util.Time;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.HashSet;
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

    protected ProviderSession providerSession;
    protected BruteForceProtector protector;

    public AuthenticationManager(ProviderSession providerSession) {
        this.providerSession = providerSession;
    }

    public AuthenticationManager(ProviderSession providerSession, BruteForceProtector protector) {
        this.providerSession = providerSession;
        this.protector = protector;
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
        if (realm.getCentralLoginLifespan() > 0) {
            token.expiration(Time.currentTime() + realm.getCentralLoginLifespan());
        }
        return token;
    }

    public void createLoginCookie(Response.ResponseBuilder builder, RealmModel realm, UserModel user, UserSessionModel session, UriInfo uriInfo, boolean rememberMe) {
        logger.info("createLoginCookie");
        String cookieName = KEYCLOAK_IDENTITY_COOKIE;
        String cookiePath = getIdentityCookiePath(realm, uriInfo);
        AccessToken identityToken = createIdentityToken(realm, user, session);
        String encoded = encodeToken(realm, identityToken);
        boolean secureOnly = !realm.isSslNotRequired();
        logger.debugv("creatingLoginCookie - name: {0} path: {1}", cookieName, cookiePath);
        int maxAge = NewCookie.DEFAULT_MAX_AGE;
        if (rememberMe) {
            maxAge = realm.getCentralLoginLifespan();
            logger.info("createLoginCookie maxAge: " + maxAge);
        }
        CookieHelper.addCookie(cookieName, encoded, cookiePath, null, null, maxAge, secureOnly, true);
        //builder.cookie(new NewCookie(cookieName, encoded, cookiePath, null, null, maxAge, secureOnly));// todo httponly , true);

        String sessionCookieValue = realm.getName() + "-" + user.getId();
        if (session != null) {
            sessionCookieValue += "-" + session.getId();
        }
        // THIS SHOULD NOT BE A HTTPONLY COOKIE!  It is used for OpenID Connect Iframe Session support!
        builder.cookie(new NewCookie(KEYCLOAK_SESSION_COOKIE, sessionCookieValue, cookiePath, null, null, maxAge, secureOnly));

    }

    public void createRememberMeCookie(HttpResponse response, RealmModel realm, UriInfo uriInfo) {
        String path = getIdentityCookiePath(realm, uriInfo);
        boolean secureOnly = !realm.isSslNotRequired();
        // remember me cookie should be persistent
        //NewCookie cookie = new NewCookie(KEYCLOAK_REMEMBER_ME, "true", path, null, null, realm.getCentralLoginLifespan(), secureOnly);// todo httponly , true);
        CookieHelper.addCookie(KEYCLOAK_REMEMBER_ME, "true", path, null, null, realm.getCentralLoginLifespan(), secureOnly, true);
    }

    protected String encodeToken(RealmModel realm, Object token) {
        String encodedToken = new JWSBuilder()
                .jsonContent(token)
                .rsa256(realm.getPrivateKey());
        return encodedToken;
    }

    public void expireIdentityCookie(RealmModel realm, UriInfo uriInfo) {
        logger.debug("Expiring identity cookie");
        String path = getIdentityCookiePath(realm, uriInfo);
        expireCookie(realm, KEYCLOAK_IDENTITY_COOKIE, path, true);
        expireCookie(realm, KEYCLOAK_SESSION_COOKIE, path, false);
    }
    public void expireRememberMeCookie(RealmModel realm, UriInfo uriInfo) {
        logger.debug("Expiring remember me cookie");
        String path = getIdentityCookiePath(realm, uriInfo);
        String cookieName = KEYCLOAK_REMEMBER_ME;
        expireCookie(realm, cookieName, path, true);
    }

    protected String getIdentityCookiePath(RealmModel realm, UriInfo uriInfo) {
        URI uri = RealmsResource.realmBaseUrl(uriInfo).build(realm.getName());
        return uri.getRawPath();
    }

    public void expireCookie(RealmModel realm, String cookieName, String path, boolean httpOnly) {
        logger.debugv("Expiring cookie: {0} path: {1}", cookieName, path);
        boolean secureOnly = !realm.isSslNotRequired();
        CookieHelper.addCookie(cookieName, "", path, null, "Expiring cookie", 0, secureOnly, httpOnly);
    }

    public AuthResult authenticateIdentityCookie(RealmModel realm, UriInfo uriInfo, HttpHeaders headers) {
        return authenticateIdentityCookie(realm, uriInfo, headers, true);
    }

    public AuthResult authenticateIdentityCookie(RealmModel realm, UriInfo uriInfo, HttpHeaders headers, boolean checkActive) {
        logger.info("authenticateIdentityCookie");
        String cookieName = KEYCLOAK_IDENTITY_COOKIE;
        return authenticateIdentityCookie(realm, uriInfo, headers, cookieName, checkActive);
    }

    private AuthResult authenticateIdentityCookie(RealmModel realm, UriInfo uriInfo, HttpHeaders headers, String cookieName, boolean checkActive) {
        logger.info("authenticateIdentityCookie");
        Cookie cookie = headers.getCookies().get(cookieName);
        if (cookie == null) {
            logger.infov("authenticateCookie could not find cookie: {0}", cookieName);
            return null;
        }

        String tokenString = cookie.getValue();
        AuthResult authResult = verifyIdentityToken(realm, uriInfo, checkActive, tokenString);
        if (authResult == null) {
            expireIdentityCookie(realm, uriInfo);
        }
        return authResult;
    }

    protected AuthResult verifyIdentityToken(RealmModel realm, UriInfo uriInfo, boolean checkActive, String tokenString) {
        try {
            AccessToken token = RSATokenVerifier.verifyToken(tokenString, realm.getPublicKey(), realm.getName(), checkActive);
            logger.info("identity token verified");
            if (checkActive) {
                logger.info("Checking if identity token is active");
                if (!token.isActive() || token.getIssuedAt() < realm.getNotBefore()) {
                    logger.info("identity cookie expired");
                    expireIdentityCookie(realm, uriInfo);
                    return null;
                } else {
                    logger.info("token.isActive() : " + token.isActive());
                    logger.info("token.issuedAt: " + token.getIssuedAt());
                    logger.info("real.notbefore: " + realm.getNotBefore());
                }
            }

            UserModel user = realm.getUserById(token.getSubject());
            if (user == null || !user.isEnabled() ) {
                logger.info("Unknown user in identity token");
                return null;
            }

            if (token.getIssuedAt() < user.getNotBefore()) {
                logger.info("Stale cookie");
                return null;
            }

            UserSessionModel session = realm.getUserSession(token.getSessionState());
            if (session == null || session.getExpires() < Time.currentTime()) {
                logger.info("User session not active");
                expireIdentityCookie(realm, uriInfo);
                return null;
            }

            return new AuthResult(user, session);
        } catch (VerificationException e) {
            logger.info("Failed to verify identity token", e);
        }
        return null;
    }

    public AuthenticationStatus authenticateForm(ClientConnection clientConnection, RealmModel realm, MultivaluedMap<String, String> formData) {
        String username = formData.getFirst(FORM_USERNAME);
        if (username == null) {
            logger.warn("Username not provided");
            return AuthenticationStatus.INVALID_USER;
        }

        if (realm.isBruteForceProtected()) {
            if (protector.isTemporarilyDisabled(realm, username)) {
                return AuthenticationStatus.ACCOUNT_TEMPORARILY_DISABLED;
            }
        }

        AuthenticationStatus status = authenticateInternal(realm, formData, username);
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

    protected AuthenticationStatus authenticateInternal(RealmModel realm, MultivaluedMap<String, String> formData, String username) {
        UserModel user = KeycloakModelUtils.findUserByNameOrEmail(realm, username);
        if (user == null) {
            AuthUser authUser = AuthenticationProviderManager.getManager(realm, providerSession).getUser(username);
            if (authUser != null) {
                // Create new user and link him with authentication provider
                user = realm.addUser(authUser.getUsername());
                user.setEnabled(true);
                user.setFirstName(authUser.getFirstName());
                user.setLastName(authUser.getLastName());
                user.setEmail(authUser.getEmail());
                realm.setAuthenticationLink(user, new AuthenticationLinkModel(authUser.getProviderName(), authUser.getId()));
                logger.info("User " + authUser.getUsername() + " created and linked with provider " + authUser.getProviderName());
            } else {
                logger.warn("User " + username + " not found");
                return AuthenticationStatus.INVALID_USER;
            }
        }

        if (!checkEnabled(user)) {
            return AuthenticationStatus.ACCOUNT_DISABLED;
        }

        Set<String> types = new HashSet<String>();

        for (RequiredCredentialModel credential : realm.getRequiredCredentials()) {
            types.add(credential.getType());
        }

        if (types.contains(CredentialRepresentation.PASSWORD)) {
            String password = formData.getFirst(CredentialRepresentation.PASSWORD);
            if (password == null) {
                logger.warn("Password not provided");
                return AuthenticationStatus.MISSING_PASSWORD;
            }

            if (user.isTotp()) {
                String token = formData.getFirst(CredentialRepresentation.TOTP);
                if (token == null) {
                    logger.warn("TOTP token not provided");
                    return AuthenticationStatus.MISSING_TOTP;
                }

                logger.debug("validating TOTP");
                if (!realm.validateTOTP(user, password, token)) {
                    return AuthenticationStatus.INVALID_CREDENTIALS;
                }
            } else {
                logger.debug("validating password for user: " + username);

                AuthProviderStatus authStatus = AuthenticationProviderManager.getManager(realm, providerSession).validatePassword(user, password);
                if (authStatus == AuthProviderStatus.INVALID_CREDENTIALS) {
                    logger.debug("invalid password for user: " + username);
                    return AuthenticationStatus.INVALID_CREDENTIALS;
                } else if (authStatus == AuthProviderStatus.FAILED) {
                    return AuthenticationStatus.FAILED;
                }
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
            logger.warn("Account is disabled, contact admin. " + user.getLoginName());
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

        public AuthResult(UserModel user, UserSessionModel session) {
            this.user = user;
            this.session = session;
        }

        public UserSessionModel getSession() {
            return session;
        }

        public UserModel getUser() {
            return user;
        }
    }

}
