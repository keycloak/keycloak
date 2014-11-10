package org.keycloak.adapters;

import java.io.IOException;

import org.jboss.logging.Logger;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.RSATokenVerifier;
import org.keycloak.VerificationException;
import org.keycloak.constants.AdapterConstants;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.IDToken;
import org.keycloak.util.KeycloakUriBuilder;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class CookieTokenStore {

    private static final Logger log = Logger.getLogger(CookieTokenStore.class);
    private static final String DELIM = "@";

    public static void setTokenCookie(KeycloakDeployment deployment, HttpFacade facade, RefreshableKeycloakSecurityContext session) {
        log.debugf("Set new %s cookie now", AdapterConstants.KEYCLOAK_ADAPTER_STATE_COOKIE);
        String accessToken = session.getTokenString();
        String idToken = session.getIdTokenString();
        String refreshToken = session.getRefreshToken();
        String cookie = new StringBuilder(accessToken).append(DELIM)
                .append(idToken).append(DELIM)
                .append(refreshToken).toString();

        String cookiePath = getContextPath(facade);
        facade.getResponse().setCookie(AdapterConstants.KEYCLOAK_ADAPTER_STATE_COOKIE, cookie, cookiePath, null, -1, deployment.getSslRequired().isRequired(facade.getRequest().getRemoteAddr()), true);
    }

    public static KeycloakPrincipal<RefreshableKeycloakSecurityContext> getPrincipalFromCookie(KeycloakDeployment deployment, HttpFacade facade, AdapterTokenStore tokenStore) {
        HttpFacade.Cookie cookie = facade.getRequest().getCookie(AdapterConstants.KEYCLOAK_ADAPTER_STATE_COOKIE);
        if (cookie == null) {
            log.debug("Not found adapter state cookie in current request");
            return null;
        }

        String cookieVal = cookie.getValue();

        String[] tokens = cookieVal.split(DELIM);
        if (tokens.length != 3) {
            log.warnf("Invalid format of %s cookie. Count of tokens: %s, expected 3", AdapterConstants.KEYCLOAK_ADAPTER_STATE_COOKIE, tokens.length);
            return null;
        }

        String accessTokenString = tokens[0];
        String idTokenString = tokens[1];
        String refreshTokenString = tokens[2];

        try {
            // Skip check if token is active now. It's supposed to be done later by the caller
            AccessToken accessToken = RSATokenVerifier.verifyToken(accessTokenString, deployment.getRealmKey(), deployment.getRealm(), false);
            IDToken idToken;
            if (idTokenString != null && idTokenString.length() > 0) {
                JWSInput input = new JWSInput(idTokenString);
                try {
                    idToken = input.readJsonContent(IDToken.class);
                } catch (IOException e) {
                    throw new VerificationException(e);
                }
            } else {
                idToken = null;
            }

            log.debug("Token Verification succeeded!");
            RefreshableKeycloakSecurityContext secContext = new RefreshableKeycloakSecurityContext(deployment, tokenStore, accessTokenString, accessToken, idTokenString, idToken, refreshTokenString);
            return new KeycloakPrincipal<RefreshableKeycloakSecurityContext>(AdapterUtils.getPrincipalName(deployment, accessToken), secContext);
        } catch (VerificationException ve) {
            log.warn("Failed verify token", ve);
            return null;
        }
    }

    public static void removeCookie(HttpFacade facade) {
        String cookiePath = getContextPath(facade);
        facade.getResponse().resetCookie(AdapterConstants.KEYCLOAK_ADAPTER_STATE_COOKIE, cookiePath);
    }

    private static String getContextPath(HttpFacade facade) {
        String uri = facade.getRequest().getURI();
        String path = KeycloakUriBuilder.fromUri(uri).getPath();
        int index = path.indexOf("/", 1);
        return index == -1 ? path : path.substring(0, index);
    }
}
