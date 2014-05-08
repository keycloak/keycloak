package org.keycloak.services.managers;

import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.UnauthorizedException;
import org.keycloak.RSATokenVerifier;
import org.keycloak.VerificationException;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.crypto.RSAProvider;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ProviderSession;
import org.keycloak.representations.AccessToken;

import org.jboss.resteasy.spi.BadRequestException;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.UriInfo;
import java.net.URI;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class AppAuthManager extends AuthenticationManager {
    protected static Logger logger = Logger.getLogger(AppAuthManager.class);

    public AppAuthManager(ProviderSession providerSession) {
        super(providerSession);
    }

    public UserModel authenticateRequest(RealmModel realm, UriInfo uriInfo, HttpHeaders headers) {
        UserModel user = authenticateIdentityCookie(realm, uriInfo, headers);
        if (user != null) return user;
        return authenticateBearerToken(realm, uriInfo, headers);
    }

    public String extractAuthorizationHeaderToken(HttpHeaders headers) {
        String tokenString = null;
        String authHeader = headers.getRequestHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader != null) {
            String[] split = authHeader.trim().split("\\s+");
            if (split == null || split.length != 2) throw new UnauthorizedException("Bearer");
            if (!split[0].equalsIgnoreCase("Bearer")) throw new UnauthorizedException("Bearer");
            tokenString = split[1];
        }
        return tokenString;
    }

    public UserModel authenticateBearerToken(RealmModel realm, UriInfo uriInfo, HttpHeaders headers) {
        String tokenString = extractAuthorizationHeaderToken(headers);
        if (tokenString == null) return null;
        return verifyIdentityToken(realm, uriInfo, true, tokenString);
    }

}
