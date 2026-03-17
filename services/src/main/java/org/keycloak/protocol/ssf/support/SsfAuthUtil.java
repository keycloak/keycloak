package org.keycloak.protocol.ssf.support;

import java.util.List;

import org.keycloak.models.KeycloakSession;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.utils.KeycloakSessionUtil;

public class SsfAuthUtil {

    public static final String AUTH_KEY = "auth";

    public static void setAuth(KeycloakSession session, AuthenticationManager.AuthResult auth) {
        session.setAttribute(AUTH_KEY, auth);
    }

    public static AuthenticationManager.AuthResult getAuthResult() {
        return (AuthenticationManager.AuthResult)KeycloakSessionUtil.getKeycloakSession().getAttribute(AUTH_KEY);
    }

    public static boolean isAuthenticated() {
        var authResult = getAuthResult();
        return authResult != null;
    }

    public static boolean hasScope(String scope) {
        var authResult = getAuthResult();
        if (authResult == null) {
            return false;
        }
        return List.of(authResult.token().getScope().split(" ")).contains(scope);
    }
}
