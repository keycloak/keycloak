package org.keycloak.services.managers;

import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.UnauthorizedException;
import org.keycloak.ClientConnection;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class AppAuthManager extends AuthenticationManager {

    protected static Logger logger = Logger.getLogger(AppAuthManager.class);

    @Override
    public AuthResult authenticateIdentityCookie(KeycloakSession session, RealmModel realm, UriInfo uriInfo, ClientConnection connection, HttpHeaders headers) {
        AuthResult authResult = super.authenticateIdentityCookie(session, realm, uriInfo, connection, headers);
        if (authResult == null) return null;
        // refresh the cookies!
        createLoginCookie(realm, authResult.getUser(), authResult.getSession(), uriInfo, connection);
        if (authResult.getSession().isRememberMe()) createRememberMeCookie(realm, authResult.getUser().getUsername(), uriInfo, connection);
        return authResult;
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

    public AuthResult authenticateBearerToken(KeycloakSession session, RealmModel realm, UriInfo uriInfo, ClientConnection connection, HttpHeaders headers) {
        String tokenString = extractAuthorizationHeaderToken(headers);
        if (tokenString == null) return null;
        AuthResult authResult = verifyIdentityToken(session, realm, uriInfo, connection, true, tokenString, headers);
        return authResult;
    }

}
