package org.keycloak.policy;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.PasswordPolicy;

public class CustomUpdatePasswordAuthenticator implements Authenticator {

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        KeycloakSession session = context.getSession();
        RealmModel realm = context.getRealm();
        PasswordPolicy policy = realm.getPasswordPolicy();

        Integer minLength = policy.getPolicyConfig("length");
        Integer maxLength = policy.getPolicyConfig("maxLength");
        Boolean requireDigits = policy.getPolicyConfig("digits") != null;
        Boolean requireLowerCase = policy.getPolicyConfig("lowerCase") != null;
        Boolean requireUpperCase = policy.getPolicyConfig("upperCase") != null;

        context.getAuthenticationSession().setAuthNote("minLength", minLength != null ? minLength.toString() : "0");
        context.getAuthenticationSession().setAuthNote("maxLength", maxLength != null ? maxLength.toString() : "0");
        context.getAuthenticationSession().setAuthNote("requireDigits", requireDigits.toString());
        context.getAuthenticationSession().setAuthNote("requireLowerCase", requireLowerCase.toString());
        context.getAuthenticationSession().setAuthNote("requireUpperCase", requireUpperCase.toString());

        context.success();
    }

    @Override
    public void action(AuthenticationFlowContext context) {
    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return false;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
    }

    @Override
    public void close() {
    }
}
