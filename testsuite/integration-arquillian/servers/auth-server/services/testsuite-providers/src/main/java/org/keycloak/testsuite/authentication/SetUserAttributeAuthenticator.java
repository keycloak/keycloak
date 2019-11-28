package org.keycloak.testsuite.authentication;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SetUserAttributeAuthenticator implements Authenticator {
    @Override
    public void authenticate(AuthenticationFlowContext context) {
        // Retrieve configuration
        Map<String, String> config = context.getAuthenticatorConfig().getConfig();
        String attrName = config.get(SetUserAttributeAuthenticatorFactory.CONF_ATTR_NAME);
        String attrValue = config.get(SetUserAttributeAuthenticatorFactory.CONF_ATTR_VALUE);

        UserModel user = context.getUser();
        if (user.getAttribute(attrName) == null) {
            user.setSingleAttribute(attrName, attrValue);
        }
        else {
            List<String> attrValues = new ArrayList<>(user.getAttribute(attrName));
            if (!attrValues.contains(attrValue)) {
                attrValues.add(attrValue);
            }
            user.setAttribute(attrName, attrValues);
        }

        context.success();
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        context.failure(AuthenticationFlowError.INTERNAL_ERROR);
    }

    @Override
    public boolean requiresUser() {
        return true;
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
