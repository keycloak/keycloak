package org.keycloak.authentication.authenticators;

import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.managers.AuthenticationManager;

/**
 * No auth, but it sets a required action.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SetRequiredActionAuthenticator implements Authenticator {

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public void authenticate(AuthenticatorContext context) {
        UserModel user = context.getUser();
        if (user == null) {
            throw new AuthenticationProcessor.AuthException(AuthenticationProcessor.Error.UNKNOWN_USER);
        }
        user.addRequiredAction(context.getAuthenticatorModel().getConfig().get("required.action"));
        context.success();
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true;
    }

    @Override
    public String getRequiredAction() {
        return null;
    }

    @Override
    public void close() {

    }
}
