package org.keycloak.protocol.ssf.support;

import java.util.List;

import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.utils.KeycloakSessionUtil;

public class SsfAuthUtil {

    public static AuthenticationManager.AuthResult getAuthResult() {
        return (AuthenticationManager.AuthResult)KeycloakSessionUtil.getKeycloakSession().getAttribute("auth");
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
