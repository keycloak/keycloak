package org.keycloak.authentication.authenticators;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

/**
 * Pass-thru atheneticator that just sets the context to attempted.
 */
public class AttemptedAuthenticator implements Authenticator {

    public static final AttemptedAuthenticator SINGLETON = new AttemptedAuthenticator();
    @Override
    public void authenticate(AuthenticationFlowContext context) {
        context.attempted();

    }

    @Override
    public void action(AuthenticationFlowContext context) {
        throw new RuntimeException("Unreachable!");

    }

    @Override
    public boolean requiresUser() {
        return false;
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
