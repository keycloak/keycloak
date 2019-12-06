package org.keycloak.authentication.authenticators.conditional;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;

import java.util.Set;

public class ConditionalRoleAuthenticator implements ConditionalAuthenticator {
    public static final ConditionalRoleAuthenticator SINGLETON = new ConditionalRoleAuthenticator();

    @Override
    public boolean matchCondition(AuthenticationFlowContext context) {
        UserModel user = context.getUser();
        AuthenticatorConfigModel authConfig = context.getAuthenticatorConfig();
        if (user != null && authConfig!=null && authConfig.getConfig()!=null) {
            Set<RoleModel> roles = user.getRoleMappings();
            String requiredRole = authConfig.getConfig().get(ConditionalRoleAuthenticatorFactory.CONDITIONAL_USER_ROLE);
            return roles.stream().anyMatch(r -> r.getName().equals(requiredRole));
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
