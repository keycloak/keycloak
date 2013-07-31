package org.keycloak.services.managers;

import org.jboss.resteasy.logging.Logger;
import org.jboss.resteasy.spi.HttpResponse;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.RSATokenVerifier;
import org.keycloak.VerificationException;
import org.keycloak.representations.SkeletonKeyToken;
import org.keycloak.representations.idm.RequiredCredentialRepresentation;
import org.keycloak.services.models.RealmModel;
import org.keycloak.services.models.RequiredCredentialModel;
import org.keycloak.services.models.UserModel;
import org.keycloak.services.resources.RealmsResource;

import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
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
    protected Logger logger = Logger.getLogger(AuthenticationManager.class);
    public static final String FORM_USERNAME = "username";

    /**
     * Grabs token from headers, authenticates, authorizes
     *
     * @param realm
     * @param headers
     * @return
     */
    public boolean isRealmAdmin(RealmModel realm, HttpHeaders headers) {
        UserModel user = authenticateBearerToken(realm, headers);
        return realm.isRealmAdmin(user);
    }

    public void expireIdentityCookie(RealmModel realm, UriInfo uriInfo) {
        URI uri = RealmsResource.realmBaseUrl(uriInfo).build(realm.getId());
        HttpResponse response = ResteasyProviderFactory.getContextData(HttpResponse.class);
        if (response == null) {
            logger.info("can't expire identity cookie, no HttpResponse");
            return;
        }
        logger.info("Expiring identity cookie");
        NewCookie expireIt = new NewCookie(TokenManager.KEYCLOAK_IDENTITY_COOKIE, "", uri.getPath(), null, "Expiring cookie", 0, false);
        response.addNewCookie(expireIt);
    }

    public UserModel authenticateIdentityCookie(RealmModel realm, UriInfo uriInfo, HttpHeaders headers) {
        Cookie cookie = headers.getCookies().get(TokenManager.KEYCLOAK_IDENTITY_COOKIE);
        if (cookie == null) return null;

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

    public boolean authenticateForm(RealmModel realm, UserModel user, MultivaluedMap<String, String> formData) {
        String username = user.getLoginName();
        Set<String> types = new HashSet<String>();

        for (RequiredCredentialModel credential : realm.getRequiredCredentials()) {
            types.add(credential.getType());
        }

        if (types.contains(RequiredCredentialRepresentation.PASSWORD)) {
            String password = formData.getFirst(RequiredCredentialRepresentation.PASSWORD);
            if (password == null) {
                logger.warn("Password not provided");
                return false;
            }

            if (types.contains(RequiredCredentialRepresentation.TOTP)) {
                String token = formData.getFirst(RequiredCredentialRepresentation.TOTP);
                if (token == null) {
                    logger.warn("TOTP token not provided");
                    return false;
                }
                return realm.validateTOTP(user, password, token);
            } else {
                return realm.validatePassword(user, password);
            }
        } else {
            logger.warn("Do not know how to authenticate user");
            return false;
        }
    }
}
