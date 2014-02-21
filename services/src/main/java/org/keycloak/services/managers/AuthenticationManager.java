package org.keycloak.services.managers;

import org.jboss.resteasy.logging.Logger;
import org.jboss.resteasy.spi.HttpResponse;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.RSATokenVerifier;
import org.keycloak.VerificationException;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.models.Constants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredCredentialModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.services.resources.AccountService;
import org.keycloak.services.resources.admin.AdminService;
import org.keycloak.services.resources.RealmsResource;

import javax.ws.rs.NotAuthorizedException;
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
    protected Logger logger = Logger.getLogger(AuthenticationManager.class);
    public static final String FORM_USERNAME = "username";
    public static final String KEYCLOAK_IDENTITY_COOKIE = "KEYCLOAK_IDENTITY";

    public AccessToken createIdentityToken(RealmModel realm, UserModel user) {
        AccessToken token = new AccessToken();
        token.id(KeycloakModelUtils.generateId());
        token.issuedNow();
        token.subject(user.getId());
        token.audience(realm.getName());
        if (realm.getAccessTokenLifespan() > 0) {
            token.expiration((System.currentTimeMillis() / 1000) + realm.getAccessTokenLifespan());
        }
        return token;
    }


    public NewCookie createLoginCookie(RealmModel realm, UserModel user, UriInfo uriInfo) {
        String cookieName = KEYCLOAK_IDENTITY_COOKIE;
        String cookiePath = getIdentityCookiePath(realm, uriInfo);
        return createLoginCookie(realm, user, null, cookieName, cookiePath);
    }

    public NewCookie createSaasIdentityCookie(RealmModel realm, UserModel user, UriInfo uriInfo) {
        String cookieName = AdminService.SAAS_IDENTITY_COOKIE;
        URI uri = AdminService.saasCookiePath(uriInfo).build();
        String cookiePath = uri.getRawPath();
        return createLoginCookie(realm, user, null, cookieName, cookiePath);
    }

    public NewCookie createAccountIdentityCookie(RealmModel realm, UserModel user, UserModel client, URI uri) {
        String cookieName = AccountService.ACCOUNT_IDENTITY_COOKIE;
        String cookiePath = uri.getRawPath();
        return createLoginCookie(realm, user, client, cookieName, cookiePath);
    }

    protected NewCookie createLoginCookie(RealmModel realm, UserModel user, UserModel client, String cookieName, String cookiePath) {
        AccessToken identityToken = createIdentityToken(realm, user);
        if (client != null) {
            identityToken.issuedFor(client.getLoginName());
        }
        String encoded = encodeToken(realm, identityToken);
        boolean secureOnly = !realm.isSslNotRequired();
        logger.debug("creatingLoginCookie - name: {0} path: {1}", cookieName, cookiePath);
        NewCookie cookie = new NewCookie(cookieName, encoded, cookiePath, null, null, NewCookie.DEFAULT_MAX_AGE, secureOnly, true);
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

    protected String getIdentityCookiePath(RealmModel realm, UriInfo uriInfo) {
        URI uri = RealmsResource.realmBaseUrl(uriInfo).build(realm.getName());
        return uri.getRawPath();
    }

    public void expireSaasIdentityCookie(UriInfo uriInfo) {
        URI uri = AdminService.saasCookiePath(uriInfo).build();
        String cookiePath = uri.getRawPath();
        expireCookie(AdminService.SAAS_IDENTITY_COOKIE, cookiePath);
    }

    public void expireAccountIdentityCookie(URI uri) {
        String cookiePath = uri.getRawPath();
        expireCookie(AccountService.ACCOUNT_IDENTITY_COOKIE, cookiePath);
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
        String cookieName = KEYCLOAK_IDENTITY_COOKIE;
        Auth auth = authenticateIdentityCookie(realm, uriInfo, headers, cookieName);
        return auth != null ? auth.getUser() : null;
    }

    public UserModel authenticateSaasIdentityCookie(RealmModel realm, UriInfo uriInfo, HttpHeaders headers) {
        String cookieName = AdminService.SAAS_IDENTITY_COOKIE;
        Auth auth = authenticateIdentityCookie(realm, uriInfo, headers, cookieName);
        return auth != null ? auth.getUser() : null;
    }

    public Auth authenticateAccountIdentityCookie(RealmModel realm, UriInfo uriInfo, HttpHeaders headers) {
        String cookieName = AccountService.ACCOUNT_IDENTITY_COOKIE;
        return authenticateIdentityCookie(realm, uriInfo, headers, cookieName);
    }

    public UserModel authenticateSaasIdentity(RealmModel realm, UriInfo uriInfo, HttpHeaders headers) {
        UserModel user = authenticateSaasIdentityCookie(realm, uriInfo, headers);
        if (user != null) return user;

        Auth auth = authenticateBearerToken(realm, headers);
        return auth != null ? auth.getUser() : null;
    }

    public Auth authenticateAccountIdentity(RealmModel realm, UriInfo uriInfo, HttpHeaders headers) {
        Auth auth = authenticateAccountIdentityCookie(realm, uriInfo, headers);
        if (auth != null) return auth;

        return authenticateBearerToken(realm, headers);
    }


    protected Auth authenticateIdentityCookie(RealmModel realm, UriInfo uriInfo, HttpHeaders headers, String cookieName) {
        Cookie cookie = headers.getCookies().get(cookieName);
        if (cookie == null) {
            logger.debug("authenticateCookie could not find cookie: {0}", cookieName);
            return null;
        }

        String tokenString = cookie.getValue();
        try {
            AccessToken token = RSATokenVerifier.verifyToken(tokenString, realm.getPublicKey(), realm.getName());
            if (!token.isActive()) {
                logger.debug("identity cookie expired");
                expireIdentityCookie(realm, uriInfo);
                return null;
            }

            Auth auth = new Auth(token);

            UserModel user = realm.getUserById(token.getSubject());
            if (user == null || !user.isEnabled()) {
                logger.debug("Unknown user in identity cookie");
                expireIdentityCookie(realm, uriInfo);
                return null;
            }
            auth.setUser(user);

            if (token.getIssuedFor() != null) {
                UserModel client = realm.getUser(token.getIssuedFor());
                if (client == null || !client.isEnabled()) {
                    logger.debug("Unknown client in identity cookie");
                    expireIdentityCookie(realm, uriInfo);
                    return null;
                }
                auth.setClient(client);
            }

            return auth;
        } catch (VerificationException e) {
            logger.debug("Failed to verify identity cookie", e);
            expireCookie(cookie.getName(), cookie.getPath());
        }
        return null;
    }

    public Auth authenticateBearerToken(RealmModel realm, HttpHeaders headers) {
        String tokenString = null;
        String authHeader = headers.getHeaderString(HttpHeaders.AUTHORIZATION);
        if (authHeader == null) {
            return null;
        } else {
            String[] split = authHeader.trim().split("\\s+");
            if (split == null || split.length != 2) throw new NotAuthorizedException("Bearer");
            if (!split[0].equalsIgnoreCase("Bearer")) throw new NotAuthorizedException("Bearer");
            tokenString = split[1];
        }


        try {
            AccessToken token = RSATokenVerifier.verifyToken(tokenString, realm.getPublicKey(), realm.getName());
            if (!token.isActive()) {
                throw new NotAuthorizedException("token_expired");
            }

            Auth auth = new Auth(token);

            UserModel user = realm.getUserById(token.getSubject());
            if (user == null || !user.isEnabled()) {
                throw new NotAuthorizedException("invalid_user");
            }
            auth.setUser(user);

            if (token.getIssuedFor() != null) {
                UserModel client = realm.getUser(token.getIssuedFor());
                if (client == null || !client.isEnabled()) {
                    throw new NotAuthorizedException("invalid_user");
                }
                auth.setClient(client);
            }

            return auth;
        } catch (VerificationException e) {
            logger.error("Failed to verify token", e);
            throw new NotAuthorizedException("invalid_token");
        }
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

        List<RequiredCredentialModel> requiredCredentials = null;
        RoleModel applicationRole = realm.getRole(Constants.APPLICATION_ROLE);
        RoleModel identityRequesterRole = realm.getRole(Constants.IDENTITY_REQUESTER_ROLE);
        if (realm.hasRole(user, applicationRole)) {
            requiredCredentials = realm.getRequiredApplicationCredentials();
        } else if (realm.hasRole(user, identityRequesterRole)) {
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
            if (!realm.validateSecret(user, secret)) {
                logger.debug("invalid secret for user: " + user.getLoginName());
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

    public enum AuthenticationStatus {
        SUCCESS, ACCOUNT_DISABLED, ACTIONS_REQUIRED, INVALID_USER, INVALID_CREDENTIALS, MISSING_PASSWORD, MISSING_TOTP, FAILED
    }

    public static class Auth {
        private AccessToken token;
        private UserModel user;
        private UserModel client;

        public Auth(AccessToken token) {
            this.token = token;
        }

        public AccessToken getToken() {
            return token;
        }

        public UserModel getUser() {
            return user;
        }

        public UserModel getClient() {
            return client;
        }

        void setUser(UserModel user) {
            this.user = user;
        }

        void setClient(UserModel client) {
            this.client = client;
        }
    }

}
