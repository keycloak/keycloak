package org.keycloak.services.managers;

import org.jboss.resteasy.logging.Logger;
import org.jboss.resteasy.spi.HttpResponse;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.RSATokenVerifier;
import org.keycloak.VerificationException;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredCredentialModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.util.Time;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.HashSet;
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
    public static final String KEYCLOAK_IDENTITY_COOKIE = "KEYCLOAK_IDENTITY";
    public static final String KEYCLOAK_REMEMBER_ME = "KEYCLOAK_REMEMBER_ME";

    public AccessToken createIdentityToken(RealmModel realm, UserModel user) {
        logger.info("createIdentityToken");
        AccessToken token = new AccessToken();
        token.id(KeycloakModelUtils.generateId());
        token.issuedNow();
        token.subject(user.getId());
        token.audience(realm.getName());
        if (realm.getCentralLoginLifespan() > 0) {
            token.expiration(Time.currentTime() + realm.getCentralLoginLifespan());
        }
        return token;
    }

    public NewCookie createLoginCookie(RealmModel realm, UserModel user, UriInfo uriInfo, boolean rememberMe) {
        logger.info("createLoginCookie");
        String cookieName = KEYCLOAK_IDENTITY_COOKIE;
        String cookiePath = getIdentityCookiePath(realm, uriInfo);
        return createLoginCookie(realm, user, null, cookieName, cookiePath, rememberMe);
    }

    protected NewCookie createLoginCookie(RealmModel realm, UserModel user, ClientModel client, String cookieName, String cookiePath, boolean rememberMe) {
        AccessToken identityToken = createIdentityToken(realm, user);
        if (client != null) {
            identityToken.issuedFor(client.getClientId());
        }
        String encoded = encodeToken(realm, identityToken);
        boolean secureOnly = !realm.isSslNotRequired();
        logger.debug("creatingLoginCookie - name: {0} path: {1}", cookieName, cookiePath);
        int maxAge = NewCookie.DEFAULT_MAX_AGE;
        if (rememberMe) {
            maxAge = realm.getCentralLoginLifespan();
            logger.info("createLoginCookie maxAge: " + maxAge);
        }
        NewCookie cookie = new NewCookie(cookieName, encoded, cookiePath, null, null, maxAge, secureOnly, true);
        return cookie;
    }

    public NewCookie createRememberMeCookie(RealmModel realm, UriInfo uriInfo) {
        String path = getIdentityCookiePath(realm, uriInfo);
        boolean secureOnly = !realm.isSslNotRequired();
        // remember me cookie should be persistent
        NewCookie cookie = new NewCookie(KEYCLOAK_REMEMBER_ME, "true", path, null, null, realm.getCentralLoginLifespan(), secureOnly, true);
        return cookie;
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
        String cookieName = KEYCLOAK_IDENTITY_COOKIE;
        expireCookie(cookieName, path);
    }
    public void expireRememberMeCookie(RealmModel realm, UriInfo uriInfo) {
        logger.debug("Expiring remember me cookie");
        String path = getIdentityCookiePath(realm, uriInfo);
        String cookieName = KEYCLOAK_REMEMBER_ME;
        expireCookie(cookieName, path);
    }

    protected String getIdentityCookiePath(RealmModel realm, UriInfo uriInfo) {
        URI uri = RealmsResource.realmBaseUrl(uriInfo).build(realm.getName());
        return uri.getRawPath();
    }

    public void expireCookie(String cookieName, String path) {
        HttpResponse response = ResteasyProviderFactory.getContextData(HttpResponse.class);
        if (response == null) {
            logger.debug("can't expire identity cookie, no HttpResponse");
            return;
        }
        logger.debug("Expiring cookie: {0} path: {1}", cookieName, path);
        NewCookie expireIt = new NewCookie(cookieName, "", path, null, "Expiring cookie", 0, false);
        response.addNewCookie(expireIt);
    }

    public UserModel authenticateIdentityCookie(RealmModel realm, UriInfo uriInfo, HttpHeaders headers) {
        return authenticateIdentityCookie(realm, uriInfo, headers, true);
    }

    public UserModel authenticateIdentityCookie(RealmModel realm, UriInfo uriInfo, HttpHeaders headers, boolean checkActive) {
        logger.info("authenticateIdentityCookie");
        String cookieName = KEYCLOAK_IDENTITY_COOKIE;
        return authenticateIdentityCookie(realm, uriInfo, headers, cookieName, checkActive);
    }

    protected UserModel authenticateIdentityCookie(RealmModel realm, UriInfo uriInfo, HttpHeaders headers, String cookieName, boolean checkActive) {
        logger.info("authenticateIdentityCookie");
        Cookie cookie = headers.getCookies().get(cookieName);
        if (cookie == null) {
            logger.info("authenticateCookie could not find cookie: {0}", cookieName);
            return null;
        }

        String tokenString = cookie.getValue();
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
                logger.info("Unknown user in identity cookie");
                expireIdentityCookie(realm, uriInfo);
                return null;
            }

            if (token.getIssuedAt() < user.getNotBefore()) {
                logger.info("Stale cookie");
                expireIdentityCookie(realm, uriInfo);
                return null;

            }

            return user;
        } catch (VerificationException e) {
            logger.info("Failed to verify identity cookie", e);
            expireCookie(cookie.getName(), cookie.getPath());
        }
        return null;
    }

    public AuthenticationStatus authenticateForm(RealmModel realm, UserModel user, MultivaluedMap<String, String> formData) {
        if (user == null) {
            logger.debug("Not Authenticated! Incorrect user name");
            return AuthenticationStatus.INVALID_USER;
        }

        if (!user.isEnabled()) {
            logger.debug("Account is disabled, contact admin. " + user.getLoginName());
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
                logger.debug("validating password for user: " + user.getLoginName());
                if (!realm.validatePassword(user, password)) {
                    logger.debug("invalid password for user: " + user.getLoginName());
                    return AuthenticationStatus.INVALID_CREDENTIALS;
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

    public enum AuthenticationStatus {
        SUCCESS, ACCOUNT_DISABLED, ACTIONS_REQUIRED, INVALID_USER, INVALID_CREDENTIALS, MISSING_PASSWORD, MISSING_TOTP, FAILED
    }

}
