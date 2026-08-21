package org.keycloak.authentication.authenticators.conditional;

import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.models.*;
import org.keycloak.models.utils.KeycloakModelUtils;

public class ConditionalGroupAuthenticator implements ConditionalAuthenticator {
    public static final ConditionalGroupAuthenticator SINGLETON = new ConditionalGroupAuthenticator();
    private static final Logger logger = Logger.getLogger(ConditionalGroupAuthenticator.class);

    @Override
    public boolean matchCondition(AuthenticationFlowContext context) {
        UserModel user = context.getUser();
        RealmModel realm = context.getRealm();
        AuthenticatorConfigModel authConfig = context.getAuthenticatorConfig();
        if (user != null && authConfig!=null && authConfig.getConfig()!=null) {
            String requiredGroup = authConfig.getConfig().get(ConditionalGroupAuthenticatorFactory.CONDITIONAL_USER_GROUP);
            boolean negateOutput = Boolean.parseBoolean(authConfig.getConfig().get(ConditionalGroupAuthenticatorFactory.CONF_NEGATE));
            GroupModel group = KeycloakModelUtils.findGroupByPath(context.getSession(), realm, requiredGroup);
            if (group == null) {
                logger.errorv("Invalid group name submitted: {0}", requiredGroup);
                return false;
            }

            return negateOutput != user.isMemberOf(group);
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
