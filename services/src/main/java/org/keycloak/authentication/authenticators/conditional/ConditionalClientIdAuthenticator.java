package org.keycloak.authentication.authenticators.conditional;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

final class ConditionalClientIdAuthenticator implements ConditionalAuthenticator {

    ConditionalClientIdAuthenticator() {
    }

    @Override
    public boolean matchCondition(AuthenticationFlowContext authenticationFlowContext) {
        ClientModel client = authenticationFlowContext.getAuthenticationSession().getClient();
        ConditionalClientIdConfig config = new ConditionalClientIdConfig(
            authenticationFlowContext.getAuthenticatorConfig());
        String clientId = client.getClientId();
        boolean matches = config.getClientIds().contains(clientId);
        return config.isNegateOutput() != matches;
    }

    @Override
    public void action(AuthenticationFlowContext authenticationFlowContext) {
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
