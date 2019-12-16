package org.keycloak.authentication.authenticators.conditional;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.models.*;

public class ConditionalClientAttributeAuthenticator implements ConditionalAuthenticator {
    public static final ConditionalClientAttributeAuthenticator SINGLETON = new ConditionalClientAttributeAuthenticator();

    @Override
    public boolean matchCondition(AuthenticationFlowContext context) {
        ClientModel client = context.getSession().getContext().getClient();
        AuthenticatorConfigModel authenticatorConfig = context.getAuthenticatorConfig();
        String attributeName = authenticatorConfig.getConfig().get(ConditionalClientAttributeAuthenticatorFactory.CONDITIONAL_ATTRIBUTE_NAME);
        String expectedValue = authenticatorConfig.getConfig().get(ConditionalClientAttributeAuthenticatorFactory.CONDITIONAL_ATTRIBUTE_VALUE);
        if (client.getAttributes().containsKey(attributeName)) {
            String actualValue = client.getAttribute(attributeName);
            return expectedValue.equals(actualValue);
        }
        return false;
    }

    @Override
    public void action(AuthenticationFlowContext context) {
    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
    }

    @Override
    public void close() {
    }
}
