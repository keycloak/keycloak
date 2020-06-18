package org.keycloak.authentication.authenticators.conditional;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.authenticators.conditional.ConditionalAuthenticator;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.GroupModel;

public class ConditionalUserGroupAuthenticator implements ConditionalAuthenticator {
    public static final ConditionalUserGroupAuthenticator SINGLETON = new ConditionalUserGroupAuthenticator();

    @Override
    public boolean matchCondition(AuthenticationFlowContext context) {
        UserModel user = context.getUser();
        AuthenticatorConfigModel authConfig = context.getAuthenticatorConfig();
        if (user != null && authConfig!=null && authConfig.getConfig()!=null) {

            String requiredGroup = authConfig.getConfig().get(ConditionalUserGroupAuthenticatorFactory.CONDITIONAL_USER_GROUP);
            GroupModel found = KeycloakModelUtils.findGroupByPath(context.getRealm(), requiredGroup);
            
            if (found != null && user.getGroups() != null && user.getGroups().size()>0 ) {
                return user.isMemberOf(found);
            }
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
