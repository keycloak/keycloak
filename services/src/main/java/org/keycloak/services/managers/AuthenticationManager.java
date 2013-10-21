package org.keycloak.services.managers;

import org.jboss.resteasy.jose.jws.JWSBuilder;
import org.jboss.resteasy.jwt.JsonSerialization;
import org.jboss.resteasy.logging.Logger;
import org.jboss.resteasy.spi.HttpResponse;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.RSATokenVerifier;
import org.keycloak.VerificationException;
import org.keycloak.models.Constants;
import org.keycloak.representations.SkeletonKeyToken;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.resources.AccountService;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.services.resources.SaasService;

import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.core.*;
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
    protected Logger logger = Logger.getLogger(AuthenticationManager.class);
    public static final String FORM_USERNAME = "username";
    public static final String KEYCLOAK_IDENTITY_COOKIE = "KEYCLOAK_IDENTITY";

    public SkeletonKeyToken createIdentityToken(RealmModel realm, String username) {
        SkeletonKeyToken token = new SkeletonKeyToken();
        token.id(RealmManager.generateId());
        token.issuedNow();
        token.principal(username);
        token.audience(realm.getId());
        if (realm.getTokenLifespan() > 0) {
            token.expiration((System.currentTimeMillis() / 1000) + realm.getTokenLifespan());
        }
        return token;
    }


    public NewCookie createLoginCookie(RealmModel realm, UserModel user, UriInfo uriInfo) {
        String cookieName = KEYCLOAK_IDENTITY_COOKIE;
        URI uri = RealmsResource.realmBaseUrl(uriInfo).build(realm.getId());
        String cookiePath = uri.getPath();
        return createLoginCookie(realm, user, cookieName, cookiePath);
    }

    public NewCookie createSaasIdentityCookie(RealmModel realm, UserModel user, UriInfo uriInfo) {
        String cookieName = SaasService.SAAS_IDENTITY_COOKIE;
        URI uri = SaasService.saasCookiePath(uriInfo).build();
        String cookiePath = uri.getPath();
        return createLoginCookie(realm, user, cookieName, cookiePath);
    }

    public NewCookie createAccountIdentityCookie(RealmModel realm, UserModel user, URI uri) {
        String cookieName = AccountService.ACCOUNT_IDENTITY_COOKIE;
        String cookiePath = uri.getPath();
        return createLoginCookie(realm, user, cookieName, cookiePath);
    }

    protected NewCookie createLoginCookie(RealmModel realm, UserModel user, String cookieName, String cookiePath) {
        SkeletonKeyToken identityToken = createIdentityToken(realm, user.getLoginName());
        String encoded = encodeToken(realm, identityToken);
        boolean secureOnly = !realm.isSslNotRequired();
        logger.info("creatingLoginCookie - name: " + cookieName + " path: " + cookiePath);
        NewCookie cookie = new NewCookie(cookieName, encoded, cookiePath, null, null, NewCookie.DEFAULT_MAX_AGE, secureOnly, true);
        return cookie;
    }

    protected String encodeToken(RealmModel realm, Object token) {
        byte[] tokenBytes = null;
        try {
            tokenBytes = JsonSerialization.toByteArray(token, false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        String encodedToken = new JWSBuilder()
                .content(tokenBytes)
                .rsa256(realm.getPrivateKey());
        return encodedToken;
    }


    public void expireIdentityCookie(RealmModel realm, UriInfo uriInfo) {
        URI uri = RealmsResource.realmBaseUrl(uriInfo).build(realm.getId());
        logger.info("Expiring identity cookie");
        String path = uri.getPath();
        String cookieName = KEYCLOAK_IDENTITY_COOKIE;
        expireCookie(cookieName, path);
    }

    public void expireSaasIdentityCookie(UriInfo uriInfo) {
        URI uri = SaasService.saasCookiePath(uriInfo).build();
        String cookiePath = uri.getPath();
        expireCookie(SaasService.SAAS_IDENTITY_COOKIE, cookiePath);
    }

    public void expireAccountIdentityCookie(URI uri) {
        String cookiePath = uri.getPath();
        expireCookie(AccountService.ACCOUNT_IDENTITY_COOKIE, cookiePath);
    }

    public void expireCookie(String cookieName, String path) {
        HttpResponse response = ResteasyProviderFactory.getContextData(HttpResponse.class);
        if (response == null) {
            logger.info("can't expire identity cookie, no HttpResponse");
            return;
        }
        logger.info("Expiring cookie: " + cookieName + " path: " + path);
        NewCookie expireIt = new NewCookie(cookieName, "", path, null, "Expiring cookie", 0, false);
        response.addNewCookie(expireIt);
    }

    public UserModel authenticateIdentityCookie(RealmModel realm, UriInfo uriInfo, HttpHeaders headers) {
        String cookieName = KEYCLOAK_IDENTITY_COOKIE;
        return authenticateIdentityCookie(realm, uriInfo, headers, cookieName);
    }

    public UserModel authenticateSaasIdentityCookie(RealmModel realm, UriInfo uriInfo, HttpHeaders headers) {
        String cookieName = SaasService.SAAS_IDENTITY_COOKIE;
        return authenticateIdentityCookie(realm, uriInfo, headers, cookieName);
    }

    public UserModel authenticateAccountIdentityCookie(RealmModel realm, UriInfo uriInfo, HttpHeaders headers) {
        String cookieName = AccountService.ACCOUNT_IDENTITY_COOKIE;
        return authenticateIdentityCookie(realm, uriInfo, headers, cookieName);
    }

    public UserModel authenticateSaasIdentity(RealmModel realm, UriInfo uriInfo, HttpHeaders headers) {
        UserModel user = authenticateSaasIdentityCookie(realm, uriInfo, headers);
        if (user != null) return user;

        return authenticateBearerToken(realm, headers);
    }


    protected UserModel authenticateIdentityCookie(RealmModel realm, UriInfo uriInfo, HttpHeaders headers, String cookieName) {
        Cookie cookie = headers.getCookies().get(cookieName);
        if (cookie == null) {
            logger.info("authenticateCookie could not find cookie: " + cookieName);
            return null;
        }

        String tokenString = cookie.getValue();
        try {
            SkeletonKeyToken token = RSATokenVerifier.verifyToken(tokenString, realm.getPublicKey(), realm.getId());
            if (!token.isActive()) {
                logger.info("identity cookie expired");
                expireIdentityCookie(realm, uriInfo);
                return null;
            }
            UserModel user = realm.getUser(token.getPrincipal());
            if (user == null || !user.isEnabled()) {
                logger.info("Unknown user in identity cookie");
                expireIdentityCookie(realm, uriInfo);
                return null;
            }
            return user;
        } catch (VerificationException e) {
            logger.info("Failed to verify identity cookie", e);
            expireIdentityCookie(realm, uriInfo);
        }
        return null;
    }

    public UserModel authenticateBearerToken(RealmModel realm, HttpHeaders headers) {
        String tokenString = null;
        String authHeader = headers.getHeaderString(HttpHeaders.AUTHORIZATION);
        if (authHeader == null) {
            throw new NotAuthorizedException("Bearer");
        } else {
            String[] split = authHeader.trim().split("\\s+");
            if (split == null || split.length != 2) throw new NotAuthorizedException("Bearer");
            if (!split[0].equalsIgnoreCase("Bearer")) throw new NotAuthorizedException("Bearer");
            tokenString = split[1];
        }


        try {
            SkeletonKeyToken token = RSATokenVerifier.verifyToken(tokenString, realm.getPublicKey(), realm.getId());
            if (!token.isActive()) {
                throw new NotAuthorizedException("token_expired");
            }
            UserModel user = realm.getUser(token.getPrincipal());
            if (user == null || !user.isEnabled()) {
                throw new NotAuthorizedException("invalid_user");
            }
            return user;
        } catch (VerificationException e) {
            logger.error("Failed to verify token", e);
            throw new NotAuthorizedException("invalid_token");
        }
    }

    public AuthenticationStatus authenticateForm(RealmModel realm, UserModel user, MultivaluedMap<String, String> formData) {
        if (user == null) {
            logger.info("Not Authenticated! Incorrect user name");
            return AuthenticationStatus.INVALID_USER;
        }

        if (!user.isEnabled()) {
            logger.info("Account is disabled, contact admin.");
            return AuthenticationStatus.ACCOUNT_DISABLED;
        }

        Set<String> types = new HashSet<String>();

        List<RequiredCredentialModel> requiredCredentials = null;
        if (realm.hasRole(user, Constants.APPLICATION_ROLE)) {
            requiredCredentials = realm.getRequiredApplicationCredentials();
        } else if (realm.hasRole(user, Constants.IDENTITY_REQUESTER_ROLE)) {
            requiredCredentials = realm.getRequiredOAuthClientCredentials();
        } else {
            requiredCredentials = realm.getRequiredCredentials();
        }

        for (RequiredCredentialModel credential : requiredCredentials) {
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
                logger.info("validating TOTP");
                if (!realm.validateTOTP(user, password, token)) {
                    return AuthenticationStatus.INVALID_CREDENTIALS;
                }
            } else {
                logger.info("validating password for user: " + user.getLoginName());
                if (!realm.validatePassword(user, password)) {
                    return AuthenticationStatus.INVALID_CREDENTIALS;
                }
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
