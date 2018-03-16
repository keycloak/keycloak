package org.keycloak.authentication;

import org.keycloak.OAuth2Constants;

/**
 * Determine OIDC display parameter type.
 *
 */
public class DisplayUtils {

    public static boolean isConsole(AuthenticationFlowContext context) {
        String displayParam = context.getAuthenticationSession().getClientNote(OAuth2Constants.DISPLAY);
        return displayParam != null && displayParam.equals("console");
    }
    public static boolean isConsole(RequiredActionContext context) {
        String displayParam = context.getAuthenticationSession().getClientNote(OAuth2Constants.DISPLAY);
        return displayParam != null && displayParam.equals("console");
    }

}
