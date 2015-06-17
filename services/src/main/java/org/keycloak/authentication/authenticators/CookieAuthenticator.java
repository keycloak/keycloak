package org.keycloak.authentication.authenticators;

import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.managers.AuthenticationManager;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class CookieAuthenticator implements Authenticator {

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public void authenticate(AuthenticatorContext context) {
        AuthenticationManager.AuthResult authResult = AuthenticationManager.authenticateIdentityCookie(context.getSession(),
                context.getRealm(), context.getUriInfo(), context.getConnection(), context.getHttpRequest().getHttpHeaders(), true);
        if (authResult == null) {
            context.attempted();
        } else {
            context.setUser(authResult.getUser());
            context.attachUserSession(authResult.getSession());
            context.success();
        }

    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
    }

    @Override
    public void close() {

    }
}
