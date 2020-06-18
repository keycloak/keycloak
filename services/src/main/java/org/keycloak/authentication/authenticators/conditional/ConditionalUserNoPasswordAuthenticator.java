package org.keycloak.authentication.authenticators.conditional;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.authenticators.conditional.ConditionalAuthenticator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;


public class ConditionalUserNoPasswordAuthenticator implements ConditionalAuthenticator {
    public static final ConditionalUserNoPasswordAuthenticator SINGLETON = new ConditionalUserNoPasswordAuthenticator();

    @Override
    public boolean matchCondition(AuthenticationFlowContext context) {
        UserModel user = context.getUser();

        if (user != null) {

            return !context.getSession().userCredentialManager().isConfiguredFor(context.getRealm(), context.getUser(), "password");
        }
        return false;
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        // Not used
    }

    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        // Not used
    }

    @Override
    public void close() {
        // Does nothing
}
}
