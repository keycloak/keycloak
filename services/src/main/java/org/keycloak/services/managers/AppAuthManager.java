package org.keycloak.services.managers;

import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.UnauthorizedException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ProviderSession;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;

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
        AuthResult authResult = authenticateIdentityCookie(realm, uriInfo, headers);
        if (authResult != null) {
            Cookie remember = headers.getCookies().get(AuthenticationManager.KEYCLOAK_REMEMBER_ME);
            boolean rememberMe = remember != null;
            // refresh the cookies!
            createLoginCookie(realm, authResult.getUser(), authResult.getSession(), uriInfo, rememberMe);
            if (rememberMe) createRememberMeCookie(realm, uriInfo);
            return authResult.getUser();
        } else {
            return authenticateBearerToken(realm, uriInfo, headers);
        }
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
        AuthResult authResult = verifyIdentityToken(realm, uriInfo, true, tokenString);
        return authResult != null ? authResult.getUser() : null;
    }

}
