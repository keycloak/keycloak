package org.keycloak.ssf.transmitter.support;

import java.util.List;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import org.keycloak.models.KeycloakSession;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.ssf.Ssf;
import org.keycloak.utils.KeycloakSessionUtil;

public class SsfAuthUtil {

    public static final String AUTH_KEY = "auth";

    public static AuthenticationManager.AuthResult authenticate() {
        KeycloakSession session = KeycloakSessionUtil.getKeycloakSession();
        var authenticator = new AppAuthManager.BearerTokenAuthenticator(session);
        var auth = authenticator.authenticate();
        if (auth == null) {
            throw new WebApplicationException(Response.Status.UNAUTHORIZED);
        }
        // make the current authentication available on the session
        SsfAuthUtil.setAuth(session, auth);
        return auth;
    }

    private static void setAuth(KeycloakSession session, AuthenticationManager.AuthResult auth) {
        session.setAttribute(AUTH_KEY, auth);
    }

    private static AuthenticationManager.AuthResult getAuthResult() {
        return (AuthenticationManager.AuthResult)KeycloakSessionUtil.getKeycloakSession().getAttribute(AUTH_KEY);
    }

    public static boolean canManage() {
        return checkScopePermission(Ssf.SCOPE_SSF_MANAGE);
    }

    public static boolean canRead() {
        return checkScopePermission(Ssf.SCOPE_SSF_READ);
    }

    public static boolean checkScopePermission(String scope) {
        var authResult = getAuthResult();
        if (authResult == null) {
            return false;
        }

        // only check that the client has the ssf.enabled attribute
        if (!Boolean.parseBoolean(authResult.client().getAttribute("ssf.enabled"))) {
            return false;
        }

        if (!authResult.client().isServiceAccountsEnabled()) {
            return false;
        }

        // only the service account should be able to do this
        if (authResult.client().getClientId().equals(authResult.user().getServiceAccountClientLink())) {
            return false;
        }


        return List.of(authResult.token().getScope().split(" ")).contains(scope);
    }
}
