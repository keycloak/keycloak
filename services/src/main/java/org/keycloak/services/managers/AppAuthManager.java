package org.keycloak.services.managers;

import org.jboss.resteasy.logging.Logger;
import org.jboss.resteasy.spi.HttpResponse;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.RSATokenVerifier;
import org.keycloak.VerificationException;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.crypto.RSAProvider;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ProviderSession;
import org.keycloak.representations.AccessToken;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.NewCookie;
import java.net.URI;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class AppAuthManager extends AuthenticationManager {
    protected static Logger logger = Logger.getLogger(AppAuthManager.class);

    private String cookieName;
    private TokenManager tokenManager;

    public AppAuthManager(ProviderSession providerSession, String cookieName, TokenManager tokenManager) {
        super(providerSession);
        this.cookieName = cookieName;
        this.tokenManager = tokenManager;
    }

    public NewCookie createCookie(RealmModel realm, ClientModel client, String code, URI uri) {
        JWSInput input = new JWSInput(code);
        boolean verifiedCode = false;
        try {
            verifiedCode = RSAProvider.verify(input, realm.getPublicKey());
        } catch (Exception ignored) {
            logger.debug("Failed to verify signature", ignored);
        }
        if (!verifiedCode) {
            logger.debug("unverified access code");
            throw new BadRequestException();
        }
        String key = input.readContentAsString();
        AccessCodeEntry accessCode = tokenManager.pullAccessCode(key);
        if (accessCode == null) {
            logger.debug("bad access code");
            throw new BadRequestException();
        }
        if (accessCode.isExpired()) {
            logger.debug("access code expired");
            throw new BadRequestException();
        }
        if (!accessCode.getToken().isActive()) {
            logger.debug("access token expired");
            throw new BadRequestException();
        }
        if (!accessCode.getRealm().getId().equals(realm.getId())) {
            logger.debug("bad realm");
            throw new BadRequestException();

        }
        if (!client.getClientId().equals(accessCode.getClient().getClientId())) {
            logger.debug("bad client");
            throw new BadRequestException();
        }

        return createLoginCookie(realm, accessCode.getUser(), accessCode.getClient(), cookieName, uri.getRawPath(), false);
    }

    public NewCookie createRefreshCookie(RealmModel realm, UserModel user, ClientModel client, URI uri) {
        return createLoginCookie(realm, user, client, cookieName, uri.getRawPath(), false);
    }

    public void expireCookie(URI uri) {
        expireCookie(cookieName, uri.getRawPath());
    }

    public Auth authenticateCookie(RealmModel realm, HttpHeaders headers) {
        return authenticateCookie(realm, headers, cookieName, true);
    }

    public Auth authenticate(RealmModel realm, HttpHeaders headers) {
        Auth auth = authenticateCookie(realm, headers);
        if (auth != null) return auth;
        return authenticateBearerToken(realm, headers);
    }

    private Auth authenticateCookie(RealmModel realm, HttpHeaders headers, String cookieName, boolean checkActive) {
        logger.info("authenticateCookie");
        Cookie cookie = headers.getCookies().get(cookieName);
        if (cookie == null) {
            logger.info("authenticateCookie could not find cookie: {0}", cookieName);
            return null;
        }

        String tokenString = cookie.getValue();
        try {
            AccessToken token = RSATokenVerifier.verifyToken(tokenString, realm.getPublicKey(), realm.getName(), checkActive);
            logger.info("token verified");
            if (checkActive && !token.isActive()) {
                logger.info("cookie expired");
                expireCookie(cookie.getName(), cookie.getPath());
                return null;
            }

            UserModel user = realm.getUserById(token.getSubject());
            if (user == null || !user.isEnabled()) {
                logger.info("Unknown user in cookie");
                expireCookie(cookie.getName(), cookie.getPath());
                return null;
            }

            ClientModel client = null;
            if (token.getIssuedFor() != null) {
                client = realm.findClient(token.getIssuedFor());
                if (client == null || !client.isEnabled()) {
                    logger.info("Unknown client in cookie");
                    expireCookie(cookie.getName(), cookie.getPath());
                    return null;
                }
            }

            return new Auth(realm, user, client);
        } catch (VerificationException e) {
            logger.info("Failed to verify cookie", e);
            expireCookie(cookie.getName(), cookie.getPath());
        }
        return null;
    }

    private Auth authenticateBearerToken(RealmModel realm, HttpHeaders headers) {
        String tokenString;
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

            UserModel user = realm.getUserById(token.getSubject());
            if (user == null || !user.isEnabled()) {
                throw new NotAuthorizedException("invalid_user");
            }

            ClientModel client = null;
            if (token.getIssuedFor() != null) {
                client = realm.findClient(token.getIssuedFor());
                if (client == null || !client.isEnabled()) {
                    throw new NotAuthorizedException("invalid_user");
                }
            }

            return new Auth(token, user, client);
        } catch (VerificationException e) {
            logger.error("Failed to verify token", e);
            throw new NotAuthorizedException("invalid_token");
        }
    }

}
